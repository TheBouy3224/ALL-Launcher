package com.movtery.zalithlauncher.all.game

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.music.AllMusicPlayer
import com.movtery.zalithlauncher.all.music.MusicRepeat
import kotlinx.coroutines.delay

private val Active = Color(0xFF2F6FED)
private val Idle = Color(0xFF232B3D)
private val InkLight = Color(0xFFE6EAF2)
private val InkDim = Color(0xFF8C97AE)

private fun clock(ms: Int): String = (ms / 1000).let { "%d:%02d".format(it / 60, it % 60) }

/** Music page of the in-game menu. Composed only while the page is open, so a closed menu costs the game nothing. */
@Composable
fun ColumnScope.AllGameMusicPage() {
    val context = LocalContext.current
    LaunchedEffect(Unit) { AllMusicPlayer.init(context) }
    val state by AllMusicPlayer.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) AllMusicPlayer.addUris(uris)
    }
    var position by remember { mutableStateOf(0 to 0) }
    LaunchedEffect(state.currentIndex, state.playing) {
        while (true) {
            position = AllMusicPlayer.positionMs()
            delay(1000)
        }
    }
    val current = state.tracks.getOrNull(state.currentIndex)

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(current?.title ?: stringResource(R.string.all_music_empty), color = InkLight, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${clock(position.first)} / ${clock(position.second)}", color = InkDim, fontSize = 12.sp)
        state.failedTitle?.let { Text(stringResource(R.string.all_music_failed, it), color = Color(0xFFFF8A8A), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val mod = Modifier.weight(1f)
            MusicButton("⇄", stringResource(R.string.all_music_shuffle), state.shuffle, mod) { AllMusicPlayer.toggleShuffle() }
            MusicButton("⏮", stringResource(R.string.all_music_previous), false, mod) { AllMusicPlayer.previous() }
            MusicButton(if (state.playing) "❚❚" else "▶", stringResource(if (state.playing) R.string.all_music_pause else R.string.all_music_play), true, mod) { AllMusicPlayer.togglePlay() }
            MusicButton("⏭", stringResource(R.string.all_music_next), false, mod) { AllMusicPlayer.next() }
            //plain text symbols: emoji glyphs have their own colour, so an off button would look on
            MusicButton(if (state.repeat == MusicRepeat.ONE) "↻1" else "↻", stringResource(R.string.all_music_repeat), state.repeat != MusicRepeat.OFF, mod) { AllMusicPlayer.cycleRepeat() }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.all_music_volume, (state.volume * 100).toInt()), color = InkDim, fontSize = 12.sp)
            Slider(state.volume, { AllMusicPlayer.setVolume(it) }, onValueChangeFinished = { AllMusicPlayer.saveSettings() }, modifier = Modifier.weight(1f))
        }
    }
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        state.tracks.forEachIndexed { index, track ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 40.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (index == state.currentIndex) Color(0x402F6FED) else Color.Transparent)
                    .clickable { AllMusicPlayer.playIndex(index) }.padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(track.title, color = InkLight, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Box(Modifier.size(40.dp).clickable { AllMusicPlayer.remove(index) }, contentAlignment = Alignment.Center) {
                    Text("✕", color = InkDim, fontSize = 14.sp)
                }
            }
        }
    }
    Box(
        Modifier.padding(8.dp).fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp)).background(Active)
            .clickable { picker.launch(arrayOf("audio/*")) },
        contentAlignment = Alignment.Center
    ) { Text("＋ " + stringResource(R.string.all_music_add), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
}

@Composable
private fun MusicButton(icon: String, label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp)).background(if (on) Active else Idle).clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(icon, color = Color.White, fontSize = 16.sp) }
}
