package com.movtery.zalithlauncher.game.download.assets

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** 下载安装的文件安全：暂存 → 校验 → 保留旧文件 → 替换；失败或取消只还原本次写入的文件 */
class DownloadSingleInstallTest {
    private lateinit var root: File
    private lateinit var staged: File

    @Before
    fun setUp() {
        root = Files.createTempDirectory("all-download-install-test").toFile()
        staged = File(root, "staged/candidate.jar").apply { parentFile.mkdirs(); writeText("NEW BYTES") }
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun folder(name: String) = File(root, name).apply { mkdirs() }

    @Test
    fun installsIntoEmptyFolderAndVerifiesContent() = runBlocking {
        val target = File(folder("mods"), "candidate.jar")
        val result = installStagedFile(staged, target)
        assertTrue(result.written)
        assertNull(result.backup)
        assertEquals("NEW BYTES", target.readText())
        assertEquals(listOf("candidate.jar"), target.parentFile.list()!!.toList())
    }

    @Test
    fun identicalExistingFileIsNotRewritten() = runBlocking {
        val target = File(folder("mods"), "candidate.jar").apply { writeText("NEW BYTES") }
        val result = installStagedFile(staged, target)
        assertFalse(result.written)
        assertNull(result.backup)
        assertEquals(listOf("candidate.jar"), target.parentFile.list()!!.toList())
    }

    @Test
    fun differentExistingFileIsKeptAsBackupNotDeleted() = runBlocking {
        val target = File(folder("mods"), "candidate.jar").apply { writeText("USER OLD BYTES") }
        val result = installStagedFile(staged, target)
        assertTrue(result.written)
        assertEquals("NEW BYTES", target.readText())
        assertEquals("USER OLD BYTES", result.backup!!.readText())
        assertEquals("candidate.jar.bak", result.backup!!.name)
    }

    @Test
    fun rollbackRestoresOldFileAndRemovesNewOne() = runBlocking {
        val target = File(folder("mods"), "candidate.jar").apply { writeText("USER OLD BYTES") }
        val install = installStagedFile(staged, target)
        rollbackStagedInstalls(listOf(install))
        assertEquals("USER OLD BYTES", target.readText())
        assertEquals(listOf("candidate.jar"), target.parentFile.list()!!.toList())
    }

    @Test
    fun rollbackLeavesFilesAloneWhenNothingWasWritten() = runBlocking {
        val target = File(folder("mods"), "candidate.jar").apply { writeText("NEW BYTES") }
        val unchanged = installStagedFile(staged, target)
        rollbackStagedInstalls(listOf(unchanged))
        assertEquals("NEW BYTES", target.readText())
    }

    @Test
    fun missingStagedFileNeverTouchesExistingTarget() = runBlocking {
        val target = File(folder("mods"), "candidate.jar").apply { writeText("USER OLD BYTES") }
        try {
            installStagedFile(File(root, "missing.jar"), target)
            fail("a missing staged file must not install")
        } catch (_: Exception) {
        }
        assertEquals("USER OLD BYTES", target.readText())
        assertEquals(listOf("candidate.jar"), target.parentFile.list()!!.toList())
    }

    @Test
    fun failedCopyLeavesTheExistingFileHolderIntact() = runBlocking {
        //目标父目录其实是一个文件：复制临时文件必然失败，已有的目标不得被删除
        val blocker = File(root, "blocker").apply { writeText("I AM A FILE") }
        val target = File(blocker, "candidate.jar")
        try {
            installStagedFile(staged, target)
            fail("writing under a file must fail")
        } catch (_: Exception) {
        }
        assertEquals("I AM A FILE", blocker.readText())
    }

    @Test
    fun multiVersionFailureRollsBackTheFirstVersion() = runBlocking {
        val first = File(folder("instanceA/mods"), "candidate.jar").apply { writeText("A OLD") }
        val blocker = File(root, "blocker").apply { writeText("x") }
        val second = File(blocker, "candidate.jar")

        val installs = mutableListOf<StagedInstall>()
        try {
            installs += installStagedFile(staged, first)
            installs += installStagedFile(staged, second)
            fail("second version must fail")
        } catch (_: Exception) {
            rollbackStagedInstalls(installs)
        }
        assertEquals("A OLD", first.readText())
        assertEquals(listOf("candidate.jar"), first.parentFile.list()!!.toList())
    }

    @Test
    fun backupNamesDoNotCollide() = runBlocking {
        val target = File(folder("mods"), "candidate.jar").apply { writeText("ONE") }
        File(target.parentFile, "candidate.jar.bak").writeText("EARLIER BACKUP")
        val result = installStagedFile(staged, target)
        assertEquals("candidate.jar.bak1", result.backup!!.name)
        assertEquals("EARLIER BACKUP", File(target.parentFile, "candidate.jar.bak").readText())
        assertEquals("ONE", result.backup!!.readText())
    }
}
