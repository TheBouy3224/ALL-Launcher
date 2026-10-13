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
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.utils.logging.Logger

private const val TAG = "ModDoctorRules"

/**
 * 一条「已知兼容成套」规则。
 *
 * 模组自己声明的 depends / breaks 已经由统一校验阶段负责（缺失依赖、依赖版本、互相冲突），
 * 这里只补一件元数据无法表达的事：一组模组同时存在时，它们的版本必须落在同一个**已验证的允许集合**里。
 * 例如 Sodium 与 Iris 各自的声明都能被满足，但仍然可能是加载器拒绝的组合。
 *
 * @param minecraft 规则适用的 Minecraft 版本
 * @param loader 规则适用的加载器（与 [ModLoader.displayName] 同名，不区分大小写）
 * @param whenPresent 触发条件：这些模组 id **全部**存在时规则才生效；缺任何一个都保持沉默
 * @param allowed 模组 id 到允许版本的映射。版本比较忽略 `+` 之后的构建元数据
 */
data class ModDoctorSetRule(
    val minecraft: String,
    val loader: String,
    val whenPresent: List<String>,
    val allowed: Map<String, String>,
    /** 规则编号，仅用于日志、测试与文档对照（可为空） */
    val id: String = "",
    /** 规则怎么用，见 [ModDoctorRuleMode]；默认值保持旧规则文件的原有行为 */
    val mode: ModDoctorRuleMode = ModDoctorRuleMode.ENFORCE,
    /**
     * 这条规则是否有真实测试证据。**只有 verified 的规则才会被引擎和 Planner 使用**；
     * 没有证据的条目只是“候选”，保留在文件里但不起作用，绝不能因为 Modrinth 上看起来合理就升级成 Known-good。
     */
    val verified: Boolean = true,
    /** 同一覆盖范围的规则互相冲突时，数值大的优先（默认 0）；覆盖范围更大的规则永远优先于它，不看这个值 */
    val priority: Int = 0,
    /** 证据指向（例如 checkpoint 条目名）；仅供人读 */
    val evidence: String = "",
)

/**
 * 一条已验证规则的用法：
 * - [ENFORCE]：已知“偏离这一套会被加载器拒绝”（有证据），不一致就报告并可修复（旧规则文件的原有语义）
 * - [KNOWN_GOOD]：只说明“这一套已经验证过能用”。它**不**说明别的组合不能用，所以不一致时不报告、不强制换版本；
 *   只能当作 Planner 的建议来源
 */
enum class ModDoctorRuleMode { ENFORCE, KNOWN_GOOD }

/**
 * 规则文件里的原始形状：字段允许缺省。写成独立一层的理由很实际：GSON 对缺失字段会给出 null，
 * 直接把 null 塞进非空模型就会在解析时炸掉，而不是把一个手写错的条目安静地丢掉。
 * Ruby 风格的 `when_present` 靠 [SerializedName] 映射，字段名与文件里写的一致。
 */
private data class ModDoctorSetRuleJson(
    val minecraft: String? = null,
    val loader: String? = null,
    @SerializedName("when_present") val whenPresent: List<String>? = null,
    val allowed: Map<String, String>? = null,
    val id: String? = null,
    val mode: String? = null,
    val verified: Boolean? = null,
    val priority: Int? = null,
    val evidence: String? = null,
)

private data class ModDoctorRulesFile(
    @SerializedName("rules_version") val rulesVersion: Int = 0,
    @SerializedName("compatible_sets") val compatibleSets: List<ModDoctorSetRuleJson>? = null,
)

/**
 * 规则文件（assets/mod_doctor_rules.json）的解析；纯函数，回归测试直接用它。
 * 缺字段、空集合、不是 JSON 都只会让对应条目被丢掉，不会抛异常。
 */
internal fun parseModDoctorRules(json: String): List<ModDoctorSetRule> {
    val file = runCatching { Gson().fromJson(json, ModDoctorRulesFile::class.java) }.getOrNull()
    //rules_version 1 及更早：每条规则都按旧语义处理（已验证 + ENFORCE），保证已经发布的规则文件行为不变。
    //rules_version >= 2：必须显式写 "verified": true 才算数；缺省视为“候选”（不起作用），mode 缺省视为 KNOWN_GOOD（不强制）。
    val strict = (file?.rulesVersion ?: 0) >= 2
    return file?.compatibleSets.orEmpty().mapNotNull { raw ->
        val minecraft = raw.minecraft?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val loader = raw.loader?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val whenPresent = raw.whenPresent?.filter { it.isNotBlank() }.orEmpty()
        val allowed = raw.allowed.orEmpty().filterKeys { it.isNotBlank() }
        if (whenPresent.isEmpty() || allowed.isEmpty()) return@mapNotNull null
        ModDoctorSetRule(
            minecraft = minecraft,
            loader = loader,
            whenPresent = whenPresent,
            allowed = allowed,
            id = raw.id.orEmpty(),
            mode = parseRuleMode(raw.mode, strict),
            verified = raw.verified ?: !strict,
            priority = raw.priority ?: 0,
            evidence = raw.evidence.orEmpty(),
        )
    }
}

