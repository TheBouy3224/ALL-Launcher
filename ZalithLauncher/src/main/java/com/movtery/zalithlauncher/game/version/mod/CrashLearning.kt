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

import com.google.gson.Gson
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.utils.logging.Logger
import com.tencent.mmkv.MMKV
import java.io.File

private const val TAG = "CrashLearning"

/**
 * 一条本地学习记录的状态。
 *
 * V1 只有这两个：刚刚猜出来（候选），和**在这台设备上**被自己的日志证实过一次。
 * 没有 VERIFIED：跨设备或跨会话的「验证」需要云端，而 V1 不联网。
 */
enum class LearningState {
    CANDIDATE,
    CONFIRMED_LOCAL,
}

/**
 * 一条崩溃后的本地学习记录。键是「签名Id + 模组集指纹」，因为一个原因只在同一套模组上才有意义：
 * 换了模组版本，上一次的结论就不能拿来用。
 *
 * V1 有意不记录的东西（模型里根本没有对应字段，也没有任何读取它们的代码路径）：
 * 仓库/云端来的任何结论、账号数据、启动器设置。
 *
 * @param instanceName 实例目录名。从崩溃日志的路径推出来，不来自内存里的任何版本注册表
 * @param logPath 这次诊断读的那份捕获日志的**绝对**路径。收尾时只读它，所以每条记录都要自己带着
 * @param modSetFingerprint 该实例**启用中**模组的 `id@version` 排序后连接
 * @param outcomeClass [LaunchOutcome] 的名字，记录这次会话的结局
 * @param ownerModId 日志里点名到的模组 id；LOW 的条目不允许点名
 * @param actionPreState 修复动作改动前的状态（被禁用的文件名）。为空表示这次没有动过任何文件
 * @param actionLogStamp 动作发生时捕获日志的指纹（见 [capturedLogStamp]），
 *        用来分辨「用户又启动过一次」和「日志还是崩溃那一次留下的」
 * @param markerSeenAfterFix / crashMarkerSeenAfterFix 修复之后那次启动的观测结果
 * @param count 同一签名在同一指纹上命中的次数
 * @param inconclusive 三次命中仍未升级，之后永不升级
 */
data class LearningRecord(
    val instanceName: String,
    /** 旧记录里可能没有这个字段，读到 null 就什么都不下结论 */
    val logPath: String? = null,
    val minecraftVersion: String,
    val loader: String,
    val modSetFingerprint: String,
    val signatureId: String,
    val outcomeClass: String,
    val ownerModId: String?,
    val diagnosisType: String,
    val confidence: String,
    val action: String,
    val actionPreState: String,
    val actionLogStamp: String,
    val markerSeenAfterFix: Boolean,
    val crashMarkerSeenAfterFix: Boolean,
    val count: Int,
    val state: LearningState,
    val inconclusive: Boolean,
)

/**
 * 一次诊断能提供的事实（纯数据，不碰文件也不碰 Android）。
 */
data class LearningMatch(
    val instanceName: String,
    /** 这次诊断读的那份捕获日志的绝对路径；收尾要靠它，所以必须在诊断那一刻记下来 */
    val logPath: String,
    val minecraftVersion: String,
    val loader: String,
    val modSetFingerprint: String,
    val signatureId: String,
    val diagnosisType: String,
    val outcomeClass: String,
    val ownerModId: String?,
    val confidence: DiagnosisConfidence,
    val action: DiagnosisAction,
)

/** 同一签名在同一模组集上命中多少次之后就不再期待升级 */
const val LEARNING_MAX_MATCHES_WITHOUT_PROMOTION = 3

/** 升级为 [LearningState.CONFIRMED_LOCAL] 需要的命中次数（第二次命中才叫「再次出现」） */
const val LEARNING_PROMOTION_MATCH_COUNT = 2

/** 一次待续的自动重启的键前缀：`relaunch|<实例名>`，值是写下它的时刻（epoch 毫秒） */
internal const val RELAUNCH_PREFIX = "relaunch|"

