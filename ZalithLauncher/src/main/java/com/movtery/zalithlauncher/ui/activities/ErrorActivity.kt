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
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import com.movtery.zalithlauncher.all.overlays.AllAlertDialog
import com.movtery.zalithlauncher.all.overlays.AllRepairHost
import com.movtery.zalithlauncher.all.overlays.AllRepairResultKind
import com.movtery.zalithlauncher.all.overlays.AllRepairUiState
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.jakewharton.processphoenix.ProcessPhoenix
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.context.COPY_LABEL_LINK
import com.movtery.zalithlauncher.game.version.mod.AllModReader
import com.movtery.zalithlauncher.game.version.mod.CrashDecision
import com.movtery.zalithlauncher.game.version.mod.CrashLearning
import com.movtery.zalithlauncher.game.version.mod.CrashOutcome
import com.movtery.zalithlauncher.game.version.mod.SnapshotDisableResult
import com.movtery.zalithlauncher.game.version.mod.disableModWithSnapshot
import com.movtery.zalithlauncher.game.version.mod.capturedLogStamp
import com.movtery.zalithlauncher.game.version.mod.classifyCapturedLog
import com.movtery.zalithlauncher.game.version.mod.diagnosisMessageRes
import com.movtery.zalithlauncher.game.version.mod.disableModFile
import com.movtery.zalithlauncher.game.version.mod.isEnabled
import com.movtery.zalithlauncher.game.version.mod.readCapturedLogTail
import com.movtery.zalithlauncher.game.version.mod.resolveCrashDecision
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.game.version.mod.CrashEvidenceKind
import com.movtery.zalithlauncher.game.version.mod.DIAGNOSIS_CLASS_MISSING_DEPENDENCY
import com.movtery.zalithlauncher.game.version.mod.DIAGNOSIS_CLASS_MOD_CONFLICT
import com.movtery.zalithlauncher.game.version.mod.DIAGNOSIS_CLASS_WRONG_VERSION
import com.movtery.zalithlauncher.game.version.mod.readModConflictEvidence
import com.movtery.zalithlauncher.game.version.mod.readWrongVersionEvidence
import com.movtery.zalithlauncher.game.version.mod.DependencyRepairSelection
import com.movtery.zalithlauncher.game.version.mod.MissingDependencyEvidence
import com.movtery.zalithlauncher.game.version.mod.ModDoctor
import com.movtery.zalithlauncher.game.version.mod.ModDoctorReport
import com.movtery.zalithlauncher.game.version.mod.crashInstanceDir
import com.movtery.zalithlauncher.game.version.mod.dependencyRequirementResolved
import com.movtery.zalithlauncher.game.version.mod.readMissingDependencyEvidence
import com.movtery.zalithlauncher.game.version.mod.FAMILY_PROBLEM_TYPES
import com.movtery.zalithlauncher.game.version.mod.FamilyPreviewResult
import com.movtery.zalithlauncher.game.version.mod.FamilyRepairPreview
import com.movtery.zalithlauncher.game.version.mod.LegacyPreviewResult
import com.movtery.zalithlauncher.game.version.mod.buildLegacyRepairPreview
import com.movtery.zalithlauncher.game.version.mod.legacyRepairResolved
import com.movtery.zalithlauncher.game.version.mod.isRepairPlanReview
import com.movtery.zalithlauncher.game.version.mod.restrictedTo
import com.movtery.zalithlauncher.game.version.mod.selectDependencyRepair
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.ui.screens.content.elements.modDoctorProblemText
import com.movtery.zalithlauncher.ui.base.BaseAppCompatActivity
import com.movtery.zalithlauncher.ui.screens.main.ErrorScreen
import com.movtery.zalithlauncher.ui.screens.main.crashlogs.ShareLinkOperation
import com.movtery.zalithlauncher.ui.theme.ZalithLauncherTheme
import com.movtery.zalithlauncher.ui.theme.backgroundColor
import com.movtery.zalithlauncher.ui.theme.onBackgroundColor
import com.movtery.zalithlauncher.utils.copyText
import com.movtery.zalithlauncher.utils.file.shareFile
import com.movtery.zalithlauncher.utils.getParcelableSafely
import com.movtery.zalithlauncher.utils.getSerializableSafely
import com.movtery.zalithlauncher.utils.network.openLink
import com.movtery.zalithlauncher.utils.string.throwableToString
import com.movtery.zalithlauncher.viewmodel.LogsUploadViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.File

