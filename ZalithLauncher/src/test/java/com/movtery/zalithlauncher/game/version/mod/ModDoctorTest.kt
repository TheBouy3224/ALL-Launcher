package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformDisplayLabel
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformProject
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformReleaseType
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthVersion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InterruptedIOException
import java.time.Instant

class ModDoctorTest {
    @Test
    fun embeddedRequiredDependencyIsNotReportedMissing() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "mod-a.jar",
                    modId = "mod-a",
                    name = "Mod A",
                    loader = ModLoader.FABRIC,
                    requiredDependencies = listOf("mod-b"),
                    providedModIds = listOf("mod-b")
                )
            ),
            minecraftVersion = "1.21.11",
            loader = ModLoader.FABRIC
        )

        assertTrue(report.isClean)
    }

    @Test
    fun ambiguousMinecraftRangeIsNotReported() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "mod-a.jar",
                    modId = "mod-a",
                    name = "Mod A",
                    loader = ModLoader.FABRIC,
                    minecraftVersionRanges = listOf("[1.21,1.22)")
                )
            ),
            minecraftVersion = "1.21.11",
            loader = ModLoader.FABRIC
        )

        assertTrue(report.isClean)
    }

    @Test
    fun reportsWrongLoader() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "forge-mod.jar",
                    modId = "forge-mod",
                    name = "Forge Mod",
                    loader = ModLoader.FORGE
                )
            ),
            minecraftVersion = "1.21.11",
            loader = ModLoader.FABRIC
        )

        assertEquals(1, report.problems.size)
        assertEquals(ModDoctorProblemType.WRONG_LOADER, report.problems.single().type)
        assertTrue(report.problems.single().reason.contains("Forge Mod"))
    }

    @Test
    fun acceptsFabricModInQuiltInstance() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "iris-fabric-1.10.6+mc1.21.11.jar",
                    modId = "iris",
                    name = "Iris",
                    loader = ModLoader.FABRIC
                )
            ),
            minecraftVersion = "1.21.11",
            loader = ModLoader.QUILT
        )

        assertTrue(report.isClean)
    }

    @Test
    fun reportsQuiltModInFabricInstance() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "quilt-only-mod.jar",
                    modId = "quilt-only-mod",
                    name = "Quilt Only Mod",
                    loader = ModLoader.QUILT
                )
            ),
            minecraftVersion = "1.21.11",
            loader = ModLoader.FABRIC
        )

        assertEquals(1, report.problems.size)
        assertEquals(ModDoctorProblemType.WRONG_LOADER, report.problems.single().type)
    }

    @Test
    fun reportsWrongMinecraftVersion() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "old-mod.jar",
                    modId = "old-mod",
                    name = "Old Mod",
                    loader = ModLoader.FABRIC,
                    minecraftVersionRanges = listOf("1.20.1")
                )
            ),
            minecraftVersion = "1.21.11",
            loader = ModLoader.FABRIC
        )

        assertEquals(1, report.problems.size)
        assertEquals(ModDoctorProblemType.WRONG_MINECRAFT_VERSION, report.problems.single().type)
        assertTrue(report.problems.single().reason.contains("1.20.1"))
    }

    @Test
    fun reportsDuplicateEnabledModIds() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata("first.jar", "same-mod", "Same Mod", ModLoader.FABRIC),
                ModDoctorMetadata("second.jar", "same-mod", "Same Mod", ModLoader.FABRIC)
            ),
            minecraftVersion = "1.21.11",
            loader = ModLoader.FABRIC
        )

        assertEquals(1, report.problems.size)
        assertEquals(ModDoctorProblemType.DUPLICATE, report.problems.single().type)
        assertTrue(report.problems.single().reason.contains("first.jar"))
        assertTrue(report.problems.single().reason.contains("second.jar"))
    }

    @Test
    fun reportsUnreadableModMetadata() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "broken.jar",
                    modId = "broken-mod",
                    name = "Broken Mod",
                    loader = ModLoader.UNKNOWN,
                    readable = false
                )
            ),
            minecraftVersion = "1.21.11",
            loader = ModLoader.FABRIC
        )

        assertEquals(1, report.problems.size)
        assertEquals(ModDoctorProblemType.CORRUPT_METADATA, report.problems.single().type)
        assertTrue(report.problems.single().reason.contains("Broken Mod"))
    }

    @Test
    fun reportsMissingRequiredDependency() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "mod-a.jar",
                    modId = "mod-a",
                    name = "Mod A",
                    loader = ModLoader.FABRIC,
                    requiredDependencies = listOf("mod-b")
                )
            ),
            minecraftVersion = "1.21.11",
            loader = ModLoader.FABRIC
        )

        assertEquals(1, report.problems.size)
        assertEquals(
            ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY,
            report.problems.single().type
        )
        assertTrue(report.problems.single().reason.contains("mod-b"))
    }

    @Test
    fun fixableMissingDependencyExposesFixAction() {
        val report = ModDoctorReport(
            listOf(
                ModDoctorProblem(
                    type = ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY,
                    fileName = "continuity.jar",
                    modId = "continuity",
                    reason = "Continuity requires fabric-api.",
                    requiredDependencyId = "fabric-api",
                    fixCandidate = ModDoctorFixCandidate(
                        dependencyId = "fabric-api",
                        projectId = "resolved-project-id",
                        platform = com.movtery.zalithlauncher.game.download.assets.platform.Platform.MODRINTH
                    )
                )
            )
        )

        assertTrue(report.hasFixableProblems)
    }

    @Test
    fun unresolvableMissingDependencyDoesNotExposeFixAction() {
        val report = ModDoctorReport(
            listOf(
                ModDoctorProblem(
                    type = ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY,
                    fileName = "continuity.jar",
                    modId = "continuity",
                    reason = "Continuity requires unknown-api.",
                    requiredDependencyId = "unknown-api"
                )
            )
        )

        assertTrue(!report.hasFixableProblems)
    }

    @Test
    fun cleanPostFixReportContinuesNormalLaunch() {
        assertEquals(
            ModDoctorFixDecision.CONTINUE_LAUNCH,
            decideAfterModDoctorFix(ModDoctorReport(emptyList()))
        )
        assertEquals(
            ModDoctorFixDecision.SHOW_REMAINING_PROBLEMS,
            decideAfterModDoctorFix(
                ModDoctorReport(
                    listOf(
                        ModDoctorProblem(
                            type = ModDoctorProblemType.CORRUPT_METADATA,
                            fileName = "broken.jar",
                            modId = null,
                            reason = "Still broken."
                        )
                    )
                )
            )
        )
    }

    @Test
    fun missingDependencyResolvesToAuthoritativeProjectWithoutBroadSearch() = runBlocking {
        val provider = ModDoctorProviderStub()

        val resolved = provider.resolve("fabric-api")

        assertTrue(resolved.hasFixableProblems)
        assertEquals("P7dR8mSH", resolved.problems.single().fixCandidate?.projectId)
        assertEquals(Platform.MODRINTH, resolved.problems.single().fixCandidate?.platform)
        assertEquals(listOf("fabric-api" to Platform.MODRINTH), provider.directLookups)
    }

    @Test
    fun missingDependencyResolutionDoesNotUseProviderSearch() = runBlocking {
        val provider = ModDoctorProviderStub()

        val resolved = provider.resolve("fabric-api")

        assertTrue(resolved.hasFixableProblems)
        assertTrue(provider.searches.isEmpty())
    }

    @Test
    fun unknownDependencyStaysUnresolved() = runBlocking {
        val provider = ModDoctorProviderStub(projectAvailable = false)

        val resolved = provider.resolve("fabric-api")

        assertTrue(!resolved.hasFixableProblems)
        assertTrue(resolved.problems.single().fixCandidate == null)
        assertTrue(
            resolved.problems.single().reason.contains(
                "the dependency project fabric-api was not found on MODRINTH."
            )
        )
    }

    @Test
    fun providerLookupTimeoutIsNotReportedAsMissingProject() = runBlocking {
        val provider = ModDoctorProviderStub(directFailure = InterruptedIOException("timeout"))

        val resolved = provider.resolve("fabric-api")

        assertTrue(!resolved.hasFixableProblems)
        assertTrue(resolved.problems.single().reason.contains("failed (timeout)"))
        assertTrue(!resolved.problems.single().reason.contains("was not found"))
        assertTrue(provider.searches.isEmpty())
    }

    // —— provider 后备兼容性判断：本地元数据无法定论时才启用 ——

    @Test
    fun providerConfirmedIncompatibleAmbiguousMetadataIsReported() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "balm-fabric-1.21.10-21.10.11.jar",
                    modId = "balm",
                    name = "Balm",
                    loader = ModLoader.FABRIC,
                    // 本地声明是模糊区间（无运算符），无法定论
                    minecraftVersionRanges = listOf("~1.21-")
                )
            ),
            minecraftVersion = MINECRAFT_VERSION,
            loader = ModLoader.FABRIC,
            providerGameVersions = mapOf(
                "balm-fabric-1.21.10-21.10.11.jar" to listOf("1.21.10")
            )
        )

        assertEquals(1, report.problems.size)
        assertEquals(ModDoctorProblemType.WRONG_MINECRAFT_VERSION, report.problems.single().type)
        assertTrue(report.problems.single().reason.contains("1.21.10"))
    }

    @Test
    fun providerConfirmedIncompatibleMetadataWithoutLocalRangesIsReported() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "quiet-mod.jar",
                    modId = "quiet-mod",
                    name = "Quiet Mod",
                    loader = ModLoader.FABRIC
                )
            ),
            minecraftVersion = MINECRAFT_VERSION,
            loader = ModLoader.FABRIC,
            providerGameVersions = mapOf("quiet-mod.jar" to listOf("1.21.10"))
        )

        assertEquals(1, report.problems.size)
        assertEquals(ModDoctorProblemType.WRONG_MINECRAFT_VERSION, report.problems.single().type)
    }

    @Test
    fun providerConfirmedCompatibleAmbiguousMetadataIsNotReported() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "balm-fabric-1.21.11.jar",
                    modId = "balm",
                    name = "Balm",
                    loader = ModLoader.FABRIC,
                    minecraftVersionRanges = listOf("~1.21-")
                )
            ),
            minecraftVersion = MINECRAFT_VERSION,
            loader = ModLoader.FABRIC,
            providerGameVersions = mapOf(
                "balm-fabric-1.21.11.jar" to listOf("1.21.11", "1.21.10")
            )
        )

        assertTrue(report.isClean)
    }

    @Test
    fun ambiguousMetadataWithoutAuthoritativeProviderMatchIsNotReported() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "unmatched.jar",
                    modId = "unmatched",
                    name = "Unmatched",
                    loader = ModLoader.FABRIC,
                    minecraftVersionRanges = listOf("[1.21,1.22)")
                )
            ),
            minecraftVersion = MINECRAFT_VERSION,
            loader = ModLoader.FABRIC
        )

        assertTrue(report.isClean)
    }

    @Test
    fun boundedLocalRangeIsNotOverriddenByProviderMetadata() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "open-range.jar",
                    modId = "open-range",
                    name = "Open Range",
                    loader = ModLoader.FABRIC,
                    // 带上界的精确声明：本地已精确证明 1.21.11 兼容，不允许被 provider 推翻
                    minecraftVersionRanges = listOf(">=1.21.10 <1.21.12")
                )
            ),
            minecraftVersion = MINECRAFT_VERSION,
            loader = ModLoader.FABRIC,
            providerGameVersions = mapOf("open-range.jar" to listOf("1.21.10"))
        )

        assertTrue(report.isClean)
    }

    // —— 开放下界声明（例如真实 Balm 的 ">=1.21.10"）：本地声明只是作者声明，不构成发布级证据 ——

    @Test
    fun broadOpenLocalRangeWithProviderReleaseExcludingCurrentVersionIsReported() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "balm-fabric-1.21.10-21.10.11.jar",
                    modId = "balm",
                    name = "Balm",
                    loader = ModLoader.FABRIC,
                    // 真实 fixture 的声明：开放下界，本地无法证明发布级兼容
                    minecraftVersionRanges = listOf(">=1.21.10")
                )
            ),
            minecraftVersion = MINECRAFT_VERSION,
            loader = ModLoader.FABRIC,
            providerGameVersions = mapOf(
                "balm-fabric-1.21.10-21.10.11.jar" to listOf("1.21.10")
            )
        )

        assertEquals(1, report.problems.size)
        assertEquals(ModDoctorProblemType.WRONG_MINECRAFT_VERSION, report.problems.single().type)
        assertTrue(report.problems.single().reason.contains("1.21.10"))
        assertTrue(report.problems.single().reason.contains(MINECRAFT_VERSION))
    }

    @Test
    fun broadOpenLocalRangeWithProviderReleaseIncludingCurrentVersionIsNotReported() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "balm-fabric-1.21.11.jar",
                    modId = "balm",
                    name = "Balm",
                    loader = ModLoader.FABRIC,
                    minecraftVersionRanges = listOf(">=1.21.10")
                )
            ),
            minecraftVersion = MINECRAFT_VERSION,
            loader = ModLoader.FABRIC,
            providerGameVersions = mapOf(
                "balm-fabric-1.21.11.jar" to listOf("1.21.11", "1.21.10")
            )
        )

        assertTrue(report.isClean)
    }

    @Test
    fun broadOpenLocalRangeWithoutAuthoritativeProviderMatchIsNotReported() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "balm-fabric-1.21.10.jar",
                    modId = "balm",
                    name = "Balm",
                    loader = ModLoader.FABRIC,
                    minecraftVersionRanges = listOf(">=1.21.10")
                )
            ),
            minecraftVersion = MINECRAFT_VERSION,
            loader = ModLoader.FABRIC
        )

        assertTrue(report.isClean)
    }

    @Test
    fun broadOpenLocalRangeWithEmptyProviderGameVersionsIsNotReported() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "balm-unknown.jar",
                    modId = "balm",
                    name = "Balm",
                    loader = ModLoader.FABRIC,
                    minecraftVersionRanges = listOf(">=1.21.10")
                )
            ),
            minecraftVersion = MINECRAFT_VERSION,
            loader = ModLoader.FABRIC,
            providerGameVersions = mapOf("balm-unknown.jar" to emptyList())
        )

        assertTrue(report.isClean)
    }

    @Test
    fun modDoctorFingerprintLookupIgnoresUserSearchPlatform() {
        //用户搜索平台为 CurseForge（默认值）时，权威指纹健康检查仍必须走 Modrinth
        assertEquals(Platform.MODRINTH, modDoctorFingerprintPlatform(Platform.CURSEFORGE))
        //用户搜索平台本来就是 Modrinth 时行为不变
        assertEquals(Platform.MODRINTH, modDoctorFingerprintPlatform(Platform.MODRINTH))
    }

    @Test
    fun modDoctorDependencyRepairPlatformIgnoresUserSearchPlatform() {
        //依赖修复的平台规则：无论用户搜索平台偏好如何，都固定为 Modrinth
        assertEquals(Platform.MODRINTH, modDoctorDependencyRepairPlatform(Platform.CURSEFORGE))
        //用户搜索平台本来就是 Modrinth 时行为不变
        assertEquals(Platform.MODRINTH, modDoctorDependencyRepairPlatform(Platform.MODRINTH))
    }

    @Test
    fun missingDependencyRepairIgnoresUserSearchPlatform() = runBlocking {
        //用户搜索平台为 CurseForge（默认值）时，依赖修复的解析与候选仍必须落在 Modrinth
        val provider = ModDoctorProviderStub(searchPlatform = Platform.CURSEFORGE)

        val resolved = provider.resolve("fabric-api")

        assertTrue(resolved.hasFixableProblems)
        assertEquals(listOf("fabric-api" to Platform.MODRINTH), provider.directLookups)
        assertEquals(listOf(FABRIC_API_PROJECT_ID to Platform.MODRINTH), provider.versionLookups)
        assertEquals(Platform.MODRINTH, resolved.problems.single().fixCandidate?.platform)
    }

    @Test
    fun missingDependencySlugLookupIgnoresUserSearchPlatform() = runBlocking {
        //项目直接查询落空时的回退搜索，同样不得跟随用户搜索平台
        val provider = ModDoctorProviderStub(
            projectAvailable = false,
            searchPlatform = Platform.CURSEFORGE,
        )

        val resolved = provider.resolve("fabric-api")

        assertEquals(
            listOf(Triple("fabric-api", MINECRAFT_VERSION, Platform.MODRINTH)),
            provider.searches
        )
        assertTrue(!resolved.hasFixableProblems)
        assertTrue(resolved.problems.single().fixCandidate == null)
        assertTrue(
            resolved.problems.single().reason.contains(
                "the dependency project fabric-api was not found on MODRINTH."
            )
        )
    }

    @Test
    fun providerFailureOnBroadOpenLocalRangeLeavesItUnknown() = runBlocking {
        val versions = providerGameVersionsForAmbiguousMods(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "balm-offline.jar",
                    modId = "balm",
                    name = "Balm",
                    loader = ModLoader.FABRIC,
                    minecraftVersionRanges = listOf(">=1.21.10")
                )
            ),
            minecraftVersion = MINECRAFT_VERSION,
            lookup = { throw InterruptedIOException("timeout") }
        )

        assertTrue(versions.isEmpty())
        assertTrue(
            validateModDoctorMetadata(
                mods = listOf(
                    ModDoctorMetadata(
                        fileName = "balm-offline.jar",
                        modId = "balm",
                        name = "Balm",
                        loader = ModLoader.FABRIC,
                        minecraftVersionRanges = listOf(">=1.21.10")
                    )
                ),
                minecraftVersion = MINECRAFT_VERSION,
                loader = ModLoader.FABRIC,
                providerGameVersions = versions
            ).isClean
        )
    }

    @Test
    fun locallyIncompatibleMetadataIsReportedEvenWhenProviderListsCurrentVersion() {
        val report = validateModDoctorMetadata(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "old-mod.jar",
                    modId = "old-mod",
                    name = "Old Mod",
                    loader = ModLoader.FABRIC,
                    minecraftVersionRanges = listOf("1.20.1")
                )
            ),
            minecraftVersion = MINECRAFT_VERSION,
            loader = ModLoader.FABRIC,
            providerGameVersions = mapOf("old-mod.jar" to listOf(MINECRAFT_VERSION))
        )

        assertEquals(1, report.problems.size)
        assertEquals(ModDoctorProblemType.WRONG_MINECRAFT_VERSION, report.problems.single().type)
        assertTrue(report.problems.single().reason.contains("1.20.1"))
    }

    @Test
    fun providerEnrichmentQueriesBroadOpenDeclarationsButSkipsPreciseOnes() = runBlocking {
        val lookedUp = mutableListOf<String>()

        val versions = providerGameVersionsForAmbiguousMods(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "bounded.jar",
                    modId = "bounded",
                    name = "Bounded",
                    loader = ModLoader.FABRIC,
                    // 带上界：本地已精确证明兼容，不需要 provider
                    minecraftVersionRanges = listOf(">=1.21.10 <1.21.12")
                ),
                ModDoctorMetadata(
                    fileName = "incompatible.jar",
                    modId = "incompatible",
                    name = "Incompatible",
                    loader = ModLoader.FABRIC,
                    // 本地规则已上报，不需要 provider
                    minecraftVersionRanges = listOf("1.20.1")
                ),
                ModDoctorMetadata(
                    fileName = "broad.jar",
                    modId = "broad",
                    name = "Broad",
                    loader = ModLoader.FABRIC,
                    // 开放下界：本地无法证明发布级兼容，必须由 provider 补充
                    minecraftVersionRanges = listOf(">=1.21.10")
                )
            ),
            minecraftVersion = MINECRAFT_VERSION,
            lookup = { fileName ->
                lookedUp += fileName
                InstalledMod(
                    platform = Platform.MODRINTH,
                    projectId = "project-$fileName",
                    versionId = "version-$fileName",
                    versionName = "1.0",
                    gameVersions = listOf("1.21.10")
                )
            }
        )

        assertEquals(listOf("broad.jar"), lookedUp)
        assertEquals(mapOf("broad.jar" to listOf("1.21.10")), versions)
    }

    @Test
    fun providerEnrichmentKeepsOnlyAuthoritativeMatches() = runBlocking {
        val versions = providerGameVersionsForAmbiguousMods(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "matched.jar",
                    modId = "matched",
                    name = "Matched",
                    loader = ModLoader.FABRIC,
                    minecraftVersionRanges = listOf("~1.21-")
                ),
                ModDoctorMetadata(
                    fileName = "unmatched.jar",
                    modId = "unmatched",
                    name = "Unmatched",
                    loader = ModLoader.FABRIC,
                    minecraftVersionRanges = listOf("~1.21-")
                ),
                ModDoctorMetadata(
                    fileName = "negative.jar",
                    modId = "negative",
                    name = "Negative",
                    loader = ModLoader.FABRIC,
                    minecraftVersionRanges = listOf("~1.21-")
                )
            ),
            minecraftVersion = MINECRAFT_VERSION,
            lookup = { fileName ->
                when (fileName) {
                    "matched.jar" -> InstalledMod(
                        platform = Platform.MODRINTH,
                        projectId = "MBAkmtvl",
                        versionId = "9HjDG5Uj",
                        versionName = "21.10.11+fabric-1.21.10",
                        gameVersions = listOf("1.21.10")
                    )
                    "negative.jar" -> InstalledMod(
                        platform = Platform.MODRINTH,
                        projectId = "",
                        versionId = "",
                        versionName = "",
                        notFound = true
                    )
                    else -> null
                }
            }
        )

        assertEquals(mapOf("matched.jar" to listOf("1.21.10")), versions)
    }

    @Test
    fun providerFailureLeavesAmbiguousModsUnknown() = runBlocking {
        val versions = providerGameVersionsForAmbiguousMods(
            mods = listOf(
                ModDoctorMetadata(
                    fileName = "offline.jar",
                    modId = "offline",
                    name = "Offline",
                    loader = ModLoader.FABRIC,
                    minecraftVersionRanges = listOf("~1.21-")
                )
            ),
            minecraftVersion = MINECRAFT_VERSION,
            lookup = { throw InterruptedIOException("timeout") }
        )

        assertTrue(versions.isEmpty())

        // 无 provider 数据时，模糊的本地声明依旧不允许阻塞启动
        assertTrue(
            validateModDoctorMetadata(
                mods = listOf(
                    ModDoctorMetadata(
                        fileName = "offline.jar",
                        modId = "offline",
                        name = "Offline",
                        loader = ModLoader.FABRIC,
                        minecraftVersionRanges = listOf("~1.21-")
                    )
                ),
                minecraftVersion = MINECRAFT_VERSION,
                loader = ModLoader.FABRIC,
                providerGameVersions = versions
            ).isClean
        )
    }

    @Test
    fun matchedProviderVersionCarriesDeclaredGameVersions() {
        val version = ModrinthVersion(
            name = "Balm 21.10.11",
            versionNumber = "21.10.11+fabric-1.21.10",
            dependencies = emptyArray(),
            gameVersions = arrayOf("1.21.10"),
            versionType = PlatformReleaseType.RELEASE,
            loaders = arrayOf("fabric"),
            featured = false,
            status = "listed",
            id = "9HjDG5Uj",
            projectId = "MBAkmtvl",
            authorId = "author-id",
            datePublished = "2026-07-01T00:00:00Z",
            downloads = 0L,
            files = emptyArray()
        )

        val installed = version.toInstalledMod()

        assertEquals("MBAkmtvl", installed.projectId)
        assertEquals(listOf("1.21.10"), installed.gameVersions)
    }
}

