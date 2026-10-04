package com.movtery.zalithlauncher.game.version.mod

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ModpackRequiredCoreTest {
    private val required = PackOwnedMod(
        name = "core-a.jar",
        sha1 = "a".repeat(40),
        url = "https://example.invalid/core-a.jar",
        requiredCore = true,
    )
    private val optional = PackOwnedMod(name = "optional.jar", requiredCore = false)

    @Test
    fun `missing required core is reported while optional absence and user extras are ignored`() {
        val versionPath = Files.createTempDirectory("required-core").toFile()
        val mods = File(versionPath, "mods").apply { mkdirs() }
        File(mods, "user-extra.jar").writeText("extra")
        val policy = ModpackPolicy(ModpackManifest(mods = listOf(required, optional)))

        val problems = policy.requiredCoreProblems(mods)

        assertEquals(1, problems.size)
        assertEquals(required.name, problems.single().fileName)
    }

    @Test
    fun `matching required core is clean and wrong hash is incompatible`() {
        val versionPath = Files.createTempDirectory("required-core-hash").toFile()
        val mods = File(versionPath, "mods").apply { mkdirs() }
        val core = File(mods, required.name).apply { writeText("wrong core") }
        val policy = ModpackPolicy(ModpackManifest(mods = listOf(required)))

        assertTrue(policy.requiredCoreProblems(mods).isNotEmpty())
        core.writeText("a")
        val matching = PackOwnedMod(name = core.name, sha1 = "86f7e437faa5a7fce15d1ddcb9eaeaea377667b8", requiredCore = true)
        assertTrue(ModpackPolicy(ModpackManifest(mods = listOf(matching))).requiredCoreProblems(mods).isEmpty())
    }

    @Test
    fun `an unlisted user mod is not pack-owned even with an incomplete manifest`() {
        val policy = ModpackPolicy(
            ModpackManifest(mods = listOf(required), modsComplete = false)
        )

        assertTrue(!policy.isPackOwned("user-extra.jar"))
    }
}
