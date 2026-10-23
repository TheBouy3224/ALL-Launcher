package com.movtery.zalithlauncher.all.download

import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersion
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.quilt.QuiltVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exactly one loader is mapped onto the existing installer's info; APIs never leak across loaders. */
class AllLoaderCatalogTest {
    private val fabric = FabricVersion("1.20.1", "0.19.5", true)
    private val quilt = QuiltVersion("1.20.1", "0.30.1")

    @Test
    fun vanillaHasNoLoader() {
        val info = AllLoaderCatalog.downloadInfo("1.20.1", "x", AllGameLoader.VANILLA, null, null)
        assertNull(info.fabric); assertNull(info.quilt); assertNull(info.forge); assertNull(info.neoforge)
    }

    @Test
    fun fabricMapsOnlyFabric() {
        val info = AllLoaderCatalog.downloadInfo("1.20.1", "x", AllGameLoader.FABRIC, fabric, null)
        assertNotNull(info.fabric); assertNull(info.quilt); assertNull(info.fabricAPI); assertNull(info.quiltAPI)
    }

    @Test
    fun aVersionOfAnotherLoaderIsIgnoredInsteadOfBeingInstalled() {
        //a Quilt version handed to the Fabric slot must not become a Fabric install
        val info = AllLoaderCatalog.downloadInfo("1.20.1", "x", AllGameLoader.FABRIC, quilt, null)
        assertNull(info.fabric); assertNull(info.quilt)
    }

    @Test
    fun quiltMapsOnlyQuilt() {
        val info = AllLoaderCatalog.downloadInfo("1.20.1", "x", AllGameLoader.QUILT, quilt, null)
        assertNotNull(info.quilt); assertNull(info.fabric)
    }

    @Test
    fun onlyFabricAndQuiltHaveTheirOwnApi() {
        assertEquals("Fabric API", AllGameLoader.FABRIC.apiTitle)
        assertEquals("Quilted Fabric API", AllGameLoader.QUILT.apiTitle)
        assertNull(AllGameLoader.FORGE.apiTitle); assertNull(AllGameLoader.NEOFORGE.apiTitle); assertNull(AllGameLoader.VANILLA.apiTitle)
    }

    @Test
    fun prereleaseDetectionUsesTheVersionNotZalithsInvertedQuiltFlag() {
        assertTrue(AllLoaderCatalog.isPreRelease(FabricVersion("1.20.1", "0.20.0", false)))
        assertTrue(AllLoaderCatalog.isPreRelease(QuiltVersion("1.20.1", "0.31.0-beta.4")))
        assertTrue(!AllLoaderCatalog.isPreRelease(quilt))
        assertTrue(!AllLoaderCatalog.isPreRelease(NeoForgeVersion("1.20.1-47.1.106", true)))
    }

    @Test
    fun defaultPickPrefersTheNewestNonPrerelease() {
        val list = listOf(QuiltVersion("1.20.1", "0.31.0-beta.4"), QuiltVersion("1.20.1", "0.30.1"))
        assertEquals("0.30.1", AllLoaderCatalog.defaultPick(list)?.getAddonVersion())
    }

    @Test fun fabricOlderThanTheStableOneIsStable() {
        val list = listOf(
            FabricVersion("1.20.1", "0.20.0", false), //newer than the stable one: unstable
            FabricVersion("1.20.1", "0.19.5", true),
            FabricVersion("1.20.1", "0.19.4", false), //older normal release: stable (meta only flags the newest)
            FabricVersion("1.20.1", "0.19.0-beta.2", false)
        )
        val (stable, unstable) = AllLoaderCatalog.splitStable(list)
        org.junit.Assert.assertEquals(listOf("0.19.5", "0.19.4"), stable.map { it.getAddonVersion() })
        org.junit.Assert.assertEquals(listOf("0.20.0", "0.19.0-beta.2"), unstable.map { it.getAddonVersion() })
    }
}
