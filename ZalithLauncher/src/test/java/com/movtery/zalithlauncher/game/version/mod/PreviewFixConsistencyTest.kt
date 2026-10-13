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
 * Universal Mod Doctor V3: Preview ↔ Fix consistency.
 * ทดสอบกติกาที่ Preview และ Fix ใช้ร่วมกัน (ลำดับเวอร์ชัน, เวอร์ชันใหม่กว่า) และการแยกหมวดของแผนโดยไม่เรียกเครือข่าย
 */
class PreviewFixConsistencyTest {
    private fun candidate(name: String, release: PlatformReleaseType, mc: String = "1.21.11", loader: String = "Fabric") =
        V3StubVersion(name, release, arrayOf(mc), listOf(V3Label(loader)))

    // ---- Preview ของ dependency ใช้ลำดับเดียวกับ Fix (stable ก่อน) ----

    @Test
    fun previewPicksTheStableBuildThatFixWouldTryFirst() {
        val versions = listOf(
            candidate("mc1.21.11-2.0.0-beta-fabric", PlatformReleaseType.BETA),
            candidate("mc1.21.11-1.9.0-fabric", PlatformReleaseType.RELEASE),
        )
        val picked = previewDependencyVersion(versions, "1.21.11", ModLoader.FABRIC)
        assertEquals("mc1.21.11-1.9.0-fabric", picked?.platformVersion())
    }

    @Test
    fun previewIgnoresBuildsThatDoNotMatchTheInstance() {
        val versions = listOf(
            candidate("mc1.21.10-2.0.0-fabric", PlatformReleaseType.RELEASE, mc = "1.21.10"),
            candidate("mc1.21.11-1.0.0-forge", PlatformReleaseType.RELEASE, loader = "Forge"),
        )
        assertNull(previewDependencyVersion(versions, "1.21.11", ModLoader.FABRIC))
    }

    // ---- ไม่ Downgrade: เวอร์ชันที่ติดตั้งต้องถูกเทียบได้ชัดเจน ----

    @Test
    fun newerBuildIsRecognisedOnlyWhenProvable() {
        assertTrue(isNewerBuildThanInstalled("mc1.21.11-1.10.8-fabric", "1.10.7+mc1.21.11", "1.21.11"))
        assertFalse(isNewerBuildThanInstalled("mc1.21.11-1.10.6-fabric", "1.10.7+mc1.21.11", "1.21.11"))
        assertFalse(isNewerBuildThanInstalled("mc1.21.11-1.10.7-fabric", "1.10.7+mc1.21.11", "1.21.11"))
    }

    @Test
    fun unknownVersionsAreNeverTreatedAsAnUpdate() {
        assertFalse(isNewerBuildThanInstalled("latest-release", "1.10.7+mc1.21.11", "1.21.11"))
        assertFalse(isNewerBuildThanInstalled("mc1.21.11-1.10.8-fabric", null, "1.21.11"))
        assertFalse(isNewerBuildThanInstalled("mc1.21.11-1.10.8-fabric", "", "1.21.11"))
    }

    // ---- แยกหมวด: ยังตรวจไม่ครบ ต้องบอกว่า Fix อาจทำอีกฝั่ง ----

    @Test
    fun unverifiedBreaksNoteSaysTheFixMayTryTheOtherSide() {
        val inst = listOf(
            GroupPlanInstalled("foo", "foo.jar", "2.0", "p-foo", breaks = mapOf("bar" to listOf("<=1.0"))),
            GroupPlanInstalled("bar", "bar.jar", "1.0", "p-bar"),
        )
        val plan = planBreaksConflicts(
            "1.21.11", "fabric", inst, findBreaksConflicts(inst),
            mapOf("bar" to listOf(GroupPlanCandidate("bar", "p-bar", "mc1.21.11-1.0-fabric", "bar.jar",
                setOf("1.21.11"), setOf("fabric"), emptySet(), emptySet(), releaseType = GroupPlanReleaseType.RELEASE))),
        )
        assertEquals(GroupPlanStatus.UNVERIFIED, plan.status)
        assertTrue(plan.notes.last().contains("ฝั่งที่ประกาศ breaks"))
    }
}

private class V3Label(private val name: String) : PlatformDisplayLabel {
    override fun getDisplayName() = name
    override fun index() = 0
}

private class V3StubVersion(
    private val name: String,
    private val release: PlatformReleaseType,
    private val gameVersions: Array<String>,
    private val loaders: List<PlatformDisplayLabel>,
) : PlatformVersion {
    override suspend fun initFile(currentProjectId: String) = true
    override fun platform() = Platform.MODRINTH
    override fun platformId() = "stub-$name"
    override fun platformProjectId() = "p-stub"
    override fun platformDisplayName() = name
    override fun platformFileName() = "stub-$name.jar"
    override fun platformGameVersion() = gameVersions
    override fun platformLoaders() = loaders
    override fun platformReleaseType() = release
    override fun platformDependencies() = emptyList<PlatformVersion.PlatformDependency>()
    override fun platformDownloadCount() = 0L
    override fun platformDownloadUrl() = ""
    override fun platformDatePublished() = Instant.parse("2026-07-28T08:48:35Z")
    override fun platformSha1() = null
    override fun platformFileSize() = 0L
    override fun platformVersion() = name
}
