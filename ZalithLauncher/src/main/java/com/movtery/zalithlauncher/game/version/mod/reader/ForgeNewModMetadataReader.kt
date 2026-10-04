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
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.version.mod.reader

import com.moandjiezana.toml.Toml
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.version.mod.LocalMod
import com.movtery.zalithlauncher.game.version.mod.ModMetadataReader
import com.movtery.zalithlauncher.game.version.mod.meta.ForgeNewModMetadata
import com.movtery.zalithlauncher.utils.GSON
import com.movtery.zalithlauncher.utils.file.UnpackZipException
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.jar.Attributes
import java.util.jar.Manifest
import java.util.zip.ZipFile as JDKZipFile
import java.util.zip.ZipInputStream

private const val TAG = "ForgeNewMetadata"

/**
 * 现代 Forge / NeoForge TOML 中 Mod Doctor 需要的事实。
 * 解析仍由 [ForgeNewModMetadataReader] 的 TOML 路径完成；这里不另造解析器。
 */
internal data class ForgeNewDoctorMetadata(
    val modId: String,
    val name: String,
    val version: String?,
    val loader: ModLoader,
    val requiredDependencies: List<String>,
    val requiredDependencyPredicates: Map<String, List<String>>,
    val minecraftVersionRanges: List<String>,
    /** 内嵌 jar（`META-INF/jarjar/` 里的 jar）自带的 id：加载器把它们当作独立 mod 解析依赖 */
    val providedModIds: List<String> = emptyList(),
    /** 内嵌 jar 自己声明的版本；外层文件的版本不属于这些 id */
    val providedModVersions: Map<String, String> = emptyMap(),
)

/**
 * [Reference HMCL](https://github.com/HMCL-dev/HMCL/blob/4650287/HMCLCore/src/main/java/org/jackhuang/hmcl/mod/modinfo/ForgeNewModMetadata.java)
 */
object ForgeNewModMetadataReader : ModMetadataReader {
    private const val ACC_FORGE = 0x01
    private const val ACC_NEO_FORGED = 0x02
    private const val JAR_JAR_PREFIX = "META-INF/jarjar/"
    private val NESTED_TOML_PATHS = setOf("META-INF/mods.toml", "META-INF/neoforge.mods.toml")

    /**
     * 给 Mod Doctor 使用的同步适配器：复用本 reader 的 `mods.toml` / `neoforge.mods.toml` 解析路径，
     * 同时保留依赖表，而普通 [LocalMod] 只保存展示所需字段。
     */
    internal fun readDoctorMetadata(modFile: File): ForgeNewDoctorMetadata {
        JDKZipFile(modFile).use { zip ->
            runCatching {
                return readDoctorMetadataFromToml(
                    zip = zip,
                    tomlPath = "META-INF/mods.toml",
                    loaderACC = ACC_FORGE or ACC_NEO_FORGED,
                    defaultLoader = ModLoader.FORGE,
                    modFile = modFile,
                )
            }
            return readDoctorMetadataFromToml(
                zip = zip,
                tomlPath = "META-INF/neoforge.mods.toml",
                loaderACC = ACC_NEO_FORGED,
                defaultLoader = ModLoader.NEOFORGE,
                modFile = modFile,
            )
        }
    }

    override suspend fun fromLocal(modFile: File): LocalMod = withContext(Dispatchers.IO) {
        try {
            JDKZipFile(modFile).use { zip ->
                //尝试 Forge
                runCatching {
                    readFromToml(
                        zip = zip,
                        tomlPath = "META-INF/mods.toml",
                        loaderACC = ACC_FORGE or ACC_NEO_FORGED,
                        defaultLoader = ModLoader.FORGE,
                        modFile = modFile
                    )
                }.onSuccess { return@withContext it }

                //尝试 NeoForge
                runCatching {
                    readFromToml(
                        zip = zip,
                        tomlPath = "META-INF/neoforge.mods.toml",
                        loaderACC = ACC_NEO_FORGED,
                        defaultLoader = ModLoader.NEOFORGE,
                        modFile = modFile
                    )
                }.onSuccess { return@withContext it }

                throw UnpackZipException("File $modFile is not a Forge 1.13+ or NeoForge mod.")
            }
        } catch (e: Exception) {
            if (e !is UnpackZipException) return@withContext readWithApacheZip(modFile)
            else throw e
        }
    }

