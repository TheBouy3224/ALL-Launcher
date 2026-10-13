package com.movtery.zalithlauncher.game.version.mod

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ModRepairSnapshotTest {
    private lateinit var version: File
    private lateinit var mods: File

    @Before
    fun setUp() {
        version = Files.createTempDirectory("all-snapshot-test").toFile()
        mods = File(version, "mods").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        version.deleteRecursively()
    }

    private fun put(name: String, content: String) = File(mods, name).also { it.writeText(content) }
    private fun names() = mods.listFiles().orEmpty().map { it.name }.sorted()
    private fun text(name: String) = File(mods, name).readText()

    private fun snapshot(): RepairSnapshot =
        (ModRepairSnapshots.create(version, mods) as SnapshotCreateResult.Created).snapshot

    /** 模拟一次成功的修复：旧 sodium 被禁用（.disabled），新 sodium 被写入 */
    private fun simulateSuccessfulRepair() {
        File(mods, "sodium-old.jar").renameTo(File(mods, "sodium-old.jar.disabled"))
        put("sodium-new.jar", "NEW")
    }

    @Test
    fun createsVerifiedSnapshotWithManifest() {
        put("sodium-old.jar", "OLD"); put("fabric-api.jar", "API")
        val s = snapshot()
        assertEquals(SnapshotStatus.PENDING, s.status)
        assertEquals(listOf("fabric-api.jar", "sodium-old.jar"), s.before.map { it.name })
        val dir = File(ModRepairSnapshots.root(version), s.id)
        assertTrue(File(dir, "manifest.json").isFile)
        assertEquals("OLD", File(dir, "files/sodium-old.jar").readText())
        assertEquals(ModRepairSnapshots.sha1(File(mods, "sodium-old.jar")), s.before.first { it.name == "sodium-old.jar" }.sha1)
    }

    @Test
    fun refusesWhenBackupWouldExceedLimitAndLeavesNothingBehind() {
        put("big.jar", "0123456789")
        val result = ModRepairSnapshots.create(version, mods, maxBytes = 5)
        assertTrue(result is SnapshotCreateResult.Refused)
        assertFalse(ModRepairSnapshots.root(version).exists() && ModRepairSnapshots.root(version).listFiles().orEmpty().isNotEmpty())
        assertEquals(listOf("big.jar"), names()) //mods ไม่ถูกแตะ
    }

    @Test
    fun unchangedRepairIsRecordedAsNoChange() {
        put("a.jar", "A")
        val s = snapshot()
        val done = ModRepairSnapshots.finalize(version, s, mods, succeeded = true)
        assertEquals(SnapshotStatus.NO_CHANGE, done.status)
        assertNull(ModRepairSnapshots.latestRollbackable(version, mods))
    }

    @Test
    fun rollbackRestoresOriginalsAndMovesNewFilesAside_noDeletion() {
        put("sodium-old.jar", "OLD"); put("fabric-api.jar", "API")
        val s = snapshot()
        simulateSuccessfulRepair()
        val applied = ModRepairSnapshots.finalize(version, s, mods, succeeded = true)
        assertEquals(SnapshotStatus.APPLIED, applied.status)

        val result = ModRepairSnapshots.rollback(version, applied, mods)

        assertTrue(result.complete)
        assertEquals(listOf("fabric-api.jar", "sodium-old.jar"), names())
        assertEquals("OLD", text("sodium-old.jar"))
        //新文件与 .disabled 被移到 quarantine，不是删除
        val q = File(ModRepairSnapshots.root(version), "${s.id}/quarantine")
        assertEquals("NEW", File(q, "sodium-new.jar").readText())
        assertTrue(File(q, "sodium-old.jar.disabled").isFile)
        assertEquals(SnapshotStatus.ROLLED_BACK, result.snapshot.status)
    }

    @Test
    fun rollbackSkipsFilesTheUserChangedAfterTheRepair() {
        put("sodium-old.jar", "OLD")
        val s = snapshot()
        simulateSuccessfulRepair()
        val applied = ModRepairSnapshots.finalize(version, s, mods, succeeded = true)
        File(mods, "sodium-new.jar").writeText("USER EDIT") //修复之后用户改了它

        val result = ModRepairSnapshots.rollback(version, applied, mods)

        assertFalse(result.complete)
        assertTrue("sodium-new.jar" in result.conflicts)
        assertEquals("USER EDIT", text("sodium-new.jar")) //没有被覆盖或移走
        assertEquals(SnapshotStatus.ROLLED_BACK_PARTIAL, result.snapshot.status)
    }

    @Test
    fun rollbackNeverOverwritesAFileThatReappearedWithDifferentContent() {
        put("sodium-old.jar", "OLD")
        val s = snapshot()
        simulateSuccessfulRepair()
        val applied = ModRepairSnapshots.finalize(version, s, mods, succeeded = true)
        File(mods, "sodium-old.jar").writeText("SOMETHING ELSE") //用户另外放了同名文件

        val result = ModRepairSnapshots.rollback(version, applied, mods)

        assertTrue("sodium-old.jar" in result.conflicts)
        assertEquals("SOMETHING ELSE", text("sodium-old.jar"))
    }

    @Test
    fun partialFailureIsRecordedAndStillRollbackable() {
        put("sodium-old.jar", "OLD")
        val s = snapshot()
        File(mods, "sodium-old.jar").renameTo(File(mods, "sodium-old.jar.disabled")) //改了一半就失败
        val partial = ModRepairSnapshots.finalize(version, s, mods, succeeded = false)
        assertEquals(SnapshotStatus.PARTIAL, partial.status)

        val rollbackable = ModRepairSnapshots.latestRollbackable(version, mods)
        assertNotNull(rollbackable)
        val result = ModRepairSnapshots.rollback(version, rollbackable!!, mods)
        assertTrue(result.complete)
        assertEquals(listOf("sodium-old.jar"), names())
        assertEquals("OLD", text("sodium-old.jar"))
    }

    @Test
    fun interruptedRepairCanBeRolledBackAfterRestart() {
        put("sodium-old.jar", "OLD")
        val s = snapshot()
        simulateSuccessfulRepair() //应用在这里被杀：manifest 还是 PENDING，没有 finalize
        //“重新打开应用”：只从磁盘读
        val found = ModRepairSnapshots.latestRollbackable(version, mods)
        assertNotNull(found)
        assertEquals(SnapshotStatus.INTERRUPTED, found!!.status)
        assertEquals(s.id, found.id)

        val result = ModRepairSnapshots.rollback(version, found, mods)
        assertTrue(result.complete)
        assertEquals("OLD", text("sodium-old.jar"))
        assertEquals(listOf("sodium-old.jar"), names())
    }

    @Test
    fun snapshotSurvivesReadFromDisk() {
        put("a.jar", "A")
        val s = snapshot()
        val reread = ModRepairSnapshots.all(version).single()
        assertEquals(s.id, reread.id)
        assertEquals(s.before, reread.before)
    }

    @Test
    fun alreadyRolledBackSnapshotIsNotOfferedAgain() {
        put("sodium-old.jar", "OLD")
        val s = snapshot()
        simulateSuccessfulRepair()
        val applied = ModRepairSnapshots.finalize(version, s, mods, succeeded = true)
        ModRepairSnapshots.rollback(version, applied, mods)
        assertNull(ModRepairSnapshots.latestRollbackable(version, mods))
    }

    @Test
    fun collisionCopyKeepsTheOriginalBytesInTheSnapshot() {
        //目标文件名已被占用且内容不同：快照里仍有原始字节，回滚能还原
        put("sodium-new.jar", "PRE-EXISTING")
        val s = snapshot()
        File(mods, "sodium-new.jar").renameTo(File(mods, "sodium-new.jar.disabled")) //执行器先保留
        put("sodium-new.jar", "REPLACEMENT")
        val applied = ModRepairSnapshots.finalize(version, s, mods, succeeded = true)

        val result = ModRepairSnapshots.rollback(version, applied, mods)

        assertTrue(result.complete)
        assertEquals("PRE-EXISTING", text("sodium-new.jar"))
        assertEquals(listOf("sodium-new.jar"), names())
    }

    @Test
    fun preserveSafelyRefusesWhenDisabledNameIsTaken() {
        val jar = put("x.jar", "X")
        put("x.jar.disabled", "EARLIER DISABLED")
        assertNull(preserveSafely(jar))
        assertEquals("X", text("x.jar"))
        assertEquals("EARLIER DISABLED", text("x.jar.disabled"))
    }

    @Test
    fun preserveSafelyRenamesWhenFree() {
        val jar = put("x.jar", "X")
        val kept = preserveSafely(jar)
        assertNotNull(kept)
        assertEquals("x.jar.disabled", kept!!.name)
        assertEquals("X", kept.readText())
    }

    // ---- 同一实例同一时间只有一个修复/回滚 ----

    @Test
    fun lockAllowsOnlyOneRepairPerInstance() {
        assertTrue(ModRepairLock.tryAcquire(version))
        assertFalse(ModRepairLock.tryAcquire(version))
        ModRepairLock.release(version)
        assertTrue(ModRepairLock.tryAcquire(version))
        ModRepairLock.release(version)
    }

    @Test
    fun runningRepairPendingSnapshotIsNotTreatedAsInterrupted() {
        put("sodium-old.jar", "OLD")
        val s = snapshot()
        simulateSuccessfulRepair() //修复仍在进行：manifest 是 PENDING
        assertTrue(ModRepairLock.tryAcquire(version))
        try {
            assertNull(ModRepairSnapshots.latestRollbackable(version, mods))
            assertEquals(SnapshotStatus.PENDING, ModRepairSnapshots.all(version).single().status) //磁盘上没有被改写
        } finally {
            ModRepairLock.release(version)
        }
        //进程被杀（锁消失）之后，PENDING 才算被中断
        assertEquals(SnapshotStatus.INTERRUPTED, ModRepairSnapshots.latestRollbackable(version, mods)!!.status)
        assertEquals(s.id, ModRepairSnapshots.all(version).single().id)
    }

    @Test
    fun interruptedRepairRecoveryNeverOverwritesFilesTheUserChangedMeanwhile() {
        put("sodium-old.jar", "OLD"); put("fabric-api.jar", "API")
        snapshot()
        simulateSuccessfulRepair() //应用被杀：没有 finalize
        val found = ModRepairSnapshots.latestRollbackable(version, mods)!! //重新打开：after = 此刻的现状
        File(mods, "sodium-new.jar").writeText("EDITED BY USER AFTER THE CRASH")
        File(mods, "fabric-api.jar").writeText("API EDITED TOO")

        val result = ModRepairSnapshots.rollback(version, found, mods)

        assertFalse(result.complete)
        assertEquals("EDITED BY USER AFTER THE CRASH", text("sodium-new.jar"))
        assertEquals("API EDITED TOO", text("fabric-api.jar"))
        assertTrue("sodium-new.jar" in result.conflicts && "fabric-api.jar" in result.conflicts)
    }
}
