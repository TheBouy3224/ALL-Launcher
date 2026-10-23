package com.movtery.zalithlauncher.game.version.mod

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.utils.file.calculateFileSha1
import com.movtery.zalithlauncher.game.download.assets.downloadSingleForVersions
import com.movtery.zalithlauncher.game.download.assets.isCompatibleWith
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformDependencyType
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformProject
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformReleaseType
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearchFilter
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearchResult
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion
import com.movtery.zalithlauncher.game.download.assets.platform.getProjectByVersion
import com.movtery.zalithlauncher.game.download.assets.platform.getVersions
import com.movtery.zalithlauncher.game.download.assets.platform.searchAssets
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.initAll
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.game.version.mod.reader.ForgeNewModMetadataReader
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.logging.Logger
import io.ktor.client.plugins.ClientRequestException
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.utils.network.downloadFile
import java.io.File
import java.io.FileNotFoundException
import java.io.InterruptedIOException
import java.util.concurrent.TimeoutException
import java.util.zip.ZipFile
import kotlin.coroutines.resume

private const val TAG = "ModDoctor"

private fun File.isActiveModCandidate(): Boolean {
    return isEnabled() && extension.lowercase() in setOf("jar", "zip", "litemod")
}

/** A single report-only problem found in an installed mod set. */
data class ModDoctorProblem(
    val type: ModDoctorProblemType,
    val fileName: String,
    val modId: String?,
    val reason: String,
    /**
     * 界面文案：原因句子的资源Id与参数，为空时界面回退到英文 [reason]
     *
     * [reason] 仍是稳定的英文诊断原文，日志与回归测试读它；
     * 因此本地化只影响呈现，不参与任何判定
     */
    val reasonRes: Int = 0,
    val reasonArgs: List<Any> = emptyList(),
    /** 追加在原因后面的界面句子，按添加顺序排列 */
    val reasonSuffixes: List<ModDoctorReasonSuffix> = emptyList(),
    val requiredDependencyId: String? = null,
    val fixCandidate: ModDoctorFixCandidate? = null,
    val replacementFix: ModDoctorReplacementFix? = null,
    /**
     * 已安装但版本不满足声明的依赖：库文件名与它的 mod id（只来自顶层已装模组，不从内嵌模块猜）。
     * 与 [requiredDependencyId] 分开：那个字段会点亮“缺失依赖安装”流程。
     */
    val dependencyFileName: String? = null,
    val dependencyModId: String? = null,
    /** 把这个库换成满足所有使用者约束的版本；只在解析出权威项目与候选时存在 */
    val dependencyVersionFix: ModDoctorReplacementFix? = null,
    /** 没有安全的库升级方案时的后备：按已发布的已知兼容成套规则，把与规则不一致的成员换成规则里的版本 */
    val ruleFallbackFix: ModDoctorSetFix? = null,
    /** 两个已安装模组冲突时的有序修复计划：目标模组优先，源模组作为回退。 */
    val breaksFix: ModDoctorBreaksFix? = null,
    /** 声明 `breaks` 的源文件；只用于解析修复计划。 */
    val breaksSourceFileName: String? = null,
    /** 被 `breaks` 命中的目标文件；只用于解析修复计划。 */
    val breaksTargetFileName: String? = null,
    /** 被 `breaks` 命中的目标模组 id；崩溃页面用它把日志里的冲突对应到这一条问题。 */
    val breaksTargetModId: String? = null,
    /** 已知兼容成套规则命中时的整组修复计划：只包含与允许集合不一致的成员 */
    val setFix: ModDoctorSetFix? = null,
    /**
     * 整合包里唯一缺失、且精确身份（url + sha1）已知的文件：只能按这份身份原样取回
     * 只有整合包安全模式会填这个字段
     */
    val exactRestoreFile: PackOwnedMod? = null,
    /**
     * 本机学到的「禁用这个文件就能启动」：纯本地动作，不下载也不安装
     *
     * 与 [replacementFix] 分开是有意的：替换会去 provider 查同项目的构建，而这条只需要动本地文件
     */
    val localDisableFix: ModDoctorLocalDisableFix? = null,
)

data class ModDoctorFixCandidate(
    val dependencyId: String,
    val projectId: String,
    val platform: Platform,
    /** เวอร์ชันแรกที่ resolve ได้ว่ารองรับ Minecraft/loader นี้ (ใช้แสดงในแผนเท่านั้น; การติดตั้งจริงยังลองตามลำดับเดิม) */
    val versionName: String? = null,
    /**
     * 已启用模组对这个依赖声明的版本约束：每一组内部按 Fabric 的数组语义判定（任一分支命中即满足），
     * 组与组之间必须同时满足。候选只从满足全部约束的版本里挑；安装时的暂存校验仍然是最终裁决。
     */
    val requirements: List<List<String>> = emptyList(),
)

/**
 * 依赖候选逐个尝试的上限：单个项目对同一游戏版本可能同时发布多个正式版与测试版，
 * 需要足够深才能走到更旧的可用版本，同时又不能把整条版本列表无休止地下载一遍
 */
internal const val MAX_DEPENDENCY_CANDIDATE_ATTEMPTS = 12

/** breaks 修复里“待安装依赖”提供者的界限：项目数与每个项目的暂存次数 */
internal const val MAX_PENDING_DEPENDENCY_PROJECTS = 2
internal const val MAX_PENDING_DEPENDENCY_ATTEMPTS = 2

/** 单个依赖候选的尝试结果 */
internal sealed interface ModDoctorCandidateAttempt<out T> {
    /** 候选通过校验并已安装 */
    data class Accepted<T>(val value: T) : ModDoctorCandidateAttempt<T>

    /** 候选被拒（自带 Fabric 元数据与已启用模组冲突），调用方必须继续尝试次新的候选 */
    data class Rejected(val reason: String) : ModDoctorCandidateAttempt<Nothing>

    /** 下载/安装失败，停止尝试 */
    data class Failed(val reason: String) : ModDoctorCandidateAttempt<Nothing>
}

/** 依赖候选的最终选择：接受这一个，或被拒候选的原因 */
internal data class ModDoctorCandidateChoice<T>(
    val accepted: T?,
    val rejections: List<String>,
    val failure: String? = null,
)

/**
 * 候选排序：正式版在前、预发布版本在后，两组各自保持平台给出的“最新在前”顺序。
 * 只有在没有任何可用的正式版时，才会轮到预发布版本。
 */
internal fun <T> stableFirstCandidates(
    candidates: List<T>,
    isPrerelease: (T) -> Boolean,
): List<T> {
    val (prereleases, stables) = candidates.partition(isPrerelease)
    return stables + prereleases
}

/**
 * 按“最新优先”顺序逐个尝试候选，返回第一个通过校验的候选。
 * 只有 [attempt] 明确接受候选时才返回它：被拒的候选必须由尝试方就地丢弃，不得进入 mods 目录。
 */
internal suspend fun <T, R> firstAcceptedCandidate(
    candidates: List<T>,
    maxAttempts: Int = MAX_DEPENDENCY_CANDIDATE_ATTEMPTS,
    attempt: suspend (T) -> ModDoctorCandidateAttempt<R>,
): ModDoctorCandidateChoice<R> {
    val rejections = mutableListOf<String>()
    for (candidate in candidates.take(maxAttempts)) {
        when (val result = attempt(candidate)) {
            is ModDoctorCandidateAttempt.Accepted -> return ModDoctorCandidateChoice(result.value, rejections)
            is ModDoctorCandidateAttempt.Rejected -> rejections += result.reason
            is ModDoctorCandidateAttempt.Failed -> return ModDoctorCandidateChoice(null, rejections, result.reason)
        }
    }
    return ModDoctorCandidateChoice(null, rejections)
}

/** 候选自带 Fabric 元数据命中已启用模组时的拒绝信号：该候选不得写入 mods 目录 */
internal class ModDoctorCandidateRejected(reason: String) : IllegalStateException(reason)

/**
 * 同项目安全替换目标：需要被可逆禁用的本地文件，以及同一权威项目在当前 Minecraft + 加载器上的替换构建
 *
 * 由“错误加载器”与“Minecraft 版本不兼容”两类问题共用
 * @param fileName 被禁用的本地文件名
 * @param projectId 该文件指纹匹配到的权威平台项目Id（替换只能来自同一项目）
 * @param platform 权威项目所属平台
 */
data class ModDoctorReplacementFix(
    val fileName: String,
    val projectId: String,
    val platform: Platform,
    /** 库版本修复用：所有使用者对这个库的版本约束（每个声明一组）；用来在下载前排除明确不满足的构建 */
    val requirements: List<List<String>> = emptyList(),
)

/**
 * 本机学到的崩溃修复目标：只需要可逆地禁用这个本地文件。
 *
 * 与 [ModDoctorReplacementFix] 分开：这条不查 provider、不下载、不安装任何东西，
 * 因此它必须能在没有网络的情况下单独完成。
 * @param fileName 要禁用的本地文件名
 */
data class ModDoctorLocalDisableFix(
    val fileName: String,
)

/**
 * 已知兼容成套规则里需要改动的一个成员。
 * @param modId 该成员自己的模组 id（替换文件必须提供同一个 id）
 * @param fileName 当前已安装、需要被可逆禁用的文件名
 * @param targetVersion 该成员在允许集合里的版本
 */
data class ModDoctorSetMember(
    val modId: String,
    val fileName: String,
    val targetVersion: String,
)

/**
 * 已知兼容成套规则的修复目标：只列出与允许集合不一致的成员。
 * 已经符合目标版本的成员不在这里，它们的文件不会被碰。
 */
data class ModDoctorSetFix(
    val members: List<ModDoctorSetMember>,
    val allowed: Map<String, String>,
)

/**
 * 已安装 `breaks` 冲突的修复计划。
 * [target] 优先升级被声明为不兼容的目标；[source] 仅在目标没有安全候选时回退。
 */
data class ModDoctorBreaksFix(
    val target: ModDoctorReplacementFix,
    val source: ModDoctorReplacementFix? = null,
)

/**
 * 问题类型
 *
 * [displayName] 是稳定的英文标识，只给日志与诊断看；界面文案走 [labelRes]。
 * 判定结果本来就与语言无关，所以增加语言不会改动任何判定
 * @param labelRes 界面上的标题文案资源Id
 */
enum class ModDoctorProblemType(val displayName: String, val labelRes: Int) {
    MISSING_REQUIRED_DEPENDENCY("Missing required dependency", R.string.mod_doctor_problem_missing_required_dependency),
    PACK_REQUIRED_CORE("Missing or incompatible required modpack core", R.string.mod_doctor_problem_required_pack_core),
    WRONG_LOADER("Wrong mod loader", R.string.mod_doctor_problem_wrong_loader),
    WRONG_MINECRAFT_VERSION("Wrong Minecraft version", R.string.mod_doctor_problem_wrong_minecraft_version),
    DUPLICATE("Duplicate mod", R.string.mod_doctor_problem_duplicate),
    CORRUPT_METADATA("Unreadable/corrupt mod metadata", R.string.mod_doctor_problem_corrupt_metadata),
    INCOMPATIBLE_DEPENDENCY_VERSION("Incompatible dependency version", R.string.mod_doctor_problem_incompatible_dependency_version),
    INCOMPATIBLE_MOD_PAIR("Incompatible mod pair", R.string.mod_doctor_problem_incompatible_mod_pair),
    INCOMPATIBLE_MOD_SET("Incompatible mod versions", R.string.mod_doctor_problem_incompatible_mod_set),
    DECLARED_CONFLICT("Declared conflict (advisory)", R.string.mod_doctor_problem_declared_conflict),
    LEARNED_LOCAL_CRASH("Known local crash (learned)", R.string.mod_doctor_problem_local_crash),
}

/**
 * 追加在原因后面的界面句子，例如“由整合包管理”
 *
 * 按添加顺序累加，与英文 [ModDoctorProblem.reason] 的拼接方式一致，
 * 因此界面不会因为本地化而少显示一条补充说明
 * @param textRes 句子资源Id
 * @param arg 需要插进句子的动态内容（例如平台给出的不可用原因）
 */
data class ModDoctorReasonSuffix(val textRes: Int, val arg: String? = null)

/** 报告里是否有问题带着“自动修复没有完成”的标记：这类问题的修复候选已被清空，重试前必须重新检查 */
internal fun ModDoctorReport.hasIncompleteRepairMark(): Boolean =
    problems.any { problem -> problem.reasonSuffixes.any { it.textRes == R.string.mod_doctor_reason_repair_incomplete } }

/** 可由“同项目安全替换”修复的问题类型：错误加载器 / Minecraft 版本不兼容 */
internal fun ModDoctorProblemType.isReplaceable(): Boolean =
    this == ModDoctorProblemType.WRONG_LOADER || this == ModDoctorProblemType.WRONG_MINECRAFT_VERSION

/**
 * 实例是否接受某个模组声明的加载器
 *
 * Quilt 实现了 Fabric 的模组模型，因此写有 fabric.mod.json 的模组在 Quilt 实例中是合法的；
 * 发布方也常常把同一份构建同时标注为支持 quilt。
 * 反向不成立：Quilt 原生模组（仅带 quilt.mod.json）需要 QuiltLoader，Fabric 实例无法加载。
 */
internal fun ModLoader.acceptsModLoader(modLoader: ModLoader): Boolean =
    modLoader == this || (this == ModLoader.QUILT && modLoader == ModLoader.FABRIC)

data class ModDoctorReport(
    val problems: List<ModDoctorProblem>,
    /**
     * 只报告、不阻塞启动的提示（Fabric 旧键 `conflicts`）。
     *
     * 加载器把声明的冲突当作启动警告并照常加载两个模组，所以它不参与 isClean 与启动门判定。
     */
    val advisories: List<ModDoctorProblem> = emptyList(),
    /** แผนซ่อมกลุ่ม (แสดงอย่างเดียว ไม่ดำเนินการ) — มีค่าเฉพาะเมื่อปัญหาเกี่ยวข้องกับกลุ่มม็อด Sodium */
    val groupPlan: GroupPlan? = null,
    /** กุญแจของแผนซ่อมทั้งชุด (Family) ที่ผู้ใช้เห็นใน Preview; Fix จะคำนวณแผนใหม่และต้องได้แผนเดียวกัน */
    val familyPlanKey: String? = null,
) {
    val isClean: Boolean get() = problems.isEmpty()
    val hasFixableProblems: Boolean get() = problems.any {
        it.fixCandidate != null || it.replacementFix != null || it.breaksFix != null ||
                it.dependencyVersionFix != null || it.ruleFallbackFix != null || it.exactRestoreFile != null || it.localDisableFix != null
    }
}

enum class ModDoctorFixDecision {
    CONTINUE_LAUNCH,
    SHOW_REMAINING_PROBLEMS,
}

data class ModDoctorFixResult(
    val report: ModDoctorReport,
    val installedFileNames: List<String> = emptyList(),
    val failureReason: String? = null,
)

internal fun decideAfterModDoctorFix(report: ModDoctorReport): ModDoctorFixDecision =
    if (report.isClean) ModDoctorFixDecision.CONTINUE_LAUNCH
    else ModDoctorFixDecision.SHOW_REMAINING_PROBLEMS

/**
 * 启动门的结果：真正拉起 Minecraft，还是先把问题交给用户。
 */
enum class LaunchGateDecision {
    LAUNCH,
    BLOCK,
}

/**
 * 启动门：模组报告决定这次启动能不能真正拉起 Minecraft。
 *
 * 只有干净的报告才能启动；只要报告里还有任何一条问题，就必须先让用户看到并处理它。
 * [userOverride] 只允许由「用户明确选择了仍然启动」的那一个动作传入，其它任何启动路径都不许自己越过这道门。
 */
internal fun decideLaunchGate(report: ModDoctorReport, userOverride: Boolean = false): LaunchGateDecision =
    if (report.isClean || userOverride) LaunchGateDecision.LAUNCH
    else LaunchGateDecision.BLOCK

/**
 * 规则/成套修复落盘前的整组预检：用现有的整组校验器 [resultingSetRejections] 检查「假设修复后」的结果集合——
 * 仍然启用的模组原样保留，被替换的文件换成暂存 jar 自己声明的事实。
 *
 * 只拒绝「新增」的问题（修复前就存在、与本次修复无关的问题不算），以及仍然牵涉被替换模组的问题
 * （方案声称要解决的冲突/依赖没解决）。返回拒绝原因；通过时返回 null。纯函数：不改任何文件。
 */
internal fun planResultRejection(enabled: List<InstalledModIdentity>, replacedFileNames: List<String>, stagedFacts: List<FabricModFacts>): String? {
    //暂存文件读不出身份 = 无法验证整组 = 不能算通过（否则它会从假设结果里悄悄消失）
    if (stagedFacts.any { it.asInstalledIdentity() == null }) return "a staged file's metadata could not be read, so the resulting set cannot be verified"
    val replaced = replacedFileNames.toSet()
    val after = enabled.filter { it.fileName !in replaced } + stagedFacts.mapNotNull { it.asInstalledIdentity() }
    val before = resultingSetRejections(enabled).toSet()
    val replacedIds = enabled.filter { it.fileName in replaced }.map { it.id }.toSet() + stagedFacts.mapNotNull { it.id }
    val afterReasons = resultingSetRejections(after)
    afterReasons.firstOrNull { it !in before }?.let { return "the plan would create a new problem: $it" }
    afterReasons.firstOrNull { reason -> replacedIds.any { id -> reason.startsWith("$id ") || reason.contains(" $id ") } }
        ?.let { return "the plan leaves a problem with a replaced mod: $it" }
    return null
}

/**
 * 修复后复检 + 失败时的安全回滚（唯一可注入的是 [rescan]，回滚始终走真实的 [ModRepairSnapshots.rollback]）。
 *
 * - 复检通过：原样返回修复结果，不回滚。
 * - 复检发现新问题 / 目标问题还在 / 复检本身抛出异常（无法验证 = 不能声称成功）：按快照回滚，并报告失败；
 *   只有回滚核对通过（[RollbackResult.complete]）才说「已回滚、原文件已还原」，否则报告回滚失败，绝不假装文件已回来。
 * - [CancellationException] 照常向上抛，不改动状态。
 */
internal suspend fun verifyRepairedSet(
    before: ModDoctorReport,
    result: ModDoctorFixResult,
    finalized: RepairSnapshot,
    versionPath: File,
    modsDir: File,
    rescan: suspend () -> ModDoctorReport,
): ModDoctorFixResult {
    val regression = try {
        postFixRegression(before, rescan())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        "the rescan after the repair could not run: ${e.message ?: e.javaClass.simpleName}"
    } ?: return result
    Logger.warning(TAG, "Post-fix rescan failed, rolling back: $regression")
    val rolledBack = withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) {
        runCatching { ModRepairSnapshots.rollback(versionPath, finalized, modsDir) }.getOrNull()
    }
    return ModDoctorFixResult(
        report = before,
        installedFileNames = emptyList(),
        failureReason = if (rolledBack?.complete == true) REPAIR_POSTCHECK_ROLLED_BACK else REPAIR_ROLLBACK_FAILED,
    )
}

/** 阶段 1 暂存并校验过的一个文件：[replacedFileName] 为空表示纯新增 */
private data class StagedEntry(
    val replacedFileName: String,
    val platformVersion: PlatformVersion,
    val file: File,
    /** 校验时的 SHA-1：阶段 2 复核用 */
    val sha1: String,
    val facts: FabricModFacts,
    /** 用于报告的名字（依赖 id / 被替换的文件名） */
    val label: String,
)

/**
 * 阶段 2 之前的复核：暂存文件还在、SHA-1 与元数据（id / 版本 / requires / breaks）都和阶段 1 校验时一致。
 * 返回不一致的原因；一致返回 null。读不出来也算不一致（证明不了就不安装）。纯函数。
 */
internal suspend fun stagedFileMismatch(file: File, expectedSha1: String, expected: FabricModFacts): String? {
    if (!file.isFile) return "the staged file is gone"
    val actual = runCatching { calculateFileSha1(file) }.getOrNull() ?: return "the staged file could not be hashed"
    if (!actual.equals(expectedSha1, ignoreCase = true)) return "its SHA-1 changed ($expectedSha1 -> $actual)"
    val facts = runCatching { readFabricModFacts(file) }.getOrNull() ?: return "its metadata could not be read"
    if (facts.id != expected.id || facts.version != expected.version || facts.requires != expected.requires || facts.breaks != expected.breaks) {
        return "its metadata changed"
    }
    return null
}

/**
 * 修复失败时的自动回滚：快照已经收尾为 PARTIAL（文件被动过但修复没成功）就用既有的真实回滚还原，
 * 并按回滚的真实结果报告：核对通过才说「已回滚」；否则明确写 ROLLED_BACK_PARTIAL，绝不假装文件都回来了。
 * 不是 PARTIAL（没有改动 / 已 APPLIED / 没有快照）原样返回。
 */
internal suspend fun failWithRollbackIfPartial(
    result: ModDoctorFixResult,
    finalized: RepairSnapshot?,
    versionPath: File,
    modsDir: File,
): ModDoctorFixResult {
    val failure = result.failureReason ?: return result
    if (finalized == null || finalized.status != SnapshotStatus.PARTIAL) return result
    val rolledBack = withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) {
        runCatching { ModRepairSnapshots.rollback(versionPath, finalized, modsDir) }.getOrNull()
    }
    val note = when {
        rolledBack == null -> "The automatic rollback could not run (snapshot PARTIAL); use Undo last repair."
        rolledBack.complete -> "The partial changes were rolled back automatically and the original files are back."
        else -> "The automatic rollback could not restore everything (ROLLED_BACK_PARTIAL; check: ${rolledBack.conflicts.joinToString()}); use Undo last repair."
    }
    return result.copy(
        installedFileNames = if (rolledBack?.complete == true) emptyList() else result.installedFileNames,
        failureReason = "$failure $note",
    )
}

/** 修复前后同一个问题的身份：不含文件名（修复会换文件名），含涉及的模组 id */
private fun problemKey(p: ModDoctorProblem): String =
    listOf(p.type.name, p.modId, p.requiredDependencyId, p.dependencyModId, p.breaksTargetModId).joinToString("|")

