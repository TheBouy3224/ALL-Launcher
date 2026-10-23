package com.movtery.zalithlauncher.ui.control.mouse

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Windows animated cursor (.ani) reader, pure Kotlin (unit tested).
 *
 * Layout: RIFF 'ACON' { 'anih' header, optional 'rate' (jiffies per step), optional 'seq ' (frame index per step),
 * LIST 'fram' { 'icon' = a whole .cur/.ico file per frame } }. One jiffy = 1/60 s. The frame images themselves are decoded by
 * Android's own ICO decoder ([decodeAniFrames] in AnimatedCursor.kt); this file only walks the container and reads the hotspot.
 */
class AniFile(
    /** each frame as a complete .cur/.ico file */
    val frames: List<ByteArray>,
    /** frame index shown at each step */
    val steps: IntArray,
    /** duration of each step in jiffies (1/60 s) */
    val jiffies: IntArray
)

/** Largest image of a .cur/.ico: its size and (for .cur) the hotspot in pixels */
data class CurInfo(val width: Int, val height: Int, val hotspotX: Int, val hotspotY: Int)

const val ANI_MAX_STEPS = 1000

fun isAniBytes(bytes: ByteArray): Boolean =
    bytes.size >= 12 && String(bytes, 0, 4, Charsets.ISO_8859_1) == "RIFF" && String(bytes, 8, 4, Charsets.ISO_8859_1) == "ACON"

fun jiffiesToMs(j: Int): Int = (j.coerceAtLeast(1) * 1000 + 30) / 60

/** @return null for anything malformed: bad sizes, missing header/frames, out-of-range sequence, too many frames/steps */
fun parseAni(bytes: ByteArray, maxFrames: Int = CursorAnimLimits.MAX_FRAMES): AniFile? = runCatching {
    if (!isAniBytes(bytes)) return null
    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val riffEnd = (8L + (buf.getInt(4).toLong() and 0xFFFFFFFFL)).coerceAtMost(bytes.size.toLong()).toInt()

    var nFrames = -1
    var nSteps = -1
    var dispRate = 0
    var rate: IntArray? = null
    var seq: IntArray? = null
    val icons = ArrayList<ByteArray>()

    fun walk(start: Int, end: Int, inFram: Boolean) {
        var off = start
        while (off + 8 <= end) {
            val id = String(bytes, off, 4, Charsets.ISO_8859_1)
            val size = buf.getInt(off + 4).toLong() and 0xFFFFFFFFL
            val data = off + 8
            if (size > end - data) error("chunk $id runs past its parent")
            val sz = size.toInt()
            when {
                id == "LIST" && sz >= 4 -> walk(data + 4, data + sz, String(bytes, data, 4, Charsets.ISO_8859_1) == "fram")
                id == "anih" && sz >= 36 -> {
                    nFrames = buf.getInt(data + 4)
                    nSteps = buf.getInt(data + 8)
                    dispRate = buf.getInt(data + 28)
                }
                id == "rate" -> rate = IntArray(sz / 4) { buf.getInt(data + it * 4) }
                id == "seq " -> seq = IntArray(sz / 4) { buf.getInt(data + it * 4) }
                id == "icon" && inFram -> icons += bytes.copyOfRange(data, data + sz)
            }
            off = data + sz + (sz and 1)
        }
    }
    walk(12, riffEnd, false)

    if (nFrames <= 0 || icons.isEmpty()) return null
    val frameCount = minOf(nFrames, icons.size)
    if (frameCount > maxFrames) return null
    val stepCount = if (nSteps > 0) nSteps else frameCount
    if (stepCount > ANI_MAX_STEPS) return null
    val order = seq?.takeIf { it.size >= stepCount }?.copyOf(stepCount) ?: IntArray(stepCount) { it % frameCount }
    if (order.any { it !in 0 until frameCount }) return null
    val times = rate?.takeIf { it.size >= stepCount }?.copyOf(stepCount) ?: IntArray(stepCount) { dispRate }
    if (icons.take(frameCount).any { curInfo(it) == null }) return null
    AniFile(icons.take(frameCount), order, IntArray(stepCount) { times[it].coerceIn(1, 600) })
}.getOrNull()

/** Reads the ICONDIR of a .cur/.ico; for a cursor (type 2) the hotspot is stored where an icon keeps planes/bit count */
fun curInfo(cur: ByteArray): CurInfo? = runCatching {
    val b = ByteBuffer.wrap(cur).order(ByteOrder.LITTLE_ENDIAN)
    val type = b.getShort(2).toInt()
    val count = b.getShort(4).toInt() and 0xFFFF
    if (b.getShort(0).toInt() != 0 || (type != 1 && type != 2) || count !in 1..64 || cur.size < 6 + count * 16) return null
    (0 until count).map { i ->
        val e = 6 + i * 16
        val w = (cur[e].toInt() and 0xFF).let { if (it == 0) 256 else it }
        val h = (cur[e + 1].toInt() and 0xFF).let { if (it == 0) 256 else it }
        val hx = if (type == 2) b.getShort(e + 4).toInt() and 0xFFFF else 0
        val hy = if (type == 2) b.getShort(e + 6).toInt() and 0xFFFF else 0
        val len = b.getInt(e + 8).toLong() and 0xFFFFFFFFL
        val at = b.getInt(e + 12).toLong() and 0xFFFFFFFFL
        if (at + len > cur.size) return null
        CurInfo(w, h, hx.coerceAtMost(w), hy.coerceAtMost(h))
    }.maxByOrNull { it.width * it.height }
}.getOrNull()

/** Hotspot of the first frame as the percent values the cursor settings store */
fun aniHotspotPercent(ani: AniFile): CursorHotspot? =
    curInfo(ani.frames.first())?.let { CursorHotspot(it.hotspotX * 100 / it.width, it.hotspotY * 100 / it.height) }

/** Android's ICO decoder takes .cur too once the type field says "icon"; the hotspot is read separately above */
fun curAsIco(cur: ByteArray): ByteArray = cur.copyOf().also { if (it.size > 3 && it[2].toInt() == 2) it[2] = 1 }
