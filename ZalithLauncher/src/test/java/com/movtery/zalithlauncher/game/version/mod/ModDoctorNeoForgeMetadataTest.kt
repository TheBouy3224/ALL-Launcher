package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * NeoForge 元数据回归：真实的 `META-INF/neoforge.mods.toml` jar 条目 → AllModReader → ModDoctor
 * 的同一解析/校验路径。
 *
 * NeoForge 的 mod 只发布 `neoforge.mods.toml`（既无 `mods.toml` 也无 `mcmod.info`），而且从 NeoForge 21
 * 起依赖表用的是 `type = "required"` 而不是 Forge 的 `mandatory = true`。两条都要认，否则 NeoForge 的
 * mod 会以「读得懂、没有任何依赖」的样子通过检查：Mod Doctor 报 0 problems，真实的加载器随后
 * 用 `Missing or unsupported mandatory dependencies` 拒绝同一套 mod。
 *
 * 内嵌 jar（jar-in-jar，`META-INF/jarjar/` 里的 jar）同样是加载器眼中的独立 mod：JEI 依赖它自带的
 * `mezz_config`，这份副本让它不是「缺失依赖」。
 */
class ModDoctorNeoForgeMetadataTest {
    @Test
    fun neoForgeModsTomlIsReadableWithItsIdVersionAndLoader() = withTempMods { modsDir ->
        writeNeoForgeJar(
            file = File(modsDir, "balm-neoforge-1.21.1-21.0.65.jar"),
            modId = "balm",
            version = "21.0.65",
            displayName = "Balm",
        )

        val metadata = readDoctorMetadata(modsDir).single()

        assertTrue(metadata.readable)
        assertEquals("balm", metadata.modId)
        assertEquals("21.0.65", metadata.version)
        assertEquals(ModLoader.NEOFORGE, metadata.loader)
        assertTrue(
            validateModDoctorMetadata(
                mods = listOf(metadata),
                minecraftVersion = "1.21.1",
                loader = ModLoader.NEOFORGE,
            ).isClean
        )
    }

    @Test
    fun requiredTypeIsReadAsARequiredDependency() = withTempMods { modsDir ->
        writeNeoForgeJar(
            file = File(modsDir, "waystones-neoforge-1.21.1-21.1.45.jar"),
            modId = "waystones",
            version = "21.1.45",
            displayName = "Waystones",
            requiredDependencies = mapOf("balm" to "[21.0.39,)"),
        )

        val metadata = readDoctorMetadata(modsDir).single()

        assertEquals(listOf("balm"), metadata.requiredDependencies)
    }

    @Test
    fun waystonesNeoForgeWithoutBalmReportsMissingRequiredDependency() = withTempMods { modsDir ->
        writeNeoForgeJar(
            file = File(modsDir, "waystones-neoforge-1.21.1-21.1.45.jar"),
            modId = "waystones",
            version = "21.1.45",
            displayName = "Waystones",
            requiredDependencies = mapOf("balm" to "[21.0.39,)"),
        )

        val report = validateModDoctorMetadata(
            mods = readDoctorMetadata(modsDir),
            minecraftVersion = "1.21.1",
            loader = ModLoader.NEOFORGE,
        )

        assertEquals(1, report.problems.size)
        assertEquals(ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY, report.problems.single().type)
        assertEquals("balm", report.problems.single().requiredDependencyId)
    }

    @Test
    fun waystonesNeoForgeWithBalmHasNoMissingDependency() = withTempMods { modsDir ->
        writeNeoForgeJar(
            file = File(modsDir, "waystones-neoforge-1.21.1-21.1.45.jar"),
            modId = "waystones",
            version = "21.1.45",
            displayName = "Waystones",
            requiredDependencies = mapOf("balm" to "[21.0.39,)"),
        )
        writeNeoForgeJar(
            file = File(modsDir, "balm-neoforge-1.21.1-21.0.65.jar"),
            modId = "balm",
            version = "21.0.65",
            displayName = "Balm",
        )

        val report = validateModDoctorMetadata(
            mods = readDoctorMetadata(modsDir),
            minecraftVersion = "1.21.1",
            loader = ModLoader.NEOFORGE,
        )

        assertFalse(report.problems.any { it.type == ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY })
    }

