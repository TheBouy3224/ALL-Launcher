package com.movtery.zalithlauncher.game.version.mod

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files

/**
 * 受控故障注入：快照拒绝（磁盘不足 / 文件被锁 / 写满）、多文件回滚、修复中断、回滚失败。
 * 这些场景在真机上要弄坏文件系统才能复现，所以用注入点验证；真机只验证安全可做的部分。
 */
class ModRepairSnapshotSafetyTest {
    private lateinit var version: File
    private lateinit var mods: File
    private var clock = 1_000L

    @Before
    fun setUp() {
        version = Files.createTempDirectory("all-snapshot-safety").toFile()
        mods = File(version, "mods").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        version.deleteRecursively()
    }

    private fun put(name: String, content: String) = File(mods, name).also { it.writeText(content) }

    /** 名字 -> sha1：用于逐文件比较 */
    private fun state(): Map<String, String> = ModRepairSnapshots.listing(mods).associate { it.name to it.sha1 }

    private fun created(
        freeSpace: (File) -> Long = { Long.MAX_VALUE },
        copy: (File, File) -> Unit = { a, b -> a.copyTo(b, overwrite = false) },
    ): SnapshotCreateResult = ModRepairSnapshots.create(version, mods, now = clock++, freeSpace = freeSpace, copy = copy)

    private fun threeMods() {
        put("alpha.jar", "ALPHA"); put("beta.jar", "BETA"); put("gamma.jar", "GAMMA")
    }

    private fun noSnapshotDirs() = ModRepairSnapshots.root(version).listFiles().orEmpty().filter { it.isDirectory }

    // ---- 1. 快照拒绝：不改原文件、不留半个备份 ----

    @Test
    fun notEnoughFreeSpaceRefusesBeforeWritingAnything() {
        threeMods()
        val before = state()

        val result = created(freeSpace = { 10L })

        assertTrue("got $result", result is SnapshotCreateResult.Refused)
        assertTrue((result as SnapshotCreateResult.Refused).reason.contains("free space"))
        assertEquals(before, state())
        assertTrue("no backup directory may be left behind", noSnapshotDirs().isEmpty())
    }

    @Test
    fun exactlyEnoughSpaceForTheBackupPlusMarginIsAccepted() {
        threeMods()
        val total = ModRepairSnapshots.listing(mods).sumOf { it.size }
        val result = created(freeSpace = { total + ModRepairSnapshots.FREE_SPACE_MARGIN_BYTES })
        assertTrue("got $result", result is SnapshotCreateResult.Created)
    }

    @Test
    fun aLockedFileMakesTheSnapshotRefuseAndLeavesNothingBehind() {
        threeMods()
        val before = state()

        val result = created(copy = { from, to ->
            if (from.name == "beta.jar") throw IOException("The process cannot access the file because it is being used by another process")
            from.copyTo(to, overwrite = false)
        })

        assertTrue("got $result", result is SnapshotCreateResult.Refused)
        assertEquals("originals untouched", before, state())
        assertTrue(noSnapshotDirs().isEmpty())
    }

    @Test
    fun diskFullDuringTheBackupRefusesAndCleansUpItsOwnCopies() {
        threeMods()
        val before = state()
        var copies = 0

        val result = created(copy = { from, to ->
            if (++copies == 3) throw IOException("No space left on device")
            from.copyTo(to, overwrite = false)
        })

        assertTrue("got $result", result is SnapshotCreateResult.Refused)
        assertEquals(before, state())
        assertTrue(noSnapshotDirs().isEmpty())
    }

    @Test
    fun aBackupCopyThatDoesNotMatchItsHashIsRefused() {
        threeMods()
        val result = created(copy = { from, to ->
            from.copyTo(to, overwrite = false)
            if (from.name == "gamma.jar") to.writeText("CORRUPT")
        })
        assertTrue("got $result", result is SnapshotCreateResult.Refused)
        assertTrue(noSnapshotDirs().isEmpty())
    }

    // ---- 2. 多文件回滚：每个文件的哈希都回到修复前 ----

    /** 一次多文件修复：禁用 alpha，把 beta 换成新版本（旧 beta 保留为 .disabled），再新增两个依赖 */
    private val repairSteps: List<() -> Unit> = listOf(
        { File(mods, "alpha.jar").renameTo(File(mods, "alpha.jar.disabled")) },
        { File(mods, "beta.jar").renameTo(File(mods, "beta.jar.disabled")) },
        { put("beta-2.jar", "BETA TWO") },
        { put("dep-one.jar", "DEP ONE") },
        { put("dep-two.jar", "DEP TWO") },
    )

    @Test
    fun aMultiFileRepairRollsBackEveryFileToItsOriginalHash() {
        threeMods()
        val before = state()
        val snapshot = (created() as SnapshotCreateResult.Created).snapshot
        repairSteps.forEach { it() }
        val applied = ModRepairSnapshots.finalize(version, snapshot, mods, succeeded = true)
        assertEquals(SnapshotStatus.APPLIED, applied.status)

        val result = ModRepairSnapshots.rollback(version, applied, mods)

        assertTrue(result.complete)
        assertEquals(SnapshotStatus.ROLLED_BACK, result.snapshot.status)
        assertEquals("every file must be back with its original SHA-1", before, state())
        assertFalse("no temporary restore folder may be left behind", File(ModRepairSnapshots.root(version), "${snapshot.id}/restore-tmp").exists())
        //被换下来的与新增的文件都在 quarantine，没有一个被删除
        val quarantine = File(ModRepairSnapshots.root(version), "${snapshot.id}/quarantine")
        assertEquals(
            setOf("alpha.jar.disabled", "beta.jar.disabled", "beta-2.jar", "dep-one.jar", "dep-two.jar"),
            quarantine.list().orEmpty().toSet(),
        )
    }

