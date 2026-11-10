package com.movtery.zalithlauncher.all.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MusicQueueTest {
    private fun t(n: Int) = MusicTrack("content://$n", "t$n")
    private fun queue(n: Int, index: Int = 0, shuffle: Boolean = false, repeat: MusicRepeat = MusicRepeat.OFF) =
        MusicQueue(MusicSavedState((0 until n).map(::t), index, shuffle, repeat), Random(7))

    @Test fun emptyQueueHasNothingToPlay() {
        val q = queue(0)
        assertEquals(-1, q.currentIndex)
        assertFalse(q.next()); assertFalse(q.previous()); assertFalse(q.advanceAfterCompletion())
    }

    @Test fun nextAndPreviousWrap() {
        val q = queue(3)
        q.previous(); assertEquals(2, q.currentIndex)
        q.next(); assertEquals(0, q.currentIndex)
    }

    @Test fun repeatOffStopsAfterLastTrack() {
        val q = queue(2, index = 1)
        assertFalse(q.advanceAfterCompletion())
        assertEquals(1, q.currentIndex)
    }

    @Test fun repeatAllWraps() {
        val q = queue(2, index = 1, repeat = MusicRepeat.ALL)
        assertTrue(q.advanceAfterCompletion())
        assertEquals(0, q.currentIndex)
    }

    @Test fun repeatOneStaysOnCompletionButNextButtonMoves() {
        val q = queue(3, index = 1, repeat = MusicRepeat.ONE)
        assertTrue(q.advanceAfterCompletion()); assertEquals(1, q.currentIndex)
        q.next(); assertEquals(2, q.currentIndex)
    }

    @Test fun cycleRepeatVisitsAllModes() {
        val q = queue(1)
        q.cycleRepeat(); assertEquals(MusicRepeat.ALL, q.repeat)
        q.cycleRepeat(); assertEquals(MusicRepeat.ONE, q.repeat)
        q.cycleRepeat(); assertEquals(MusicRepeat.OFF, q.repeat)
    }

    @Test fun shuffleKeepsCurrentAndPlaysEveryTrackOnce() {
        val q = queue(20, index = 5)
        q.toggleShuffle()
        assertEquals(5, q.currentIndex)
        val seen = mutableListOf(q.currentIndex)
        while (q.advanceAfterCompletion()) seen += q.currentIndex
        assertEquals((0 until 20).toList(), seen.sorted())
        assertTrue("order must actually change", seen != (5 until 20).toList() + (0 until 5).toList())
    }

    @Test fun shuffleOffRestoresListOrderFromCurrent() {
        val q = queue(5, index = 2)
        q.toggleShuffle(); q.toggleShuffle()
        assertEquals(2, q.currentIndex)
        q.next(); assertEquals(3, q.currentIndex)
    }

    @Test fun addSkipsDuplicatesAndKeepsCurrent() {
        val q = queue(2, index = 1)
        assertEquals(1, q.add(listOf(t(0), t(9), t(9))))
        assertEquals(3, q.tracks.size)
        assertEquals(1, q.currentIndex)
    }

    @Test fun firstAddSelectsFirstTrack() {
        val q = queue(0)
        q.add(listOf(t(1)))
        assertEquals(0, q.currentIndex)
    }

    @Test fun removeBeforeCurrentKeepsSameTrack() {
        val q = queue(3, index = 2)
        assertFalse(q.remove(0))
        assertEquals(t(2), q.current)
    }

    @Test fun removeCurrentMovesToNeighbourAndLastEmpties() {
        val q = queue(2, index = 1)
        assertTrue(q.remove(1)); assertEquals(t(0), q.current)
        assertTrue(q.remove(0)); assertEquals(-1, q.currentIndex)
    }

    @Test fun removeBadIndexChangesNothing() {
        val q = queue(1)
        assertFalse(q.remove(4))
        assertEquals(1, q.tracks.size)
    }

    @Test fun savedStateRoundTrips() {
        val q = queue(3, index = 2, repeat = MusicRepeat.ALL)
        val back = decodeMusicState(q.toSaved(0.25f).encode())
        assertEquals(q.tracks, back.tracks)
        assertEquals(2, back.index)
        assertEquals(MusicRepeat.ALL, back.repeat)
        assertEquals(0.25f, back.volume, 0f)
    }

    @Test fun damagedSavedStateIsSafe() {
        assertEquals(MusicSavedState(), decodeMusicState(null))
        assertEquals(MusicSavedState(), decodeMusicState("{not json"))
        val odd = decodeMusicState("""{"tracks":[null,{"uri":""},{"uri":"content://a"}],"index":40,"volume":9,"repeat":"NOPE"}""")
        assertEquals(1, odd.tracks.size)
        assertEquals("", odd.tracks[0].title)
        assertEquals(0, odd.index)
        assertEquals(1f, odd.volume, 0f)
        assertEquals(MusicRepeat.OFF, odd.repeat)
    }
}