private const val BUNDLE_EXIT_TYPE = "BUNDLE_EXIT_TYPE"
private const val BUNDLE_THROWABLE = "BUNDLE_THROWABLE"
private const val BUNDLE_JVM_CRASH = "BUNDLE_JVM_CRASH"
private const val BUNDLE_CAN_RESTART = "BUNDLE_CAN_RESTART"
private const val EXIT_JVM = "EXIT_JVM"
private const val EXIT_LAUNCHER = "EXIT_LAUNCHER"

fun showExitMessage(
    context: Context,
    code: Int,
    isSignal: Boolean,
    logPath: String
) {
    val intent = Intent(context, ErrorActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(BUNDLE_EXIT_TYPE, EXIT_JVM)
        putExtra(BUNDLE_JVM_CRASH, JvmCrash(code, isSignal, logPath))
    }
    context.startActivity(intent)
}

@Parcelize
private data class JvmCrash(
    val code: Int,
    val isSignal: Boolean,
    val logPath: String
): Parcelable

@AndroidEntryPoint
class ErrorActivity : BaseAppCompatActivity() {

    /**
     * 游戏崩溃日志上传逻辑管理 ViewModel
     */
    private val viewModel: LogsUploadViewModel by viewModels()

    /** 「安装依赖并修复」：检查或修复是否正在进行、待确认的预览、需要告诉用户的说明 */
    private val repairChecking = mutableStateOf(false)
    private val pendingRepair = mutableStateOf<DependencyRepairPreview?>(null)
    private val repairUi = mutableStateOf<AllRepairUiState?>(null)
    private val repairExtra = mutableStateOf<(() -> Unit)?>(null)
    private var repairJob: kotlinx.coroutines.Job? = null
    /** temporary-disable fallback (set by onCreate once the crash decision is ready) */
    private val disableAction = mutableStateOf<(() -> Unit)?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = intent.extras ?: return runFinish()
        extras.classLoader = javaClass.classLoader

        val exitType = extras.getString(BUNDLE_EXIT_TYPE, EXIT_LAUNCHER)

        val errorMessage = when (exitType) {
            EXIT_JVM -> {
                val jvmCrash = extras.getParcelableSafely(BUNDLE_JVM_CRASH, JvmCrash::class.java) ?: return runFinish()
                val messageResId = if (jvmCrash.isSignal) R.string.crash_singnal_message else R.string.crash_exit_message
                val message = getString(messageResId, jvmCrash.code)
                val messageBody = getString(R.string.crash_exit_note)
                val logFile = File(jvmCrash.logPath)

                //游戏进程已经死了，能看懂这次日志的地方只能是默认进程——也就是这个页面
                val diagnosis = diagnose(logFile, jvmCrash.code)

                //落地决议（动作 + 学习记录）只从日志的路径和文本推出来，不读内存里的任何版本
                //注册表：崩溃页面很可能是被冷启动起来的进程，那时注册表是空的（2026-09-24 真机
                //运行时进程被回收，按钮因此从来没被组合出来）。推出来的东西要读模组目录里的 jar，
                //所以放在后台；推不出来就只是「这次没有动作」。
                val decision = mutableStateOf<CrashDecision?>(null)
                lifecycleScope.launch(Dispatchers.IO) {
                    val resolved = runCatching {
                        resolveCrashDecision(logFile, diagnosis.text, diagnosis.outcome)
                    }.getOrNull()

                    //写记录失败也只是少一条记录，不能让崩溃页面因此失败
                    resolved?.match?.let { match ->
                        runCatching { CrashLearning.recordDiagnosis(match) }
                    }
                    resolved?.let { withContext(Dispatchers.Main) { decision.value = it } }
                }

                ErrorMessage(
                    message = message,
                    messageBody = messageBody,
                    crashType = CrashType.GAME_CRASH,
                    logFile = logFile.also { file ->
                        //检查日志文件是否适合上传
                        viewModel.check(file)
                    },
                    diagnosisLines = diagnosis.lines,
                    dependencyEvidence = diagnosis.dependencyEvidence,
                    decision = decision,
                )
            }
            else -> {
                val throwable = extras.getSerializableSafely(BUNDLE_THROWABLE, Throwable::class.java) ?: return runFinish()
                val message = getString(R.string.crash_launcher_message)
                val messageBody = throwableToString(throwable)
                ErrorMessage(
                    message = message,
                    messageBody = messageBody,
                    crashType = CrashType.LAUNCHER_CRASH,
                    logFile = PathManager.FILE_CRASH_REPORT
                )
            }
        }

