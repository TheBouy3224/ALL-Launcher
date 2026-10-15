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

package com.movtery.zalithlauncher.game.download.assets

import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion
import com.movtery.zalithlauncher.game.download.assets.platform.mcim.mapMCIMMirrorUrls
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.utils.file.calculateFileSha1
import com.movtery.zalithlauncher.utils.file.ensureParentDirectory
import com.movtery.zalithlauncher.utils.file.formatFileSize
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.network.downloadFileFromSources
import com.movtery.zalithlauncher.utils.network.toLocal
import com.movtery.zalithlauncher.utils.network.withSpeedReport
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import okio.IOException
import org.apache.commons.io.FileUtils
import java.io.File
import java.net.ConnectException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import java.util.UUID

private const val TAG = "DownloadSingle"

/**
 * 为一些版本下载单独的资源文件
 * @param version 要下载单独资源版本信息
 * @param versions 为哪些游戏版本下载
 * @param folder 版本游戏目录下的相对路径
 * @param validate 复制进版本目录前对暂存文件的校验：返回原因即拒绝该候选，不安装
 * @param onFileCopied 文件已成功复制到版本游戏目录后 单独回调
 * @param onFileCancelled 文件安装已取消 单独回调
 * @param onFileRejected 候选文件在暂存阶段被拒绝时的回调（该文件不会进入版本目录）
 * @param stageOnly 只暂存：下载与校验完成后把暂存文件交给 [onFileStaged]，不复制、不清理、不动版本目录；
 *                  默认false 保持既有行为
 * @param onFileStaged 暂存完成回调（仅 stageOnly 模式使用）；文件由调用方接管并负责清理
 */
fun downloadSingleForVersions(
    version: PlatformVersion,
    versions: List<Version>,
    folder: String,
    validate: suspend (zip: File) -> String? = { null },
    onFileCopied: suspend (zip: File, folder: File, install: StagedInstall) -> Unit = { _, _, _ -> },
    onFileCancelled: (zip: File, folder: File) -> Unit = { _, _ -> },
    onFileRejected: (fileName: String, reason: String) -> Unit = { _, _ -> },
    stageOnly: Boolean = false,
    onFileStaged: suspend (zip: File, folder: File) -> Unit = { _, _ -> },
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val fileKey = version.platformSha1() ?: version.platformFileName()
    val cacheFile = File(File(PathManager.DIR_CACHE, "assets"), fileKey)
    //本次任务实际写入过的版本目录文件：取消或失败时只回滚这些，不碰任务开始前就存在的文件
    val installs = mutableListOf<StagedInstall>()

    downloadSingleFile(
        version = version,
        taskId = downloadTaskId(fileKey, versions),
        file = cacheFile,
        onDownloaded = { task ->
            //暂存位上的校验：未通过的候选在这里就被丢弃，不会被复制进版本目录
            //（暂存文件由 onFinally 清理）
            val callerRejection = validate(cacheFile)
            val rejection = callerRejection
                ?: unreadableArchiveReason(cacheFile, version.platformFileName(), hasSha1 = version.platformSha1() != null)
            if (rejection == null && stageOnly) {
                //只暂存：文件留给调用方，版本目录与暂存文件的生命周期都由调用方接管
                onFileStaged(cacheFile, File(PathManager.DIR_CACHE, "assets"))
            } else if (rejection == null) {
                task.updateProgress(-1f)
                task.updateMessage(androidText(R.string.download_assets_install_progress_installing, version.platformFileName()))
                val targets = versions.map { ver ->
                    val targetFolder = File(ver.getGameDir(), folder)
                    targetFolder to File(targetFolder, version.platformFileName())
                }
                //任何一步失败（安装或回调）都只还原本次写入的文件，已存在的同名文件照原样保留
                installStagedToTargets(cacheFile, targets, installs, onFileCopied)
            } else {
                Logger.info(TAG, "Rejected the staged candidate ${version.platformFileName()}: $rejection")
                //被拒绝的暂存文件不留在缓存里（只暂存模式下 onFinally 不清理）
                FileUtils.deleteQuietly(cacheFile)
                onFileRejected(version.platformFileName(), rejection)
                //调用方自己的校验拒绝由它自己处理；下载器的完整性检查拒绝必须让用户看到失败，不能无声消失
                if (callerRejection == null) {
                    submitError(
                        ErrorViewModel.ThrowableMessage(
                            title = androidText(R.string.download_assets_install_failed),
                            message = androidText(rejection)
                        )
                    )
                }
            }
        },
        onError = { e ->
            Logger.warning(TAG, "An error occurred while downloading the resource files.", e)

            submitError(
                ErrorViewModel.ThrowableMessage(
                    title = androidText(R.string.download_assets_install_failed),
                    message = mapExceptionToMessage(e)
                )
            )
        },
        onCancel = {
            FileUtils.deleteQuietly(cacheFile)
            if (stageOnly) {
                //只暂存：没有版本目录里的目标文件要清理
                onFileCancelled(cacheFile, File(PathManager.DIR_CACHE, "assets"))
            } else {
                //取消只还原本次任务写入的文件；任务开始前已存在的同名文件保持原样
                rollbackStagedInstalls(installs)
                installs.clear()
                versions.forEach { ver ->
                    val targetFolder = File(ver.getGameDir(), folder)
                    onFileCancelled(File(targetFolder, version.platformFileName()), targetFolder) //文件已取消回调
                }
            }
        },
        onFinally = {
            if (!stageOnly) {
                Logger.info(TAG, "Attempting to clear cached resource files.")
                FileUtils.deleteQuietly(cacheFile)
            }
        }
    )
}

