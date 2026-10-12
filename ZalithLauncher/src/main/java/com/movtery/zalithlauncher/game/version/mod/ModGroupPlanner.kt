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
 * 成套修复的**计划**（Plan only：只计算，不下载、不安装、不改文件）。
 *
 * 输入全部是纯数据：已安装的模组、平台上查到的候选版本、已确认的规则或发现模式。
 * 输出说明每个文件要从什么版本换到什么版本、为什么、有什么风险、确定到什么程度。
 * 任何证据不足的地方都标成 [GroupPlanConfidence.UNVERIFIED]，不会把猜测当成结论。
 *
 * 有两条路：
 * - [planModGroupRepair]：按已确认的 Known-good 规则（[ModDoctorSetRule]，ผูก Minecraft + loader）
 * - [planModGroupDiscovery]：没有规则时，从平台数据发现；只提出“当前版本不支持目标 Minecraft/loader”的成员，
 *   结论的确定度最高为 MEDIUM（没有 Known-good 背书）
 */

/** 已安装模组的一个文件：`modId` 来自模组自己声明的 id，`version` 来自它的 jar 元数据 */
data class GroupPlanInstalled(
    val modId: String,
    val fileName: String,
    val version: String?,
)

/**
 * 平台上某个候选版本的元数据。
 * - [requiredModIds] / [incompatibleModIds]：已经映射成模组 id 的依赖与冲突
 * - [unresolvedRequiredProjects]：平台声明必需、但无法映射到已安装模组的项目；无法确认是否满足，计划必须 UNVERIFIED
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
    val unresolvedRequiredProjects: Set<String> = emptySet(),
)

enum class GroupPlanStatus {
    /** 已安装的版本都已符合要求，不需要改任何文件 */
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
    /** 当前计划可能带来的风险（仅列出可以确认的事实） */
    val risks: List<String>,
    /** 这个计划来自已确认的 Known-good 规则，还是从平台数据发现 */
    val fromKnownGoodRule: Boolean = false,
)

/**
 * 按已确认的规则计算成套修复计划。
 *
 * 规则：
 * 1. Minecraft/loader 与规则不符，或规则要求的模组没有**全部**安装 → 规则保持沉默，返回 NO_CHANGE。
 * 2. 已经符合规则版本的模组不动（最少改动）。
 * 3. 需要改的模组，只从 Minecraft 版本与加载器都匹配、且版本号等于规则允许版本的候选中选。
 * 4. 选出的候选必须通过 [checkConsistency]（依赖、冲突、无法映射的依赖）。
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
    if (!ruleApplies(rule, minecraftVersion, loader) || !ruleComplete(rule, installed)) return silent()

    val byModId = installed.groupBy { it.modId }
    val changes = mutableListOf<GroupPlanChange>()
    val notes = mutableListOf<String>()
    val chosen = mutableMapOf<String, GroupPlanCandidate>()

    for (modId in rule.whenPresent) {
        val target = rule.allowed[modId] ?: continue
        val current = byModId.getValue(modId).first()
        if (current.version?.matchesModVersion(target) == true) {
            notes += "$modId ${current.version} ตรงกับกฎ $target แล้ว ไม่แตะ"
            continue
        }
        if (isPackOwned(current.fileName)) return blockedPackOwned(current.fileName)
        val pick = candidatesByModId[modId].orEmpty()
            .filter { minecraftVersion in it.gameVersions && loader.lowercase() in it.loaders }
            .filter { it.versionName.baseModVersion() == target.baseModVersion() }
            .maxWithOrNull(versionOrder)
            ?: return unverified("ไม่พบ $modId $target ที่รองรับ $loader $minecraftVersion บนแพลตฟอร์ม", notes)
        chosen[modId] = pick
        changes += GroupPlanChange(
            modId = modId,
            fromFile = current.fileName,
            fromVersion = current.version,
            toFile = pick.fileName,
            toVersion = pick.versionName,
            projectId = pick.projectId,
            reason = "ตรงกับกฎที่ยืนยันแล้ว ($modId $target)",
        )
    }

    if (changes.isEmpty()) return GroupPlan(GroupPlanStatus.NO_CHANGE, GroupPlanConfidence.HIGH, emptyList(), notes, emptyList(), fromKnownGoodRule = true)

    checkConsistency(chosen.values.toList(), installed, notes)?.let { return unverified(it, notes) }

    val risks = listOf("ยังไม่ได้ทดสอบการเปิดเกมด้วยชุดนี้ ต้องทดสอบจริงก่อนถือว่าใช้ได้")
    val confidence = if (chosen.values.all { it.requiredModIds.isNotEmpty() || it.incompatibleModIds.isNotEmpty() })
        GroupPlanConfidence.HIGH else GroupPlanConfidence.MEDIUM
    return GroupPlan(GroupPlanStatus.PLAN, confidence, changes, notes, risks, fromKnownGoodRule = true)
}

/**
 * ไม่มี Known-good 规则 (หรือกฎไม่ตรงกับ Minecraft/loader นี้): จากข้อมูลของแพลตฟอร์มเอง
 * หาเฉพาะ “สมาชิกที่เวอร์ชันปัจจุบันไม่รองรับ Minecraft/loader นี้” แล้วเลือกเวอร์ชันใหม่สุดที่รองรับ
 * ไม่เปลี่ยนสมาชิกที่ยังรองรับอยู่ และไม่เดาเมื่อข้อมูลไม่พอ ผลสูงสุดคือ MEDIUM
 */