    private fun readDoctorMetadataFromToml(
        zip: JDKZipFile,
        tomlPath: String,
        loaderACC: Int,
        defaultLoader: ModLoader,
        modFile: File,
    ): ForgeNewDoctorMetadata {
        val tomlEntry = zip.getEntry(tomlPath) ?: throw IOException("TOML file $tomlPath not found")
        val toml = zip.getInputStream(tomlEntry).bufferedReader().use { reader ->
            Toml().read(reader.readText())
        }
        val tomlMap = toml.toMap() as MutableMap<String, Any?>
        fixAuthorsField(tomlMap)
        val metadata = GSON.fromJson(GSON.toJsonTree(tomlMap), ForgeNewModMetadata::class.java)
            ?: throw IOException("Failed to parse TOML metadata, file = $modFile")
        val mod = metadata.mods.firstOrNull()
            ?: throw IOException("Mod $modFile `$tomlPath` is malformed")
        val jarVersion = readVersion(zip, modFile)
        val resolvedVersion = mod.version.replace("\${file.jarVersion}", jarVersion ?: "")
        val loader = analyzeLoader(toml, mod.modId, loaderACC, defaultLoader)
        val dependencies = dependencyTables(toml, mod.modId)
            .filter { dependency -> dependency.isRequiredDependency() }
        val required = dependencies
            .mapNotNull { it["modId"] as? String }
            .filterNot { it in FORGE_ENVIRONMENT_DEPENDENCIES }
            .distinct()
        val requiredPredicates = dependencies
            .mapNotNull { dependency ->
                val id = dependency["modId"] as? String ?: return@mapNotNull null
                if (id in FORGE_ENVIRONMENT_DEPENDENCIES) return@mapNotNull null
                val predicate = dependency["versionRange"] as? String ?: return@mapNotNull null
                id to listOf(predicate)
            }
            .toMap()
        val minecraftRanges = dependencies
            .filter { it["modId"] == "minecraft" }
            .mapNotNull { it["versionRange"] as? String }
        val nestedProviders = nestedJarProviders(zip)

        return ForgeNewDoctorMetadata(
            modId = mod.modId,
            name = mod.displayName,
            version = resolvedVersion.takeIf { it.isNotBlank() },
            loader = loader,
            requiredDependencies = required,
            requiredDependencyPredicates = requiredPredicates,
            minecraftVersionRanges = minecraftRanges,
            providedModIds = nestedProviders.map { (id, _) -> id }.distinct(),
            providedModVersions = nestedProviders
                .mapNotNull { (id, version) -> version?.takeIf { it.isNotBlank() }?.let { id to it } }
                .toMap(),
        )
    }

    private fun dependencyTables(toml: Toml, modId: String): List<Map<String, Any>> {
        val direct = runCatching { toml.getTables("dependencies") }.getOrNull()
        if (direct != null) return direct.map { it.toMap() as Map<String, Any> }
        val grouped = toml.getTable("dependencies")?.getTables(modId)
        return grouped?.map { it.toMap() as Map<String, Any> } ?: emptyList()
    }

    /**
     * 该依赖是否为必需依赖：缺了它加载器就会拒绝这套 mod。
     *
     * Forge 的 `mods.toml` 写 `mandatory = true`；NeoForge 21 起改用 `type`，必需是 `type = "required"`。
     * 其余取值（`optional` / `incompatible` / `discouraged`）都不是「缺了就装不上」，不能当成必需依赖上报。
     */
    private fun Map<String, Any>.isRequiredDependency(): Boolean {
        if (this["mandatory"] as? Boolean == true) return true
        return (this["type"] as? String)?.equals("required", ignoreCase = true) == true
    }

    /**
     * 内嵌 jar（jar-in-jar）提供的 id 与版本。
     *
     * NeoForge 把 `META-INF/jarjar/` 里的每个内嵌 jar 当作独立 mod 来解析依赖，所以别人声明依赖它时
     * 并不是「缺失依赖」（JEI 就依赖它自带的 `mezz_config`，顶层 mods 目录里没有这个文件）。
     * id 与版本都取自内嵌 jar 自己的 TOML，不借用外层文件的版本。
     */
    private fun nestedJarProviders(zip: JDKZipFile): List<Pair<String, String?>> {
        return Collections.list(zip.entries())
            .filter { entry -> entry.name.startsWith(JAR_JAR_PREFIX) && entry.name.endsWith(".jar") }
            .flatMap { entry ->
                runCatching {
                    zip.getInputStream(entry).use { input ->
                        ZipInputStream(input).use { nested -> readNestedTomlMod(nested) }
                    }
                }.getOrDefault(emptyList())
            }
    }

