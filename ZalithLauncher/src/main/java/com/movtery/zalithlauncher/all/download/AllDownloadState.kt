package com.movtery.zalithlauncher.all.download

import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformProject
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TitledTask
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersion

/** ALL presentation categories; each keeps its own query, page and selected-project state. */
enum class AllDownloadCategory(val assetClass: PlatformClasses?) {
    GAME(null),
    MODPACK(PlatformClasses.MOD_PACK),
    MOD(PlatformClasses.MOD),
    RESOURCE_PACK(PlatformClasses.RESOURCE_PACK),
    SHADER(PlatformClasses.SHADERS),
    WORLD(PlatformClasses.SAVES),
    BY_ID(null)
}

enum class AllDownloadRoute {
    BROWSE,
    PROJECT,
    GAME_VERSION
}

data class AllDownloadProject(
    val platform: Platform,
    val projectId: String,
    val title: String,
    val description: String,
    val author: String,
    val iconUrl: String?,
    val downloads: Long
)

data class AllDownloadCategoryState(
    val query: String = "",
    val platform: Platform = Platform.MODRINTH,
    val pageIndex: Int = 0,
    val totalPages: Int = 0,
    val projects: List<AllDownloadProject> = emptyList(),
    val project: PlatformProject? = null,
    val versions: List<PlatformVersion> = emptyList(),
    val selectedVersion: PlatformVersion? = null,
    val route: AllDownloadRoute = AllDownloadRoute.BROWSE,
    val loading: Boolean = false,
    /** true once a search for the current query has completed (separates "no results" from "not searched yet") */
    val searched: Boolean = false,
    val error: String? = null,
    val idSearchClass: PlatformClasses = PlatformClasses.MOD,
    val targetChooserOpen: Boolean = false,
    val targetNames: Set<String> = emptySet(),
    val installConfirmationOpen: Boolean = false,
    val targetNameInput: String = ""
)

data class AllDownloadState(
    val category: AllDownloadCategory = AllDownloadCategory.GAME,
    val categories: Map<AllDownloadCategory, AllDownloadCategoryState> = emptyMap(),
    val installedVersions: List<Version> = emptyList(),
    val targetVersionNames: Set<String> = emptySet(),
    /** 从某个实例窗口打开下载时指定的默认安装目标；null = 当前启用的实例 */
    val preferredTargetName: String? = null,
    val targetChooserOpen: Boolean = false,
    val installConfirmationOpen: Boolean = false,
    val mobileDataConfirmationOpen: Boolean = false,
    val gameVersions: List<MinecraftVersion> = emptyList(),
    val selectedGameVersion: MinecraftVersion? = null,
    val gameVersionName: String = "",
    val gameInstallName: String = "",
    val gameLoader: AllGameLoader = AllGameLoader.VANILLA,
    /** loaders that really have versions for the selected Minecraft version (Vanilla always); null = still probing */
    val supportedLoaders: List<AllGameLoader>? = null,
    /** versions of the selected loader (already limited to this Minecraft version) */
    val loaderVersions: List<com.movtery.zalithlauncher.game.addons.modloader.AddonVersion>? = null,
    val loaderLoading: Boolean = false,
    val loaderError: String? = null,
    val selectedLoaderVersion: com.movtery.zalithlauncher.game.addons.modloader.AddonVersion? = null,
    /** the loader's OWN optional API (Fabric API / Quilted Fabric API); null = none published or loader has none */
    val loaderApiVersion: com.movtery.zalithlauncher.game.addons.modloader.modlike.ModVersion? = null,
    /** optional: never forced */
    val includeLoaderApi: Boolean = false,
    /** the plan the user must confirm before anything is downloaded */
    val gameInstallConfirmOpen: Boolean = false,
    /** summary of a mod/resource/shader/world install, shown before anything is downloaded */
    val assetConfirmOpen: Boolean = false,
    /** Modpacks create a new game: the version list is checked against this Minecraft version / loader (null = any) */
    /** Fabric bundle: the Mod Menu file that fits the chosen Minecraft version (looked up on Modrinth), installed after the game */
    val modMenuVersion: PlatformVersion? = null,
    val modMenuLoading: Boolean = false,
    val modpackMc: String? = null,
    val modpackLoader: String? = null,
    val modpackInstallName: String = "",
    val busy: Boolean = false,
    val progressMessage: String? = null,
    val installError: String? = null,
    val runningTaskIds: List<String> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val installerTasks: List<TitledTask> = emptyList(),
    val trackedAssetTaskIds: Set<String> = emptySet()
)
