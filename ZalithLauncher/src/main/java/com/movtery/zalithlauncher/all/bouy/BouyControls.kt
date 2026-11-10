package com.movtery.zalithlauncher.all.bouy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.movtery.layer_controller.event.ClickEvent
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas

private const val UP = 1
private const val DOWN = 2
private const val LEFT = 4
private const val RIGHT = 8

private val MOVE_KEYS = listOf(UP to "K:GLFW_KEY_W", DOWN to "K:GLFW_KEY_S", LEFT to "K:GLFW_KEY_A", RIGHT to "K:GLFW_KEY_D")

/** On-screen rectangle of an item (top-left + size in px); the height keeps the sprite's own aspect times [BouyItem.heightScale] */
fun bouyRect(item: BouyItem, sprite: ImageBitmap?, screen: IntSize, density: Float): Pair<Offset, Offset> {
    val w = item.sizeDp * density
    val h = (if (sprite == null) w else w * sprite.height / sprite.width) * item.heightScale
    return Offset(item.x * screen.width - w / 2f, item.y * screen.height - h / 2f) to Offset(w, h)
}

/** Direction bits for a finger at [p] relative to the centre of a [size] box (dead zone in the middle, diagonals when both axes pass) */
internal fun dirBits(p: Offset, centre: Offset, halfW: Float, halfH: Float, dead: Float): Int {
    val ux = (p.x - centre.x) / halfW
    val uy = (p.y - centre.y) / halfH
    var bits = 0
    if (uy < -dead) bits = bits or UP
    if (uy > dead) bits = bits or DOWN
    if (ux < -dead) bits = bits or LEFT
    if (ux > dead) bits = bits or RIGHT
    return bits
}

private class Active(var item: BouyItem) {
    var sneakTouch = false; var cam = false
    // message button: where the finger landed, and whether it drifted (a drag is not a tap)
    var pid: PointerId? = null; var start = Offset.Zero; var moved = false
}

private fun BouyItem.isMsg() = kind == BouyKind.BUTTON && msgs != null

/**
 * The game's control layer. [content] (camera / mouse layer) is drawn underneath; this layer sees every finger first,
 * claims the ones that land on a widget (and tells the game via [onOccupied] so the camera ignores them) and leaves the rest alone,
 * so walking and camera turning work together. Every key that goes down is tracked per widget and released when the finger
 * leaves, the layer hides, or the composable leaves the screen - nothing can stay pressed.
 */
