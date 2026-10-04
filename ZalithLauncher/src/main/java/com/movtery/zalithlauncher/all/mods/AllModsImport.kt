package com.movtery.zalithlauncher.all.mods

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.movtery.zalithlauncher.all.bridge.AllSubmitError
import com.movtery.zalithlauncher.contract.extensionToMimeType
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.all.bridge.rememberAllUriImportTaskBuilder
import java.io.File

private const val MOD_EXTENSION = "jar"

/**
 * ALL 模组窗口的“导入模组”动作。
 *
 * 只是把 Zalith 既有的导入任务构建器接到 ALL 自己的按钮上：
 * 复制文件、后缀校验、错误上报仍然全部由 [rememberMultipleUriImportTaskBuilder] 完成，
 * 所以导入行为与经典模组管理页完全一致，没有第二套导入实现。
 *
 * 选择文件用的是 AndroidX 自己的文件选择器，因此 ALL 不依赖 Zalith 的按钮组件。
 *
 * @return 点击“导入”时调用的函数
 */
@Composable
fun rememberAllModsImportAction(
    modsDir: File,
    onError: AllSubmitError,
    onImported: () -> Unit
): () -> Unit {
    val taskBuilder = rememberAllUriImportTaskBuilder(
        id = "ContentManager.Mods.Import",
        targetDir = modsDir,
        checkExtension = listOf(MOD_EXTENSION),
        submitError = onError,
        onImported = onImported
    )
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            TaskSystem.submitTask(taskBuilder(uris))
        }
    }
    return { picker.launch(MOD_EXTENSION.extensionToMimeType()) }
}
