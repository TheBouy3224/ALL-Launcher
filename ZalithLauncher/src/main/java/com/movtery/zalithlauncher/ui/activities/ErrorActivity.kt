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
import androidx.compose.material3.AlertDialog
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
import com.movtery.zalithlauncher.game.version.mod.DIAGNOSIS_CLASS_MISSING_DEPENDENCY
import com.movtery.zalithlauncher.game.version.mod.DependencyRepairSelection
import com.movtery.zalithlauncher.game.version.mod.MissingDependencyEvidence
import com.movtery.zalithlauncher.game.version.mod.ModDoctor
import com.movtery.zalithlauncher.game.version.mod.ModDoctorReport
import com.movtery.zalithlauncher.game.version.mod.crashInstanceDir
import com.movtery.zalithlauncher.game.version.mod.dependencyRequirementResolved
import com.movtery.zalithlauncher.game.version.mod.readMissingDependencyEvidence
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
    private val repairNotice = mutableStateOf<RepairNotice?>(null)

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
        val canRestart: Boolean = extras.getBoolean(BUNDLE_CAN_RESTART, true)
        val logExists = logFile.exists() && logFile.isFile

        setContent {
            ZalithLauncherTheme {
                //确认之前不改动任何文件：这里只展示要做什么，确认后才进入 applyCrashFix
                pendingFix.value?.let { decision ->
                    val fix = decision.fix ?: return@let
                    AlertDialog(
                        onDismissRequest = { pendingFix.value = null },
                        title = { Text(stringResource(R.string.crash_fix_preview_title)) },
                        text = { Text(stringResource(R.string.crash_fix_preview_body, fix.modId)) },
                        confirmButton = {
                            TextButton(onClick = {
                                pendingFix.value = null
                                applyCrashFix(decision, logFile)
                            }) { Text(stringResource(R.string.generic_confirm)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { pendingFix.value = null }) { Text(stringResource(R.string.generic_cancel)) }
                        }
                    )
                }
                //安装依赖并修复：先看预览（要装什么、如何校验、快照与撤销、影响范围）；确认之前不改任何文件
                pendingRepair.value?.let { preview ->
                    AlertDialog(
                        onDismissRequest = { pendingRepair.value = null },
                        title = { Text(stringResource(R.string.crash_repair_preview_title, preview.evidence.dependencyId)) },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            ) {
                                preview.lines.forEach { Text(it) }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                pendingRepair.value = null
                                applyDependencyRepair(preview)
                            }) { Text(stringResource(R.string.crash_repair_confirm)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { pendingRepair.value = null }) { Text(stringResource(R.string.generic_cancel)) }
                        }
                    )
                }
                //检查或修复没有走到底：说明原因；只有确有可禁用的文件时，才提供临时禁用这个备用动作
                repairNotice.value?.let { notice ->
                    AlertDialog(
                        onDismissRequest = { repairNotice.value = null },
                        title = { Text(notice.title) },
                        text = { Text(notice.body) },
                        confirmButton = {
                            TextButton(onClick = { repairNotice.value = null }) { Text(stringResource(R.string.generic_close)) }
                        },
                        dismissButton = if (notice.offerDisable) ({
                            TextButton(onClick = {
                                repairNotice.value = null
                                errorMessage.decision.value?.let { pendingFix.value = it }
                            }) { Text(stringResource(R.string.crash_repair_offer_disable)) }
                        }) else null
                    )
                }
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
                                if (repairChecking.value) R.string.crash_action_repair_checking else R.string.crash_action_repair,
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
        val evidence = finding?.takeIf { it.diagnosisType == DIAGNOSIS_CLASS_MISSING_DEPENDENCY }
            ?.let { readMissingDependencyEvidence(text) }

        val lines = buildList {
            add(getString(R.string.crash_diagnosis_heading))
            add(getString(diagnosisMessageRes(finding?.messageKey.orEmpty())))
            add(getString(outcome.confidence.labelRes))
            finding?.ownerModId?.let { id -> add(getString(R.string.crash_diagnosis_owner, id)) }
            evidence?.let { found ->
                add(getString(R.string.crash_repair_cause, found.requirerName, found.requirerVersion, found.requirerId))
                add(getString(R.string.crash_repair_dependency, found.dependencyId, found.constraint))
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
     * 用 Mod Doctor 现有的扫描看这个实例，找出日志点名的那一条缺失依赖。只有它真的有一个通过全部校验的候选
     * （Mod ID、Minecraft 版本、加载器、版本范围）才出预览；否则只说明原因，什么都不安装。
     */
    private fun checkDependencyRepair(logFile: File, evidence: MissingDependencyEvidence, offerDisable: Boolean) {
        if (repairChecking.value) return
        repairChecking.value = true
        lifecycleScope.launch(Dispatchers.IO) {
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

                val report = ModDoctor().inspect(version)
                when (val selection = selectDependencyRepair(report, evidence)) {
                    is DependencyRepairSelection.Ready -> withContext(Dispatchers.Main) {
                        pendingRepair.value = DependencyRepairPreview(
                            version = version,
                            report = report.restrictedTo(selection.problem),
                            evidence = evidence,
                            lines = repairPreviewLines(version, selection.candidate.versionName, evidence),
                            offerDisable = offerDisable,
                        )
                    }
                    is DependencyRepairSelection.NoSafeVersion -> showRepairNotice(
                        getString(R.string.crash_repair_unavailable_title),
                        getString(
                            R.string.crash_repair_unavailable_body,
                            evidence.requirerName,
                            evidence.dependencyId,
                            modDoctorProblemText(this@ErrorActivity, selection.problem),
                        ),
                        offerDisable,
                    )
                    DependencyRepairSelection.NotReported -> showRepairNotice(
                        getString(R.string.crash_repair_unavailable_title),
                        getString(R.string.crash_repair_not_reported_body, evidence.requirerName, evidence.dependencyId),
                        offerDisable,
                    )
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                showRepairNotice(getString(R.string.crash_repair_failed_title), error.message ?: error.toString(), offerDisable)
            } finally {
                withContext(Dispatchers.Main) { repairChecking.value = false }
            }
        }
    }

    /**
     * 「安装依赖并修复」的执行阶段：只有用户在预览里确认之后才调用。
     *
     * 修复本身（下载、校验、快照）由 [ModDoctor.fixProblems] 完成，且只包含预览里的那一条问题。
     * 修复之后重新检查：这条依赖的需求必须已经不再被报告，才重启，由启动路径拉起这个实例（它会再过一次模组检查）。
     * 这里不登记「已修复」：是否真的修好，要由游戏启动来证明，而不是由这一步声称。
     */
    private fun applyDependencyRepair(preview: DependencyRepairPreview) {
        if (repairChecking.value) return
        repairChecking.value = true
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fix = ModDoctor().fixProblems(preview.version, preview.report)
                fix.failureReason?.let { reason ->
                    showRepairNotice(
                        getString(R.string.crash_repair_failed_title),
                        getString(R.string.crash_repair_failed_body, reason),
                        preview.offerDisable,
                    )
                    return@launch
                }

                val rechecked = ModDoctor().inspect(preview.version)
                if (!dependencyRequirementResolved(rechecked, preview.evidence)) {
                    showRepairNotice(
                        getString(R.string.crash_repair_failed_title),
                        getString(
                            R.string.crash_repair_still_missing_body,
                            preview.evidence.requirerName,
                            preview.evidence.dependencyId,
                        ),
                        preview.offerDisable,
                    )
                    return@launch
                }

                //修复已经落盘：进程马上重启，由启动路径接手并拉起这个实例
                runCatching { CrashLearning.markRelaunchPending(preview.version.getVersionName()) }
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
                withContext(Dispatchers.Main) { repairChecking.value = false }
            }
        }
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

    private suspend fun showRepairNotice(title: String, body: String, offerDisable: Boolean) {
        withContext(Dispatchers.Main) { repairNotice.value = RepairNotice(title, body, offerDisable) }
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
    /** 只含这一条缺失依赖的报告：确认后修复只做它，报告里的其它问题不会被顺手改动 */
    val report: ModDoctorReport,
    val evidence: MissingDependencyEvidence,
    val lines: List<String>,
    /** 修复没有成功时能否改为临时禁用（只有确有可禁用的文件时才可以） */
    val offerDisable: Boolean,
)

/** 检查或修复没有走到底时，告诉用户原因的一条说明 */
private data class RepairNotice(val title: String, val body: String, val offerDisable: Boolean)

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