@Composable
fun BouyControls(
    layout: BouyLayout,
    isGrabbing: Boolean,
    opacity: Float,
    onEvent: (ClickEvent, Boolean) -> Unit,
    isOccupied: (PointerId) -> Boolean,
    onOccupied: (PointerId) -> Unit,
    onRelease: (PointerId) -> Unit,
    onMoveOnly: (PointerId) -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val curLayout by rememberUpdatedState(layout)
    val scope = rememberCoroutineScope()
    var msgUi by remember { mutableStateOf<String?>(null) } // id of the message button whose list is open
    val curGrabbing by rememberUpdatedState(isGrabbing)
    val curOnEvent by rememberUpdatedState(onEvent)
    val curIsOccupied by rememberUpdatedState(isOccupied)
    val curOnOccupied by rememberUpdatedState(onOccupied)
    val curOnRelease by rememberUpdatedState(onRelease)
    val curOnMoveOnly by rememberUpdatedState(onMoveOnly)

    var screen = remember { IntSize(1, 1) }
    val active = remember { HashMap<PointerId, Active>() }
    val held = remember { HashMap<String, Set<String>>() } // owner item id -> events now down
    val dirState = remember { mutableStateMapOf<String, Int>() } // dpad/joystick direction bits (drawn + pressed look)
    val stick = remember { mutableStateMapOf<String, Offset>() } // joystick thumb offset in px
    val pressedState = remember { mutableStateMapOf<String, Boolean>() }
    val toggled = remember { mutableStateMapOf<String, Boolean>() }

    fun sync(owner: String, want: Set<String>) {
        val have = held[owner].orEmpty()
        if (com.movtery.zalithlauncher.BuildConfig.DEBUG && have != want) android.util.Log.i("ALLBOUY", "$owner down=$want (was $have)") // debug builds only
        (have - want).forEach { curOnEvent(it.toClickEvent(), false) }
        (want - have).forEach { curOnEvent(it.toClickEvent(), true) }
        if (want.isEmpty()) held.remove(owner) else held[owner] = want
    }

    fun releaseAll() {
        held.keys.toList().forEach { sync(it, emptySet()) }
        active.keys.toList().forEach { curOnRelease(it) }
        active.clear(); dirState.clear(); stick.clear(); pressedState.clear(); toggled.clear()
    }

    fun visible(item: BouyItem) = !item.gameOnly || curGrabbing

    fun sendMsg(text: String) { curOnEvent(ClickEvent(ClickEvent.Type.SendText, text), true) } // the launcher's own sender: opens chat, types, Enter

    /** one tap: the list of saved messages (none saved yet: the button's own key once, i.e. it opens the chat) */
    fun msgTap(id: String) {
        val item = curLayout.items.firstOrNull { it.id == id } ?: return
        if (item.msgs.isNullOrEmpty()) scope.launch { val k = "$id#tap"; sync(k, item.events.toSet()); delay(60); sync(k, emptySet()) }
        else msgUi = id
    }

    fun moveEvents(bits: Int) = MOVE_KEYS.filter { bits and it.first != 0 }.map { it.second }.toSet()

    fun update(a: Active, p: Offset) {
        val item = a.item
        if (item.isMsg()) { // drifting away from where it landed = a drag, not a tap
            if (!a.moved && (p - a.start).getDistance() > 24f * density) a.moved = true
            return
        }
        if (a.sneakTouch) return // a touch that began on the centre (Sneak) never walks, even if the finger slides out
        val sprite = BouySprites.get(context, if (item.kind == BouyKind.JOYSTICK) item.sprite + "_base" else item.sprite)
        val (tl, sz) = bouyRect(item, sprite, screen, density)
        val c = tl + Offset(sz.x / 2f, sz.y / 2f)
        when (item.kind) {
            BouyKind.DPAD -> { val b = dirBits(p, c, sz.x / 2f, sz.y / 2f, 1f / 3f); dirState[item.id] = b; sync(item.id, moveEvents(b)) }
            BouyKind.JOYSTICK -> {
                val r = sz.x / 2f * 0.62f // thumb travel
                val d = p - c
                val len = hypot(d.x, d.y)
                val clamped = if (len > r) d * (r / len) else d
                stick[item.id] = clamped
                val b = dirBits(c + clamped, c, r, r, 0.38f)
                dirState[item.id] = b; sync(item.id, moveEvents(b))
            }
            BouyKind.BUTTON -> {}
        }
    }

    fun press(a: Active, p: Offset) {
        val item = a.item
        pressedState[item.id] = true
        if (item.isMsg()) { a.start = p; a.moved = false; return } // nothing is pressed in the game: a tap (on release) opens the list
        if (item.kind == BouyKind.BUTTON) {
            if (item.toggle && !item.dragCam) { // a camera-drag button never latches: every camera drag would flip (and could leave) the key down
                val on = !(toggled[item.id] ?: false)
                toggled[item.id] = on
                sync(item.id, if (on) item.events.toSet() else emptySet())
            } else sync(item.id, item.events.toSet())
        } else {
            update(a, p)
            // the centre of the D-Pad is the Sneak button: one tap toggles it (LEFT_SHIFT), own owner key so walking keys never release it
            if (item.kind == BouyKind.DPAD && (dirState[item.id] ?: 0) == 0) {
                a.sneakTouch = true
                val key = item.id + "#sneak"
                val on = !(toggled[key] ?: false)
                toggled[key] = on
                sync(key, if (on) setOf("K:GLFW_KEY_LEFT_SHIFT") else emptySet())
            }
        }
    }

    fun release(a: Active) {
        val item = a.item
        pressedState.remove(item.id)
        if (item.isMsg()) { if (!a.moved) msgTap(item.id); return }
        when (item.kind) {
            BouyKind.BUTTON -> if (!(item.toggle && !item.dragCam)) sync(item.id, emptySet())
            else -> { dirState.remove(item.id); stick.remove(item.id); sync(item.id, emptySet()) }
        }
    }

    fun inside(item: BouyItem, p: Offset): Boolean {
        val sprite = BouySprites.get(context, if (item.kind == BouyKind.JOYSTICK) item.sprite + "_base" else item.sprite)
        val (tl, sz) = bouyRect(item, sprite, screen, density)
        return p.x in tl.x..(tl.x + sz.x) && p.y in tl.y..(tl.y + sz.y)
    }

    // a finger that started on a plain button and is dragged onto a button with "accept dragged fingers" hands over to it.
    // A finger that started on a camera-drag button never slides (it is the camera finger), so the two options cannot clash.
    fun slide(a: Active, p: Offset) {
        if (a.cam || a.item.kind != BouyKind.BUTTON) return
        val target = curLayout.items.lastOrNull { t ->
            t.kind == BouyKind.BUTTON && t.slideIn && t.id != a.item.id && visible(t) && active.values.none { it.item.id == t.id } && inside(t, p)
        } ?: return
        release(a); a.item = target; press(a, p)
    }

    // hiding the layer (menu / inventory opened) or swapping the layout must not leave a key down
    LaunchedEffect(isGrabbing, layout) { releaseAll() }
    DisposableEffect(Unit) { onDispose { releaseAll() } }

    Box(
        modifier = modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    for (c in event.changes) {
                        val a = active[c.id]
                        if (a != null) {
                            if (c.pressed) { update(a, c.position); slide(a, c.position) } else { release(a); active.remove(c.id); if (!a.cam) curOnRelease(c.id) } // a camera finger is released by the camera layer itself (it must still see it as move-only on lift)
                            if (!a.cam) c.consume() // camera-drag finger stays visible to the camera layer
                            continue
                        }
                        if (!c.pressed || c.previousPressed || c.isConsumed || curIsOccupied(c.id)) continue
                        val hit = curLayout.items.lastOrNull { item ->
                            if (!visible(item)) return@lastOrNull false
                            val sprite = BouySprites.get(context, if (item.kind == BouyKind.JOYSTICK) item.sprite + "_base" else item.sprite)
                            val (tl, sz) = bouyRect(item, sprite, screen, density)
                            c.position.x in tl.x..(tl.x + sz.x) && c.position.y in tl.y..(tl.y + sz.y)
                        } ?: continue
                        val na = Active(hit)
                        na.pid = c.id
                        active[c.id] = na
                        if (hit.dragCam && hit.kind == BouyKind.BUTTON) { na.cam = true; curOnMoveOnly(c.id) } // camera layer turns the view for this finger but never taps/long-presses
                        else { curOnOccupied(c.id); c.consume() }
                        press(na, c.position)
                    }
                }
            }
        }
    ) {
        content()
        val animTick = rememberAnimTick(curLayout)
        Canvas(Modifier.fillMaxSize().alpha(opacity.coerceIn(0f, 1f))) {
            animTick.longValue // read so a GIF frame change redraws
            screen = IntSize(size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1))
            curLayout.items.forEach { item ->
                if (!visible(item)) return@forEach
                drawBouyItem(context, item, screen, density, dirState[item.id] ?: 0, stick[item.id] ?: Offset.Zero, pressedState[item.id] == true || toggled[item.id] == true, sneakOn = toggled[item.id + "#sneak"] == true)
            }
        }
        msgUi?.let { id ->
            val it0 = curLayout.items.firstOrNull { it.id == id }
            if (it0 == null) msgUi = null
            else BouyMsgList(it0,
                onSend = { t -> msgUi = null; scope.launch { delay(150); sendMsg(t) } }, // let the dialog leave first so the game owns the keys again
                onClose = { msgUi = null })
        }
    }
}

