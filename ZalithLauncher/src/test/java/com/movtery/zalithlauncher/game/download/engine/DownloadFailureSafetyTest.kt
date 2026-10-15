package com.movtery.zalithlauncher.game.download.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.Random
import java.util.concurrent.TimeUnit

/**
 * 下载失败与取消的安全性（可控假源）：任何失败或取消都不能留下可能被当作模组加载的残片，
 * 也不能覆盖目标位置原有的文件；失败必须以异常报告，不能“成功返回”。
 */
class DownloadFailureSafetyTest {
    private val servers = mutableListOf<MockWebServer>()
    private val dirs = mutableListOf<File>()
    private val payload = ByteArray(1_200_000).also { Random(7).nextBytes(it) }
    private val client = OkHttpClient.Builder()
        .callTimeout(30, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val closers = mutableListOf<() -> Unit>()

    @After
    fun tearDown() {
        closers.forEach { runCatching { it() } }
        servers.forEach { runCatching { it.close() } }
        dirs.forEach { it.deleteRecursively() }
    }

    private fun newDir(): File = Files.createTempDirectory("download-safety").toFile().also { dirs += it }

    private fun serve(dispatcher: Dispatcher): String {
        val server = MockWebServer().also { it.dispatcher = dispatcher; it.start(); servers += it }
        return server.url("/mod.jar").toString()
    }

    private fun sha1Of(bytes: ByteArray) = MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun respond(block: (RecordedRequest) -> MockResponse) = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse = block(request)
    }

    private fun full(bytes: ByteArray = payload) = MockResponse.Builder()
        .addHeader("Content-Length", bytes.size.toString())
        .body(Buffer().write(bytes))
        .build()

    private suspend fun download(url: String, target: File, sha1: String? = null) =
        withTimeout(90_000) { DownloadEngine.download(DownloadRequest(listOf(url), target, sha1), client = client) }

    /** 目录里只允许剩下 [allowed]：任何 .part/临时残片都算失败 */
    private fun assertOnly(dir: File, vararg allowed: String) =
        assertEquals("leftovers in ${dir.name}: ${dir.list()!!.toList()}", allowed.toSet(), dir.list()!!.toSet())

    private fun expectFailure(url: String, target: File, sha1: String? = null) = runBlocking {
        try {
            download(url, target, sha1)
            fail("the download must fail, not report success")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Throwable) {
        }
    }

    // ---- 取消 ----

    @Test
    fun cancelMidTransferLeavesNoPartialAndKeepsTheOriginalFile() = runBlocking {
        val dir = newDir()
        val target = File(dir, "mod.jar").apply { writeText("ORIGINAL") }
        val slow = serve(respond {
            MockResponse.Builder()
                .addHeader("Content-Length", payload.size.toString())
                .body(Buffer().write(payload))
                .throttleBody(64 * 1024, 1, TimeUnit.SECONDS)
                .build()
        })
        val part = File(dir, "mod.jar.part")

        val job = launch(Dispatchers.Default) { download(slow, target, sha1Of(payload)) }
        val deadline = System.currentTimeMillis() + 15_000
        while ((!part.exists() || part.length() == 0L) && System.currentTimeMillis() < deadline) delay(20)
        assertTrue("the transfer must be in flight before cancelling", part.exists())

        job.cancelAndJoin()
        delay(500) //迟到的写入不应让残片复活

        assertOnly(dir, "mod.jar")
        assertEquals("ORIGINAL", target.readText())
    }

    @Test
    fun cancelBeforeTheTransferStartsLeavesNothing() = runBlocking {
        val dir = newDir()
        val target = File(dir, "mod.jar")
        val job = launch(Dispatchers.Default, start = kotlinx.coroutines.CoroutineStart.LAZY) { download(serve(respond { full() }), target) }
        job.cancel()
        job.join()
        assertOnly(dir)
    }

    /**
     * 在传输、校验、提交的任意时刻取消：目录里要么什么都没有（或原文件原样），要么是完整且校验通过的文件，
     * 绝不能是残片。多个取消时刻让取消落在不同阶段，而不依赖某一个精确时序。
     */
    @Test
    fun cancelAtAnyMomentNeverLeavesAPartialOrCorruptTarget() = runBlocking {
        val big = ByteArray(8_000_000).also { Random(11).nextBytes(it) }
        val url = serve(respond { full(big) })
        for (delayMs in listOf(0L, 1, 3, 6, 10, 20, 40, 80, 160)) {
            val dir = newDir()
            val target = File(dir, "mod.jar")
            val job = launch(Dispatchers.Default) { download(url, target, sha1Of(big)) }
            delay(delayMs)
            job.cancelAndJoin()
            delay(100)

            val names = dir.list()!!.toSet()
            assertTrue("no fragments allowed after cancel at ${delayMs}ms, got $names", names.all { it == "mod.jar" })
            if (target.exists()) assertArrayEquals("a surviving target must be complete (cancel at ${delayMs}ms)", big, target.readBytes())
        }
    }

