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

import com.google.gson.annotations.SerializedName
import com.movtery.zalithlauncher.game.version.installed.VersionsManager.getZalithVersionPath
import com.movtery.zalithlauncher.game.download.modpack.install.ModFile
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.utils.GSON
import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File

private const val TAG = "ModpackPolicy"

/**
 * Modpack manifest file identity and its required-core classification.
 *
 * 安装整合包时按包内清单原样记录，因此 [sha1] / [url] 是“精确恢复身份”：
 * 文件缺失或被损坏时，只能按这份身份原样取回，不能改成同项目的其它版本。
 */
data class PackOwnedMod(
    @SerializedName("name") val name: String,
    @SerializedName("sha1") val sha1: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("requiredCore") val requiredCore: Boolean? = null,
)

/**
 * Modpack source file records. Only [PackOwnedMod.requiredCore] entries are enforced as core.
 *
 * @param modsComplete Whether the source file list covers all installed mod files. Kept as provenance only;
 *        an incomplete manifest never makes an unlisted file required core.
 */
data class ModpackManifest(
    @SerializedName("name") val name: String = "",
    @SerializedName("platform") val platform: String = "",
    @SerializedName("mods") val mods: List<PackOwnedMod> = emptyList(),
    @SerializedName("modsComplete") val modsComplete: Boolean = false,
)

/**
 * 整合包安全模式的判断依据
 *
 * - 实例不是整合包安装的（没有清单）：一切照旧，Mod Doctor 行为不变
 * - 实例是整合包安装的：只有明确标记为 required core 的文件受包规则保护
 */
class ModpackPolicy(
    val manifest: ModpackManifest?,
    private val coreDefinitionAvailable: Boolean = true,
) {
    val isPackManaged: Boolean get() = manifest != null

    private val requiredCoreByName: Map<String, PackOwnedMod> =
        manifest?.mods.orEmpty().filter { it.requiredCore == true }.associateBy { it.name }

    /**
     * 这个文件名是否属于整合包
     *
     * 只由来源清单明确标记为 required core 的文件属于核心；未列出的文件与可选文件可由用户自定义。
     */
    fun isPackOwned(modFileName: String): Boolean = requiredCoreByName.containsKey(modFileName)

    /** 整合包为这个文件记录的原始身份（未出现在清单里时为 null） */
    fun packOwnedMod(modFileName: String): PackOwnedMod? = requiredCoreByName[modFileName]

    /** 可以按精确身份恢复的整合包文件（有 url 也有 sha1 才算） */
    fun exactRestoreIdentity(modFileName: String): PackOwnedMod? =
        requiredCoreByName[modFileName]?.takeIf { !it.url.isNullOrBlank() && !it.sha1.isNullOrBlank() }

    /** 清单里有、但 mods 目录里已经找不到的整合包文件 */
    fun missingPackMods(modsDir: File): List<PackOwnedMod> {
        val present = runCatching {
            modsDir.listFiles().orEmpty().map { it.name }.toSet()
        }.getOrDefault(emptySet())
        return requiredCoreByName.values.filter { it.name !in present }
    }

    /** Missing required core and known SHA-1 constraint violations; optional files and extras are ignored. */
    fun requiredCoreProblems(modsDir: File): List<ModDoctorProblem> {
        if (!coreDefinitionAvailable) {
            return listOf(
                ModDoctorProblem(
                    type = ModDoctorProblemType.PACK_REQUIRED_CORE,
                    fileName = FILE_NAME,
                    modId = null,
                    reason = "Required modpack core could not be verified because the manifest has no explicit core classification.",
                )
            )
        }
        val installed = runCatching { modsDir.listFiles().orEmpty().filter { it.isFile }.associateBy { it.name } }
            .getOrDefault(emptyMap())
        return requiredCoreByName.values.mapNotNull { core ->
            val file = installed[core.name]
            val reason = when {
                file == null -> "Required modpack core file is missing."
                !core.sha1.isNullOrBlank() && file.sha1OrNull() != core.sha1 ->
                    "Required modpack core file does not match the recorded version."
                else -> return@mapNotNull null
            }
            ModDoctorProblem(
                type = ModDoctorProblemType.PACK_REQUIRED_CORE,
                fileName = core.name,
                modId = null,
                reason = reason,
                exactRestoreFile = if (file == null) core.takeIf { !it.url.isNullOrBlank() && !it.sha1.isNullOrBlank() } else null,
            )
        }
    }

    companion object {
        const val FILE_NAME = "modpack.json"

        /** 非整合包实例：没有任何限制 */
        val STANDALONE = ModpackPolicy(null)

        /** 读取实例的整合包清单；实例不是整合包安装的（或清单损坏）时返回 [STANDALONE] */
        fun load(versionPath: File): ModpackPolicy {
            val file = File(getZalithVersionPath(versionPath), FILE_NAME)
            if (!file.isFile) return STANDALONE
            val parsed = runCatching {
                GSON.fromJson(file.readText(), ModpackManifest::class.java)
            }
            val manifest = parsed.getOrNull()
            if (manifest == null) {
                parsed.exceptionOrNull()?.let { e ->
                    Logger.error(TAG, "Could not read $FILE_NAME; required modpack core cannot be verified", e)
                }
                return ModpackPolicy(ModpackManifest(), coreDefinitionAvailable = false)
            }
            return ModpackPolicy(
                manifest = manifest,
                coreDefinitionAvailable = manifest.mods.all { it.requiredCore != null },
            )
        }
    }
}

