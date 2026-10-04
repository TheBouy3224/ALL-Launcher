package com.movtery.zalithlauncher.game.version.mod

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 崩溃判定的决策表回归。
 *
 * 每个片段都是从真实捕获日志里**逐字**抄下来的，出处写在各自的注释里；没有真实日志证据的签名
 * 一个都不在这里。判定本身是纯函数，所以这些回归不需要设备。
 */
class CrashDiagnosisTest {
    private val signatures = parseCrashSignatures(
        File("src/main/assets/crash_signatures.json").readText()
    )

    /**
     * 真实片段，逐字取自
     * `D:\ALL Mod Doctor Warehouse\crash-lab\evidence\1.21.11-fabric-v2-conflict-axiom\raw\a-only-game.log`
     * 第 506 行与第 516 行（一次真实启动失败：入口点抛出 UnsatisfiedLinkError）。
     */
    private val platformLimitExcerpt = listOf(
        "java.lang.RuntimeException: Could not execute entrypoint stage 'main' due to errors, provided by 'axiom' at 'com.moulberry.axiom.Axiom'!",
        "Caused by: java.lang.UnsatisfiedLinkError: /data/data/com.all.launcher.app.debug/cache/imgui-moulberry92-java-natives/1.92-moulberry.2/libimgui-moulberry92-java64.so: dlopen failed: library \"libstdc++.so.6\" not found",
    ).joinToString("\n")

    /** 同一个文件的第 314 行：另一种缺失的本地库，同样是一条真实证据 */
    private val platformLimitLibcExcerpt = listOf(
        "java.lang.RuntimeException: Could not execute entrypoint stage 'main' due to errors, provided by 'axiom' at 'com.moulberry.axiom.Axiom'!",
        "java.lang.UnsatisfiedLinkError: /data/data/com.all.launcher.app.debug/cache/jna10255532882083346123.tmp: dlopen failed: library \"libc.so.6\" not found",
    ).joinToString("\n")

    /** 同一文件的第 648 行：游戏自己的崩溃标记 */
    private val crashMarker =
        "#@!@# Game crashed! Crash report saved to: #@!@# /storage/emulated/0/Android/data/com.all.launcher.app.debug/files/.minecraft/versions/1.21.11 Fabric 0.19.5/crash-reports/crash-2026-09-24_15.06.26-client.txt"

    /**
     * 真实片段，逐字取自 `D:\ALL001\test-artifacts\release-moddoctor-wrong-version-e2e\13-game-latest.log`
     * 第 347 行（模组为另一个 Minecraft 版本构建时的 mixin 失败）。
     */
    private val mixinExcerpt =
        "Caused by: org.spongepowered.asm.mixin.throwables.MixinApplyError: Mixin [balm.mixins.json:AbstractContainerScreenAccessor from mod balm] from phase [DEFAULT] in config [balm.mixins.json] FAILED during APPLY"

    /**
     * 加载器的**警告**，逐字取自 `test-artifacts/public-beta/fabric-fo-fixed-gamelog.txt` 第 257 行
     * 与 `test-artifacts/4mod-certification/runner/state/1.18.1__fabric.evidence.txt` 第 252 行。
     * 这两份日志都是一次成功的启动。
     */
    private val recommendsWarnings = listOf(
        " - 'Minecraft' (minecraft) 1.21.1 recommends any version after 6.5.0 of Fabulously Optimized, which is missing!",
        " - Mod 'Fabric Rendering Fluids (v1)' (fabric-rendering-fluids-v1) 0.1.43d95c8 recommends any version of fabric-textures-v0, which is missing!",
    ).joinToString("\n")

    /** 同一份成功日志的第 853 行：加载器写下的启动成功标记 */
    private val launchMarkerLine = "[18:44:00] [Render thread/INFO]: Sound engine started"

