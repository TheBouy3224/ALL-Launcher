package com.movtery.zalithlauncher.all.launchersettings

import com.movtery.zalithlauncher.all.ui.AllColors
import com.movtery.zalithlauncher.all.ui.AllButton
import com.movtery.zalithlauncher.all.ui.allStyleForColor
import com.movtery.zalithlauncher.all.ui.AllButtonStyle
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import com.movtery.zalithlauncher.all.ui.AlertDialog
import com.movtery.zalithlauncher.all.ui.OutlinedTextField
import androidx.compose.material3.Text
import com.movtery.zalithlauncher.all.ui.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.layer_controller.data.lang.createTranslatable
import com.movtery.layer_controller.layout.EmptyControlLayout
import com.movtery.layer_controller.layout.EmptyLayoutInfo
import com.movtery.layer_controller.utils.NAME_LENGTH
import com.movtery.layer_controller.utils.newRandomFileName
import com.movtery.layer_controller.utils.saveToFile
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.bridge.AllSubmitError
import com.movtery.zalithlauncher.all.bridge.allStartControlEditor
import com.movtery.zalithlauncher.contract.extensionToMimeType
import com.movtery.zalithlauncher.game.control.ControlData
import com.movtery.zalithlauncher.game.control.ControlManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.Locale

/**
 * ALL Controls tab (PHASE 3.5d).
 *
 * Backend = Zalith's [ControlManager] (layout files in DIR_CONTROL_LAYOUTS, dataList / selectedLayout /
 * isRefreshing flows, selectControl, deleteControl, saveControl, importControl) and the layer_controller model.
 * Copy = same as Classic (pack the layout, write a new random-named json). Rename edits the layout's default name and
 * saves it through ControlManager.saveControl. The layout editor is still Zalith's ControlEditorActivity,
 * opened through the bridge ([allStartControlEditor]); the list is refreshed when the user comes back.
 * Not ported: translated names, author/description/version editing, share, gamepad button mapping (needs a device).
 */
