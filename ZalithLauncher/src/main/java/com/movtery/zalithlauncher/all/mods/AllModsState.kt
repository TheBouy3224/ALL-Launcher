package com.movtery.zalithlauncher.all.mods

import java.io.File

/**
 * ALL 自己的模组条目。
 *
 * 只保存界面要显示与操作的数据，数据来源全部是 Zalith 既有的模组读取结果
 * （[com.movtery.zalithlauncher.game.version.mod.AllModReader] / LocalMod），
 * 这里不解析任何模组格式，也不重复实现加载器或依赖判断。
 */
data class AllModItem(
    /** 真实文件。启用/禁用是重命名同一个文件，删除也是删它 */
    val file: File,
    /** 文件名（含 .jar，禁用时含 .disabled） */
    val fileName: String,
    /** 模组显示名；非模组文件时等于文件名 */
    val name: String,
    val id: String,
    val version: String?,
    /** 加载器显示名，未知时为空串 */
    val loaderName: String,
    val sizeBytes: Long,
    val enabled: Boolean,
    /** Zalith 判定为“不是模组”的文件（例如放了别的 jar） */
    val notMod: Boolean
)
