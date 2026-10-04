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

import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Required core is protected; optional pack files and user additions remain customizable.
 * 非整合包实例的行为必须完全不变。
 */
class ModpackSafeModeTest {
    private val pinnedFile = "pack-pinned-mod-1.2.3.jar"
    private val otherFile = "pack-other-mod-4.5.6.jar"

    private fun tempInstance(withManifest: Boolean, manifest: ModpackManifest? = null): File {
        val versionPath = Files.createTempDirectory("all-safe-mode").toFile()
        File(versionPath, "mods").mkdirs()
        if (withManifest) {
            recordModpackManifest(
                versionPath = versionPath,
                manifest = manifest ?: ModpackManifest(
                    name = "Test Pack",
                    platform = "Modrinth",
                    mods = listOf(PackOwnedMod(name = pinnedFile, sha1 = "a".repeat(40), url = "https://example.invalid/$pinnedFile", requiredCore = true)),
                    modsComplete = true,
                ),
                modsDir = File(versionPath, "mods"),
            )
        }
        return versionPath
    }

    private fun replacementProblem(fileName: String) = ModDoctorProblem(
        type = ModDoctorProblemType.WRONG_LOADER,
        fileName = fileName,
        modId = "packmod",
        reason = "published for NeoForge, not Fabric.",
        replacementFix = ModDoctorReplacementFix(
            fileName = fileName,
            projectId = "AANobbMI",
            platform = Platform.MODRINTH,
        ),
    )

    private fun missingDependencyProblem() = ModDoctorProblem(
        type = ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY,
        fileName = otherFile,
        modId = "othermod",
        reason = "requires balm.",
        requiredDependencyId = "balm",
        fixCandidate = ModDoctorFixCandidate(
            dependencyId = "balm",
            projectId = "MBAkmtvl",
            platform = Platform.MODRINTH,
        ),
    )

    // ---- A. 手工管理的实例：行为必须完全不变 ----

    @Test
    fun `a standalone instance keeps the current Mod Doctor plan untouched`() {
        val versionPath = tempInstance(withManifest = false)
        val modsDir = File(versionPath, "mods")
        val report = ModDoctorReport(listOf(replacementProblem(pinnedFile), missingDependencyProblem()))

        val policy = ModpackPolicy.load(versionPath)
        assertFalse(policy.isPackManaged)

        val filtered = report.applyPackSafeMode(policy, modsDir)
        assertEquals(report, filtered)
        assertTrue(filtered.hasFixableProblems)
    }

    // ---- B. 整合包钉住的版本：不允许被替换成别的版本 ----

    @Test
    fun `a pack-owned mod is never auto-replaced, even by a compatible build`() {
        val versionPath = tempInstance(withManifest = true)
        val modsDir = File(versionPath, "mods")
        val report = ModDoctorReport(listOf(replacementProblem(pinnedFile)))

        val filtered = report.applyPackSafeMode(ModpackPolicy.load(versionPath), modsDir)

        val problem = filtered.problems.single()
        assertNull(problem.replacementFix)
        assertFalse(filtered.hasFixableProblems)
        assertTrue(problem.reason.contains("managed by the modpack"))
        //仍然是报告：问题本身不能被隐藏
        assertEquals(ModDoctorProblemType.WRONG_LOADER, problem.type)
    }

    // ---- C. Normal Mod Doctor repairs stay available after the core passes ----

    @Test
    fun `normal dependency fixes are not rewritten as pack snapshot restores`() {
        val versionPath = tempInstance(withManifest = true)
        val modsDir = File(versionPath, "mods")
        val report = ModDoctorReport(listOf(missingDependencyProblem()))

        val filtered = report.applyPackSafeMode(ModpackPolicy.load(versionPath), modsDir)

        assertEquals(report, filtered)
        assertNotNull(filtered.problems.single().fixCandidate)
    }

    @Test
    fun `a missing required core without exact identity is report-only`() {
        val versionPath = tempInstance(
            withManifest = true,
            manifest = ModpackManifest(
                name = "Test Pack",
                platform = "CurseForge",
                mods = listOf(PackOwnedMod(name = pinnedFile, sha1 = null, url = null, requiredCore = true)),
                modsComplete = true,
            ),
        )
        val modsDir = File(versionPath, "mods")
        val problem = ModpackPolicy.load(versionPath).requiredCoreProblems(modsDir).single()
        assertNull(problem.exactRestoreFile)
        assertEquals(ModDoctorProblemType.PACK_REQUIRED_CORE, problem.type)
    }

    @Test
    fun `each missing required core with exact identity can be restored`() {
        val versionPath = tempInstance(
            withManifest = true,
            manifest = ModpackManifest(
                name = "Test Pack",
                platform = "Modrinth",
                mods = listOf(
                    PackOwnedMod(name = pinnedFile, sha1 = "a".repeat(40), url = "https://example.invalid/a.jar", requiredCore = true),
                    PackOwnedMod(name = otherFile, sha1 = "b".repeat(40), url = "https://example.invalid/b.jar", requiredCore = true),
                ),
                modsComplete = true,
            ),
        )
        val modsDir = File(versionPath, "mods")
        val problems = ModpackPolicy.load(versionPath).requiredCoreProblems(modsDir)
        assertEquals(2, problems.size)
        assertEquals(setOf(pinnedFile, otherFile), problems.map { it.exactRestoreFile?.name }.toSet())
    }

