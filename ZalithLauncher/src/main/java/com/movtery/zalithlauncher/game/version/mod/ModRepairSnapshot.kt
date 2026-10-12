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
import java.io.File
import java.security.MessageDigest

/**
 * 修复前快照与用户触发的回滚（Phase B：文件安全）。
 *
 * 目标：任何修复动作之前，mods 目录里的**每一个顶层文件**都先被复制并核对 SHA-1，写进持久化的 manifest
 * （实例目录 `ALLLauncher/repair-backups/<id>/`）。这样：
 * - 修复中途失败、应用被杀、或之后用户想撤销，都能按 manifest 恢复；
 * - 复制不成功、校验不一致、体积超限 → 不开始修复（宁可不修，也不在没有备份的情况下改文件）。
 *
 * 回滚的铁律：**不删除、不覆盖** 修复之后被用户改过的文件。
 * - 需要移走的文件（修复新增的、或替换掉原文件的）一律移到快照的 `quarantine/`，不是删除；
 * - 当前内容与“修复刚完成时记录的 SHA-1”不一致 → 视为用户之后改过，跳过并报告冲突。
 *
 * 本文件只做文件与 manifest 的纯 IO，不依赖 Android，可以直接用 JVM 单元测试。
 */
internal data class SnapshotFile(
    val name: String,
    val sha1: String,
    val size: Long,
)

internal enum class SnapshotStatus {
    /** 已备份，修复进行中（若应用在此时被杀，下次可当作“被中断”来回滚） */
    PENDING,
    /** 修复完成并改动了文件 */
    APPLIED,
    /** 修复中途失败但已经有文件被改动 */
    PARTIAL,
    /** 修复没有改动任何文件（快照与现状一致） */
    NO_CHANGE,
    /** 上次修复过程中应用被中断，已按当时的现状记录 */
    INTERRUPTED,
    /** 已回滚 */
    ROLLED_BACK,
    /** 回滚完成了一部分，另一部分因冲突被跳过 */
    ROLLED_BACK_PARTIAL,
}

internal data class RepairSnapshot(
    val id: String,
    val createdAt: Long,
    val modsDirPath: String,
    val before: List<SnapshotFile>,
    val status: SnapshotStatus,
    /** 修复刚结束时 mods 目录的状态；回滚用它判断“用户之后有没有改过” */
    val after: List<SnapshotFile>? = null,
    /** 回滚时因冲突而没有触碰的文件 */
    val conflicts: List<String> = emptyList(),
    /** 回滚时移走（不是删除）的文件，对应快照 `quarantine/` 里的同名文件 */
    val quarantined: List<String> = emptyList(),
)

internal sealed interface SnapshotCreateResult {
    data class Created(val snapshot: RepairSnapshot) : SnapshotCreateResult
    data class Refused(val reason: String) : SnapshotCreateResult
}

internal data class RollbackResult(
    val snapshot: RepairSnapshot,
    val restored: List<String>,
    val quarantined: List<String>,
    val conflicts: List<String>,
    val failure: String? = null,
) {
    val complete: Boolean get() = failure == null && conflicts.isEmpty()
}

internal object ModRepairSnapshots {
    private const val DIR = "ALLLauncher/repair-backups"
    private const val MANIFEST = "manifest.json"
    private const val FILES = "files"
    private const val QUARANTINE = "quarantine"
    const val DEFAULT_MAX_BYTES: Long = 1L * 1024 * 1024 * 1024

    private val gson = Gson()

    fun root(versionPath: File): File = File(versionPath, DIR)

    fun sha1(file: File): String = MessageDigest.getInstance("SHA-1").let { md ->
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                md.update(buffer, 0, read)
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    }

    /** mods 目录里的顶层普通文件（包含 `.disabled`），按文件名排序，保证结果可重复 */
    fun listing(modsDir: File): List<SnapshotFile> =
        modsDir.listFiles { f -> f.isFile }.orEmpty()
            .sortedBy { it.name }
            .map { SnapshotFile(it.name, sha1(it), it.length()) }

