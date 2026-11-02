package com.movtery.zalithlauncher.all.accounts

import androidx.compose.ui.graphics.Brush

import com.movtery.zalithlauncher.all.ui.AllColors
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
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

private val AccSurface = Color(0xFF161B28)
private val AccCard = Color(0xFF1B2232)
private val AccBorder = Color(0xFF2D3548)
private val AccText = Color(0xFFD5DBE8)
private val AccDim = Color(0xFF8C97AE)
private val AccAccent = Color(0xFF2F6FED)
private val AccOk = Color(0xFF5BD08A)
private val AccDanger = Color(0xFFFF6B6B)
private val AccChip = Color(0xFF232B3D)

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

    Column(modifier = modifier.fillMaxSize().background(AccSurface).padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        //top: account chips (viewing one never changes the active account)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.foundation.lazy.LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                items(accounts, key = { it.uniqueUUID }) { account ->
                    AccountChip(account, viewed = account.uniqueUUID == viewed?.uniqueUUID, active = account.uniqueUUID == activeId) { viewedId = account.uniqueUUID }
                }
                item {
                    Text("＋ " + stringResource(R.string.all_acc_add), color = AccDim, fontSize = 13.sp, maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(18.dp)).border(1.dp, AccBorder, RoundedCornerShape(18.dp))
                            .clickable { if (showOffline) addChooser = true else msTip = true }.padding(horizontal = 14.dp, vertical = 9.dp))
                }
            }
            AllLogoMark {
                if (logoTaps.tap(android.os.SystemClock.uptimeMillis())) {
                    AllSettings.allOfflineUnlocked.save(true)
                    unlockedNotice = true
                }
            }
        }
        if (accounts.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { Text(stringResource(R.string.all_acc_empty), color = AccDim, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        else Row(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AccCard).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkinPreviewPanel(account = viewed, note = skinNote, busy = skinBusy, modifier = Modifier.weight(0.38f).fillMaxHeight(),
                onRefresh = { viewed?.let(::refreshSkin) })
            if (viewed != null) AccountDetails(
                account = viewed,
                active = viewed.uniqueUUID == activeId,
                skinBusy = skinBusy,
                modifier = Modifier.weight(0.62f).fillMaxHeight(),
                onUse = { AccountsManager.setCurrentAccount(viewed) },
                onRefreshSkin = { refreshSkin(viewed) },
                onManage = { manageFor = viewed },
                onLogout = { pendingDelete = viewed }
            )
        }
        if (com.movtery.zalithlauncher.game.account.microsoft.usesLegacyMinecraftAuth) {
            Text(stringResource(R.string.all_accounts_ms_shared_client), color = AccDim, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }

    if (addChooser) {
        FlatDialog(stringResource(R.string.all_acc_add), { addChooser = false },
            buttons = { DarkBtn(stringResource(R.string.generic_cancel), { addChooser = false }) }) {
            DarkBtn("⊞ " + stringResource(R.string.all_acc_add_ms), { addChooser = false; msTip = true }, modifier = Modifier.fillMaxWidth(), primary = true)
            DarkBtn("🎮 " + stringResource(R.string.all_acc_add_offline), { addChooser = false; adding = true }, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.all_acc_offline_note), color = AccDim, fontSize = 12.sp)
        }
    }

    manageFor?.let { account ->
        FlatDialog(stringResource(R.string.all_acc_manage) + " · " + account.username, { manageFor = null },
            buttons = { DarkBtn(stringResource(R.string.generic_close), { manageFor = null }) }) {
            if (!account.isAuthServerAccount()) DarkBtn("🎨 " + stringResource(R.string.all_accounts_customize_skin), {
                manageFor = null
                viewModel.onIntent(
                    com.movtery.zalithlauncher.viewmodel.AccountManageIntent.UpdateAccountSkinOp(
                        com.movtery.zalithlauncher.ui.screens.content.elements.AccountSkinOperation.ChangeSkin(account)
                    )
                )
            }, modifier = Modifier.fillMaxWidth())
            //delete lives here, away from the main buttons, and still asks for confirmation
            DarkBtn("🗑 " + stringResource(if (account.isMicrosoftAccount()) R.string.all_accounts_remove else R.string.all_acc_delete),
                { manageFor = null; pendingDelete = account }, modifier = Modifier.fillMaxWidth(), danger = true)
        }
    }

    com.movtery.zalithlauncher.ui.screens.content.AccountSkinOperation(
        accountSkinOperation = operations.accountSkinOp,
        skinDialogState = operations.accountSkinDialogState,
        accountCapes = profile.accountCapeOpMap,
        onIntent = viewModel::onIntent
    )

    if (msTip) {
        FlatDialog(stringResource(R.string.all_accounts_add_microsoft), { msTip = false },
            buttons = {
                DarkBtn(stringResource(R.string.generic_cancel), { msTip = false })
                DarkBtn(stringResource(R.string.all_accounts_ms_signin), {
                    msTip = false; msWaiting = true; msActive.set(true)
                    viewModel.onIntent(
                        com.movtery.zalithlauncher.viewmodel.AccountManageIntent.PerformMicrosoftLogin(
                            toWeb = { url -> openMicrosoftPage(context, url) },
                            backToMain = {},
                            checkIfInWebScreen = { msActive.get() }
                        )
                    )
                }, primary = true)
            }) { Text(stringResource(R.string.all_accounts_ms_steps), color = AccText, fontSize = 14.sp) }
    }

    if (msWaiting) {
        FlatDialog(stringResource(R.string.all_accounts_add_microsoft), {},
            buttons = { DarkBtn(stringResource(R.string.generic_cancel), { msActive.set(false); msWaiting = false }) }) {
            Text(stringResource(R.string.all_accounts_ms_waiting), color = AccText, fontSize = 14.sp)
        }
    }

    if (unlockedNotice) {
        FlatDialog(stringResource(R.string.account_type_local), { unlockedNotice = false },
            buttons = { DarkBtn(stringResource(R.string.generic_confirm), { unlockedNotice = false }, primary = true) }) {
            Text(stringResource(R.string.offline_mode_unlocked), color = AccText, fontSize = 14.sp)
        }
    }

    if (adding) {
        var name by remember { mutableStateOf("") }
        val invalid = name.isNotEmpty() && (InvalidNameChars.containsMatchIn(name) || name.length > 16)
        val ok = name.isNotBlank() && !invalid
        FlatDialog(stringResource(R.string.account_local_create_account), { adding = false },
            buttons = {
                DarkBtn(stringResource(R.string.generic_cancel), { adding = false })
                DarkBtn(stringResource(R.string.generic_confirm), { localLogin(name.trim(), null); adding = false }, primary = true, enabled = ok)
            }) {
            androidx.compose.foundation.text.BasicTextField(
                value = name, onValueChange = { name = it.take(32) }, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(AccAccent),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AccChip)
                    .border(1.dp, if (invalid) AccDanger else Color.Transparent, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                decorationBox = { inner -> Box { if (name.isEmpty()) Text(stringResource(R.string.all_accounts_username), color = AccDim, fontSize = 15.sp); inner() } }
            )
        }
    }

    pendingDelete?.let { account ->
        FlatDialog(stringResource(if (account.isMicrosoftAccount()) R.string.all_accounts_remove else R.string.account_delete_title), { pendingDelete = null },
            buttons = {
                DarkBtn(stringResource(R.string.generic_cancel), { pendingDelete = null })
                DarkBtn(stringResource(R.string.generic_confirm), { AccountsManager.deleteAccount(account); pendingDelete = null }, danger = true)
            }) {
            Text(stringResource(if (account.isMicrosoftAccount()) R.string.all_accounts_remove_message else R.string.account_delete_message, account.username), color = AccText, fontSize = 14.sp)
        }
    }
}

/** Flat dark dialog for this window: title, body column, right-aligned buttons (dismiss first, main action last). */
@Composable
private fun FlatDialog(
    title: String,
    onDismiss: () -> Unit,
    buttons: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.imePadding().padding(horizontal = 16.dp, vertical = 12.dp).widthIn(max = 420.dp).fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)).background(AccSurface).border(1.dp, AccBorder, RoundedCornerShape(14.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) { buttons() }
        }
    }
}

