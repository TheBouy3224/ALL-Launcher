package com.movtery.zalithlauncher.all.launchersettings

import android.content.Context
import android.hardware.input.InputManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.bridge.AllSubmitError
import com.movtery.zalithlauncher.all.ui.AlertDialog
import com.movtery.zalithlauncher.all.ui.AllButton
import com.movtery.zalithlauncher.all.ui.AllButtonStyle
import com.movtery.zalithlauncher.all.ui.AllMaterialTheme
import com.movtery.zalithlauncher.all.ui.OutlinedTextField
import com.movtery.zalithlauncher.all.ui.TextButton
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.GamepadInputMode
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.ui.control.GamepadBindingKeyboard
import com.movtery.zalithlauncher.ui.control.gamepad.GamepadMap
import com.movtery.zalithlauncher.ui.control.gamepad.getNameByGamepadEvent
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.GAMEPAD_CONFIG_NAME_LENGTH
import com.movtery.zalithlauncher.viewmodel.GamepadViewModel

private val GpText = Color(0xFF1B1B1B)
private val GpDanger = Color(0xFFB3261E)
private val GpOk = Color(0xFF2E7D32)

/**
 * ALL Gamepad tab.
 *
 * Everything is Zalith's own backend: [GamepadViewModel] (the list of mapping configs, the current one, create/delete), the
 * [com.movtery.zalithlauncher.ui.control.gamepad.GamepadMappingList] storage (saveMapping / findByMap, MMKV) and the AllSettings units
 * of the Classic gamepad page. Key edits are staged in a [GamepadMappingDraft] and only written on Save; Cancel and Reset never
 * touch saved data. Not ported: the per-device remapping wizard (it needs the physical gamepad).
 */
