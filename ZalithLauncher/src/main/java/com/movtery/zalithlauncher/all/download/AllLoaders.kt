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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */
package com.movtery.zalithlauncher.all.download

import com.movtery.zalithlauncher.game.addons.modloader.AddonVersion
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricAPIVersions
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersion
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersions
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.quilt.QuiltAPIVersions
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.quilt.QuiltVersion
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.quilt.QuiltVersions
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge.ForgeVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge.ForgeVersions
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersions
import com.movtery.zalithlauncher.game.addons.modloader.modlike.ModVersion
import com.movtery.zalithlauncher.game.addons.modloader.optifine.OptiFineVersion
import com.movtery.zalithlauncher.game.addons.modloader.optifine.OptiFineVersions
import com.movtery.zalithlauncher.game.download.game.GameDownloadInfo

/**
 * Loaders the ALL installer / loader manager offers. Everything is delegated to Zalith's existing version lists and
 * `GameInstaller`; this file only maps a choice to the right [GameDownloadInfo] field.
 *
 * Each loader has its OWN optional API (Fabric API for Fabric, Quilted Fabric API for Quilt). Forge and NeoForge have none.
 * APIs are never shared between loaders and never installed unless the user ticks them.
 */
enum class AllGameLoader(val slug: String, val title: String, val apiTitle: String?) {
    VANILLA("vanilla", "Vanilla", null),
    FABRIC("fabric", "Fabric", "Fabric API"),
    QUILT("quilt", "Quilt", "Quilted Fabric API"),
    NEOFORGE("neoforge", "NeoForge", null),
    FORGE("forge", "Forge", null),
    OPTIFINE("optifine", "OptiFine", null),
}

object AllLoaderCatalog {
    /** Versions of [loader] that exist for [mc] (the backend already filters by Minecraft version); empty = not supported. */
    suspend fun fetch(loader: AllGameLoader, mc: String): List<AddonVersion> = when (loader) {
        AllGameLoader.VANILLA -> emptyList()
        AllGameLoader.FABRIC -> FabricVersions.fetchFabricLoaderList(mc).orEmpty()
        AllGameLoader.QUILT -> QuiltVersions.fetchQuiltLoaderList(mc).orEmpty()
        AllGameLoader.NEOFORGE -> NeoForgeVersions.fetchNeoForgeList(gameVersion = mc).orEmpty()
        AllGameLoader.FORGE -> ForgeVersions.fetchForgeList(mc).orEmpty()
        AllGameLoader.OPTIFINE -> OptiFineVersions.fetchOptiFineList(gameVersion = mc).orEmpty()
    }

    /** Newest build of the loader's own API for [mc]; null = none published (or the loader has no API). */
    suspend fun fetchApi(loader: AllGameLoader, mc: String): ModVersion? = when (loader) {
        AllGameLoader.FABRIC -> FabricAPIVersions.fetchVersionList(mc)?.firstOrNull()
        AllGameLoader.QUILT -> QuiltAPIVersions.fetchVersionList(mc)?.firstOrNull()
        else -> null
    }

    fun isPreRelease(version: AddonVersion): Boolean = when (version) {
        is FabricVersion -> !version.stable
        //Zalith's QuiltVersion.stable is inverted (true for "beta"), so judge by the version name instead
        is QuiltVersion -> version.getAddonVersion().contains("beta", ignoreCase = true)
        is NeoForgeVersion -> version.isBeta
        is OptiFineVersion -> version.isPreview
        else -> false
    }

    fun isRecommended(version: AddonVersion): Boolean = version is ForgeVersion && version.isRecommended

    private val UNSTABLE_NAME = Regex("(?i)(alpha|beta|pre|rc|snapshot)")

    /**
     * Stable / unstable lists for the picker (newest first in, order kept). Fabric meta flags only the NEWEST stable loader as
     * stable, so per-version flags would call every older release unstable: for Fabric, versions newer than the first stable one
     * are unstable, older ones are stable unless their name says beta/rc/pre. Other loaders use [isPreRelease].
     */
    fun splitStable(versions: List<AddonVersion>): Pair<List<AddonVersion>, List<AddonVersion>> {
        val firstStableFabric = versions.indexOfFirst { it is FabricVersion && it.stable }
        val unstable = versions.filterIndexed { i, v ->
            if (v is FabricVersion) (firstStableFabric < 0 || i < firstStableFabric) && !v.stable || UNSTABLE_NAME.containsMatchIn(v.getAddonVersion())
            else isPreRelease(v)
        }
        return versions.filterNot { it in unstable } to unstable
    }

    /** Recommended (Forge) > newest stable > newest. */
    fun defaultPick(versions: List<AddonVersion>): AddonVersion? =
        versions.firstOrNull { isRecommended(it) } ?: versions.firstOrNull { !isPreRelease(it) } ?: versions.firstOrNull()

    /** The download description for exactly one loader (+ its own optional API). */
    fun downloadInfo(
        mc: String,
        name: String,
        loader: AllGameLoader,
        version: AddonVersion?,
        api: ModVersion?,
    ): GameDownloadInfo = when (loader) {
        AllGameLoader.VANILLA -> GameDownloadInfo(gameVersion = mc, customVersionName = name)
        AllGameLoader.FABRIC -> GameDownloadInfo(mc, name, fabric = version as? FabricVersion, fabricAPI = api.takeIf { version is FabricVersion })
        AllGameLoader.QUILT -> GameDownloadInfo(mc, name, quilt = version as? QuiltVersion, quiltAPI = api.takeIf { version is QuiltVersion })
        AllGameLoader.NEOFORGE -> GameDownloadInfo(mc, name, neoforge = version as? NeoForgeVersion)
        AllGameLoader.FORGE -> GameDownloadInfo(mc, name, forge = version as? ForgeVersion)
        AllGameLoader.OPTIFINE -> GameDownloadInfo(mc, name, optifine = version as? OptiFineVersion)
    }
}
