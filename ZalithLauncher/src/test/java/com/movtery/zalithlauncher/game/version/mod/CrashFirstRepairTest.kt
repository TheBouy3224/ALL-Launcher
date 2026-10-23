package com.movtery.zalithlauncher.game.version.mod

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 崩溃优先修复：证据只来自这一次启动的日志，修复只经过 Mod Doctor 自己的报告。
 *
 * 日志段是 2026-10-16 在 QA-V3-1（LDPlayer）上真实启动捕获的 Fabric 输出，逐字保留，
 * 没有按想象写的文本。
 */
class CrashFirstRepairTest {

    private val signatures = parseCrashSignatures(File("src/main/assets/crash_signatures.json").readText())

    /** Sodium 0.8.12 + Reese's Sodium Options 2.2.4（要求 sodium =0.8.14）真实启动失败 */
    private val wrongVersionLog = """
        [00:37:15] [main/INFO]: Loading Minecraft 1.21.11 with Fabric Loader 0.19.5
        [00:37:16] [main/WARN]: Mod resolution failed
        [00:37:16] [main/INFO]: Reason: [HARD_DEP reeses-sodium-options 2.2.4+mc1.21.11 {depends sodium @ [=0.8.14+mc1.21.11]}]
        [00:37:16] [main/ERROR]: Incompatible mods found!
        net.fabricmc.loader.impl.FormattedException: Some of your mods are incompatible with the game or each other!
        A potential solution has been determined, this may resolve your problem:
        	 - Replace mod 'Sodium' (sodium) 0.8.12+mc1.21.11 with version 0.8.14+mc1.21.11.
        More details:
        	 - Mod 'Reese's Sodium Options' (reeses-sodium-options) 2.2.4+mc1.21.11 requires version 0.8.14+mc1.21.11 of mod 'Sodium' (sodium), but only the wrong version is present: 0.8.12+mc1.21.11!
        	at net.fabricmc.loader.impl.FormattedException.ofLocalized(FormattedException.java:51)
    """.trimIndent()

    /** Sodium 0.8.14 + Iris 1.10.7（Sodium 声明 breaks iris <=1.10.7）真实启动失败 */
    private val conflictLog = """
        [00:39:27] [main/INFO]: Loading Minecraft 1.21.11 with Fabric Loader 0.19.5
        [00:39:28] [main/WARN]: Mod resolution failed
        [00:39:28] [main/INFO]: Reason: [NEG_HARD_DEP sodium 0.8.14+mc1.21.11 {breaks iris @ [<=1.10.7]}]
        [00:39:28] [main/ERROR]: Incompatible mods found!
        net.fabricmc.loader.impl.FormattedException: Some of your mods are incompatible with the game or each other!
        More details:
        	 - Mod 'Sodium' (sodium) 0.8.14+mc1.21.11 is incompatible with version 1.10.7 or earlier of mod 'Iris' (iris), yet a conflicting version is present: 1.10.7+mc1.21.11!
        	at net.fabricmc.loader.impl.FormattedException.ofLocalized(FormattedException.java:51)
    """.trimIndent()

    // ---- 证据：原句、撇号、机器格式 ----

    @Test
    fun theRealWrongVersionLineNamesBothModsAndBothVersionsEvenWithAnApostropheInTheName() {
        val e = readWrongVersionEvidence(wrongVersionLog)

        assertNotNull(e)
        assertEquals(CrashEvidenceKind.WRONG_VERSION, e!!.kind)
        assertEquals("Reese's Sodium Options", e.requirerName)
        assertEquals("reeses-sodium-options", e.requirerId)
        assertEquals("2.2.4+mc1.21.11", e.requirerVersion)
        assertEquals("sodium", e.dependencyId)
        assertEquals("0.8.14+mc1.21.11", e.constraint)
        assertEquals("0.8.12+mc1.21.11", e.presentVersion)
        assertEquals("the Fabric line and the HARD_DEP reason are both evidence", 2, e.evidence.size)
    }

    @Test
    fun theRealConflictLineNamesTheBreakerTheTargetAndTheInstalledVersion() {
        val e = readModConflictEvidence(conflictLog)

        assertNotNull(e)
        assertEquals(CrashEvidenceKind.MOD_CONFLICT, e!!.kind)
        assertEquals("sodium", e.requirerId)
        assertEquals("0.8.14+mc1.21.11", e.requirerVersion)
        assertEquals("iris", e.dependencyId)
        assertEquals("version 1.10.7 or earlier", e.constraint)
        assertEquals("1.10.7+mc1.21.11", e.presentVersion)
        assertEquals(2, e.evidence.size)
    }

    @Test
    fun theThreeEvidenceReadersDoNotReadEachOthersLines() {
        assertNull(readWrongVersionEvidence(conflictLog))
        assertNull(readModConflictEvidence(wrongVersionLog))
        assertNull(readMissingDependencyEvidence(wrongVersionLog))
        assertNull(readMissingDependencyEvidence(conflictLog))
    }

    // ---- 判定：只有日志里有加载器原句才指认；owner 来自同一行 ----

