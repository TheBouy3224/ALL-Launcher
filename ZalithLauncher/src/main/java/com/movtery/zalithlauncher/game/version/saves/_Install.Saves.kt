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

package com.movtery.zalithlauncher.game.version.saves

import com.movtery.zalithlauncher.game.download.assets.StagedInstall
import com.movtery.zalithlauncher.game.download.assets.restoreKeptFile
import com.movtery.zalithlauncher.utils.file.CompressZipEntryAdapter
import com.movtery.zalithlauncher.utils.file.JavaZipEntryAdapter
import com.movtery.zalithlauncher.utils.file.UnpackZipException
import com.movtery.zalithlauncher.utils.file.ZipEntryBase
import com.movtery.zalithlauncher.utils.file.extractFromZip
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile as JDKZipFile

private const val TAG = "InstallSaves"

/**
 * 解压存档压缩包
 */
suspend fun unpackSaveZip(
    zipFile: File,
    targetPath: File,
    /** 解压成功后是否删除 zip；由下载层传入 false 时，不属于本次任务的 zip 必须原样保留 */
    removeZipWhenDone: Boolean = true,
): File = withContext(Dispatchers.IO) {
    val target = File(targetPath, zipFile.nameWithoutExtension)
    //只会写入一个全新的世界目录：同名世界（无论是谁的）一律拒绝，绝不合并或覆盖
    //mkdir 是原子占位：两个任务同时解压同名世界时，只有一个能占到目录
    targetPath.mkdirs()
    if (!target.mkdir()) throw IOException(worldNameTakenMessage(target.name, targetPath.path))
    try {
        val path = extractLevelPath(zipFile) ?: throw IOException("Unable to locate the level where the level.dat file is stored.")
        Logger.info(TAG, "Found the level of the level.data file: $path")
        try {
            JDKZipFile(zipFile).use { zip ->
                zip.extractFromZip(path, target)
                Logger.info(TAG, "Decompression is complete")
            }
        } catch (e: Exception) {
            if (e !is UnpackZipException) {
                tryApacheZip(zipFile, path, target)
            } else {
                throw e
            }
        }
    } catch (e: Throwable) {
        //目标在开始前不存在，所以失败时删除的只会是本次解压创建的半成品；zip 不删除
        FileUtils.deleteQuietly(target)
        throw e
    }
    if (removeZipWhenDone) FileUtils.deleteQuietly(zipFile)
    target
}

/**
 * 一次下载在多个实例里解压同一个世界。只记录本次 [unpack] 通过 mkdir 占位创建的世界目录：
 * 之后任一实例失败，只删除这些目录（它们由本次任务创建，不会碰任何已存在的世界）。
 * zip 的回滚由下载层（[installStagedToTargets]）负责，这里不删除 zip。
 */
class SaveWorldBatch {
    private val created = mutableListOf<File>()

    /** 解压一个实例的世界，成功后把 zip 交还给用户（见 [restoreKeptFile]）；失败时撤销本批次已创建的世界 */
    suspend fun unpack(zip: File, folder: File, install: StagedInstall) {
        try {
            created += unpackSaveZip(zip, folder, removeZipWhenDone = install.written)
            install.restoreKeptFile()
        } catch (e: Throwable) {
            val left = undo()
            if (left.isNotEmpty()) {
                throw IOException(
                    "Installation failed and ${left.size} partly installed world(s) could not be removed: " +
                            left.joinToString { it.path } + ". No existing world was touched."
                )
            }
            throw e
        }
    }

    /** 删除本批次创建的世界目录，返回仍然删不掉的目录（用于报告部分成功） */
    fun undo(): List<File> {
        val left = created.asReversed().filter { !it.deleteRecursively() && it.exists() }
        created.clear()
        return left
    }
}

/** 同名世界已存在时的统一提示；创建下载前检查和解压时检查共用，保证文案一致 */
fun worldNameTakenMessage(worldName: String, location: String): String =
    "A world named \"$worldName\" already exists in $location. Rename or delete it first."