/**
 * 修复后复检：不许出现修复前没有的问题；规则方案声称要解决的问题必须消失。
 * 返回失败原因（调用方据此回滚）；通过时返回 null。纯函数。
 */
internal fun postFixRegression(before: ModDoctorReport, after: ModDoctorReport): String? {
    val beforeKeys = before.problems.map(::problemKey).toSet()
    after.problems.firstOrNull { problemKey(it) !in beforeKeys }
        ?.let { return "a new problem appeared after the repair: ${it.type.displayName} (${it.fileName})" }
    val afterKeys = after.problems.map(::problemKey).toSet()
    before.problems.firstOrNull { it.ruleFallbackFix != null && problemKey(it) in afterKeys }
        ?.let { return "the problem the repair planned to fix is still reported: ${it.type.displayName} (${it.fileName})" }
    return null
}

/** 规则方案里真正会被替换的文件（原文件会被可逆保留）：`文件名 → 目标版本`，只含方案成员，不含只是被问题点名的文件 */
internal fun ModDoctorSetFix.plannedChanges(): List<String> = members.map { "${it.fileName} → ${it.targetVersion}" }

/** Play First 的启动前安全检查（纯函数，便于测试）：非整合包实例没有任何阻塞项；整合包只检查 required core。 */
internal fun prelaunchSafetyProblems(policy: ModpackPolicy, modsDir: File): ModDoctorReport =
    if (!policy.isPackManaged) ModDoctorReport(emptyList()) else ModDoctorReport(policy.requiredCoreProblems(modsDir))

/**
 * Small metadata shape used by the pure validation pass and its regression test.
 * It deliberately contains no provider identity or repair plan.
 */
/**
 * @param version 模组自身声明的版本；读不到时为null。只有它自己的版本可以带在身上，
 *                内嵌 jar 的 id（[providedModIds]）版本与本文件不同，不能借用
 * @param requiredDependencyPredicates 依赖Id -> 声明的版本谓词（原样保留，可能是 `0.5.9 || 0.5.11`
 *                这种可选分支写法）；谓词不再在解析时被丢弃，否则无法判断已安装依赖的版本是否满足声明
 */
internal data class ModDoctorMetadata(
    val fileName: String,
    val modId: String,
    val name: String,
    val loader: ModLoader,
    val version: String? = null,
    val requiredDependencies: List<String> = emptyList(),
    val requiredDependencyPredicates: Map<String, List<String>> = emptyMap(),
    /** Fabric `breaks` 目标Id -> 版本谓词；这里只携带数据，判定在统一校验阶段完成。 */
    val breakPredicates: Map<String, List<String>> = emptyMap(),
    /** Fabric 旧键 `conflicts` 目标Id -> 版本谓词；只携带数据，判定在统一校验阶段完成。 */
    val conflictPredicates: Map<String, List<String>> = emptyMap(),
    val minecraftVersionRanges: List<String> = emptyList(),
    /**
     * 这个文件额外提供、可供依赖解析的 id（「已装」判定用）：
     * 内嵌 jar 的 id（[providedModVersions] 里带它们自己的版本），以及顶层 `provides` 声明的别名 id。
     */
    val providedModIds: List<String> = emptyList(),
    /**
     * 内嵌 jar 自己声明的 id → 版本（不是外层文件借来的版本）。
     *
     * 真实用例：某个模组内嵌一份更低版本的依赖副本，删掉顶层依赖文件后这个 id 仍然“装了”，
     * 缺失依赖规则会保持沉默；此时能说话的就是这份副本自己的版本。
     */
    val providedModVersions: Map<String, String> = emptyMap(),
    val readable: Boolean = true,
)

/** 整合包安全模式在问题描述里加的一句话 */
internal const val PACK_MANAGED_REASON =
    "This mod is managed by the modpack, so ALL won't change it automatically."

/** Exact required-core restoration explanation retained for the UI/report text. */
internal const val PACK_RESTORE_REASON =
    "The modpack file is missing, so ALL will restore exactly the version the modpack installed."

/**
 * Pack policy: prevent automatic replacement of explicit required-core files.
 *
 * The explicit required core is protected. Normal findings and repairs for optional files and user additions
 * continue through the existing Mod Doctor rules.
 *
 * 非整合包实例原样返回，行为完全不变。
 */
internal fun ModDoctorReport.applyPackSafeMode(
    policy: ModpackPolicy,
    modsDir: File,
): ModDoctorReport {
    if (!policy.isPackManaged) return this

    return copy(
        problems = problems.map { problem ->
            val ownedReplacementTarget =
                problem.replacementFix?.fileName?.let { policy.isPackOwned(it) } == true
            val ownedLocalDisableTarget =
                problem.localDisableFix?.fileName?.let { policy.isPackOwned(it) } == true
            val ownedBreaksTarget = problem.breaksFix?.let { fix ->
                policy.isPackOwned(fix.target.fileName) ||
                        fix.source?.fileName?.let(policy::isPackOwned) == true
            } == true
            when {
                //本机学到的禁用同样会改动文件：整合包文件一律不动
                ownedLocalDisableTarget -> problem.copy(
                    localDisableFix = null,
                    reason = "${problem.reason} $PACK_MANAGED_REASON",
                    reasonSuffixes = problem.reasonSuffixes + ModDoctorReasonSuffix(R.string.mod_doctor_reason_pack_managed),
                )
                //breaks 修复同样会替换文件：整合包安全模式保持原策略，不自动改动包文件
                ownedBreaksTarget -> problem.copy(
                    breaksFix = null,
                    reason = "${problem.reason} $PACK_MANAGED_REASON",
                    reasonSuffixes = problem.reasonSuffixes + ModDoctorReasonSuffix(R.string.mod_doctor_reason_pack_managed),
                )
                //库版本修复同样会换掉文件：整合包文件一律不动
                problem.dependencyVersionFix?.fileName?.let { policy.isPackOwned(it) } == true -> problem.copy(
                    dependencyVersionFix = null,
                    reason = "${problem.reason} $PACK_MANAGED_REASON",
                    reasonSuffixes = problem.reasonSuffixes + ModDoctorReasonSuffix(R.string.mod_doctor_reason_pack_managed),
                )
                //替换会禁用并换掉整合包文件：一律不做
                ownedReplacementTarget -> problem.copy(
                    replacementFix = null,
                    reason = "${problem.reason} $PACK_MANAGED_REASON",
                    reasonSuffixes = problem.reasonSuffixes + ModDoctorReasonSuffix(R.string.mod_doctor_reason_pack_managed),
                )
                else -> problem
            }
        }
    )
}

/**
 * 一条本机学习记录是否够格在启动前警告。
 *
 * 身份必须对得上：实例名（大小写不敏感）、Minecraft 版本、加载器（记录里存的是日志文本里的写法
 * `Fabric`，而 [ModLoader.name] 是 `FABRIC`，所以按大小写不敏感比较），以及记录自己点名的 owner 模组。
 *
 * 而且必须是一次**成功**的修复：动过文件（[LearningRecord.actionPreState] 非空）、之后那次启动
 * 看到了启动标记、没有看到崩溃标记，并且没有被判为不成立。
 *
 * 有意**不**看模组集指纹：动作把 owner 文件禁用之后模组集本身就变了，用当前指纹算出来的键
 * 永远找不到那条记录——这正是「修好之后再也想不起来」的缺陷所在。
 */
internal fun learnedWarningCandidates(
    records: List<LearningRecord>,
    instanceName: String,
    minecraftVersion: String,
    loader: ModLoader,
): List<LearningRecord> = records
    .filter { record ->
        record.signatureId.isNotBlank() &&
                !record.ownerModId.isNullOrBlank() &&
                record.instanceName.equals(instanceName, ignoreCase = true) &&
                record.minecraftVersion == minecraftVersion &&
                record.loader.equals(loader.name, ignoreCase = true) &&
                !record.inconclusive &&
                //缺失依赖由启动前的模组检查直接报告，并可以安装依赖修复；学习记录不再把「禁用它就能启动」当作建议
                record.diagnosisType != DIAGNOSIS_CLASS_MISSING_DEPENDENCY &&
                record.actionPreState.isNotBlank() &&
                record.markerSeenAfterFix &&
                !record.crashMarkerSeenAfterFix
    }
    //同一个 owner 只警告一次：留下最好的那条
    .groupBy { it.ownerModId.orEmpty() }
    .map { (_, owned) -> owned.minWith(learnedRecordOrder) }
    .sortedWith(learnedRecordOrder)
    .take(LEARNED_WARNING_LIMIT)

/** 「最好」的那条记录：已确认优先，其次命中次数多的，最后按签名Id稳定排序 */
private val learnedRecordOrder: Comparator<LearningRecord> = compareBy<LearningRecord>(
    { if (it.state == LearningState.CONFIRMED_LOCAL) 0 else 1 },
    { -it.count },
    { it.signatureId },
)

/**
 * 把够格的学习记录变成启动前的问题：owner 模组必须**在这个实例里存在且启用**。
 *
 * 文件不存在或已被禁用时什么都不产出——不是问题，也不是提示。提示会弹出一个没有取消按钮的
 * 对话框，而修复成功之后 owner 本来就是禁用的，那会让每一次启动都被拦一次。
 */
internal fun learnedLocalCrashProblems(
    records: List<LearningRecord>,
    instanceName: String,
    minecraftVersion: String,
    loader: ModLoader,
    locals: List<LocalMod>,
): List<ModDoctorProblem> = learnedWarningCandidates(records, instanceName, minecraftVersion, loader)
    .mapNotNull { record ->
        val ownerModId = record.ownerModId.orEmpty()
        val owner = locals.firstOrNull { it.id == ownerModId && !it.notMod && it.file.isEnabled() }
            ?: return@mapNotNull null
        val confirmed = record.state == LearningState.CONFIRMED_LOCAL
        val label = owner.name.ifBlank { ownerModId }
        ModDoctorProblem(
            type = ModDoctorProblemType.LEARNED_LOCAL_CRASH,
            fileName = owner.file.name,
            modId = ownerModId,
            reason = if (confirmed) {
                "$label crashed this instance before, and disabling it fixed the launch again. " +
                        "Confirmed locally on this device; it does not claim a general incompatibility."
            } else {
                "$label crashed this instance before, and disabling it fixed the launch. " +
                        "Learned from this device's logs; it does not claim a general incompatibility."
            },
            reasonRes = if (confirmed) {
                R.string.mod_doctor_reason_local_crash_confirmed
            } else {
                R.string.mod_doctor_reason_local_crash
            },
            reasonArgs = listOf(label),
            localDisableFix = ModDoctorLocalDisableFix(owner.file.name),
        )
    }

/**
 * 启动前的本机学习警告：把「这台设备上学到的、这个实例上确实修好过一次的崩溃」变成一条可修复的问题。
 *
 * 先读学习记录（便宜），只有真的有一条够格的记录时才去读模组目录：启动路径上不能为了一个
 * 大概率不存在的警告去解压整个 mods 目录。
 */
internal suspend fun ModDoctorReport.withLearnedLocalCrashWarnings(
    instanceName: String,
    minecraftVersion: String,
    loader: ModLoader,
    modsDir: File,
): ModDoctorReport {
    val records = CrashLearning.learnedRecords()
    if (learnedWarningCandidates(records, instanceName, minecraftVersion, loader).isEmpty()) return this

    val locals = runCatching { AllModReader(modsDir).readAllLocals() }.getOrDefault(emptyList())
    val warnings = learnedLocalCrashProblems(records, instanceName, minecraftVersion, loader, locals)
    if (warnings.isEmpty()) return this

    Logger.info(
        TAG,
        "learned local crash warnings: ${warnings.size} problem(s) " +
                warnings.joinToString(prefix = "[", postfix = "]") { it.fileName }
    )
    return copy(problems = problems + warnings)
}

/**
 * 冲突修复的每个冲突侧允许检查的根候选数
 */
internal const val MAX_REPAIR_ROOT_CANDIDATES = 12

/** 一个计划里最多连带替换多少个“依赖已不再被满足”的已启用模组 */
internal const val MAX_REPAIR_DEPENDENTS = 3

/** 单个连带模组的候选尝试次数 */
internal const val MAX_REPAIR_DEPENDENT_ATTEMPTS = 4

/** 一个计划里的替换总数（冲突侧 + 连带模组） */
internal const val MAX_REPAIR_REPLACEMENTS = 4

/**
 * 一次修复里允许的暂存下载总数
 *
 * 取最坏走查的完整容量：两个冲突侧各自最多 [MAX_REPAIR_ROOT_CANDIDATES] 个根候选，每个根候选的尝试
 * 要暂存 1 次自己，外加最多 [MAX_REPAIR_DEPENDENTS] 个连带模组 × [MAX_REPAIR_DEPENDENT_ATTEMPTS]
 * 次尝试；上限小于这条走查就会把一个本来成立的计划挡在最后一个候选之前。
 */
internal const val MAX_REPAIR_STAGED_DOWNLOADS =
    2 * MAX_REPAIR_ROOT_CANDIDATES * (1 + MAX_REPAIR_DEPENDENTS * MAX_REPAIR_DEPENDENT_ATTEMPTS)

/** 单个候选的暂存等待上限，避免界面长久停留在“正在修复…” */
internal const val REPAIR_CANDIDATE_TIMEOUT_MILLIS = 45_000L

/** 一次修复的总时长上限 */
internal const val REPAIR_TOTAL_TIMEOUT_MILLIS = 10 * 60 * 1000L

/** 版本列表最多查询并缓存多少个项目 */
internal const val MAX_REPAIR_PROJECT_LOOKUPS = 5

/** 权威身份（指纹）最多查询多少个文件 */
internal const val MAX_REPAIR_FINGERPRINTS = 5

/** 库版本修复的界限：只换这一个库，其他模组一个都不能被连带替换（任何使用者不接受候选就整个放弃该候选） */
internal val LIBRARY_REPAIR_BOUNDS = ModDoctorRepairBounds(dependentsPerPlan = 0, replacementsPerPlan = 1)

/**
 * 修复界限
 *
 * 默认值就是生产界限；测试可以收紧它们来验证“越界只做安全拒绝”。
 */
internal data class ModDoctorRepairBounds(
    val rootCandidatesPerPass: Int = MAX_REPAIR_ROOT_CANDIDATES,
    val dependentsPerPlan: Int = MAX_REPAIR_DEPENDENTS,
    val attemptsPerDependent: Int = MAX_REPAIR_DEPENDENT_ATTEMPTS,
    val replacementsPerPlan: Int = MAX_REPAIR_REPLACEMENTS,
    val stagedDownloads: Int = MAX_REPAIR_STAGED_DOWNLOADS,
    val candidateTimeoutMillis: Long = REPAIR_CANDIDATE_TIMEOUT_MILLIS,
    val totalTimeoutMillis: Long = REPAIR_TOTAL_TIMEOUT_MILLIS,
)

/**
 * 计划里的一次替换：被替换（可逆禁用）的本地文件 + 已暂存待复制的替换文件
 * @param fileName 被替换的原始文件名
 * @param target 权威同项目候选
 * @param stagedFile 暂存位上的替换文件；在整组计划通过前不得进入 mods 目录
 * @param facts 暂存文件自带的 Fabric 事实（身份由它校验，不用平台元数据替代）
 */
internal data class ModDoctorPlannedReplacement(
    val fileName: String,
    val target: PlatformVersion,
    val stagedFile: File,
    val facts: FabricModFacts,
)

/** 冲突修复的完整计划：全部文件都已暂存，只有整组校验通过才会执行 */
internal data class ModDoctorRepairPlan(
    val replacements: List<ModDoctorPlannedReplacement>,
)

/**
 * 冲突修复的结果
 *
 * [NoSafePlan] 覆盖“找不到安全计划”与“碰到修复界限”两种情形：两者都只做安全拒绝，不抛异常。
 */
internal sealed interface ModDoctorRepairOutcome {
    /** 计划已执行：替换文件已复制进 mods 目录，原文件保留为可逆的禁用状态 */
    data class Repaired(
        val installedFileNames: List<String>,
        val replacedFileNames: List<String>,
    ) : ModDoctorRepairOutcome

    /** 基础设施失败（下载/查询），mods 目录已恢复到修复前的状态 */
    data class Failed(val reason: String) : ModDoctorRepairOutcome

    /** 没有安全计划：mods 目录一个字节都没有改动 */
    data object NoSafePlan : ModDoctorRepairOutcome
}

/**
 * 冲突的一个可替换侧：要替换的本地文件、它声明的 id（替换必须提供同一个 id），
 * 以及同一冲突的另一侧文件名（本侧永远不展开它）。
 */
private class RepairPass(
    val root: ModDoctorReplacementFix,
    val replacedId: String,
    val otherSideFile: String?,
    /** 当前安装的版本（来自已启用模组的身份）；未知时目标侧不允许替换 */
    val installedVersion: String?,
    /** 只有声明 breaks 的源侧可以因冲突证据换到更旧的构建；目标侧绝不自动降级 */
    val mayDowngrade: Boolean,
)

/** 修复界限命中：只做安全拒绝 */
private class ModDoctorRepairBoundReached : Exception()

/** 修复基础设施失败（下载/查询）：报告失败，不动 mods 目录 */
private class ModDoctorRepairFailure(reason: String) : Exception(reason)

/**
 * 当前已启用模组的 Fabric 身份（Id + 版本 + 依赖声明），用于校验候选自带的 breaks/depends。
 *
 * 只读本地文件，不产生网络请求。读取顺序由 [AllModReader] 的并发回收决定；
 * 需要确定性顺序的调用方自行排序。
 */
internal suspend fun readEnabledModIdentities(modsDir: File): List<InstalledModIdentity> = runCatching {
    AllModReader(modsDir).readAllLocals()
        .filter { it.file.isActiveModCandidate() }
        .mapNotNull { local ->
            val facts = readFabricModFacts(local.file)
            val id = facts.id?.takeIf { it.isNotBlank() } ?: local.id.takeIf { it.isNotBlank() }
            id?.let {
                InstalledModIdentity(
                    id = it,
                    version = facts.version ?: local.version,
                    requires = facts.requires,
                    breaks = facts.breaks,
                    fileName = local.file.name,
                    //这个文件额外提供的 id（内嵌模块自己的 id 与别名、顶层 `provides` 别名）同样算「已装」：
                    //整组校验必须看得见它们，否则 Fabric API 提供的模块会被判成没有提供者。
                    providedModIds = facts.providedModIds,
                    providedModVersions = facts.providedModVersions,
                )
            }
        }
}.getOrDefault(emptyList())

/**
 * 有界多模组修复：以 [ModDoctorBreaksFix]（目标侧优先、源侧回退）为入口。
 *
 * 两次尝试（先目标侧、后源侧）互相独立，任何一次都不会把另一侧当作“可连带替换的依赖方”展开。
 * 每次尝试只沿 `requires` 方向展开一层：把“依赖不再被满足”的已启用模组一并替换（最多三个），
 * 计划总数不超过四个替换。所有候选先经既有 DownloadSingle 管线暂存，整组 depends/breaks
 * 校验通过后才真正改动 mods 目录；任何失败都删除副本、恢复原文件并核对修复前的哈希集合。
 * 任何界限命中都只做安全拒绝（[ModDoctorRepairOutcome.NoSafePlan]），不抛异常。
 */
