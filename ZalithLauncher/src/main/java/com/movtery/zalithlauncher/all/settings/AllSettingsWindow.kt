package com.movtery.zalithlauncher.all.settings

import android.Manifest
import android.os.Build
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.bridge.AllSubmitError
import com.movtery.zalithlauncher.all.bridge.AllToast
import com.movtery.zalithlauncher.game.support.touch_controller.VibrationHandler
import com.movtery.zalithlauncher.game.version.installed.GraphicsApi
import com.movtery.zalithlauncher.game.version.installed.SettingState
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.unit.getOrMin
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.utils.microphone.MicMeter
import com.movtery.zalithlauncher.utils.platform.getMaxMemoryForSettings
import kotlin.math.roundToInt

private val SettingsSurface = Color(0xFFF3F1E8)
private val SettingsPanel = Color(0xFFFFFFFF)
private val SettingsRail = Color(0xFFD3E5FA)
private val SettingsBorder = Color(0xFFB9B4A2)
private val SettingsText = Color(0xFF1B1B1B)
private val SettingsMuted = Color(0xFF69665D)
private val SettingsAccent = Color(0xFF316AC5)
private val SettingsAccentPale = Color(0xFFE8EDF7)

/** ALL-owned settings presentation over Zalith's existing per-instance config and global registry. */
@Composable
fun AllSettingsWindow(
    version: Version,
    onCheckVulkan: () -> Unit,
    showToast: (AllToast) -> Unit,
    submitError: AllSubmitError,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val maxMemory = remember(context) { getMaxMemoryForSettings(context) }
    val viewModel: AllSettingsViewModel = viewModel(key = "ALL_Settings_" + version.getVersionName()) {
        AllSettingsViewModel(version, submitError)
    }
    val state = viewModel.state
    val config = viewModel.service.config

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SettingsSurface)
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(5.dp))
                .background(SettingsPanel)
                .border(1.dp, SettingsBorder, RoundedCornerShape(5.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.generic_setting),
                    color = SettingsText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = version.getVersionName(),
                    color = SettingsMuted,
                    fontSize = 12.sp
                )
            }
            Text(
                text = "${stringResource(R.string.generic_follow_global)} · ${stringResource(R.string.versions_config_version_settings)}",
                color = SettingsAccent,
                fontSize = 12.sp,
                maxLines = 1
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(164.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(SettingsRail)
                    .border(1.dp, SettingsBorder, RoundedCornerShape(5.dp))
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                AllSettingsSection.entries.forEach { section ->
                    val selected = section == state.section
                    val title = when (section) {
                        AllSettingsSection.VERSION -> stringResource(R.string.versions_config_version_settings)
                        AllSettingsSection.GAME -> stringResource(R.string.versions_config_game_settings)
                        AllSettingsSection.SUPPORT -> stringResource(R.string.versions_config_support_settings)
                    }
                    Text(
                        text = title,
                        color = if (selected) Color.White else SettingsText,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (selected) SettingsAccent else Color.Transparent)
                            .clickable { viewModel.selectSection(section) }
                            .padding(horizontal = 10.dp, vertical = 12.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.generic_follow_global),
                    color = SettingsMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(7.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (state.section) {
                    AllSettingsSection.VERSION -> VersionSettingsSection(viewModel, state.revision)
                    AllSettingsSection.GAME -> GameSettingsSection(viewModel, state.revision, maxMemory)
                    AllSettingsSection.SUPPORT -> SupportSettingsSection(
                        viewModel = viewModel,
                        version = version,
                        context = context,
                        onCheckVulkan = onCheckVulkan,
                        showToast = showToast
                    )
                }
            }
        }
    }

    if (state.microphoneOpen) {
        MicrophoneDialog(
            level = state.microphoneLevel,
            onLevel = viewModel::updateMicrophoneLevel,
            onNoPermission = { showToast(androidText(R.string.microphone_check_no_permissions)) },
            onDismiss = viewModel::closeMicrophoneCheck
        )
    }
}