fun planModGroupDiscovery(
    minecraftVersion: String,
    loader: String,
    installed: List<GroupPlanInstalled>,
    familyModIds: Set<String>,
    candidatesByModId: Map<String, List<GroupPlanCandidate>>,
    isPackOwned: (String) -> Boolean = { false },
): GroupPlan {
    val members = installed.filter { it.modId in familyModIds }
    if (members.isEmpty()) return silent()

    val notes = mutableListOf<String>()
    val changes = mutableListOf<GroupPlanChange>()
    val chosen = mutableListOf<GroupPlanCandidate>()
    val lowerLoader = loader.lowercase()

    for (member in members) {
        val all = candidatesByModId[member.modId].orEmpty()
        val supported = all.filter { minecraftVersion in it.gameVersions && lowerLoader in it.loaders }
        val currentStillSupported = member.version != null &&
                supported.any { it.versionName.baseModVersion() == member.version.baseModVersion() }
        if (currentStillSupported) {
            notes += "${member.modId} ${member.version} ยังรองรับ $loader $minecraftVersion จึงไม่แตะ"
            continue
        }
        if (isPackOwned(member.fileName)) return blockedPackOwned(member.fileName)
        val pick = supported.maxWithOrNull(versionOrder)
            ?: return unverified("ไม่พบ ${member.modId} ที่รองรับ $loader $minecraftVersion บนแพลตฟอร์ม", notes)
        chosen += pick
        changes += GroupPlanChange(
            modId = member.modId,
            fromFile = member.fileName,
            fromVersion = member.version,
            toFile = pick.fileName,
            toVersion = pick.versionName,
            projectId = pick.projectId,
            reason = "เวอร์ชันเดิมไม่รองรับ $loader $minecraftVersion เลือกเวอร์ชันใหม่สุดที่รองรับ",
        )
    }

    if (changes.isEmpty()) return GroupPlan(GroupPlanStatus.NO_CHANGE, GroupPlanConfidence.HIGH, emptyList(), notes, emptyList())

    //สมาชิกที่ไม่เปลี่ยนก็ต้องอยู่ร่วมกับชุดใหม่ได้ ตรวจทั้งชุดที่จะอยู่หลังแก้
    val finalCandidates = chosen.toList()
    checkConsistency(finalCandidates, installed, notes)?.let { return unverified(it, notes) }

    val risks = listOf(
        "ชุดนี้ไม่ใช่ Known-good Set (ไม่มีกฎที่ยืนยันแล้วสำหรับ $loader $minecraftVersion)",
        "ยังไม่ได้ทดสอบการเปิดเกมด้วยชุดนี้ ต้องทดสอบจริงก่อนถือว่าใช้ได้",
    )
    return GroupPlan(GroupPlanStatus.PLAN, GroupPlanConfidence.MEDIUM, changes, notes, risks)
}

