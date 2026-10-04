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
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 已启用模组之间的依赖检查有两件事：「有没有装」与「装的版本对不对」。
 *
 * 「有没有装」由既有的 MISSING_REQUIRED_DEPENDENCY 负责，行为不变；
 * 「版本对不对」由 INCOMPATIBLE_DEPENDENCY_VERSION 负责，只报告，不给 Fix。
 *
 * 真实用例（4-mod 认证矩阵里 Mod Doctor 报 0 而真正的加载器拒绝的单元格）：
 * - Minecraft 1.20.5：Iris 要求 `sodium 0.5.9 || 0.5.11`，装的是 0.5.8
 * - Minecraft 26.1：Iris 要求 `sodium 0.9.x`，装的是 0.8.9
 *
 * 覆盖不到的（读不出版本或读不懂谓词）必须保持沉默：权威判定留给真正的加载器。
 */
class ModDoctorInstalledDependencyTest {
    private fun mod(
        fileName: String,
        modId: String,
        name: String,
        version: String? = null,
        requires: List<String> = emptyList(),
        predicates: Map<String, List<String>> = emptyMap(),
        provided: Map<String, String> = emptyMap(),
    ) = ModDoctorMetadata(
        fileName = fileName,
        modId = modId,
        name = name,
        loader = ModLoader.FABRIC,
        version = version,
        requiredDependencies = requires,
        requiredDependencyPredicates = predicates,
        providedModIds = provided.keys.toList(),
        providedModVersions = provided,
    )

    private fun irisRequiringSodium(predicate: String) = mod(
        fileName = "iris-fabric-1.8.0+mc1.21.11.jar",
        modId = "iris",
        name = "Iris",
        requires = listOf("sodium"),
        predicates = mapOf("sodium" to listOf(predicate)),
    )

    private fun sodiumInstalledAt(version: String?) = mod(
        fileName = "sodium-fabric.jar",
        modId = "sodium",
        name = "Sodium",
        version = version,
    )

    private fun report(
        mods: List<ModDoctorMetadata>,
        minecraftVersion: String = "1.21.11",
    ) = validateModDoctorMetadata(mods = mods, minecraftVersion = minecraftVersion, loader = ModLoader.FABRIC)

    private fun mismatches(report: ModDoctorReport) =
        report.problems.filter { it.type == ModDoctorProblemType.INCOMPATIBLE_DEPENDENCY_VERSION }

    // ---- 「有没有装」：既有行为不变 ----

