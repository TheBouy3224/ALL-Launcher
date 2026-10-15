package com.movtery.zalithlauncher.coroutine

import com.movtery.zalithlauncher.utils.network.withDownloadTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference

/** 超时、取消、取消后立即重试：任务系统必须报告真实结果，且不让新旧任务互相踩踏 */
class TaskSystemSafetyTest {

    private fun newId() = "safety-${UUID.randomUUID()}"

    @Test
    fun ownTimeoutBecomesATimeoutExceptionNotASilentCancellation() = runBlocking {
        try {
            withDownloadTimeout(50) { delay(5_000) }
            fail("must time out")
        } catch (e: TimeoutException) {
            assertTrue(e.message!!.contains("50"))
        }
    }

    @Test
    fun outerCancellationIsNotDisguisedAsATimeout() = runBlocking {
        val outcome = AtomicReference<Throwable?>()
        val started = CountDownLatch(1)
        val job = async(Dispatchers.Default) {
            try {
                withDownloadTimeout(60_000) { started.countDown(); delay(60_000) }
            } catch (t: Throwable) {
                outcome.set(t)
                throw t
            }
        }
        assertTrue(started.await(5, TimeUnit.SECONDS))
        job.cancel()
        runCatching { job.await() }
        assertTrue("got ${outcome.get()}", outcome.get() is CancellationException)
    }

    @Test
    fun aTimedOutTaskReachesOnErrorInsteadOfVanishing() {
        val error = AtomicReference<Throwable?>()
        val done = CountDownLatch(1)
        TaskSystem.submitTask(
            Task.runTask(
                id = newId(),
                task = { withDownloadTimeout(50) { delay(5_000) } },
                onError = { error.set(it) },
                onFinally = { done.countDown() },
            )
        )
        assertTrue(done.await(10, TimeUnit.SECONDS))
        assertTrue("onError must receive the timeout, got ${error.get()}", error.get() is TimeoutException)
    }

    /** 取消后的收尾（不可中断的写盘）还没结束时，取消清理不能抢先运行 */
    @Test
    fun cancelCleanupRunsOnlyAfterTheTaskHasStopped() {
        val events = CopyOnWriteArrayList<String>()
        val cancelled = CountDownLatch(1)
        val running = CountDownLatch(1)
        val task = Task.runTask(
            id = newId(),
            task = {
                try {
                    running.countDown()
                    delay(60_000)
                } finally {
                    withContext(NonCancellable) { Thread.sleep(300); events += "stopped" }
                }
            },
            onCancel = { events += "cancel-cleanup"; cancelled.countDown() },
        )
        TaskSystem.submitTask(task)
        assertTrue(running.await(5, TimeUnit.SECONDS))

        TaskSystem.cancelTask(task)

        assertTrue(cancelled.await(10, TimeUnit.SECONDS))
        assertEquals(listOf("stopped", "cancel-cleanup"), events.toList())
    }

    /** 取消后立刻用同一个 id 重试：旧任务收尾不能注销新任务，两者也不能同时运行 */
    @Test
    fun retryAfterCancelWaitsForTheOldTaskAndStaysRegistered() {
        val id = newId()
        val events = CopyOnWriteArrayList<String>()
        val oldRunning = CountDownLatch(1)
        val newStarted = CountDownLatch(1)
        val releaseNew = CountDownLatch(1)

        val old = Task.runTask(
            id = id,
            task = {
                try {
                    oldRunning.countDown()
                    delay(60_000)
                } finally {
                    withContext(NonCancellable) { Thread.sleep(400); events += "old-stopped" }
                }
            },
        )
        TaskSystem.submitTask(old)
        assertTrue(oldRunning.await(5, TimeUnit.SECONDS))
        TaskSystem.cancelTask(old)

        val retry = Task.runTask(
            id = id,
            task = {
                events += "new-started"
                newStarted.countDown()
                withContext(Dispatchers.IO) { releaseNew.await(10, TimeUnit.SECONDS) }
            },
        )
        TaskSystem.submitTask(retry)

        assertTrue(newStarted.await(10, TimeUnit.SECONDS))
        assertEquals("the retry must not start until the old task stopped", listOf("old-stopped", "new-started"), events.toList())
        //旧任务早已结束，新任务仍在运行：它必须仍然登记在任务系统里（可见、可取消）
        Thread.sleep(300)
        assertTrue("the old task's completion must not unregister the retry", TaskSystem.containsTask(id))

        releaseNew.countDown()
        val deadline = System.currentTimeMillis() + 5_000
        while (TaskSystem.containsTask(id) && System.currentTimeMillis() < deadline) Thread.sleep(20)
        assertTrue("the finished retry must be unregistered", !TaskSystem.containsTask(id))
    }
}

