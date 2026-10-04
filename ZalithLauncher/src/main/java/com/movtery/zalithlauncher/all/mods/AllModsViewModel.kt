package com.movtery.zalithlauncher.all.mods

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.File

/**
 * ALL 模组窗口的状态。
 *
 * 放在 ViewModel 里而不是窗口的 remember 里：最小化会把窗口内容移出组合，
 * 状态若放在组合内，每次还原都要重新扫一遍所有 jar。
 */
class AllModsViewModel(modsDir: File) : ViewModel() {
    private val service = AllModsService(modsDir)

    var items by mutableStateOf<List<AllModItem>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set

    /** 搜索关键字，只过滤已读到的列表，不重新读盘 */
    var filter by mutableStateOf("")

    /** 上一次读取时目录里的文件数量，-1 表示还没读过 */
    private var loadedCount = -1

    val enabledCount: Int get() = items.count { it.enabled }
    val disabledCount: Int get() = items.count { !it.enabled }

    val visibleItems: List<AllModItem>
        get() {
            val keyword = filter.trim()
            if (keyword.isEmpty()) return items
            return items.filter {
                it.name.contains(keyword, ignoreCase = true) ||
                    it.fileName.contains(keyword, ignoreCase = true)
            }
        }

    /** 完整重读一次目录 */
    fun load() {
        loadedCount = service.fileCount()
        loading = true
        viewModelScope.launch {
            items = service.list()
            loading = false
        }
    }

    /**
     * 窗口每次打开或还原时调用：只有目录内容数量变了才重读，
     * 否则直接沿用现有列表（与经典界面“打开时对一次数量”的做法一致）。
     */
    fun loadIfChanged() {
        if (loadedCount != service.fileCount()) load()
    }

    /** 启用/禁用。只改这一条，不重读列表 */
    fun toggle(item: AllModItem) {
        viewModelScope.launch {
            val updated = service.setEnabled(item, !item.enabled) ?: return@launch
            items = items.map { if (it.file == item.file) updated else it }
        }
    }

    /** 删除。失败时重读一次让界面回到真实状态，而不是留下一个已经不在的条目 */
    fun delete(item: AllModItem) {
        viewModelScope.launch {
            if (service.delete(item)) {
                items = items.filterNot { it.file == item.file }
                loadedCount = service.fileCount()
            } else {
                load()
            }
        }
    }
}
