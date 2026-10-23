package com.movtery.zalithlauncher.all.accounts

import com.movtery.zalithlauncher.all.ui.AllButton
import com.movtery.zalithlauncher.all.ui.AllListRow
import com.movtery.zalithlauncher.all.ui.allStyleForColor
import com.movtery.zalithlauncher.all.ui.AllButtonStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.movtery.zalithlauncher.all.ui.AlertDialog
import com.movtery.zalithlauncher.all.ui.OutlinedTextField
import androidx.compose.material3.Text
import com.movtery.zalithlauncher.all.ui.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.isLocalAccount
import com.movtery.zalithlauncher.game.account.isMicrosoftAccount
import com.movtery.zalithlauncher.game.account.isAuthServerAccount
import com.movtery.zalithlauncher.game.account.localLogin
import com.movtery.zalithlauncher.setting.AllSettings

/**
 * ALL accounts window (PHASE 3.4).
 *
 * Everything is Zalith's backend: [AccountsManager] (Room database, accountsFlow / currentAccountFlow),
 * [localLogin] (creates the offline account exactly like Classic, using the same default UUID),
 * [AccountsManager.setCurrentAccount] (only writes the "current account" setting; no token is read or refreshed)
 * and [AccountsManager.deleteAccount].
 * V2 (3 columns, XP Luna): list | details | full-body 3D skin. Tapping a row only *views* it; "use this account" is the
 * only thing that changes the active account. Skin refresh only re-downloads the skin file (never saves the account).
 */

private val AccSurface = Color(0xFFF3F1E8)
private val AccCard = Color(0xFFFFFFFF)
private val AccBorder = Color(0xFFB9B4A2)
private val AccText = Color(0xFF1B1B1B)
private val AccDim = Color(0xFF6B675C)
private val AccAccent = Color(0xFF316AC5)
private val AccOk = Color(0xFF2E7D32)
private val AccDanger = Color(0xFFB3261E)
private val AccPanelBg = Color(0xFFF8F7F2)
private val AccSelected = Color(0xFFDDEBFF)

/** Same rule as Classic's offline dialog: only letters, digits and underscore */
private val InvalidNameChars = Regex("[^a-zA-Z0-9_]")

