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
 * 成套修复的**计划**（Phase A：只计算，不下载、不改文件）。
 *
 * 输入全部是纯数据：已安装的模组、平台上查到的候选版本、已确认的 [ModDoctorSetRule]。
 * 输出说明每个文件要从什么版本换到什么版本、为什么、有什么风险、以及确定到什么程度。
 * 任何证据不足的地方都标成 [GroupPlanConfidence.UNVERIFIED]，不会把猜测当成结论。
 */

/** 已安装模组的一个文件：`modId` 来自模组自己声明的 id，`version` 来自它的 jar 元数据 */
data class GroupPlanInstalled(
    val modId: String,
    val fileName: String,
    val version: String?,
)

/**
 * 平台上某个候选版本的元数据。
 * [modId] 是模组内部 id（候选若无法映射成 id，就不能参与计划，见 [planModGroupRepair]）。
 */
data class GroupPlanCandidate(
    val modId: String,
    val projectId: String,
    val versionName: String,
    val fileName: String,
    val gameVersions: Set<String>,
    val loaders: Set<String>,
    val requiredModIds: Set<String>,
    val incompatibleModIds: Set<String>,
)

enum class GroupPlanStatus {
    /** 已安装的版本都已符合规则，不需要改任何文件 */
    NO_CHANGE,
    /** 找到一组可以互相兼容的版本，可以展示给用户确认 */
    PLAN,
    /** 证据不足，无法给出安全的计划；必须展示原因，不能假装有计划 */
    UNVERIFIED,
    /** 计划会改到整合包自己管理的文件；必须由用户明确授权，Phase A 不提供 */
    BLOCKED_PACK_OWNED,
}

enum class GroupPlanConfidence { HIGH, MEDIUM, UNVERIFIED }

data class GroupPlanChange(
    val modId: String,
    val fromFile: String,
    val fromVersion: String?,
    val toFile: String,
    val toVersion: String,
    val projectId: String,
    val reason: String,
)

data class GroupPlan(
    val status: GroupPlanStatus,
    val confidence: GroupPlanConfidence,
    val changes: List<GroupPlanChange>,
    /** 不改动但需要用户知道的事（例如某个成员已经符合、或某个候选被排除的原因） */
    val notes: List<String>,
    /** 当前规则下，计划可能带来的风险（仅列出可以确认的事实） */
    val risks: List<String>,
)

/**
 * 计算某条已确认规则的成套修复计划。
 *
 * 规则：
 * 1. 规则要求的模组没有**全部**安装 → 规则保持沉默，返回 [GroupPlanStatus.NO_CHANGE]。
 * 2. 已经符合规则版本的模组不动（最少改动）。
 * 3. 需要改的模组，只从 Minecraft 版本与加载器都匹配、且版本号等于规则允许版本的候选中选。
 * 4. 选出的候选之间要满足：候选声明的必需模组要么已安装且不在换掉列表里，要么也在这组里；
 *    候选声明的不兼容模组不能同时存在。任何一条不满足 → UNVERIFIED。
 * 5. 整合包自带的文件不能被改动（[isPackOwned]）；碰到就返回 BLOCKED_PACK_OWNED。
 */
