package com.movtery.zalithlauncher.game.download.assets.platform.modrinth

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections

/**
 * Modrinth 版本列表分页回归：单次超大响应（Fabric API 约 1200 个版本、约 1.2MB）会顶到
 * 应用的 30 秒请求超时，分页后每次请求都必须是有界的一页，且结果与一次拉取全量一致。
 */
class ModrinthVersionPagingTest {
    @Test
    fun `fabric api sized list is fetched in bounded pages without loss or reorder`() = runBlocking {
        val requested = Collections.synchronizedList(mutableListOf<Pair<Int, Int>>())
        // 2026-09-17 实测：GET /project/P7dR8mSH/version 返回 1199 个版本
        val total = 1199

        val versions = ModrinthSearcher().fetchVersionsPaged { pageSize, offset ->
            requested += pageSize to offset
            (offset until minOf(offset + pageSize, total)).toList()
        }

        assertEquals(total, versions.size)
        assertEquals((0 until total).toList(), versions)
        assertTrue(requested.isNotEmpty())
        assertTrue(requested.all { (pageSize, _) -> pageSize == ModrinthSearcher.VERSION_PAGE_SIZE })
        assertTrue(requested.map { (_, offset) -> offset }.distinct().size > 1)
    }

    @Test
    fun `an exactly full last page is not treated as the end of the list`() = runBlocking {
        val requested = Collections.synchronizedList(mutableListOf<Pair<Int, Int>>())
        // 12 整页：不能因为最后一页刚好是满页就提前结束
        val total = 1200

        val versions = ModrinthSearcher().fetchVersionsPaged { pageSize, offset ->
            requested += pageSize to offset
            (offset until minOf(offset + pageSize, total)).toList()
        }

        assertEquals(total, versions.size)
        assertEquals((0 until total).toList(), versions)
        assertTrue(requested.any { (_, offset) -> offset == total })
    }

    /** 页面按 [sizes] 给出：offset/pageSize 对应第 n 页，内容是全局序号 */
    private fun pagedBy(sizes: List<Int>): suspend (Int, Int) -> List<Int> {
        val starts = sizes.runningFold(0) { a, b -> a + b }
        return { pageSize, offset ->
            val page = offset / pageSize
            if (page < sizes.size) (starts[page] until starts[page] + sizes[page]).toList() else emptyList()
        }
    }

    @Test
    fun `a short page in the middle does not end the list`() = runBlocking {
        // 实测 Modrinth AANobbMI：100 / 94 / 64
        val versions = ModrinthSearcher().fetchVersionsPaged(fetchChunk = pagedBy(listOf(100, 94, 64)))

        assertEquals(258, versions.size)
        assertEquals(258, versions.toSet().size)
    }

    @Test
    fun `an empty first page gives an empty list`() = runBlocking {
        assertEquals(emptyList<Int>(), ModrinthSearcher().fetchVersionsPaged(fetchChunk = pagedBy(emptyList())))
    }

    @Test
    fun `a server that ignores offset cannot loop forever`() = runBlocking {
        var calls = 0
        val versions = ModrinthSearcher().fetchVersionsPaged { pageSize, _ -> calls++; List(pageSize) { it } }

        assertTrue(calls <= ModrinthSearcher.MAX_VERSION_OFFSET / ModrinthSearcher.VERSION_PAGE_SIZE)
        assertTrue(versions.size <= ModrinthSearcher.MAX_VERSION_OFFSET)
    }

    @Test
    fun `an api failure is reported and never returns a silently truncated list`() {
        val failure = runCatching {
            runBlocking {
                ModrinthSearcher().fetchVersionsPaged { _, offset ->
                    if (offset >= 100) throw java.io.IOException("boom") else List(100) { it }
                }
            }
        }.exceptionOrNull()

        assertTrue(failure is java.io.IOException)
    }

    @Test
    fun `the real sodium project pages give all 258 versions including 0_5_3`() = runBlocking {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
        val serializer = kotlinx.serialization.builtins.ListSerializer(com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthVersion.serializer())
        val requested = Collections.synchronizedList(mutableListOf<Int>())

        val versions = ModrinthSearcher().fetchVersionsPaged { _, offset ->
            requested += offset
            val text = javaClass.getResource("/modrinth-sodium/page-$offset.json")?.readText()
            if (text == null) emptyList() else json.decodeFromString(serializer, text)
        }

        assertEquals(258, versions.size)
        assertEquals(258, versions.map { it.id }.toSet().size)
        assertTrue(versions.any { it.id == "4OZL6q9h" })
        assertTrue(requested.contains(200))
    }
}
