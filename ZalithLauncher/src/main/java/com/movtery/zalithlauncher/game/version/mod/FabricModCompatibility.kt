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
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.version.mod

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * 已启用模组在 Fabric 眼中的身份
 * @param id fabric.mod.json 里的 `id`
 * @param version fabric.mod.json 里的 `version`，读不到时为null
 * @param requires 它声明的 `depends`（不含运行环境条目）：依赖Id -> 必须同时满足的版本谓词
 * @param breaks 它声明的 `breaks`：目标Id -> 命中任一项即冲突的版本谓词
 */
internal data class InstalledModIdentity(
    val id: String,
    val version: String?,
    val requires: Map<String, List<String>> = emptyMap(),
    /** 已启用模组声明的 `breaks`，供替换候选做反向校验。 */
    val breaks: Map<String, List<String>> = emptyMap(),
    /** 这个身份对应的本地文件名；用于确定性顺序与“替换了哪一个文件”的判定，与兼容性判定无关。 */
    val fileName: String = "",
    /**
     * 这个文件额外提供、可供依赖解析的 id：内嵌模块（jar-in-jar）自己的 id 与它们的 `provides` 别名，
     * 以及顶层 `provides` 声明的别名。只解决「有没有装」，不产生版本。
     */
    val providedModIds: List<String> = emptyList(),
    /** 额外提供的 id 里、明确声明了版本的那些；读不出来时不登记，绝不借用外层文件的版本。 */
    val providedModVersions: Map<String, String> = emptyMap(),
)

/**
 * 候选模组文件自带的 Fabric 事实：身份、版本、breaks 与 depends
 */
internal data class FabricModFacts(
    val fileName: String,
    val id: String?,
    val version: String?,
    val breaks: Map<String, List<String>>,
    val requires: Map<String, List<String>>,
    /**
     * 这个文件额外提供的 id：内嵌模块自己的 id 与它们的 `provides` 别名，以及顶层 `provides` 别名。
     * 真正的加载器把它们登记成独立的模组，整组校验的“有没有提供者”必须看得见。
     */
    val providedModIds: List<String> = emptyList(),
    /** 额外提供的 id 里、明确声明了版本的那些（内嵌模块自己的版本、`provides` 里显式写出的版本）。 */
    val providedModVersions: Map<String, String> = emptyMap(),
)

/**
 * 内嵌模块（jar-in-jar）自己的身份
 *
 * @param id 内嵌 jar 的 `fabric.mod.json` 里声明的 id
 * @param version 内嵌 jar 自己声明的版本；读不出来时为null，绝不借用外层文件的版本
 * @param providedModIds 内嵌 jar 自己声明的 `provides` 别名
 * @param providedModVersions 别名里显式声明了版本的条目；没写版本的别名不登记版本
 */
internal data class FabricEmbeddedModule(
    val id: String,
    val version: String?,
    val providedModIds: List<String> = emptyList(),
    val providedModVersions: Map<String, String> = emptyMap(),
)

/**
 * 按 Fabric 的版本谓词语义判断 [predicate] 是否命中 [version]
 *
 * 支持的子集与 fabric-loader 的 `VersionPredicate` 一致的部分：
 * `*` 任意版本；`=`（裸版本同样按相等处理）`<` `<=` `>` `>=` `~`（同主次版本）`^`（同主版本）；
 * 段位里的 `x` / `*` 通配，位于结尾时覆盖该位及其以下（因此 `1.17.x` 命中 1.17，但不命中 1.18）；
 * 空格分隔的多个谓词为“并且”。
 * 比较时忽略 `+` 之后的构建元数据、`-` 之后的预发布段低于同号正式版本，
 * 因此带构建元数据的版本仍然命中针对它基础版本的上界声明（与 Fabric 自己的判定一致）。
 */
internal fun fabricVersionMatches(version: String?, predicate: String): Boolean {
    val target = version?.trim().orEmpty()
    if (target.isEmpty()) return false

    val tokens = predicate.trim().split(' ').filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return false
    return tokens.all { matchesToken(target, it) }
}

/**
 * 把版本谓词按 `||` 拆成可选分支
 *
 * `||` 是 fabric-loader 的 VersionPredicate 写法，例如 `0.5.9 || 0.5.11`：任一分支命中即命中。
 * 拆分只在这一处做，Minecraft 声明与已安装依赖声明的判定都从这里取分支。
 */