/** 没写 → 旧文件保持 ENFORCE、新文件用 KNOWN_GOOD；写了但不认识 → 一律 KNOWN_GOOD（宁可不强制） */
private fun parseRuleMode(raw: String?, strict: Boolean): ModDoctorRuleMode = when (raw?.trim()?.lowercase()) {
    null, "" -> if (strict) ModDoctorRuleMode.KNOWN_GOOD else ModDoctorRuleMode.ENFORCE
    "enforce" -> ModDoctorRuleMode.ENFORCE
    else -> ModDoctorRuleMode.KNOWN_GOOD
}

/**
 * 在同一个 Minecraft 版本 + 加载器下，从多条规则里选出真正生效的那些（规则可以互相重叠）。
 *
 * 选择规则（固定顺序，没有“看情况”）：
 * 1. 只看 [ModDoctorSetRule.verified] 的规则；Minecraft、加载器必须一致，`whenPresent` 必须全部已安装。
 * 2. **范围更大的优先**：如果另一条候选规则的 `whenPresent` 是它的真超集，这条被遮蔽（shadowed），不再生效。
 * 3. 覆盖范围完全相同的规则：`priority` 大的赢；`priority` 也相同但允许的版本不同 → 无法决定（ambiguous）。
 * 4. 没有互相遮蔽、但对同一个模组给出了不同允许版本的规则 → 都不生效（ambiguous），**不猜**。
 * 结果里保留原来的顺序，方便测试与日志；ambiguous / shadowed 的规则不会产生任何问题或计划。
 */
internal data class RuleSelection(
    val applied: List<ModDoctorSetRule>,
    val shadowed: List<ModDoctorSetRule>,
    val ambiguous: List<ModDoctorSetRule>,
)

internal fun selectApplicableRules(
    rules: List<ModDoctorSetRule>,
    minecraftVersion: String,
    loaderName: String,
    installedModIds: Set<String>,
): RuleSelection {
    val candidates = rules.filter { rule ->
        rule.verified &&
                rule.minecraft.equals(minecraftVersion, ignoreCase = true) &&
                rule.loader.equals(loaderName, ignoreCase = true) &&
                rule.whenPresent.all { it in installedModIds }
    }

    //2. 真超集遮蔽
    val shadowed = candidates.filter { rule ->
        candidates.any { other ->
            other !== rule &&
                    other.whenPresent.toSet().containsAll(rule.whenPresent) &&
                    other.whenPresent.toSet() != rule.whenPresent.toSet()
        }
    }
    val live = candidates.filterNot { rule -> shadowed.any { it === rule } }

    //3. 相同范围：priority 决定；相同 priority 且内容不同 → ambiguous；内容完全相同的重复条目只留第一条
    val ambiguous = mutableListOf<ModDoctorSetRule>()
    val kept = mutableListOf<ModDoctorSetRule>()
    val lowered = mutableListOf<ModDoctorSetRule>()
    live.groupBy { it.whenPresent.toSet() }.values.forEach { group ->
        val top = group.maxOf { it.priority }
        val winners = group.filter { it.priority == top }
        lowered += group.filter { it.priority < top }
        val distinct = winners.distinctBy { it.allowed.mapValues { (_, v) -> v.baseModVersion() } }
        if (distinct.size == 1) kept += winners.first()
        else ambiguous += winners
        //被 priority 压下去的同范围规则：不生效，但也不算歧义
    }

    //4. 不同范围但对同一模组给出不同版本
    val conflicting = kept.filter { rule ->
        kept.any { other ->
            other !== rule && rule.allowed.any { (modId, version) ->
                other.allowed[modId]?.let { it.baseModVersion() != version.baseModVersion() } == true
            }
        }
    }
    ambiguous += conflicting
    val applied = kept.filterNot { rule -> conflicting.any { it === rule } }

    val order = rules.withIndex().associate { it.value to it.index }
    return RuleSelection(
        applied = applied.sortedBy { order[it] ?: 0 },
        shadowed = (shadowed + lowered).sortedBy { order[it] ?: 0 },
        ambiguous = ambiguous.distinct().sortedBy { order[it] ?: 0 },
    )
}

