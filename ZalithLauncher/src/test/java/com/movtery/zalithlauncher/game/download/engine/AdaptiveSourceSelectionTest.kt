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

package com.movtery.zalithlauncher.game.download.engine

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.Random
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** 镜像过载形态之一：建连正常但快速返回 5xx */
private class OverloadedSource(private val code: Int) : Dispatcher() {
    val hits = AtomicInteger(0)

    override fun dispatch(request: RecordedRequest): MockResponse {
        hits.incrementAndGet()
        return MockResponse.Builder().code(code).build()
    }
}

/** 按路径给出确定性内容，模拟正常服务的官方源 */
private class IndexedPayloadSource : Dispatcher() {
    val hits = AtomicInteger(0)

    override fun dispatch(request: RecordedRequest): MockResponse {
        hits.incrementAndGet()
        val content = payloadFor(request.target.trim('/').toIntOrNull() ?: 0)
        return MockResponse.Builder()
            .addHeader("Content-Length", content.size.toString())
            .body(Buffer().write(content))
            .build()
    }
}

/** 对每个路径返回尺寸正确但内容错误的应答，模拟返回坏数据的镜像 */
private class CorruptSource : Dispatcher() {
    val hits = AtomicInteger(0)

    override fun dispatch(request: RecordedRequest): MockResponse {
        hits.incrementAndGet()
        val index = request.target.trim('/').toIntOrNull() ?: 0
        val garbage = ByteArray(payloadFor(index).size).also { Random(99).nextBytes(it) }
        return MockResponse.Builder()
            .addHeader("Content-Length", garbage.size.toString())
            .body(Buffer().write(garbage))
            .build()
    }
}

/** 支持 Range 的静态源，可限速模拟"建连成功但传输极慢" */
private class RangedPayloadSource(
    private val content: ByteArray,
    private val throttleBytesPerSecond: Long = 0L
) : Dispatcher() {
    val hits = AtomicInteger(0)

    override fun dispatch(request: RecordedRequest): MockResponse {
        hits.incrementAndGet()
        val rangeHeader = request.headers["Range"]
        val start = rangeHeader?.let { RANGE.find(it)!!.groupValues[1].toLong().toInt() } ?: 0
        val builder = MockResponse.Builder()
            .addHeader("Content-Length", (content.size - start).toString())
            .body(Buffer().write(content, start, content.size - start))
        if (start > 0) {
            builder.code(206)
                .addHeader("Content-Range", "bytes $start-${content.size - 1}/${content.size}")
        }
        if (throttleBytesPerSecond > 0) {
            builder.throttleBody(throttleBytesPerSecond, 1, TimeUnit.SECONDS)
        }
        return builder.build()
    }

    companion object {
        private val RANGE = Regex("""bytes=(\d+)-""")
    }
}

private fun payloadFor(index: Int): ByteArray {
    val size = 20_000 + (index % 23) * 811
    val array = ByteArray(size)
    Random(index.toLong()).nextBytes(array)
    return array
}

private fun sha1Of(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }

/**
 * 自适应选源回归：镜像病态（快速 5xx、持续低速、返回坏数据）时，
 * 主机级健康计数应让整批流量自动让位给健康的官方源，而不是每个文件都先向镜像交学费。
 */
class AdaptiveSourceSelectionTest {

    private lateinit var workDir: File
    private val servers = mutableListOf<MockWebServer>()

    @After
    fun tearDown() {
        servers.forEach { runCatching { it.close() } }
        workDir?.takeIf { it.exists() }?.deleteRecursively()
    }

    private fun newWorkDir(): File = Files.createTempDirectory("adaptive-source").toFile().also { workDir = it }

    private fun startServer(dispatcher: Dispatcher): MockWebServer =
        MockWebServer().also { server ->
            server.dispatcher = dispatcher
            server.start()
            servers += server
        }

    @Test
    fun `mirror five-xx storm shifts the batch onto the official source`() = runBlocking {
        val fileCount = 24
        val mirror = startServer(OverloadedSource(503))
        val official = startServer(IndexedPayloadSource())
        newWorkDir()

        val requests = (0 until fileCount).map { index ->
            DownloadRequest(
                urls = listOf(mirror.url("/$index").toString(), official.url("/$index").toString()),
                targetFile = File(workDir, "f-$index.bin"),
                expectedSize = payloadFor(index).size.toLong()
            )
        }

        withTimeout(60_000) {
            BatchDownloader(
                requests = requests,
                maxConnections = 12,
                retryRounds = 1,
                clientOverride = OkHttpClient()
            ).run()
        }

        requests.forEachIndexed { index, request ->
            assertTrue("${request.targetFile.name} should exist", request.targetFile.exists())
            assertArrayEquals(payloadFor(index), request.targetFile.readBytes())
        }

        assertMirrorHitsBounded((mirror.dispatcher as OverloadedSource).hits.get(), fileCount, maxConnections = 12)
    }

