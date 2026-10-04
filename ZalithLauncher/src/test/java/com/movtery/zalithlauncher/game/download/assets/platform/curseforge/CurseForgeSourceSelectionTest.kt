package com.movtery.zalithlauncher.game.download.assets.platform.curseforge

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 官方 CurseForge API 的每个请求都要带 x-api-key，没有密钥就一律 403。
 * 因此“是否启用 MCIM 镜像源”必须同时取决于地区与密钥是否配置。
 */
class CurseForgeSourceSelectionTest {
    @Test
    fun mirrorIsEnabledInMainlandChina() {
        assertTrue(curseForgeMirrorEnabled(mainland = true, hasApiKey = true))
        assertTrue(curseForgeMirrorEnabled(mainland = true, hasApiKey = false))
    }

    @Test
    fun mirrorIsEnabledOutsideMainlandChinaWhenTheApiKeyIsMissing() {
        assertTrue(curseForgeMirrorEnabled(mainland = false, hasApiKey = false))
    }

    @Test
    fun mirrorStaysDisabledOutsideMainlandChinaWhileTheApiKeyIsConfigured() {
        assertFalse(curseForgeMirrorEnabled(mainland = false, hasApiKey = true))
    }

    @Test
    fun mirrorServesTheSamePlatformAsTheOfficialSource() {
        val mirror = CurseForgeSearcher(api = MCIM_CURSEFORGE_API, source = "MCIM CurseForge")
        val official = CurseForgeSearcher()

        assertTrue(mirror.platform == official.platform)
        assertTrue(official.api == CURSEFORGE_API)
    }
}
