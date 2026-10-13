package com.movtery.zalithlauncher.game.version.mod

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Universal Mod Doctor V1 (Plan-only): การชนจาก breaks ของม็อดใดก็ได้ ไม่จำกัดกลุ่ม Sodium
 * ทดสอบเฉพาะ Planner (ข้อมูลเป็นค่าคงที่ ไม่มีเครือข่าย)
 */
class UniversalBreaksPlanTest {
    /** foo@2.0 ประกาศว่าชน bar<=1.0; bar@1.0 ติดตั้งอยู่; baz ไม่เกี่ยวข้อง */
    private fun installed(
        barVersion: String? = "1.0",
        fooBreaks: Map<String, List<String>> = mapOf("bar" to listOf("<=1.0")),
    ) = listOf(
        GroupPlanInstalled("foo", "foo.jar", "2.0", "p-foo", breaks = fooBreaks),
        GroupPlanInstalled("bar", "bar.jar", barVersion, "p-bar"),
        GroupPlanInstalled("baz", "baz.jar", "3.0", "p-baz"),
    )

    private fun cand(
        modId: String,
        name: String,
        requires: Set<String> = emptySet(),
        incompat: Set<String> = emptySet(),
    ) = GroupPlanCandidate(
        modId = modId, projectId = "p-$modId", versionName = name, fileName = "$modId-$name.jar",
        gameVersions = setOf("1.21.11"), loaders = setOf("fabric"),
        requiredModIds = requires, incompatibleModIds = incompat,
        releaseType = GroupPlanReleaseType.RELEASE,
    )

    private fun plan(
        inst: List<GroupPlanInstalled> = installed(),
        candidates: Map<String, List<GroupPlanCandidate>>,
        isPackOwned: (String) -> Boolean = { false },
    ) = planBreaksConflicts("1.21.11", "fabric", inst, findBreaksConflicts(inst), candidates, isPackOwned)

    @Test
    fun conflictOutsideSodiumGroupPlansOnlyTheConflictingPair() {
        val plan = plan(candidates = mapOf("bar" to listOf(cand("bar", "mc1.21.11-1.1-fabric"))))
        assertEquals(GroupPlanStatus.PLAN, plan.status)
        assertEquals(listOf("bar"), plan.changes.map { it.modId })
        assertEquals("mc1.21.11-1.1-fabric", plan.changes.single().toVersion)
        assertTrue(plan.causes.single().contains("foo"))
        assertTrue(plan.risks.isNotEmpty())
    }

    @Test
    fun crossGroupDependencyNotInstalledIsUnverified() {
        val plan = plan(candidates = mapOf("bar" to listOf(cand("bar", "mc1.21.11-1.1-fabric", requires = setOf("qux")))))
        assertEquals(GroupPlanStatus.UNVERIFIED, plan.status)
        assertTrue(plan.changes.isEmpty())
    }

    @Test
    fun crossGroupDependencyInstalledIsPlanned() {
        val plan = plan(candidates = mapOf("bar" to listOf(cand("bar", "mc1.21.11-1.1-fabric", requires = setOf("baz")))))
        assertEquals(GroupPlanStatus.PLAN, plan.status)
        assertEquals(listOf("bar"), plan.changes.map { it.modId })
    }

    @Test
    fun candidateThatIncompatibleWithTheSourceIsNotChosen() {
        val plan = plan(candidates = mapOf("bar" to listOf(cand("bar", "mc1.21.11-1.1-fabric", incompat = setOf("foo")))))
        assertEquals(GroupPlanStatus.UNVERIFIED, plan.status)
        assertTrue(plan.changes.isEmpty())
    }

    @Test
    fun noBreaksMeansNoConflictAndNoPlan() {
        val inst = installed(fooBreaks = emptyMap())
        assertTrue(findBreaksConflicts(inst).isEmpty())
    }

    @Test
    fun missingTargetVersionIsNotGuessedAsConflict() {
        //ไม่รู้เวอร์ชันของเป้าหมาย → ไม่สรุปว่าชน (metadata ไม่ครบ)
        assertTrue(findBreaksConflicts(installed(barVersion = null)).isEmpty())
    }

    @Test
    fun unparseableCandidateVersionIsUnverified() {
        //ชื่อเวอร์ชันบนแพลตฟอร์มไม่บอกเวอร์ชันของ jar ได้แน่ชัด → ห้ามเดาว่าพ้นเงื่อนไข
        val plan = plan(candidates = mapOf("bar" to listOf(cand("bar", "latest-release"))))
        assertEquals(GroupPlanStatus.UNVERIFIED, plan.status)
        assertTrue(plan.changes.isEmpty())
    }

    @Test
    fun packOwnedTargetIsBlockedNotChanged() {
        val plan = plan(
            candidates = mapOf("bar" to listOf(cand("bar", "mc1.21.11-1.1-fabric"))),
            isPackOwned = { it == "bar.jar" },
        )
        assertEquals(GroupPlanStatus.BLOCKED_PACK_OWNED, plan.status)
        assertTrue(plan.changes.isEmpty())
    }

    @Test
    fun lookupBudgetIsEnforcedBeforeAnyRequest() {
        fun ev(i: Int) = BreaksEvidence(
            GroupPlanInstalled("s$i", "s$i.jar", "2.0"),
            GroupPlanInstalled("t$i", "t$i.jar", "1.0"),
            "<=1.0",
        )
        //4 คู่ = 8 ไฟล์ (ยังอยู่ในขีดจำกัด) ; 5 คู่ = 10 ไฟล์ (เกิน) → null ไม่ค้นหาบางส่วน
        assertEquals(8, planLookupFiles((0 until 4).map(::ev))!!.size)
        assertNull(planLookupFiles((0 until 5).map(::ev)))
    }

    @Test
    fun knownGoodIsNotUsedToDowngradeTheUniversalPlan() {
        //แผนที่มาจาก breaks ต้องไม่เสนอ downgrade ของ foo หรือ baz
        val plan = plan(candidates = mapOf("bar" to listOf(cand("bar", "mc1.21.11-1.1-fabric"))))
        assertFalse(plan.changes.any { it.modId == "foo" || it.modId == "baz" })
        assertTrue(plan.suggestions.isEmpty())
    }
}