/** 目标实例：Minecraft 1.21.11 + Fabric，与已捕获的 Mod Doctor 现场一致 */
private const val MINECRAFT_VERSION = "1.21.11"

/** Fabric API 在 Modrinth 上的权威项目 Id */
private const val FABRIC_API_PROJECT_ID = "P7dR8mSH"

/**
 * 通过生产入口注入 provider 边界，并记录 provider 侧被调用的查询。
 * 候选、解析顺序与失败原因全部来自生产代码，测试不复制解析算法。
 */
private class ModDoctorProviderStub(
    private val projectAvailable: Boolean = true,
    private val directFailure: Throwable? = null,
    private val searchPlatform: Platform = Platform.MODRINTH,
) {
    val directLookups = mutableListOf<Pair<String, Platform>>()
    val searches = mutableListOf<Triple<String, String, Platform>>()
    val versionLookups = mutableListOf<Pair<String, Platform>>()

    suspend fun resolve(dependencyId: String): ModDoctorReport {
        val problem = ModDoctorProblem(
            type = ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY,
            fileName = "continuity-3.0.1-beta.1+1.21.11.jar",
            modId = "continuity",
            reason = "Continuity requires $dependencyId, but no enabled installation was found.",
            requiredDependencyId = dependencyId,
        )
        return ModDoctorReport(listOf(problem)).withFixCandidates(
            minecraftVersion = MINECRAFT_VERSION,
            loader = ModLoader.FABRIC,
            platform = searchPlatform,
            directProject = { id, platform ->
                directLookups += id to platform
                directFailure?.let { throw it }
                FabricApiProject.takeIf { projectAvailable }
            },
            searchedProject = { id, minecraftVersion, platform ->
                searches += Triple(id, minecraftVersion, platform)
                FabricApiProject.takeIf { projectAvailable }
            },
            projectVersions = { id, platform ->
                versionLookups += id to platform
                listOf(FabricApiVersion)
            },
        )
    }
}

