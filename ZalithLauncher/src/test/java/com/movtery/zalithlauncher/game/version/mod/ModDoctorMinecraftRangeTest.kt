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
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 模组声明的 Minecraft 版本约束必须由同一个求值器给出同一个答案。
 *
 * 真实用例（4-mod 认证矩阵里被判 BLOCKED 的单元格）：
 * - Reese's Sodium Options 声明 `1.16.x`，目标实例 1.16.5
 * - Iris 声明 `1.17.x`，目标实例 1.17
 * 两者都是合法组合。约束里的 `x` 是通配段，不是字面版本。
 */
class ModDoctorMinecraftRangeTest {
    private fun reportOf(range: String, minecraftVersion: String): ModDoctorReport =
        validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "probe-mod.jar",
                    modId = "probe-mod",
                    name = "Probe Mod",
                    loader = ModLoader.FABRIC,
                    minecraftVersionRanges = listOf(range),
                )
            ),
            minecraftVersion = minecraftVersion,
            loader = ModLoader.FABRIC,
        )

    @Test
    fun wildcardRangeCoversEveryPatchReleaseOfThatMinorLine() {
        assertTrue(fabricVersionMatches("1.16", "1.16.x"))
        assertTrue(fabricVersionMatches("1.16.5", "1.16.x"))
        assertFalse(fabricVersionMatches("1.17", "1.16.x"))
    }

    @Test
    fun wildcardRangeCoversTheBareMinorRelease() {
        assertTrue(fabricVersionMatches("1.17", "1.17.x"))
        assertTrue(fabricVersionMatches("1.17.1", "1.17.x"))
        assertFalse(fabricVersionMatches("1.18", "1.17.x"))
    }

    @Test
    fun wildcardSegmentIsCaseInsensitive() {
        assertTrue(fabricVersionMatches("1.20.1", "1.20.X"))
        assertFalse(fabricVersionMatches("1.21.1", "1.20.X"))
    }

    @Test
    fun exactVersionStaysExact() {
        assertTrue(fabricVersionMatches("1.21.1", "1.21.1"))
        assertFalse(fabricVersionMatches("1.21.2", "1.21.1"))
    }

    @Test
    fun comparatorRangesKeepWorking() {
        assertTrue(fabricVersionMatches("1.21.11", ">=1.21.10 <1.21.12"))
        assertFalse(fabricVersionMatches("1.21.12", ">=1.21.10 <1.21.12"))
        assertTrue(fabricVersionMatches("1.21.11", ">=1.21.10"))
    }

    @Test
    fun realWildcardFixtureIsNotReportedAsWrongMinecraftVersion() {
        assertTrue(reportOf("1.16.x", "1.16.5").isClean)
        assertTrue(reportOf("1.17.x", "1.17").isClean)
    }

    @Test
    fun aGenuinelyDifferentMinorLineIsStillReported() {
        val report = reportOf("1.16.x", "1.18")
        assertEquals(1, report.problems.count { it.type == ModDoctorProblemType.WRONG_MINECRAFT_VERSION })
    }

    /** `A || B` 是可选分支：任一命中即命中，且只命中列出的分支，不做字符串替换或模糊比较 */
    @Test
    fun orAlternativesAreUnderstoodByTheSharedWrapper() {
        val alternatives = predicateAlternatives("0.5.9 || 0.5.11")

        assertTrue(fabricVersionPredicateMatches("0.5.9", alternatives))
        assertTrue(fabricVersionPredicateMatches("0.5.11", alternatives))
        assertFalse(fabricVersionPredicateMatches("0.5.8", alternatives))
        assertFalse(fabricVersionPredicateMatches("0.5.10", alternatives))
        assertFalse(fabricVersionPredicateMatches("0.5.12", alternatives))
    }

    /** 可选分支在真实上报路径上同样生效，不只是包装函数自己成立 */
    @Test
    fun orAlternativesWorkThroughTheReportedMinecraftDecision() {
        assertTrue(reportOf("1.21.9 || 1.21.11", "1.21.11").isClean)
        assertTrue(reportOf("1.21.9 || 1.21.11", "1.21.9").isClean)
        assertFalse(reportOf("1.21.9 || 1.21.11", "1.21.10").isClean)
    }

    /** 上报用的判定与共享求值器必须是同一个答案，否则“同一个约束两处不同结果”会复发 */
    @Test
    fun theReportAgreesWithTheSharedEvaluatorOnEveryConstraint() {
        val cases = listOf(
            "1.16.x" to "1.16.5",
            "1.17.x" to "1.17",
            "1.16.x" to "1.18",
            "1.21.1" to "1.21.2",
            ">=1.21.10 <1.21.12" to "1.21.11",
        )
        for ((range, minecraftVersion) in cases) {
            assertEquals(
                "range $range on Minecraft $minecraftVersion",
                fabricVersionMatches(minecraftVersion, range),
                reportOf(range, minecraftVersion).isClean,
            )
        }
    }
}
