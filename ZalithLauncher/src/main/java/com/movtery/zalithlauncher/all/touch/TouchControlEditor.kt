package com.movtery.zalithlauncher.all.touch

import com.movtery.zalithlauncher.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import kotlin.math.roundToInt

private enum class Dlg { NONE, ADD, IMAGE, ACTION, PROFILES, NAME, MSG, RESET, COLOR, GROUP, GEDIT, PROPS }

/** group helpers (V2 group panel): a rename / delete reaches every member and every "G:<mode>:<group>" event of every button */
private fun groupOfEvent(e: String): String? = if (e.isGroupEvent()) e.removePrefix("G:").split(":", limit = 2).getOrNull(1) else null
internal fun TouchLayout.renameGroup(old: String, new: String): TouchLayout {
    fun ev(l: List<String>) = l.map { e -> if (groupOfEvent(e) == old) groupEvent(e.removePrefix("G:").substringBefore(":"), new) else e }
    return copy(items = items.map { (if (it.group == old) it.copy(group = new) else it).mapAllEvents(::ev) },
        hiddenGroups = hiddenGroups.map { if (it == old) new else it }.toSet(), groups = groups.map { if (it == old) new else it }.distinct())
}
internal fun TouchLayout.deleteGroup(g: String): TouchLayout {
    fun ev(l: List<String>) = l.filterNot { groupOfEvent(it) == g }
    return copy(items = items.map { (if (it.group == g) it.copy(group = null) else it).mapAllEvents(::ev) }, hiddenGroups = hiddenGroups - g, groups = groups - g)
}

private val CAPTION_COLORS = listOf(0, 0xFFFFFFFF.toInt(), 0xFFFFD27A.toInt(), 0xFFFF9A90.toInt(), 0xFF8BBCFF.toInt(), 0xFFCFB3FF.toInt(), 0xFF5EEAD4.toInt(), 0xFF20242B.toInt())

/** Studio text uses the app's own font (Noto Sans Thai) */
@Composable
fun TouchControlEditor(initial: TouchLayout, onExit: () -> Unit, preview: Boolean = false) {
    val context = LocalContext.current
    TouchMaterial {
        androidx.compose.material3.ProvideTextStyle(androidx.compose.material3.LocalTextStyle.current.copy(fontFamily = remember { TouchFont.family(context) }, color = TouchTheme.text)) {
            TouchControlEditorBody(initial, onExit, preview)
        }
    }
}

/** what the button DOES (its key), not what it looks like: the settings header must not call a swapped-icon button by the old icon name */
private fun TouchItem.what(): String = when {
    msgs != null -> "ปุ่มข้อความ"
    kind == TouchKind.BUTTON && events.isNotEmpty() -> events.joinToString(" + ") { eventLabel(it) }
    else -> title()
}

private fun TouchItem.title(): String = when (kind) {
    TouchKind.DPAD -> "D-Pad"
    TouchKind.JOYSTICK -> "Joystick"
    else -> label ?: sprite.substringAfterLast("/").removePrefix("control_").removePrefix("key_")
}

private fun eventLabel(e: String): String = when {
    e.isGuiEvent() -> "GUI"
    e.contains("toggle_mouse") -> "Mouse"
    e.contains("scroll_up") -> "Scroll Up"
    e.contains("scroll_down") -> "Scroll Down"
    e.startsWith("L:") -> "ALL: " + e.substringAfterLast(".")
    e.isGroupEvent() -> e.removePrefix("G:").split(":", limit = 2).let { (when (it[0]) { "show" -> "เปิด"; "hide" -> "ปิด"; else -> "สลับ" }) + "กลุ่ม " + it.getOrElse(1) { "" } }
    e.startsWith("T:") -> "“" + e.substring(2).take(12) + "”"
    e.contains("MOUSE_BUTTON_LEFT") -> "Mouse L"
    e.contains("MOUSE_BUTTON_RIGHT") -> "Mouse R"
    e.contains("MOUSE_BUTTON_MIDDLE") -> "Mouse M"
    else -> e.removePrefix("K:GLFW_KEY_")
}

/**
 * ALL TOUCH Studio: lay out, resize, re-skin and add keyboard keys for the control layer, on top of the running game.
 * Drag a widget to move it; tap it to select (size, behaviour, picture, action, delete). Changes are saved as they are made.
 */
