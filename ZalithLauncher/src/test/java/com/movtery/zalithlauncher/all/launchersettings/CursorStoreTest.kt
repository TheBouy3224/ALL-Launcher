package com.movtery.zalithlauncher.all.launchersettings

import com.movtery.zalithlauncher.ui.control.mouse.cursorFramesBytes
import com.movtery.zalithlauncher.ui.control.mouse.nextCursorFrame
import com.movtery.zalithlauncher.ui.control.mouse.scaledFrameDelay
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CursorStoreTest {
    private fun store(): Pair<CursorStore, File> {
        val dir = Files.createTempDirectory("cursor").toFile()
        val live = File(dir, "default_pointer.image")
        return CursorStore(live) to live
    }

    @Test fun importIsStagedAndLiveIsUntouchedUntilApply() {
        val (s, live) = store()
        live.writeBytes(byteArrayOf(1, 1))
        s.stage(byteArrayOf(2, 2))
        assertArrayEquals(byteArrayOf(1, 1), live.readBytes())
        s.discard() //Cancel
        assertFalse(s.hasStaged())
        assertArrayEquals(byteArrayOf(1, 1), live.readBytes())
    }

    @Test fun applyKeepsThePreviousCursorAndRestoreSwapsBack() {
        val (s, live) = store()
        live.writeBytes(byteArrayOf(1))
        s.stage(byteArrayOf(2))
        s.apply(toDefault = false)
        assertArrayEquals(byteArrayOf(2), live.readBytes())
        assertArrayEquals(byteArrayOf(1), s.backup.readBytes())
        assertFalse(s.hasStaged())
        s.restore()
        assertArrayEquals(byteArrayOf(1), live.readBytes())
        assertArrayEquals(byteArrayOf(2), s.backup.readBytes())
    }

    @Test fun resetToDefaultKeepsABackupAndRestoreBringsItBack() {
        val (s, live) = store()
        live.writeBytes(byteArrayOf(7))
        s.apply(toDefault = true)
        assertFalse(live.exists())
        s.restore()
        assertArrayEquals(byteArrayOf(7), live.readBytes())
        assertFalse(s.hasBackup())
    }

    @Test fun formats() {
        assertEquals(CursorFormat.PNG, sniffCursorFormat(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D)))
        assertEquals(CursorFormat.GIF, sniffCursorFormat("GIF89a".toByteArray()))
        assertEquals(CursorFormat.WEBP, sniffCursorFormat("RIFF\u0000\u0000\u0000\u0000WEBPVP8 ".toByteArray(Charsets.ISO_8859_1)))
        assertNull(sniffCursorFormat("<svg xmlns=".toByteArray()))
        assertNull(sniffCursorFormat(ByteArray(2)))
    }

    @Test fun animationTiming() {
        assertEquals(1, nextCursorFrame(0, 3, loop = false))
        assertEquals(0, nextCursorFrame(2, 3, loop = true))
        assertNull(nextCursorFrame(2, 3, loop = false))
        assertNull(nextCursorFrame(0, 1, loop = true))
        assertEquals(50L, scaledFrameDelay(100, 200))
        assertEquals(200L, scaledFrameDelay(100, 50))
        assertEquals(16L, scaledFrameDelay(10, 300)) //never faster than a display frame
        assertTrue(cursorFramesBytes(256, 256, 120) > 24L * 1024 * 1024) //the frame cap alone is not enough; bytes are checked too
    }
}
