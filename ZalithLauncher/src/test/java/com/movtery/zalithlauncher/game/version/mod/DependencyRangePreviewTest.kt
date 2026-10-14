package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformDisplayLabel
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformReleaseType
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * 依赖候选的版本约束：预览挑出的版本必须已经满足需要它的模组声明的范围，而不是等安装时被拒绝。
 * 范围判定沿用 Fabric 的写法（数组=任一分支，空格分隔=同时满足），与 [versionPredicateDecision] 同一条路径。
 */
class DependencyRangePreviewTest {

    /** Litematica 0.26.16 对 malilib 的声明：空格分隔的两个比较符，必须同时满足 */
    private val litematicaRange = listOf(listOf(">=0.27.19- <0.28.0-"))

    @Test
    fun withoutRequirementsEveryVersionIsAcceptedAsBefore() {
        assertTrue(dependencyVersionSatisfies("0.28.1", emptyList()))
    }

    @Test
    fun aVersionInsideTheRangeSatisfiesAndOutsideDoesNot() {
        assertTrue(dependencyVersionSatisfies("0.27.20", litematicaRange))
        assertTrue("the lower bound is inclusive", dependencyVersionSatisfies("0.27.19", litematicaRange))
        assertFalse("the upper bound is exclusive", dependencyVersionSatisfies("0.28.0", litematicaRange))
        assertFalse(dependencyVersionSatisfies("0.27.18", litematicaRange))
    }

    @Test
    fun aVersionNameThatCannotBeJudgedIsNotAccepted() {
        assertFalse("an empty name is not guessed", dependencyVersionSatisfies("", litematicaRange))
        assertFalse("a name that is not a version is not guessed", dependencyVersionSatisfies("latest", litematicaRange))
    }

    @Test
    fun everyRequirerMustBeSatisfied() {
        val twoRequirers = listOf(listOf(">=0.27.19-"), listOf("<0.27.21"))

        assertTrue(dependencyVersionSatisfies("0.27.20", twoRequirers))
        assertFalse("the second requirer still rejects 0.27.21", dependencyVersionSatisfies("0.27.21", twoRequirers))
    }

    @Test
    fun thePreviewPicksTheNewestCompatibleVersionInsideTheRange() {
        //平台给出的顺序是最新在前：0.28.1 超出范围，必须跳过它而不是只在安装时被拒绝
        val versions = listOf(
            stub("0.28.1"),
            stub("0.27.20"),
            stub("0.27.19"),
        )

        val picked = previewDependencyVersion(versions, MINECRAFT, ModLoader.FABRIC, litematicaRange)

        assertEquals("0.27.20", picked?.platformVersion())
    }

    @Test
    fun withoutRequirementsThePreviewStillPicksTheNewest() {
        val versions = listOf(stub("0.28.1"), stub("0.27.20"))

        assertEquals("0.28.1", previewDependencyVersion(versions, MINECRAFT, ModLoader.FABRIC)?.platformVersion())
    }

    @Test
    fun whenNoVersionIsInRangeThePreviewReturnsNothingSoNothingIsProposed() {
        val versions = listOf(stub("0.28.1"), stub("0.28.0"), stub("0.27.18"))

        assertNull(previewDependencyVersion(versions, MINECRAFT, ModLoader.FABRIC, litematicaRange))
    }

    @Test
    fun aVersionForAnotherMinecraftIsStillExcludedWhateverItsNumber() {
        val versions = listOf(stub("0.27.20", minecraft = "1.21.10"))

        assertNull(previewDependencyVersion(versions, MINECRAFT, ModLoader.FABRIC, litematicaRange))
    }

    private fun stub(versionName: String, minecraft: String = MINECRAFT): PlatformVersion = RangeStubVersion(
        projectId = "malilib-project",
        versionName = versionName,
        gameVersions = arrayOf(minecraft),
        loaders = listOf(RangeStubLabel(ModLoader.FABRIC.displayName)),
    )

    private companion object {
        const val MINECRAFT = "1.21.11"
    }
}

private class RangeStubLabel(private val name: String) : PlatformDisplayLabel {
    override fun getDisplayName() = name
    override fun index() = 0
}

private class RangeStubVersion(
    private val projectId: String,
    private val versionName: String,
    private val gameVersions: Array<String>,
    private val loaders: List<PlatformDisplayLabel>,
) : PlatformVersion {
    override suspend fun initFile(currentProjectId: String) = true
    override fun platform() = Platform.MODRINTH
    override fun platformId() = "range-$versionName"
    override fun platformProjectId() = projectId
    override fun platformDisplayName() = versionName
    override fun platformFileName() = "range-$versionName.jar"
    override fun platformGameVersion() = gameVersions
    override fun platformLoaders() = loaders
    override fun platformReleaseType() = PlatformReleaseType.RELEASE
    override fun platformDependencies() = emptyList<PlatformVersion.PlatformDependency>()
    override fun platformDownloadCount() = 0L
    override fun platformDownloadUrl() = ""
    override fun platformDatePublished() = Instant.parse("2026-07-28T08:48:35Z")
    override fun platformSha1() = null
    override fun platformFileSize() = 0L
    override fun platformVersion() = versionName
}
