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

import com.tencent.mmkv.MMKV

/**
 * 模组项目缓存 MMKV，文件 HASH 值对应项目
 */
fun modProjectCache(): MMKV = MMKV.mmkvWithID("ModProjectHashMapper", MMKV.MULTI_PROCESS_MODE)
/**
 * 模组版本文件缓存 MMKV，文件 HASH 值对应平台文件
 */
fun modFileCache(): MMKV = MMKV.mmkvWithID("ModFileHashMapper", MMKV.MULTI_PROCESS_MODE)
/**
 * 本地模组安装信息缓存 MMKV，平台:指纹 值对应安装信息
 */
fun installedModCache(): MMKV = MMKV.mmkvWithID("InstalledModHashMapper", MMKV.MULTI_PROCESS_MODE)
/**
 * 崩溃后本地学习记录 MMKV，`签名Id:模组集指纹` 值对应一条候选/已确认记录
 *
 * 多进程模式：写下候选的是默认进程（崩溃页面），而读取它收尾的也永远是默认进程，
 * 但崩溃日志本身是 `:game` 进程写下的，这个存储将来可能被它读到，因此沿用同样的进程模式。
 */
fun postCrashLearning(): MMKV = MMKV.mmkvWithID("PostCrashLearning", MMKV.MULTI_PROCESS_MODE)
