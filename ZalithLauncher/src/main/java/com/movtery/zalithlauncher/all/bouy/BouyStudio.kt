package com.movtery.zalithlauncher.all.bouy

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

private enum class Dlg { NONE, ADD, QUICK, IMAGE, ACTION, PROFILES, NAME, MSG, RESET, COLOR }

private val CAPTION_COLORS = listOf(0, 0xFFFFFFFF.toInt(), 0xFFFFD27A.toInt(), 0xFFFF9A90.toInt(), 0xFF8BBCFF.toInt(), 0xFFCFB3FF.toInt(), 0xFF5EEAD4.toInt(), 0xFF20242B.toInt())

/** Studio text uses the app's own font (Noto Sans Thai) */
@Composable
fun BouyStudio(initial: BouyLayout, onExit: () -> Unit) {
    val context = LocalContext.current
    BouyMaterial {
        androidx.compose.material3.ProvideTextStyle(androidx.compose.material3.LocalTextStyle.current.copy(fontFamily = remember { BouyFont.family(context) }, color = BouyTheme.text)) {
            BouyStudioBody(initial, onExit)
        }
    }
}

/** what the button DOES (its key), not what it looks like: the settings header must not call a swapped-icon button by the old icon name */
private fun BouyItem.what(): String = when {
    msgs != null -> "ปุ่มข้อความ"
    kind == BouyKind.BUTTON && events.isNotEmpty() -> events.joinToString(" + ") { eventLabel(it) }
    else -> title()
}

private fun BouyItem.title(): String = when (kind) {
    BouyKind.DPAD -> "D-Pad"
    BouyKind.JOYSTICK -> "Joystick"
    else -> label ?: sprite.substringAfterLast("/").removePrefix("control_").removePrefix("key_")
}

private fun eventLabel(e: String): String = when {
    e.contains("scroll_up") -> "Scroll Up"
    e.contains("scroll_down") -> "Scroll Down"
    e.startsWith("L:") -> "ALL: " + e.substringAfterLast(".")
    e.contains("MOUSE_BUTTON_LEFT") -> "Mouse L"
    e.contains("MOUSE_BUTTON_RIGHT") -> "Mouse R"
    e.contains("MOUSE_BUTTON_MIDDLE") -> "Mouse M"
    else -> e.removePrefix("K:GLFW_KEY_")
}

/**
 * ALL BOUY Studio: lay out, resize, re-skin and add keyboard keys for the control layer, on top of the running game.
 * Drag a widget to move it; tap it to select (size, behaviour, picture, action, delete). Changes are saved as they are made.
 */