internal fun predicateAlternatives(predicate: String): List<String> =
    predicate.split("||").map { it.trim() }.filter { it.isNotEmpty() }

/**
 * 任一可选分支命中即命中；分支内部仍由共享求值器 [fabricVersionMatches] 判定，
 * 因此通配段（`x`）、比较运算符与空格分隔的“同时满足”语义都保持不变。
 */
internal fun fabricVersionPredicateMatches(version: String?, alternatives: List<String>): Boolean =
    alternatives.any { fabricVersionMatches(version, it) }

/**
 * 候选模组自带的 `breaks` 是否命中已启用模组
 * @param candidateFileName 候选文件名，仅用于生成原因
 * @param breaks 候选声明的 `breaks`：被声明的模组Id -> 版本谓词
 * @param enabledMods 当前已启用的模组
 * @return 命中时返回可直接展示的原因，未命中返回null
 */
internal fun candidateConflictReason(
    candidateFileName: String,
    breaks: Map<String, List<String>>,
    enabledMods: List<InstalledModIdentity>,
): String? {
    for (mod in enabledMods) {
        val predicates = breaks[mod.id]?.takeIf { it.isNotEmpty() } ?: continue
        if (predicates.any { fabricVersionMatches(mod.version, it) }) {
            val installed = mod.version?.takeIf { it.isNotBlank() } ?: "an unknown version"
            return "$candidateFileName declares that it breaks the installed ${mod.id} $installed"
        }
    }
    return null
}

/**
 * 解析 fabric.mod.json 里的 `breaks` 段
 * @return 被声明的模组Id -> 版本谓词列表；数组声明视为多个可选项
 */
internal fun parseFabricBreaks(json: String): Map<String, List<String>> {
    val root = runCatching { JsonParser.parseString(json) }.getOrNull()?.takeIf { it.isJsonObject }
        ?: return emptyMap()
    return parseBreaks(root.asJsonObject)
}

/**
 * 解析 fabric.mod.json 里的 `depends` 段（不含运行环境条目）
 * @return 依赖Id -> 必须同时满足的版本谓词
 */
internal fun parseFabricDepends(json: String): Map<String, List<String>> {
    val root = runCatching { JsonParser.parseString(json) }.getOrNull()?.takeIf { it.isJsonObject }
        ?: return emptyMap()
    return parseRequires(root.asJsonObject)
}

/**
 * 读取候选模组文件自带的 `breaks` 声明；读不出时视为没有声明
 */
internal fun readFabricBreaks(modFile: File): Map<String, List<String>> =
    readFabricModFacts(modFile).breaks

/**
 * 读取模组文件自带的 Fabric 事实；读不出时返回空身份，不把读取失败当成“兼容”
 *
 * 内嵌模块（`jars` 段里的 jar-in-jar）自己的 id 与 `provides` 别名一并读出来：真正的加载器把它们
 * 登记成独立的模组，只认外层文件 id 会让这些依赖看起来没有提供者，整组校验因此误报缺失。
 */
internal fun readFabricModFacts(modFile: File): FabricModFacts = runCatching {
    ZipFile(modFile).use { zip ->
        val entry = zip.getEntry(FABRIC_MOD_JSON)
            ?: return FabricModFacts(modFile.name, null, null, emptyMap(), emptyMap())
        val text = zip.getInputStream(entry).bufferedReader().use { it.readText() }
        parseFabricModFacts(modFile.name, text, readEmbeddedModules(zip, text))
    }
}.getOrElse { FabricModFacts(modFile.name, null, null, emptyMap(), emptyMap()) }

/**
 * 解析模组自带的 Fabric 事实
 *
 * @param embeddedModules 这个文件 `jars` 段里内嵌模块自己的身份；读不出来时传空列表
 */
internal fun parseFabricModFacts(
    fileName: String,
    json: String,
    embeddedModules: List<FabricEmbeddedModule> = emptyList(),
): FabricModFacts {
    val root = runCatching { JsonParser.parseString(json) }.getOrNull()?.takeIf { it.isJsonObject }
        ?: return FabricModFacts(fileName, null, null, emptyMap(), emptyMap())
    val objectRoot = root.asJsonObject
    val aliases = parseProvidedAliases(objectRoot)
    return FabricModFacts(
        fileName = fileName,
        id = objectRoot.stringMemberOrNull("id"),
        version = objectRoot.stringMemberOrNull("version"),
        breaks = parseBreaks(objectRoot),
        requires = parseRequires(objectRoot),
        providedModIds = providedIdsOf(embeddedModules, aliases),
        //只有内嵌模块自己声明的版本、以及 `provides` 条目里显式写出的版本才登记；
        //外层文件自己的版本永远不借给这些 id，读不出来就保持沉默。
        providedModVersions = providedVersionsOf(embeddedModules, aliases),
    )
}