/** One widget, drawn from its sprites without scaling distortion (nearest-neighbour keeps the pixel edges sharp) */
fun DrawScope.drawBouyItem(
    context: android.content.Context, item: BouyItem, screen: IntSize, density: Float,
    dir: Int, stickOffset: Offset, pressed: Boolean, selected: Boolean = false, sneakOn: Boolean = false,
) {
    val tint = ColorFilter.tint(Color(0x55000000), BlendMode.SrcAtop)
    fun img(path: String, tl: Offset, sz: Offset, filter: ColorFilter? = null, iconA: Float = item.iconAlpha, part: String? = null) {
        val own = part?.let { item.parts[it] }?.let { BouyStyle.picture(it) }
        if (own != null) { drawFitted(own, tl, sz, false, item.iconAlpha, filter); return } // player picture for this piece, aspect kept
        val bmp = BouySprites.get(context, path)?.let { BouyStyle.styled(BouySkin.current + "|" + path, it, item.tint, item.bgAlpha, iconA) } ?: return
        drawImage(bmp, dstOffset = IntOffset(tl.x.toInt(), tl.y.toInt()), dstSize = IntSize(sz.x.toInt().coerceAtLeast(1), sz.y.toInt().coerceAtLeast(1)),
            filterQuality = FilterQuality.None, colorFilter = filter)
    }
    when (item.kind) {
        BouyKind.JOYSTICK -> {
            val base = BouySprites.get(context, item.sprite + "_base") ?: return
            val (tl, sz) = bouyRect(item, base, screen, density)
            img(item.sprite + if (pressed) "_base_pressed" else "_base", tl, sz, part = "base")
            val thumb = BouySprites.get(context, item.sprite + "_thumb")
            if (thumb != null) {
                val tw = sz.x * thumb.width / base.width
                val th = tw * thumb.height / thumb.width
                val c = tl + Offset(sz.x / 2f, sz.y / 2f) + stickOffset
                img(item.sprite + if (pressed) "_thumb_pressed" else "_thumb", Offset(c.x - tw / 2f, c.y - th / 2f), Offset(tw, th), part = "thumb")
            }
            if (selected) drawSelection(tl, sz)
        }
        BouyKind.DPAD -> {
            // hit area / size come from the cross sprite; the picture is 9 separate tiles (dpad8_gray): 4 arrows + centre always,
            // the 4 diagonals only while a finger is on the pad (dir tells which tile is active, it gets the dark tint)
            val bmp = BouySprites.get(context, item.sprite) ?: return
            val (tl, sz) = bouyRect(item, bmp, screen, density)
            val cell = Offset(sz.x / 3f, sz.y / 3f)
            fun tile(name: String, col: Int, row: Int, active: Boolean) =
                img("dpad8_gray/dpad_$name", Offset(tl.x + cell.x * col, tl.y + cell.y * row), cell, if (active) tint else null, part = name)
            // diagonals only next to what the finger is on: on an arrow -> its two neighbours; on a diagonal -> just that one; centre -> none
            if (pressed) {
                if (dir == UP || dir == LEFT || dir == (UP or LEFT)) tile("northwest", 0, 0, dir == (UP or LEFT))
                if (dir == UP || dir == RIGHT || dir == (UP or RIGHT)) tile("northeast", 2, 0, dir == (UP or RIGHT))
                if (dir == DOWN || dir == LEFT || dir == (DOWN or LEFT)) tile("southwest", 0, 2, dir == (DOWN or LEFT))
                if (dir == DOWN || dir == RIGHT || dir == (DOWN or RIGHT)) tile("southeast", 2, 2, dir == (DOWN or RIGHT))
            }
            tile("up", 1, 0, dir == UP)
            tile("down", 1, 2, dir == DOWN)
            tile("left", 0, 1, dir == LEFT)
            tile("right", 2, 1, dir == RIGHT)
            img("touch_controls/control_sneak", Offset(tl.x + cell.x, tl.y + cell.y), cell, if (sneakOn) tint else null, part = "center")
            if (selected) drawSelection(tl, sz)
        }
        BouyKind.BUTTON -> {
            val bmp = BouySprites.get(context, item.sprite) ?: return
            val (tl, sz) = bouyRect(item, bmp, screen, density)
            val pic = item.image?.let { BouyStyle.picture(it) }
            // a caption (the player's own name) replaces the icon: skin-coloured frame + text, the key it presses is untouched
            // "ชื่ออย่างเดียว": caption replaces the icon. "ไอคอน + ชื่อ" (captionIcon) keeps the icon and writes the name under it (below)
            if (pic == null && item.caption != null && !item.captionIcon) {
                drawCaption(context, item, tl, sz, pressed)
                if (selected) drawSelection(tl, sz)
                return
            }
            // with a player picture the button is a blank frame (colour + background opacity) and the picture is its icon
            img(item.sprite, tl, sz, if (pressed) tint else null, if (pic != null) item.bgAlpha else item.iconAlpha)
            if (pic != null) {
                // icon box = centre of the button, [imageScale] of its size; Fit inside it or Crop to fill it (clipped); aspect never changed
                val box = Offset(sz.x * item.imageScale, sz.y * item.imageScale)
                val bl = Offset(tl.x + (sz.x - box.x) / 2f, tl.y + (sz.y - box.y) / 2f)
                val s = if (item.crop) max(box.x / pic.width, box.y / pic.height) else min(box.x / pic.width, box.y / pic.height)
                val dw = pic.width * s; val dh = pic.height * s
                clipRect(bl.x, bl.y, bl.x + box.x, bl.y + box.y) {
                    drawImage(pic, dstOffset = IntOffset((bl.x + (box.x - dw) / 2f).toInt(), (bl.y + (box.y - dh) / 2f).toInt()),
                        dstSize = IntSize(dw.toInt().coerceAtLeast(1), dh.toInt().coerceAtLeast(1)), alpha = item.iconAlpha,
                        filterQuality = FilterQuality.Medium, colorFilter = if (pressed) tint else null)
                }
            }
            if (pic == null) item.label?.let { drawLabel(context, it, tl, sz, item.iconAlpha, pressed) }
            if (item.caption != null && item.captionIcon) drawNameUnder(context, item, tl, sz)
            if (selected) drawSelection(tl, sz)
        }
    }
}

