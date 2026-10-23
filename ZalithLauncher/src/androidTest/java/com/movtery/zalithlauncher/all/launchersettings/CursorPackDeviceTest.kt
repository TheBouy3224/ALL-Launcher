package com.movtery.zalithlauncher.all.launchersettings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.movtery.zalithlauncher.setting.AllSettings
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Runs inside the QA debug package on the phone: the real pack pipeline (ZIP -> decode check -> library -> install -> undo) with
 * a pack built in memory. Only the QA package's own cursor folder and settings are touched; the previous Arrow cursor is put back.
 */
@RunWith(AndroidJUnit4::class)
class CursorPackDeviceTest {
    private fun le(vararg ints: Int) = ByteBuffer.allocate(ints.size * 4).order(ByteOrder.LITTLE_ENDIAN).apply { ints.forEach { putInt(it) } }.array()
    private fun chunk(id: String, data: ByteArray) = id.toByteArray(Charsets.ISO_8859_1) + le(data.size) + data + (if (data.size % 2 == 1) byteArrayOf(0) else byteArrayOf())

    private fun cur32(size: Int, argb: Int, hx: Int, hy: Int): ByteArray {
        val maskRow = ((size + 31) / 32) * 4
        val dib = ByteBuffer.allocate(40 + size * size * 4 + maskRow * size).order(ByteOrder.LITTLE_ENDIAN)
        dib.putInt(40); dib.putInt(size); dib.putInt(size * 2); dib.putShort(1); dib.putShort(32)
        repeat(6) { dib.putInt(0) }
        repeat(size * size) { dib.putInt(argb) }
        repeat(maskRow * size) { dib.put(0) }
        val body = dib.array()
        val dir = ByteBuffer.allocate(22).order(ByteOrder.LITTLE_ENDIAN)
        dir.putShort(0); dir.putShort(2); dir.putShort(1)
        dir.put(size.toByte()); dir.put(size.toByte()); dir.put(0); dir.put(0)
        dir.putShort(hx.toShort()); dir.putShort(hy.toShort()); dir.putInt(body.size); dir.putInt(22)
        return dir.array() + body
    }

    private fun ani(vararg colors: Int, hx: Int = 3, hy: Int = 3): ByteArray {
        val out = ByteArrayOutputStream()
        out.write("ACON".toByteArray())
        out.write(chunk("anih", le(36, colors.size, colors.size, 0, 0, 0, 0, 6, 1)))
        val fram = ByteArrayOutputStream().apply { write("fram".toByteArray()); colors.forEach { write(chunk("icon", cur32(32, it, hx, hy))) } }.toByteArray()
        out.write(chunk("LIST", fram))
        val b = out.toByteArray()
        return "RIFF".toByteArray() + le(b.size) + b
    }

    private val inf = """
        [Wreg]
        HKCU,"Control Panel\Cursors",Arrow,0x00020000,"%10%\%CUR_DIR%\%pointer%"
        HKCU,"Control Panel\Cursors",IBeam,0x00020000,"%10%\%CUR_DIR%\%text%"
        HKCU,"Control Panel\Cursors",Hand,0x00020000,"%10%\%CUR_DIR%\%link%"
        HKCU,"Control Panel\Cursors",crosshair,0x00020000,"%10%\%CUR_DIR%\%precision%"
        HKCU,"Control Panel\Cursors",SizeNS,0x00020000,"%10%\%CUR_DIR%\%vert%"
        HKCU,"Control Panel\Cursors",SizeWE,0x00020000,"%10%\%CUR_DIR%\%horz%"
        HKCU,"Control Panel\Cursors",SizeAll,0x00020000,"%10%\%CUR_DIR%\%move%"
        HKCU,"Control Panel\Cursors",No,0x00020000,"%10%\%CUR_DIR%\%unavailable%"
        HKLM,"SOFTWARE\Microsoft\Windows\CurrentVersion\Runonce\Setup\","",,"rundll32.exe shell32.dll,Control_RunDLL main.cpl @0,1"
        [Strings]
        SCHEME_NAME = "QA Test Pack"
        pointer = "Normal.ani"
        text = "Text.ani"
        link = "Link.ani"
        precision = "Precision.ani"
        vert = "Vertical.ani"
        horz = "Horizontal.ani"
        move = "Move.ani"
        unavailable = "Unavailable.ani"
        busy = "Busy.ani"
    """.trimIndent()