/**
 * 读出这个文件 `jars` 段里每一个内嵌模块自己的身份
 *
 * 内嵌 jar 读不出来时按“没有这个模块”处理，不影响外层文件自己的事实。与权威元数据路径
 * （[ModDoctorMetadata.providedModIds]）共用同一套解析，避免两处对同一份文件给出不同答案。
 */
internal fun readEmbeddedModules(zip: ZipFile, rootJson: String): List<FabricEmbeddedModule> =
    embeddedJarEntryNames(rootJson).mapNotNull { entryName ->
        val entry = zip.getEntry(entryName) ?: return@mapNotNull null
        runCatching { zip.getInputStream(entry).use { input -> parseEmbeddedModule(input) } }.getOrNull()
    }

/** 单独读取一个内嵌模块；[entryName] 是外层 jar 里指向内嵌 jar 的条目名 */
internal fun readEmbeddedModule(modFile: File, entryName: String): FabricEmbeddedModule? = runCatching {
    ZipFile(modFile).use { zip ->
        val entry = zip.getEntry(entryName) ?: return@use null
        zip.getInputStream(entry).use { input -> parseEmbeddedModule(input) }
    }
}.getOrNull()

/**
 * 候选是否与已启用模组冲突
 *
 * 两个方向都要看，缺一不可：
 * 1. 候选自己声明“我破坏了某个已启用模组”
 * 2. 已启用模组声明“我要的依赖版本是某个范围”，而候选的版本不在其中
 *
 * @return 冲突原因，兼容时返回null
 */
internal fun candidateRejectionReason(
    candidate: FabricModFacts,
    enabledMods: List<InstalledModIdentity>,
): String? {
    candidateConflictReason(candidate.fileName, candidate.breaks, enabledMods)?.let { return it }
    installedBreaksCandidateReason(candidate, enabledMods)?.let { return it }
    return missingRequirementReason(candidate, enabledMods)
}

/** 已启用模组声明会破坏候选版本时给出原因（反向 `breaks` 校验）。 */
private fun installedBreaksCandidateReason(
    candidate: FabricModFacts,
    enabledMods: List<InstalledModIdentity>,
): String? {
    val candidateId = candidate.id?.takeIf { it.isNotBlank() } ?: return null
    val candidateVersion = candidate.version?.takeIf { it.isNotBlank() } ?: return null
    for (mod in enabledMods) {
        val predicates = mod.breaks[candidateId]?.takeIf { it.isNotEmpty() } ?: continue
        if (!fabricVersionPredicateMatches(candidateVersion, predicates)) continue
        val installedVersion = mod.version?.takeIf { it.isNotBlank() } ?: "an unknown version"
        return "${mod.id} $installedVersion declares that it breaks " +
                "$candidateId $candidateVersion (${candidate.fileName})"
    }
    return null
}

/**
 * 结构化的候选兼容性结论
 *
 * [reason] 是“breaks 方向”的硬拒绝原因：候选自己声明破坏某个已启用模组，或某个已启用模组声明的
 * `breaks` 命中候选。这一类问题换掉候选本身才能解决，不能靠连带替换依赖方。
 *
 * [blockingDependents] 只收集“requires 方向”上被候选卡住的已启用模组 id：它们声明的依赖版本不再被
 * 候选满足，因此可能需要连同它们一起替换。版本读不出来时保持沉默，不制造误报。
 *
 * [requiresReason] 是 [blockingDependents] 对应的第一条原因，供日志与诊断使用。
 */
internal data class FabricCandidateCompatibility(
    val reason: String? = null,
    val blockingDependents: List<String> = emptyList(),
    val requiresReason: String? = null,
)

