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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */
package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformProject
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformReleaseType
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion
import com.movtery.zalithlauncher.utils.file.calculateFileSha1
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * 成套修复规划器（ONE repair plan）。
 *
 * 目标：同一个实例里有一组互相依赖 / 互相冲突的 Fabric 模组（例如 Sodium、Sodium Extra、Iris、
 * Reese's Sodium Options 与 Fabric API）时，不是一次只换一个模组，而是在**整组**上求一个自洽的版本组合：
 * 每个涉及的模组既可以保持不变，也可以升级 / 降级到同一个权威项目里的另一个构建，缺少的依赖可以安装。
 *
 * 全部判据都来自真实的 jar 元数据（`fabric.mod.json` 的 depends / breaks 与版本范围），
 * 这里**没有**任何按模组名或版本对硬编码的规则：候选先用已装模组声明的范围做不下载的预筛，
 * 再暂存下载，用候选自己声明的元数据通过 [resultingSetRejections] 校验整组结果。
 *
 * 搜索顺序（保证改动最少、先升级后降级）：
 * 1. 只允许升级；改动模组数 0（只装缺失依赖）→ 1 → 2 → … → [FamilyRepairBounds.maxReplacements]
 * 2. 允许降级，同样的顺序
 * 第一个让整组没有任何 depends / breaks 问题的组合就是计划。没有则安全拒绝并说明原因。
 *
 * 本类只暂存文件，不改 mods 目录；调用方对计划做 Preview，用户确认后由 [ModDoctorMultiModRepair.apply] 事务执行。
 */

/** 计划里一项改动的种类 */
enum class FamilyChangeKind { INSTALL, UPGRADE, DOWNGRADE, SWITCH, DISABLE, RESTORE }

/**
 * 计划里的一项改动
 * @param fileName 被替换（可逆禁用）的原文件；纯新增为空
 */
internal data class FamilyChange(
    val kind: FamilyChangeKind,
    val modId: String,
    val fileName: String,
    val fromVersion: String?,
    val toVersion: String?,
    val reasons: List<String>,
    val target: PlatformVersion,
    val stagedFile: File,
    val facts: FabricModFacts,
    val sha1: String,
    /** ไฟล์เดิมที่จะถูกแทนที่: SHA-1 ตอนวางแผน (ตรวจซ้ำก่อน Apply ว่าไฟล์ยังเหมือนเดิม); ติดตั้งใหม่ = null */
    val replacedSha1: String? = null,
    /** เป้าหมายเป็น Beta/Alpha: Preview ต้องเตือนผู้ใช้ */
    val prerelease: Boolean = false,
)

/** 一份完整的成套计划：全部文件已暂存，用户确认前什么都没有改 */
internal data class FamilyRepairPlan(
    val changes: List<FamilyChange>,
    /** 计划要解决的、修复前整组里存在的全部问题（原文，给用户看） */
    val problems: List<String>,
) {
    /** 计划的身份：Preview 与 Fix 各自规划一次，靠它确认两次是同一份计划 */
    val key: String
        get() = changes.joinToString("|") { "${it.fileName}>${it.target.platformId()}" }
}

/** 规划结果 */
internal sealed interface FamilyOutcome {
    /** 整组本来就自洽，没有可做的事 */
    data object NotApplicable : FamilyOutcome

    data class Plan(val plan: FamilyRepairPlan) : FamilyOutcome

    /** 有问题但没有安全计划：mods 目录没有任何改动 */
    data class Refused(val reason: String) : FamilyOutcome

    /** 基础设施失败（下载 / 查询）：mods 目录没有任何改动 */
    data class Failed(val reason: String) : FamilyOutcome
}

/** 规划界限：测试可以收紧 */
internal data class FamilyRepairBounds(
    /** 一个计划里最多改动多少个已安装的模组 */
    val maxReplacements: Int = 3,
    /** 每个模组最多考虑多少个候选（预筛之后） */
    val candidatesPerMod: Int = 10,
    /** 每个缺失依赖最多尝试多少个候选 */
    val candidatesPerInstall: Int = 6,
    /** 最多评估多少个组合 */
    val maxAssignments: Int = 60,
    /** 最多暂存下载多少个文件 */
    val stagedDownloads: Int = 30,
    val candidateTimeoutMillis: Long = REPAIR_CANDIDATE_TIMEOUT_MILLIS,
    val totalTimeoutMillis: Long = REPAIR_TOTAL_TIMEOUT_MILLIS,
)

