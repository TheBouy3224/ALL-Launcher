package com.movtery.zalithlauncher.all.launchersettings

import com.movtery.zalithlauncher.all.ui.AllColors
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.ui.AllButton
import com.movtery.zalithlauncher.all.ui.AllButtonStyle
import com.movtery.zalithlauncher.bridge.CursorShape
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.unit.ParcelableSettingUnit
import com.movtery.zalithlauncher.ui.control.mouse.CENTER_HOTSPOT
import com.movtery.zalithlauncher.ui.control.mouse.CursorHotspot
import com.movtery.zalithlauncher.ui.control.mouse.LEFT_TOP_HOTSPOT
import com.movtery.zalithlauncher.ui.control.mouse.MousePointer
import com.movtery.zalithlauncher.ui.control.mouse.arrowPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.crossHairPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.iBeamPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.linkPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.notAllowedPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.resizeAllPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.resizeEWPointerFile
import com.movtery.zalithlauncher.ui.control.mouse.resizeNSPointerFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val CuText: Color get() = AllColors.Text
private val CuDanger: Color get() = AllColors.Danger
private val CuOk: Color get() = AllColors.Ok

internal class CursorEntry(
    val shape: CursorShape,
    val titleRes: Int,
    val file: File,
    val hotspot: ParcelableSettingUnit<CursorHotspot>,
    val defaultHotspot: CursorHotspot
)

internal fun cursorEntries() = listOf(
    CursorEntry(CursorShape.Arrow, R.string.settings_control_mouse_pointer_arrow_title, arrowPointerFile, AllSettings.arrowMouseHotspot, LEFT_TOP_HOTSPOT),
    CursorEntry(CursorShape.Hand, R.string.settings_control_mouse_pointer_link_title, linkPointerFile, AllSettings.linkMouseHotspot, CursorHotspot(23, 0)),
    CursorEntry(CursorShape.IBeam, R.string.settings_control_mouse_pointer_ibeam_title, iBeamPointerFile, AllSettings.iBeamMouseHotspot, CENTER_HOTSPOT),
    CursorEntry(CursorShape.CrossHair, R.string.settings_control_mouse_pointer_crosshair_title, crossHairPointerFile, AllSettings.crossHairMouseHotspot, CENTER_HOTSPOT),
    CursorEntry(CursorShape.ResizeNS, R.string.settings_control_mouse_pointer_resize_ns_title, resizeNSPointerFile, AllSettings.resizeNSMouseHotspot, CENTER_HOTSPOT),
    CursorEntry(CursorShape.ResizeEW, R.string.settings_control_mouse_pointer_resize_ew_title, resizeEWPointerFile, AllSettings.resizeEWMouseHotspot, CENTER_HOTSPOT),
    CursorEntry(CursorShape.ResizeAll, R.string.settings_control_mouse_pointer_resize_all_title, resizeAllPointerFile, AllSettings.resizeAllMouseHotspot, CENTER_HOTSPOT),
    CursorEntry(CursorShape.NotAllowed, R.string.settings_control_mouse_pointer_not_allowed_title, notAllowedPointerFile, AllSettings.notAllowedMouseHotspot, CENTER_HOTSPOT)
)

/** Reads at most [CURSOR_MAX_FILE_BYTES] + 1 bytes so a huge file is rejected without loading it */
private fun readLimited(context: android.content.Context, uri: Uri): ByteArray? =
    context.contentResolver.openInputStream(uri)?.use { input ->
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        while (out.size() <= CURSOR_MAX_FILE_BYTES) {
            val n = input.read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
        }
        out.toByteArray()
    }

/**
 * ALL Cursor tab: the same cursor files, hotspot settings and size setting that Zalith's virtual pointer (MousePointer) uses.
 * Changes are staged and only written by Apply; Cancel throws them away; Apply keeps one backup that "Restore previous" swaps back.
 */
@Composable
internal fun CursorSettings(modifier: Modifier = Modifier) {
    val entries = remember { cursorEntries() }
    var editing by remember { mutableStateOf<CursorEntry?>(null) }
    var rev by remember { mutableIntStateOf(0) }
    var openPack by remember { mutableStateOf<Pair<CursorPack, PackImport?>?>(null) }

    openPack?.let { (pack, import) ->
        CursorPackDetail(pack, import, modifier, onClose = { openPack = null; rev++ })
        return
    }

    editing?.let { entry ->
        CursorEditor(entry, modifier, onClose = { editing = null; rev++ })
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(StSurface).padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(Modifier.padding(top = 2.dp)) }
        item { StepRow(stringResource(R.string.settings_control_mouse_size_title), AllSettings.mouseSize, 1, " dp") }
        item {
            Card {
                Text(stringResource(R.string.all_cursor_animation), color = CuText, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.all_cursor_animation_hint), color = StDim, fontSize = 12.sp)
            }
        }
        item { SwitchRow(stringResource(R.string.all_cursor_loop), null, AllSettings.cursorAnimLoop.state) { AllSettings.cursorAnimLoop.save(it) } }
        item { StepRow(stringResource(R.string.all_cursor_speed), AllSettings.cursorAnimSpeed, 25, "%") }
        item { CursorPackSection(rev, onOpen = { p, i -> openPack = p to i }, onChanged = { rev++ }) }
        items(entries, key = { it.shape.name }) { entry ->
            val custom = remember(rev) { entry.file.isFile }
            Card {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(40.dp).background(AllColors.Disabled), contentAlignment = Alignment.Center) {
                        MousePointer(mouseSize = 32.dp, cursorShape = entry.shape, mouseFile = entry.file, centerIcon = true, triggerRefresh = rev, animate = false)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(entry.titleRes), color = CuText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(stringResource(if (custom) R.string.all_cursor_custom else R.string.all_cursor_builtin), color = StDim, fontSize = 12.sp)
                    }
                    AllButton(stringResource(R.string.all_cursor_customize), { editing = entry }, compact = true)
                }
            }
        }
        item { Spacer(Modifier.padding(bottom = 8.dp)) }
    }
}

