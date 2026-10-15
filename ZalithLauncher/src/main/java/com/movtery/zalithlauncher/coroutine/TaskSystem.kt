/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.coroutine

import com.movtery.zalithlauncher.keepalive.TaskKeepAlive
import com.movtery.zalithlauncher.utils.network.isInterruptedIOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

object TaskSystem {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val _tasksFlow: MutableStateFlow<List<Task>> = MutableStateFlow(emptyList())
    val tasksFlow = _tasksFlow.asStateFlow()

    private val allJobs = ConcurrentHashMap<String, Job>()
    private val settlingJobs = ConcurrentHashMap<String, Job>()
    private val stuckIds: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val allListeners = ConcurrentHashMap<String, () -> Unit>()

    /**
     * 提交并立即运行任务
     */
    fun submitTask(task: Task) {
        if (containsTask(task)) return
        //同 id 的旧任务取消后超过等待时间仍没停下：不让新任务和它共用同一个文件，明确报告失败
        if (task.id in stuckIds) {
            scope.launch {
                runCatching { task.onError(IllegalStateException("The previous task is still stopping; try again shortly: ${task.id}")) }
                task.onFinally()
            }
            return
        }
        addTask(task)
        //持有保活，避免启动器切至后台后任务被系统中断
        TaskKeepAlive.acquire()

        //取消后立刻用同一个 id 重试时，旧任务可能仍在收尾（不可中断的写盘）：新任务等它结束再开始，
        //避免两个任务同时写同一个暂存文件
        val previous = allJobs[task.id] ?: settlingJobs[task.id]
        allJobs[task.id] = scope.launch(task.dispatcher) {
            previous?.join()
            try {
                task.updateStage(TaskStage.RUNNING)
                task.task(this@launch, task)
                task.updateStage(TaskStage.COMPLETED)
            } catch (th: Throwable) {
                if (th is CancellationException || th.isInterruptedIOException()) return@launch
                task.onError(th)
            } finally {
                task.onFinally()
            }
        }.also { job ->
            job.invokeOnCompletion {
                try {
                    //已被同 id 的新任务接替时，旧任务的收尾不能注销新任务
                    if (allJobs[task.id].let { it == null || it === job }) onTaskEnded(task)
                } finally {
                    //确保保活一定被释放，避免任务异常导致前台服务无法停止
                    TaskKeepAlive.release()
                }
            }
        }
    }

    /**
     * 提交并立即运行任务
     * 若任务已存在，则忽略，但任务监听器会被覆盖
     * @param onEnded 任务结束时的监听器
     */
    fun submitTask(task: Task, onEnded: () -> Unit) {
        putTaskEndedListener(taskId = task.id, onEnded = onEnded)
        submitTask(task)
    }

    /**
     * 添加任务结束的监听器，监听器会在任务的一切流程结束时被调用
     * 任务监听器执行时发生的异常将会被忽略
     * @param taskId 指定监听器应用到的哪个任务上
     */
    fun putTaskEndedListener(taskId: String, onEnded: () -> Unit) {
        allListeners[taskId] = onEnded
    }

    /**
     * 移除任务流程结束的监听器
     */
    fun removeTaskEndedListener(taskId: String) =
        allListeners.remove(taskId)

    private fun onTaskEnded(task: Task) {
        removeTask(task)
        runCatching {
            allListeners[task.id]?.invoke()
        }
        allJobs.remove(task.id)
        removeTaskEndedListener(task.id)
    }

    private fun onTaskCanceled(task: Task, job: Job?) {
        //清理必须在被取消的任务真正停下之后：任务可能还在不可中断的写盘，并发清理会和它互相踩踏。
        //超时只用来把“停不下来”报告为失败，不会让清理提前开始；清理完成前同 id 的重试都不能开始
        val cleanup = scope.launch {
            if (job != null && withTimeoutOrNull(cancelSettleTimeoutMs) { job.join() } == null) {
                stuckIds += task.id
                runCatching { task.onError(IllegalStateException("The cancelled task could not be stopped safely yet: ${task.id}")) }
                job.join()
            }
            task.onCancel()
        }
        //已取消但还在收尾（含清理）的任务：同 id 的重试要等它，卡住时直接拒绝
        settlingJobs[task.id] = cleanup
        cleanup.invokeOnCompletion {
            settlingJobs.remove(task.id, cleanup)
            stuckIds -= task.id
        }
        onTaskEnded(task)
    }

    /**
     * 取消任务
     */
    fun cancelTask(task: Task) {
        val job = allJobs[task.id]
        job?.cancel()
        onTaskCanceled(task, job)
    }

    /**
     * 取消任务
     */
    fun cancelTask(id: String) {
        val job = allJobs[id]
        job?.cancel()
        _tasksFlow.value.find { it.id == id }?.let { onTaskCanceled(it, job) }
    }

    /** 等待被取消的任务停下的最长时间，超过后仍会执行取消清理 */
    internal var cancelSettleTimeoutMs = 10_000L

    /**
     * @return 是否包括任务
     */
    fun containsTask(task: Task) = _tasksFlow.value.contains(task)

    /**
     * @return 是否包括任务
     */
    fun containsTask(id: String) = _tasksFlow.value.any { it.id == id }

    /**
     * 停止所有任务
     */
    fun stopAll() {
        scope.cancel()
        _tasksFlow.update { emptyList() }
        allJobs.clear()
        allListeners.clear()
        TaskKeepAlive.reset()
    }

    private fun addTask(task: Task) {
        _tasksFlow.update { it + task }
    }

    private fun removeTask(task: Task) {
        _tasksFlow.update { it - task }
    }
}