    @Test
    fun theFourOutcomes() {
        assertEquals(
            LaunchOutcome.NORMAL_EXIT_AFTER_MARKER,
            classifyCapturedLog(launchMarkerLine, exitCode = 0, signatures = signatures).launchOutcome,
        )
        assertEquals(
            LaunchOutcome.REACHED_MARKER,
            classifyCapturedLog(launchMarkerLine, exitCode = 1, signatures = signatures).launchOutcome,
        )
        assertEquals(
            LaunchOutcome.CRASHED_UNEXPECTEDLY,
            classifyCapturedLog("nothing else here\n$crashMarker", exitCode = 1, signatures = signatures).launchOutcome,
        )
        assertEquals(
            LaunchOutcome.FAILED_DURING_STARTUP,
            classifyCapturedLog("Incompatible mods found", exitCode = 1, signatures = signatures).launchOutcome,
        )
    }

    @Test
    fun theCrashMarkerTakesPrecedenceOverEverythingBelow() {
        //既有成功标记、退出码又是 0，只要游戏自己写了崩溃标记，结论就是崩溃
        val text = listOf(launchMarkerLine, crashMarker).joinToString("\n")
        val outcome = classifyCapturedLog(text, exitCode = 0, signatures = signatures)

        assertEquals(LaunchOutcome.CRASHED_UNEXPECTEDLY, outcome.launchOutcome)
    }

    @Test
    fun aNonZeroExitWithNothingElseIsAnUnclassifiedStartupFailure() {
        val outcome = classifyCapturedLog("some ordinary log text", exitCode = 1, signatures = signatures)

        assertEquals(LaunchOutcome.FAILED_DURING_STARTUP, outcome.launchOutcome)
        assertEquals(DiagnosisConfidence.LOW, outcome.confidence)
        assertNull("nothing was recognised, so nothing may be named", outcome.finding)
    }

    @Test
    fun anUnreadableLogIsNeverCalledACrash() {
        val outcome = classifyCapturedLog("", exitCode = 0, signatures = signatures)

        assertEquals(LaunchOutcome.FAILED_DURING_STARTUP, outcome.launchOutcome)
        assertEquals(DiagnosisConfidence.LOW, outcome.confidence)
    }

    @Test
    fun aRealPlatformLimitFailureNamesTheOwningMod() {
        val outcome = classifyCapturedLog(platformLimitExcerpt, exitCode = 1, signatures = signatures)

        assertEquals(LaunchOutcome.FAILED_DURING_STARTUP, outcome.launchOutcome)
        assertEquals("platform_limit_native", outcome.finding?.signatureId)
        assertEquals("platform_limit", outcome.finding?.diagnosisType)
        assertEquals(DiagnosisConfidence.HIGH, outcome.confidence)
        assertEquals(DiagnosisAction.DISABLE, outcome.finding?.action)
        assertEquals("axiom", outcome.finding?.ownerModId)
    }

    @Test
    fun theMissingLibcVariantMatchesToo() {
        val outcome = classifyCapturedLog(platformLimitLibcExcerpt, exitCode = 1, signatures = signatures)

        assertEquals("platform_limit_native", outcome.finding?.signatureId)
        assertEquals(DiagnosisConfidence.HIGH, outcome.confidence)
        assertEquals("axiom", outcome.finding?.ownerModId)
    }

    @Test
    fun aRealPlatformLimitCrashIsStillNamed() {
        //真实证据的完整形状：崩溃标记 + 特征行，两者都在同一份日志里
        val outcome = classifyCapturedLog(
            listOf(platformLimitExcerpt, crashMarker).joinToString("\n"),
            exitCode = 1,
            signatures = signatures,
        )

        assertEquals(LaunchOutcome.CRASHED_UNEXPECTEDLY, outcome.launchOutcome)
        assertEquals("platform_limit_native", outcome.finding?.signatureId)
        assertEquals("axiom", outcome.finding?.ownerModId)
    }

    @Test
    fun aRealMixinFailureIsMediumConfidenceAndNeverDisablesAnything() {
        val outcome = classifyCapturedLog(mixinExcerpt, exitCode = 1, signatures = signatures)

        assertEquals(LaunchOutcome.FAILED_DURING_STARTUP, outcome.launchOutcome)
        assertEquals("mixin_apply_failed", outcome.finding?.signatureId)
        assertEquals("mixin_failure", outcome.finding?.diagnosisType)
        assertEquals(DiagnosisConfidence.MEDIUM, outcome.confidence)
        assertEquals("balm", outcome.finding?.ownerModId)
        assertEquals(DiagnosisAction.NONE, outcome.finding?.action)
    }

