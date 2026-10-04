package com.movtery.zalithlauncher.all.launchersettings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.movtery.zalithlauncher.all.ui.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.bridge.AllSubmitError
import com.movtery.zalithlauncher.context.getFileName
import com.movtery.zalithlauncher.contract.extensionToMimeType
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.multirt.Runtime
import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.utils.file.checkExtensionOrThrow
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.IOException

/**
 * ALL Java Manager (PHASE 3.5c), the "Java" tab of the launcher settings window.
 *
 * Backend is Zalith's [RuntimesManager] (runtime folder, release-file parsing, tar.xz install, removal) and the
 * global [AllSettings.javaRuntime] / [AllSettings.autoPickJavaRuntime]. Same install/delete tasks as Classic;
 * additions: a runtime that is currently the default cannot be deleted, an import never overwrites an existing
 * runtime folder, and a built-in compatible runtime is not deletable (Classic rule).
 * Not here: "run a .jar with a runtime" (Classic-only tool), per-instance runtime (instance Settings window).
 */
@Composable
internal fun JavaSettings(onError: AllSubmitError, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var runtimes by remember { mutableStateOf(RuntimesManager.getRuntimes()) }
    fun refresh() { runtimes = RuntimesManager.getRuntimes(forceLoad = true) }

    var pendingDelete by remember { mutableStateOf<Runtime?>(null) }
    var blocked by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { importRuntime(context, it, ::refresh, onError) { blocked = it } }
    }

    val defaultName = AllSettings.javaRuntime.state

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                ActionButton(stringResource(R.string.generic_refresh), onClick = { refresh() })
                ActionButton(stringResource(R.string.generic_import), onClick = { picker.launch("xz".extensionToMimeType()) })
            }
        }
        item {
            SwitchRow(
                stringResource(R.string.settings_game_auto_pick_java_runtime_title),
                stringResource(R.string.settings_game_auto_pick_java_runtime_summary),
                AllSettings.autoPickJavaRuntime.state
            ) { AllSettings.autoPickJavaRuntime.save(it) }
        }
        if (runtimes.isEmpty()) {
            item { Text(stringResource(R.string.multirt_runtime_corrupt), color = StDim) }
        }
        items(runtimes, key = { it.name }) { rt ->
            val isDefault = rt.name == defaultName
            RuntimeCard(
                runtime = rt,
                isDefault = isDefault,
                onSetDefault = { AllSettings.javaRuntime.save(if (isDefault) "" else rt.name) },
                onDelete = {
                    if (isDefault) blocked = rt.name else pendingDelete = rt
                }
            )
        }
        item { Spacer(Modifier.padding(bottom = 8.dp)) }
    }

    pendingDelete?.let { rt ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.generic_warning)) },
            text = { Text(stringResource(R.string.multirt_runtime_delete_message, rt.name)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    //re-check at the moment of confirmation: the default may have changed while the dialog was open
                    if (rt.name == AllSettings.javaRuntime.getValue()) blocked = rt.name
                    else deleteRuntime(rt, ::refresh, onError, context.getString(R.string.multirt_runtime_delete_failed))
                }) { Text(stringResource(R.string.generic_confirm), color = Color(0xFFB3261E)) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.generic_cancel)) } }
        )
    }
    blocked?.let { msg ->
        AlertDialog(
            onDismissRequest = { blocked = null },
            title = { Text(stringResource(R.string.generic_warning)) },
            text = { Text(if (msg.contains(' ') || msg.contains('/')) msg else stringResource(R.string.all_java_in_use, msg)) },
            confirmButton = { TextButton(onClick = { blocked = null }) { Text(stringResource(R.string.generic_confirm)) } }
        )
    }
}

