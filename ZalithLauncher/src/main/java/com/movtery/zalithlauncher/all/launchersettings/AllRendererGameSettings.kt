package com.movtery.zalithlauncher.all.launchersettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.plugin.driver.DriverPluginManager
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.game.version.installed.GraphicsApi
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.ResolutionRule

/**
 * Renderer / Game slice of the ALL launcher settings (PHASE 3.5b).
 *
 * All values are the existing global [AllSettings] units (MMKV). Per-instance overrides live in the instance
 * Settings window and fall back to these. Values are read when the game launches, so changes here apply to the next launch.
 * Global memory, JVM arguments, custom version info, custom resolution, shader logging and the system Vulkan driver switch live in
 * [AllGlobalExtras.kt]. Still Classic-only: renderer plugin config/download and the native-library plugin list.
 */
@Composable
internal fun RendererSettings(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val renderers = remember { Renderers.getRenderers() }
    val drivers = remember { DriverPluginManager.getDriverList() }
    val rendererId = AllSettings.renderer.state
    //nothing is highlighted until the user picks one; with no choice the launcher picks a supported renderer per game version
    val effective = renderers.find { it.getUniqueIdentifier() == rendererId }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Note(stringResource(R.string.all_settings_applies_on_launch)) }
        item {
            ChoiceRow(
                title = stringResource(R.string.settings_renderer_global_renderer_title),
                options = renderers,
                selected = effective,
                label = { it.getRendererName() },
                onSelect = { AllSettings.renderer.save(it.getUniqueIdentifier()) },
                note = effective?.getRendererSummary() ?: if (rendererId.isEmpty()) stringResource(R.string.all_renderer_auto_note) else null
            )
        }
        item {
            val driverId = AllSettings.vulkanDriver.state
            ChoiceRow(
                title = stringResource(R.string.settings_renderer_global_vulkan_driver_title),
                options = drivers,
                selected = drivers.find { it.id == driverId },
                label = { it.name },
                onSelect = { AllSettings.vulkanDriver.save(it.id) }
            )
        }
        item {
            ChoiceRow(
                title = stringResource(R.string.settings_game_graphics_api_title),
                options = GraphicsApi.entries,
                selected = AllSettings.graphicsApi.state,
                label = {
                    when (it) {
                        GraphicsApi.DEFAULT -> stringResource(R.string.settings_game_graphics_api_default)
                        GraphicsApi.DEFAULT_OPENGL -> stringResource(R.string.settings_game_graphics_api_default_opengl)
                        else -> it.displayName
                    }
                },
                onSelect = { AllSettings.graphicsApi.save(it) }
            )
        }
        item {
            ChoiceRow(
                title = stringResource(R.string.settings_renderer_resolution_rule_title),
                options = ResolutionRule.entries,
                selected = AllSettings.resolutionRule.state,
                label = { stringResource(it.nameRes) },
                onSelect = {
                    AllSettings.resolutionRule.save(it)
                    //same as Classic: the first time "custom" is chosen the boxes start from the real screen size
                    if (it == ResolutionRule.CUSTOM) com.movtery.zalithlauncher.utils.ensureCustomResolutionInitialized(context)
                }
            )
        }
        if (AllSettings.resolutionRule.state == ResolutionRule.CUSTOM) item { CustomResolutionRows() }
        //the percentage only applies to the percentage rule (Classic hides it for custom, too)
        if (AllSettings.resolutionRule.state == ResolutionRule.PERCENTAGE) item {
            StepRow(
                title = stringResource(R.string.settings_renderer_resolution_scale_title),
                unit = AllSettings.resolutionRatio,
                step = 5,
                suffix = "%"
            )
        }
        item {
            SwitchRow(
                stringResource(R.string.settings_renderer_full_screen_title),
                stringResource(R.string.settings_renderer_full_screen_summary),
                AllSettings.gameFullScreen.state
            ) { AllSettings.gameFullScreen.save(it) }
        }
        item {
            SwitchRow(
                stringResource(R.string.settings_renderer_sustained_performance_title),
                stringResource(R.string.settings_renderer_sustained_performance_summary),
                AllSettings.sustainedPerformance.state
            ) { AllSettings.sustainedPerformance.save(it) }
        }
        item {
            SwitchRow(
                stringResource(R.string.settings_renderer_vsync_in_zink_title),
                stringResource(R.string.settings_renderer_vsync_in_zink_summary),
                AllSettings.vsyncInZink.state
            ) { AllSettings.vsyncInZink.save(it) }
        }
        item {
            SwitchRow(
                stringResource(R.string.settings_renderer_vulkan_driver_system_title),
                stringResource(R.string.settings_renderer_vulkan_driver_system_summary),
                AllSettings.zinkPreferSystemDriver.state
            ) { AllSettings.zinkPreferSystemDriver.save(it) }
        }
        item {
            SwitchRow(
                stringResource(R.string.settings_renderer_shader_dump_title),
                stringResource(R.string.settings_renderer_shader_dump_summary),
                AllSettings.dumpShaders.state
            ) { AllSettings.dumpShaders.save(it) }
        }
        item {
            SwitchRow(
                stringResource(R.string.settings_renderer_surface_title),
                stringResource(R.string.settings_renderer_surface_summary),
                AllSettings.useSurfaceView.state
            ) { AllSettings.useSurfaceView.save(it) }
        }
        item { Spacer(Modifier.padding(bottom = 8.dp)) }
    }
}

@Composable
internal fun GameSettings(modifier: Modifier = Modifier) {
    LazyColumn(
        //imePadding only has an effect while a text field here has focus (it switches the window to resize mode), so the list shrinks above the keyboard and scrolls the field into view
        modifier = modifier.fillMaxSize().imePadding().padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Note(stringResource(R.string.all_settings_applies_on_launch)) }
        item {
            SwitchRow(
                stringResource(R.string.settings_game_version_isolation_title),
                stringResource(R.string.settings_game_version_isolation_summary),
                AllSettings.versionIsolation.state
            ) { AllSettings.versionIsolation.save(it) }
        }
        item {
            SwitchRow(
                stringResource(R.string.settings_game_skip_game_integrity_check_title),
                stringResource(R.string.settings_game_skip_game_integrity_check_summary),
                AllSettings.skipGameIntegrityCheck.state
            ) { AllSettings.skipGameIntegrityCheck.save(it) }
        }
        item {
            SwitchRow(
                stringResource(R.string.settings_game_show_log_automatic_title),
                stringResource(R.string.settings_game_show_log_automatic_summary),
                AllSettings.showLogAutomatic.state
            ) { AllSettings.showLogAutomatic.save(it) }
        }
        item { GlobalRamRow() }
        item {
            GlobalTextRow(
                stringResource(R.string.settings_game_jvm_args_title),
                stringResource(R.string.settings_game_jvm_args_summary),
                AllSettings.jvmArgs,
                multiline = true,
                placeholder = "-Xss1M -Dkey=value"
            )
        }
        item {
            GlobalTextRow(
                stringResource(R.string.settings_game_version_custom_info_title),
                stringResource(R.string.settings_game_version_custom_info_summary),
                AllSettings.versionCustomInfo
            )
        }
        item { StepRow(stringResource(R.string.settings_game_log_text_size_title), AllSettings.logTextSize, 1, "Sp") }
        item { StepRow(stringResource(R.string.settings_game_log_buffer_flush_interval_title), AllSettings.logBufferFlushInterval, 50, "ms") }
        item { Spacer(Modifier.padding(bottom = 8.dp)) }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, color = StDim, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
}
