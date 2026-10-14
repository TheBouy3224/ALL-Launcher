/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.version.mod

/**
 * 崩溃页面的「安装依赖并修复」：从 Fabric 日志读出缺失依赖的证据，并在 Mod Doctor 的报告里找到同一条问题。
 *
 * 这里不下载、不解析模组、不做校验和快照：这些全部由 [ModDoctor] 的现有修复入口负责。
 * 这里只回答三件事：日志点名了什么、Mod Doctor 是否认同这条问题、修复应当限定在哪一条问题上。
 */

/** 诊断类别名，与 `crash_signatures.json` 里缺失依赖规则的 class 一致 */
const val DIAGNOSIS_CLASS_MISSING_DEPENDENCY = "loader_missing_dependency"

/**
 * 日志点名的一条缺失依赖。
 *
 * @param requirerName 需要依赖的模组显示名，例如 `Litematica`
 * @param requirerId 需要依赖的模组 id，例如 `litematica`
 * @param requirerVersion 需要依赖的模组版本，例如 `0.26.16`
 * @param dependencyId 缺失的依赖 id，例如 `malilib`
 * @param constraint 版本范围，例如 `>=0.27.19- <0.28.0-`；优先取 `Reason:` 行的机器格式，没有则取英文原句
 * @param evidence 证据原文（逐行，去掉首尾空白），给用户看
 */
data class MissingDependencyEvidence(
    val requirerName: String,
    val requirerId: String,
    val requirerVersion: String,
    val dependencyId: String,
    val constraint: String,
    val evidence: List<String>,
)

/** Fabric 的缺失依赖原句：`Mod 'Litematica' (litematica) 0.26.16 requires <范围> of malilib, which is missing!` */
private val MISSING_REQUIREMENT_LINE = Regex(
    """Mod '([^']+)' \(([^)]+)\)\s+(\S+)\s+requires\s+(.+?)\s+of\s+(\S+),\s+which is missing!"""
)

/** 加载器的机器格式：`Reason: [HARD_DEP litematica 0.26.16 {depends malilib @ [>=0.27.19- <0.28.0-]}]` */
private val HARD_DEPENDENCY_REASON = Regex(
    """\[HARD_DEP\s+(\S+)\s+(\S+)\s+\{depends\s+(\S+)\s+@\s+\[([^\]]+)\]\}\]"""
)

/**
 * 从捕获日志里读出缺失依赖的证据；没有 Fabric 的缺失依赖原句时返回 null。
 *
 * 纯函数：不读文件、不碰 Android。只认原句，不靠猜；机器格式的 `Reason:` 行只在它点名的
 * 是同一个模组、同一个依赖时才采用，否则版本范围就用原句里的英文。
 */
fun readMissingDependencyEvidence(text: String): MissingDependencyEvidence? {
    val requirement = MISSING_REQUIREMENT_LINE.find(text) ?: return null
    val requirerName = requirement.groupValues[1]
    val requirerId = requirement.groupValues[2]
    val requirerVersion = requirement.groupValues[3]
    val humanRange = requirement.groupValues[4].trim()
    val dependencyId = requirement.groupValues[5]

    val reason = HARD_DEPENDENCY_REASON.find(text)?.takeIf {
        it.groupValues[1] == requirerId && it.groupValues[3] == dependencyId
    }
    val machineRange = reason?.groupValues?.get(4)?.trim().orEmpty()

    return MissingDependencyEvidence(
        requirerName = requirerName,
        requirerId = requirerId,
        requirerVersion = requirerVersion,
        dependencyId = dependencyId,
        constraint = machineRange.ifEmpty { humanRange },
        evidence = buildList {
            add(requirement.value.trim())
            reason?.let { add(it.value.trim()) }
        },
    )
}

/** Mod Doctor 对日志里这条缺失依赖的结论 */
sealed interface DependencyRepairSelection {
    /** 报告里有这条问题，并且已经挑出一个通过全部校验的候选：可以出预览 */
    data class Ready(val problem: ModDoctorProblem, val candidate: ModDoctorFixCandidate) : DependencyRepairSelection

    /** 报告里有这条问题，但没有安全候选：只说明原因，不安装任何东西 */
    data class NoSafeVersion(val problem: ModDoctorProblem) : DependencyRepairSelection

    /** 报告里没有这条问题：要么已经满足，要么 Mod Doctor 不认为它缺失。两种情况都不安装任何东西 */
    object NotReported : DependencyRepairSelection
}

/** 在 Mod Doctor 的报告里找到日志点名的那一条缺失依赖：需要者与依赖 id 都必须对上 */
fun selectDependencyRepair(report: ModDoctorReport, evidence: MissingDependencyEvidence): DependencyRepairSelection {
    val problem = report.problems.firstOrNull {
        it.type == ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY &&
            it.modId == evidence.requirerId &&
            it.requiredDependencyId == evidence.dependencyId
    } ?: return DependencyRepairSelection.NotReported
    val candidate = problem.fixCandidate ?: return DependencyRepairSelection.NoSafeVersion(problem)
    return DependencyRepairSelection.Ready(problem, candidate)
}

/** 修复只针对这一条问题：报告里的其它问题（如果有）不会被顺手改动 */
fun ModDoctorReport.restrictedTo(problem: ModDoctorProblem): ModDoctorReport = ModDoctorReport(problems = listOf(problem))

/**
 * 修复之后重新检查：这条依赖的需求是否已经不再被报告。
 * 已安装但版本不满足声明（INCOMPATIBLE_DEPENDENCY_VERSION）同样算未解决，因为加载器同样会拒绝启动。
 */
fun dependencyRequirementResolved(report: ModDoctorReport, evidence: MissingDependencyEvidence): Boolean =
    report.problems.none {
        it.modId == evidence.requirerId && (
            (it.type == ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY && it.requiredDependencyId == evidence.dependencyId) ||
                it.type == ModDoctorProblemType.INCOMPATIBLE_DEPENDENCY_VERSION
            )
    }
