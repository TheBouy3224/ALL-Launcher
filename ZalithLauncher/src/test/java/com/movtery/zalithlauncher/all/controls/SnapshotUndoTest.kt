package com.movtery.zalithlauncher.all.controls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotUndoTest {
    private fun SnapshotUndo<Int>.settle(from: Int, to: Int) { observe(to, to); /* stable sample */ }

    @Test
    fun aFinishedEditIsOneStep() {
        val u = SnapshotUndo<Int>()
        u.observe(0, 0)           // first sample = baseline
        u.observe(0, 1)           // still changing: no step yet
        u.observe(1, 2)
        assertEquals(0, u.size)
        u.observe(2, 2)           // quiet for one interval: the edit 0 -> 2 is one step
        assertEquals(1, u.size)
        assertEquals(0, u.undo(2))
        assertNull(u.undo(0))
    }

    @Test
    fun separateEditsUndoInReverseOrder() {
        val u = SnapshotUndo<Int>()
        u.observe(0, 0)
        u.observe(1, 1); u.observe(1, 1)   // edit 1
        u.observe(2, 2); u.observe(2, 2)   // edit 2
        assertEquals(2, u.size)
        assertEquals(1, u.undo(2))
        assertEquals(0, u.undo(1))
        assertNull(u.undo(0))
    }

    @Test
    fun anUnfinishedEditIsRevertedToTheLastStableStateFirst() {
        val u = SnapshotUndo<Int>()
        u.observe(0, 0)
        u.observe(5, 5); u.observe(5, 5)   // stable = 5, stack [0]
        assertEquals(5, u.undo(7))         // user is mid-edit (7): undo goes back to 5, keeps history
        assertEquals(1, u.size)
        assertEquals(0, u.undo(5))
    }

    @Test
    fun undoDoesNotCreateAStepAndNothingToUndoIsReported() {
        val u = SnapshotUndo<Int>()
        assertFalse(u.canUndo(0))
        u.observe(0, 0)
        assertFalse(u.canUndo(0))
        u.observe(3, 3); u.observe(3, 3)
        assertTrue(u.canUndo(3))
        u.undo(3)                          // restored to 0 ...
        u.observe(0, 0)                    // ... the tracker sees it as the stable state, no new step
        assertEquals(0, u.size)
        assertFalse(u.canUndo(0))
    }

    @Test
    fun historyIsLimited() {
        val u = SnapshotUndo<Int>(limit = 3)
        u.observe(0, 0)
        for (i in 1..10) { u.observe(i, i); u.observe(i, i) }
        assertEquals(3, u.size)
    }
}
