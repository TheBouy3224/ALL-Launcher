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
import com.movtery.zalithlauncher.game.version.saves.unpackSaveZip
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

    fun compatibleVersions(
        category: AllDownloadCategory,
        candidates: List<PlatformVersion>,
        targets: List<Version>,
        selectedClass: PlatformClasses? = category.assetClass
    ): List<PlatformVersion> {
        if (targets.isEmpty()) return candidates
        val requiresLoader = selectedClass == PlatformClasses.MOD
        return candidates.filter { candidate ->
            targets.all { target ->
                //真实的 Minecraft 版本来自实例元数据；实例名不一定以版本号开头（如 QA-MD-1）
                val mcVersion = target.getVersionInfo()?.minecraftVersion ?: target.getVersionName().substringBefore(' ')
                val correctGame = mcVersion in candidate.platformGameVersion()
                val loaderName = target.getVersionInfo()?.loaderInfo?.loader?.displayName.orEmpty()
                val loaderRequired = target.getVersionInfo()?.loaderInfo?.loader?.isLoader == true
                val correctLoader = !requiresLoader || !loaderRequired || candidate.platformLoaders()
                    .any { it.toString().contains(loaderName, ignoreCase = true) }
                correctGame && correctLoader
            }
        }
    }

    suspend fun installAsset(category: AllDownloadCategory, version: PlatformVersion, targets: List<Version>, idClass: PlatformClasses? = null) {
        if (targets.isEmpty()) throw IOException("Choose at least one install target.")
        val classes = (if (category == AllDownloadCategory.BY_ID) idClass else category.assetClass) ?: throw IOException("No asset folder is available.")
        if (classes == PlatformClasses.MOD_PACK) throw IOException("Install modpacks from the Modpacks category.")
        val isWorld = classes == AllDownloadCategory.WORLD.assetClass
        if (isWorld) {
            // unpackSaveZip merges into an existing folder (overwrites files), so never let a reinstall touch an existing world
            val worldName = version.platformFileName().substringBeforeLast('.')
            targets.firstOrNull { java.io.File(java.io.File(it.getGameDir(), classes.versionFolder.folderName), worldName).exists() }?.let {
                throw IOException("A world named \"$worldName\" already exists in ${it.getVersionName()}. Rename or delete it first.")
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
        downloadSingleForVersions(
            version = version,
            versions = targets,
            folder = classes.versionFolder.folderName,
            onFileCopied = if (isWorld) {
                { zip, folder ->
                    try {
                        unpackSaveZip(zip, folder)
                    } catch (e: Throwable) {
                        // a rejected/broken zip must not leave a half-extracted world or the zip itself in saves/ (the guard above guarantees the folder was ours)
                        org.apache.commons.io.FileUtils.deleteQuietly(java.io.File(folder, zip.nameWithoutExtension))
                        org.apache.commons.io.FileUtils.deleteQuietly(zip)
                        throw e
                    }
                }
            } else {
                { _, _ -> }
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
        onAlreadyInstalled: () -> Unit
    ): GameInstaller {
        val installer = GameInstaller(
            context = context,
            info = GameDownloadInfo(
                gameVersion = version.version.id,
                customVersionName = targetName
            ),
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