@Composable
private fun BouyStudioBody(initial: BouyLayout, onExit: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    var layout by remember { mutableStateOf(initial) }
    BouySkin.current = layout.skin // sprites (canvas, pickers) follow the profile's skin
    var selId by remember { mutableStateOf<String?>(null) }
    var dlg by remember { mutableStateOf(Dlg.NONE) }
    var screen by remember { mutableStateOf(IntSize(1, 1)) }
    var saved by remember { mutableStateOf(true) }
    var pickPart by remember { mutableStateOf<String?>(null) } // D-Pad / Joystick piece the picker is for (null = whole button)
    val scope = rememberCoroutineScope()

    var base by remember { mutableStateOf(initial) } // last SAVED state: what Undo returns to (live drag / slider changes are not in it)
    val undo = remember { mutableListOf<BouyLayout>() }
    var undoCount by remember { mutableStateOf(0) }
    var placing by remember { mutableStateOf<BouyItem?>(null) } // a new button waiting for the player to tap its position (no free spot)
    var more by remember { mutableStateOf(false) }
    var colorRow by remember { mutableStateOf(false) } // the "ตกแต่ง" row under the toolbar
    var menu by remember { mutableStateOf(false) }     // the "⋯" menu
    var adv by remember { mutableStateOf(false) }      // "ขั้นสูง" folded open inside the settings panel
    var moving by remember { mutableStateOf(false) }
    var resizing by remember { mutableStateOf(false) } // a resize handle is being dragged
    var tbSize by remember { mutableStateOf(IntSize(1, 1)) }
    var decSize by remember { mutableStateOf(IntSize(1, 1)) }

    fun commit(next: BouyLayout, why: String = "edit") {
        studioLog { "commit($why) moved=" + moved(layout, next) }
        if (next != base) { undo.add(base); if (undo.size > 40) undo.removeAt(0); undoCount = undo.size }
        base = next
        layout = next; saved = BouyStore.save(next)
    }
    /** layout replaced from disk (Reset / profile switch): nothing to undo across profiles */
    fun reload() { layout = BouyStore.load(context); base = layout; undo.clear(); undoCount = 0; selId = null; saved = true; placing = null }
    fun doUndo() {
        if (undo.isEmpty()) return
        val prev = undo.removeAt(undo.size - 1); undoCount = undo.size
        studioLog { "undo" }
        base = prev; layout = prev; saved = BouyStore.save(prev)
        if (layout.items.none { it.id == selId }) selId = null
    }
    androidx.compose.runtime.DisposableEffect(Unit) { studioLog { "OPEN items=${layout.items.size}" }; onDispose { studioLog { "CLOSE" } } }
    fun edit(id: String, f: (BouyItem) -> BouyItem) = commit(layout.copy(items = layout.items.map { if (it.id == id) f(it) else it }))
    // Android Photo Picker (no storage permission; falls back to the system file chooser on phones without it)
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = selId
        if (uri != null && target != null) scope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { BouyStyle.importPicture(it, target + (pickPart?.let { p -> "_$p" } ?: "")) } }.getOrNull()
                    ?: BouyStyle.ImportResult.Fail("เปิดไฟล์ไม่ได้")
            }
            when (r) {
                is BouyStyle.ImportResult.Ok -> if (pickPart != null) {
                    val part = pickPart!!
                    layout.items.firstOrNull { it.id == target }?.parts?.get(part)?.let { BouyStyle.deletePicture(it) }
                    edit(target) { it.copy(parts = it.parts + (part to r.name)) }
                } else {
                    layout.items.firstOrNull { it.id == target }?.image?.let { BouyStyle.deletePicture(it) }
                    edit(target) { it.copy(image = r.name, origSprite = it.origSprite ?: it.sprite, sprite = BLANK_BASE) }
                }
                is BouyStyle.ImportResult.Fail -> android.widget.Toast.makeText(context, r.reason, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    val sel = layout.items.firstOrNull { it.id == selId }
    androidx.compose.runtime.LaunchedEffect(selId) { more = false; colorRow = false; adv = false }

    fun spriteOf(i: BouyItem) = BouySprites.get(context, if (i.kind == BouyKind.JOYSTICK) i.sprite + "_base" else i.sprite)
    fun hit(p: Offset) = layout.items.lastOrNull { i ->
        val (tl, sz) = bouyRect(i, spriteOf(i), screen, density)
        p.x in tl.x..(tl.x + sz.x) && p.y in tl.y..(tl.y + sz.y)
    }

    /** A new button goes to a free spot (never on top of another); with no free spot the player taps where it should go. It ends up selected. */
    fun addItem(n: BouyItem) {
        val sp = BouySprites.get(context, if (n.kind == BouyKind.JOYSTICK) n.sprite + "_base" else n.sprite)
        val drawer = if (dlg == Dlg.QUICK) listOf(Offset(screen.width * 0.4f, 0f) to Offset(screen.width * 0.6f, screen.height.toFloat())) else emptyList() // the open drawer hides the right side: a new button must land where it can be seen
        val spot = freeSpot(layout.items.map { bouyRect(it, spriteOf(it), screen, density) } + drawer, bouyRect(n, sp, screen, density).second, screen)
        if (spot == null) {
            placing = n; selId = null
            android.widget.Toast.makeText(context, "ไม่มีที่ว่าง — แตะบนจอตรงที่จะวางปุ่ม", android.widget.Toast.LENGTH_LONG).show()
        } else {
            val at = n.copy(x = spot.x, y = spot.y)
            commit(layout.copy(items = layout.items + at), "add"); selId = at.id
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xE6101418))) {
        val animTick = rememberAnimTick(layout)
        Canvas(
            Modifier.fillMaxSize().pointerInput(Unit) {
                var dragging: String? = null
                var cur = layout
                var dragStartPos = Offset.Zero
                var dragFirst = false
                detectDragGestures(
                    onDragStart = { p -> cur = layout; dragging = if (placing != null) null else hit(p)?.id; if (placing == null) selId = dragging; moving = dragging != null; dragStartPos = p; dragFirst = true; studioLog { "dragStart pos=$p hit=$dragging screen=$screen items=${layout.items.size}" } },
                    onDragEnd = { studioLog { "dragEnd item=$dragging" }; dragging = null; moving = false; commit(layout, "dragEnd") },
                    onDragCancel = { studioLog { "dragCancel item=$dragging (not saved)" }; dragging = null; moving = false },
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
            }.pointerInput(Unit) {
                detectTapGestures(onTap = { p ->
                    val w = placing
                    if (w != null) { // the player chose the spot for a button that found no free one
                        val at = w.copy(x = (p.x / screen.width).coerceIn(0.04f, 0.96f), y = (p.y / screen.height).coerceIn(0.04f, 0.96f))
                        placing = null; commit(layout.copy(items = layout.items + at), "place"); selId = at.id
                    } else { selId = hit(p)?.id; studioLog { "tap pos=$p hit=$selId" } }
                })
            }
        ) {
            screen = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1))
            animTick.longValue // read so a GIF frame change redraws
            layout.items.forEach { drawBouyItem(context, it, screen, density, 0, Offset.Zero, false, selected = it.id == selId) }
        }

        // bottom bar: the four things needed all the time; profile / skin / reset live behind "⋯". Saving is automatic: only a failure is shown
        if (menu) {
            Box(Modifier.fillMaxSize().clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { menu = false })
            Panel(Modifier.align(Alignment.BottomCenter).padding(bottom = 58.dp).width(300.dp)) {
                MenuRow("โปรไฟล์", remember(dlg, layout) { BouyStore.activeName(context) }) { menu = false; dlg = Dlg.PROFILES }
                MenuRow("ชุดปุ่ม", BouySkin.name(layout.skin) + "  (แตะเพื่อสลับ)") { commit(layout.copy(skin = BouySkin.next(layout.skin)), "skin") }
                MenuRow("รีเซ็ตโปรไฟล์นี้", "คืนเป็นค่าเริ่มต้น (ถามก่อนทำ)") { menu = false; dlg = Dlg.RESET }
            }
        }
        Panel(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)) {
            androidx.compose.foundation.layout.Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill("เสร็จ", onExit, primary = true)
                Pill("+ เพิ่มปุ่ม", { dlg = Dlg.QUICK; selId = null; menu = false })
                Pill("↶", { doUndo() }, enabled = undoCount > 0)
                Pill("⋯", { menu = !menu })
                if (!saved) Pill("บันทึกไม่สำเร็จ!", {}, color = BouyTheme.danger)
            }
        }

        if (placing != null) Panel(Modifier.align(Alignment.TopCenter).padding(8.dp)) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("แตะบนจอตรงที่จะวางปุ่มใหม่"); Pill("ยกเลิก", { placing = null })
            }
        }

        if (sel != null && !moving && !resizing && !more && dlg != Dlg.QUICK) { // the drawer covers half the screen: the toolbar waits until it is closed
            val (tl, sz) = bouyRect(sel, spriteOf(sel), screen, density)
            val gap = (8 * density).toInt()
            val below = (tl.y + sz.y).toInt() + gap
            val placedBelow = below + tbSize.height < screen.height - (70 * density).toInt()
            val y = if (placedBelow) below else (tl.y.toInt() - tbSize.height - gap).coerceAtLeast(gap)
            val x = (tl.x + sz.x / 2f - tbSize.width / 2f).toInt().coerceIn(gap, (screen.width - tbSize.width - gap).coerceAtLeast(gap))
            // the main row never moves when "ตกแต่ง" opens: the colour / icon row is a second panel next to it (on the side away from the button)
            Panel(Modifier.offset { androidx.compose.ui.unit.IntOffset(x, y) }.onSizeChanged { tbSize = it }) {
                Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // one capsule for size: − makes it smaller, ＋ bigger (8 dp steps)
                    Row(Modifier.clip(RoundedCornerShape(50)).background(BouyTheme.raised), verticalAlignment = Alignment.CenterVertically) {
                        Text("−", Modifier.clickable { edit(sel.id) { it.copy(sizeDp = (it.sizeDp - 8f).coerceAtLeast(30f)) } }.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 16.sp)
                        Text("＋", Modifier.clickable { edit(sel.id) { it.copy(sizeDp = (it.sizeDp + 8f).coerceAtMost(260f)) } }.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 16.sp)
                    }
                    Pill(if (colorRow) "ตกแต่ง ▴" else "ตกแต่ง ▾", { colorRow = !colorRow })
                    if (sel.msgs != null) Pill("ข้อความ", { dlg = Dlg.MSG })
                    Pill("ลบ", { commit(layout.copy(items = layout.items.filterNot { it.id == sel.id }), "delete"); selId = null }, color = BouyTheme.danger)
                    Pill("ตั้งค่า", { more = true })
                }
            }
            if (colorRow) {
                val dy = if (placedBelow) y + tbSize.height + gap else (y - decSize.height - gap).let { if (it < gap) y + tbSize.height + gap else it }
                val dx = (tl.x + sz.x / 2f - decSize.width / 2f).toInt().coerceIn(gap, (screen.width - decSize.width - gap).coerceAtLeast(gap))
                Panel(Modifier.offset { androidx.compose.ui.unit.IntOffset(dx, dy) }.onSizeChanged { decSize = it }) {
                    Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            QUICK_COLORS.forEach { c ->
                                Box(Modifier.size(28.dp).clip(RoundedCornerShape(50)).background(if (c == 0) Color(0xFFA9B4C6) else Color(c))
                                    .then(if (c == sel.tint) Modifier.border(2.dp, BouyTheme.accent, RoundedCornerShape(50)) else Modifier).clickable { edit(sel.id) { it.copy(tint = c) } }, contentAlignment = Alignment.Center) {
                                    if (c == 0) Text("เดิม", fontSize = 9.sp, color = Color(0xFF1E2430))
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill("สีอื่น…", { dlg = Dlg.COLOR })
                            if (sel.kind == BouyKind.BUTTON) {
                                Pill("ไอคอน", { dlg = Dlg.IMAGE })
                                Pill("รูปของฉัน", { pickPart = null; pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                                Pill("ชื่อ", { dlg = Dlg.NAME })
                            } else Pill("รูปแต่ละชิ้น", { more = true })
                        }
                    }
                }
            }
        }

        // resize handles (BUTTON only): right edge = width, bottom edge = height, corner = both with the proportions kept; a tag shows the numbers while dragging
        if (sel != null && sel.kind == BouyKind.BUTTON && !moving && !more && dlg == Dlg.NONE) {
            val (htl, hsz) = bouyRect(sel, spriteOf(sel), screen, density)
            val aspect = spriteOf(sel)?.let { it.height.toFloat() / it.width } ?: 1f
            val selId0 = sel.id
            ResizeHandles(htl, hsz, screen, aspect,
                getItem = { layout.items.firstOrNull { it.id == selId0 } },
                onLive = { f -> layout = layout.copy(items = layout.items.map { if (it.id == selId0) f(it) else it }) },
                onStart = { resizing = true }, onEnd = { resizing = false; commit(layout, "resize") })
            if (resizing) Panel(Modifier.offset { androidx.compose.ui.unit.IntOffset((htl.x + hsz.x / 2f).toInt() - (70 * density).toInt(), (htl.y - 46 * density).toInt().coerceAtLeast(0)) }) {
                Text("กว้าง ${sel.sizeDp.roundToInt()} × สูง ${(sel.sizeDp * aspect * sel.heightScale).roundToInt()} dp", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 14.sp)
            }
        }

        if (sel != null && more) {
            Panel(Modifier.align(if (sel.x > 0.5f) Alignment.TopStart else Alignment.TopEnd).padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 62.dp).width(290.dp).fillMaxHeight()) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ตั้งค่า · " + sel.what(), Modifier.weight(1f), fontSize = 15.sp); Pill("ปิด", { more = false })
                    }
                    if (sel.kind == BouyKind.BUTTON) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("ปุ่มที่กด", color = BouyTheme.dim, fontSize = 12.sp)
                            Text(if (sel.events.isNotEmpty()) sel.events.joinToString(" + ") { eventLabel(it) } else "—")
                        }
                        Pill("เปลี่ยน", { dlg = Dlg.ACTION })
                    }
                    run {
                        val asp = spriteOf(sel)?.let { it.height.toFloat() / it.width } ?: 1f
                        SizeFields(sel, asp,
                            onApply = { w, h -> edit(sel.id) { it.copy(sizeDp = w, heightScale = if (h == null) it.heightScale else (h / (w * asp)).coerceIn(MIN_HS, MAX_HS)) } },
                            onReset = { edit(sel.id) { it.copy(heightScale = 1f) } })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("แสดงเฉพาะตอนเล่น", Modifier.weight(1f)); Switch(sel.gameOnly, { v -> edit(sel.id) { it.copy(gameOnly = v) } })
                    }
                    if (sel.kind != BouyKind.BUTTON) PartsPanel(sel, onPick = { part -> pickPart = part; pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onRestore = { part -> sel.parts[part]?.let { BouyStyle.deletePicture(it) }; edit(sel.id) { it.copy(parts = it.parts - part) } })
                    Text(if (adv) "▾ ขั้นสูง" else "▸ ขั้นสูง", Modifier.clickable { adv = !adv }.padding(vertical = 4.dp), color = BouyTheme.accent)
                    if (adv) {
                        if (sel.kind == BouyKind.BUTTON) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("ล็อกค้าง (แตะ = กด แตะอีกที = ปล่อย)", Modifier.weight(1f)); Switch(sel.toggle, { v -> edit(sel.id) { it.copy(toggle = v, dragCam = if (v) false else it.dragCam) } })
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("แตะแล้วลากต่อเพื่อหมุนกล้อง", Modifier.weight(1f)); Switch(sel.dragCam, { v -> edit(sel.id) { it.copy(dragCam = v, toggle = if (v) false else it.toggle) } })
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("รับนิ้วที่ลากมาจากปุ่มอื่น", Modifier.weight(1f)); Switch(sel.slideIn, { v -> edit(sel.id) { it.copy(slideIn = v) } })
                            }
                            Text("ล็อกค้างกับหมุนกล้องเปิดพร้อมกันไม่ได้", color = BouyTheme.dim, fontSize = 12.sp)
                        }
                        AppearancePanel(sel, onLive = { f -> layout = layout.copy(items = layout.items.map { if (it.id == sel.id) f(it) else it }) },
                            onCommit = { commit(layout) }, onEdit = { f -> edit(sel.id, f) },
                            onPick = { pickPart = null; pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                    }
                }
            }
        }

        // quick-add drawer: stays open so several buttons can be added in a row; the new button appears on the left
        if (dlg == Dlg.QUICK) Panel(Modifier.align(Alignment.TopEnd).fillMaxWidth(0.6f).fillMaxHeight().padding(top = 4.dp, bottom = 62.dp, end = 4.dp)) {
            QuickAdd(onItem = { n -> addItem(n) }, onAll = { dlg = Dlg.ADD }, onClose = { dlg = Dlg.NONE })
        }
    }

    val close = { dlg = Dlg.NONE }
    when (dlg) {
        Dlg.NONE -> {}
        Dlg.QUICK -> {} // drawn as a side drawer inside the Studio box, so the game stays visible
        Dlg.ADD -> BouyDialog("คีย์บอร์ดทั้งหมด", close) {
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
                close()
            }
            BouyDialog("ชื่อบนปุ่ม", close) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // buttons first: on a landscape phone the keyboard hides the bottom of this dialog
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Pill("ตกลง", { apply() }, primary = true)
                        Pill("ไอคอนอย่างเดียว", { mode = 0 }, color = if (mode == 0) BouyTheme.accent else null)
                        Pill("ไอคอน + ชื่อ", { mode = 1 }, color = if (mode == 1) BouyTheme.accent else null)
                        Pill("ชื่ออย่างเดียว", { mode = 2 }, color = if (mode == 2) BouyTheme.accent else null)
                    }
                    Text("ปุ่มยังกด " + s.events.joinToString(" + ") { eventLabel(it) } + " เหมือนเดิม" + when (mode) {
                        0 -> " · แสดงแต่ไอคอน"
                        1 -> " · ชื่อจะอยู่ใต้ปุ่ม (ไม่เกิน 8 ตัวอักษร)"
                        else -> " · ชื่อจะแทนไอคอน (ไม่เกิน 12 ตัวอักษร)"
                    }, color = BouyTheme.dim)
                    if (mode != 0) {
                        OutlinedTextField(text, { text = it.take(limit) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("ชื่อ เช่น ซูม") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { apply() }))
                        Text("ขนาดตัวอักษร")
                        Slider(sc, { sc = it }, valueRange = 0.6f..1.6f)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            CAPTION_COLORS.forEach { c ->
                                Box(Modifier.size(34.dp).background(if (c == 0) Color(0xFFA9B4C6) else Color(c), RoundedCornerShape(6.dp))
                                    .then(if (c == col) Modifier.border(2.dp, BouyTheme.accent, RoundedCornerShape(6.dp)) else Modifier).clickable { col = c }, contentAlignment = Alignment.Center) {
                                    if (c == 0) Text("อัตโนมัติ", fontSize = 9.sp, color = Color(0xFF1E2430))
                                }
                            }
                        }
                    }
                }
            }
        }
        Dlg.MSG -> sel?.takeIf { it.msgs != null }?.let { s ->
            BouyMsgSettings(s, { m -> edit(s.id) { it.copy(msgs = m) } }, close)
        }
        Dlg.COLOR -> sel?.let { s ->
            BouyDialog("เลือกสี", close) {
                ColorPickerContent(s.tint, remember { BouyRecentColors.get(context) },
                    onApply = { c -> BouyRecentColors.add(context, c); edit(s.id) { it.copy(tint = c) }; close() },
                    onOriginal = { edit(s.id) { it.copy(tint = 0) }; close() })
            }
        }
        Dlg.RESET -> androidx.compose.ui.window.Dialog(close) {
            Panel {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("รีเซ็ตโปรไฟล์นี้?", fontSize = 17.sp)
                    Text("ปุ่มทั้งหมดของโปรไฟล์นี้จะกลับเป็นค่าเริ่มต้น โปรไฟล์อื่นไม่เปลี่ยน", color = BouyTheme.dim)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("ยกเลิก", close)
                        Pill("รีเซ็ต", { BouyStore.resetActive(context); reload(); close() }, color = BouyTheme.danger)
                    }
                }
            }
        }
        Dlg.PROFILES -> BouyDialog("โปรไฟล์ปุ่ม", close) {
            BouyProfilesPanel(manage = true, textColor = Color.Unspecified) { reload() }
        }
        Dlg.IMAGE -> BouyDialog("เปลี่ยนภาพ (ไม่เปลี่ยนการทำงาน)", close) {
            SpriteGrid(BouyCatalog.all(context).filter { it != "touch_controls/control_dpad_cross" }) { s -> sel?.let { edit(it.id) { i -> i.copy(sprite = s, origSprite = i.origSprite ?: i.sprite) } }; close() }
        }
        Dlg.ACTION -> BouyDialog("เลือกปุ่มที่จะกด", close) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BouyStore.SPECIAL_ACTIONS.forEach { (label, ev) -> OutlinedButton({ sel?.let { edit(it.id) { i -> i.copy(events = listOf(ev)) } }; close() }) { Text(label) } }
                }
                SpriteGrid(BouyCatalog.keyboard(context), Modifier.weight(1f).fillMaxWidth()) { s ->
                    keyboardEvent(s)?.let { ev -> sel?.let { edit(it.id) { i -> i.copy(events = listOf(ev)) } } }; close()
                }
            }
        }
    }
}