    /**
     * 创建快照：复制每个顶层文件到 `files/`，复制后重新核对 SHA-1，最后才写 manifest。
     * 任何一步失败都只清理这次快照自己的目录，并返回 [SnapshotCreateResult.Refused]（调用方必须停止修复）。
     */
    fun create(
        versionPath: File,
        modsDir: File,
        now: Long = System.currentTimeMillis(),
        maxBytes: Long = DEFAULT_MAX_BYTES,
    ): SnapshotCreateResult {
        if (!modsDir.isDirectory) return SnapshotCreateResult.Refused("mods directory is missing")
        val before = runCatching { listing(modsDir) }.getOrElse {
            return SnapshotCreateResult.Refused("could not read the mods directory: ${it.message}")
        }
        val total = before.sumOf { it.size }
        if (total > maxBytes) {
            return SnapshotCreateResult.Refused("mods directory is $total bytes, above the $maxBytes byte backup limit")
        }

        val id = "$now"
        val dir = File(root(versionPath), id)
        if (dir.exists()) return SnapshotCreateResult.Refused("snapshot $id already exists")
        val filesDir = File(dir, FILES)
        return try {
            if (!filesDir.mkdirs()) return SnapshotCreateResult.Refused("could not create the backup directory")
            for (entry in before) {
                val source = File(modsDir, entry.name)
                val copy = File(filesDir, entry.name)
                source.copyTo(copy, overwrite = false)
                if (sha1(copy) != entry.sha1) {
                    discardIncomplete(dir)
                    return SnapshotCreateResult.Refused("backup copy of ${entry.name} did not match its SHA-1")
                }
            }
            val snapshot = RepairSnapshot(id, now, modsDir.absolutePath, before, SnapshotStatus.PENDING)
            write(dir, snapshot)
            SnapshotCreateResult.Created(snapshot)
        } catch (e: Throwable) {
            discardIncomplete(dir)
            SnapshotCreateResult.Refused("could not create the backup: ${e.message ?: e::class.java.simpleName}")
        }
    }

    /**
     * 修复结束后记录结果。现状与快照一致 → [SnapshotStatus.NO_CHANGE]；有差异则按 [succeeded] 记 APPLIED / PARTIAL。
     * @return 更新后的快照
     */
    fun finalize(versionPath: File, snapshot: RepairSnapshot, modsDir: File, succeeded: Boolean): RepairSnapshot {
        val after = listing(modsDir)
        val status = when {
            after == snapshot.before -> SnapshotStatus.NO_CHANGE
            succeeded -> SnapshotStatus.APPLIED
            else -> SnapshotStatus.PARTIAL
        }
        val updated = snapshot.copy(status = status, after = after)
        write(File(root(versionPath), snapshot.id), updated)
        return updated
    }

    /** 读取全部快照，最新的在前；读不出来的目录被跳过 */
    fun all(versionPath: File): List<RepairSnapshot> =
        root(versionPath).listFiles { f -> f.isDirectory }.orEmpty()
            .mapNotNull { read(it) }
            .sortedByDescending { it.createdAt }

    /**
     * 最新一份“可以回滚”的快照。上次修复被中断（manifest 还是 PENDING）时，
     * 按当时 mods 目录的现状补记 after，并标成 [SnapshotStatus.INTERRUPTED]。
     */
    fun latestRollbackable(versionPath: File, modsDir: File): RepairSnapshot? {
        val latest = all(versionPath).firstOrNull { it.status != SnapshotStatus.NO_CHANGE } ?: return null
        val candidate = if (latest.status == SnapshotStatus.PENDING) {
            val updated = latest.copy(status = SnapshotStatus.INTERRUPTED, after = listing(modsDir))
            write(File(root(versionPath), latest.id), updated)
            updated
        } else latest
        return candidate.takeIf {
            it.status == SnapshotStatus.APPLIED || it.status == SnapshotStatus.PARTIAL ||
                    it.status == SnapshotStatus.INTERRUPTED || it.status == SnapshotStatus.ROLLED_BACK_PARTIAL
        }
    }

