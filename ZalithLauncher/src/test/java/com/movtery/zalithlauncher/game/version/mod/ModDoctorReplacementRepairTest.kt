package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformDisplayLabel
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformReleaseType
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Mod Doctor “同项目安全替换”修复的边界回归（错误加载器 + Minecraft 版本不兼容）。
 *
 * 现场：实例为 Minecraft 1.21.11 + Fabric。
 * 期望：只在能通过现有指纹匹配取得权威项目身份、且同一项目存在兼容当前 Minecraft 版本 + 加载器的构建时，
 * 该问题才可修复；无法权威映射、无兼容构建、或候选属于其它项目时，必须保持不可修复。
 * 替换必须来自同一权威项目，且不得使用跨项目或模糊匹配。
 */
class ModDoctorReplacementRepairTest {
    // ---- 错误加载器（已完成，行为必须保持不变）----

    @Test
    fun neoForgeBuildInFabricInstanceBecomesFixableWithSameProjectFabricBuild() = runBlocking {
        val harness = ReplacementHarness(
            installed = InstalledMod(
                platform = Platform.MODRINTH,
                projectId = BALM_PROJECT_ID,
                versionId = BALM_NEOFORGE_VERSION_ID,
                versionName = "21.11.9.1+neoforge-1.21.11"
            ),
            versions = listOf(
                // 同项目更新的 NeoForge 构建：必须被加载器过滤掉
                stubVersion(BALM_PROJECT_ID, versionName = "21.11.9.1+neoforge-1.21.11", loaders = "NeoForge"),
                // 同项目的 Fabric 构建：才是合法替换
                stubVersion(BALM_PROJECT_ID, versionName = "21.11.9.1+fabric-1.21.11", loaders = "Fabric"),
            )
        )

        val resolved = harness.resolve()
        val problem = resolved.problems.single { it.type == ModDoctorProblemType.WRONG_LOADER }
        val fix = problem.replacementFix

        assertTrue(problem.reason, resolved.hasFixableProblems)
        assertTrue(fix != null)
        assertEquals(BALM_PROJECT_ID, fix?.projectId)
        assertEquals(Platform.MODRINTH, fix?.platform)
        assertEquals(BALM_LOADER_FILE, fix?.fileName)
        // 身份只来自现有指纹匹配，且只查询同一个权威项目
        assertEquals(listOf(BALM_LOADER_FILE), harness.installedQueries)
        assertEquals(listOf(BALM_PROJECT_ID to Platform.MODRINTH), harness.versionQueries)
    }

    @Test
    fun fabricBuildIsAFixCandidateForAQuiltInstance() = runBlocking {
        val harness = ReplacementHarness(
            installed = InstalledMod(
                platform = Platform.MODRINTH,
                projectId = BALM_PROJECT_ID,
                versionId = BALM_NEOFORGE_VERSION_ID,
                versionName = "21.11.9.1+neoforge-1.21.11"
            ),
            versions = listOf(
                // 同项目的 NeoForge 构建：Quilt 实例同样不能用它
                stubVersion(BALM_PROJECT_ID, versionName = "21.11.9.1+neoforge-1.21.11", loaders = "NeoForge"),
                // Quilt 实现了 Fabric 的模组模型，Fabric 构建是合法替换
                stubVersion(BALM_PROJECT_ID, versionName = "21.11.9.1+fabric-1.21.11", loaders = "Fabric"),
            ),
            loader = ModLoader.QUILT,
        )

        val resolved = harness.resolve()
        val problem = resolved.problems.single { it.type == ModDoctorProblemType.WRONG_LOADER }
        val fix = problem.replacementFix

        assertTrue(problem.reason, resolved.hasFixableProblems)
        assertTrue(fix != null)
        assertEquals(BALM_LOADER_FILE, fix?.fileName)
    }

