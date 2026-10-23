package com.movtery.zalithlauncher.all.download

import android.content.Context
import com.movtery.zalithlauncher.all.bridge.AllSubmitError
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.download.assets.DependencyRequest
import com.movtery.zalithlauncher.game.download.assets.downloadDependenciesForVersions
import com.movtery.zalithlauncher.game.download.assets.downloadSingleForVersions
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearchData
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearchFilter
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion
import com.movtery.zalithlauncher.game.download.assets.platform.getProjectByVersion
import com.movtery.zalithlauncher.game.download.assets.platform.getVersionById
import com.movtery.zalithlauncher.game.download.assets.platform.getVersions
import com.movtery.zalithlauncher.game.download.assets.platform.searchAssets
import com.movtery.zalithlauncher.game.download.game.GameDownloadInfo
import com.movtery.zalithlauncher.game.download.game.GameInstaller
import com.movtery.zalithlauncher.game.download.modpack.install.ModPackInfo
import com.movtery.zalithlauncher.game.download.modpack.install.ModPackInstaller
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersion
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersions
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.toAndroidString
import com.movtery.zalithlauncher.game.version.saves.findExistingWorldTarget
import com.movtery.zalithlauncher.game.version.saves.SaveWorldBatch
import com.movtery.zalithlauncher.game.version.saves.worldNameTakenMessage
import kotlinx.coroutines.CoroutineScope
import java.io.IOException

/** Provider clients, dependency resolution, installers and background tasks remain Zalith-owned. */
class AllDownloadService(private val submitError: AllSubmitError, private val context: Context) {
    fun installedVersions(): List<Version> = VersionsManager.versions.value
    fun gameVersions(): List<MinecraftVersion> = MinecraftVersions.allVersions.value

    fun defaultPlatform(category: AllDownloadCategory): Platform = when (category) {
        AllDownloadCategory.MOD -> AllSettings.searchModPlatform.getValue()
        AllDownloadCategory.MODPACK -> AllSettings.searchModpackPlatform.getValue()
        AllDownloadCategory.RESOURCE_PACK -> AllSettings.searchResourcePackPlatform.getValue()
        AllDownloadCategory.SHADER -> AllSettings.searchShadersPlatform.getValue()
        AllDownloadCategory.WORLD -> Platform.CURSEFORGE
        AllDownloadCategory.BY_ID -> AllSettings.searchModPlatform.getValue()
        AllDownloadCategory.GAME -> Platform.MODRINTH
    }

    fun savePlatform(category: AllDownloadCategory, platform: Platform) {
        when (category) {
            AllDownloadCategory.MOD -> AllSettings.searchModPlatform.save(platform)
            AllDownloadCategory.MODPACK -> AllSettings.searchModpackPlatform.save(platform)
            AllDownloadCategory.RESOURCE_PACK -> AllSettings.searchResourcePackPlatform.save(platform)
            AllDownloadCategory.SHADER -> AllSettings.searchShadersPlatform.save(platform)
            AllDownloadCategory.BY_ID -> AllSettings.searchModPlatform.save(platform)
            AllDownloadCategory.GAME, AllDownloadCategory.WORLD -> Unit
        }
    }

    suspend fun search(
        category: AllDownloadCategory,
        platform: Platform,
        query: String,
        pageIndex: Int,
        gameVersion: String = "",
        sortField: com.movtery.zalithlauncher.game.download.assets.platform.PlatformSortField =
            com.movtery.zalithlauncher.game.download.assets.platform.PlatformSortField.RELEVANCE
    ): SearchPage {
        val platformClass = category.assetClass ?: throw IOException("This category cannot search projects.")
        var result: com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearchResult? = null
        var failure: String? = null
        searchAssets(
            searchPlatform = platform,
            searchFilter = PlatformSearchFilter(
                searchName = query,
                gameVersion = gameVersion,
                sortField = sortField,
                index = pageIndex
            ),
            platformClasses = platformClass,
            onSuccess = { result = it },
            onError = { failure = it.message.toAndroidString(context) }
        )
        failure?.let { throw IOException(it) }
        val page = result?.getAssetsPage(platformClass) ?: throw IOException("Search returned no project page.")
        return SearchPage(
            projects = page.data.map { (data, _) -> data.toAllDownloadProject() },
            currentPage = page.pageIndex,
            totalPages = page.totalPage,
            isLastPage = page.isLastPage
        )
    }

    suspend fun project(item: AllDownloadProject) = getProjectByVersion(item.projectId, item.platform)

    suspend fun projectById(id: String, platform: Platform) = getProjectByVersion(id.trim(), platform)

    suspend fun versions(item: AllDownloadProject): List<PlatformVersion> =
        getVersions(item.projectId, item.platform)

    suspend fun versionsById(id: String, platform: Platform): List<PlatformVersion> =
        getVersions(id.trim(), platform)

    fun projectSummary(project: com.movtery.zalithlauncher.game.download.assets.platform.PlatformProject) =
        AllDownloadProject(
            platform = project.platform(),
            projectId = project.platformId(),
            title = project.platformTitle(),
            description = project.platformSummary().orEmpty(),
            author = project.platformAuthor().orEmpty(),
            iconUrl = project.platformIconUrl(),
            downloads = project.platformDownloadCount()
        )