@Composable
private fun TouchControlEditorBody(initial: TouchLayout, onExit: () -> Unit, preview: Boolean) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    var layout by remember { mutableStateOf(initial) }
    TouchSkin.current = layout.skin // sprites (canvas, pickers) follow the profile's skin
    var selId by remember { mutableStateOf<String?>(null) }
    var dlg by remember { mutableStateOf(Dlg.NONE) }
    var touchKind by remember { mutableStateOf(0) } // which list the key dialog edits: 0 press, 1 long press, 2 double tap
    var addingKey by remember { mutableStateOf(false) } // the key dialog ADDS to the button's keys (Multi-Key) instead of replacing them
    var screen by remember { mutableStateOf(IntSize(1, 1)) }
    var saved by remember { mutableStateOf(true) }
    var pickPart by remember { mutableStateOf<String?>(null) } // D-Pad / Joystick piece the picker is for (null = whole button)
    val scope = rememberCoroutineScope()

    var base by remember { mutableStateOf(initial) } // last SAVED state: what Undo returns to (live drag / slider changes are not in it)
    val undo = remember { mutableListOf<TouchLayout>() }
    var undoCount by remember { mutableStateOf(0) }
    val redo = remember { mutableListOf<TouchLayout>() }
    var redoCount by remember { mutableStateOf(0) }
    var placing by remember { mutableStateOf<TouchItem?>(null) } // a new button waiting for the player to tap its position (no free spot)
    var menu by remember { mutableStateOf(false) }     // the header "⋯" menu (button set / reset)
    var leftOpen by remember { mutableStateOf(true) }  // V2: tool panel (left) and group panel (right) fold away; the header folds too
    var rightOpen by remember { mutableStateOf(true) }
    var away by remember { mutableStateOf(false) }     // side panels folded for now: a tap on empty canvas, or while a button is dragged / resized
    var dragAway by remember { mutableStateOf(false) } // …and the fold came from a drag / resize, so it comes back when that ends
    val lOn = leftOpen && !away; val rOn = rightOpen && !away
    fun foldForDrag() { if (!away) { away = true; dragAway = true } }
    fun unfoldAfterDrag() { if (dragAway) { away = false; dragAway = false } }
    var headerOpen by remember { mutableStateOf(true) }
    var leftMode by remember { mutableStateOf("tools") } // tools | keys | system
    var propTab by remember { mutableStateOf("general") }
    var returnProps by remember { mutableStateOf(false) } // a dialog opened from the settings dialog comes back to it
    var targetGroup by remember { mutableStateOf<String?>(null) } // new buttons join this group (chosen in the group panel)
    var previewMode by remember { mutableStateOf(false) } // V2 "ดูตัวอย่าง": panels hidden, tap a button to try its actions
    var pvOff by remember { mutableStateOf(emptySet<String>()) } // groups hidden right now in the preview
    var pvGui by remember { mutableStateOf(false) }               // the preview's "hide all buttons" state
    var moving by remember { mutableStateOf(false) }
    var resizing by remember { mutableStateOf(false) } // a resize handle is being dragged
    var tbSize by remember { mutableStateOf(IntSize(1, 1)) }
    var decSize by remember { mutableStateOf(IntSize(1, 1)) }
    var gEdit by remember { mutableStateOf<String?>(null) }                 // group being managed ("" = a new one)
    var editorHidden by remember { mutableStateOf(emptySet<String>()) }     // groups hidden in the Studio only (the eye of the group panel); never saved
    var expanded by remember { mutableStateOf(emptySet<String>()) }

    fun commit(next: TouchLayout, why: String = "edit") {
        studioLog { "commit($why) moved=" + moved(layout, next) }
        if (next != base) { undo.add(base); if (undo.size > 40) undo.removeAt(0); undoCount = undo.size; redo.clear(); redoCount = 0 }
        base = next
        layout = next; saved = TouchStore.save(next)
    }
    /** layout replaced from disk (Reset / profile switch): nothing to undo across profiles */
    fun reload() { layout = TouchStore.load(context); base = layout; undo.clear(); undoCount = 0; redo.clear(); redoCount = 0; selId = null; saved = true; placing = null }
    fun doUndo() {
        if (undo.isEmpty()) return
        val prev = undo.removeAt(undo.size - 1); undoCount = undo.size
        studioLog { "undo" }
        redo.add(layout); redoCount = redo.size
        base = prev; layout = prev; saved = TouchStore.save(prev)
        if (layout.items.none { it.id == selId }) selId = null
    }
    fun doRedo() {
        if (redo.isEmpty()) return
        val nx = redo.removeAt(redo.size - 1); redoCount = redo.size
        undo.add(base); undoCount = undo.size
        base = nx; layout = nx; saved = TouchStore.save(nx)
        if (layout.items.none { it.id == selId }) selId = null
    }
    androidx.compose.runtime.DisposableEffect(Unit) { studioLog { "OPEN items=${layout.items.size}" }; onDispose { studioLog { "CLOSE" } } }
    fun edit(id: String, f: (TouchItem) -> TouchItem) = commit(layout.copy(items = layout.items.map { if (it.id == id) f(it) else it }))
    // Android Photo Picker (no storage permission; falls back to the system file chooser on phones without it)
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = selId
        if (uri != null && target != null) scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { TouchStyle.importPicture(it, target + (pickPart?.let { p -> "_$p" } ?: "")) } }.getOrNull()
                    ?: TouchStyle.ImportResult.Fail("เปิดไฟล์ไม่ได้")
            }
            when (r) {
                is TouchStyle.ImportResult.Ok -> if (pickPart != null) {
                    val part = pickPart!!
                    layout.items.firstOrNull { it.id == target }?.parts?.get(part)?.let { TouchStyle.deletePicture(it) }
                    edit(target) { it.copy(parts = it.parts + (part to r.name)) }
                } else {
                    layout.items.firstOrNull { it.id == target }?.image?.let { TouchStyle.deletePicture(it) }
                    edit(target) { it.copy(image = r.name, origSprite = it.origSprite ?: it.sprite, sprite = BLANK_BASE) }
                }
                is TouchStyle.ImportResult.Fail -> android.widget.Toast.makeText(context, r.reason, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    val sel = layout.items.firstOrNull { it.id == selId }

    fun spriteOf(i: TouchItem) = TouchSprites.hitSprite(context, i)
    /** drawn / touchable now: the Studio hides a group with its eye; the preview follows the group and GUI actions it has run */
    fun shown(i: TouchItem): Boolean = if (previewMode) (i.group == null || i.group !in pvOff) && (!pvGui || i.events.any { it.isGuiEvent() }) else i.group !in editorHidden
    fun hit(p: Offset) = layout.items.lastOrNull { i ->
        if (!shown(i)) return@lastOrNull false
        val (tl, sz) = touchRect(i, spriteOf(i), screen, density)
        p.x in tl.x..(tl.x + sz.x) && p.y in tl.y..(tl.y + sz.y)
    }

    /** A new button goes to a free spot (never on top of another); with no free spot the player taps where it should go. It ends up selected. */
    fun addItem(n0: TouchItem, defaults: Boolean = true) {
        val n1 = if (defaults) studioDefault(n0) else n0 // v1.2 defaults (56 dp / 48 dp system, no auto toggle): only for the button being added (a copy keeps its own)
        val n = if (n1.group == null && targetGroup != null) n1.copy(group = targetGroup) else n1
        val sp = spriteOf(n)
        val dens = density
        val drawer = ArrayList<Pair<Offset, Offset>>().apply { // the open panels and the header hide part of the screen: a new button must land where it can be seen
            if (lOn) add(Offset(0f, 0f) to Offset(((screen.width / dens) * 0.20f).coerceIn(154f, 235f) * dens, screen.height.toFloat()))
            if (rOn) ((screen.width / dens) * 0.24f).coerceIn(190f, 280f).let { w -> add(Offset(screen.width - w * dens, 0f) to Offset(w * dens, screen.height.toFloat())) }
            if (headerOpen) add(Offset(0f, 0f) to Offset(screen.width.toFloat(), 54f * dens))
        }
        val hotbar = listOf(Offset(screen.width * 0.27f, screen.height * 0.84f) to Offset(screen.width * 0.46f, screen.height * 0.16f)) // the game's hotbar (and the toolbar under it) is never covered
        //movement controls go to the left thumb, everything else to the right thumb
        val prefer = if (n.kind == TouchKind.BUTTON) Offset(0.78f, 0.5f) else Offset(0.14f, 0.70f)
        val spot = freeSpot(layout.items.map { touchRect(it, spriteOf(it), screen, density) } + drawer + hotbar, touchRect(n, sp, screen, density).second, screen, prefer)
        if (spot == null) {
            placing = n; selId = null
            android.widget.Toast.makeText(context, "ไม่มีที่ว่าง — แตะบนจอตรงที่จะวางปุ่ม", android.widget.Toast.LENGTH_LONG).show()
        } else {
            val at = n.copy(x = spot.x, y = spot.y)
            commit(layout.copy(items = layout.items + at), "add"); selId = at.id
            if (at.msgs != null) dlg = Dlg.MSG // a new message button asks for its first message at once
        }
    }

    fun simulate(i: TouchItem, kind: Int) { // preview: run the group / GUI actions for real, name the rest ("what would be pressed")
        val evs = when (kind) { 0 -> i.events + i.clickEvents; 1 -> i.longEvents; else -> i.doubleEvents }
        val sent = ArrayList<String>()
        evs.forEach { e ->
            when {
                e.isGuiEvent() -> pvGui = !pvGui
                e.isGroupEvent() -> pvOff = pvOff.afterGroupEvent(e)
                else -> sent.add(eventLabel(e))
            }
        }
        if (kind == 0 && i.kind != TouchKind.BUTTON) sent.add("เดิน WASD")
        if (sent.isNotEmpty()) android.widget.Toast.makeText(context, "จำลอง: " + sent.joinToString(" + "), android.widget.Toast.LENGTH_SHORT).show()
    }
    fun togglePreview() { previewMode = !previewMode; selId = null; dlg = Dlg.NONE; menu = false; if (previewMode) { pvOff = layout.hiddenGroups; pvGui = false } }
    fun copySel() { val s = sel ?: return; addItem(TouchStore.copyLayout(TouchLayout(listOf(s))).items.first().copy(id = newTouchId()), false) }
    fun deleteSel() { val s = sel ?: return; commit(layout.copy(items = layout.items.filterNot { it.id == s.id }), "delete"); selId = null }
    fun openFromProps(d: Dlg) { returnProps = true; dlg = d }
    val profileName = remember(dlg, layout) { TouchStore.activeName(context) }
    val leftW = ((screen.width / density) * 0.20f).coerceIn(154f, 235f).dp
    val rightW = ((screen.width / density) * 0.24f).coerceIn(190f, 280f).dp
    val topPad = if (headerOpen) 54.dp else 8.dp

    // V2 Studio: header bar (top) · tool panel (left) · group panel (right), both fold away · context bar over the selected button · tabbed settings dialog · preview
    // preview (opened from Settings, no game behind): lighter scrim so the sample game screen stays visible for placing buttons
    Box(Modifier.fillMaxSize().background(if (preview) Color(0x55101418) else Color(0xE6101418))) {
        val animTick = rememberAnimTick(layout)
        Canvas(
            Modifier.fillMaxSize().pointerInput(previewMode) {
                if (previewMode) return@pointerInput
                var dragging: String? = null
                var cur = layout
                var dragStartPos = Offset.Zero
                var dragFirst = false
                detectDragGestures(
                    onDragStart = { p -> cur = layout; dragging = if (placing != null) null else hit(p)?.id; if (placing == null) selId = dragging; moving = dragging != null; if (dragging != null) foldForDrag(); dragStartPos = p; dragFirst = true; studioLog { "dragStart pos=$p hit=$dragging screen=$screen items=${layout.items.size}" } },
                    onDragEnd = { studioLog { "dragEnd item=$dragging" }; dragging = null; moving = false; unfoldAfterDrag(); commit(layout, "dragEnd") },
                    onDragCancel = { studioLog { "dragCancel item=$dragging (not saved)" }; dragging = null; moving = false; unfoldAfterDrag() },
                    onDrag = { change, delta ->
                        change.consume()
                        if (dragFirst) { dragFirst = false; studioLog { "drag pointer=${change.id.value} start=$dragStartPos first=$delta" } }
                        val id = dragging ?: return@detectDragGestures
                        cur = layout.copy(items = layout.items.map { // always from the CURRENT layout: another finger's change (Add, Reset, slider) is never overwritten by an old snapshot
                            if (it.id == id) it.copy(
                                x = (it.x + delta.x / screen.width).coerceIn(0.02f, 0.98f),
                                y = (it.y + delta.y / screen.height).coerceIn(0.02f, 0.98f)
                            ) else it
                        })
                        layout = cur
                    }
                )
            }.pointerInput(previewMode) {
                if (previewMode) detectTapGestures(
                    onTap = { p -> hit(p)?.let { simulate(it, 0) } },
                    onLongPress = { p -> hit(p)?.let { simulate(it, 1) } },
                    onDoubleTap = { p -> hit(p)?.let { simulate(it, 2) } })
                else detectTapGestures(onTap = { p ->
                    val w = placing
                    if (w != null) { // the player chose the spot for a button that found no free one
                        val at = w.copy(x = (p.x / screen.width).coerceIn(0.04f, 0.96f), y = (p.y / screen.height).coerceIn(0.04f, 0.96f))
                        placing = null; commit(layout.copy(items = layout.items + at), "place"); selId = at.id
                        if (at.msgs != null) dlg = Dlg.MSG
                    } else { selId = hit(p)?.id; if (selId == null) away = true; studioLog { "tap pos=$p hit=$selId" } } // empty canvas: nothing selected and the side panels step aside (☰ / กลุ่ม bring them back)
                })
            }
        ) {
            screen = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1))
            animTick.longValue // read so a GIF frame change redraws
            layout.items.forEach { if (shown(it)) drawTouchItem(context, it, screen, density, 0, Offset.Zero, false, selected = !previewMode && it.id == selId) }
        }

        // resize handles (BUTTON only): right edge = width, bottom edge = height, corner = both with the proportions kept; a tag shows the numbers while dragging
        if (sel != null && sel.kind == TouchKind.BUTTON && !moving && !previewMode && dlg == Dlg.NONE) {
            val (htl, hsz) = touchRect(sel, spriteOf(sel), screen, density)
            val aspect = spriteOf(sel)?.let { it.height.toFloat() / it.width } ?: 1f
            val selId0 = sel.id
            ResizeHandles(htl, hsz, screen, aspect,
                getItem = { layout.items.firstOrNull { it.id == selId0 } },
                onLive = { f -> layout = layout.copy(items = layout.items.map { if (it.id == selId0) f(it) else it }) },
                onStart = { resizing = true; foldForDrag() }, onEnd = { resizing = false; unfoldAfterDrag(); commit(layout, "resize") })
            if (resizing) Panel(Modifier.offset { androidx.compose.ui.unit.IntOffset((htl.x + hsz.x / 2f).toInt() - (70 * density).toInt(), (htl.y - 46 * density).toInt().coerceAtLeast(0)) }) {
                Text("กว้าง ${sel.sizeDp.roundToInt()} × สูง ${(sel.sizeDp * aspect * sel.heightScale).roundToInt()} dp", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 14.sp)
            }
        }

        // left: tools (create / manage), folds away
        if (lOn && !previewMode) Panel(Modifier.align(Alignment.TopStart).padding(start = 6.dp, top = topPad, bottom = 6.dp).width(leftW).fillMaxHeight()) {
            ToolsPanel(mode = leftMode, onMode = { leftMode = it }, targetGroup = targetGroup, onItem = { n -> addItem(n) }, onAll = { dlg = Dlg.ADD },
                onNewGroup = { gEdit = ""; dlg = Dlg.GEDIT }, onProfiles = { dlg = Dlg.PROFILES }, onClose = { leftOpen = false })
        }

        // right: groups, folds away. name = the group new buttons join; eye = show / hide in the Studio only; ⋯ = manage
        if (rOn && !previewMode) Panel(Modifier.align(Alignment.TopEnd).padding(end = 6.dp, top = topPad, bottom = 6.dp).width(rightW).fillMaxHeight()) {
            Row(Modifier.padding(start = 12.dp, top = 8.dp, end = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("กลุ่มปุ่ม", Modifier.weight(1f), fontSize = 15.sp); Pill("＋", { gEdit = ""; dlg = Dlg.GEDIT }); Pill("✕", { rightOpen = false })
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val all = layout.allGroups()
                if (all.isEmpty()) Text("ยังไม่มีกลุ่ม: กด ＋ เพื่อสร้าง แล้วใส่ปุ่มเข้ากลุ่ม", color = TouchTheme.dim, fontSize = 12.sp)
                all.forEach { g ->
                    val members = layout.items.filter { it.group == g }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(TouchTheme.raised).then(if (g == targetGroup) Modifier.border(1.dp, TouchTheme.accent, RoundedCornerShape(12.dp)) else Modifier).padding(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (g in expanded) "▾" else "▸", Modifier.clickable { expanded = if (g in expanded) expanded - g else expanded + g }.padding(8.dp))
                            Text(g + "  " + members.size + if (g in layout.hiddenGroups) "  (ซ่อนตอนเริ่ม)" else "", Modifier.weight(1f).clickable { targetGroup = if (targetGroup == g) null else g }.padding(vertical = 8.dp), color = if (g == targetGroup) TouchTheme.accent else TouchTheme.text, maxLines = 2, fontSize = 13.sp)
                            Text(if (g in editorHidden) "⊘" else "◉", Modifier.clickable { editorHidden = if (g in editorHidden) editorHidden - g else editorHidden + g }.padding(8.dp))
                            Text("⋯", Modifier.clickable { gEdit = g; dlg = Dlg.GEDIT }.padding(8.dp))
                        }
                        if (g in expanded) {
                            if (members.isEmpty()) Text("ยังไม่มีปุ่ม", Modifier.padding(start = 8.dp, bottom = 4.dp), color = TouchTheme.dim, fontSize = 12.sp)
                            members.forEach { m -> Text(m.caption ?: m.title(), Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (m.id == selId) TouchTheme.accent.copy(alpha = 0.35f) else TouchTheme.surface).clickable { selId = m.id }.padding(horizontal = 10.dp, vertical = 7.dp), color = TouchTheme.text, fontSize = 12.sp, maxLines = 1) }
                        }
                    }
                }
                Text("ไม่มีกลุ่ม: " + layout.items.count { it.group == null } + " ปุ่ม", color = TouchTheme.dim, fontSize = 12.sp)
                Text("◉ / ⊘ = ซ่อนดูเฉพาะใน Studio · ค่าเริ่มเกมอยู่ใน ⋯ · เปิด/ปิดในเกมใช้คำสั่ง ควบคุมกลุ่ม", color = TouchTheme.dim, fontSize = 11.sp)
            }
            Pill("จัดการกลุ่มที่เลือก", { (targetGroup ?: sel?.group ?: layout.allGroups().firstOrNull())?.let { gEdit = it; dlg = Dlg.GEDIT } }, Modifier.padding(8.dp).fillMaxWidth(), enabled = layout.allGroups().isNotEmpty())
        }

        // header: tools · profile · undo / redo · groups · preview · done. Saving is automatic: only a failure is shown
        if (headerOpen) Panel(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 4.dp)) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill("☰ เครื่องมือ", { if (away) { away = false; dragAway = false; leftOpen = true } else leftOpen = !leftOpen; if (leftOpen) leftMode = "tools" }, color = if (lOn) TouchTheme.accent else null)
                Pill(profileName + " ▾", { dlg = Dlg.PROFILES })
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                Pill("↶", { doUndo() }, enabled = undoCount > 0)
                Pill("↷", { doRedo() }, enabled = redoCount > 0)
                Pill("▤ กลุ่ม", { if (away) { away = false; dragAway = false; rightOpen = true } else rightOpen = !rightOpen }, color = if (rOn) TouchTheme.accent else null)
                Pill(if (previewMode) "✕ ออกจากตัวอย่าง" else "◉ ดูตัวอย่าง", { togglePreview() }, color = if (previewMode) TouchTheme.accent else null)
                Pill("⋯", { menu = !menu })
                if (!saved) Pill(stringResource(R.string.all_t_ca504bef), {}, color = TouchTheme.danger)
                Pill(stringResource(R.string.all_t_47be792e), onExit, primary = true)
                Pill("▴", { headerOpen = false })
            }
        } else Pill("▾", { headerOpen = true }, Modifier.align(Alignment.TopCenter).padding(top = 4.dp))

        if (menu) {
            Box(Modifier.fillMaxSize().clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { menu = false })
            Panel(Modifier.align(Alignment.TopEnd).padding(top = topPad, end = 10.dp).width(300.dp)) {
                MenuRow("ชุดปุ่ม", TouchSkin.name(layout.skin) + "  (แตะเพื่อสลับ)") { commit(layout.copy(skin = TouchSkin.next(layout.skin)), "skin") }
                MenuRow("รีเซ็ตโปรไฟล์นี้", "คืนเป็นค่าเริ่มต้น (ถามก่อนทำ)") { menu = false; dlg = Dlg.RESET }
            }
        }

        if (previewMode) Panel(Modifier.align(Alignment.TopCenter).padding(top = topPad + 4.dp)) {
            Text("โหมดตัวอย่าง · แตะปุ่มเพื่อลอง (กลุ่ม / GUI ทำงานจริง) · กดค้างและแตะสองครั้งใช้ได้", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = TouchTheme.dim, fontSize = 12.sp)
        }

        if (placing != null) Panel(Modifier.align(Alignment.TopCenter).padding(top = topPad + 4.dp)) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.all_t_d0f661ac))
                Pill(stringResource(R.string.all_cs_place_center), { // no free spot: put it in the middle of the screen (above the hotbar) instead of tapping
                    placing?.let { w -> val at = w.copy(x = 0.5f, y = 0.45f); placing = null; commit(layout.copy(items = layout.items + at), "place"); selId = at.id; if (at.msgs != null) dlg = Dlg.MSG }
                })
                Pill(stringResource(R.string.all_t_7d3094b8), { placing = null })
            }
        }

        // context bar over (or under) the selected button: edit · copy · group · delete. Everything else lives in the settings dialog
        if (sel != null && !previewMode && !moving && !resizing && dlg == Dlg.NONE) {
            val (tl, sz) = touchRect(sel, spriteOf(sel), screen, density)
            val gap = (8 * density).toInt()
            val below = (tl.y + sz.y).toInt() + gap
            val placedBelow = below + tbSize.height < screen.height - gap
            val y = if (placedBelow) below else (tl.y.toInt() - tbSize.height - gap).coerceAtLeast(if (headerOpen) (58 * density).toInt() else gap)
            val lo = if (lOn) ((leftW.value + 6) * density).toInt() + gap else gap // stay between the open side panels
            val hi = (screen.width - (if (rOn) ((rightW.value + 6) * density).toInt() + gap else gap) - tbSize.width).coerceAtLeast(lo)
            val x = (tl.x + sz.x / 2f - tbSize.width / 2f).toInt().coerceIn(lo, hi)
            Panel(Modifier.offset { androidx.compose.ui.unit.IntOffset(x, y) }.onSizeChanged { tbSize = it }) {
                Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill("⚙ แก้ไข", { propTab = "general"; dlg = Dlg.PROPS })
                    Pill("⧉ คัดลอก", { copySel() })
                    Pill("⇄ กลุ่ม", { propTab = "group"; dlg = Dlg.PROPS })
                    Pill(stringResource(R.string.all_t_221622d8), { deleteSel() }, color = TouchTheme.danger)
                }
            }
        }

    }

    val close = { dlg = if (returnProps && sel != null) Dlg.PROPS else Dlg.NONE; returnProps = false } // a dialog opened from the settings dialog returns to it
    val done = { returnProps = false; dlg = Dlg.NONE } // a finished single choice (colour, picture, group) ends at the layout: no second Close
    when (dlg) {
        Dlg.NONE -> {}
        Dlg.PROPS -> sel?.let { s ->
            PropsDialog(s, layout, propTab, { propTab = it }, spriteOf(s)?.let { it.height.toFloat() / it.width } ?: 1f,
                edit = { f -> edit(s.id, f) },
                live = { f -> layout = layout.copy(items = layout.items.map { if (it.id == s.id) f(it) else it }) },
                commitLive = { commit(layout) },
                openAction = { k, add -> touchKind = k; addingKey = add; openFromProps(Dlg.ACTION) },
                openName = { openFromProps(Dlg.NAME) }, openColor = { openFromProps(Dlg.COLOR) }, openImage = { openFromProps(Dlg.IMAGE) }, openMsg = { openFromProps(Dlg.MSG) },
                pickPicture = { part -> pickPart = part; pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                restorePart = { part -> s.parts[part]?.let { TouchStyle.deletePicture(it) }; edit(s.id) { it.copy(parts = it.parts - part) } },
                openGroupPicker = { openFromProps(Dlg.GROUP) }, openGroupManage = { g -> gEdit = g; openFromProps(Dlg.GEDIT) },
                onCopy = { returnProps = false; dlg = Dlg.NONE; copySel() }, onDelete = { returnProps = false; dlg = Dlg.NONE; deleteSel() },
                onClose = { returnProps = false; dlg = Dlg.NONE })
        }
        Dlg.ADD -> TouchDialog("คีย์บอร์ดทั้งหมด", close) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AddPicker(Modifier.weight(1f).fillMaxWidth()) { n -> close(); addItem(n) }
            }
        }
        Dlg.NAME -> sel?.let { s ->
            // 0 = icon only, 1 = icon + name (name under the icon), 2 = name only (replaces the icon)
            var mode by remember(s.id) { mutableStateOf(if (s.caption == null) 0 else if (s.captionIcon) 1 else 2) }
            var text by remember(s.id) { mutableStateOf(s.caption ?: "") }
            var sc by remember(s.id) { mutableStateOf(s.captionScale) }
            var col by remember(s.id) { mutableStateOf(s.captionColor) }
            val limit = if (mode == 1) 8 else 12
            val apply = {
                val t = text.trim().take(limit)
                if (mode == 0 || t.isEmpty()) edit(s.id) { it.copy(caption = null, captionIcon = false) }
                else edit(s.id) { it.copy(caption = t, captionIcon = mode == 1, captionScale = sc, captionColor = col) }
                done()
            }
            TouchDialog("ชื่อบนปุ่ม", close) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // buttons first: on a landscape phone the keyboard hides the bottom of this dialog
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Pill(stringResource(R.string.all_t_55c86611), { apply() }, primary = true)
                        Pill(stringResource(R.string.all_t_5a604c41), { mode = 0 }, color = if (mode == 0) TouchTheme.accent else null)
                        Pill(stringResource(R.string.all_t_bd31c3e7), { mode = 1 }, color = if (mode == 1) TouchTheme.accent else null)
                        Pill(stringResource(R.string.all_t_6781fab3), { mode = 2 }, color = if (mode == 2) TouchTheme.accent else null)
                    }
                    Text("ปุ่มยังกด " + s.events.joinToString(" + ") { eventLabel(it) } + " เหมือนเดิม" + when (mode) {
                        0 -> " · แสดงแต่ไอคอน"
                        1 -> " · ชื่อจะอยู่ใต้ปุ่ม (ไม่เกิน 8 ตัวอักษร)"
                        else -> " · ชื่อจะแทนไอคอน (ไม่เกิน 12 ตัวอักษร)"
                    }, color = TouchTheme.dim)
                    if (mode != 0) {
                        OutlinedTextField(text, { text = it.take(limit) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.all_t_48391cb3)) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { apply() }))
                        Text(stringResource(R.string.all_t_da0ab428))
                        Slider(sc, { sc = it }, valueRange = 0.6f..1.6f)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            CAPTION_COLORS.forEach { c ->
                                Box(Modifier.size(34.dp).background(if (c == 0) Color(0xFFA9B4C6) else Color(c), RoundedCornerShape(6.dp))
                                    .then(if (c == col) Modifier.border(2.dp, TouchTheme.accent, RoundedCornerShape(6.dp)) else Modifier).clickable { col = c }, contentAlignment = Alignment.Center) {
                                    if (c == 0) Text(stringResource(R.string.all_t_a5461753), fontSize = 9.sp, color = Color(0xFF1E2430))
                                }
                            }
                        }
                    }
                }
            }
        }
        Dlg.MSG -> sel?.takeIf { it.msgs != null }?.let { s ->
            TouchMsgSettings(s, { m -> edit(s.id) { it.copy(msgs = m) } }, close)
        }
        Dlg.COLOR -> sel?.let { s ->
            TouchDialog("เลือกสี", close) {
                ColorPickerContent(s.tint, remember { TouchRecentColors.get(context) },
                    onApply = { c -> TouchRecentColors.add(context, c); edit(s.id) { it.copy(tint = c) }; done() },
                    onOriginal = { edit(s.id) { it.copy(tint = 0) }; done() })
            }
        }
        Dlg.RESET -> androidx.compose.ui.window.Dialog(close) {
            Panel {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.all_t_de121938), fontSize = 17.sp)
                    Text(stringResource(R.string.all_t_f9a89107), color = TouchTheme.dim)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(stringResource(R.string.all_t_7d3094b8), close)
                        Pill(stringResource(R.string.all_t_d0284f46), { TouchStore.resetActive(context); reload(); close() }, color = TouchTheme.danger)
                    }
                }
            }
        }
        Dlg.PROFILES -> TouchDialog("โปรไฟล์ปุ่ม", close) {
            TouchProfilesPanel(manage = true, textColor = Color.Unspecified) { reload() }
        }
        Dlg.IMAGE -> TouchDialog("เปลี่ยนภาพ (ไม่เปลี่ยนการทำงาน)", close) {
            SpriteGrid(TouchCatalog.all(context).filter { it != "touch_controls/control_dpad_cross" }) { s -> sel?.let { edit(it.id) { i -> i.copy(sprite = s, origSprite = i.origSprite ?: i.sprite) } }; done() }
        }
        Dlg.GEDIT -> gEdit?.let { g0 ->
            val creating = g0.isEmpty()
            var name by remember(g0) { mutableStateOf(g0) }
            var startHidden by remember(g0) { mutableStateOf(g0 in layout.hiddenGroups) }
            val back = { close() }
            val n = name.trim()
            val dup = n.isNotEmpty() && n != g0 && n in layout.allGroups()
            TouchDialog(if (creating) "สร้างกลุ่มใหม่" else "จัดการกลุ่ม · $g0", back) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it.replace(":", "").take(16) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("ชื่อกลุ่ม") })
                    if (dup) Text("ชื่อนี้มีแล้ว", color = TouchTheme.danger)
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("ซ่อนเมื่อเริ่มเกม (ลิ้นชัก)", Modifier.weight(1f)); Switch(startHidden, { startHidden = it }) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(if (creating) "สร้างกลุ่ม" else "บันทึก", {
                            var l = layout
                            if (creating) l = l.copy(groups = l.groups + n) else if (n != g0) { l = l.renameGroup(g0, n); editorHidden = editorHidden.map { if (it == g0) n else it }.toSet(); expanded = expanded.map { if (it == g0) n else it }.toSet() }
                            commit(l.copy(hiddenGroups = if (startHidden) l.hiddenGroups + n else l.hiddenGroups - n), "group"); close()
                        }, primary = true, enabled = n.isNotEmpty() && !dup)
                        if (!creating) {
                            Pill("คัดลอกกลุ่ม", {
                                var cn = g0 + " สำเนา"; var k = 2; while (cn in layout.allGroups()) cn = g0 + " สำเนา " + k++
                                val copies = TouchStore.copyLayout(TouchLayout(layout.items.filter { it.group == g0 })).items.map { it.copy(id = newTouchId(), group = cn, x = (it.x + 0.03f).coerceAtMost(0.98f), y = (it.y + 0.03f).coerceAtMost(0.98f)) }
                                commit(layout.copy(items = layout.items + copies, hiddenGroups = if (g0 in layout.hiddenGroups) layout.hiddenGroups + cn else layout.hiddenGroups, groups = if (copies.isEmpty()) layout.groups + cn else layout.groups), "group"); close()
                            })
                            Pill("ลบกลุ่ม", { commit(layout.deleteGroup(g0), "group"); editorHidden = editorHidden - g0; expanded = expanded - g0; close() }, color = TouchTheme.danger)
                        }
                    }
                    if (!creating) {
                        val members = layout.items.filter { it.group == g0 }
                        Text("สมาชิก (" + members.size + ")", color = TouchTheme.dim)
                        members.forEach { m ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(m.caption ?: m.title(), Modifier.weight(1f), maxLines = 1)
                                Pill("นำออก", { edit(m.id) { it.copy(group = null) } })
                            }
                        }
                        sel?.takeIf { it.group != g0 }?.let { s -> Pill("ใส่ปุ่มที่เลือก (" + (s.caption ?: s.title()) + ") เข้ากลุ่มนี้", { edit(s.id) { it.copy(group = g0) } }) }
                        Text("ลบกลุ่ม = ปุ่มสมาชิกอยู่ต่อ (ไม่มีกลุ่ม) และคำสั่งควบคุมกลุ่มนี้ถูกถอดออกจากทุกปุ่ม ย้อนกลับได้", color = TouchTheme.dim, fontSize = 12.sp)
                    }
                }
            }
        }
        Dlg.GROUP -> sel?.let { s ->
            var name by remember(s.id) { mutableStateOf("") }
            val groups = layout.allGroups()
            TouchDialog("กลุ่มของปุ่ม", close) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(name, { name = it.replace(":", "").take(16) }, Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.all_t_f33c1213)) })
                        Pill(stringResource(R.string.all_t_3c6e7df8), { name.trim().takeIf { it.isNotEmpty() }?.let { g -> edit(s.id) { it.copy(group = g) }; done() } }, enabled = name.isNotBlank())
                    }
                    if (s.group != null) Pill(stringResource(R.string.all_t_cbcec9d6), { edit(s.id) { it.copy(group = null) }; done() })
                    groups.forEach { g ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill(g, { edit(s.id) { it.copy(group = g) }; done() }, color = if (g == s.group) TouchTheme.accent else null)
                            Text(stringResource(R.string.all_t_acfdca0c), Modifier.weight(1f), color = TouchTheme.dim)
                            Switch(g in layout.hiddenGroups, { v -> commit(layout.copy(hiddenGroups = if (v) layout.hiddenGroups + g else layout.hiddenGroups - g), "group") })
                        }
                    }
                    Text("ปุ่มในกลุ่มเดียวกันแสดง/ซ่อนพร้อมกัน ใช้ปุ่มที่ตั้ง \"สลับกลุ่ม\" เพื่อเปิด/ปิดในเกม (ลิ้นชัก)", color = TouchTheme.dim)
                }
            }
        }
        Dlg.ACTION -> TouchDialog(if (touchKind == 8) "ทำเมื่อแตะแล้วปล่อยนิ้ว" else if (touchKind >= 3) "ปุ่มของทิศ / ล็อกนี้" else if (touchKind == 1) "ทำเมื่อกดค้าง" else if (touchKind == 2) "ทำเมื่อแตะสองครั้ง" else if (addingKey) "เพิ่มปุ่มที่จะกดพร้อมกัน" else "เลือกปุ่มที่จะกด", close) {
            fun apply(ev: String) { sel?.let { edit(it.id) { i -> i.withEvents(touchKind, if (addingKey) i.eventsOf(touchKind).withEvent(ev) else listOf(ev)) } } }
            var text by remember { mutableStateOf("") }
            var ctl by remember { mutableStateOf(false) }          // V2 "ควบคุมกลุ่ม" page
            var op by remember { mutableStateOf("toggle") }
            var chosen by remember { mutableStateOf(setOf<String>()) }
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (ctl) Pill("‹ กลับ", { ctl = false }) else Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { // Multi-Action: a text sent to chat when the button is pressed
                    OutlinedTextField(text, { text = it.take(MSG_LEN) }, Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.all_t_12c01c9c)) })
                    Pill(stringResource(R.string.all_t_8a7912bd), { cleanMsg(text)?.let { apply("T:$it"); close() } }, enabled = cleanMsg(text) != null)
                }
                if (!ctl) Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton({ ctl = true }) { Text("ควบคุมกลุ่ม…") } // first in the row: it was off-screen at the end
                    TouchStore.SPECIAL_ACTIONS.forEach { (label, ev) -> OutlinedButton({ apply(ev); close() }) { Text(label) } }
                }
                if (ctl) {
                    val groups = layout.allGroups()
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("เมื่อเกิดเหตุการณ์นี้ ให้ทำอะไร?", color = TouchTheme.dim)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("toggle" to "เปิด/ปิดสลับกัน", "show" to "เปิดเท่านั้น", "hide" to "ปิดเท่านั้น").forEach { (m, n) -> Pill(n, { op = m }, primary = op == m) } }
                        Text("เลือกกลุ่มที่ต้องการควบคุม (หลายกลุ่มได้)", color = TouchTheme.dim)
                        if (groups.isEmpty()) Text("ยังไม่มีกลุ่ม: ปิดหน้านี้แล้วกด \"กลุ่ม\" ที่แถบล่างเพื่อสร้าง", color = TouchTheme.dim)
                        groups.forEach { g ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(TouchTheme.raised).clickable { chosen = if (g in chosen) chosen - g else chosen + g }.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Checkbox(g in chosen, { chosen = if (it) chosen + g else chosen - g })
                                Text(g + "  ·  " + layout.items.count { it.group == g } + " ปุ่ม  ·  เริ่มเกม: " + if (g in layout.hiddenGroups) "ซ่อน" else "แสดง", maxLines = 2)
                            }
                        }
                        val picked = groups.filter { it in chosen }
                        Text(if (picked.isEmpty()) "ยังไม่ได้เลือกกลุ่ม" else mapOf("toggle" to "เปิด/ปิดสลับกัน: ", "show" to "เปิด: ", "hide" to "ปิด: ").getValue(op) + picked.joinToString(" และ "), color = TouchTheme.accent)
                        if (sel?.group in picked && op != "show") Text("ปุ่มนี้อยู่ในกลุ่มที่เลือก จึงอาจถูกซ่อนไปด้วยและกดกลับไม่ได้ ควรย้ายปุ่มไปกลุ่มที่แสดงตลอด", color = TouchTheme.danger, fontSize = 12.sp)
                        Pill(if (picked.isEmpty()) "เลือกกลุ่มก่อน" else "เพิ่มคำสั่ง (" + picked.size + " กลุ่ม)", {
                            sel?.let { s -> edit(s.id) { i -> i.withEvents(touchKind, picked.map { groupEvent(op, it) }.fold(i.eventsOf(touchKind)) { a, e -> a.withEvent(e) }) } }; close()
                        }, Modifier.fillMaxWidth(), primary = true, enabled = picked.isNotEmpty())
                    }
                } else SpriteGrid(TouchCatalog.keyboard(context), Modifier.weight(1f).fillMaxWidth()) { s ->
                    keyboardEvent(s)?.let { ev -> apply(ev) }; close()
                }
            }
        }
    }
}

