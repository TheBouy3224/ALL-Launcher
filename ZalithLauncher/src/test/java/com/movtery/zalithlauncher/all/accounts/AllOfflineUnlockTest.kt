package com.movtery.zalithlauncher.all.accounts

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AllOfflineUnlockTest {
    @Test
    fun sevenQuickTapsUnlock() {
        val t = OfflineUnlockTaps()
        val results = (0 until 7).map { t.tap(1_000L + it * 400L) }
        assertTrue(results.last())
        assertTrue(results.dropLast(1).none { it })
    }

    @Test
    fun sixTapsDoNotUnlock() {
        val t = OfflineUnlockTaps()
        assertFalse((0 until 6).map { t.tap(it * 500L) }.any { it })
    }

    @Test
    fun aPauseOverThreeSecondsRestartsTheCount() {
        val t = OfflineUnlockTaps()
        repeat(5) { t.tap(it * 300L) }
        //pause: counting restarts at 1, so six more taps are still not enough
        val after = (0 until 6).map { t.tap(10_000L + it * 300L) }
        assertFalse(after.any { it })
        assertTrue(t.tap(10_000L + 6 * 300L))
    }

    @Test
    fun theSequenceCanBeRepeatedAfterUnlocking() {
        val t = OfflineUnlockTaps()
        assertTrue((0 until 7).map { t.tap(it * 200L) }.last())
        assertFalse(t.tap(2_000L))
    }

    @Test
    fun offlineStaysHiddenForNormalUsersButNeverForExistingOfflineUsers() {
        assertFalse(offlineOptionVisible(unlocked = false, hasLocalAccount = false))
        assertTrue(offlineOptionVisible(unlocked = true, hasLocalAccount = false))
        assertTrue(offlineOptionVisible(unlocked = false, hasLocalAccount = true))
    }
}
