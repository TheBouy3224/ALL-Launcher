package com.movtery.zalithlauncher.game.version.mod

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 依赖候选选择：候选自带的 Fabric 元数据与已启用模组冲突时必须被拒，
 * 并继续尝试次新的候选，直到某个候选通过校验为止；
 * 全部候选都冲突时不返回任何可安装的候选。
 *
 * 用例数据取自真实模组元数据（Sodium 0.8.14 / 0.8.13 / 0.8.13-beta.2 与
 * Iris 1.10.7 / Sodium Extra 0.9.4 / 0.9.3 / Reese's Sodium Options 2.2.4 / 2.2.3）。
 */
class ModDoctorCandidateCompatibilityTest {
    /** 与 sodium-fabric-0.8.14+mc1.21.11.jar 内 fabric.mod.json 的 breaks 一致 */
    private val sodium814BreaksJson = """
        {
          "id": "sodium",
          "version": "0.8.14+mc1.21.11",
          "breaks": {
            "embeddium": "*",
            "optifabric": "*",
            "canvas": "*",
            "vulkanmod": "*",
            "optimalaim": "<2.0.0",
            "sodium-blendingregistry": "*",
            "ocrenderfix_sodium": "*",
            "betterfpsdist": "<=1.21-4.5",
            "bobby": "<5.2.4",
            "chunksfadein": "<2.0.2-1.21.2",
            "cull-less-leaves": "<=1.3.0",
            "cullleaves": "<=3.4.0-fabric",
            "custom_hud": "<3.4.2",
            "farsight": "<=1.21-4.3",
            "iceberg": "<1.2.7",
            "iris": "<=1.10.7",
            "movingelevators": "<=1.4.7",
            "notenoughcrashes": "<4.4.8",
            "noxesium": "<2.3.3",
            "reeses-sodium-options": "<2.0.2",
            "fabric-api": "<0.140.0",
            "sodium-extra": "<0.8.0",
            "audio_engine_tweaks": "<1.2.12",
            "sspb": "<6.0.0",
            "moreculling": "<1.6.0-beta.2",
            "simply-no-shading": "<7.6.2",
            "betterend": "<=21.0.11",
            "enchanteds_sodium_options": "<1.0.1"
          }
        }
    """.trimIndent()

    private val sodium814 = parseFabricModFacts("sodium-fabric-0.8.14+mc1.21.11.jar", sodium814BreaksJson)

    /** 0.8.13 的 breaks 与 0.8.14 相同；0.8.13-beta.2 的 iris 断点更低（<=1.10.6，真实 jar 值） */
    private val sodium813 = sodium814.copy(
        fileName = "sodium-fabric-0.8.13+mc1.21.11.jar",
        version = "0.8.13+mc1.21.11",
    )

    private val sodium813Beta2 = sodium814.copy(
        fileName = "sodium-fabric-0.8.13-beta.2+mc1.21.11.jar",
        version = "0.8.13-beta.2+mc1.21.11",
        breaks = sodium814.breaks + ("iris" to listOf("<=1.10.6")),
    )

    private val sodiumCandidates = listOf(sodium814, sodium813, sodium813Beta2)

    private val iris = InstalledModIdentity("iris", "1.10.7+mc1.21.11", requires = mapOf("sodium" to listOf("0.8.x")))
    private val fabricApi = InstalledModIdentity("fabric-api", "0.141.6+1.21.11")
    private val sodiumExtra094 = InstalledModIdentity(
        id = "sodium-extra",
        version = "0.9.4+mc1.21.11",
        requires = mapOf("sodium" to listOf(">=0.8.14+mc1.21.11")),
    )
    private val reeses224 = InstalledModIdentity(
        id = "reeses-sodium-options",
        version = "2.2.4+mc1.21.11",
        requires = mapOf("sodium" to listOf("=0.8.14+mc1.21.11")),
    )
    private val sodiumExtra093 = InstalledModIdentity(
        id = "sodium-extra",
        version = "0.9.3+mc1.21.11",
        requires = mapOf("sodium" to listOf(">=0.8.13+mc1.21.11")),
    )
    private val reeses223 = InstalledModIdentity(
        id = "reeses-sodium-options",
        version = "2.2.3+mc1.21.11",
        requires = mapOf("sodium" to listOf(">=0.8.11")),
    )