/** Wide dialog (the platform default is too narrow for a sprite grid in landscape) */
@Composable
private fun BouyDialog(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    androidx.compose.ui.window.Dialog(onClose, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Panel(Modifier.fillMaxWidth(0.82f).fillMaxHeight(0.94f)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), fontSize = 16.sp); Pill("ปิด", onClose)
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
            val bmp = BouySprites.get(context, s)
            Box(Modifier.size(72.dp).background(Color(0x22FFFFFF), RoundedCornerShape(6.dp)).clickable { onPick(s) }, contentAlignment = Alignment.Center) {
                if (bmp != null) Image(bmp, s, Modifier.padding(4.dp), filterQuality = FilterQuality.None)
            }
        }
    }
}

private fun hex(c: Int) = String.format("%06X", c and 0xFFFFFF)

/** Colour (HSV sliders + HEX), opacity, player picture, reset: everything edits the selected item live; [onCommit] saves */
@Composable
private fun AppearancePanel(sel: BouyItem, onLive: ((BouyItem) -> BouyItem) -> Unit, onCommit: () -> Unit, onEdit: ((BouyItem) -> BouyItem) -> Unit, onPick: () -> Unit) {
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
    Text("— รูปลักษณ์ —")
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
    if (sel.kind == BouyKind.BUTTON) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = onPick) { Text("เลือกรูปภาพ") }
            if (hasPic) OutlinedButton(onClick = { BouyStyle.deletePicture(sel.image); onEdit { it.copy(image = null, sprite = it.origSprite ?: it.sprite, origSprite = null) } }) { Text("ใช้ไอคอน ALL") }
        }
        if (hasPic) Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (sel.crop) "ตัดให้เต็มปุ่ม (Crop)" else "พอดีปุ่ม (Fit)", Modifier.weight(1f)); Switch(sel.crop, { v -> onEdit { it.copy(crop = v) } })
        }
    }
    OutlinedButton(onClick = { BouyStyle.deletePicture(sel.image); sel.parts.values.forEach { BouyStyle.deletePicture(it) }; hexText = ""; onEdit { it.copy(parts = emptyMap(), tint = 0, bgAlpha = 1f, iconAlpha = 1f, image = null, crop = false, imageScale = 0.6f, sprite = it.origSprite ?: it.sprite, origSprite = null, caption = null, captionIcon = false, captionScale = 1f, captionColor = 0) } }) { Text("คืนค่ารูปลักษณ์เริ่มต้น (รวมไอคอนและชื่อ)") }
}
internal const val BLANK_BASE = "touch_controls/control_blank_m"

