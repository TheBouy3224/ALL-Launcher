package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 崩溃页面「安装依赖并修复」的判定：证据从日志来，结论必须来自 Mod Doctor 的同一份报告。
 * 只测判定；下载、快照、重启在 ErrorActivity 里，靠真机 E2E 验证。
 */
class CrashDependencyRepairTest {

    /** 真实 QA-V3-1 启动（Litematica 0.26.16 缺 malilib）捕获的 Fabric 输出，逐字保留 */
    private val realLog = """
        [02:14:18] [main/INFO]: Loading Minecraft 1.21.11 with Fabric Loader 0.19.5
        [02:14:19] [main/WARN]: Mod resolution failed
        [02:14:19] [main/INFO]: Reason: [HARD_DEP litematica 0.26.16 {depends malilib @ [>=0.27.19- <0.28.0-]}]
        [02:14:19] [main/ERROR]: Incompatible mods found!
        net.fabricmc.loader.impl.FormattedException: Some of your mods are incompatible with the game or each other!
        A potential solution has been determined, this may resolve your problem:
        	 - Install malilib, any version between 0.27.19- (inclusive) and 0.28.0- (exclusive).
        More details:
        	 - Mod 'Litematica' (litematica) 0.26.16 requires any version between 0.27.19- (inclusive) and 0.28.0- (exclusive) of malilib, which is missing!
        	at net.fabricmc.loader.impl.FormattedException.ofLocalized(FormattedException.java:51)
    """.trimIndent()

    private val humanRange = "any version between 0.27.19- (inclusive) and 0.28.0- (exclusive)"

    private val litematicaEvidence = MissingDependencyEvidence(
        requirerName = "Litematica",
        requirerId = "litematica",
        requirerVersion = "0.26.16",
        dependencyId = "malilib",
        constraint = ">=0.27.19- <0.28.0-",
        evidence = listOf("evidence"),
    )

    // ---- 证据 ----

    @Test
    fun theRealFabricLogNamesTheRequirerTheDependencyAndTheMachineRange() {
        val evidence = readMissingDependencyEvidence(realLog)

        assertNotNull(evidence)
        assertEquals("Litematica", evidence!!.requirerName)
        assertEquals("litematica", evidence.requirerId)
        assertEquals("0.26.16", evidence.requirerVersion)
        assertEquals("malilib", evidence.dependencyId)
        assertEquals(">=0.27.19- <0.28.0-", evidence.constraint)
        assertEquals("the Fabric line and the Reason line are both kept as evidence", 2, evidence.evidence.size)
        assertTrue(evidence.evidence[0].endsWith("which is missing!"))
        assertTrue(evidence.evidence[1].startsWith("[HARD_DEP litematica 0.26.16"))
    }

    @Test
    fun withoutTheReasonLineTheEnglishRangeIsUsedAsTheConstraint() {
        val logWithoutReason = realLog.lines().filterNot { it.contains("Reason:") }.joinToString("\n")

        val evidence = readMissingDependencyEvidence(logWithoutReason)

        assertNotNull(evidence)
        assertEquals(humanRange, evidence!!.constraint)
        assertEquals("only the Fabric line is evidence", 1, evidence.evidence.size)
    }

    @Test
    fun aReasonLineAboutAnotherModIsNotUsedForTheRange() {
        val mismatched = realLog.replace("Reason: [HARD_DEP litematica", "Reason: [HARD_DEP somethingelse")

        val evidence = readMissingDependencyEvidence(mismatched)

        assertNotNull(evidence)
        assertEquals("a Reason line that names another requirer must not supply the range", humanRange, evidence!!.constraint)
        assertEquals(1, evidence.evidence.size)
    }

    @Test
    fun aBreaksOnlyLoaderErrorHasNoDependencyEvidence() {
        val breaksLog = """
            [02:14:19] [main/ERROR]: Incompatible mods found!
            	 - Mod 'Sodium' (sodium) 0.8.14 breaks iris 1.10.7
        """.trimIndent()

        assertNull(readMissingDependencyEvidence(breaksLog))
    }

    @Test
    fun aMissingLineWithoutAParsableRequirerHasNoEvidence() {
        val vague = """
            [02:14:19] [main/ERROR]: Incompatible mods found!
            	 - something requires a thing, which is missing!
        """.trimIndent()

        assertNull("no requirer can be named, so nothing may be offered", readMissingDependencyEvidence(vague))
    }

    // ---- 选择：只认 Mod Doctor 报告里同一条问题 ----

    @Test
    fun selectionIsReadyOnlyWhenTheRequirerAndDependencyMatchAndACandidateExists() {
        val candidate = candidate("malilib", versionName = "0.27.20")
        val report = ModDoctorReport(listOf(missingDependency("litematica", "malilib", candidate)))

        val selection = selectDependencyRepair(report, litematicaEvidence)

        assertTrue(selection is DependencyRepairSelection.Ready)
        assertEquals("0.27.20", (selection as DependencyRepairSelection.Ready).candidate.versionName)
    }