/**
 * 平台没有给出 sha1 时，下载没有内容校验：被截断的流或服务器返回的 200 错误页都会“下载成功”。
 * 对 jar/zip 文件至少要求能作为压缩包打开，否则拒绝安装。有 sha1 时内容已由下载器校验，不重复检查。
 * @return 拒绝原因；null 表示可以继续
 */
internal fun unreadableArchiveReason(file: File, fileName: String, hasSha1: Boolean): String? {
    if (hasSha1) return null
    val lower = fileName.lowercase()
    if (!lower.endsWith(".jar") && !lower.endsWith(".zip")) return null
    return try {
        java.util.zip.ZipFile(file).use { if (it.size() == 0) "The downloaded archive is empty." else null }
    } catch (e: java.io.IOException) {
        "The downloaded file is not a readable archive."
    }
}

/**
 * 一次写入版本目录的记录：[written] 为 false 表示目标已是相同内容、没有改动；
 * [backup] 是被替换前的旧文件，已改名保留（从不删除），没有则 null
 */
class StagedInstall(
    val target: File,
    /** 被替换前的旧文件（已改名保留）；解压成功后会被改回原名并清空 */
    var backup: File?,
    /** 本次任务是否写入了 [target]：只有 true 时 [target] 才归本次任务所有，可以在失败时删除 */
    var written: Boolean,
)

/**
 * 解压等回调成功后，把本次写入的文件交还给用户：本次写入的新文件已被消费（例如解压后删除 zip），
 * 把任务开始前的旧文件改回原名，并把记录改为“不再拥有”，这样之后的回滚不会误删用户的旧文件。
 */
internal fun StagedInstall.restoreKeptFile() {
    if (!written) return
    val kept = backup
    if (kept != null) {
        if (target.exists() && !target.delete()) throw IOException("Could not remove the newly written file.")
        if (!kept.renameTo(target)) throw IOException("Could not restore the kept file: ${kept.name}")
    }
    backup = null
    written = false
}

/**
 * 把暂存文件安装到每个 (文件夹, 目标) 上，然后逐个执行 [onFileCopied]。
 * 任何一步失败都会回滚本次写入的文件（[rollbackStagedInstalls]），已有的同名文件照原样保留。
 */
internal suspend fun installStagedToTargets(
    staged: File,
    targets: List<Pair<File, File>>,
    installs: MutableList<StagedInstall>,
    onFileCopied: suspend (zip: File, folder: File, install: StagedInstall) -> Unit,
) {
    try {
        val results = targets.map { (folder, file) ->
            Triple(file, folder, installStagedFile(staged, file).also { installs += it })
        }
        results.forEach { (file, folder, install) -> onFileCopied(file, folder, install) }
    } catch (e: Throwable) {
        rollbackStagedInstalls(installs)
        installs.clear()
        throw e
    }
}

/**
 * 把暂存文件安装到 [target]。失败时 [target] 与旧文件都保持原样：
 * 1. 目标已是相同内容 → 不写入
 * 2. 先复制到同目录的临时文件，并核对 sha1 与暂存文件一致
 * 3. 旧文件改名为 `.bak`（不删除），再把临时文件改名到位；改名失败时还原旧文件
 */