    /**
     * 回滚到快照记录的修复前状态。绝不删除、绝不覆盖：
     * - 修复新增的文件、被修复改过内容的文件：只有当前 SHA-1 等于修复刚结束时记录的值才会被移到 `quarantine/`；
     * - 修复前存在、现在缺失的文件：从快照复制回来（复制后核对 SHA-1），目标已存在则视为冲突；
     * - 当前内容与 after 记录不一致（用户之后改过）→ 跳过并报告冲突。
     */
    fun rollback(versionPath: File, snapshot: RepairSnapshot, modsDir: File): RollbackResult {
        val after = snapshot.after
            ?: return RollbackResult(snapshot, emptyList(), emptyList(), emptyList(), "the snapshot has no recorded after-state")
        val dir = File(root(versionPath), snapshot.id)
        val filesDir = File(dir, FILES)
        val quarantineDir = File(dir, QUARANTINE)

        val beforeByName = snapshot.before.associateBy { it.name }
        val afterByName = after.associateBy { it.name }
        val current = runCatching { listing(modsDir).associateBy { it.name } }.getOrElse {
            return RollbackResult(snapshot, emptyList(), emptyList(), emptyList(), "could not read the mods directory: ${it.message}")
        }

        val restored = mutableListOf<String>()
        val quarantined = mutableListOf<String>()
        val conflicts = mutableListOf<String>()

        fun quarantine(name: String): Boolean {
            quarantineDir.mkdirs()
            val target = File(quarantineDir, name)
            if (target.exists()) return false
            return runCatching { File(modsDir, name).toPath().let { java.nio.file.Files.move(it, target.toPath()) } }.isSuccess
        }

        //先处理“现在有、但修复前不是这样”的文件：移走为前提，原文件才有位置可以回来
        for ((name, now) in current) {
            val was = beforeByName[name]
            if (was != null && was.sha1 == now.sha1) continue
            val unchangedSinceRepair = afterByName[name]?.sha1 == now.sha1
            if (!unchangedSinceRepair) { conflicts += name; continue }
            if (quarantine(name)) quarantined += name else conflicts += name
        }

        //再把修复前存在、现在缺失的文件复制回来
        for ((name, was) in beforeByName) {
            val target = File(modsDir, name)
            if (target.exists()) {
                if (sha1(target) != was.sha1 && name !in conflicts) conflicts += name
                continue
            }
            val source = File(filesDir, name)
            if (!source.isFile || sha1(source) != was.sha1) { conflicts += name; continue }
            val ok = runCatching { source.copyTo(target, overwrite = false) }.isSuccess && sha1(target) == was.sha1
            if (ok) restored += name else conflicts += name
        }

        val status = if (conflicts.isEmpty()) SnapshotStatus.ROLLED_BACK else SnapshotStatus.ROLLED_BACK_PARTIAL
        val updated = snapshot.copy(status = status, conflicts = conflicts.toList(), quarantined = quarantined.toList())
        write(dir, updated)
        return RollbackResult(updated, restored, quarantined, conflicts)
    }

    private fun write(dir: File, snapshot: RepairSnapshot) {
        dir.mkdirs()
        val tmp = File(dir, "$MANIFEST.tmp")
        tmp.writeText(gson.toJson(snapshot))
        java.nio.file.Files.move(
            tmp.toPath(), File(dir, MANIFEST).toPath(),
            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
        )
    }

    private fun read(dir: File): RepairSnapshot? = runCatching {
        gson.fromJson(File(dir, MANIFEST).readText(), RepairSnapshot::class.java)
            ?.takeIf { it.id.isNotBlank() && it.before != null }
    }.getOrNull()

    /** 只清理“这一次尚未完成的快照”自己创建的目录（里面全是我们的备份副本，不含用户文件） */
    private fun discardIncomplete(dir: File) {
        runCatching { dir.deleteRecursively() }
    }
}