/** Wide dialog (the platform default is too narrow for a sprite grid in landscape) */
@Composable
private fun TouchDialog(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    androidx.compose.ui.window.Dialog(onClose, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Panel(Modifier.fillMaxWidth(0.82f).fillMaxHeight(0.94f)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), fontSize = 16.sp); Pill(stringResource(R.string.all_t_1b6d61ea), onClose)
                }
                Box(Modifier.padding(top = 10.dp).weight(1f).fillMaxWidth()) { content() }
            }
        }
    }
}

@Composable
private fun SpriteGrid(sprites: List<String>, modifier: Modifier = Modifier.fillMaxSize(), onPick: (String) -> Unit) {
    val context = LocalContext.current
    LazyVerticalGrid(GridCells.Adaptive(72.dp), modifier, verticalArrangement = Arrangement.spacedBy(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(sprites) { s ->
            val bmp = TouchSprites.get(context, s)
            Box(Modifier.size(72.dp).background(Color(0x22FFFFFF), RoundedCornerShape(6.dp)).clickable { onPick(s) }, contentAlignment = Alignment.Center) {
                if (bmp != null) Image(bmp, s, Modifier.padding(4.dp), filterQuality = FilterQuality.None)
            }
        }
    }
}

private fun hex(c: Int) = String.format("%06X", c and 0xFFFFFF)

/** Colour (HSV sliders + HEX), opacity, player picture, reset: everything edits the selected item live; [onCommit] saves */
@Composable
private fun AppearancePanel(sel: TouchItem, onLive: ((TouchItem) -> TouchItem) -> Unit, onCommit: () -> Unit, onEdit: ((TouchItem) -> TouchItem) -> Unit, onPick: () -> Unit) {
    val hasPic = sel.image != null
    val hsv = remember(sel.id, sel.tint == 0) {
        val f = FloatArray(3)
        if (sel.tint != 0) android.graphics.Color.colorToHSV(sel.tint, f) else { f[0] = 210f; f[1] = 0.2f; f[2] = 0.8f }
        androidx.compose.runtime.mutableStateOf(f)
    }
    var hexText by remember(sel.id, sel.tint) { mutableStateOf(if (sel.tint != 0) hex(sel.tint) else "") }
    fun setHsv(h: Float, s: Float, v: Float, save: Boolean) {
        hsv.value = floatArrayOf(h, s, v)
        val c = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
        onLive { it.copy(tint = c) }
        if (save) onCommit()
    }
    Text(stringResource(R.string.all_t_a7be30a3))
    run {
        Text("ความทึบพื้นหลัง ${(sel.bgAlpha * 100).toInt()}%")
        Slider(sel.bgAlpha, { v -> onLive { it.copy(bgAlpha = v) } }, onValueChangeFinished = onCommit, valueRange = 0f..1f)
    }
    if (hasPic) {
        Text("ขนาดรูป ${(sel.imageScale * 100).toInt()}%")
        Slider(sel.imageScale, { v -> onLive { it.copy(imageScale = v) } }, onValueChangeFinished = onCommit, valueRange = 0.2f..1f)
    }
    Text((if (hasPic) "ความทึบรูป " else "ความทึบไอคอน ") + "${(sel.iconAlpha * 100).toInt()}%")
    Slider(sel.iconAlpha, { v -> onLive { it.copy(iconAlpha = v) } }, onValueChangeFinished = onCommit, valueRange = 0f..1f)
    if (sel.kind == TouchKind.BUTTON) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = onPick) { Text(stringResource(R.string.all_t_cc3b09cb)) }
            if (hasPic) OutlinedButton(onClick = { TouchStyle.deletePicture(sel.image); onEdit { it.copy(image = null, sprite = it.origSprite ?: it.sprite, origSprite = null) } }) { Text(stringResource(R.string.all_t_4a5c2c72)) }
        }
        if (hasPic) Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (sel.crop) "ตัดให้เต็มปุ่ม (Crop)" else "พอดีปุ่ม (Fit)", Modifier.weight(1f)); Switch(sel.crop, { v -> onEdit { it.copy(crop = v) } })
        }
    }
    OutlinedButton(onClick = { TouchStyle.deletePicture(sel.image); sel.parts.values.forEach { TouchStyle.deletePicture(it) }; hexText = ""; onEdit { it.copy(parts = emptyMap(), tint = 0, bgAlpha = 1f, iconAlpha = 1f, image = null, crop = false, imageScale = 0.6f, sprite = it.origSprite ?: it.sprite, origSprite = null, caption = null, captionIcon = false, captionScale = 1f, captionColor = 0) } }) { Text(stringResource(R.string.all_t_46bfe6ee)) }
}
internal const val BLANK_BASE = "touch_controls/control_blank_m"

