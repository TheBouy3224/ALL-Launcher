package com.movtery.zalithlauncher.all.launchersettings

import androidx.compose.ui.graphics.Brush

import com.movtery.zalithlauncher.all.ui.AllColors
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

private val ExText: Color get() = AllColors.Text
private val ExAccentPale: Color get() = AllColors.BluePale

/** Global memory allocation: same unit as Classic (null = never set, the effective value then is the minimum).
 * Tap the MB number to type an exact value; it is saved only on confirm and only inside 256..max. The slider still works as before. */
@Composable
internal fun GlobalRamRow() {
    val context = LocalContext.current
    val max = remember { getMaxMemoryForSettings(context).coerceAtLeast(256) }
    val saved = AllSettings.ramAllocation.state ?: AllSettings.ramAllocation.valueRange.first
    var draft by remember(saved) { mutableStateOf(saved.coerceIn(256, max).toFloat()) }
    var editing by remember { mutableStateOf(false) }
    var typed by remember(saved) { mutableStateOf("") }
    val typedValue = typed.toIntOrNull()
    val valid = typedValue != null && typedValue in 256..max
    Card {
        Text(stringResource(R.string.settings_game_java_memory_title), color = ExText, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.settings_game_java_memory_summary), color = StDim, fontSize = 12.sp)
        if (!editing) Text(
            "${draft.roundToInt()} MB", color = StAccent, fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable {
                typed = draft.roundToInt().toString()
                editing = true
            }.padding(vertical = 4.dp)
        )
        else Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BasicTextField(
                value = typed, onValueChange = { typed = it.filter(Char::isDigit).take(5) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp), cursorBrush = SolidColor(AllColors.BlueLight),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AllColors.Card)
                    .border(1.dp, if (typed.isNotEmpty() && !valid) AllColors.Danger else AllColors.BlueLight, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
            if (typed.isNotEmpty() && !valid) Text(
                stringResource(R.string.all_ram_range_error, 256, max), color = AllColors.Danger, fontSize = 11.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.generic_cancel), color = ExText, fontSize = 13.sp,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(AllColors.Card)
                        .clickable { editing = false }.padding(vertical = 9.dp)
                        , textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    stringResource(R.string.generic_confirm), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (valid) AllColors.BlueLight else AllColors.Card)
                        .clickable(enabled = valid) {
                            val v = typedValue!!
                            draft = v.toFloat()
                            AllSettings.ramAllocation.save(v)
                            editing = false
                        }.padding(vertical = 9.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
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
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun GlobalTextRow(title: String, summary: String, unit: StringSettingUnit, multiline: Boolean = false, placeholder: String? = null) {
    //local draft so the IME keeps its cursor/composition while typing; every edit is still written to the setting right away
    var draft by remember(unit) { mutableStateOf(unit.state) }
    //when the field gets focus, ask the existing scroll/IME handling to bring the WHOLE box into view (not just the caret line) once the keyboard has opened
    val bring = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    //while THIS field has focus the activity resizes above the keyboard (instead of panning), so the desktop window shrinks and its own list scrolls the field into view
    val activityWindow = (androidx.compose.ui.platform.LocalContext.current as? android.app.Activity)?.window
    val savedMode = remember { activityWindow?.attributes?.softInputMode }
    androidx.compose.runtime.DisposableEffect(activityWindow) {
        onDispose { savedMode?.let { activityWindow?.setSoftInputMode(it) } }
    }
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
                .bringIntoViewRequester(bring)
                .onFocusChanged {
                    if (it.isFocused) {
                        activityWindow?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                        scope.launch { delay(350); bring.bringIntoView() }
                    } else savedMode?.let { m -> activityWindow?.setSoftInputMode(m) }
                }
                .heightIn(min = if (multiline) 76.dp else 44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(androidx.compose.ui.graphics.Color(0xFF232B3D))
                .border(1.dp, AllColors.BorderStrong, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            //thin frame + a visible hint while empty, so it reads as a text field
            decorationBox = { inner ->
                androidx.compose.foundation.layout.Box {
                    if (draft.isEmpty()) Text(
                        placeholder ?: androidx.compose.ui.res.stringResource(R.string.all_field_placeholder),
                        color = StDim, fontSize = 14.sp
                    )
                    inner()
                }
            }
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
            textStyle = TextStyle(color = if (valid) ExText else AllColors.Danger, fontSize = 14.sp),
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
