package com.movtery.zalithlauncher.all.bridge

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.net.Uri
import androidx.compose.ui.unit.Dp
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.components.SimpleTaskDialog
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.AssetsIcon
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.initAll
import com.movtery.zalithlauncher.ui.screens.content.elements.DeleteVersionDialog
import com.movtery.zalithlauncher.ui.screens.content.elements.rememberMultipleUriImportTaskBuilder
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionIconImage
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel
import kotlinx.coroutines.Dispatchers

/**
 * ALL Bridge：ALL 界面与 Zalith 之间的唯一接缝。
 *
 * ALL 自己的界面只 import 本文件，不再直接 import Zalith 的界面实现
 * （对话框与图标组件）与需要的 Zalith callbacks/types。
 * Download 自 PHASE 2.4 起由 ALL 自有 UI + service adapter 实现（all/download/）。
 * Settings 自 PHASE 2.3 起由 ALL 自有 UI 直接调用 config/backend adapters（all/settings/）。
 * Files window 自 PHASE 2.2 起使用 ALL 自有呈现和适配层（all/files/），不再经过本层。
 * 模组窗口自 PHASE 2.1 起改由 ALL 自己实现（all/mods/），不再经过本层。
 * Zalith 将来改动这些界面或改名，只会影响本文件；ALL 界面不用跟着改。
 *
 * 本层只做转发：参数、默认值、可见性判断、生命周期都与原来完全一致，不改行为。
 * 未纳入本层的是导航 Key 本身（NormalNavKey / NestedNavKey）——它们是导航契约，
 * 不是界面实现，收进本层要连同返回栈一起重做，属于下一阶段。
 */

/** 返回栈 ViewModel。ALL 界面只按别名引用，不直接 import Zalith 的 viewmodel 包。 */
typealias AllBackStackViewModel = ScreenBackStackViewModel

/** 提示与错误上报回调的类型别名。别名是透明的，调用方传入的 lambda 不需要改动。 */
typealias AllToast = AndroidStringText
typealias AllSubmitError = (ErrorViewModel.ThrowableMessage) -> Unit

// ---------------------------------------------------------------------------
// 文件 / 下载
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// 对话框与图标
// ---------------------------------------------------------------------------

/** 删除实例的确认对话框：复用经典界面的对话框与任务执行方式。 */
@Composable
fun AllDeleteVersionDialog(
    version: Version,
    onDismissRequest: () -> Unit,
    onConfirm: (titleRes: Int, task: suspend () -> Unit) -> Unit,
    onVersionDeleted: () -> Unit
) {
    DeleteVersionDialog(
        version = version,
        onDismissRequest = onDismissRequest,
        onConfirm = onConfirm,
        onVersionDeleted = onVersionDeleted,
        allPresentation = true
    )
}

/** 任务进度对话框。调度器由本层固定，上层不必知道它跑在哪个线程。 */
@Composable
fun AllTaskDialog(
    title: String,
    task: suspend () -> Unit,
    onDismiss: () -> Unit
) {
    SimpleTaskDialog(
        title = title,
        task = task,
        context = Dispatchers.IO,
        onDismiss = onDismiss,
        allPresentation = true
    )
}

/** 项目封面网络图标（Coil 加载、缓存、占位与失败回退都沿用 Zalith 的实现）。 */
@Composable
fun AllAssetIcon(size: Dp, iconUrl: String?, modifier: Modifier = Modifier) {
    AssetsIcon(modifier = modifier, size = size, iconUrl = iconUrl)
}

/** 与经典下载页相同：对每个版本调用 initFile，并丢弃没有可下载文件的版本。 */
suspend fun List<PlatformVersion>.allInitFiles(projectId: String): List<PlatformVersion> = initAll(projectId)

/** 多文件导入任务构建器（复制、后缀校验、错误上报沿用 Zalith 的实现）。 */
@Composable
fun rememberAllUriImportTaskBuilder(
    id: String,
    targetDir: java.io.File,
    checkExtension: List<String>?,
    submitError: AllSubmitError,
    onImported: () -> Unit,
    errorMessage: String? = null,
    onFileCopied: suspend (Task, java.io.File) -> Unit = { _, _ -> }
): (List<Uri>) -> Task = rememberMultipleUriImportTaskBuilder(
    id = id,
    targetDir = targetDir,
    checkExtension = checkExtension,
    submitError = submitError,
    errorMessage = errorMessage ?: androidx.compose.ui.res.stringResource(com.movtery.zalithlauncher.R.string.error_import_file),
    onImported = onImported,
    onFileCopied = onFileCopied,
)

/** Zalith's layout editor (ControlEditorActivity) is still the editor; opened only through this seam. */
fun allStartControlEditor(context: android.content.Context, file: java.io.File) =
    com.movtery.zalithlauncher.ui.activities.startEditorActivity(context, file)

/** 实例图标。 */
@Composable
fun AllVersionIcon(
    version: Version,
    modifier: Modifier = Modifier
) {
    VersionIconImage(version = version, modifier = modifier)
}
