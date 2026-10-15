package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 修复失败后点“重试”：带“修复未完成”标记的报告必须触发重新检查，否则失败的那一项会被静默跳过 */
class ModDoctorRetryAfterFailureTest {
    private fun problem(suffixes: List<ModDoctorReasonSuffix> = emptyList()) = ModDoctorProblem(
        type = ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY,
        fileName = "jade.jar",
        modId = "jade",
        reason = "Jade requires fabric-api.",
        reasonSuffixes = suffixes,
        requiredDependencyId = "fabric-api",
    )

    @Test
    fun failedRepairMarkRequiresAFreshInspectBeforeTheRetry() {
        val failed = ModDoctorReport(listOf(problem(listOf(ModDoctorReasonSuffix(R.string.mod_doctor_reason_repair_incomplete)))))
        assertTrue(failed.hasIncompleteRepairMark())
    }

    @Test
    fun aFreshReportNeedsNoReinspection() {
        assertFalse(ModDoctorReport(listOf(problem())).hasIncompleteRepairMark())
    }

    @Test
    fun otherSuffixesDoNotTriggerReinspection() {
        val packManaged = ModDoctorReport(listOf(problem(listOf(ModDoctorReasonSuffix(R.string.mod_doctor_reason_pack_managed)))))
        assertFalse(packManaged.hasIncompleteRepairMark())
    }
}