        val logFile = errorMessage.logFile
        //等待用户确认的修复（为空表示没有待确认的动作）
        val pendingFix = mutableStateOf<CrashDecision?>(null)
        disableAction.value = { errorMessage.decision.value?.let { pendingFix.value = it } }
        val canRestart: Boolean = extras.getBoolean(BUNDLE_CAN_RESTART, true)
        val logExists = logFile.exists() && logFile.isFile

        setContent {
            ZalithLauncherTheme {
                //确认之前不改动任何文件：这里只展示要做什么，确认后才进入 applyCrashFix
                pendingFix.value?.let { decision ->
                    val fix = decision.fix ?: return@let
                    AllAlertDialog(
                        title = stringResource(R.string.crash_fix_preview_title),
                        text = stringResource(R.string.crash_fix_preview_body, fix.modId),
                        onConfirm = {
                            pendingFix.value = null
                            applyCrashFix(decision, logFile)
                        },
                        onDismiss = { pendingFix.value = null },
                    )
                }
                //ALL-owned repair UI: Planning → Preview → Confirm → Applying → Success / Error / No fix.
                //Cancel never touches the installed mods; the plan, transaction, hash checks and rollback are ModDoctor's.
                AllRepairHost(
                    state = repairUi.value,
                    onConfirm = { pendingRepair.value?.let { applyDependencyRepair(it) } },
                    onCancel = { cancelRepair() },
                    onClose = { repairUi.value = null },
                    onExtra = { repairExtra.value?.invoke() },
                )
                ShareLinkOperation(
                    operation = viewModel.operation,
                    onChange = { viewModel.operation = it },
                    onUploadChancel = { viewModel.cancel() },
                    onUpload = {
                        viewModel.upload(logFile) { link ->
                            openLink(link)
                            copyText(COPY_LABEL_LINK, link, this@ErrorActivity)
                        }
                    }
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = backgroundColor(),
                    contentColor = onBackgroundColor()
                ) {
                    ErrorScreen(
                        details = (listOf(errorMessage.message, errorMessage.messageBody) + errorMessage.diagnosisLines).joinToString("\n\n"),
                        onCopyLog = if (logExists) ({
                            lifecycleScope.launch {
                                try {
                                    val content = withContext(Dispatchers.IO) { com.movtery.zalithlauncher.ui.screens.content.readLog(logFile) }
                                    copyText(logFile.name, content, this@ErrorActivity)
                                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                    throw cancelled
                                } catch (error: Exception) {
                                    com.movtery.zalithlauncher.all.overlays.showAllErrorDialog(this@ErrorActivity,
                                        getString(R.string.all_log_failed), error.toString())
                                }
                            }
                        }) else null,
                        crashType = errorMessage.crashType,
                        shareLogs = logExists,
                        canUpload = viewModel.canUpload,
                        canRestart = canRestart,
                        diagnosisLines = errorMessage.diagnosisLines,
                        repairActionLabel = errorMessage.dependencyEvidence?.let { evidence ->
                            getString(
                                when {
                                    repairChecking.value -> R.string.crash_action_repair_checking
                                    evidence.kind == CrashEvidenceKind.WRONG_VERSION -> R.string.crash_action_repair_wrong_version
                                    evidence.kind == CrashEvidenceKind.MOD_CONFLICT -> R.string.crash_action_repair_conflict
                                    else -> R.string.crash_action_repair
                                },
                                evidence.dependencyId
                            )
                        },
                        onRepairActionClick = {
                            errorMessage.dependencyEvidence?.let { evidence ->
                                checkDependencyRepair(
                                    logFile,
                                    evidence,
                                    offerDisable = errorMessage.decision.value?.fix != null,
                                )
                            }
                        },
                        fixActionLabel = errorMessage.decision.value?.fix
                            ?.let { fix -> getString(R.string.crash_diagnosis_action_disable, fix.modId) },
                        onFixActionClick = {
                            val decision = errorMessage.decision.value
                            if (decision?.fix == null || decision.match == null) {
                                //还没推出来、或这次什么都推不出来：页面与过去完全一致，只重启
                                ProcessPhoenix.triggerRebirth(this@ErrorActivity)
                            } else {
                                //先给用户看预览；确认之前不改任何文件
                                pendingFix.value = decision
                            }
                        },
                        onShareLogsClick = {
                            if (logExists) {
                                shareFile(this@ErrorActivity, logFile)
                            }
                        },
                        onUploadClick = {
                            viewModel.operation = ShareLinkOperation.Tip
                        },
                        onRestartClick = {
                            ProcessPhoenix.triggerRebirth(this@ErrorActivity)
                        },
                        onExitClick = { finish() },
                        onOrientationChanged = {
                            this@ErrorActivity.requestedOrientation = it
                        },
                    ) {
                        Text(
                            text = errorMessage.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = errorMessage.messageBody,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    /**
     * 判定一次启动失败。
     *
     * 证据是启动器自己捕获的游戏标准输出：[logFile]。它只说得清「哪一次运行、日志里写了什么」，
     * 说不清任何内存里的版本状态：崩溃页面很可能是被冷启动起来的进程，那时版本注册表是空的，
     * 所以「是哪一次运行」只能从日志路径推。这个函数只负责判定和文案，落地决议见
     * [resolveCrashDecision]。
     */
    private fun diagnose(logFile: File, exitCode: Int): DiagnosisUi {
        val text = readCapturedLogTail(logFile)
        val outcome = runCatching {
            classifyCapturedLog(text, exitCode)
        }.getOrNull() ?: return DiagnosisUi(text = text)

        val finding = outcome.finding
        //缺失依赖：点名需要者、依赖与版本范围，并附上日志原句，让用户看清楚要装什么、为什么
        val evidence = when (finding?.diagnosisType) {
            DIAGNOSIS_CLASS_MISSING_DEPENDENCY -> readMissingDependencyEvidence(text)
            //版本不对 / 两个模组冲突：同样只认加载器的原句，修复入口仍是 Mod Doctor
            DIAGNOSIS_CLASS_WRONG_VERSION -> readWrongVersionEvidence(text)
            DIAGNOSIS_CLASS_MOD_CONFLICT -> readModConflictEvidence(text)
            else -> null
        }

        val lines = buildList {
            add(getString(R.string.crash_diagnosis_heading))
            add(getString(diagnosisMessageRes(finding?.messageKey.orEmpty())))
            add(getString(outcome.confidence.labelRes))
            finding?.ownerModId?.let { id -> add(getString(R.string.crash_diagnosis_owner, id)) }
            evidence?.let { found ->
                add(getString(R.string.crash_repair_cause, found.requirerName, found.requirerVersion, found.requirerId))
                when (found.kind) {
                    CrashEvidenceKind.MISSING_DEPENDENCY -> add(getString(R.string.crash_repair_dependency, found.dependencyId, found.constraint))
                    CrashEvidenceKind.WRONG_VERSION -> add(getString(R.string.crash_repair_wrong_version, found.dependencyId, found.constraint, found.presentVersion))
                    CrashEvidenceKind.MOD_CONFLICT -> add(getString(R.string.crash_repair_conflict, found.dependencyId, found.presentVersion, found.constraint))
                }
                found.evidence.forEach { add(getString(R.string.crash_repair_evidence, it)) }
            }
        }

        return DiagnosisUi(outcome = outcome, text = text, lines = lines, dependencyEvidence = evidence)
    }

    /**
     * 用户确认之后才执行的修复：先做快照，再禁用，再登记学习记录。
     *
     * 快照失败就什么都不改（与 Mod Doctor 的修复一致）；改动之后快照收尾为 APPLIED，
     * 之后可用「ย้อนกลับการซ่อมล่าสุด」恢复。
     */
    private fun applyCrashFix(decision: CrashDecision, logFile: File) {
        val fix = decision.fix ?: return
        val match = decision.match ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            //快照放在实例目录（mods 的上一级）下，与 Mod Doctor 的修复共用同一把锁、同一套备份与回滚
            val versionPath = fix.modsDir.parentFile ?: return@launch
            when (val result = disableModWithSnapshot(versionPath, fix.modsDir) { disableModById(fix.modsDir, fix.modId) }) {
                is SnapshotDisableResult.Disabled -> {
                    //只有真的禁用成功才登记动作：登记值就是下次启动用日志收尾的凭据
                    runCatching {
                        CrashLearning.recordActionTaken(
                            match = match,
                            preState = result.preState,
                            logStamp = capturedLogStamp(logFile)
                        )
                    }
                    //修复已经执行，进程马上重启：把这次重启记成待续，重启之后由启动路径自己接手
                    runCatching {
                        CrashLearning.markRelaunchPending(
                            instanceName = match.instanceName,
                            atMillis = System.currentTimeMillis()
                        )
                    }
                    withContext(Dispatchers.Main) {
                        ProcessPhoenix.triggerRebirth(this@ErrorActivity)
                    }
                }
                //失败：不登记、不自动重启，留在本页并说明原因
                else -> withContext(Dispatchers.Main) {
                    val message = when (result) {
                        SnapshotDisableResult.Busy -> getString(R.string.crash_fix_busy)
                        is SnapshotDisableResult.SnapshotRefused -> getString(R.string.crash_fix_snapshot_failed)
                        else -> getString(R.string.crash_fix_disable_failed)
                    }
                    com.movtery.zalithlauncher.all.overlays.showAllErrorDialog(
                        this@ErrorActivity,
                        getString(R.string.crash_fix_failed_title),
                        message
                    )
                }
            }
        }
    }

    /**
     * 按项目已有的可逆方式禁用命中的模组文件（重命名为 `.disabled`）。
     *
     * 只按目录里的模组 id 找文件，不需要任何加载好的版本对象。
     * @return 改动前的文件名；没找到文件或改名失败都返回 null，调用方据此不登记任何动作
     */
    private suspend fun disableModById(modsDir: File, modId: String): String? {
        val local = AllModReader(modsDir).readAllLocals()
            .firstOrNull { it.id == modId && !it.notMod && it.file.isEnabled() }
            ?: return null

        val preState = local.file.name
        return preState.takeIf { local.file.disableModFile() != null }
    }

    /**
     * 「安装依赖并修复」的检查阶段：只读，不改任何文件。
     *
     * ขั้นแรกให้ Family Planner คิดแผนทั้งชุดครั้งเดียว (ใช้ร่วมกับหน้า Mods); ถ้าไม่ใช่เคสของมัน (กฎ known-good / ปัญหาอื่น)
     * ใช้เส้นทางเดิมของ Mod Doctor แต่แสดงด้วย UI ของ ALL เหมือนกัน
     */
    private fun checkDependencyRepair(logFile: File, evidence: MissingDependencyEvidence, offerDisable: Boolean) {
        if (repairChecking.value) return
        repairChecking.value = true
        repairExtra.value = null
        repairUi.value = AllRepairUiState.Planning
        repairJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                //实例从日志路径推出来，不依赖内存里的版本列表（崩溃页面可能是冷启动的进程）
                val version = crashInstanceDir(logFile)?.let { VersionsManager.loadVersionFromFolder(it) }
                if (version == null) {
                    showRepairNotice(
                        getString(R.string.crash_repair_failed_title),
                        getString(R.string.crash_repair_instance_missing),
                        offerDisable,
                    )
                    return@launch
                }

                var familyRefusal: String? = null
                when (val family = ModDoctor().previewFamilyRepair(version)) {
                    is FamilyPreviewResult.Plan -> {
                        showRepairPreview(
                            DependencyRepairPreview(version, family.preview.report, evidence, emptyList(), offerDisable, logFile, family.preview)
                        )
                        return@launch
                    }
                    is FamilyPreviewResult.Failed -> {
                        showRepairNotice(getString(R.string.crash_repair_failed_title), getString(R.string.crash_repair_failed_body, family.reason), offerDisable)
                        return@launch
                    }
                    is FamilyPreviewResult.NoFix -> familyRefusal = family.reason
                    FamilyPreviewResult.NotApplicable -> Unit
                }

                val report = ModDoctor().inspect(version)
                when (val selection = selectDependencyRepair(report, evidence)) {
                    //ไม่ใช่ planner ทั้งชุด: Preview ได้เฉพาะที่ระบุไฟล์ล่วงหน้าได้แน่ ๆ (กฎ known-good); อย่างอื่นปฏิเสธการซ่อมอัตโนมัติ
                    is DependencyRepairSelection.Ready -> showLegacyOrRefuse(
                        version, report.restrictedTo(selection.problem), evidence, offerDisable, logFile
                    )
                    is DependencyRepairSelection.ReadyFix -> showLegacyOrRefuse(
                        version, report.restrictedTo(selection.problem), evidence, offerDisable, logFile
                    )
                    is DependencyRepairSelection.NoSafeVersion -> showRepairNotice(
                        getString(R.string.all_repair_nofix_title),
                        if (familyRefusal != null) getString(R.string.crash_repair_family_refused_body, familyRefusal)
                        else getString(
                            R.string.crash_repair_unavailable_body,
                            evidence.requirerName,
                            evidence.dependencyId,
                            modDoctorProblemText(this@ErrorActivity, selection.problem),
                        ),
                        offerDisable,
                        AllRepairResultKind.NoFix,
                    )
                    DependencyRepairSelection.NotReported -> showRepairNotice(
                        getString(R.string.all_repair_nofix_title),
                        if (familyRefusal != null) getString(R.string.crash_repair_family_refused_body, familyRefusal)
                        else getString(R.string.crash_repair_not_reported_body, evidence.requirerName, evidence.dependencyId),
                        offerDisable,
                        AllRepairResultKind.NoFix,
                    )
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                showRepairNotice(getString(R.string.crash_repair_failed_title), error.message ?: error.toString(), offerDisable)
            } finally {
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) { repairChecking.value = false }
            }
        }
    }

    private suspend fun showLegacyOrRefuse(
        version: Version,
        restricted: ModDoctorReport,
        evidence: MissingDependencyEvidence,
        offerDisable: Boolean,
        logFile: File,
    ) {
        when (val legacy = buildLegacyRepairPreview(restricted)) {
            is LegacyPreviewResult.Plan -> showRepairPreview(
                DependencyRepairPreview(version, legacy.preview.report, evidence, emptyList(), offerDisable, logFile, legacy.preview)
            )
            is LegacyPreviewResult.NoFix -> showRepairNotice(
                getString(R.string.all_repair_nofix_title),
                getString(R.string.crash_repair_family_refused_body, legacy.reason),
                offerDisable,
                AllRepairResultKind.NoFix,
            )
        }
    }

    /** 把预览交给 ALL 的预览对话框；确认之前什么都没改 */
    private suspend fun showRepairPreview(preview: DependencyRepairPreview) {
        withContext(Dispatchers.Main) {
            pendingRepair.value = preview
            val family = preview.family
            repairUi.value = AllRepairUiState.Preview(
                title = when {
                    family != null -> getString(R.string.all_repair_preview_title, family.items.size)
                    preview.evidence.kind == CrashEvidenceKind.MISSING_DEPENDENCY -> getString(R.string.crash_repair_preview_title, preview.evidence.dependencyId)
                    else -> getString(R.string.crash_repair_preview_title_fix, preview.evidence.requirerName)
                },
                items = family?.items.orEmpty(),
                lines = preview.lines,
                hasPrerelease = family?.hasPrerelease == true,
                confirmText = getString(
                    if (family == null && preview.evidence.kind == CrashEvidenceKind.MISSING_DEPENDENCY) R.string.crash_repair_confirm else R.string.all_repair_confirm
                ),
            )
        }
    }

    /** Cancel / 关闭预览：不改任何已安装的模组；Family 计划已暂存的文件一并删除 */
    private fun cancelRepair() {
        repairJob?.cancel()
        pendingRepair.value?.let { preview -> if (preview.family != null) ModDoctor().discardFamilyPreview(preview.version) }
        pendingRepair.value = null
        repairUi.value = null
        repairChecking.value = false
    }

    /**
     * 「安装依赖并修复」的执行阶段：只有用户在预览里确认之后才调用。
     *
     * 修复本身（校验、快照、事务、回滚）由 [ModDoctor.fixProblems] 完成，且只包含预览里的那一份计划；
     * 计划或文件在确认前后发生变化时 Fix 不会改任何文件，改为要求重新预览。
     * 修复之后重新检查，通过才重启，由启动路径拉起这个实例。这里不登记「已修复」：是否真的修好由游戏启动来证明。
     */
    private fun applyDependencyRepair(preview: DependencyRepairPreview) {
        if (repairChecking.value && repairUi.value == AllRepairUiState.Applying) return
        repairChecking.value = true
        pendingRepair.value = null
        repairExtra.value = null
        repairUi.value = AllRepairUiState.Applying
        repairJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fix = ModDoctor().fixProblems(preview.version, preview.report)
                fix.failureReason?.let { reason ->
                    if (isRepairPlanReview(reason)) {
                        showRepairNotice(
                            getString(R.string.crash_repair_failed_title),
                            getString(R.string.all_repair_plan_changed),
                            preview.offerDisable,
                            AllRepairResultKind.Error,
                            reviewAgain = { checkDependencyRepair(preview.logFile, preview.evidence, preview.offerDisable) },
                        )
                    } else {
                        showRepairNotice(getString(R.string.crash_repair_failed_title), getString(R.string.crash_repair_failed_body, reason), preview.offerDisable)
                    }
                    return@launch
                }

                val rechecked = ModDoctor().inspect(preview.version)
                if (preview.family != null) {
                    if (rechecked.problems.any { it.type in FAMILY_PROBLEM_TYPES }) {
                        showRepairNotice(getString(R.string.crash_repair_failed_title), getString(R.string.crash_repair_family_still_body), preview.offerDisable)
                        return@launch
                    }
                } else if (!dependencyRequirementResolved(rechecked, preview.evidence)) {
                    showRepairNotice(
                        getString(R.string.crash_repair_failed_title),
                        getString(R.string.crash_repair_still_missing_body, preview.evidence.requirerName, preview.evidence.dependencyId),
                        preview.offerDisable,
                    )
                    return@launch
                }

                //修复已经落盘：先告诉用户结果，随后进程重启，由启动路径接手并拉起这个实例
                runCatching { CrashLearning.markRelaunchPending(preview.version.getVersionName()) }
                withContext(Dispatchers.Main) {
                    repairUi.value = AllRepairUiState.Result(
                        AllRepairResultKind.Success,
                        getString(R.string.all_repair_success_title),
                        getString(R.string.all_repair_success_crash_body),
                    )
                }
                kotlinx.coroutines.delay(1800)
                withContext(Dispatchers.Main) { ProcessPhoenix.triggerRebirth(this@ErrorActivity) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                showRepairNotice(
                    getString(R.string.crash_repair_failed_title),
                    getString(R.string.crash_repair_failed_body, error.message ?: error.toString()),
                    preview.offerDisable,
                )
            } finally {
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main) { repairChecking.value = false }
            }
        }
    }

