package com.movtery.zalithlauncher.game.version.installed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PersistentControlsTest {
    @Test fun defaultSurvivesReloadWithoutChangingAnotherInstance() {
        val root = Files.createTempDirectory("instance-controls").toFile()
        val first = VersionConfig(File(root, "first"))
        val second = VersionConfig(File(root, "second"))
        assertTrue(second.setControlAndSave("other.json"))
        val otherConfig = File(VersionsManager.getZalithVersionPath(second.getVersionPath()), "version.config")
        val before = otherConfig.readBytes()
        assertTrue(first.setControlAndSave("pvp.json"))
        assertEquals("pvp.json", VersionConfig.parseConfig(first.getVersionPath()).control)
        assertTrue(before.contentEquals(otherConfig.readBytes()))
        assertEquals("other.json", VersionConfig.parseConfig(second.getVersionPath()).control)
    }

    @Test fun failedWriteKeepsThePreviousDefault() {
        val root = Files.createTempDirectory("instance-controls-fail").toFile()
        val config = VersionConfig(root)
        config.control = "previous.json"
        // A file where the launcher directory must be makes saving fail on every OS.
        VersionsManager.getZalithVersionPath(root).writeText("blocked")
        assertFalse(config.setControlAndSave("new.json"))
        assertEquals("previous.json", config.control)
    }
}
