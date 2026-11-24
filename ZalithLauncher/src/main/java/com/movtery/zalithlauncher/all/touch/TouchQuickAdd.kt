package com.movtery.zalithlauncher.all.touch

import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SCROLL_DOWN_SINGLE
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SCROLL_UP_SINGLE
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SWITCH_IME
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_SWITCH_MENU
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_TOGGLE_GUI
import com.movtery.zalithlauncher.ui.control.event.LAUNCHER_EVENT_TOGGLE_MOUSE
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val QUICK_COLORS = listOf(0, 0xFFE53935.toInt(), 0xFFFB8C00.toInt(), 0xFFFDD835.toInt(), 0xFF43A047.toInt(), 0xFF1E88E5.toInt(), 0xFF8E24AA.toInt(), 0xFFEC407A.toInt(), 0xFF212121.toInt(), 0xFFFAFAFA.toInt())

/** First spot (nearest the right-hand thumb zone) where a [size] box fits on screen without touching any of [taken] (top-left, size); null = the screen is full. Returns the CENTRE as screen fractions. */
internal fun freeSpot(taken: List<Pair<Offset, Offset>>, size: Offset, screen: IntSize, prefer: Offset = Offset(0.78f, 0.5f)): Offset? {
    val w = screen.width.toFloat(); val h = screen.height.toFloat()
    val pad = 6f
    val cands = ArrayList<Offset>()
    var fy = 0.14f
    while (fy <= 0.80f) { var fx = 0.06f; while (fx <= 0.94f) { cands.add(Offset(fx, fy)); fx += 0.04f }; fy += 0.06f }
    cands.sortBy { (it.x - prefer.x) * (it.x - prefer.x) * 2f + (it.y - prefer.y) * (it.y - prefer.y) }
    return cands.firstOrNull { c ->
        val l = c.x * w - size.x / 2f; val t = c.y * h - size.y / 2f
        l >= 0f && t >= 0f && l + size.x <= w && t + size.y <= h &&
            taken.none { (tl, sz) -> l < tl.x + sz.x + pad && l + size.x + pad > tl.x && t < tl.y + sz.y + pad && t + size.y + pad > tl.y }
    }
}

private class Opt(val name: String, val sub: String, val hay: String, val event: String, val make: (android.content.Context) -> TouchItem, val sprite: String?, val label: String?, val grp: Int = 1)

/** the message button: a chat button that sends text (1 tap = pinned message, 2 taps = all, hold = settings); it starts empty and presses Chat until a message is saved */
private fun msgOpt() = Opt("ปุ่มข้อความ", "กด 1 ครั้งเปิดรายการข้อความ แล้วแตะข้อความที่จะส่ง", "", "",
    { c -> newItemFor(c, "touch_controls/control_chat").copy(msgs = emptyList(), gameOnly = true) }, "touch_controls/control_chat", null)

private val CHIPS =listOf("ยอดนิยม", "ปุ่มเกม", "คีย์บอร์ด", "F1–F12", "Numpad", "เมาส์")
private val F_KEY = Regex("F\\d+")

private class QuickOpts(val quick: List<Opt>, val all: List<Opt>, val move: List<Opt>, val system: List<Opt>)

