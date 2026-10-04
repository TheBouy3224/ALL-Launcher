package com.movtery.zalithlauncher.game.version.mod

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 自动修复的候选偏好：先看正式版（各自保持平台给出的“最新在前”顺序），
 * 只有在没有任何可用的正式版时，才轮到预发布版本；兼容性判断仍复用既有逻辑。
 */
class ModDoctorStableFirstCandidateTest {
    private val enabledMods = listOf(
        InstalledModIdentity("iris", "1.10.7+mc1.21.11", requires = mapOf("sodium" to listOf("0.8.x"))),
        InstalledModIdentity("sodium-extra", "0.9.1+mc1.21.11", requires = mapOf("sodium" to listOf(">=0.8.1"))),
        InstalledModIdentity("reeses-sodium-options", "2.2.3+mc1.21.11", requires = mapOf("sodium" to listOf(">=0.8.11"))),
    )

    /** 平台给出的候选顺序始终是“最新在前” */
    private val newestFirst = listOf(
        candidate("sodium-fabric-0.8.14+mc1.21.11.jar", "0.8.14+mc1.21.11", irisBreak = "<=1.10.7", prerelease = false),
        candidate("sodium-fabric-0.8.14-beta.2+mc1.21.11.jar", "0.8.14-beta.2+mc1.21.11", irisBreak = "<=1.10.7", prerelease = true),
        candidate("sodium-fabric-0.8.13+mc1.21.11.jar", "0.8.13+mc1.21.11", irisBreak = "<=1.10.7", prerelease = false),
        candidate("sodium-fabric-0.8.13-beta.2+mc1.21.11.jar", "0.8.13-beta.2+mc1.21.11", irisBreak = "<=1.10.6", prerelease = true),
        candidate("sodium-fabric-0.8.12+mc1.21.11.jar", "0.8.12+mc1.21.11", irisBreak = "<=1.10.6", prerelease = false),
    )

    @Test
    fun case1_aCompatibleStableIsPreferredOverANewerCompatiblePrerelease() = runBlocking {
        val choice = select(
            listOf(
                candidate("sodium-fabric-0.8.13-beta.2+mc1.21.11.jar", "0.8.13-beta.2+mc1.21.11", irisBreak = "<=1.10.6", prerelease = true),
                candidate("sodium-fabric-0.8.12+mc1.21.11.jar", "0.8.12+mc1.21.11", irisBreak = "<=1.10.6", prerelease = false),
            )
        )

        assertEquals("sodium-fabric-0.8.12+mc1.21.11.jar", choice.accepted?.fileName)
        assertTrue(choice.rejections.isEmpty())
    }

    @Test
    fun case2_incompatibleStableCandidatesAreSkippedAndTheNextStableWins() = runBlocking {
        val choice = select(newestFirst)

        assertEquals("sodium-fabric-0.8.12+mc1.21.11.jar", choice.accepted?.fileName)
        assertEquals(2, choice.rejections.size)
        assertTrue(choice.rejections[0].contains("sodium-fabric-0.8.14+mc1.21.11.jar"))
        assertTrue(choice.rejections[1].contains("sodium-fabric-0.8.13+mc1.21.11.jar"))
    }

    @Test
    fun case3_aCompatiblePrereleaseIsUsedOnlyWhenNoStableCandidateFits() = runBlocking {
        val choice = select(newestFirst.filterNot { it.fileName.contains("0.8.12+") })

        assertEquals("sodium-fabric-0.8.13-beta.2+mc1.21.11.jar", choice.accepted?.fileName)
        //正式版全部被拒后才轮到预发布版本，其中更新的预发布版本同样要先通过兼容性校验
        assertEquals(3, choice.rejections.size)
        assertTrue(choice.rejections[0].contains("sodium-fabric-0.8.14+mc1.21.11.jar"))
        assertTrue(choice.rejections[1].contains("sodium-fabric-0.8.13+mc1.21.11.jar"))
        assertTrue(choice.rejections[2].contains("sodium-fabric-0.8.14-beta.2+mc1.21.11.jar"))
    }

    @Test
    fun case4_noCandidateIsInstalledWhenNothingIsCompatible() = runBlocking {
        val choice = select(
            listOf(
                candidate("sodium-fabric-0.8.14+mc1.21.11.jar", "0.8.14+mc1.21.11", irisBreak = "<=1.10.7", prerelease = false),
                candidate("sodium-fabric-0.8.13+mc1.21.11.jar", "0.8.13+mc1.21.11", irisBreak = "<=1.10.7", prerelease = false),
                candidate("sodium-fabric-0.8.13-beta.2+mc1.21.11.jar", "0.8.13-beta.2+mc1.21.11", irisBreak = "<=1.10.7", prerelease = true),
            )
        )

        assertNull(choice.accepted)
        assertEquals(3, choice.rejections.size)
        assertNull(choice.failure)
    }

    @Test
    fun stableCandidatesKeepTheirNewestFirstOrderAndStayAheadOfPrereleases() {
        val ordered = stableFirstCandidates(newestFirst) { it.prerelease }

        assertEquals(
            listOf(
                "sodium-fabric-0.8.14+mc1.21.11.jar",
                "sodium-fabric-0.8.13+mc1.21.11.jar",
                "sodium-fabric-0.8.12+mc1.21.11.jar",
                "sodium-fabric-0.8.14-beta.2+mc1.21.11.jar",
                "sodium-fabric-0.8.13-beta.2+mc1.21.11.jar",
            ),
            ordered.map { it.fileName }
        )
    }

    /** 与生产路径一致：稳定优先排序 → 逐个做兼容性校验 */
    private suspend fun select(candidates: List<Candidate>): ModDoctorCandidateChoice<Candidate> =
        firstAcceptedCandidate(stableFirstCandidates(candidates) { it.prerelease }) { candidate ->
            val reason = candidateRejectionReason(candidate.facts, enabledMods)
            if (reason == null) {
                ModDoctorCandidateAttempt.Accepted(candidate)
            } else {
                ModDoctorCandidateAttempt.Rejected(reason)
            }
        }

    private fun candidate(
        fileName: String,
        version: String,
        irisBreak: String,
        prerelease: Boolean,
    ) = Candidate(
        facts = FabricModFacts(
            fileName = fileName,
            id = "sodium",
            version = version,
            breaks = mapOf("iris" to listOf(irisBreak)),
            requires = emptyMap(),
        ),
        prerelease = prerelease,
    )

    private data class Candidate(
        val facts: FabricModFacts,
        val prerelease: Boolean,
    ) {
        val fileName: String get() = facts.fileName
    }
}
