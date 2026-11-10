package com.movtery.zalithlauncher.all.launchersettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.game.renderer.renderers.MobileGluesRenderer
import com.movtery.zalithlauncher.setting.AllSettings

/**
 * ALL Frame Generation settings. Separate from the ALL Render tab and from shader settings: the native module only
 * reads the `ALL_FG` environment variable that [GameLauncher] sets from this value. "Auto" is not offered because
 * there is no real suitability check yet.
 */
@Composable
internal fun FrameGenerationSettings(modifier: Modifier = Modifier) {
    val mode = AllSettings.frameGeneration.state
    val rendererId = AllSettings.renderer.state
    val usesMobileGlues = rendererId == MobileGluesRenderer.getUniqueIdentifier()
    val options = listOf("off", "on")
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { FgNote(stringResource(R.string.all_perf_outer_note)) }
        item {
            ChoiceRow(
                title = stringResource(R.string.all_fg_title),
                options = options,
                selected = if (mode == "on") "on" else "off",
                label = { stringResource(if (it == "on") R.string.all_fg_on else R.string.all_fg_off) },
                onSelect = { AllSettings.frameGeneration.save(it) },
                note = stringResource(R.string.all_fg_note)
            )
        }
        item {
            FgNote(
                stringResource(if (usesMobileGlues) R.string.all_fg_status_ok else R.string.all_fg_status_unsupported)
            )
        }
    }
}

@Composable
private fun FgNote(text: String) {
    androidx.compose.material3.Text(text, color = StDim, fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp))
}