    /** 版本不对 / 冲突的预览：问题与方案原文来自 Mod Doctor 自己的报告，确认之前什么都没改 */
    private fun fixPreviewLines(evidence: MissingDependencyEvidence, problem: com.movtery.zalithlauncher.game.version.mod.ModDoctorProblem): List<String> = buildList {
        add(getString(R.string.crash_repair_preview_original, evidence.requirerName, evidence.requirerVersion, evidence.requirerId))
        add(modDoctorProblemText(this@ErrorActivity, problem))
        if (evidence.kind == CrashEvidenceKind.MOD_CONFLICT && problem.ruleFallbackFix == null) add(getString(R.string.crash_repair_preview_conflict_plan))
        add(getString(R.string.crash_repair_preview_backup))
        add(getString(R.string.crash_repair_preview_impact_replace))
        add(getString(R.string.crash_repair_preview_other))
    }

    /** 预览的每一行都是一个事实：动什么、依据什么校验、怎么撤销、影响多大 */
    private fun repairPreviewLines(version: Version, candidateName: String?, evidence: MissingDependencyEvidence): List<String> {
        val info = version.getVersionInfo()
        return listOf(
            getString(R.string.crash_repair_preview_original, evidence.requirerName, evidence.requirerVersion, evidence.requirerId),
            getString(
                R.string.crash_repair_preview_install,
                evidence.dependencyId,
                candidateName ?: getString(R.string.generic_unknown),
            ),
            getString(
                R.string.crash_repair_preview_checked,
                evidence.dependencyId,
                info?.minecraftVersion.orEmpty(),
                info?.loaderInfo?.loader?.displayName.orEmpty(),
                evidence.constraint,
            ),
            getString(R.string.crash_repair_preview_backup),
            getString(R.string.crash_repair_preview_impact),
            getString(R.string.crash_repair_preview_other),
        )
    }

