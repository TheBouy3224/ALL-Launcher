package com.movtery.zalithlauncher.game.version.mod

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 本地学习记录的状态机回归：候选、以及「两个条件同时成立才升级」。
 *
 * 这里的每个判决都是纯函数——升级只用应用自己能观测到的信号（本机日志里的启动标记与崩溃标记），
 * 不需要设备，也不需要网络。
 *
 * 最后两组用例走的是**冷启动**路径：2026-09-24 的真机运行时，启动器主进程被当作缓存进程回收，
 * 崩溃页面在一个新进程里起来，那时 `VersionsManager` 是空的、动作按钮因此从来没被组合出来。
 * 这两组用例只给「崩溃日志的路径 + 日志文本」，不会也不允许有任何加载好的版本注册表。
 */
class CrashLearningTest {
    /** 真机上那份捕获日志的路径形状，逐字取自 2026-09-24 真机运行 */
    private val deviceLogPath =
        "/storage/emulated/0/Android/data/com.all.launcher.app.debug/files/.minecraft/versions/" +
            "1.21.11 Fabric 0.19.5/ALLLauncher/latest_game.log"

    private val signatures = parseCrashSignatures(
        File("src/main/assets/crash_signatures.json").readText()
    )

    private fun match(
        confidence: DiagnosisConfidence = DiagnosisConfidence.HIGH,
        fingerprint: String = "axiom@4.9.0,sodium@0.8.12",
        logPath: String = deviceLogPath,
    ) = LearningMatch(
        instanceName = "1.21.11 Fabric 0.19.5",
        logPath = logPath,
        minecraftVersion = "1.21.11",
        loader = "Fabric",
        modSetFingerprint = fingerprint,
        signatureId = "platform_limit_native",
        diagnosisType = "platform_limit",
        outcomeClass = LaunchOutcome.CRASHED_UNEXPECTEDLY.name,
        ownerModId = "axiom",
        confidence = confidence,
        action = DiagnosisAction.DISABLE,
    )

    private fun acted(record: LearningRecord) = withActionTaken(record, "axiom-4.9.0.jar", "53465:1")

    @Test
    fun aSingleMatchStaysCandidate() {
        val record = mergeLearningMatch(null, match())!!

        assertEquals(LearningState.CANDIDATE, record.state)
        assertEquals(1, record.count)
        assertFalse(record.inconclusive)
        assertFalse(record.markerSeenAfterFix)
        assertTrue("no action has touched a file yet", record.actionPreState.isEmpty())
    }

    @Test
    fun aLowConfidenceFindingIsRefused() {
        assertNull(mergeLearningMatch(null, match(confidence = DiagnosisConfidence.LOW)))
    }

    @Test
    fun aRuleWithoutAModSetFingerprintIsRefused() {
        assertNull(mergeLearningMatch(null, match(fingerprint = "")))
    }

    @Test
    fun promotionNeedsBothASecondMatchAndAnObservedRecovery() {
        val first = mergeLearningMatch(null, match())!!
        val second = mergeLearningMatch(first, match())!!
        assertEquals(2, second.count)

        //只有第一个条件（同一指纹上第二次命中）：没有动作、没有日志证据，还是候选
        val noAction = applyActionOutcome(second, logRewritten = true, markerSeen = true, crashMarkerSeen = false)
        assertEquals(LearningState.CANDIDATE, noAction.state)

        //只有第二个条件（动作 + 日志证据）：只命中过一次，还是候选
        val onceAndActed = applyActionOutcome(acted(first), logRewritten = true, markerSeen = true, crashMarkerSeen = false)
        assertEquals(LearningState.CANDIDATE, onceAndActed.state)

        //两个条件同时成立，才升级为本地已确认
        val promoted = applyActionOutcome(acted(second), logRewritten = true, markerSeen = true, crashMarkerSeen = false)
        assertEquals(LearningState.CONFIRMED_LOCAL, promoted.state)
        assertTrue(promoted.markerSeenAfterFix)
        assertFalse(promoted.crashMarkerSeenAfterFix)
    }

    @Test
    fun anObservedCrashAfterTheFixDoesNotPromote() {
        val second = mergeLearningMatch(mergeLearningMatch(null, match())!!, match())!!
        val action = acted(second)

        assertEquals(LearningState.CANDIDATE, applyActionOutcome(action, true, markerSeen = false, crashMarkerSeen = true).state)
        assertEquals(LearningState.CANDIDATE, applyActionOutcome(action, true, markerSeen = true, crashMarkerSeen = true).state)
    }