/** 整组分析：缺失的依赖 id 与卷入 depends / breaks 冲突的模组 id */
internal data class FamilySetAnalysis(
    /** 缺失依赖 id -> 需要它的模组 id */
    val missing: Map<String, List<String>>,
    /** 卷入冲突（breaks 命中、依赖版本不满足）的模组 id，含冲突两侧 */
    val tangled: Set<String>,
    /** 原因文本（与 [resultingSetRejections] 同一套判据） */
    val reasons: List<String>,
) {
    val isConsistent: Boolean get() = reasons.isEmpty()
}

/**
 * 分析整组：与 [resultingSetRejections] 使用同一套求值器（真实 jar 元数据、版本谓词、提供者），
 * 只是把原因结构化成“缺失依赖”与“卷入冲突的模组”，供规划器决定要动谁。
 */
internal fun analyzeFamilySet(mods: List<InstalledModIdentity>): FamilySetAnalysis {
    val missing = LinkedHashMap<String, MutableList<String>>()
    val tangled = LinkedHashSet<String>()
    val byId = mods.filter { it.id.isNotBlank() }.groupBy { it.id }

    for (source in mods) {
        for ((targetId, predicates) in source.breaks) {
            val target = byId[targetId].orEmpty().firstOrNull { it !== source } ?: continue
            val targetVersion = target.version?.takeIf { it.isNotBlank() } ?: continue
            if (versionPredicateDecision(predicates, targetVersion) != true) continue
            tangled += source.id
            tangled += target.id
        }
    }

    val providers = resultingSetProviders(mods)
    for (mod in mods) {
        for ((dependencyId, predicates) in mod.requires) {
            val candidates = providers[dependencyId].orEmpty()
            if (candidates.isEmpty()) {
                missing.getOrPut(dependencyId) { mutableListOf() } += mod.id
                continue
            }
            if (candidates.any { version -> installedDependencyVerdict(predicates, version) != false }) continue
            tangled += mod.id
            //版本不满足的提供者：文件自己的 id，或者它内嵌/别名提供这个 id 的那个文件的 id
            mods.filter { owner -> owner.id == dependencyId || dependencyId in owner.providedModIds }
                .forEach { owner -> tangled += owner.id }
        }
    }
    return FamilySetAnalysis(missing, tangled, resultingSetRejections(mods))
}