private val DPAD_PARTS = listOf("up" to "ขึ้น", "down" to "ลง", "left" to "ซ้าย", "right" to "ขวา",
    "northwest" to "เฉียงซ้ายบน", "northeast" to "เฉียงขวาบน", "southwest" to "เฉียงซ้ายล่าง", "southeast" to "เฉียงขวาล่าง", "center" to "กลาง (ย่อ)")
private val STICK_PARTS = listOf("base" to "ฐาน", "thumb" to "หัว (ก้านจอย)")

/** Per-piece pictures for D-Pad / Joystick: pick a phone picture for one piece, or bring the original piece back. Hit areas and 8-way logic never change. */
@Composable
private fun PartsPanel(sel: BouyItem, onPick: (String) -> Unit, onRestore: (String) -> Unit) {
    Text("— ภาพแต่ละชิ้นส่วน (รักษาสัดส่วน/ความโปร่งใส) —")
    (if (sel.kind == BouyKind.DPAD) DPAD_PARTS else STICK_PARTS).forEach { (key, label) ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label + if (sel.parts.containsKey(key)) " ✓" else "", Modifier.weight(1f))
            OutlinedButton(onClick = { onPick(key) }) { Text("เลือกรูป") }
            if (sel.parts.containsKey(key)) OutlinedButton(onClick = { onRestore(key) }) { Text("ต้นฉบับ") }
        }
    }
}

