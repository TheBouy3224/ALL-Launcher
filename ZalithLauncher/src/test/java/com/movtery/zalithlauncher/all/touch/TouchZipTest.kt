package com.movtery.zalithlauncher.all.touch

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TouchZipTest {
    private val profile = """{"version":1,"skin":"a","items":[{"id":"a1","kind":"BUTTON"}]}"""
    private fun zip(vararg e: Pair<String, ByteArray>) = ByteArrayOutputStream().also { o -> ZipOutputStream(o).use { z -> e.forEach { (n, b) -> z.putNextEntry(ZipEntry(n)); z.write(b); z.closeEntry() } } }.toByteArray()

    @Test fun roundTripKeepsProfileAndPictures() {
        val png = byteArrayOf(-119, 80, 78, 71, 1, 2, 3); val gif = "GIF89a....".toByteArray()
        val out = ByteArrayOutputStream(); TouchZip.write(out, "My Profile", profile, mapOf("a1_1.png" to png, "a1_2.gif" to gif))
        assertTrue(TouchZip.isZip(out.toByteArray()))
        val p = TouchZip.read(out.toByteArray())
        assertEquals("My Profile", p.name); assertEquals(profile, p.profile); assertEquals(0, p.skipped)
        assertEquals(setOf("a1_1.png", "a1_2.gif"), p.images.keys)
        assertArrayEquals(png, p.images["a1_1.png"]); assertArrayEquals(gif, p.images["a1_2.gif"])
    }

    @Test fun unsafeAndForeignEntriesAreIgnored() {
        val p = TouchZip.read(zip("profile.json" to profile.toByteArray(), "images/../../evil.png" to byteArrayOf(1), "images/sub/x.png" to byteArrayOf(1),
            "images/run.sh" to byteArrayOf(1), "notes.txt" to byteArrayOf(1), "images/ok.PNG" to byteArrayOf(1)))
        assertEquals(setOf("ok.PNG"), p.images.keys); assertEquals(4, p.skipped)
    }

    @Test fun windowsBackslashNamesWork() {
        val p = TouchZip.read(zip("manifest.json" to "{\"name\":\"W\"}".toByteArray(), "profile.json" to profile.toByteArray(), "images\\a.png" to byteArrayOf(1), "images\\..\\b.png" to byteArrayOf(1)))
        assertEquals(setOf("a.png"), p.images.keys); assertEquals("W", p.name); assertEquals(1, p.skipped)
    }

    @Test fun oversizeImageIsSkippedNotFatal() {
        val p = TouchZip.read(zip("profile.json" to profile.toByteArray(), "images/big.png" to ByteArray(11 * 1024 * 1024), "images/s.png" to byteArrayOf(1)))
        assertEquals(setOf("s.png"), p.images.keys); assertEquals(1, p.skipped)
    }

    @Test fun badArchivesGiveAReason() {
        fun msg(b: ByteArray) = runCatching { TouchZip.read(b) }.exceptionOrNull()!!.message!!
        assertTrue(msg(zip("images/a.png" to byteArrayOf(1))).contains("profile.json"))
        assertTrue(msg(zip("profile.json" to "not json".toByteArray())).contains("อ่านไม่ได้"))
        assertTrue(msg(zip("profile.json" to "{\"x\":1}".toByteArray())).contains("อ่านไม่ได้"))
        assertFalse(TouchZip.isZip("{\"items\":[]}".toByteArray()))
    }

    @Test fun tooManyEntriesRejected() {
        val e = (0..401).map { "images/i$it.png" to byteArrayOf(1) }.toTypedArray()
        assertTrue(runCatching { TouchZip.read(zip("profile.json" to profile.toByteArray(), *e)) }.isFailure)
    }
}