/** 整合包安全模式在问题描述里加的一句话（说明文案定义在 ModDoctor.kt，与安全模式过滤放在一起） */

/**
 * 包内清单里的模组文件 → 整合包文件身份
 *
 * 只记录 mods 目录下的文件：Mod Doctor 只会改动这些文件，配置文件/overrides 不参与自动修复判断。
 * CurseForge 清单不提供 hashes 与直链，因此这里可能是 null —— 仍然算整合包文件（按文件名保护），
 * 只是没有“精确恢复身份”。
 */
fun List<ModFile>.toPackOwnedMods(): List<PackOwnedMod> = mapNotNull { file ->
    val output = file.outputFile ?: return@mapNotNull null
    if (output.parentFile?.name != VersionFolders.MOD.folderName) return@mapNotNull null
    PackOwnedMod(
        name = output.name,
        sha1 = file.sha1,
        url = file.downloadUrls?.firstOrNull(),
        requiredCore = file.requiredCore,
    )
}

private fun File.sha1OrNull(): String? = runCatching {
    val digest = java.security.MessageDigest.getInstance("SHA-1")
    inputStream().buffered().use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}.getOrNull()

/**
 * 记录 pack source metadata, including explicit required-core classification.
 *
 * @param versionPath 已经完成安装的实例目录
 * @param manifest 包内清单
 * @param modsDir 实例的 mods 目录，用于判断清单是否覆盖了全部模组文件
 * @return 实际写入的清单（[ModpackManifest.modsComplete] 可能因 overrides 里的模组文件被置为 false）
 */
fun recordModpackManifest(
    versionPath: File,
    manifest: ModpackManifest,
    modsDir: File,
): ModpackManifest {
    val listed = manifest.mods.map { it.name }.toSet()
    val extra = runCatching {
        modsDir.listFiles().orEmpty()
            .filter { it.isFile && it.extension.lowercase() in setOf("jar", "zip", "litemod") }
            .count { it.name !in listed }
    }.getOrDefault(0)

    val recorded = manifest.copy(modsComplete = manifest.modsComplete && extra == 0)
    runCatching {
        val folder = getZalithVersionPath(versionPath)
        folder.mkdirs()
        File(folder, ModpackPolicy.FILE_NAME).writeText(GSON.toJson(recorded))
        Logger.info(
            TAG,
            "Recorded modpack manifest: ${recorded.name}, ${recorded.mods.size} mod(s), complete=${recorded.modsComplete}"
        )
    }.onFailure { e ->
        Logger.error(TAG, "Could not record the modpack manifest", e)
    }
    return recorded
}
