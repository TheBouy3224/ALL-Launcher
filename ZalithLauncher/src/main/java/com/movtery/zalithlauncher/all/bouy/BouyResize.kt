package com.movtery.zalithlauncher.all.bouy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

internal enum class ResizeEdge { RIGHT, BOTTOM, CORNER }

/** the smallest / largest width (dp) a button may have, and how far its height may be stretched against the sprite's own proportions */
internal const val MIN_W_DP = 30f
internal const val MAX_W_DP = 400f
internal const val MIN_HS = 0.2f
internal const val MAX_HS = 4f

/**
 * Three drag handles on the selected BUTTON: right edge = width only, bottom edge = height only, bottom-right corner = both,
 * keeping the proportions. The top-left corner stays where it is, so the handle follows the finger exactly.
 * [aspect] = sprite height / width. [getItem] must return the CURRENT item (it changes while dragging).
 */
@Composable
internal fun ResizeHandles(
    tl: Offset, sz: Offset, screen: IntSize, aspect: Float,
    getItem: () -> BouyItem?, onLive: ((BouyItem) -> BouyItem) -> Unit, onStart: () -> Unit, onEnd: () -> Unit,
) {
    val density = LocalDensity.current.density
    val rPx = 13f * density

    fun resize(edge: ResizeEdge, d: Offset) {
        val it0 = getItem() ?: return
        val ow = it0.sizeDp * density
        val oh = ow * aspect * it0.heightScale
        var nw = ow; var nh = oh
        when (edge) {
            ResizeEdge.RIGHT -> nw = (ow + d.x).coerceIn(MIN_W_DP * density, MAX_W_DP * density)
            ResizeEdge.BOTTOM -> nh = (oh + d.y).coerceIn(ow * aspect * MIN_HS, ow * aspect * MAX_HS)
            ResizeEdge.CORNER -> { nw = (ow + d.x).coerceIn(MIN_W_DP * density, MAX_W_DP * density); nh = oh * nw / ow }
        }
        val hs = (nh / (nw * aspect)).coerceIn(MIN_HS, MAX_HS)
        nh = nw * aspect * hs
        val nx = it0.x + (nw - ow) / 2f / screen.width
        val ny = it0.y + (nh - oh) / 2f / screen.height
        onLive { it.copy(sizeDp = nw / density, heightScale = hs, x = nx.coerceIn(0.02f, 0.98f), y = ny.coerceIn(0.02f, 0.98f)) }
    }

    @Composable
    fun Handle(cx: Float, cy: Float, edge: ResizeEdge) {
        Box(
            Modifier.offset { IntOffset((cx - rPx).toInt(), (cy - rPx).toInt()) }.size(26.dp).clip(CircleShape)
                .background(if (edge == ResizeEdge.CORNER) Color(0xFFFFD54F) else BouyTheme.accent)
                .border(3.dp, Color(0xFF0B1220), CircleShape)
                .pointerInput(edge) {
                    detectDragGestures(onDragStart = { onStart() }, onDragEnd = { onEnd() }, onDragCancel = { onEnd() }) { change, d ->
                        change.consume(); resize(edge, d)
                    }
                }
        )
    }
    Handle(tl.x + sz.x, tl.y + sz.y / 2f, ResizeEdge.RIGHT)
    Handle(tl.x + sz.x / 2f, tl.y + sz.y, ResizeEdge.BOTTOM)
    Handle(tl.x + sz.x, tl.y + sz.y, ResizeEdge.CORNER)
}

/** Width (and height for a button) typed in dp. "ใช้ตัวเลข" or the keyboard's Done applies both; "คืนสัดส่วน" gives back the sprite's own proportions. */
@Composable
internal fun SizeFields(item: BouyItem, aspect: Float, onApply: (Float, Float?) -> Unit, onReset: () -> Unit) {
    val isButton = item.kind == BouyKind.BUTTON
    val w = item.sizeDp; val h = w * aspect * item.heightScale
    var wt by remember(item.id, w.roundToInt(), h.roundToInt()) { mutableStateOf(w.roundToInt().toString()) }
    var ht by remember(item.id, w.roundToInt(), h.roundToInt()) { mutableStateOf(h.roundToInt().toString()) }
    fun apply() {
        val nw = wt.toFloatOrNull()?.coerceIn(MIN_W_DP, MAX_W_DP) ?: return
        val nh = if (isButton) ht.toFloatOrNull()?.coerceIn(10f, 1000f) else null
        onApply(nw, nh)
    }
    val kb = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
    val ka = KeyboardActions(onDone = { apply() })
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ขนาด (dp) · ลากจุดสีฟ้าบนปุ่มก็ได้", color = BouyTheme.dim, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(wt, { wt = it.filter { c -> c.isDigit() }.take(3) }, Modifier.weight(1f), singleLine = true, label = { Text("กว้าง") }, keyboardOptions = kb, keyboardActions = ka)
            if (isButton) OutlinedTextField(ht, { ht = it.filter { c -> c.isDigit() }.take(4) }, Modifier.weight(1f), singleLine = true, label = { Text("สูง") }, keyboardOptions = kb, keyboardActions = ka)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("ใช้ตัวเลข", { apply() }, primary = true)
            if (isButton) Pill("คืนสัดส่วน", onReset, enabled = item.heightScale != 1f)
        }
    }
}