/** requires 方向：声明“必须同时满足”的依赖版本不再被候选满足的已启用模组 id（按输入顺序，可复现） */
internal fun blockingDependentIds(
    candidateId: String?,
    candidateVersion: String?,
    enabledMods: List<InstalledModIdentity>,
): List<String> {
    val id = candidateId?.takeIf { it.isNotBlank() } ?: return emptyList()
    return enabledMods.filter { mod ->
        val predicates = mod.requires[id]?.takeIf { it.isNotEmpty() } ?: return@filter false
        installedDependencyVerdict(predicates, candidateVersion) == false
    }.map { it.id }
}

/**
 * 候选与已启用模组集合的结构化兼容性判定
 *
 * 原因文本与 [candidateRejectionReason] 取自同一套求值器；额外把“可以通过连带替换解决”的
 * requires 方向单独列出来，供多模组修复决定是否要一起替换依赖方。
 */
internal fun candidateCompatibility(
    candidate: FabricModFacts,
    enabledMods: List<InstalledModIdentity>,
): FabricCandidateCompatibility {
    candidateConflictReason(candidate.fileName, candidate.breaks, enabledMods)?.let { reason ->
        return FabricCandidateCompatibility(reason = reason)
    }
    installedBreaksCandidateReason(candidate, enabledMods)?.let { reason ->
        return FabricCandidateCompatibility(reason = reason)
    }
    return FabricCandidateCompatibility(
        blockingDependents = blockingDependentIds(candidate.id, candidate.version, enabledMods),
        requiresReason = missingRequirementReason(candidate, enabledMods),
    )
}

/**
 * 整组结果的兼容性校验：结果集合里每一条 `depends` 与 `breaks`（两个方向）都必须成立。
 *
 * 复用 [candidateRejectionReason] 同一套求值器与同一条“读不出来就保持沉默”的规则：
 * 版本未知或谓词读不懂时不得报错，只有明确不成立才返回原因。
 *
 * 依赖的“提供者”按真正的加载器算：文件自己的 id，加上它额外提供的 id（内嵌模块自己的 id 与别名、
 * 顶层 `provides` 别名）。额外提供的 id 只有明确声明了版本时才参与版本判定，读不出来时保持沉默。
 *
 * @return 第一个不成立的原因；整组自洽时返回null
 */
internal fun resultingSetRejection(mods: List<InstalledModIdentity>): String? {
    val byId = mods.filter { it.id.isNotBlank() }.groupBy { it.id }

    //breaks 两个方向：任何一条命中的声明都会让加载器拒绝这个组合
    for (source in mods) {
        for ((targetId, predicates) in source.breaks) {
            val target = byId[targetId].orEmpty().firstOrNull { it !== source } ?: continue
            val targetVersion = target.version?.takeIf { it.isNotBlank() } ?: continue
            if (versionPredicateDecision(predicates, targetVersion) != true) continue
            return "${source.id} declares that it breaks ${target.id} $targetVersion"
        }
    }

    //depends：每一条声明的依赖都必须由结果集合里的某个模组提供，且版本声明成立。
    //提供者不只是文件自己的 id：内嵌模块自己的 id 与顶层 `provides` 别名同样算数，
    //与真正的加载器一致（Fabric API 的模块就是这样被登记的）。
    val providers = resultingSetProviders(mods)
    for (mod in mods) {
        for ((dependencyId, predicates) in mod.requires) {
            val candidates = providers[dependencyId].orEmpty()
            if (candidates.isEmpty()) {
                return "${mod.id} requires $dependencyId, but the resulting set does not provide it"
            }
            if (candidates.any { version -> installedDependencyVerdict(predicates, version) != false }) continue
            //至少有一个提供者明确不满足才会走到这里；原因里的版本取最先登记且读得出的那一个
            val installed = candidates.asSequence()
                .mapNotNull { version -> version?.takeIf { it.isNotBlank() } }
                .firstOrNull() ?: "an unknown version"
            return "${mod.id} requires $dependencyId ${predicates.joinToString(" ")}, " +
                    "but the resulting set provides $installed"
        }
    }
    return null
}

/**
 * 结果集合的提供者索引：依赖 id -> 承担它的那些版本（读不出来记作null）
 *
 * 文件自己的 id 与它自己的版本先登记（原因文本里的版本沿用这个 id 的权威文件），
 * 随后登记它额外提供的 id：内嵌模块自己的 id 与别名、以及顶层 `provides` 别名。
 * 额外提供的 id 只有明确声明了版本才带版本，读不出来时保持 null，版本判定照旧“无法定论就沉默”，
 * 不拿外层文件的版本充数。
 */