    /** Iris 1.10.7 + 一条只要求 sodium >=0.8.11 的 Reese's：0.8.13-beta.2 是唯一同时满足两个方向的候选 */
    private val reesesOnlyFixture = listOf(iris, fabricApi, reeses223)

    /** 最新版 Sodium Extra / Reese's：分别要求 sodium >=0.8.14 与 =0.8.14 */
    private val latestFixture = listOf(iris, fabricApi, sodiumExtra094, reeses224)

    /** 旧一版 Reese's（>=0.8.11）与旧一版 Sodium Extra（>=0.8.13） */
    private val olderFixture = listOf(iris, fabricApi, sodiumExtra093, reeses223)

    @Test
    fun newestCandidateThatBreaksAnEnabledModIsRejectedAndAnOlderOneIsSelected() = runBlocking {
        val choice = firstAcceptedCandidate(sodiumCandidates) { candidate -> attempt(candidate, reesesOnlyFixture) }

        assertEquals("sodium-fabric-0.8.13-beta.2+mc1.21.11.jar", choice.accepted?.fileName)
        assertEquals(2, choice.rejections.size)
        assertTrue(choice.rejections[0].contains("sodium-fabric-0.8.14+mc1.21.11.jar"))
        assertTrue(choice.rejections[0].contains("iris 1.10.7+mc1.21.11"))
        assertTrue(choice.rejections[1].contains("sodium-fabric-0.8.13+mc1.21.11.jar"))
        assertNull(choice.failure)
    }

    @Test
    fun aPrereleaseCandidateDoesNotSatisfyAReleaseRequirement() = runBlocking {
        val choice = firstAcceptedCandidate(sodiumCandidates) { candidate -> attempt(candidate, olderFixture) }

        assertNull(choice.accepted)
        assertEquals(3, choice.rejections.size)
        assertTrue(choice.rejections[2].contains("sodium-extra 0.9.3+mc1.21.11 requires sodium >=0.8.13+mc1.21.11"))
    }

    @Test
    fun aCandidateThatDoesNotSatisfyAnEnabledModsRequirementIsRejectedAsWell() = runBlocking {
        val choice = firstAcceptedCandidate(sodiumCandidates) { candidate -> attempt(candidate, latestFixture) }

        assertNull(choice.accepted)
        assertEquals(3, choice.rejections.size)
        assertTrue(choice.rejections[2].contains("sodium-fabric-0.8.13-beta.2+mc1.21.11.jar"))
        //原因里给出的是第一个未被满足的要求
        assertTrue(choice.rejections[2].contains("sodium-extra 0.9.4+mc1.21.11 requires sodium >=0.8.14+mc1.21.11"))
        assertTrue(choice.rejections[2].contains("provides 0.8.13-beta.2+mc1.21.11"))
    }

    @Test
    fun noInstallCandidateIsReturnedWhenEveryCandidateConflicts() = runBlocking {
        val choice = firstAcceptedCandidate(sodiumCandidates) { candidate ->
            ModDoctorCandidateAttempt.Rejected("${candidate.fileName} conflicts")
        }

        assertNull(choice.accepted)
        assertNull(choice.failure)
        assertEquals(3, choice.rejections.size)
    }

    @Test
    fun declaredBreaksThatNoInstalledModMatchesDoNotRejectTheCandidate() {
        val withoutIris = latestFixture.filter { it.id != "iris" }

        assertNull(candidateConflictReason(sodium814.fileName, sodium814.breaks, withoutIris))
    }

    @Test
    fun theInstalledModsOwnDependsRangesAreHonouredInBothDirections() {
        assertNull(candidateRejectionReason(sodium813Beta2, reesesOnlyFixture))
        assertTrue(candidateRejectionReason(sodium813Beta2, latestFixture) != null)
        assertTrue(candidateRejectionReason(sodium814, latestFixture) != null)
        assertNull(candidateRejectionReason(sodium814, reesesOnlyFixture.filter { it.id != "iris" }))
    }