@Composable
internal fun GamepadSettings(onError: AllSubmitError, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: GamepadViewModel = viewModel()

    //bumps whenever something outside Compose state changed (configs reloaded, saved, devices plugged)
    var rev by mutableIntStateOf(0)
    var inGame by remember { mutableStateOf(true) }
    var editing by remember { mutableStateOf<GamepadMap?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    val configName = viewModel.currentMapping?.name
    val draft = remember(configName) {
        GamepadMappingDraft { map, g -> viewModel.currentMapping?.findByMap(map, g) ?: emptySet() }
    }
    //Compose cannot see the draft's own map change, so every edit bumps this counter
    var draftRev by mutableIntStateOf(0)
    val dirty = draftRev >= 0 && draft.isDirty()

    //connected gamepads, kept live
    var gamepads by remember { mutableStateOf(connectedGamepadNames()) }
    DisposableEffect(Unit) {
        val manager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) { gamepads = connectedGamepadNames() }
            override fun onInputDeviceRemoved(deviceId: Int) { gamepads = connectedGamepadNames() }
            override fun onInputDeviceChanged(deviceId: Int) { gamepads = connectedGamepadNames() }
        }
        manager.registerInputDeviceListener(listener, null)
        onDispose { manager.unregisterInputDeviceListener(listener) }
    }

    val gamepadOn = AllSettings.gamepadControl.state
    val mapped = AllSettings.gamepadInputMode.state == GamepadInputMode.Mapped
    val remapEnabled = gamepadOn && mapped
    val configs = remember(rev, configName) { viewModel.getAllConfigKeys().sorted() }

    fun fail(e: Throwable) = onError(
        ErrorViewModel.ThrowableMessage(androidText(context.getString(R.string.all_gamepad_save_failed)), androidText(e.getMessageOrToString()))
    )

    Column(modifier.fillMaxSize().background(StSurface)) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.padding(top = 2.dp)) }
            item {
                Card {
                    Text(stringResource(R.string.all_gamepad_status), color = GpText, fontWeight = FontWeight.Bold)
                    if (gamepads.isEmpty()) {
                        Text(stringResource(R.string.all_gamepad_none), color = StDim, fontSize = 13.sp)
                        Text(stringResource(R.string.all_gamepad_none_hint), color = StDim, fontSize = 12.sp)
                    } else {
                        Text(stringResource(R.string.all_gamepad_found, gamepads.joinToString(", ")), color = GpOk, fontSize = 13.sp)
                    }
                }
            }
            item { SwitchRow(stringResource(R.string.settings_gamepad_title), stringResource(R.string.settings_gamepad_summary), gamepadOn) { AllSettings.gamepadControl.save(it) } }
            item {
                Card {
                    Text(stringResource(R.string.settings_gamepad_input_mode_title), color = GpText, fontWeight = FontWeight.Bold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GamepadInputMode.entries.forEach { mode ->
                            AllButton(
                                stringResource(mode.titleRes), { AllSettings.gamepadInputMode.save(mode) },
                                style = if (AllSettings.gamepadInputMode.state == mode) AllButtonStyle.Primary else AllButtonStyle.Neutral,
                                compact = true, enabled = gamepadOn
                            )
                        }
                    }
                    Text(stringResource(AllSettings.gamepadInputMode.state.summaryRes), color = StDim, fontSize = 12.sp)
                }
            }
            item { StepRow(stringResource(R.string.settings_gamepad_deadzone_title), AllSettings.gamepadDeadZoneScale, 5, "%") }
            item { StepRow(stringResource(R.string.settings_gamepad_cursor_sensitivity_title), AllSettings.gamepadCursorSensitivity, 5, "%") }
            item { StepRow(stringResource(R.string.settings_gamepad_camera_sensitivity_title), AllSettings.gamepadCameraSensitivity, 5, "%") }
            item {
                Card {
                    Text(stringResource(R.string.settings_gamepad_joystick_mode_title), color = GpText, fontWeight = FontWeight.Bold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        com.movtery.zalithlauncher.ui.control.gamepad.JoystickMode.entries.forEach { mode ->
                            AllButton(
                                stringResource(mode.titleRes), { AllSettings.joystickControlMode.save(mode) },
                                style = if (AllSettings.joystickControlMode.state == mode) AllButtonStyle.Primary else AllButtonStyle.Neutral,
                                compact = true, enabled = gamepadOn
                            )
                        }
                    }
                }
            }

            item { Text(stringResource(R.string.all_gamepad_bindings), color = GpText, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            item {
                Card {
                    Text(stringResource(R.string.settings_gamepad_config_title) + ": " + (configName ?: "-"), color = GpText, fontWeight = FontWeight.Bold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        configs.forEach { name ->
                            AllButton(
                                name,
                                {
                                    AllSettings.gamepadMappingConfig.save(name)
                                    viewModel.reloadAllMappings()
                                    rev++
                                },
                                style = if (name == configName) AllButtonStyle.Primary else AllButtonStyle.Neutral,
                                compact = true, enabled = !dirty
                            )
                        }
                        AllButton(stringResource(R.string.settings_gamepad_config_create), { creating = true }, compact = true, enabled = !dirty)
                        AllButton(stringResource(R.string.settings_gamepad_config_delete), { deleting = true }, style = AllButtonStyle.Danger, compact = true, enabled = !dirty && configs.size > 1)
                    }
                    if (dirty) Text(stringResource(R.string.all_gamepad_save_first), color = StDim, fontSize = 12.sp)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    AllButton(stringResource(R.string.settings_gamepad_mapping_in_game), { inGame = true }, style = if (inGame) AllButtonStyle.Primary else AllButtonStyle.Neutral, compact = true)
                    AllButton(stringResource(R.string.settings_gamepad_mapping_in_menu), { inGame = false }, style = if (!inGame) AllButtonStyle.Primary else AllButtonStyle.Neutral, compact = true)
                }
            }
            if (!remapEnabled) item { Text(stringResource(R.string.all_gamepad_mapped_only), color = StDim, fontSize = 12.sp) }
            if (viewModel.currentMapping == null) item { Text(stringResource(R.string.all_gamepad_no_config), color = GpDanger, fontSize = 13.sp) }
            else items(GamepadMap.entries, key = { it.identifier }) { item ->
                //no remember: the label must always follow the draft (a cached copy did not refresh after edits made in the key dialog)
                val keys = if (draftRev >= 0 && rev >= 0) draft.get(item, inGame).toList() else emptyList()
                val modified = draftRev >= 0 && draft.isModified(item, inGame)
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Image(painterResource(item.getIconRes()), null, Modifier.size(32.dp), contentScale = ContentScale.Fit)
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (keys.isEmpty()) stringResource(R.string.settings_gamepad_mapping_unbound)
                                else keys.map { keyName(it) }.joinToString("  "),
                                color = if (modified) StAccent else GpText, fontWeight = if (modified) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp
                            )
                            if (modified) Text(stringResource(R.string.all_gamepad_unsaved), color = StAccent, fontSize = 11.sp)
                        }
                        AllButton(stringResource(R.string.all_gamepad_edit), { editing = item }, compact = true, enabled = remapEnabled)
                        AllButton(stringResource(R.string.generic_reset), { draft.resetOne(item, inGame); draftRev++ }, compact = true, enabled = remapEnabled)
                    }
                }
            }
            item { Text(stringResource(R.string.all_gamepad_unsupported), color = StDim, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp)) }
        }

        //Save / Cancel / Reset are always in the same place, never scrolled away
        Row(
            Modifier.fillMaxWidth().background(StCard).padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(if (dirty) R.string.all_gamepad_unsaved_changes else R.string.all_gamepad_all_saved),
                color = if (dirty) StAccent else StDim, fontSize = 12.sp, modifier = Modifier.weight(1f)
            )
            AllButton(stringResource(R.string.all_gamepad_reset_all), { confirmReset = true }, style = AllButtonStyle.Danger, compact = true, enabled = remapEnabled)
            AllButton(stringResource(R.string.all_setup_cancel), { draft.discard(); draftRev++ }, compact = true, enabled = dirty)
            AllButton(stringResource(R.string.generic_save), {
                runCatching {
                    val list = viewModel.currentMapping ?: return@runCatching
                    draft.changes().forEach { (key, keys) -> list.saveMapping(key.first, keys, key.second) }
                    draft.discard()
                }.onFailure(::fail)
                draftRev++; rev++
            }, style = AllButtonStyle.Success, compact = true, enabled = dirty)
        }
    }

    editing?.let { map ->
        val selected = remember(map, inGame) { mutableStateListOf<String>().apply { addAll(draft.get(map, inGame)) } }
        AllMaterialTheme {
            GamepadBindingKeyboard(
                selectedKeys = selected,
                onKeyAdd = { selected.add(it); draft.set(map, inGame, selected.toSet()); draftRev++ },
                onKeyRemove = { selected.remove(it); draft.set(map, inGame, selected.toSet()); draftRev++ },
                onDismissRequest = { editing = null }
            )
        }
    }

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text(stringResource(R.string.all_gamepad_reset_all)) },
        text = { Text(stringResource(R.string.all_gamepad_reset_all_message)) },
        confirmButton = { TextButton(onClick = { draft.resetAll(); draftRev++; confirmReset = false }) { Text(stringResource(R.string.generic_confirm), color = GpDanger) } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.generic_cancel)) } }
    )

    if (creating) {
        var name by remember { mutableStateOf("") }
        val taken = name.isNotBlank() && viewModel.containsConfig(name)
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text(stringResource(R.string.settings_gamepad_config_create)) },
            text = {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(GAMEPAD_CONFIG_NAME_LENGTH) }, singleLine = true, isError = taken,
                    label = { Text(stringResource(R.string.settings_gamepad_config_create_name)) }
                )
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank() && !taken, onClick = {
                    viewModel.createNewConfig(name.trim(), onContainsConfig = {}, onFinished = { rev++ })
                    creating = false
                }) { Text(stringResource(R.string.generic_confirm)) }
            },
            dismissButton = { TextButton(onClick = { creating = false }) { Text(stringResource(R.string.generic_cancel)) } }
        )
    }

    if (deleting) AlertDialog(
        destructive = true,
        onDismissRequest = { deleting = false },
        title = { Text(stringResource(R.string.settings_gamepad_config_delete)) },
        text = { Text(stringResource(R.string.settings_gamepad_config_delete_message)) },
        confirmButton = {
            TextButton(onClick = {
                viewModel.currentMapping?.name?.let { viewModel.deleteConfig(it, onFinished = { rev++ }) }
                deleting = false
            }) { Text(stringResource(R.string.generic_confirm), color = GpDanger) }
        },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.generic_cancel)) } }
    )
}

@Composable
private fun keyName(code: String): String = getNameByGamepadEvent(code)