private val KEY_BASE = 38.dp

/** A new button for a keyboard / mouse cell: the real sprite when there is one, otherwise the standard frame with the label drawn on it */
internal fun itemForCell(context: android.content.Context, c: KCell): BouyItem =
    if (c.sprite != null) newItemFor(context, c.sprite)
    else BouyItem(newBouyId(), BouyKind.BUTTON, BLANK_BASE, 0.5f, 0.5f, 46f, listOf(c.event), gameOnly = c.gameOnly, label = c.label)

/**
 * Add-a-button page: ONE screen shaped like a full PC keyboard (main block, navigation block, numpad), a Mouse row, then the touch-only
 * buttons. Tap a key = it is added at once; press and hold = what it is, what it does, its real Key Mapping and how it behaves.
 * The picture is a fixed size (keys never shrink); on a small screen it scrolls both ways.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AddPicker(modifier: Modifier, onItem: (BouyItem) -> Unit) {
    val context = LocalContext.current
    var info by remember { mutableStateOf<Triple<String, String, BouyItem>?>(null) } // title, body, item to add
    val frame = BouySprites.get(context, BLANK_BASE)
    var zoom by remember { mutableStateOf(1.25f) } // keys never shrink below the base size; + makes them bigger (the picture scrolls)
    val KEY_U = KEY_BASE * zoom

    @Composable
    fun Key(c: KCell) {
        if (c.w < 0f) { Box(Modifier.size(KEY_U * -c.w, KEY_U)); return }
        val bmp = c.sprite?.let { BouySprites.get(context, it) } ?: frame
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
            Text("แตะ = เพิ่มทันที  •  แตะค้าง = รายละเอียด", Modifier.weight(1f), color = Color(0xFF9AA4B2))
            Text("ขนาดปุ่ม")
            OutlinedButton({ zoom = (zoom - 0.25f).coerceAtLeast(1f) }, enabled = zoom > 1f) { Text("−") }
            OutlinedButton({ zoom = (zoom + 0.25f).coerceAtMost(2f) }, enabled = zoom < 2f) { Text("+") }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (r in KB_MAIN.indices) Row {
                Row { KB_MAIN[r].forEach { Key(it) } }; Box(Modifier.width(KEY_U * 0.5f))
                Row { KB_NAV[r].forEach { Key(it) } }; Box(Modifier.width(KEY_U * 0.5f))
                Row { KB_NUM[r].forEach { Key(it) } }
            }
            Text("Mouse · เมาส์", Modifier.padding(top = 8.dp))
            Row { KB_MOUSE.forEach { Key(it) } }
            Text("ปุ่มสัมผัสพิเศษ (ไม่มีบนคีย์บอร์ด PC)", Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ onItem(newDpad(0.5f, 0.5f)) }) { Text("D-Pad") }
                OutlinedButton({ onItem(newJoystick(0.5f, 0.5f)) }) { Text("Joystick วงกลม") }
                OutlinedButton({ onItem(newJoystick(0.5f, 0.5f, true)) }) { Text("Joystick เหลี่ยม") }
            }
            val touch = BouyCatalog.touch(context).filter { it != "touch_controls/control_dpad_cross" }
            touch.chunked(10).forEach { rowS ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    rowS.forEach { s ->
                        val bmp = BouySprites.get(context, s)
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
                Button({ info = null; onItem(item) }) { Text("เพิ่มปุ่มนี้") }
                TextButton({ info = null }) { Text("ปิด") }
            }
        }
    }
}

/** Debug-only trace of everything that can move a button (tag ALLBOUY); nothing is logged in release builds */
private inline fun studioLog(msg: () -> String) {
    if (com.movtery.zalithlauncher.BuildConfig.DEBUG) android.util.Log.i("ALLBOUY", "studio: " + msg())
}

private fun moved(a: BouyLayout, b: BouyLayout): String {
    val old = a.items.associateBy { it.id }
    val d = b.items.filter { n -> old[n.id]?.let { o -> o.x != n.x || o.y != n.y } ?: true }
    return if (d.isEmpty()) "none" else d.joinToString(",") { n ->
        val o = old[n.id]; "${n.sprite.substringAfterLast("/")}(${o?.x?.let { "%.3f/%.3f".format(it, o.y) } ?: "new"}->${"%.3f/%.3f".format(n.x, n.y)})"
    }
}
