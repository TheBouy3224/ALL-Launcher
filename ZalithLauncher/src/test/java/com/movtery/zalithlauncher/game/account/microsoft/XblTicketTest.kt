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

/**
 * A device-code login that succeeded at Microsoft was still refused by Xbox Live with 401 on
 * `user.auth.xboxlive.com/user/authenticate`: the legacy Live token needs the bare RpsTicket form
 * first, and a 401 must also be retried with the alternate form — not only a 400.
 */
class XblTicketTest {
    @Test
    fun `the legacy Live flow sends the bare token first`() {
        assertEquals("TOKEN" to "d=TOKEN", xblTicketForms(legacyMinecraftAuth = true, accessToken = "TOKEN"))
    }

    @Test
    fun `the Azure v2 flow sends the d= prefixed token first`() {
        assertEquals("d=TOKEN" to "TOKEN", xblTicketForms(legacyMinecraftAuth = false, accessToken = "TOKEN"))
    }

    @Test
    fun `a rejected ticket is retried with the alternate form on 400 and 401`() {
        assertTrue(shouldRetryXblWithAlternateTicket(400))
        assertTrue(shouldRetryXblWithAlternateTicket(401))
        assertFalse(shouldRetryXblWithAlternateTicket(403))
    }
}