    @Test
    fun aRecommendationWarningIsNotAFailure() {
        //这两行是加载器的警告，出现在成功的启动日志里，不能被当成失败的原因
        val outcome = classifyCapturedLog(
            listOf(recommendsWarnings, launchMarkerLine).joinToString("\n"),
            exitCode = 0,
            signatures = signatures,
        )

        assertEquals(LaunchOutcome.NORMAL_EXIT_AFTER_MARKER, outcome.launchOutcome)
        assertNull("a warning names no failing signature", outcome.finding)
    }

    @Test
    fun aRecommendationWarningAloneIsNeverACause() {
        val outcome = classifyCapturedLog(recommendsWarnings, exitCode = 0, signatures = signatures)

        assertTrue(
            "the warning must not be turned into a cause",
            outcome.launchOutcome != LaunchOutcome.CRASHED_UNEXPECTEDLY,
        )
        assertNull(outcome.finding)
    }

    @Test
    fun theLoaderFatalTokensAreRecognisedWithoutAModName() {
        for (token in listOf(
            "Mod resolution failed",
            "Incompatible mods found",
            "Missing or unsupported mandatory dependencies",
            "Incompatible mod set",
        )) {
            val outcome = classifyCapturedLog(token, exitCode = 1, signatures = signatures)

            assertEquals("$token must be recognised", "loader_fatal_unattributed", outcome.finding?.signatureId)
            assertEquals(DiagnosisConfidence.LOW, outcome.confidence)
            assertNull("nothing named a mod, so nothing may be named", outcome.finding?.ownerModId)
            assertEquals(DiagnosisAction.NONE, outcome.finding?.action)
        }
    }

    @Test
    fun aFileThatIsNotThereReadsAsNothing() {
        val missing = File("src/main/assets/this-file-does-not-exist.log")

        assertEquals("", readCapturedLogTail(missing))
        assertEquals("0:0", capturedLogStamp(missing))
    }

    /**
     * 加载器自己写下的那一行，逐字取自 2026-09-24 真机运行的捕获日志
     * （`D:\ALL Mod Doctor Warehouse\crash-lab\milestone-post-crash-doctor-v1\e2e\20-launch1-latest_game.log`
     * 第 250 行）。这一行同时给出 Minecraft 版本和加载器。
     */
    private val loaderLine = "[15:06:09] [main/INFO]: Loading Minecraft 1.21.11 with Fabric Loader 0.19.5"

    /** 同一份日志第 8 行：启动器自己的横幅，逐字 */
    private val launcherInfoLine = "▷ Minecraft Info: 1.21.11, Fabric - 0.19.5"

    @Test
    fun theLoaderLineNamesTheMinecraftVersionAndTheLoader() {
        val identity = readLaunchIdentity(loaderLine)

        assertEquals("1.21.11", identity.minecraftVersion)
        assertEquals("Fabric", identity.loader)
    }

    @Test
    fun theLauncherBannerIsTheSecondSourceWhenTheLoaderLineIsMissing() {
        val identity = readLaunchIdentity(launcherInfoLine)

        assertEquals("1.21.11", identity.minecraftVersion)
        assertEquals("Fabric", identity.loader)
    }

    @Test
    fun aLogThatNamesNeitherRecordsUnknownInsteadOfFailing() {
        assertEquals(LaunchIdentity(), readLaunchIdentity("some ordinary log text"))
    }

    @Test
    fun theInstanceDirectoryComesFromTheLogPathAlone() {
        val versionsHome = File("/storage/emulated/0/Android/data/com.all.launcher.app.debug/files/.minecraft/versions")
        val instanceDir = File(versionsHome, "1.21.11 Fabric 0.19.5")
        val logFile = File(File(instanceDir, "ALLLauncher"), "latest_game.log")

        assertEquals(instanceDir, crashInstanceDir(logFile))
    }

    @Test
    fun aPathTooShortToBeALogPathNamesNoInstance() {
        assertNull(crashInstanceDir(File("latest_game.log")))
    }
}