    @Test
    fun wrongLoaderFileWithoutAuthoritativeProviderMatchStaysUnfixable() = runBlocking {
        val harness = ReplacementHarness(installed = null, versions = listOf(stubVersion(BALM_PROJECT_ID)))

        val resolved = harness.resolve()
        val problem = resolved.problems.single { it.type == ModDoctorProblemType.WRONG_LOADER }

        assertTrue(!resolved.hasFixableProblems)
        assertNull(problem.replacementFix)
        assertTrue(problem.reason.contains("could not be matched to a provider project"))
        assertTrue(harness.versionQueries.isEmpty())
    }

    @Test
    fun wrongLoaderFileWithoutCompatibleLoaderBuildStaysUnfixable() = runBlocking {
        val installed = InstalledMod(
            platform = Platform.MODRINTH,
            projectId = BALM_PROJECT_ID,
            versionId = BALM_NEOFORGE_VERSION_ID,
            versionName = "21.11.9.1+neoforge-1.21.11"
        )

        // 同项目只有 NeoForge 构建
        val neoForgeOnly = ReplacementHarness(installed, listOf(stubVersion(BALM_PROJECT_ID, loaders = "NeoForge")))
        assertTrue(!neoForgeOnly.resolve().hasFixableProblems)

        // 同项目有 Fabric 构建，但不是当前的 Minecraft 版本
        val otherMinecraft = ReplacementHarness(
            installed,
            listOf(stubVersion(BALM_PROJECT_ID, minecraftVersion = "1.21.8", loaders = "Fabric"))
        )
        assertTrue(!otherMinecraft.resolve().hasFixableProblems)
    }

    @Test
    fun wrongLoaderReplacementCandidateFromAnotherProjectIsRejected() = runBlocking {
        val harness = ReplacementHarness(
            installed = InstalledMod(
                platform = Platform.MODRINTH,
                projectId = BALM_PROJECT_ID,
                versionId = BALM_NEOFORGE_VERSION_ID,
                versionName = "21.11.9.1+neoforge-1.21.11"
            ),
            versions = listOf(stubVersion("someOtherProject", loaders = "Fabric"))
        )

        val resolved = harness.resolve()
        val problem = resolved.problems.single { it.type == ModDoctorProblemType.WRONG_LOADER }

        assertTrue(!resolved.hasFixableProblems)
        assertNull(problem.replacementFix)
        assertTrue(problem.reason.contains("does not belong to"))
    }

    // ---- Minecraft 版本不兼容 ----

    @Test
    fun outdatedFabricBuildBecomesFixableWithSameProjectCurrentBuild() = runBlocking {
        val harness = ReplacementHarness(
            mods = listOf(outdatedVersionMetadata(), fabricApiMetadata()),
            installed = InstalledMod(
                platform = Platform.MODRINTH,
                projectId = BALM_PROJECT_ID,
                versionId = BALM_OUTDATED_VERSION_ID,
                versionName = "21.11.0+fabric-1.21.8"
            ),
            versions = listOf(
                // 同项目的旧版本构建：与当前 Minecraft 不兼容，必须被过滤掉
                stubVersion(
                    BALM_PROJECT_ID,
                    versionName = "21.11.0+fabric-1.21.8",
                    minecraftVersion = "1.21.8",
                    loaders = "Fabric"
                ),
                // 同项目的当前版本构建：合法替换
                stubVersion(BALM_PROJECT_ID, versionName = "21.11.9.1+fabric-1.21.11", loaders = "Fabric"),
            )
        )

        val resolved = harness.resolve()
        val problem = resolved.problems.single { it.type == ModDoctorProblemType.WRONG_MINECRAFT_VERSION }
        val fix = problem.replacementFix

        assertTrue(problem.reason, resolved.hasFixableProblems)
        assertTrue(fix != null)
        assertEquals(BALM_PROJECT_ID, fix?.projectId)
        assertEquals(Platform.MODRINTH, fix?.platform)
        assertEquals(BALM_VERSION_FILE, fix?.fileName)
        assertEquals(listOf(BALM_VERSION_FILE), harness.installedQueries)
        assertEquals(listOf(BALM_PROJECT_ID to Platform.MODRINTH), harness.versionQueries)
    }