    private suspend fun showRepairNotice(
        title: String,
        body: String,
        offerDisable: Boolean,
        kind: AllRepairResultKind = AllRepairResultKind.Error,
        reviewAgain: (() -> Unit)? = null,
    ) {
        withContext(Dispatchers.Main) {
            val disable = if (offerDisable) disableAction.value else null
            val extra: Pair<String, () -> Unit>? = when {
                reviewAgain != null -> getString(R.string.all_repair_review_again) to reviewAgain
                disable != null -> getString(R.string.crash_repair_offer_disable) to disable
                else -> null
            }
            repairExtra.value = extra?.let { pair -> { repairUi.value = null; pair.second() } }
            repairUi.value = AllRepairUiState.Result(kind, title, body, extra?.first)
        }
    }

    private data class ErrorMessage(
        val message: String,
        val messageBody: String,
        val crashType: CrashType,
        val logFile: File,
        /** 给用户看的诊断文案；读不出日志时为空，页面与过去完全一致 */
        val diagnosisLines: List<String> = emptyList(),
        /** 日志点名的缺失依赖证据；只有缺失依赖的诊断才有，其它诊断为空 */
        val dependencyEvidence: MissingDependencyEvidence? = null,
        /** 从日志路径和文本推出来的落地决议；还没推出来或推不出来时为空，此时没有任何动作 */
        val decision: MutableState<CrashDecision?> = mutableStateOf(null),
    )