    @Test
    fun aLogThatWasNotRewrittenConcludesNothing() {
        val second = mergeLearningMatch(mergeLearningMatch(null, match())!!, match())!!
        val action = acted(second)

        //日志还是崩溃那一次留下的：既不能算成功，也不能算失败
        val untouched = applyActionOutcome(action, logRewritten = false, markerSeen = false, crashMarkerSeen = true)

        assertEquals(LearningState.CANDIDATE, untouched.state)
        assertFalse("an unread log may not be recorded as a crash after the fix", untouched.crashMarkerSeenAfterFix)
    }

    @Test
    fun threeMatchesWithoutPromotionAreInconclusiveForever() {
        var record = mergeLearningMatch(null, match())!!
        record = mergeLearningMatch(record, match())!!
        record = mergeLearningMatch(record, match())!!

        assertEquals(3, record.count)
        assertTrue(record.inconclusive)

        //之后就算两个条件都凑齐，也不再升级
        val afterEvidence = applyActionOutcome(acted(record), true, markerSeen = true, crashMarkerSeen = false)
        assertEquals(LearningState.CANDIDATE, afterEvidence.state)
        assertTrue(afterEvidence.inconclusive)

        val fourth = mergeLearningMatch(afterEvidence, match())!!
        assertEquals(4, fourth.count)
        assertEquals(LearningState.CANDIDATE, fourth.state)
        assertTrue(fourth.inconclusive)
    }

    @Test
    fun aConfirmedRecordStaysConfirmed() {
        val second = mergeLearningMatch(mergeLearningMatch(null, match())!!, match())!!
        val promoted = applyActionOutcome(acted(second), true, markerSeen = true, crashMarkerSeen = false)

        val again = mergeLearningMatch(promoted, match())!!

        assertEquals(LearningState.CONFIRMED_LOCAL, again.state)
        assertEquals(3, again.count)
        assertFalse(again.inconclusive)
    }

    @Test
    fun aDifferentModSetIsADifferentRule() {
        val first = mergeLearningMatch(null, match())!!

        val otherSet = mergeLearningMatch(null, match(fingerprint = "axiom@4.9.1,sodium@0.8.12"))!!

        assertEquals(1, otherSet.count)
        assertEquals(1, first.count)
        assertTrue(learningRuleKey("platform_limit_native", "a") != learningRuleKey("platform_limit_native", "b"))
    }

    // ---- 崩溃页面冷启动：只有日志路径和日志文本 ----

    /**
     * 真机那次崩溃的日志形状，逐字：启动器横幅、加载器启动行、入口点失败的两行。
     * 版本、加载器、原因三样都在同一份日志里，所以冷启动的崩溃页面什么都不缺。
     */
    private val coldStartLogText = listOf(
        "▷ Minecraft Info: 1.21.11, Fabric - 0.19.5",
        "[15:06:09] [main/INFO]: Loading Minecraft 1.21.11 with Fabric Loader 0.19.5",
        "java.lang.RuntimeException: Could not execute entrypoint stage 'main' due to errors, provided by 'axiom' at 'com.moulberry.axiom.Axiom'!",
        "Caused by: java.lang.UnsatisfiedLinkError: dlopen failed: library \"libstdc++.so.6\" not found",
    ).joinToString("\n")

    /** 真机的一行：模组为另一个 Minecraft 版本构建时的 mixin 失败（MEDIUM，不给任何动作） */
    private val mixinFailureLine =
        "Caused by: org.spongepowered.asm.mixin.throwables.MixinApplyError: Mixin [balm.mixins.json:Accessor from mod balm] from phase [DEFAULT] in config [balm.mixins.json] FAILED during APPLY"

    /** 逐字取自同一份日志：加载器写下的启动成功标记 */
    private val launchMarkerLine = "[18:44:00] [Render thread/INFO]: Sound engine started"

    /** 逐字取自同一份日志：游戏自己写下的崩溃标记 */
    private val crashMarkerLine =
        "#@!@# Game crashed! Crash report saved to: #@!@# /storage/emulated/0/Android/data/" +
            "com.all.launcher.app.debug/files/.minecraft/versions/1.21.11 Fabric 0.19.5/" +
            "crash-reports/crash-2026-09-24_22.09.40-client.txt"