@Composable
internal fun ControlSettings(onError: AllSubmitError, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val list by ControlManager.dataList.collectAsStateWithLifecycle()
    val selected by ControlManager.selectedLayout.collectAsStateWithLifecycle()
    val refreshing by ControlManager.isRefreshing.collectAsStateWithLifecycle()
    val locale = remember { Locale.getDefault() }
    //three cards per row on wide screens keeps the previews small enough to see several layouts at once
    val cols = if (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 700) 3 else 2

    var creating by remember { mutableStateOf(false) }
    var presetPick by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<ControlData?>(null) }
    var pendingDelete by remember { mutableStateOf<ControlData?>(null) }

    fun fail(e: Throwable) = onError(
        ErrorViewModel.ThrowableMessage(
            androidText(context.getString(R.string.control_manage_failed_to_save)),
            androidText(e.getMessageOrToString())
        )
    )

    //refresh when we come back from Zalith's layout editor (or any other activity)
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) ControlManager.refresh() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri ->
            scope.launch {
                val input = context.contentResolver.openInputStream(uri) ?: return@launch
                ControlManager.importControl(
                    inputStream = input,
                    onSerializationError = { fail(it) },
                    catchedError = { fail(it) },
                    onFinished = { ControlManager.refresh() }
                )
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp).horizontalScroll(rememberScrollState())
            ) {
                CtlPrimary("🎮 " + stringResource(R.string.all_studio_new), onClick = { presetPick = true })
                CtlButton(stringResource(R.string.control_manage_create_new), onClick = { creating = true })
                CtlButton(stringResource(R.string.generic_import), onClick = { importPicker.launch("json".extensionToMimeType()) })
                CtlButton(stringResource(R.string.generic_refresh), onClick = { ControlManager.refresh() })
            }
        }
        if (refreshing && list.isEmpty()) item { Text(stringResource(R.string.generic_loading), color = StDim) }
        else if (list.isEmpty()) item { Text(stringResource(R.string.control_manage_list_empty), color = StDim) }
        //2-3 cards per row; each card = preview thumbnail + name + one main action + a "more" menu
        items(list.chunked(cols), key = { row -> row.first().file.name }) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { data ->
                    val isSel = selected?.file?.name == data.file.name
                    LayoutCard(
                        data = data, isSel = isSel, locale = locale, modifier = Modifier.weight(1f),
                        onSelect = { ControlManager.selectControl(data) },
                        onStudio = { com.movtery.zalithlauncher.all.controls.startControlsStudio(context, data.file) },
                        onEditLayout = { allStartControlEditor(context, data.file) },
                        onShare = { com.movtery.zalithlauncher.utils.file.shareFile(context, data.file) },
                        onRename = { renaming = data },
                        onCopy = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val f = File(PathManager.DIR_CONTROL_LAYOUTS, "${newRandomFileName()}.json")
                                    try { data.controlLayout.pack().saveToFile(f) } catch (e: Exception) { FileUtils.deleteQuietly(f); fail(e) }
                                }
                                ControlManager.refresh()
                            }
                        },
                        onDelete = { pendingDelete = data }
                    )
                }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        item { Text(stringResource(R.string.all_controls_global), color = AllColors.Text, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        item { SwitchRow(stringResource(R.string.settings_control_mouse_physical_mouse_mode_title), null, AllSettings.physicalMouseMode.state) { AllSettings.physicalMouseMode.save(it) } }
        item { SwitchRow(stringResource(R.string.settings_control_mouse_hide_title), null, AllSettings.hideMouse.state) { AllSettings.hideMouse.save(it) } }
        item { SwitchRow(stringResource(R.string.settings_control_mouse_enable_click_title), null, AllSettings.enableMouseClick.state) { AllSettings.enableMouseClick.save(it) } }
        item { StepRow(stringResource(R.string.settings_control_mouse_size_title), AllSettings.mouseSize, 1, "") }
        item { StepRow(stringResource(R.string.settings_control_mouse_sensitivity_title), AllSettings.cursorSensitivity, 5, "%") }
        item { SwitchRow(stringResource(R.string.settings_control_gesture_control_title), null, AllSettings.gestureControl.state) { AllSettings.gestureControl.save(it) } }
        item { SwitchRow(stringResource(R.string.settings_control_gyroscope_title), null, AllSettings.gyroscopeControl.state) { AllSettings.gyroscopeControl.save(it) } }
        item { StepRow(stringResource(R.string.settings_control_gyroscope_sensitivity_title), AllSettings.gyroscopeSensitivity, 5, "%") }
        finerControlRows()
        item { Text(stringResource(R.string.settings_gamepad_title), color = AllColors.Text, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        item { SwitchRow(stringResource(R.string.settings_gamepad_title), null, AllSettings.gamepadControl.state) { AllSettings.gamepadControl.save(it) } }
        item { StepRow(stringResource(R.string.settings_gamepad_deadzone_title), AllSettings.gamepadDeadZoneScale, 5, "%") }
        item { StepRow(stringResource(R.string.settings_gamepad_cursor_sensitivity_title), AllSettings.gamepadCursorSensitivity, 5, "%") }
        item { StepRow(stringResource(R.string.settings_gamepad_camera_sensitivity_title), AllSettings.gamepadCameraSensitivity, 5, "%") }
        item { Spacer(Modifier.padding(bottom = 8.dp)) }
    }

    if (presetPick) {
        val thai = locale.language == "th"
        val names = mapOf(
            com.movtery.zalithlauncher.all.controls.StudioPreset.STANDARD to stringResource(R.string.all_studio_preset_standard),
            com.movtery.zalithlauncher.all.controls.StudioPreset.PVP to stringResource(R.string.all_studio_preset_pvp),
            com.movtery.zalithlauncher.all.controls.StudioPreset.BLANK to stringResource(R.string.all_studio_preset_blank)
        )
        AlertDialog(
            onDismissRequest = { presetPick = false },
            title = { Text(stringResource(R.string.all_studio_new)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.all_studio_new_hint), fontSize = 12.sp, color = StDim)
                    names.forEach { (preset, label) ->
                        CtlButton(label, onClick = {
                            presetPick = false
                            scope.launch {
                                //always a new file: an existing profile is never overwritten
                                val f = File(PathManager.DIR_CONTROL_LAYOUTS, "${newRandomFileName()}.json")
                                val ok = withContext(Dispatchers.IO) {
                                    runCatching { com.movtery.zalithlauncher.all.controls.presetLayout(preset, label, thai).saveToFile(f) }
                                        .onFailure { FileUtils.deleteQuietly(f); fail(it) }.isSuccess
                                }
                                ControlManager.refresh()
                                if (ok) com.movtery.zalithlauncher.all.controls.startControlsStudio(context, f)
                            }
                        })
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { presetPick = false }) { Text(stringResource(R.string.generic_cancel)) } }
        )
    }

    if (creating) NameDialog(
        title = stringResource(R.string.control_manage_create_new),
        initial = "",
        onDismiss = { creating = false },
        onConfirm = { name ->
            creating = false
            scope.launch {
                withContext(Dispatchers.IO) {
                    val layout = EmptyControlLayout.copy(info = EmptyLayoutInfo.copy(name = createTranslatable(default = name)))
                    val f = File(PathManager.DIR_CONTROL_LAYOUTS, "${newRandomFileName()}.json")
                    try { layout.saveToFile(f) } catch (e: Exception) { FileUtils.deleteQuietly(f); fail(e) }
                }
                ControlManager.refresh()
            }
        }
    )
    renaming?.let { data ->
        NameDialog(
            title = stringResource(R.string.generic_rename),
            initial = data.controlLayout.info.name.default,
            onDismiss = { renaming = null },
            onConfirm = { name ->
                renaming = null
                data.controlLayout.info.name.default = name
                ControlManager.saveControl(data) { fail(it) }
            }
        )
    }
    pendingDelete?.let { data ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.generic_warning)) },
            text = { Text(stringResource(R.string.control_manage_delete_message, data.controlLayout.info.name.translate(locale))) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    //re-check at confirm time: never delete the layout that is currently selected
                    if (ControlManager.selectedLayout.value?.file?.name != data.file.name) ControlManager.deleteControl(data)
                }) { Text(stringResource(R.string.generic_confirm), color = AllColors.Danger) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.generic_cancel)) } }
        )
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(text, { if (it.length <= NAME_LENGTH) text = it }, singleLine = true) },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text.trim()) }) { Text(stringResource(R.string.generic_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.generic_cancel)) } }
    )
}

