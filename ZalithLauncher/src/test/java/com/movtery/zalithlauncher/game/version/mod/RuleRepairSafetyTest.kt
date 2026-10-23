package com.movtery.zalithlauncher.game.version.mod

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 规则修复的整组安全：落盘前预检「假设结果」（planResultRejection）、落盘后复检（postFixRegression）。
 * 全部使用现有的整组校验器 resultingSetRejections；这里不下载、不改任何文件。
 */
class RuleRepairSafetyTest {
    private fun mod(id: String, version: String, requires: Map<String, List<String>> = emptyMap(), breaks: Map<String, List<String>> = emptyMap()) =
        InstalledModIdentity(id = id, version = version, requires = requires, breaks = breaks, fileName = "$id-$version.jar")

    private fun facts(id: String, version: String, requires: Map<String, List<String>> = emptyMap(), breaks: Map<String, List<String>> = emptyMap()) =
        FabricModFacts(fileName = "$id-$version.jar", id = id, version = version, breaks = breaks, requires = requires)

    /** 一个计划里的一项：被替换的文件名 + 暂存 jar 自己声明的事实 */
    private data class Step(val fileName: String, val facts: FabricModFacts)

    private fun planned(fileName: String, f: FabricModFacts) = Step(fileName, f)

    private fun planResultRejection(enabled: List<InstalledModIdentity>, steps: List<Step>) =
        planResultRejection(enabled, steps.map { it.fileName }, steps.map { it.facts })

    //真机上的整组：Sodium 0.8.14 声明 breaks iris<=1.10.7；RSO 2.2.4 要求 sodium =0.8.14
    private val sodium814 = mod("sodium", "0.8.14+mc1.21.11", breaks = mapOf("iris" to listOf("<=1.10.7")))
    private val extra = mod("sodium-extra", "0.9.1+mc1.21.11", requires = mapOf("sodium" to listOf(">=0.8.0")))
    private val iris = mod("iris", "1.10.7+mc1.21.11", requires = mapOf("sodium" to listOf("0.8.x")))
    private val rso224 = mod("reeses-sodium-options", "2.2.4+mc1.21.11", requires = mapOf("sodium" to listOf("=0.8.14+mc1.21.11")))
    private val enabled = listOf(sodium814, extra, iris, rso224)

    private val newSodium812 = planned("sodium-0.8.14+mc1.21.11.jar", facts("sodium", "0.8.12+mc1.21.11"))
    private val newRso223 = planned(
        "reeses-sodium-options-2.2.4+mc1.21.11.jar",
        facts("reeses-sodium-options", "2.2.3+mc1.21.11", requires = mapOf("sodium" to listOf("=0.8.12+mc1.21.11"))),
    )

    @Test
    fun theWholeSetPlanPassesThePreApplyCheck() {
        assertNull(planResultRejection(enabled, listOf(newSodium812, newRso223)))
    }

    @Test
    fun thePairOnlyPlanIsRefusedBecauseRsoWouldBreak() {
        val reason = planResultRejection(enabled, listOf(newSodium812))

        assertNotNull("sodium 0.8.12 alone leaves RSO 2.2.4 needing =0.8.14", reason)
        assertTrue(reason!!, reason.contains("reeses-sodium-options"))
    }

    @Test
    fun aPlanThatKeepsTheConflictIsRefused() {
        //只换 RSO：Sodium 0.8.14 仍然 breaks Iris，而新的 RSO 又要求 sodium =0.8.12 —— 新问题，预检就拒绝
        assertNotNull(planResultRejection(enabled, listOf(newRso223)))
    }

    @Test
    fun aPlanThatBreaksAnotherModIsRefused() {
        val brokenExtra = enabled.map { if (it.id == "sodium-extra") mod("sodium-extra", "0.9.1+mc1.21.11", requires = mapOf("sodium" to listOf(">=0.9.0"))) else it }

        val reason = planResultRejection(brokenExtra, listOf(newSodium812, newRso223))

        assertNotNull("a mod that is not part of the plan but depends on the replaced one must be re-checked", reason)
        assertTrue(reason!!, reason.contains("sodium-extra"))
    }

    @Test
    fun aProblemThatExistedBeforeAndIsUnrelatedDoesNotBlockTheFix() {
        val litematica = mod("litematica", "0.26.16", requires = mapOf("malilib" to listOf(">=0.27.19-")))

        assertNull(
            "litematica was already missing malilib; the sodium repair did not cause it",
            planResultRejection(enabled + litematica, listOf(newSodium812, newRso223)),
        )
    }

    @Test
    fun anUnreadableStagedFileNeverCountsAsASafePlan() {
        //暂存文件读不出身份：整组里少了 sodium，依赖 sodium 的模组立刻成为新问题
        val unreadable = planned("sodium-0.8.14+mc1.21.11.jar", FabricModFacts(fileName = "x.jar", id = null, version = null, breaks = emptyMap(), requires = emptyMap()))

        assertNotNull(planResultRejection(enabled, listOf(unreadable, newRso223)))
    }

    // ---- 修复后复检 ----

    private fun problem(type: ModDoctorProblemType, modId: String, fileName: String = "$modId.jar", target: String? = null, withRule: Boolean = false) =
        ModDoctorProblem(
            type = type, fileName = fileName, modId = modId, reason = "r", breaksTargetModId = target,
            ruleFallbackFix = if (withRule) ModDoctorSetFix(emptyList(), emptyMap()) else null,
        )

    private val pairProblem = problem(ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR, "sodium", target = "iris", withRule = true)

    @Test
    fun aCleanRescanAfterTheFixPasses() {
        assertNull(postFixRegression(ModDoctorReport(listOf(pairProblem)), ModDoctorReport(emptyList())))
    }

    @Test
    fun aNewProblemAfterTheFixIsAFailureEvenIfTheTargetIsGone() {
        val after = ModDoctorReport(listOf(problem(ModDoctorProblemType.INCOMPATIBLE_DEPENDENCY_VERSION, "reeses-sodium-options")))

        assertNotNull(postFixRegression(ModDoctorReport(listOf(pairProblem)), after))
    }

    @Test
    fun theTargetProblemStillReportedIsAFailureEvenWithADifferentFileName() {
        val after = ModDoctorReport(listOf(pairProblem.copy(fileName = "sodium-0.8.12.jar, iris.jar")))

        assertNotNull(postFixRegression(ModDoctorReport(listOf(pairProblem)), after))
    }

    @Test
    fun anUnrelatedProblemThatWasAlreadyThereDoesNotFailTheRepair() {
        val old = problem(ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY, "litematica").copy(requiredDependencyId = "malilib")
        val before = ModDoctorReport(listOf(pairProblem, old))
        val after = ModDoctorReport(listOf(old))

        assertNull(postFixRegression(before, after))
    }

    // ---- 校验器本身没有被改变 ----

    @Test
    fun theSingleReasonValidatorStillReturnsTheFirstReason() {
        val reasons = resultingSetRejections(enabled)

        assertEquals(reasons.firstOrNull(), resultingSetRejection(enabled))
        assertEquals("the real quartet with Sodium 0.8.14 breaks Iris", "sodium declares that it breaks iris 1.10.7+mc1.21.11", reasons.first())
    }
}