private val DPAD_PARTS = listOf("up" to "ขึ้น", "down" to "ลง", "left" to "ซ้าย", "right" to "ขวา",
    "northwest" to "เฉียงซ้ายบน", "northeast" to "เฉียงขวาบน", "southwest" to "เฉียงซ้ายล่าง", "southeast" to "เฉียงขวาล่าง", "center" to "กลาง (ย่อ)")
private val STICK_PARTS = listOf("base" to "ฐาน", "thumb" to "หัว (ก้านจอย)")

/** Per-piece pictures for D-Pad / Joystick: pick a phone picture for one piece, or bring the original piece back. Hit areas and 8-way logic never change. */
@Composable
private fun PartsPanel(sel: TouchItem, onPick: (String) -> Unit, onRestore: (String) -> Unit) {
    Text(stringResource(R.string.all_t_2816a936))
    (if (sel.kind == TouchKind.DPAD) DPAD_PARTS else STICK_PARTS).forEach { (key, label) ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label + if (sel.parts.containsKey(key)) " ✓" else "", Modifier.weight(1f))
            OutlinedButton(onClick = { onPick(key) }) { Text(stringResource(R.string.all_t_18c57d25)) }
            if (sel.parts.containsKey(key)) OutlinedButton(onClick = { onRestore(key) }) { Text(stringResource(R.string.all_t_0074fca3)) }
        }
    }
}