fun planModGroupRepair(
    minecraftVersion: String,
    loader: String,
    installed: List<GroupPlanInstalled>,
    rule: ModDoctorSetRule,
    candidatesByModId: Map<String, List<GroupPlanCandidate>>,
    isPackOwned: (String) -> Boolean = { false },
): GroupPlan {
    if (!rule.minecraft.equals(minecraftVersion, ignoreCase = true) ||
        !rule.loader.equals(loader, ignoreCase = true)
    ) return silent()

    val byModId = installed.groupBy { it.modId }
    //规则只在整组都存在时生效；缺任何一个都保持沉默（与现有规则语义一致）
    if (rule.whenPresent.any { it !in byModId }) return silent()

    val changes = mutableListOf<GroupPlanChange>()
    val notes = mutableListOf<String>()
    val risks = mutableListOf<String>()
    val chosen = mutableMapOf<String, GroupPlanCandidate>()

    for (modId in rule.whenPresent) {
        val target = rule.allowed[modId] ?: continue
        val current = byModId.getValue(modId).first()
        if (current.version?.matchesModVersion(target) == true) {
            notes += "$modId ${current.version} ตรงกับกฎ $target แล้ว ไม่แตะ"
            continue
        }
        if (isPackOwned(current.fileName)) {
            return GroupPlan(
                status = GroupPlanStatus.BLOCKED_PACK_OWNED,
                confidence = GroupPlanConfidence.UNVERIFIED,
                changes = emptyList(),
                notes = listOf("${current.fileName} เป็นไฟล์ที่ม็อดแพ็กจัดการ ต้องได้รับอนุญาตก่อนแก้"),
                risks = emptyList(),
            )
        }
        val pick = candidatesByModId[modId].orEmpty()
            .filter { minecraftVersion in it.gameVersions && loader.lowercase() in it.loaders.map { l -> l.lowercase() } }
            .filter { it.versionName.baseModVersion() == target.baseModVersion() }
            .maxByOrNull { it.versionName } //ไม่มีตัวเลือกเชิงลำดับจริง จึงเลือกแบบคงที่เพื่อให้ผลซ้ำได้
        if (pick == null) {
            return unverified("ไม่พบ $modId $target ที่รองรับ $loader $minecraftVersion บนแพลตฟอร์ม", notes)
        }
        chosen[modId] = pick
        changes += GroupPlanChange(
            modId = modId,
            fromFile = current.fileName,
            fromVersion = current.version,
            toFile = pick.fileName,
            toVersion = pick.versionName,
            projectId = pick.projectId,
            reason = "ตรงกับกฎที่ยืนยันแล้ว ($modId ${target})",
        )
    }

    if (changes.isEmpty()) return GroupPlan(GroupPlanStatus.NO_CHANGE, GroupPlanConfidence.HIGH, emptyList(), notes, risks)

    //ตรวจความสอดคล้องของชุดใหม่ก่อนเสนอ: ทุกตัวต้องมีของที่ต้องพึ่งพร้อม และไม่ขัดกันเอง
    val finalMods = mutableMapOf<String, String?>() //modId -> version
    for (inst in installed) finalMods[inst.modId] = inst.version
    for ((modId, cand) in chosen) finalMods[modId] = cand.versionName

    for (cand in chosen.values) {
        for (req in cand.requiredModIds) {
            if (req !in finalMods) return unverified("${cand.modId} ${cand.versionName} ต้องการ $req แต่ไม่มีในชุดที่เสนอ", notes)
        }
        for (bad in cand.incompatibleModIds) {
            if (bad in finalMods) return unverified("${cand.modId} ${cand.versionName} ประกาศว่าไม่เข้ากับ $bad ซึ่งยังอยู่ในชุด", notes)
        }
    }

    if (chosen.values.none { it.requiredModIds.isNotEmpty() }) {
        risks += "ข้อมูลความสัมพันธ์ของม็อดบางตัวไม่ได้ประกาศไว้ จึงตรวจได้เฉพาะเงื่อนไขที่ระบุ"
    }
    val confidence = if (chosen.values.all { it.requiredModIds.isNotEmpty() || it.incompatibleModIds.isNotEmpty() })
        GroupPlanConfidence.HIGH else GroupPlanConfidence.MEDIUM
    risks += "ยังไม่ได้ทดสอบการเปิดเกมด้วยชุดนี้ ต้องทดสอบจริงก่อนถือว่าใช้ได้"

    return GroupPlan(GroupPlanStatus.PLAN, confidence, changes, notes, risks)
}

private fun silent() = GroupPlan(GroupPlanStatus.NO_CHANGE, GroupPlanConfidence.HIGH, emptyList(), emptyList(), emptyList())

private fun unverified(reason: String, notes: List<String>) = GroupPlan(
    status = GroupPlanStatus.UNVERIFIED,
    confidence = GroupPlanConfidence.UNVERIFIED,
    changes = emptyList(),
    notes = notes + reason,
    risks = emptyList(),
)
