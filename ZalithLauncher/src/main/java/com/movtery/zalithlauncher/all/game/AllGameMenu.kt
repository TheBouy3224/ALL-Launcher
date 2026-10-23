package com.movtery.zalithlauncher.all.game

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.control.ControlManager
import com.movtery.zalithlauncher.game.sdl.SdlBridge
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.GamepadInputMode
import com.movtery.zalithlauncher.setting.enums.GestureActionType
import com.movtery.zalithlauncher.setting.enums.MouseControlMode
import com.movtery.zalithlauncher.setting.enums.ResolutionRule
import com.movtery.zalithlauncher.setting.unit.IntSettingUnit
import com.movtery.zalithlauncher.setting.unit.floatRange
import com.movtery.zalithlauncher.ui.AndroidStringText
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.components.MenuState
import com.movtery.zalithlauncher.ui.control.HotbarRule
import com.movtery.zalithlauncher.ui.control.event.lwjglEvent
import com.movtery.zalithlauncher.ui.control.gyroscope.isGyroscopeAvailable
import com.movtery.zalithlauncher.utils.customResolutionRange
import com.movtery.zalithlauncher.utils.ensureCustomResolutionInitialized
import com.movtery.zalithlauncher.utils.getRealScreenSize
import com.movtery.zalithlauncher.viewmodel.GamepadViewModel
import java.io.File
import kotlin.math.roundToInt

/**
 * ALL in-game quick menu (Windows XP Luna). A UI rework of Zalith's GameMenuSubscreen: one side panel (<= 40% of the screen),
 * the same AllSettings keys, the same callbacks into GameScreen and the same input backends. Every one of the 46 audited functions
 * is still reachable (see ALL_CORE_CHECKPOINT "In-Game Quick Menu V1" for the mapping). Nothing here pauses the game.
 */

private val Luna = Brush.verticalGradient(listOf(Color(0xFF3D95FF), Color(0xFF0A5BE0), Color(0xFF0042C8)))
private val LunaBorder = Color(0xFF0831D9)
private val Body = Color(0xFFECE9D8)
private val Card = Color(0xFFFFFFFF)
private val CardBorder = Color(0xFFB9B4A2)
private val Ink = Color(0xFF1B1B1B)
private val Dim = Color(0xFF6B675C)
private val Blue = Color(0xFF316AC5)
private val Danger = Color(0xFFC62828)
private val Warn = Color(0xFFB7791F)

private enum class Page { MAIN, PROFILES, KEYS, SETTINGS }
private enum class SettingsTab(val icon: String, val label: Int) {
    CONTROL("🎮", R.string.all_gm_tab_control), DISPLAY("🖥", R.string.all_gm_tab_display),
    TOOLS("🛠", R.string.all_gm_tab_tools), ADVANCED("⚙", R.string.all_gm_tab_advanced)
}
private enum class ControlTab(val label: Int) {
    MOUSE(R.string.all_gm_sub_mouse), GESTURE(R.string.all_gm_sub_gesture), HOTBAR(R.string.all_gm_sub_hotbar),
    GAMEPAD(R.string.all_gm_sub_gamepad), GYRO(R.string.all_gm_sub_gyro)
}