/**
 * 读取随包发布的规则文件。读不到时返回空列表：规则缺失只意味着少这一类检查，
 * 不能让启动路径失败。
 */
internal object ModDoctorRules {
    private const val ASSET_NAME = "mod_doctor_rules.json"

    @Volatile
    private var cached: List<ModDoctorSetRule>? = null

    fun load(): List<ModDoctorSetRule> {
        cached?.let { return it }
        val loaded = runCatching {
            val json = GlobalContext.applicationContext.assets.open(ASSET_NAME)
                .bufferedReader()
                .use { it.readText() }
            parseModDoctorRules(json)
        }.getOrElse { error ->
            Logger.warning(TAG, "The Mod Doctor rule file could not be read; continuing without set rules", error)
            emptyList()
        }
        cached = loaded
        return loaded
    }
}

/** 版本比较忽略构建元数据：已安装的 `0.9.1+mc1.21.11` 与规则里的 `0.9.1` 是同一个版本 */
internal fun String.baseModVersion(): String = substringBefore('+').trim()

/**
 * 这段版本号是否就是规则里写的那个版本。
 *
 * 同一个项目在两个地方写版本号的方式并不一致：模组 jar 自己声明的是 `0.8.12+mc1.21.11`，
 * 而 Modrinth 给这个项目的版本号实际写成 `mc1.21.11-0.8.12-fabric`。先按「去掉构建元数据后相等」比，
 * 再退到「作为一个完整版本片段出现」，这样 `0.8.12` 不会把 `0.8.120` 认成自己。
 * 精确相等优先，是为了让 `0.8.12` 不去瞄 `0.8.12-beta.2` 这种带后缀的写法。
 */
internal fun String.matchesModVersion(target: String): Boolean {
    val wanted = target.baseModVersion()
    if (wanted.isEmpty()) return false
    if (baseModVersion() == wanted) return true
    return Regex("(?<![0-9.])" + Regex.escape(wanted) + "(?![0-9.])").containsMatchIn(this)
}

/**
 * 评估已知兼容成套规则。
 *
 * 只有规则点名的模组全部已装才生效，因此「只装了 Sodium」不会触发任何四件套规则。
 * 版本读不到的模组保持沉默（读不出来就不下结论），允许集合里没列出的模组不参与判断。
 *
 * @return 每条被破坏的规则一个可修复问题；成套问题只报告**与目标版本不一致**的成员
 */
internal fun evaluateSetRules(
    mods: List<ModDoctorMetadata>,
    minecraftVersion: String,
    loader: ModLoader,
    rules: List<ModDoctorSetRule>,
): List<ModDoctorProblem> {
    val byId = mods.filter { it.readable && it.modId.isNotBlank() }.associateBy { it.modId }

    val selection = selectApplicableRules(rules, minecraftVersion, loader.displayName, byId.keys)
    return selection.applied.mapNotNull { rule ->
        //KNOWN_GOOD 只说明这一套验证过能用，不说明别的组合不能用：不一致不报告、不强制换版本
        if (rule.mode != ModDoctorRuleMode.ENFORCE) return@mapNotNull null

        val mismatched = rule.allowed.mapNotNull { (modId, target) ->
            val installed = byId[modId] ?: return@mapNotNull null
            val current = installed.version?.baseModVersion() ?: return@mapNotNull null
            if (current == target.baseModVersion()) null
            else ModDoctorSetMember(modId = modId, fileName = installed.fileName, targetVersion = target)
        }
        if (mismatched.isEmpty()) return@mapNotNull null

        val allowedText = rule.allowed.entries.joinToString(", ") { (id, version) -> "$id $version" }
        val wrongText = mismatched.joinToString(", ") { member -> "${member.fileName} is ${member.targetVersion}" }
        ModDoctorProblem(
            type = ModDoctorProblemType.INCOMPATIBLE_MOD_SET,
            fileName = mismatched.joinToString(", ") { it.fileName },
            modId = null,
            reason = "Installed versions do not match the known compatible set: $wrongText (known good: $allowedText).",
            reasonRes = R.string.mod_doctor_reason_incompatible_mod_set,
            reasonArgs = listOf(wrongText, allowedText),
            setFix = ModDoctorSetFix(members = mismatched, allowed = rule.allowed),
        )
    }
}
