package com.movtery.zalithlauncher.ui.control.mouse

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.movtery.zalithlauncher.all.launchersettings.CursorImportError
import com.movtery.zalithlauncher.all.launchersettings.checkCursorBytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Runs on the phone: real Windows-style .ani files (32-bit BMP frames inside .cur, the classic layout) built in memory are decoded
 * by Android's own ICO decoder through the same path the cursor settings and the game pointer use. No user files are read.
 */
@RunWith(AndroidJUnit4::class)
class AniDecodeDeviceTest {
    private fun le(vararg ints: Int) = ByteBuffer.allocate(ints.size * 4).order(ByteOrder.LITTLE_ENDIAN).apply { ints.forEach { putInt(it) } }.array()
    private fun chunk(id: String, data: ByteArray): ByteArray =
        id.toByteArray(Charsets.ISO_8859_1) + le(data.size) + data + (if (data.size % 2 == 1) byteArrayOf(0) else byteArrayOf())

    /** .cur holding one 32-bit BMP (BITMAPINFOHEADER, height doubled, bottom-up BGRA, then a 1-bit AND mask) */
    private fun cur32(size: Int, argb: Int, hx: Int, hy: Int): ByteArray {
        val maskRow = ((size + 31) / 32) * 4
        val dib = ByteBuffer.allocate(40 + size * size * 4 + maskRow * size).order(ByteOrder.LITTLE_ENDIAN)
        dib.putInt(40); dib.putInt(size); dib.putInt(size * 2); dib.putShort(1); dib.putShort(32)
        dib.putInt(0); dib.putInt(size * size * 4); dib.putInt(0); dib.putInt(0); dib.putInt(0); dib.putInt(0)
        repeat(size * size) { dib.putInt(argb) } //little-endian int = B,G,R,A bytes
        repeat(maskRow * size) { dib.put(0) } //0 = opaque
        val body = dib.array()
        val dir = ByteBuffer.allocate(22).order(ByteOrder.LITTLE_ENDIAN)
        dir.putShort(0); dir.putShort(2); dir.putShort(1)
        dir.put(size.toByte()); dir.put(size.toByte()); dir.put(0); dir.put(0)
        dir.putShort(hx.toShort()); dir.putShort(hy.toShort()); dir.putInt(body.size); dir.putInt(22)
        return dir.array() + body
    }

    private fun ani(frames: List<ByteArray>, dispRate: Int, seq: IntArray? = null): ByteArray {
        val out = ByteArrayOutputStream()
        out.write("ACON".toByteArray())
        val steps = seq?.size ?: frames.size
        out.write(chunk("anih", le(36, frames.size, steps, 0, 0, 0, 0, dispRate, if (seq != null) 3 else 1)))
        seq?.let { out.write(chunk("seq ", le(*it))) }
        val fram = ByteArrayOutputStream().apply { write("fram".toByteArray()); frames.forEach { write(chunk("icon", it)) } }.toByteArray()
        out.write(chunk("LIST", fram))
        val b = out.toByteArray()
        return "RIFF".toByteArray() + le(b.size) + b
    }

    private val red = 0xFFFF0000.toInt()
    private val blue = 0xFF0000FF.toInt()

    @Test fun bmpFramesDecodeWithColoursTimingAndHotspot() {
        val bytes = ani(listOf(cur32(32, red, 4, 2), cur32(32, blue, 4, 2)), dispRate = 6, seq = intArrayOf(0, 1, 1))
        val check = checkCursorBytes(bytes)
        assertNull(check.error)
        assertEquals(CursorHotspot(12, 6), check.hotspot)
        val frames = decodeAniFrames(bytes)
        assertNotNull(frames)
        frames!!
        assertEquals(3, frames.size)
        assertEquals(100, frames.durationsMs[0])
        val a = frames.bitmaps[0].asAndroidBitmap()
        assertEquals(32, a.width)
        assertEquals(red, a.getPixel(16, 16))
        assertEquals(blue, frames.bitmaps[1].asAndroidBitmap().getPixel(16, 16))
        //steps 1 and 2 are the same decoded frame, not a copy
        assertEquals(true, frames.bitmaps[1] === frames.bitmaps[2])
    }

    @Test fun brokenAniIsRejectedBeforeItCanBeStaged() {
        val good = ani(listOf(cur32(32, red, 0, 0), cur32(32, blue, 0, 0)), dispRate = 5)
        assertEquals(CursorImportError.CORRUPT, checkCursorBytes(good.copyOf(good.size - 100)).error)
        //valid container, but the frame bytes are not an image
        val bad = ani(listOf(ByteArray(22).also { it[2] = 2; it[4] = 1 } + ByteArray(40)), dispRate = 5)
        assertNotNull(checkCursorBytes(bad).error)
    }
}
