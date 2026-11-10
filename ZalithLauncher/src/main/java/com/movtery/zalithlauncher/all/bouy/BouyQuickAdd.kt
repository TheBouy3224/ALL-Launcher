package com.movtery.zalithlauncher.all.bouy

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
internal fun freeSpot(taken: List<Pair<Offset, Offset>>, size: Offset, screen: IntSize): Offset? {
    val w = screen.width.toFloat(); val h = screen.height.toFloat()
    val pad = 6f
    val cands = ArrayList<Offset>()
    var fy = 0.14f
    while (fy <= 0.80f) { var fx = 0.06f; while (fx <= 0.94f) { cands.add(Offset(fx, fy)); fx += 0.04f }; fy += 0.06f }
    cands.sortBy { (it.x - 0.78f) * (it.x - 0.78f) * 2f + (it.y - 0.5f) * (it.y - 0.5f) }
    return cands.firstOrNull { c ->
        val l = c.x * w - size.x / 2f; val t = c.y * h - size.y / 2f
        l >= 0f && t >= 0f && l + size.x <= w && t + size.y <= h &&
            taken.none { (tl, sz) -> l < tl.x + sz.x + pad && l + size.x + pad > tl.x && t < tl.y + sz.y + pad && t + size.y + pad > tl.y }
    }
}

private class Opt(val name: String, val sub: String, val hay: String, val event: String, val make: (android.content.Context) -> BouyItem, val sprite: String?, val label: String?, val grp: Int = 1)

/** the message button: a chat button that sends text (1 tap = pinned message, 2 taps = all, hold = settings); it starts empty and presses Chat until a message is saved */
private fun msgOpt() = Opt("ปุ่มข้อความ", "กด 1 ครั้งเปิดรายการข้อความ แล้วแตะข้อความที่จะส่ง", "", "",
    { c -> newItemFor(c, "touch_controls/control_chat").copy(msgs = emptyList(), gameOnly = true) }, "touch_controls/control_chat", null)

private val CHIPS =listOf("ยอดนิยม", "ปุ่มเกม", "คีย์บอร์ด", "F1–F12", "Numpad", "เมาส์")
private val F_KEY = Regex("F\\d+")

/** Common buttons + search over touch buttons, every key and the mouse. One tap = the button is added at once. */
@Composable
internal fun QuickAdd(onItem: (BouyItem) -> Unit, onAll: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    var q by remember { mutableStateOf("") }
    val quick = remember {
        val touch = listOf("jump", "attack", "use", "sprint", "sneak", "inventory", "chat", "eye").map { n ->
            val s = "touch_controls/control_$n"
            val nm = TOUCH_INFO[s]!!
            Opt(nm.first.substringAfter(" · "), nm.second, "", "", { c -> newItemFor(c, s) }, s, null)
        }
        touch + Opt("ทิ้งของ", "ทิ้งของที่ถืออยู่ (Q)", "", "", { c -> newItemFor(c, "keyboard/key_q") }, "keyboard/key_q", null) + msgOpt()
    }
    val all = remember {
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
        out
    }
    val frame = BouySprites.get(context, BLANK_BASE)
    var tab by remember { mutableStateOf(0) }
    val list = if (q.isBlank()) (if (tab == 0) quick else all.filter { it.grp == tab }) else q.trim().lowercase().split(" ").filter { it.isNotEmpty() }.let { t -> all.filter { o -> t.all { it in o.hay || it in o.name.lowercase() } } }
    Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val focus = androidx.compose.ui.platform.LocalFocusManager.current
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // the keyboard's ✓ closes it so the results are visible (a landscape phone keyboard covers the list)
            OutlinedTextField(q, { q = it }, Modifier.weight(1f), singleLine = true, label = { Text("ค้นหา กระโดด, Jump, Space, F5") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focus.clearFocus() }))
            OutlinedButton(onClose) { Text("ปิด ✕") }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CHIPS.forEachIndexed { i, t ->
                val on = q.isBlank() && tab == i
                Text(t, Modifier.background(if (on) Color(0xFFD9DCE2) else Color(0x33FFFFFF), RoundedCornerShape(4.dp)).clickable { q = ""; tab = i }.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = if (on) Color(0xFF1C1F24) else Color.Unspecified, fontSize = 13.sp, maxLines = 1)
            }
            Text("D-Pad / จอย ▸", Modifier.background(Color(0x33FFFFFF), RoundedCornerShape(4.dp)).clickable { onAll() }.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 13.sp, maxLines = 1)
        }
        if (list.isEmpty()) Text("ไม่พบปุ่มที่ค้นหา ลองหมวด D-Pad / จอย ด้านบน", color = Color(0xFF9AA4B2))
        LazyVerticalGrid(GridCells.Fixed(4), Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(list) { o ->
                val bmp = BouySprites.get(context, o.sprite ?: BLANK_BASE) ?: frame
                Column(Modifier.background(Color(0x22FFFFFF), RoundedCornerShape(8.dp)).clickable { onItem(o.make(context)) }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        if (bmp != null) Image(bmp, o.name, Modifier.fillMaxSize(), filterQuality = FilterQuality.None)
                        if (o.sprite == null && o.label != null) Text(o.label, color = Color(0xFF1E2430), fontSize = 13.sp)
                    }
                    Text(o.name, fontSize = 12.sp, maxLines = 2)
                }
            }
        }
    }
}