    @Test
    fun theRealSodiumDeclarationsAreParsedAndEvaluated() {
        assertEquals(listOf("<=1.10.7"), sodium814.breaks["iris"])
        assertEquals(listOf("<0.8.0"), sodium814.breaks["sodium-extra"])
        assertEquals(listOf("<2.0.2"), sodium814.breaks["reeses-sodium-options"])
        assertEquals("sodium", sodium814.id)
        assertEquals("0.8.14+mc1.21.11", sodium814.version)

        val extraDepends = parseFabricDepends(
            """
            {
              "id": "sodium-extra",
              "version": "0.9.4+mc1.21.11",
              "depends": {
                "minecraft": ">=1.21.11",
                "fabricloader": ">=0.16.0",
                "fabric-api": "*",
                "sodium": ">=0.8.14+mc1.21.11"
              }
            }
            """.trimIndent()
        )

        assertEquals(setOf("fabric-api", "sodium"), extraDepends.keys)
        assertEquals(listOf(">=0.8.14+mc1.21.11"), extraDepends["sodium"])
    }

    @Test
    fun fabricPredicateSemanticsFollowTheLoaderRules() {
        assertTrue(fabricVersionMatches("1.10.7+mc1.21.11", "<=1.10.7"))
        assertFalse(fabricVersionMatches("1.10.7+mc1.21.11", "<=1.10.6"))
        assertTrue(fabricVersionMatches("0.8.14+mc1.21.11", "=0.8.14+mc1.21.11"))
        assertFalse(fabricVersionMatches("0.8.13-beta.2+mc1.21.11", ">=0.8.14+mc1.21.11"))
        //预发布版本低于同号正式版本，因此严格的 >=0.8.13 不接受 0.8.13-beta.2
        assertFalse(fabricVersionMatches("0.8.13-beta.2+mc1.21.11", ">=0.8.13+mc1.21.11"))
        assertTrue(fabricVersionMatches("0.8.13-beta.2+mc1.21.11", ">=0.8.11"))
        assertFalse(fabricVersionMatches("0.141.6+1.21.11", "<0.140.0"))
        assertTrue(fabricVersionMatches("0.8.12+mc1.21.11", "0.8.x"))
        assertFalse(fabricVersionMatches("0.9.4+mc1.21.11", "0.8.x"))
        assertTrue(fabricVersionMatches("0.9.4+mc1.21.11", "*"))
        assertTrue(fabricVersionMatches("1.2.3", ">=1.0.0 <2.0.0"))
        assertFalse(fabricVersionMatches("1.2.3", ">=1.0.0 <1.2.0"))
        assertTrue(fabricVersionMatches("1.10.7", "~1.10.0"))
        assertFalse(fabricVersionMatches("1.11.0", "~1.10.0"))
        assertTrue(fabricVersionMatches("1.2.3-beta.1", "<1.2.3"))
        assertFalse(fabricVersionMatches(null, "*"))
    }

    @Test
    fun installedModsBreaksRejectsTheConflictingTargetAndSelectsANewerOne() = runBlocking {
        val installedSodium = InstalledModIdentity(
            id = "sodium",
            version = "0.8.14+mc1.21.11",
            breaks = mapOf("iris" to listOf("<=1.10.7")),
        )
        val iris107 = FabricModFacts(
            fileName = "iris-fabric-1.10.7+mc1.21.11.jar",
            id = "iris",
            version = "1.10.7+mc1.21.11",
            breaks = emptyMap(),
            requires = mapOf("sodium" to listOf("0.8.x")),
        )
        val iris108 = iris107.copy(
            fileName = "iris-fabric-1.10.8+mc1.21.11.jar",
            version = "1.10.8+mc1.21.11",
        )

        val choice = firstAcceptedCandidate(listOf(iris107, iris108)) { candidate ->
            val rejection = candidateRejectionReason(candidate, listOf(installedSodium))
            if (rejection == null) ModDoctorCandidateAttempt.Accepted(candidate)
            else ModDoctorCandidateAttempt.Rejected(rejection)
        }

        assertEquals("iris-fabric-1.10.8+mc1.21.11.jar", choice.accepted?.fileName)
        assertEquals(1, choice.rejections.size)
        assertTrue(choice.rejections.single().contains("sodium 0.8.14+mc1.21.11"))
        assertTrue(choice.rejections.single().contains("iris 1.10.7+mc1.21.11"))
    }

    @Test
    fun aFailedAttemptStopsTheSequence() = runBlocking {
        val choice = firstAcceptedCandidate(listOf(1, 2, 3)) { attempt ->
            if (attempt == 2) ModDoctorCandidateAttempt.Failed("download failed")
            else ModDoctorCandidateAttempt.Rejected("rejected $attempt")
        }

        assertNull(choice.accepted)
        assertEquals("download failed", choice.failure)
        assertEquals(1, choice.rejections.size)
    }