internal class ModDoctorMultiModRepair(
    private val minecraftVersion: String,
    private val loader: ModLoader,
    private val modsDir: File,
    private val isPackOwned: (String) -> Boolean = { false },
    private val installedIdentity: suspend (String) -> InstalledMod?,
    private val projectVersions: suspend (String, Platform) -> List<PlatformVersion>,
    private val stageCandidate: suspend (PlatformVersion) -> File?,
    private val bounds: ModDoctorRepairBounds = ModDoctorRepairBounds(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
    /**
     * 同一次修复里还缺少、且已由既有依赖解析器解析出提供者的依赖（项目Id, 平台）。
     * 它们不会在这里安装（仍由既有的依赖步骤安装）；只是把暂存并校验过的提供者算进“结果集合”，
     * 这样只因依赖尚未安装而被拒绝的替换候选不会被误判。
     */
    private val pendingDependencies: List<Pair<String, Platform>> = emptyList(),
) {
    suspend fun repair(fix: ModDoctorBreaksFix): ModDoctorRepairOutcome =
        execute { session -> solve(fix, session) }

    /**
     * 已装库版本不对：把这个库换成满足所有使用者的构建（可升级也可降级）。
     * 复用同一个有界解算器：库文件是唯一的根，连带替换其他模组的名额为 0 ——
     * 只要还有已启用模组不接受候选，这个候选就整个放弃；找不到共同版本时 [ModDoctorRepairOutcome.NoSafePlan]，不改任何文件。
     * 候选自带的 depends/breaks 与整组结果仍由同一套校验把关。
     */
    suspend fun repairLibrary(fix: ModDoctorReplacementFix): ModDoctorRepairOutcome =
        execute { session ->
            val enabled = readEnabledModIdentities(modsDir).sortedBy { it.fileName }
            val pass = repairPass(fix, null, enabled, mayDowngrade = true) ?: return@execute null
            val pending = stagePendingDependencies(enabled, session)
            //求值器已经明确判定不满足的构建在下载前就排除（读不懂的仍交给暂存后的真实元数据校验）：
            //不为一定会被拒绝的构建多下载文件
            val plausible = rootCandidates(pass, session).filter { candidate ->
                val names = comparableVersionNames(candidate)
                fix.requirements.all { predicates -> names.any { versionPredicateDecision(predicates, it) != false } }
            }
            for (candidate in plausible) {
                tryRootCandidate(pass, candidate, enabled, pending, session)?.let { plan -> return@execute plan }
            }
            null
        }

    private suspend fun execute(solveBlock: suspend (RepairSession) -> ModDoctorRepairPlan?): ModDoctorRepairOutcome {
        val staged = mutableListOf<File>()
        val outcome = try {
            val plan = solveBlock(RepairSession(staged))
            if (plan == null) ModDoctorRepairOutcome.NoSafePlan else apply(plan)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ModDoctorRepairBoundReached) {
            Logger.info(TAG, "The conflicting-mod repair stopped at its own limit; nothing was changed.")
            ModDoctorRepairOutcome.NoSafePlan
        } catch (e: ModDoctorRepairFailure) {
            ModDoctorRepairOutcome.Failed(e.message ?: "The replacement failed.")
        } catch (e: Throwable) {
            Logger.warning(TAG, "The conflicting-mod repair failed", e)
            ModDoctorRepairOutcome.Failed("The replacement failed.")
        }
        staged.forEach { file -> runCatching { file.delete() } }
        return outcome
    }

    /**
     * 有界搜索：先目标侧、后源侧。每个冲突侧按“正式版在前、最新在前”的顺序走查根候选；
     * 对每个根候选只沿 `requires` 方向展开一层，把依赖不再被满足的已启用模组一并替换，
     * 最后用整组 depends/breaks 校验决定这个组合是否成立。
     *
     * 两侧互相独立：[RepairPass.otherSideFile] 永远不进入展开范围，因此目标侧尝试不会顺手替换源模组，
     * 源侧尝试也不会顺手替换目标模组。
     */
    private suspend fun solve(
        fix: ModDoctorBreaksFix,
        session: RepairSession,
    ): ModDoctorRepairPlan? {
        val enabled = readEnabledModIdentities(modsDir).sortedBy { it.fileName }
        val passes = listOfNotNull(
            //目标侧：不允许降级（只能换到能确认更新的构建）
            repairPass(fix.target, fix.source?.fileName, enabled, mayDowngrade = false),
            //源侧（声明 breaks 的一方）：可以因冲突证据换到更旧的构建，并经过 staging 的完整校验
            fix.source?.let { source -> repairPass(source, fix.target.fileName, enabled, mayDowngrade = true) },
        )
        val pending = stagePendingDependencies(enabled, session)
        for (pass in passes) {
            for (candidate in rootCandidates(pass, session)) {
                tryRootCandidate(pass, candidate, enabled, pending, session)?.let { plan -> return plan }
            }
        }
        return null
    }

    /**
     * 待安装依赖的提供者：最多 [MAX_PENDING_DEPENDENCY_PROJECTS] 个项目、每个最多 [MAX_PENDING_DEPENDENCY_ATTEMPTS] 次暂存，
     * 取第一个事实可读、且自带 breaks 不命中已启用模组的候选（正式版优先）。任何一个读不出来就不计入，保持原有的拒绝行为。
     */
    private suspend fun stagePendingDependencies(
        enabled: List<InstalledModIdentity>,
        session: RepairSession,
    ): List<InstalledModIdentity> {
        val identities = mutableListOf<InstalledModIdentity>()
        for ((projectId, platform) in pendingDependencies.take(MAX_PENDING_DEPENDENCY_PROJECTS)) {
            val candidates = session.versionsOf(projectId, platform)
                .filter { it.platformProjectId() == projectId && it.isCompatibleWithVersion(minecraftVersion, loader) }
                .let { compatible -> stableFirstCandidates(compatible) { it.platformReleaseType() != PlatformReleaseType.RELEASE } }
                .take(MAX_PENDING_DEPENDENCY_ATTEMPTS)
            for (candidate in candidates) {
                val facts = readFabricModFacts(session.stage(candidate))
                val identity = facts.asInstalledIdentity() ?: continue
                if (enabled.any { it.id == identity.id }) break
                if (candidateCompatibility(facts, enabled).reason != null) continue
                identities += identity
                break
            }
        }
        return identities
    }

    /** 这一侧能不能被替换：文件必须已启用、可读出自己的 id，且不属于整合包 */
    private fun repairPass(
        root: ModDoctorReplacementFix,
        otherSideFile: String?,
        enabled: List<InstalledModIdentity>,
        mayDowngrade: Boolean,
    ): RepairPass? {
        if (isPackOwned(root.fileName)) return null
        val identity = enabled.firstOrNull { it.fileName == root.fileName } ?: return null
        val replacedId = identity.id.takeIf { it.isNotBlank() } ?: return null
        return RepairPass(root, replacedId, otherSideFile, identity.version, mayDowngrade)
    }

    /**
     * 同一个权威项目的兼容候选：只保留属于该项目的构建，正式版优先、最新在前。
     * 目标侧还必须能确认比当前安装版本更新（不自动降级）；无法确认版本的候选不进入目标侧。
     */
    private suspend fun rootCandidates(
        pass: RepairPass,
        session: RepairSession,
    ): List<PlatformVersion> = session.versionsOf(pass.root.projectId, pass.root.platform)
        .filter { it.platformProjectId() == pass.root.projectId && it.isCompatibleWithVersion(minecraftVersion, loader) }
        .filter { pass.mayDowngrade || isNewerBuildThanInstalled(it.platformVersion(), pass.installedVersion, minecraftVersion) }
        .let { compatible -> stableFirstCandidates(compatible) { it.platformReleaseType() != PlatformReleaseType.RELEASE } }
        .take(bounds.rootCandidatesPerPass)

    /**
     * 一个根候选构成完整计划：它自己 + 每个“依赖不再被满足”的已启用模组。
     * 任何一步不成立都只放弃这个候选，不会留下任何改动。
     */
    private suspend fun tryRootCandidate(
        pass: RepairPass,
        candidate: PlatformVersion,
        enabled: List<InstalledModIdentity>,
        pending: List<InstalledModIdentity>,
        session: RepairSession,
    ): ModDoctorRepairPlan? {
        val stagedRoot = session.stage(candidate)
        val rootFacts = readFabricModFacts(stagedRoot)
        //替换必须提供被替换文件的 id，且暂存文件的事实必须读得出来
        if (rootFacts.id != pass.replacedId) return null
        val rootIdentity = rootFacts.asInstalledIdentity() ?: return null

        //只有“依赖不再被满足”的已启用模组才会被连带替换；冲突的另一侧永远不在展开范围内
        val expandable = enabled.filter { it.fileName != pass.root.fileName && it.fileName != pass.otherSideFile }
        val compatibility = candidateCompatibility(rootFacts, expandable)
        if (compatibility.reason != null) return null
        if (compatibility.blockingDependents.size > bounds.dependentsPerPlan) {
            Logger.info(TAG, "More than ${bounds.dependentsPerPlan} dependent mod(s) still require the old file; refusing safely.")
            return null
        }

        val plan = mutableListOf(
            ModDoctorPlannedReplacement(pass.root.fileName, candidate, stagedRoot, rootFacts)
        )
        for (dependentId in compatibility.blockingDependents) {
            val dependent = expandable.firstOrNull { it.id == dependentId } ?: return null
            //整合包文件永远不进入计划
            if (isPackOwned(dependent.fileName)) return null
            val replacement = chooseDependentReplacement(dependent, rootIdentity, session) ?: return null
            plan += replacement
        }
        if (plan.size > bounds.replacementsPerPlan) return null
        //两个候选落到同一个文件名时整组效果无法判定，直接放弃
        if (plan.map { it.target.platformFileName() }.toSet().size != plan.size) return null

        //待安装依赖的提供者（已暂存并校验）也属于结果集合：完整的安全校验不降级，只是不再忽略“即将装上”的依赖
        val rejection = resultingSetRejection(resultingSet(plan, enabled) + pending)
        if (rejection != null) {
            Logger.info(TAG, "The resulting mod set is still inconsistent: $rejection")
            return null
        }
        return ModDoctorRepairPlan(plan)
    }

    /** 连带模组的候选走查：权威同项目身份 → 兼容版本（正式版优先、最新在前）→ 暂存事实必须成立 */
    private suspend fun chooseDependentReplacement(
        dependent: InstalledModIdentity,
        rootIdentity: InstalledModIdentity,
        session: RepairSession,
    ): ModDoctorPlannedReplacement? {
        val installed = session.identityOf(dependent.fileName) ?: return null
        if (installed.notFound || installed.projectId.isBlank()) return null
        val platform = installed.platform
        val candidates = session.versionsOf(installed.projectId, platform)
            .filter { it.platformProjectId() == installed.projectId && it.isCompatibleWithVersion(minecraftVersion, loader) }
            .let { compatible -> stableFirstCandidates(compatible) { it.platformReleaseType() != PlatformReleaseType.RELEASE } }
            .take(bounds.attemptsPerDependent)

        for (candidate in candidates) {
            val staged = session.stage(candidate)
            val facts = readFabricModFacts(staged)
            //替换必须提供被替换的 id，且暂存文件的事实必须读得出来
            if (facts.id != dependent.id || facts.asInstalledIdentity() == null) continue
            //替换后仍然必须接受新的冲突侧版本：明确不满足就换下一个候选
            val requirement = facts.requires[rootIdentity.id]
            if (!requirement.isNullOrEmpty() &&
                installedDependencyVerdict(requirement, rootIdentity.version) == false
            ) continue
            return ModDoctorPlannedReplacement(dependent.fileName, candidate, staged, facts)
        }
        return null
    }

    /** 整组结果：仍然启用的模组原样保留，被替换的文件换成暂存候选自己的事实 */
    private fun resultingSet(
        plan: List<ModDoctorPlannedReplacement>,
        enabled: List<InstalledModIdentity>,
    ): List<InstalledModIdentity> {
        val replaced = plan.map { it.fileName }.toSet()
        return enabled.filter { it.fileName !in replaced } + plan.mapNotNull { it.facts.asInstalledIdentity() }
    }

    /**
     * 一次修复里的界限与缓存
     *
     * 版本列表每个项目只取一次（最多 [MAX_REPAIR_PROJECT_LOOKUPS] 个项目），权威身份每个文件只查一次
     * （最多 [MAX_REPAIR_FINGERPRINTS] 个文件），暂存下载与候选等待都受 [bounds] 限制；
     * 任何一项越界都只抛出 [ModDoctorRepairBoundReached]，由 [repair] 折叠成安全拒绝。
     */
    private inner class RepairSession(private val staged: MutableList<File>) {
        private val startedAt = nowMillis()
        private val versionLists = LinkedHashMap<String, List<PlatformVersion>>()
        private val identities = LinkedHashMap<String, InstalledMod?>()
        private var downloads = 0

        private fun expired(): Boolean = nowMillis() - startedAt > bounds.totalTimeoutMillis

        /** 版本列表：每个项目最多一次；查询失败是基础设施失败，不是“没有候选” */
        suspend fun versionsOf(projectId: String, platform: Platform): List<PlatformVersion> {
            val key = "${platform.name}/$projectId"
            versionLists[key]?.let { return it }
            if (versionLists.size >= MAX_REPAIR_PROJECT_LOOKUPS || expired()) throw ModDoctorRepairBoundReached()
            val loaded = try {
                projectVersions(projectId, platform)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                throw ModDoctorRepairFailure("The replacement version lookup failed.")
            }
            versionLists[key] = loaded
            return loaded
        }

        /** 权威身份：每个文件最多一次；查不到就保持不可连带替换，而不是当成兼容 */
        suspend fun identityOf(fileName: String): InstalledMod? {
            if (identities.containsKey(fileName)) return identities[fileName]
            if (identities.size >= MAX_REPAIR_FINGERPRINTS || expired()) throw ModDoctorRepairBoundReached()
            val resolved = try {
                installedIdentity(fileName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                null
            }
            identities[fileName] = resolved
            return resolved
        }

        /** 暂存一个候选：仍然走既有 DownloadSingle 管线（stage-only），并在单候选上限内等待 */
        suspend fun stage(candidate: PlatformVersion): File {
            if (downloads >= bounds.stagedDownloads || expired()) throw ModDoctorRepairBoundReached()
            downloads++
            val started = nowMillis()
            val stagedFile = withTimeoutOrNull(bounds.candidateTimeoutMillis) { stageCandidate(candidate) }
                ?: throw ModDoctorRepairFailure(
                    if (nowMillis() - started >= bounds.candidateTimeoutMillis) {
                        "The candidate download timed out."
                    } else {
                        "The candidate download/install failed."
                    }
                )
            staged += stagedFile
            return stagedFile
        }
    }

    /**
     * 执行完整计划：只有走到这里才会改动 mods 目录。
     *
     * 先禁用全部原文件，再复制全部暂存替换并逐个核对 sha1；任何一步失败都删除已复制的文件、
     * 恢复全部原文件，并核对修复前的哈希集合。暂存文件由调用方在 finally 里清理。
     */
    internal suspend fun apply(plan: ModDoctorRepairPlan): ModDoctorRepairOutcome {
        if (plan.replacements.isEmpty()) return ModDoctorRepairOutcome.NoSafePlan
        //fileName 为空 = 纯新增（缺失依赖）：没有原文件要保留；其余逻辑（可逆保留、逐个核对 sha1、整体回滚）完全一样
        val originals = plan.replacements.filter { it.fileName.isNotBlank() }.map { File(modsDir, it.fileName) }
        val preState = LinkedHashMap<String, String>()
        for (original in originals) {
            val sha1 = sha1Of(original) ?: return ModDoctorRepairOutcome.Failed(REPAIR_PRESERVE_FAILED)
            preState[original.name] = sha1
        }

        //原文件的可逆保留位（.disabled）：回滚必须按它们的真实路径恢复，不能按启用路径猜
        val disabled = mutableListOf<File>()
        val copied = mutableListOf<File>()

        suspend fun rollback(): Boolean {
            var restored = true
            copied.forEach { file -> if (file.exists() && !file.delete()) restored = false }
            disabled.forEach { file -> if (file.exists() && file.enableModFile() == null) restored = false }
            originals.forEach { original -> if (sha1Of(original) != preState[original.name]) restored = false }
            return restored
        }

        //禁用全部原文件：先全部可逆保留，再开始复制，任何原文件无法保留都不改动 mods 目录
        for (original in originals) {
            val preserved = preserveSafely(original)
            if (preserved == null) {
                val restored = rollback()
                return ModDoctorRepairOutcome.Failed(
                    if (restored) REPAIR_PRESERVE_FAILED else REPAIR_ROLLBACK_FAILED
                )
            }
            disabled += preserved
        }

        //复制全部暂存替换并逐个核对 sha1：任何一个不成立就整体回滚
        for (planned in plan.replacements) {
            val target = File(modsDir, planned.target.platformFileName())
            val expected = planned.target.platformSha1()?.takeIf { it.isNotBlank() } ?: sha1Of(planned.stagedFile)
            var failure: String? = null
            //目标文件名已被占用：不删除。先可逆地保留（.disabled），保留不了就停止；回滚时会把它还回来
            val collision = if (target.exists()) preserveSafely(target) else null
            if (target.exists() && collision == null) {
                failure = REPAIR_COPY_FAILED
            } else {
                if (collision != null) disabled += collision
                try {
                    planned.stagedFile.copyTo(target, overwrite = true)
                    copied += target
                    if (expected == null || sha1Of(target) != expected) failure = REPAIR_VERIFY_FAILED
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    failure = REPAIR_COPY_FAILED
                }
            }
            if (failure != null) {
                val restored = rollback()
                return ModDoctorRepairOutcome.Failed(if (restored) failure else REPAIR_ROLLBACK_FAILED)
            }
        }

        return ModDoctorRepairOutcome.Repaired(
            installedFileNames = plan.replacements.map { it.target.platformFileName() },
            replacedFileNames = plan.replacements.map { it.fileName }.filter { it.isNotBlank() },
        )
    }

    /** 复用现有指纹实现取 SHA-1（带内存缓存），只用于修复前后的一致性核对 */
    private suspend fun sha1Of(file: File): String? =
        runCatching { computeModFingerprints(file).sha1 }.getOrNull()
}

/** 修复失败原因：同一个实例上已有修复或回滚在运行 */
private const val REPAIR_BUSY = "Another repair or rollback is already running for this instance."

/** 修复失败原因：修复之前无法备份 mods 目录，所以没有改动任何文件 */
private const val REPAIR_SNAPSHOT_FAILED = "Could not back up the mods folder first, so nothing was changed."

/**
 * 可逆地保留一个文件（改名为 `.disabled`）。
 * `disableModFile` 内部用 REPLACE_EXISTING：如果 `.disabled` 已经存在会被悄悄覆盖，这里先拒绝这种情况。
 * @return 保留后的文件；`.disabled` 已被占用或改名失败时返回 null（调用方必须停止，不得继续改动）
 */
internal fun preserveSafely(file: File): File? {
    if (File("${file.absolutePath}.disabled").exists()) return null
    return file.disableModFile()
}

/** 修复失败原因：原文件无法可逆保留 */
private const val REPAIR_PRESERVE_FAILED = "Could not preserve the incompatible mod file safely."

/** 修复失败原因：替换文件复制失败 */
internal const val REPAIR_PLAN_REVIEW = "The repair plan was re-checked after the earlier attempt. Review the updated plan and confirm again; nothing was changed."

private const val REPAIR_POSTCHECK_ROLLED_BACK = "The rescan after the repair found a problem, so the repair was rolled back and the original files were restored."

/** ชนิดปัญหาที่เกี่ยวกับความสัมพันธ์ระหว่างม็อด (depends/breaks): ใช้ planner ทั้งชุดแก้ */
internal val FAMILY_PROBLEM_TYPES = setOf(
    ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY,
    ModDoctorProblemType.INCOMPATIBLE_DEPENDENCY_VERSION,
    ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR,
)

/** วิเคราะห์ depends/breaks จาก fabric.mod.json: ใช้ได้กับ Fabric และ Quilt (Quilt ใช้โมเดลม็อดของ Fabric) */
internal fun ModLoader?.supportsFamilyRepair(): Boolean = this == ModLoader.FABRIC || this == ModLoader.QUILT

private const val REPAIR_COPY_FAILED = "The replacement copy failed."

/** Fix เจอว่าแผนหรือไฟล์เปลี่ยนหลังจาก Preview: ไม่แตะอะไร ให้ UI ขอ Preview/Confirm ใหม่ */
internal fun isRepairPlanReview(reason: String?): Boolean = reason == REPAIR_PLAN_REVIEW

/** 修复失败原因：复制后的替换文件没有通过 sha1 核对 */
private const val REPAIR_VERIFY_FAILED = "The replacement file failed verification."

/** 修复失败原因：回滚后原文件没有回到修复前的状态 */
private const val REPAIR_ROLLBACK_FAILED = "The replacement failed and the original files could not be restored."

/**
 * Report-only validation for the selected instance. It never writes to the mods directory.
 */
class ModDoctor {
    /**
     * Play First：启动前只保留整合包 required core 的完整性检查（文件缺失/校验和不符会让整合包本身不完整），
     * 不做依赖、版本、冲突扫描，也不访问网络；那些问题由游戏崩溃后的崩溃页面、或用户在模组页面手动「ตรวจม็อด」处理。
     */
    suspend fun prelaunchSafetyReport(version: Version): ModDoctorReport =
        prelaunchSafetyProblems(ModpackPolicy.load(version.getVersionPath()), VersionFolders.MOD.getDir(version.getGameDir()))

    suspend fun inspect(version: Version): ModDoctorReport {
        val versionInfo = version.getVersionInfo() ?: return ModDoctorReport(emptyList())
        val loader = versionInfo.loaderInfo?.loader ?: ModLoader.UNKNOWN
        val modsDir = VersionFolders.MOD.getDir(version.getGameDir())
        val policy = ModpackPolicy.load(version.getVersionPath())
        if (policy.isPackManaged) {
            val coreProblems = policy.requiredCoreProblems(modsDir)
            if (coreProblems.isNotEmpty()) {
                Logger.info(TAG, "required pack core blocked normal rules: ${coreProblems.size} problem(s)")
                return ModDoctorReport(coreProblems)
            }
        }
        val report = inspect(
            modsDir = modsDir,
            minecraftVersion = versionInfo.minecraftVersion,
            loader = loader,
        )
        //本机学到的警告在安全模式之前注入：整合包文件仍然不许被自动改动
        val learned = report.withLearnedLocalCrashWarnings(
            instanceName = version.getVersionName(),
            minecraftVersion = versionInfo.minecraftVersion,
            loader = loader,
            modsDir = modsDir,
        )
        //只限制 required core 的自动替换；普通扫描结果仍完整保留，包括用户额外安装模组的问题。
        return learned.applyPackSafeMode(policy, modsDir)
            .withGroupPlan(version, modsDir, versionInfo.minecraftVersion, loader)
    }

    suspend fun inspect(
        modsDir: File,
        minecraftVersion: String,
        loader: ModLoader,
    ): ModDoctorReport {
        //可观测性：每次 inspect 的起止都写一行，且必须在 try 之外，
        //否则日志抛出会被 catch 洗成 CORRUPT_METADATA 而误报并阻塞启动
        Logger.info(TAG, "inspect started")
        val result = try {
            val locals = AllModReader(modsDir).readAllLocals()
            val metadata = locals
                .filter { it.file.isActiveModCandidate() }
                .map { local -> toDoctorMetadata(local, loader) }
            //本地元数据无法定论时，用权威指纹匹配得到的 provider 文件声明的游戏版本补充判断；
            //匹配不到、离线或 provider 失败都保持未知，不产生阻塞性不兼容
            val providerGameVersions = providerGameVersionsForAmbiguousMods(
                mods = metadata,
                minecraftVersion = minecraftVersion,
                lookup = { fileName ->
                    lookupInstalledMod(modsDir, fileName, modDoctorFingerprintPlatform(AllSettings.searchModPlatform.getValue()))
                },
            )
            val report = validateModDoctorMetadata(
                mods = metadata,
                minecraftVersion = minecraftVersion,
                loader = loader,
                providerGameVersions = providerGameVersions,
                setRules = ModDoctorRules.load(),
            )
            //同一依赖可能被多个已启用模组声明：候选必须同时满足它们各自的约束，这里不替它们猜
            val requirements = metadata
                .flatMap { it.requiredDependencyPredicates.entries }
                .filter { it.value.isNotEmpty() }
                .groupBy({ it.key }, { it.value })
            report.withFixCandidates(
                minecraftVersion,
                loader,
                modsDir = modsDir,
                requiredDependencyRequirements = requirements,
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            ModDoctorReport(
                listOf(
                    ModDoctorProblem(
                        type = ModDoctorProblemType.CORRUPT_METADATA,
                        fileName = modsDir.name,
                        modId = null,
                        reason = "Could not inspect mod metadata safely: ${e.message ?: "unknown error"}",
                    )
                )
            )
        }
        Logger.info(TAG, "inspect finished: ${result.problems.size} problem(s) ${result.problems.joinToString(prefix = "[", postfix = "]") { it.type.name }}")
        return result
    }

    /**
     * 修复报告中可修复的问题：同项目安全替换（错误加载器 / Minecraft 版本不兼容）与缺失必装依赖
     */
    suspend fun fixProblems(
        version: Version,
        report: ModDoctorReport,
    ): ModDoctorFixResult {
        val versionPath = version.getVersionPath()
        val modsDir = VersionFolders.MOD.getDir(version.getGameDir())
        //同一个实例同时只允许一个修复/回滚
        if (!ModRepairLock.tryAcquire(versionPath)) {
            return ModDoctorFixResult(report = report, failureReason = REPAIR_BUSY)
        }
        try {
            //上一次修复没有完成（下载/安装失败）时，该问题的 fixCandidate 已被清空：直接重试会把失败的那一项静默跳过，
            //所以重试前先重新检查，拿到带候选的最新报告
            if (report.hasIncompleteRepairMark()) {
                //上一次尝试没走完，重新检查得到的方案可能与用户确认过的预览不同：这一次不动任何文件，把新的报告交回去让用户重新看、重新确认
                return ModDoctorFixResult(report = inspect(version), failureReason = REPAIR_PLAN_REVIEW)
            }
            return fixProblemsWithSnapshot(version, report, versionPath, modsDir)
        } finally {
            ModRepairLock.release(versionPath)
        }
    }

    private suspend fun fixProblemsWithSnapshot(
        version: Version,
        report: ModDoctorReport,
        versionPath: File,
        modsDir: File,
    ): ModDoctorFixResult {
        //任何修复之前先备份整个 mods 目录并核对；备份不成功就一个文件都不动
        val snapshot = when (val created = withContext(Dispatchers.IO) { ModRepairSnapshots.create(versionPath, modsDir) }) {
            is SnapshotCreateResult.Created -> created.snapshot
            is SnapshotCreateResult.Refused -> {
                Logger.warning(TAG, "Repair refused: ${created.reason}")
                return ModDoctorFixResult(report = report, failureReason = REPAIR_SNAPSHOT_FAILED)
            }
        }
        var result: ModDoctorFixResult? = null
        var finalized: RepairSnapshot? = null
        try {
            result = fixProblemsUnprotected(version, report)
        } finally {
            val ok = result != null && result.failureReason == null
            finalized = runCatching { withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) { ModRepairSnapshots.finalize(versionPath, snapshot, modsDir, ok) } }.getOrNull()
            //异常 / 取消（result 还是 null）而快照是 PARTIAL：同样用真实回滚还原，不把半截状态留给用户
            if (result == null && finalized?.status == SnapshotStatus.PARTIAL) {
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) { runCatching { ModRepairSnapshots.rollback(versionPath, finalized!!, modsDir) } }
            }
        }
        val done = result!!
        //失败且快照是 PARTIAL（例如前一阶段已改动、后一阶段失败）：自动回滚，报告真实结果
        if (done.failureReason != null) return failWithRollbackIfPartial(done, finalized, versionPath, modsDir)
        return verifyAfterFix(version, report, done, finalized, versionPath, modsDir)
    }

    /**
     * 修复后复检（不让「成功」是假的）：重新扫描一次，修复前没有的新问题、或规则方案声称要解决却还在的问题，
     * 都按快照安全回滚（只移动、不删除），并把这次修复报告为失败。
     */
    private suspend fun verifyAfterFix(
        version: Version,
        before: ModDoctorReport,
        result: ModDoctorFixResult,
        finalized: RepairSnapshot?,
        versionPath: File,
        modsDir: File,
    ): ModDoctorFixResult {
        if (result.failureReason != null || finalized == null || finalized.status != SnapshotStatus.APPLIED) return result
        val info = version.getVersionInfo() ?: return result
        return verifyRepairedSet(
            before = before,
            result = result,
            finalized = finalized,
            versionPath = versionPath,
            modsDir = modsDir,
            rescan = { inspect(modsDir, info.minecraftVersion, info.loaderInfo?.loader ?: ModLoader.UNKNOWN) },
        )
    }

    /** 用户触发的回滚：恢复最近一次修复之前的 mods 状态；没有可回滚的快照时返回 null */
    internal suspend fun rollbackLastRepair(version: Version): RollbackResult? = withContext(Dispatchers.IO) {
        val versionPath = version.getVersionPath()
        val modsDir = VersionFolders.MOD.getDir(version.getGameDir())
        //修复或另一个回滚正在运行：不开始
        if (!ModRepairLock.tryAcquire(versionPath)) return@withContext null
        try {
            //锁已由本次回滚持有，所以这里要直接读快照，不能再走会因锁而返回 null 的 latestRollbackable
            val snapshot = ModRepairSnapshots.latestRollbackableLocked(versionPath, modsDir) ?: return@withContext null
            ModRepairSnapshots.rollback(versionPath, snapshot, modsDir)
        } finally {
            ModRepairLock.release(versionPath)
        }
    }

    /** 这个实例上是否有修复或回滚正在运行 */
    fun isRepairBusy(version: Version): Boolean = ModRepairLock.isHeld(version.getVersionPath())

    /** 是否存在可以回滚的修复（用于决定要不要显示“回滚”入口） */
    suspend fun hasRollbackableRepair(version: Version): Boolean = withContext(Dispatchers.IO) {
        ModRepairSnapshots.latestRollbackable(version.getVersionPath(), VersionFolders.MOD.getDir(version.getGameDir())) != null
    }

    private suspend fun fixProblemsUnprotected(
        version: Version,
        report: ModDoctorReport,
    ): ModDoctorFixResult {
        val installed = mutableListOf<String>()
        val modsDir = VersionFolders.MOD.getDir(version.getGameDir())

        //Protect required-core replacements, while preserving ordinary rules and repairs for user additions.
        val policy = ModpackPolicy.load(version.getVersionPath())
        val effective = report.applyPackSafeMode(policy, modsDir)

        //Missing required-core entries with exact source identity reuse the existing downloader.
        val restores = effective.problems.mapNotNull { it.exactRestoreFile }.distinctBy { it.name }
        for (restore in restores) {
            val url = restore.url
            val sha1 = restore.sha1
            if (url.isNullOrBlank() || sha1.isNullOrBlank()) {
                return ModDoctorFixResult(
                    report = effective,
                    installedFileNames = installed,
                    failureReason = "The modpack file could not be restored exactly."
                )
            }
            val restored = runCatching {
                downloadFile(url = url, outputFile = File(modsDir, restore.name), sha1 = sha1)
            }.isSuccess
            if (!restored) {
                return ModDoctorFixResult(
                    report = effective,
                    installedFileNames = installed,
                    failureReason = "The modpack file could not be restored exactly."
                )
            }
            installed += restore.name
        }
        if (restores.isNotEmpty()) return ModDoctorFixResult(effective, installedFileNames = installed)

        //整合包实例里被安全模式拦下的动作：只报告，不做任何改动
        if (policy.isPackManaged && effective != report) {
            return ModDoctorFixResult(
                report = effective,
                installedFileNames = installed,
                failureReason = PACK_MANAGED_REASON
            )
        }

        //本机学到的「禁用这个文件就能启动」是纯本地动作：不下载、不安装，也不排在网络工作后面
        val localDisables = effective.problems
            .mapNotNull { it.localDisableFix }
            .distinctBy { it.fileName }
        for (fix in localDisables) {
            val target = File(modsDir, fix.fileName)
            val disabled = if (target.isFile && target.isEnabled()) preserveSafely(target) else null
            if (disabled == null) {
                return ModDoctorFixResult(
                    report = effective.markLocalDisableFixFailed(fix.fileName),
                    installedFileNames = installed,
                    failureReason = REPAIR_PRESERVE_FAILED,
                )
            }
        }

        //已知兼容成套规则排在其它修复之前：规则里的允许集合就是这一整组模组的权威版本，
        //而成对冲突（breaks）与缺失依赖都是在拿不准整组的情况下做的单点修补，先跑会把这一组
        //拆开逐对修，反而得不到成套规则已经给出答案的那套版本。
        val setFixes = effective.problems
            .mapNotNull { it.setFix }
            .distinctBy { setFix -> setFix.members.joinToString { member -> member.fileName } }
        for (setFix in setFixes) {
            when (val repair = repairKnownSet(setFix, version, modsDir, policy)) {
                is ModDoctorRepairOutcome.Repaired -> {
                    installed += repair.installedFileNames
                    return ModDoctorFixResult(effective, installedFileNames = installed)
                }
                is ModDoctorRepairOutcome.NoSafePlan -> return ModDoctorFixResult(
                    report = effective.markSetFixUnavailable(),
                    installedFileNames = installed,
                    failureReason = "The known compatible set could not be installed."
                )
                is ModDoctorRepairOutcome.Failed -> return ModDoctorFixResult(
                    report = effective.markSetFixUnavailable(),
                    installedFileNames = installed,
                    failureReason = repair.reason,
                )
            }
        }

        //已发布规则命中的版本不对 / 成对冲突：同样排在通用修复之前（只会下载规则点名的版本）；
        //规则不能执行（NoSafePlan）才退回库升级或冲突解算器，两者都仍要过完整的暂存校验
        effective.problems.firstOrNull { it.ruleFallbackFix != null }?.ruleFallbackFix?.let { ruleFix ->
            when (val ruled = repairKnownSet(ruleFix, version, modsDir, policy)) {
                is ModDoctorRepairOutcome.Repaired -> return ModDoctorFixResult(effective, installedFileNames = installed + ruled.installedFileNames)
                is ModDoctorRepairOutcome.Failed -> return ModDoctorFixResult(effective.markSetFixUnavailable(), installed, ruled.reason)
                is ModDoctorRepairOutcome.NoSafePlan -> Unit
            }
        }

        //ชุด Sodium/Iris และม็อดที่เกี่ยวข้อง: คำนวณแผนทั้งชุดครั้งเดียว (ติดตั้ง/อัปเกรด/ดาวน์เกรด) จาก metadata จริง
        //แล้วใช้ transaction เดียว; ถ้าไม่มีแผนที่ปลอดภัยและผู้ใช้ยังไม่ได้เห็น Preview ของแผนนี้ ก็ใช้เส้นทางเดิมต่อ
        //ใช้ได้เฉพาะเมื่อผู้ใช้เห็น Preview และ Confirm แผนนั้นแล้ว (มี familyPlanKey) — ไม่มี key = เส้นทางเดิมทั้งหมด
        val approvedKey = effective.familyPlanKey
        if (approvedKey != null && effective.problems.any { it.type in FAMILY_PROBLEM_TYPES }) {
            val info = version.getVersionInfo()
            if (info != null && info.loaderInfo?.loader.supportsFamilyRepair()) {
                return applyApprovedFamilyPlan(version, modsDir, policy, info, effective, approvedKey, installed)
            }
        }

        val breaksFixes = effective.problems
            .mapNotNull { it.breaksFix }
            .distinctBy { it.target.fileName }
        //同一次修复里还缺少依赖且已解析出提供者：交给解算器一并校验（整合包实例不放行，保持原有的拒绝行为）
        val pendingDependencies = if (policy.isPackManaged) emptyList() else effective.problems
            .filter { it.type == ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY }
            .mapNotNull { it.fixCandidate }
            .distinctBy { "${it.platform.name}/${it.projectId}" }
            .map { it.projectId to it.platform }
        for (fix in breaksFixes) {
            //目标模组优先、源模组回退的逻辑在有界多模组解算器里：
            //只有在整组替换计划校验通过后才会真正改动 mods 目录。
            when (val repair = repairBreaksConflict(fix, version, modsDir, policy, pendingDependencies)) {
                is ModDoctorRepairOutcome.Repaired -> installed += repair.installedFileNames
                is ModDoctorRepairOutcome.NoSafePlan -> return ModDoctorFixResult(
                    report = effective.markBreaksFixUnavailable(),
                    installedFileNames = installed,
                    failureReason = "No safe compatible replacement was found for either conflicting mod."
                )
                is ModDoctorRepairOutcome.Failed -> return ModDoctorFixResult(
                    report = effective.markBreaksFixUnavailable(),
                    installedFileNames = installed,
                    failureReason = repair.reason,
                )
            }
        }

        val replacementFixes = effective.problems
            .mapNotNull { it.replacementFix }
            .distinctBy { it.fileName }
        if (replacementFixes.isNotEmpty()) {
            val replacementsDir = VersionFolders.MOD.getDir(version.getGameDir())
            val staged = mutableListOf<StagedEntry>()
            try {
                //阶段 1：只暂存、不碰 mods；全部暂存完用整组校验器检查「假设结果」，不安全就在改动任何文件之前停下
                val refusal = stageAndValidateReplacements(replacementFixes, version, replacementsDir, staged)
                if (refusal != null) {
                    return ModDoctorFixResult(
                        report = effective.markReplacementFixFailed(replacementFixes.first().fileName),
                        installedFileNames = installed,
                        failureReason = refusal,
                    )
                }
                //阶段 2：安装的就是阶段 1 校验过的那几个文件本身（先确认没变），由既有的 apply() 一次完成：
                //可逆保留原文件 → 复制 → 逐个核对 sha1，任何一步失败整体回滚。没有第二次下载，也没有异步写 mods 的任务
                when (val outcome = commitStaged(version, replacementsDir, policy, staged)) {
                    is ModDoctorRepairOutcome.Repaired -> installed += outcome.installedFileNames
                    is ModDoctorRepairOutcome.NoSafePlan -> return ModDoctorFixResult(
                        report = effective.markReplacementFixFailed(replacementFixes.first().fileName),
                        installedFileNames = installed,
                        failureReason = "The replacement plan could not be applied safely.",
                    )
                    is ModDoctorRepairOutcome.Failed -> return ModDoctorFixResult(
                        report = effective.markReplacementFixFailed(replacementFixes.first().fileName),
                        installedFileNames = installed,
                        failureReason = outcome.reason,
                    )
                }
            } finally {
                staged.forEach { runCatching { it.file.delete() } }
            }
        }

        //已装但版本不对的库：每个库文件只换一次，换完靠修复后的重新检查确认；没有共同版本就整体拒绝
        val libraryFixes = effective.problems.mapNotNull { it.dependencyVersionFix }.distinctBy { it.fileName }
        for (fix in libraryFixes) {
            when (val repair = repairLibraryVersion(fix, version, VersionFolders.MOD.getDir(version.getGameDir()), policy, pendingDependencies)) {
                is ModDoctorRepairOutcome.Repaired -> installed += repair.installedFileNames
                is ModDoctorRepairOutcome.NoSafePlan -> {
                    val fallback = effective.problems.firstOrNull { it.dependencyVersionFix?.fileName == fix.fileName && it.ruleFallbackFix != null }?.ruleFallbackFix
                    if (fallback != null) {
                        when (val ruled = repairKnownSet(fallback, version, VersionFolders.MOD.getDir(version.getGameDir()), policy)) {
                            is ModDoctorRepairOutcome.Repaired -> return ModDoctorFixResult(effective, installedFileNames = installed + ruled.installedFileNames)
                            is ModDoctorRepairOutcome.Failed -> return ModDoctorFixResult(effective.markLibraryFixUnavailable(fix.fileName, ruled.reason, incomplete = true), installed, ruled.reason)
                            is ModDoctorRepairOutcome.NoSafePlan -> Unit
                        }
                    }
                    return ModDoctorFixResult(
                        report = effective.markLibraryFixUnavailable(
                            fix.fileName,
                            "no version of this library satisfies every installed mod that uses it without a new conflict.",
                            incomplete = false,
                        ),
                        installedFileNames = installed,
                        failureReason = "No safe version of the shared dependency was found; the original files were kept.",
                    )
                }
                is ModDoctorRepairOutcome.Failed -> return ModDoctorFixResult(
                    report = effective.markLibraryFixUnavailable(fix.fileName, repair.reason, incomplete = true),
                    installedFileNames = installed,
                    failureReason = repair.reason,
                )
            }
        }

        //没有库升级方案（解析阶段就没有候选）但规则给出了后备：按规则执行
        effective.problems.firstOrNull { it.type == ModDoctorProblemType.INCOMPATIBLE_DEPENDENCY_VERSION && it.dependencyVersionFix == null && it.ruleFallbackFix != null }?.ruleFallbackFix?.let { ruleFix ->
            when (val ruled = repairKnownSet(ruleFix, version, VersionFolders.MOD.getDir(version.getGameDir()), policy)) {
                is ModDoctorRepairOutcome.Repaired -> return ModDoctorFixResult(effective, installedFileNames = installed + ruled.installedFileNames)
                is ModDoctorRepairOutcome.Failed -> return ModDoctorFixResult(effective.markSetFixUnavailable(), installed, ruled.reason)
                is ModDoctorRepairOutcome.NoSafePlan -> return ModDoctorFixResult(effective.markSetFixUnavailable(), installed, "The known compatible set could not be installed.")
            }
        }

        val fixable = effective.problems
            .filter { it.type == ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY }
            .mapNotNull { it.fixCandidate }
            .distinctBy { "${it.platform.name}/${it.projectId}" }
        if (fixable.isEmpty()) return ModDoctorFixResult(effective, installedFileNames = installed)

        //阶段 1：每个依赖选出（并暂存）一个候选，全部暂存后用整组校验器检查「已启用模组 + 全部新依赖」，这期间不碰 mods
        val dependencyModsDir = VersionFolders.MOD.getDir(version.getGameDir())
        val entries = mutableListOf<StagedEntry>()
        val everyStagedFile = mutableListOf<File>()
        try {
            for (candidate in fixable) {
                val matched = runCatching {
                    matchInstalledMods(
                        fingerprints = scanModFingerprints(dependencyModsDir),
                        platform = candidate.platform
                    )
                }.getOrNull()
                if (matched == null || !matched.complete) {
                    return ModDoctorFixResult(
                        report = effective.markFixFailed(candidate.dependencyId),
                        installedFileNames = installed,
                        failureReason = "Could not verify the installed dependency set safely."
                    )
                }
                if (candidate.projectId in matched.byProject) continue

                val compatibleCandidates = runCatching {
                    getVersions(candidate.projectId, candidate.platform)
                        .initAll(candidate.projectId)
                        .filter { it.isCompatibleWith(version, PlatformClasses.MOD) }
                        //与预览用同一个约束：不把一定会被拒绝的版本下载下来
                        .filter { platformVersionSatisfies(it, candidate.requirements) }
                }.getOrNull().orEmpty()
                if (compatibleCandidates.isEmpty()) {
                    return ModDoctorFixResult(
                        report = effective.markFixFailed(candidate.dependencyId),
                        installedFileNames = installed,
                        failureReason = "No authoritative compatible dependency file was found."
                    )
                }

                //候选自己声明的 Fabric 元数据才是权威依据：逐个按“最新优先”暂存并校验（breaks 双向 + 已启用模组对它的要求），
                //被拒的候选当场丢弃；候选自己的依赖由下面的整组校验一并检查
                val enabledMods = enabledModIdentities(dependencyModsDir)
                val orderedCandidates = stableFirstCandidates(compatibleCandidates) { platformVersion ->
                    platformVersion.platformReleaseType() != PlatformReleaseType.RELEASE
                }
                val choice = firstAcceptedCandidate(orderedCandidates) { platformVersion ->
                    val stagedFile = stageModFile(platformVersion, version)
                    if (stagedFile == null) {
                        ModDoctorCandidateAttempt.Failed("The dependency download/install failed.")
                    } else {
                        everyStagedFile += stagedFile
                        //暂存文件名是缓存键，原因里要用真正的模组文件名
                        val facts = readFabricModFacts(stagedFile).copy(fileName = platformVersion.platformFileName())
                        val stagedSha1 = runCatching { calculateFileSha1(stagedFile) }.getOrNull()
                        val reason = when {
                            facts.asInstalledIdentity() == null -> "The dependency file's metadata could not be read, so it cannot be verified."
                            stagedSha1 == null -> "The dependency file could not be hashed, so it cannot be verified."
                            else -> candidateRejectionReason(facts, enabledMods)
                        }
                        if (reason != null) {
                            runCatching { stagedFile.delete() }
                            ModDoctorCandidateAttempt.Rejected(reason)
                        } else {
                            ModDoctorCandidateAttempt.Accepted(StagedEntry("", platformVersion, stagedFile, stagedSha1!!, facts, candidate.dependencyId))
                        }
                    }
                }
                choice.failure?.let { failureReason ->
                    return ModDoctorFixResult(
                        report = effective.markFixFailed(candidate.dependencyId),
                        installedFileNames = installed,
                        failureReason = failureReason
                    )
                }
                val accepted = choice.accepted
                if (accepted == null) {
                    return ModDoctorFixResult(
                        report = effective.markFixUnavailable(candidate.dependencyId, choice.rejections),
                        installedFileNames = installed,
                        failureReason = "Every compatible dependency release conflicts with the installed mod set."
                    )
                }
                entries += accepted
            }
            if (entries.isEmpty()) return ModDoctorFixResult(effective, installedFileNames = installed)

            planResultRejection(enabledModIdentities(dependencyModsDir), emptyList(), entries.map { it.facts })?.let { reason ->
                Logger.info(TAG, "The dependency plan was refused before any file changed: $reason")
                return ModDoctorFixResult(
                    report = effective.markFixUnavailable(entries.first().label, listOf(reason)),
                    installedFileNames = installed,
                    failureReason = "The dependency set would not be consistent: $reason",
                )
            }

            //阶段 2：安装的就是阶段 1 校验过的那几个文件本身（先确认没变），由既有的 apply() 一次完成，整体回滚；没有第二次下载
            when (val outcome = commitStaged(version, dependencyModsDir, policy, entries)) {
                is ModDoctorRepairOutcome.Repaired -> installed += outcome.installedFileNames
                is ModDoctorRepairOutcome.NoSafePlan -> return ModDoctorFixResult(
                    report = effective.markFixFailed(entries.first().label),
                    installedFileNames = installed,
                    failureReason = "The dependency plan could not be applied safely.",
                )
                is ModDoctorRepairOutcome.Failed -> return ModDoctorFixResult(
                    report = effective.markFixFailed(entries.first().label),
                    installedFileNames = installed,
                    failureReason = outcome.reason,
                )
            }
        } finally {
            everyStagedFile.forEach { runCatching { it.delete() } }
        }
        return ModDoctorFixResult(effective, installedFileNames = installed)
    }

    /**
     * ONE repair plan สำหรับทั้งชุด: อ่านเฉพาะ (ดาวน์โหลดเข้าที่พักชั่วคราวแล้วลบทิ้ง ไม่แตะ mods)
     * ผล [FamilyOutcome.Plan] เก็บเฉพาะข้อมูลแสดงผล ไฟล์ที่พักถูกลบแล้ว; Fix จะคำนวณใหม่และต้องได้แผนเดียวกัน (key)
     */
    internal suspend fun planFamilyRepair(version: Version): FamilyOutcome {
        val info = version.getVersionInfo() ?: return FamilyOutcome.NotApplicable
        if (!info.loaderInfo?.loader.supportsFamilyRepair()) return FamilyOutcome.NotApplicable
        val modsDir = VersionFolders.MOD.getDir(version.getGameDir())
        val planner = familyPlanner(version, modsDir, ModpackPolicy.load(version.getVersionPath()), info)
        val outcome = planner.plan()
        //ไฟล์ที่ดาวน์โหลดและตรวจ Hash แล้วเก็บไว้ให้ Fix ใช้ต่อ (Fix ตรวจซ้ำทุกครั้ง); ยกเลิก = discardFamilyPreview
        if (outcome is FamilyOutcome.Plan) FamilyPlanCache.put(version.getVersionPath().absolutePath, outcome.plan)
        else FamilyPlanCache.discard(version.getVersionPath().absolutePath)
        return outcome
    }

    /** ผู้ใช้กด Cancel ใน Preview: ลบไฟล์ที่พักไว้ ไม่แตะ mods */
    internal fun discardFamilyPreview(version: Version) = FamilyPlanCache.discard(version.getVersionPath().absolutePath)

    /**
     * Preview ที่เป็นแผนเดียวกันทุกทางเข้า (หน้า Crash และหน้า Mods): ตรวจรายงาน → แผนทั้งชุด
     * กฎ known-good และปัญหาที่ไม่ใช่เรื่องความสัมพันธ์ของม็อด คืน NotApplicable (ใช้เส้นทางเดิม)
     */
    internal suspend fun previewFamilyRepair(version: Version): FamilyPreviewResult {
        val report = inspect(version)
        val relation = report.problems.filter { it.type in FAMILY_PROBLEM_TYPES }
        if (relation.isEmpty() || report.problems.any { it.ruleFallbackFix != null || it.setFix != null }) return FamilyPreviewResult.NotApplicable
        return when (val outcome = planFamilyRepair(version)) {
            is FamilyOutcome.Plan -> FamilyPreviewResult.Plan(
                FamilyRepairPreview(
                    items = outcome.plan.changes.map { it.toPreviewItem() },
                    key = outcome.plan.key,
                    report = ModDoctorReport(problems = relation, familyPlanKey = outcome.plan.key),
                )
            )
            is FamilyOutcome.Refused -> FamilyPreviewResult.NoFix(outcome.reason)
            is FamilyOutcome.Failed -> FamilyPreviewResult.Failed(outcome.reason)
            FamilyOutcome.NotApplicable -> FamilyPreviewResult.NotApplicable
        }
    }

    /**
     * Apply แผนที่ผู้ใช้ยืนยัน: ใช้ไฟล์ที่พักจาก Preview ถ้ายังผ่าน [validateFamilyPlan] ทุกข้อ
     * ไม่เช่นนั้นคำนวณใหม่ได้ก็ต่อเมื่อแผนเดียวกัน (key ตรงกัน) — ไม่ตรง = ไม่แตะอะไร ให้ผู้ใช้ดู Preview ใหม่
     */
    private suspend fun applyApprovedFamilyPlan(
        version: Version,
        modsDir: File,
        policy: ModpackPolicy,
        info: com.movtery.zalithlauncher.game.version.installed.VersionInfo,
        effective: ModDoctorReport,
        approvedKey: String,
        installed: List<String>,
    ): ModDoctorFixResult {
        val instance = version.getVersionPath().absolutePath
        var plan: FamilyRepairPlan? = FamilyPlanCache.take(instance, approvedKey)
        if (plan != null) {
            val reason = validateFamilyPlan(plan, modsDir, readEnabledModIdentities(modsDir).sortedBy { it.fileName })
            if (reason != null) {
                Logger.info(TAG, "The cached repair plan is no longer valid: $reason")
                FamilyPlanCache.delete(plan)
                plan = null
            }
        }
        val planner = familyPlanner(version, modsDir, policy, info)
        if (plan == null) {
            //ไม่มีไฟล์ที่พักที่ใช้ได้: คำนวณใหม่ (ดาวน์โหลดอีกครั้ง) ใช้ต่อได้เฉพาะเมื่อเป็นแผนเดียวกับที่ผู้ใช้ยืนยัน
            when (val family = planner.plan()) {
                is FamilyOutcome.Plan -> plan = family.plan
                is FamilyOutcome.Failed -> return ModDoctorFixResult(effective, installed, family.reason)
                is FamilyOutcome.Refused -> return ModDoctorFixResult(effective, installed, "The repair plan shown earlier is no longer possible: ${family.reason}")
                FamilyOutcome.NotApplicable -> return ModDoctorFixResult(effective, installed, REPAIR_PLAN_REVIEW)
            }
            if (plan.key != approvedKey) {
                val changed = plan
                FamilyPlanCache.delete(changed)
                return ModDoctorFixResult(effective.copy(familyPlanKey = changed.key), installed, REPAIR_PLAN_REVIEW)
            }
        }
        val approved = plan
        try {
            val entries = approved.changes.map { StagedEntry(it.fileName, it.target, it.stagedFile, it.sha1, it.facts, it.modId) }
            return when (val outcome = commitStaged(version, modsDir, policy, entries)) {
                is ModDoctorRepairOutcome.Repaired -> ModDoctorFixResult(effective, installedFileNames = installed + outcome.installedFileNames)
                is ModDoctorRepairOutcome.NoSafePlan -> ModDoctorFixResult(effective, installed, "The repair plan could not be applied safely.")
                is ModDoctorRepairOutcome.Failed -> ModDoctorFixResult(effective, installed, outcome.reason)
            }
        } finally {
            FamilyPlanCache.delete(approved)
        }
    }

    private fun familyPlanner(
        version: Version,
        modsDir: File,
        policy: ModpackPolicy,
        info: com.movtery.zalithlauncher.game.version.installed.VersionInfo,
    ): FamilyRepairPlanner {
        val userPlatform = AllSettings.searchModPlatform.getValue()
        val fingerprintPlatform = modDoctorFingerprintPlatform(userPlatform)
        val dependencyPlatform = modDoctorDependencyRepairPlatform(userPlatform)
        return FamilyRepairPlanner(
            minecraftVersion = info.minecraftVersion,
            loader = info.loaderInfo?.loader ?: ModLoader.UNKNOWN,
            modsDir = modsDir,
            isPackOwned = policy::isPackOwned,
            installedIdentity = { fileName -> lookupInstalledMod(modsDir, fileName, fingerprintPlatform) },
            projectVersions = { projectId, platform -> loadProjectVersions(projectId, platform) },
            dependencyProject = { id -> lookupProjectDirect(id, dependencyPlatform) },
            dependencyPlatform = dependencyPlatform,
            stageCandidate = { candidate -> stageModFile(candidate, version) },
        )
    }

    /**
     * 把阶段 1 校验过的暂存文件交给既有的 [ModDoctorMultiModRepair.apply]：可逆保留原文件（新增则没有）、复制、逐个核对 sha1，
     * 任何一步失败整体回滚并核对原文件哈希。apply 之前先确认每个暂存文件的 SHA-1 与元数据仍与校验时一致，否则拒绝、不动 mods。
     */
    private suspend fun commitStaged(
        version: Version,
        modsDir: File,
        policy: ModpackPolicy,
        entries: List<StagedEntry>,
    ): ModDoctorRepairOutcome {
        entries.firstNotNullOfOrNull { stagedFileMismatch(it.file, it.sha1, it.facts) }?.let { reason ->
            Logger.warning(TAG, "A staged file changed after it was verified; nothing was installed: $reason")
            return ModDoctorRepairOutcome.Failed("A staged file changed after it was verified, so nothing was installed.")
        }
        val versionInfo = version.getVersionInfo()
            ?: return ModDoctorRepairOutcome.Failed("The instance metadata could not be read safely.")
        val platform = modDoctorFingerprintPlatform(AllSettings.searchModPlatform.getValue())
        val executor = ModDoctorMultiModRepair(
            minecraftVersion = versionInfo.minecraftVersion,
            loader = versionInfo.loaderInfo?.loader ?: ModLoader.UNKNOWN,
            modsDir = modsDir,
            isPackOwned = policy::isPackOwned,
            installedIdentity = { fileName -> lookupInstalledMod(modsDir, fileName, platform) },
            projectVersions = { projectId, targetPlatform -> loadProjectVersions(projectId, targetPlatform) },
            stageCandidate = { candidate -> stageModFile(candidate, version) },
        )
        val plan = ModDoctorRepairPlan(entries.map { ModDoctorPlannedReplacement(it.replacedFileName, it.platformVersion, it.file, it.facts) })
        return try {
            executor.apply(plan)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Logger.warning(TAG, "Applying the staged plan failed", e)
            ModDoctorRepairOutcome.Failed("The replacement failed.")
        }
    }

    /**
     * 替换（WRONG_LOADER / WRONG_MINECRAFT_VERSION）的阶段 1：对每一项找到权威兼容构建并只暂存（文件留在 [out]，由调用方清理），
     * 全部暂存完后用整组校验器检查「假设结果」；不安全 / 读不出元数据 / 下载失败都返回拒绝原因，mods 目录完全没动。
     */
    private suspend fun stageAndValidateReplacements(
        fixes: List<ModDoctorReplacementFix>,
        version: Version,
        modsDir: File,
        out: MutableList<StagedEntry>,
    ): String? {
        val enabledBefore = readEnabledModIdentities(modsDir)
        for (fix in fixes) {
            val platformVersion = runCatching {
                getVersions(fix.projectId, fix.platform)
                    .initAll(fix.projectId)
                    .firstOrNull { it.isCompatibleWith(version, PlatformClasses.MOD) }
            }.getOrNull() ?: return "No authoritative compatible replacement file was found."
            val file = stageModFile(platformVersion, version) ?: return "The replacement download/install failed."
            val facts = readFabricModFacts(file).copy(fileName = platformVersion.platformFileName())
            val sha1 = runCatching { calculateFileSha1(file) }.getOrNull()
            //先登记，调用方统一清理暂存文件（含下面任何一次拒绝）
            out += StagedEntry(fix.fileName, platformVersion, file, sha1.orEmpty(), facts, fix.fileName)
            if (sha1 == null) return "The replacement file could not be hashed, so it cannot be verified."
        }
        planResultRejection(enabledBefore, out.map { it.replacedFileName }, out.map { it.facts })?.let { reason ->
            Logger.info(TAG, "The replacement plan was refused before any file changed: $reason")
            return "The replacement would not keep the mod set consistent: $reason"
        }
        return null
    }

    /**
     * 冲突修复：目标侧优先、源侧回退，两侧都交给同一个有界多模组解算器。
     *
     * 候选版本与权威身份都走与既存修复相同的来源（指纹匹配 + 平台版本列表），不需要新的解析器。
     */
    private suspend fun repairBreaksConflict(
        fix: ModDoctorBreaksFix,
        version: Version,
        modsDir: File,
        policy: ModpackPolicy,
        pendingDependencies: List<Pair<String, Platform>> = emptyList(),
    ): ModDoctorRepairOutcome {
        val versionInfo = version.getVersionInfo()
            ?: return ModDoctorRepairOutcome.Failed("The instance metadata could not be read safely.")
        val fingerprintPlatform = modDoctorFingerprintPlatform(AllSettings.searchModPlatform.getValue())
        return ModDoctorMultiModRepair(
            minecraftVersion = versionInfo.minecraftVersion,
            loader = versionInfo.loaderInfo?.loader ?: ModLoader.UNKNOWN,
            modsDir = modsDir,
            isPackOwned = policy::isPackOwned,
            installedIdentity = { fileName -> lookupInstalledMod(modsDir, fileName, fingerprintPlatform) },
            projectVersions = { projectId, platform -> loadProjectVersions(projectId, platform) },
            stageCandidate = { candidate -> stageModFile(candidate, version) },
            pendingDependencies = pendingDependencies,
        ).repair(fix)
    }

    /**
     * 把候选下载到既有暂存位并只交回文件本身（stage-only）：mods 目录在这里不会被改动，
     * 复制与校验由修复流程在整组计划通过后统一执行。
     *
     * 等待已经超时/取消后文件才交回时不再有人接管，就地清理，避免在暂存位留下垃圾。
     */
    private suspend fun stageModFile(
        platformVersion: PlatformVersion,
        version: Version,
    ): File? = suspendCancellableCoroutine { continuation ->
        var finished = false
        fun finish(staged: File?) {
            if (finished || !continuation.isActive) return
            finished = true
            continuation.resume(staged)
        }

        downloadSingleForVersions(
            version = platformVersion,
            versions = listOf(version),
            folder = VersionFolders.MOD.folderName,
            stageOnly = true,
            onFileStaged = { staged, _ ->
                if (finished || !continuation.isActive) runCatching { staged.delete() } else finish(staged)
            },
            onFileCancelled = { _, _ -> finish(null) },
            onFileRejected = { _, _ -> finish(null) },
            submitError = { finish(null) },
        )
    }

    /** 当前已启用模组的 Fabric 身份（Id + 版本），用于校验候选自带的 breaks 声明 */
    private suspend fun enabledModIdentities(modsDir: File): List<InstalledModIdentity> =
        readEnabledModIdentities(modsDir)

    /** breaks 修复没有安全结果：保留原文件，移除 Fix，并给界面追加本地化说明。 */
    private fun ModDoctorReport.markBreaksFixUnavailable(): ModDoctorReport = copy(
        problems = problems.map { problem ->
            if (problem.breaksFix != null) {
                problem.copy(
                    breaksFix = null,
                    reason = "${problem.reason} No safe automatic repair was found; the original files were restored.",
                    reasonSuffixes = problem.reasonSuffixes + ModDoctorReasonSuffix(
                        R.string.mod_doctor_reason_breaks_no_safe_repair
                    ),
                )
            } else problem
        }
    )

    /**
     * 已知兼容成套规则的修复：把每个与允许集合不一致的成员换成集合里的那个版本。
     *
     * 候选先经既有暂存管线落地，并核对它自己声明的 id 确是被替换成员；整组准备完毕后交给既有多模组
     * 执行器一次性应用（先全部禁用原文件，复制并逐个核验，任何失败都回滚并核对修复前哈希）。
     * 拿不到安全计划时一个字节也不改。
     */
    private suspend fun repairKnownSet(
        fix: ModDoctorSetFix,
        version: Version,
        modsDir: File,
        policy: ModpackPolicy,
    ): ModDoctorRepairOutcome {
        val versionInfo = version.getVersionInfo()
            ?: return ModDoctorRepairOutcome.Failed("The instance metadata could not be read safely.")
        val platform = modDoctorFingerprintPlatform(AllSettings.searchModPlatform.getValue())
        val staged = mutableListOf<File>()

        try {
            val replacements = mutableListOf<ModDoctorPlannedReplacement>()
            for (member in fix.members) {
                //整合包自带的文件不参与自动替换
                if (policy.isPackOwned(member.fileName)) return ModDoctorRepairOutcome.NoSafePlan

                //权威身份只能来自现有指纹匹配：匹配不到就宁可不换
                val identity = lookupInstalledMod(modsDir, member.fileName, platform)
                    ?: return ModDoctorRepairOutcome.NoSafePlan
                val candidates = loadProjectVersions(identity.projectId, identity.platform)
                    .filter { it.isCompatibleWith(version, PlatformClasses.MOD) }
                    .filter { it.platformVersion().matchesModVersion(member.targetVersion) }
                if (candidates.isEmpty()) return ModDoctorRepairOutcome.NoSafePlan

                //逐个候选暂存，再用暂存文件**自己声明**的 id 与版本确认它是目标版本：
                //平台版本号的写法与 jar 内部的写法并不一致，只有 jar 自己的声明是权威的。
                //不符的候选就地丢弃，继续试下一个（复用既有的候选步进器）。
                val choice = firstAcceptedCandidate(candidates) { candidate ->
                    val stagedFile = stageModFile(candidate, version)
                    if (stagedFile == null) {
                        ModDoctorCandidateAttempt.Failed("The replacement file could not be downloaded.")
                    } else {
                        staged += stagedFile
                        val facts = readFabricModFacts(stagedFile)
                        val declaresTarget = facts.id == member.modId &&
                                (facts.version ?: "").matchesModVersion(member.targetVersion)
                        if (declaresTarget) ModDoctorCandidateAttempt.Accepted(candidate to stagedFile)
                        else ModDoctorCandidateAttempt.Rejected(
                            "${candidate.platformFileName()} does not provide ${member.modId} ${member.targetVersion}"
                        )
                    }
                }
                choice.failure?.let { reason -> return ModDoctorRepairOutcome.Failed(reason) }
                val accepted = choice.accepted ?: return ModDoctorRepairOutcome.NoSafePlan

                replacements += ModDoctorPlannedReplacement(
                    fileName = member.fileName,
                    target = accepted.first,
                    stagedFile = accepted.second,
                    facts = readFabricModFacts(accepted.second),
                )
            }
            if (replacements.isEmpty()) return ModDoctorRepairOutcome.NoSafePlan

            //整组预检：先用暂存 jar 的事实 + 全部保留的 jar 检查「假设结果」，有新问题就在改动任何文件之前拒绝
            planResultRejection(readEnabledModIdentities(modsDir), replacements.map { it.fileName }, replacements.map { it.facts })?.let { reason ->
                Logger.info(TAG, "The known-set plan was refused before any file changed: $reason")
                return ModDoctorRepairOutcome.NoSafePlan
            }

            val executor = ModDoctorMultiModRepair(
                minecraftVersion = versionInfo.minecraftVersion,
                loader = versionInfo.loaderInfo?.loader ?: ModLoader.UNKNOWN,
                modsDir = modsDir,
                isPackOwned = policy::isPackOwned,
                installedIdentity = { fileName -> lookupInstalledMod(modsDir, fileName, platform) },
                projectVersions = { projectId, targetPlatform -> loadProjectVersions(projectId, targetPlatform) },
                stageCandidate = { candidate -> stageModFile(candidate, version) },
            )
            return try {
                executor.apply(ModDoctorRepairPlan(replacements))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Logger.warning(TAG, "The known-set repair failed", e)
                ModDoctorRepairOutcome.Failed("The replacement failed.")
            }
        } finally {
            staged.forEach { file -> runCatching { file.delete() } }
        }
    }

    /** 已知兼容成套规则没有安全结果：保留原文件，移除 Fix，并给界面追加本地化说明。 */
    private fun ModDoctorReport.markSetFixUnavailable(): ModDoctorReport = copy(
        problems = problems.map { problem ->
            if (problem.setFix != null || problem.ruleFallbackFix != null) {
                problem.copy(
                    setFix = null,
                    ruleFallbackFix = null,
                    reason = "${problem.reason} No safe automatic repair was found; the original files were restored.",
                    reasonSuffixes = problem.reasonSuffixes + ModDoctorReasonSuffix(
                        R.string.mod_doctor_reason_set_no_safe_repair
                    ),
                )
            } else problem
        }
    )

    private fun ModDoctorReport.markFixFailed(dependencyId: String): ModDoctorReport {
        return copy(
            problems = problems.map { problem ->
                if (problem.requiredDependencyId == dependencyId) {
                    problem.copy(
                        fixCandidate = null,
                        reason = "${problem.reason} Automatic repair could not complete.",
                        reasonSuffixes = problem.reasonSuffixes + ModDoctorReasonSuffix(R.string.mod_doctor_reason_repair_incomplete)
                    )
                } else problem
            }
        )
    }

    /** 所有候选都与已启用模组冲突：不安装任何东西，并把每个候选被拒的原因保留在问题描述里 */
    private fun ModDoctorReport.markFixUnavailable(
        dependencyId: String,
        rejections: List<String>,
    ): ModDoctorReport {
        val reason = if (rejections.isEmpty()) {
            "no compatible release could be installed."
        } else {
            "every compatible release conflicts with the installed mod set: ${rejections.joinToString("; ")}"
        }
        return copy(
            problems = problems.map { problem ->
                if (problem.requiredDependencyId == dependencyId) {
                    problem.copy(
                        fixCandidate = null,
                        reason = "${problem.reason} Automatic repair is unavailable: $reason",
                        reasonSuffixes = problem.reasonSuffixes + ModDoctorReasonSuffix(R.string.mod_doctor_reason_repair_unavailable, reason)
                    )
                } else problem
            }
        )
    }

    /** 库版本修复没有安全计划/没能完成：移除 Fix，保留原文件，给界面追加原因 */
    private fun ModDoctorReport.markLibraryFixUnavailable(fileName: String, reasonText: String, incomplete: Boolean): ModDoctorReport = copy(
        problems = problems.map { problem ->
            if (problem.dependencyVersionFix?.fileName == fileName) {
                problem.copy(
                    dependencyVersionFix = null,
                    reason = "${problem.reason} Automatic repair could not complete: $reasonText",
                    reasonSuffixes = problem.reasonSuffixes + if (incomplete) {
                        ModDoctorReasonSuffix(R.string.mod_doctor_reason_repair_incomplete)
                    } else {
                        ModDoctorReasonSuffix(R.string.mod_doctor_reason_repair_unavailable, reasonText)
                    }
                )
            } else problem
        }
    )

    private suspend fun repairLibraryVersion(
        fix: ModDoctorReplacementFix,
        version: Version,
        modsDir: File,
        policy: ModpackPolicy,
        pendingDependencies: List<Pair<String, Platform>>,
    ): ModDoctorRepairOutcome {
        val versionInfo = version.getVersionInfo()
            ?: return ModDoctorRepairOutcome.Failed("The instance metadata could not be read safely.")
        val fingerprintPlatform = modDoctorFingerprintPlatform(AllSettings.searchModPlatform.getValue())
        return ModDoctorMultiModRepair(
            minecraftVersion = versionInfo.minecraftVersion,
            loader = versionInfo.loaderInfo?.loader ?: ModLoader.UNKNOWN,
            modsDir = modsDir,
            isPackOwned = policy::isPackOwned,
            installedIdentity = { fileName -> lookupInstalledMod(modsDir, fileName, fingerprintPlatform) },
            projectVersions = { projectId, platform -> loadProjectVersions(projectId, platform) },
            stageCandidate = { candidate -> stageModFile(candidate, version) },
            bounds = LIBRARY_REPAIR_BOUNDS,
            pendingDependencies = pendingDependencies,
        ).repairLibrary(fix)
    }

    private fun ModDoctorReport.markReplacementFixFailed(fileName: String): ModDoctorReport {
        return copy(
            problems = problems.map { problem ->
                if (problem.replacementFix?.fileName == fileName) {
                    problem.copy(
                        replacementFix = null,
                        reason = "${problem.reason} Automatic repair could not complete.",
                        reasonSuffixes = problem.reasonSuffixes + ModDoctorReasonSuffix(R.string.mod_doctor_reason_repair_incomplete)
                    )
                } else problem
            }
        )
    }

    /** 本机学到的禁用没能执行：保留原文件，移除 Fix，并给界面追加本地化说明。 */
    private fun ModDoctorReport.markLocalDisableFixFailed(fileName: String): ModDoctorReport = copy(
        problems = problems.map { problem ->
            if (problem.localDisableFix?.fileName == fileName) {
                problem.copy(
                    localDisableFix = null,
                    reason = "${problem.reason} Automatic repair could not complete.",
                    reasonSuffixes = problem.reasonSuffixes + ModDoctorReasonSuffix(R.string.mod_doctor_reason_repair_incomplete),
                )
            } else problem
        }
    )

    //internal 而不是 private：单元测试要驱动同一个解析路径（真实 jar → fabric.mod.json → ModDoctorMetadata）
    internal fun toDoctorMetadata(local: LocalMod): ModDoctorMetadata = toDoctorMetadata(local, ModLoader.UNKNOWN)

    /**
     * 同一个解析路径，但知道实例用的是哪个加载器。
     *
     * 一个 jar 可以同时带多个加载器的元数据（LambDynamicLights、Puzzle 这类一体包同时带
     * `fabric.mod.json` 与 `META-INF/neoforge.mods.toml`）。这时必须读实例真正会用的那一份，
     * 否则会拿另一份加载器的声明误报「错误加载器」，并漏掉只有那份元数据才声明的内嵌依赖
     * （Fabric 的内嵌 jar 在 `META-INF/jars/`，由 fabric.mod.json 的 `jars` 登记）。
     */
    internal fun toDoctorMetadata(local: LocalMod, instanceLoader: ModLoader): ModDoctorMetadata {
        if (local.notMod) {
            return ModDoctorMetadata(
                fileName = local.file.name,
                modId = "",
                name = local.name,
                loader = ModLoader.UNKNOWN,
                readable = false,
            )
        }

        val effectiveLoader = metadataLoaderFor(local, instanceLoader)
        return when (effectiveLoader) {
            ModLoader.FABRIC -> readFabricMetadata(local, effectiveLoader)
            // NeoForge 的 mod 只发布 `META-INF/neoforge.mods.toml`（既无 `mods.toml` 也无 `mcmod.info`），
            // 与 Forge 走同一个 reader：否则它会以“无依赖”的样子通过检查，缺失的必需依赖永远不会被发现。
            ModLoader.FORGE, ModLoader.NEOFORGE -> readForgeMetadata(local)
            else -> ModDoctorMetadata(
                fileName = local.file.name,
                modId = local.id,
                name = local.name,
                loader = effectiveLoader,
            )
        }
    }

    /**
     * 这个文件的元数据该按哪个加载器读。
     *
     * jar 里声明了实例认得的加载器元数据时以实例为准（一体包）；一份都没有时沿用本地读取器的判断 ——
     * 那正是「错误加载器」该报的情形：文件只带了别的加载器的元数据。
     */
    private fun metadataLoaderFor(local: LocalMod, instanceLoader: ModLoader): ModLoader {
        val declared = declaredModLoaders(local.file)
        return if (declared.any { instanceLoader.acceptsModLoader(it) }) instanceLoader else local.loader
    }

    /** jar 里声明了哪些加载器的元数据 */
    private fun declaredModLoaders(file: File): List<ModLoader> = runCatching {
        ZipFile(file).use { zip ->
            buildList {
                if (zip.getEntry("fabric.mod.json") != null) add(ModLoader.FABRIC)
                if (zip.getEntry("META-INF/neoforge.mods.toml") != null) add(ModLoader.NEOFORGE)
                if (zip.getEntry("META-INF/mods.toml") != null) add(ModLoader.FORGE)
                if (zip.getEntry("mcmod.info") != null) add(ModLoader.FORGE)
            }
        }
    }.getOrDefault(emptyList())

    private fun readFabricMetadata(local: LocalMod, loader: ModLoader): ModDoctorMetadata {
        return try {
            val json = readJsonEntry(local.file, "fabric.mod.json")
            val depends = json.objectMember("depends")
            val required = depends
                ?.entrySet()
                ?.mapNotNull { (id, value) ->
                    if (id in ENVIRONMENT_DEPENDENCIES) null else id.takeIf { value.isJsonPrimitive || value.isJsonArray }
                }
                ?: emptyList()
            //谓词原样带走：已安装依赖存在时还要看它的版本是否落在声明里
            val requiredPredicates = depends
                ?.entrySet()
                ?.mapNotNull { (id, value) ->
                    if (id in ENVIRONMENT_DEPENDENCIES) null else id to value.asStringList()
                }
                ?.filter { (_, predicates) -> predicates.isNotEmpty() }
                ?.toMap()
                ?: emptyMap()
            val minecraftRanges = depends
                ?.get("minecraft")
                ?.asStringList()
                ?: emptyList()
            //`breaks` 与 `depends` 使用同一套 Fabric 版本谓词语法；原样带到统一校验阶段，
            //不在解析器里另写一套版本判断。
            val breakPredicates = json.objectMember("breaks")
                ?.entrySet()
                ?.mapNotNull { (id, value) -> id to value.asStringList() }
                ?.filter { (id, predicates) -> id.isNotBlank() && predicates.isNotEmpty() }
                ?.toMap()
                ?: emptyMap()
            //`conflicts` 是 Fabric 的旧键，语义与 `breaks` 相同（目标Id -> 版本谓词），
            //但加载器只把它当作启动警告，两个模组仍然会被加载。
            //这里只读出目标与谓词，交给统一校验阶段作为非阻塞提示上报。
            val conflictPredicates = json.objectMember("conflicts")
                ?.entrySet()
                ?.mapNotNull { (id, value) -> id to value.asStringList() }
                ?.filter { (id, predicates) -> id.isNotBlank() && predicates.isNotEmpty() }
                ?.toMap()
                ?: emptyMap()
            val embeddedMods = json.asJsonObject.get("jars")
                ?.asStringObjectList("file")
                ?.flatMap { entryName -> readNestedFabricMods(local.file, entryName) }
                ?: emptyList()
            //顶层 provides 声明的 id 是这个模组额外提供的别名 id，真正的加载器用它们解析依赖。
            //真实用例：某个模组用 provides 声明别名 id，其它模组依赖那个别名；
            //加载器能解析，而只认文件自身 id 的检查会误报缺失。
            //只登记 id（算作「已装」）；别名不生成版本，版本判定沿用「读不到就沉默」的保守规则。
            val declaredProvides = json.asJsonObject.get("provides")?.asStringList().orEmpty()
            ModDoctorMetadata(
                fileName = local.file.name,
                modId = json.asJsonObject.stringMember("id") ?: local.id,
                name = json.asJsonObject.stringMember("name") ?: local.name,
                loader = loader,
                version = json.asJsonObject.stringMember("version") ?: local.version,
                requiredDependencies = required,
                requiredDependencyPredicates = requiredPredicates,
                breakPredicates = breakPredicates,
                conflictPredicates = conflictPredicates,
                minecraftVersionRanges = minecraftRanges,
                providedModIds = (embeddedMods.map { (id, _) -> id } + declaredProvides).distinct(),
                //内嵌 jar 自己声明的版本属于那个 id，不是从外层文件借来的；读不出（占位符/空）就不登记，
                //版本判定因此保持沉默，而不是拿外层版本凭空造一个。
                //顶层 provides 的别名同样不登记版本：别名该不该继承提供者的版本，属于另一条产品规则，
                //这里只解决「有没有装」。
                providedModVersions = embeddedMods
                    .mapNotNull { (id, version) -> version?.takeIf { it.isNotBlank() }?.let { id to it } }
                    .toMap(),
            )
        } catch (_: Exception) {
            ModDoctorMetadata(
                fileName = local.file.name,
                modId = local.id,
                name = local.name,
                loader = loader,
                readable = false,
            )
        }
    }

    /** 现代 `META-INF/mods.toml` 优先，旧版 `mcmod.info` 保持回退兼容 */
    private fun readForgeMetadata(local: LocalMod): ModDoctorMetadata {
        return runCatching {
            val metadata = ForgeNewModMetadataReader.readDoctorMetadata(local.file)
            ModDoctorMetadata(
                fileName = local.file.name,
                modId = metadata.modId,
                name = metadata.name,
                loader = metadata.loader,
                version = metadata.version ?: local.version,
                requiredDependencies = metadata.requiredDependencies,
                requiredDependencyPredicates = metadata.requiredDependencyPredicates,
                minecraftVersionRanges = metadata.minecraftVersionRanges,
                providedModIds = metadata.providedModIds,
                providedModVersions = metadata.providedModVersions,
            )
        }.getOrElse {
            readForgeOldMetadata(local)
        }
    }

    private fun readForgeOldMetadata(local: LocalMod): ModDoctorMetadata {
        return try {
            val json = readJsonEntry(local.file, "mcmod.info")
            val entry = when {
                json.isJsonArray -> json.asJsonArray.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
                json.isJsonObject -> json.asJsonObject
                else -> null
            }
            ModDoctorMetadata(
                fileName = local.file.name,
                modId = entry?.stringMember("modid") ?: local.id,
                name = entry?.stringMember("name") ?: local.name,
                loader = local.loader,
                version = entry?.stringMember("version") ?: local.version,
                minecraftVersionRanges = entry?.stringMember("mcversion")?.let(::listOf) ?: emptyList(),
            )
        } catch (_: Exception) {
            ModDoctorMetadata(
                fileName = local.file.name,
                modId = local.id,
                name = local.name,
                loader = local.loader,
                readable = false,
            )
        }
    }

    private fun readJsonEntry(file: File, entryName: String): JsonElement {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry(entryName) ?: error("$entryName not found")
            return zip.getInputStream(entry).bufferedReader().use { reader ->
                JsonParser.parseReader(reader)
            }
        }
    }

    private fun JsonElement.objectMember(name: String): JsonObject? {
        return takeIf { it.isJsonObject }?.asJsonObject?.get(name)?.takeIf { it.isJsonObject }?.asJsonObject
    }

    private fun JsonObject.stringMember(name: String): String? {
        return get(name)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }
    }

    private fun JsonElement.asStringList(): List<String> {
        return when {
            isJsonArray -> asJsonArray.mapNotNull { it.takeIf { value -> value.isJsonPrimitive }?.asString }
            isJsonPrimitive -> listOf(asString)
            else -> emptyList()
        }.filter { it.isNotBlank() }
    }

    private fun JsonElement.asStringObjectList(key: String): List<String> {
        if (!isJsonArray) return emptyList()
        return asJsonArray.mapNotNull { value ->
            value.takeIf { it.isJsonObject }
                ?.asJsonObject
                ?.get(key)
                ?.takeIf { it.isJsonPrimitive }
                ?.asString
                ?.takeIf { it.isNotBlank() }
        }
    }

    /** 一个内嵌模块自己的 id 与版本；读不出来时按“没有这个模块”处理 */
    private fun readNestedFabricMods(file: File, entryName: String): List<Pair<String, String?>> =
        readEmbeddedModule(file, entryName)?.let { module -> listOf(module.id to module.version) } ?: emptyList()

    private companion object {
        val ENVIRONMENT_DEPENDENCIES = setOf("minecraft", "fabricloader", "java", "forge", "neoforge")
    }
}

