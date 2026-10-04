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
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * 启动门：模组报告必须先放行，Minecraft 才能被真正拉起。
 *
 * 回归的是「坏模组 + 点启动 → 游戏照常启动」这一类漏网：报告里只要还有任何一条问题，
 * 启动就必须停住等用户处理，唯一允许越过的是用户自己在问题界面上选择「仍然启动」。
 */
class ModDoctorLaunchGateTest {
    private fun reportOf(vararg mods: ModDoctorMetadata) = validateModDoctorMetadata(
        mods = mods.toList(),
        minecraftVersion = "1.21.1",
        loader = ModLoader.FABRIC
    )

    private fun fabricMod(
        fileName: String = "fine.jar",
        modId: String = "fine",
        loader: ModLoader = ModLoader.FABRIC,
        minecraftVersionRanges: List<String> = emptyList(),
        requiredDependencies: List<String> = emptyList(),
    ) = ModDoctorMetadata(
        fileName = fileName,
        modId = modId,
        name = modId,
        loader = loader,
        minecraftVersionRanges = minecraftVersionRanges,
        requiredDependencies = requiredDependencies,
    )

    // ---- A. 干净的实例：放行 ----

    @Test
    fun `a clean instance may launch`() {
        val report = reportOf(fabricMod(minecraftVersionRanges = listOf("[1.21,1.22)")))
        assertEquals(LaunchGateDecision.LAUNCH, decideLaunchGate(report))
    }

    // ---- B. 错误加载器：拦住 ----

    @Test
    fun `a wrong loader blocks the launch`() {
        val report = reportOf(fabricMod(fileName = "neoforge-mod.jar", modId = "neoforge-mod", loader = ModLoader.NEOFORGE))
        assertEquals(
            ModDoctorProblemType.WRONG_LOADER,
            report.problems.single().type
        )
        assertEquals(LaunchGateDecision.BLOCK, decideLaunchGate(report))
    }

    // ---- C. Minecraft 版本不兼容：拦住 ----

    @Test
    fun `a wrong minecraft version blocks the launch`() {
        //一个只说 1.21 的模组装进 1.21.1 实例：本地声明就能定论（真实整合包里就是这么报出来的）
        val report = reportOf(fabricMod(minecraftVersionRanges = listOf("1.21")))
        assertEquals(
            ModDoctorProblemType.WRONG_MINECRAFT_VERSION,
            report.problems.single().type
        )
        assertEquals(LaunchGateDecision.BLOCK, decideLaunchGate(report))
    }

    // ---- D. 缺少必装依赖：拦住（可选依赖缺失不算问题） ----

    @Test
    fun `a missing required dependency blocks the launch`() {
        val report = reportOf(fabricMod(requiredDependencies = listOf("absent-lib")))
        assertEquals(
            ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY,
            report.problems.single().type
        )
        assertEquals(LaunchGateDecision.BLOCK, decideLaunchGate(report))

        //没有必装依赖声明的模组不会产生问题：故意不装的可选依赖不该拦住启动
        val noDependencies = reportOf(fabricMod())
        assertEquals(LaunchGateDecision.LAUNCH, decideLaunchGate(noDependencies))
    }

    @Test
    fun `a duplicate mod blocks the launch`() {
        val report = reportOf(
            fabricMod(fileName = "a.jar", modId = "same-id"),
            fabricMod(fileName = "b.jar", modId = "same-id"),
        )
        assertEquals(
            ModDoctorProblemType.DUPLICATE,
            report.problems.first().type
        )
        assertEquals(LaunchGateDecision.BLOCK, decideLaunchGate(report))
    }

    // ---- E. 只有用户明确选择「仍然启动」才越过启动门 ----

    @Test
    fun `the gate only opens for the explicit user override`() {
        val report = reportOf(fabricMod(fileName = "neoforge-mod.jar", modId = "neoforge-mod", loader = ModLoader.NEOFORGE))

        assertEquals(LaunchGateDecision.BLOCK, decideLaunchGate(report))
        assertEquals(LaunchGateDecision.LAUNCH, decideLaunchGate(report, userOverride = true))
    }

    // ---- F. 整合包实例：安全模式只是不自动改动文件，真正的不兼容依然拦住启动 ----

    @Test
    fun `a pack-managed instance still blocks a real incompatibility`() {
        val versionPath = Files.createTempDirectory("all-launch-gate").toFile()
        File(versionPath, "mods").mkdirs()
        recordModpackManifest(
            versionPath = versionPath,
            manifest = ModpackManifest(
                name = "Test Pack",
                platform = "Modrinth",
                mods = listOf(PackOwnedMod(name = "neoforge-mod.jar")),
                modsComplete = true,
            ),
            modsDir = File(versionPath, "mods"),
        )

        val report = reportOf(fabricMod(fileName = "neoforge-mod.jar", modId = "neoforge-mod", loader = ModLoader.NEOFORGE))
        val filtered = report.applyPackSafeMode(ModpackPolicy.load(versionPath), File(versionPath, "mods"))

        //安全模式不隐藏问题，只摘掉自动改动的动作
        assertEquals(1, filtered.problems.size)
        assertEquals(ModDoctorProblemType.WRONG_LOADER, filtered.problems.single().type)
        assertEquals(LaunchGateDecision.BLOCK, decideLaunchGate(filtered))
    }
}