@Composable
private fun VersionSettingsSection(viewModel: AllSettingsViewModel, revision: Int) {
    val service = viewModel.service
    val config = service.config
    @Suppress("UNUSED_VARIABLE") val refresh = revision

    SettingChoiceRow(
        title = stringResource(R.string.versions_config_isolation_title),
        summary = stringResource(R.string.versions_config_isolation_summary),
        currentId = config.isolationType.name,
        options = SettingState.entries.map {
            AllSettingsOption(it.name, stringResource(it.textRes))
        },
        selectedText = settingStateText(config.isolationType, service.versionIsolationGlobal()),
        onSelect = { id -> SettingState.entries.find { it.name == id }?.let(viewModel::saveIsolation) }
    )

    SettingChoiceRow(
        title = stringResource(R.string.settings_game_skip_game_integrity_check_title),
        summary = stringResource(R.string.settings_game_skip_game_integrity_check_summary),
        currentId = config.skipGameIntegrityCheck.name,
        options = SettingState.entries.map {
            AllSettingsOption(it.name, stringResource(it.textRes))
        },
        selectedText = settingStateText(config.skipGameIntegrityCheck, service.skipIntegrityGlobal()),
        onSelect = { id -> SettingState.entries.find { it.name == id }?.let(viewModel::saveIntegrity) }
    )

    SettingChoiceRow(
        title = stringResource(R.string.versions_config_renderer),
        summary = stringResource(R.string.generic_follow_global),
        currentId = config.renderer,
        options = service.renderers(),
        selectedText = selectedOption(service.renderers(), config.renderer, service.rendererGlobal()),
        onSelect = viewModel::saveRenderer
    )

    SettingChoiceRow(
        title = stringResource(R.string.versions_config_vulkan_driver),
        summary = stringResource(R.string.generic_follow_global),
        currentId = config.driver,
        options = service.drivers(),
        selectedText = selectedOption(service.drivers(), config.driver, service.driverGlobal()),
        onSelect = viewModel::saveDriver
    )

    val graphicsOptions = service.graphicsApis()
    SettingChoiceRow(
        title = stringResource(R.string.settings_game_graphics_api_title),
        summary = stringResource(R.string.settings_game_graphics_api_summary),
        currentId = config.graphicsApi?.name.orEmpty(),
        options = graphicsOptions,
        selectedText = graphicsOptionText(config.graphicsApi?.name.orEmpty(), graphicsOptions),
        onSelect = { id -> viewModel.saveGraphicsApi(GraphicsApi.entries.find { it.name == id }) }
    )

    val controls = viewModel.state.controlOptions
    SettingChoiceRow(
        title = stringResource(R.string.versions_config_control),
        summary = stringResource(R.string.generic_follow_global),
        currentId = config.control,
        options = controls,
        selectedText = selectedOption(controls, config.control, service.controlGlobal()),
        onSelect = viewModel::saveControl
    )
}

@Composable
private fun GameSettingsSection(
    viewModel: AllSettingsViewModel,
    revision: Int,
    maxMemory: Int
) {
    val service = viewModel.service
    val config = service.config
    @Suppress("UNUSED_VARIABLE") val refresh = revision
    val runtimes = service.runtimes()

    SettingChoiceRow(
        title = stringResource(R.string.settings_game_java_runtime_title),
        summary = stringResource(R.string.versions_config_java_runtime_summary),
        currentId = config.javaRuntime,
        options = runtimes,
        selectedText = selectedOption(runtimes, config.javaRuntime, service.runtimeGlobal()),
        onSelect = viewModel::saveRuntime
    )

    val followsGlobal = config.ramAllocation < 256
    SettingCard {
        SettingHeader(
            title = stringResource(R.string.settings_game_java_memory_title),
            summary = stringResource(R.string.settings_game_java_memory_summary)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.generic_follow_global),
                color = SettingsText,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = followsGlobal,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        viewModel.followGlobalRam()
                    } else {
                        viewModel.saveRam(viewModel.state.ramDraft.coerceIn(256, maxMemory))
                    }
                }
            )
        }
        if (!followsGlobal) {
            Text("${viewModel.state.ramDraft} MB", color = SettingsAccent, fontWeight = FontWeight.Bold)
            Slider(
                value = viewModel.state.ramDraft.toFloat().coerceIn(256f, maxMemory.toFloat()),
                onValueChange = { viewModel.updateRamDraft(it.roundToInt()) },
                onValueChangeFinished = { viewModel.saveRam(viewModel.state.ramDraft.coerceIn(256, maxMemory)) },
                valueRange = 256f..maxMemory.toFloat()
            )
        } else {
            Text(
                text = stringResource(
                    R.string.settings_game_java_memory_allocation_text,
                    service.effectiveRam()
                ),
                color = SettingsMuted,
                fontSize = 12.sp
            )
        }
    }

    TextSettingRow(
        title = stringResource(R.string.settings_game_version_custom_info_title),
        summary = stringResource(R.string.settings_game_version_custom_info_summary),
        value = config.customInfo,
        placeholder = service.customInfoGlobal(),
        onValueChange = viewModel::saveCustomInfo
    )

    TextSettingRow(
        title = stringResource(R.string.settings_game_jvm_args_title),
        summary = stringResource(R.string.settings_game_jvm_args_summary),
        value = config.jvmArgs,
        placeholder = service.jvmArgsGlobal(),
        multiline = true,
        onValueChange = viewModel::saveJvmArgs
    )

    TextSettingRow(
        title = stringResource(R.string.versions_config_auto_join_server_ip_title),
        summary = stringResource(R.string.versions_config_auto_join_server_ip_summary),
        value = config.serverIp,
        placeholder = stringResource(R.string.versions_config_disable_if_blank),
        onValueChange = viewModel::saveServerIp
    )
}