/** 依赖项目解析结果：无候选时 [unavailableReason] 说明原因，避免把超时误报为“平台无此项目” */
internal data class ModDoctorFixResolution(
    val candidate: ModDoctorFixCandidate? = null,
    val replacementFix: ModDoctorReplacementFix? = null,
    val unavailableReason: String? = null,
    /** 版本修复的预览：预计会换成的版本名（仅供展示，真正安装前还要经过暂存校验） */
    val previewVersion: String? = null,
)

/**
 * 已装但版本不满足声明的依赖：只在“指纹匹配到的同一个权威项目”里找，候选必须匹配 Minecraft 版本与加载器，
 * 并同时满足所有已启用模组对这个库的版本约束。没有共同版本时返回明确原因（例如两个模组的范围互相排斥）。
 */
internal suspend fun resolveLibraryVersionFix(
    fileName: String,
    dependencyId: String,
    requirements: List<List<String>>,
    minecraftVersion: String,
    loader: ModLoader,
    installedMod: suspend (String) -> InstalledMod?,
    projectVersions: suspend (String, Platform) -> List<PlatformVersion>,
): ModDoctorFixResolution {
    val installed = try {
        installedMod(fileName)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        return ModDoctorFixResolution(
            unavailableReason = "the installed $dependencyId file could not be matched to a provider project (${e.failureLabel()})."
        )
    }
    if (installed == null || installed.notFound || installed.projectId.isBlank()) {
        return ModDoctorFixResolution(
            unavailableReason = "the installed $dependencyId file could not be matched to a provider project."
        )
    }
    val versions = try {
        projectVersions(installed.projectId, installed.platform).filter { it.platformProjectId() == installed.projectId }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        return ModDoctorFixResolution(
            unavailableReason = "the ${installed.platform.name} version lookup for ${installed.projectId} failed (${e.failureLabel()})."
        )
    }
    val preview = previewDependencyVersion(versions, minecraftVersion, loader, requirements)
        ?: return ModDoctorFixResolution(
            unavailableReason = "no build of ${installed.projectId} supports Minecraft $minecraftVersion with the " +
                    "${loader.displayName} loader and satisfies every installed mod that uses $dependencyId " +
                    "(required: ${requirements.joinToString("; ") { it.joinToString(" || ") }}); nothing was changed."
        )
    return ModDoctorFixResolution(
        replacementFix = ModDoctorReplacementFix(
            fileName = fileName, projectId = installed.projectId, platform = installed.platform, requirements = requirements,
        ),
        previewVersion = preview.platformVersion(),
    )
}

