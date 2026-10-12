package com.movtery.zalithlauncher.game.version.mod

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModGroupPlannerTest {
    private val rule = ModDoctorSetRule(
        minecraft = "1.21.11",
        loader = "fabric",
        whenPresent = listOf("sodium", "sodium-extra", "reeses-sodium-options", "iris"),
        allowed = mapOf(
            "sodium" to "0.8.12",
            "sodium-extra" to "0.9.1",
            "reeses-sodium-options" to "2.2.3",
            "iris" to "1.10.7",
        ),
    )

    private fun installedAll(
        sodium: String = "0.8.12", extra: String = "0.9.1", reese: String = "2.2.3", iris: String = "1.10.7",
    ) = listOf(
        GroupPlanInstalled("sodium", "sodium.jar", sodium),
        GroupPlanInstalled("sodium-extra", "sodium-extra.jar", extra),
        GroupPlanInstalled("reeses-sodium-options", "reese.jar", reese),
        GroupPlanInstalled("iris", "iris.jar", iris),
    )

    private fun cand(
        modId: String,
        version: String,
        mc: String = "1.21.11",
        loader: String = "fabric",
        requires: Set<String> = emptySet(),
        incompat: Set<String> = emptySet(),
    ) = GroupPlanCandidate(
        modId = modId, projectId = "p-$modId", versionName = version, fileName = "$modId-$version.jar",
        gameVersions = setOf(mc), loaders = setOf(loader),
        requiredModIds = requires, incompatibleModIds = incompat,
    )

    @Test
    fun silentWhenGroupIncomplete() {
        val plan = planModGroupRepair("1.21.11", "fabric",
            installed = installedAll().filterNot { it.modId == "iris" }, rule = rule, candidatesByModId = emptyMap())
        assertEquals(GroupPlanStatus.NO_CHANGE, plan.status)
        assertTrue(plan.changes.isEmpty())
    }

    @Test
    fun silentForOtherVersionOrLoader() {
        assertEquals(GroupPlanStatus.NO_CHANGE, planModGroupRepair("1.20.1", "fabric",
            installedAll(sodium = "0.5"), rule, emptyMap()).status)
        assertEquals(GroupPlanStatus.NO_CHANGE, planModGroupRepair("1.21.11", "forge",
            installedAll(sodium = "0.5"), rule, emptyMap()).status)
    }

    @Test
    fun noChangeWhenAlreadyMatching() {
        val plan = planModGroupRepair("1.21.11", "fabric", installedAll(), rule, emptyMap())
        assertEquals(GroupPlanStatus.NO_CHANGE, plan.status)
        assertEquals(GroupPlanConfidence.HIGH, plan.confidence)
        assertEquals(4, plan.notes.size)
    }

    @Test
    fun changesOnlyTheMismatchedMember() {
        val candidates = mapOf(
            "sodium" to listOf(cand("sodium", "0.8.12")),
            "sodium-extra" to listOf(cand("sodium-extra", "0.9.1")),
        )
        //sodium-extra เป็น 0.9.0 (ไม่ตรงกฎ) ต้องแก้เฉพาะตัวนี้
        val plan = planModGroupRepair("1.21.11", "fabric",
            installedAll(extra = "0.9.0"), rule, candidates)
        assertEquals(GroupPlanStatus.PLAN, plan.status)
        assertEquals(1, plan.changes.size)
        assertEquals("sodium-extra", plan.changes.single().modId)
        assertEquals("0.9.0", plan.changes.single().fromVersion)
        assertEquals("0.9.1", plan.changes.single().toVersion)
        assertTrue(plan.notes.any { it.startsWith("sodium ") })
    }

    @Test
    fun unverifiedWhenNoCandidateForTarget() {
        val plan = planModGroupRepair("1.21.11", "fabric",
            installedAll(iris = "1.9.0"), rule, mapOf("iris" to listOf(cand("iris", "1.10.6"))))
        assertEquals(GroupPlanStatus.UNVERIFIED, plan.status)
        assertEquals(GroupPlanConfidence.UNVERIFIED, plan.confidence)
        assertTrue(plan.changes.isEmpty())
    }

    @Test
    fun candidateWrongMinecraftVersionIsIgnored() {
        val plan = planModGroupRepair("1.21.11", "fabric",
            installedAll(iris = "1.9.0"), rule,
            mapOf("iris" to listOf(cand("iris", "1.10.7", mc = "1.21.10"))))
        assertEquals(GroupPlanStatus.UNVERIFIED, plan.status)
    }

    @Test
    fun candidateWrongLoaderIsIgnored() {
        val plan = planModGroupRepair("1.21.11", "fabric",
            installedAll(iris = "1.9.0"), rule,
            mapOf("iris" to listOf(cand("iris", "1.10.7", loader = "neoforge"))))
        assertEquals(GroupPlanStatus.UNVERIFIED, plan.status)
    }

    @Test
    fun unverifiedWhenRequiredModMissingFromSet() {
        val plan = planModGroupRepair("1.21.11", "fabric",
            installedAll(iris = "1.9.0"), rule,
            mapOf("iris" to listOf(cand("iris", "1.10.7", requires = setOf("indium")))))
        assertEquals(GroupPlanStatus.UNVERIFIED, plan.status)
        assertTrue(plan.notes.any { it.contains("indium") })
    }

    @Test
    fun requiredModPresentInstalledIsAccepted() {
        val installed = installedAll(iris = "1.9.0") + GroupPlanInstalled("indium", "indium.jar", "1.0")
        val plan = planModGroupRepair("1.21.11", "fabric", installed, rule,
            mapOf("iris" to listOf(cand("iris", "1.10.7", requires = setOf("indium")))))
        assertEquals(GroupPlanStatus.PLAN, plan.status)
    }

    @Test
    fun unverifiedWhenCandidateDeclaresConflictWithPresentMod() {
        val installed = installedAll(iris = "1.9.0") + GroupPlanInstalled("bad", "bad.jar", "1")
        val plan = planModGroupRepair("1.21.11", "fabric", installed, rule,
            mapOf("iris" to listOf(cand("iris", "1.10.7", incompat = setOf("bad")))))
        assertEquals(GroupPlanStatus.UNVERIFIED, plan.status)
    }

    @Test
    fun blockedWhenTargetIsPackOwned() {
        val plan = planModGroupRepair("1.21.11", "fabric", installedAll(iris = "1.9.0"), rule,
            mapOf("iris" to listOf(cand("iris", "1.10.7"))),
            isPackOwned = { it == "iris.jar" })
        assertEquals(GroupPlanStatus.BLOCKED_PACK_OWNED, plan.status)
        assertTrue(plan.changes.isEmpty())
    }

    @Test
    fun versionMatchIgnoresBuildMetadataAndDoesNotPrefixMatch() {
        val ok = planModGroupRepair("1.21.11", "fabric", installedAll(sodium = "0.8.12+mc1.21.11"), rule, emptyMap())
        assertEquals(GroupPlanStatus.NO_CHANGE, ok.status)
        val wrong = planModGroupRepair("1.21.11", "fabric", installedAll(sodium = "0.8.120"),
            rule, mapOf("sodium" to listOf(cand("sodium", "0.8.12"))))
        assertEquals(GroupPlanStatus.PLAN, wrong.status)
    }

    @Test
    fun planNeverReportsHighWithoutExplicitDependencyData() {
        val plan = planModGroupRepair("1.21.11", "fabric", installedAll(iris = "1.9.0"), rule,
            mapOf("iris" to listOf(cand("iris", "1.10.7"))))
        assertEquals(GroupPlanStatus.PLAN, plan.status)
        assertEquals(GroupPlanConfidence.MEDIUM, plan.confidence)
        assertTrue(plan.risks.any { it.contains("ยังไม่ได้ทดสอบ") })
    }

    private val family = setOf("sodium", "sodium-extra", "reeses-sodium-options", "iris")

    @Test
    fun discoveryUnverifiedWhenAnotherMemberHasNoCandidates() {
        val plan = planModGroupDiscovery("1.21.12", "fabric",
            installed = installedAll(), familyModIds = family,
            candidatesByModId = mapOf("sodium" to listOf(cand("sodium", "0.8.12", mc = "1.21.12"))))
        //sodium รองรับอยู่แล้ว แต่สมาชิกอื่นไม่มีผู้สมัครเลย -> ตรวจไม่ได้ จึงต้องไม่เสนอแผน
        assertEquals(GroupPlanStatus.UNVERIFIED, plan.status)
    }

    @Test
    fun discoveryProposesOnlyTheUnsupportedMember() {
        val installed = installedAll()
        val candidates = mapOf(
            "sodium" to listOf(cand("sodium", "0.8.12", mc = "1.21.11"), cand("sodium", "0.9.0", mc = "1.21.12"), cand("sodium", "0.9.2", mc = "1.21.12")),
            "sodium-extra" to listOf(cand("sodium-extra", "0.9.1", mc = "1.21.12")),
            "reeses-sodium-options" to listOf(cand("reeses-sodium-options", "2.2.3", mc = "1.21.12")),
            "iris" to listOf(cand("iris", "1.10.7", mc = "1.21.12")),
        )
        val plan = planModGroupDiscovery("1.21.12", "fabric", installed, family, candidates)
        assertEquals(GroupPlanStatus.PLAN, plan.status)
        assertEquals(1, plan.changes.size)
        assertEquals("sodium", plan.changes.single().modId)
        assertEquals("0.9.2", plan.changes.single().toVersion)
        assertEquals(GroupPlanConfidence.MEDIUM, plan.confidence)
    }

    @Test
    fun discoveryPlanIsMediumAndNotKnownGood() {
        val installed = listOf(GroupPlanInstalled("sodium", "sodium.jar", "0.8.12"))
        val plan = planModGroupDiscovery("1.21.12", "fabric", installed, family,
            mapOf("sodium" to listOf(cand("sodium", "0.9.2", mc = "1.21.12"), cand("sodium", "0.9.0", mc = "1.21.12"))))
        assertEquals(GroupPlanStatus.PLAN, plan.status)
        assertEquals(GroupPlanConfidence.MEDIUM, plan.confidence)
        assertEquals("0.9.2", plan.changes.single().toVersion)
        assertEquals(false, plan.fromKnownGoodRule)
        assertTrue(plan.risks.any { it.contains("Known-good") })
    }

    @Test
    fun discoveryIgnoresNonFamilyMods() {
        val plan = planModGroupDiscovery("1.21.12", "fabric",
            installed = listOf(GroupPlanInstalled("fabric-api", "fabric-api.jar", "0.1")), familyModIds = family,
            candidatesByModId = emptyMap())
        assertEquals(GroupPlanStatus.NO_CHANGE, plan.status)
    }

    @Test
    fun unresolvedRequiredDependencyMakesPlanUnverified() {
        val plan = planModGroupRepair("1.21.11", "fabric", installedAll(iris = "1.9.0"), rule,
            mapOf("iris" to listOf(cand("iris", "1.10.7").copy(unresolvedRequiredProjects = setOf("ABC123")))))
        assertEquals(GroupPlanStatus.UNVERIFIED, plan.status)
        assertTrue(plan.notes.any { it.contains("ABC123") })
    }

    @Test
    fun knownGoodRuleStillUsedWhenItMatches() {
        val plan = planModGroupRepair("1.21.11", "fabric", installedAll(iris = "1.9.0"), rule,
            mapOf("iris" to listOf(cand("iris", "1.10.7"))))
        assertTrue(plan.fromKnownGoodRule)
    }

    @Test
    fun versionOrderIsNumeric() {
        assertTrue(compareVersionText("1.10.7", "1.9.0") > 0)
        assertTrue(compareVersionText("0.9.2", "0.9.10") < 0)
        assertEquals(0, compareVersionText("2.2.3", "2.2.3"))
    }

    // ---- รูปแบบชื่อจริงของ Modrinth ----

    private fun real(modId: String, name: String, type: GroupPlanReleaseType, mc: String = "1.21.11", project: String = "p-$modId") =
        cand(modId, name, mc = mc).copy(projectId = project, releaseType = type)

    @Test
    fun realModrinthNameMatchesJarVersion() {
        assertTrue(candidateMatchesVersion("mc1.21.11-0.8.14-fabric", "0.8.14+mc1.21.11"))
        assertTrue(candidateMatchesVersion("mc1.21.11-0.9.4+fabric", "0.9.4+mc1.21.11"))
        assertTrue(candidateMatchesVersion("mc1.21.11-0.8.15-beta.1-fabric", "0.8.15-beta.1+mc1.21.11"))
    }

    @Test
    fun prereleaseNameIsNotMatchedAsStable() {
        assertEquals(false, candidateMatchesVersion("mc1.21.11-0.8.14-beta.1-fabric", "0.8.14+mc1.21.11"))
        assertEquals(false, candidateMatchesVersion("mc1.21.11-0.8.14-rc.2-fabric", "0.8.14"))
    }

    @Test
    fun wrongOrPrefixVersionIsNotMatched() {
        assertEquals(false, candidateMatchesVersion("mc1.21.11-0.8.140-fabric", "0.8.14+mc1.21.11"))
        assertEquals(false, candidateMatchesVersion("mc1.21.11-1.0.8.14-fabric", "0.8.14"))
        assertEquals(false, candidateMatchesVersion("mc1.21.11-0.8.15-beta.10-fabric", "0.8.15-beta.1"))
    }

    @Test
    fun healthySodiumIsNotUpgradedBecauseOfNameFormat() {
        //QA จริง: sodium 0.8.14 และ sodium-extra 0.9.4 รองรับ 1.21.11 อยู่แล้ว แม้มี beta 0.8.15 ที่ใหม่กว่า
        val installed = listOf(
            GroupPlanInstalled("sodium", "sodium.jar", "0.8.14+mc1.21.11", projectId = "AANobbMI"),
            GroupPlanInstalled("sodium-extra", "sodium-extra.jar", "0.9.4+mc1.21.11", projectId = "PtjYWJkn"),
        )
        val candidates = mapOf(
            "sodium" to listOf(
                real("sodium", "mc1.21.11-0.8.15-beta.1-fabric", GroupPlanReleaseType.BETA, project = "AANobbMI"),
                real("sodium", "mc1.21.11-0.8.14-fabric", GroupPlanReleaseType.RELEASE, project = "AANobbMI"),
            ),
            "sodium-extra" to listOf(real("sodium-extra", "mc1.21.11-0.9.4+fabric", GroupPlanReleaseType.RELEASE, project = "PtjYWJkn")),
        )
        val plan = planModGroupDiscovery("1.21.11", "fabric", installed, family, candidates)
        assertEquals(GroupPlanStatus.NO_CHANGE, plan.status)
        assertTrue(plan.changes.isEmpty())
    }

    @Test
    fun proposesStableBeforeBeta() {
        val installed = listOf(GroupPlanInstalled("sodium", "sodium.jar", "0.7.3+mc1.21.10", projectId = "AANobbMI"))
        val candidates = mapOf("sodium" to listOf(
            real("sodium", "mc1.21.11-0.8.15-beta.1-fabric", GroupPlanReleaseType.BETA, project = "AANobbMI"),
            real("sodium", "mc1.21.11-0.8.14-fabric", GroupPlanReleaseType.RELEASE, project = "AANobbMI"),
        ))
        val plan = planModGroupDiscovery("1.21.11", "fabric", installed, family, candidates)
        assertEquals(GroupPlanStatus.PLAN, plan.status)
        assertEquals("mc1.21.11-0.8.14-fabric", plan.changes.single().toVersion)
        assertEquals(GroupPlanConfidence.MEDIUM, plan.confidence)
    }

    @Test
    fun betaOnlyIsProposedWithExplicitWarning() {
        val installed = listOf(GroupPlanInstalled("sodium", "sodium.jar", "0.7.3+mc1.21.10", projectId = "AANobbMI"))
        val candidates = mapOf("sodium" to listOf(
            real("sodium", "mc1.21.11-0.8.15-beta.1-fabric", GroupPlanReleaseType.BETA, project = "AANobbMI"),
        ))
        val plan = planModGroupDiscovery("1.21.11", "fabric", installed, family, candidates)
        assertEquals(GroupPlanStatus.PLAN, plan.status)
        assertTrue(plan.notes.any { it.contains("ไม่มีรีลีส stable") })
        assertTrue(plan.risks.any { it.contains("ไม่ใช่ stable") })
    }

    @Test
    fun candidatesFromAnotherProjectAreIgnored() {
        val installed = listOf(GroupPlanInstalled("sodium", "sodium.jar", "0.7.3+mc1.21.10", projectId = "AANobbMI"))
        val candidates = mapOf("sodium" to listOf(
            real("sodium", "mc1.21.11-0.8.14-fabric", GroupPlanReleaseType.RELEASE, project = "OTHER"),
        ))
        val plan = planModGroupDiscovery("1.21.11", "fabric", installed, family, candidates)
        assertEquals(GroupPlanStatus.UNVERIFIED, plan.status)
    }

    @Test
    fun knownGoodRulePrefersStableAndMatchesRealNames() {
        val installed = installedAll(iris = "1.9.0").map { it.copy(projectId = "p-${it.modId}") }
        val plan = planModGroupRepair("1.21.11", "fabric", installed, rule, mapOf("iris" to listOf(
            real("iris", "1.10.7+mc1.21.11-beta.2", GroupPlanReleaseType.BETA),
            real("iris", "mc1.21.11-1.10.7-fabric", GroupPlanReleaseType.RELEASE),
        )))
        assertEquals(GroupPlanStatus.PLAN, plan.status)
        assertEquals("mc1.21.11-1.10.7-fabric", plan.changes.single().toVersion)
        assertTrue(plan.fromKnownGoodRule)
    }
}