/**
 * 在 [targets] 的 saves 目录里找到已存在同名世界的那个版本（没有则 null）。
 * 只读检查：用于开始下载前就拒绝，真正的保护仍由 [unpackSaveZip] 在解压时再检查一次。
 */
fun findExistingWorldTarget(worldName: String, targets: List<File>): File? =
    targets.firstOrNull { File(it, worldName).exists() }

private suspend fun tryApacheZip(zipFile: File, path: String, target: File) {
    FileUtils.deleteQuietly(target) //清除一次目标文件夹（如果之前解压出错）
    val zipFile1 = ZipFile.Builder()
        .setFile(zipFile)
        .get()

    zipFile1.use { zip ->
        zip.extractFromZip(path, target)
        Logger.info(TAG, "Decompression is complete")
    }
}

/**
 * 读取zip文件，并找到level.data文件所在的路径
 * @param file 压缩包文件
 */
private fun extractLevelPath(file: File): String? {
    if (!file.exists() || !file.isFile) {
        return null
    }

    if (!file.name.endsWith(".zip", ignoreCase = true)) {
        return null
    }

    try {
        JDKZipFile(file).use { zip ->
            val entries = zip.entries().asSequence()
                .map { entry ->
                    JavaZipEntryAdapter(entry)
                }
            return findLevelEntryName(entries)
        }
    } catch (e: Exception) {
        if (e !is UnpackZipException) {
            return extractLevelPathCompress(file)
        } else {
            throw e
        }
    }
}

private fun extractLevelPathCompress(file: File): String? {
    val zipFile = ZipFile.Builder()
        .setFile(file)
        .get()

    zipFile.use { zip ->
        val entries = zip.entries.asSequence()
            .map { entry ->
                CompressZipEntryAdapter(entry)
            }
        return findLevelEntryName(entries)
    }
}

private fun <T : ZipEntryBase> findLevelEntryName(
    entries: Sequence<T>
): String? {
    val allEntries = entries.toList()
    var currentPrefix = ""

    while (true) {
        val inLayer = allEntries.filter {
            //过滤当前层级内的所有内容
            it.name.startsWith(currentPrefix) && it.name != currentPrefix
        }
        if (inLayer.isEmpty()) {
            //当前层无任何有效内容，这是一个无效的存档格式
            return null
        }

        //检查当前层级是否存在 level.dat
        val hasLevelDat = inLayer.any {
            val relative = it.name.removePrefix(currentPrefix)
            !it.isDirectory && relative.equals("level.dat", ignoreCase = true)
        }

        if (hasLevelDat) {
            //存在 level.dat 文件时，必须至少有一个文件夹
            //否则认为这是一个无效的存档格式
            val hasFolder = inLayer.any {
                val relative = it.name.removePrefix(currentPrefix)
                it.isDirectory || relative.contains("/")
            }
            return if (hasFolder) currentPrefix.removeSuffix("/") else null
        }

        //不存在 level.dat 时，只有在当前层级只有一个目录且没有文件的情况下才深入
        val relativeNames = inLayer.map {
            it.name.removePrefix(currentPrefix)
        }

        val topLevelNames = relativeNames.map {
            it.substringBefore("/")
        }.distinct()

        if (topLevelNames.size == 1) {
            val name = topLevelNames[0]
            val isFolder = inLayer.any {
                val rel = it.name.removePrefix(currentPrefix)
                rel.startsWith("$name/") || (rel == name && it.isDirectory)
            }
            val hasFileWithSameName = inLayer.any {
                val rel = it.name.removePrefix(currentPrefix)
                !it.isDirectory && rel == name
            }

            if (isFolder && !hasFileWithSameName) {
                currentPrefix = "$currentPrefix$name/"
                continue
            }
        }
        return null
    }
}
