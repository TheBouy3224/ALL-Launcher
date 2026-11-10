package com.movtery.zalithlauncher.all.bouy

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.Movie
import android.os.SystemClock
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Per-button look. The supplied sprites are one picture (grey body + dark outline/icon), so "body" and "icon" are told apart by
 * brightness: light pixels = body (gets the colour and the body opacity), dark pixels = outline + icon (keep their own colour,
 * get the icon opacity). A soft edge between the two avoids a halo. Results are cached; the hit area never depends on any of this.
 */
object BouyStyle {
    private val cache = LruCache<String, ImageBitmap>(96)

    private fun smooth(e0: Float, e1: Float, x: Float): Float {
        val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
        return t * t * (3 - 2 * t)
    }

    /** [tint] 0 = keep the original colours */
    @Synchronized
    fun styled(key: String, src: ImageBitmap, tint: Int, bg: Float, icon: Float): ImageBitmap {
        if (tint == 0 && bg >= 0.999f && icon >= 0.999f) return src
        val k = "$key|$tint|${(bg * 100).toInt()}|${(icon * 100).toInt()}"
        cache.get(k)?.let { return it }
        val s = src.asAndroidBitmap()
        val w = s.width; val h = s.height
        val px = IntArray(w * h)
        s.getPixels(px, 0, w, 0, 0, w, h)
        val tr = AColor.red(tint); val tg = AColor.green(tint); val tb = AColor.blue(tint)
        for (i in px.indices) {
            val c = px[i]
            val a = AColor.alpha(c)
            if (a == 0) continue
            val r = AColor.red(c); val g = AColor.green(c); val b = AColor.blue(c)
            val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            val body = smooth(0.38f, 0.58f, lum)
            var nr = r; var ng = g; var nb = b
            if (tint != 0 && body > 0f) {
                // multiply (x1.3 so a mid colour is not muddy), only on the body
                nr = (r + (min(255f, r * tr / 255f * 1.3f) - r) * body).toInt()
                ng = (g + (min(255f, g * tg / 255f * 1.3f) - g) * body).toInt()
                nb = (b + (min(255f, b * tb / 255f * 1.3f) - b) * body).toInt()
            }
            val na = (a * (body * bg + (1f - body) * icon)).toInt().coerceIn(0, 255)
            px[i] = AColor.argb(na, nr.coerceIn(0, 255), ng.coerceIn(0, 255), nb.coerceIn(0, 255))
        }
        val out = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888).asImageBitmap()
        cache.put(k, out)
        return out
    }

    // ---- player pictures ----
    private val pictures = HashMap<String, ImageBitmap?>()
    const val MAX_INPUT_BYTES = 10 * 1024 * 1024
    private const val MAX_SIDE = 512

    /** a GIF turned into a short list of frames (decoded once, in the background); [picture] returns the frame for the current time */
    private class Anim(val frames: List<ImageBitmap>, val intervalMs: Int)
    private val anims = HashMap<String, Anim>()
    private val animBusy = HashSet<String>() // decoding now, or failed (never retried: a bad file must not spawn a thread on every frame)

    fun isAnimated(name: String?) = name != null && name.endsWith(".gif", ignoreCase = true)

    @Synchronized
    fun picture(name: String): ImageBitmap? {
        if (isAnimated(name)) {
            anims[name]?.let { a -> return a.frames[((SystemClock.uptimeMillis() / a.intervalMs) % a.frames.size).toInt()] }
            if (animBusy.add(name)) Thread {
                val r = runCatching { decodeGif(name) }.getOrNull()
                synchronized(this) { if (r != null) anims[name] = r }
            }.start()
            return null
        }
        return pictures.getOrPut(name) {
            runCatching { BitmapFactory.decodeFile(File(BouyStore.imagesDir(), name).path)?.asImageBitmap() }.getOrNull()
        }
    }

    /** at most 40 frames, longest side 256 px, at most the first 8 s of the loop: small in memory, fine to redraw 15 times a second */
    private fun decodeGif(name: String): Anim {
        val bytes = File(BouyStore.imagesDir(), name).readBytes()
        val m = Movie.decodeByteArray(bytes, 0, bytes.size) ?: error("not a gif")
        val w = m.width(); val h = m.height()
        require(w > 0 && h > 0)
        val dur = m.duration()
        val scale = min(1f, 256f / max(w, h))
        val tw = max(1, (w * scale).toInt()); val th = max(1, (h * scale).toInt())
        val total = if (dur > 0) min(dur, 8000) else 1
        val n = (total / 50).coerceIn(1, 40)
        val step = if (dur > 0) max(40, total / n) else 1000
        val frames = (0 until n).map { i ->
            val bmp = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp); c.scale(scale, scale)
            m.setTime(if (dur > 0) (i * step) % dur else 0); m.draw(c, 0f, 0f)
            bmp.asImageBitmap()
        }
        return Anim(frames, step)
    }

    sealed interface ImportResult {
        data class Ok(val name: String) : ImportResult
        data class Fail(val reason: String) : ImportResult
    }

    /**
     * Copy a picked picture into the app's private folder: PNG / JPG / WebP / GIF, at most 10 MB, longest side scaled down to 512 px
     * (aspect kept, alpha kept), stored as PNG under a new name. Never throws: a broken or unsupported file is a [ImportResult.Fail].
     */
    fun importPicture(stream: InputStream, itemId: String): ImportResult = try {
        val buf = ByteArrayOutputStream()
        val chunk = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val n = stream.read(chunk)
            if (n < 0) break
            total += n
            if (total > MAX_INPUT_BYTES) return ImportResult.Fail("ไฟล์ใหญ่เกิน 10 MB")
            buf.write(chunk, 0, n)
        }
        val bytes = buf.toByteArray()
        if (bytes.size > 6 && String(bytes, 0, 4, Charsets.ISO_8859_1) == "GIF8") { // animated (or still) GIF: kept as the file itself, played by [picture]
            val m = Movie.decodeByteArray(bytes, 0, bytes.size)
            if (m == null || m.width() <= 0 || m.height() <= 0) return ImportResult.Fail("ไฟล์ GIF เสียหาย")
            if (m.width().toLong() * m.height() > 16_000_000L) return ImportResult.Fail("GIF มีขนาดพิกเซลใหญ่เกินไป")
            val gname = "${itemId}_${System.currentTimeMillis()}.gif"
            File(BouyStore.imagesDir(), gname).writeBytes(bytes)
            return ImportResult.Ok(gname)
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return ImportResult.Fail("ไฟล์รูปเสียหาย หรือไม่ใช่รูปภาพ")
        if (bounds.outMimeType !in setOf("image/png", "image/jpeg", "image/webp")) return ImportResult.Fail("รองรับเฉพาะ PNG, JPG, WebP, GIF")
        if (!looksComplete(bytes, bounds.outMimeType)) return ImportResult.Fail("ไฟล์รูปเสียหาย (ข้อมูลไม่ครบ)")
        if (bounds.outWidth.toLong() * bounds.outHeight > 64_000_000L) return ImportResult.Fail("รูปมีขนาดพิกเซลใหญ่เกินไป")
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return ImportResult.Fail("ไฟล์รูปเสียหาย")
        val scale = MAX_SIDE.toFloat() / max(bmp.width, bmp.height)
        val fin = if (scale < 1f) Bitmap.createScaledBitmap(bmp, max(1, (bmp.width * scale).toInt()), max(1, (bmp.height * scale).toInt()), true) else bmp
        val name = "${itemId}_${System.currentTimeMillis()}.png"
        File(BouyStore.imagesDir(), name).outputStream().use { fin.compress(Bitmap.CompressFormat.PNG, 100, it) }
        ImportResult.Ok(name)
    } catch (t: Throwable) { // OOM included: a bad picture must never take the game down
        ImportResult.Fail("นำเข้ารูปไม่สำเร็จ")
    }

    /** BitmapFactory silently decodes a cut-off file as a half picture; refuse those (PNG needs IEND, JPEG needs EOI, WebP needs its RIFF length) */
    private fun looksComplete(b: ByteArray, mime: String?): Boolean {
        val tail = b.copyOfRange(max(0, b.size - 32), b.size)
        return when (mime) {
            "image/png" -> String(tail, Charsets.ISO_8859_1).contains("IEND")
            "image/jpeg" -> (0 until tail.size - 1).any { tail[it] == 0xFF.toByte() && tail[it + 1] == 0xD9.toByte() }
            "image/webp" -> b.size >= 12 && (((b[4].toInt() and 0xFF) or ((b[5].toInt() and 0xFF) shl 8) or ((b[6].toInt() and 0xFF) shl 16) or ((b[7].toInt() and 0xFF) shl 24)) + 8L) <= b.size
            else -> false
        }
    }

    fun deletePicture(name: String?) {
        if (name == null) return
        synchronized(this) { pictures.remove(name); anims.remove(name); animBusy.remove(name) }
        runCatching { File(BouyStore.imagesDir(), name).delete() }
    }
}
