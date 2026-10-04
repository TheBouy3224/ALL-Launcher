/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.version.mod

import com.google.gson.Gson
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 一体包（同一个 jar 带多个加载器的元数据）的回归。
 *
 * 真实的 Fabulously Optimized 1.21.1 安装里，`lambdynamiclights-4.8.8+1.21.1.jar` 与
 * `puzzle-fabric-2.3.0+1.21.1.jar` 都同时带 `fabric.mod.json` 与 `META-INF/neoforge.mods.toml`。
 * 如果只按本地读取器选中的那一个加载器解析，Fabric 实例上会出现两类误报：
 *
 * 1. 「错误加载器」：拿 NeoForge 那份元数据去比对 Fabric 实例；
 * 2. 「缺少必需依赖」：Fabric 的内嵌 jar 登记在 `fabric.mod.json` 的 `jars` 里（磁盘上的
 *    `META-INF/jars/` 目录），只有读 Fabric 那份元数据才会被认作已提供。
 *
 * 这个整合包本该是干净的：误报会让真实的启动被拦下来。
 */
class ModDoctorMultiLoaderJarTest {
    @Test
    fun aJarCarryingBothLoadersIsReadAsTheInstanceLoader() = withTempMods { modsDir ->
        writeMultiLoaderJar(
            file = File(modsDir, "lambdynamiclights-4.8.8+1.21.1.jar"),
            fabricId = "lambdynlights",
            version = "4.8.8+1.21.1",
            requiredRuntimeId = "lambdynlights_runtime",
            nestedRuntimeEntry = "META-INF/jars/lambdynamiclights-4.8.8+1.21.1.jar",
        )

        val metadata = readDoctorMetadata(modsDir, ModLoader.FABRIC).single()

        assertEquals(ModLoader.FABRIC, metadata.loader)
        assertTrue(metadata.providedModIds.contains("lambdynlights_runtime"))
        assertEquals(
            "the nested runtime must not be reported missing",
            emptyList<ModDoctorProblem>(),
            validateModDoctorMetadata(
                mods = listOf(metadata),
                minecraftVersion = "1.21.1",
                loader = ModLoader.FABRIC,
            ).problems
        )
    }

    @Test
    fun aNeoForgeOnlyJarInAFabricInstanceIsStillWrongLoader() = withTempMods { modsDir ->
        writeNeoForgeOnlyJar(
            file = File(modsDir, "jade-neoforge-1.21.1.jar"),
            modId = "jade",
            version = "15.10.6",
        )

        val metadata = readDoctorMetadata(modsDir, ModLoader.FABRIC).single()

        assertEquals(
            listOf(ModDoctorProblemType.WRONG_LOADER),
            validateModDoctorMetadata(
                mods = listOf(metadata),
                minecraftVersion = "1.21.1",
                loader = ModLoader.FABRIC,
            ).problems.map { it.type }
        )
    }

    @Test
    fun aFabricOnlyJarInANeoForgeInstanceIsStillWrongLoader() = withTempMods { modsDir ->
        writeFabricOnlyJar(
            file = File(modsDir, "sodium-fabric-0.5.11+mc1.21.1.jar"),
            modId = "sodium",
            version = "0.5.11+mc1.21.1",
        )

        val metadata = readDoctorMetadata(modsDir, ModLoader.NEOFORGE).single()

        assertEquals(
            listOf(ModDoctorProblemType.WRONG_LOADER),
            validateModDoctorMetadata(
                mods = listOf(metadata),
                minecraftVersion = "1.21.1",
                loader = ModLoader.NEOFORGE,
            ).problems.map { it.type }
        )
    }

    // ------------------------------------------------------------------ helpers

    private fun readDoctorMetadata(modsDir: File, loader: ModLoader): List<ModDoctorMetadata> = runBlocking {
        val doctor = ModDoctor()
        AllModReader(modsDir).readAllLocals().map { local -> doctor.toDoctorMetadata(local, loader) }
    }

    private fun fabricJson(
        modId: String,
        version: String,
        depends: Map<String, String> = emptyMap(),
        jars: List<String> = emptyList(),
    ): String {
        val json = linkedMapOf<String, Any>(
            "schemaVersion" to 1,
            "id" to modId,
            "name" to modId,
            "version" to version,
            "depends" to depends,
        )
        if (jars.isNotEmpty()) json["jars"] = jars.map { linkedMapOf("file" to it) }
        return Gson().toJson(json)
    }

    private fun neoForgeToml(modId: String, version: String, required: List<String> = emptyList()): String = buildString {
        appendLine("modLoader = \"javafml\"")
        appendLine("loaderVersion = \"[2,)\"")
        appendLine("license = \"test\"")
        appendLine("[[mods]]")
        appendLine("\tmodId = \"$modId\"")
        appendLine("\tversion = \"$version\"")
        appendLine("\tdisplayName = \"$modId\"")
        required.forEach { dependency ->
            appendLine("[[dependencies.$modId]]")
            appendLine("\tmodId = \"$dependency\"")
            appendLine("\ttype = \"required\"")
            appendLine("\tversionRange = \"[1,)\"")
            appendLine("\tordering = \"NONE\"")
            appendLine("\tside = \"BOTH\"")
        }
    }

    private fun nestedJar(vararg entries: Pair<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun writeJar(file: File, entries: List<Pair<String, ByteArray>>) {
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
    }

    /** 一体包：Fabric 与 NeoForge 元数据都在，内嵌运行库只登记在 Fabric 那一份的 `jars` 里 */
    private fun writeMultiLoaderJar(
        file: File,
        fabricId: String,
        version: String,
        requiredRuntimeId: String,
        nestedRuntimeEntry: String,
    ) {
        writeJar(
            file,
            listOf(
                "fabric.mod.json" to fabricJson(
                    modId = fabricId,
                    version = version,
                    depends = mapOf("minecraft" to ">=1.21 <=1.21.1", requiredRuntimeId to ">=$version"),
                    jars = listOf(nestedRuntimeEntry),
                ).toByteArray(Charsets.UTF_8),
                "META-INF/neoforge.mods.toml" to neoForgeToml(fabricId, version, listOf(requiredRuntimeId)).toByteArray(Charsets.UTF_8),
                nestedRuntimeEntry to nestedJar(
                    "fabric.mod.json" to fabricJson(modId = requiredRuntimeId, version = version)
                ),
            )
        )
    }

    private fun writeNeoForgeOnlyJar(file: File, modId: String, version: String) {
        writeJar(file, listOf("META-INF/neoforge.mods.toml" to neoForgeToml(modId, version).toByteArray(Charsets.UTF_8)))
    }

    private fun writeFabricOnlyJar(file: File, modId: String, version: String) {
        writeJar(file, listOf("fabric.mod.json" to fabricJson(modId, version, mapOf("minecraft" to "1.21.1")).toByteArray(Charsets.UTF_8)))
    }

    private fun withTempMods(block: (File) -> Unit) {
        val dir = Files.createTempDirectory("all-multiloader").toFile()
        try {
            block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }
}