    @Test
    fun wrongVersionFileWithoutAuthoritativeProviderMatchStaysUnfixable() = runBlocking {
        val harness = ReplacementHarness(
            mods = listOf(outdatedVersionMetadata(), fabricApiMetadata()),
            installed = null,
            versions = listOf(stubVersion(BALM_PROJECT_ID))
        )

        val resolved = harness.resolve()
        val problem = resolved.problems.single { it.type == ModDoctorProblemType.WRONG_MINECRAFT_VERSION }

        assertTrue(!resolved.hasFixableProblems)
        assertNull(problem.replacementFix)
        assertTrue(problem.reason.contains("could not be matched to a provider project"))
        assertTrue(harness.versionQueries.isEmpty())
    }

    @Test
    fun wrongVersionFileWithoutCompatibleMinecraftBuildStaysUnfixable() = runBlocking {
        val harness = ReplacementHarness(
            mods = listOf(outdatedVersionMetadata(), fabricApiMetadata()),
            installed = InstalledMod(
                platform = Platform.MODRINTH,
                projectId = BALM_PROJECT_ID,
                versionId = BALM_OUTDATED_VERSION_ID,
                versionName = "21.11.0+fabric-1.21.8"
            ),
            // 同项目只有旧版本构建，没有支持当前 Minecraft 的构建
            versions = listOf(
                stubVersion(BALM_PROJECT_ID, minecraftVersion = "1.21.8", loaders = "Fabric")
            )
        )

        val resolved = harness.resolve()
        val problem = resolved.problems.single { it.type == ModDoctorProblemType.WRONG_MINECRAFT_VERSION }

        assertTrue(!resolved.hasFixableProblems)
        assertNull(problem.replacementFix)
        assertTrue(problem.reason.contains("no build of $BALM_PROJECT_ID supports Minecraft"))
    }

    @Test
    fun wrongVersionReplacementCandidateFromAnotherProjectIsRejected() = runBlocking {
        val harness = ReplacementHarness(
            mods = listOf(outdatedVersionMetadata(), fabricApiMetadata()),
            installed = InstalledMod(
                platform = Platform.MODRINTH,
                projectId = BALM_PROJECT_ID,
                versionId = BALM_OUTDATED_VERSION_ID,
                versionName = "21.11.0+fabric-1.21.8"
            ),
            versions = listOf(stubVersion("someOtherProject", loaders = "Fabric"))
        )

        val resolved = harness.resolve()
        val problem = resolved.problems.single { it.type == ModDoctorProblemType.WRONG_MINECRAFT_VERSION }

        assertTrue(!resolved.hasFixableProblems)
        assertNull(problem.replacementFix)
        assertTrue(problem.reason.contains("does not belong to"))
    }

    @Test
    fun correctMinecraftVersionWithMatchingLoaderIsNotReported() = runBlocking {
        val harness = ReplacementHarness(
            mods = listOf(outdatedVersionMetadata(range = INSTANCE_MINECRAFT_VERSION), fabricApiMetadata()),
            installed = InstalledMod(
                platform = Platform.MODRINTH,
                projectId = BALM_PROJECT_ID,
                versionId = BALM_OUTDATED_VERSION_ID,
                versionName = "21.11.9.1+fabric-1.21.11"
            ),
            versions = listOf(stubVersion(BALM_PROJECT_ID))
        )

        // 版本与加载器都正确：不得被误判为版本不兼容，也不得产生任何修复候选
        assertTrue(harness.resolve().isClean)
    }
}

/** 目标实例：Minecraft 1.21.11 + Fabric，与 V1/V2 QA 使用的一致 */
private const val INSTANCE_MINECRAFT_VERSION = "1.21.11"

