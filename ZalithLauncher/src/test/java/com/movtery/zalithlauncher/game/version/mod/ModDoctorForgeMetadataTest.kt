package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 现代 Forge 元数据回归：真实 jar 条目 → AllModReader → ModDoctor 的同一解析/校验路径。
 */
class ModDoctorForgeMetadataTest {
    @Test
    fun modernForgeModsTomlIsReadableWithItsIdAndVersion() = withTempMods { modsDir ->
        writeModernForgeJar(
            file = File(modsDir, "balm-forge-1.20.1-7.3.43.jar"),
            modId = "balm",
            version = "7.3.43",
            displayName = "Balm",
        )

        val metadata = readDoctorMetadata(modsDir).single()

        assertTrue(metadata.readable)
        assertEquals("balm", metadata.modId)
        assertEquals("7.3.43", metadata.version)
        assertEquals(ModLoader.FORGE, metadata.loader)
        assertTrue(
            validateModDoctorMetadata(
                mods = listOf(metadata),
                minecraftVersion = "1.20.1",
                loader = ModLoader.FORGE,
            ).isClean
        )
    }

    @Test
    fun waystonesWithoutBalmReportsMissingRequiredDependency() = withTempMods { modsDir ->
        writeModernForgeJar(
            file = File(modsDir, "waystones-forge-1.20.1-14.1.21.jar"),
            modId = "waystones",
            version = "14.1.21",
            displayName = "Waystones",
            requiredDependencies = mapOf("balm" to "[7.2.0,)")
        )

        val report = validateModDoctorMetadata(
            mods = readDoctorMetadata(modsDir),
            minecraftVersion = "1.20.1",
            loader = ModLoader.FORGE,
        )

        assertEquals(1, report.problems.size)
        assertEquals(ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY, report.problems.single().type)
        assertEquals("balm", report.problems.single().requiredDependencyId)
    }

    @Test
    fun waystonesWithBalmHasNoMissingDependency() = withTempMods { modsDir ->
        writeModernForgeJar(
            file = File(modsDir, "waystones-forge-1.20.1-14.1.21.jar"),
            modId = "waystones",
            version = "14.1.21",
            displayName = "Waystones",
            requiredDependencies = mapOf("balm" to "[7.2.0,)")
        )
        writeModernForgeJar(
            file = File(modsDir, "balm-forge-1.20.1-7.3.43.jar"),
            modId = "balm",
            version = "7.3.43",
            displayName = "Balm",
        )

        val report = validateModDoctorMetadata(
            mods = readDoctorMetadata(modsDir),
            minecraftVersion = "1.20.1",
            loader = ModLoader.FORGE,
        )

        assertFalse(report.problems.any { it.type == ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY })
    }

    @Test
    fun legacyMcmodInfoStillParses() = withTempMods { modsDir ->
        writeJar(
            File(modsDir, "legacy-forge.jar"),
            mapOf(
                "mcmod.info" to """
                    [{
                      "modid": "legacy_example",
                      "name": "Legacy Example",
                      "version": "1.2.3",
                      "mcversion": "1.12.2"
                    }]
                """.trimIndent()
            )
        )

        val metadata = readDoctorMetadata(modsDir).single()

        assertTrue(metadata.readable)
        assertEquals("legacy_example", metadata.modId)
        assertEquals("1.2.3", metadata.version)
        assertEquals(listOf("1.12.2"), metadata.minecraftVersionRanges)
    }

    @Test
    fun forgeJarWithoutSupportedMetadataRemainsCorrupt() = withTempMods { modsDir ->
        writeJar(File(modsDir, "unreadable-forge.jar"), mapOf("some/entry.txt" to "not metadata"))

        val metadata = readDoctorMetadata(modsDir).single()
        val report = validateModDoctorMetadata(
            mods = listOf(metadata),
            minecraftVersion = "1.20.1",
            loader = ModLoader.FORGE,
        )

        assertFalse(metadata.readable)
        assertEquals(ModDoctorProblemType.CORRUPT_METADATA, report.problems.single().type)
    }

    private fun readDoctorMetadata(modsDir: File): List<ModDoctorMetadata> = runBlocking {
        val doctor = ModDoctor()
        AllModReader(modsDir).readAllLocals().map(doctor::toDoctorMetadata)
    }

    private fun writeModernForgeJar(
        file: File,
        modId: String,
        version: String,
        displayName: String,
        requiredDependencies: Map<String, String> = emptyMap(),
    ) {
        val dependencies = buildString {
            appendLine("[[dependencies.$modId]]")
            appendLine("modId=\"forge\"")
            appendLine("mandatory=true")
            appendLine("versionRange=\"[47,)\"")
            appendLine("ordering=\"NONE\"")
            appendLine("side=\"BOTH\"")
            appendLine("[[dependencies.$modId]]")
            appendLine("modId=\"minecraft\"")
            appendLine("mandatory=true")
            appendLine("versionRange=\"[1.20.1,1.21)\"")
            appendLine("ordering=\"NONE\"")
            appendLine("side=\"BOTH\"")
            requiredDependencies.forEach { (dependencyId, predicate) ->
                appendLine("[[dependencies.$modId]]")
                appendLine("modId=\"$dependencyId\"")
                appendLine("mandatory=true")
                appendLine("versionRange=\"$predicate\"")
                appendLine("ordering=\"NONE\"")
                appendLine("side=\"BOTH\"")
            }
        }
        val toml = """
            modLoader="javafml"
            loaderVersion="[47,)"
            license="Test"
            [[mods]]
            modId="$modId"
            version="$version"
            displayName="$displayName"
            authors="QA"
            description='''Test fixture'''
            $dependencies
        """.trimIndent()
        writeJar(file, mapOf("META-INF/mods.toml" to toml))
    }

    private fun writeJar(file: File, entries: Map<String, String>) {
        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
    }

    private fun withTempMods(block: (File) -> Unit) {
        val dir = Files.createTempDirectory("mod-doctor-forge-").toFile()
        try {
            block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }
}