private fun resultingSetProviders(mods: List<InstalledModIdentity>): Map<String, List<String?>> {
    val providers = LinkedHashMap<String, MutableList<String?>>()
    fun register(id: String, version: String?) {
        if (id.isBlank()) return
        providers.getOrPut(id) { mutableListOf() } += version
    }
    mods.forEach { mod -> register(mod.id, mod.version) }
    mods.forEach { mod ->
        mod.providedModIds.forEach { providedId -> register(providedId, mod.providedModVersions[providedId]) }
    }
    return providers
}

/** 暂存文件自带的 Fabric 事实转成已启用模组身份，供整组校验使用；读不出 id 时返回null */
internal fun FabricModFacts.asInstalledIdentity(): InstalledModIdentity? =
    id?.takeIf { it.isNotBlank() }?.let { modId ->
        InstalledModIdentity(
            id = modId,
            version = version,
            requires = requires,
            breaks = breaks,
            fileName = fileName,
            providedModIds = providedModIds,
            providedModVersions = providedModVersions,
        )
    }

/** 已启用模组要求的候选版本不被满足时给出原因 */
private fun missingRequirementReason(
    candidate: FabricModFacts,
    enabledMods: List<InstalledModIdentity>,
): String? {
    val candidateId = candidate.id?.takeIf { it.isNotBlank() } ?: return null
    for (mod in enabledMods) {
        val predicates = mod.requires[candidateId]?.takeIf { it.isNotEmpty() } ?: continue
        if (predicates.all { fabricVersionMatches(candidate.version, it) }) continue
        val installed = mod.version?.takeIf { it.isNotBlank() } ?: "an unknown version"
        val provided = candidate.version?.takeIf { it.isNotBlank() } ?: "an unknown version"
        return "${mod.id} $installed requires $candidateId ${predicates.joinToString(" ")}, " +
                "but ${candidate.fileName} provides $provided"
    }
    return null
}

private fun com.google.gson.JsonObject.stringMemberOrNull(name: String): String? =
    get(name)?.takeIf { it.isJsonPrimitive }?.asString

private fun parseBreaks(root: com.google.gson.JsonObject): Map<String, List<String>> =
    root.get("breaks")
        ?.takeIf { it.isJsonObject }
        ?.asJsonObject
        ?.toPredicateMap()
        ?: emptyMap()

private fun parseRequires(root: com.google.gson.JsonObject): Map<String, List<String>> =
    root.get("depends")
        ?.takeIf { it.isJsonObject }
        ?.asJsonObject
        ?.toPredicateMap()
        ?.filterKeys { it !in ENVIRONMENT_DEPENDENCY_IDS }
        ?: emptyMap()

/**
 * 顶层 `provides` 段：数组元素可以是 id 字符串，也可以是 `{"id": …, "version": …}`。
 * 只登记 id 与显式写在条目里的版本；别名绝不继承提供者自己的版本。
 */
private fun parseProvidedAliases(root: com.google.gson.JsonObject): ProvidedAliases {
    val element = root.get("provides")?.takeIf { it.isJsonArray } ?: return ProvidedAliases()
    val ids = mutableListOf<String>()
    val versions = mutableMapOf<String, String>()
    element.asJsonArray.forEach { entry ->
        when {
            entry.isJsonPrimitive -> entry.asString.takeIf { it.isNotBlank() }?.let { ids += it }
            entry.isJsonObject -> {
                val objectEntry = entry.asJsonObject
                val id = objectEntry.stringMemberOrNull("id")?.takeIf { it.isNotBlank() } ?: return@forEach
                ids += id
                objectEntry.stringMemberOrNull("version")?.takeIf { it.isNotBlank() }?.let { versions[id] = it }
            }
        }
    }
    return ProvidedAliases(ids.distinct(), versions)
}

/** 一个文件声明的 `provides`：别名 id 与其中显式带版本的条目 */
private class ProvidedAliases(
    val ids: List<String> = emptyList(),
    val versions: Map<String, String> = emptyMap(),
)