/** 误装 NeoForge 构建的真实文件（V1 wrong-loader QA 使用过） */
private const val BALM_LOADER_FILE = "balm-neoforge-1.21.11-21.11.9.1.jar"

/** 为旧 Minecraft 版本构建的 Fabric 文件 */
private const val BALM_VERSION_FILE = "balm-fabric-1.21.11-21.11.0.jar"

/** 该文件声明的 Minecraft 版本：与实例不兼容 */
private const val BALM_OUTDATED_MINECRAFT_VERSION = "1.21.8"

/** Balm 在 Modrinth 上的权威项目Id */
private const val BALM_PROJECT_ID = "MBAkmtvl"

private const val BALM_NEOFORGE_VERSION_ID = "tKWaiIeW"

private const val BALM_OUTDATED_VERSION_ID = "stubOutdatedVersion"

private fun wrongLoaderMetadata() = ModDoctorMetadata(
    fileName = BALM_LOADER_FILE,
    modId = "balm",
    name = "Balm",
    loader = ModLoader.NEOFORGE,
)

private fun outdatedVersionMetadata(
    range: String = BALM_OUTDATED_MINECRAFT_VERSION,
) = ModDoctorMetadata(
    fileName = BALM_VERSION_FILE,
    modId = "balm",
    name = "Balm",
    loader = ModLoader.FABRIC,
    minecraftVersionRanges = listOf(range),
)

private fun fabricApiMetadata() = ModDoctorMetadata(
    fileName = "fabric-api-0.141.6+1.21.11.jar",
    modId = "fabric-api",
    name = "Fabric API",
    loader = ModLoader.FABRIC,
)

/**
 * 通过生产入口注入权威身份与项目版本查询，记录被调用的查询。
 * 检测、身份判定与替换筛选全部来自生产代码，测试不复制算法。
 */
private class ReplacementHarness(
    private val installed: InstalledMod?,
    private val versions: List<PlatformVersion>,
    private val mods: List<ModDoctorMetadata> = listOf(wrongLoaderMetadata(), fabricApiMetadata()),
    private val loader: ModLoader = ModLoader.FABRIC,
) {
    val installedQueries = mutableListOf<String>()
    val versionQueries = mutableListOf<Pair<String, Platform>>()

    suspend fun resolve(): ModDoctorReport {
        val detected = validateModDoctorMetadata(
            mods = mods,
            minecraftVersion = INSTANCE_MINECRAFT_VERSION,
            loader = loader,
        )
        return detected.withFixCandidates(
            minecraftVersion = INSTANCE_MINECRAFT_VERSION,
            loader = loader,
            platform = Platform.MODRINTH,
            installedMod = { fileName ->
                installedQueries += fileName
                installed
            },
            projectVersions = { projectId, platform ->
                versionQueries += projectId to platform
                versions
            },
        )
    }
}

private fun stubVersion(
    projectId: String,
    versionName: String = "21.11.9.1+fabric-1.21.11",
    minecraftVersion: String = INSTANCE_MINECRAFT_VERSION,
    loaders: String = "Fabric",
): PlatformVersion = StubPlatformVersion(
    projectId = projectId,
    versionName = versionName,
    gameVersions = arrayOf(minecraftVersion),
    loaders = listOf(StubLoaderLabel(loaders)),
)

private class StubLoaderLabel(private val name: String) : PlatformDisplayLabel {
    override fun getDisplayName() = name
    override fun index() = 0
}

private class StubPlatformVersion(
    private val projectId: String,
    private val versionName: String,
    private val gameVersions: Array<String>,
    private val loaders: List<PlatformDisplayLabel>,
) : PlatformVersion {
    override suspend fun initFile(currentProjectId: String) = true
    override fun platform() = Platform.MODRINTH
    override fun platformId() = "stub-$versionName"
    override fun platformProjectId() = projectId
    override fun platformDisplayName() = versionName
    override fun platformFileName() = "stub-$versionName.jar"
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