    private fun writeModJar(modsDir: File, fileName: String, id: String, version: String) {
        ZipOutputStream(File(modsDir, fileName).outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("fabric.mod.json"))
            zip.write(
                ("{\"schemaVersion\":1,\"id\":\"$id\",\"version\":\"$version\",\"name\":\"$id\"," +
                    "\"depends\":{\"minecraft\":\"*\"}}").toByteArray(Charsets.UTF_8)
            )
            zip.closeEntry()
        }
    }

    /**
     * 动作与模组集指纹都必须只从崩溃日志的路径和文本算出来。
     *
     * 这里没有任何加载好的版本注册表：实例名从路径上取，版本与加载器从日志文本里取，
     * 模组目录从路径推出来（`<实例目录>/mods`）。
     */
    @Test
    fun theActionAndTheFingerprintComeFromTheLogPathAndTextAlone() = runBlocking {
        val root = Files.createTempDirectory("crash-cold-start").toFile()
        try {
            val instanceDir = File(root, "1.21.11 Fabric 0.19.5")
            val modsDir = File(instanceDir, "mods").apply { mkdirs() }
            writeModJar(modsDir, "axiom-4.9.0.jar", "axiom", "4.9.0")
            writeModJar(modsDir, "sodium-0.8.12.jar", "sodium", "0.8.12")
            val logFile = File(File(instanceDir, "ALLLauncher").apply { mkdirs() }, "latest_game.log")
            logFile.writeText(coldStartLogText)

            val outcome = classifyCapturedLog(coldStartLogText, exitCode = 1, signatures = signatures)
            val decision = resolveCrashDecision(logFile, coldStartLogText, outcome)

            assertNotNull("a HIGH finding naming a mod that is in the directory must give an action", decision)
            assertEquals("axiom", decision?.fix?.modId)
            assertEquals(modsDir, decision?.fix?.modsDir)
            assertEquals("axiom@4.9.0,sodium@0.8.12", decision?.match?.modSetFingerprint)
            assertEquals("1.21.11 Fabric 0.19.5", decision?.match?.instanceName)
            assertEquals("1.21.11", decision?.match?.minecraftVersion)
            assertEquals("Fabric", decision?.match?.loader)
            assertEquals(logFile.absolutePath, decision?.match?.logPath)
        } finally {
            root.deleteRecursively()
        }
    }

    /** 日志点名了模组，但那个文件已经不在目录里：诊断照记，动作不给（没有文件可动） */
    @Test
    fun aNamedModThatIsNotInTheDirectoryGivesNoActionButStillADiagnosis() = runBlocking {
        val root = Files.createTempDirectory("crash-owner-missing").toFile()
        try {
            val instanceDir = File(root, "1.21.11 Fabric 0.19.5")
            val modsDir = File(instanceDir, "mods").apply { mkdirs() }
            writeModJar(modsDir, "sodium-0.8.12.jar", "sodium", "0.8.12")
            val logFile = File(File(instanceDir, "ALLLauncher").apply { mkdirs() }, "latest_game.log")
            logFile.writeText(coldStartLogText)

            val outcome = classifyCapturedLog(coldStartLogText, exitCode = 1, signatures = signatures)
            val decision = resolveCrashDecision(logFile, coldStartLogText, outcome)

            assertNull("nothing to disable, so no action", decision?.fix)
            assertNotNull("the diagnosis itself is still recorded", decision?.match)
            assertEquals("sodium@0.8.12", decision?.match?.modSetFingerprint)
        } finally {
            root.deleteRecursively()
        }
    }

    /** 实例目录推不出来（路径上根本没有那一层）：什么都不记、也不给动作 */
    @Test
    fun aLogPathWithoutADerivableModsDirectoryDecidesNothing() = runBlocking {
        val root = Files.createTempDirectory("crash-no-mods").toFile()
        try {
            val logFile = File(root, "latest_game.log")
            logFile.writeText(coldStartLogText)

            val outcome = classifyCapturedLog(coldStartLogText, exitCode = 1, signatures = signatures)

            assertNull(resolveCrashDecision(logFile, coldStartLogText, outcome))
        } finally {
            root.deleteRecursively()
        }
    }

    /** MEDIUM 的结论仍然只诊断、不动文件：它要动文件必须先有一次用户确认，而 V1 不做这一步 */
    @Test
    fun aMediumConfidenceFindingStillGivesNoAction() = runBlocking {
        val root = Files.createTempDirectory("crash-medium").toFile()
        try {
            val instanceDir = File(root, "1.21.11 Fabric 0.19.5")
            val modsDir = File(instanceDir, "mods").apply { mkdirs() }
            writeModJar(modsDir, "balm-21.10.11.jar", "balm", "21.10.11")
            val logFile = File(File(instanceDir, "ALLLauncher").apply { mkdirs() }, "latest_game.log")
            logFile.writeText(mixinFailureLine)

            val outcome = classifyCapturedLog(mixinFailureLine, exitCode = 1, signatures = signatures)
            val decision = resolveCrashDecision(logFile, mixinFailureLine, outcome)

            assertEquals(DiagnosisConfidence.MEDIUM, outcome.confidence)
            assertNull("MEDIUM never disables anything", decision?.fix)
            assertNotNull("but the diagnosis is still recorded", decision?.match)
        } finally {
            root.deleteRecursively()
        }
    }

    // ---- 收尾：只读记录自己带的日志路径 ----

    /** 一次已经登记过动作、等着用「下一次启动的日志」收尾的记录 */
    private fun pendingRecord(logPath: String, stamp: String): LearningRecord {
        val first = mergeLearningMatch(null, match(logPath = logPath))!!
        val second = mergeLearningMatch(first, match(logPath = logPath))!!
        return withActionTaken(second, "axiom-4.9.0.jar", stamp)
    }

    /**
     * 收尾只看记录**自己带的**日志路径指向的那份日志：它被重写过（说明用户又启动过一次），
     * 而且里面还写着崩溃标记时，这次修复就是失败的——不能升级。
     */
    @Test
    fun thePendingCheckResolvesFromTheLogPathStoredInTheRecord() {
        val dir = Files.createTempDirectory("crash-pending").toFile()
        try {
            val logFile = File(dir, "latest_game.log")
            logFile.writeText(listOf(launchMarkerLine, crashMarkerLine).joinToString("\n"))
            val record = pendingRecord(logFile.absolutePath, "1:1")

            val resolved = CrashLearning.resolvePendingOutcome(record)

            assertNotNull("the stored log was rewritten, so this startup does say something", resolved)
            assertTrue("the launch marker is in it", resolved!!.markerSeenAfterFix)
            assertTrue("and so is the crash marker", resolved.crashMarkerSeenAfterFix)
            assertEquals("a crash after the fix must not promote", LearningState.CANDIDATE, resolved.state)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun aRecoveredStartupPromotesTheRecordFoundThroughItsOwnLogPath() {
        val dir = Files.createTempDirectory("crash-pending-promote").toFile()
        try {
            val logFile = File(dir, "latest_game.log")
            logFile.writeText(launchMarkerLine)
            val record = pendingRecord(logFile.absolutePath, "1:1")

            val resolved = CrashLearning.resolvePendingOutcome(record)!!

            assertEquals(LearningState.CONFIRMED_LOCAL, resolved.state)
            assertTrue(resolved.markerSeenAfterFix)
            assertFalse(resolved.crashMarkerSeenAfterFix)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun aStoredLogThatWasNotRewrittenStillConcludesNothing() {
        val dir = Files.createTempDirectory("crash-pending-same-log").toFile()
        try {
            val logFile = File(dir, "latest_game.log")
            logFile.writeText(launchMarkerLine)
            //指纹与登记动作时一模一样：这份日志还是崩溃那一次留下的
            val record = pendingRecord(logFile.absolutePath, capturedLogStamp(logFile))

            assertNull(CrashLearning.resolvePendingOutcome(record))
        } finally {
            dir.deleteRecursively()
        }
    }

    /** 旧版本写下的记录没有 logPath 字段：读不到日志，就什么都不下结论 */
    @Test
    fun aRecordWithoutAStoredLogPathIsNeverConcluded() {
        val dir = Files.createTempDirectory("crash-pending-old-record").toFile()
        try {
            val logFile = File(dir, "latest_game.log")
            logFile.writeText(launchMarkerLine)
            val record = pendingRecord(logFile.absolutePath, "1:1").copy(logPath = null)

            assertNull(CrashLearning.resolvePendingOutcome(record))
        } finally {
            dir.deleteRecursively()
        }
    }
}