    /**
     * 崩溃页面拿到的判定：给用户看的几行文案。
     *
     * 「能不能做点什么」不在这里定：那要读模组目录，见 [resolveCrashDecision]。
     */
    private data class DiagnosisUi(
        val outcome: CrashOutcome? = null,
        /** 判定用的日志文本，落地决议还要用它读版本与加载器 */
        val text: String = "",
        val lines: List<String> = emptyList(),
        val dependencyEvidence: MissingDependencyEvidence? = null,
    )
}

/** 「安装依赖并修复」待确认的预览：只读的计划，确认之前什么都没改 */
private class DependencyRepairPreview(
    val version: Version,
    /** Family 计划：关系类问题 + 计划 key；其它路径：只含这一条问题的报告，确认后修复只做它 */
    val report: ModDoctorReport,
    val evidence: MissingDependencyEvidence,
    /** 规则 / 旧路径的纯文本预览；Family 计划用结构化的 [family] */
    val lines: List<String>,
    /** 修复没有成功时能否改为临时禁用（只有确有可禁用的文件时才可以） */
    val offerDisable: Boolean,
    val logFile: File,
    val family: FamilyRepairPreview? = null,
)

/**
 * 崩溃类型
 */
enum class CrashType(val textRes: Int) {
    /**
     * 启动器崩溃
     */
    LAUNCHER_CRASH(R.string.crash_type_launcher),

    /**
     * 游戏运行崩溃
     */
    GAME_CRASH(R.string.crash_type_game)
}

/**
 * 启动软件崩溃信息页面
 */
fun showLauncherCrash(context: Context, throwable: Throwable, canRestart: Boolean = true) {
    val intent = Intent(context, ErrorActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(BUNDLE_EXIT_TYPE, EXIT_LAUNCHER)
        putExtra(BUNDLE_THROWABLE, throwable)
        putExtra(BUNDLE_CAN_RESTART, canRestart)
    }
    context.startActivity(intent)
}