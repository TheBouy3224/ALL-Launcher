package com.movtery.zalithlauncher.game.version.mod

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 真实 Modrinth 模组（STEP 2 已校验 SHA-1）自己声明的依赖范围，对照真实的已安装版本：
 * 满足的组合不能误报，越界的组合必须判为不满足。范围字符串逐字取自各 jar 的 fabric.mod.json。
 */
class RealMetadataRangeTest {
    private val fabricApi = "0.141.6+1.21.11"
    private val mc = "1.21.11"

    private fun ok(range: String, version: String) = installedDependencyVerdict(listOf(range), version)

    @Test
    fun realDeclaredRangesAcceptTheRealInstalledVersions() {
        assertEquals(true, ok(">=0.139", fabricApi))                 // Jade 21.0.1 -> fabric-api
        assertEquals(true, ok(">=0.133.5", fabricApi))               // Jade 20.1.0 -> fabric-api
        assertEquals(true, ok(">=0.140.0+1.21.11", fabricApi))       // YACL 3.8.2 -> fabric-api
        assertEquals(true, ok(">=1.21.11", mc))                      // Jade -> minecraft
        assertEquals(true, ok(">=1.21.11 <26", mc))                  // Mod Menu
        assertEquals(true, ok(">=1.21.11 <1.22", mc))                // FerriteCore
        assertEquals(true, ok("~1.21.11", mc))                       // YACL
        assertEquals(true, ok("1.21.11", mc))                        // Lithium (exact)
        assertEquals(true, ok(">=1.21.11- <1.21.12-", mc))           // fabric-api itself
        assertEquals(true, ok("*", "0.141.6"))                       // Mod Menu -> fabric-* modules
    }

    @Test
    fun realDeclaredRangesRejectVersionsOutsideThem() {
        assertEquals(false, ok(">=0.140.0+1.21.11", "0.139.0+1.21.11"))  // YACL with an older fabric-api
        assertEquals(false, ok(">=0.139", "0.138.4+1.21.10"))            // Jade with an older fabric-api
        assertEquals(false, ok(">=1.21.11", "1.21.10"))
        assertEquals(false, ok("1.21.11", "1.21.10"))                    // Lithium exact
        assertEquals(false, ok("~1.21.11", "1.21.9"))
        assertEquals(false, ok(">=1.21.11 <1.22", "1.22"))
    }

    @Test
    fun theCandidateFilterUsesTheSameRangesAsTheDetector() {
        val yacl = listOf(listOf(">=0.140.0+1.21.11"))
        assertTrue(dependencyVersionSatisfies(fabricApi, yacl))
        assertFalse(dependencyVersionSatisfies("0.139.0+1.21.11", yacl))
        //两个依赖方同时约束同一个依赖：候选必须同时满足
        val both = listOf(listOf(">=0.139"), listOf(">=0.140.0+1.21.11"))
        assertTrue(dependencyVersionSatisfies(fabricApi, both))
        assertFalse(dependencyVersionSatisfies("0.139.5+1.21.11", both))
    }

    @Test
    fun unreadableInstalledVersionIsNeverReportedAsWrong() {
        assertEquals(null, installedDependencyVerdict(listOf(">=0.139"), null))
        assertEquals(null, installedDependencyVerdict(listOf(">=0.139"), "${'$'}{version}"))
    }
}

/** `~` / `^` 简写：与 Fabric 的 semver 语义一致 */
class VersionShorthandTest {
    private fun v(range: String, version: String) = installedDependencyVerdict(listOf(range), version)

    @Test
    fun tildeMeansSameMinor() {
        assertEquals(true, v("~1.21.11", "1.21.11"))
        assertEquals(true, v("~1.21.11", "1.21.12"))
        assertEquals(false, v("~1.21.11", "1.22.0"))
        assertEquals(false, v("~1.21.11", "1.21.10"))
        assertEquals(true, v("~1.21", "1.21.9"))
        assertEquals(false, v("~1.21", "1.22.0"))
    }

    @Test
    fun caretMeansSameMajorAndTightensBelowOne() {
        assertEquals(true, v("^1.2.3", "1.9.0"))
        assertEquals(false, v("^1.2.3", "2.0.0"))
        assertEquals(true, v("^0.140.0", "0.140.9"))
        assertEquals(false, v("^0.140.0", "0.141.0"))
        assertEquals(false, v("^0.0.3", "0.0.4"))
    }

    @Test
    fun shorthandCombinesWithAlternativesAndOtherPredicates() {
        assertEquals(true, v("~1.20.1 || ~1.21.11", "1.21.11"))
        assertEquals(false, v("~1.20.1 || ~1.21.11", "1.19.4"))
        //读不懂的写法保持“无法定论”，不误报
        assertEquals(null, v("~1.21.11-beta", "1.21.11"))
    }
}
