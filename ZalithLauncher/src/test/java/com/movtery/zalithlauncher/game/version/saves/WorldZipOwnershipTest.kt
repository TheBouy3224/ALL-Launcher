package com.movtery.zalithlauncher.game.version.saves

import com.movtery.zalithlauncher.game.download.assets.installStagedToTargets
import com.movtery.zalithlauncher.game.download.assets.rollbackStagedInstalls
import com.movtery.zalithlauncher.game.download.assets.StagedInstall
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

/**
 * 世界 ZIP 的归属：本次任务写入的 zip 才能在解压后删除；任务开始前已存在的同名 zip（即使 SHA-1 相同）必须原样保留。
 * 同时覆盖多实例：失败时只撤销本次创建的世界，已存在的世界不动。
 */
class WorldZipOwnershipTest {
    private lateinit var root: File

    @Before
    fun setUp() {
        root = Files.createTempDirectory("all-world-zip-ownership").toFile()
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun validWorldZip(file: File, name: String = "Mine", levelText: String = "LEVEL"): File {
        file.parentFile?.mkdirs()
        ZipOutputStream(file.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("$name/level.dat")); out.write(levelText.toByteArray()); out.closeEntry()
            out.putNextEntry(ZipEntry("$name/region/")); out.closeEntry()
            out.putNextEntry(ZipEntry("$name/region/r.0.0.mca")); out.write("REGION".toByteArray()); out.closeEntry()
        }
        return file
    }

    /** 下载暂存位上的文件（候选）：默认是合法世界 zip */
    private fun staged(bytes: ByteArray? = null): File {
        val file = File(root, "cache/Mine.zip")
        file.parentFile.mkdirs()
        if (bytes == null) validWorldZip(file) else file.writeBytes(bytes)
        return file
    }

    private fun savesDir(instance: String) = File(root, "$instance/saves").apply { mkdirs() }

    /** 与 downloadSingleForVersions 的非暂存路径相同：安装 → 解压回调（世界批次）；失败/取消回滚本次写入 */
    private fun installWorlds(
        stagedZip: File,
        targets: List<File>,
        batch: SaveWorldBatch = SaveWorldBatch(),
    ): List<StagedInstall> = runBlocking {
        val installs = mutableListOf<StagedInstall>()
        installStagedToTargets(
            stagedZip,
            targets.map { it to File(it, "Mine.zip") },
            installs,
        ) { zip, folder, install -> batch.unpack(zip, folder, install) }
        installs
    }

    @Test
    fun identicalZipIsKeptWhenWorldUnpacksSuccessfully() {
        val saves = savesDir("A")
        val bytes = validWorldZip(File(root, "src.zip")).readBytes()
        File(saves, "Mine.zip").writeBytes(bytes)

        val installs = installWorlds(staged(bytes), listOf(saves))

        assertEquals("LEVEL", File(saves, "Mine/level.dat").readText())
        assertTrue("the user's identical zip must not be deleted", File(saves, "Mine.zip").isFile)
        assertTrue(bytes.contentEquals(File(saves, "Mine.zip").readBytes()))
        assertFalse(installs.single().written)
    }

    @Test
    fun differentUserZipIsRestoredAfterSuccessfulUnpack() {
        val saves = savesDir("A")
        File(saves, "Mine.zip").writeText("USER ZIP")

        val installs = installWorlds(staged(), listOf(saves))

        assertEquals("LEVEL", File(saves, "Mine/level.dat").readText())
        assertEquals("USER ZIP", File(saves, "Mine.zip").readText())
        assertFalse("kept file must be restored, not left as .bak", File(saves, "Mine.zip.bak").exists())
        assertFalse(installs.single().written)
    }

    @Test
    fun differentUserZipIsRestoredWhenUnpackFails() {
        val saves = savesDir("A")
        File(saves, "Mine.zip").writeText("USER ZIP")

        try {
            installWorlds(staged("not a zip".toByteArray()), listOf(saves))
            fail("a broken zip must fail to unpack")
        } catch (_: Exception) {
        }

        assertEquals("USER ZIP", File(saves, "Mine.zip").readText())
        assertFalse(File(saves, "Mine").exists())
        assertFalse(File(saves, "Mine.zip.bak").exists())
    }

    @Test
    fun identicalUserZipIsKeptWhenUnpackFails() {
        val saves = savesDir("A")
        val broken = "not a zip".toByteArray()
        File(saves, "Mine.zip").writeBytes(broken)

        try {
            installWorlds(staged(broken), listOf(saves))
            fail("a broken zip must fail to unpack")
        } catch (_: Exception) {
        }

        assertTrue("the same-SHA-1 zip existed before the task, so it must stay", File(saves, "Mine.zip").isFile)
        assertTrue(broken.contentEquals(File(saves, "Mine.zip").readBytes()))
    }

    @Test
    fun cancelAfterInstallRestoresTheUserZip() {
        val saves = savesDir("A")
        File(saves, "Mine.zip").writeText("USER ZIP")
        val installs = mutableListOf<StagedInstall>()
        runBlocking {
            //安装成功，但在解压前就被取消：取消路径只调用 rollbackStagedInstalls
            installStagedToTargets(staged(), listOf(saves to File(saves, "Mine.zip")), installs) { _, _, _ -> }
        }
        rollbackStagedInstalls(installs)

        assertEquals("USER ZIP", File(saves, "Mine.zip").readText())
        assertFalse(File(saves, "Mine.zip.bak").exists())
    }

    @Test
    fun successfulUnpackToSeveralInstancesKeepsEachWorld() {
        val a = savesDir("A")
        val c = savesDir("C")
        File(c, "Mine.zip").writeText("USER ZIP C")

        installWorlds(staged(), listOf(a, c))

        assertEquals("LEVEL", File(a, "Mine/level.dat").readText())
        assertEquals("LEVEL", File(c, "Mine/level.dat").readText())
        assertEquals("USER ZIP C", File(c, "Mine.zip").readText())
        assertFalse(File(a, "Mine.zip").exists())
    }

    @Test
    fun laterInstanceFailureUndoesOwnWorldsAndKeepsExistingWorld() {
        val a = savesDir("A")
        val b = savesDir("B")
        File(b, "Mine/level.dat").apply { parentFile.mkdirs(); writeText("USER WORLD") }
        File(b, "Mine.zip").writeText("USER ZIP B")

        try {
            installWorlds(staged(), listOf(a, b))
            fail("instance B already has a world with this name")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("already exists"))
        }

        assertFalse("world created by this task in A must be undone", File(a, "Mine").exists())
        assertFalse("A never had a zip, and the new one was consumed or rolled back", File(a, "Mine.zip").exists())
        assertEquals("USER WORLD", File(b, "Mine/level.dat").readText())
        assertEquals("USER ZIP B", File(b, "Mine.zip").readText())
        assertFalse(File(b, "Mine.zip.bak").exists())
    }

    @Test
    fun batchUndoOnlyRemovesWorldsItClaimed() {
        val a = savesDir("A")
        val batch = SaveWorldBatch()
        val installs = mutableListOf<StagedInstall>()
        runBlocking {
            installStagedToTargets(staged(), listOf(a to File(a, "Mine.zip")), installs) { zip, folder, install ->
                batch.unpack(zip, folder, install)
            }
        }
        assertTrue(File(a, "Mine").isDirectory)
        assertTrue(batch.undo().isEmpty())
        assertFalse(File(a, "Mine").exists())
    }
}
