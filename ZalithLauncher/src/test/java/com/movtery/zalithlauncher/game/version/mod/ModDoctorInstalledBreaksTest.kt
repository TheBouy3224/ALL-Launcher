/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

/**
 * 已启用模组之间的 Fabric `breaks` 关系必须在真正加载器之前被发现。
 *
 * 回归来源：Iris 1.10.7 + Sodium 0.8.14 在真实 Fabric 0.19.5 上被加载器拒绝，
 * 但旧 Mod Doctor 只检查 `depends`，错误地报告 0 problems。
 */
class ModDoctorInstalledBreaksTest {
    private fun mod(
        fileName: String,
        modId: String,
        name: String,
        version: String? = null,
        requires: List<String> = emptyList(),
        predicates: Map<String, List<String>> = emptyMap(),
        breaks: Map<String, List<String>> = emptyMap(),
    ) = ModDoctorMetadata(
        fileName = fileName,
        modId = modId,
        name = name,
        loader = ModLoader.FABRIC,
        version = version,
        requiredDependencies = requires,
        requiredDependencyPredicates = predicates,
        breakPredicates = breaks,
    )

    private fun report(mods: List<ModDoctorMetadata>) = validateModDoctorMetadata(
        mods = mods,
        minecraftVersion = "1.21.11",
        loader = ModLoader.FABRIC,
    )

    private fun breaksProblems(report: ModDoctorReport) =
        report.problems.filter { it.type == ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR }

    @Test
    fun matchingInstalledBreaksPredicateIsBlocking() {
        val report = report(
            listOf(
                mod("sodium.jar", "sodium", "Sodium", "0.8.14+mc1.21.11", breaks = mapOf("iris" to listOf("<=1.10.7"))),
                mod("iris.jar", "iris", "Iris", "1.10.7+mc1.21.11"),
            )
        )

        val problem = breaksProblems(report).single()
        assertEquals("sodium", problem.modId)
        assertTrue(problem.reason.contains("Sodium 0.8.14+mc1.21.11"))
        assertTrue(problem.reason.contains("Iris 1.10.7+mc1.21.11"))
        assertFalse(problem.type.isReplaceable())
        assertEquals(LaunchGateDecision.BLOCK, decideLaunchGate(report))
    }

    @Test
    fun nonMatchingBreaksPredicateDoesNotReport() {
        val report = report(
            listOf(
                mod("sodium.jar", "sodium", "Sodium", "0.8.14", breaks = mapOf("iris" to listOf("<1.10.7"))),
                mod("iris.jar", "iris", "Iris", "1.10.7"),
            )
        )

        assertTrue(breaksProblems(report).isEmpty())
    }

    @Test
    fun absentBreaksTargetDoesNotReport() {
        val report = report(
            listOf(mod("sodium.jar", "sodium", "Sodium", "0.8.14", breaks = mapOf("iris" to listOf("<=1.10.7"))))
        )

        assertTrue(breaksProblems(report).isEmpty())
    }

    @Test
    fun disabledTargetCannotCreateAnActiveBreaksConflict() {
        // validateModDoctorMetadata receives enabled/readable metadata only; leaving Iris out models a
        // disabled .jar.disabled file, exactly like ModDoctor.inspect filters it before this seam.
        val enabled = listOf(
            mod("sodium.jar", "sodium", "Sodium", "0.8.14", breaks = mapOf("iris" to listOf("<=1.10.7")))
        )

        assertTrue(breaksProblems(report(enabled)).isEmpty())
    }

    @Test
    fun packSafeModeKeepsTheBreaksProblemBlockingAndReportOnly() {
        val report = report(
            listOf(
                mod("sodium.jar", "sodium", "Sodium", "0.8.14", breaks = mapOf("iris" to listOf("<=1.10.7"))),
                mod("iris.jar", "iris", "Iris", "1.10.7"),
            )
        )
        val modsDir = Files.createTempDirectory("moddoctor-breaks-pack").toFile()
        try {
            val guarded = report.applyPackSafeMode(
                ModpackPolicy(
                    ModpackManifest(
                        name = "QA",
                        mods = listOf(
                            PackOwnedMod(name = "sodium.jar", requiredCore = true),
                            PackOwnedMod(name = "iris.jar", requiredCore = true),
                        ),
                        modsComplete = true,
                    )
                ),
                modsDir,
            )

            assertEquals(1, breaksProblems(guarded).size)
            assertEquals(LaunchGateDecision.BLOCK, decideLaunchGate(guarded))
            assertFalse(guarded.hasFixableProblems)
        } finally {
            modsDir.deleteRecursively()
        }
    }

    @Test
    fun normalDependsPresenceAndVersionRulesRemainUnchanged() {
        val dependencyProblem = report(
            listOf(
                mod(
                    "iris.jar",
                    "iris",
                    "Iris",
                    "1.10.7",
                    requires = listOf("sodium"),
                    predicates = mapOf("sodium" to listOf("0.8.x")),
                ),
                mod("sodium.jar", "sodium", "Sodium", "0.8.14"),
            )
        )

        assertTrue(dependencyProblem.isClean)
    }
}
