package com.movtery.zalithlauncher.all.accounts

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.ui.OutlinedTextField
import com.movtery.zalithlauncher.all.ui.XpBorder
import com.movtery.zalithlauncher.all.ui.XpButton
import com.movtery.zalithlauncher.all.ui.XpError
import com.movtery.zalithlauncher.all.ui.XpInk
import com.movtery.zalithlauncher.all.ui.XpMuted
import com.movtery.zalithlauncher.all.ui.XpOk
import com.movtery.zalithlauncher.all.ui.XpWindow
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.cancelMicrosoftLogin
import com.movtery.zalithlauncher.game.account.isLocalAccount
import com.movtery.zalithlauncher.game.account.localLogin
import com.movtery.zalithlauncher.game.account.microsoft.usesLegacyMinecraftAuth
import com.movtery.zalithlauncher.game.account.microsoftLogin
import com.movtery.zalithlauncher.game.account.microsoft.LoginStage
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.network.isNetworkAvailable
import java.util.concurrent.atomic.AtomicBoolean

private enum class GatePhase { IDLE, RUNNING, FAILED }

private val InvalidNameChars = Regex("[^a-zA-Z0-9_]")

/**
 * ALL login gate (Windows XP look), shown before ALL Desktop.
 *
 * Everything is the existing backend: [microsoftLogin] (device code -> Xbox -> XSTS -> Minecraft -> ownership check), [AccountsManager]
 * and [localLogin]. A usable Microsoft account lets the user straight in; otherwise Microsoft sign-in is required. There is no way
 * past the ownership check for a Microsoft account; offline accounts are a separate, labelled choice behind the logo Easter egg.
 */
