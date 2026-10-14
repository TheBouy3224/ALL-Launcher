package com.movtery.zalithlauncher.game.version.mod

import kotlinx.coroutines.runBlocking
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

/**
 * Crash Doctor: การปิดม็อดแบบมีสแนปชอต ต้องใช้ [ModRepairLock] เดียวกับ Mod Doctor,
 * ปล่อยล็อกเสมอ, ใช้ผลจริงของการปิดในการ finalize, และไม่ทำอะไรเลยเมื่อสแนปชอตไม่ได้
 */
class CrashDisableSnapshotTest {
    private lateinit var version: File
    private lateinit var mods: File
    private lateinit var mod: File

    @Before
    fun setUp() {
        version = Files.createTempDirectory("crash-disable-test").toFile()
        mods = File(version, "mods").apply { mkdirs() }
        mod = File(mods, "litematica.jar").apply { writeText("LITEMATICA") }
    }

    @After
    fun tearDown() {
        ModRepairLock.release(version)
        version.deleteRecursively()
    }

    /** เลียนแบบ disableModById: เปลี่ยนชื่อเป็น .disabled แล้วคืนชื่อเดิม; ไม่มีไฟล์หรือเปลี่ยนชื่อไม่ได้ = null */
    private fun renameToDisabled(): String? {
        if (!mod.isFile) return null
        val preState = mod.name
        return preState.takeIf { mod.renameTo(File(mods, "$preState.disabled")) }
    }

    private fun disableWithSnapshot(disable: suspend () -> String?): SnapshotDisableResult =
        runBlocking { disableModWithSnapshot(version, mods, disable) }

    @Test
    fun theSameLockIsHeldDuringTheWholeOperationAndReleasedOnSuccess() {
        var heldInside = false
        val result = disableWithSnapshot {
            heldInside = ModRepairLock.isHeld(version)
            renameToDisabled()
        }

        assertTrue("the crash fix must hold the shared ModRepairLock while it works", heldInside)
        assertEquals(SnapshotDisableResult.Disabled("litematica.jar"), result)
        assertFalse("the lock must be released after success", ModRepairLock.isHeld(version))
    }

    @Test
    fun aRunningRepairOrRollbackBlocksTheCrashFixAndNothingChanges() {
        assertTrue(ModRepairLock.tryAcquire(version))
        var disableCalled = false

        val result = disableWithSnapshot {
            disableCalled = true
            renameToDisabled()
        }

        assertEquals(SnapshotDisableResult.Busy, result)
        assertFalse("a busy lock must stop before the disable step", disableCalled)
        assertTrue("the file must not be touched", mod.isFile)
        assertEquals(emptyList<RepairSnapshot>(), ModRepairSnapshots.all(version))
        //ผู้ที่ถือล็อกอยู่ก่อนยังถือต่อ: ฟังก์ชันนี้ไม่ไปปล่อยของคนอื่น
        assertTrue(ModRepairLock.isHeld(version))
    }

    @Test
    fun failedDisableReleasesTheLockAndFinalizesTheSnapshotAsNoChange() {
        val result = disableWithSnapshot { null }

        assertEquals(SnapshotDisableResult.DisableFailed, result)
        assertFalse(ModRepairLock.isHeld(version))
        val snapshot = ModRepairSnapshots.all(version).single()
        assertEquals("a failed disable with no file change is recorded as NO_CHANGE, not as a repair", SnapshotStatus.NO_CHANGE, snapshot.status)
        assertNull("nothing to roll back after a failed disable", ModRepairSnapshots.latestRollbackable(version, mods))
    }

    @Test
    fun aDisableThatThrowsIsAFailureAndStillReleasesTheLock() {
        val result = disableWithSnapshot { throw IllegalStateException("rename exploded") }

        assertEquals(SnapshotDisableResult.DisableFailed, result)
        assertFalse(ModRepairLock.isHeld(version))
        assertTrue("the file must still be where it was", mod.isFile)
    }

    @Test
    fun aRefusedSnapshotChangesNothingAndNeverDisables() {
        val missingMods = File(version, "no-mods-here")
        var disableCalled = false

        val result = runBlocking {
            disableModWithSnapshot(version, missingMods) {
                disableCalled = true
                renameToDisabled()
            }
        }

        assertTrue("expected a refused snapshot, got $result", result is SnapshotDisableResult.SnapshotRefused)
        assertFalse("no disable may run without a snapshot", disableCalled)
        assertFalse("the lock must be released after a refusal", ModRepairLock.isHeld(version))
        assertTrue(mod.isFile)
    }

    @Test
    fun successIsFinalizedAsAppliedSoItCanBeRolledBackToTheOriginalFile() {
        val result = disableWithSnapshot { renameToDisabled() }
        assertTrue(result is SnapshotDisableResult.Disabled)

        val snapshot = ModRepairSnapshots.all(version).single()
        assertEquals(SnapshotStatus.APPLIED, snapshot.status)
        assertNotNull(ModRepairSnapshots.latestRollbackable(version, mods))
        val rollbackable = ModRepairSnapshots.latestRollbackable(version, mods)!!

        val rolledBack = ModRepairSnapshots.rollback(version, rollbackable, mods)
        assertTrue(rolledBack.complete)
        assertEquals("LITEMATICA", mod.readText())
        assertFalse("the disabled copy moves to quarantine, it is never deleted", File(mods, "litematica.jar.disabled").exists())
    }

    @Test
    fun aFailureAfterTheRenameIsFinalizedAsPartialSoTheUserCanStillRollBack() {
        //禁用改了文件却报告失败：快照必须记为 PARTIAL（可回滚），而不是 NO_CHANGE 或 APPLIED
        val result = disableWithSnapshot {
            renameToDisabled()
            null
        }

        assertEquals(SnapshotDisableResult.DisableFailed, result)
        assertEquals(SnapshotStatus.PARTIAL, ModRepairSnapshots.all(version).single().status)
        assertNotNull(ModRepairSnapshots.latestRollbackable(version, mods))
        assertFalse(ModRepairLock.isHeld(version))
    }
}
