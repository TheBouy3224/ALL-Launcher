package com.movtery.zalithlauncher.ui.control.mouse

import com.movtery.zalithlauncher.all.launchersettings.CursorFormat
import com.movtery.zalithlauncher.all.launchersettings.sniffCursorFormat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Builds .ani files in memory the way Windows lays them out (RIFF ACON, anih, rate, seq, LIST fram of .cur files) */
internal object AniBuilder {
    private fun le(vararg ints: Int) = ByteBuffer.allocate(ints.size * 4).order(ByteOrder.LITTLE_ENDIAN).apply { ints.forEach { putInt(it) } }.array()
    private fun chunk(id: String, data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(id.toByteArray(Charsets.ISO_8859_1)); out.write(le(data.size)); out.write(data)
        if (data.size % 2 == 1) out.write(0)
        return out.toByteArray()
    }

    /** A .cur with one entry of [w]x[h], hotspot ([hx],[hy]) and [payload] as the image data */
    fun cur(w: Int, h: Int, hx: Int, hy: Int, payload: ByteArray = ByteArray(40)): ByteArray {
        val b = ByteBuffer.allocate(6 + 16 + payload.size).order(ByteOrder.LITTLE_ENDIAN)
        b.putShort(0); b.putShort(2); b.putShort(1)
        b.put((w and 0xFF).toByte()); b.put((h and 0xFF).toByte()); b.put(0); b.put(0)
        b.putShort(hx.toShort()); b.putShort(hy.toShort()); b.putInt(payload.size); b.putInt(22)
        b.put(payload)
        return b.array()
    }

    fun ani(frames: List<ByteArray>, steps: Int = frames.size, dispRate: Int = 5, rate: IntArray? = null, seq: IntArray? = null, nFramesField: Int = frames.size): ByteArray {
        val body = ByteArrayOutputStream()
        body.write("ACON".toByteArray())
        body.write(chunk("anih", le(36, nFramesField, steps, 0, 0, 0, 0, dispRate, if (seq != null) 3 else 1)))
        rate?.let { body.write(chunk("rate", le(*it))) }
        seq?.let { body.write(chunk("seq ", le(*it))) }
        val fram = ByteArrayOutputStream().apply { write("fram".toByteArray()); frames.forEach { write(chunk("icon", it)) } }.toByteArray()
        body.write(chunk("LIST", fram))
        val b = body.toByteArray()
        return "RIFF".toByteArray() + le(b.size) + b
    }
}

class AniCursorTest {
    private val f = List(3) { AniBuilder.cur(32, 32, 8, 4) }

    @Test fun plainAniUsesHeaderRateForEveryFrame() {
        val ani = parseAni(AniBuilder.ani(f, dispRate = 5))!!
        assertEquals(3, ani.frames.size)
        assertArrayEquals(intArrayOf(0, 1, 2), ani.steps)
        assertArrayEquals(intArrayOf(5, 5, 5), ani.jiffies)
        assertEquals(83, jiffiesToMs(5))
        assertEquals(CursorHotspot(25, 12), aniHotspotPercent(ani))
    }

    @Test fun rateAndSequenceChunksAreFollowed() {
        val ani = parseAni(AniBuilder.ani(f, steps = 4, rate = intArrayOf(2, 4, 6, 8), seq = intArrayOf(2, 0, 1, 0)))!!
        assertArrayEquals(intArrayOf(2, 0, 1, 0), ani.steps)
        assertArrayEquals(intArrayOf(2, 4, 6, 8), ani.jiffies)
    }

    @Test fun brokenFilesAreRejected() {
        val good = AniBuilder.ani(f)
        assertNull(parseAni(good.copyOf(good.size - 30))) //truncated chunk
        assertNull(parseAni(AniBuilder.ani(f, steps = 2, seq = intArrayOf(0, 7)))) //sequence points past the frames
        assertNull(parseAni(AniBuilder.ani(emptyList(), nFramesField = 3))) //no frames at all
        assertNull(parseAni(AniBuilder.ani(f, nFramesField = 0)))
        assertNull(parseAni(AniBuilder.ani(List(5) { AniBuilder.cur(32, 32, 0, 0) }), maxFrames = 4)) //over the frame limit
        assertNull(parseAni(AniBuilder.ani(listOf(byteArrayOf(1, 2, 3))))) //an icon chunk that is not a .cur
        assertNull(parseAni("RIFF\u0000\u0000\u0000\u0000WAVE".toByteArray(Charsets.ISO_8859_1)))
    }

    @Test fun curInfoPicksTheLargestImageAnd256IsStoredAsZero() {
        val c = AniBuilder.cur(0, 0, 128, 64)
        assertEquals(CurInfo(256, 256, 128, 64), curInfo(c))
        assertEquals(1.toByte(), curAsIco(c)[2])
        assertNotNull(curInfo(curAsIco(c)))
    }

    @Test fun sniffing() {
        assertEquals(CursorFormat.ANI, sniffCursorFormat(AniBuilder.ani(f)))
    }
}