@Composable
private fun rememberQuickOpts(): QuickOpts {
    val context = LocalContext.current
    return remember {
        val touch = listOf("jump", "attack", "use", "sprint", "sneak", "inventory", "chat", "eye").map { n ->
            val s = "touch_controls/control_$n"
            val nm = TOUCH_INFO[s]!!
            Opt(nm.first.substringAfter(" · "), nm.second, "", "", { c -> newItemFor(c, s) }, s, null)
        }
        val blank = Opt("ปุ่มเปล่า", "กรอบเปล่า ตั้งคำสั่งทีหลังได้", "blank เปล่า ว่าง", "", { c -> newItemFor(c, "touch_controls/control_blank_m") }, "touch_controls/control_blank_m", null)
        val quick = listOf(blank) + touch + Opt("ทิ้งของ", "ทิ้งของที่ถืออยู่ (Q)", "", "", { c -> newItemFor(c, "keyboard/key_q") }, "keyboard/key_q", null) + msgOpt()
        val seen = HashSet<String>()
        val out = ArrayList<Opt>()
        fun add(o: Opt) { if (o.event.isEmpty() || seen.add(o.event + "|" + o.grp)) out.add(o) }
        TOUCH_INFO.forEach { (s, v) ->
            if (s.endsWith("dpad_cross")) return@forEach
            val ev = TOUCH_DEFAULTS[s]?.first?.firstOrNull() ?: ""
            add(Opt(v.first.substringAfter(" · "), v.second, (v.first + " " + v.second + " " + ev.substringAfterLast("_")).lowercase(), ev, { c -> newItemFor(c, s) }, s, null))
        }
        val numEvents = KB_NUM.flatten().map { it.event }.toSet()
        (KB_MAIN + KB_NAV + KB_NUM).flatten().forEach { c ->
            if (c.w < 0f || c.event.isEmpty()) return@forEach
            val g = if (c.event in numEvents) 4 else if (F_KEY.matches(c.event.substringAfterLast("_"))) 3 else 2
            add(Opt(c.en + (if (c.th.isNotEmpty() && c.th != c.en) " · " + c.th else ""), c.hint, (c.en + " " + c.th + " " + c.hint + " " + c.event.substringAfterLast("_")).lowercase(), c.event, { ctx -> itemForCell(ctx, c) }, c.sprite, c.label, g))
        }
        add(msgOpt().let { Opt(it.name, it.sub, "message msg chat text ข้อความ แชท พิมพ์ ส่ง", "", it.make, it.sprite, null, 1) })
        KB_MOUSE.forEach { c -> add(Opt(c.en + " · " + c.th, c.hint, (c.en + " " + c.th + " " + c.hint).lowercase(), c.event, { ctx -> itemForCell(ctx, c) }, null, c.label, 5)) }
        val move = listOf(
            Opt(context.getString(R.string.all_cs_dpad), "WASD", "d-pad dpad pad จอย เดิน wasd move", "", { newDpad(0.5f, 0.5f) }, "touch_controls/control_dpad_cross", null, 6),
            Opt(context.getString(R.string.all_cs_joy_round), "WASD", "joystick joy จอย เดิน wasd move", "", { newJoystick(0.5f, 0.5f) }, "joystick/joystick_circle", null, 6),
            Opt(context.getString(R.string.all_cs_joy_square), "WASD", "joystick joy square จอย เหลี่ยม wasd", "", { newJoystick(0.5f, 0.5f, true) }, "joystick/joystick_square", null, 6),
        )
        fun sys(nameRes: Int, subRes: Int, caption: String, ev: String, hay: String) =
            Opt(context.getString(nameRes), context.getString(subRes), hay, ev, { systemItem(caption, ev) }, null, caption, 6)
        val system = listOf(
            sys(R.string.all_cs_gui, R.string.all_cs_gui_sub, "GUI", "L:$LAUNCHER_EVENT_TOGGLE_GUI", "gui hide show ซ่อน แสดง ปุ่มทั้งหมด"),
            sys(R.string.all_cs_mouse, R.string.all_cs_mouse_sub, "Mouse", "L:$LAUNCHER_EVENT_TOGGLE_MOUSE", "mouse เมาส์ ซ่อน แสดง pointer"),
            sys(R.string.all_cs_menu, R.string.all_cs_menu_sub, "Menu", "L:$LAUNCHER_EVENT_SWITCH_MENU", "menu เมนู"),
            sys(R.string.all_cs_ime, R.string.all_cs_ime_sub, "Kbd", "L:$LAUNCHER_EVENT_SWITCH_IME", "keyboard คีย์บอร์ด พิมพ์ ime"),
            sys(R.string.all_cs_scroll_up, R.string.all_cs_scroll_sub, "▲", "L:$LAUNCHER_EVENT_SCROLL_UP_SINGLE", "scroll up เลื่อน ช่อง"),
            sys(R.string.all_cs_scroll_down, R.string.all_cs_scroll_sub, "▼", "L:$LAUNCHER_EVENT_SCROLL_DOWN_SINGLE", "scroll down เลื่อน ช่อง"),
        )
        QuickOpts(quick, out, move, system)
    }
}

/**
 * V2 left tool panel (collapsible). mode "tools" = the tool list (create / manage / target group), "keys" = add a button (search + categories), "system" = GUI / Mouse / Menu ...
 * One tap on a row adds the button at once; every option comes from the same lists the old quick drawer used.
 */
