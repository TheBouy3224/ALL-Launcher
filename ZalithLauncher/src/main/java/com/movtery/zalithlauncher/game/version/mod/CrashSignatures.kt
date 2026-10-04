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
import com.google.gson.annotations.SerializedName
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.context.GlobalContext
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File

private const val TAG = "CrashSignatures"

/**
 * 诊断的可信度。
 *
 * 只表示「这句话有多大的把握」，不表示严重程度：LOW 的条目仍然会被展示，
 * 只是不允许它驱动任何改动文件的动作。
 */
enum class DiagnosisConfidence(val labelRes: Int) {
    HIGH(R.string.crash_diagnosis_confidence_high),
    MEDIUM(R.string.crash_diagnosis_confidence_medium),
    LOW(R.string.crash_diagnosis_confidence_low),
}

/**
 * 一条诊断允许建议的动作。
 *
 * V1 只有两种：可逆地禁用被点名的模组文件，或者什么都不做。
 * [NONE] 是默认的诚实答案——看不懂就不要动用户的文件。
 */
enum class DiagnosisAction(val id: String, val labelRes: Int?) {
    DISABLE("disable", R.string.crash_diagnosis_action_disable),
    NONE("none", null),
}

/**
 * 从日志里取到该条特征所属模组 id 的方法。
 *
 * 只在「命中行」上尝试（即包含该条特征任一匹配字面量的那些行），所以正则不必自己锚定整段文本。
 */
data class CrashSignatureOwner(
    /** 在命中行里寻找的正则，必须且只需捕获一个模组 id */
    val regex: String,
    /** 要取的分组序号，默认第 1 组 */
    val group: Int = 1,
)

/**
 * 一条崩溃特征：足以让启动器在崩溃之后说出「可能是什么原因」的最小事实。
 *
 * 匹配是区分大小写的字面量包含判断，模组的类名被有意省略：类名随加载器版本变化，
 * 而加载器自己写下的句子反而更稳定。
 *
 * @param className 这条特征属于哪一类（`platform_limit` / `mixin_failure` / `loader_fatal`）
 * @param match 这些字面量**全部**都要出现
 * @param matchAny 这些字面量**至少**出现一个（为空表示这一层不要求）
 * @param owner 如何从命中行里取出被点名的模组 id；为 null 表示这条特征不点名任何模组
 * @param messageKey 用户可见文案的键，文案本身留在 Kotlin 里（见 [diagnosisMessageRes]）
 */
data class CrashSignature(
    val id: String,
    val className: String,
    val match: List<String>,
    val matchAny: List<String>,
    val owner: CrashSignatureOwner?,
    val confidence: DiagnosisConfidence,
    val action: DiagnosisAction,
    val messageKey: String,
) {
    /** 这段日志文本是否命中该条特征 */
    fun matches(text: String): Boolean {
        val required = match + matchAny
        if (required.isEmpty()) return false
        if (match.any { !text.contains(it) }) return false
        if (matchAny.isNotEmpty() && matchAny.none { text.contains(it) }) return false
        return true
    }

    /**
     * 命中的行里被点名模组 id；取不到时返回 null（那就只诊断，不指认任何模组）
     */
    fun captureOwner(text: String): String? {
        val spec = owner ?: return null
        val pattern = runCatching { Regex(spec.regex) }.getOrNull() ?: return null
        val anchors = match + matchAny

        return text.lineSequence()
            .filter { line -> anchors.any { line.contains(it) } }
            .firstNotNullOfOrNull { line ->
                pattern.find(line)
                    ?.groupValues
                    ?.getOrNull(spec.group)
                    ?.takeIf { it.isNotBlank() }
            }
    }
}

/**
 * 数据文件里的原始形状：字段允许缺省。理由与 [ModDoctorSetRuleJson] 相同——
 * GSON 对缺失字段给出 null，直接把 null 塞进非空模型会在解析时炸掉，
 * 而不是把一个手写错的条目安静地丢掉。
 */
private data class CrashSignatureOwnerJson(
    val regex: String? = null,
    val group: Int? = null,
)

private data class CrashSignatureJson(
    val id: String? = null,
    @SerializedName("class") val className: String? = null,
    val match: List<String>? = null,
    @SerializedName("match_any") val matchAny: List<String>? = null,
    val owner: CrashSignatureOwnerJson? = null,
    val confidence: String? = null,
    val action: String? = null,
    @SerializedName("message_key") val messageKey: String? = null,
)

private data class CrashSignaturesFile(
    @SerializedName("signatures_version") val signaturesVersion: Int = 0,
    val signatures: List<CrashSignatureJson>? = null,
)

/**
 * 特征文件（assets/crash_signatures.json）的解析；纯函数，回归测试直接用它。
 * 缺字段、空集合、不是 JSON 都只会让对应条目被丢掉，不会抛异常。
 */
internal fun parseCrashSignatures(json: String): List<CrashSignature> {
    val file = runCatching { Gson().fromJson(json, CrashSignaturesFile::class.java) }.getOrNull()

    return file?.signatures.orEmpty().mapNotNull { raw ->
        val id = raw.id?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val className = raw.className?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val match = raw.match?.filter { it.isNotBlank() }.orEmpty()
        val matchAny = raw.matchAny?.filter { it.isNotBlank() }.orEmpty()
        //一条谁都能命中的特征比没有特征更危险，所以两个匹配层至少要有内容
        if (match.isEmpty() && matchAny.isEmpty()) return@mapNotNull null
        val confidence = raw.confidence.toConfidence() ?: return@mapNotNull null
        val action = raw.action.toAction() ?: return@mapNotNull null
        val messageKey = raw.messageKey?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val owner = raw.owner?.regex?.takeIf { it.isNotBlank() }?.let { regex ->
            CrashSignatureOwner(regex = regex, group = raw.owner.group?.takeIf { it > 0 } ?: 1)
        }

        CrashSignature(
            id = id,
            className = className,
            match = match,
            matchAny = matchAny,
            owner = owner,
            confidence = confidence,
            action = action,
            messageKey = messageKey,
        )
    }
}