/** The ALL wordmark; tapping it 7 times in a row unlocks offline accounts (Easter egg, see [AllSettings.allOfflineUnlocked]) */
@Composable
private fun AllLogoMark(onClick: () -> Unit) {
    androidx.compose.foundation.Image(
        androidx.compose.ui.res.painterResource(R.drawable.img_all_logo), "ALL",
        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(2.dp)
    )
}

private fun typeLabelRes(account: Account) = when {
    account.isMicrosoftAccount() -> R.string.account_type_microsoft
    account.isLocalAccount() -> R.string.all_acc_type_offline
    else -> R.string.all_acc_type_server
}

@Composable
private fun AccountChip(account: Account, viewed: Boolean, active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.clip(shape).background(if (viewed) AccAccent.copy(alpha = 0.22f) else AccChip)
            .border(1.dp, if (viewed) AccAccent else Color.Transparent, shape).clickable(onClick = onClick).padding(start = 5.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace(account = account, avatarSize = 26.dp)
        Spacer(Modifier.width(8.dp))
        Text(account.username, color = AccText, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 140.dp))
        //green = the account games launch with
        if (active) { Spacer(Modifier.width(8.dp)); Box(Modifier.size(8.dp).clip(CircleShape).background(AccOk)) }
    }
}

/** Flat dark button for this window (the shared AllButton is the light XP one). */
@Composable
private fun DarkBtn(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false, danger: Boolean = false, enabled: Boolean = true) {
    Box(
        modifier.clip(RoundedCornerShape(8.dp))
            .background(if (primary && enabled) AccAccent else AccChip)
            .clickable(enabled = enabled, onClick = onClick).heightIn(min = 40.dp).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = when { danger -> AccDanger; primary && enabled -> Color.White; enabled -> AccText; else -> AccDim }, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(AccChip).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            com.movtery.zalithlauncher.ui.screens.content.elements.PlayerFace(account = account, avatarSize = 40.dp)
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
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(AccChip).padding(horizontal = 8.dp, vertical = 4.dp)) {
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
            DarkBtn(if (active) "✓ " + stringResource(R.string.all_acc_in_use) else "▶ " + stringResource(R.string.all_acc_use), onUse,
                modifier = Modifier.weight(1.3f), primary = true, enabled = !active)
            DarkBtn(if (skinBusy) "…" else "↻ " + stringResource(R.string.all_acc_refresh_skin), onRefreshSkin, modifier = Modifier.weight(1.1f), enabled = !skinBusy)
            DarkBtn("⚙ " + stringResource(R.string.all_acc_manage), onManage, modifier = Modifier.weight(1f))
            //signing out = ALL forgets this Microsoft sign-in (the real Microsoft account is untouched); offline accounts have nothing to sign out of
            if (ms) DarkBtn("⎋ " + stringResource(R.string.all_acc_logout), onLogout, modifier = Modifier.weight(1f), danger = true)
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

    Box(modifier.clip(RoundedCornerShape(6.dp)).background(AccChip)) {
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
        Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(AccCard.copy(alpha = if (enabled) 0.92f else 0.5f)).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(icon, fontSize = 18.sp, color = AccText) }
}
