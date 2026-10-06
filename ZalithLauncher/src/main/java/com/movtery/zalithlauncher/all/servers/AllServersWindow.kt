package com.movtery.zalithlauncher.all.servers

import com.movtery.zalithlauncher.all.ui.AllButton
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.movtery.zalithlauncher.all.ui.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import com.movtery.zalithlauncher.all.ui.OutlinedTextField
import androidx.compose.material3.Text
import com.movtery.zalithlauncher.all.ui.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.multiplayer.AllServers
import com.movtery.zalithlauncher.game.version.multiplayer.ServerData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ALL servers window (PHASE 3.3d).
 *
 * All data and network logic is Zalith's: [AllServers] reads/writes servers.dat (NBT, keeps the .dat_old backup
 * and hidden entries), [ServerData.load] resolves and pings the server (Minecraft status protocol, favicon update).
 * This window only draws the list and calls add / edit / remove / save.
 */

private val SrvSurface = Color(0xFFF3F1E8)
private val SrvCard = Color(0xFFFFFFFF)
private val SrvBorder = Color(0xFFB9B4A2)
private val SrvText = Color(0xFF1B1B1B)
private val SrvDim = Color(0xFF6B675C)
private val SrvAccent = Color(0xFF316AC5)
private val SrvOk = Color(0xFF2E7D32)
private val SrvDanger = Color(0xFFB3261E)

class AllServersViewModel(private val gameDir: File) : ViewModel() {
    private val file = File(gameDir, "servers.dat")
    private val all = AllServers()
    private val mutex = Mutex()
    private val pings = mutableMapOf<ServerData, Job>()

    val servers = mutableStateListOf<ServerData>()
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            mutex.withLock {
                withContext(Dispatchers.Main) { loading = true; error = null }
                pings.values.forEach { it.cancel() }
                pings.clear()
                all.loadServers(file)
                withContext(Dispatchers.Main) {
                    servers.clear()
                    servers.addAll(all.serverList)
                    loading = false
                    all.serverList.forEach { ping(it) }
                }
            }
        }
    }

    private fun ping(data: ServerData) {
        pings[data]?.cancel()
        pings[data] = viewModelScope.launch(Dispatchers.IO) {
            data.load(requestSave = { save() })
        }
    }

    /** Persist through Zalith's writer (temp file, servers.dat_old backup, then replace) */
    private fun save(after: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            mutex.withLock {
                gameDir.mkdirs()
                all.save(gameDir)
            }
            withContext(Dispatchers.Main) { after?.invoke() }
        }
    }

    fun add(name: String, address: String) {
        val data = ServerData(name = name, originIp = address)
        all.addServer(data)
        servers.add(data)
        save()
        ping(data)
    }

    fun edit(data: ServerData, name: String, address: String) {
        data.name = name
        data.originIp = address
        save { ping(data) }
        // ServerData is mutable; replace the slot so the list recomposes
        val i = servers.indexOf(data)
        if (i >= 0) servers[i] = data
    }

    fun delete(data: ServerData) {
        pings.remove(data)?.cancel()
        all.removeServer(data)
        servers.remove(data)
        save()
    }

    override fun onCleared() {
        pings.values.forEach { it.cancel() }
    }
}

@Composable
fun AllServersWindow(version: Version, modifier: Modifier = Modifier) {
    val gameDir = remember(version) { version.getGameDir() }
    val viewModel: AllServersViewModel = viewModel(key = "ALL_Servers_" + version.toString()) {
        AllServersViewModel(gameDir)
    }
    LaunchedEffect(Unit) { viewModel.load() }

    var editing by remember { mutableStateOf<ServerData?>(null) }
    var adding by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ServerData?>(null) }

    Column(modifier = modifier.fillMaxSize().background(SrvSurface)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            SrvButton(stringResource(R.string.servers_list_add_server), { adding = true }, SrvAccent)
            SrvButton(stringResource(R.string.generic_refresh), { viewModel.load() })
            Text("${viewModel.servers.size}", color = SrvDim, fontSize = 12.sp)
        }
        when {
            viewModel.loading && viewModel.servers.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = SrvAccent)
            }
            viewModel.servers.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(stringResource(R.string.servers_list_no_servers), color = SrvDim)
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                items(viewModel.servers, key = { System.identityHashCode(it) }) { server ->
                    ServerRow(server, onEdit = { editing = server }, onDelete = { pendingDelete = server })
                }
            }
        }
    }

    if (adding) {
        ServerDialog(
            title = stringResource(R.string.servers_list_add_server),
            initialName = stringResource(R.string.servers_list_add_server_default_name),
            initialAddress = "",
            onDismiss = { adding = false },
            onConfirm = { n, a -> viewModel.add(n, a); adding = false }
        )
    }
    editing?.let { s ->
        ServerDialog(
            title = stringResource(R.string.servers_list_edit_server),
            initialName = s.name,
            initialAddress = s.originIp,
            onDismiss = { editing = null },
            onConfirm = { n, a -> viewModel.edit(s, n, a); editing = null }
        )
    }
    pendingDelete?.let { s ->
        AlertDialog(
            destructive = true,
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.servers_list_delete_server)) },
            text = { Text(stringResource(R.string.servers_list_delete_server_text, s.name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(s); pendingDelete = null }) {
                    Text(stringResource(R.string.generic_confirm), color = SrvDanger)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.generic_cancel)) } }
        )
    }
}

@Composable
private fun ServerRow(server: ServerData, onEdit: () -> Unit, onDelete: () -> Unit) {
    // reading these makes the row recompose when the ping finishes or the icon refreshes
    val op = server.operation
    @Suppress("UNUSED_VARIABLE") val tick = server.refreshUI
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(SrvCard)
            .border(1.dp, SrvBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(server.name, color = SrvText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(server.originIp, color = SrvDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            when (op) {
                is ServerData.Operation.Loading -> Text(stringResource(R.string.generic_loading), color = SrvDim, fontSize = 12.sp)
                is ServerData.Operation.Failed -> Text(stringResource(R.string.servers_list_failed_to_connect), color = SrvDanger, fontSize = 12.sp)
                is ServerData.Operation.Loaded -> {
                    val st = op.result.status
                    Text(
                        "${st.players.online}/${st.players.max} · ${st.version.name} · ${op.result.pingMs} ms",
                        color = SrvOk, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        SrvButton(stringResource(R.string.servers_list_edit_server), onEdit)
        Spacer(Modifier.width(6.dp))
        SrvButton(stringResource(R.string.generic_delete), onDelete, SrvDanger)
    }
}

@Composable
private fun ServerDialog(
    title: String,
    initialName: String,
    initialAddress: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, address: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var address by remember { mutableStateOf(initialAddress) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, singleLine = true, label = { Text(stringResource(R.string.servers_list_add_server_name)) })
                OutlinedTextField(address, { address = it }, singleLine = true, label = { Text(stringResource(R.string.servers_list_add_server_ip)) })
            }
        },
        confirmButton = {
            TextButton(enabled = address.isNotBlank(), onClick = { onConfirm(name.trim().ifEmpty { address.trim() }, address.trim()) }) {
                Text(stringResource(R.string.generic_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.generic_cancel)) } }
    )
}

@Composable
private fun SrvButton(text: String, onClick: () -> Unit, color: Color = SrvText) =
    AllButton(text, onClick, style = allStyleForColor(color), compact = true)