@Composable
internal fun ToolsPanel(mode: String, onMode: (String) -> Unit, targetGroup: String?, onItem: (TouchItem) -> Unit, onAll: () -> Unit, onNewGroup: () -> Unit, onProfiles: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val o = rememberQuickOpts()
    var q by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }
    val frame = TouchSprites.get(context, BLANK_BASE)
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    @Composable
    fun ToolRow(glyph: String, title: String, sub: String?, onClick: () -> Unit) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(TouchTheme.raised).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(glyph, Modifier.width(24.dp), color = TouchTheme.accent, fontSize = 18.sp)
            Column { Text(title, fontSize = 14.sp, maxLines = 1); if (sub != null) Text(sub, color = TouchTheme.dim, fontSize = 11.sp, maxLines = 1) }
        }
    }
    @Composable
    fun OptRow(op: Opt) {
        val bmp = TouchSprites.get(context, op.sprite ?: BLANK_BASE) ?: frame
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(TouchTheme.raised).clickable { onItem(op.make(context)) }.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                if (bmp != null) Image(bmp, op.name, Modifier.fillMaxSize(), filterQuality = FilterQuality.None)
                if (op.sprite == null && op.label != null) Text(op.label, color = Color(0xFF1E2430), fontSize = 12.sp)
            }
            Column { Text(op.name, fontSize = 13.sp, maxLines = 1); if (op.sub.isNotEmpty()) Text(op.sub, color = TouchTheme.dim, fontSize = 10.sp, maxLines = 1) }
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 12.dp, top = 8.dp, end = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(when (mode) { "keys" -> "เพิ่มปุ่ม"; "system" -> "ปุ่มระบบ"; else -> "เครื่องมือ" }, Modifier.weight(1f), fontSize = 15.sp)
            Pill("✕", onClose)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (mode == "tools") {
                Text("สร้างปุ่ม", color = TouchTheme.dim, fontSize = 11.sp)
                ToolRow("＋", "เพิ่มปุ่ม", "ปุ่มคีย์ เมาส์ ค้นหาได้") { onMode("keys") }
                o.move.forEach { m -> ToolRow(if (m.sprite?.contains("dpad") == true) "✥" else "◉", m.name, m.sub) { onItem(m.make(context)) } }
                ToolRow("▤", "ปุ่มข้อความ", "ส่งข้อความ เช่น /home") { onItem(msgOpt().make(context)) }
                ToolRow("⚙", "ปุ่มระบบ", "GUI · เมาส์ · เมนู") { onMode("system") }
                Text("จัดการ", color = TouchTheme.dim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                ToolRow("▧", "สร้างกลุ่ม", null, onNewGroup)
                ToolRow("▤", "โปรไฟล์", null, onProfiles)
                Text("กลุ่มเป้าหมาย", color = TouchTheme.dim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                Text(targetGroup ?: "ไม่มีกลุ่ม", Modifier.clip(RoundedCornerShape(50)).background(TouchTheme.raised).padding(horizontal = 12.dp, vertical = 6.dp), color = TouchTheme.accent, fontSize = 13.sp)
                Text("ปุ่มใหม่จะเข้ากลุ่มนี้ (เลือกที่แผงกลุ่มด้านขวา)", color = TouchTheme.dim, fontSize = 11.sp)
            } else {
                Pill("‹ กลับ", { q = ""; onMode("tools") })
                val searching = q.isNotBlank()
                if (mode == "keys") {
                    OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.all_t_a56dd2a1), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focus.clearFocus() }))
                    if (!searching) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CHIPS.forEachIndexed { i, t -> Pill(t, { tab = i }, primary = tab == i) }
                    }
                }
                val list = when {
                    searching -> q.trim().lowercase().split(" ").filter { it.isNotEmpty() }.let { t -> (o.quick + o.all + o.move + o.system).filter { x -> t.all { it in x.hay || it in x.name.lowercase() } }.distinctBy { it.name + it.event } }.take(60)
                    mode == "system" -> o.system
                    tab == 0 -> o.quick
                    else -> o.all.filter { it.grp == tab }
                }
                if (list.isEmpty()) Text(stringResource(R.string.all_t_60312e2b), color = TouchTheme.dim)
                list.forEach { OptRow(it) }
                if (mode == "keys" && !searching) Pill(stringResource(R.string.all_t_ea02b5b6), onAll, Modifier.fillMaxWidth())
            }
        }
    }
}

/** A system button (GUI / Mouse / Menu ...): a framed button named by its caption, 48 dp, visible in game AND in the menu so GUI can always bring the buttons back. */
internal fun systemItem(caption: String, event: String) =
    TouchItem(newTouchId(), TouchKind.BUTTON, BLANK_BASE, 0.5f, 0.5f, 48f, listOf(event), caption = caption, gameOnly = false)

/**
 * Defaults for a button created in the Studio (v1.2): plain buttons 56 dp and never a toggle (Sneak used to start as one), launcher / system buttons 48 dp.
 * Applied ONLY to the item being added: templates, imports and saved profiles keep what they have.
 */
internal fun studioDefault(n: TouchItem): TouchItem {
    if (n.kind != TouchKind.BUTTON) return n
    if (n.events.any { it.startsWith("L:") || it.isGroupEvent() }) return n.copy(sizeDp = 48f)
    return n.copy(sizeDp = if (n.sprite.endsWith("key_space")) n.sizeDp else 56f, toggle = false)
}