/** 一次待续的自动重启最多还能有多旧；超过这个时间就不再自动拉起游戏 */
internal const val RELAUNCH_MAX_AGE_MILLIS = 5 * 60 * 1000L

/** 启动前最多警告几个本机学到的崩溃 */
internal const val LEARNED_WARNING_LIMIT = 3

/**
 * 一次待续的自动重启是否还有效。
 *
 * 零值、未来值、以及超过 [RELAUNCH_MAX_AGE_MILLIS] 的值都不算：一个过期的标记不能拉起
 * 用户没有要过的实例，也可能是另一个实例。
 */
internal fun relaunchIsDue(atMillis: Long, nowMillis: Long): Boolean =
    atMillis >= 1L && atMillis <= nowMillis && nowMillis - atMillis <= RELAUNCH_MAX_AGE_MILLIS

internal fun learningRuleKey(signatureId: String, modSetFingerprint: String): String =
    "$signatureId:$modSetFingerprint"

/**
 * 把一次命中并入已有记录。
 *
 * V1 拒绝记录：LOW 可信度的结论、没有签名、没有实例、没有模组集指纹的规则。
 * 最后一条最要紧——没有指纹，这条规则换一套模组也能被套用，那不是本机学习，是猜测。
 *
 * @return 更新后的记录；被拒绝时返回 null，调用方不得写入任何东西
 */
internal fun mergeLearningMatch(existing: LearningRecord?, match: LearningMatch): LearningRecord? {
    if (match.confidence == DiagnosisConfidence.LOW) return null
    if (match.signatureId.isBlank()) return null
    if (match.instanceName.isBlank()) return null
    if (match.modSetFingerprint.isBlank()) return null

    val count = (existing?.count ?: 0) + 1
    val state = existing?.state ?: LearningState.CANDIDATE
    val inconclusive = existing?.inconclusive == true ||
        (state != LearningState.CONFIRMED_LOCAL && count >= LEARNING_MAX_MATCHES_WITHOUT_PROMOTION)

    return LearningRecord(
        instanceName = match.instanceName,
        logPath = match.logPath,
        minecraftVersion = match.minecraftVersion,
        loader = match.loader,
        modSetFingerprint = match.modSetFingerprint,
        signatureId = match.signatureId,
        outcomeClass = match.outcomeClass,
        ownerModId = match.ownerModId,
        diagnosisType = match.diagnosisType,
        confidence = match.confidence.name,
        action = match.action.id,
        actionPreState = existing?.actionPreState.orEmpty(),
        actionLogStamp = existing?.actionLogStamp.orEmpty(),
        markerSeenAfterFix = existing?.markerSeenAfterFix ?: false,
        crashMarkerSeenAfterFix = existing?.crashMarkerSeenAfterFix ?: false,
        count = count,
        state = state,
        inconclusive = inconclusive,
    )
}

/**
 * 登记一次**真的执行了**的修复动作。只允许调用方在文件确实被改动之后调用：
 * [actionPreState] 非空是「这次动作动过文件」的唯一凭据，也是升级的前置条件之一。
 */
internal fun withActionTaken(record: LearningRecord, preState: String, logStamp: String): LearningRecord =
    record.copy(actionPreState = preState, actionLogStamp = logStamp)

/**
 * 用修复之后那次启动的观测结果收尾。
 *
 * 升级为 [LearningState.CONFIRMED_LOCAL] 需要同时成立：
 * 1. 同一签名在同一模组集指纹上已经命中过第二次（[LearningRecord.count]）
 * 2. 这次动作动过文件，且之后那次启动的日志里**有**启动标记、**没有**崩溃标记
 *
 * 还差一个条件就保持候选；已经判为不成立的记录永不升级。
 *
 * @param logRewritten 日志在这次动作之后被重写过吗。没有重写过就说明用户还没再启动过，
 *        此时什么都不下结论——把「还是崩溃那一次的旧日志」读成「修复失败」是错的
 */
