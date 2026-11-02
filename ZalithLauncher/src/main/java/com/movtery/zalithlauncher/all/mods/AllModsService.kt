package com.movtery.zalithlauncher.all.mods

import com.movtery.zalithlauncher.game.version.mod.AllModReader
import com.movtery.zalithlauncher.game.version.mod.LocalMod
import com.movtery.zalithlauncher.game.version.mod.disableModFile
import com.movtery.zalithlauncher.game.version.mod.enableModFile
import com.movtery.zalithlauncher.game.version.mod.isEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File

/**
 * ALL 模组数据的唯一入口。
 *
 * 所有读写都交给 Zalith 既有实现：
 * - 读取元数据：[AllModReader]（它内部已经处理 Fabric / Forge / NeoForge 等格式与并发）
 * - 启用/禁用：[enableModFile] / [disableModFile]，即既有的 .disabled 可逆重命名语义
 * - 删除文件：与经典模组管理页相同的 FileUtils 删除
 *
 * 这里不复制模组安装器、不复制模组更新流程、也不建立第二份模组数据。
 */
class AllModsService(val modsDir: File) {

    /** 读取目录内所有模组。读不到元数据的文件由 AllModReader 标记为非模组后照样返回 */
    suspend fun list(): List<AllModItem> = withContext(Dispatchers.IO) {
        AllModReader(modsDir).readAllLocals()
            .map { it.toAllItem() }
            .sortedBy { it.fileName.lowercase() }
    }

    /** 目录内的文件数量。用来判断窗口重新打开时是否真的需要重读 */
    fun fileCount(): Int = modsDir.listFiles()?.count { !it.isDirectory } ?: 0

    /**
     * 启用或禁用单个模组。只重命名这一个文件，不重读整个列表（重读会扫全部 jar，卡顿明显）。
     *
     * @return 更新后的条目；已处于目标状态或重命名失败时返回 null
     */
    suspend fun setEnabled(item: AllModItem, enabled: Boolean): AllModItem? = withContext(Dispatchers.IO) {
        if (item.enabled == enabled) return@withContext null
        val target = if (enabled) item.file.enableModFile() else item.file.disableModFile()
        target?.let {
            item.copy(file = it, fileName = it.name, enabled = it.isEnabled())
        }
    }

    /**
     * 删除模组文件。删除不可逆，因此调用方必须先让用户确认。
     *
     * @return 文件确实被删除时为 true
     */
    suspend fun delete(item: AllModItem): Boolean = withContext(Dispatchers.IO) {
        FileUtils.deleteQuietly(item.file)
    }
}

private fun LocalMod.toAllItem(): AllModItem = AllModItem(
    file = file,
    fileName = file.name,
    name = name,
    id = id,
    version = version,
    loaderName = loader.displayName,
    sizeBytes = fileSize,
    enabled = file.isEnabled(),
    notMod = notMod,
    icon = icon
)
