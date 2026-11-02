package com.movtery.zalithlauncher.all.accounts

import com.movtery.zalithlauncher.all.ui.AllColors
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
import com.movtery.zalithlauncher.all.ui.XpChip
import com.movtery.zalithlauncher.all.ui.XpCard
import com.movtery.zalithlauncher.all.ui.XpBlue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
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
 * ALL login gate (V3 flat navy look), shown before ALL Desktop.
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

    XpWindow("") {
        if (!loaded || hasUsableMicrosoftAccount(accounts)) {
            //reading the database / entering: no login page flash for returning users
            Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
                Text(stringResource(R.string.all_gate_checking), color = XpMuted, fontSize = 16.sp)
            }
            return@XpWindow
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val wide = maxWidth >= 600.dp
            //landscape: two columns (main action | offline option or code) so nothing needs scrolling
            val twoCols = wide && (phase == GatePhase.RUNNING || (phase == GatePhase.IDLE && offlineVisible))
            //tapping the ALL mark 7 times in a row unlocks the offline option (Easter egg)
            val logoTap = {
                if (taps.tap(SystemClock.uptimeMillis())) {
                    AllSettings.allOfflineUnlocked.save(true)
                    unlockedNotice = true
                }
            }
            val useLocal: (Account) -> Unit = { AccountsManager.setCurrentAccount(it); enter() }
            val createLocal: (String) -> Unit = { pendingOffline = it; localLogin(it, null) }
            val copyCode = { code?.let { clipboard.setText(AnnotatedString(it)) }; Unit }
            val codeButtons: @Composable () -> Unit = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (code != null) {
                        XpButton(stringResource(R.string.all_gate_copy), onClick = copyCode, modifier = Modifier.weight(1f))
                        verifyUrl?.let { url ->
                            XpButton(stringResource(R.string.all_gate_open_web), onClick = { openMicrosoftPage(context, url) }, default = true, modifier = Modifier.weight(1f))
                        }
                    }
                }
                XpButton(stringResource(R.string.all_setup_cancel), onClick = ::cancel, modifier = Modifier.fillMaxWidth())
            }
            val scroll = Modifier.verticalScroll(rememberScrollState()).imePadding()

            if (twoCols) {
                Row(
                    scroll.widthIn(max = 900.dp).fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Brand(logoTap, compact = phase == GatePhase.RUNNING)
                        if (phase == GatePhase.IDLE) IdleMain(needsRelogin(accounts), usesLegacyMinecraftAuth, unlockedNotice, ::start)
                        else RunningSteps(step)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (phase == GatePhase.IDLE) Offline(accounts.filter { it.isLocalAccount() }, useLocal, createLocal, collapsible = false)
                        else {
                            if (code != null && step <= 1) CodeCard(code!!, copyCode)
                            codeButtons()
                        }
                    }
                }
            } else {
                Column(
                    scroll.widthIn(max = 420.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Brand(logoTap, compact = false)
                    when (phase) {
                        GatePhase.IDLE -> {
                            IdleMain(needsRelogin(accounts), usesLegacyMinecraftAuth, unlockedNotice, ::start, buttonFirst = true)
                            if (offlineVisible) Offline(accounts.filter { it.isLocalAccount() }, useLocal, createLocal, collapsible = true)
                        }
                        GatePhase.RUNNING -> {
                            RunningSteps(step)
                            if (code != null && step <= 1) CodeCard(code!!, copyCode)
                            codeButtons()
                        }
                        GatePhase.FAILED -> {
                            Failed(errorKind, errorDetail, detailsOpen) { detailsOpen = !detailsOpen }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                XpButton(stringResource(R.string.all_setup_cancel), onClick = { phase = GatePhase.IDLE }, modifier = Modifier.weight(1f))
                                XpButton(stringResource(R.string.all_setup_retry), onClick = ::start, default = true, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The ALL mark (tap it 7 times to unlock offline mode) and the heading */
@Composable
private fun Brand(onLogoTap: () -> Unit, compact: Boolean) {
    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.foundation.Image(
            androidx.compose.ui.res.painterResource(R.drawable.img_all_logo), "ALL",
            modifier = Modifier.size(if (compact) 44.dp else 56.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onLogoTap).padding(4.dp)
        )
        Text(stringResource(R.string.all_gate_heading), color = Color.White, fontWeight = FontWeight.Medium, fontSize = 18.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun IdleMain(relogin: Boolean, legacy: Boolean, unlockedNotice: Boolean, onStart: () -> Unit, buttonFirst: Boolean = false) {
    //narrow single column: the sign-in button comes right after the heading so it is visible without scrolling
    val signIn = @Composable { XpButton(stringResource(R.string.all_gate_signin), onClick = onStart, default = true, modifier = Modifier.fillMaxWidth()) }
    if (buttonFirst) signIn()
    Text(stringResource(R.string.all_gate_body), color = XpMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
    if (relogin) Text(
        stringResource(R.string.all_gate_relogin), color = XpError, fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF3A2128)).padding(horizontal = 12.dp, vertical = 8.dp)
    )
    if (!buttonFirst) signIn()
    if (legacy) Text(stringResource(R.string.all_accounts_ms_shared_client), color = XpMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
    if (unlockedNotice) Text(stringResource(R.string.all_gate_offline_unlocked), color = XpOk, fontSize = 13.sp)
    //Zalith Launcher 2 additional terms (GPLv3 7): modified versions must say they are unofficial on the startup screen or main interface
    Text(stringResource(R.string.all_unofficial_notice), color = XpMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
}

@Composable
private fun RunningSteps(step: Int) {
    val labels = listOf(R.string.all_gate_step_code, R.string.all_gate_step_confirm, R.string.all_gate_step_xbox, R.string.all_gate_step_owner, R.string.all_gate_step_done)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(XpCard).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, res ->
            val mark = when { i < step -> "✔" to XpOk; i == step -> "▶" to XpBorder; else -> "○" to XpMuted }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(mark.first, color = mark.second, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.widthIn(min = 24.dp))
                Text(stringResource(res), color = if (i > step) XpMuted else XpInk, fontSize = 14.sp, fontWeight = if (i == step) FontWeight.Medium else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun CodeCard(code: String, onCopy: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1B2232)).clickable(onClick = onCopy).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.all_gate_code_label), color = XpMuted, fontSize = 12.sp)
        Text(code, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 26.sp, fontFamily = FontFamily.Monospace, letterSpacing = 4.sp)
        Text(stringResource(R.string.all_gate_code_help), color = XpMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
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
        color = XpError, fontSize = 14.sp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF3A2128)).padding(horizontal = 12.dp, vertical = 10.dp)
    )
    if (detail.isNotEmpty()) {
        Text(
            (if (open) "▾ " else "▸ ") + stringResource(R.string.all_setup_details),
            color = XpBorder, fontSize = 13.sp, modifier = Modifier.clickable(onClick = toggle).padding(vertical = 6.dp)
        )
        if (open) Text(detail, color = XpMuted, fontSize = 12.sp)
    }
}

/** Offline account option. [collapsible] = single-column layout (open by default when an offline account already exists); two columns show it open. */
@Composable
private fun Offline(locals: List<Account>, onUse: (Account) -> Unit, onCreate: (String) -> Unit, collapsible: Boolean) {
    var name by remember { mutableStateOf("") }
    var open by remember { mutableStateOf(locals.isNotEmpty()) }
    val shown = !collapsible || open
    val invalid = name.isNotEmpty() && (InvalidNameChars.containsMatchIn(name) || name.length > 16)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(XpCard)) {
        Row(
            Modifier.fillMaxWidth().then(if (collapsible) Modifier.clickable { open = !open } else Modifier).padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.all_gate_offline_title), color = XpError, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.weight(1f))
            if (collapsible) Text(if (open) "▾" else "▸", color = XpMuted, fontSize = 14.sp)
        }
        if (shown) Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.all_gate_offline_body), color = XpMuted, fontSize = 12.sp)
            locals.forEach { acc ->
                XpButton(stringResource(R.string.all_gate_offline_continue, acc.username), onClick = { onUse(acc) }, modifier = Modifier.fillMaxWidth())
            }
            BasicTextField(
                value = name, onValueChange = { name = it }, singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp), cursorBrush = SolidColor(XpBlue),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(XpChip)
                    .border(1.dp, if (invalid) XpError else Color.Transparent, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (name.isEmpty()) Text(stringResource(R.string.all_accounts_username), color = XpMuted, fontSize = 15.sp)
                        inner()
                    }
                }
            )
            XpButton(stringResource(R.string.all_gate_offline_create), onClick = { onCreate(name.trim()) }, enabled = name.isNotBlank() && !invalid, modifier = Modifier.fillMaxWidth())
        }
    }
}