    @Test
    fun removedCompanionIsReportedAsAMissingDependency() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                mod("sodium-extra-mc1.17.1-0.3.6.jar", "sodium-extra", "Sodium Extra", requires = listOf("fabric", "sodium")),
                mod("reeses_sodium_options-mc1.17.1-1.2.3.jar", "reeses-sodium-options", "Reese's Sodium Options", requires = listOf("sodium")),
                mod("iris-mc1.17-1.1.1.jar", "iris", "Iris", requires = listOf("fabric-resource-loader-v0")),
                mod("fabric-api-0.36.0+1.17.jar", "fabric", "Fabric API"),
            ),
            minecraftVersion = "1.17",
            loader = ModLoader.FABRIC,
        )

        assertTrue(
            "Sodium is gone while two enabled mods declare they require it",
            report.problems.any { it.type == ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY },
        )
    }

    @Test
    fun anAbsentDependencyStillReportsTheMissingTypeOnly() {
        val report = report(listOf(irisRequiringSodium("0.5.9 || 0.5.11")))

        assertEquals(1, report.problems.count { it.type == ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY })
        assertTrue(mismatches(report).isEmpty())
        assertEquals("sodium", report.problems.single().requiredDependencyId)
    }

    // ---- 「版本对不对」：`||` 可选分支 ----

    @Test
    fun installedVersionOutsideOrAlternativesIsReported() {
        val report = report(listOf(irisRequiringSodium("0.5.9 || 0.5.11"), sodiumInstalledAt("0.5.8")))

        val found = mismatches(report)
        assertEquals(1, found.size)
        assertTrue(found.single().reason.contains("0.5.9 || 0.5.11"))
        assertTrue(found.single().reason.contains("0.5.8"))
    }

    @Test
    fun installedVersionMatchingTheFirstOrAlternativeIsNotReported() {
        val report = report(listOf(irisRequiringSodium("0.5.9 || 0.5.11"), sodiumInstalledAt("0.5.9")))

        assertTrue(mismatches(report).isEmpty())
    }

    @Test
    fun installedVersionMatchingTheSecondOrAlternativeIsNotReported() {
        val report = report(listOf(irisRequiringSodium("0.5.9 || 0.5.11"), sodiumInstalledAt("0.5.11")))

        assertTrue(mismatches(report).isEmpty())
    }

    // ---- 「版本对不对」：通配段与比较运算符 ----

    @Test
    fun installedVersionOutsideTheWildcardRangeIsReported() {
        val report = report(listOf(irisRequiringSodium("0.9.x"), sodiumInstalledAt("0.8.9")))

        assertEquals(1, mismatches(report).size)
    }

    @Test
    fun installedVersionInsideTheWildcardRangeIsNotReported() {
        val report = report(listOf(irisRequiringSodium("0.9.x"), sodiumInstalledAt("0.9.2")))

        assertTrue(mismatches(report).isEmpty())
    }

    @Test
    fun installedVersionMatchingAComparatorRangeIsNotReported() {
        val report = report(listOf(irisRequiringSodium(">=0.8.11"), sodiumInstalledAt("0.8.12")))

        assertTrue(mismatches(report).isEmpty())
    }

    // ---- 读不出来就沉默 ----

    @Test
    fun aPredicateTheEvaluatorCannotFullyReadIsNotReported() {
        //括号区间不是 fabric-loader 的写法，求值器不声明理解它，因此保持沉默而不是误报
        val report = report(listOf(irisRequiringSodium("[0.5.9,0.5.11]"), sodiumInstalledAt("0.5.8")))

        assertTrue(mismatches(report).isEmpty())
    }

    @Test
    fun anUnknownInstalledVersionIsNotReported() {
        val report = report(listOf(irisRequiringSodium("0.5.9 || 0.5.11"), sodiumInstalledAt(null)))

        assertTrue(mismatches(report).isEmpty())
    }

    @Test
    fun anInstalledVersionThatIsAPlaceholderIsNotReported() {
        val report = report(listOf(irisRequiringSodium("0.5.9 || 0.5.11"), sodiumInstalledAt("\${file.jarVersion}")))

        assertTrue(mismatches(report).isEmpty())
    }

    // ---- 内嵌副本：id 确实装了，那份副本自己的版本才是真版本 ----

    @Test
    fun aBundledCopyOfTheDependencyDoesNotSilenceTheVersionMismatch() {
        //真实单元格 1.17：Iris 1.1.1 内嵌 sodium 0.3.0+IRIS2。删掉顶层 sodium 文件后 id 仍算装了，
        //所以缺失依赖规则保持沉默（这一点与真正的加载器一致：它看到的内嵌 jar 也是模组）；
        //但 Reese's Sodium Options 要 >=0.3.3，内嵌副本满足不了，加载器正是因此拒绝这个集合。
        val report = report(
            listOf(
                mod(
                    fileName = "reeses_sodium_options-mc1.17.1-1.2.3.jar",
                    modId = "reeses-sodium-options",
                    name = "Reese's Sodium Options",
                    requires = listOf("sodium"),
                    predicates = mapOf("sodium" to listOf(">=0.3.3")),
                ),
                mod(
                    fileName = "sodium-extra-mc1.17.1-0.3.6.jar",
                    modId = "sodium-extra",
                    name = "Sodium Extra",
                    requires = listOf("sodium"),
                    predicates = mapOf("sodium" to listOf("0.3.x")),
                ),
                mod(
                    fileName = "iris-mc1.17-1.1.1.jar",
                    modId = "iris",
                    name = "Iris",
                    provided = mapOf("sodium" to "0.3.0+IRIS2"),
                ),
            ),
            minecraftVersion = "1.17",
        )

        assertTrue(
            "the bundled copy proves the id is installed, so this is not a missing dependency",
            report.problems.none { it.type == ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY },
        )
        val found = mismatches(report)
        assertEquals(1, found.size)
        assertEquals("reeses-sodium-options", found.single().modId)
        assertTrue(found.single().reason.contains("0.3.0+IRIS2"))
    }

    @Test
    fun aBundledCopyThatSatisfiesTheConstraintIsNotReported() {
        val report = report(
            listOf(
                mod(
                    fileName = "reeses_sodium_options-mc1.17.1-1.2.3.jar",
                    modId = "reeses-sodium-options",
                    name = "Reese's Sodium Options",
                    requires = listOf("sodium"),
                    predicates = mapOf("sodium" to listOf(">=0.3.3")),
                ),
                mod(
                    fileName = "iris-mc1.17-1.1.1.jar",
                    modId = "iris",
                    name = "Iris",
                    provided = mapOf("sodium" to "0.3.4+build.13"),
                ),
            ),
            minecraftVersion = "1.17",
        )

        assertTrue(report.isClean)
    }

    @Test
    fun aBundledCopyWhoseVersionCannotBeReadStaysSilent() {
        val report = report(
            listOf(
                mod(
                    fileName = "reeses_sodium_options-mc1.17.1-1.2.3.jar",
                    modId = "reeses-sodium-options",
                    name = "Reese's Sodium Options",
                    requires = listOf("sodium"),
                    predicates = mapOf("sodium" to listOf(">=0.3.3")),
                ),
                mod(
                    fileName = "iris-mc1.17-1.1.1.jar",
                    modId = "iris",
                    name = "Iris",
                    provided = mapOf("sodium" to "\${file.jarVersion}"),
                ),
            ),
            minecraftVersion = "1.17",
        )

        assertTrue(mismatches(report).isEmpty())
    }

    // ---- 只报告，不修 ----

    @Test
    fun anIncompatibleInstalledVersionOffersNoRepair() {
        val detected = report(listOf(irisRequiringSodium("0.5.9 || 0.5.11"), sodiumInstalledAt("0.5.8")))
        val providerQueries = mutableListOf<String>()

        val resolved = runBlocking {
            detected.withFixCandidates(
                minecraftVersion = "1.21.11",
                loader = ModLoader.FABRIC,
                platform = Platform.MODRINTH,
                directProject = { projectId, _ -> providerQueries += projectId; null },
                searchedProject = { projectId, _, _ -> providerQueries += projectId; null },
                projectVersions = { projectId, _ -> providerQueries += projectId; emptyList() },
                installedMod = { null },
            )
        }

        assertTrue("no provider lookup may run for a version mismatch", providerQueries.isEmpty())
        assertFalse(resolved.hasFixableProblems)
        assertTrue(resolved.problems.all { it.fixCandidate == null && it.replacementFix == null })
        assertTrue(resolved.problems.all { it.requiredDependencyId == null })
    }
}