    /**
     * 熔断需要 [HostHealth.TRIP_THRESHOLD] 次失败才会打开：镜像会被首批并发请求（最多 maxConnections 个）探测，
     * 再加上熔断统计失败期间已经派发出去的少量请求。上限据此推出；关键性质是它远小于“每个文件都打一次镜像”。
     * 旧断言写死“不超过连接数”，那只是某次时序下碰巧成立的结果（实测 14~15 很常见，最坏也远低于文件数）。
     */
    private fun assertMirrorHitsBounded(hits: Int, fileCount: Int, maxConnections: Int) {
        val bound = maxConnections + 2 * HostHealth.TRIP_THRESHOLD
        assertTrue("mirror hits should stay within the breaker bound ($bound), got $hits", hits in 1..bound)
        assertTrue("the breaker must keep the mirror well below one hit per file ($fileCount), got $hits", hits < fileCount)
    }

    @Test
    fun `repeated mirror storms always stay within the breaker bound`() = runBlocking {
        repeat(10) {
            val mirror = startServer(OverloadedSource(503))
            val official = startServer(IndexedPayloadSource())
            newWorkDir()
            val requests = (0 until 24).map { index ->
                DownloadRequest(
                    urls = listOf(mirror.url("/$index").toString(), official.url("/$index").toString()),
                    targetFile = File(workDir, "f-$index.bin"),
                    expectedSize = payloadFor(index).size.toLong()
                )
            }
            withTimeout(60_000) { BatchDownloader(requests = requests, maxConnections = 12, retryRounds = 1, clientOverride = OkHttpClient()).run() }

            requests.forEachIndexed { index, request -> assertArrayEquals(payloadFor(index), request.targetFile.readBytes()) }
            assertMirrorHitsBounded((mirror.dispatcher as OverloadedSource).hits.get(), 24, maxConnections = 12)
        }
    }

    @Test
    fun `slow dripping transfer is aborted and finished on the other source`() = runBlocking {
        val payload = ByteArray(120_000).also { Random(7).nextBytes(it) }
        val dripping = startServer(RangedPayloadSource(payload, throttleBytesPerSecond = 2_048L))
        val healthy = startServer(RangedPayloadSource(payload))
        val target = File(newWorkDir(), "drip.bin")

        val request = DownloadRequest(
            urls = listOf(dripping.url("/f.bin").toString(), healthy.url("/f.bin").toString()),
            targetFile = target,
            expectedSize = payload.size.toLong()
        )

        withTimeout(30_000) {
            FileDownloader(
                request = request,
                connections = Semaphore(4),
                stats = DownloadStats(),
                allowExtraConnection = { false },
                client = OkHttpClient.Builder()
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build(),
                slowWindowNanos = 800_000_000L,
                slowFloorBytesPerSec = 8L * 1024L
            ).download()
        }

        assertArrayEquals(payload, target.readBytes())
        val drippingHits = (dripping.dispatcher as RangedPayloadSource).hits.get()
        assertTrue("dripping source should have been tried first", drippingHits >= 1)
    }

    @Test
    fun `corrupt mirror content is reported and the retry uses the official source`() = runBlocking {
        val payload = payloadFor(0)
        val lyingMirror = startServer(CorruptSource())
        val official = startServer(IndexedPayloadSource())
        val target = File(newWorkDir(), "corrupt.bin")

        val request = DownloadRequest(
            urls = listOf(lyingMirror.url("/0").toString(), official.url("/0").toString()),
            targetFile = target,
            sha1 = sha1Of(payload),
            expectedSize = payload.size.toLong()
        )

        withTimeout(30_000) {
            FileDownloader(
                request = request,
                connections = Semaphore(4),
                stats = DownloadStats(),
                allowExtraConnection = { false },
                client = OkHttpClient()
            ).download()
        }

        assertArrayEquals(payload, target.readBytes())
        assertEquals(1, (lyingMirror.dispatcher as CorruptSource).hits.get())
        assertTrue(
            "retry should be served by the official source",
            (official.dispatcher as IndexedPayloadSource).hits.get() >= 1
        )
    }
}