internal fun applyActionOutcome(
    record: LearningRecord,
    logRewritten: Boolean,
    markerSeen: Boolean,
    crashMarkerSeen: Boolean,
): LearningRecord {
    if (!logRewritten) return record

    val observed = record.copy(
        markerSeenAfterFix = markerSeen,
        crashMarkerSeenAfterFix = crashMarkerSeen,
    )
    val promoted = !record.inconclusive &&
        record.state != LearningState.CONFIRMED_LOCAL &&
        record.count >= LEARNING_PROMOTION_MATCH_COUNT &&
        record.actionPreState.isNotBlank() &&
        markerSeen &&
        !crashMarkerSeen

    return if (promoted) observed.copy(state = LearningState.CONFIRMED_LOCAL) else observed
}

/**
 * 实例当前启用模组的指纹：排序后的 `id@version` 连接。
 *
 * 读不到模组（目录不存在、全部禁用、全部读不出来）时返回空串，调用方据此拒绝记录这条规则。
 */
internal fun modSetFingerprint(locals: List<LocalMod>): String = locals
    .filter { !it.notMod && it.file.isEnabled() && it.id.isNotBlank() }
    .map { "${it.id}@${it.version.orEmpty()}" }
    .sorted()
    .joinToString(",")

/**
 * 崩溃页面的落地决议。
 *
 * @param match 要写进本地学习记录的一条事实；模组集指纹读不出来时为 null，那就什么都不记
 * @param fix 唯一一个可逆动作：要禁用的模组 id 与它所在的目录；目录里没有那个文件时为空
 */
internal data class CrashDecision(
    val match: LearningMatch?,
    val fix: CrashFix?,
)

internal data class CrashFix(
    val modsDir: File,
    val modId: String,
)

/**
 * 把一次诊断落到模组目录上。
 *
 * 只从两样东西推出来：崩溃日志的**路径**和它的**文本**。任何内存里的版本注册表都不参与——
 * 崩溃页面很可能是被冷启动起来的进程，那时 `VersionsManager` 是空的（2026-09-24 真机运行时
 * 进程被回收后按钮从来没被组合出来，就是这条依赖造成的）。
 *
 * 目录从路径推：日志永远放在 `<versionsHome>/<实例名>/ALLLauncher/<log>`，所以实例目录是日志文件的
 * 上上级，模组目录是它下面的 `mods`。推不出来、或模组集指纹读不出来时返回 null：那只说明这次什么都不记。
 *
 * 指纹在这里一次算出来并跟着决议一起交出去：动作真的禁用掉那个文件之后，**模组集本身就变了**，
 * 那时再算指纹会得到另一个键，动作就登记不到这条诊断上了。
 */
internal suspend fun resolveCrashDecision(
    logFile: File,
    text: String,
    outcome: CrashOutcome?,
): CrashDecision? {
    val decided = outcome ?: return null
    val finding = decided.finding ?: return null
    val instanceDir = crashInstanceDir(logFile) ?: return null
    val modsDir = VersionFolders.MOD.getDir(instanceDir)
    if (!modsDir.isDirectory) return null

    val locals = runCatching { AllModReader(modsDir).readAllLocals() }.getOrDefault(emptyList())
    val fingerprint = modSetFingerprint(locals)
    if (fingerprint.isBlank()) return null

    val identity = readLaunchIdentity(text)
    val match = LearningMatch(
        instanceName = instanceDir.name,
        logPath = logFile.absolutePath,
        minecraftVersion = identity.minecraftVersion,
        loader = identity.loader,
        modSetFingerprint = fingerprint,
        signatureId = finding.signatureId,
        diagnosisType = finding.diagnosisType,
        outcomeClass = decided.launchOutcome.name,
        ownerModId = finding.ownerModId,
        confidence = finding.confidence,
        action = finding.action,
    )

    //动作只在四条同时成立时出现：可信、建议禁用、日志点名了模组、那个文件真的还在这份目录里
    val fix = if (finding.confidence == DiagnosisConfidence.HIGH && finding.action == DiagnosisAction.DISABLE) {
        finding.ownerModId?.let { ownerModId ->
            locals.firstOrNull { it.id == ownerModId && !it.notMod && it.file.isEnabled() }
                ?.let { CrashFix(modsDir = modsDir, modId = ownerModId) }
        }
    } else null

    return CrashDecision(match = match, fix = fix)
}

