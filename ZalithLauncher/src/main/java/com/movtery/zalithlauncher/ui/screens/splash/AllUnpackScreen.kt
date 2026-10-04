package com.movtery.zalithlauncher.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.components.InstallableItem
import com.movtery.zalithlauncher.all.overlays.DlgButton

/** ALL-owned startup presentation over the existing unpack item/task state. */
@Composable
internal fun AllUnpackScreen(items: List<InstallableItem>, onStart: () -> Unit) {
    var installing by remember { mutableStateOf(false) }
    val running = items.any { it.state.collectAsStateWithLifecycle().value == InstallableItem.State.RUNNING }
    val surface = Color(0xFFF3F1E8)
    val ink = Color(0xFF1B1B1B)

    Row(
        Modifier.fillMaxSize().background(surface).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            Modifier.weight(7f).fillMaxHeight().background(Color.White, RoundedCornerShape(12.dp))
                .verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                val state by item.state.collectAsStateWithLifecycle()
                val message by item.task.taskMessage.collectAsStateWithLifecycle()
                Column(Modifier.fillMaxWidth().background(surface, RoundedCornerShape(8.dp)).padding(10.dp)) {
                    Text(item.name, color = ink, fontWeight = FontWeight.Bold)
                    item.summary?.let { Text(it, color = ink) }
                    val stateText = when (state) {
                        InstallableItem.State.NOT_STARTED -> stringResource(R.string.all_startup_not_installed)
                        InstallableItem.State.PENDING -> stringResource(R.string.all_startup_update_required)
                        InstallableItem.State.RUNNING -> stringResource(R.string.all_startup_unpacking)
                        InstallableItem.State.FINISHED -> stringResource(R.string.all_startup_ready)
                        InstallableItem.State.NOT_EXISTS -> stringResource(R.string.all_startup_unavailable)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (state == InstallableItem.State.RUNNING) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                        Text(stateText, color = ink)
                    }
                    if (state == InstallableItem.State.RUNNING) message?.let { Text(it, color = ink) }
                }
            }
        }

        Column(
            Modifier.weight(3f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(if (running) R.string.splash_screen_installing else R.string.splash_screen_unpack_desc), color = ink)
            DlgButton(stringResource(R.string.splash_screen_agree), onClick = { installing = true; onStart() },
                enabled = !installing && !running, modifier = Modifier.fillMaxWidth())
        }
    }
}
