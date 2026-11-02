package com.movtery.zalithlauncher.all.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.inputmap.keycodes.ControlEventKeyName
import com.movtery.layer_controller.data.JoystickData
import com.movtery.layer_controller.data.NormalData
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.layer_controller.layout.ControlLayout
import com.movtery.layer_controller.utils.saveToFile
import com.movtery.layer_controller.utils.widgetPosition
import com.movtery.zalithlauncher.R
import com.movtery.layer_controller.data.ButtonPosition
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import com.movtery.zalithlauncher.all.ui.AllButton
import com.movtery.zalithlauncher.all.ui.AllButtonStyle
import com.movtery.zalithlauncher.all.ui.AllDialog
import com.movtery.zalithlauncher.all.ui.OutlinedTextField
import com.movtery.zalithlauncher.game.control.ControlManager
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

private val StudioBar = Brush.verticalGradient(listOf(Color(0xF011151F), Color(0xF011151F)))
private val SelectBlue = Color(0xFF6F9BFF)
private val WarnOrange = Color(0xFFFF9800)

/** A widget waiting for the user to tap where it goes (no free space was found) */
private class PendingPlace(val w: Float, val h: Float, val make: (ButtonPosition) -> Pair<ControlLayout, String>)

private enum class StudioMode { EDIT, TRY }

/**
 * ALL Controls Studio V1: the whole screen is the game area (same size and the same position/size rules as the game), a compact
 * XP toolbar floats on top. Edits go into a draft with Undo; only Save writes the profile file.
 */