/** the last colours picked with the colour square (app-wide, newest first, at most 5) */
internal object BouyRecentColors {
    private const val KEY = "recent"
    private fun prefs(c: android.content.Context) = c.getSharedPreferences("bouy_ui", android.content.Context.MODE_PRIVATE)
    fun get(c: android.content.Context): List<Int> = runCatching { prefs(c).getString(KEY, "").orEmpty().split(",").mapNotNull { it.toIntOrNull() } }.getOrDefault(emptyList())
    fun add(c: android.content.Context, color: Int) {
        runCatching { prefs(c).edit().putString(KEY, (listOf(color) + get(c).filter { it != color }).take(5).joinToString(",")).apply() }
    }
}

private fun hexOf(h: Float, s: Float, v: Float) = String.format("%06X", android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)) and 0xFFFFFF)

/** A colour square (saturation x brightness) + a rainbow bar (hue) + live preview, the last 5 colours and a HEX field for those who want to type */
@Composable
internal fun ColorPickerContent(start: Int, recent: List<Int>, onApply: (Int) -> Unit, onOriginal: () -> Unit) {
    val init = FloatArray(3).also { if (start != 0) android.graphics.Color.colorToHSV(start, it) else { it[0] = 210f; it[1] = 0.4f; it[2] = 0.85f } }
    var h by remember { mutableFloatStateOf(init[0]) }
    var s by remember { mutableFloatStateOf(init[1]) }
    var v by remember { mutableFloatStateOf(init[2]) }
    var hex by remember { mutableStateOf(hexOf(init[0], init[1], init[2])) }
    val cur = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
    val hueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(h, 1f, 1f)))
    fun setHsv(nh: Float = h, ns: Float = s, nv: Float = v) { h = nh; s = ns; v = nv; hex = hexOf(nh, ns, nv) }
    fun setColor(c: Int) { val f = FloatArray(3); android.graphics.Color.colorToHSV(c, f); setHsv(f[0], f[1], f[2]) }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, BouyTheme.line, RoundedCornerShape(12.dp))
                .pointerInput(Unit) { detectTapGestures { p -> setHsv(ns = (p.x / size.width).coerceIn(0f, 1f), nv = 1f - (p.y / size.height).coerceIn(0f, 1f)) } }
                .pointerInput(Unit) { detectDragGestures { c, _ -> c.consume(); setHsv(ns = (c.position.x / size.width).coerceIn(0f, 1f), nv = 1f - (c.position.y / size.height).coerceIn(0f, 1f)) } }) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                    val c = Offset(s * size.width, (1f - v) * size.height)
                    drawCircle(Color.Black, 13.dp.toPx(), c); drawCircle(Color.White, 11.dp.toPx(), c); drawCircle(Color(cur), 8.dp.toPx(), c)
                }
            }
            Box(Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(15.dp))
                .pointerInput(Unit) { detectTapGestures { p -> setHsv(nh = (p.x / size.width).coerceIn(0f, 1f) * 360f) } }
                .pointerInput(Unit) { detectDragGestures { c, _ -> c.consume(); setHsv(nh = (c.position.x / size.width).coerceIn(0f, 1f) * 360f) } }) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                    val c = Offset(h / 360f * size.width, size.height / 2f)
                    drawCircle(Color.Black, 14.dp.toPx(), c); drawCircle(Color.White, 12.dp.toPx(), c); drawCircle(hueColor, 9.dp.toPx(), c)
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // the buttons first, so a small landscape screen never cuts them off
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("คืนสีเดิม", onOriginal)
                Pill("ตกลง", { onApply(cur) }, primary = true)
            }
            Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp)).background(Color(cur)).border(1.dp, BouyTheme.line, RoundedCornerShape(14.dp)))
            OutlinedTextField(hex, { t ->
                val c = t.removePrefix("#").uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                hex = c
                if (c.length == 6) setColor(0xFF000000.toInt() or c.toInt(16)).also { hex = c }
            }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("HEX") }, prefix = { Text("#") })
            if (recent.isNotEmpty()) {
                Text("สีที่ใช้ล่าสุด (แตะใช้ซ้ำ)", color = BouyTheme.dim, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recent.forEach { c -> Box(Modifier.size(34.dp).clip(CircleShape).background(Color(c)).border(1.dp, BouyTheme.line, CircleShape).clickable { setColor(c) }) }
                }
            }
        }
    }
}
