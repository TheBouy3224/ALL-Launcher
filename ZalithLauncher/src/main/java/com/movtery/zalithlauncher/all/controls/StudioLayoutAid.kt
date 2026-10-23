package com.movtery.zalithlauncher.all.controls

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import com.movtery.layer_controller.data.ButtonPosition
import com.movtery.layer_controller.data.NormalData
import com.movtery.layer_controller.layout.ControlLayout
import com.movtery.layer_controller.utils.widgetPosition
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Layout helpers for ALL Controls Studio (pure geometry, screen pixels, same position rules as the game).
 * Nothing here touches input: it only decides where buttons go.
 */

/** Where each visible widget sits on a [sw] x [sh] screen, exactly as the game places it */
fun ControlLayout.widgetRects(sw: Float, sh: Float, density: Float): Map<String, Rect> = buildMap {
    layers.filterNot { it.hide }.forEach { l ->
        l.normalButtons.forEach { b ->
            val (w, h) = buttonPixelSize(b.buttonSize, sw, sh, density)
            put(b.uuid, rectAt(b.position, w, h, sw, sh))
        }
        l.joystickButtons.forEach { j ->
            val s = joystickPixelSize(j, sh, density)
            put(j.uuid, rectAt(j.position, s, s, sw, sh))
        }
    }
}

private fun rectAt(p: ButtonPosition, w: Float, h: Float, sw: Float, sh: Float): Rect {
    val o = widgetPosition(p.xPercentage(), p.yPercentage(), IntSize(w.toInt(), h.toInt()), IntSize(sw.toInt(), sh.toInt()))
    return Rect(Offset(o.x, o.y), Size(w, h))
}

/** The Studio's own toolbar strip (top centre); new buttons are never dropped under it */
fun toolbarZone(sw: Float, sh: Float) = Rect(sw * 0.15f, 0f, sw * 0.85f, sh * 0.18f)

/**
 * Minecraft's automatic GUI scale: the largest s with 320*s <= width and 240*s <= height.
 * ponytail: assumes GUI Scale "Auto" (the default); a fixed in-game GUI scale makes the HUD smaller, so the zones are a guide, not a rule.
 */
fun guiScale(sw: Float, sh: Float): Float = max(1f, min(floor(sw / 320f), floor(sh / 240f)))

/** Vanilla hotbar (182 x 22) plus the hearts/food rows above it, bottom centre */
fun hotbarZone(sw: Float, sh: Float): Rect {
    val s = guiScale(sw, sh)
    val w = 182 * s
    val h = 40 * s
    return Rect((sw - w) / 2, sh - h, (sw + w) / 2, sh)
}

/** A small square around the crosshair in the middle of the screen */
fun crosshairZone(sw: Float, sh: Float): Rect {
    val r = 12 * guiScale(sw, sh)
    return Rect(sw / 2 - r, sh / 2 - r, sw / 2 + r, sh / 2 + r)
}

/**
 * Nearest free top-left for a [w] x [h] widget: not over [blocked] (kept [gap] apart) and fully on screen.
 * Searches a grid of 1/40 screen height and picks the spot closest to [near] (default: centre). Null when nothing fits.
 */
fun findFreeSpot(w: Float, h: Float, sw: Float, sh: Float, blocked: List<Rect>, gap: Float, near: Offset? = null): Offset? {
    if (w > sw || h > sh) return null
    val step = sh / 40f
    val target = near ?: Offset((sw - w) / 2, (sh - h) / 2)
    fun axis(limit: Float) = generateSequence(0f) { (it + step).takeIf { n -> n <= limit } }.toList() + limit
    val inflated = blocked.map { it.inflate(gap) }
    return axis(sw - w).flatMap { x -> axis(sh - h).map { y -> Offset(x, y) } }
        .filter { c -> val r = Rect(c, Size(w, h)); inflated.none { it.overlaps(r) } }
        .minByOrNull { (it - target).getDistanceSquared() }
}

/** Where to drop a new widget of this size: away from other widgets, the toolbar, the hotbar and the crosshair */
fun ControlLayout.spotForNew(w: Float, h: Float, sw: Float, sh: Float, density: Float, near: Offset? = null): Offset? =
    findFreeSpot(w, h, sw, sh, widgetRects(sw, sh, density).values + listOf(toolbarZone(sw, sh), hotbarZone(sw, sh), crosshairZone(sw, sh)), 6 * density, near)

/** A copy keeps everything (name, keys, size, style, the three switches) with a new id and a new position */
fun ControlLayout.duplicateButton(id: String, at: ButtonPosition): Pair<ControlLayout, String>? {
    val src: NormalData = findButton(id) ?: return null
    val copy = src.copy(uuid = newId(), position = at)
    return copy(layers = layers.map { l -> if (l.normalButtons.any { it.uuid == id }) l.copy(normalButtons = l.normalButtons + copy) else l }) to copy.uuid
}

/** Result of snapping a dragged rectangle: how far to nudge it and which guide lines to draw */
data class Snap(val nudge: Offset, val xLine: Float?, val yLine: Float?)

/**
 * Snaps the left / centre / right (and top / middle / bottom) of [r] to the same edges of [others] or the screen centre,
 * when within [threshold] pixels. Each axis snaps on its own.
 */
fun snapRect(r: Rect, others: List<Rect>, sw: Float, sh: Float, threshold: Float): Snap {
    fun best(mine: List<Float>, targets: List<Float>): Pair<Float, Float>? =
        mine.flatMap { m -> targets.map { t -> (t - m) to t } }.filter { abs(it.first) <= threshold }.minByOrNull { abs(it.first) }
    val bx = best(listOf(r.left, r.center.x, r.right), others.flatMap { listOf(it.left, it.center.x, it.right) } + sw / 2)
    val by = best(listOf(r.top, r.center.y, r.bottom), others.flatMap { listOf(it.top, it.center.y, it.bottom) } + sh / 2)
    return Snap(Offset(bx?.first ?: 0f, by?.first ?: 0f), bx?.second, by?.second)
}

/** Widgets that cover another widget or one of the game's HUD [zones] */
fun overlapping(rects: Map<String, Rect>, zones: List<Rect>): Set<String> =
    rects.filter { (id, r) -> rects.any { (o, other) -> o != id && other.overlaps(r) } || zones.any { it.overlaps(r) } }.keys