    private val red = 0xFFE53935.toInt()
    private val blue = 0xFF1E88E5.toInt()
    private val green = 0xFF43A047.toInt()

    private fun packZip(withTraversal: Boolean = false): ByteArray {
        val names = listOf("Normal", "Text", "Link", "Precision", "Vertical", "Horizontal", "Move", "Unavailable", "Busy")
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            fun put(n: String, b: ByteArray) { z.putNextEntry(ZipEntry(n)); z.write(b); z.closeEntry() }
            names.forEachIndexed { i, n -> put("WindowsTest/Test/$n.ani", ani(red, if (i % 2 == 0) blue else green)) }
            put("WindowsTest/Test/install.inf", inf.toByteArray())
            put("WindowsTest/Test/Broken.ani", "RIFF\u0010\u0000\u0000\u0000ACONjunkjunk".toByteArray(Charsets.ISO_8859_1))
            if (withTraversal) put("../evil.ani", ani(red))
            put("Readme.txt", "hello".toByteArray())
        }
        return out.toByteArray()
    }

    @Test fun zipPackImportsInstallsAndUndoes() {
        val imp = keepRenderable(readCursorZip(ByteArrayInputStream(packZip()), "fallback"))
        assertEquals("QA Test Pack", imp.name)
        assertEquals(9, imp.files.size)
        val skipped = imp.skipped.toMap()
        assertEquals(PackSkip.INVALID, skipped["Broken.ani"])
        assertEquals(PackSkip.NOT_A_CURSOR, skipped["Readme.txt"])
        val roles = guessCursorRoles(imp.files.keys, imp.infText)
        assertEquals(8, roles.size)
        assertEquals("Normal.ani", roles["Arrow"])

        val lib = packLibrary()
        lib.list().filter { it.name == "QA Test Pack" }.forEach { lib.delete(it) }
        val pack = lib.add(imp.name, imp.files, roles)

        val arrow = cursorEntries().first { it.shape.name == "Arrow" }
        val before = arrow.file.takeIf { it.isFile }?.readBytes()
        val beforeHotspot = arrow.hotspot.getValue()

        installPack(pack, roles)
        assertArrayEquals(pack.file("Normal.ani").readBytes(), arrow.file.readBytes())
        assertEquals(9, arrow.hotspot.getValue().xPercent) //3 px of 32
        assertTrue(canRestorePack())

        restoreLastPack()
        if (before == null) assertTrue(!arrow.file.exists()) else assertArrayEquals(before, arrow.file.readBytes())
        assertEquals(beforeHotspot, AllSettings.arrowMouseHotspot.getValue())
        //the pack stays in the library for the UI check
        assertTrue(lib.list().any { it.name == "QA Test Pack" && it.roles.size == 8 })
    }

    @Test fun zipWithATraversalPathIsRefusedOrSkipped() {
        //Android 14+ refuses the entry itself; older Android lists it and our name check skips it - nothing is ever written
        val r = runCatching { readCursorZip(ByteArrayInputStream(packZip(withTraversal = true)), "x") }
        r.onSuccess { assertEquals(PackSkip.UNSAFE_NAME, it.skipped.toMap()["../evil.ani"]) }
        r.onFailure { assertTrue(it is PackUnsafeException) }
    }

    @Test fun multiFileImportKeepsOnlyRenderableCursors() {
        val files = listOf(
            "Normal.ani" to ani(red, blue),
            "Text.ani" to ani(green),
            "notes.txt" to "x".toByteArray(),
            "bad.cur" to byteArrayOf(0, 0, 2, 0, 1, 0)
        )
        val imp = keepRenderable(readCursorFiles(files.map { (n, b) -> n to { ByteArrayInputStream(b) } }, "My cursors"))
        assertEquals(listOf("Normal.ani", "Text.ani"), imp.files.keys.toList())
        assertEquals(mapOf("Arrow" to "Normal.ani", "IBeam" to "Text.ani"), guessCursorRoles(imp.files.keys, null))
    }
}