private fun String?.toConfidence(): DiagnosisConfidence? = this?.let { value ->
    DiagnosisConfidence.values().firstOrNull { it.name.equals(value, ignoreCase = true) }
}

private fun String?.toAction(): DiagnosisAction? = this?.let { value ->
    DiagnosisAction.values().firstOrNull { it.id.equals(value, ignoreCase = true) }
}

/**
 * 用户可见文案：数据文件只说自己是哪一条，说什么由这里决定。
 * 认不出来的键退回「不认识的启动失败」，而不是把键本身显示给用户。
 */
fun diagnosisMessageRes(messageKey: String): Int = when (messageKey) {
    "crash_diagnosis_platform_limit" -> R.string.crash_diagnosis_platform_limit
    "crash_diagnosis_mixin_failure" -> R.string.crash_diagnosis_mixin_failure
    "crash_diagnosis_loader_fatal" -> R.string.crash_diagnosis_loader_fatal
    else -> R.string.crash_diagnosis_unclassified
}

/**
 * 这个启动器能读的最高规则包 schema 版本。
 *
 * 版本门是外部规则包唯一的安全阀：写坏的文件、为更新的启动器写的文件都不能替换掉随包的规则，
 * 否则一份坏文件会让所有诊断静默失效。
 */
internal const val CRASH_SIGNATURE_SCHEMA_VERSION = 1

/**
 * 外部规则包的解析：先过版本门，再要一份非空的条目表。
 *
 * 条目本身复用 [parseCrashSignatures]：缺字段、写坏的条目只会被丢掉，不会抛异常。
 *
 * @param max 这个启动器能读的最高 schema 版本
 * @return 通过版本门且至少有一条特征的列表；任何一条不满足都返回 null，调用方必须退回随包规则
 */
internal fun parseExternalRulePack(
    json: String,
    max: Int = CRASH_SIGNATURE_SCHEMA_VERSION,
): List<CrashSignature>? {
    val file = runCatching { Gson().fromJson(json, CrashSignaturesFile::class.java) }.getOrNull() ?: return null
    val version = file.signaturesVersion
    if (version <= 0 || version > max) return null

    val signatures = parseCrashSignatures(json)
    return signatures.takeIf { it.isNotEmpty() }
}

/**
 * 读取一份外部规则包文件。
 *
 * 文件不在、目录不在、读不出来、或没过版本门，一律返回 null：调用方必须退回随包规则。
 */
internal fun readExternalRulePack(file: File): List<CrashSignature>? {
    if (!file.isFile) return null
    val json = runCatching { file.readText() }.getOrNull() ?: return null
    val signatures = parseExternalRulePack(json)
    if (signatures == null) {
        Logger.warning(TAG, "The external crash rule pack was rejected; using the bundled signatures.")
    }
    return signatures
}

/**
 * 读取崩溃特征文件：外部规则包优先，退回随包资源，再退回空列表。
 *
 * 随包资源不走版本门——它随代码一起审核，是可信的；外部规则包必须过门。
 * 读不到时返回空列表：特征缺失只意味着这一轮诊断说不出原因，不能让崩溃页面失败。
 */
internal object CrashSignatures {
    private const val ASSET_NAME = "crash_signatures.json"

    /** 外部规则包在应用私有目录里的位置；由 adb 安装，启动器自己不下载它 */
    private const val EXTERNAL_PACK_PATH = "rules/crash_signatures.json"

    @Volatile
    private var cached: List<CrashSignature>? = null

    /** 上次读取时外部规则包的指纹；没有外部包时是 "none" */
    @Volatile
    private var cachedExternalStamp: String? = null

    @Synchronized
    fun load(): List<CrashSignature> {
        val externalFile = externalRulePackFile()
        val stamp = externalRulePackStamp(externalFile)
        //同一个外部包（或一直都没有）就复用上次读到的结果；随包资源不会在原地变化，读过一次就够
        if (stamp == cachedExternalStamp) cached?.let { return it }

        val external = externalFile?.let { readExternalRulePack(it) }
        external?.let { Logger.info(TAG, "crash rule pack loaded: source=external, count=${it.size}") }
        val loaded = external ?: loadBundledSignatures()

        cached = loaded
        cachedExternalStamp = stamp
        return loaded
    }

    /** 外部规则包的位置；应用私有目录还没初始化时（例如单元测试）当作没有外部包 */
    private fun externalRulePackFile(): File? = runCatching {
        File(PathManager.DIR_FILES_PRIVATE, EXTERNAL_PACK_PATH)
    }.getOrNull()

    /** 外部规则包的指纹：文件不在时是 "none"，文件变了就重新读 */
    private fun externalRulePackStamp(file: File?): String =
        if (file == null || !file.isFile) "none" else "${file.length()}:${file.lastModified()}"

    /** 随包发布的特征文件；读不到时返回空列表，并留下一条警告；它不经过版本门 */
    private fun loadBundledSignatures(): List<CrashSignature> {
        val loaded = runCatching {
            val json = GlobalContext.applicationContext.assets.open(ASSET_NAME)
                .bufferedReader()
                .use { it.readText() }
            parseCrashSignatures(json)
        }.getOrElse { error ->
            Logger.warning(TAG, "The crash signature file could not be read; continuing without signatures", error)
            emptyList()
        }
        Logger.info(TAG, "crash rule pack loaded: source=bundled, count=${loaded.size}")
        return loaded
    }
}