/**
 * 本地学习记录的读写。所有函数都不抛异常：学习是副产品，它坏掉不能让崩溃页面或启动失败。
 */
object CrashLearning {
    private const val PENDING_PREFIX = "pending|"

    /**
     * 记录一次诊断（候选）。被 V1 拒绝时返回 null，并且不写任何东西。
     */
    fun recordDiagnosis(match: LearningMatch): LearningRecord? = runCatching {
        val store = postCrashLearning()
        val key = learningRuleKey(match.signatureId, match.modSetFingerprint)
        val recorded = mergeLearningMatch(readRecord(store, key), match) ?: return@runCatching null

        writeRecord(store, key, recorded)
        Logger.info(
            TAG,
            "Diagnosis recorded: ${recorded.signatureId} (${recorded.state}, count=${recorded.count})"
        )
        recorded
    }.getOrNull()

    /**
     * 登记一次已经执行的修复动作，并把它挂到实例上等待下一次启动的结果。
     */
    fun recordActionTaken(match: LearningMatch, preState: String, logStamp: String): LearningRecord? = runCatching {
        if (preState.isBlank()) return@runCatching null

        val store = postCrashLearning()
        val key = learningRuleKey(match.signatureId, match.modSetFingerprint)
        val current = readRecord(store, key) ?: return@runCatching null

        val updated = withActionTaken(current, preState, logStamp)
        writeRecord(store, key, updated)
        store.encode(PENDING_PREFIX + match.instanceName, key)
        updated
    }.getOrNull()

    /**
     * 启动时收尾：每个还挂着待验证动作的实例，用它这一次的捕获日志升级或丢弃。
     *
     * 一次启动调用一次，不阻塞界面（调用方放在 IO 上），永不抛异常。
     */
    fun resolvePendingActions() {
        runCatching {
            val store = postCrashLearning()
            store.allKeys()
                .orEmpty()
                .filter { it.startsWith(PENDING_PREFIX) }
                .forEach { resolvePending(store, it) }
        }.onFailure { error ->
            Logger.warning(TAG, "Pending post-crash actions could not be resolved", error)
        }
    }

    /**
     * 本机学习记录的全部条目（只读快照）。
     *
     * 给启动前的警告消费者用：它按记录自己的身份字段匹配，**不**用当前模组集指纹去算键——
     * 动作把 owner 文件禁用之后模组集本身就变了，用当前指纹算出来的键永远找不到那条记录。
     */
    internal fun learnedRecords(): List<LearningRecord> = runCatching {
        val store = postCrashLearning()
        store.allKeys().orEmpty()
            .filter { !it.startsWith(PENDING_PREFIX) && !it.startsWith(RELAUNCH_PREFIX) }
            .mapNotNull { key -> readRecord(store, key) }
    }.getOrDefault(emptyList())

    /**
     * 记下一次待续的自动重启：修复动作已经执行、进程马上要重启，重启之后由启动路径接手。
     *
     * 只有崩溃页面的修复分支会写这个键；值是写下它的时刻，用来判断它是否还新鲜。
     */
    fun markRelaunchPending(instanceName: String, atMillis: Long = System.currentTimeMillis()): Boolean =
        runCatching {
            if (instanceName.isBlank() || atMillis <= 0L) return@runCatching false
            postCrashLearning().encode(RELAUNCH_PREFIX + instanceName, atMillis)
        }.getOrDefault(false)

