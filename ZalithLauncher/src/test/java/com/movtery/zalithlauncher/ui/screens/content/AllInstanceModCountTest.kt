package com.movtery.zalithlauncher.ui.screens.content

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AllInstanceModCountTest {

    @Test
    fun countsTopLevelJarsAndDisabledJarsOnly() {
        val gameDir = Files.createTempDirectory("instance-mod-count").toFile()
        try {
            val mods = File(gameDir, "mods").apply { mkdirs() }
            File(mods, "a.jar").writeText("x")
            File(mods, "b.jar.disabled").writeText("x")
            File(mods, "readme.txt").writeText("x")
            File(mods, "sub").apply { mkdirs() }.let { File(it, "nested.jar").writeText("x") }

            assertEquals(2, countInstalledModJars(gameDir))
        } finally {
            gameDir.deleteRecursively()
        }
    }

    @Test
    fun missingModsFolderCountsAsZero() {
        val gameDir = Files.createTempDirectory("instance-mod-count-empty").toFile()
        try {
            assertEquals(0, countInstalledModJars(gameDir))
        } finally {
            gameDir.deleteRecursively()
        }
    }
}