private val KEY_BASE = 38.dp

/** A new button for a keyboard / mouse cell: the real sprite when there is one, otherwise the standard frame with the label drawn on it */
internal fun itemForCell(context: android.content.Context, c: KCell): TouchItem =
    if (c.sprite != null) newItemFor(context, c.sprite)
    else TouchItem(newTouchId(), TouchKind.BUTTON, BLANK_BASE, 0.5f, 0.5f, 46f, listOf(c.event), gameOnly = c.gameOnly, label = c.label)

/**
 * Add-a-button page: ONE screen shaped like a full PC keyboard (main block, navigation block, numpad), a Mouse row, then the touch-only
 * buttons. Tap a key = it is added at once; press and hold = what it is, what it does, its real Key Mapping and how it behaves.
 * The picture is a fixed size (keys never shrink); on a small screen it scrolls both ways.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AddPicker(modifier: Modifier, onItem: (TouchItem) -> Unit) {
    val context = LocalContext.current
    var info by remember { mutableStateOf<Triple<String, String, TouchItem>?>(null) } // title, body, item to add
    val frame = TouchSprites.get(context, BLANK_BASE)
    var zoom by remember { mutableStateOf(1.25f) } // keys never shrink below the base size; + makes them bigger (the picture scrolls)
    val KEY_U = KEY_BASE * zoom

    @Composable
    fun Key(c: KCell) {
        if (c.w < 0f) { Box(Modifier.size(KEY_U * -c.w, KEY_U)); return }
        val bmp = c.sprite?.let { TouchSprites.get(context, it) } ?: frame
        if (c.gameOnly) { // mouse: a normal square frame with the short mark, its name underneath
            Column(Modifier.width(KEY_U * c.w).combinedClickable(onClick = { onItem(itemForCell(context, c)) }, onLongClick = {
                info = Triple(c.en + " · " + c.th, c.hint, itemForCell(context, c))
            }), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(KEY_U).padding(1.5.dp), contentAlignment = Alignment.Center) {
                    if (bmp != null) Image(bmp, c.en, Modifier.fillMaxSize().padding(2.dp), filterQuality = FilterQuality.None)
                    Text(c.label, color = Color(0xFF1E2430), fontSize = 14.sp)
                }
                Text(c.th, fontSize = 11.sp)
            }
            return
        }
        Box(Modifier.size(KEY_U * c.w, KEY_U).padding(1.5.dp).background(Color(0x22FFFFFF), RoundedCornerShape(5.dp))
            .combinedClickable(onClick = { onItem(itemForCell(context, c)) }, onLongClick = {
                val it = itemForCell(context, c)
                info = Triple(c.en + (if (c.th.isNotEmpty() && c.th != c.en) " · " + c.th else ""), c.hint, it)
            }), contentAlignment = Alignment.Center) {
            if (bmp != null) Image(bmp, c.en, Modifier.fillMaxSize().padding(2.dp), filterQuality = FilterQuality.None)
            if (c.sprite == null) Text(c.label, color = Color(0xFF1E2430), maxLines = 1, fontSize = if (c.label.length > 3) 9.sp else 14.sp)
        }
    }

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.all_t_0f27a030), Modifier.weight(1f), color = Color(0xFF9AA4B2))
            Text(stringResource(R.string.all_t_9ad85607))
            OutlinedButton({ zoom = (zoom - 0.25f).coerceAtLeast(1f) }, enabled = zoom > 1f) { Text("−") }
            OutlinedButton({ zoom = (zoom + 0.25f).coerceAtMost(2f) }, enabled = zoom < 2f) { Text("+") }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (r in KB_MAIN.indices) Row {
                Row { KB_MAIN[r].forEach { Key(it) } }; Box(Modifier.width(KEY_U * 0.5f))
                Row { KB_NAV[r].forEach { Key(it) } }; Box(Modifier.width(KEY_U * 0.5f))
                Row { KB_NUM[r].forEach { Key(it) } }
            }
            Text(stringResource(R.string.all_t_18ac7013), Modifier.padding(top = 8.dp))
            Row { KB_MOUSE.forEach { Key(it) } }
            Text(stringResource(R.string.all_t_37cc9187), Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ onItem(newDpad(0.5f, 0.5f)) }) { Text(stringResource(R.string.all_t_1174a244)) }
                OutlinedButton({ onItem(newJoystick(0.5f, 0.5f)) }) { Text(stringResource(R.string.all_t_f5fc10b7)) }
                OutlinedButton({ onItem(newJoystick(0.5f, 0.5f, true)) }) { Text(stringResource(R.string.all_t_d33b7b79)) }
            }
            val touch = TouchCatalog.touch(context).filter { it != "touch_controls/control_dpad_cross" }
            touch.chunked(10).forEach { rowS ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    rowS.forEach { s ->
                        val bmp = TouchSprites.get(context, s)
                        Box(Modifier.size(52.dp).background(Color(0x22FFFFFF), RoundedCornerShape(6.dp)).combinedClickable(
                            onClick = { onItem(newItemFor(context, s)) },
                            onLongClick = {
                                val n = newItemFor(context, s)
                                val (nm, fn) = TOUCH_INFO[s] ?: (s.substringAfterLast("/") to "ปุ่มสัมผัส")
                                info = Triple(nm, fn, n)
                            }), contentAlignment = Alignment.Center) {
                            if (bmp != null) Image(bmp, s, Modifier.padding(3.dp), filterQuality = FilterQuality.None)
                        }
                    }
                }
            }
        }
        // details are shown inline under the keyboard (no second dialog on top of this one)
        info?.let { (title, body, item) ->
            Row(Modifier.fillMaxWidth().padding(top = 6.dp).background(Color(0x22FFFFFF), RoundedCornerShape(8.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(title)
                    Text("หน้าที่: $body", fontSize = 13.sp)
                    Text("Key Mapping ที่ใช้จริง: " + (item.events.joinToString(" + ") { eventLabel(it) }.ifEmpty { "ไม่มี (กรอบเปล่า)" }), color = Color(0xFF9AA4B2), fontSize = 13.sp)
                    Text(BEHAVIOUR_HINT, color = Color(0xFF9AA4B2), fontSize = 11.sp)
                }
                Button({ info = null; onItem(item) }) { Text(stringResource(R.string.all_t_1fce4366)) }
                TextButton({ info = null }) { Text(stringResource(R.string.all_t_1b6d61ea)) }
            }
        }
    }
}

/** Debug-only trace of everything that can move a button (tag ALLTOUCH); nothing is logged in release builds */
private inline fun studioLog(msg: () -> String) {
    if (com.movtery.zalithlauncher.BuildConfig.DEBUG) android.util.Log.i("ALLTOUCH", "studio: " + msg())
}