@Composable
fun AllGameMenu(
    state: MenuState,
    gamepadViewModel: GamepadViewModel,
    currentControlFile: File?,
    instanceControlName: String?,
    closeScreen: () -> Unit,
    onForceCloseConfirmed: () -> Unit,
    onSwitchLog: () -> Unit,
    enableTerracotta: Boolean,
    onOpenTerracottaMenu: () -> Unit,
    onRefreshWindowSize: () -> Unit,
    onInputMethod: () -> Unit,
    onFullKeyboard: () -> Unit,
    onPickLayout: (File) -> Unit,
    onSetInstanceLayout: (File) -> Unit,
    onOpenStudio: () -> Unit,
    onOpenClassicEditor: () -> Unit,
    onShowToast: (AndroidStringText, Int) -> Unit
) {
    var page by remember { mutableStateOf(Page.MAIN) }
    var tab by remember { mutableStateOf(SettingsTab.CONTROL) }
    var sub by remember { mutableStateOf(ControlTab.MOUSE) }
    var confirmForceClose by remember { mutableStateOf(false) }
    val visible = state == MenuState.SHOW
    LaunchedEffect(visible) { if (!visible) page = Page.MAIN }
    //VMActivity ignores Back while the menu is open (key handling is off), so the menu handles it: sub-page -> main -> close
    androidx.activity.compose.BackHandler(enabled = visible) { if (page != Page.MAIN) page = Page.MAIN else closeScreen() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        //tapping the game beside the panel closes the menu (like the old scrim); nothing is sent to the game
        if (visible) Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null, onClick = closeScreen))
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally { -it } + fadeIn(),
            exit = slideOutHorizontally { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            //target: at most 40% of the real screen, but never narrower than a readable 300dp
            val panelW = (maxWidth * 0.4f).coerceIn(300.dp, 440.dp).coerceAtMost(maxWidth)
            Column(
                Modifier.padding(6.dp).width(panelW).fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                    .border(2.dp, LunaBorder, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(Body)
                    .clickable(remember { MutableInteractionSource() }, null) {}
            ) {
                TitleBar(
                    title = stringResource(when (page) {
                        Page.MAIN -> R.string.all_gm_title
                        Page.PROFILES -> R.string.all_gm_profiles
                        Page.KEYS -> R.string.all_gm_keys
                        Page.SETTINGS -> R.string.all_gm_settings
                    }),
                    onBack = if (page != Page.MAIN) ({ page = Page.MAIN }) else null,
                    onClose = closeScreen
                )
                when (page) {
                    Page.MAIN -> MainPage(
                        onKeyboard = { onInputMethod(); closeScreen() },
                        onProfiles = { page = Page.PROFILES },
                        onStudio = { onOpenStudio(); closeScreen() },
                        onKeys = { page = Page.KEYS },
                        onSettings = { page = Page.SETTINGS },
                        onClose = closeScreen
                    )
                    Page.PROFILES -> ProfilesPage(currentControlFile, instanceControlName, onPick = { onPickLayout(it) }, onSetDefault = onSetInstanceLayout, onEdit = { onOpenStudio(); closeScreen() })
                    Page.KEYS -> KeysPage(onFullKeyboard = { onFullKeyboard(); closeScreen() })
                    Page.SETTINGS -> SettingsPage(
                        tab = tab, onTab = { tab = it }, sub = sub, onSub = { sub = it },
                        gamepadViewModel = gamepadViewModel,
                        onRefreshWindowSize = onRefreshWindowSize,
                        onShowToast = onShowToast,
                        onKeyboard = { onInputMethod(); closeScreen() },
                        onKeys = { page = Page.KEYS },
                        onLog = { onSwitchLog(); closeScreen() },
                        enableTerracotta = enableTerracotta,
                        onTerracotta = { onOpenTerracottaMenu(); closeScreen() },
                        onClassicEditor = { onOpenClassicEditor(); closeScreen() },
                        onForceClose = { confirmForceClose = true }
                    )
                }
            }
        }
    }

    if (confirmForceClose) ForceCloseDialog(onDismiss = { confirmForceClose = false }, onConfirm = { confirmForceClose = false; onForceCloseConfirmed() })
}

@Composable
private fun TitleBar(title: String, onBack: (() -> Unit)?, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(40.dp).background(Luna).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) TitleButton("‹", onBack) else Text("ALL", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.padding(start = 4.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
        TitleButton("✕", onClose, red = true)
    }
}

@Composable
private fun TitleButton(text: String, onClick: () -> Unit, red: Boolean = false) {
    Box(
        Modifier.size(32.dp).clip(RoundedCornerShape(4.dp))
            .background(if (red) Brush.verticalGradient(listOf(Color(0xFFE8826B), Color(0xFFC94328))) else Brush.verticalGradient(listOf(Color(0xFF6FA8FF), Color(0xFF2F6FE0))))
            .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
}

@Composable
private fun ColumnScope.Scroll(content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(5.dp), content = content)

// ---------- main ----------

@Composable
private fun ColumnScope.MainPage(onKeyboard: () -> Unit, onProfiles: () -> Unit, onStudio: () -> Unit, onKeys: () -> Unit, onSettings: () -> Unit, onClose: () -> Unit) {
    Scroll {
        NavRow("⌨", stringResource(R.string.all_gm_keyboard), onKeyboard)
        NavRow("🔀", stringResource(R.string.all_gm_profiles), onProfiles)
        NavRow("✏", stringResource(R.string.all_gm_studio), onStudio)
        NavRow("🔣", stringResource(R.string.all_gm_keys), onKeys)
        NavRow("⚙", stringResource(R.string.all_gm_settings), onSettings)
    }
    //fixed bottom bar: always visible without scrolling
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        //L6: whole-layout opacity; min 10% here so a child can never make every button invisible from the quick menu
        Box(Modifier.weight(1f)) { IntSlider(stringResource(R.string.all_gm_opacity_short), AllSettings.controlsOpacity, suffix = "%", minOverride = 10) }
        LunaButton("✕ " + stringResource(R.string.all_gm_close), onClose, primary = true)
    }
}

