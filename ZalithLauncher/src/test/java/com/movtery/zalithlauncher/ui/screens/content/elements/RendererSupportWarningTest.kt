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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.screens.content.elements

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 「渲染器不支持此版本」的警告不能变成绕过模组检查的捷径。
 *
 * 真实用例（4-mod 认证矩阵 26.3|fabric）：
 * 当前渲染器的版本范围不含 26.3 → 弹警告 → 点「继续」→ 直接 RealLaunch，
 * 整条 Mod Doctor 检查被跳过，日志里根本没有 inspect 记录。
 *
 * 期望：点「继续」只是不再重复问这个警告，启动仍然走正常的前置检查（Mod Doctor 等），
 * 由 Mod Doctor 决定是启动还是给出问题清单。
 */
class RendererSupportWarningTest {
    /** 不支持 + 用户没确认过 → 必须先问用户。 */
    @Test
    fun anUnacceptedRendererWarningStopsTheLaunch() {
        assertTrue(decideRendererSupportWarning(isRendererUnsupported = true, userAccepted = false))
    }

    /** 不支持 + 用户已确认 → 放行，让流程继续走到 Mod Doctor 等检查（不能再跳 RealLaunch）。 */
    @Test
    fun anAcceptedRendererWarningLetsTheLaunchContinueIntoTheNormalChecks() {
        assertFalse(decideRendererSupportWarning(isRendererUnsupported = true, userAccepted = true))
    }

    /** 渲染器受支持时本来就不会有警告，用户是否确认过都不影响。 */
    @Test
    fun aSupportedRendererIsNeverWarnedAbout() {
        assertFalse(decideRendererSupportWarning(isRendererUnsupported = false, userAccepted = false))
        assertFalse(decideRendererSupportWarning(isRendererUnsupported = false, userAccepted = true))
    }
}