    /** 内嵌 jar 里第一个 `mods.toml` / `neoforge.mods.toml` 声明的 id 与版本。 */
    private fun readNestedTomlMod(nested: ZipInputStream): List<Pair<String, String?>> {
        var entry = nested.nextEntry
        while (entry != null) {
            if (entry.name in NESTED_TOML_PATHS) {
                val text = nested.readBytes().toString(Charsets.UTF_8)
                return runCatching {
                    // 与另外两条解析路径一致：authors 在 TOML 里是字符串，先归一化再映射，否则 GSON 会失败
                    val tomlMap = Toml().read(text).toMap() as MutableMap<String, Any?>
                    fixAuthorsField(tomlMap)
                    val metadata = GSON.fromJson(GSON.toJsonTree(tomlMap), ForgeNewModMetadata::class.java)
                    metadata?.mods?.firstOrNull()?.let { mod ->
                        listOf(mod.modId to mod.version.takeIf { it.isNotBlank() })
                    } ?: emptyList()
                }.getOrDefault(emptyList())
            }
            entry = nested.nextEntry
        }
        return emptyList()
    }

    private fun readFromToml(
        zip: JDKZipFile,
        tomlPath: String,
        loaderACC: Int,
        defaultLoader: ModLoader,
        modFile: File
    ): LocalMod {
        val tomlEntry = zip.getEntry(tomlPath) ?: throw IOException("TOML file $tomlPath not found")

        zip.getInputStream(tomlEntry).bufferedReader().use { reader ->
            val toml = Toml().read(reader.readText())
            val tomlMap = toml.toMap() as MutableMap<String, Any?>

            fixAuthorsField(tomlMap)

            val json = GSON.toJsonTree(tomlMap)
            val metadata = GSON.fromJson(json, ForgeNewModMetadata::class.java)
                ?: throw IOException("Failed to parse TOML metadata, file = $modFile")

            if (metadata.mods.isEmpty()) {
                throw IOException("Mod $modFile `$tomlPath` is malformed")
            }

            val mod = metadata.mods[0]
            val jarVersion = readVersion(zip, modFile)
            val resolvedVersion = mod.version.replace("\${file.jarVersion}", jarVersion ?: "")
            val loaderType = analyzeLoader(toml, mod.modId, loaderACC, defaultLoader)
            val icon = zip.tryGetIcon(metadata.logoFile)

            return LocalMod(
                modFile = modFile,
                fileSize = FileUtils.sizeOf(modFile),
                id = mod.modId,
                loader = loaderType,
                name = mod.displayName,
                description = mod.description,
                version = resolvedVersion,
                authors = mod.authors ?: emptyList(),
                icon = icon
            )
        }
    }

