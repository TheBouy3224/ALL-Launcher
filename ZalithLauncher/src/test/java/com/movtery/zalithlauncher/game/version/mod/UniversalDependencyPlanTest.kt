package com.movtery.zalithlauncher.game.version.mod

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Universal Mod Doctor V2 (Plan-only): dependency ที่ขาดทั่วทั้งอินสแตนซ์ ใช้ผู้ให้บริการที่ resolve แล้วเดิม
 * ทดสอบเฉพาะ Planner ไม่มีเครือข่าย
 */
class UniversalDependencyPlanTest {
    private fun finding(
        dependencyId: String,
        requiredBy: String,
        project: String? = "p-$dependencyId",
        version: String? = "mc1.21.11-2.0-fabric",
        reason: String? = null,
    ) = MissingDependencyFinding(dependencyId, requiredBy, project, version, reason)

    private fun breaksPlan(status: GroupPlanStatus = GroupPlanStatus.PLAN, changes: List<GroupPlanChange> = emptyList()) = GroupPlan(
        status = status,
        confidence = GroupPlanConfidence.HIGH,
        changes = changes,
        notes = if (status == GroupPlanStatus.UNVERIFIED) listOf("breaks ยังยืนยันไม่ได้") else emptyList(),
        risks = emptyList(),
    )

    private val barChange = GroupPlanChange("bar", "bar.jar", "1.0", "bar-1.1.jar", "mc1.21.11-1.1-fabric", "p-bar", "breaks")

    @Test
    fun sameDependencyRequiredByManyModsIsInstalledOnce() {
        val plan = planMissingDependencies(listOf(
            finding("fabric-api", requiredBy = "alpha"),
            finding("fabric-api", requiredBy = "beta"),
            finding("fabric-api", requiredBy = "gamma"),
        ))
        assertEquals(1, plan.installs.size)
        assertEquals(listOf("alpha", "beta", "gamma"), plan.installs.single().requiredBy)
        assertEquals(listOf("fabric-api"), plan.installs.single().dependencyIds)
        assertTrue(plan.unverified.isEmpty())
    }

    @Test
    fun differentDependenciesResolvingToTheSameProjectAreInstalledOnce() {
        val plan = planMissingDependencies(listOf(
            finding("alias-a", requiredBy = "alpha", project = "p-shared"),
            finding("alias-b", requiredBy = "beta", project = "p-shared"),
        ))
        assertEquals(1, plan.installs.size)
        assertEquals(listOf("alias-a", "alias-b"), plan.installs.single().dependencyIds)
    }

    @Test
    fun unresolvedDependencyIsUnverifiedWithItsOriginalReasonAndNotGuessed() {
        val plan = planMissingDependencies(listOf(
            finding("mystery-lib", requiredBy = "alpha", project = null, version = null,
                reason = "the dependency project mystery-lib was not found on Modrinth"),
        ))
        assertTrue(plan.installs.isEmpty())
        assertEquals(1, plan.unverified.size)
        assertTrue(plan.unverified.single().contains("mystery-lib"))
        assertTrue(plan.unverified.single().contains("was not found on Modrinth"))
    }

    @Test
    fun noMissingDependencyMeansNoDependencyPlan() {
        val plan = planMissingDependencies(emptyList())
        assertTrue(plan.installs.isEmpty())
        assertTrue(plan.unverified.isEmpty())
        assertNull((null as GroupPlan?).withDependencyPlan(plan, packManaged = false))
    }

    @Test
    fun versionNameComesFromTheResolvedCandidateOnly() {
        val plan = planMissingDependencies(listOf(finding("fabric-api", requiredBy = "alpha", version = "mc1.21.11-2.0-fabric")))
        assertEquals("mc1.21.11-2.0-fabric", plan.installs.single().versionName)
    }

    @Test
    fun dependencyInstallNeverChangesOtherMods() {
        val deps = planMissingDependencies(listOf(finding("fabric-api", requiredBy = "alpha")))
        val merged = breaksPlan(GroupPlanStatus.NO_CHANGE).withDependencyPlan(deps, packManaged = false)!!
        assertTrue(merged.changes.isEmpty())
        assertEquals(listOf("fabric-api"), merged.dependencyInstalls.single().dependencyIds)
        assertEquals(GroupPlanStatus.PLAN, merged.status)
        assertEquals(GroupPlanConfidence.MEDIUM, merged.confidence)
    }

    @Test
    fun breaksAndMissingDependencyAreBothShown() {
        val deps = planMissingDependencies(listOf(finding("fabric-api", requiredBy = "alpha")))
        val merged = breaksPlan(changes = listOf(barChange)).withDependencyPlan(deps, packManaged = false)!!
        assertEquals(listOf("bar"), merged.changes.map { it.modId })
        assertEquals(listOf("fabric-api"), merged.dependencyInstalls.single().dependencyIds)
        assertEquals(GroupPlanStatus.PLAN, merged.status)
    }

    @Test
    fun unverifiedBreaksIsNotHiddenByAResolvedDependency() {
        val deps = planMissingDependencies(listOf(finding("fabric-api", requiredBy = "alpha")))
        val merged = breaksPlan(GroupPlanStatus.UNVERIFIED).withDependencyPlan(deps, packManaged = false)!!
        assertEquals(GroupPlanStatus.UNVERIFIED, merged.status)
        assertTrue(merged.notes.contains("breaks ยังยืนยันไม่ได้"))
        assertEquals(1, merged.dependencyInstalls.size)
    }

    @Test
    fun unverifiedDependencyMakesThePlanUnverifiedAndKeepsResolvedOnes() {
        val deps = planMissingDependencies(listOf(
            finding("fabric-api", requiredBy = "alpha"),
            finding("mystery-lib", requiredBy = "beta", project = null, version = null, reason = "no provider"),
        ))
        val merged = breaksPlan(changes = listOf(barChange)).withDependencyPlan(deps, packManaged = false)!!
        assertEquals(GroupPlanStatus.UNVERIFIED, merged.status)
        assertEquals(1, merged.dependencyInstalls.size)
        assertTrue(merged.notes.any { it.contains("mystery-lib") })
    }

    @Test
    fun packManagedInstanceBlocksDependencyInstallWithoutPlanningIt() {
        val deps = planMissingDependencies(listOf(finding("fabric-api", requiredBy = "alpha")))
        val merged = (null as GroupPlan?).withDependencyPlan(deps, packManaged = true)!!
        assertEquals(GroupPlanStatus.BLOCKED_PACK_OWNED, merged.status)
        assertTrue(merged.dependencyInstalls.isEmpty())
        assertTrue(merged.notes.any { it.contains("แพ็ก") })
    }

    @Test
    fun unverifiedDependencyOnlyPlanIsUnverifiedNotNoChange() {
        val deps = planMissingDependencies(listOf(finding("mystery-lib", requiredBy = "alpha", project = null, version = null, reason = "no provider")))
        val merged = (null as GroupPlan?).withDependencyPlan(deps, packManaged = false)!!
        assertEquals(GroupPlanStatus.UNVERIFIED, merged.status)
        assertEquals(GroupPlanConfidence.UNVERIFIED, merged.confidence)
        assertFalse(merged.status == GroupPlanStatus.NO_CHANGE)
    }
}