/** 取消后任务超过等待时间仍在写盘：清理不能和写入重叠，重试不能和旧任务共用文件，新任务不能被旧任务注销 */
class TaskSystemStuckCancelTest {
    private fun newId() = "stuck-${UUID.randomUUID()}"

    private class Scenario(val id: String, val settleMs: Long, val writeForMs: Long) {
        val writing = java.util.concurrent.atomic.AtomicBoolean(false)
        val events = CopyOnWriteArrayList<String>()
        val running = CountDownLatch(1)
        val cleanupDone = CountDownLatch(1)
        val errors = CopyOnWriteArrayList<Throwable>()
        val overlap = java.util.concurrent.atomic.AtomicBoolean(false)

        fun task() = Task.runTask(
            id = id,
            task = {
                try {
                    running.countDown()
                    delay(60_000)
                } finally {
                    withContext(NonCancellable) {
                        writing.set(true)
                        val end = System.currentTimeMillis() + writeForMs
                        while (System.currentTimeMillis() < end) Thread.sleep(10)
                        writing.set(false)
                        events += "old-stopped"
                    }
                }
            },
            onError = { errors += it },
            onCancel = {
                if (writing.get()) overlap.set(true)
                events += "cleanup"
                cleanupDone.countDown()
            },
        )
    }

    @Test
    fun cleanupNeverOverlapsAWriteThatOutlivesTheWait() {
        val saved = TaskSystem.cancelSettleTimeoutMs
        TaskSystem.cancelSettleTimeoutMs = 300
        try {
            val s = Scenario(newId(), 300, writeForMs = 1_500)
            val task = s.task()
            TaskSystem.submitTask(task)
            assertTrue(s.running.await(5, TimeUnit.SECONDS))
            TaskSystem.cancelTask(task)

            assertTrue(s.cleanupDone.await(10, TimeUnit.SECONDS))
            assertTrue("cleanup ran while the old job was still writing", !s.overlap.get())
            assertEquals(listOf("old-stopped", "cleanup"), s.events.toList())
            assertEquals("a job that could not be stopped in time must be reported once", 1, s.errors.size)
        } finally {
            TaskSystem.cancelSettleTimeoutMs = saved
        }
    }

    @Test
    fun retryWhileTheOldJobIsStuckIsRejectedThenAllowedAfterCleanup() {
        val saved = TaskSystem.cancelSettleTimeoutMs
        TaskSystem.cancelSettleTimeoutMs = 300
        try {
            val s = Scenario(newId(), 300, writeForMs = 1_500)
            val old = s.task()
            TaskSystem.submitTask(old)
            assertTrue(s.running.await(5, TimeUnit.SECONDS))
            TaskSystem.cancelTask(old)
            Thread.sleep(600) //超过等待时间，旧任务仍在写

            val ranWhileStuck = CountDownLatch(1)
            val rejected = AtomicReference<Throwable?>()
            TaskSystem.submitTask(Task.runTask(id = s.id, task = { ranWhileStuck.countDown() }, onError = { rejected.set(it) }))
            Thread.sleep(200)
            assertEquals("the retry must not run while the old job still writes", 1L, ranWhileStuck.count)
            assertTrue("the retry must fail visibly", rejected.get() != null)
            assertTrue("a rejected retry must not be registered", !TaskSystem.containsTask(s.id))

            assertTrue(s.cleanupDone.await(10, TimeUnit.SECONDS))
            val ran = CountDownLatch(1)
            val release = CountDownLatch(1)
            TaskSystem.submitTask(Task.runTask(id = s.id, task = { ran.countDown(); withContext(Dispatchers.IO) { release.await(10, TimeUnit.SECONDS) } }))
            assertTrue(ran.await(5, TimeUnit.SECONDS))
            Thread.sleep(300)
            assertTrue("the old job's completion must not unregister the new task", TaskSystem.containsTask(s.id))
            release.countDown()
        } finally {
            TaskSystem.cancelSettleTimeoutMs = saved
        }
    }
}