/**
 * 解析依赖项目所需的最小可替换接缝。默认参数保持现有生产行为，仅测试时替换，
 * 用于验证依赖项是按 Id/Slug 直接查询，而不是先走一遍平台搜索。
 *
 * 依赖项目与版本解析固定走 [modDoctorDependencyRepairPlatform]（恒为 Modrinth），与用户的
 * 搜索/下载平台偏好无关；[platform] 仍是该偏好进入本接缝的对照输入。
 */
internal suspend fun ModDoctorReport.withFixCandidates(
    minecraftVersion: String,
    loader: ModLoader,
    modsDir: File? = null,
    platform: Platform = AllSettings.searchModPlatform.getValue(),
    directProject: suspend (String, Platform) -> PlatformProject? = ::lookupProjectDirect,
    searchedProject: suspend (String, String, Platform) -> PlatformProject? = ::searchProjectBySlug,
    projectVersions: suspend (String, Platform) -> List<PlatformVersion> = ::loadProjectVersions,
    installedMod: suspend (String) -> InstalledMod? = { fileName -> lookupInstalledMod(modsDir, fileName, modDoctorFingerprintPlatform(platform)) },
    /** 依赖Id -> 已启用模组对它的版本约束（每个声明一组）；候选必须同时满足它们 */
    requiredDependencyRequirements: Map<String, List<List<String>>> = emptyMap(),
): ModDoctorReport {
    val dependencyIds = problems
        .filter { it.type == ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY }
        .mapNotNull { it.requiredDependencyId }
        .distinct()
    val replaceableFiles = problems
        .filter { it.type.isReplaceable() }
        .map { it.fileName }
        .filter { it.isNotBlank() }
        .distinct()
    val breaksProblems = problems.filter { it.type == ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR }
    val versionProblems = problems.filter { it.type == ModDoctorProblemType.INCOMPATIBLE_DEPENDENCY_VERSION }
    if (dependencyIds.isEmpty() && replaceableFiles.isEmpty() && breaksProblems.isEmpty() && versionProblems.isEmpty()) return this

    //已装但版本不对的库：每个库文件只解析一次；权威项目只来自指纹匹配
    val libraryResolutions = versionProblems
        .mapNotNull { p -> p.dependencyFileName?.takeIf { it.isNotBlank() }?.let { file -> p.dependencyModId?.let { id -> file to id } } }
        .distinct()
        .associateWith { (file, id) ->
            resolveLibraryVersionFix(
                fileName = file,
                dependencyId = id,
                requirements = requiredDependencyRequirements[id].orEmpty(),
                minecraftVersion = minecraftVersion,
                loader = loader,
                installedMod = installedMod,
                projectVersions = projectVersions,
            )
        }

    val resolutions = dependencyIds.associateWith { dependencyId ->
        resolveFixCandidate(
            dependencyId = dependencyId,
            minecraftVersion = minecraftVersion,
            loader = loader,
            platform = modDoctorDependencyRepairPlatform(platform),
            directProject = directProject,
            searchedProject = searchedProject,
            projectVersions = projectVersions,
            requirements = requiredDependencyRequirements[dependencyId].orEmpty(),
        )
    }
    val replacements = replaceableFiles.associateWith { fileName ->
        resolveReplacementFix(
            fileName = fileName,
            minecraftVersion = minecraftVersion,
            loader = loader,
            installedMod = installedMod,
            projectVersions = projectVersions,
        )
    }
    val breaksResolutions = breaksProblems.associateWith { problem ->
        val target = problem.breaksTargetFileName?.let { fileName ->
            resolveReplacementFix(
                fileName = fileName,
                minecraftVersion = minecraftVersion,
                loader = loader,
                installedMod = installedMod,
                projectVersions = projectVersions,
            ).replacementFix
        }
        val source = problem.breaksSourceFileName?.let { fileName ->
            resolveReplacementFix(
                fileName = fileName,
                minecraftVersion = minecraftVersion,
                loader = loader,
                installedMod = installedMod,
                projectVersions = projectVersions,
            ).replacementFix
        }
        target?.let { ModDoctorBreaksFix(target = it, source = source) }
    }
    return copy(
        problems = problems.map { problem ->
            when {
                problem.type == ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY -> {
                    val resolution = problem.requiredDependencyId?.let { resolutions[it] }
                    when {
                        resolution?.candidate != null -> problem.copy(fixCandidate = resolution.candidate)
                        else -> problem.copy(
                            reason = "${problem.reason} Automatic repair is unavailable: " +
                                    (resolution?.unavailableReason
                                        ?: "no authoritative compatible provider was resolved."),
                            reasonSuffixes = problem.reasonSuffixes + ModDoctorReasonSuffix(
                                R.string.mod_doctor_reason_repair_unavailable,
                                resolution?.unavailableReason
                                    ?: "no authoritative compatible provider was resolved."
                            )
                        )
                    }
                }
                problem.type == ModDoctorProblemType.INCOMPATIBLE_DEPENDENCY_VERSION -> {
                    val key = problem.dependencyFileName?.takeIf { it.isNotBlank() }?.let { f -> problem.dependencyModId?.let { f to it } }
                    val resolution = key?.let { libraryResolutions[it] }
                    val ruleNote = problem.ruleFallbackFix?.let { rf -> rf.members.joinToString(", ") { m -> "${m.modId} → ${m.targetVersion}" } }
                    if (ruleNote != null) {
                        //已发布的已知兼容规则优先：只下载规则点名的那一个版本，不先去下载库的新版本再试错；
                        //库升级方案仅作为规则无法执行时的后备保留
                        problem.copy(
                            dependencyVersionFix = resolution?.replacementFix,
                            reasonSuffixes = problem.reasonSuffixes + ModDoctorReasonSuffix(R.string.mod_doctor_reason_rule_fallback_preview, ruleNote)
                        )
                    } else if (resolution?.replacementFix != null) {
                        problem.copy(
                            dependencyVersionFix = resolution.replacementFix,
                            reasonSuffixes = problem.reasonSuffixes + ModDoctorReasonSuffix(
                                R.string.mod_doctor_reason_dependency_fix_preview,
                                "${resolution.replacementFix.fileName} → ${resolution.previewVersion}"
                            )
                        )
                    } else {
                        val why = resolution?.unavailableReason
                            ?: "the dependency is not a top-level installed mod, so it cannot be replaced safely."
                        problem.copy(
                            reason = "${problem.reason} Automatic repair is unavailable: $why",
                            reasonSuffixes = problem.reasonSuffixes + ModDoctorReasonSuffix(R.string.mod_doctor_reason_repair_unavailable, why)
                        )
                    }
                }
                problem.type == ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR -> {
                    val fix = breaksResolutions[problem]
                    val pairRuleNote = problem.ruleFallbackFix?.let { rf -> rf.members.joinToString(", ") { m -> "${m.modId} → ${m.targetVersion}" } }
                    if (pairRuleNote != null) {
                        problem.copy(
                            breaksFix = fix,
                            reasonSuffixes = problem.reasonSuffixes + ModDoctorReasonSuffix(R.string.mod_doctor_reason_rule_fallback_preview, pairRuleNote)
                        )
                    } else if (fix != null) {
                        problem.copy(breaksFix = fix)
                    } else {
                        problem.copy(
                            reason = "${problem.reason} Automatic repair is unavailable: " +
                                    "the conflicting target could not be matched to a compatible provider project.",
                            reasonSuffixes = problem.reasonSuffixes + ModDoctorReasonSuffix(
                                R.string.mod_doctor_reason_repair_unavailable,
                                "the conflicting target could not be matched to a compatible provider project."
                            )
                        )
                    }
                }
                problem.type.isReplaceable() -> {
                    val resolution = replacements[problem.fileName]
                    when {
                        resolution?.replacementFix != null ->
                            problem.copy(replacementFix = resolution.replacementFix)
                        else -> problem.copy(
                            reason = "${problem.reason} Automatic repair is unavailable: " +
                                    (resolution?.unavailableReason
                                        ?: "no authoritative compatible provider was resolved."),
                            reasonSuffixes = problem.reasonSuffixes + ModDoctorReasonSuffix(
                                R.string.mod_doctor_reason_repair_unavailable,
                                resolution?.unavailableReason
                                    ?: "no authoritative compatible provider was resolved."
                            )
                        )
                    }
                }
                else -> problem
            }
        }
    )
}

