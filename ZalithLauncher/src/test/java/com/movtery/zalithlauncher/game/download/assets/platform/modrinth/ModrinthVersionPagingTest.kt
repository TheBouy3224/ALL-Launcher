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
}