@Composable
private fun CtlButton(text: String, onClick: () -> Unit, color: Color = AllColors.Text) =
    AllButton(text, onClick, style = allStyleForColor(color), compact = true)

/** The one solid-blue button of the tab (Controls Studio). */
@Composable
private fun CtlPrimary(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(AllColors.Blue).clickable(onClick = onClick).heightIn(min = 44.dp).padding(horizontal = 16.dp)
    ) { Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1) }
}

/** A rectangle (or circle) of the layout preview, in 0..1 coordinates of the screen. */
private class PreviewShape(val x: Float, val y: Float, val w: Float, val h: Float, val circle: Boolean)

//ponytail: approximate preview: assumes an 800x360dp landscape screen and ignores layers' conditional visibility, custom shapes and styles
private const val REF_W = 800f
private const val REF_H = 360f

private fun previewShapes(layout: com.movtery.layer_controller.layout.ControlLayout): List<PreviewShape> = buildList {
    layout.layers.filterNot { it.hide }.forEach { layer ->
        layer.normalButtons.forEach { b ->
            val size = b.buttonSize
            fun dim(dp: Float, pct: Int, ref: com.movtery.layer_controller.data.ButtonSize.Reference, total: Float): Float = when (size.type) {
                com.movtery.layer_controller.data.ButtonSize.Type.Dp -> dp / total
                com.movtery.layer_controller.data.ButtonSize.Type.Percentage ->
                    pct / 10000f * (if (ref == com.movtery.layer_controller.data.ButtonSize.Reference.ScreenWidth) REF_W else REF_H) / total
                else -> 60f / total
            }
            val w = dim(size.widthDp, size.widthPercentage, size.widthReference, REF_W).coerceIn(0.02f, 1f)
            val h = dim(size.heightDp, size.heightPercentage, size.heightReference, REF_H).coerceIn(0.04f, 1f)
            add(PreviewShape(b.position.xPercentage() * (1f - w), b.position.yPercentage() * (1f - h), w, h, false))
        }
        layer.joystickButtons.forEach { j ->
            val d = (if (j.sizeType == com.movtery.layer_controller.data.ButtonSize.Type.Dp) j.sizeDp else j.sizePercentage / 10000f * REF_H).coerceIn(20f, REF_H)
            val w = d / REF_W; val h = d / REF_H
            add(PreviewShape(j.position.xPercentage() * (1f - w), j.position.yPercentage() * (1f - h), w, h, true))
        }
    }
}

