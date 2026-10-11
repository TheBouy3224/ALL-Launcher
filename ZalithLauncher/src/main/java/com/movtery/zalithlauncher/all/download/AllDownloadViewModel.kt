package com.movtery.zalithlauncher.all.download

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movtery.zalithlauncher.all.bridge.AllSubmitError
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSortField
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion
import com.movtery.zalithlauncher.game.download.game.GameInstaller
import com.movtery.zalithlauncher.game.download.modpack.install.ModPackInstaller
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersion
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersions
import com.movtery.zalithlauncher.all.bridge.allInitFiles
import com.movtery.zalithlauncher.path.PathManager
import org.apache.commons.io.FileUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import java.io.IOException

/** ALL-owned Download state. Provider, installer and task implementations stay in Zalith. */
/** 每页条数：与 Zalith 搜索的默认 limit 一致 */
private const val PAGE_LIMIT = 20

class AllDownloadViewModel(
    private val context: Context,
    submitError: AllSubmitError
) : ViewModel() {
    private val service = AllDownloadService(submitError, context)
    //每个分类各有一个请求与序号：切换分类不会取消别的分类的请求，也不会让它们互相覆盖；
    //同一分类里换 Provider / 关键词 / 页码时，旧请求被取消，其过期结果按序号丢弃
    private val searchJobs = mutableMapOf<AllDownloadCategory, Job>()
    private val searchSeqs = mutableMapOf<AllDownloadCategory, Long>()
    private var lastSession = -1
    private var lastRequestedCategory: AllDownloadCategory? = null
    private var gameInstaller: GameInstaller? = null
    private var modpackInstaller: ModPackInstaller? = null
    private var mobileDataDecision: CompletableDeferred<Boolean>? = null

    var state by mutableStateOf(AllDownloadState())
        private set

    init {
        state = state.copy(
            categories = AllDownloadCategory.entries.associateWith { category ->
                AllDownloadCategoryState(platform = service.defaultPlatform(category))
            },
            installedVersions = service.installedVersions(),
            gameVersions = service.gameVersions(),
            gameVersionName = VersionsManager.currentVersion.value?.getVersionName().orEmpty()
        )
        viewModelScope.launch {
            VersionsManager.versions.collect { versions -> state = state.copy(installedVersions = versions) }
        }
        viewModelScope.launch {
            VersionsManager.currentVersion.collect { version ->
                state = state.copy(gameVersionName = version?.getVersionName().orEmpty())
            }
        }
        viewModelScope.launch {
            runCatching { MinecraftVersions.refreshVersions(force = false) }
            MinecraftVersions.allVersions.collect { versions -> state = state.copy(gameVersions = versions) }
        }
        viewModelScope.launch {
            TaskSystem.tasksFlow.collect { tasks ->
                state = state.copy(runningTaskIds = tasks.map { it.id }, tasks = tasks)
            }
        }
    }

    /** 切换分类后自动加载该分类的推荐列表（不需要输入或按搜索） */
    fun selectCategory(category: AllDownloadCategory) {
        state = state.copy(category = category, progressMessage = if (state.busy) state.progressMessage else null)
        ensureBrowsed()
    }

    /** 只有可在线浏览的分类才自动加载：游戏版本有自己的列表，ID 搜索需要明确输入 */
    private fun browseCurrent() {
        val category = state.category
        if (category == AllDownloadCategory.GAME || category == AllDownloadCategory.BY_ID) return
        if (current().route != AllDownloadRoute.BROWSE) return
        search(0)
    }

    /**
     * 窗口进入组合时调用。[session] 在“关闭”时递增、在“最小化”时不变：
     * 关闭后重新打开 -> 清掉所有分类的旧搜索并显示 [category] 的推荐列表；
     * 最小化后还原 -> 保持原有的关键词、页码和结果，已有结果不会再次请求。
     */
    fun openWindow(category: AllDownloadCategory, session: Int) {
        val reopened = session != lastSession
        if (reopened) {
            lastSession = session
            resetDiscovery()
        }
        //只有重开或入口指定了新分类才改分类；最小化后还原保持用户当时所在的分类
        val newRequest = reopened || category != lastRequestedCategory
        lastRequestedCategory = category
        if (newRequest && state.category != category) state = state.copy(category = category, progressMessage = if (state.busy) state.progressMessage else null)
        ensureBrowsed()
    }

    private fun resetDiscovery() {
        searchJobs.values.forEach { it.cancel() }
        searchJobs.clear()
        state = state.copy(
            categories = state.categories.mapValues { (_, c) ->
                AllDownloadCategoryState(platform = c.platform, idSearchClass = c.idSearchClass)
            },
            selectedGameVersion = null
        )
    }

    private fun ensureBrowsed() {
        val category = state.category
        if (category == AllDownloadCategory.GAME || category == AllDownloadCategory.BY_ID) return
        val current = current()
        if (current.route != AllDownloadRoute.BROWSE) return
        if (current.loading || current.projects.isNotEmpty() || current.error != null || current.searched) return
        search(current.pageIndex)
    }

    fun refreshTargets() {
        VersionsManager.refresh("ALL Download")
    }

    fun current(): AllDownloadCategoryState = state.categories[state.category]
        ?: AllDownloadCategoryState(platform = service.defaultPlatform(state.category))

    fun updateQuery(query: String) {
        val wasBlank = current().query.isBlank()
        updateCategory { it.copy(query = query, error = null, searched = false) }
        //清空搜索框后立即回到推荐列表；输入中的文字仍然要按搜索按钮才会查询
        if (query.isBlank() && !wasBlank) browseCurrent()
    }

    fun selectPlatform(platform: Platform) {
        service.savePlatform(state.category, platform)
        updateCategory { it.copy(platform = platform, error = null) }
        browseCurrent()
    }

    fun selectIdClass(platformClass: PlatformClasses) = updateCategory { it.copy(idSearchClass = platformClass) }

    /**
     * 加载一页：有关键词则按相关度搜索，没有关键词则按下载量给出热门列表（同一 API、同一分页）。
     * [page] 是页码（从 0 开始）。Zalith 的索引是条目偏移量，每页 [PAGE_LIMIT] 条，这里换算后发送。
     * 每次加载取消上一次未完成的请求，并用序号丢弃过期结果，避免旧分类或旧 Provider 的数据覆盖新请求。
     */
    fun search(page: Int = 0) {
        val category = state.category
        if (category == AllDownloadCategory.GAME || category == AllDownloadCategory.BY_ID) return
        val current = current()
        val query = current.query.trim()
        val platform = current.platform
        val seq = (searchSeqs[category] ?: 0L) + 1
        searchSeqs[category] = seq
        searchJobs[category]?.cancel()
        updateCategory(category) { it.copy(loading = true, error = null, pageIndex = page, route = AllDownloadRoute.BROWSE, searched = false, projects = emptyList(), totalPages = 0) }
        searchJobs[category] = viewModelScope.launch {
            runCatching {
                withTimeout(60_000) {
                    service.search(
                        category = category,
                        platform = platform,
                        query = query,
                        pageIndex = page * PAGE_LIMIT,
                        gameVersion = "",
                        sortField = if (query.isEmpty()) PlatformSortField.DOWNLOADS else PlatformSortField.RELEVANCE
                    )
                }
            }.onSuccess { result ->
                if (seq == searchSeqs[category]) updateCategory(category) {
                    it.copy(
                        loading = false,
                        projects = result.projects,
                        totalPages = result.totalPages,
                        pageIndex = result.currentPage / PAGE_LIMIT,
                        searched = true,
                        error = null
                    )
                }
            }.onFailure { error ->
                if (seq == searchSeqs[category]) updateCategory(category) { it.copy(loading = false, error = error.message ?: error.toString()) }
            }
        }
    }

    fun searchById() {
        val category = state.category
        val current = current()
        if (current.query.isBlank()) return
        updateCategory(category) { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { withTimeout(60_000) { service.projectById(current.query.trim(), current.platform) } }
                .onSuccess { project ->
                    val item = service.projectSummary(project)
                    updateCategory(category) { it.copy(loading = false, project = project, versions = emptyList(), route = AllDownloadRoute.PROJECT) }
                    loadVersions(item, category)
                }
                .onFailure { error -> updateCategory(category) { it.copy(loading = false, error = error.message ?: error.toString()) } }
        }
    }

    fun openProject(item: AllDownloadProject) {
        val category = state.category
        updateCategory(category) { it.copy(loading = true, error = null, route = AllDownloadRoute.PROJECT) }
        viewModelScope.launch {
            runCatching { withTimeout(60_000) { service.project(item) } }
                .onSuccess { project ->
                    updateCategory(category) { it.copy(loading = false, project = project, versions = emptyList(), selectedVersion = null) }
                    loadVersions(item, category)
                }
                .onFailure { error -> updateCategory(category) { it.copy(loading = false, error = error.message ?: error.toString()) } }
        }
    }

    private fun loadVersions(item: AllDownloadProject, category: AllDownloadCategory) {
        updateCategory(category) { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val allVersions = withTimeout(60_000) { service.versions(item).allInitFiles(item.projectId) } // same as Classic: initFile + drop files-less versions
                val targets = state.targetVersionNames.mapNotNull { name ->
                    state.installedVersions.find { it.getVersionName() == name }
                }.ifEmpty {
                    listOfNotNull(VersionsManager.currentVersion.value)
                }
                val selectedClass = if (category == AllDownloadCategory.BY_ID) state.categories[category]?.idSearchClass else category.assetClass
                if (allVersions.isEmpty()) throw IOException("No downloadable file found for this project.")
                service.compatibleVersions(category, allVersions, targets, selectedClass)
            }
                .onSuccess { versions -> updateCategory(category) { it.copy(loading = false, versions = versions) } }
                .onFailure { error -> updateCategory(category) { it.copy(loading = false, error = error.message ?: error.toString()) } }
        }
    }

    fun selectVersion(version: PlatformVersion) = updateCategory { it.copy(selectedVersion = version) }

    fun back(): Boolean {
        val current = current()
        if (state.targetChooserOpen) {
            state = state.copy(targetChooserOpen = false)
            return true
        }
        if (state.installConfirmationOpen) {
            state = state.copy(installConfirmationOpen = false)
            return true
        }
        if (current.route != AllDownloadRoute.BROWSE) {
            updateCategory { it.copy(route = AllDownloadRoute.BROWSE, project = null, versions = emptyList(), selectedVersion = null, error = null) }
            state = state.copy(progressMessage = null)
            if (state.category == AllDownloadCategory.GAME) state = state.copy(selectedGameVersion = null)
            return true
        }
        return false
    }

    fun closeTargetChooser() {
        state = state.copy(targetChooserOpen = false)
    }

    fun closeModpackInstall() {
        state = state.copy(installConfirmationOpen = false)
    }

    /** 由实例窗口打开下载时指定安装目标；null 时回到当前启用的实例 */
    fun setPreferredTarget(name: String?) {
        if (state.preferredTargetName != name) state = state.copy(preferredTargetName = name)
    }

    fun openTargetChooser() {
        val current = current()
        val active = VersionsManager.currentVersion.value?.getVersionName()
        VersionsManager.refresh("ALL Download") // drop instances deleted/renamed since the list was cached
        state = state.copy(
            targetVersionNames = (state.preferredTargetName ?: active)?.let(::setOf).orEmpty(),
            targetChooserOpen = true
        )
    }

    fun toggleTarget(name: String) {
        val updated = state.targetVersionNames.toMutableSet()
        if (!updated.add(name)) updated.remove(name)
        state = state.copy(targetVersionNames = updated)
    }

    fun confirmAssetInstall() {
        val selected = current().selectedVersion ?: return
        val targets = state.installedVersions.filter { it.getVersionName() in state.targetVersionNames }
        if (targets.isEmpty()) return
        val category = state.category
        state = state.copy(targetChooserOpen = false, busy = true, progressMessage = selected.platformFileName())
        val before = TaskSystem.tasksFlow.value.map { it.id }.toSet()
        viewModelScope.launch {
            runCatching { service.installAsset(category, selected, targets, current().idSearchClass) }
                .onSuccess {
                    val started = TaskSystem.tasksFlow.value.map { it.id }.filterNot(before::contains).toSet()
                    state = state.copy(trackedAssetTaskIds = state.trackedAssetTaskIds + started)
                }
                .onFailure { state = state.copy(installError = it.message ?: it.toString()) }
            state = state.copy(busy = false)
        }
    }

    fun chooseGameVersion(version: MinecraftVersion) {
        VersionsManager.refresh("ALL Download")
        updateCategory { it.copy(route = AllDownloadRoute.GAME_VERSION) }
        state = state.copy(selectedGameVersion = version, gameInstallName = version.version.id)
    }

    fun updateGameInstallName(name: String) {
        state = state.copy(gameInstallName = name)
    }

    fun installGame() {
        val selected = state.selectedGameVersion ?: return
        val name = state.gameInstallName.trim()
        if (name.isEmpty()) return
        state = state.copy(busy = true, progressMessage = selected.version.id, installError = null)
        val installer = service.installGame(
            context = context,
            scope = viewModelScope,
            version = selected,
            targetName = name,
            onInstalled = { installed ->
                state = state.copy(busy = false, progressMessage = "Installed $installed")
                VersionsManager.refresh("ALL Download")
            },
            onError = { error -> clearInstallerTemp(); state = state.copy(busy = false, installError = error.message ?: error.toString()) },
            onAlreadyInstalled = { state = state.copy(busy = false, installError = "This game version is already installed.") }
        )
        gameInstaller = installer
        viewModelScope.launch {
            installer.tasksFlow.collect { tasks -> state = state.copy(installerTasks = tasks) }
        }
    }

    fun openModpackInstall() {
        VersionsManager.refresh("ALL Download")
        val selected = current().selectedVersion ?: return
        state = state.copy(
            installConfirmationOpen = true,
            modpackInstallName = current().project?.platformTitle().orEmpty(),
            progressMessage = selected.platformFileName()
        )
    }

    fun updateModpackName(name: String) {
        state = state.copy(modpackInstallName = name)
    }

    fun installModpack() {
        val project = current().project ?: return
        val selected = current().selectedVersion ?: return
        val name = state.modpackInstallName.trim()
        if (name.isEmpty()) return
        state = state.copy(installConfirmationOpen = false, busy = true, installError = null)
        val installer = service.installModpack(
            context = context,
            scope = viewModelScope,
            version = selected,
            project = service.projectSummary(project),
            targetName = name,
            waitForMobileData = { awaitMobileDataDecision() },
            onInstalled = { installed ->
                state = state.copy(busy = false, progressMessage = "Installed $installed")
                VersionsManager.refresh("ALL Download")
            },
            onCancelled = { state = state.copy(busy = false) },
            onError = { error -> clearInstallerTemp(); state = state.copy(busy = false, installError = if (error::class.simpleName == "GameAlreadyInstalledException") "An instance with this name already exists." else error.message ?: error.toString()) }
        )
        modpackInstaller = installer
        viewModelScope.launch {
            installer.tasksFlow.collect { tasks -> state = state.copy(installerTasks = tasks) }
        }
    }

    fun respondToMobileData(allow: Boolean) {
        state = state.copy(mobileDataConfirmationOpen = false)
        mobileDataDecision?.complete(allow)
        mobileDataDecision = null
    }

    private suspend fun awaitMobileDataDecision(): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        withContext(Dispatchers.Main) {
            mobileDataDecision = deferred
            state = state.copy(mobileDataConfirmationOpen = true)
        }
        return deferred.await()
    }

    fun cancelTask(id: String) {
        if (id in state.trackedAssetTaskIds) {
            TaskSystem.cancelTask(id)
            state = state.copy(trackedAssetTaskIds = state.trackedAssetTaskIds - id)
        }
    }

    fun cancelInstall() {
        gameInstaller?.cancelInstall(clearTarget = true)
        modpackInstaller?.cancelInstall()
        clearInstallerTemp()
        state.trackedAssetTaskIds.forEach(TaskSystem::cancelTask)
        state = state.copy(busy = false, progressMessage = null, trackedAssetTaskIds = emptySet())
    }

    /** Zalith only clears these at the start of the next install; a cancelled/failed run would otherwise leave partial files in cache */
    private fun clearInstallerTemp() {
        FileUtils.deleteQuietly(PathManager.DIR_CACHE_MODPACK_DOWNLOADER)
        FileUtils.deleteQuietly(PathManager.DIR_CACHE_GAME_DOWNLOADER)
    }

    fun dismissInstallError() {
        state = state.copy(installError = null)
    }

    private inline fun updateCategory(update: (AllDownloadCategoryState) -> AllDownloadCategoryState) =
        updateCategory(state.category, update)

    private inline fun updateCategory(
        category: AllDownloadCategory,
        update: (AllDownloadCategoryState) -> AllDownloadCategoryState
    ) {
        val current = state.categories[category] ?: AllDownloadCategoryState(platform = service.defaultPlatform(category))
        state = state.copy(categories = state.categories + (category to update(current)))
    }
}