@Composable
fun AllAccountsWindow(modifier: Modifier = Modifier, submitError: (com.movtery.zalithlauncher.viewmodel.ErrorViewModel.ThrowableMessage) -> Unit = {}) {
    //复用 Classic 的账号 ViewModel：离线皮肤按账号分开保存、正版皮肤走官方 API 上传，这里不另写一套
    val eventViewModel: com.movtery.zalithlauncher.viewmodel.EventViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val viewModel: com.movtery.zalithlauncher.viewmodel.AccountManageViewModel =
        androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel { factory: com.movtery.zalithlauncher.viewmodel.AccountManageViewModel.Factory -> factory.create(eventViewModel) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var msTip by remember { mutableStateOf(false) }
    var msWaiting by remember { mutableStateOf(false) }
    //polled by the login task: false = the user cancelled the waiting dialog
    val msActive = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    val operations by viewModel.operationUiState.collectAsStateWithLifecycle()
    val profile by viewModel.profileUiState.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            if (effect is com.movtery.zalithlauncher.viewmodel.AccountManageEffect.ShowError) {
                msWaiting = false; msActive.set(false)
                submitError(com.movtery.zalithlauncher.viewmodel.ErrorViewModel.ThrowableMessage(effect.title, effect.message))
            }
        }
    }
    val accounts by AccountsManager.accountsFlow.collectAsStateWithLifecycle()
    val currentId = AllSettings.currentAccount.state
    val activeId = remember(accounts, currentId) {
        accounts.find { it.uniqueUUID == currentId }?.uniqueUUID ?: accounts.firstOrNull()?.uniqueUUID
    }

    //a new account appeared while waiting = the Microsoft sign-in succeeded
    androidx.compose.runtime.LaunchedEffect(accounts.size) { if (msWaiting) { msWaiting = false; msActive.set(false); returnToAll(context) } }
    CancelWhenBackFromMicrosoftPage(waiting = msWaiting) { msActive.set(false); msWaiting = false }

    var adding by remember { mutableStateOf(false) }
    var unlockedNotice by remember { mutableStateOf(false) }
    val logoTaps = remember { OfflineUnlockTaps() }
    //offline accounts stay hidden until the Easter egg; people who already have one keep the option
    val showOffline = offlineOptionVisible(AllSettings.allOfflineUnlocked.state, accounts.any { it.isLocalAccount() })
    var pendingDelete by remember { mutableStateOf<Account?>(null) }

    //the account being looked at; viewing never changes the active account (only "use this account" does)
    var viewedId by remember { mutableStateOf<String?>(null) }
    val viewed = accounts.find { it.uniqueUUID == viewedId } ?: accounts.find { it.uniqueUUID == activeId }
    var addChooser by remember { mutableStateOf(false) }
    var manageFor by remember { mutableStateOf<Account?>(null) }
    var skinBusy by remember { mutableStateOf(false) }
    var skinNote by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val noSkinText = stringResource(R.string.all_acc_skin_none)
    val skinDoneText = stringResource(R.string.all_acc_skin_refreshed)

    fun refreshSkin(account: Account) {
        if (skinBusy) return
        skinBusy = true; skinNote = null
        scope.launch {
            //skin + cape only, from the public session server (no token, no account save, active account untouched)
            runCatching { account.downloadYggdrasil() }
            AccountsManager.refreshWardrobe()
            skinNote = if (account.getSkinFile().exists()) skinDoneText else noSkinText
            skinBusy = false
        }
    }

    Row(modifier = modifier.fillMaxSize().background(AccSurface).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        //left: accounts
        AccPanel("👥", stringResource(R.string.all_acc_list_title), Modifier.weight(0.30f), trailing = {
            AllLogoMark {
                if (logoTaps.tap(android.os.SystemClock.uptimeMillis())) {
                    AllSettings.allOfflineUnlocked.save(true)
                    unlockedNotice = true
                }
            }
        }) {
            if (accounts.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { Text(stringResource(R.string.all_acc_empty), color = AccDim, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            } else LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(accounts, key = { it.uniqueUUID }) { account ->
                    AccountCard(account, viewed = account.uniqueUUID == viewed?.uniqueUUID, active = account.uniqueUUID == activeId) { viewedId = account.uniqueUUID }
                }
            }
            if (com.movtery.zalithlauncher.game.account.microsoft.usesLegacyMinecraftAuth) {
                Text(stringResource(R.string.all_accounts_ms_shared_client), color = AccDim, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            AllButton("＋ " + stringResource(R.string.all_acc_add), { if (showOffline) addChooser = true else msTip = true }, modifier = Modifier.fillMaxWidth(), style = AllButtonStyle.Primary, fill = true)
            AllButton("⊞ " + stringResource(R.string.all_acc_signin_ms), { msTip = true }, modifier = Modifier.fillMaxWidth(), fill = true)
        }

        //middle: details + actions
        AccPanel("👤", stringResource(R.string.all_acc_detail_title), Modifier.weight(0.38f)) {
            if (viewed == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { Text(stringResource(R.string.all_acc_empty), color = AccDim, fontSize = 13.sp) }
            } else AccountDetails(
                account = viewed,
                active = viewed.uniqueUUID == activeId,
                skinBusy = skinBusy,
                modifier = Modifier.weight(1f),
                onUse = { AccountsManager.setCurrentAccount(viewed) },
                onRefreshSkin = { refreshSkin(viewed) },
                onManage = { manageFor = viewed },
                onLogout = { pendingDelete = viewed }
            )
        }

        //right: full-body 3D skin
        AccPanel("👕", stringResource(R.string.all_acc_skin_title), Modifier.weight(0.32f)) {
            SkinPreviewPanel(account = viewed, note = skinNote, busy = skinBusy, modifier = Modifier.weight(1f).fillMaxWidth(),
                onRefresh = { viewed?.let(::refreshSkin) })
        }
    }

    if (addChooser) {
        AlertDialog(
            onDismissRequest = { addChooser = false },
            title = { Text(stringResource(R.string.all_acc_add)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AllButton("⊞ " + stringResource(R.string.all_acc_add_ms), { addChooser = false; msTip = true }, modifier = Modifier.fillMaxWidth(), style = AllButtonStyle.Primary)
                    AllButton("🎮 " + stringResource(R.string.all_acc_add_offline), { addChooser = false; adding = true }, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.all_acc_offline_note), color = AccDim, fontSize = 12.sp)
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { addChooser = false }) { Text(stringResource(R.string.generic_cancel)) } }
        )
    }

    manageFor?.let { account ->
        AlertDialog(
            onDismissRequest = { manageFor = null },
            title = { Text(stringResource(R.string.all_acc_manage) + " · " + account.username) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!account.isAuthServerAccount()) AllButton("🎨 " + stringResource(R.string.all_accounts_customize_skin), {
                        manageFor = null
                        viewModel.onIntent(
                            com.movtery.zalithlauncher.viewmodel.AccountManageIntent.UpdateAccountSkinOp(
                                com.movtery.zalithlauncher.ui.screens.content.elements.AccountSkinOperation.ChangeSkin(account)
                            )
                        )
                    }, modifier = Modifier.fillMaxWidth())
                    //delete lives here, away from the main buttons, and still asks for confirmation
                    AllButton("🗑 " + stringResource(if (account.isMicrosoftAccount()) R.string.all_accounts_remove else R.string.all_acc_delete),
                        { manageFor = null; pendingDelete = account }, modifier = Modifier.fillMaxWidth(), style = AllButtonStyle.Danger)
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { manageFor = null }) { Text(stringResource(R.string.generic_close)) } }
        )
    }

    com.movtery.zalithlauncher.ui.screens.content.AccountSkinOperation(
        accountSkinOperation = operations.accountSkinOp,
        skinDialogState = operations.accountSkinDialogState,
        accountCapes = profile.accountCapeOpMap,
        onIntent = viewModel::onIntent
    )

    if (msTip) {
        AlertDialog(
            onDismissRequest = { msTip = false },
            title = { Text(stringResource(R.string.all_accounts_add_microsoft)) },
            text = { Text(stringResource(R.string.all_accounts_ms_steps)) },
            confirmButton = {
                TextButton(onClick = {
                    msTip = false; msWaiting = true; msActive.set(true)
                    viewModel.onIntent(
                        com.movtery.zalithlauncher.viewmodel.AccountManageIntent.PerformMicrosoftLogin(
                            toWeb = { url -> openMicrosoftPage(context, url) },
                            backToMain = {},
                            checkIfInWebScreen = { msActive.get() }
                        )
                    )
                }) { Text(stringResource(R.string.all_accounts_ms_signin)) }
            },
            dismissButton = { TextButton(onClick = { msTip = false }) { Text(stringResource(R.string.generic_cancel)) } }
        )
    }

    if (msWaiting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.all_accounts_add_microsoft)) },
            text = { Text(stringResource(R.string.all_accounts_ms_waiting)) },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { msActive.set(false); msWaiting = false }) { Text(stringResource(R.string.generic_cancel)) } }
        )
    }

    if (unlockedNotice) {
        AlertDialog(
            onDismissRequest = { unlockedNotice = false },
            title = { Text(stringResource(R.string.account_type_local)) },
            text = { Text(stringResource(R.string.offline_mode_unlocked)) },
            confirmButton = { TextButton(onClick = { unlockedNotice = false }) { Text(stringResource(R.string.generic_confirm)) } }
        )
    }

    if (adding) {
        var name by remember { mutableStateOf("") }
        val invalid = name.isNotEmpty() && (InvalidNameChars.containsMatchIn(name) || name.length > 16)
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text(stringResource(R.string.account_local_create_account)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    isError = invalid,
                    label = { Text(stringResource(R.string.all_accounts_username)) }
                )
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank() && !invalid, onClick = { localLogin(name.trim(), null); adding = false }) {
                    Text(stringResource(R.string.generic_confirm))
                }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text(stringResource(R.string.generic_cancel)) } }
        )
    }

    pendingDelete?.let { account ->
        AlertDialog(
            destructive = true,
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(if (account.isMicrosoftAccount()) R.string.all_accounts_remove else R.string.account_delete_title)) },
            text = { Text(stringResource(if (account.isMicrosoftAccount()) R.string.all_accounts_remove_message else R.string.account_delete_message, account.username)) },
            confirmButton = {
                TextButton(onClick = { AccountsManager.deleteAccount(account); pendingDelete = null }) {
                    Text(stringResource(R.string.generic_confirm), color = AccDanger)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.generic_cancel)) } }
        )
    }
}