/**
 * ตรวจชุดที่จะอยู่หลังแก้: ทุกผู้สมัครต้องมีของที่ต้องพึ่งพร้อม ไม่ขัดกับม็อดที่ยังอยู่ และไม่มี dependency ที่ยังแมปไม่ได้
 * คืนเหตุผล (ถ้าไม่ผ่าน) หรือ null เมื่อผ่าน
 */
private fun checkConsistency(
    candidates: List<GroupPlanCandidate>,
    installed: List<GroupPlanInstalled>,
    notes: MutableList<String>,
): String? {
    val finalMods = mutableMapOf<String, String?>()
    for (inst in installed) finalMods[inst.modId] = inst.version
    for (cand in candidates) finalMods[cand.modId] = cand.versionName

    for (cand in candidates) {
        if (cand.unresolvedRequiredProjects.isNotEmpty()) {
            return "${cand.modId} ${cand.versionName} ต้องการโปรเจกต์ ${cand.unresolvedRequiredProjects.joinToString()} ที่ยังแมปเป็นม็อดในเครื่องไม่ได้ จึงตรวจไม่ได้"
        }
        for (req in cand.requiredModIds) {
            if (req !in finalMods) return "${cand.modId} ${cand.versionName} ต้องการ $req แต่ไม่มีในชุดที่เสนอ"
        }
        for (bad in cand.incompatibleModIds) {
            if (bad in finalMods) return "${cand.modId} ${cand.versionName} ประกาศว่าไม่เข้ากับ $bad ซึ่งยังอยู่ในชุด"
        }
    }
    return null
}

/** เรียงเวอร์ชันแบบตัวเลขต่อส่วน (1.10.7 > 1.9.0); ส่วนที่ไม่ใช่ตัวเลขเทียบแบบข้อความ */
internal val versionOrder: Comparator<GroupPlanCandidate> = Comparator { a, b ->
    compareVersionText(a.versionName.baseModVersion(), b.versionName.baseModVersion())
}

internal fun compareVersionText(a: String, b: String): Int {
    val pa = a.split('.', '-', '_')
    val pb = b.split('.', '-', '_')
    for (i in 0 until maxOf(pa.size, pb.size)) {
        val x = pa.getOrNull(i) ?: "0"
        val y = pb.getOrNull(i) ?: "0"
        val nx = x.toIntOrNull()
        val ny = y.toIntOrNull()
        val c = if (nx != null && ny != null) nx.compareTo(ny) else x.compareTo(y)
        if (c != 0) return c
    }
    return 0
}

private fun ruleApplies(rule: ModDoctorSetRule, minecraftVersion: String, loader: String) =
    rule.minecraft.equals(minecraftVersion, ignoreCase = true) && rule.loader.equals(loader, ignoreCase = true)

private fun ruleComplete(rule: ModDoctorSetRule, installed: List<GroupPlanInstalled>): Boolean {
    val present = installed.map { it.modId }.toSet()
    return rule.whenPresent.all { it in present }
}

private fun silent() = GroupPlan(GroupPlanStatus.NO_CHANGE, GroupPlanConfidence.HIGH, emptyList(), emptyList(), emptyList())

private fun blockedPackOwned(fileName: String) = GroupPlan(
    status = GroupPlanStatus.BLOCKED_PACK_OWNED,
    confidence = GroupPlanConfidence.UNVERIFIED,
    changes = emptyList(),
    notes = listOf("$fileName เป็นไฟล์ที่ม็อดแพ็กจัดการ ต้องได้รับอนุญาตก่อนแก้"),
    risks = emptyList(),
)

private fun unverified(reason: String, notes: List<String>) = GroupPlan(
    status = GroupPlanStatus.UNVERIFIED,
    confidence = GroupPlanConfidence.UNVERIFIED,
    changes = emptyList(),
    notes = notes + reason,
    risks = emptyList(),
)