internal suspend fun installStagedFile(staged: File, target: File): StagedInstall {
    val expected = calculateFileSha1(staged)
    if (target.isFile && calculateFileSha1(target) == expected) return StagedInstall(target, null, written = false)

    val parent = target.parentFile ?: throw IOException("The target folder is invalid.")
    parent.mkdirs()
    val temp = File(parent, ".${target.name}.part-${UUID.randomUUID()}")
    try {
        staged.copyTo(temp)
        if (calculateFileSha1(temp) != expected) throw IOException("The copied file does not match the staged file.")
        val backup = if (target.exists()) {
            uniqueBackupFile(target).also { if (!target.renameTo(it)) throw IOException("Could not keep the existing file aside.") }
        } else null
        if (!temp.renameTo(target)) {
            backup?.renameTo(target)
            throw IOException("Could not move the new file into place.")
        }
        return StagedInstall(target, backup, written = true)
    } finally {
        if (temp.exists()) temp.delete()
    }
}

/** 还原本次写入的文件：删除我们写入的新文件，再把保留下来的旧文件改回原名；只处理 [StagedInstall.written] 的记录 */
internal fun rollbackStagedInstalls(installs: List<StagedInstall>) {
    installs.asReversed().filter { it.written }.forEach { install ->
        if (install.target.exists() && !install.target.delete()) {
            Logger.warning(TAG, "Could not remove the newly written file while rolling back: ${install.target.name}")
            return@forEach
        }
        install.backup?.let { backup ->
            if (backup.exists() && !backup.renameTo(install.target)) {
                Logger.warning(TAG, "Could not restore the kept file: ${backup.name}")
            }
        }
    }
}

private fun uniqueBackupFile(target: File): File {
    val parent = target.parentFile
    var candidate = File(parent, "${target.name}.bak")
    var index = 1
    while (candidate.exists()) candidate = File(parent, "${target.name}.bak$index").also { index++ }
    return candidate
}

/**
 * 下载任务的Id
 * 同一文件安装到不同的游戏版本时属于不同的任务，避免被误判为重复任务而丢弃目标版本
 */
private fun downloadTaskId(fileKey: String, versions: List<Version>): String {
    if (versions.isEmpty()) return fileKey
    return "$fileKey|${versions.map { it.getVersionName() }.sorted().joinToString(",")}"
}

private fun downloadSingleFile(
    version: PlatformVersion,
    taskId: String,
    file: File,
    onDownloaded: suspend (Task) -> Unit,
    onError: (Throwable) -> Unit = {},
    onCancel: () -> Unit = {},
    onFinally: () -> Unit = {}
) {
    TaskSystem.submitTask(
        Task.runTask(
            id = taskId,
            task = { task ->
                val totalFileSize = version.platformFileSize()
                var downloadedSize = 0L

                //更新下载任务进度
                fun updateProgress() {
                    task.updateProgress(
                        (downloadedSize.toDouble() / totalFileSize.toDouble()).toFloat()
                    )
                    task.updateMessage(
                        androidText(
                            R.string.download_assets_install_progress_downloading,
                            version.platformFileName(),
                            formatFileSize(downloadedSize),
                            formatFileSize(totalFileSize),
                        )
                    )
                }
                updateProgress()

                withSpeedReport(
                    onSpeedReport = { bytes ->
                        task.updateSpeed(bytes)
                    },
                    onClear = {
                        task.clearSpeed()
                    }
                ) { report ->
                    downloadFileFromSources(
                        urls = version
                            .platformDownloadUrl()
                            .mapMCIMMirrorUrls(),
                        sha1 = version.platformSha1(),
                        outputFile = file.ensureParentDirectory(),
                        sizeCallback = { size ->
                            downloadedSize += size
                            updateProgress()
                            report(size)
                        }
                    )
                }

                onDownloaded(task)
            },
            onError = onError,
            onCancel = onCancel,
            onFinally = onFinally
        )
    )
}

fun mapExceptionToMessage(e: Throwable): AndroidStringText {
    return when (e) {
        is HttpRequestTimeoutException, is java.util.concurrent.TimeoutException -> androidText(R.string.error_timeout)
        is UnknownHostException, is UnresolvedAddressException -> androidText(R.string.error_network_unreachable)
        is ConnectException -> androidText(R.string.error_connection_failed)
        is ResponseException -> e.toLocal()
        else -> {
            androidText(e.localizedMessage ?: e::class.simpleName ?: "Unknown error")
        }
    }
}