/** 只提供生产解析路径真正读取的字段 */
private object FabricApiProject : PlatformProject {
    override fun platform() = Platform.MODRINTH
    override fun platformId() = FABRIC_API_PROJECT_ID
    override fun platformSlug() = "fabric-api"
    override fun platformTitle() = "Fabric API"
    override fun platformClasses(defaultClasses: PlatformClasses) = defaultClasses
    override fun platformIconUrl() = null
    override fun platformSummary() = null
    override fun platformAuthor() = null
    override fun platformDownloadCount() = 0L
    override fun platformFollows() = null
    override fun platformModLoaders(): List<PlatformDisplayLabel> = listOf(FabricLoaderLabel)
    override fun checkClasses() = Unit
    override fun platformCategories(classes: PlatformClasses) = null
    override fun platformUrls(defaultClasses: PlatformClasses) = PlatformProject.Urls()
    override fun platformScreenshots() = emptyList<PlatformProject.Screenshot>()
}

private object FabricLoaderLabel : PlatformDisplayLabel {
    override fun getDisplayName() = "Fabric"
    override fun index() = 0
}

private object FabricApiVersion : PlatformVersion {
    override suspend fun initFile(currentProjectId: String) = true
    override fun platform() = Platform.MODRINTH
    override fun platformId() = "6qAuTtLR"
    override fun platformProjectId() = FABRIC_API_PROJECT_ID
    override fun platformDisplayName() = "0.141.6+1.21.11"
    override fun platformFileName() = "fabric-api-0.141.6+1.21.11.jar"
    override fun platformGameVersion() = arrayOf(MINECRAFT_VERSION)
    override fun platformLoaders(): List<PlatformDisplayLabel> = listOf(FabricLoaderLabel)
    override fun platformReleaseType() = PlatformReleaseType.RELEASE
    override fun platformDependencies() = emptyList<PlatformVersion.PlatformDependency>()
    override fun platformDownloadCount() = 0L
    override fun platformDownloadUrl() = "https://cdn.modrinth.com/data/$FABRIC_API_PROJECT_ID/versions/6qAuTtLR/fabric-api-0.141.6+1.21.11.jar"
    override fun platformDatePublished() = Instant.parse("2026-07-28T08:48:35Z")
    override fun platformSha1() = null
    override fun platformFileSize() = 0L
    override fun platformVersion() = "0.141.6+1.21.11"
}