private fun deleteRuntime(rt: Runtime, refresh: () -> Unit, onError: AllSubmitError, failedTitle: String) {
    TaskSystem.submitTask(
        Task.runTask(
            id = rt.name,
            dispatcher = Dispatchers.IO,
            task = { task ->
                task.updateMessage(androidText(R.string.multirt_runtime_deleting, rt.name))
                RuntimesManager.removeRuntime(rt.name)
            },
            onError = {
                onError(ErrorViewModel.ThrowableMessage(androidText(failedTitle), androidText(it.getMessageOrToString())))
            },
            onFinally = refresh
        )
    )
}

private fun importRuntime(
    context: android.content.Context,
    uri: Uri,
    refresh: () -> Unit,
    onError: AllSubmitError,
    block: (String) -> Unit
) {
    val name = context.getFileName(uri) ?: run {
        block(context.getString(R.string.multirt_runtime_import_failed_file_name)); return
    }
    //RuntimesManager.installRuntime deletes an existing folder of the same name first: never let an import replace a runtime silently
    if (File(PathManager.DIR_MULTIRT, name).exists()) {
        block(context.getString(R.string.multirt_runtime_import_failed) + ": " + name + " already exists"); return
    }
    TaskSystem.submitTask(
        Task.runTask(
            id = name,
            dispatcher = Dispatchers.IO,
            task = { task ->
                name.checkExtensionOrThrow(listOf("xz"))
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IOException(context.getString(R.string.multirt_runtime_import_failed_input_stream))
                RuntimesManager.installRuntime(
                    nativeLibDir = PathManager.DIR_NATIVE_LIB,
                    inputStream = input,
                    name = name,
                    updateProgress = { textRes, args -> task.updateMessage(androidText(textRes, *args)) }
                )
            },
            onError = {
                onError(
                    ErrorViewModel.ThrowableMessage(
                        androidText(context.getString(R.string.multirt_runtime_import_failed)),
                        androidText(it.getMessageOrToString())
                    )
                )
            },
            onFinally = refresh,
            onCancel = {
                //same cleanup as Classic: remove the half-extracted folder (it did not exist before this import)
                runCatching { RuntimesManager.removeRuntime(name) }
                refresh()
            }
        )
    )
}

@Composable
private fun RuntimeCard(runtime: Runtime, isDefault: Boolean, onSetDefault: () -> Unit, onDelete: () -> Unit) {
    val deletable = !runtime.isProvidedByLauncher || !runtime.isCompatible()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isDefault) Color(0xFFE8F1E8) else StCard)
            .border(1.dp, if (isDefault) Color(0xFF2E7D32) else StBorder, RoundedCornerShape(6.dp))
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(runtime.name, color = Color(0xFF1B1B1B), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val lines = listOfNotNull(
                if (runtime.isProvidedByLauncher) stringResource(R.string.multirt_runtime_provided_by_launcher) else null,
                if (isDefault) "✓ " + stringResource(R.string.settings_game_java_runtime_title) else null,
                runtime.versionString?.let { stringResource(R.string.multirt_runtime_version_name, it) }
                    ?: stringResource(R.string.multirt_runtime_corrupt),
                if (runtime.javaVersion != 0) stringResource(R.string.multirt_runtime_version_code, runtime.javaVersion.toString()) else null,
                runtime.arch?.let {
                    if (runtime.isCompatible()) stringResource(R.string.multirt_runtime_version_arch, it)
                    else stringResource(R.string.multirt_runtime_incompatible_arch, it)
                }
            )
            Text(lines.joinToString(" · "), color = StDim, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        if (runtime.isCompatible() && runtime.versionString != null) {
            ActionButton(
                stringResource(if (isDefault) R.string.all_java_unset_default else R.string.all_java_set_default),
                onSetDefault
            )
        }
        if (deletable) {
            Spacer(Modifier.width(6.dp))
            ActionButton(stringResource(R.string.generic_delete), onDelete, Color(0xFFB3261E))
        }
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit, color: Color = Color(0xFF1B1B1B)) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(StCard)
            .border(1.dp, if (color == Color(0xFF1B1B1B)) StBorder else color, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
    ) { Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1) }
}
