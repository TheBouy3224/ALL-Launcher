package com.movtery.zalithlauncher.game.version.mod

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Crash Doctor 的安全底线，用真机上抓到的真实加载器日志（QA-V3-1，STEP 3）和全部内置特征来固定：
 * - 已知原因（缺失依赖）：HIGH，点名模组，证据完整，才可能给出动作
 * - 没有安全修复的失败（依赖版本不对）：LOW，不点名任何模组，没有任何动作——绝不靠猜去禁用
 * - 只有“高把握 + 能点名模组”的特征才允许 DISABLE
 */
class CrashDoctorSafetyTest {
    private val signatures = parseCrashSignatures(File("src/main/assets/crash_signatures.json").readText())

    private val missingDependencyLog = """
        [09:51:09] [main/ERROR]: Incompatible mods found!
        	 - Mod 'Litematica' (litematica) 0.26.16 requires any version between 0.27.19- (inclusive) and 0.28.0- (exclusive) of malilib, which is missing!
        Reason: [HARD_DEP litematica 0.26.16 {depends malilib @ [>=0.27.19- <0.28.0-]}]
    """.trimIndent()

    private val wrongVersionLog = """
        [09:59:55] [main/ERROR]: Incompatible mods found!
        	 - Mod 'YetAnotherConfigLib' (yet_another_config_lib_v3) 3.8.2+1.21.11-fabric requires version 0.140.0+1.21.11 or later of mod 'Fabric API' (fabric-api), but only the wrong version is present: 0.139.5+1.21.11!
    """.trimIndent()

    @Test
    fun aRealMissingDependencyLogIsHighConfidenceAndNamesTheRequirer() {
        val outcome = classifyCapturedLog(missingDependencyLog, exitCode = 1, signatures = signatures)

        assertEquals(LaunchOutcome.FAILED_DURING_STARTUP, outcome.launchOutcome)
        assertEquals(DIAGNOSIS_CLASS_MISSING_DEPENDENCY, outcome.finding?.diagnosisType)
        assertEquals(DiagnosisConfidence.HIGH, outcome.confidence)
        assertEquals("litematica", outcome.finding?.ownerModId)
    }

    @Test
    fun aRealMissingDependencyLogYieldsCompleteEvidenceForTheRepairPreview() {
        val evidence = readMissingDependencyEvidence(missingDependencyLog)

        assertNotNull(evidence)
        assertEquals("litematica", evidence!!.requirerId)
        assertEquals("malilib", evidence.dependencyId)
        assertEquals(">=0.27.19- <0.28.0-", evidence.constraint)
        assertEquals(2, evidence.evidence.size)
    }

    @Test
    fun aRealWrongVersionLogNamesTheRequirerButNeverOffersADisableAction() {
        //Crash-First V1：Fabric 的「版本不对」原句现在有自己的特征（HIGH，点名声明依赖的模组），
        //但动作永远是 NONE：修复只走 Mod Doctor 的预览与快照，不会出现「临时禁用」
        val outcome = classifyCapturedLog(wrongVersionLog, exitCode = 1, signatures = signatures)

        assertEquals(LaunchOutcome.FAILED_DURING_STARTUP, outcome.launchOutcome)
        assertEquals("loader_wrong_version", outcome.finding?.diagnosisType)
        assertEquals(DiagnosisConfidence.HIGH, outcome.confidence)
        assertNotNull(outcome.finding?.ownerModId)
        assertEquals(DiagnosisAction.NONE, outcome.finding?.action)
    }

    @Test
    fun theWrongVersionLogIsNotMistakenForAMissingDependency() {
        assertNull(readMissingDependencyEvidence(wrongVersionLog))
    }

    @Test
    fun onlyHighConfidenceSignaturesThatCanNameAModMayDisableAnything() {
        for (signature in signatures) {
            if (signature.action == DiagnosisAction.DISABLE) {
                assertEquals("${signature.id}: a disabling signature must be HIGH confidence", DiagnosisConfidence.HIGH, signature.confidence)
                assertNotNull("${signature.id}: a disabling signature must be able to name the owner mod", signature.owner)
            }
        }
    }

    @Test
    fun mixinAndUnattributedLoaderFailuresNeverDisableAnything() {
        val mixin = signatures.single { it.className == "mixin_failure" }
        val fatal = signatures.single { it.className == "loader_fatal" }

        assertEquals(DiagnosisAction.NONE, mixin.action)
        assertEquals(DiagnosisAction.NONE, fatal.action)
        assertTrue(mixin.confidence != DiagnosisConfidence.HIGH)
    }

    @Test
    fun aLoaderFatalSignatureWithoutAnOwnerNeverProducesAnAction() {
        val text = "Incompatible mods found!\nsomething we cannot attribute"
        val outcome = classifyCapturedLog(text, exitCode = 1, signatures = signatures)

        val finding = outcome.finding
        assertFalse(finding != null && finding.action == DiagnosisAction.DISABLE && finding.ownerModId == null)
    }
}