@Composable
private fun CursorEditor(entry: CursorEntry, modifier: Modifier, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(entry) { CursorStore(entry.file) }
    var rev by remember { mutableIntStateOf(0) }
    var toDefault by remember { mutableStateOf(false) }
    var hotspot by remember { mutableStateOf(entry.hotspot.getValue()) }
    var message by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var busy by remember { mutableStateOf(false) }

    //a staged copy left over from an interrupted session is not applied silently
    remember(entry) { store.discard(); 0 }

    val staged = remember(rev) { store.hasStaged() }
    val dirty = staged || toDefault || hotspot != entry.hotspot.getValue()
    val previewFile = when {
        toDefault -> null
        staged -> store.staged
        else -> entry.file.takeIf { it.isFile }
    }

    val errorText = mapOf(
        CursorImportError.TOO_LARGE to stringResource(R.string.all_cursor_err_large),
        CursorImportError.UNSUPPORTED to stringResource(R.string.all_cursor_err_format),
        CursorImportError.CORRUPT to stringResource(R.string.all_cursor_err_corrupt),
        CursorImportError.TOO_BIG_SIDE to stringResource(R.string.all_cursor_err_side),
        CursorImportError.ANIMATION_TOO_BIG to stringResource(R.string.all_cursor_err_anim)
    )
    val stagedOk = stringResource(R.string.all_cursor_staged)
    val appliedOk = stringResource(R.string.all_cursor_applied)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = readLimited(context, uri) ?: return@runCatching CursorCheck(CursorImportError.CORRUPT)
                    checkCursorBytes(bytes).also { if (it.error == null) store.stage(bytes) }
                }.getOrElse { CursorCheck(CursorImportError.CORRUPT) }
            }
            busy = false
            if (result.error == null) {
                toDefault = false
                //.ani / .cur carry their own hotspot: start from it (the user can still tap to change it)
                result.hotspot?.let { hotspot = it }
                message = stagedOk to true
            } else {
                message = errorText.getValue(result.error) to false
            }
            rev++
        }
    }

    Column(modifier.fillMaxSize().background(StSurface)) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(entry.titleRes), color = CuText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                //the preview box maps 1:1 to the in-game cursor box: hotspot % of its size, image drawn from the top-left
                Box(
                    Modifier.size(160.dp).background(AllColors.Disabled).border(1.dp, StBorder)
                        .pointerInput(Unit) {
                            detectTapGestures { p: Offset ->
                                hotspot = CursorHotspot(
                                    xPercent = (p.x / size.width * 100).toInt().coerceIn(0, 100),
                                    yPercent = (p.y / size.height * 100).toInt().coerceIn(0, 100)
                                )
                            }
                        }
                ) {
                    MousePointer(mouseSize = 160.dp, cursorShape = entry.shape, mouseFile = previewFile, triggerRefresh = rev)
                    Canvas(Modifier.size(160.dp)) {
                        val c = Offset(size.width * hotspot.xPercent / 100f, size.height * hotspot.yPercent / 100f)
                        drawLine(Color.Red, c.copy(x = c.x - 12), c.copy(x = c.x + 12), 2f)
                        drawLine(Color.Red, c.copy(y = c.y - 12), c.copy(y = c.y + 12), 2f)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.all_cursor_hotspot_hint), color = StDim, fontSize = 12.sp)
                    Text(stringResource(R.string.all_cursor_hotspot_value, hotspot.xPercent, hotspot.yPercent), color = CuText, fontSize = 13.sp)
                    Text(stringResource(R.string.all_cursor_limits), color = StDim, fontSize = 12.sp)
                    message?.let { (text, ok) -> Text(text, color = if (ok) CuOk else CuDanger, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AllButton(stringResource(R.string.all_cursor_import), { picker.launch(arrayOf("image/*", "application/octet-stream", "application/x-navi-animation", "*/*")) }, style = AllButtonStyle.Primary, compact = true, enabled = !busy)
                AllButton(stringResource(R.string.generic_reset), {
                    store.discard(); toDefault = true; hotspot = entry.defaultHotspot; message = null; rev++
                }, compact = true, enabled = !busy)
                AllButton(stringResource(R.string.all_cursor_restore), {
                    runCatching { store.restore() }
                    message = null; rev++
                }, compact = true, enabled = !busy && !dirty && store.hasBackup())
            }
        }
        Row(
            Modifier.fillMaxWidth().background(StCard).padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(if (dirty) R.string.all_gamepad_unsaved_changes else R.string.all_gamepad_all_saved),
                color = if (dirty) StAccent else StDim, fontSize = 12.sp, modifier = Modifier.weight(1f)
            )
            AllButton(stringResource(R.string.all_setup_cancel), { store.discard(); onClose() }, compact = true, enabled = !busy)
            AllButton(stringResource(R.string.all_cursor_apply), {
                runCatching {
                    store.apply(toDefault)
                    entry.hotspot.save(hotspot)
                    toDefault = false
                    message = appliedOk to true
                }.onFailure { message = (it.message ?: it::class.java.simpleName) to false }
                rev++
            }, style = AllButtonStyle.Success, compact = true, enabled = dirty && !busy)
        }
    }
}
