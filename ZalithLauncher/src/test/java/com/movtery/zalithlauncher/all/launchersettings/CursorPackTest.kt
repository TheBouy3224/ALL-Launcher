package com.movtery.zalithlauncher.all.launchersettings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CursorPackTest {
    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z -> entries.forEach { (n, b) -> z.putNextEntry(ZipEntry(n)); z.write(b); z.closeEntry() } }
        return out.toByteArray()
    }

    private val ani = "RIFF....ACON".toByteArray()

    /** the role part of a real Windows cursor scheme .inf (same layout as the Mita pack) */
    private val inf = """
        [Wreg]
        HKCU,"Control Panel\Cursors",,0x00020000,"%SCHEME_NAME%"
        HKCU,"Control Panel\Cursors",Arrow,0x00020000,"%10%\%CUR_DIR%\%pointer%"
        HKCU,"Control Panel\Cursors",crosshair,0x00020000,"%10%\%CUR_DIR%\%precision%"
        HKCU,"Control Panel\Cursors",Hand,0x00020000,"%10%\%CUR_DIR%\%link%"
        HKCU,"Control Panel\Cursors",IBeam,0x00020000,"%10%\%CUR_DIR%\%text%"
        HKCU,"Control Panel\Cursors",No,0x00020000,"%10%\%CUR_DIR%\%unavailable%"
        HKCU,"Control Panel\Cursors",NWPen,0x00020000,"%10%\%CUR_DIR%\%hand%"
        HKCU,"Control Panel\Cursors",SizeAll,0x00020000,"%10%\%CUR_DIR%\%move%"
        HKCU,"Control Panel\Cursors",SizeNS,0x00020000,"%10%\%CUR_DIR%\%vert%"
        HKCU,"Control Panel\Cursors",SizeWE,0x00020000,"%10%\%CUR_DIR%\%horz%"
        HKCU,"Control Panel\Cursors",Wait,0x00020000,"%10%\%CUR_DIR%\%busy%"
        HKLM,"SOFTWARE\Microsoft\Windows\CurrentVersion\Runonce\Setup\","",,"rundll32.exe shell32.dll,Control_RunDLL main.cpl @0,1"
        [Strings]
        CUR_DIR = "Cursors\Test"
        SCHEME_NAME = "TestPack"
        pointer = "Normal.ani"
        precision = "Precision.ani"
        text = "Text.ani"
        hand = "Handwriting.ani"
        unavailable = "Unavailable.ani"
        vert = "Vertical.ani"
        horz = "Horizontal.ani"
        move = "Move.ani"
        link = "Link.ani"
        busy = "Busy.ani"
    """.trimIndent()

    private val names = listOf("Normal", "Precision", "Text", "Handwriting", "Unavailable", "Vertical", "Horizontal", "Move", "Link", "Busy")

    @Test fun nestedZipWithInfMapsEveryRole() {
        val z = zip(*(names.map { "WindowsMita/Mita/$it.ani" to ani } + ("WindowsMita/Mita/install.inf" to inf.toByteArray()) + ("Readme.txt" to "hi".toByteArray())).toTypedArray())
        val r = readCursorZip(ByteArrayInputStream(z), "fallback")
        assertEquals("TestPack", r.name)
        assertEquals(10, r.files.size)
        assertEquals(listOf("Readme.txt" to PackSkip.NOT_A_CURSOR), r.skipped)
        val roles = guessCursorRoles(r.files.keys, r.infText)
        assertEquals(
            mapOf(
                "Arrow" to "Normal.ani", "IBeam" to "Text.ani", "Hand" to "Link.ani", "CrossHair" to "Precision.ani",
                "ResizeNS" to "Vertical.ani", "ResizeEW" to "Horizontal.ani", "ResizeAll" to "Move.ani", "NotAllowed" to "Unavailable.ani"
            ), roles
        )
    }

    @Test fun traversalAndOddNamesAreSkippedNotWritten() {
        val r = readCursorZip(ByteArrayInputStream(zip("../evil.ani" to ani, "a/../../x.ani" to ani, "ok/.hidden.ani" to ani, "a/b:c.ani" to ani, "good.ani" to ani)), "p")
        assertEquals(listOf("good.ani"), r.files.keys.toList())
        assertEquals(4, r.skipped.count { it.second == PackSkip.UNSAFE_NAME })
        assertNull(safeEntryName("..\\..\\win.ani"))
        assertEquals("Normal.ani", safeEntryName("WindowsMita\\Mita\\Normal.ani"))
    }

    @Test fun zipBombsAreStopped() {
        //one entry that inflates past the per-file limit: skipped, the rest still imports
        val one = readCursorZip(ByteArrayInputStream(zip("big.ani" to ByteArray(30 * 1024 * 1024), "ok.ani" to ani)), "p")
        assertEquals(listOf("big.ani" to PackSkip.TOO_LARGE), one.skipped)
        assertEquals(listOf("ok.ani"), one.files.keys.toList())
        //many entries just under the per-file limit: the whole archive is refused
        val many = zip(*Array(20) { "f$it.ani" to ByteArray(CursorPackLimits.MAX_FILE_BYTES - 10) })
        var refused = false
        try { readCursorZip(ByteArrayInputStream(many), "p") } catch (_: PackTooLargeException) { refused = true }
        assertTrue(refused)
    }

    @Test fun duplicatesAndTooManyFiles() {
        val r = readCursorZip(ByteArrayInputStream(zip("a/Normal.ani" to ani, "b/normal.ANI" to ani)), "p")
        assertEquals(1, r.files.size)
        assertEquals(PackSkip.DUPLICATE, r.skipped.single().second)
        val lots = readCursorZip(ByteArrayInputStream(zip(*Array(CursorPackLimits.MAX_FILES + 3) { "c$it.cur" to ani })), "p")
        assertEquals(CursorPackLimits.MAX_FILES, lots.files.size)
        assertEquals(3, lots.skipped.count { it.second == PackSkip.TOO_MANY })
    }

    @Test fun multiFileImportAndNameOnlyGuess() {
        val r = readCursorFiles(listOf("arrow.cur", "text.ani", "link.png", "Handwriting.ani").map { n -> n to { ByteArrayInputStream(ani) } }, "My files")
        assertEquals("My files", r.name)
        assertEquals(mapOf("Arrow" to "arrow.cur", "IBeam" to "text.ani", "Hand" to "link.png"), guessCursorRoles(r.files.keys, null))
    }

    @Test fun libraryKeepsPacksAndIgnoresBadManifestLines() {
        val root = Files.createTempDirectory("packs").toFile()
        val lib = CursorPackLibrary(root)
        val p = lib.add("Mita!", mapOf("Normal.ani" to ani, "Text.ani" to ani), mapOf("Arrow" to "Normal.ani", "IBeam" to "Missing.ani"))
        assertEquals(mapOf("Arrow" to "Normal.ani"), p.roles)
        File(p.dir, CursorPack.MANIFEST).appendText("role.Arrow=../../etc/passwd\nrole.Bogus=Text.ani\n")
        val again = lib.list().single()
        assertEquals("Mita!", again.name)
        assertEquals(mapOf("Arrow" to "Normal.ani"), again.roles)
        assertEquals(listOf("Normal.ani", "Text.ani"), again.files)
        val second = lib.add("Mita!", mapOf("Normal.ani" to ani), emptyMap())
        assertFalse(second.dir == p.dir) //never overwrites another pack
        lib.delete(second)
        assertEquals(1, lib.list().size)
    }
}
