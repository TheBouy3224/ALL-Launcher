package com.movtery.zalithlauncher.all.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.movtery.zalithlauncher.game.account.localLogin
import com.movtery.zalithlauncher.setting.AllSettings

/**
 * ALL accounts window (PHASE 3.4).
 *
 * Everything is Zalith's backend: [AccountsManager] (Room database, accountsFlow / currentAccountFlow),
 * [localLogin] (creates the offline account exactly like Classic, using the same default UUID),
 * [AccountsManager.setCurrentAccount] (only writes the "current account" setting; no token is read or refreshed)
 * and [AccountsManager.deleteAccount]. Offline accounts only can be created/deleted here;
 * Microsoft / auth-server login, refresh and removal stay out of this window (separate phase).
 */

private val AccSurface = Color(0xFFF3F1E8)
private val AccCard = Color(0xFFFFFFFF)
private val AccBorder = Color(0xFFB9B4A2)
private val AccText = Color(0xFF1B1B1B)
private val AccDim = Color(0xFF6B675C)
private val AccAccent = Color(0xFF316AC5)
private val AccOk = Color(0xFF2E7D32)
private val AccDanger = Color(0xFFB3261E)

/** Same rule as Classic's offline dialog: only letters, digits and underscore */
private val InvalidNameChars = Regex("[^a-zA-Z0-9_]")

@Composable
fun AllAccountsWindow(modifier: Modifier = Modifier) {
    val accounts by AccountsManager.accountsFlow.collectAsStateWithLifecycle()
    val currentId = AllSettings.currentAccount.state
    val activeId = remember(accounts, currentId) {
        accounts.find { it.uniqueUUID == currentId }?.uniqueUUID ?: accounts.firstOrNull()?.uniqueUUID
    }

    var adding by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Account?>(null) }

    Column(modifier = modifier.fillMaxSize().background(AccSurface)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            AccButton(stringResource(R.string.account_local_create_account), { adding = true }, AccAccent)
            Text("${accounts.size}", color = AccDim, fontSize = 12.sp)
        }
        if (accounts.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text(stringResource(R.string.account_add_new_account), color = AccDim) }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                items(accounts, key = { it.uniqueUUID }) { account ->
                    AccountRow(
                        account = account,
                        active = account.uniqueUUID == activeId,
                        onSelect = { AccountsManager.setCurrentAccount(account) },
                        onDelete = { pendingDelete = account }
                    )
                }
            }
        }
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
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.account_delete_title)) },
            text = { Text(stringResource(R.string.account_delete_message, account.username)) },
            confirmButton = {
                TextButton(onClick = { AccountsManager.deleteAccount(account); pendingDelete = null }) {
                    Text(stringResource(R.string.generic_confirm), color = AccDanger)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.generic_cancel)) } }
        )
    }
}

@Composable
private fun AccountRow(account: Account, active: Boolean, onSelect: () -> Unit, onDelete: () -> Unit) {
    val type = when {
        account.isLocalAccount() -> stringResource(R.string.account_type_local)
        account.isMicrosoftAccount() -> stringResource(R.string.account_type_microsoft)
        else -> account.otherBaseUrl ?: "?"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) Color(0xFFE8F1E8) else AccCard)
            .border(1.dp, if (active) AccOk else AccBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(if (active) AccOk else AccBorder))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(account.username, color = AccText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(type + if (active) " · ✓" else "", color = AccDim, fontSize = 12.sp, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        if (!active) AccButton(stringResource(R.string.all_accounts_select), onSelect)
        // only offline accounts can be removed here; Microsoft / auth-server removal belongs to the login phase
        if (account.isLocalAccount()) {
            Spacer(Modifier.width(6.dp))
            AccButton(stringResource(R.string.generic_delete), onDelete, AccDanger)
        }
    }
}

@Composable
private fun AccButton(text: String, onClick: () -> Unit, color: Color = AccText) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(AccCard)
            .border(1.dp, if (color == AccText) AccBorder else color, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
    ) { Text(text, color = color, fontWeight = FontWeight.Bold, maxLines = 1) }
}
