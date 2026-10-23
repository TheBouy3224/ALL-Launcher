package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Play First, Repair on Crash：启动前只剩整合包 required core 的完整性检查；
 * 崩溃页面与手动检查共用同一套已发布规则（成对冲突也先看规则）。
 */
class PlayFirstHybridTest {
    private val shippedRules = parseModDoctorRules(File("src/main/assets/mod_doctor_rules.json").readText())

    // ---- 启动前检查 ----

    @Test
    fun aNormalInstanceHasNoPrelaunchBlockerEvenWithBrokenMods() {
        val dir = Files.createTempDirectory("play-first").toFile()
        File(dir, "broken-mod.jar").writeText("not a jar")

        val report = prelaunchSafetyProblems(ModpackPolicy(manifest = null), dir)

        assertTrue("nothing is scanned or blocked before launch for a normal instance", report.isClean)
        assertTrue(report.advisories.isEmpty())
    }

    @Test
    fun aModpackStillBlocksWhenItsRequiredCoreIsMissing() {
        val dir = Files.createTempDirectory("play-first-pack").toFile()
        val manifest = ModpackManifest(
            name = "Test Pack", platform = "Modrinth", modsComplete = true,
            mods = listOf(PackOwnedMod(name = "core-1.0.jar", sha1 = "a".repeat(40), url = "https://example.invalid/core-1.0.jar", requiredCore = true)),
        )

        val report = prelaunchSafetyProblems(ModpackPolicy(manifest = manifest), dir)

        assertFalse("a missing required core is the one integrity problem that still stops the launch", report.isClean)
        assertEquals(ModDoctorProblemType.PACK_REQUIRED_CORE, report.problems.first().type)
    }

    @Test
    fun theLaunchGateItselfIsUnchanged() {
        assertEquals(LaunchGateDecision.LAUNCH, decideLaunchGate(ModDoctorReport(emptyList())))
    }

    // ---- 规则优先：成对冲突 ----

    private fun sodium(version: String, breaksIris: Boolean = false) = ModDoctorMetadata(
        fileName = "sodium-fabric-$version+mc1.21.11.jar", modId = "sodium", name = "Sodium", loader = ModLoader.FABRIC,
        version = "$version+mc1.21.11",
        breakPredicates = if (breaksIris) mapOf("iris" to listOf("<=1.10.7")) else emptyMap(),
    )

    private fun needsSodium(id: String, name: String, version: String) = ModDoctorMetadata(
        fileName = "$id-$version+mc1.21.11.jar", modId = id, name = name, loader = ModLoader.FABRIC,
        version = "$version+mc1.21.11",
        requiredDependencies = listOf("sodium"),
        requiredDependencyPredicates = mapOf("sodium" to listOf(">=0.8.0")),
    )

    private fun report(vararg mods: ModDoctorMetadata) = validateModDoctorMetadata(
        mods = mods.toList(), minecraftVersion = "1.21.11", loader = ModLoader.FABRIC, setRules = shippedRules,
    )

    @Test
    fun aRealPairConflictInsideThePublishedSetGetsTheRuleFixFirst() {
        val r = report(
            sodium("0.8.14", breaksIris = true),
            needsSodium("sodium-extra", "Sodium Extra", "0.9.1"),
            needsSodium("iris", "Iris", "1.10.7"),
            needsSodium("reeses-sodium-options", "Reese's Sodium Options", "2.2.3"),
        )

        val pair = r.problems.single { it.type == ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR }
        assertEquals("sodium", pair.modId)
        assertEquals("iris", pair.breaksTargetModId)
        val fix = pair.ruleFallbackFix
        assertNotNull("the published known-good set names both mods, so its fix is offered first", fix)
        //只改与规则不一致的成员：Sodium 0.8.14 -> 0.8.12；Iris 已在规则版本，不动
        assertEquals(listOf("sodium"), fix!!.members.map { it.modId })
        assertEquals("0.8.12", fix.members.single().targetVersion)
    }

    @Test
    fun aPairConflictOutsideAnyPublishedRuleGetsNoRuleFix() {
        val r = report(sodium("0.8.14", breaksIris = true), needsSodium("iris", "Iris", "1.10.7"))

        val pair = r.problems.single { it.type == ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR }
        assertNull("no rule covers this set: only the generic planner (with full validation) may act", pair.ruleFallbackFix)
    }

    @Test
    fun theCrashPageTreatsARuleBackedConflictAsReadyEvenWithoutAGenericPlan() {
        val evidence = MissingDependencyEvidence(
            requirerName = "Sodium", requirerId = "sodium", requirerVersion = "0.8.14+mc1.21.11",
            dependencyId = "iris", constraint = "version 1.10.7 or earlier", evidence = listOf("line"),
            kind = CrashEvidenceKind.MOD_CONFLICT, presentVersion = "1.10.7+mc1.21.11",
        )
        val problem = ModDoctorProblem(
            type = ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR, fileName = "a.jar, b.jar", modId = "sodium", reason = "x",
            breaksTargetModId = "iris",
            ruleFallbackFix = ModDoctorSetFix(
                members = listOf(ModDoctorSetMember("sodium", "sodium-fabric-0.8.14+mc1.21.11.jar", "0.8.12")),
                allowed = mapOf("sodium" to "0.8.12"),
            ),
        )

        assertTrue(selectDependencyRepair(ModDoctorReport(listOf(problem)), evidence) is DependencyRepairSelection.ReadyFix)
        assertTrue(
            "with neither a generic plan nor a rule there is nothing to offer",
            selectDependencyRepair(ModDoctorReport(listOf(problem.copy(ruleFallbackFix = null))), evidence) is DependencyRepairSelection.NoSafeVersion,
        )
    }

    /**
     * 规则方案覆盖「已安装的全部规则成员」：冲突是 sodium 0.8.14 对 iris，但 RSO 2.2.4 声明 `sodium =0.8.14`，
     * 所以方案必须同时把 RSO 换到规则里的 2.2.3，否则「冲突解决了、整组没修好」（2026-10-16 真机 step9 B 的崩溃）。
     * 没安装的成员不强装；已经在规则版本上的成员不动。
     */
    @Test
    fun theRulePlanCoversEveryInstalledRuleMemberThatDiffersNotJustThePair() {
        val rso224 = ModDoctorMetadata(
            fileName = "reeses-sodium-options-2.2.4+mc1.21.11.jar", modId = "reeses-sodium-options", name = "Reese's Sodium Options",
            loader = ModLoader.FABRIC, version = "2.2.4+mc1.21.11",
            requiredDependencies = listOf("sodium"),
            requiredDependencyPredicates = mapOf("sodium" to listOf("=0.8.14+mc1.21.11")),
        )
        val before = report(
            sodium("0.8.14", breaksIris = true), needsSodium("sodium-extra", "Sodium Extra", "0.9.1"),
            needsSodium("iris", "Iris", "1.10.7"), rso224,
        )
        val pair = before.problems.single { it.type == ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR }
        val members = pair.ruleFallbackFix!!.members
        assertEquals(setOf("sodium", "reeses-sodium-options"), members.map { it.modId }.toSet())
        assertEquals("0.8.12", members.single { it.modId == "sodium" }.targetVersion)
        assertEquals("2.2.3", members.single { it.modId == "reeses-sodium-options" }.targetVersion)
    }
}
