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
)

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
    return file?.compatibleSets.orEmpty().mapNotNull { raw ->
        val minecraft = raw.minecraft?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val loader = raw.loader?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val whenPresent = raw.whenPresent?.filter { it.isNotBlank() }.orEmpty()
        val allowed = raw.allowed.orEmpty().filterKeys { it.isNotBlank() }
        if (whenPresent.isEmpty() || allowed.isEmpty()) return@mapNotNull null
        ModDoctorSetRule(minecraft = minecraft, loader = loader, whenPresent = whenPresent, allowed = allowed)
    }
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

    return rules.mapNotNull { rule ->
        if (!rule.minecraft.equals(minecraftVersion, ignoreCase = true)) return@mapNotNull null
        if (!rule.loader.equals(loader.displayName, ignoreCase = true)) return@mapNotNull null
        if (!rule.whenPresent.all { byId.containsKey(it) }) return@mapNotNull null

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