private fun DrawScope.drawSelection(tl: Offset, sz: Offset) {
    drawRect(Color(0xFFFFD54F), tl - Offset(4f, 4f), androidx.compose.ui.geometry.Size(sz.x + 8f, sz.y + 8f), style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
}


/** Picture inside the box [bl]+[box]: Fit (whole picture) or Crop (fills, clipped); aspect and alpha kept */
internal fun DrawScope.drawFitted(pic: ImageBitmap, bl: Offset, box: Offset, crop: Boolean, alpha: Float, filter: ColorFilter?) {
    val s = if (crop) max(box.x / pic.width, box.y / pic.height) else min(box.x / pic.width, box.y / pic.height)
    val dw = pic.width * s; val dh = pic.height * s
    clipRect(bl.x, bl.y, bl.x + box.x, bl.y + box.y) {
        drawImage(pic, dstOffset = IntOffset((bl.x + (box.x - dw) / 2f).toInt(), (bl.y + (box.y - dh) / 2f).toInt()),
            dstSize = IntSize(dw.toInt().coerceAtLeast(1), dh.toInt().coerceAtLeast(1)), alpha = alpha,
            filterQuality = FilterQuality.Medium, colorFilter = filter)
    }
}

/** Text on a standard ALL frame (keys without a sprite): centred, shrunk to fit the frame, dark like the sprite icons */
private fun DrawScope.drawLabel(context: android.content.Context, text: String, tl: Offset, sz: Offset, alpha: Float, pressed: Boolean) {
    drawFitText(context, text, tl, sz, alpha, pressed, ink = 0xFF1E2430.toInt(), scale = 1f)
}

/** Shared text drawing: the app font, centred, shrunk to fit the frame width (never overflows), moves 2px down while pressed */
private fun DrawScope.drawFitText(context: android.content.Context, text: String, tl: Offset, sz: Offset, alpha: Float, pressed: Boolean, ink: Int, scale: Float) {
    val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        typeface = android.graphics.Typeface.create(BouyFont.typeface(context), android.graphics.Typeface.BOLD)
        color = android.graphics.Color.argb((alpha * 255).toInt().coerceIn(0, 255), android.graphics.Color.red(ink), android.graphics.Color.green(ink), android.graphics.Color.blue(ink))
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = sz.y * 0.34f * scale
    }
    val maxW = sz.x * 0.82f
    val w = p.measureText(text)
    if (w > maxW) p.textSize *= maxW / w
    val cy = tl.y + sz.y / 2f - (p.descent() + p.ascent()) / 2f + (if (pressed) 2f else 0f)
    drawContext.canvas.nativeCanvas.drawText(text, tl.x + sz.x / 2f, cy, p)
}

