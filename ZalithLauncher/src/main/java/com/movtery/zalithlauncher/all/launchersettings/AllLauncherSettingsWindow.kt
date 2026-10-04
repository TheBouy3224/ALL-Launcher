package com.movtery.zalithlauncher.all.launchersettings

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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.bridge.AllSubmitError
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.DarkMode
import com.movtery.zalithlauncher.setting.enums.InterfaceMode
import com.movtery.zalithlauncher.setting.unit.IntSettingUnit
import com.movtery.zalithlauncher.ui.theme.ColorThemeType
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

internal val StSurface = Color(0xFFF3F1E8)
internal val StCard = Color(0xFFFFFFFF)
internal val StBorder = Color(0xFFB9B4A2)
private val StText = Color(0xFF1B1B1B)
internal val StDim = Color(0xFF6B675C)
private val StAccent = Color(0xFF316AC5)
private val StAccentPale = Color(0xFFE8EDF7)

@Composable
fun AllLauncherSettingsWindow(onError: AllSubmitError, modifier: Modifier = Modifier) {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.all_settings_tab_appearance),
        stringResource(R.string.all_settings_tab_renderer),
        stringResource(R.string.all_settings_tab_game),
        stringResource(R.string.all_settings_tab_java)
    )
    Column(modifier = modifier.fillMaxSize().background(StSurface)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            tabs.forEachIndexed { i, label ->
                val on = i == tab
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (on) StAccent else StCard)
                        .border(1.dp, if (on) StAccent else StBorder, RoundedCornerShape(4.dp))
                        .clickable { tab = i }
                ) { Text(label, color = if (on) Color.White else StText, fontWeight = FontWeight.Bold, maxLines = 1) }
            }
        }
        when (tab) {
            0 -> AppearanceSettings(Modifier.weight(1f))
            1 -> RendererSettings(Modifier.weight(1f))
            2 -> GameSettings(Modifier.weight(1f))
            else -> JavaSettings(onError, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AppearanceSettings(modifier: Modifier = Modifier) {
    var confirmClassic by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(StSurface).padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(Modifier.padding(top = 2.dp)) }

        item {
            val theme = AllSettings.launcherColorTheme.state
            ChoiceRow(
                title = stringResource(R.string.settings_launcher_color_theme_title),
                options = ColorThemeType.entries.filter { it != ColorThemeType.CUSTOM },
                selected = theme,
                enabled = { it != ColorThemeType.DYNAMIC || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S },
                label = { themeLabel(it) },
                onSelect = { AllSettings.launcherColorTheme.save(it) },
                note = if (theme == ColorThemeType.CUSTOM) stringResource(R.string.generic_custom) else null
            )
        }
        item {
            ChoiceRow(
                title = stringResource(R.string.settings_launcher_dark_mode_title),
                options = DarkMode.entries,
                selected = AllSettings.launcherDarkMode.state,
                label = { stringResource(it.textRes) },
                onSelect = { AllSettings.launcherDarkMode.save(it) }
            )
        }
        item {
            ChoiceRow(
                title = stringResource(R.string.interface_mode_title),
                options = InterfaceMode.entries,
                selected = AllSettings.launcherInterfaceMode.state,
                label = { stringResource(it.textRes) },
                onSelect = { if (it == InterfaceMode.CLASSIC) confirmClassic = true else AllSettings.launcherInterfaceMode.save(it) }
            )
        }
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
        item {
            SwitchRow(
                title = stringResource(R.string.all_settings_task_menu),
                summary = null,
                checked = AllSettings.launcherTaskMenuExpanded.state,
                onChange = { AllSettings.launcherTaskMenuExpanded.save(it) }
            )
        }
        item {
            StepRow(
                title = stringResource(R.string.settings_launcher_animate_speed_title),
                unit = AllSettings.launcherAnimateSpeed,
                step = 1,
                suffix = "x"
            )
        }
        item {
            StepRow(
                title = stringResource(R.string.settings_launcher_animate_extent_title),
                unit = AllSettings.launcherAnimateExtent,
                step = 1,
                suffix = "x"
            )
        }
        item {
            ChoiceRow(
                title = stringResource(R.string.settings_launcher_swap_animate_type_title),
                options = TransitionAnimationType.entries,
                selected = AllSettings.launcherSwapAnimateType.state,
                label = { stringResource(it.textRes) },
                onSelect = { AllSettings.launcherSwapAnimateType.save(it) }
            )
        }
        item { Spacer(Modifier.padding(bottom = 8.dp)) }
    }

    if (confirmClassic) {
        AlertDialog(
            onDismissRequest = { confirmClassic = false },
            title = { Text(stringResource(R.string.interface_mode_title)) },
            text = { Text(stringResource(R.string.interface_mode_classic)) },
            confirmButton = {
                TextButton(onClick = { AllSettings.launcherInterfaceMode.save(InterfaceMode.CLASSIC); confirmClassic = false }) {
                    Text(stringResource(R.string.generic_confirm))
                }
            },
            dismissButton = { TextButton(onClick = { confirmClassic = false }) { Text(stringResource(R.string.generic_cancel)) } }
        )
    }
}

@Composable
private fun themeLabel(t: ColorThemeType): String = stringResource(
    when (t) {
        ColorThemeType.DYNAMIC -> R.string.theme_color_dynamic
        ColorThemeType.EMBERMIRE -> R.string.theme_color_embermire
        ColorThemeType.VELVET_ROSE -> R.string.theme_color_velvet_rose
        ColorThemeType.MISTWAVE -> R.string.theme_color_mistwave
        ColorThemeType.GLACIER -> R.string.theme_color_glacier
        ColorThemeType.VERDANTFIELD -> R.string.theme_color_verdant_field
        ColorThemeType.URBAN_ASH -> R.string.theme_color_urban_ash
        ColorThemeType.VERDANT_DAWN -> R.string.theme_color_verdant_dawn
        ColorThemeType.CUSTOM -> R.string.generic_custom
    }
)

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(StCard)
            .border(1.dp, StBorder, RoundedCornerShape(6.dp))
            .padding(10.dp)
    ) { content() }
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
                val on = option == selected
                val ok = enabled(option)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (on) StAccent else StCard)
                        .border(1.dp, if (on) StAccent else StBorder, RoundedCornerShape(4.dp))
                        .clickable(enabled = ok) { onSelect(option) }
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        label(option),
                        color = if (on) Color.White else if (ok) StText else StDim,
                        fontSize = 13.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal
                    )
                }
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
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (checked) StAccent else StCard)
                    .border(1.dp, if (checked) StAccent else StBorder, RoundedCornerShape(4.dp))
                    .clickable { onChange(!checked) }
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    stringResource(if (checked) R.string.generic_enable else R.string.generic_disable),
                    color = if (checked) Color.White else StText,
                    fontWeight = FontWeight.Bold
                )
            }
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
private fun StepButton(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 40.dp)
            .width(44.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(StAccentPale)
            .border(1.dp, StAccent, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
    ) { Text(text, color = StAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
}
