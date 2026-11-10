package com.movtery.zalithlauncher.all.launchersettings

import com.movtery.zalithlauncher.all.ui.AllColors
import com.movtery.zalithlauncher.all.ui.AllButton
import com.movtery.zalithlauncher.all.ui.allStyleForColor
import com.movtery.zalithlauncher.all.ui.AllButtonStyle
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.movtery.zalithlauncher.all.ui.AlertDialog
import androidx.compose.material3.Text
import com.movtery.zalithlauncher.all.ui.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.ui.AllCard
import com.movtery.zalithlauncher.all.ui.AllToggle
import com.movtery.zalithlauncher.all.bridge.AllSubmitError
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.unit.IntSettingUnit
import com.movtery.zalithlauncher.utils.animation.TransitionAnimationType

/**
 * ALL launcher settings window, slice "Launcher / Appearance" (PHASE 3.5a).
 *
 * No new settings system: every row reads and writes the existing [AllSettings] units
 * (MMKV storage, Compose `state`), so values are shared with Classic's settings page and survive restarts.
 * Shown: colour theme, dark mode, interface mode, full screen, festival effects, desktop UI scale,
 * task menu expanded, animation speed / extent / swap type.
 * Not here (still Zalith): language (needs the activity), background image/video/blur, mirrors, logs,
 * custom colour picker.
 */

internal val StSurface: Color get() = AllColors.WindowBody
internal val StCard: Color get() = AllColors.Card
internal val StBorder: Color get() = AllColors.Border
private val StText: Color get() = AllColors.Text
internal val StDim: Color get() = AllColors.TextDim
internal val StAccent: Color get() = AllColors.Blue
private val StAccentPale: Color get() = AllColors.BluePale

@Composable
fun AllLauncherSettingsWindow(onError: AllSubmitError, modifier: Modifier = Modifier, initialTab: Int = 0) {
    var tab by remember(initialTab) { mutableStateOf(initialTab) }
    val tabs = listOf(
        stringResource(R.string.all_settings_tab_appearance),
        stringResource(R.string.all_settings_tab_renderer),
        stringResource(R.string.all_settings_tab_game),
        stringResource(R.string.all_settings_tab_java),
        stringResource(R.string.all_settings_tab_controls),
        stringResource(R.string.all_settings_tab_gamepad),
        stringResource(R.string.all_settings_tab_cursor),
        stringResource(R.string.all_unlockfps_tab)
    ) + //Frame Generation is experimental and hidden from release builds
        (if (com.movtery.zalithlauncher.BuildConfig.DEBUG) listOf(stringResource(R.string.all_fg_tab)) else emptyList())
    Column(modifier = modifier.fillMaxSize().background(StSurface)) {
        com.movtery.zalithlauncher.all.ui.AllTabs(tabs, tab, { tab = it }, Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = if (com.movtery.zalithlauncher.all.ui.LocalAllCompact.current) 2.dp else 6.dp))
        when (tab) {
            0 -> AppearanceSettings(Modifier.weight(1f))
            1 -> RendererSettings(Modifier.weight(1f))
            2 -> GameSettings(Modifier.weight(1f))
            3 -> JavaSettings(onError, Modifier.weight(1f))
            4 -> ControlSettings(onError, Modifier.weight(1f))
            5 -> GamepadSettings(onError, Modifier.weight(1f))
            6 -> CursorSettings(Modifier.weight(1f))
            7 -> UnlockFpsSettings(Modifier.weight(1f))
            else -> FrameGenerationSettings(Modifier.weight(1f))
        }
    }
}

@Composable
private fun AppearanceSettings(modifier: Modifier = Modifier) {

    LazyColumn(
        modifier = modifier.fillMaxSize().background(StSurface).padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(Modifier.padding(top = 2.dp)) }

        item {
            SwitchRow(
                title = stringResource(R.string.settings_launcher_full_screen_title),
                summary = stringResource(R.string.settings_launcher_full_screen_summary),
                checked = AllSettings.launcherFullScreen.state,
                onChange = { AllSettings.launcherFullScreen.save(it) }
            )
        }
        item {
            SwitchRow(
                title = stringResource(R.string.settings_launcher_festivals_effects_title),
                summary = stringResource(R.string.settings_launcher_festivals_effects_summary),
                checked = AllSettings.launcherFestivalEffects.state,
                onChange = { AllSettings.launcherFestivalEffects.save(it) }
            )
        }
        item {
            StepRow(
                title = stringResource(R.string.all_desktop_ui_scale),
                unit = AllSettings.allDesktopUiScale,
                step = 5,
                suffix = "%"
            )
        }
        item { Spacer(Modifier.padding(bottom = 8.dp)) }
    }

}

@Composable
internal fun Card(content: @Composable () -> Unit) {
    AllCard { content() }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun <T> ChoiceRow(
    title: String,
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    enabled: (T) -> Boolean = { true },
    note: String? = null
) {
    Card {
        Text(title, color = StText, fontWeight = FontWeight.Bold)
        note?.let { Text(it, color = StDim, fontSize = 12.sp) }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 6.dp)
        ) {
            options.forEach { option ->
                com.movtery.zalithlauncher.all.ui.AllChoiceChip(label(option), option == selected, { onSelect(option) }, enabled = enabled(option))
            }
        }
    }
}

@Composable
internal fun SwitchRow(title: String, summary: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = StText, fontWeight = FontWeight.Bold)
                summary?.let { Text(it, color = StDim, fontSize = 12.sp) }
            }
            Spacer(Modifier.width(8.dp))
            AllToggle(
                checked = checked,
                onChange = onChange,
                onLabel = stringResource(R.string.generic_enable),
                offLabel = stringResource(R.string.generic_disable)
            )
        }
    }
}

@Composable
internal fun StepRow(title: String, unit: IntSettingUnit, step: Int, suffix: String) {
    val value = unit.state
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = StText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            StepButton("−") { unit.save(value - step) }
            Text("$value$suffix", color = StText, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
            StepButton("+") { unit.save(value + step) }
        }
        Text("${unit.valueRange.first}–${unit.valueRange.last}", color = StDim, fontSize = 11.sp)
    }
}

@Composable
private fun StepButton(text: String, onClick: () -> Unit) =
    AllButton(text, onClick, style = AllButtonStyle.Primary, compact = true, modifier = Modifier.width(48.dp))
