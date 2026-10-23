package com.movtery.zalithlauncher.ui.control.mouse

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Movie
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import java.io.File

/**
 * Animated cursor V1 (ALL): a GIF cursor is decoded ONCE into a small list of frames, then [AnimatedPointerImage] only switches
 * which prepared bitmap is shown. No bitmap is created per render frame, and the virtual pointer stays the same Compose image
 * inside [MousePointer] (no new overlay or renderer).
 *
 * Limits keep a bad file from eating memory: [MAX_FRAMES] frames, [MAX_SIDE] px per side, [MAX_BYTES] of decoded pixels.
 */
object CursorAnimLimits {
    const val MAX_FILE_BYTES = 2 * 1024 * 1024
    const val MAX_SIDE = 256
    const val MAX_FRAMES = 120
    const val MAX_BYTES = 24L * 1024 * 1024
    /** frames shorter than this are sampled at this step (most GIF players clamp tiny delays the same way) */
    const val SAMPLE_MS = 20
}

class CursorFrames(val bitmaps: List<ImageBitmap>, val durationsMs: IntArray) {
    val size: Int get() = bitmaps.size
}

/** Decoded pixel bytes of [frames] frames of [w] x [h] ARGB_8888 */
fun cursorFramesBytes(w: Int, h: Int, frames: Int): Long = w.toLong() * h * 4 * frames

/** Next frame to show, or null when a non-looping animation has reached its last frame */
fun nextCursorFrame(index: Int, size: Int, loop: Boolean): Int? = when {
    size <= 1 -> null
    index + 1 < size -> index + 1
    loop -> 0
    else -> null
}

/** How long a frame stays on screen at [speedPercent] (100 = the file's own timing); never below one display frame */
fun scaledFrameDelay(durationMs: Int, speedPercent: Int): Long =
    (durationMs.toLong() * 100 / speedPercent.coerceIn(10, 1000)).coerceAtLeast(16L)

fun isGifFile(file: File): Boolean = runCatching {
    file.inputStream().use { input ->
        val head = ByteArray(4)
        input.read(head) == 4 && head[0] == 'G'.code.toByte() && head[1] == 'I'.code.toByte() && head[2] == 'F'.code.toByte() && head[3] == '8'.code.toByte()
    }
}.getOrDefault(false)

/**
 * Decodes a GIF into frames by sampling its timeline (android.graphics.Movie) and merging identical neighbours.
 * @return null when it is not an animated GIF or it breaks a limit (the caller then shows the file as a normal image)
 */
@Suppress("DEPRECATION")
fun decodeCursorFrames(bytes: ByteArray): CursorFrames? {
    val movie = Movie.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val w = movie.width()
    val h = movie.height()
    val duration = movie.duration()
    if (w <= 0 || h <= 0 || w > CursorAnimLimits.MAX_SIDE || h > CursorAnimLimits.MAX_SIDE || duration <= 0) return null

    val bitmaps = ArrayList<Bitmap>()
    val durations = ArrayList<Int>()
    var t = 0
    while (t < duration) {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        movie.setTime(t)
        movie.draw(Canvas(bmp), 0f, 0f)
        val last = bitmaps.lastOrNull()
        if (last != null && last.sameAs(bmp)) {
            durations[durations.size - 1] += CursorAnimLimits.SAMPLE_MS
            bmp.recycle()
        } else {
            bitmaps += bmp
            durations += CursorAnimLimits.SAMPLE_MS
            if (bitmaps.size > CursorAnimLimits.MAX_FRAMES || cursorFramesBytes(w, h, bitmaps.size) > CursorAnimLimits.MAX_BYTES) {
                bitmaps.forEach { it.recycle() }
                return null
            }
        }
        t += CursorAnimLimits.SAMPLE_MS
    }
    if (bitmaps.size < 2) {
        bitmaps.forEach { it.recycle() }
        return null
    }
    return CursorFrames(bitmaps.map { it.asImageBitmap() }, durations.toIntArray())
}

/** Decodes a Windows .ani into frames: every frame is decoded once, steps reuse those bitmaps (no copy per step) */
fun decodeAniFrames(bytes: ByteArray): CursorFrames? {
    val ani = parseAni(bytes) ?: return null
    val decoded = ArrayList<Bitmap>()
    fun fail(): CursorFrames? { decoded.forEach { it.recycle() }; return null }
    for (cur in ani.frames) {
        val bmp = BitmapFactory.decodeByteArray(curAsIco(cur), 0, cur.size) ?: return fail()
        decoded += bmp
        if (bmp.width > CursorAnimLimits.MAX_SIDE || bmp.height > CursorAnimLimits.MAX_SIDE) return fail()
        if (decoded.sumOf { cursorFramesBytes(it.width, it.height, 1) } > CursorAnimLimits.MAX_BYTES) return fail()
    }
    val images = decoded.map { it.asImageBitmap() }
    return CursorFrames(ani.steps.map { images[it] }, IntArray(ani.steps.size) { jiffiesToMs(ani.jiffies[it]) })
}

/** Small cache so the game pointer and previews do not decode the same file twice; keyed by path + size + modified time */
object CursorFrameCache {
    private val cache = object : LinkedHashMap<String, CursorFrames?>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CursorFrames?>?) = size > 10
    }

    @Synchronized
    fun get(file: File): CursorFrames? {
        if (!file.isFile || file.length() > CursorAnimLimits.MAX_FILE_BYTES) return null
        val key = "${file.absolutePath}:${file.length()}:${file.lastModified()}"
        if (cache.containsKey(key)) return cache[key]
        return runCatching {
            val bytes = file.readBytes()
            when {
                isAniBytes(bytes) -> decodeAniFrames(bytes)
                //a single .cur / .ico: one frame (Coil cannot read the cursor type, Android's ICO decoder can)
                bytes.size > 4 && bytes[0].toInt() == 0 && bytes[1].toInt() == 0 && bytes[3].toInt() == 0 && (bytes[2].toInt() == 1 || bytes[2].toInt() == 2) && curInfo(bytes) != null ->
                    BitmapFactory.decodeByteArray(curAsIco(bytes), 0, bytes.size)?.let { CursorFrames(listOf(it.asImageBitmap()), intArrayOf(1000)) }
                isGifFile(file) -> decodeCursorFrames(bytes)
                else -> null
            }
        }.getOrNull().also { cache[key] = it }
    }
}

@Composable
fun AnimatedPointerImage(
    frames: CursorFrames,
    loop: Boolean,
    speedPercent: Int,
    alignment: Alignment,
    modifier: Modifier = Modifier
) {
    var index by remember(frames) { mutableIntStateOf(0) }
    LaunchedEffect(frames, loop, speedPercent) {
        index = 0
        while (true) {
            delay(scaledFrameDelay(frames.durationsMs[index], speedPercent))
            index = nextCursorFrame(index, frames.size, loop) ?: break
        }
    }
    Image(
        bitmap = frames.bitmaps[index.coerceIn(0, frames.size - 1)],
        contentDescription = null,
        alignment = alignment,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