@Composable
private fun SupportSettingsSection(
    viewModel: AllSettingsViewModel,
    version: Version,
    context: android.content.Context,
    onCheckVulkan: () -> Unit,
    showToast: (AllToast) -> Unit
) {
    val config = viewModel.service.config
    var vibrationDuration by remember(version) { mutableIntStateOf(config.touchVibrateDuration) }
    val kind = config.touchVibrateKind ?: VibrationHandler.VibrateKind.default
    val vibrationOptions = VibrationHandler.VibrateKind.entries.map {
        val title = when (it) {
            VibrationHandler.VibrateKind.ONE_SHOT -> stringResource(R.string.vibrate_kind_one_shot)
            VibrationHandler.VibrateKind.CLICK -> stringResource(R.string.vibrate_kind_click)
            VibrationHandler.VibrateKind.DOUBLE_CLICK -> stringResource(R.string.vibrate_kind_double_click)
            VibrationHandler.VibrateKind.HEAVY_CLICK -> stringResource(R.string.vibrate_kind_heavy_click)
            VibrationHandler.VibrateKind.TICK -> stringResource(R.string.vibrate_kind_tick)
        }
        AllSettingsOption(it.name, title)
    }

    SettingChoiceRow(
        title = stringResource(R.string.versions_config_vibrate_kind_title),
        summary = stringResource(R.string.versions_config_vibrate_kind_summary),
        currentId = kind.name,
        options = vibrationOptions.filter { it.id == VibrationHandler.VibrateKind.ONE_SHOT.name || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q },
        onSelect = { id ->
            VibrationHandler.VibrateKind.entries.find { it.name == id }?.let { selected ->
                viewModel.saveVibrationKind(selected)
                viewModel.service.previewVibration(context, config.touchVibrateDuration, selected)
            }
        }
    )

    if (kind == VibrationHandler.VibrateKind.ONE_SHOT) {
        SettingCard {
            SettingHeader(
                title = stringResource(R.string.versions_config_vibrate_duration_title),
                summary = stringResource(R.string.versions_config_vibrate_duration_summary)
            )
            Text("$vibrationDuration ms", color = SettingsAccent, fontWeight = FontWeight.Bold)
            Slider(
                value = vibrationDuration.toFloat(),
                onValueChange = { vibrationDuration = it.roundToInt() },
                onValueChangeFinished = {
                    viewModel.saveVibrationDuration(vibrationDuration)
                    viewModel.service.previewVibration(context, vibrationDuration, kind)
                },
                valueRange = 80f..500f
            )
        }
    }

    SettingCard {
        SettingHeader(
            title = stringResource(R.string.game_vulkan_check_title),
            summary = stringResource(R.string.game_vulkan_check_text)
        )
        AllSettingsButton(
            text = stringResource(R.string.game_vulkan_check_title),
            onClick = onCheckVulkan
        )
    }

    SettingCard {
        SettingHeader(
            title = stringResource(R.string.versions_config_microphone_check_title),
            summary = stringResource(R.string.versions_config_microphone_check_summary)
        )
        AllSettingsButton(
            text = stringResource(R.string.versions_config_microphone_check_title),
            onClick = viewModel::showMicrophoneCheck
        )
    }

    @Suppress("UNUSED_VARIABLE") val toast = showToast
}

