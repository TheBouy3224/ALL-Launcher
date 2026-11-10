package com.movtery.zalithlauncher.all.launchersettings

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.unlockfps.AllUnlockFps
import com.movtery.zalithlauncher.setting.AllSettings

/** Unlock FPS tab: one switch plus read-only facts. Separate from Frame Generation and from the renderer settings. */
@Composable
internal fun UnlockFpsSettings(modifier: Modifier = Modifier) {
    val activity = LocalContext.current as? Activity
    val status = activity?.let { AllUnlockFps.status(it) }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { UfNote(stringResource(R.string.all_perf_outer_note)) }
        item {
            SwitchRow(
                title = stringResource(R.string.all_unlockfps_title),
                summary = stringResource(R.string.all_unlockfps_summary),
                checked = AllSettings.unlockFps.state,
                onChange = { AllSettings.unlockFps.save(it) }
            )
        }
        item {
            val modes = status?.supportedHz?.joinToString(" / ") { "%.0f".format(it) } ?: "?"
            val peak = status?.systemPeakHz?.let { "%.0f Hz".format(it) } ?: stringResource(R.string.all_unlockfps_unknown)
            UfNote(stringResource(R.string.all_unlockfps_facts, modes, peak))
        }
    }
}

@Composable
private fun UfNote(text: String) {
    Text(text, color = StDim, fontSize = 12.sp)
}