/** "ไอคอน + ชื่อ": the name on a small dark pill just under the button, so the icon art stays whole (the pill is not part of the touch area) */
private fun DrawScope.drawNameUnder(context: android.content.Context, item: BouyItem, tl: Offset, sz: Offset) {
    val text = item.caption ?: return
    val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        typeface = android.graphics.Typeface.create(BouyFont.typeface(context), android.graphics.Typeface.BOLD)
        color = if (item.captionColor != 0) item.captionColor else android.graphics.Color.WHITE
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = (sz.y * 0.24f * item.captionScale).coerceAtLeast(20f)
    }
    val maxW = sz.x * 1.5f
    val w = p.measureText(text)
    if (w > maxW) p.textSize *= maxW / w
    val tw = p.measureText(text); val h = p.descent() - p.ascent()
    val padX = p.textSize * 0.5f; val padY = p.textSize * 0.22f
    val cx = tl.x + sz.x / 2f
    val top = tl.y + sz.y + sz.y * 0.05f
    drawRoundRect(Color(0xB3101418), Offset(cx - tw / 2f - padX, top), androidx.compose.ui.geometry.Size(tw + padX * 2, h + padY * 2),
        androidx.compose.ui.geometry.CornerRadius(h, h))
    drawContext.canvas.nativeCanvas.drawText(text, cx, top + padY - p.ascent(), p)
}

