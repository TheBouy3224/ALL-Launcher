package com.movtery.zalithlauncher.all.controls

import androidx.compose.foundation.Canvas
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

private val XpBlue = Brush.verticalGradient(listOf(Color(0xFF3D95FF), Color(0xFF0A5BE0), Color(0xFF0042C8)))
private val SelectBlue = Color(0xFF3A8DFF)
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
    var confirmExit by remember { mutableStateOf(false) }
    var toolbarHidden by remember { mutableStateOf(false) }
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

    //Back = the close button: asks before throwing edits away
    androidx.activity.compose.BackHandler { if (mode == StudioMode.TRY) mode = StudioMode.EDIT else if (history.isDirty()) confirmExit = true else onExit() }

    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF6E9BC9), Color(0xFF4F7C46), Color(0xFF2F4E2A))))) {
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
                        snap = snapper?.let { f -> { r: Rect -> f(j.uuid, r).also { guide = it } } }, onDragDone = { guide = null },
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
                        snap = snapper?.let { f -> { r: Rect -> f(b.uuid, r).also { guide = it } } }, onDragDone = { guide = null },
                        onSelect = { selected = b.uuid },
                        onOpen = { settingsFor = b.uuid },
                        onMoved = { pos -> commit(history.current.moveWidget(b.uuid, pos)) },
                        onResized = { w, h -> commit(history.current.resizeButton(b.uuid, w, h, screenH)) },
                        onDelete = { commit(history.current.removeWidget(b.uuid)); selected = null },
                        onPressed = { lastPressed = it })
                }
            }
        }

        if (layout.layers.all { it.normalButtons.isEmpty() && it.joystickButtons.isEmpty() }) {
            Text(stringResource(R.string.all_studio_empty), color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(24.dp))
        }

        //try mode banner: what was pressed
        if (mode == StudioMode.TRY) {
            Text(
                lastPressed?.let { stringResource(R.string.all_studio_pressed, it) } ?: stringResource(R.string.all_studio_try_hint),
                color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xCC0A3A8A)).padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        if (warned.isNotEmpty()) {
            Text("⚠ " + stringResource(R.string.all_studio_overlap), color = Color.White, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xCCE65100)).padding(horizontal = 8.dp, vertical = 3.dp))
        }
        placing?.let {
            Row(Modifier.align(Alignment.Center).clip(RoundedCornerShape(8.dp)).background(Color(0xE60A3A8A)).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.all_studio_place_hint), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                AllButton(stringResource(R.string.generic_cancel), { placing = null }, compact = true)
            }
        }

        //compact XP toolbar (top centre), can be tucked away
        Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            if (!toolbarHidden) Row(
                Modifier.padding(top = 4.dp).clip(RoundedCornerShape(8.dp)).background(XpBlue).border(1.dp, Color(0xFF0831D9), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text("🎮", fontSize = 14.sp)
                Text(
                    initial.info.name.translate().ifBlank { file.nameWithoutExtension } + if (history.isDirty()) " •" else "",
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(110.dp)
                )
                if (mode == StudioMode.EDIT) {
                    AllButton("＋ " + stringResource(R.string.all_studio_add), { adding = true }, compact = true, style = AllButtonStyle.Success)
                    AllButton("↶ " + stringResource(R.string.all_studio_undo), { history.undo(); rev++; selected = null }, compact = true, enabled = history.canUndo())
                    AllButton("▶ " + stringResource(R.string.all_studio_try), { mode = StudioMode.TRY; selected = null; lastPressed = null; placing = null }, compact = true)
                    AllButton("📐", { helpers = !helpers; guide = null }, compact = true, style = if (helpers) AllButtonStyle.Primary else AllButtonStyle.Neutral)
                } else {
                    AllButton("✎ " + stringResource(R.string.all_studio_back_to_edit), { mode = StudioMode.EDIT }, compact = true, style = AllButtonStyle.Primary)
                }
                AllButton("💾 " + stringResource(R.string.generic_save), { save() }, compact = true, style = AllButtonStyle.Primary, enabled = history.isDirty())
                AllButton("✕", { if (history.isDirty()) confirmExit = true else onExit() }, compact = true, style = AllButtonStyle.Danger)
            }
            Text(
                if (toolbarHidden) "▼" else "▲", color = Color.White, fontSize = 12.sp,
                modifier = Modifier.clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)).background(Color(0x990A3A8A))
                    .clickable { toolbarHidden = !toolbarHidden }.padding(horizontal = 14.dp, vertical = 1.dp)
            )
        }
        if (savedNotice) {
            Text(stringResource(R.string.all_studio_saved), color = Color.White, fontSize = 12.sp, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xCC2E7D32)).padding(horizontal = 10.dp, vertical = 4.dp))
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

    if (adding) AddActionDialog(thai, onDismiss = { adding = false }, onCustom = { adding = false; addingCustom = true }) { action ->
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
                onCopy = {
                    settingsFor = null
                    val sw = screen.width; val sh = screen.height
                    val (w, h) = buttonPixelSize(b.buttonSize, sw, sh, densityOf)
                    val src = history.current.widgetRects(sw, sh, densityOf)[id]
                    val make = { pos: ButtonPosition -> history.current.duplicateButton(id, pos)!! }
                    //right next to the original if there is room, else the nearest free spot, else the user picks
                    val spot = history.current.spotForNew(w, h, sw, sh, densityOf, src?.let { Offset(it.right + 8 * densityOf, it.top) })
                    if (spot != null) { val (next, nid) = make(percentPosition(spot.x, spot.y, w, h, sw, sh)); commit(next); selected = nid }
                    else placing = PendingPlace(w, h, make)
                },
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

private fun eventLabel(events: List<ClickEvent>): String = events.joinToString(" + ") { e ->
    if (e.type == ClickEvent.Type.Key) ControlEventKeyName.getNameByKey(e.key) ?: e.key else e.key.substringAfterLast('.')
}

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
    val bg = if (pressed) (style?.pressedBackgroundColor ?: Color.Gray.copy(alpha = 0.7f)) else (style?.backgroundColor ?: Color.Black.copy(alpha = 0.5f))
    val fg = style?.contentColor ?: Color.White
    val label = b.text.translate().ifBlank { actionOf(b)?.label(thai) ?: "?" }
    val left = (base.x + drag.x + nudge.x).coerceIn(0f, (screenW - w).coerceAtLeast(0f))
    val top = (base.y + drag.y + nudge.y).coerceIn(0f, (screenH - h).coerceAtLeast(0f))

    Box(
        Modifier.offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .size((w / density).dp, (h / density).dp)
    ) {
        Box(
            Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(bg.copy(alpha = bg.alpha * alpha))
                //an overlap warning wins over the selection colour (the handles still show it is selected)
                .border(if (warn) 3.dp else if (isSelected) 2.dp else 1.dp, if (warn) WarnOrange else if (isSelected) SelectBlue else Color.White.copy(alpha = 0.35f * alpha), RoundedCornerShape(10.dp))
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
            Text(label, color = fg.copy(alpha = fg.alpha * alpha), fontWeight = FontWeight.Bold, fontSize = minOf(h / density * 0.26f, w / density / (label.length.coerceAtLeast(1) * 0.62f)).coerceIn(8f, 18f).sp, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
        }
        if (warn) Box(Modifier.align(Alignment.TopStart).offset((-8).dp, (-8).dp).size(20.dp).clip(CircleShape).background(WarnOrange), contentAlignment = Alignment.Center) {
            Text("!", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        if (isSelected && mode == StudioMode.EDIT) {
            //delete (top right) and resize handle (bottom right)
            Box(Modifier.align(Alignment.TopEnd).offset(10.dp, (-10).dp).size(24.dp).clip(CircleShape).background(Color(0xFFE53935)).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
                Text("✕", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                Modifier.align(Alignment.BottomEnd).offset(8.dp, 8.dp).size(22.dp).clip(RoundedCornerShape(4.dp)).background(Color.White).border(2.dp, SelectBlue, RoundedCornerShape(4.dp))
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
            Box(Modifier.align(Alignment.TopEnd).size(24.dp).clip(CircleShape).background(Color(0xFFE53935)).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
                Text("✕", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                Modifier.align(Alignment.BottomEnd).size(22.dp).clip(RoundedCornerShape(4.dp)).background(Color.White).border(2.dp, SelectBlue, RoundedCornerShape(4.dp))
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
 * Three groups: common actions and hotbar slots are always visible at the top; everything else scrolls on its own below,
 * so the important buttons never need scrolling.
 */
@Composable
private fun AddActionDialog(thai: Boolean, onDismiss: () -> Unit, onCustom: () -> Unit, onPick: (StudioAction) -> Unit) {
    AllDialog(
        title = stringResource(R.string.all_studio_add_title),
        onDismissRequest = onDismiss,
        //4th group "กำหนดเอง" (V1.2) sits in the always-visible button bar so the other groups keep their room
        actions = {
            AllButton("⌨ " + stringResource(R.string.all_studio_group_custom) + ": " + stringResource(R.string.all_studio_custom_button), onCustom, compact = true, style = AllButtonStyle.Primary)
            AllButton(stringResource(R.string.generic_cancel), onDismiss, compact = true)
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GroupTitle(stringResource(R.string.all_studio_group_common))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                CommonActions.forEach { a -> ActionTile(a, thai, Modifier.weight(1f), big = true) { onPick(a) } }
            }
            GroupTitle(stringResource(R.string.all_studio_slots))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StudioAction.entries.filter { it.isSlot }.forEach { a ->
                    AllButton(a.name.last().toString(), { onPick(a) }, modifier = Modifier.weight(1f), compact = true)
                }
            }
            GroupTitle(stringResource(R.string.all_studio_group_other))
            //only this group scrolls when the window is short
            Column(Modifier.heightIn(max = 96.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StudioAction.entries.filter { !it.isSlot && it !in CommonActions }.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { a -> ActionTile(a, thai, Modifier.weight(1f), big = false) { onPick(a) } }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupTitle(text: String) = Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A3A8A))

@Composable
private fun ActionTile(a: StudioAction, thai: Boolean, modifier: Modifier, big: Boolean, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(6.dp)).background(if (a == StudioAction.MOVE) Color(0xFFDDEBFF) else Color(0xFFF7F5EE))
            .border(1.dp, Color(0xFF8DA2C0), RoundedCornerShape(6.dp)).clickable(onClick = onClick)
            .padding(vertical = if (big) 5.dp else 3.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (big) Text(a.icon(), fontSize = 18.sp)
        Text((if (big || a.icon().isEmpty()) "" else a.icon() + " ") + a.label(thai), fontSize = if (big) 12.sp else 11.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF1E2A3A))
    }
}

/** Small settings window: name, command, size, opacity, delete. Changes apply on OK as one Undo step. */
@Composable
private fun ButtonSettingsDialog(layout: ControlLayout, b: NormalData, thai: Boolean, onDismiss: () -> Unit, onApply: (ControlLayout) -> Unit, onDelete: () -> Unit, onCopy: () -> Unit) {
    var name by remember { mutableStateOf(b.text.default) }
    //the click-event list is the single source of truth: the command menu replaces it, "more keys" adds to it
    var events by remember { mutableStateOf(b.clickEvents) }
    val action = StudioAction.entries.firstOrNull { it != StudioAction.MOVE && it.events == events }
    var hold by remember { mutableStateOf(b.isToggleable) }
    var passThrough by remember { mutableStateOf(b.isPenetrable) }
    var draggable by remember { mutableStateOf(b.isSwipple && !b.isPenetrable) }
    var color by remember { mutableStateOf(layout.colorOf(b)) }
    var advanced by remember { mutableStateOf(false) }
    var keyMenu by remember { mutableStateOf(false) }
    var rebinding by remember { mutableStateOf(false) }
    val startSize = if (b.buttonSize.type == com.movtery.layer_controller.data.ButtonSize.Type.Percentage) b.buttonSize.heightPercentage.toFloat() else 1400f
    var size by remember { mutableFloatStateOf(startSize.coerceIn(600f, 4000f)) }
    var opacity by remember { mutableFloatStateOf(layout.opacityOf(b)) }
    var menu by remember { mutableStateOf(false) }
    AllDialog(
        title = stringResource(R.string.all_studio_button_settings),
        onDismissRequest = onDismiss,
        actions = {
            AllButton("🗑 " + stringResource(R.string.all_studio_delete), onDelete, compact = true, style = AllButtonStyle.Danger)
            //copies the saved state of this button (OK first to copy unsaved changes too)
            AllButton("⧉ " + stringResource(R.string.all_studio_copy), onCopy, compact = true)
            AllButton(stringResource(R.string.generic_cancel), onDismiss, compact = true)
            AllButton(stringResource(R.string.generic_confirm), {
                var next = layout
                if (events != b.clickEvents) next = next.setButtonEvents(b.uuid, events, thai)
                if (hold != b.isToggleable || passThrough != b.isPenetrable || draggable != b.isSwipple) next = next.setButtonBehavior(b.uuid, hold, passThrough, draggable)
                if (color != layout.colorOf(b)) next = next.setButtonColor(b.uuid, color)
                if (name.isNotBlank() && name != b.text.default) next = next.setButtonName(b.uuid, name.trim())
                if (size != startSize) {
                    val ratio = b.buttonSize.widthPercentage.toFloat() / b.buttonSize.heightPercentage.coerceAtLeast(1)
                    next = next.updateButton(b.uuid) { it.copy(buttonSize = it.buttonSize.copy(
                        type = com.movtery.layer_controller.data.ButtonSize.Type.Percentage,
                        heightPercentage = size.roundToInt(), widthPercentage = (size * ratio).roundToInt().coerceIn(300, 10000),
                        widthReference = com.movtery.layer_controller.data.ButtonSize.Reference.ScreenHeight,
                        heightReference = com.movtery.layer_controller.data.ButtonSize.Reference.ScreenHeight
                    )) }
                }
                if (opacity != layout.opacityOf(b)) next = next.setButtonOpacity(b.uuid, opacity)
                onApply(next)
            }, compact = true, style = AllButtonStyle.Primary)
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SettingRow(stringResource(R.string.all_studio_name)) {
                OutlinedTextField(value = name, onValueChange = { name = it.take(24) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            SettingRow(stringResource(R.string.all_studio_command)) {
                Box {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AllButton((action?.label(thai) ?: comboLabel(events)) + " ▾", { menu = true }, compact = true)
                        //V1.2: bind any key / combo after the button was made
                        AllButton("⌨ " + stringResource(R.string.all_studio_change_key), { rebinding = true }, compact = true)
                    }
                    com.movtery.zalithlauncher.all.ui.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        StudioAction.entries.filter { it != StudioAction.MOVE }.forEach { a ->
                            com.movtery.zalithlauncher.all.ui.DropdownMenuItem(text = { Text(a.label(thai) + "  ·  " + a.inputHint(thai)) }, onClick = { events = a.events; menu = false })
                        }
                    }
                }
            }
            SettingRow(stringResource(R.string.all_studio_size)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(value = size, onValueChange = { size = it }, valueRange = 600f..4000f, modifier = Modifier.weight(1f), colors = SliderDefaults.colors())
                    Text("${(size / 100).roundToInt()}", fontSize = 13.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                }
            }
            SettingRow(stringResource(R.string.all_studio_opacity)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 0.1f..1f, modifier = Modifier.weight(1f))
                    Text("${(opacity * 100).roundToInt()}%", fontSize = 13.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                }
            }
            //advanced options stay folded so the first screen stays simple
            Text(
                (if (advanced) "▾ " else "▸ ") + stringResource(R.string.all_studio_more_options),
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SelectBlue,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable { advanced = !advanced }.padding(vertical = 6.dp)
            )
            if (advanced) {
                StudioSwitch(stringResource(R.string.all_studio_hold), stringResource(R.string.all_studio_hold_hint), hold) { hold = it }
                StudioSwitch(stringResource(R.string.all_studio_pass), stringResource(R.string.all_studio_pass_hint), passThrough) { passThrough = it; if (it) draggable = false }
                StudioSwitch(stringResource(R.string.all_studio_drag), stringResource(R.string.all_studio_drag_hint), draggable) { draggable = it; if (it) passThrough = false }
                SettingRow(stringResource(R.string.all_studio_color)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StudioColors.forEach { c ->
                            Box(Modifier.size(28.dp).clip(CircleShape).background(c)
                                .border(if (c.copy(alpha = 1f) == color.copy(alpha = 1f)) 3.dp else 1.dp, if (c.copy(alpha = 1f) == color.copy(alpha = 1f)) SelectBlue else Color.Gray, CircleShape)
                                .clickable { color = c })
                        }
                    }
                }
                SettingRow(stringResource(R.string.all_studio_keys)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        events.forEach { e ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(eventName(e, thai), fontSize = 12.sp, modifier = Modifier.weight(1f))
                                //the last key cannot be removed: a button must always do something
                                if (events.size > 1) Text("✕", color = Color(0xFFE53935), fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clip(CircleShape).clickable { events = events - e }.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                        }
                        Box {
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
        }
    }
    if (rebinding) CustomKeyDialog(
        title = stringResource(R.string.all_studio_change_key), initialName = name, initialEvents = events,
        onDismiss = { rebinding = false }
    ) { newName, newEvents -> events = newEvents; if (newName.isNotBlank()) name = newName; rebinding = false }
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
            Text(hint, fontSize = 11.sp, color = Color(0xFF55554F))
        }
        androidx.compose.material3.Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun SettingRow(label: String, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
        Box(Modifier.weight(1f)) { content() }
    }
}