    @Test
    fun atMostTheConfiguredNumberOfCandidatesIsAttempted() = runBlocking {
        var attempts = 0

        val choice = firstAcceptedCandidate(listOf(1, 2, 3, 4, 5, 6), maxAttempts = 2) { candidate ->
            attempts++
            ModDoctorCandidateAttempt.Rejected("rejected $candidate")
        }

        assertNull(choice.accepted)
        assertEquals(2, attempts)
        assertEquals(2, choice.rejections.size)
    }

    /**
     * 真实现场（QA 2026-09-21）：修复选中 Sodium 0.8.12，而两个已启用模组用带构建元数据的谓词
     * （`=0.8.14+mc1.21.11`、`>=0.8.14+mc1.21.11`）要求 0.8.14。两个依赖方都必须被判成「被卡住」：
     * 少判一个，多模组计划就只替换 Sodium 一个文件，加载器随后以 HARD_DEP 拒绝整组。
     */
    @Test
    fun buildMetadataRequirementsAreDecidedAndBothBlockedDependentsAreReported() {
        val blocking = blockingDependentIds("sodium", "0.8.12+mc1.21.11", latestFixture)

        //iris 只要求 0.8.x，0.8.12 满足它：不得被误判成被卡住
        assertEquals(listOf("sodium-extra", "reeses-sodium-options"), blocking)
    }

    /** 谓词里的构建元数据是谓词自己的写法：候选版本没有构建元数据时同样必须能判定 */
    @Test
    fun aRequirementWrittenWithBuildMetadataIsDecidedAgainstAPlainCandidateVersion() {
        assertEquals(
            listOf("sodium-extra", "reeses-sodium-options"),
            blockingDependentIds("sodium", "0.8.12", latestFixture),
        )
    }

    @Test
    fun candidateCompatibilityReportsBothBlockedDependentsForTheSodiumCandidate() {
        val sodium812 = FabricModFacts(
            fileName = "sodium-fabric-0.8.12+mc1.21.11.jar",
            id = "sodium",
            version = "0.8.12+mc1.21.11",
            breaks = emptyMap(),
            requires = emptyMap(),
        )

        val compatibility = candidateCompatibility(sodium812, latestFixture)

        assertNull("0.8.12 不声明 breaks：硬拒绝原因必须为空", compatibility.reason)
        assertEquals(listOf("sodium-extra", "reeses-sodium-options"), compatibility.blockingDependents)
        assertTrue(
            compatibility.requiresReason.orEmpty()
                .contains("sodium-extra 0.9.4+mc1.21.11 requires sodium >=0.8.14+mc1.21.11")
        )
    }

    /**
     * 只替换那一个文件的结果集合必须被拒（对应加载器的 HARD_DEP 行为）。
     * 整组结果按生产路径构造：被替换的文件退出，候选自己的事实加入。
     */
    @Test
    fun theOneFileReplacementResultIsRejectedByTheWholeSetCheck() {
        val installed = sodium814.asInstalledIdentity()!!
        val oneFilePlanResult = (latestFixture + installed).filter { it.fileName != sodium814.fileName } +
                InstalledModIdentity(
                    id = "sodium",
                    version = "0.8.12+mc1.21.11",
                    fileName = "sodium-fabric-0.8.12+mc1.21.11.jar",
                )

        val rejection = resultingSetRejection(oneFilePlanResult)

        assertNotNull("一个文件的计划必须被整组校验拒绝", rejection)
        assertTrue(rejection!!.contains("sodium-extra"))
        assertTrue(rejection.contains(">=0.8.14+mc1.21.11"))
        assertTrue(rejection.contains("0.8.12+mc1.21.11"))
    }

    /** 与生产路径一致：候选先落在暂存位，用自己的 Fabric 元数据校验后才安装 */
    private fun attempt(
        candidate: FabricModFacts,
        enabled: List<InstalledModIdentity>,
    ): ModDoctorCandidateAttempt<FabricModFacts> {
        val reason = candidateRejectionReason(candidate, enabled)
        return if (reason == null) {
            ModDoctorCandidateAttempt.Accepted(candidate)
        } else {
            ModDoctorCandidateAttempt.Rejected(reason)
        }
    }
}