/** 这个文件额外提供的 id：内嵌模块自己的 id 与它们的别名、以及顶层 `provides` 声明的别名 */
private fun providedIdsOf(
    embeddedModules: List<FabricEmbeddedModule>,
    aliases: ProvidedAliases,
): List<String> = (embeddedModules.map { it.id } +
        embeddedModules.flatMap { it.providedModIds } +
        aliases.ids)
    .filter { it.isNotBlank() }
    .distinct()

/**
 * 额外提供的 id 里明确带版本的条目
 *
 * 只有内嵌模块自己声明的版本、以及 `provides` 条目里显式写出的版本才算数；
 * 外层文件自己的版本永远不借给这些 id，读不出来就保持沉默（版本规则照旧“无法定论就不报”）。
 */
private fun providedVersionsOf(
    embeddedModules: List<FabricEmbeddedModule>,
    aliases: ProvidedAliases,
): Map<String, String> {
    val versions = LinkedHashMap<String, String>()
    embeddedModules.forEach { module ->
        module.version?.takeIf { it.isNotBlank() }?.let { version ->
            if (module.id.isNotBlank() && module.id !in versions) versions[module.id] = version
        }
        module.providedModVersions.forEach { (id, version) ->
            if (id.isNotBlank() && id !in versions) versions[id] = version
        }
    }
    aliases.versions.forEach { (id, version) -> if (id !in versions) versions[id] = version }
    return versions
}

/** `jars` 段列出的内嵌 jar 条目名 */
private fun embeddedJarEntryNames(rootJson: String): List<String> {
    val root = runCatching { JsonParser.parseString(rootJson) }.getOrNull()?.takeIf { it.isJsonObject }
        ?: return emptyList()
    val jars = root.asJsonObject.get(FABRIC_JARS)?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
    return jars.mapNotNull { element ->
        element.takeIf { it.isJsonObject }?.asJsonObject
            ?.get(FABRIC_JAR_FILE_KEY)?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }
    }
}

/** 内嵌 jar 的输入流 -> 它自己的身份；没有可读的 `fabric.mod.json` 时返回null */
private fun parseEmbeddedModule(input: InputStream): FabricEmbeddedModule? = runCatching {
    ZipInputStream(input).use { nestedZip ->
        var entry = nestedZip.nextEntry
        while (entry != null) {
            if (entry.name == FABRIC_MOD_JSON) {
                return@use parseEmbeddedModuleJson(nestedZip.readBytes().toString(Charsets.UTF_8))
            }
            entry = nestedZip.nextEntry
        }
        null
    }
}.getOrNull()

/** 内嵌 jar 的 `fabric.mod.json`：只有它自己声明的 id / 版本 / `provides` 算数 */
private fun parseEmbeddedModuleJson(json: String): FabricEmbeddedModule? {
    val root = runCatching { JsonParser.parseString(json) }.getOrNull()?.takeIf { it.isJsonObject } ?: return null
    val objectRoot = root.asJsonObject
    val id = objectRoot.stringMemberOrNull("id")?.takeIf { it.isNotBlank() } ?: return null
    val aliases = parseProvidedAliases(objectRoot)
    return FabricEmbeddedModule(
        id = id,
        version = objectRoot.stringMemberOrNull("version")?.takeIf { it.isNotBlank() },
        providedModIds = aliases.ids.filter { it != id },
        providedModVersions = aliases.versions,
    )
}

/** Fabric 模组描述文件的固定名字与 `jars` 段的键 */
private const val FABRIC_MOD_JSON = "fabric.mod.json"
private const val FABRIC_JARS = "jars"
private const val FABRIC_JAR_FILE_KEY = "file"

private fun com.google.gson.JsonObject.toPredicateMap(): Map<String, List<String>> =
    entrySet()
        .mapNotNull { (id, value) -> value.asPredicateList()?.let { id to it } }
        .toMap()

/** 依赖声明里的运行环境条目：它们不是被修复的那个依赖项目 */
private val ENVIRONMENT_DEPENDENCY_IDS = setOf("minecraft", "fabricloader", "java", "forge", "neoforge")

private fun JsonElement.asPredicateList(): List<String>? = when {
    isJsonPrimitive -> listOf(asString)
    isJsonArray -> asJsonArray.mapNotNull { runCatching { it.asString }.getOrNull() }
    else -> null
}

