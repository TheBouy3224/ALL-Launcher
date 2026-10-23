package com.movtery.zalithlauncher.all.launchersettings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.unit.IntSettingUnit
import com.movtery.zalithlauncher.setting.unit.StringSettingUnit
import com.movtery.zalithlauncher.utils.customResolutionRange
import com.movtery.zalithlauncher.utils.getRealScreenSize
import com.movtery.zalithlauncher.utils.platform.getMaxMemoryForSettings
import kotlin.math.roundToInt

/**
 * Extra GLOBAL rows of the ALL launcher settings (RC3+ "Settings integration"): memory, JVM arguments, custom version info,
 * custom resolution, shader logging, system Vulkan driver and the finer mouse / gesture / gyroscope settings.
 *
 * Everything reads and writes the existing [AllSettings] units (MMKV), exactly like Classic. Nothing is written until the user
 * changes a control (sliders save when released, text saves while typing, switches on tap); opening the page never changes a value.
 * Per-instance overrides stay in the instance Settings window and fall back to these globals.
 */

private val ExText = Color(0xFF1B1B1B)
private val ExAccentPale = Color(0xFFE8EDF7)

/** Global memory allocation: same unit as Classic (null = never set, the effective value then is the minimum) */
@Composable
internal fun GlobalRamRow() {
    val context = LocalContext.current
    val max = remember { getMaxMemoryForSettings(context).coerceAtLeast(256) }
    val saved = AllSettings.ramAllocation.state ?: AllSettings.ramAllocation.valueRange.first
    var draft by remember(saved) { mutableStateOf(saved.coerceIn(256, max).toFloat()) }
    Card {
        Text(stringResource(R.string.settings_game_java_memory_title), color = ExText, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.settings_game_java_memory_summary), color = StDim, fontSize = 12.sp)
        Text("${draft.roundToInt()} MB", color = StAccent, fontWeight = FontWeight.Bold)
        Slider(
            value = draft,
            onValueChange = { draft = it },
            onValueChangeFinished = { AllSettings.ramAllocation.save(draft.roundToInt()) },
            valueRange = 256f..max.toFloat()
        )
        Text("256 – $max MB", color = StDim, fontSize = 11.sp)
    }
}