    /**
     * JEI 的依赖表要求 `mezz_config`，而这份依赖由它自己内嵌的 jar 提供，顶层 mods 目录里没有这个文件。
     * 加载器能解析它，所以 Mod Doctor 也必须把它算作「已装」。
     */
    @Test
    fun nestedJarProviderSatisfiesItsDependency() = withTempMods { modsDir ->
        writeNeoForgeJar(
            file = File(modsDir, "jei-1.21.1-neoforge-19.56.0.441.jar"),
            modId = "jei",
            version = "19.56.0.441",
            displayName = "Just Enough Items",
            requiredDependencies = mapOf("mezz_config" to "[0.5.6,1.0.0)"),
            nestedJars = mapOf("META-INF/jarjar/mezz_config-1.21.1-neoforge-0.5.6.jar" to neoforgeJarBytes(
                modId = "mezz_config",
                version = "0.5.6",
                displayName = "Mezz Config",
            )),
        )

        val metadata = readDoctorMetadata(modsDir).single()

        val observed = "readable=${metadata.readable} modId=${metadata.modId} loader=${metadata.loader} " +
                "required=${metadata.requiredDependencies} provided=${metadata.providedModIds} " +
                "providedVersions=${metadata.providedModVersions}"
        assertTrue(observed, metadata.providedModIds.contains("mezz_config"))
        assertFalse(
            observed,
            validateModDoctorMetadata(
                mods = listOf(metadata),
                minecraftVersion = "1.21.1",
                loader = ModLoader.NEOFORGE,
            ).problems.any { it.type == ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY }
        )
    }

    @Test
    fun neoForgeJarWithoutSupportedMetadataRemainsCorrupt() = withTempMods { modsDir ->
        writeJar(File(modsDir, "unreadable-neoforge.jar"), mapOf("some/entry.txt" to "not metadata".toByteArray()))

        val metadata = readDoctorMetadata(modsDir).single()
        val report = validateModDoctorMetadata(
            mods = listOf(metadata),
            minecraftVersion = "1.21.1",
            loader = ModLoader.NEOFORGE,
        )

        assertFalse(metadata.readable)
        assertEquals(ModDoctorProblemType.CORRUPT_METADATA, report.problems.single().type)
    }

    private fun readDoctorMetadata(modsDir: File): List<ModDoctorMetadata> = runBlocking {
        val doctor = ModDoctor()
        AllModReader(modsDir).readAllLocals().map(doctor::toDoctorMetadata)
    }

    /**
     * The NeoForge 21 TOML shape as the published jars write it: `type = "required"`, never the Forge
     * `mandatory = true`, and the loader entry is what makes the file a NeoForge mod.
     */
    private fun neoforgeToml(
        modId: String,
        version: String,
        displayName: String,
        requiredDependencies: Map<String, String>,
    ): String {
        val dependencies = buildString {
            appendLine("[[dependencies.$modId]]")
            appendLine("modId=\"neoforge\"")
            appendLine("type=\"required\"")
            appendLine("versionRange=\"[21.1,)\"")
            appendLine("ordering=\"NONE\"")
            appendLine("side=\"BOTH\"")
            appendLine("[[dependencies.$modId]]")
            appendLine("modId=\"minecraft\"")
            appendLine("type=\"required\"")
            appendLine("versionRange=\"[1.21.1,)\"")
            appendLine("ordering=\"NONE\"")
            appendLine("side=\"BOTH\"")
            requiredDependencies.forEach { (dependencyId, predicate) ->
                appendLine("[[dependencies.$modId]]")
                appendLine("modId=\"$dependencyId\"")
                appendLine("type=\"required\"")
                appendLine("versionRange=\"$predicate\"")
                appendLine("ordering=\"NONE\"")
                appendLine("side=\"BOTH\"")
            }
        }
        return """
            modLoader="javafml"
            loaderVersion="[1,)"
            license="Test"
            [[mods]]
            modId="$modId"
            version="$version"
            displayName="$displayName"
            authors="QA"
            description='''Test fixture'''
            $dependencies
        """.trimIndent()
    }

    private fun neoforgeJarBytes(modId: String, version: String, displayName: String): ByteArray =
        buildJar(mapOf("META-INF/neoforge.mods.toml" to neoforgeToml(modId, version, displayName, emptyMap()).toByteArray()))

    private fun writeNeoForgeJar(
        file: File,
        modId: String,
        version: String,
        displayName: String,
        requiredDependencies: Map<String, String> = emptyMap(),
        nestedJars: Map<String, ByteArray> = emptyMap(),
    ) {
        val entries = mutableMapOf(
            "META-INF/neoforge.mods.toml" to neoforgeToml(modId, version, displayName, requiredDependencies).toByteArray()
        )
        nestedJars.forEach { (name, bytes) -> entries[name] = bytes }
        writeJar(file, entries)
    }

    private fun writeJar(file: File, entries: Map<String, ByteArray>) {
        file.writeBytes(buildJar(entries))
    }

    private fun buildJar(entries: Map<String, ByteArray>): ByteArray {
        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return buffer.toByteArray()
    }

    private fun withTempMods(block: (File) -> Unit) {
        val dir = Files.createTempDirectory("mod-doctor-neoforge-").toFile()
        try {
            block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }
}
