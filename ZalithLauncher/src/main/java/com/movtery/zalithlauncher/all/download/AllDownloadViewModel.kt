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

    fun selectCategory(category: AllDownloadCategory) {
        state = state.copy(category = category, progressMessage = if (state.busy) state.progressMessage else null)
    }

    fun refreshTargets() {
        VersionsManager.refresh("ALL Download")
    }

    fun current(): AllDownloadCategoryState = state.categories[state.category]
        ?: AllDownloadCategoryState(platform = service.defaultPlatform(state.category))

    fun updateQuery(query: String) = updateCategory { it.copy(query = query, error = null, searched = false) }

    fun selectPlatform(platform: Platform) {
        service.savePlatform(state.category, platform)
        updateCategory { it.copy(platform = platform, error = null) }
    }

    fun selectIdClass(platformClass: PlatformClasses) = updateCategory { it.copy(idSearchClass = platformClass) }

    /**
     * [page] 是页码（从 0 开始）。Zalith 的搜索索引是条目偏移量（offset），每页 [PAGE_LIMIT] 条，
     * 所以这里换算成 offset 发给服务；返回的 offset 再换回页码，保证按钮的页码与列表一致。
     * 每页替换列表（按钮翻页），不再把页面追加到后面。
     */
    fun search(page: Int = 0) {
        val category = state.category
        val current = current()
        if (category == AllDownloadCategory.GAME || current.query.isBlank()) return
        updateCategory(category) { it.copy(loading = true, error = null, pageIndex = page, route = AllDownloadRoute.BROWSE, searched = false, projects = emptyList(), totalPages = 0) }
        viewModelScope.launch {
            runCatching {
                withTimeout(60_000) {
                    service.search(
                        category = category,
                        platform = current.platform,
                        query = current.query.trim(),
                        pageIndex = page * PAGE_LIMIT,
                        gameVersion = ""
                    )
                }
            }.onSuccess { result ->
                updateCategory(category) {
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
                updateCategory(category) { it.copy(loading = false, error = error.message ?: error.toString()) }
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