@Composable
fun AllControlsStudio(file: File, initial: ControlLayout, onExit: () -> Unit) {
    val thai = remember { Locale.getDefault().language == "th" }
    val history = remember { StudioHistory(initial) }
    var rev by remember { mutableIntStateOf(0) }
    val layout = remember(rev) { history.current }
    fun commit(next: ControlLayout) { history.push(next); rev++ }

    var mode by remember { mutableStateOf(StudioMode.EDIT) }
    var selected by remember { mutableStateOf<String?>(null) }
    var settingsFor by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var addingCustom by remember { mutableStateOf(false) }
    var addingSendText by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    var toolbarHidden by remember { mutableStateOf(false) }
    //true while a widget is being dragged: the quick bar waits until it lands
    var dragging by remember { mutableStateOf(false) }
    //first-run tip: shown until "Got it"; the help button brings it back for this session
    var tipOpen by remember { mutableStateOf(!com.movtery.zalithlauncher.setting.AllSettings.allStudioTipSeen.state) }
    var lastPressed by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var savedNotice by remember { mutableStateOf(false) }
    //layout helpers (snap lines, HUD markers, overlap warnings): one switch, on by default
    var helpers by remember { mutableStateOf(true) }
    var guide by remember { mutableStateOf<Snap?>(null) }
    //no free space: the next tap on an empty spot places the pending widget there
    var placing by remember { mutableStateOf<PendingPlace?>(null) }
    var screen by remember { mutableStateOf(Size.Zero) }
    val scope = rememberCoroutineScope()
    val densityOf = LocalDensity.current.density

    fun save(after: () -> Unit = {}) {
        val toSave = history.current
        scope.launch {
            runCatching { toSave.saveToFile(file) }
                .onSuccess { history.markSaved(); rev++; savedNotice = true; ControlManager.refresh(); after() }
                .onFailure { saveError = it.message ?: it::class.java.simpleName }
        }
    }

    //copy a button (also used by the quick bar): right next to the original if there is room, else the nearest free spot, else the user picks
    fun copyWidget(id: String) {
        val b = history.current.findButton(id) ?: return
        val sw = screen.width; val sh = screen.height
        val (w, h) = buttonPixelSize(b.buttonSize, sw, sh, densityOf)
        val src = history.current.widgetRects(sw, sh, densityOf)[id]
        val make = { pos: ButtonPosition -> history.current.duplicateButton(id, pos)!! }
        val spot = history.current.spotForNew(w, h, sw, sh, densityOf, src?.let { Offset(it.right + 8 * densityOf, it.top) })
        if (spot != null) { val (next, nid) = make(percentPosition(spot.x, spot.y, w, h, sw, sh)); commit(next); selected = nid }
        else placing = PendingPlace(w, h, make)
    }

    //Back = the close button: asks before throwing edits away
    androidx.activity.compose.BackHandler { if (mode == StudioMode.TRY) mode = StudioMode.EDIT else if (history.isDirty()) confirmExit = true else onExit() }

    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3B4A68), Color(0xFF313E5A))))) {
        val density = LocalDensity.current.density
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        SideEffect { screen = Size(screenW, screenH) }
        val rects = remember(rev, screenW, screenH) { layout.widgetRects(screenW, screenH, density) }
        val hud = remember(screenW, screenH) { listOf(hotbarZone(screenW, screenH), crosshairZone(screenW, screenH)) }
        val warned = if (helpers && mode == StudioMode.EDIT) overlapping(rects, hud) else emptySet()
        //snap a dragged rectangle to the other widgets (never to itself); null when helpers are off
        val snapper: ((String, Rect) -> Snap)? = if (helpers) { id, r -> snapRect(r, rects.filterKeys { it != id }.values.toList(), screenW, screenH, 8 * density) } else null
        //faint grid so positions are easy to judge
        Canvas(Modifier.fillMaxSize()) {
            val step = size.height / 10f
            var x = 0f
            while (x < size.width) { drawLine(Color.White.copy(alpha = 0.07f), Offset(x, 0f), Offset(x, size.height)); x += step }
            var y = 0f
            while (y < size.height) { drawLine(Color.White.copy(alpha = 0.07f), Offset(0f, y), Offset(size.width, y)); y += step }
        }
        //tap on empty space clears the selection
        Box(Modifier.fillMaxSize().pointerInput(mode, placing) {
            detectTapGestures { tap ->
                val p = placing
                if (p != null) {
                    val left = (tap.x - p.w / 2).coerceIn(0f, (screenW - p.w).coerceAtLeast(0f))
                    val top = (tap.y - p.h / 2).coerceIn(0f, (screenH - p.h).coerceAtLeast(0f))
                    val (next, id) = p.make(percentPosition(left, top, p.w, p.h, screenW, screenH))
                    commit(next); selected = id; placing = null
                } else if (mode == StudioMode.EDIT) selected = null
            }
        })
        //helpers: where the game's hotbar and crosshair are, plus the live snap lines
        if (helpers && mode == StudioMode.EDIT) {
            Canvas(Modifier.fillMaxSize()) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                val hb = hud[0]
                drawRect(Color(0x33FFFFFF), hb.topLeft, hb.size)
                drawRect(Color(0xCCFFFFFF), hb.topLeft, hb.size, style = Stroke(2f, pathEffect = dash))
                val c = hud[1].center
                val r = hud[1].width / 2
                drawLine(Color.White, Offset(c.x - r, c.y), Offset(c.x + r, c.y), 3f)
                drawLine(Color.White, Offset(c.x, c.y - r), Offset(c.x, c.y + r), 3f)
                guide?.xLine?.let { drawLine(Color(0xFFFF4FD8), Offset(it, 0f), Offset(it, size.height), 2f) }
                guide?.yLine?.let { drawLine(Color(0xFFFF4FD8), Offset(0f, it), Offset(size.width, it), 2f) }
            }
            Text(stringResource(R.string.all_studio_hotbar), color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp,
                modifier = Modifier.offset { IntOffset(hud[0].left.roundToInt() + 6, (hud[0].top - 16 * density).roundToInt()) })
        }

        layout.layers.filterNot { it.hide }.forEach { layer ->
            layer.joystickButtons.forEach { j ->
                key2(j.uuid) {
                    StudioJoystick(j, screenW, screenH, density, mode, selected == j.uuid, j.uuid in warned,
                        snap = snapper?.let { f -> { r: Rect -> dragging = true; f(j.uuid, r).also { guide = it } } }, onDragDone = { guide = null; dragging = false },
                        onSelect = { selected = j.uuid },
                        onOpen = { settingsFor = j.uuid },
                        onMoved = { pos -> commit(history.current.moveWidget(j.uuid, pos)) },
                        onResized = { side -> commit(history.current.resizeJoystick(j.uuid, side, screenH)) },
                        onDelete = { commit(history.current.removeWidget(j.uuid)); selected = null },
                        onPressed = { lastPressed = StudioAction.MOVE.label(thai) + " (W A S D)" })
                }
            }
            layer.normalButtons.forEach { b ->
                key2(b.uuid) {
                    StudioButton(layout, b, screenW, screenH, density, mode, selected == b.uuid, b.uuid in warned, thai,
                        snap = snapper?.let { f -> { r: Rect -> dragging = true; f(b.uuid, r).also { guide = it } } }, onDragDone = { guide = null; dragging = false },
                        onSelect = { selected = b.uuid },
                        onOpen = { settingsFor = b.uuid },
                        onMoved = { pos -> commit(history.current.moveWidget(b.uuid, pos)) },
                        onResized = { w, h -> commit(history.current.resizeButton(b.uuid, w, h, screenH)) },
                        onDelete = { commit(history.current.removeWidget(b.uuid)); selected = null },
                        onPressed = { lastPressed = it })
                }
            }
        }

        //quick actions: settings / copy / delete next to the selected widget (same handlers as the settings dialog and the old delete)
        val quickId = selected
        if (mode == StudioMode.EDIT && quickId != null && !dragging && placing == null && settingsFor == null) rects[quickId]?.let { r ->
            val isButton = layout.findButton(quickId) != null
            QuickBar(
                target = r, screenW = screenW, screenH = screenH, density = density, withCopy = isButton,
                onSettings = { settingsFor = quickId },
                onCopy = { copyWidget(quickId) },
                onDelete = { commit(history.current.removeWidget(quickId)); selected = null }
            )
        }

        if (layout.layers.all { it.normalButtons.isEmpty() && it.joystickButtons.isEmpty() }) {
            Text(stringResource(R.string.all_studio_empty), color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(24.dp))
        }

        //try mode banner: what was pressed
        if (mode == StudioMode.TRY) {
            Text(
                lastPressed?.let { stringResource(R.string.all_studio_pressed, it) } ?: stringResource(R.string.all_studio_try_hint),
                color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xE611151F)).padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        if (warned.isNotEmpty()) {
            Text("⚠ " + stringResource(R.string.all_studio_overlap), color = Color.White, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xCCE65100)).padding(horizontal = 8.dp, vertical = 3.dp))
        }
        placing?.let {
            Row(Modifier.align(Alignment.Center).clip(RoundedCornerShape(8.dp)).background(Color(0xF011151F)).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.all_studio_place_hint), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                AllButton(stringResource(R.string.generic_cancel), { placing = null }, compact = true)
            }
        }

        //compact XP toolbar (top centre), can be tucked away
        Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            if (!toolbarHidden) Row(
                Modifier.padding(top = 4.dp).clip(RoundedCornerShape(12.dp)).background(StudioBar).border(1.dp, Color(0xFF2D3548), RoundedCornerShape(12.dp))
                    .horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🎮", fontSize = 14.sp)
                Text(
                    initial.info.name.translate().ifBlank { file.nameWithoutExtension } + if (history.isDirty()) " •" else "",
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(110.dp)
                )
                if (mode == StudioMode.EDIT) {
                    AllButton(stringResource(R.string.all_studio_add), { adding = true }, compact = true, style = AllButtonStyle.Primary, icon = R.drawable.ic_add)
                    AllButton(stringResource(R.string.all_studio_undo), { history.undo(); rev++; selected = null }, compact = true, enabled = history.canUndo(), icon = R.drawable.ic_undo)
                    AllButton(stringResource(R.string.all_studio_redo), { history.redo(); rev++; selected = null }, compact = true, enabled = history.canRedo(), icon = R.drawable.ic_redo)
                    AllButton(stringResource(R.string.all_studio_try), { mode = StudioMode.TRY; selected = null; lastPressed = null; placing = null }, compact = true, icon = R.drawable.ic_play_arrow_filled)
                    AllButton("📐", { helpers = !helpers; guide = null }, compact = true, style = if (helpers) AllButtonStyle.Primary else AllButtonStyle.Neutral)
                    AllButton("?", { tipOpen = !tipOpen }, compact = true, style = if (tipOpen) AllButtonStyle.Primary else AllButtonStyle.Neutral)
                } else {
                    AllButton(stringResource(R.string.all_studio_back_to_edit), { mode = StudioMode.EDIT }, compact = true, style = AllButtonStyle.Primary)
                }
                AllButton(stringResource(R.string.generic_save), { save() }, compact = true, style = AllButtonStyle.Primary, enabled = history.isDirty(), icon = R.drawable.ic_save_filled)
                AllButton("✕", { if (history.isDirty()) confirmExit = true else onExit() }, compact = true, style = AllButtonStyle.Danger)
            }
            Text(
                if (toolbarHidden) "▼" else "▲", color = Color.White, fontSize = 12.sp,
                modifier = Modifier.clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)).background(Color(0xCC11151F))
                    .clickable { toolbarHidden = !toolbarHidden }.padding(horizontal = 14.dp, vertical = 1.dp)
            )
        }
        //tip bar: under the toolbar, small, never over the buttons' bottom area; only in edit mode
        if (tipOpen && mode == StudioMode.EDIT && !toolbarHidden) Row(
            Modifier.align(Alignment.TopCenter).padding(top = 64.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xF011151F))
                .border(1.dp, Color(0xFF2D3548), RoundedCornerShape(10.dp)).padding(start = 12.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("① " + stringResource(R.string.all_studio_tip_select), color = Color(0xFFD5DBE8), fontSize = 12.sp, maxLines = 1)
            Text("② " + stringResource(R.string.all_studio_tip_drag), color = Color(0xFFD5DBE8), fontSize = 12.sp, maxLines = 1)
            Text("③ " + stringResource(R.string.all_studio_tip_settings), color = Color(0xFFD5DBE8), fontSize = 12.sp, maxLines = 1)
            AllButton(stringResource(R.string.all_studio_tip_ok), {
                tipOpen = false
                com.movtery.zalithlauncher.setting.AllSettings.allStudioTipSeen.save(true)
            }, compact = true, style = AllButtonStyle.Primary)
        }
        if (savedNotice) {
            Text(stringResource(R.string.all_studio_saved), color = Color.White, fontSize = 12.sp, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xE61E3A2A)).padding(horizontal = 10.dp, vertical = 4.dp))
            androidx.compose.runtime.LaunchedEffect(rev) { kotlinx.coroutines.delay(1800); savedNotice = false }
        }
    }

    if (addingCustom) CustomKeyDialog(
        title = stringResource(R.string.all_studio_group_custom), initialName = "", initialEvents = emptyList(),
        onDismiss = { addingCustom = false }
    ) { name, events ->
        addingCustom = false
        val sw = screen.width; val sh = screen.height
        //build it first (size depends on the label), then drop it on a free spot like every other new button
        val (tmp, id) = history.current.addCustom(name, events, null)
        val (w, h) = buttonPixelSize(tmp.findButton(id)!!.buttonSize, sw, sh, densityOf)
        val make = { pos: ButtonPosition -> tmp.moveWidget(id, pos) to id }
        val spot = history.current.spotForNew(w, h, sw, sh, densityOf, Offset(sw * 0.72f - w / 2, sh * 0.45f - h / 2))
        if (spot != null) { val (next, nid) = make(percentPosition(spot.x, spot.y, w, h, sw, sh)); commit(next); selected = nid }
        else placing = PendingPlace(w, h, make)
    }

    if (addingSendText) SendTextDialog(initialMessage = "", initialName = "", onDismiss = { addingSendText = false }) { message, name ->
        addingSendText = false
        val sw = screen.width; val sh = screen.height
        val (tmp, id) = history.current.addSendText(message, name, null)
        val (w, h) = buttonPixelSize(tmp.findButton(id)!!.buttonSize, sw, sh, densityOf)
        val make = { pos: ButtonPosition -> tmp.moveWidget(id, pos) to id }
        val spot = history.current.spotForNew(w, h, sw, sh, densityOf, Offset(sw * 0.72f - w / 2, sh * 0.45f - h / 2))
        if (spot != null) { val (next, nid) = make(percentPosition(spot.x, spot.y, w, h, sw, sh)); commit(next); selected = nid }
        else placing = PendingPlace(w, h, make)
    }

    if (adding) AddActionDialog(thai, onDismiss = { adding = false }, onCustom = { adding = false; addingCustom = true }, onSendText = { adding = false; addingSendText = true }) { action ->
        adding = false
        val sw = screen.width; val sh = screen.height
        val px = sh * action.sizePct / 10000f
        val pw = sh * action.widthPct / 10000f
        val make = { pos: ButtonPosition -> history.current.addAction(action, thai, pos) }
        //a free spot on the right side (the stick prefers bottom left); otherwise the user taps where it goes
        val near = if (action == StudioAction.MOVE) Offset(sw * 0.04f, sh - px) else Offset(sw * 0.72f - pw / 2, sh * 0.45f - px / 2) //action buttons live on the right
        val spot = history.current.spotForNew(pw, px, sw, sh, densityOf, near)
        if (spot != null) { val (next, id) = make(percentPosition(spot.x, spot.y, pw, px, sw, sh)); commit(next); selected = id }
        else placing = PendingPlace(pw, px, make)
    }

    settingsFor?.let { id ->
        val b = layout.findButton(id)
        val j = layout.findJoystick(id)
        when {
            b != null -> ButtonSettingsDialog(layout, b, thai, onDismiss = { settingsFor = null }, onApply = { next -> commit(next); settingsFor = null },
                onCopy = { settingsFor = null; copyWidget(id) },
                onDelete = { commit(history.current.removeWidget(id)); selected = null; settingsFor = null })
            j != null -> JoystickSettingsDialog(j, onDismiss = { settingsFor = null }, onApply = { pct -> commit(history.current.updateJoystick(id) { it.copy(sizePercentage = pct, sizeType = com.movtery.layer_controller.data.ButtonSize.Type.Percentage) }); settingsFor = null },
                onDelete = { commit(history.current.removeWidget(id)); selected = null; settingsFor = null })
            else -> settingsFor = null
        }
    }

    if (confirmExit) AllDialog(
        title = stringResource(R.string.all_studio_unsaved_title),
        onDismissRequest = { confirmExit = false },
        actions = {
            AllButton(stringResource(R.string.all_studio_keep_editing), { confirmExit = false }, compact = true)
            AllButton(stringResource(R.string.all_studio_discard), { confirmExit = false; onExit() }, compact = true, style = AllButtonStyle.Danger)
            AllButton(stringResource(R.string.all_studio_save_exit), { confirmExit = false; save(onExit) }, compact = true, style = AllButtonStyle.Primary)
        }
    ) { Text(stringResource(R.string.all_studio_unsaved_body), fontSize = 14.sp) }

    saveError?.let { e ->
        AllDialog(title = stringResource(R.string.generic_error), onDismissRequest = { saveError = null },
            actions = { AllButton(stringResource(R.string.generic_close), { saveError = null }, compact = true) }) { Text(e) }
    }
}

