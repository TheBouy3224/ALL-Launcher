package com.movtery.zalithlauncher.game.version.mod

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.random.Random

/**
 * 修复后复检失败时的自动回滚，用真实文件 + 真实 [ModRepairSnapshots] 验证（只有「复检」是注入的）：
 * 创建快照 → 按 Mod Doctor 的方式替换文件（preserveSafely + 新文件）→ finalize(APPLIED) → verifyRepairedSet。
 * 回滚、隔离区、SHA-1 都是真实代码，不 mock。
 */
class VerifyRepairedSetTest {
    private lateinit var versionPath: File
    private lateinit var modsDir: File
    private lateinit var snapshot: RepairSnapshot
    private lateinit var originalHashes: Map<String, String>

    private fun sha1(f: File): String = MessageDigest.getInstance("SHA-1").digest(f.readBytes()).joinToString("") { "%02x".format(it) }

    private fun hashes(): Map<String, String> = modsDir.listFiles().orEmpty().filter { it.isFile }.associate { it.name to sha1(it) }

    private val targetProblem = ModDoctorProblem(
        type = ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR, fileName = "orig-a.jar, keep.jar", modId = "a", reason = "r",
        breaksTargetModId = "keep",
        ruleFallbackFix = ModDoctorSetFix(emptyList(), emptyMap()),
    )
    private val before = ModDoctorReport(listOf(targetProblem))
    private fun fixResult() = ModDoctorFixResult(report = before, installedFileNames = listOf("new-a.jar"))

    @Before
    fun setUp() {
        versionPath = Files.createTempDirectory("verify-repair").toFile()
        modsDir = File(versionPath, "mods").also { it.mkdirs() }
        File(modsDir, "orig-a.jar").writeBytes(Random(1).nextBytes(4096))
        File(modsDir, "keep.jar").writeBytes(Random(2).nextBytes(4096))
        originalHashes = hashes()

        snapshot = (ModRepairSnapshots.create(versionPath, modsDir) as SnapshotCreateResult.Created).snapshot
        //与 repairKnownSet/apply 相同的落盘方式：原文件可逆保留为 .disabled，再放入新文件
        assertNotNull(preserveSafely(File(modsDir, "orig-a.jar")))
        File(modsDir, "new-a.jar").writeBytes(Random(3).nextBytes(4096))
        snapshot = ModRepairSnapshots.finalize(versionPath, snapshot, modsDir, succeeded = true)
        assertEquals(SnapshotStatus.APPLIED, snapshot.status)
    }

    @After
    fun tearDown() {
        versionPath.deleteRecursively()
    }

    private fun restoredExactly() = hashes() == originalHashes

    private suspend fun verify(rescan: suspend () -> ModDoctorReport) =
        verifyRepairedSet(before, fixResult(), snapshot, versionPath, modsDir, rescan)

    @Test
    fun aNewProblemFoundByTheRescanRollsBackForRealAndReportsFailure() = runBlocking {
        val result = verify { ModDoctorReport(listOf(ModDoctorProblem(ModDoctorProblemType.INCOMPATIBLE_DEPENDENCY_VERSION, "x.jar", "x", "new", dependencyModId = "a"))) }

        assertNotNull("a rolled-back repair is a failure, never a success", result.failureReason)
        assertTrue(result.failureReason!!, result.failureReason!!.contains("rolled back"))
        assertTrue("nothing is reported as installed", result.installedFileNames.isEmpty())
        assertTrue("the original files are back, byte for byte (SHA-1)", restoredExactly())
        assertFalse("the repair's new file was moved out of mods", File(modsDir, "new-a.jar").exists())
        assertFalse("the .disabled original is gone from mods because the original is back", File(modsDir, "orig-a.jar.disabled").exists())
        assertEquals(SnapshotStatus.ROLLED_BACK, ModRepairSnapshots.all(versionPath).first().status)
        assertTrue("the new file is kept in the quarantine, not deleted", File(versionPath, "ALLLauncher/repair-backups/${snapshot.id}/quarantine/new-a.jar").exists() ||
            ModRepairSnapshots.all(versionPath).first().quarantined.contains("new-a.jar"))
    }

    @Test
    fun theTargetProblemStillReportedAlsoRollsBack() = runBlocking {
        val result = verify { before }

        assertNotNull(result.failureReason)
        assertTrue(restoredExactly())
    }

    @Test
    fun aCleanRescanKeepsTheRepairAndDoesNotRollBack() = runBlocking {
        val repairedState = hashes()

        val result = verify { ModDoctorReport(emptyList()) }

        assertEquals(null, result.failureReason)
        assertEquals(listOf("new-a.jar"), result.installedFileNames)
        assertEquals("the repaired files stay exactly as the repair left them", repairedState, hashes())
        assertEquals(SnapshotStatus.APPLIED, ModRepairSnapshots.all(versionPath).first().status)
    }

    @Test
    fun anUnrelatedProblemThatWasAlreadyThereDoesNotCauseARollback() = runBlocking {
        val old = ModDoctorProblem(ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY, "l.jar", "litematica", "old", requiredDependencyId = "malilib")
        val beforeWithOld = ModDoctorReport(listOf(targetProblem, old))

        val result = verifyRepairedSet(beforeWithOld, fixResult(), snapshot, versionPath, modsDir) { ModDoctorReport(listOf(old)) }

        assertEquals(null, result.failureReason)
        assertTrue(File(modsDir, "new-a.jar").exists())
    }

    @Test
    fun aRescanThatThrowsIsUnverifiableSoItRollsBackAndReportsFailure() = runBlocking {
        val result = verify { throw IllegalStateException("provider exploded") }

        assertNotNull(result.failureReason)
        assertTrue(result.failureReason!!, result.failureReason!!.contains("rolled back"))
        assertTrue(restoredExactly())
        assertEquals(SnapshotStatus.ROLLED_BACK, ModRepairSnapshots.all(versionPath).first().status)
    }

    @Test
    fun cancellationIsNotSwallowedAndTouchesNothing() {
        val repairedState = hashes()

        val thrown = runCatching { runBlocking { verify { throw CancellationException("user left") } } }.exceptionOrNull()

        assertTrue(thrown is CancellationException)
        assertEquals("a cancelled check leaves the repaired files alone (the snapshot stays APPLIED for a manual rollback)", repairedState, hashes())
        assertEquals(SnapshotStatus.APPLIED, ModRepairSnapshots.all(versionPath).first().status)
    }

    @Test
    fun whenTheRollbackItselfCannotCompleteItIsNeverReportedAsRestored() = runBlocking {
        //修复之后用户（或别的程序）又放回了同名但内容不同的原文件：回滚必须把它当成冲突，不覆盖、不删除
        val userFile = File(modsDir, "orig-a.jar").apply { writeBytes(Random(99).nextBytes(100)) }
        val userHash = sha1(userFile)

        val result = verify { before }

        assertNotNull(result.failureReason)
        assertFalse("must not claim the original files are back", result.failureReason!!.contains("original files were restored"))
        assertTrue(result.failureReason!!, result.failureReason!!.contains("could not be restored"))
        assertEquals("the user's file is never overwritten or deleted", userHash, sha1(File(modsDir, "orig-a.jar")))
        assertEquals(SnapshotStatus.ROLLED_BACK_PARTIAL, ModRepairSnapshots.all(versionPath).first().status)
    }
}