@Composable
fun AllLoginGate(onEnter: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val accounts by AccountsManager.accountsFlow.collectAsStateWithLifecycle()
    val loaded by AccountsManager.accountsLoaded.collectAsStateWithLifecycle()

    var phase by remember { mutableStateOf(GatePhase.IDLE) }
    var step by remember { mutableIntStateOf(0) }
    var code by remember { mutableStateOf<String?>(null) }
    var verifyUrl by remember { mutableStateOf<String?>(null) }
    var errorKind by remember { mutableStateOf(LoginErrorKind.OTHER) }
    var errorDetail by remember { mutableStateOf("") }
    var detailsOpen by remember { mutableStateOf(false) }
    var pendingOffline by remember { mutableStateOf<String?>(null) }
    var unlockedNotice by remember { mutableStateOf(false) }
    val active = remember { AtomicBoolean(false) }
    val backHome = remember { AtomicBoolean(false) }
    val taps = remember { OfflineUnlockTaps() }
    var entered by remember { mutableStateOf(false) }

    val enter = { if (!entered) { entered = true; onEnter() } }

    //a usable Microsoft account (existing user, or the sign-in that just finished) is the ticket in
    LaunchedEffect(loaded, accounts) { if (loaded && hasUsableMicrosoftAccount(accounts)) enter() }
    //an offline account that was just created through the gate
    LaunchedEffect(accounts, pendingOffline) {
        val name = pendingOffline ?: return@LaunchedEffect
        accounts.find { it.isLocalAccount() && it.username == name }?.let {
            AccountsManager.setCurrentAccount(it)
            enter()
        }
    }

    fun fail(kind: LoginErrorKind, detail: String) {
        errorKind = kind; errorDetail = detail; detailsOpen = false; phase = GatePhase.FAILED
    }

    fun start() {
        if (!isNetworkAvailable(context)) { fail(LoginErrorKind.NO_NETWORK, ""); return }
        step = 0; code = null; verifyUrl = null; backHome.set(false)
        phase = GatePhase.RUNNING
        active.set(true)
        microsoftLogin(
            context = context,
            toWeb = { url ->
                verifyUrl = url
                //Microsoft's own page as a Custom Tab inside ALL (see MicrosoftWebPage.kt)
                openMicrosoftPage(context, url)
            },
            backToMain = {},
            checkIfInWebScreen = { active.get() },
            updateOperation = {},
            showToast = { _, _ -> },
            submitError = {},
            onSuccess = { active.set(false); if (backHome.compareAndSet(false, true)) returnToAll(context) },
            onStage = { stage, userCode ->
                if (!active.get()) return@microsoftLogin
                step = stage.step()
                if (userCode != null) code = userCode
                //the code was accepted: close the Microsoft tab so the user watches the Xbox / Minecraft checks in ALL
                if (stage.step() > LoginStage.WAITING_USER.step() && backHome.compareAndSet(false, true)) returnToAll(context)
            },
            onFailed = { th ->
                if (active.getAndSet(false)) {
                    fail(classifyLoginError(th), th::class.java.simpleName + (th.message?.let { ": " + it.take(160) } ?: ""))
                }
            }
        )
    }

    fun cancel() {
        active.set(false)
        cancelMicrosoftLogin()
        phase = GatePhase.IDLE
    }

    //closing the Microsoft tab without signing in = cancel, back to the gate
    CancelWhenBackFromMicrosoftPage(waiting = phase == GatePhase.RUNNING && step == LoginStage.WAITING_USER.step()) { cancel() }

    val offlineVisible = offlineOptionVisible(AllSettings.allOfflineUnlocked.state, accounts.any { it.isLocalAccount() })
    val title = stringResource(R.string.all_gate_title)

    XpWindow(title) {
        if (!loaded || hasUsableMicrosoftAccount(accounts)) {
            //reading the database / entering: no login page flash for returning users
            Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
                Text(stringResource(R.string.all_gate_checking), color = XpMuted, fontSize = 16.sp)
            }
            return@XpWindow
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val wide = maxWidth >= 600.dp
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(Modifier.widthIn(max = if (wide) 640.dp else 480.dp).fillMaxWidth()) {
                    Column(
                        Modifier.weight(1f, fill = false).fillMaxWidth().padding(8.dp)
                            .background(Color.White).border(1.dp, Color(0xFF7F9DB9)).padding(10.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (phase) {
                            GatePhase.IDLE -> Idle(
                                relogin = needsRelogin(accounts),
                                onLogoTap = {
                                    if (taps.tap(SystemClock.uptimeMillis())) {
                                        AllSettings.allOfflineUnlocked.save(true)
                                        unlockedNotice = true
                                    }
                                }
                            )
                            GatePhase.RUNNING -> Running(step, code, onCopy = { code?.let { clipboard.setText(AnnotatedString(it)) } })
                            GatePhase.FAILED -> Failed(errorKind, errorDetail, detailsOpen) { detailsOpen = !detailsOpen }
                        }
                        if (unlockedNotice) Text(stringResource(R.string.all_gate_offline_unlocked), color = XpOk, fontSize = 14.sp)
                        if (phase == GatePhase.IDLE && offlineVisible) {
                            Offline(accounts.filter { it.isLocalAccount() }, onUse = { AccountsManager.setCurrentAccount(it); enter() }, onCreate = {
                                pendingOffline = it
                                localLogin(it, null)
                            })
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (phase) {
                            GatePhase.IDLE -> XpButton(stringResource(R.string.all_gate_signin), onClick = ::start, default = true)
                            GatePhase.RUNNING -> {
                                if (code != null) {
                                    XpButton(stringResource(R.string.all_gate_copy), onClick = { code?.let { clipboard.setText(AnnotatedString(it)) } })
                                    verifyUrl?.let { url ->
                                        XpButton(stringResource(R.string.all_gate_open_web), onClick = {
                                            openMicrosoftPage(context, url)
                                        })
                                    }
                                }
                                XpButton(stringResource(R.string.all_setup_cancel), onClick = ::cancel)
                            }
                            GatePhase.FAILED -> {
                                XpButton(stringResource(R.string.all_setup_cancel), onClick = { phase = GatePhase.IDLE })
                                XpButton(stringResource(R.string.all_setup_retry), onClick = ::start, default = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Idle(relogin: Boolean, onLogoTap: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        //tapping the ALL wordmark 7 times in a row unlocks offline mode (Easter egg)
        Text(
            "ALL", color = XpBorder, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, fontSize = 30.sp,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onLogoTap).padding(horizontal = 6.dp)
        )
        Text(stringResource(R.string.all_gate_heading), color = XpBorder, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
    Text(stringResource(R.string.all_gate_body), color = XpInk, fontSize = 16.sp)
    if (relogin) Text(stringResource(R.string.all_gate_relogin), color = XpError, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    if (usesLegacyMinecraftAuth) Text(stringResource(R.string.all_accounts_ms_shared_client), color = XpMuted, fontSize = 12.sp)
}

@Composable
private fun Running(step: Int, code: String?, onCopy: () -> Unit) {
    val labels = listOf(R.string.all_gate_step_code, R.string.all_gate_step_confirm, R.string.all_gate_step_xbox, R.string.all_gate_step_owner, R.string.all_gate_step_done)
    labels.forEachIndexed { i, res ->
        val mark = when { i < step -> "✔" to XpOk; i == step -> "▶" to XpBorder; else -> "○" to XpMuted }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(mark.first, color = mark.second, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.widthIn(min = 24.dp))
            Text(stringResource(res), color = if (i > step) XpMuted else XpInk, fontSize = 16.sp, fontWeight = if (i == step) FontWeight.Bold else FontWeight.Normal)
        }
    }
    if (code != null && step <= 1) {
        Text(stringResource(R.string.all_gate_code_label), color = XpMuted, fontSize = 13.sp)
        Text(
            code, color = XpBorder, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.clickable(onClick = onCopy)
        )
        Text(stringResource(R.string.all_gate_code_help), color = XpInk, fontSize = 14.sp)
    }
}

@Composable
private fun Failed(kind: LoginErrorKind, detail: String, open: Boolean, toggle: () -> Unit) {
    Text(
        stringResource(
            when (kind) {
                LoginErrorKind.NO_NETWORK -> R.string.all_gate_err_network
                LoginErrorKind.NOT_OWNED -> R.string.all_gate_err_not_owned
                LoginErrorKind.EXPIRED -> R.string.all_gate_err_expired
                LoginErrorKind.OTHER -> R.string.all_gate_err_other
            }
        ),
        color = XpError, fontWeight = FontWeight.Bold, fontSize = 16.sp
    )
    if (detail.isNotEmpty()) {
        Text(
            (if (open) "▾ " else "▸ ") + stringResource(R.string.all_setup_details),
            color = XpBorder, fontSize = 14.sp, modifier = Modifier.clickable(onClick = toggle).padding(vertical = 4.dp)
        )
        if (open) Text(detail, color = XpMuted, fontSize = 12.sp)
    }
}

@Composable
private fun Offline(locals: List<Account>, onUse: (Account) -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val invalid = name.isNotEmpty() && (InvalidNameChars.containsMatchIn(name) || name.length > 16)
    Column(Modifier.fillMaxWidth().border(1.dp, Color(0xFFD0D0C8)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.all_gate_offline_title), color = XpError, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(stringResource(R.string.all_gate_offline_body), color = XpMuted, fontSize = 13.sp)
        locals.forEach { acc ->
            XpButton(stringResource(R.string.all_gate_offline_continue, acc.username), onClick = { onUse(acc) })
        }
        OutlinedTextField(
            value = name, onValueChange = { name = it }, singleLine = true, isError = invalid,
            label = { Text(stringResource(R.string.all_accounts_username)) }
        )
        XpButton(stringResource(R.string.all_gate_offline_create), onClick = { onCreate(name.trim()) }, enabled = name.isNotBlank() && !invalid)
    }
}
