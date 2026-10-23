package com.movtery.zalithlauncher.all.accounts

import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.AccountType
import com.movtery.zalithlauncher.game.account.microsoft.LoginStage
import com.movtery.zalithlauncher.game.account.microsoft.NotPurchasedMinecraftException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ConnectException
import java.net.UnknownHostException

class AllLoginGateLogicTest {
    private fun ms(refresh: String) = Account(username = "Steve", accountType = AccountType.MICROSOFT.tag, refreshToken = refresh, accessToken = "at")
    private val offline = Account(username = "Alex", accountType = AccountType.LOCAL.tag)

    @Test fun microsoftAccountWithRealRefreshTokenGetsIn() {
        assertTrue(hasUsableMicrosoftAccount(listOf(offline, ms("rt-value"))))
        assertFalse(needsRelogin(listOf(ms("rt-value"))))
    }

    @Test fun offlineAccountsAreNeverATicket() {
        assertFalse(hasUsableMicrosoftAccount(listOf(offline)))
        assertFalse(hasUsableMicrosoftAccount(emptyList()))
        assertFalse(needsRelogin(listOf(offline)))
    }

    @Test fun lostTokenMeansSignInAgainButTheAccountIsKept() {
        for (placeholder in listOf("0", "", "None", "NULL")) {
            val accounts = listOf(ms(placeholder), offline)
            assertFalse(hasUsableMicrosoftAccount(accounts))
            assertTrue(needsRelogin(accounts))
        }
    }

    @Test fun errorKinds() {
        assertEquals(LoginErrorKind.NOT_OWNED, classifyLoginError(NotPurchasedMinecraftException()))
        assertEquals(LoginErrorKind.NO_NETWORK, classifyLoginError(UnknownHostException("login.live.com")))
        assertEquals(LoginErrorKind.NO_NETWORK, classifyLoginError(ConnectException()))
        assertEquals(LoginErrorKind.OTHER, classifyLoginError(IllegalStateException("x")))
    }

    @Test fun stagesMapToTheFiveVisibleSteps() {
        assertEquals(0, LoginStage.REQUESTING_CODE.step())
        assertEquals(1, LoginStage.WAITING_USER.step())
        assertEquals(2, LoginStage.XSTS.step())
        assertEquals(3, LoginStage.OWNERSHIP.step())
        assertEquals(4, LoginStage.SAVING.step())
    }
}