@Composable
private fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(SettingsPanel)
            .border(1.dp, SettingsBorder, RoundedCornerShape(6.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
private fun SettingHeader(title: String, summary: String?) {
    Text(title, color = SettingsText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    if (!summary.isNullOrBlank()) {
        Text(summary, color = SettingsMuted, fontSize = 12.sp)
    }
}

@Composable
private fun SettingChoiceRow(
    title: String,
    summary: String?,
    currentId: String,
    options: List<AllSettingsOption>,
    selectedText: String = selectedOption(options, currentId, ""),
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    SettingCard {
        SettingHeader(title, summary)
        Box {
            AllSettingsButton(
                text = selectedText.ifBlank { stringResource(R.string.generic_follow_global) },
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(optionTitle(option))
                                if (!option.summary.isNullOrBlank()) {
                                    Text(option.summary, color = SettingsMuted, fontSize = 11.sp)
                                }
                            }
                        },
                        onClick = {
                            expanded = false
                            onSelect(option.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TextSettingRow(
    title: String,
    summary: String,
    value: String,
    placeholder: String,
    multiline: Boolean = false,
    onValueChange: (String) -> Unit
) {
    SettingCard {
        SettingHeader(title, summary)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = !multiline,
            textStyle = TextStyle(color = SettingsText, fontSize = 14.sp),
            cursorBrush = SolidColor(SettingsAccent),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (multiline) 76.dp else 48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SettingsAccentPale)
                .padding(10.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, color = SettingsMuted, fontSize = 13.sp)
                    inner()
                }
            }
        )
    }
}

@Composable
private fun AllSettingsButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = SettingsAccent,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(SettingsAccentPale)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    )
}

@Composable
private fun settingStateText(state: SettingState, global: Boolean): String {
    return when (state) {
        SettingState.FOLLOW_GLOBAL -> stringResource(R.string.generic_follow_global) + " · " +
            stringResource(if (global) R.string.generic_enable else R.string.generic_disable)
        SettingState.ENABLE -> stringResource(R.string.generic_enable)
        SettingState.DISABLE -> stringResource(R.string.generic_disable)
    }
}

@Composable
private fun selectedOption(options: List<AllSettingsOption>, id: String, global: String): String {
    if (id.isBlank()) return stringResource(R.string.generic_follow_global) +
        global.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
    if (id.isBlank()) return stringResource(R.string.generic_follow_global) +
        global.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
    return options.find { it.id == id }?.title ?: id
}

@Composable
private fun graphicsOptionText(id: String, options: List<AllSettingsOption>): String {
    if (id.isBlank()) return stringResource(R.string.generic_follow_global) + " · " +
        options.firstOrNull { it.id == AllSettings.graphicsApi.getValue().name }?.title.orEmpty()
    return when (id) {
        GraphicsApi.DEFAULT.name -> stringResource(R.string.settings_game_graphics_api_default)
        GraphicsApi.DEFAULT_OPENGL.name -> stringResource(R.string.settings_game_graphics_api_default_opengl)
        else -> GraphicsApi.entries.find { it.name == id }?.displayName ?: id
    }
}

@Composable
private fun optionTitle(option: AllSettingsOption): String {
    if (option.id.isBlank()) return stringResource(R.string.generic_follow_global)
    if (option.id == GraphicsApi.DEFAULT.name) return stringResource(R.string.settings_game_graphics_api_default)
    if (option.id == GraphicsApi.DEFAULT_OPENGL.name) return stringResource(R.string.settings_game_graphics_api_default_opengl)
    return option.title
}

@Composable
private fun MicrophoneDialog(
    level: Double,
    onLevel: (Double) -> Unit,
    onNoPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val meter = remember { MicMeter() }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            meter.start(context, onLevel, onPermissionRequest = {})
        } else {
            meter.stop()
            onNoPermission()
            onDismiss()
        }
    }
    LaunchedEffect(Unit) {
        meter.start(
            context = context,
            onLevelUpdate = onLevel,
            onPermissionRequest = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        )
    }
    DisposableEffect(meter) {
        onDispose { meter.stop() }
    }

    AlertDialog(
        onDismissRequest = {
            meter.stop()
            onDismiss()
        },
        title = { Text(stringResource(R.string.microphone_check_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val fraction = (level / 100.0).toFloat().coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(SettingsBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .background(SettingsAccent)
                    )
                    Text(
                        text = stringResource(R.string.microphone_check_volume, level.toInt()),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                meter.stop()
                onDismiss()
            }) {
                Text(stringResource(R.string.generic_close))
            }
        }
    )
}
