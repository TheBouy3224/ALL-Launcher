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

import com.movtery.zalithlauncher.BuildKeys

/**
 * Minecraft: Java Edition 自己的启动器使用的公开客户端 ID。
 *
 * 设备码流程必须携带 client_id，否则 Microsoft 会以 AADSTS900144 拒绝请求；而打包时没有配置
 * OAUTH_CLIENT_ID 的构建（环境变量、根目录 .oauth_client_id.txt、Gradle 属性三者皆无）会带上空值，
 * 导致登录连第一步都走不到。上游旧实现正是用这个公开 ID 兜底，这里保持同样的兜底行为：
 * 构建方配置了自己的 Azure 应用注册时用它，没有配置时用这个公开 ID。
 */
const val LEGACY_MINECRAFT_SCOPE = "service::user.auth.xboxlive.com::MBI_SSL"

const val PUBLIC_MINECRAFT_CLIENT_ID = "00000000402b5328"

/**
 * OAuth 请求实际要携带的客户端 ID。
 *
 * @param configured 构建时注入的 OAUTH_CLIENT_ID，可能为空
 * @return 配置值（去空白）；未配置时返回 [PUBLIC_MINECRAFT_CLIENT_ID]
 */
fun resolveMicrosoftClientId(configured: String): String =
    configured.trim().ifEmpty { PUBLIC_MINECRAFT_CLIENT_ID }

/**
 * 构建是否没有自己的应用注册。
 *
 * 公开客户端 ID 属于 Microsoft 账户（Live）应用，只能在旧版 Live 端点
 * （login.live.com/oauth20_connect.srf）上使用；Azure v2 端点会以 AADSTS700016 拒绝它。
 * 因此“未配置”和“已配置”必须走不同的端点，这一点与上游旧实现一致。
 *
 * @param configured 构建时注入的 OAUTH_CLIENT_ID，可能为空
 */
fun isLegacyMinecraftAuth(configured: String): Boolean = configured.trim().isEmpty()

/** 当前构建使用的客户端 ID。 */
val microsoftClientId: String
    get() = resolveMicrosoftClientId(BuildKeys.OAUTH_CLIENT_ID)

/** 当前构建是否走旧版 Live 设备码端点。 */
val usesLegacyMinecraftAuth: Boolean
    get() = isLegacyMinecraftAuth(BuildKeys.OAUTH_CLIENT_ID)

/** 旧版 Live 令牌端点刷新时必须带 scope，否则返回 400 invalid_scope；Azure 路径保持原样。 */
fun refreshScope(configured: String): String? =
    if (isLegacyMinecraftAuth(configured)) LEGACY_MINECRAFT_SCOPE else null