    suspend fun installAsset(category: AllDownloadCategory, version: PlatformVersion, targets: List<Version>, idClass: PlatformClasses? = null) {
        if (targets.isEmpty()) throw IOException("Choose at least one install target.")
        val classes = (if (category == AllDownloadCategory.BY_ID) idClass else category.assetClass) ?: throw IOException("No asset folder is available.")
        if (classes == PlatformClasses.MOD_PACK) throw IOException("Install modpacks from the Modpacks category.")
        val isWorld = classes == AllDownloadCategory.WORLD.assetClass
        if (isWorld) {
            //同名世界已存在就在下载前拒绝；解压时 unpackSaveZip 还会再检查一次（防止下载期间被别的任务创建）
            val worldName = version.platformFileName().substringBeforeLast('.')
            findExistingWorldTarget(worldName, targets.map { java.io.File(it.getGameDir(), classes.versionFolder.folderName) })?.let {
                //it = <game>/saves, so the game (instance) name is its parent
                throw IOException(worldNameTakenMessage(worldName, it.parentFile?.name ?: it.path))
            }
        }
        if (classes == PlatformClasses.MOD) {
            val requests = version.platformDependencies()
                .filter { it.type.name.equals("REQUIRED", ignoreCase = true) }
                .mapNotNull { dependency ->
                    val projectId = dependency.projectId ?: dependency.versionId?.let { fileId ->
                        getVersionById(fileId, dependency.platform, printLog = false).platformProjectId()
                    }
                    projectId?.let {
                        DependencyRequest(
                            platform = dependency.platform,
                            projectId = it,
                            versionId = dependency.versionId,
                            classes = PlatformClasses.MOD,
                            projectTitle = it
                        )
                    }
                }
            if (requests.isNotEmpty()) downloadDependenciesForVersions(requests, targets, submitError)
        }
        //本次任务内多个实例的解压记录：任一失败只撤销本次创建的世界，已存在的世界不动
        val worlds = SaveWorldBatch()
        downloadSingleForVersions(
            version = version,
            versions = targets,
            folder = classes.versionFolder.folderName,
            //zip 的归属由下载层给出：本次写入的才会在解压后删除，已存在的同名 zip 原样保留
            onFileCopied = if (isWorld) {
                { zip, folder, install -> worlds.unpack(zip, folder, install) }
            } else {
                { _, _, _ -> }
            },
            submitError = submitError
        )
    }

    fun installGame(
        context: Context,
        scope: CoroutineScope,
        version: MinecraftVersion,
        targetName: String,
        onInstalled: (String) -> Unit,
        onError: (Throwable) -> Unit,
        onAlreadyInstalled: () -> Unit,
        loader: AllGameLoader = AllGameLoader.VANILLA,
        loaderVersion: com.movtery.zalithlauncher.game.addons.modloader.AddonVersion? = null,
        loaderApi: com.movtery.zalithlauncher.game.addons.modloader.modlike.ModVersion? = null
    ): GameInstaller {
        val installer = GameInstaller(
            context = context,
            //exactly one loader (+ its own optional API) is mapped onto the existing installer's info
            info = AllLoaderCatalog.downloadInfo(version.version.id, targetName, loader, loaderVersion, loaderApi),
            scope = scope
        )
        installer.installGame(onInstalled = onInstalled, onError = onError, onGameAlreadyInstalled = onAlreadyInstalled)
        return installer
    }

    fun installModpack(
        context: Context,
        scope: CoroutineScope,
        version: PlatformVersion,
        project: AllDownloadProject,
        targetName: String,
        waitForMobileData: suspend () -> Boolean,
        onInstalled: (String) -> Unit,
        onCancelled: () -> Unit,
        onError: (Throwable) -> Unit
    ): ModPackInstaller {
        val installer = ModPackInstaller(
            context = context,
            version = version,
            iconUrl = project.iconUrl,
            scope = scope,
            waitForVersionName = { _: ModPackInfo -> targetName },
            waitForConfirmMobileData = waitForMobileData
        )
        installer.installModPack(onInstalled = onInstalled, onCancelled = onCancelled, onError = onError)
        return installer
    }

    private fun PlatformSearchData.toAllDownloadProject() = AllDownloadProject(
        platform = platform(),
        projectId = platformId(),
        title = platformTitle(),
        description = platformDescription(),
        author = platformAuthor(),
        iconUrl = platformIconUrl(),
        downloads = platformDownloadCount()
    )
}

data class SearchPage(
    val projects: List<AllDownloadProject>,
    val currentPage: Int,
    val totalPages: Int,
    val isLastPage: Boolean
)

/**
 * Does a project file fit one install target? Same Minecraft version, and for mods the target's loader must be one of the file's
 * loaders. A mod never fits a game without a loader (Vanilla): it could not run there (Download V4 - before, any loader passed).
 */
internal fun fitsTarget(mcVersion: String, targetLoader: String?, fileGameVersions: List<String>, fileLoaders: List<String>, requiresLoader: Boolean): Boolean {
    if (mcVersion !in fileGameVersions) return false
    if (!requiresLoader) return true
    if (targetLoader.isNullOrBlank()) return false
    return fileLoaders.any { it.contains(targetLoader, ignoreCase = true) }
}