/** Frame in the skin's colours (tint / background opacity of the button still apply) + the player's caption */
private fun DrawScope.drawCaption(context: android.content.Context, item: BouyItem, tl: Offset, sz: Offset, pressed: Boolean) {
    val dark = BouySkin.current == "b"
    val body = if (item.tint != 0) Color(item.tint) else if (dark) Color(0xF0181C22) else Color(0xFFD7DBE2)
    val edge = if (dark) Color(0x2EFFFFFF) else Color(0xFFAEB4BF)
    val m = minOf(sz.x, sz.y)
    val r = androidx.compose.ui.geometry.CornerRadius(m * 0.22f, m * 0.22f)
    val a = item.bgAlpha.coerceIn(0f, 1f)
    drawRoundRect(body.copy(alpha = body.alpha * a), tl, androidx.compose.ui.geometry.Size(sz.x, sz.y), r)
    drawRoundRect(edge.copy(alpha = edge.alpha * a), tl, androidx.compose.ui.geometry.Size(sz.x, sz.y), r, style = androidx.compose.ui.graphics.drawscope.Stroke(maxOf(2f, m * 0.035f)))
    if (pressed) drawRoundRect(Color(0x55000000), tl, androidx.compose.ui.geometry.Size(sz.x, sz.y), r)
    val ink = if (item.captionColor != 0) item.captionColor else if (dark) 0xFF5EEAD4.toInt() else 0xFF20242B.toInt()
    drawFitText(context, item.caption ?: "", tl, sz, item.iconAlpha, pressed, ink, item.captionScale)
}
