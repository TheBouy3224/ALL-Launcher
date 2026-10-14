package com.movtery.zalithlauncher.game.version.mod

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/**
 * Crash Doctor: Fabric "missing dependency" 崩溃的归因（真实日志的同一句话）。
 * 只测判定；动作（预览 → 快照 → 禁用）在 ErrorActivity 里，靠真机 E2E 验证。
 */
class CrashMissingDependencyTest {
    private val signatures = parseCrashSignatures(File("src/main/assets/crash_signatures.json").readText())

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

    @Test
    fun realFabricMissingDependencyIsAttributedToTheRequiringMod() {
        val outcome = classifyCapturedLog(realLog, exitCode = 1, signatures = signatures)

        val finding = outcome.finding
        assertEquals("missing_required_dependency_fabric", finding?.signatureId)
        assertEquals(DiagnosisConfidence.HIGH, outcome.confidence)
        assertEquals("litematica", finding?.ownerModId)
        assertEquals(DiagnosisAction.DISABLE, finding?.action)
        assertEquals("crash_diagnosis_missing_dependency", finding?.messageKey)
        assertEquals(LaunchOutcome.FAILED_DURING_STARTUP, outcome.launchOutcome)
    }

    @Test
    fun breaksOnlyLoaderErrorIsNotGivenTheDependencyAction() {
        //同样是 Incompatible mods found!，但没有 "which is missing!"：不是缺依赖，不得取得禁用动作
        val breaksLog = """
            [02:14:19] [main/ERROR]: Incompatible mods found!
            	 - Mod 'Sodium' (sodium) 0.8.14 breaks iris 1.10.7
        """.trimIndent()

        val finding = classifyCapturedLog(breaksLog, exitCode = 1, signatures = signatures).finding

        assertEquals("loader_fatal_unattributed", finding?.signatureId)
        assertEquals(DiagnosisAction.NONE, finding?.action)
        assertNull(finding?.ownerModId)
    }

    @Test
    fun missingLineWithoutAParsableRequirerHasNoOwnerSoNoAction() {
        //缺依赖的句子存在，但点不出是哪个模组要求的：只诊断，不指认任何模组，因此不会给出禁用动作
        val vagueLog = """
            [02:14:19] [main/ERROR]: Incompatible mods found!
            	 - something requires a thing, which is missing!
        """.trimIndent()

        val finding = classifyCapturedLog(vagueLog, exitCode = 1, signatures = signatures).finding

        assertEquals("missing_required_dependency_fabric", finding?.signatureId)
        assertNull(finding?.ownerModId)
    }

    @Test
    fun theBundledFileKeepsTheMissingDependencyEntryAheadOfTheGenericOne() {
        //通用的 loader_fatal 必须排在后面，否则它会先命中并抹掉原因
        val ids = signatures.map { it.id }
        org.junit.Assert.assertTrue(ids.indexOf("missing_required_dependency_fabric") < ids.indexOf("loader_fatal_unattributed"))
    }
}