/** Free text bound to a String setting (saved while typing) */
@Composable
internal fun GlobalTextRow(title: String, summary: String, unit: StringSettingUnit, multiline: Boolean = false) {
    //local draft so the IME keeps its cursor/composition while typing; every edit is still written to the setting right away
    var draft by remember(unit) { mutableStateOf(unit.state) }
    Card {
        Text(title, color = ExText, fontWeight = FontWeight.Bold)
        Text(summary, color = StDim, fontSize = 12.sp)
        BasicTextField(
            value = draft,
            onValueChange = { draft = it; unit.save(it) },
            singleLine = !multiline,
            textStyle = TextStyle(color = ExText, fontSize = 14.sp),
            cursorBrush = SolidColor(StAccent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .heightIn(min = if (multiline) 76.dp else 44.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ExAccentPale)
                .padding(10.dp)
        )
    }
}

/** Number box: only a value inside [range] is saved, so a half-typed number never reaches the setting */
@Composable
private fun NumberField(label: String, unit: IntSettingUnit, range: IntRange, modifier: Modifier = Modifier) {
    var text by remember(unit.state) { mutableStateOf(unit.state.toString()) }
    val valid = text.toIntOrNull()?.let { it in range } == true
    Column(modifier) {
        Text(label, color = StDim, fontSize = 12.sp)
        BasicTextField(
            value = text,
            onValueChange = { new ->
                if (new.length <= 6 && new.all { it.isDigit() }) {
                    text = new
                    new.toIntOrNull()?.takeIf { it in range }?.let { unit.save(it) }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(color = if (valid) ExText else Color(0xFFB3261E), fontSize = 14.sp),
            cursorBrush = SolidColor(StAccent),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(ExAccentPale)
                .padding(10.dp)
        )
        Text("${range.first}–${range.last}", color = StDim, fontSize = 11.sp)
    }
}

/** Width / height of the "custom" resolution rule */
@Composable
internal fun CustomResolutionRows() {
    val size = getRealScreenSize(LocalContext.current)
    Card {
        Text(stringResource(R.string.settings_renderer_resolution_custom_summary), color = StDim, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
            NumberField(
                stringResource(R.string.settings_renderer_resolution_custom_width),
                AllSettings.customResolutionWidth, customResolutionRange(size.width), Modifier.weight(1f)
            )
            NumberField(
                stringResource(R.string.settings_renderer_resolution_custom_height),
                AllSettings.customResolutionHeight, customResolutionRange(size.height), Modifier.weight(1f)
            )
        }
    }
}

/** Mouse / gesture / gyroscope settings that Classic has and the first ALL Controls tab left out */
internal fun androidx.compose.foundation.lazy.LazyListScope.finerControlRows() {
    item {
        ChoiceRow(
            title = stringResource(R.string.settings_control_mouse_control_mode_title),
            options = com.movtery.zalithlauncher.setting.enums.MouseControlMode.entries,
            selected = AllSettings.mouseControlMode.state,
            label = { stringResource(it.nameRes) },
            onSelect = { AllSettings.mouseControlMode.save(it) },
            note = stringResource(R.string.settings_control_mouse_control_mode_summary)
        )
    }
    item { StepRow(stringResource(R.string.settings_control_mouse_capture_sensitivity_title), AllSettings.mouseCaptureSensitivity, 5, "%") }
    item { StepRow(stringResource(R.string.settings_control_mouse_long_press_delay_title), AllSettings.mouseLongPressDelay, 50, "ms") }
    item {
        ChoiceRow(
            title = stringResource(R.string.settings_control_gesture_tap_action_title),
            options = com.movtery.zalithlauncher.setting.enums.GestureActionType.entries,
            selected = AllSettings.gestureTapMouseAction.state,
            label = { stringResource(it.nameRes) },
            onSelect = { AllSettings.gestureTapMouseAction.save(it) }
        )
    }
    item {
        ChoiceRow(
            title = stringResource(R.string.settings_control_gesture_long_press_action_title),
            options = com.movtery.zalithlauncher.setting.enums.GestureActionType.entries,
            selected = AllSettings.gestureLongPressMouseAction.state,
            label = { stringResource(it.nameRes) },
            onSelect = { AllSettings.gestureLongPressMouseAction.save(it) }
        )
    }
    item { StepRow(stringResource(R.string.settings_control_gesture_long_press_delay_title), AllSettings.gestureLongPressDelay, 50, "ms") }
    item { StepRow(stringResource(R.string.settings_control_gyroscope_sample_rate_title), AllSettings.gyroscopeSampleRate, 1, "ms") }
    item {
        SwitchRow(
            stringResource(R.string.settings_control_gyroscope_smoothing_title),
            stringResource(R.string.settings_control_gyroscope_smoothing_summary),
            AllSettings.gyroscopeSmoothing.state
        ) { AllSettings.gyroscopeSmoothing.save(it) }
    }
    item { StepRow(stringResource(R.string.settings_control_gyroscope_smoothing_window_title), AllSettings.gyroscopeSmoothingWindow, 1, "") }
    item {
        SwitchRow(
            stringResource(R.string.settings_control_gyroscope_invert_x_title),
            stringResource(R.string.settings_control_gyroscope_invert_x_summary),
            AllSettings.gyroscopeInvertX.state
        ) { AllSettings.gyroscopeInvertX.save(it) }
    }
    item {
        SwitchRow(
            stringResource(R.string.settings_control_gyroscope_invert_y_title),
            stringResource(R.string.settings_control_gyroscope_invert_y_summary),
            AllSettings.gyroscopeInvertY.state
        ) { AllSettings.gyroscopeInvertY.save(it) }
    }
}