@Composable
private fun NavRow(icon: String, text: String, onClick: () -> Unit, color: Color = Ink, sub: String? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(6.dp)).background(Card)
            .border(1.dp, CardBorder, RoundedCornerShape(6.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 18.sp, modifier = Modifier.width(30.dp))
        Column(Modifier.weight(1f)) {
            Text(text, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            sub?.let { Text(it, color = Dim, fontSize = 12.sp) }
        }
        Text("›", color = Blue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LunaButton(text: String, onClick: () -> Unit, primary: Boolean = false, danger: Boolean = false, enabled: Boolean = true, modifier: Modifier = Modifier) {
    val face = when {
        !enabled -> Brush.verticalGradient(listOf(Color(0xFFF5F4EF), Color(0xFFF5F4EF)))
        danger -> Brush.verticalGradient(listOf(Color(0xFFF08A6E), Color(0xFFD03A1E)))
        primary -> Brush.verticalGradient(listOf(Color(0xFF6FA8FF), Color(0xFF2B66D9)))
        else -> Brush.verticalGradient(listOf(Color.White, Color(0xFFECEBE5), Color(0xFFD6D0C5)))
    }
    val ink = if (!enabled) Color(0xFF9A9A8E) else if (primary || danger) Color.White else Ink
    Box(
        modifier.heightIn(min = 44.dp).widthIn(min = 88.dp).clip(RoundedCornerShape(4.dp)).background(face)
            .border(1.dp, if (enabled) Color(0xFF003C74) else Color(0xFFC9C7BA), RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
}

// ---------- change layout ----------

@Composable
private fun ColumnScope.ProfilesPage(current: File?, instanceDefault: String?, onPick: (File) -> Unit, onSetDefault: (File) -> Unit, onEdit: () -> Unit) {
    val dataList by ControlManager.dataList.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val controls = remember(dataList) { dataList.filter { it.isSupport } }
    Scroll {
        Text(stringResource(R.string.all_gm_profiles_hint), color = Dim, fontSize = 12.sp)
        if (controls.isEmpty()) {
            Text(stringResource(R.string.control_manage_list_empty), color = Dim, fontSize = 14.sp)
            LunaButton(stringResource(R.string.generic_refresh), { ControlManager.refresh() })
        }
        controls.forEach { data ->
            val name = data.controlLayout.info.name.translate(locale).ifBlank { data.file.nameWithoutExtension }
            val active = current?.name == data.file.name
            val buttons = data.controlLayout.layers.value.sumOf { it.normalButtons.value.size + it.joystickButtons.value.size }
            //same names happen (presets): the file name and button count always tell them apart
            val detail = stringResource(R.string.all_gm_profile_buttons, buttons) + " · " + data.file.name
            val isDefault = instanceDefault == data.file.name
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (active) Color(0xFFDDEBFF) else Card)
                    .border(if (active) 2.dp else 1.dp, if (active) Blue else CardBorder, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(name, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(detail, color = Dim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (active) Text("✓ " + stringResource(R.string.all_gm_in_use), color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        if (isDefault) Text("★ " + stringResource(R.string.all_gm_instance_default), color = Warn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                //two explicit choices: try it for this session, or make it what this game always opens with
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LunaButton(stringResource(R.string.all_gm_use_temporary), { onPick(data.file) }, enabled = !active, modifier = Modifier.weight(1f))
                    LunaButton("★ " + stringResource(R.string.all_gm_set_default), { onSetDefault(data.file) }, primary = true, enabled = !isDefault, modifier = Modifier.weight(1f))
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
        LunaButton("✏ " + stringResource(R.string.all_gm_studio), onEdit, primary = true)
    }
}

// ---------- special keys ----------

private val QuickKeys = listOf(
    "ESC" to "GLFW_KEY_ESCAPE", "TAB" to "GLFW_KEY_TAB", "F3" to "GLFW_KEY_F3",
    "F5" to "GLFW_KEY_F5", "CHAT" to "GLFW_KEY_T", "F1" to "GLFW_KEY_F1",
    "INV" to "GLFW_KEY_E", "SHIFT" to "GLFW_KEY_LEFT_SHIFT", "CTRL" to "GLFW_KEY_LEFT_CONTROL",
    "ALT" to "GLFW_KEY_LEFT_ALT", "SPACE" to "GLFW_KEY_SPACE", "ENTER" to "GLFW_KEY_ENTER"
)

@Composable
private fun ColumnScope.KeysPage(onFullKeyboard: () -> Unit) {
    //held keys are released when the page closes, like the old Send Keycode keyboard
    val held = remember { mutableStateListOf<String>() }
    DisposableEffect(Unit) { onDispose { held.forEach { lwjglEvent(it, isMouse = false, isPressed = false) } } }
    Scroll {
        Text(stringResource(R.string.all_gm_keys_hint), color = Dim, fontSize = 12.sp)
        QuickKeys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (label, key) ->
                    val down = key in held
                    Box(
                        Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(4.dp))
                            .background(if (down) Brush.verticalGradient(listOf(Color(0xFF6FA8FF), Color(0xFF2B66D9))) else Brush.verticalGradient(listOf(Color.White, Color(0xFFD6D0C5))))
                            .border(1.dp, Color(0xFF003C74), RoundedCornerShape(4.dp))
                            .pointerInput(key) {
                                //press on touch, release on lift: same press/release events the old keyboard sends (holding works)
                                awaitEachGesture {
                                    awaitFirstDown()
                                    held.add(key); lwjglEvent(key, isMouse = false, isPressed = true)
                                    waitForUpOrCancellation()
                                    held.remove(key); lwjglEvent(key, isMouse = false, isPressed = false)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) { Text(label, color = if (down) Color.White else Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
        LunaButton("⌨ " + stringResource(R.string.all_gm_full_keyboard), onFullKeyboard, primary = true)
    }
}

// ---------- settings ----------

@Composable
private fun ColumnScope.SettingsPage(
    tab: SettingsTab, onTab: (SettingsTab) -> Unit, sub: ControlTab, onSub: (ControlTab) -> Unit,
    gamepadViewModel: GamepadViewModel, onRefreshWindowSize: () -> Unit, onShowToast: (AndroidStringText, Int) -> Unit,
    onKeyboard: () -> Unit, onKeys: () -> Unit, onLog: () -> Unit, enableTerracotta: Boolean, onTerracotta: () -> Unit,
    onClassicEditor: () -> Unit, onForceClose: () -> Unit
) {
    TabStrip(SettingsTab.entries, tab, { it.icon + "\n" + stringResource(it.label) }, onTab, tall = true)
    if (tab == SettingsTab.CONTROL) TabStrip(ControlTab.entries, sub, { stringResource(it.label) }, onSub)
    //each tab starts at the top (a shared scroll state would open the next tab half-way down)
    androidx.compose.runtime.key(tab, sub) { Scroll {
        when (tab) {
            SettingsTab.CONTROL -> when (sub) {
                ControlTab.MOUSE -> MouseSettings()
                ControlTab.GESTURE -> GestureSettings()
                ControlTab.HOTBAR -> HotbarSettings()
                ControlTab.GAMEPAD -> GamepadSettings(gamepadViewModel)
                ControlTab.GYRO -> GyroSettings()
            }
            SettingsTab.DISPLAY -> DisplaySettings(onRefreshWindowSize, onShowToast)
            SettingsTab.TOOLS -> {
                NavRow("⌨", stringResource(R.string.all_gm_keyboard), onKeyboard)
                NavRow("🔣", stringResource(R.string.all_gm_keys), onKeys)
                NavRow("📜", stringResource(R.string.all_gm_log), onLog, sub = stringResource(R.string.all_gm_log_sub))
                //R3: same condition as before (enableTerracotta); not exposed anywhere new
                if (enableTerracotta) NavRow("🌐", stringResource(R.string.terracotta_menu), onTerracotta)
            }
            SettingsTab.ADVANCED -> AdvancedSettings(onClassicEditor, onForceClose)
        }
    } }
}

@Composable
private fun <T> TabStrip(items: List<T>, current: T, label: @Composable (T) -> String, onPick: (T) -> Unit, tall: Boolean = false) {
    Row(Modifier.fillMaxWidth().background(Color(0xFFD4D0C8)).padding(start = 4.dp, end = 4.dp, top = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        items.forEach { item ->
            val on = item == current
            Box(
                Modifier.weight(1f).heightIn(min = if (tall) 48.dp else 36.dp)
                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                    .background(if (on) Body else Color(0xFFF1EFE6))
                    .border(1.dp, if (on) Color(0xFF919B9C) else Color(0xFFC0BDB3), RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                    .then(if (on) Modifier.background(Brush.verticalGradient(listOf(Color(0xFFFFC73C), Color.Transparent), endY = 10f)) else Modifier)
                    .clickable { onPick(item) }.padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label(item), color = if (on) Blue else Ink, fontSize = if (tall) 12.sp else 12.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center, maxLines = 2, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
private fun MouseSettings() {
    val mode = AllSettings.mouseControlMode.state
    Choice(stringResource(R.string.settings_control_mouse_control_mode_title), MouseControlMode.entries, mode, { stringResource(it.nameRes) }) { AllSettings.mouseControlMode.save(it) }
    Toggle(stringResource(R.string.settings_control_mouse_hide_title), AllSettings.hideMouse.state, enabled = mode == MouseControlMode.CLICK,
        reason = stringResource(R.string.all_gm_reason_tap_only)) { AllSettings.hideMouse.save(it) }
    Toggle(stringResource(R.string.settings_control_mouse_enable_click_title), AllSettings.enableMouseClick.state, enabled = mode == MouseControlMode.SLIDE,
        reason = stringResource(R.string.all_gm_reason_swipe_only)) { AllSettings.enableMouseClick.save(it) }
    IntSlider(stringResource(R.string.settings_control_mouse_size_title), AllSettings.mouseSize, suffix = "dp")
    IntSlider(stringResource(R.string.settings_control_mouse_sensitivity_title), AllSettings.cursorSensitivity, suffix = "%")
    IntSlider(stringResource(R.string.all_gm_camera_sensitivity), AllSettings.mouseCaptureSensitivity, suffix = "%")
    IntSlider(stringResource(R.string.all_gm_mouse_long_press), AllSettings.mouseLongPressDelay, suffix = "ms")
}

@Composable
private fun GestureSettings() {
    val on = AllSettings.gestureControl.state
    val off = stringResource(R.string.all_gm_reason_gesture_off)
    Toggle(stringResource(R.string.settings_control_gesture_control_title), on) { AllSettings.gestureControl.save(it) }
    Choice(stringResource(R.string.settings_control_gesture_tap_action_title), GestureActionType.entries, AllSettings.gestureTapMouseAction.state, { stringResource(it.nameRes) },
        enabled = on, reason = off) { AllSettings.gestureTapMouseAction.save(it) }
    Choice(stringResource(R.string.settings_control_gesture_long_press_action_title), GestureActionType.entries, AllSettings.gestureLongPressMouseAction.state, { stringResource(it.nameRes) },
        enabled = on, reason = off) { AllSettings.gestureLongPressMouseAction.save(it) }
    IntSlider(stringResource(R.string.all_gm_gesture_long_press), AllSettings.gestureLongPressDelay, suffix = "ms", enabled = on, reason = off)
}

@Composable
private fun HotbarSettings() {
    val custom = AllSettings.hotbarRule.state == HotbarRule.Custom
    val notCustom = stringResource(R.string.all_gm_reason_hotbar_custom)
    Text(stringResource(R.string.all_gm_hotbar_hint), color = Dim, fontSize = 12.sp)
    Choice(stringResource(R.string.game_menu_option_hotbar_rule), HotbarRule.entries, AllSettings.hotbarRule.state, { stringResource(it.nameRes) }) { AllSettings.hotbarRule.save(it) }
    //stored as 0..1000 (per mille), shown as %
    PermilleSlider(stringResource(R.string.game_menu_option_hotbar_width), AllSettings.hotbarWidth, enabled = custom, reason = notCustom)
    PermilleSlider(stringResource(R.string.game_menu_option_hotbar_height), AllSettings.hotbarHeight, enabled = custom, reason = notCustom)
    Toggle(stringResource(R.string.game_menu_option_hotbar_double_click), AllSettings.hotbarDoubleClick.state) { AllSettings.hotbarDoubleClick.save(it) }
    Toggle(stringResource(R.string.game_menu_option_hotbar_long_click), AllSettings.hotbarLongClick.state) { AllSettings.hotbarLongClick.save(it) }
    IntSlider(stringResource(R.string.game_menu_option_hotbar_long_click_delay), AllSettings.hotbarLongClickDelay, suffix = "ms",
        enabled = AllSettings.hotbarLongClick.state, reason = stringResource(R.string.all_gm_reason_long_click_off))
}

@Composable
private fun GamepadSettings(gamepadViewModel: GamepadViewModel) {
    val on = AllSettings.gamepadControl.state
    val mapped = on && AllSettings.gamepadInputMode.state == GamepadInputMode.Mapped
    val notMapped = stringResource(if (on) R.string.all_gm_reason_sdl_mode else R.string.all_gm_reason_gamepad_off)
    if (!gamepadViewModel.gamepadEngaged) Text("ℹ " + stringResource(R.string.all_gm_no_gamepad), color = Dim, fontSize = 12.sp)
    Toggle(stringResource(R.string.settings_gamepad_title), on) { AllSettings.gamepadControl.save(it) }
    Choice(stringResource(R.string.settings_gamepad_input_mode_title), GamepadInputMode.entries, AllSettings.gamepadInputMode.state, { stringResource(it.titleRes) },
        enabled = on, reason = stringResource(R.string.all_gm_reason_gamepad_off)) { AllSettings.gamepadInputMode.save(it) }
    IntSlider(stringResource(R.string.settings_gamepad_cursor_sensitivity_title), AllSettings.gamepadCursorSensitivity, suffix = "%", enabled = mapped, reason = notMapped)
    IntSlider(stringResource(R.string.settings_gamepad_camera_sensitivity_title), AllSettings.gamepadCameraSensitivity, suffix = "%", enabled = mapped, reason = notMapped)
    val configs = remember(gamepadViewModel) { gamepadViewModel.getAllConfigKeys() }
    if (configs.isNotEmpty()) Choice(stringResource(R.string.settings_gamepad_config_title), configs, AllSettings.gamepadMappingConfig.state, { it },
        enabled = mapped, reason = notMapped) { AllSettings.gamepadMappingConfig.save(it); gamepadViewModel.reloadAllMappings() }
    else Disabled(stringResource(R.string.settings_gamepad_config_title), stringResource(R.string.settings_gamepad_config_no_items))
}

@Composable
private fun GyroSettings() {
    val context = LocalContext.current
    val available = remember(context) { isGyroscopeAvailable(context) }
    val on = available && AllSettings.gyroscopeControl.state
    val reason = stringResource(if (!available) R.string.all_gm_reason_no_gyro else R.string.all_gm_reason_gyro_off)
    Toggle(stringResource(R.string.settings_control_gyroscope_title), AllSettings.gyroscopeControl.state, enabled = available,
        reason = stringResource(R.string.all_gm_reason_no_gyro)) { AllSettings.gyroscopeControl.save(it) }
    IntSlider(stringResource(R.string.settings_control_gyroscope_sensitivity_title), AllSettings.gyroscopeSensitivity, suffix = "%", enabled = on, reason = reason)
    Toggle(stringResource(R.string.all_gm_gyro_smoothing), AllSettings.gyroscopeSmoothing.state, enabled = on, reason = reason) { AllSettings.gyroscopeSmoothing.save(it) }
    Toggle(stringResource(R.string.settings_control_gyroscope_invert_x_title), AllSettings.gyroscopeInvertX.state, enabled = on, reason = reason) { AllSettings.gyroscopeInvertX.save(it) }
    Toggle(stringResource(R.string.settings_control_gyroscope_invert_y_title), AllSettings.gyroscopeInvertY.state, enabled = on, reason = reason) { AllSettings.gyroscopeInvertY.save(it) }
    Text("⚙ " + stringResource(R.string.all_gm_more_in_advanced), color = Dim, fontSize = 12.sp)
}

@Composable
private fun DisplaySettings(onRefreshWindowSize: () -> Unit, onShowToast: (AndroidStringText, Int) -> Unit) {
    val context = LocalContext.current
    Text(stringResource(R.string.all_gm_display_hint), color = Dim, fontSize = 12.sp)
    Choice(stringResource(R.string.settings_renderer_resolution_rule_title), ResolutionRule.entries, AllSettings.resolutionRule.state, { stringResource(it.nameRes) }) { rule ->
        AllSettings.resolutionRule.save(rule)
        if (rule == ResolutionRule.CUSTOM) ensureCustomResolutionInitialized(context)
        onRefreshWindowSize()
    }
    if (AllSettings.resolutionRule.state == ResolutionRule.PERCENTAGE) {
        IntSlider(stringResource(R.string.settings_renderer_resolution_scale_title), AllSettings.resolutionRatio, suffix = "%", onFinished = { onRefreshWindowSize() })
        if (AllSettings.resolutionRatio.state > 150) Text("⚠ " + stringResource(R.string.all_gm_res_warn), color = Warn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    } else {
        val screen = remember(context) { getRealScreenSize(context) }
        IntStepper(stringResource(R.string.settings_renderer_resolution_custom_width), AllSettings.customResolutionWidth, customResolutionRange(screen.width), onRefreshWindowSize)
        IntStepper(stringResource(R.string.settings_renderer_resolution_custom_height), AllSettings.customResolutionHeight, customResolutionRange(screen.height), onRefreshWindowSize)
    }
    Toggle(stringResource(R.string.game_menu_option_show_menu), AllSettings.showMenuBall.state, note = stringResource(R.string.all_gm_ball_note)) { value ->
        AllSettings.showMenuBall.save(value)
        if (!value) onShowToast(androidText(R.string.game_menu_option_show_menu_hided), Toast.LENGTH_LONG)
    }
    val ball = AllSettings.showMenuBall.state
    val ballOff = stringResource(R.string.all_gm_reason_ball_off)
    IntSlider(stringResource(R.string.all_gm_ball_opacity), AllSettings.menuBallOpacity, suffix = "%", enabled = ball, reason = ballOff)
    Toggle(stringResource(R.string.game_menu_option_switch_fps), AllSettings.showFPS.state, enabled = ball, reason = ballOff) { AllSettings.showFPS.save(it) }
    Toggle(stringResource(R.string.game_menu_option_switch_memory), AllSettings.showMemory.state, enabled = ball, reason = ballOff) { AllSettings.showMemory.save(it) }
    IntSlider(stringResource(R.string.game_menu_option_controls_opacity), AllSettings.controlsOpacity, suffix = "%")
}

@Composable
private fun AdvancedSettings(onClassicEditor: () -> Unit, onForceClose: () -> Unit) {
    val sdl by SdlBridge.enabled.collectAsState()
    val context = LocalContext.current
    val gyro = remember(context) { isGyroscopeAvailable(context) } && AllSettings.gyroscopeControl.state
    val gyroReason = stringResource(R.string.all_gm_reason_gyro_off)
    val mapped = AllSettings.gamepadControl.state && AllSettings.gamepadInputMode.state == GamepadInputMode.Mapped
    Toggle(stringResource(R.string.game_menu_option_auto_show_ime), AllSettings.sdlAutoShowIme.state, enabled = sdl,
        reason = stringResource(R.string.all_gm_reason_sdl_off)) { AllSettings.sdlAutoShowIme.save(it) }
    IntSlider(stringResource(R.string.settings_gamepad_deadzone_title), AllSettings.gamepadDeadZoneScale, suffix = "%", enabled = mapped,
        reason = stringResource(R.string.all_gm_reason_sdl_mode))
    IntSlider(stringResource(R.string.settings_control_gyroscope_sample_rate_title), AllSettings.gyroscopeSampleRate, suffix = "ms", enabled = gyro, reason = gyroReason)
    IntSlider(stringResource(R.string.settings_control_gyroscope_smoothing_window_title), AllSettings.gyroscopeSmoothingWindow, suffix = "",
        enabled = gyro && AllSettings.gyroscopeSmoothing.state, reason = gyroReason)
    NavRow("🧰", stringResource(R.string.all_gm_classic_editor), onClassicEditor, sub = stringResource(R.string.all_gm_classic_editor_sub))
    Spacer(Modifier.height(10.dp))
    //R1 last, red and separated
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFEBEE)).border(1.dp, Danger, RoundedCornerShape(6.dp)).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("⚠ " + stringResource(R.string.all_gm_danger_zone), color = Danger, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        LunaButton("✖ " + stringResource(R.string.game_button_force_close), onForceClose, danger = true, modifier = Modifier.fillMaxWidth())
    }
}

// ---------- rows ----------

@Composable
private fun RowCard(enabled: Boolean, content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (enabled) Card else Color(0xFFF4F3EE))
        .border(1.dp, CardBorder, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 6.dp), content = content)

@Composable
private fun Reason(enabled: Boolean, reason: String?, note: String? = null) {
    if (!enabled && reason != null) Text("ⓘ $reason", color = Warn, fontSize = 11.sp)
    else if (note != null) Text(note, color = Dim, fontSize = 11.sp)
}

@Composable
private fun Toggle(title: String, value: Boolean, enabled: Boolean = true, reason: String? = null, note: String? = null, onChange: (Boolean) -> Unit) = RowCard(enabled) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(enabled = enabled) { onChange(!value) }) {
        Text(title, color = if (enabled) Ink else Dim, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange, enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = Blue, checkedThumbColor = Color.White))
    }
    Reason(enabled, reason, note)
}

@Composable
private fun IntSlider(
    title: String, setting: IntSettingUnit, suffix: String, enabled: Boolean = true, reason: String? = null,
    minOverride: Int? = null, onFinished: (Int) -> Unit = {}
) = RowCard(enabled) {
    val range = setting.floatRange.let { r -> (minOverride?.toFloat()?.coerceAtLeast(r.start) ?: r.start)..r.endInclusive }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = if (enabled) Ink else Dim, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text("${setting.state}$suffix", color = if (enabled) Blue else Dim, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
    Slider(
        modifier = Modifier.height(32.dp),
        value = setting.state.toFloat().coerceIn(range), valueRange = range, enabled = enabled,
        onValueChange = { setting.updateState(it.roundToInt()) },
        onValueChangeFinished = { setting.save(setting.state); onFinished(setting.state) },
        colors = SliderDefaults.colors(thumbColor = Blue, activeTrackColor = Blue)
    )
    Reason(enabled, reason)
}

@Composable
private fun PermilleSlider(title: String, setting: IntSettingUnit, enabled: Boolean, reason: String?) = RowCard(enabled) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = if (enabled) Ink else Dim, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text("${setting.state / 10f}%", color = if (enabled) Blue else Dim, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
    Slider(value = setting.state / 10f, valueRange = 0f..100f, enabled = enabled,
        onValueChange = { setting.updateState((it * 10f).toInt()) }, onValueChangeFinished = { setting.save(setting.state) },
        colors = SliderDefaults.colors(thumbColor = Blue, activeTrackColor = Blue))
    Reason(enabled, reason)
}

/** Custom width/height: -/+ steppers instead of typing while playing */
@Composable
private fun IntStepper(title: String, setting: IntSettingUnit, range: IntRange, onCommit: () -> Unit) = RowCard(true) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
        fun set(v: Int) { setting.save(v.coerceIn(range)); onCommit() }
        LunaButton("−", { set(setting.state - 40) }, modifier = Modifier.widthIn(max = 48.dp))
        Text("${setting.state}", color = Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        LunaButton("+", { set(setting.state + 40) }, modifier = Modifier.widthIn(max = 48.dp))
    }
}

@Composable
private fun <T> Choice(title: String, items: List<T>, current: T, label: @Composable (T) -> String, enabled: Boolean = true, reason: String? = null, onPick: (T) -> Unit) = RowCard(enabled) {
    Text(title, color = if (enabled) Ink else Dim, fontSize = 14.sp)
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { item ->
            val on = item == current
            Box(
                Modifier.weight(1f).heightIn(min = 36.dp).clip(RoundedCornerShape(4.dp))
                    .background(if (on && enabled) Color(0xFFDDEBFF) else Color(0xFFF7F6F1))
                    .border(if (on) 2.dp else 1.dp, if (on && enabled) Blue else CardBorder, RoundedCornerShape(4.dp))
                    .clickable(enabled = enabled && !on) { onPick(item) }.padding(4.dp),
                contentAlignment = Alignment.Center
            ) { Text(label(item), color = if (enabled) (if (on) Blue else Ink) else Dim, fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal) }
        }
    }
    Reason(enabled, reason)
}

@Composable
private fun Disabled(title: String, reason: String) = RowCard(false) {
    Text(title, color = Dim, fontSize = 14.sp)
    Text("ⓘ $reason", color = Warn, fontSize = 11.sp)
}

// ---------- force close ----------

@Composable
private fun ForceCloseDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var understood by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.widthIn(max = 460.dp).padding(16.dp).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .border(2.dp, LunaBorder, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(Body)
        ) {
            Row(Modifier.fillMaxWidth().height(36.dp).background(Luna).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⚠ " + stringResource(R.string.all_gm_force_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                TitleButton("✕", onDismiss, red = true)
            }
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.game_menu_option_force_close_text), color = Ink, fontSize = 14.sp)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Card).border(1.dp, CardBorder, RoundedCornerShape(4.dp))
                        .clickable { understood = !understood }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(22.dp).border(1.dp, Color(0xFF1C5180)).background(Color.White), contentAlignment = Alignment.Center) {
                        if (understood) Text("✔", color = Color(0xFF21A121), fontWeight = FontWeight.Bold)
                    }
                    Text(stringResource(R.string.all_gm_force_understand), color = Ink, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LunaButton(stringResource(R.string.generic_cancel), onDismiss, modifier = Modifier.weight(1f))
                    LunaButton("✖ " + stringResource(R.string.game_button_force_close), onConfirm, danger = true, enabled = understood, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
