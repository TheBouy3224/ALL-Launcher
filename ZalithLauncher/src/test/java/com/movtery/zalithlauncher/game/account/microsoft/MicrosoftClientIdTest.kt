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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.Json
import com.movtery.zalithlauncher.game.account.microsoft.models.DeviceCodeResponse

/**
 * 设备码流程必须带上 client_id：空值会让 Microsoft 直接以 AADSTS900144 拒绝，登录无法开始。
 * 因此“构建没配置 OAUTH_CLIENT_ID”必须是可用的状态，而不是一个空字符串。
 */
class MicrosoftClientIdTest {
    @Test
    fun `a build without a configured client id falls back to the public Minecraft one`() {
        assertEquals(PUBLIC_MINECRAFT_CLIENT_ID, resolveMicrosoftClientId(""))
        assertEquals(PUBLIC_MINECRAFT_CLIENT_ID, resolveMicrosoftClientId("   "))
    }

    @Test
    fun `a configured client id is used as it is`() {
        assertEquals("11111111-2222-3333-4444-555555555555", resolveMicrosoftClientId("11111111-2222-3333-4444-555555555555"))
        assertEquals("11111111-2222-3333-4444-555555555555", resolveMicrosoftClientId("  11111111-2222-3333-4444-555555555555\n"))
    }

    @Test
    fun `only a build without a client id uses the legacy Live endpoints`() {
        assertTrue(isLegacyMinecraftAuth(""))
        assertTrue(isLegacyMinecraftAuth("   "))
        assertFalse(isLegacyMinecraftAuth("11111111-2222-3333-4444-555555555555"))
    }

    /**
     * The legacy Live token endpoint rejects a refresh without `scope` (400 invalid_scope), which made every
     * Microsoft account refresh fail on launch. The Azure path keeps its request unchanged.
     */
    @Test
    fun `a legacy refresh carries the same scope as the legacy login`() {
        assertEquals(LEGACY_MINECRAFT_SCOPE, refreshScope(""))
        assertEquals(null, refreshScope("11111111-2222-3333-4444-555555555555"))
    }

    /**
     * The legacy Live device-code response carries no `message` field, so a required field made every
     * login fail at deserialization even though Microsoft had answered correctly.
     */
    @Test
    fun `a device-code response without a message field still decodes`() {
        val decoded = Json { ignoreUnknownKeys = true }.decodeFromString<DeviceCodeResponse>(
            """{"user_code":"ABCD-EFGH","device_code":"x","verification_uri":"https://www.microsoft.com/link","expires_in":900,"interval":5}"""
        )
        assertEquals("ABCD-EFGH", decoded.userCode)
        assertEquals(900, decoded.expiresIn)
        assertEquals("", decoded.message)
    }
}