    @Test
    fun aMatchingProblemWithoutACandidateIsNoSafeVersionAndKeepsTheReason() {
        val problem = missingDependency("litematica", "malilib", fixCandidate = null, reason = "no safe release")
        val report = ModDoctorReport(listOf(problem))

        val selection = selectDependencyRepair(report, litematicaEvidence)

        assertTrue(selection is DependencyRepairSelection.NoSafeVersion)
        assertEquals("no safe release", (selection as DependencyRepairSelection.NoSafeVersion).problem.reason)
    }

    @Test
    fun aProblemForAnotherRequirerOrAnotherDependencyIsNeverSelected() {
        val candidate = candidate("malilib", versionName = "0.27.20")
        val otherRequirer = ModDoctorReport(listOf(missingDependency("sodium", "malilib", candidate)))
        val otherDependency = ModDoctorReport(listOf(missingDependency("litematica", "fabric-api", candidate)))
        val nothing = ModDoctorReport(emptyList())

        assertEquals(DependencyRepairSelection.NotReported, selectDependencyRepair(otherRequirer, litematicaEvidence))
        assertEquals(DependencyRepairSelection.NotReported, selectDependencyRepair(otherDependency, litematicaEvidence))
        assertEquals(DependencyRepairSelection.NotReported, selectDependencyRepair(nothing, litematicaEvidence))
    }

    @Test
    fun theFixIsRestrictedToTheSelectedProblemOnly() {
        val candidate = candidate("malilib", versionName = "0.27.20")
        val selected = missingDependency("litematica", "malilib", candidate)
        val unrelated = missingDependency("sodium", "iris", candidate)
        val report = ModDoctorReport(listOf(unrelated, selected), advisories = listOf(unrelated))

        val restricted = report.restrictedTo(selected)

        assertEquals(listOf(selected), restricted.problems)
        assertTrue("advisories are not part of this fix", restricted.advisories.isEmpty())
    }

    // ---- 修复之后：需求是否仍被报告 ----

    @Test
    fun aCleanReportResolvesTheRequirement() {
        assertTrue(dependencyRequirementResolved(ModDoctorReport(emptyList()), litematicaEvidence))
    }

    @Test
    fun aStillMissingDependencyDoesNotResolveTheRequirement() {
        val report = ModDoctorReport(listOf(missingDependency("litematica", "malilib", candidate("malilib", "0.27.20"))))

        assertFalse(dependencyRequirementResolved(report, litematicaEvidence))
    }

    @Test
    fun aVersionMismatchForTheRequirerDoesNotResolveTheRequirement() {
        //已装但版本不满足声明：加载器同样会拒绝启动，因此不能算解决
        val mismatch = ModDoctorProblem(
            type = ModDoctorProblemType.INCOMPATIBLE_DEPENDENCY_VERSION,
            fileName = "litematica-fabric-1.21.11-0.26.16.jar",
            modId = "litematica",
            reason = "version mismatch",
        )

        assertFalse(dependencyRequirementResolved(ModDoctorReport(listOf(mismatch)), litematicaEvidence))
    }

    // ---- 学习记录不得把「禁用它就能启动」当作缺失依赖的建议 ----

    @Test
    fun missingDependencyRecordsAreNeverOfferedAsLearnedDisableWarnings() {
        val missing = learningRecord(diagnosisType = DIAGNOSIS_CLASS_MISSING_DEPENDENCY)
        val platform = learningRecord(diagnosisType = "platform_limit")

        val warnings = learnedWarningCandidates(
            records = listOf(missing, platform),
            instanceName = "QA-V3-1",
            minecraftVersion = "1.21.11",
            loader = ModLoader.FABRIC,
        )

        assertEquals("only the non-dependency record may still warn", listOf(platform), warnings)
    }

    private fun candidate(dependencyId: String, versionName: String) = ModDoctorFixCandidate(
        dependencyId = dependencyId,
        projectId = "project-$dependencyId",
        platform = Platform.MODRINTH,
        versionName = versionName,
    )

    private fun missingDependency(
        requirerId: String,
        dependencyId: String,
        fixCandidate: ModDoctorFixCandidate?,
        reason: String = "$requirerId requires $dependencyId, but no enabled installation was found.",
    ) = ModDoctorProblem(
        type = ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY,
        fileName = "$requirerId.jar",
        modId = requirerId,
        reason = reason,
        requiredDependencyId = dependencyId,
        fixCandidate = fixCandidate,
    )

    private fun learningRecord(diagnosisType: String) = LearningRecord(
        instanceName = "QA-V3-1",
        logPath = null,
        minecraftVersion = "1.21.11",
        loader = "FABRIC",
        modSetFingerprint = "litematica@0.26.16",
        signatureId = "signature-$diagnosisType",
        outcomeClass = "FAILED_DURING_STARTUP",
        ownerModId = "litematica",
        diagnosisType = diagnosisType,
        confidence = "HIGH",
        action = "disable",
        actionPreState = "litematica-fabric-1.21.11-0.26.16.jar",
        actionLogStamp = "stamp",
        markerSeenAfterFix = true,
        crashMarkerSeenAfterFix = false,
        count = 2,
        state = LearningState.CONFIRMED_LOCAL,
        inconclusive = false,
    )
}