    // ---- 失败：不装入、不覆盖、不留残片、不报成功 ----

    /** 声明完整长度、只发一半就断开连接的服务器（网络中断） */
    private fun serveDroppingConnection(): String {
        val socket = java.net.ServerSocket(0)
        val thread = Thread {
            while (!socket.isClosed) {
                try {
                    socket.accept().use { conn ->
                        val input = conn.getInputStream()
                        val seen = StringBuilder()
                        while (!seen.endsWith("\r\n\r\n")) seen.append(input.read().also { if (it < 0) return@use }.toChar())
                        val out = conn.getOutputStream()
                        out.write("HTTP/1.1 200 OK\r\nContent-Length: ${payload.size}\r\n\r\n".toByteArray())
                        out.write(payload, 0, payload.size / 2)
                        out.flush()
                    }
                } catch (_: java.io.IOException) {
                }
            }
        }.apply { isDaemon = true; start() }
        closers += { socket.close(); thread.interrupt() }
        return "http://127.0.0.1:${socket.localPort}/mod.jar"
    }

    @Test
    fun connectionDroppedMidBodyFailsWithoutLeftoversOrOverwrite() {
        val dir = newDir()
        val target = File(dir, "mod.jar").apply { writeText("ORIGINAL") }
        val url = serveDroppingConnection()

        expectFailure(url, target, sha1Of(payload))

        assertOnly(dir, "mod.jar")
        assertEquals("ORIGINAL", target.readText())
    }

    @Test
    fun truncatedBodyWithoutSha1IsStillIncompleteNotSuccess() {
        val dir = newDir()
        val target = File(dir, "mod.jar")
        val url = serveDroppingConnection()

        expectFailure(url, target, sha1 = null)

        assertOnly(dir)
    }

    @Test
    fun httpServerErrorFailsWithoutLeftovers() {
        val dir = newDir()
        val target = File(dir, "mod.jar").apply { writeText("ORIGINAL") }
        expectFailure(serve(respond { MockResponse.Builder().code(500).build() }), target, sha1Of(payload))
        assertOnly(dir, "mod.jar")
        assertEquals("ORIGINAL", target.readText())
    }

    @Test
    fun httpNotFoundFailsWithoutLeftovers() {
        val dir = newDir()
        val target = File(dir, "mod.jar")
        expectFailure(serve(respond { MockResponse.Builder().code(404).build() }), target, sha1Of(payload))
        assertOnly(dir)
    }

    @Test
    fun hashMismatchIsRejectedAndNeverInstalled() {
        val dir = newDir()
        val target = File(dir, "mod.jar").apply { writeText("ORIGINAL") }
        val corrupt = payload.copyOf().also { it[it.size / 2] = (it[it.size / 2] + 1).toByte() }

        expectFailure(serve(respond { full(corrupt) }), target, sha1Of(payload))

        assertOnly(dir, "mod.jar")
        assertEquals("the existing file must survive a mismatching download", "ORIGINAL", target.readText())
    }

    @Test
    fun wrongLengthBodyIsRejectedByTheHash() {
        val dir = newDir()
        val target = File(dir, "mod.jar")
        val shorter = payload.copyOf(payload.size - 10)

        expectFailure(serve(respond { full(shorter) }), target, sha1Of(payload))

        assertOnly(dir)
    }

    // ---- 重试 ----

    @Test
    fun retryAfterAFailedAttemptSucceedsAndVerifies() = runBlocking {
        val dir = newDir()
        val target = File(dir, "mod.jar")
        val broken = serve(respond { MockResponse.Builder().code(503).build() })
        expectFailure(broken, target, sha1Of(payload))
        assertOnly(dir)

        download(serve(respond { full() }), target, sha1Of(payload))

        assertOnly(dir, "mod.jar")
        assertArrayEquals(payload, target.readBytes())
    }

    @Test
    fun retryAfterACancelledAttemptSucceeds() = runBlocking {
        val dir = newDir()
        val target = File(dir, "mod.jar")
        val slow = serve(respond {
            MockResponse.Builder()
                .addHeader("Content-Length", payload.size.toString())
                .body(Buffer().write(payload))
                .throttleBody(64 * 1024, 1, TimeUnit.SECONDS)
                .build()
        })
        val part = File(dir, "mod.jar.part")
        val job = launch(Dispatchers.Default) { download(slow, target, sha1Of(payload)) }
        val deadline = System.currentTimeMillis() + 15_000
        while ((!part.exists() || part.length() == 0L) && System.currentTimeMillis() < deadline) delay(20)
        job.cancelAndJoin()
        assertFalse(target.exists())

        download(serve(respond { full() }), target, sha1Of(payload))

        assertOnly(dir, "mod.jar")
        assertArrayEquals(payload, target.readBytes())
    }
}
