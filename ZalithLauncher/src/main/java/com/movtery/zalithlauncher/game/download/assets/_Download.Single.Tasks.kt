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
    onFileCopied: suspend (zip: File, folder: File) -> Unit = { _, _ -> },
    onFileCancelled: (zip: File, folder: File) -> Unit = { _, _ -> },
    onFileRejected: (fileName: String, reason: String) -> Unit = { _, _ -> },
    stageOnly: Boolean = false,
    onFileStaged: suspend (zip: File, folder: File) -> Unit = { _, _ -> },
    submitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val fileKey = version.platformSha1() ?: version.platformFileName()
    val cacheFile = File(File(PathManager.DIR_CACHE, "assets"), fileKey)

    downloadSingleFile(
        version = version,
        taskId = downloadTaskId(fileKey, versions),
        file = cacheFile,
        onDownloaded = { task ->
            //暂存位上的校验：未通过的候选在这里就被丢弃，不会被复制进版本目录
            //（暂存文件由 onFinally 清理）
            val rejection = validate(cacheFile)
            if (rejection == null && stageOnly) {
                //只暂存：文件留给调用方，版本目录与暂存文件的生命周期都由调用方接管
                onFileStaged(cacheFile, File(PathManager.DIR_CACHE, "assets"))
            } else if (rejection == null) {
                task.updateProgress(-1f)
                task.updateMessage(androidText(R.string.download_assets_install_progress_installing, version.platformFileName()))
                versions.forEach { ver ->
                    val targetFolder = File(ver.getGameDir(), folder)
                    val targetFile = File(targetFolder, version.platformFileName())
                    if (targetFile.exists() && !targetFile.delete()) throw IOException("Failed to properly delete the existing target file.")
                    cacheFile.copyTo(targetFile)
                    onFileCopied(targetFile, targetFolder) //文件已复制回调
                }
            } else {
                Logger.info(TAG, "Rejected the staged candidate ${version.platformFileName()}: $rejection")
                onFileRejected(version.platformFileName(), rejection)
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
                versions.forEach { ver ->
                    val targetFolder = File(ver.getGameDir(), folder)
                    val targetFile = File(targetFolder, version.platformFileName())
                    if (targetFile.exists()) FileUtils.deleteQuietly(targetFile)
                    onFileCancelled(targetFile, targetFolder) //文件已取消回调
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
        is HttpRequestTimeoutException -> androidText(R.string.error_timeout)
        is UnknownHostException, is UnresolvedAddressException -> androidText(R.string.error_network_unreachable)
        is ConnectException -> androidText(R.string.error_connection_failed)
        is ResponseException -> e.toLocal()
        else -> {
            androidText(e.localizedMessage ?: e::class.simpleName ?: "Unknown error")
        }
    }
}