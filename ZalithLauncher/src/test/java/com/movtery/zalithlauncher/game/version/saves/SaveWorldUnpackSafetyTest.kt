package com.movtery.zalithlauncher.game.version.saves

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** 世界(存档)解压的安全性：同名世界不覆盖、失败只清理自己创建的目录、成功才删除 zip */
class SaveWorldUnpackSafetyTest {
    private lateinit var root: File
    private lateinit var saves: File

    @Before
    fun setUp() {
        root = Files.createTempDirectory("all-world-unpack-test").toFile()
        saves = File(root, "saves").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    /** 合法世界 zip：`<name>/level.dat` 加上一个子目录，符合 findLevelEntryName 的要求 */
    private fun worldZip(name: String, levelText: String = "LEVEL") = File(root, "$name.zip").also { file ->
        ZipOutputStream(file.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("$name/level.dat")); out.write(levelText.toByteArray()); out.closeEntry()
            out.putNextEntry(ZipEntry("$name/region/")); out.closeEntry()
            out.putNextEntry(ZipEntry("$name/region/r.0.0.mca")); out.write("REGION".toByteArray()); out.closeEntry()
        }
    }

    private fun existingWorld(name: String) = File(saves, name).apply {
        mkdirs()
        File(this, "level.dat").writeText("USER WORLD")
    }

    @Test
    fun successfulUnpackCreatesWorldAndRemovesZip() = runBlocking {
        val zip = worldZip("Fresh")
        unpackSaveZip(zip, saves)
        assertEquals("LEVEL", File(saves, "Fresh/level.dat").readText())
        assertFalse(zip.exists())
    }

    @Test
    fun existingWorldIsRefusedAndNeverTouched() = runBlocking {
        val user = existingWorld("Mine")
        val zip = worldZip("Mine", levelText = "OTHER")
        try {
            unpackSaveZip(zip, saves)
            fail("an existing world must not be merged or overwritten")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("already exists"))
        }
        assertEquals("USER WORLD", File(user, "level.dat").readText())
        assertFalse(File(user, "region").exists())
        assertTrue("refused zip is left for the download layer to roll back", zip.exists())
    }

    @Test
    fun brokenZipLeavesNoWorldFolder() = runBlocking {
        val zip = File(root, "Broken.zip").apply { writeText("not a zip at all") }
        try {
            unpackSaveZip(zip, saves)
            fail("a broken zip must fail")
        } catch (_: Exception) {
        }
        assertFalse("half-created world folder must be removed", File(saves, "Broken").exists())
    }

    @Test
    fun zipWithoutLevelDatLeavesNoWorldFolder() = runBlocking {
        val zip = File(root, "NoLevel.zip")
        ZipOutputStream(zip.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("readme.txt")); out.write("x".toByteArray()); out.closeEntry()
        }
        try {
            unpackSaveZip(zip, saves)
            fail("a zip without level.dat must fail")
        } catch (_: Exception) {
        }
        assertFalse(File(saves, "NoLevel").exists())
    }

    @Test
    fun failedUnpackDoesNotDeleteAWorldThatAppearedMeanwhile() = runBlocking {
        //另一个任务在我们占位前就已经创建了同名世界：解压必须拒绝，并且不能删掉它
        val other = existingWorld("Race")
        val zip = worldZip("Race")
        try {
            unpackSaveZip(zip, saves)
            fail("must refuse")
        } catch (_: IOException) {
        }
        assertEquals("USER WORLD", File(other, "level.dat").readText())
    }

    @Test
    fun findExistingWorldTargetReportsOnlyTheFolderWithTheWorld() {
        val other = File(root, "other/saves").apply { mkdirs() }
        existingWorld("Taken")
        assertEquals(saves, findExistingWorldTarget("Taken", listOf(other, saves)))
        assertEquals(null, findExistingWorldTarget("Free", listOf(other, saves)))
    }

    @Test
    fun downloadRollbackRestoresAUserZipWhenUnpackFails() = runBlocking {
        //下载层：目标 zip 已有用户的旧内容 → 新 zip 被写入 → 解压失败 → 回滚必须把旧 zip 还原，不留 .bak
        val userZip = File(saves, "Shared.zip").apply { writeText("USER ZIP") }
        val staged = File(root, "cache/candidate.zip").apply { parentFile.mkdirs(); writeText("not a world") }

        val install = com.movtery.zalithlauncher.game.download.assets.installStagedFile(staged, userZip)
        try {
            unpackSaveZip(userZip, saves)
            fail("broken zip must fail to unpack")
        } catch (_: Exception) {
            com.movtery.zalithlauncher.game.download.assets.rollbackStagedInstalls(listOf(install))
        }
        assertEquals("USER ZIP", userZip.readText())
        assertFalse(File(saves, "Shared.zip.bak").exists())
        assertFalse(File(saves, "Shared").exists())
    }
}
