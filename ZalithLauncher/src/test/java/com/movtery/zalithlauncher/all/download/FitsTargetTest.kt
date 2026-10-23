package com.movtery.zalithlauncher.all.download

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FitsTargetTest {
    @Test fun modsNeedTheTargetsMinecraftVersionAndLoader() {
        assertTrue(fitsTarget("1.20.1", "Fabric", listOf("1.20.1"), listOf("FABRIC", "QUILT"), requiresLoader = true))
        assertFalse(fitsTarget("1.20.1", "Forge", listOf("1.20.1"), listOf("FABRIC"), requiresLoader = true))
        assertFalse(fitsTarget("1.21.1", "Fabric", listOf("1.20.1"), listOf("FABRIC"), requiresLoader = true))
        //a mod never fits Vanilla
        assertFalse(fitsTarget("1.20.1", null, listOf("1.20.1"), listOf("FABRIC"), requiresLoader = true))
    }

    @Test fun resourcePacksOnlyNeedTheMinecraftVersion() {
        assertTrue(fitsTarget("1.20.1", null, listOf("1.20.1"), emptyList(), requiresLoader = false))
        assertFalse(fitsTarget("1.20.4", null, listOf("1.20.1"), emptyList(), requiresLoader = false))
    }
}