    // ---- D. 可选依赖：整合包里缺失的可选依赖不会被自动安装 ----

    @Test
    fun `an absent optional dependency is never turned into an install target`() {
        val versionPath = tempInstance(withManifest = true)
        val modsDir = File(versionPath, "mods")
        //可选依赖不会产生 fixCandidate，安全模式也不允许自己造一个出来
        val report = ModDoctorReport(
            listOf(
                ModDoctorProblem(
                    type = ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY,
                    fileName = otherFile,
                    modId = "othermod",
                    reason = "optional dependency is absent.",
                )
            )
        )

        val filtered = report.applyPackSafeMode(ModpackPolicy.load(versionPath), modsDir)

        assertNull(filtered.problems.single().fixCandidate)
        assertNull(filtered.problems.single().exactRestoreFile)
        assertFalse(filtered.hasFixableProblems)
    }

    // ---- E. 归属无法证明：清单不完整时，未列出的文件同样按整合包文件对待 ----

    @Test
    fun `an unlisted mod remains customizable when the pack manifest is incomplete`() {
        val versionPath = tempInstance(
            withManifest = true,
            manifest = ModpackManifest(
                name = "Test Pack",
                platform = "Modrinth",
                mods = listOf(PackOwnedMod(name = pinnedFile, requiredCore = true)),
                modsComplete = false,
            ),
        )
        val modsDir = File(versionPath, "mods")
        val report = ModDoctorReport(listOf(replacementProblem("user-added-later.jar")))

        val filtered = report.applyPackSafeMode(ModpackPolicy.load(versionPath), modsDir)

        assertNotNull(filtered.problems.single().replacementFix)
    }

    @Test
    fun `a mod the pack never shipped stays fixable when the manifest is complete`() {
        val versionPath = tempInstance(withManifest = true)
        val modsDir = File(versionPath, "mods")
        val report = ModDoctorReport(listOf(replacementProblem("user-added-later.jar")))

        val filtered = report.applyPackSafeMode(ModpackPolicy.load(versionPath), modsDir)

        assertNotNull(filtered.problems.single().replacementFix)
    }

    // ---- 清单读写与归属判断 ----

    @Test
    fun `the recorded manifest survives a round trip and lists the pack mods`() {
        val versionPath = tempInstance(withManifest = true)
        val policy = ModpackPolicy.load(versionPath)

        assertTrue(policy.isPackManaged)
        assertEquals(true, policy.manifest?.modsComplete)
        assertEquals("Test Pack", policy.manifest?.name)
        assertTrue(policy.isPackOwned(pinnedFile))
        assertFalse(policy.isPackOwned("user-added-later.jar"))
        assertEquals("https://example.invalid/$pinnedFile", policy.exactRestoreIdentity(pinnedFile)?.url)
    }

    @Test
    fun `an extra jar under the instance does not become required core`() {
        val versionPath = tempInstance(withManifest = false)
        val modsDir = File(versionPath, "mods").apply { mkdirs() }
        File(modsDir, "override-mod.jar").writeText("x")

        val recorded = recordModpackManifest(
            versionPath = versionPath,
            manifest = ModpackManifest(
                name = "Pack with overrides",
                platform = "Modrinth",
                mods = listOf(PackOwnedMod(name = pinnedFile, requiredCore = true)),
                modsComplete = true,
            ),
            modsDir = modsDir,
        )

        assertFalse(recorded.modsComplete)
        assertFalse(ModpackPolicy.load(versionPath).isPackOwned("override-mod.jar"))
    }

    @Test
    fun `an unreadable manifest blocks because required core cannot be verified`() {
        val versionPath = tempInstance(withManifest = false)
        val configDir = File(versionPath, BuildKeysIdentifier).apply { mkdirs() }
        File(configDir, ModpackPolicy.FILE_NAME).writeText("{ this is not json")

        val policy = ModpackPolicy.load(versionPath)
        assertTrue(policy.isPackManaged)
        assertFalse(policy.isPackOwned("anything.jar"))
        assertEquals(ModDoctorProblemType.PACK_REQUIRED_CORE, policy.requiredCoreProblems(File(versionPath, "mods")).single().type)
    }

    @Test
    fun `a legacy snapshot without required-core labels is not enforced as a lockfile`() {
        val versionPath = tempInstance(
            withManifest = true,
            manifest = ModpackManifest(
                name = "Legacy pack",
                mods = listOf(PackOwnedMod(name = pinnedFile)),
                modsComplete = true,
            ),
        )
        val policy = ModpackPolicy.load(versionPath)

        assertFalse(policy.isPackOwned(pinnedFile))
        assertEquals(ModDoctorProblemType.PACK_REQUIRED_CORE, policy.requiredCoreProblems(File(versionPath, "mods")).single().type)
    }
}

/** 实例元数据目录名（与 VersionConfig 使用同一个标识） */
private val BuildKeysIdentifier: String =
    com.movtery.zalithlauncher.BuildKeys.LAUNCHER_IDENTIFIER
