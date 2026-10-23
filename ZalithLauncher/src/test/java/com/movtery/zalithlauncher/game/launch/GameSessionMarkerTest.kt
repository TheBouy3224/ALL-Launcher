package com.movtery.zalithlauncher.game.launch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class GameSessionMarkerTest {
    private fun dir(): File = Files.createTempDirectory("marker").toFile()

    @Test
    fun aReportedExitLeavesNothingToWarnAbout() {
        val d = dir()
        GameSessionMarker.begin(d, "QA-1", "/x/log", pid = 4242, now = 1L)
        GameSessionMarker.end(d)
        assertNull(GameSessionMarker.takeUnexpectedEnd(d) { false })
        assertTrue(d.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun aMarkerWhoseProcessIsGoneIsReportedOnceWithoutACause() {
        val d = dir()
        GameSessionMarker.begin(d, "QA-1", "/x/log", pid = 4242, now = 7L)
        val s = GameSessionMarker.takeUnexpectedEnd(d) { false }
        assertNotNull(s)
        assertEquals("QA-1", s!!.instance)
        assertEquals("/x/log", s.logPath)
        assertEquals(7L, s.startedAtMillis)
        //reported once only
        assertNull(GameSessionMarker.takeUnexpectedEnd(d) { false })
    }

    @Test
    fun aStillRunningGameIsNotReported() {
        val d = dir()
        GameSessionMarker.begin(d, "QA-1", "/x/log", pid = 4242)
        assertNull(GameSessionMarker.takeUnexpectedEnd(d) { pid -> pid == 4242 })
        //and the marker is kept for later
        assertNotNull(GameSessionMarker.takeUnexpectedEnd(d) { false })
    }

    @Test
    fun aCorruptMarkerIsDiscardedQuietly() {
        val d = dir()
        File(d, "game_session.properties").writeText("not json")
        assertNull(GameSessionMarker.takeUnexpectedEnd(d) { false })
        assertTrue(d.listFiles().orEmpty().isEmpty())
    }
}
