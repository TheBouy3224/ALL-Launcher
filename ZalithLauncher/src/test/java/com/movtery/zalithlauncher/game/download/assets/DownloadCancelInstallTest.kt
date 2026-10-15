package com.movtery.zalithlauncher.game.download.assets

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** 取消发生在“已装入、回调未完成”之间，以及没有 sha1 时的压缩包完整性检查 */
class DownloadCancelInstallTest {
    private lateinit var root: File

    @Before
    fun setUp() {
        root = Files.createTempDirectory("all-download-cancel-test").toFile()
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun stagedFile(text: String) = File(root, "staged/candidate.jar").apply { parentFile.mkdirs(); writeText(text) }

    @Test
    fun cancellationAfterTheFileIsPlacedRestoresTheUsersOldMod() = runBlocking {
        val mods = File(root, "mods").apply { mkdirs() }
        val target = File(mods, "candidate.jar").apply { writeText("USER OLD BYTES") }
        val installs = mutableListOf<StagedInstall>()

        try {
            installStagedToTargets(stagedFile("NEW BYTES"), listOf(mods to target), installs) { _, _, _ ->
                throw CancellationException("user cancelled while installing")
            }
            fail("the cancellation must propagate")
        } catch (_: CancellationException) {
        }

        assertEquals("USER OLD BYTES", target.readText())
        assertEquals(listOf("candidate.jar"), mods.list()!!.toList())
        assertTrue("records must be cleared so a second rollback cannot touch anything", installs.isEmpty())
    }

    @Test
    fun cancellationOnTheSecondVersionRollsBackTheFirstToo() = runBlocking {
        val folderA = File(root, "a").apply { mkdirs() }
        val folderB = File(root, "b").apply { mkdirs() }
        val targetA = File(folderA, "candidate.jar").apply { writeText("A OLD") }
        val targetB = File(folderB, "candidate.jar").apply { writeText("B OLD") }
        var copied = 0

        try {
            installStagedToTargets(stagedFile("NEW"), listOf(folderA to targetA, folderB to targetB), mutableListOf()) { _, _, _ ->
                if (++copied == 2) throw CancellationException("cancelled")
            }
            fail("the cancellation must propagate")
        } catch (_: CancellationException) {
        }

        assertEquals("A OLD", targetA.readText())
        assertEquals("B OLD", targetB.readText())
        assertEquals(listOf("candidate.jar"), folderA.list()!!.toList())
        assertEquals(listOf("candidate.jar"), folderB.list()!!.toList())
    }

    /**
     * 复制 → 校验 → 改名 的任意时刻取消：旧文件要么原样在原位，要么已被完整的新文件替换且旧文件保留为 .bak；
     * 永远不会留下半个文件或临时文件。
     */
    @Test
    fun cancelAtAnyMomentDuringInstallNeverLeavesAPartialOrDamagesTheOldFile() = runBlocking {
        val staged = File(root, "staged/big.jar").apply {
            parentFile.mkdirs()
            writeBytes(ByteArray(24_000_000).also { java.util.Random(5).nextBytes(it) })
        }
        for (delayMs in listOf(0L, 1, 2, 4, 8, 15, 30, 60, 120)) {
            val mods = File(root, "mods-$delayMs").apply { mkdirs() }
            val target = File(mods, "big.jar").apply { writeText("OLD") }
            val job = launch(kotlinx.coroutines.Dispatchers.Default) { installStagedFile(staged, target) }
            kotlinx.coroutines.delay(delayMs)
            job.cancelAndJoin()

            val names = mods.list()!!.toSet()
            assertTrue("no temp files after cancel at ${delayMs}ms, got $names", names.all { it == "big.jar" || it == "big.jar.bak" })
            if (target.readBytes().contentEquals(staged.readBytes())) {
                assertEquals("OLD", File(mods, "big.jar.bak").readText())
            } else {
                assertEquals("the old file must be untouched (cancel at ${delayMs}ms)", "OLD", target.readText())
                assertFalse(File(mods, "big.jar.bak").exists())
            }
        }
    }

    private fun zipWithEntry(file: File) {
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("fabric.mod.json"))
            zip.write("{}".toByteArray())
            zip.closeEntry()
        }
    }

    @Test
    fun validJarWithoutSha1IsAccepted() {
        val jar = File(root, "ok.jar").also(::zipWithEntry)
        assertNull(unreadableArchiveReason(jar, "ok.jar", hasSha1 = false))
    }

    @Test
    fun htmlErrorPageSavedAsJarWithoutSha1IsRejected() {
        val jar = File(root, "bad.jar").apply { writeText("<html>503 Service Unavailable</html>") }
        assertNotNull(unreadableArchiveReason(jar, "bad.jar", hasSha1 = false))
    }

    @Test
    fun truncatedJarWithoutSha1IsRejected() {
        val whole = File(root, "whole.jar").also(::zipWithEntry)
        val cut = File(root, "cut.jar").apply { writeBytes(whole.readBytes().copyOf((whole.length() / 2).toInt())) }
        assertNotNull(unreadableArchiveReason(cut, "cut.jar", hasSha1 = false))
    }

    @Test
    fun emptyArchiveWithoutSha1IsRejected() {
        val empty = File(root, "empty.jar")
        ZipOutputStream(empty.outputStream()).use { }
        assertNotNull(unreadableArchiveReason(empty, "empty.jar", hasSha1 = false))
    }

    @Test
    fun withSha1TheDownloaderAlreadyVerifiedSoNoSecondGuess() {
        val junk = File(root, "junk.jar").apply { writeText("not a zip") }
        assertNull(unreadableArchiveReason(junk, "junk.jar", hasSha1 = true))
    }

    @Test
    fun nonArchiveFilesAreNotChecked() {
        val config = File(root, "options.txt").apply { writeText("a=b") }
        assertNull(unreadableArchiveReason(config, "options.txt", hasSha1 = false))
        assertFalse(config.length() == 0L)
    }
}