@Composable
private fun key2(id: String, content: @Composable () -> Unit) = androidx.compose.runtime.key(id) { content() }

private fun eventLabel(events: List<ClickEvent>): String = events.joinToString(" + ") { e -> keyLabel(e) }

/** One button, drawn with its own style colours; edit mode = drag to move, corner = resize, tap = settings; try mode = press only */
@Composable
private fun StudioButton(
    layout: ControlLayout, b: NormalData, screenW: Float, screenH: Float, density: Float, mode: StudioMode, isSelected: Boolean, warn: Boolean, thai: Boolean,
    snap: ((Rect) -> Snap)?, onDragDone: () -> Unit,
    onSelect: () -> Unit, onOpen: () -> Unit, onMoved: (com.movtery.layer_controller.data.ButtonPosition) -> Unit,
    onResized: (Float, Float) -> Unit, onDelete: () -> Unit, onPressed: (String) -> Unit
) {
    val (w0, h0) = buttonPixelSize(b.buttonSize, screenW, screenH, density)
    var dw by remember(b) { mutableFloatStateOf(0f) }
    var dh by remember(b) { mutableFloatStateOf(0f) }
    val w = (w0 + dw).coerceAtLeast(24f * density)
    val h = (h0 + dh).coerceAtLeast(24f * density)
    val base = widgetPosition(b.position.xPercentage(), b.position.yPercentage(), IntSize(w0.toInt(), h0.toInt()), IntSize(screenW.toInt(), screenH.toInt()))
    var drag by remember(b) { mutableStateOf(Offset.Zero) }
    var nudge by remember(b) { mutableStateOf(Offset.Zero) }
    var pressed by remember { mutableStateOf(false) }
    val style = layout.styles.firstOrNull { it.uuid == b.buttonStyle }?.lightStyle
    val alpha = style?.alpha ?: 1f
    //V1.3: normal and pressed looks come from the button's style (same fields the game draws)
    val look = if (pressed) layout.lookOf(b, pressed = true) else layout.lookOf(b, pressed = false)
    val bg = look.background
    val fg = look.content
    val shape = RoundedCornerShape(look.radius.dp)
    val label = b.text.translate().ifBlank { actionOf(b)?.label(thai) ?: "?" }
    val left = (base.x + drag.x + nudge.x).coerceIn(0f, (screenW - w).coerceAtLeast(0f))
    val top = (base.y + drag.y + nudge.y).coerceIn(0f, (screenH - h).coerceAtLeast(0f))

    Box(
        Modifier.offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .size((w / density).dp, (h / density).dp)
    ) {
        Box(
            Modifier.fillMaxSize().clip(shape).background(bg.copy(alpha = bg.alpha * alpha))
                .then(if (look.borderWidth > 0) Modifier.border(look.borderWidth.dp, look.borderColor.copy(alpha = look.borderColor.alpha * alpha), shape) else Modifier)
                //tap detector sits outside the drag detector so a drag (handled first) cancels the tap instead of being eaten by it
                .pointerInput(mode, b, isSelected) { if (mode == StudioMode.EDIT) detectTapGestures { if (isSelected) onOpen() else onSelect() } }
                .pointerInput(mode, b) {
                    if (mode == StudioMode.TRY) {
                        awaitEachGesture {
                            awaitFirstDown()
                            pressed = true
                            onPressed(label + " (" + (actionOf(b)?.inputHint(thai) ?: eventLabel(b.clickEvents)) + ")")
                            do { val e = awaitPointerEvent() } while (e.changes.any { it.pressed })
                            pressed = false
                        }
                    } else {
                        detectDragGestures(
                            onDragStart = { onSelect() },
                            onDragEnd = {
                                val l = (base.x + drag.x + nudge.x).coerceIn(0f, (screenW - w0).coerceAtLeast(0f))
                                val t = (base.y + drag.y + nudge.y).coerceIn(0f, (screenH - h0).coerceAtLeast(0f))
                                onMoved(percentPosition(l, t, w0, h0, screenW, screenH)); drag = Offset.Zero; nudge = Offset.Zero; onDragDone()
                            },
                            onDragCancel = { drag = Offset.Zero; nudge = Offset.Zero; onDragDone() }
                        ) { change, amount ->
                            change.consume(); drag += amount
                            nudge = snap?.invoke(Rect(Offset(base.x + drag.x, base.y + drag.y), Size(w0, h0)))?.nudge ?: Offset.Zero
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = fg.copy(alpha = fg.alpha * alpha), fontWeight = FontWeight.Bold, fontSize = look.fontSize.sp, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
        }
        if (warn || isSelected) Box(Modifier.fillMaxSize().clip(shape).border(if (warn) 3.dp else 2.dp, if (warn) WarnOrange else SelectBlue, shape))
        if (warn) Box(Modifier.align(Alignment.TopStart).offset((-8).dp, (-8).dp).size(20.dp).clip(CircleShape).background(WarnOrange), contentAlignment = Alignment.Center) {
            Text("!", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        if (isSelected && mode == StudioMode.EDIT) {
            //resize handle (bottom right); delete / copy / settings live in the quick bar
            Box(
                Modifier.align(Alignment.BottomEnd).offset(8.dp, 8.dp).size(22.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF1F335B)).border(2.dp, SelectBlue, RoundedCornerShape(6.dp))
                    .pointerInput(b) {
                        detectDragGestures(
                            onDragEnd = { onResized((w0 + dw).coerceAtLeast(24f * density), (h0 + dh).coerceAtLeast(24f * density)); dw = 0f; dh = 0f },
                            onDragCancel = { dw = 0f; dh = 0f }
                        ) { change, amount -> change.consume(); dw += amount.x; dh += amount.y }
                    },
                contentAlignment = Alignment.Center
            ) { Text("⤡", color = SelectBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun StudioJoystick(
    j: JoystickData, screenW: Float, screenH: Float, density: Float, mode: StudioMode, isSelected: Boolean, warn: Boolean,
    snap: ((Rect) -> Snap)?, onDragDone: () -> Unit,
    onSelect: () -> Unit, onOpen: () -> Unit, onMoved: (com.movtery.layer_controller.data.ButtonPosition) -> Unit,
    onResized: (Float) -> Unit, onDelete: () -> Unit, onPressed: () -> Unit
) {
    val s0 = joystickPixelSize(j, screenH, density)
    var ds by remember(j) { mutableFloatStateOf(0f) }
    val s = (s0 + ds).coerceAtLeast(screenH * 0.2f)
    val base = widgetPosition(j.position.xPercentage(), j.position.yPercentage(), IntSize(s0.toInt(), s0.toInt()), IntSize(screenW.toInt(), screenH.toInt()))
    var drag by remember(j) { mutableStateOf(Offset.Zero) }
    var nudge by remember(j) { mutableStateOf(Offset.Zero) }
    val left = (base.x + drag.x + nudge.x).coerceIn(0f, (screenW - s).coerceAtLeast(0f))
    val top = (base.y + drag.y + nudge.y).coerceIn(0f, (screenH - s).coerceAtLeast(0f))
    Box(Modifier.offset { IntOffset(left.roundToInt(), top.roundToInt()) }.size((s / density).dp)) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape).background(Color.Black.copy(alpha = 0.35f))
                .border(if (warn) 3.dp else if (isSelected) 2.dp else 1.dp, if (warn) WarnOrange else if (isSelected) SelectBlue else Color.White.copy(alpha = 0.5f), CircleShape)
                .pointerInput(mode, j, isSelected) { if (mode == StudioMode.EDIT) detectTapGestures { if (isSelected) onOpen() else onSelect() } }
                .pointerInput(mode, j) {
                    if (mode == StudioMode.TRY) detectTapGestures(onPress = { onPressed() })
                    else detectDragGestures(
                        onDragStart = { onSelect() },
                        onDragEnd = {
                            val l = (base.x + drag.x + nudge.x).coerceIn(0f, (screenW - s0).coerceAtLeast(0f))
                            val t = (base.y + drag.y + nudge.y).coerceIn(0f, (screenH - s0).coerceAtLeast(0f))
                            onMoved(percentPosition(l, t, s0, s0, screenW, screenH)); drag = Offset.Zero; nudge = Offset.Zero; onDragDone()
                        },
                        onDragCancel = { drag = Offset.Zero; nudge = Offset.Zero; onDragDone() }
                    ) { change, amount ->
                        change.consume(); drag += amount
                        nudge = snap?.invoke(Rect(Offset(base.x + drag.x, base.y + drag.y), Size(s0, s0)))?.nudge ?: Offset.Zero
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size((s / density * 0.38f).dp).clip(CircleShape).background(Color.White.copy(alpha = 0.55f)))
            Text("▲", color = Color.White, fontSize = 12.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp))
            Text("▼", color = Color.White, fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp))
            Text("◀", color = Color.White, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp))
            Text("▶", color = Color.White, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp))
        }
        if (isSelected && mode == StudioMode.EDIT) {
            Box(
                Modifier.align(Alignment.BottomEnd).size(22.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF1F335B)).border(2.dp, SelectBlue, RoundedCornerShape(6.dp))
                    .pointerInput(j) {
                        detectDragGestures(onDragEnd = { onResized((s0 + ds).coerceAtLeast(screenH * 0.2f)); ds = 0f }, onDragCancel = { ds = 0f }) { change, amount -> change.consume(); ds += (amount.x + amount.y) / 2f }
                    },
                contentAlignment = Alignment.Center
            ) { Text("⤡", color = SelectBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

/** Friendly icon for each action (display only) */
private fun StudioAction.icon(): String = when (this) {
    StudioAction.JUMP -> "⬆"; StudioAction.ATTACK -> "⚔"; StudioAction.USE -> "✋"; StudioAction.INVENTORY -> "🎒"
    StudioAction.SPRINT -> "🏃"; StudioAction.SNEAK -> "🐢"; StudioAction.ESC -> "⏸"; StudioAction.DROP -> "🗑"
    StudioAction.CHAT -> "💬"; StudioAction.PERSPECTIVE -> "🎥"; StudioAction.HOTBAR_PREV -> ""; StudioAction.HOTBAR_NEXT -> "" //their names already carry the arrow
    StudioAction.KEYBOARD -> "⌨"; StudioAction.MENU -> "☰"; StudioAction.PLAYER_LIST -> "👥"; StudioAction.DEBUG_INFO -> "📊"
    StudioAction.FULLSCREEN -> "⛶"; StudioAction.MOVE -> "🕹"
    else -> ""
}

private val CommonActions = listOf(StudioAction.JUMP, StudioAction.ATTACK, StudioAction.USE, StudioAction.INVENTORY, StudioAction.SPRINT, StudioAction.SNEAK)

/**
 * Four tabs instead of one scrolling list: common, hotbar slots, system keys, custom. Every action stays reachable.
 */
@Composable
private fun AddActionDialog(thai: Boolean, onDismiss: () -> Unit, onCustom: () -> Unit, onSendText: () -> Unit, onPick: (StudioAction) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val first = CommonActions + listOf(StudioAction.CHAT, StudioAction.ESC, StudioAction.MOVE)
    val slots = StudioAction.entries.filter { it.isSlot } + listOf(StudioAction.HOTBAR_PREV, StudioAction.HOTBAR_NEXT)
    val system = StudioAction.entries.filter { it !in first && it !in slots }
    val tabs = listOf(stringResource(R.string.all_studio_group_common), stringResource(R.string.all_studio_slots_short), stringResource(R.string.all_studio_group_system), stringResource(R.string.all_studio_group_custom))
    AllDialog(
        title = stringResource(R.string.all_studio_add_title),
        onDismissRequest = onDismiss,
        actions = { AllButton(stringResource(R.string.generic_cancel), onDismiss, compact = true) }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tabs.forEachIndexed { i, t ->
                    Text(
                        t, fontSize = 13.sp, color = if (i == tab) Color(0xFF6F9BFF) else Color(0xFFD5DBE8), maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (i == tab) Color(0xFF1F335B) else Color(0xFF232B3D))
                            .border(1.dp, if (i == tab) Color(0xFF2F6FED) else Color.Transparent, RoundedCornerShape(16.dp))
                            .clickable { tab = i }.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
            when (tab) {
                0, 1, 2 -> {
                    val list = when (tab) { 0 -> first; 1 -> slots; else -> system }
                    val cols = if (tab == 1) 4 else 3
                    Column(Modifier.height(200.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        list.chunked(cols).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { a -> ActionTile(a, thai, Modifier.weight(1f), big = tab != 1) { onPick(a) } }
                                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                else -> Column(Modifier.height(200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AllButton("⌨ " + stringResource(R.string.all_studio_custom_button), onCustom, style = AllButtonStyle.Primary, fill = true)
                    AllButton("💬 " + stringResource(R.string.all_studio_send_text), onSendText, fill = true)
                }
            }
        }
    }
}

@Composable
private fun GroupTitle(text: String) = Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8C97AE))

@Composable
private fun ActionTile(a: StudioAction, thai: Boolean, modifier: Modifier, big: Boolean, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(6.dp)).background(if (a == StudioAction.MOVE) Color(0xFF1F335B) else Color(0xFF232B3D))
            .border(1.dp, if (a == StudioAction.MOVE) Color(0xFF2F6FED) else Color(0xFF2D3548), RoundedCornerShape(6.dp)).clickable(onClick = onClick)
            .padding(vertical = if (big) 5.dp else 3.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (big) Text(a.icon(), fontSize = 18.sp)
        Text((if (big || a.icon().isEmpty()) "" else a.icon() + " ") + a.label(thai), fontSize = if (big) 12.sp else 11.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFFD5DBE8))
    }
}

/** Small settings window: name, command, size, opacity, delete. Changes apply on OK as one Undo step. */
@Composable
private fun ButtonSettingsDialog(layout: ControlLayout, b: NormalData, thai: Boolean, onDismiss: () -> Unit, onApply: (ControlLayout) -> Unit, onDelete: () -> Unit, onCopy: () -> Unit) {
    var name by remember { mutableStateOf(b.text.default) }
    //the click-event list is the single source of truth: the command menu replaces it, "more keys" adds to it
    var events by remember { mutableStateOf(b.clickEvents) }
    val action = StudioAction.entries.firstOrNull { it != StudioAction.MOVE && it.events == events }
    //V1.3: a send-text button has exactly one event, its message
    val isSend = events.size == 1 && events[0].type == ClickEvent.Type.SendText
    var sendEdit by remember { mutableStateOf(false) }
    var hold by remember { mutableStateOf(b.isToggleable) }
    var passThrough by remember { mutableStateOf(b.isPenetrable) }
    var draggable by remember { mutableStateOf(b.isSwipple && !b.isPenetrable) }
    var tab by remember { mutableIntStateOf(0) }
    var editPressed by remember { mutableStateOf(false) }
    var keyMenu by remember { mutableStateOf(false) }
    var rebinding by remember { mutableStateOf(false) }
    //V1.3: width and height are set separately (percent of screen height, as the game reads them)
    val pctType = b.buttonSize.type == com.movtery.layer_controller.data.ButtonSize.Type.Percentage
    val startW = if (pctType) b.buttonSize.widthPercentage.toFloat() else 1500f
    val startH = if (pctType) b.buttonSize.heightPercentage.toFloat() else 1400f
    var width by remember { mutableFloatStateOf(startW.coerceIn(SIZE_SLIDER_MIN, SIZE_SLIDER_MAX)) }
    var height by remember { mutableFloatStateOf(startH.coerceIn(SIZE_SLIDER_MIN, SIZE_SLIDER_MAX)) }
    var opacity by remember { mutableFloatStateOf(layout.opacityOf(b)) }
    var menu by remember { mutableStateOf(false) }
    //the look of each state is edited on its own; OK writes only the states that changed
    val startNormal = layout.lookOf(b, pressed = false)
    val startPressed = layout.lookOf(b, pressed = true)
    var normalLook by remember { mutableStateOf(startNormal) }
    var pressedLook by remember { mutableStateOf(startPressed) }
    val look = if (editPressed) pressedLook else normalLook
    fun setLook(f: (ButtonLook) -> ButtonLook) { if (editPressed) pressedLook = f(pressedLook) else normalLook = f(normalLook) }
    val previewLabel = name.trim().ifBlank { sendTextOf(b) ?: action?.label(thai) ?: "?" }
    StudioSheet(
        title = stringResource(R.string.all_studio_button_settings),
        onDismissRequest = onDismiss,
        header = {
            //tabs + a live preview that stays visible on every tab; all values live in the state above, so switching tabs loses nothing
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(R.string.all_studio_tab_action, R.string.all_studio_tab_look, R.string.all_studio_tab_behavior).forEachIndexed { i, r ->
                        Text(
                            stringResource(r), fontSize = 13.sp, maxLines = 1, textAlign = TextAlign.Center,
                            color = if (i == tab) Color(0xFF6F9BFF) else Color(0xFFD5DBE8),
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (i == tab) Color(0xFF1F335B) else Color(0xFF232B3D))
                                .border(1.dp, if (i == tab) Color(0xFF2F6FED) else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { tab = i }.padding(vertical = if (LocalSheetCompact.current) 4.dp else 8.dp)
                        )
                    }
                }
                val pv = normalLook
                Box(
                    Modifier.width(76.dp).height(34.dp).clip(RoundedCornerShape(pv.radius.coerceAtMost(17).dp)).background(pv.background)
                        .then(if (pv.borderWidth > 0) Modifier.border(pv.borderWidth.coerceAtMost(3).dp, pv.borderColor, RoundedCornerShape(pv.radius.coerceAtMost(17).dp)) else Modifier),
                    contentAlignment = Alignment.Center
                ) { Text(previewLabel, color = pv.content, fontWeight = FontWeight.Bold, fontSize = pv.fontSize.coerceAtMost(14).sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp)) }
            }
        },
        actions = {
            AllButton("🗑 " + stringResource(R.string.all_studio_delete), onDelete, compact = true, style = AllButtonStyle.Danger)
            //copies the saved state of this button (OK first to copy unsaved changes too)
            AllButton("⧉ " + stringResource(R.string.all_studio_copy), onCopy, compact = true)
            AllButton(stringResource(R.string.generic_cancel), onDismiss, compact = true)
            AllButton(stringResource(R.string.generic_confirm), {
                var next = layout
                if (events != b.clickEvents) {
                    val message = events.singleOrNull()?.takeIf { it.type == ClickEvent.Type.SendText }?.key
                    next = if (message != null) {
                        //a button that had the action's default name takes the message as its name; a name the user chose stays
                        next.setButtonSendText(b.uuid, message, if (b.text.default == actionOf(b)?.label(thai)) message else null)
                    } else next.setButtonEvents(b.uuid, events, thai)
                }
                if (hold != b.isToggleable || passThrough != b.isPenetrable || draggable != b.isSwipple) next = next.setButtonBehavior(b.uuid, hold, passThrough, draggable)
                if (name.isNotBlank() && name != b.text.default) next = next.setButtonName(b.uuid, name.trim())
                val w = width.roundToInt(); val h = height.roundToInt()
                if (w != startW.roundToInt() || h != startH.roundToInt()) next = next.setButtonSize(b.uuid, w, h)
                if (opacity != layout.opacityOf(b)) next = next.setButtonOpacity(b.uuid, opacity)
                if (normalLook != startNormal) next = next.setButtonLook(b.uuid, false, normalLook)
                if (pressedLook != startPressed) next = next.setButtonLook(b.uuid, true, pressedLook)
                onApply(next)
            }, compact = true, style = AllButtonStyle.Primary)
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when (tab) {
                0 -> {
                    SettingRow(stringResource(R.string.all_studio_name)) {
                        OutlinedTextField(value = name, onValueChange = { name = it.take(24) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    SettingRow(stringResource(R.string.all_studio_command)) {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                AllButton((action?.label(thai) ?: comboLabel(events)) + " ▾", { menu = true }, compact = true)
                                com.movtery.zalithlauncher.all.ui.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    StudioAction.entries.filter { it != StudioAction.MOVE }.forEach { a ->
                                        com.movtery.zalithlauncher.all.ui.DropdownMenuItem(text = { Text(a.label(thai) + "  ·  " + a.inputHint(thai)) }, onClick = { events = a.events; menu = false })
                                    }
                                    //V1.3: send a chat message (or a /command) when pressed
                                    com.movtery.zalithlauncher.all.ui.DropdownMenuItem(text = { Text("💬 " + stringResource(R.string.all_studio_send_text) + "…") }, onClick = { menu = false; sendEdit = true })
                                }
                            }
                            //V1.2: bind any key / combo after the button was made
                            AllButton("⌨ " + stringResource(R.string.all_studio_change_key), { rebinding = true }, compact = true)
                            AllButton("💬 " + stringResource(R.string.all_studio_send_text), { sendEdit = true }, compact = true)
                        }
                    }
                    SettingRow(stringResource(R.string.all_studio_keys)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            events.forEach { e ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(eventName(e, thai), fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    //the last key cannot be removed: a button must always do something
                                    if (events.size > 1) Text("✕", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clip(CircleShape).clickable { events = events - e }.padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                            //a send-text button stays one message; extra keys would only be pressed with it
                            if (!isSend) Box {
                                AllButton("＋ " + stringResource(R.string.all_studio_add_key), { keyMenu = true }, compact = true)
                                com.movtery.zalithlauncher.all.ui.DropdownMenu(expanded = keyMenu, onDismissRequest = { keyMenu = false }) {
                                    StudioAction.entries.filter { it != StudioAction.MOVE && it.events.size == 1 && it.events[0] !in events }.forEach { a ->
                                        com.movtery.zalithlauncher.all.ui.DropdownMenuItem(text = { Text(a.label(thai) + "  ·  " + a.inputHint(thai)) }, onClick = { events = events + a.events; keyMenu = false })
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    SettingRow(stringResource(R.string.all_studio_width)) {
                        SliderValue(width, SIZE_SLIDER_MIN..SIZE_SLIDER_MAX, "${(width / 100).roundToInt()}") { width = it }
                    }
                    SettingRow(stringResource(R.string.all_studio_height)) {
                        SliderValue(height, SIZE_SLIDER_MIN..SIZE_SLIDER_MAX, "${(height / 100).roundToInt()}") { height = it }
                    }
                    SettingRow(stringResource(R.string.all_studio_opacity)) {
                        SliderValue(opacity, 0.1f..1f, "${(opacity * 100).roundToInt()}%") { opacity = it }
                    }
                    //V1.3: colours, text, border and corners per state
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AllButton(stringResource(R.string.all_studio_state_normal), { editPressed = false }, compact = true, style = if (!editPressed) AllButtonStyle.Primary else AllButtonStyle.Neutral)
                        AllButton(stringResource(R.string.all_studio_state_pressed), { editPressed = true }, compact = true, style = if (editPressed) AllButtonStyle.Primary else AllButtonStyle.Neutral)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                        StylePreview(normalLook, previewLabel, stringResource(R.string.all_studio_state_normal), Modifier.weight(1f))
                        StylePreview(pressedLook, previewLabel, stringResource(R.string.all_studio_state_pressed), Modifier.weight(1f))
                    }
                    SettingRow(stringResource(R.string.all_studio_bg_color)) { ColorSwatches(StudioColors, look.background) { c -> setLook { it.copy(background = c) } } }
                    SettingRow(stringResource(R.string.all_studio_text_color)) { ColorSwatches(TextColors, look.content) { c -> setLook { it.copy(content = c) } } }
                    SettingRow(stringResource(R.string.all_studio_text_size)) {
                        SliderValue(look.fontSize.toFloat(), FONT_MIN.toFloat()..FONT_MAX.toFloat(), "${look.fontSize}") { v -> setLook { it.copy(fontSize = v.roundToInt()) } }
                    }
                    SettingRow(stringResource(R.string.all_studio_border_color)) { ColorSwatches(BorderColors, look.borderColor) { c -> setLook { it.copy(borderColor = c) } } }
                    SettingRow(stringResource(R.string.all_studio_border_width)) {
                        SliderValue(look.borderWidth.toFloat(), BORDER_MIN.toFloat()..BORDER_MAX.toFloat(), "${look.borderWidth}") { v -> setLook { it.copy(borderWidth = v.roundToInt()) } }
                    }
                    SettingRow(stringResource(R.string.all_studio_radius)) {
                        SliderValue(look.radius.toFloat(), RADIUS_MIN.toFloat()..RADIUS_MAX.toFloat(), "${look.radius}") { v -> setLook { it.copy(radius = v.roundToInt()) } }
                    }
                }
                else -> {
                    StudioSwitch(stringResource(R.string.all_studio_hold), stringResource(R.string.all_studio_hold_hint), hold) { hold = it }
                    StudioSwitch(stringResource(R.string.all_studio_pass), stringResource(R.string.all_studio_pass_hint), passThrough) { passThrough = it; if (it) draggable = false }
                    StudioSwitch(stringResource(R.string.all_studio_drag), stringResource(R.string.all_studio_drag_hint), draggable) { draggable = it; if (it) passThrough = false }
                    Text(stringResource(R.string.all_studio_behavior_note), fontSize = 11.sp, color = Color(0xFF8C97AE))
                }
            }
        }
    }
    if (rebinding) CustomKeyDialog(
        title = stringResource(R.string.all_studio_change_key), initialName = name, initialEvents = events,
        onDismiss = { rebinding = false }
    ) { newName, newEvents -> events = newEvents; if (newName.isNotBlank()) name = newName; rebinding = false }
    if (sendEdit) SendTextDialog(initialMessage = sendTextOf(b) ?: "", initialName = name, onDismiss = { sendEdit = false }) { message, newName ->
        sendEdit = false
        events = sendTextEvents(message)
        if (newName.isNotBlank()) name = newName
    }
}

@Composable
private fun JoystickSettingsDialog(j: JoystickData, onDismiss: () -> Unit, onApply: (Int) -> Unit, onDelete: () -> Unit) {
    var size by remember { mutableFloatStateOf(j.sizePercentage.toFloat().coerceIn(2000f, 6000f)) }
    AllDialog(
        title = stringResource(R.string.all_studio_joystick_settings),
        onDismissRequest = onDismiss,
        actions = {
            AllButton("🗑 " + stringResource(R.string.all_studio_delete), onDelete, compact = true, style = AllButtonStyle.Danger)
            AllButton(stringResource(R.string.generic_cancel), onDismiss, compact = true)
            AllButton(stringResource(R.string.generic_confirm), { onApply(size.roundToInt()) }, compact = true, style = AllButtonStyle.Primary)
        }
    ) {
        SettingRow(stringResource(R.string.all_studio_size)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(value = size, onValueChange = { size = it }, valueRange = 2000f..6000f, modifier = Modifier.weight(1f))
                Text("${(size / 100).roundToInt()}", fontSize = 13.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
            }
        }
    }
}

private val StudioColors = listOf(
    Color(0x80000000), Color(0xCC1565C0), Color(0xCC2E7D32), Color(0xCCC62828), Color(0xCCF9A825), Color(0xCC6A1B9A), Color(0xCCFFFFFF)
)

@Composable
private fun StudioSwitch(title: String, hint: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onChange(!value) }) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(hint, fontSize = 11.sp, color = Color(0xFF8C97AE))
        }
        androidx.compose.material3.Switch(checked = value, onCheckedChange = onChange)
    }
}

/** Width/height slider range in the editor: percent of screen height x100 (3% to 60%) */
private const val SIZE_SLIDER_MIN = 300f
private const val SIZE_SLIDER_MAX = 6000f

/** Text colours offered for a button's text (light and dark text on the colours above) */
private val TextColors = listOf(
    Color.White, Color(0xFF111111), Color(0xFFFFD54F), Color(0xFFE53935), Color(0xFF43A047), Color(0xFF26C6DA), Color(0xFFFF9800)
)

/** Border colours offered for a button's border */
private val BorderColors = listOf(
    Color.White, Color(0xFF111111), Color(0xFFFFC107), Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF9E9E9E)
)

/** A row of round colour swatches; the chosen colour gets a blue ring (alpha ignored when comparing) */
@Composable
private fun ColorSwatches(colors: List<Color>, current: Color, onPick: (Color) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        colors.forEach { c ->
            val same = c.copy(alpha = 1f) == current.copy(alpha = 1f)
            Box(Modifier.size(26.dp).clip(CircleShape).background(c)
                .border(if (same) 3.dp else 1.dp, if (same) SelectBlue else Color.Gray, CircleShape)
                .clickable { onPick(c) })
        }
    }
}

/** A slider with its value shown at the end */
@Composable
private fun SliderValue(value: Float, range: ClosedFloatingPointRange<Float>, text: String, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f), colors = SliderDefaults.colors())
        Text(text, fontSize = 13.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
    }
}

/** One state of a button drawn the way the game draws it (background, border, corners, text size and colour) */
@Composable
private fun StylePreview(look: ButtonLook, label: String, caption: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        val shape = RoundedCornerShape(look.radius.dp)
        Box(
            Modifier.fillMaxWidth().height(44.dp).clip(shape).background(look.background)
                .then(if (look.borderWidth > 0) Modifier.border(look.borderWidth.dp, look.borderColor, shape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = look.content, fontWeight = FontWeight.Bold, fontSize = look.fontSize.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp))
        }
        Text(caption, fontSize = 11.sp)
    }
}

/** Message to send to the chat when pressed, with the button's own name kept separate (e.g. "ฟีฟาย" sent, "FF" on the button) */
@Composable
private fun SendTextDialog(initialMessage: String, initialName: String, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var message by remember { mutableStateOf(initialMessage) }
    var name by remember { mutableStateOf(initialName) }
    val valid = validSendText(message) != null
    AllDialog(
        title = stringResource(R.string.all_studio_send_text_title),
        onDismissRequest = onDismiss,
        actions = {
            AllButton(stringResource(R.string.generic_cancel), onDismiss, compact = true)
            AllButton(stringResource(R.string.generic_confirm), { validSendText(message)?.let { onConfirm(it, name.trim()) } }, compact = true, style = AllButtonStyle.Primary, enabled = valid)
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SettingRow(stringResource(R.string.all_studio_message)) {
                //no line breaks: the chat takes one line; the length limit matches the saved value
                OutlinedTextField(value = message, onValueChange = { message = it.filter { c -> !c.isISOControl() }.take(SEND_TEXT_MAX + 1) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            Text(stringResource(R.string.all_studio_message_hint), fontSize = 12.sp, color = Color.Gray)
            Text(
                if (valid) "${message.trim().length}/$SEND_TEXT_MAX" else stringResource(R.string.all_studio_message_error),
                fontSize = 12.sp, color = if (valid) Color.Gray else Color(0xFFE53935), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End
            )
            SettingRow(stringResource(R.string.all_studio_name)) {
                OutlinedTextField(value = name, onValueChange = { name = it.take(24) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
        Box(Modifier.weight(1f)) { content() }
    }
}

/**
 * Small tool bar next to the selected widget: Settings / Copy / Delete. Sits just under the widget (clear of the resize handle at its
 * bottom-right corner), above it when there is no room below, and is clamped to the screen so it never leaves it.
 */
@Composable
private fun QuickBar(
    target: Rect, screenW: Float, screenH: Float, density: Float, withCopy: Boolean,
    onSettings: () -> Unit, onCopy: () -> Unit, onDelete: () -> Unit
) {
    val items = if (withCopy) 3 else 2
    val barW = (items * 84 + 12) * density
    val barH = 44f * density
    val gap = 18f * density   //clears the resize handle (22dp square, 8dp outside the corner)
    val topLimit = 56f * density  //the toolbar lives at the top
    val below = target.bottom + gap
    val above = target.top - gap - barH
    val y = when {
        below + barH <= screenH -> below
        above >= topLimit -> above
        else -> (screenH - barH).coerceAtLeast(0f)
    }
    val x = (target.center.x - barW / 2).coerceIn(0f, (screenW - barW).coerceAtLeast(0f))
    Row(
        Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) }.clip(RoundedCornerShape(10.dp)).background(Color(0xF011151F))
            .border(1.dp, Color(0xFF2D3548), RoundedCornerShape(10.dp)).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        QuickItem(R.drawable.ic_settings_filled, stringResource(R.string.generic_setting), Color(0xFFD5DBE8), onSettings)
        if (withCopy) QuickItem(R.drawable.ic_content_copy_filled, stringResource(R.string.all_controls_copy), Color(0xFFD5DBE8), onCopy)
        QuickItem(R.drawable.ic_delete_outlined, stringResource(R.string.generic_delete), Color(0xFFFF6B6B), onDelete)
    }
}

@Composable
private fun QuickItem(icon: Int, text: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.width(84.dp).height(38.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
    ) {
        androidx.compose.material3.Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = tint, fontSize = 12.sp, maxLines = 1)
    }
}

/**
 * Dialog shell for the button settings: title, a pinned header (tabs), a body that is the only part that scrolls, and a pinned
 * footer. With the Android keyboard open the window is resized above it and the title bar is dropped, so the field being typed in and the
 * footer buttons both stay on screen.
 */
/** true while the sheet is squeezed above the keyboard: tabs and footer shrink so the field being typed in is fully visible */
private val LocalSheetCompact = androidx.compose.runtime.compositionLocalOf { false }

@Composable
private fun StudioSheet(
    title: String,
    onDismissRequest: () -> Unit,
    header: @Composable () -> Unit,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            ?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val compact = maxHeight < 300.dp
            val shape = RoundedCornerShape(10.dp)
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = if (compact) 4.dp else 12.dp).widthIn(max = 480.dp).fillMaxWidth()
                    .shadow(14.dp, shape).clip(shape).background(com.movtery.zalithlauncher.all.ui.AllColors.WindowBody)
                    .border(1.dp, com.movtery.zalithlauncher.all.ui.AllColors.BorderStrong, shape)
            ) {
                if (!compact) Text(
                    title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF223049)).padding(horizontal = 14.dp, vertical = 10.dp)
                )
                androidx.compose.runtime.CompositionLocalProvider(LocalSheetCompact provides compact) {
                    Box(Modifier.padding(start = 12.dp, end = 12.dp, top = if (compact) 4.dp else 8.dp, bottom = if (compact) 0.dp else 4.dp)) { header() }
                }
                androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides com.movtery.zalithlauncher.all.ui.AllColors.Text) {
                    Column(
                        Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(start = 12.dp, end = 12.dp, top = if (compact) 0.dp else 4.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) { content() }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = if (compact) 0.dp else 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically
                ) { actions() }
            }
        }
    }
}
