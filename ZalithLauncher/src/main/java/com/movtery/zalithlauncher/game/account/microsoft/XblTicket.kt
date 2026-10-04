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

package com.movtery.zalithlauncher.game.account.microsoft

/**
 * Xbox Live 的 RpsTicket 形式。
 *
 * 旧版 Live（MBI_SSL）与 Azure v2 两种流程对 RpsTicket 的期望形式不同，而且 Xbox Live 会因区域/
 * 端点差异改变要求，所以两种形式都要准备：先发主形式，被拒后用另一种形式重试。
 *
 * @return (主形式, 备用形式)
 */
fun xblTicketForms(legacyMinecraftAuth: Boolean, accessToken: String): Pair<String, String> =
    if (legacyMinecraftAuth) {
        accessToken to "d=$accessToken"
    } else {
        "d=$accessToken" to accessToken
    }

/**
 * Xbox Live 拒绝了主形式的 RpsTicket 时是否应改用另一种形式重试。
 *
 * 400 与 401 都要重试：旧版 Live 令牌被拒时返回的就是 401（响应体为空），只认 400 会把可恢复的
 * 请求变成登录失败。
 *
 * @param httpStatus Xbox Live 返回的状态码
 */
fun shouldRetryXblWithAlternateTicket(httpStatus: Int): Boolean =
    httpStatus == 400 || httpStatus == 401