/** The ALL wordmark; tapping it 7 times in a row unlocks offline accounts (Easter egg, see [AllSettings.allOfflineUnlocked]) */
@Composable
private fun AllLogoMark(onClick: () -> Unit) {
    Text(
        "ALL", color = com.movtery.zalithlauncher.all.ui.AllColors.Blue, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic, fontSize = 20.sp,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun AccPanel(icon: String, title: String, modifier: Modifier, trailing: @Composable () -> Unit = {}, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(AccPanelBg).border(1.dp, AccBorder, RoundedCornerShape(8.dp)).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(title, color = AccText, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            trailing()
        }
        content()
    }
}

private fun typeLabelRes(account: Account) = when {
    account.isMicrosoftAccount() -> R.string.account_type_microsoft
    account.isLocalAccount() -> R.string.all_acc_type_offline
    else -> R.string.all_acc_type_server
}

@Composable
private fun AccountCard(account: Account, viewed: Boolean, active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(if (viewed) AccSelected else AccCard)
            .border(if (viewed) 2.dp else 1.dp, if (viewed) AccAccent else AccBorder, shape).clickable(onClick = onClick).padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace(account = account, avatarSize = 40.dp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(account.username, color = AccText, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text((if (account.isMicrosoftAccount()) "⊞ " else "🎮 ") + stringResource(typeLabelRes(account)), color = AccDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (account.isLocalAccount()) Text(stringResource(R.string.all_acc_not_real), color = AccDanger, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        //green = the account games launch with
        Box(Modifier.size(12.dp).clip(CircleShape).background(if (active) AccOk else AccBorder))
    }
}

@Composable
private fun AccountDetails(
    account: Account, active: Boolean, skinBusy: Boolean, modifier: Modifier,
    onUse: () -> Unit, onRefreshSkin: () -> Unit, onManage: () -> Unit, onLogout: () -> Unit
) {
    val ms = account.isMicrosoftAccount()
    val usable = account.isUsableMicrosoftAccount()
    Column(modifier.fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(AccCard).border(1.dp, AccBorder, RoundedCornerShape(6.dp)).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace(account = account, avatarSize = 48.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(account.username, color = AccText, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text((if (ms) "⊞ " else "🎮 ") + stringResource(typeLabelRes(account)), color = AccDim, fontSize = 13.sp, maxLines = 1)
                val (chip, chipColor) = when {
                    ms && usable -> stringResource(R.string.all_acc_chip_ready) to AccOk
                    ms -> stringResource(R.string.all_acc_chip_relogin) to AccDanger
                    account.isLocalAccount() -> stringResource(R.string.all_acc_not_real) to AccDanger
                    else -> stringResource(R.string.all_acc_type_server) to AccDim
                }
                Text(chip, color = chipColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(chipColor.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 2.dp))
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(AccCard).border(1.dp, AccBorder, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
            InfoLine("🪪", stringResource(R.string.all_acc_row_type), stringResource(typeLabelRes(account)), AccText)
            InfoLine("🔒", stringResource(R.string.all_acc_row_status), when {
                ms && usable && AccountsManager.isSessionValidated(account) -> stringResource(R.string.all_acc_status_verified)
                ms && usable -> stringResource(R.string.all_acc_status_signed_in)
                ms -> stringResource(R.string.all_acc_status_relogin)
                account.isLocalAccount() -> stringResource(R.string.all_acc_status_offline)
                else -> stringResource(R.string.all_acc_type_server)
            }, if (ms && usable) AccOk else if (ms) AccDanger else AccDim)
            //a Microsoft account is only saved after the ownership check passed at sign-in
            InfoLine("🎮", "Minecraft", if (ms) stringResource(R.string.all_acc_java_owned) else stringResource(R.string.all_acc_java_unverified), if (ms) AccText else AccDim)
            InfoLine("✅", stringResource(R.string.all_acc_row_active), stringResource(if (active) R.string.all_acc_active_yes else R.string.all_acc_active_no), if (active) AccOk else AccDim)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AllButton(if (active) "✓ " + stringResource(R.string.all_acc_in_use) else "▶ " + stringResource(R.string.all_acc_use), onUse,
                modifier = Modifier.weight(1f), style = AllButtonStyle.Primary, enabled = !active, fill = true)
            AllButton(if (skinBusy) "…" else "↻ " + stringResource(R.string.all_acc_refresh_skin), onRefreshSkin, modifier = Modifier.weight(1f), enabled = !skinBusy, fill = true)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AllButton("⚙ " + stringResource(R.string.all_acc_manage), onManage, modifier = Modifier.weight(1f), fill = true)
            //signing out = ALL forgets this Microsoft sign-in (the real Microsoft account is untouched); offline accounts have nothing to sign out of
            if (ms) AllButton("⎋ " + stringResource(R.string.all_acc_logout), onLogout, modifier = Modifier.weight(1f), style = AllButtonStyle.Danger, fill = true)
            else Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun InfoLine(icon: String, label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 13.sp)
        Spacer(Modifier.width(6.dp))
        Text(label, color = AccDim, fontSize = 13.sp, maxLines = 1)
        Spacer(Modifier.width(8.dp))
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AccButton(text: String, onClick: () -> Unit, color: Color = AccText) =
    AllButton(text, onClick, style = allStyleForColor(color))

/** Full-body 3D model: the existing skinview3d WebView (Steve/Alex, outer layer, drag to rotate 360°) with reset / refresh buttons */
@Composable
private fun SkinPreviewPanel(account: Account?, note: String?, busy: Boolean, modifier: Modifier, onRefresh: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val refreshWardrobe by AccountsManager.refreshWardrobe.collectAsStateWithLifecycle()
    val playerSkin = remember { com.movtery.zalithlauncher.ui.components.PlayerSkin(context) }
    var pageFinished by remember { mutableStateOf(false) }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { playerSkin.destroy() } }
    val hasSkin = account?.getSkinFile()?.exists() == true
    fun resetView() = playerSkin.setAzimuthAndPitch(-25, 8)

    Box(modifier.clip(RoundedCornerShape(6.dp)).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF9CC8F2), Color(0xFFD8ECFB), Color(0xFF8CBF6A))))) {
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                playerSkin.loadWebView(ctx, onPageFinished = {
                    pageFinished = true
                    playerSkin.startAnim(com.movtery.zalithlauncher.ui.components.ModelAnimation.NewIdle)
                    resetView()
                })
            },
            update = {
                if (pageFinished) {
                    runCatching {
                        //reload when the viewed account or its skin changes; no file = the default skin (placeholder)
                        refreshWardrobe.hashCode()
                        val file = account?.getSkinFile()?.takeIf { it.exists() }
                        file?.inputStream().use { playerSkin.loadSkin(it, account?.skinModelType) }
                    }
                }
            }
        )
        Column(Modifier.align(Alignment.TopEnd).padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SkinTool("⟲", enabled = pageFinished) { resetView() }
            SkinTool(if (busy) "…" else "↻", enabled = account != null && !busy, onClick = onRefresh)
        }
        val message = note ?: if (account != null && !hasSkin) stringResource(R.string.all_acc_skin_default) else null
        Column(Modifier.align(Alignment.BottomCenter).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            message?.let { Text(it, color = Color.White, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xB3000000)).padding(horizontal = 8.dp, vertical = 3.dp)) }
            Text("☝ " + stringResource(R.string.all_acc_drag_rotate), color = Color.White, fontSize = 12.sp,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0x99000000)).padding(horizontal = 10.dp, vertical = 3.dp))
        }
    }
}

@Composable
private fun SkinTool(icon: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = if (enabled) 0.92f else 0.5f))
            .border(1.dp, AccBorder, RoundedCornerShape(8.dp)).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(icon, fontSize = 18.sp, color = AccText) }
}