private fun moved(a: TouchLayout, b: TouchLayout): String {
    val old = a.items.associateBy { it.id }
    val d = b.items.filter { n -> old[n.id]?.let { o -> o.x != n.x || o.y != n.y } ?: true }
    return if (d.isEmpty()) "none" else d.joinToString(",") { n ->
        val o = old[n.id]; "${n.sprite.substringAfterLast("/")}(${o?.x?.let { "%.3f/%.3f".format(it, o.y) } ?: "new"}->${"%.3f/%.3f".format(n.x, n.y)})"
    }
}

/** V2 tabbed settings dialog of one button / D-Pad / Joystick. Every old settings function lives in a tab; nested pickers (key, name, colour, picture, group) return here. */
@Composable
private fun PropsDialog(
    sel: TouchItem, layout: TouchLayout, tab: String, onTab: (String) -> Unit, aspect: Float,
    edit: ((TouchItem) -> TouchItem) -> Unit, live: ((TouchItem) -> TouchItem) -> Unit, commitLive: () -> Unit,
    openAction: (Int, Boolean) -> Unit, openName: () -> Unit, openColor: () -> Unit, openImage: () -> Unit, openMsg: () -> Unit,
    pickPicture: (String?) -> Unit, restorePart: (String) -> Unit, openGroupPicker: () -> Unit, openGroupManage: (String) -> Unit,
    onCopy: () -> Unit, onDelete: () -> Unit, onClose: () -> Unit,
) {
    val isBtn = sel.kind == TouchKind.BUTTON
    val tabs = buildList {
        add("general" to "ทั่วไป")
        if (isBtn) add("actions" to "คำสั่ง") else add("directions" to "ทิศทาง")
        if (sel.msgs != null) add("messages" to "ข้อความ")
        add("style" to "รูปลักษณ์"); add("behavior" to "พฤติกรรม"); add("group" to "กลุ่ม")
    }
    val cur = if (tabs.any { it.first == tab }) tab else "general"
    androidx.compose.ui.window.Dialog(onClose, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Panel(Modifier.fillMaxWidth(0.88f).fillMaxHeight(0.94f)) {
            Row(Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("ตั้งค่าปุ่ม · " + sel.what(), Modifier.weight(1f), fontSize = 16.sp, maxLines = 1); Pill(stringResource(R.string.all_t_1b6d61ea), onClose)
            }
            Row(Modifier.weight(1f).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.width(120.dp).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tabs.forEach { (k, n) -> Pill(n, { onTab(k) }, Modifier.fillMaxWidth(), primary = k == cur) }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (cur) {
                        "general" -> {
                            if (isBtn) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("ชื่อบนปุ่ม", Modifier.weight(1f)); Text(sel.caption ?: "—", color = TouchTheme.dim); Pill(stringResource(R.string.all_t_5e775867), openName)
                            }
                            var xs by remember(sel.id, sel.x) { mutableStateOf(String.format(java.util.Locale.US, "%.1f", sel.x * 100)) }
                            var ys by remember(sel.id, sel.y) { mutableStateOf(String.format(java.util.Locale.US, "%.1f", sel.y * 100)) }
                            val setX = { xs.replace(',', '.').toFloatOrNull()?.let { v -> edit { it.copy(x = (v / 100f).coerceIn(0.02f, 0.98f)) } }; Unit }
                            val setY = { ys.replace(',', '.').toFloatOrNull()?.let { v -> edit { it.copy(y = (v / 100f).coerceIn(0.02f, 0.98f)) } }; Unit }
                            val kb = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal, imeAction = androidx.compose.ui.text.input.ImeAction.Done)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(xs, { xs = it.take(6) }, Modifier.weight(1f), singleLine = true, label = { Text("ตำแหน่ง X (%)") }, keyboardOptions = kb, keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { setX() }))
                                OutlinedTextField(ys, { ys = it.take(6) }, Modifier.weight(1f), singleLine = true, label = { Text("ตำแหน่ง Y (%)") }, keyboardOptions = kb, keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { setY() }))
                            }
                            Text("กด ✓ บนคีย์บอร์ดเพื่อใช้ค่า หรือลากปุ่มบนหน้าจอ", color = TouchTheme.dim, fontSize = 11.sp)
                            SizeFields(sel, aspect,
                                onApply = { w, h -> edit { it.copy(sizeDp = w, heightScale = if (h == null) it.heightScale else (h / (w * aspect)).coerceIn(MIN_HS, MAX_HS)) } },
                                onReset = { edit { it.copy(heightScale = 1f) } })
                        }
                        "actions" -> {
                            Text("กำหนดได้หลายคำสั่งต่อเหตุการณ์ · แตะคำสั่งเพื่อเอาออก", color = TouchTheme.dim, fontSize = 12.sp)
                            TriggerRow(sel, 0, "กดปุ่ม · Press", edit, openAction, true)
                            if (sel.msgs == null) {
                                TriggerRow(sel, 8, "แตะแล้วปล่อย · Click", edit, openAction, false)
                                TriggerRow(sel, 1, "กดค้าง · Long Press", edit, openAction, false)
                                TriggerRow(sel, 2, "แตะสองครั้ง · Double Tap", edit, openAction, false)
                            }
                        }
                        "directions" -> {
                            Text("ค่าเริ่มต้น WASD · ตั้งปุ่มแต่ละทิศได้ (ทิศเฉียงกดสองทิศพร้อมกัน)", color = TouchTheme.dim, fontSize = 12.sp)
                            listOf(3 to "ขึ้น", 4 to "ลง", 5 to "ซ้าย", 6 to "ขวา").forEach { (k, n) -> TriggerRow(sel, k, n, edit, openAction, false) }
                            if (sel.kind == TouchKind.JOYSTICK) TriggerRow(sel, 7, "ล็อกเดินหน้า (ลากเลยขอบขึ้นบน)", edit, openAction, false)
                            Pill(stringResource(R.string.all_t_1f20a4d3), { edit { it.copy(dirEvents = emptyMap(), lockEvents = emptyList()) } })
                        }
                        "messages" -> {
                            Text("ข้อความที่บันทึกไว้ " + (sel.msgs?.size ?: 0) + " รายการ (ส่งเมื่อกดปุ่ม)", color = TouchTheme.dim)
                            Pill(stringResource(R.string.all_t_bb7af620), openMsg)
                        }
                        "style" -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                QUICK_COLORS.forEach { c ->
                                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(50)).background(if (c == 0) Color(0xFFA9B4C6) else Color(c))
                                        .then(if (c == sel.tint) Modifier.border(2.dp, TouchTheme.accent, RoundedCornerShape(50)) else Modifier).clickable { edit { it.copy(tint = c) } }, contentAlignment = Alignment.Center) {
                                        if (c == 0) Text(stringResource(R.string.all_t_80a95aab), fontSize = 9.sp, color = Color(0xFF1E2430))
                                    }
                                }
                            }
                            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Pill(stringResource(R.string.all_t_076d56a9), openColor)
                                if (isBtn) {
                                    Pill(stringResource(R.string.all_t_a96b4620), openImage)
                                    Pill(stringResource(R.string.all_t_c00ea644), { pickPicture(null) })
                                    Pill(stringResource(R.string.all_t_5e775867), openName)
                                }
                            }
                            AppearancePanel(sel, onLive = live, onCommit = commitLive, onEdit = edit, onPick = { pickPicture(null) })
                            if (!isBtn) PartsPanel(sel, onPick = { pickPicture(it) }, onRestore = restorePart)
                        }
                        "behavior" -> {
                            if (isBtn) {
                                SwitchRow(stringResource(R.string.all_t_f9a5fbec), sel.toggle) { v -> edit { it.copy(toggle = v, dragCam = if (v) false else it.dragCam) } }
                                SwitchRow(stringResource(R.string.all_t_e898a369), sel.dragCam) { v -> edit { it.copy(dragCam = v, toggle = if (v) false else it.toggle) } }
                                SwitchRow(stringResource(R.string.all_t_29110cb1), sel.slideIn) { v -> edit { it.copy(slideIn = v) } }
                                Text(stringResource(R.string.all_t_ae14bb46), color = TouchTheme.dim, fontSize = 12.sp)
                            }
                            SwitchRow(stringResource(R.string.all_t_dfc37d44), sel.gameOnly) { v -> edit { it.copy(gameOnly = v) } }
                        }
                        else -> {
                            Text("กลุ่มปัจจุบัน: " + (sel.group ?: "ไม่มี") + if (sel.group in layout.hiddenGroups) " (ซ่อนตอนเริ่มเกม)" else "")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Pill(stringResource(R.string.all_t_afea86fb), openGroupPicker)
                                sel.group?.let { g -> Pill("จัดการกลุ่มนี้", { openGroupManage(g) }) }
                            }
                            Text("ให้ปุ่มนี้เปิด/ปิดกลุ่มในเกม: แท็บคำสั่ง → เพิ่ม → ควบคุมกลุ่ม…", color = TouchTheme.dim, fontSize = 12.sp)
                        }
                    }
                }
            }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill("⧉ คัดลอก", onCopy); Pill(stringResource(R.string.all_t_221622d8), onDelete, color = TouchTheme.danger)
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                Pill("เสร็จสิ้น", onClose, primary = true)
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(value, onChange) }
}

/** one trigger of a button (press / click / long press / double tap) or one direction: its actions as chips (tap = take out) + add (press can also replace) */
@Composable
private fun TriggerRow(sel: TouchItem, k: Int, name: String, edit: ((TouchItem) -> TouchItem) -> Unit, openAction: (Int, Boolean) -> Unit, replace: Boolean) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(TouchTheme.raised).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(name, Modifier.weight(1f), fontSize = 14.sp)
            if (replace) Pill(stringResource(R.string.all_t_5ed2f494), { openAction(k, false) })
            Pill(stringResource(R.string.all_t_f2ff8cd7), { openAction(k, true) })
        }
        val l = sel.eventsOf(k)
        if (l.isEmpty()) Text("ปิดอยู่", color = TouchTheme.dim)
        else androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            l.forEach { ev ->
                Pill(eventLabel(ev) + if (k != 0 || l.size > 1) " ×" else "", { edit { i -> i.withEvents(k, if (k == 0) i.eventsOf(0).withoutEvent(ev) else i.eventsOf(k) - ev) } })
            }
        }
    }
}
