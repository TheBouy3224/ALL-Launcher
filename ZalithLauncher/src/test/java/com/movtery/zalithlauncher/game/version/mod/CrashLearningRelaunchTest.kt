package com.movtery.zalithlauncher.game.version.mod

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * 待续自动重启的时效判定，以及收尾在「日志刚被清空」时的行为。
 *
 * 两个判决都是纯函数：不碰 MMKV，也不碰设备。真机那次重启之后日志会被立刻重建，
 * 收尾要是把「标记还没写下来」读成「修复失败」，升级窗口就被毁掉了。
 */
class CrashLearningRelaunchTest {
    /** 一个固定的“现在”，免得用例依赖真实时钟 */
    private val now = 1_790_265_461_000L

    @Test
    fun aFreshRelaunchIsDue() {
        assertTrue(relaunchIsDue(now - 1_000L, now))
        assertTrue(relaunchIsDue(now, now))
    }

    /** 上限本身仍然有效：边界含在内 */
    @Test
    fun theAgeBoundaryIsInclusive() {
        assertTrue(relaunchIsDue(now - RELAUNCH_MAX_AGE_MILLIS, now))
        assertFalse(relaunchIsDue(now - RELAUNCH_MAX_AGE_MILLIS - 1L, now))
    }

    @Test
    fun aZeroOrFutureValueIsNeverFired() {
        assertFalse("a zero value is not a timestamp", relaunchIsDue(0L, now))
        assertFalse("a future value must not fire", relaunchIsDue(now + 1L, now))
        assertFalse("a negative value must not fire", relaunchIsDue(-1L, now))
    }

    // ---- 收尾：一份刚被重建出来的日志里什么都还没写 ----

    /** 生产路径上那份被登记的待验证记录 */
    private fun pendingRecord(logPath: String, stamp: String): LearningRecord {
        val match = LearningMatch(
            instanceName = "1.21.11 Fabric 0.19.5",
            logPath = logPath,
            minecraftVersion = "1.21.11",
            loader = "Fabric",
            modSetFingerprint = "axiom@6.0.5,sodium@0.8.12",
            signatureId = "platform_limit_native",
            diagnosisType = "platform_limit",
            outcomeClass = LaunchOutcome.CRASHED_UNEXPECTEDLY.name,
            ownerModId = "axiom",
            confidence = DiagnosisConfidence.HIGH,
            action = DiagnosisAction.DISABLE,
        )
        val first = mergeLearningMatch(null, match)!!
        val second = mergeLearningMatch(first, match)!!
        return withActionTaken(second, "Axiom-6.0.5-for-MC1.21.11.jar", stamp)
    }

    /** 逐字取自真机日志的启动器横幅；两种标记都不在里面 */
    private val bannerOnlyLine = "▷ Minecraft Info: 1.21.11, Fabric - 0.19.5"

    /** 逐字取自真机日志的游戏崩溃标记 */
    private val crashMarkerLine = "#@!@# Game crashed! Crash report saved to: #@!@# /storage/emulated/0/Logs/crash.txt"

    /**
     * 自动重启刚把日志重建出来：只有启动器横幅。这时**什么都不下结论**，
     * 也绝不能把 markerSeenAfterFix 写成 false——待办留给下一次启动。
     */
    @Test
    fun aJustTruncatedLogConcludesNothing() {
        val dir = Files.createTempDirectory("crash-truncated-log").toFile()
        try {
            val logFile = File(dir, "latest_game.log")
            logFile.writeText(bannerOnlyLine)
            val record = pendingRecord(logFile.absolutePath, "1:1")

            assertNull(
                "neither marker is written yet, so this startup says nothing",
                CrashLearning.resolvePendingOutcome(record),
            )
            assertFalse("and nothing may be recorded as a failed fix", record.markerSeenAfterFix)
        } finally {
            dir.deleteRecursively()
        }
    }

    /** 同一份新日志里只要有崩溃标记，就仍然是结论：这条守卫不能把失败的修复漏报成「还没写」 */
    @Test
    fun aRewrittenLogWithOnlyTheCrashMarkerStillConcludes() {
        val dir = Files.createTempDirectory("crash-marker-only").toFile()
        try {
            val logFile = File(dir, "latest_game.log")
            logFile.writeText(crashMarkerLine)
            val record = pendingRecord(logFile.absolutePath, "1:1")

            val resolved = CrashLearning.resolvePendingOutcome(record)

            assertNotNull(resolved)
            assertTrue("the crash marker is in it", resolved!!.crashMarkerSeenAfterFix)
            assertFalse(resolved.markerSeenAfterFix)
            assertEquals("a crash after the fix must not promote", LearningState.CANDIDATE, resolved.state)
        } finally {
            dir.deleteRecursively()
        }
    }
}