/**
 * 解析同项目安全替换候选：本地文件先经现有指纹匹配取得权威项目身份，
 * 再只在该项目内挑选与当前 Minecraft 版本 + 加载器兼容的构建；跨项目或无法权威映射时保持不可修复
 *
 * 由“错误加载器”与“Minecraft 版本不兼容”两类问题共用
 */
private suspend fun resolveReplacementFix(
    fileName: String,
    minecraftVersion: String,
    loader: ModLoader,
    installedMod: suspend (String) -> InstalledMod?,
    projectVersions: suspend (String, Platform) -> List<PlatformVersion>,
): ModDoctorFixResolution {
    val installed = try {
        installedMod(fileName)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        return ModDoctorFixResolution(
            unavailableReason = "the installed file could not be matched to a provider project " +
                    "(${e.failureLabel()})."
        )
    }
    if (installed == null || installed.notFound || installed.projectId.isBlank()) {
        return ModDoctorFixResolution(
            unavailableReason = "the installed file could not be matched to a provider project."
        )
    }

    val platform = installed.platform
    val replacement = try {
        projectVersions(installed.projectId, platform)
            .firstOrNull { it.isCompatibleWithVersion(minecraftVersion, loader) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        return ModDoctorFixResolution(
            unavailableReason = "the ${platform.name} version lookup for ${installed.projectId} " +
                    "failed (${e.failureLabel()})."
        )
    } ?: return ModDoctorFixResolution(
        unavailableReason = "no build of ${installed.projectId} supports Minecraft $minecraftVersion " +
                "with the ${loader.displayName} loader on ${platform.name}."
    )

    if (replacement.platformProjectId() != installed.projectId) {
        return ModDoctorFixResolution(
            unavailableReason = "the resolved ${platform.name} version does not belong to ${installed.projectId}."
        )
    }

    return ModDoctorFixResolution(
        replacementFix = ModDoctorReplacementFix(
            fileName = fileName,
            projectId = installed.projectId,
            platform = platform,
        )
    )
}

/**
 * 解析依赖的修复候选；失败原因被保留，超时不再被折叠成“平台无此项目”
 */
/**
 * ตัวอย่างเวอร์ชันของ dependency ใน Preview ใช้กติกาเดียวกับ Fix จริง:
 * เฉพาะที่รองรับ Minecraft/loader แล้วเรียง stable ก่อน (ตัวแรกคือตัวที่ Fix จะลองก่อน)
 * Fix ยังตรวจ breaks ตอน staging และอาจข้ามตัวนี้ไปตัวถัดไปได้ จึงเป็นเพียงตัวเลือกเบื้องต้น
 */
internal fun previewDependencyVersion(
    versions: List<PlatformVersion>,
    minecraftVersion: String,
    loader: ModLoader,
    requirements: List<List<String>> = emptyList(),
): PlatformVersion? = stableFirstCandidates(
    versions.filter {
        it.isCompatibleWithVersion(minecraftVersion, loader) &&
            platformVersionSatisfies(it, requirements)
    }
) { it.platformReleaseType() != PlatformReleaseType.RELEASE }.firstOrNull()

/**
 * 候选版本名是否满足所有已启用模组对它的版本约束。
 *
 * 读不出版本名、或求值器判定不了的一律不满足：这里宁可少给一个候选，也不猜一个会被加载器拒绝的版本。
 * 没有任何约束时恒为真。
 */
internal fun dependencyVersionSatisfies(versionName: String, requirements: List<List<String>>): Boolean =
    requirements.all { predicates -> versionPredicateDecision(predicates, versionName) == true }

/**
 * 平台上的版本名（例如 Modrinth 的 `mc1.21.11-0.8.14-fabric`）常常不是 jar 自己声明的版本（`0.8.14+mc1.21.11`），
 * 而依赖范围写的是后者（例如 Reese's Sodium Options 2.2.4 要求 `sodium =0.8.14+mc1.21.11`）。
 * 所以同时用平台版本名和文件名里的版本片段比对，任何一个能判定满足就算满足；片段只接受严格的“数字.数字…(+构建)”写法。
 */
internal fun comparableVersionNames(version: PlatformVersion): List<String> =
    listOfNotNull(version.platformVersion(), fileNameVersionToken(version.platformFileName())).distinct()

private val FILE_VERSION_TOKEN = Regex("""(?:^|[-_ ])(\d+(?:\.\d+)+(?:\+[A-Za-z0-9._-]+)?)\.jar$""", RegexOption.IGNORE_CASE)

internal fun fileNameVersionToken(fileName: String): String? = FILE_VERSION_TOKEN.find(fileName)?.groupValues?.get(1)

internal fun platformVersionSatisfies(version: PlatformVersion, requirements: List<List<String>>): Boolean =
    requirements.isEmpty() || comparableVersionNames(version).any { dependencyVersionSatisfies(it, requirements) }

/**
 * ผู้สมัคร [candidateName] “ยืนยันได้ว่าใหม่กว่า” เวอร์ชันที่ติดตั้ง [installedVersion] หรือไม่
 * ไม่รู้เวอร์ชันของผู้สมัครหรือของที่ติดตั้ง → false (ไม่เดาว่าเป็นการอัปเดต)
 */
internal fun isNewerBuildThanInstalled(candidateName: String, installedVersion: String?, minecraftVersion: String): Boolean {
    val installed = installedVersion?.baseModVersion()?.takeIf { it.isNotBlank() } ?: return false
    val candidate = candidateJarVersion(candidateName, minecraftVersion) ?: return false
    return compareVersionText(candidate, installed) > 0
}

private suspend fun resolveFixCandidate(
    dependencyId: String,
    minecraftVersion: String,
    loader: ModLoader,
    platform: Platform,
    directProject: suspend (String, Platform) -> PlatformProject?,
    searchedProject: suspend (String, String, Platform) -> PlatformProject?,
    projectVersions: suspend (String, Platform) -> List<PlatformVersion>,
    requirements: List<List<String>> = emptyList(),
): ModDoctorFixResolution {
    // 依赖项只提供模组 Id：先按 Id/Slug 直接查询平台项目，与依赖安装路径一致；
    // 只有当平台明确回答“无此项目”（例如 CurseForge 只接受数字 Id）时，才回退到搜索
    val project = try {
        directProject(dependencyId, platform)
            ?: searchedProject(dependencyId, minecraftVersion, platform)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        return ModDoctorFixResolution(
            unavailableReason = "the ${platform.name} provider lookup failed (${e.failureLabel()})."
        )
    } ?: return ModDoctorFixResolution(
        unavailableReason = "the dependency project $dependencyId was not found on ${platform.name}."
    )

    if (project.platformSlug() != dependencyId) {
        return ModDoctorFixResolution(
            unavailableReason = "the ${platform.name} project ${project.platformId()} does not match $dependencyId."
        )
    }

    val compatible = try {
        previewDependencyVersion(projectVersions(project.platformId(), platform), minecraftVersion, loader, requirements)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        return ModDoctorFixResolution(
            unavailableReason = "the ${platform.name} version lookup for ${project.platformTitle()} " +
                    "failed (${e.failureLabel()})."
        )
    } ?: return ModDoctorFixResolution(
        unavailableReason = "no ${project.platformTitle()} release supports Minecraft $minecraftVersion " +
                "with the ${loader.displayName} loader" +
                (if (requirements.isEmpty()) "" else " within the required range ${requirements.joinToString(" and ") { it.joinToString(" ") }}") +
                "."
    )

    if (compatible.platformProjectId() != project.platformId()) {
        return ModDoctorFixResolution(
            unavailableReason = "the resolved ${platform.name} version does not belong to ${project.platformId()}."
        )
    }

    return ModDoctorFixResolution(
        candidate = ModDoctorFixCandidate(
            dependencyId = dependencyId,
            projectId = project.platformId(),
            platform = platform,
            versionName = compatible.platformVersion(),
            requirements = requirements,
        )
    )
}

/** 按 Id/Slug 直接查询平台项目；平台明确回答“无此项目”时返回null */
private suspend fun lookupProjectDirect(projectIdOrSlug: String, platform: Platform): PlatformProject? =
    try {
        getProjectByVersion(projectIdOrSlug, platform, printLog = false)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        if (e.isProjectMissingOnProvider()) null else throw e
    }

/** 平台不支持按 Id/Slug 直接查询时的回退搜索（例如 CurseForge 只接受数字Id） */
private suspend fun searchProjectBySlug(
    dependencyId: String,
    minecraftVersion: String,
    platform: Platform,
): PlatformProject? {
    var searchResult: PlatformSearchResult? = null
    searchAssets(
        searchPlatform = platform,
        searchFilter = PlatformSearchFilter(
            searchName = dependencyId,
            gameVersion = minecraftVersion,
        ),
        platformClasses = PlatformClasses.MOD,
        onSuccess = { searchResult = it },
        onError = { }
    )
    return searchResult
        ?.getAssetsPage(PlatformClasses.MOD)
        ?.data
        ?.mapNotNull { (data, _) ->
            runCatching {
                getProjectByVersion(data.platformId(), platform, printLog = false)
            }.getOrNull()
        }
        ?.filter { it.platformSlug() == dependencyId }
        ?.distinctBy { it.platformId() }
        ?.singleOrNull()
}

/** 拉取依赖项目在平台上的全部版本（Modrinth 侧已改为分页请求，避免单次超大响应触发超时） */
private suspend fun loadProjectVersions(
    projectId: String,
    platform: Platform,
): List<PlatformVersion> = getVersions(projectId, platform).initAll(projectId)

/**
 * 用现有指纹匹配取得本地模组文件在平台上的权威身份；
 * 无法匹配（文件不存在 / 平台未命中 / 匹配不完整）时返回null，调用方必须保持不可修复
 */
private suspend fun lookupInstalledMod(
    modsDir: File?,
    fileName: String,
    platform: Platform,
): InstalledMod? {
    val file = modsDir?.let { dir -> File(dir, fileName) } ?: return null
    if (!file.isFile) return null
    return try {
        val matched = matchInstalledMods(listOf(computeModFingerprints(file)), platform)
        matched.byProject.values.firstOrNull { it.projectId.isNotBlank() && !it.notFound }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        null
    }
}

/** 失败原因摘要，用于区分平台超时、查询失败与项目确实不存在 */
private fun Throwable.failureLabel(): String = when {
    isTimeoutFailure() -> "timeout"
    isProjectMissingOnProvider() -> "not found"
    else -> message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
}

private fun Throwable.isTimeoutFailure(): Boolean =
    generateSequence(this as Throwable?) { it.cause }
        .any { it is InterruptedIOException || it is TimeoutException }

/** 平台是否明确回答了“该项目不存在” */
private fun Throwable.isProjectMissingOnProvider(): Boolean =
    (this is ClientRequestException && response.status.value == 404) ||
            this is FileNotFoundException ||
            this is NotFoundException

internal fun com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion.isCompatibleWithVersion(
    minecraftVersion: String,
    loader: ModLoader,
): Boolean {
    if (minecraftVersion !in platformGameVersion()) return false
    if (loader == ModLoader.UNKNOWN) return true
    val supportedLoaders = platformLoaders()
    return supportedLoaders.isEmpty() || supportedLoaders.any {
        it.getDisplayName().equals(loader.displayName, ignoreCase = true) ||
                // Quilt 实现了 Fabric 的模组模型，所以标注 Fabric 的构建同样是 Quilt 实例的合法候选，
                // 与 ModLoader.acceptsModLoader 保持同一套规则。
                (loader == ModLoader.QUILT &&
                        it.getDisplayName().equals(ModLoader.FABRIC.displayName, ignoreCase = true))
    }
}

/** The one shared, side-effect-free validation pass used by ModDoctor. */
internal fun validateModDoctorMetadata(
    mods: List<ModDoctorMetadata>,
    minecraftVersion: String,
    loader: ModLoader,
    providerGameVersions: Map<String, List<String>> = emptyMap(),
    setRules: List<ModDoctorSetRule> = emptyList(),
): ModDoctorReport {
    val problems = mutableListOf<ModDoctorProblem>()
    //只报告、不阻塞启动的提示（Fabric 旧键 `conflicts`）：单独累加，绝不进入 problems，
    //因此 isClean、hasFixableProblems 与启动门判定都不受它影响。
    val advisories = mutableListOf<ModDoctorProblem>()
    val readable = mods.filter { it.readable }
    val installedIds = readable
        .flatMap { mod -> listOf(mod.modId) + mod.providedModIds }
        .filter { it.isNotBlank() }
        .toSet()
    //只有模组自身的 id 带得上版本：[ModDoctorMetadata.providedModIds] 是内嵌 jar 的 id，
    //版本与外层文件无关，借用外层版本会凭空造出一个版本。内嵌 jar 自己声明的版本登记在
    //[ModDoctorMetadata.providedModVersions] 里，是唯一能用于版本判定的来源。
    val installedVersions = mutableMapOf<String, String?>()
    //顶层已装模组的 id -> 文件名：版本修复只能换“自己的 id 就是这个依赖”的文件
    val installedFiles = mutableMapOf<String, String>()
    readable.forEach { mod ->
        val id = mod.modId
        if (id.isNotBlank() && id !in installedVersions) {
            installedVersions[id] = mod.version?.takeIf { it.isNotBlank() }
            installedFiles[id] = mod.fileName
        }
    }
    //内嵌副本的版本也要登记（已由上一步登记的条目优先，外层文件仍是该 id 的权威版本）：
    //真实用例：某个模组内嵌的依赖副本会因这份登记而让缺失依赖规则沉默，
    //能看出问题的是版本规则；不登记副本版本会让整条版本检查沉默，把加载器拒绝的集合报成干净
    readable.forEach { mod ->
        mod.providedModVersions.forEach { (id, version) ->
            if (id.isNotBlank() && id !in installedVersions) installedVersions[id] = version
        }
    }

    mods.filterNot { it.readable }.forEach { mod ->
        problems += ModDoctorProblem(
            type = ModDoctorProblemType.CORRUPT_METADATA,
            fileName = mod.fileName,
            modId = mod.modId.takeIf { it.isNotBlank() },
            reason = "${mod.name} does not contain readable supported mod metadata.",
            reasonRes = R.string.mod_doctor_reason_corrupt_metadata,
            reasonArgs = listOf(mod.name),
        )
    }

    readable.filter { loader != ModLoader.UNKNOWN && !loader.acceptsModLoader(it.loader) }.forEach { mod ->
        problems += ModDoctorProblem(
            type = ModDoctorProblemType.WRONG_LOADER,
            fileName = mod.fileName,
            modId = mod.modId,
            reason = "${mod.name} declares ${mod.loader.displayName}, but this instance uses ${loader.displayName}.",
            reasonRes = R.string.mod_doctor_reason_wrong_loader,
            reasonArgs = listOf(mod.name, mod.loader.displayName, loader.displayName),
        )
    }

    readable.forEach { mod ->
        val providerVersions = providerGameVersions[mod.fileName].orEmpty()
        //本地已精确证明兼容（带上界的声明）时不允许被 provider 推翻；
        //"*" 或 ">=1.21.10" 这类开放声明不能证明具体发布版本兼容，才由权威 provider 发布数据补充
        val providerIncompatible = !mod.provesReleaseCompatibility(minecraftVersion) &&
                providerVersions.isNotEmpty() &&
                minecraftVersion !in providerVersions

        when {
            mod.isLocallyIncompatible(minecraftVersion) -> problems += ModDoctorProblem(
                type = ModDoctorProblemType.WRONG_MINECRAFT_VERSION,
                fileName = mod.fileName,
                modId = mod.modId,
                reason = "${mod.name} declares Minecraft ${mod.minecraftVersionRanges.joinToString(" or ")}, not $minecraftVersion.",
                reasonRes = R.string.mod_doctor_reason_wrong_minecraft_version_local,
                reasonArgs = listOf(mod.name, mod.minecraftVersionRanges.joinToString(" or "), minecraftVersion),
            )
            providerIncompatible -> problems += ModDoctorProblem(
                type = ModDoctorProblemType.WRONG_MINECRAFT_VERSION,
                fileName = mod.fileName,
                modId = mod.modId,
                reason = "${mod.name} is published for Minecraft ${providerVersions.joinToString(" or ")}, not $minecraftVersion.",
                reasonRes = R.string.mod_doctor_reason_wrong_minecraft_version_provider,
                reasonArgs = listOf(mod.name, providerVersions.joinToString(" or "), minecraftVersion),
            )
        }
    }

    readable.groupBy { it.modId }
        .filterKeys { it.isNotBlank() }
        .filterValues { it.size > 1 }
        .forEach { (id, duplicates) ->
            problems += ModDoctorProblem(
                type = ModDoctorProblemType.DUPLICATE,
                fileName = duplicates.joinToString(", ") { it.fileName },
                modId = id,
                reason = "${duplicates.first().name} is installed more than once: ${duplicates.joinToString { it.fileName }}.",
                reasonRes = R.string.mod_doctor_reason_duplicate,
                reasonArgs = listOf(duplicates.first().name, duplicates.joinToString { it.fileName }),
            )
        }

    readable.forEach { mod ->
        mod.requiredDependencies
            .filter { it !in installedIds }
            .distinct()
            .forEach { dependency ->
                problems += ModDoctorProblem(
                    type = ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY,
                    fileName = mod.fileName,
                    modId = mod.modId,
                    requiredDependencyId = dependency,
                    reason = "${mod.name} requires $dependency, but no enabled installation was found.",
                    reasonRes = R.string.mod_doctor_reason_missing_required_dependency,
                    reasonArgs = listOf(mod.name, dependency),
                )
            }
    }

    //已安装依赖在，但它自己的版本不满足声明时单独上报。
    //这一条只报告，不携带 requiredDependencyId：换掉一个已被多方共享的依赖需要单独的产品决策，
    //带上那个字段会点亮现有的缺失依赖安装流程，变成一个半成品的自动替换。
    readable.forEach { mod ->
        mod.requiredDependencies
            .filter { it in installedIds }
            .distinct()
            .forEach { dependency ->
                val predicates = mod.requiredDependencyPredicates[dependency]?.takeIf { it.isNotEmpty() }
                    ?: return@forEach
                val installedVersion = installedVersions[dependency] ?: return@forEach
                if (installedDependencyVerdict(predicates, installedVersion) != false) return@forEach
                problems += ModDoctorProblem(
                    type = ModDoctorProblemType.INCOMPATIBLE_DEPENDENCY_VERSION,
                    fileName = mod.fileName,
                    modId = mod.modId,
                    reason = "${mod.name} requires $dependency ${predicates.joinToString(" ")}, " +
                            "but the installed version is $installedVersion.",
                    reasonRes = R.string.mod_doctor_reason_incompatible_dependency_version,
                    reasonArgs = listOf(mod.name, dependency, predicates.joinToString(" "), installedVersion),
                    dependencyFileName = installedFiles[dependency],
                    dependencyModId = dependency,
                )
            }
    }

    //已启用模组之间的 Fabric `breaks`：源模组声明不兼容，目标模组确实已安装，
    //且目标版本命中同一个共享谓词求值器时，报告一个阻塞问题。
    //未知版本或读不懂谓词仍保持沉默，把最后裁决权留给真正的加载器。
    //这一条只报告，不携带任何 Fix 字段，也不会删除、替换或挑选版本。
    readable.forEach { source ->
        source.breakPredicates.forEach { (targetId, predicates) ->
            val target = readable.firstOrNull { it.modId == targetId }
                ?: return@forEach
            val sourceVersion = source.version?.takeIf { it.isNotBlank() }
                ?: return@forEach
            val targetVersion = target.version?.takeIf { it.isNotBlank() }
                ?: return@forEach
            if (versionPredicateDecision(predicates, targetVersion) != true) return@forEach

            problems += ModDoctorProblem(
                type = ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR,
                fileName = "${source.fileName}, ${target.fileName}",
                modId = source.modId,
                reason = "${source.name} $sourceVersion cannot be used with ${target.name} $targetVersion.",
                reasonRes = R.string.mod_doctor_reason_incompatible_mod_pair,
                reasonArgs = listOf(source.name, sourceVersion, target.name, targetVersion),
                breaksSourceFileName = source.fileName,
                breaksTargetFileName = target.fileName,
                breaksTargetModId = target.modId,
            )
        }
    }

    //已启用模组之间的 Fabric 旧键 `conflicts`：声明与谓词求值器都与 `breaks` 共用一套，
    //但加载器把它报成启动警告并照常加载两个模组，所以这里只累加非阻塞提示，不进入 problems。
    //这一条只报告，不携带任何 Fix 字段，也不会删除、替换或挑选版本。
    readable.forEach { source ->
        source.conflictPredicates.forEach { (targetId, predicates) ->
            val target = readable.firstOrNull { it.modId == targetId }
                ?: return@forEach
            val sourceVersion = source.version?.takeIf { it.isNotBlank() }
                ?: return@forEach
            val targetVersion = target.version?.takeIf { it.isNotBlank() }
                ?: return@forEach
            if (versionPredicateDecision(predicates, targetVersion) != true) return@forEach

            advisories += ModDoctorProblem(
                type = ModDoctorProblemType.DECLARED_CONFLICT,
                fileName = "${source.fileName}, ${target.fileName}",
                modId = source.modId,
                reason = "${source.name} $sourceVersion declares a conflict with ${target.name} $targetVersion. " +
                        "The loader reports a declared conflict as a startup warning and still loads both mods, " +
                        "so the launch is not blocked.",
                reasonRes = R.string.mod_doctor_reason_declared_conflict,
                reasonArgs = listOf(source.name, sourceVersion, target.name, targetVersion),
            )
        }
    }

    //已知兼容成套规则：只点名“同时存在”才生效的模组集合，与模组自身的 depends/breaks 互不替代。
    //放在最后：它描述的是整组之间的关系，需要先知道每个文件自己读出了什么。
    problems += evaluateSetRules(readable, minecraftVersion, loader, setRules)

    //真实存在的版本不对问题，若涉及的模组在已发布规则里，挂上规则后备修复（没有问题就什么都不挂）
    return ModDoctorReport(attachRuleFallbacks(problems, readable, minecraftVersion, loader, setRules), advisories)
}

/**
 * 版本谓词判定：true 命中 / false 明确不命中 / null 求值器看不懂这段声明（保持“无法定论”）
 *
 * `||` 是可选分支（fabric-loader 的 VersionPredicate 写法，例如 `0.5.9 || 0.5.11`），
 * 由共享切分器 [predicateAlternatives] 拆开，分支内部交给共享求值器 [fabricVersionPredicateMatches]；
 * Minecraft 声明与已安装依赖声明共用这一条路径，因此同一个约束不会在两处得到不同答案。
 */
internal fun versionPredicateDecision(predicates: List<String>, target: String): Boolean? {
    val alternatives = predicates.flatMap(::predicateAlternatives).map(::expandVersionShorthand)
    if (alternatives.isEmpty()) return null
    val readable = alternatives.filter { it == "*" || isComprehensibleVersionPredicate(it) }
    if (fabricVersionPredicateMatches(target, readable)) return true
    //命中不了：只有“还有看不懂的分支”才无法定论，否则就是明确的不命中
    return if (readable.size != alternatives.size) null else false
}

private val VERSION_SHORTHAND = Regex("""([~^])\s*(\d+)(?:\.(\d+))?(?:\.(\d+))?(?![\w.+-])""")

/**
 * Fabric 的 `~` / `^` 简写展开为等价的比较范围，让它和其他写法走同一个求值器（真实例子：YACL 声明 `minecraft: ~1.21.11`）。
 * `~a.b.c` = `>=a.b.c <a.(b+1).0`；`^a.b.c` 在 a>0 时 = `>=a.b.c <(a+1).0.0`，0.x 版本按 semver 收紧。
 * 带预发布/构建元数据等读不懂的写法原样保留，仍然是“无法定论”。
 */
internal fun expandVersionShorthand(predicate: String): String = VERSION_SHORTHAND.replace(predicate) { m ->
    val op = m.groupValues[1]
    val a = m.groupValues[2].toLongOrNull() ?: return@replace m.value
    val b = m.groupValues[3].takeIf { it.isNotEmpty() }?.toLongOrNull()
    val c = m.groupValues[4].takeIf { it.isNotEmpty() }?.toLongOrNull()
    val lower = listOfNotNull(a, b, c).joinToString(".")
    val upper = if (op == "~") {
        if (b == null) "${a + 1}" else "$a.${b + 1}.0"
    } else when {
        a > 0 -> "${a + 1}.0.0"
        b == null -> "1.0.0"
        b > 0 -> "0.${b + 1}.0"
        c == null -> "0.1.0"
        else -> "0.0.${c + 1}"
    }
    ">=$lower <$upper"
}

/**
 * 已安装依赖的版本判定：true 满足 / false 不满足 / null 无法定论（保持沉默，不制造误报）
 *
 * 版本读不出来、写法不像版本（例如 `${file.jarVersion}` 这种占位符），或谓词读不懂，都返回 null。
 */
internal fun installedDependencyVerdict(predicates: List<String>, installedVersion: String?): Boolean? {
    val version = installedVersion?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (!version.first().isDigit()) return null
    return versionPredicateDecision(predicates, version)
}

/**
 * 求值器能否完整理解这段声明：裸版本（含 `x` 通配段）或全部由比较运算符组成的合取式
 *
 * 版本 token 的字符集必须覆盖共享求值器 [fabricVersionMatches] 真正会处理的写法，否则一类完全可判定的
 * 声明会被当成「看不懂」，判定退化成 null（无法定论）：漏掉 `+` 时，任何带构建元数据的谓词
 * （`=<版本>+<构建元数据>`、`>=<版本>+<构建元数据>`）都不可判定，于是「依赖不再被满足」的已启用模组
 * 既不会被展开，整组校验也不会拒绝只替换一个文件的计划。`+` 之后的构建元数据在比较时被忽略
 * （与 Fabric 自己的语义一致），不影响判定结果。
 */
private fun isComprehensibleVersionPredicate(predicate: String): Boolean {
    if (predicate.matches(Regex(VERSION_TOKEN_PATTERN))) return true
    val constraints = Regex("([<>]=?|=)\\s*($VERSION_TOKEN_PATTERN)").findAll(predicate).toList()
    if (constraints.isEmpty()) return false
    val normalized = predicate.replace("\\s+".toRegex(), "")
    return constraints.joinToString("") { it.value.replace("\\s+".toRegex(), "") } == normalized
}

/** 版本 token 的合法写法：字母数字、`.`、`_`、`-`（预发布段）与 `+`（构建元数据，比较时忽略） */
private const val VERSION_TOKEN_PATTERN = "[0-9][A-Za-z0-9._+-]*"


/**
 * Mod Doctor 权威已安装文件指纹的来源平台。
 *
 * 健康检查与用户的搜索/下载平台偏好解耦：CurseForge 的指纹接口需要 API Key，
 * 若跟随用户偏好（默认 CURSEFORGE），权威判定会在默认配置下永久停留在 UNKNOWN，
 * 从而漏报 provider 已确认的不兼容。参数 [userSearchPlatform] 只作为对照输入，
 * 明确表达该偏好不影响本健康检查。
 */
internal fun modDoctorFingerprintPlatform(userSearchPlatform: Platform): Platform = Platform.MODRINTH

/**
 * Mod Doctor 修复路径上「缺失依赖」项目与版本解析的来源平台。
 *
 * 与用户的搜索/下载平台偏好解耦：CurseForge 的项目接口需要 API Key，若跟随用户偏好
 * （默认 CURSEFORGE），默认配置下的依赖修复会在解析阶段就不可用。参数
 * [userSearchPlatform] 只作为对照输入，明确表达该偏好不影响依赖修复。
 */
internal fun modDoctorDependencyRepairPlatform(userSearchPlatform: Platform): Platform = Platform.MODRINTH

/**
 * 本地元数据无法定论、或只有开放声明（例如 ">=1.21.10"）时，
 * 用权威指纹匹配到的 provider 文件声明的游戏版本补充发布级兼容性判断。
 *
 * 匹配不到、离线或 provider 报错都返回“未知”，绝不把网络失败变成阻塞性不兼容。
 *
 * @param lookup 依赖注入的权威安装文件查询；生产实现复用现有指纹匹配（[lookupInstalledMod]）
 */
internal suspend fun providerGameVersionsForAmbiguousMods(
    mods: List<ModDoctorMetadata>,
    minecraftVersion: String,
    lookup: suspend (String) -> InstalledMod?,
): Map<String, List<String>> {
    //只查询本地无法精确证明发布级兼容的模组：明确不兼容的已由本地规则上报，带上界的声明已能定论
    val candidates = mods.filter { mod ->
        mod.readable &&
                !mod.isLocallyIncompatible(minecraftVersion) &&
                !mod.provesReleaseCompatibility(minecraftVersion)
    }
    if (candidates.isEmpty()) return emptyMap()

    // ponytail: 逐个文件调用现有指纹匹配（带持久缓存）；出现批量未命中导致请求过多时，
    // 再考虑一次性批量匹配
    val versions = mutableMapOf<String, List<String>>()
    for (mod in candidates) {
        val installed = try {
            lookup(mod.fileName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            continue
        }
        if (installed == null || installed.notFound || installed.gameVersions.isEmpty()) continue
        versions[mod.fileName] = installed.gameVersions
    }
    return versions
}

/** 本地 Minecraft 声明在当前版本上的判定：true 兼容 / false 不兼容 / null 无法定论 */
internal fun ModDoctorMetadata.minecraftRangeResults(minecraftVersion: String): List<Boolean?> =
    minecraftVersionRanges.map { range -> versionPredicateDecision(listOf(range), minecraftVersion) }

/** 本地声明明确不支持当前 Minecraft 版本（由既有本地规则负责上报） */
internal fun ModDoctorMetadata.isLocallyIncompatible(minecraftVersion: String): Boolean {
    val results = minecraftRangeResults(minecraftVersion)
    return results.isNotEmpty() && results.all { it == false }
}

/**
 * 本地声明是否精确证明了当前 Minecraft 版本的发布级兼容。
 *
 * 只有带上界的声明（例如 ">=1.21.10 <1.21.12"）才算精确；"*" 与 ">=1.21.10" 这类开放声明
 * 只是作者对“不早于某版本”的声明，不能证明某个具体发布版本兼容，仍需权威 provider 发布数据补充。
 */
internal fun ModDoctorMetadata.provesReleaseCompatibility(minecraftVersion: String): Boolean {
    if (minecraftVersionRanges.isEmpty()) return false
    return minecraftRangeResults(minecraftVersion).all { it == true } &&
            minecraftVersionRanges.all { range -> range.contains('<') }
}

/** ม็อดทั้งหมดในกลุ่มที่ Planner รู้จัก: ยึดตามกฎที่ยืนยันแล้วทุกชุด (ไม่ผูกกับ Minecraft หรือ loader ใดเป็นพิเศษ) */
internal fun groupFamilyModIds(): Set<String> = ModDoctorRules.load().filter { it.verified }.flatMap { it.whenPresent }.toSet()

/**
 * แนบแผนซ่อมกลุ่มเมื่อมีปัญหาที่เกี่ยวกับม็อดในกลุ่มเท่านั้น ไม่แตะรายงานอื่น
 * ความล้มเหลวของเครือข่ายหรือข้อมูลไม่ครบจะกลายเป็น UNVERIFIED ที่แสดงเหตุผล ไม่ทำให้การเปิดเกมพัง
 */
private suspend fun ModDoctorReport.withGroupPlan(
    version: Version,
    modsDir: File,
    minecraftVersion: String,
    loader: ModLoader,
): ModDoctorReport {
    val family = groupFamilyModIds()
    //เส้นทางกลุ่ม Known-good ทำงานเฉพาะเมื่อปัญหาแตะสมาชิกกลุ่ม; การชนจาก breaks ของม็อดใดก็ได้ตรวจทุกครั้ง (อ่านจากเครื่อง ไม่ใช้เครือข่าย)
    val familyTouched = problems.any { it.modId in family || it.requiredDependencyId in family }
    val conflictReported = problems.any { it.type == ModDoctorProblemType.INCOMPATIBLE_MOD_PAIR }
    //Universal V2: dependency ที่ขาดทั่วทั้งอินสแตนซ์ ใช้ Fix Candidate ที่ resolve แล้วจาก withFixCandidates (ไม่ยิงคำขอเพิ่ม)
    val dependencyFindings = problems
        .filter { it.type == ModDoctorProblemType.MISSING_REQUIRED_DEPENDENCY }
        .mapNotNull { problem ->
            val dependencyId = problem.requiredDependencyId ?: return@mapNotNull null
            MissingDependencyFinding(
                dependencyId = dependencyId,
                requiredBy = problem.modId ?: problem.fileName,
                providerProjectId = problem.fixCandidate?.projectId,
                providerVersionName = problem.fixCandidate?.versionName,
                unresolvedReason = problem.reason.substringAfter(UNAVAILABLE_REPAIR_PREFIX, missingDelimiterValue = "")
                    .trimEnd('.')
                    .takeIf { it.isNotBlank() },
            )
        }
    if (!familyTouched && !conflictReported && dependencyFindings.isEmpty()) return this
    val plan = runCatching {
        buildGroupPlan(version, modsDir, minecraftVersion, loader.displayName.lowercase(), family, familyTouched, dependencyFindings)
    }.getOrElse { error ->
        GroupPlan(
            status = GroupPlanStatus.UNVERIFIED,
            confidence = GroupPlanConfidence.UNVERIFIED,
            changes = emptyList(),
            notes = listOf("อ่านข้อมูลจากแพลตฟอร์มไม่สำเร็จ จึงยังสร้างแผนไม่ได้: ${error.message ?: error::class.java.simpleName}"),
            risks = emptyList(),
        )
    }
    //ไม่มีอะไรให้วางแผนจริง (เช่น ปัญหาที่ไม่เกี่ยวกับการชนของม็อด) ไม่แสดงแผนว่างให้ผู้ใช้สับสน
    return if (plan == null) this else copy(groupPlan = plan)
}

/** คำนำหน้าเหตุผลเดิมเมื่อ resolve Fix Candidate ไม่ได้ (ใช้ดึงเหตุผลกลับมาแสดงในแผน) */
private const val UNAVAILABLE_REPAIR_PREFIX = "Automatic repair is unavailable: "

/**
 * ประกอบข้อมูลจริงจากแพลตฟอร์ม (อ่านอย่างเดียว) แล้วส่งให้ Planner; ไม่มีการดาวน์โหลดหรือแก้ไฟล์
 * รวมสองส่วนโดยไม่ซ่อนกัน: แผน breaks/Known-good และแผน dependency ที่ขาด
 */
private suspend fun buildGroupPlan(
    version: Version,
    modsDir: File,
    minecraftVersion: String,
    loader: String,
    family: Set<String>,
    familyTouched: Boolean,
    dependencyFindings: List<MissingDependencyFinding>,
): GroupPlan? {
    val identities = readEnabledModIdentities(modsDir)
    val local = identities.map { GroupPlanInstalled(modId = it.id, fileName = it.fileName, version = it.version, breaks = it.breaks) }

    //หลักฐานชนจาก breaks ของ jar ทุกม็อดในอินสแตนซ์: อ่านจากเครื่องทั้งหมด ไม่ยิงเครือข่าย
    val conflicts = findBreaksConflicts(local)
    val base: GroupPlan? = when {
        conflicts.isNotEmpty() -> universalBreaksPlan(version, modsDir, minecraftVersion, loader, identities, local, conflicts)
        //ไม่มีหลักฐานชน: คงเส้นทางกลุ่ม Known-good/discovery เดิม เฉพาะเมื่อปัญหาแตะสมาชิกกลุ่มและมีสมาชิกติดตั้งอยู่
        familyTouched && identities.any { it.id in family } -> familyGroupPlan(version, modsDir, minecraftVersion, loader, identities, family)
        else -> null
    }
    val policy = ModpackPolicy.load(version.getVersionPath())
    return base.withDependencyPlan(planMissingDependencies(dependencyFindings), policy.isPackManaged)
}

/**
 * Universal V1: แผนสำหรับการชนที่พิสูจน์จาก breaks ของ jar (ม็อดใดก็ได้)
 * ค้นหาบนแพลตฟอร์มเฉพาะม็อดที่มีส่วนในการชนเท่านั้น (ไม่เกิน [MAX_PLAN_LOOKUPS]) แผนแก้เฉพาะเป้าหมายที่ชน
 * Known-good ไม่ถูกนำมาเป็นแผนหรือข้อบังคับในเส้นทางนี้
 */
private suspend fun universalBreaksPlan(
    version: Version,
    modsDir: File,
    minecraftVersion: String,
    loader: String,
    identities: List<InstalledModIdentity>,
    local: List<GroupPlanInstalled>,
    conflicts: List<BreaksEvidence>,
): GroupPlan {
    val involved = planLookupFiles(conflicts) ?: return GroupPlan(
        status = GroupPlanStatus.UNVERIFIED,
        confidence = GroupPlanConfidence.UNVERIFIED,
        changes = emptyList(),
        notes = listOf("ม็อดที่เกี่ยวกับการชนมีมากกว่า $MAX_PLAN_LOOKUPS รายการ จึงยังไม่วิเคราะห์อัตโนมัติ (ไม่เดาและไม่ค้นหาทั้งหมด)"),
        risks = emptyList(),
    )
    val platform = modDoctorFingerprintPlatform(AllSettings.searchModPlatform.getValue())
    val policy = ModpackPolicy.load(version.getVersionPath())

    //เฉพาะไฟล์ที่เกี่ยวกับการชน: ใช้แมป dependency/conflict ของ Modrinth กลับเป็น modId
    val projectByFile = mutableMapOf<String, String>()
    val projectToMod = mutableMapOf<String, String>()
    for (identity in identities.filter { it.fileName in involved }) {
        val matched = lookupInstalledMod(modsDir, identity.fileName, platform) ?: continue
        projectByFile[identity.fileName] = matched.projectId
        projectToMod[matched.projectId] = identity.id
    }

    val installed = local.map { it.copy(projectId = projectByFile[it.fileName]) }
    val candidates = mutableMapOf<String, List<GroupPlanCandidate>>()
    for (target in conflicts.map { it.target.fileName }.distinct()) {
        val member = installed.firstOrNull { it.fileName == target } ?: continue
        val projectId = member.projectId ?: continue
        candidates[member.modId] = loadProjectVersions(projectId, platform).map { candidate ->
            candidate.toGroupCandidate(member.modId, projectId, projectToMod)
        }
    }
    return planBreaksConflicts(minecraftVersion, loader, installed, findBreaksConflicts(installed), candidates, policy::isPackOwned)
}

/** เส้นทางกลุ่ม Known-good/discovery เดิม: ค้นหาเฉพาะสมาชิกกลุ่มที่ติดตั้งอยู่ */
private suspend fun familyGroupPlan(
    version: Version,
    modsDir: File,
    minecraftVersion: String,
    loader: String,
    identities: List<InstalledModIdentity>,
    family: Set<String>,
): GroupPlan {
    val platform = modDoctorFingerprintPlatform(AllSettings.searchModPlatform.getValue())
    val members = identities.filter { it.id in family }

    //แมป dependency/conflict กลับเป็น modId เฉพาะม็อดในกลุ่ม (ไม่ค้นหาม็อดนอกกลุ่มเพิ่ม)
    val projectByFile = mutableMapOf<String, String>()
    val projectToMod = mutableMapOf<String, String>()
    for (identity in members) {
        val matched = lookupInstalledMod(modsDir, identity.fileName, platform) ?: continue
        projectByFile[identity.fileName] = matched.projectId
        projectToMod[matched.projectId] = identity.id
    }

    val installed = members
        .map { GroupPlanInstalled(modId = it.id, fileName = it.fileName, version = it.version, projectId = projectByFile[it.fileName], breaks = it.breaks) }

    val candidates = mutableMapOf<String, List<GroupPlanCandidate>>()
    for (member in installed) {
        val projectId = projectByFile[member.fileName] ?: continue
        candidates[member.modId] = loadProjectVersions(projectId, platform).map { candidate ->
            candidate.toGroupCandidate(member.modId, projectId, projectToMod)
        }
    }

    val policy = ModpackPolicy.load(version.getVersionPath())
    //Known-good 规则的选择与 Inspect 使用同一套：已验证 + 范围更大的优先 + 冲突不猜
    val rule = selectApplicableRules(ModDoctorRules.load(), minecraftVersion, loader, installed.map { it.modId }.toSet())
        .applied.maxWithOrNull(compareBy<ModDoctorSetRule>({ it.whenPresent.size }, { it.priority }))
    return if (rule != null) {
        planModGroupRepair(minecraftVersion, loader, installed, rule, candidates, policy::isPackOwned)
    } else {
        planModGroupDiscovery(minecraftVersion, loader, installed, family, candidates, policy::isPackOwned)
    }
}

/**
 * แปลงข้อมูลเวอร์ชันของ Modrinth เป็นผู้สมัครของ Planner
 * - dependency ที่เป็น REQUIRED แต่แมปไม่ได้ → ไม่รู้ว่ามีหรือไม่ จึงเก็บไว้ให้ Planner ตัดสินเป็น UNVERIFIED
 * - INCOMPATIBLE ที่แมปไม่ได้ → ไม่มีผลถ้าโปรเจกต์นั้นไม่ได้ติดตั้งอยู่ จึงทิ้งได้
 */
private fun PlatformVersion.toGroupCandidate(
    modId: String,
    projectId: String,
    projectToMod: Map<String, String>,
): GroupPlanCandidate {
    val deps = platformDependencies()
    val required = deps.filter { it.type == PlatformDependencyType.REQUIRED }.mapNotNull { it.projectId }
    val incompatible = deps.filter { it.type == PlatformDependencyType.INCOMPATIBLE }.mapNotNull { it.projectId }
    return GroupPlanCandidate(
        modId = modId,
        projectId = projectId,
        versionName = platformVersion(),
        fileName = platformFileName(),
        gameVersions = platformGameVersion().toSet(),
        loaders = platformLoaders().map { it.getDisplayName().lowercase() }.toSet(),
        requiredModIds = required.mapNotNull { projectToMod[it] }.toSet(),
        incompatibleModIds = incompatible.mapNotNull { projectToMod[it] }.toSet(),
        unresolvedRequiredProjects = required.filter { it !in projectToMod }.toSet(),
        releaseType = when (platformReleaseType()) {
            PlatformReleaseType.RELEASE -> GroupPlanReleaseType.RELEASE
            PlatformReleaseType.BETA -> GroupPlanReleaseType.BETA
            PlatformReleaseType.ALPHA -> GroupPlanReleaseType.ALPHA
        },
    )
}