    @Test
    fun aRealWrongVersionLogIsClassifiedHighWithItsOwnerAndNoDisableAction() {
        val outcome = classifyCapturedLog(wrongVersionLog, exitCode = 1, signatures = signatures)

        assertEquals(LaunchOutcome.FAILED_DURING_STARTUP, outcome.launchOutcome)
        val finding = outcome.finding!!
        assertEquals("incompatible_dependency_version_fabric", finding.signatureId)
        assertEquals(DIAGNOSIS_CLASS_WRONG_VERSION, finding.diagnosisType)
        assertEquals(DiagnosisConfidence.HIGH, finding.confidence)
        assertEquals("reeses-sodium-options", finding.ownerModId)
        assertEquals("never a disable action: the repair goes through Mod Doctor only", DiagnosisAction.NONE, finding.action)
    }

    @Test
    fun aRealConflictLogIsClassifiedHighWithTheBreakerAsOwnerAndNoDisableAction() {
        val finding = classifyCapturedLog(conflictLog, exitCode = 1, signatures = signatures).finding!!

        assertEquals("incompatible_mod_conflict_fabric", finding.signatureId)
        assertEquals(DIAGNOSIS_CLASS_MOD_CONFLICT, finding.diagnosisType)
        assertEquals("sodium", finding.ownerModId)
        assertEquals(DiagnosisAction.NONE, finding.action)
    }

    @Test
    fun theWrongVersionLogDoesNotProduceAFixThroughTheDisablePath() {
        //resolveCrashDecision 只在 HIGH + DISABLE 时给出禁用动作；这两类是 NONE，所以崩溃页面的「临时禁用」不会出现
        val finding = classifyCapturedLog(wrongVersionLog, 1, signatures).finding!!
        assertFalse(finding.confidence == DiagnosisConfidence.HIGH && finding.action == DiagnosisAction.DISABLE)
    }

    // ---- C: 与模组无关的崩溃 ----

    @Test
    fun aCrashThatNamesNoModGetsNoFindingAndNoRepair() {
        val log = """
            [12:00:00] [Render thread/INFO]: Sound engine started
            [12:01:00] [Render thread/ERROR]: #@!@# Game crashed! Crash report saved to: #@!@# /sdcard/crash-reports/crash.txt
            java.lang.OutOfMemoryError: Java heap space
        """.trimIndent()

        val outcome = classifyCapturedLog(log, exitCode = 1, signatures = signatures)

        assertEquals(LaunchOutcome.CRASHED_UNEXPECTEDLY, outcome.launchOutcome)
        assertNull("an out-of-memory crash is not a mod problem: no finding, so no repair is offered", outcome.finding)
        assertNull(readWrongVersionEvidence(log))
        assertNull(readModConflictEvidence(log))
        assertNull(readMissingDependencyEvidence(log))
    }

    @Test
    fun aKilledHealthySessionIsNotBlamedOnAnyMod() {
        val log = "[12:00:00] [Render thread/INFO]: Sound engine started\n[12:00:30] [main/INFO]: Stopping!"

        val outcome = classifyCapturedLog(log, exitCode = 137, signatures = signatures)

        assertEquals(LaunchOutcome.REACHED_MARKER, outcome.launchOutcome)
        assertNull(outcome.finding)
    }

    // ---- D: 旧的 / 不完整的日志 ----

    @Test
    fun aLogCutBeforeTheOffendingLineDoesNotInventADiagnosis() {
        //加载器只打了前几行就停了：没有原句，就没有指认，也没有可修复的证据
        val truncated = wrongVersionLog.lines().take(3).joinToString("\n")

        val outcome = classifyCapturedLog(truncated, exitCode = 1, signatures = signatures)

        //只剩「Mod resolution failed」：最多是未归因的加载器失败（LOW），不点名模组、没有动作、没有可修复的证据
        assertEquals("loader_fatal_unattributed", outcome.finding?.signatureId)
        assertEquals(DiagnosisConfidence.LOW, outcome.confidence)
        assertNull(outcome.finding?.ownerModId)
        assertEquals(DiagnosisAction.NONE, outcome.finding?.action)
        assertNull(readWrongVersionEvidence(truncated))
    }

    @Test
    fun anEmptyOrMissingLogProducesNothing() {
        assertNull(classifyCapturedLog("", exitCode = 1, signatures = signatures).finding)
        assertEquals("", readCapturedLogTail(File("does-not-exist.log")))
        assertNull(readWrongVersionEvidence(""))
    }

    @Test
    fun theHeaderLineAloneWithoutTheDetailLineStaysUnattributed() {
        //只有 "Incompatible mods found!" 一行（被截断的日志）：落到未归因的加载器失败，LOW，没有 owner 也没有动作
        val finding = classifyCapturedLog("[00:37:16] [main/ERROR]: Incompatible mods found!", 1, signatures).finding!!

        assertEquals("loader_fatal_unattributed", finding.signatureId)
        assertEquals(DiagnosisConfidence.LOW, finding.confidence)
        assertNull(finding.ownerModId)
        assertEquals(DiagnosisAction.NONE, finding.action)
    }