    // ---- 3. 修复中断：任意一步被杀都能恢复，不说谎 ----

    @Test
    fun aRepairInterruptedAtAnyStepRollsBackExactly() {
        for (completedSteps in 0..repairSteps.size) {
            tearDown(); setUp()
            threeMods()
            val before = state()
            val snapshot = (created() as SnapshotCreateResult.Created).snapshot
            repairSteps.take(completedSteps).forEach { it() }
            //进程在这里被杀：manifest 仍是 PENDING，没有人调用 finalize

            val recovered = ModRepairSnapshots.latestRollbackableLocked(version, mods)
            assertNotNull("step $completedSteps: an interrupted repair must be offered for rollback", recovered)
            assertEquals(SnapshotStatus.INTERRUPTED, recovered!!.status)
            val result = ModRepairSnapshots.rollback(version, recovered, mods)

            assertTrue("step $completedSteps: $result", result.complete)
            assertEquals("step $completedSteps: original state", before, state())
            assertEquals(SnapshotStatus.ROLLED_BACK, result.snapshot.status)
        }
    }

    @Test
    fun aFailedRepairIsNeverRecordedAsApplied() {
        threeMods()
        val snapshot = (created() as SnapshotCreateResult.Created).snapshot
        repairSteps.take(3).forEach { it() }

        val done = ModRepairSnapshots.finalize(version, snapshot, mods, succeeded = false)

        assertEquals(SnapshotStatus.PARTIAL, done.status)
    }

    // ---- 4. 回滚失败：不说 ROLLED_BACK，不留半截文件，可以再试 ----

    @Test
    fun aFailedRestoreIsReportedAsPartialAndNeverLeavesATruncatedOriginal() {
        threeMods()
        val before = state()
        val snapshot = (created() as SnapshotCreateResult.Created).snapshot
        repairSteps.forEach { it() }
        val applied = ModRepairSnapshots.finalize(version, snapshot, mods, succeeded = true)

        //恢复 beta 时磁盘写满：复制写出一半后抛异常
        val result = ModRepairSnapshots.rollback(version, applied, mods, copy = { from, to ->
            if (from.name == "beta.jar") {
                to.writeText("BE")
                throw IOException("No space left on device")
            }
            from.copyTo(to, overwrite = false)
        })

        assertFalse(result.complete)
        assertEquals(SnapshotStatus.ROLLED_BACK_PARTIAL, result.snapshot.status)
        assertTrue(result.conflicts.contains("beta.jar"))
        //mods 里不能出现一个名为 beta.jar 的半截文件
        assertFalse(File(mods, "beta.jar").exists())
        //已经成功恢复的文件哈希正确
        assertEquals(before["alpha.jar"], state()["alpha.jar"])
        assertEquals(before["gamma.jar"], state()["gamma.jar"])
        //备份副本完好，还能再试
        val retryable = ModRepairSnapshots.latestRollbackableLocked(version, mods)
        assertNotNull(retryable)

        val retry = ModRepairSnapshots.rollback(version, retryable!!, mods)

        assertTrue("retry: $retry", retry.complete)
        assertEquals(SnapshotStatus.ROLLED_BACK, retry.snapshot.status)
        assertEquals(before, state())
    }

    @Test
    fun aRestoreWhoseCopyIsCorruptIsNeverAcceptedAsRestored() {
        threeMods()
        val snapshot = (created() as SnapshotCreateResult.Created).snapshot
        File(mods, "alpha.jar").renameTo(File(mods, "alpha.jar.disabled"))
        val applied = ModRepairSnapshots.finalize(version, snapshot, mods, succeeded = true)

        val result = ModRepairSnapshots.rollback(version, applied, mods, copy = { from, to ->
            from.copyTo(to, overwrite = false)
            to.writeText("TAMPERED")
        })

        assertFalse(result.complete)
        assertEquals(SnapshotStatus.ROLLED_BACK_PARTIAL, result.snapshot.status)
        assertFalse("a file with the wrong content must not sit under the original name", File(mods, "alpha.jar").exists())
    }

    @Test
    fun aRollbackThatWasKilledHalfWayCanBeRunAgain() {
        threeMods()
        val before = state()
        val snapshot = (created() as SnapshotCreateResult.Created).snapshot
        repairSteps.forEach { it() }
        val applied = ModRepairSnapshots.finalize(version, snapshot, mods, succeeded = true)
        //第一次回滚已经把两个新增文件移进 quarantine 就被杀了（manifest 仍是 APPLIED）
        val quarantine = File(ModRepairSnapshots.root(version), "${snapshot.id}/quarantine").apply { mkdirs() }
        Files.move(File(mods, "dep-one.jar").toPath(), File(quarantine, "dep-one.jar").toPath())
        Files.move(File(mods, "dep-two.jar").toPath(), File(quarantine, "dep-two.jar").toPath())

        val result = ModRepairSnapshots.rollback(version, applied, mods)

        assertTrue("$result", result.complete)
        assertEquals(before, state())
    }

    @Test
    fun aFileTheUserAddedAfterTheRepairIsNeverTouchedAndTheRollbackSaysSo() {
        threeMods()
        val snapshot = (created() as SnapshotCreateResult.Created).snapshot
        repairSteps.forEach { it() }
        val applied = ModRepairSnapshots.finalize(version, snapshot, mods, succeeded = true)
        put("mine.jar", "USER FILE")

        val result = ModRepairSnapshots.rollback(version, applied, mods)

        assertEquals("USER FILE", File(mods, "mine.jar").readText())
        assertEquals(SnapshotStatus.ROLLED_BACK_PARTIAL, result.snapshot.status)
        assertTrue(result.conflicts.contains("mine.jar"))
    }
}