/** 规划器注入的外部能力（生产实现在 [ModDoctor]，测试注入替身） */
internal class FamilyRepairPlanner(
    private val minecraftVersion: String,
    private val loader: ModLoader,
    private val modsDir: File,
    private val isPackOwned: (String) -> Boolean = { false },
    /** 文件 -> 权威平台身份（指纹匹配）；匹配不到时返回 null，该文件视为不可替换 */
    private val installedIdentity: suspend (String) -> InstalledMod?,
    private val projectVersions: suspend (String, Platform) -> List<PlatformVersion>,
    /** 依赖 id / slug -> 平台项目；平台明确回答没有时返回 null */
    private val dependencyProject: suspend (String) -> PlatformProject?,
    private val dependencyPlatform: Platform = Platform.MODRINTH,
    private val stageCandidate: suspend (PlatformVersion) -> File?,
    private val bounds: FamilyRepairBounds = FamilyRepairBounds(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private class BoundReached : Exception()
    private class StageFailure(reason: String) : Exception(reason)

    private val stagedFiles = mutableListOf<File>()
    private val stagedByCandidate = HashMap<String, StagedCandidate>()
    private val versionLists = HashMap<String, List<PlatformVersion>>()
    private val identities = HashMap<String, InstalledMod?>()
    private var downloads = 0
    private var assignments = 0
    private var startedAt = 0L

    private class StagedCandidate(val file: File, val facts: FabricModFacts, val sha1: String)

    suspend fun plan(): FamilyOutcome {
        startedAt = nowMillis()
        val outcome = try {
            solve()
        } catch (e: CancellationException) {
            cleanup(keep = emptySet())
            throw e
        } catch (e: BoundReached) {
            FamilyOutcome.Refused(
                "the search for a compatible set reached its own limit before finding one; nothing was changed."
            )
        } catch (e: StageFailure) {
            FamilyOutcome.Failed(e.message ?: "The candidate download failed.")
        } catch (e: Throwable) {
            Logger.warning(TAG, "The family repair planning failed", e)
            FamilyOutcome.Failed("The repair plan could not be computed.")
        }
        val keep = (outcome as? FamilyOutcome.Plan)?.plan?.changes?.map { it.stagedFile }?.toSet().orEmpty()
        cleanup(keep)
        return outcome
    }

    /** 计划不再需要时（Preview 结束 / Fix 完成）删除它持有的暂存文件 */
    fun release(plan: FamilyRepairPlan) {
        plan.changes.forEach { runCatching { it.stagedFile.delete() } }
    }

    private fun cleanup(keep: Set<File>) {
        stagedFiles.filter { it !in keep }.forEach { runCatching { it.delete() } }
        stagedFiles.clear()
        stagedFiles.addAll(keep)
    }

    private fun expired(): Boolean = nowMillis() - startedAt > bounds.totalTimeoutMillis

    // ---------------------------------------------------------------------------------------------------------

    private suspend fun solve(): FamilyOutcome {
        val enabled = readEnabledModIdentities(modsDir).sortedBy { it.fileName }
        val before = analyzeFamilySet(enabled)
        if (before.isConsistent) return FamilyOutcome.NotApplicable

        //被整合包管理的文件永远不进入计划；它们参与冲突时只能拒绝
        val tangledIdentities = before.tangled.flatMap { id -> enabled.filter { it.id == id } }.distinctBy { it.fileName }
        tangledIdentities.firstOrNull { isPackOwned(it.fileName) }?.let { owned ->
            return FamilyOutcome.Refused("${owned.fileName} is managed by the modpack and cannot be changed automatically.")
        }

        //缺失的依赖要先有项目：没有项目就不可能装上，直接说明
        val installProjects = LinkedHashMap<String, PlatformProject>()
        for (missingId in before.missing.keys) {
            val project = try {
                dependencyProject(missingId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                throw StageFailure("the provider lookup for the missing dependency $missingId failed.")
            } ?: return FamilyOutcome.Refused("the missing dependency $missingId was not found on the provider.")
            if (project.platformSlug() != missingId) {
                return FamilyOutcome.Refused("the provider project ${project.platformId()} does not match the missing dependency $missingId.")
            }
            installProjects[missingId] = project
        }

        //可以替换的模组：卷入冲突、不属于整合包、并且指纹能匹配到权威项目
        val changeable = LinkedHashMap<String, Changeable>()
        for (identity in tangledIdentities) {
            val installed = identityOf(identity.fileName) ?: continue
            changeable[identity.fileName] = Changeable(identity, installed)
        }
        val limit = minOf(bounds.maxReplacements, changeable.size)

        var lastReason: String? = null
        for (allowDowngrade in listOf(false, true)) {
            for (k in 0..limit) {
                if (k == 0 && before.missing.isEmpty()) continue
                for (subset in combinations(changeable.values.toList(), k)) {
                    val result = searchSubset(enabled, before, subset, installProjects, allowDowngrade)
                    if (result.plan != null) return FamilyOutcome.Plan(result.plan)
                    result.reason?.let { lastReason = it }
                }
            }
        }

        val unchangeable = tangledIdentities.filter { it.fileName !in changeable }
        val why = buildString {
            append("no combination of compatible versions makes every installed mod work together")
            if (unchangeable.isNotEmpty()) {
                append(" (the provider could not identify ${unchangeable.joinToString { it.fileName }})")
            }
            lastReason?.let { append("; last check: ").append(it) }
            append(". Nothing was changed.")
        }
        return FamilyOutcome.Refused(why)
    }

    private class Changeable(val identity: InstalledModIdentity, val installed: InstalledMod)

    private class SubsetResult(val plan: FamilyRepairPlan? = null, val reason: String? = null)

    private suspend fun searchSubset(
        enabled: List<InstalledModIdentity>,
        before: FamilySetAnalysis,
        subset: List<Changeable>,
        installProjects: Map<String, PlatformProject>,
        allowDowngrade: Boolean,
    ): SubsetResult {
        val replacedFiles = subset.map { it.identity.fileName }.toSet()
        val fixed = enabled.filter { it.fileName !in replacedFiles }
        val optionLists = mutableListOf<List<PlatformVersion>>()
        for (member in subset) {
            val options = optionsFor(member, fixed, allowDowngrade)
            if (options.isEmpty()) return SubsetResult()
            optionLists += options
        }

        var reason: String? = null
        val picks = arrayOfNulls<PlatformVersion>(subset.size)
        suspend fun walk(index: Int): FamilyRepairPlan? {
            if (index == subset.size) {
                if (assignments >= bounds.maxAssignments || expired()) throw BoundReached()
                assignments++
                val attempt = evaluate(enabled, before, subset, picks.map { it!! }, installProjects)
                if (attempt.reason != null) reason = attempt.reason
                return attempt.plan
            }
            for (option in optionLists[index]) {
                picks[index] = option
                walk(index + 1)?.let { return it }
            }
            return null
        }
        return SubsetResult(walk(0), reason)
    }

    /**
     * 一个模组的候选：同一权威项目、支持当前 Minecraft + 加载器、不是当前这个文件；
     * 先用固定部分已声明的范围做不下载的预筛（明确会被拒绝的不下载）；
     * 先升级（正式版优先、最新在前），[allowDowngrade] 时再加上其余构建。
     */
    private suspend fun optionsFor(
        member: Changeable,
        fixed: List<InstalledModIdentity>,
        allowDowngrade: Boolean,
    ): List<PlatformVersion> {
        val projectId = member.installed.projectId
        val all = versionsOf(projectId, member.installed.platform)
            .filter { it.platformProjectId() == projectId && it.isCompatibleWithVersion(minecraftVersion, loader) }
            .filter { it.platformFileName() != member.identity.fileName }
            .filter { plausibleAgainst(it, member.identity.id, fixed) }
        val (upgrades, others) = all.partition { isNewerBuildThanInstalled(it.platformVersion(), member.identity.version, minecraftVersion) }
        val ordered = stableFirstCandidates(upgrades) { it.platformReleaseType() != PlatformReleaseType.RELEASE } +
            if (allowDowngrade) stableFirstCandidates(others) { it.platformReleaseType() != PlatformReleaseType.RELEASE } else emptyList()
        return ordered.take(bounds.candidatesPerMod)
    }

    /**
     * 不下载的预筛：固定部分里的模组如果明确声明了会命中这个候选的 breaks，
     * 或者明确声明了这个候选不满足的 depends，这个候选一定会被拒绝。读不懂的范围一律保留。
     */
    private fun plausibleAgainst(candidate: PlatformVersion, modId: String, fixed: List<InstalledModIdentity>): Boolean {
        val names = comparableVersionNames(candidate)
        if (names.isEmpty()) return true
        for (other in fixed) {
            other.breaks[modId]?.takeIf { it.isNotEmpty() }?.let { predicates ->
                if (names.any { versionPredicateDecision(predicates, it) == true }) return false
            }
            other.requires[modId]?.takeIf { it.isNotEmpty() }?.let { predicates ->
                if (names.none { versionPredicateDecision(predicates, it) != false }) return false
            }
        }
        return true
    }

    private class Attempt(val plan: FamilyRepairPlan? = null, val reason: String? = null)

    /** 评估一个具体组合：暂存、核对身份、补齐缺失依赖、整组校验 */
    private suspend fun evaluate(
        enabled: List<InstalledModIdentity>,
        before: FamilySetAnalysis,
        subset: List<Changeable>,
        picks: List<PlatformVersion>,
        installProjects: Map<String, PlatformProject>,
    ): Attempt {
        val replaced = mutableListOf<Pair<Changeable, StagedCandidate>>()
        for ((index, member) in subset.withIndex()) {
            val staged = stage(picks[index], picks[index].platformFileName()) ?: return Attempt(
                reason = "${picks[index].platformFileName()}: metadata could not be read"
            )
            //替换必须提供被替换文件的 id
            if (staged.facts.id != member.identity.id) return Attempt(
                reason = "${picks[index].platformFileName()} does not provide ${member.identity.id}"
            )
            replaced += member to staged
        }
        val replacedFiles = subset.map { it.identity.fileName }.toSet()
        var current = enabled.filter { it.fileName !in replacedFiles } +
            replaced.mapNotNull { (_, staged) -> staged.facts.asInstalledIdentity() }

        //替换之后的整组里仍然缺的依赖：每个缺失 id 选一个候选装上
        val installs = mutableListOf<Triple<String, PlatformVersion, StagedCandidate>>()
        var analysis = analyzeFamilySet(current)
        var guard = 0
        while (analysis.missing.isNotEmpty() && guard++ < MAX_INSTALL_ROUNDS) {
            val missingId = analysis.missing.keys.first()
            val project = installProjects[missingId] ?: dependencyProjectOrNull(missingId) ?: return Attempt(
                reason = "$missingId is required but no provider project is known"
            )
            val chosen = chooseInstall(missingId, project, current) ?: return Attempt(
                reason = "no $missingId build fits the resulting set"
            )
            installs += chosen
            current = current + chosen.third.facts.asInstalledIdentity()!!
            analysis = analyzeFamilySet(current)
        }

        //修复前整组里的每一个问题都必须消失，并且不能出现新的问题
        if (!analysis.isConsistent) {
            return Attempt(reason = analysis.reasons.first())
        }

        val changes = buildList {
            replaced.forEach { (member, staged) ->
                add(
                    FamilyChange(
                        kind = kindOf(member.identity.version, staged.facts.version),
                        modId = member.identity.id,
                        fileName = member.identity.fileName,
                        fromVersion = member.identity.version,
                        toVersion = staged.facts.version,
                        reasons = reasonsFor(member.identity.id, before),
                        target = picks[replaced.indexOfFirst { it.first === member }],
                        stagedFile = staged.file,
                        facts = staged.facts,
                        sha1 = staged.sha1,
                        replacedSha1 = runCatching { calculateFileSha1(File(modsDir, member.identity.fileName)) }.getOrNull(),
                        prerelease = picks[replaced.indexOfFirst { it.first === member }].platformReleaseType() != PlatformReleaseType.RELEASE,
                    )
                )
            }
            installs.forEach { (id, version, staged) ->
                add(
                    FamilyChange(
                        kind = FamilyChangeKind.INSTALL,
                        modId = id,
                        fileName = "",
                        fromVersion = null,
                        toVersion = staged.facts.version,
                        reasons = before.missing[id].orEmpty().map { requirer -> "$requirer requires $id, which is not installed" },
                        target = version,
                        stagedFile = staged.file,
                        facts = staged.facts,
                        sha1 = staged.sha1,
                        prerelease = version.platformReleaseType() != PlatformReleaseType.RELEASE,
                    )
                )
            }
        }
        if (changes.isEmpty()) return Attempt(reason = "the plan would not change any file")
        //同一次计划里两个候选落到同一个文件名：整组效果无法判定
        if (changes.map { it.target.platformFileName() }.toSet().size != changes.size) {
            return Attempt(reason = "two candidates have the same file name")
        }
        return Attempt(plan = FamilyRepairPlan(changes = changes, problems = before.reasons))
    }

    /** 缺失依赖的候选：支持当前环境、预筛通过的按“正式版优先、最新在前”逐个暂存，第一个与整组相容的就是它 */
    private suspend fun chooseInstall(
        missingId: String,
        project: PlatformProject,
        current: List<InstalledModIdentity>,
    ): Triple<String, PlatformVersion, StagedCandidate>? {
        val candidates = versionsOf(project.platformId(), dependencyPlatform)
            .filter { it.platformProjectId() == project.platformId() && it.isCompatibleWithVersion(minecraftVersion, loader) }
            .filter { plausibleAgainst(it, missingId, current) }
            .let { compatible -> stableFirstCandidates(compatible) { it.platformReleaseType() != PlatformReleaseType.RELEASE } }
            .take(bounds.candidatesPerInstall)
        for (candidate in candidates) {
            val staged = stage(candidate, candidate.platformFileName()) ?: continue
            val facts = staged.facts
            //提供者必须真的提供缺失的 id（自己的 id，或者内嵌模块 / 别名）
            if (facts.id != missingId && missingId !in facts.providedModIds) continue
            if (current.any { it.id == facts.id }) continue
            if (candidateCompatibility(facts, current).reason != null) continue
            return Triple(missingId, candidate, staged)
        }
        return null
    }

    private suspend fun dependencyProjectOrNull(id: String): PlatformProject? =
        runCatching { dependencyProject(id) }.getOrNull()?.takeIf { it.platformSlug() == id }

    private fun reasonsFor(modId: String, before: FamilySetAnalysis): List<String> =
        before.reasons.filter { reason -> reason.startsWith("$modId ") || reason.contains(" $modId ") }.take(3)

    private fun kindOf(from: String?, to: String?): FamilyChangeKind {
        if (from.isNullOrBlank() || to.isNullOrBlank()) return FamilyChangeKind.SWITCH
        if (fabricVersionMatches(to, ">$from")) return FamilyChangeKind.UPGRADE
        if (fabricVersionMatches(to, "<$from")) return FamilyChangeKind.DOWNGRADE
        return FamilyChangeKind.SWITCH
    }

    // ---------------------------------------------------------------------------------------------------------

    private suspend fun identityOf(fileName: String): InstalledMod? {
        if (identities.containsKey(fileName)) return identities[fileName]
        if (identities.size >= MAX_IDENTITY_LOOKUPS) return null
        val resolved = try {
            installedIdentity(fileName)?.takeIf { !it.notFound && it.projectId.isNotBlank() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            null
        }
        identities[fileName] = resolved
        return resolved
    }

    private suspend fun versionsOf(projectId: String, platform: Platform): List<PlatformVersion> {
        versionLists[projectId]?.let { return it }
        if (versionLists.size >= MAX_VERSION_LOOKUPS) throw BoundReached()
        val list = try {
            projectVersions(projectId, platform)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw StageFailure("the version lookup for $projectId failed.")
        }
        versionLists[projectId] = list
        return list
    }

    /** 暂存一个候选并读出它自己的 Fabric 事实；同一个候选只下载一次 */
    private suspend fun stage(candidate: PlatformVersion, displayFileName: String): StagedCandidate? {
        stagedByCandidate[candidate.platformId()]?.let { return it }
        if (downloads >= bounds.stagedDownloads || expired()) throw BoundReached()
        downloads++
        val started = nowMillis()
        val file = withTimeoutOrNull(bounds.candidateTimeoutMillis) { stageCandidate(candidate) }
            ?: throw StageFailure(
                if (nowMillis() - started >= bounds.candidateTimeoutMillis) "The candidate download timed out."
                else "The candidate download failed."
            )
        stagedFiles += file
        //暂存文件名是缓存键，原因里要用真正的模组文件名
        val facts = readFabricModFacts(file).copy(fileName = displayFileName)
        val sha1 = runCatching { calculateFileSha1(file) }.getOrNull()
        //平台公布的 SHA-1 与实际下载不一致 = 这个文件不可信
        val published = candidate.platformSha1()?.takeIf { it.isNotBlank() }
        if (facts.asInstalledIdentity() == null || sha1 == null || (published != null && !published.equals(sha1, ignoreCase = true))) {
            runCatching { file.delete() }
            stagedFiles.remove(file)
            return null
        }
        return StagedCandidate(file, facts, sha1).also { stagedByCandidate[candidate.platformId()] = it }
    }

    private companion object {
        const val TAG = "FamilyRepair"
        const val MAX_INSTALL_ROUNDS = 4
        const val MAX_IDENTITY_LOOKUPS = 8
        const val MAX_VERSION_LOOKUPS = 8
    }
}

/** 稳定顺序的组合：输入顺序里取 [k] 个（k=0 给出一个空组合） */
internal fun <T> combinations(items: List<T>, k: Int): List<List<T>> {
    if (k == 0) return listOf(emptyList())
    if (k > items.size) return emptyList()
    val result = mutableListOf<List<T>>()
    fun pick(start: Int, current: MutableList<T>) {
        if (current.size == k) {
            result += current.toList()
            return
        }
        for (i in start until items.size) {
            current += items[i]
            pick(i + 1, current)
            current.removeAt(current.lastIndex)
        }
    }
    pick(0, mutableListOf())
    return result
}

/**
 * ไฟล์ที่ดาวน์โหลดและตรวจ Hash แล้วในช่วง Preview: เก็บไว้ใช้ตอน Fix แทนการดาวน์โหลดรอบสอง
 *
 * ปลอดภัยเพราะก่อน Apply ต้องผ่าน [validateFamilyPlan] ทุกครั้ง (ไฟล์ที่พักยังมี Hash/metadata เดิม,
 * ไฟล์ม็อดเดิมที่จะถูกแทนที่ยังเหมือนเดิม, ปัญหาของทั้งชุดยังเหมือนตอนวางแผน, ผลลัพธ์ทั้งชุดยังสอดคล้อง)
 * ไม่ผ่านข้อใดก็ทิ้ง cache แล้วกลับไปขอ Preview/Confirm ใหม่
 */
internal object FamilyPlanCache {
    private class Entry(val plan: FamilyRepairPlan, val createdAt: Long)

    private val entries = java.util.concurrent.ConcurrentHashMap<String, Entry>()

    /** แผนที่รอการยืนยันเกินเวลานี้ถือว่าเก่าเกินไป (ไฟล์ม็อดอาจเปลี่ยน) */
    const val MAX_AGE_MILLIS = 15 * 60 * 1000L

    fun put(instance: String, plan: FamilyRepairPlan, now: Long = System.currentTimeMillis()) {
        entries.put(instance, Entry(plan, now))?.let { delete(it.plan) }
    }

    /** เอาแผนออกจาก cache (ผู้เรียกรับผิดชอบลบไฟล์ที่พักเมื่อใช้เสร็จ) */
    fun take(instance: String, key: String, now: Long = System.currentTimeMillis()): FamilyRepairPlan? {
        val entry = entries[instance] ?: return null
        if (entry.plan.key != key || now - entry.createdAt > MAX_AGE_MILLIS) {
            entries.remove(instance, entry)
            delete(entry.plan)
            return null
        }
        entries.remove(instance, entry)
        return entry.plan
    }

    fun discard(instance: String) {
        entries.remove(instance)?.let { delete(it.plan) }
    }

    fun delete(plan: FamilyRepairPlan) {
        plan.changes.forEach { runCatching { it.stagedFile.delete() } }
    }
}

/**
 * ตรวจแผนที่เก็บไว้ก่อน Apply: คืนเหตุผลที่ใช้ไม่ได้ (ให้กลับไปขอ Preview/Confirm ใหม่) หรือ null เมื่อยังใช้ได้
 * ฟังก์ชันบริสุทธิ์ด้านการตัดสิน: อ่านแต่ไฟล์ ไม่แก้อะไร
 */
internal suspend fun validateFamilyPlan(
    plan: FamilyRepairPlan,
    modsDir: File,
    enabledNow: List<InstalledModIdentity>,
): String? {
    for (change in plan.changes) {
        stagedFileMismatch(change.stagedFile, change.sha1, change.facts)?.let { return "a downloaded file changed: $it" }
        if (change.fileName.isNotBlank()) {
            val original = File(modsDir, change.fileName)
            if (!original.isFile) return "${change.fileName} is no longer in the mods folder"
            val now = runCatching { calculateFileSha1(original) }.getOrNull()
            if (change.replacedSha1 == null || now == null || !now.equals(change.replacedSha1, ignoreCase = true)) {
                return "${change.fileName} changed after the plan was shown"
            }
        } else if (enabledNow.any { it.id == change.facts.id }) {
            return "${change.modId} was installed after the plan was shown"
        }
    }
    val now = analyzeFamilySet(enabledNow)
    if (now.reasons != plan.problems) return "the problems in the mod set changed after the plan was shown"
    val replaced = plan.changes.map { it.fileName }.filter { it.isNotBlank() }.toSet()
    val after = enabledNow.filter { it.fileName !in replaced } + plan.changes.mapNotNull { it.facts.asInstalledIdentity() }
    val result = analyzeFamilySet(after)
    if (!result.isConsistent) return "the resulting set is no longer consistent: ${result.reasons.first()}"
    return null
}

/** รายการหนึ่งใน Preview (ข้อมูลล้วน ๆ ให้ UI ของ ALL แสดง) */
data class FamilyPreviewItem(
    val kind: FamilyChangeKind,
    val modId: String,
    val fromVersion: String?,
    val toVersion: String?,
    /** ไฟล์เดิมที่จะถูกปิดใช้งาน (ว่างเมื่อติดตั้งใหม่) */
    val fromFile: String,
    val toFile: String,
    val reasons: List<String>,
    val prerelease: Boolean,
)

internal fun FamilyChange.toPreviewItem() = FamilyPreviewItem(
    kind = kind, modId = modId, fromVersion = fromVersion, toVersion = toVersion,
    fromFile = fileName, toFile = target.platformFileName(), reasons = reasons, prerelease = prerelease,
)

/** Preview ที่ผู้ใช้เห็นและยืนยัน: [report] คือสิ่งที่ส่งให้ Fix (มี key ของแผนนี้) */
internal data class FamilyRepairPreview(
    val items: List<FamilyPreviewItem>,
    val key: String,
    val report: ModDoctorReport,
) {
    val hasPrerelease: Boolean get() = items.any { it.prerelease }
}

internal sealed interface FamilyPreviewResult {
    data class Plan(val preview: FamilyRepairPreview) : FamilyPreviewResult
    /** ไม่มีชุดเวอร์ชันที่เข้ากันได้อย่างปลอดภัย */
    data class NoFix(val reason: String) : FamilyPreviewResult
    data class Failed(val reason: String) : FamilyPreviewResult
    /** ไม่ใช่เคสของ planner ทั้งชุด (กฎ known-good / ปัญหาอื่น): ใช้เส้นทางเดิม */
    data object NotApplicable : FamilyPreviewResult
}

// ------------------------------------------------------------------------------------------------------------
// Legacy (non-family) fixes: preview only what is known EXACTLY before Apply; refuse the rest.
// ------------------------------------------------------------------------------------------------------------

/** กุญแจของ Preview ที่มาจากเส้นทางเดิม (กฎ known-good / ปิดไฟล์ในเครื่อง / กู้ไฟล์แพ็กตรงตัว) */
internal const val LEGACY_PLAN_KEY = "legacy"

internal sealed interface LegacyPreviewResult {
    data class Plan(val preview: FamilyRepairPreview, val refused: List<String>) : LegacyPreviewResult
    /** ไม่มีการซ่อมไหนที่ระบุรายการไฟล์ล่วงหน้าได้: ไม่ซ่อมอัตโนมัติ */
    data class NoFix(val reason: String) : LegacyPreviewResult
}

/**
 * Preview สำหรับการซ่อมเส้นทางเดิม: แสดงเฉพาะที่ระบุไฟล์ได้แน่ ๆ ล่วงหน้า
 * - กฎ known-good ([ModDoctorProblem.setFix] / [ModDoctorProblem.ruleFallbackFix]): ไฟล์ -> เวอร์ชันเป้าหมายตามกฎ
 * - ปิดไฟล์ที่เรียนรู้ในเครื่อง ([ModDoctorProblem.localDisableFix])
 * - กู้ไฟล์แพ็กตรงตัว url+sha1 ([ModDoctorProblem.exactRestoreFile])
 * ส่วนที่เหลือ (แทนที่ตามโปรเจกต์ / breaks / ไลบรารี / ติดตั้งดีเพนเดนซี) เลือกไฟล์ตอน staging จึงไม่มีรายการจริงให้โชว์:
 * ไม่สร้าง Preview หลอก — ถูกตัดออกจากรายงานที่ส่งให้ Fix และรายงานเป็นข้อจำกัด
 * รายงานที่ส่งให้ Fix มีเฉพาะการกระทำที่แสดงไว้ใน Preview (ฟิลด์ซ่อมอื่นถูกล้าง) จึงทำเกิน Preview ไม่ได้
 */
internal fun buildLegacyRepairPreview(report: ModDoctorReport): LegacyPreviewResult {
    val items = LinkedHashMap<String, FamilyPreviewItem>()
    val kept = mutableListOf<ModDoctorProblem>()
    val refused = mutableListOf<String>()
    for (problem in report.problems) {
        val stripped = problem.copy(fixCandidate = null, replacementFix = null, dependencyVersionFix = null, breaksFix = null)
        var supported = false
        listOfNotNull(problem.setFix, problem.ruleFallbackFix).forEach { rule ->
            rule.members.forEach { member ->
                supported = true
                items.putIfAbsent(
                    "rule:${member.fileName}",
                    FamilyPreviewItem(
                        kind = FamilyChangeKind.SWITCH, modId = member.modId, fromVersion = null, toVersion = member.targetVersion,
                        fromFile = member.fileName, toFile = "",
                        reasons = listOf(problem.reason).filter { it.isNotBlank() }.take(1), prerelease = false,
                    )
                )
            }
        }
        problem.localDisableFix?.let { fix ->
            supported = true
            items.putIfAbsent(
                "disable:${fix.fileName}",
                FamilyPreviewItem(FamilyChangeKind.DISABLE, problem.modId.orEmpty(), null, null, fix.fileName, "", listOf(problem.reason).filter { it.isNotBlank() }.take(1), false)
            )
        }
        problem.exactRestoreFile?.let { restore ->
            supported = true
            items.putIfAbsent(
                "restore:${restore.name}",
                FamilyPreviewItem(FamilyChangeKind.RESTORE, problem.modId.orEmpty(), null, null, "", restore.name, listOf(problem.reason).filter { it.isNotBlank() }.take(1), false)
            )
        }
        if (supported) {
            //เก็บเฉพาะฟิลด์ที่ Preview ครอบคลุม; ถ้ามีฟิลด์อื่นที่ถูกล้างไป ก็ต้องรายงานว่าส่วนนั้นไม่ได้ซ่อม
            kept += stripped
            if (problem.fixCandidate != null || problem.replacementFix != null || problem.dependencyVersionFix != null || problem.breaksFix != null) {
                refused += problem.fileName
            }
        } else if (problem.fixCandidate != null || problem.replacementFix != null || problem.dependencyVersionFix != null || problem.breaksFix != null) {
            refused += problem.fileName
        }
    }
    if (items.isEmpty()) {
        return LegacyPreviewResult.NoFix(
            "this kind of repair cannot list the files it will change before they are downloaded and checked" +
                (if (refused.isEmpty()) "" else " (" + refused.distinct().joinToString() + ")") +
                ", so no automatic repair was applied."
        )
    }
    return LegacyPreviewResult.Plan(
        FamilyRepairPreview(
            items = items.values.toList(),
            key = LEGACY_PLAN_KEY,
            report = ModDoctorReport(problems = kept, advisories = emptyList()),
        ),
        refused.distinct(),
    )
}

/** การซ่อมเส้นทางเดิมสำเร็จจริงไหม: Fix ไม่ล้มเหลว และปัญหาที่ Preview ครอบคลุมหายไป หรือมีไฟล์ถูกติดตั้ง */
internal fun legacyRepairResolved(before: ModDoctorReport, after: ModDoctorReport, installed: List<String>): Boolean {
    fun key(p: ModDoctorProblem) = listOf(p.type.name, p.modId, p.requiredDependencyId, p.dependencyModId, p.breaksTargetModId, p.fileName).joinToString("|")
    val remaining = after.problems.map(::key).toSet()
    return installed.isNotEmpty() || before.problems.none { key(it) in remaining }
}