    @Test
    fun aLogWithBothAMissingDependencyAndAWrongVersionKeepsTheMissingDependencyFirst() {
        val both = wrongVersionLog + "\n" +
            "\t - Mod 'Litematica' (litematica) 0.26.16 requires any version between 0.27.19- (inclusive) and 0.28.0- (exclusive) of malilib, which is missing!"

        val finding = classifyCapturedLog(both, 1, signatures).finding!!

        assertEquals("the original missing-dependency flow is unchanged", "missing_required_dependency_fabric", finding.signatureId)
    }

    // ---- 选择：结论只来自 Mod Doctor 的同一份报告 ----

    private fun versionProblem(withFix: Boolean) = ModDoctorProblem(
        type = ModDoctorProblemType.INCOMPATIBLE_DEPENDENCY_VERSION,
        fileName = "reeses-sodium-options-fabric-2.2.4+mc1.21.11.jar",
        modId = "reeses-sodium-options",
        reason = "needs sodium =0.8.14",
        dependencyFileName = "sodium-fabric-0.8.12+mc1.21.11.jar",
        dependencyModId = "sodium",
        ruleFallbackFix = if (withFix) ModDoctorSetFix(
            members = listOf(ModDoctorSetMember("reeses-sodium-options", "reeses-sodium-options-fabric-2.2.4+mc1.21.11.jar", "2.2.3")),
            allowed = mapOf("reeses-sodium-options" to "2.2.3", "sodium" to "0.8.12"),
        ) else null,
    )

    @Test
    fun aWrongVersionProblemWithAFixIsReadyAndOneWithoutIsNotInstalled() {
        val evidence = readWrongVersionEvidence(wrongVersionLog)!!

        val ready = selectDependencyRepair(ModDoctorReport(problems = listOf(versionProblem(withFix = true))), evidence)
        assertTrue(ready is DependencyRepairSelection.ReadyFix)

        val none = selectDependencyRepair(ModDoctorReport(problems = listOf(versionProblem(withFix = false))), evidence)
        assertTrue("no safe plan: only an explanation, nothing is changed", none is DependencyRepairSelection.NoSafeVersion)

        val absent = selectDependencyRepair(ModDoctorReport(problems = emptyList()), evidence)
        assertTrue("Mod Doctor does not see the problem (already fixed / stale log)", absent === DependencyRepairSelection.NotReported)
    }

    @Test
    fun aProblemAboutAnotherModIsNeverSelectedForTheLogsEvidence() {
        val evidence = readWrongVersionEvidence(wrongVersionLog)!!
        val other = versionProblem(withFix = true).copy(modId = "someothermod")

        assertTrue(selectDependencyRepair(ModDoctorReport(problems = listOf(other)), evidence) === DependencyRepairSelection.NotReported)
    }

    private fun pairProblem(withPlan: Boolean) = ModDoctorProblem(
        type = ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR,
        fileName = "sodium-fabric-0.8.14+mc1.21.11.jar, iris-fabric-1.10.7+mc1.21.11.jar",
        modId = "sodium",
        reason = "Sodium cannot be used with Iris",
        breaksSourceFileName = "sodium-fabric-0.8.14+mc1.21.11.jar",
        breaksTargetFileName = "iris-fabric-1.10.7+mc1.21.11.jar",
        breaksTargetModId = "iris",
        breaksFix = if (withPlan) ModDoctorBreaksFix(
            target = ModDoctorReplacementFix("iris-fabric-1.10.7+mc1.21.11.jar", "iris-project", com.movtery.zalithlauncher.game.download.assets.platform.Platform.MODRINTH),
        ) else null,
    )

    @Test
    fun aConflictIsMatchedByBothIdsAndOnlyOffersWhatModDoctorPlanned() {
        val evidence = readModConflictEvidence(conflictLog)!!

        assertTrue(selectDependencyRepair(ModDoctorReport(problems = listOf(pairProblem(true))), evidence) is DependencyRepairSelection.ReadyFix)
        assertTrue(selectDependencyRepair(ModDoctorReport(problems = listOf(pairProblem(false))), evidence) is DependencyRepairSelection.NoSafeVersion)
        val wrongTarget = pairProblem(true).copy(breaksTargetModId = "somethingelse")
        assertTrue(selectDependencyRepair(ModDoctorReport(problems = listOf(wrongTarget)), evidence) === DependencyRepairSelection.NotReported)
    }

    @Test
    fun afterTheRepairTheRecheckMustNoLongerReportTheSameProblem() {
        val wrong = readWrongVersionEvidence(wrongVersionLog)!!
        val conflict = readModConflictEvidence(conflictLog)!!

        assertFalse(dependencyRequirementResolved(ModDoctorReport(problems = listOf(versionProblem(true))), wrong))
        assertTrue(dependencyRequirementResolved(ModDoctorReport(problems = emptyList()), wrong))
        assertFalse(dependencyRequirementResolved(ModDoctorReport(problems = listOf(pairProblem(true))), conflict))
        assertTrue(dependencyRequirementResolved(ModDoctorReport(problems = emptyList()), conflict))
    }
}