    private fun readVersion(zip: JDKZipFile, modFile: File): String? {
        val manifestEntry = zip.getEntry("META-INF/MANIFEST.MF") ?: return null

        return try {
            zip.getInputStream(manifestEntry).use { stream ->
                Manifest(stream).mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION)
            }
        } catch (e: Exception) {
            Logger.warning(TAG, "Failed to parse MANIFEST.MF in file $modFile", e)
            null
        }
    }

    private fun readWithApacheZip(modFile: File): LocalMod {
        val zipFile = ZipFile.builder()
            .setFile(modFile)
            .get()

        zipFile.use { zip ->
            runCatching {
                readFromTomlApache(
                    zip,
                    "META-INF/mods.toml",
                    ACC_FORGE or ACC_NEO_FORGED,
                    ModLoader.FORGE,
                    modFile
                )
            }.onSuccess { return it }

            runCatching {
                readFromTomlApache(
                    zip,
                    "META-INF/neoforge.mods.toml",
                    ACC_NEO_FORGED,
                    ModLoader.NEOFORGE,
                    modFile
                )
            }.onSuccess { return it }

            throw IOException("File $modFile is not a Forge 1.13+ or NeoForge mod.")
        }
    }

    private fun readFromTomlApache(
        zip: ZipFile,
        tomlPath: String,
        loaderACC: Int,
        defaultLoader: ModLoader,
        modFile: File
    ): LocalMod {
        val tomlEntry = zip.getEntry(tomlPath) ?: throw IOException("TOML file $tomlPath not found")

        zip.getInputStream(tomlEntry).bufferedReader().use { reader ->
            val toml = Toml().read(reader.readText())
            val tomlMap = toml.toMap() as MutableMap<String, Any?>

            fixAuthorsField(tomlMap)

            val json = GSON.toJsonTree(tomlMap)
            val metadata = GSON.fromJson(json, ForgeNewModMetadata::class.java)
                ?: throw IOException("Failed to parse TOML metadata, file = $modFile")

            if (metadata.mods.isEmpty()) {
                throw IOException("Mod $modFile `$tomlPath` is malformed")
            }

            val mod = metadata.mods[0]
            val jarVersion = readVersionApache(zip, modFile)
            val resolvedVersion = mod.version.replace("\${file.jarVersion}", jarVersion ?: "")
            val loaderType = analyzeLoader(toml, mod.modId, loaderACC, defaultLoader)
            val icon = zip.tryGetIcon(metadata.logoFile)

            return LocalMod(
                modFile = modFile,
                fileSize = FileUtils.sizeOf(modFile),
                id = mod.modId,
                loader = loaderType,
                name = mod.displayName,
                description = mod.description,
                version = resolvedVersion,
                authors = mod.authors ?: emptyList(),
                icon = icon
            )
        }
    }

    private fun readVersionApache(zip: ZipFile, modFile: File): String? {
        val manifestEntry = zip.getEntry("META-INF/MANIFEST.MF") ?: return null

        return try {
            zip.getInputStream(manifestEntry).use { stream ->
                Manifest(stream).mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION)
            }
        } catch (e: Exception) {
            Logger.warning(TAG, "Failed to parse MANIFEST.MF in file $modFile", e)
            null
        }
    }

    /**
     * 修复作者列表
     * 有的模组作者名称只是单个字符串；
     * 有的模组则是作者名称列表
     */
    @Suppress("UNCHECKED_CAST")
    private fun fixAuthorsField(map: MutableMap<String, Any?>) {
        val mods = map["mods"] as? List<MutableMap<String, Any?>> ?: return
        for (mod in mods) {
            val authors = mod["authors"]
            when (authors) {
                is String -> mod["authors"] = parseAuthors(authors)
                is List<*> -> {}
                else -> mod["authors"] = emptyList<String>()
            }
        }
    }

    private fun parseAuthors(authors: String?): List<String> {
        return authors?.split(',', ';', '&')?.map {
            it.trim()
        }?.filter {
            it.isNotBlank()
        } ?: emptyList()
    }

    /**
     * 分析模组加载器类型
     */
    private val FORGE_ENVIRONMENT_DEPENDENCIES = setOf(
        "minecraft",
        "forge",
        "neoforge",
        "javafml",
        "lowcodefml",
    )

    private fun analyzeLoader(
        toml: Toml,
        modID: String,
        loaderACC: Int,
        defaultLoader: ModLoader
    ): ModLoader {
        val depsArray = runCatching {
            toml.getTables("dependencies")
        }.getOrNull()

        val dependencies: List<Map<String, Any>>? = when (depsArray) {
            null -> { //尝试 dependencies.{modID}
                val depsTable = toml.getTable("dependencies")
                val modDepsArray = depsTable?.getTables(modID)
                modDepsArray?.map { it.toMap() as Map<String, Any> }
            }

            else -> {
                depsArray.map { it.toMap() as Map<String, Any> }
            }
        }

        fun checkLoaderACC(current: Int, target: Int, res: ModLoader): ModLoader {
            return if (target and current != 0) res else throw IOException("Mismatched loader")
        }

        dependencies?.forEach { dependency ->
            when (dependency["modId"] as? String) {
                "forge" -> return checkLoaderACC(loaderACC, ACC_FORGE, ModLoader.FORGE)
                "neoforge" -> return checkLoaderACC(loaderACC, ACC_NEO_FORGED, ModLoader.NEOFORGE)
            }
        }

        return defaultLoader
    }
}