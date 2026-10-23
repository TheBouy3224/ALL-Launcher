package com.movtery.zalithlauncher.all.accounts

import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.isReloginRequired
import com.movtery.zalithlauncher.game.account.isMicrosoftAccount
import com.movtery.zalithlauncher.game.account.microsoft.LoginStage
import com.movtery.zalithlauncher.game.account.microsoft.NotPurchasedMinecraftException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.util.network.UnresolvedAddressException
import java.net.ConnectException
import java.net.UnknownHostException

/**
 * Pure rules of the ALL login gate (no Android, unit tested).
 *
 * A Microsoft account row only exists after the whole sign-in succeeded, and that chain includes the Minecraft ownership check
 * (an account without the game fails with NotPurchasedMinecraftException before anything is saved), so "has a usable Microsoft
 * account" is the entry ticket. Offline accounts are never a ticket by themselves; they are a separate, clearly labelled choice.
 */
private val placeholders = setOf("", "0", "None", "NULL")

/** A Microsoft account whose refresh token is real; a lost/undecryptable token is stored as "0" and needs a new sign-in */
fun Account.isUsableMicrosoftAccount(): Boolean = isMicrosoftAccount() && refreshToken !in placeholders

fun hasUsableMicrosoftAccount(accounts: List<Account>): Boolean = accounts.any { it.isUsableMicrosoftAccount() }

/** Microsoft accounts that exist but can no longer be refreshed: the gate tells the user to sign in again instead of silently dropping them */
fun needsRelogin(accounts: List<Account>): Boolean =
    accounts.any { it.isMicrosoftAccount() } && !hasUsableMicrosoftAccount(accounts)

enum class LoginErrorKind { NO_NETWORK, NOT_OWNED, EXPIRED, OTHER }

fun classifyLoginError(th: Throwable): LoginErrorKind = when {
    th is NotPurchasedMinecraftException -> LoginErrorKind.NOT_OWNED
    th is UnknownHostException || th is UnresolvedAddressException || th is ConnectException || th is HttpRequestTimeoutException ->
        LoginErrorKind.NO_NETWORK
    th.isReloginRequired() -> LoginErrorKind.EXPIRED
    else -> LoginErrorKind.OTHER
}

/** Which of the five visible steps a stage belongs to: 0 code, 1 confirm, 2 Xbox, 3 Minecraft ownership, 4 done */
fun LoginStage.step(): Int = when (this) {
    LoginStage.REQUESTING_CODE -> 0
    LoginStage.WAITING_USER -> 1
    LoginStage.ACCESS_TOKEN, LoginStage.XBOX_LIVE, LoginStage.XSTS -> 2
    LoginStage.MINECRAFT, LoginStage.OWNERSHIP, LoginStage.PROFILE -> 3
    LoginStage.SAVING -> 4
}