private fun matchesToken(version: String, token: String): Boolean {
    if (token == "*") return true

    val operator = FABRIC_PREDICATE_OPERATORS.firstOrNull { token.startsWith(it) }
    val raw = (if (operator == null) token else token.removePrefix(operator)).trim()
    if (raw.isEmpty() || raw == "*") return true

    val wildcard = raw.hasWildcardSegment()
    if (wildcard && (operator == null || operator == "=")) return wildcardMatches(version, raw)

    val comparison = compareFabricVersions(version, if (wildcard) raw.withWildcardSegmentsAsZero() else raw)
    return when (operator ?: "=") {
        "=" -> comparison == 0
        ">=" -> comparison >= 0
        "<=" -> comparison <= 0
        ">" -> comparison > 0
        "<" -> comparison < 0
        "~" -> comparison >= 0 && sharesPrefix(version, raw, segments = 2)
        "^" -> comparison >= 0 && sharesPrefix(version, raw, segments = 1)
        else -> false
    }
}

/** 比较时忽略 `+` 构建元数据，`-` 之后的预发布段单独比较 */
private fun compareFabricVersions(left: String, right: String): Int {
    val (leftBase, leftPreRelease) = splitVersion(left)
    val (rightBase, rightPreRelease) = splitVersion(right)

    val baseSize = maxOf(leftBase.size, rightBase.size)
    for (index in 0 until baseSize) {
        val comparison = comparePart(leftBase.getOrNull(index), rightBase.getOrNull(index))
        if (comparison != 0) return comparison
    }

    if (leftPreRelease.isEmpty() && rightPreRelease.isEmpty()) return 0
    if (leftPreRelease.isEmpty()) return 1
    if (rightPreRelease.isEmpty()) return -1

    val preReleaseSize = maxOf(leftPreRelease.size, rightPreRelease.size)
    for (index in 0 until preReleaseSize) {
        val comparison = comparePart(leftPreRelease.getOrNull(index), rightPreRelease.getOrNull(index))
        if (comparison != 0) return comparison
    }
    return 0
}

private fun splitVersion(version: String): Pair<List<String>, List<String>> {
    val withoutBuild = version.substringBefore('+')
    val base = withoutBuild.substringBefore('-')
    val preRelease = withoutBuild.substringAfter('-', "")
    return base.split('.').filter { it.isNotEmpty() } to
            preRelease.split('.').filter { it.isNotEmpty() }
}

/** 缺段视为更小；数字段高于同位置的非数字段 */
private fun comparePart(left: String?, right: String?): Int {
    if (left == null) return if (right == null) 0 else -1
    if (right == null) return 1

    val leftNumber = left.toBigIntegerOrNull()
    val rightNumber = right.toBigIntegerOrNull()
    return when {
        leftNumber != null && rightNumber != null -> leftNumber.compareTo(rightNumber)
        leftNumber != null -> 1
        rightNumber != null -> -1
        else -> left.compareTo(right)
    }
}

private fun String.isWildcardSegment(): Boolean =
    this == "*" || equals("x", ignoreCase = true)

private fun String.hasWildcardSegment(): Boolean =
    split('.').any { it.isWildcardSegment() }

private fun String.withWildcardSegmentsAsZero(): String =
    split('.').joinToString(".") { if (it.isWildcardSegment()) "0" else it }

private fun wildcardMatches(version: String, pattern: String): Boolean {
    val versionBase = splitVersion(version).first
    val patternSegments = splitVersion(pattern).first
    //结尾的通配段表示“这一位及其以下”，所以声明 `1.17.x` 也命中没有这一位的 1.17；
    //`1.*.5` 这类非结尾通配仍然要求版本必须有对应的位。
    val requiredSegments = patternSegments.indexOfLast { !it.isWildcardSegment() } + 1
    if (versionBase.size < requiredSegments) return false
    return patternSegments.withIndex().all { (index, segment) ->
        segment.isWildcardSegment() || comparePart(versionBase.getOrNull(index), segment) == 0
    }
}

private fun sharesPrefix(version: String, raw: String, segments: Int): Boolean {
    val versionBase = splitVersion(version).first
    val rawBase = splitVersion(raw).first
    for (index in 0 until segments) {
        if (comparePart(versionBase.getOrNull(index), rawBase.getOrNull(index)) != 0) return false
    }
    return true
}

/** 长的写在前面，避免把 `>=` 误判成 `>` */
private val FABRIC_PREDICATE_OPERATORS = listOf(">=", "<=", "~", "^", ">", "<", "=")
