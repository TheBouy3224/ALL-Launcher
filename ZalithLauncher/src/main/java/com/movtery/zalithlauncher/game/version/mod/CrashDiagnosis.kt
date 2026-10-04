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

import java.io.File
import java.io.RandomAccessFile

/**
 * 启动成功标记：加载器自己在客户端初始化阶段写下的那一行。
 *
 * 来源：Minecraft 客户端的 SoundEngine（`[Render thread/INFO]: Sound engine started`），
 * 出现时机在客户端已经初始化、标题画面还没画出来之前，所以它只能说明
 * 「这次会话走到了客户端初始化完成」，**不能**说成「进了主菜单」。
 */
const val LAUNCH_REACHED_MARKER = "Sound engine started"

/**
 * 崩溃标记：Minecraft 自己的崩溃处理器写下的那一行。
 *
 * 来源：`net.minecraft.CrashReport` 保存报告后输出的 `#@!@# Game crashed! Crash report saved to: #@!@#`，
 * 后面跟着报告文件路径。
 */
const val GAME_CRASH_MARKER = "#@!@# Game crashed! Crash report saved to: #@!@#"

/**
 * 读取捕获日志时最多看多少字节。
 *
 * 崩溃证据（崩溃标记、特征行）永远写在文件的末尾，所以只读尾巴；上限存在的意义是
 * 崩溃页面永远不会因为一个失控大小的文件把内存吃光。被截断时最坏的结果是「诊断不出原因」，
 * 而不是「诊断出错误的原因」。
 */
const val CAPTURED_LOG_TAIL_LIMIT_BYTES: Long = 1024L * 1024L

/**
 * 这次启动会话的结局。名字就是它字面的意思，界面上的说法必须与之对应。
 */
enum class LaunchOutcome {
    /** (A) 走到了客户端初始化完成，但进程以非零码结束 */
    REACHED_MARKER,

    /** (B) 走到了客户端初始化完成，然后正常退出 */
    NORMAL_EXIT_AFTER_MARKER,

    /** (C) 在启动阶段就失败了 */
    FAILED_DURING_STARTUP,

    /** (D) 崩溃了（加载器/游戏自己写了崩溃标记） */
    CRASHED_UNEXPECTEDLY,
}

/**
 * 命中的一条特征（一次诊断最多一条）。
 *
 * @param ownerModId 从日志里点名到的模组 id；取不到时为 null，此时不允许做任何动作
 */
data class CrashFinding(
    val signatureId: String,
    val diagnosisType: String,
    val confidence: DiagnosisConfidence,
    val ownerModId: String?,
    val action: DiagnosisAction,
    val messageKey: String,
)

/**
 * 对一段捕获日志的判定结果。
 *
 * @param confidence 对**原因**的把握。没有命中任何特征时是 LOW：知道它失败了，但说不出为什么
 * @param finding 命中的特征；没有则为 null
 */
data class CrashOutcome(
    val launchOutcome: LaunchOutcome,
    val confidence: DiagnosisConfidence,
    val finding: CrashFinding?,
)

/**
 * 判定一段捕获日志，以及它是否指认了某条已知特征。
 *
 * 纯函数：不读文件、不碰 Android，回归测试可以直接喂字符串。
 *
 * 判定顺序就是产品承诺的顺序，不能调换：
 * 1. 有崩溃标记 → (D)。这是游戏自己承认崩溃，压过下面全部判断
 * 2. 有成功标记且退出码为 0 → (B)
 * 3. 有成功标记 → (A)
 * 4. 命中了 loader_fatal 特征 → (C)（与规则 5 是同一个结论，区别只在有没有一条特征能说明原因）
 * 5. 退出码非 0 → (C)，LOW，且只能说「未归类的启动失败」
 * 6. 其余 → (C)，LOW
 *
 * 绝不默认成 (D)：把一次安静的启动失败说成崩溃，会把用户引向错误的排查方向。
 * 注意 `recommends any version of X, which is missing!` 是加载器的**警告**，不是失败，
 * 因此它不在任何特征里，也不会让判定变成 (C)。
 */