    /**
     * 取出一次待续的自动重启，并立刻把它删掉（一次性）。
     *
     * 先读、再删、最后才判断时效：无论这次是否真的拉起游戏，这个标记都只能被消费一次。
     *
     * @return 要自动拉起的实例名；没有待续重启、或标记已失效时为 null
     */
    @Synchronized
    fun consumePendingRelaunch(nowMillis: Long = System.currentTimeMillis()): String? = runCatching {
        val store = postCrashLearning()
        val key = store.allKeys().orEmpty().firstOrNull { it.startsWith(RELAUNCH_PREFIX) }
            ?: return@runCatching null
        val atMillis = runCatching { store.decodeLong(key) }.getOrDefault(0L)
        runCatching { store.removeValueForKey(key) }

        val instanceName = key.removePrefix(RELAUNCH_PREFIX)
        if (instanceName.isBlank() || !relaunchIsDue(atMillis, nowMillis)) {
            Logger.info(TAG, "A pending relaunch was dropped: instance=$instanceName")
            return@runCatching null
        }
        Logger.info(TAG, "A pending relaunch is due: instance=$instanceName")
        instanceName
    }.getOrNull()

    private fun resolvePending(store: MMKV, pendingKey: String) {
        val ruleKey = runCatching { store.decodeString(pendingKey) }.getOrNull()
        val record = ruleKey?.let { readRecord(store, it) }
        if (ruleKey == null || record == null) {
            runCatching { store.removeValueForKey(pendingKey) }
            return
        }
        if (record.logPath.isNullOrBlank()) {
            //旧版本写的记录没有留下日志路径，永远读不到它：丢掉这条待办，记录本身保持原样
            runCatching { store.removeValueForKey(pendingKey) }
            return
        }

        val updated = resolvePendingOutcome(record) ?: return

        writeRecord(store, ruleKey, updated)
        runCatching { store.removeValueForKey(pendingKey) }
        Logger.info(TAG, "Post-crash action resolved: ${updated.signatureId} (${updated.state})")
    }

    /**
     * 用记录**自己带的**日志路径收尾。
     *
     * 路径在诊断那一刻就写进了记录，所以这里不需要任何内存里的版本状态：启动时注册表有没有加载完，
     * 都和这次收尾无关。
     *
     * @return 新的记录；读不到日志、或这次的日志还是原来那一份（没被重写过）时为 null——
     *        那只是「还下不了结论」，调用方保持原样，下次启动再看
     */
    internal fun resolvePendingOutcome(record: LearningRecord): LearningRecord? {
        val logFile = record.logPath?.takeIf { it.isNotBlank() }?.let { File(it) } ?: return null
        if (!logFile.isFile) return null
        if (capturedLogStamp(logFile) == record.actionLogStamp) return null

        val text = readCapturedLogTail(logFile)
        val markerSeen = text.contains(LAUNCH_REACHED_MARKER)
        val crashMarkerSeen = text.contains(GAME_CRASH_MARKER)
        //自动重启之后日志会被立刻重建：这时两种标记都还没写下来，任何结论都是错的。
        //保留待办，等下一次启动再看——把「还没写」读成「修复失败」会毁掉升级窗口。
        if (!markerSeen && !crashMarkerSeen) return null

        return applyActionOutcome(
            record = record,
            logRewritten = true,
            markerSeen = markerSeen,
            crashMarkerSeen = crashMarkerSeen,
        )
    }

    private fun readRecord(store: MMKV, key: String): LearningRecord? = runCatching {
        store.decodeString(key)?.let { json -> Gson().fromJson(json, LearningRecord::class.java) }
    }.getOrNull()

    private fun writeRecord(store: MMKV, key: String, record: LearningRecord) {
        runCatching { store.encode(key, Gson().toJson(record)) }
            .onFailure { Logger.warning(TAG, "A learning record could not be written", it) }
    }
}