@Composable
private fun LayoutPreview(data: ControlData, modifier: Modifier = Modifier) {
    val shapes = remember(data.file, data.controlLayout) { if (data.isSupport) runCatching { previewShapes(data.controlLayout.pack()) }.getOrDefault(emptyList()) else emptyList() }
    androidx.compose.foundation.Canvas(modifier.fillMaxWidth().androidxAspect().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(Color(0xFF232B3D))) {
        val fill = Color(0xFF3B4C6E)
        shapes.forEach { s ->
            val o = androidx.compose.ui.geometry.Offset(s.x * size.width, s.y * size.height)
            val sz = androidx.compose.ui.geometry.Size(s.w * size.width, s.h * size.height)
            if (s.circle) drawOval(fill, o, sz) else drawRoundRect(fill, o, sz, androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
        }
    }
}

private fun Modifier.androidxAspect(): Modifier = this.then(Modifier.aspectRatio(REF_W / REF_H))

@Composable
private fun LayoutCard(
    data: ControlData, isSel: Boolean, locale: Locale, modifier: Modifier,
    onSelect: () -> Unit, onStudio: () -> Unit, onEditLayout: () -> Unit, onShare: () -> Unit,
    onRename: () -> Unit, onCopy: () -> Unit, onDelete: () -> Unit
) {
    val name = if (data.isSupport) data.controlLayout.info.name.translate(locale) else data.file.name
    val author = if (data.isSupport) data.controlLayout.info.author.translate(locale) else ""
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Column(modifier.clip(shape).background(StCard).border(1.dp, if (isSel) AllColors.Green else StBorder, shape)) {
        Box {
            LayoutPreview(data)
            if (isSel) Text(
                stringResource(R.string.all_acc_in_use), color = AllColors.Ok, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1E3A2A)).padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Column(Modifier.padding(10.dp)) {
            Text(name, color = AllColors.Text, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(author.takeIf { it.isNotBlank() }, if (!data.isSupport) stringResource(R.string.control_manage_info_unsupport) else null, data.file.name).joinToString(" · "),
                color = StDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (data.isSupport) {
                    //main action: the layout in use opens in Studio, any other one is selected first
                    CtlCardBtn(
                        text = if (isSel) "✏ " + stringResource(R.string.all_studio_open) else stringResource(R.string.all_accounts_select),
                        primary = !isSel, modifier = Modifier.weight(1f), onClick = if (isSel) onStudio else onSelect
                    )
                    if (!isSel) CtlCardBtn("✏", primary = false, onClick = onStudio)
                } else Spacer(Modifier.weight(1f))
                Box {
                    CtlCardBtn("⋯", primary = false, onClick = { menu = true })
                    com.movtery.zalithlauncher.all.ui.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (data.isSupport) {
                            com.movtery.zalithlauncher.all.ui.DropdownMenuItem({ Text(stringResource(R.string.control_manage_info_edit)) }, { menu = false; onEditLayout() })
                            com.movtery.zalithlauncher.all.ui.DropdownMenuItem({ Text(stringResource(R.string.generic_share)) }, { menu = false; onShare() })
                            com.movtery.zalithlauncher.all.ui.DropdownMenuItem({ Text(stringResource(R.string.generic_rename)) }, { menu = false; onRename() })
                            com.movtery.zalithlauncher.all.ui.DropdownMenuItem({ Text(stringResource(R.string.all_controls_copy)) }, { menu = false; onCopy() })
                        }
                        // the layout in use cannot be removed from here (Classic lets you, then silently falls back to another)
                        if (!isSel) com.movtery.zalithlauncher.all.ui.DropdownMenuItem({ Text(stringResource(R.string.generic_delete), color = AllColors.Danger) }, { menu = false; onDelete() })
                    }
                }
            }
        }
    }
}

@Composable
private fun CtlCardBtn(text: String, primary: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(if (primary) AllColors.Blue else Color(0xFF232B3D)).clickable(onClick = onClick).heightIn(min = 40.dp).padding(horizontal = 12.dp)
    ) { Text(text, color = if (primary) Color.White else AllColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1) }
}