fun classifyCapturedLog(
    text: String,
    exitCode: Int,
    signatures: List<CrashSignature> = CrashSignatures.load(),
): CrashOutcome {
    val matched = signatures.firstOrNull { it.matches(text) }
    val finding = matched?.let { signature ->
        CrashFinding(
            signatureId = signature.id,
            diagnosisType = signature.className,
            confidence = signature.confidence,
            ownerModId = signature.captureOwner(text),
            action = signature.action,
            messageKey = signature.messageKey,
        )
    }
    val confidence = finding?.confidence ?: DiagnosisConfidence.LOW

    if (text.contains(GAME_CRASH_MARKER)) {
        return CrashOutcome(LaunchOutcome.CRASHED_UNEXPECTEDLY, confidence, finding)
    }

    val reachedMarker = text.contains(LAUNCH_REACHED_MARKER)
    if (reachedMarker && exitCode == 0) {
        return CrashOutcome(LaunchOutcome.NORMAL_EXIT_AFTER_MARKER, confidence, finding)
    }
    if (reachedMarker) {
        return CrashOutcome(LaunchOutcome.REACHED_MARKER, confidence, finding)
    }
    //没有走到客户端初始化完成，就是一次启动失败；退出码只是其中一个证据。
    //命中的 loader_fatal 特征落在这里而不是单独一支：它给出的结论和非零退出码给出的
    //是同一个「启动阶段失败」，区别只在有没有一条特征能说明原因，而这一点已经由
    //finding 是否为 null 表达了。
    return CrashOutcome(LaunchOutcome.FAILED_DURING_STARTUP, confidence, finding)
}

/**
 * 读一段捕获日志的尾巴，最多 [limitBytes] 字节；读不到时返回空串，永不抛异常。
 *
 * 这里不假设文件是 UTF-8 对齐的：截断点上可能出现半个字符，解码器会给出替换字符，
 * 而所有用来匹配的字面量都是 ASCII，因此不影响判定。
 */
fun readCapturedLogTail(file: File, limitBytes: Long = CAPTURED_LOG_TAIL_LIMIT_BYTES): String {
    if (!file.isFile) return ""

    return runCatching {
        RandomAccessFile(file, "r").use { reader ->
            val length = reader.length()
            val start = (length - limitBytes).coerceAtLeast(0L)
            reader.seek(start)

            val size = (length - start).toInt()
            if (size <= 0) return@use ""

            val bytes = ByteArray(size)
            reader.readFully(bytes)
            String(bytes, Charsets.UTF_8)
        }
    }.getOrDefault("")
}

/**
 * 这次启动的 Minecraft 版本与加载器名字，从**日志文本**里读出来。
 *
 * 两个来源都在这份日志里，而且都是写下它的人自己的话：
 * 1. 加载器自己写的 `Loading Minecraft <mc> with Fabric Loader <version>`
 * 2. 启动器自己写的横幅 `Minecraft Info: <mc>, <loader> - <loaderVersion>`
 *
 * 读不出来时两个字段都是空串：它们只是学习记录里的两个字段，缺了就让它们缺着，
 * 不能因此不记这次诊断。
 */
data class LaunchIdentity(
    val minecraftVersion: String = "",
    val loader: String = "",
)

/** 加载器自己写的启动行；`.` 不跨行，所以只在那一行里取 */
private val LOADER_LINE = Regex("""Loading Minecraft (\S+) with (.+?) Loader \S+""")

/** 启动器横幅里的那一行（`Minecraft Info: <mc>, <loader> - <version>`） */
private val LAUNCHER_INFO_LINE = Regex("""Minecraft Info: ([^,\r\n]+), ([^\r\n]+)""")

fun readLaunchIdentity(text: String): LaunchIdentity {
    LOADER_LINE.find(text)?.let { matched ->
        return LaunchIdentity(
            minecraftVersion = matched.groupValues[1],
            loader = matched.groupValues[2].trim(),
        )
    }

    LAUNCHER_INFO_LINE.find(text)?.let { matched ->
        return LaunchIdentity(
            minecraftVersion = matched.groupValues[1].trim(),
            loader = matched.groupValues[2].substringBefore(" - ").trim(),
        )
    }

    return LaunchIdentity()
}

/**
 * 从捕获日志的路径推出它属于哪个实例目录。
 *
 * 日志永远放在 `<versionsHome>/<实例名>/ALLLauncher/<log>`（见 `GameLauncher.getLogFile`），
 * 所以实例目录就是日志文件的上上级。推不出来时返回 null：那只说明这次既不做动作、也不记录。
 */
fun crashInstanceDir(logFile: File): File? = logFile.parentFile?.parentFile

/**
 * 一个日志文件当前的指纹：长度 + 修改时间。
 *
 * 用来回答「这个文件在这次修复之后被重写过吗」——`LoggerBridge.start` 每次启动都会截断它，
 * 所以重写过就说明用户又启动过一次，写没重写过就什么都不能下结论。
 */
fun capturedLogStamp(file: File): String = "${file.length()}:${file.lastModified()}"
