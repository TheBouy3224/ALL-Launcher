package com.movtery.zalithlauncher.all.settings

import android.content.Context
import android.os.Vibrator
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.context.GlobalContext
import com.movtery.zalithlauncher.all.bridge.AllSubmitError
import com.movtery.zalithlauncher.game.control.ControlData
import com.movtery.zalithlauncher.game.plugin.driver.DriverPluginManager
import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.game.support.touch_controller.VibrationHandler
import com.movtery.zalithlauncher.game.version.installed.GraphicsApi
import com.movtery.zalithlauncher.game.version.installed.SettingState
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionConfig
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel

/** Semantic ALL adapter over the existing per-instance config and global settings registry. */
class AllSettingsService(
    val version: Version,
    private val submitError: AllSubmitError
) {
    val config: VersionConfig = version.getVersionConfig()

    fun versionIsolationGlobal(): Boolean = AllSettings.versionIsolation.getValue()
    fun skipIntegrityGlobal(): Boolean = AllSettings.skipGameIntegrityCheck.getValue()
    fun rendererGlobal(): String = AllSettings.renderer.getValue()
    fun driverGlobal(): String = AllSettings.vulkanDriver.getValue()
    fun graphicsApiGlobal(): GraphicsApi = AllSettings.graphicsApi.getValue()
    fun controlGlobal(): String = AllSettings.controlLayout.getValue()
    fun runtimeGlobal(): String = AllSettings.javaRuntime.getValue()
    fun effectiveRam(context: Context = GlobalContext): Int = version.getRamAllocation(context)
    fun customInfoGlobal(): String = AllSettings.versionCustomInfo.getValue()
    fun jvmArgsGlobal(): String = AllSettings.jvmArgs.getValue()

    fun renderers(): List<AllSettingsOption> = buildList {
        add(AllSettingsOption("", "", null))
        Renderers.getRenderers().forEach {
            add(AllSettingsOption(it.getUniqueIdentifier(), it.getRendererName(), it.getRendererSummary()))
        }
    }

    fun drivers(): List<AllSettingsOption> = buildList {
        add(AllSettingsOption("", "", null))
        DriverPluginManager.getDriverList().forEach {
            add(AllSettingsOption(it.id, it.name, it.summary))
        }
    }

    fun runtimes(): List<AllSettingsOption> = buildList {
        add(AllSettingsOption("", "", null))
        RuntimesManager.getRuntimes()
            .filter { it.isCompatible() }
            .forEach { runtime ->
                add(AllSettingsOption(runtime.name, runtime.name, runtime.versionString))
            }
    }

    fun controls(items: List<ControlData>): List<AllSettingsOption> = buildList {
        add(AllSettingsOption("", "", null))
        items.filter { it.isSupport }.forEach {
            add(AllSettingsOption(it.file.name, it.controlLayout.info.name.default, null))
        }
    }

    fun graphicsApis(): List<AllSettingsOption> = listOf(AllSettingsOption("", "")) + GraphicsApi.entries.map {
        val label = when (it) {
            GraphicsApi.DEFAULT -> "Use game's setting"
            GraphicsApi.DEFAULT_OPENGL -> "Default OpenGL"
            else -> it.displayName
        }
        AllSettingsOption(it.name, label)
    }

    fun saveIsolation(value: SettingState) = save { config.isolationType = value }
    fun saveIntegrity(value: SettingState) = save { config.skipGameIntegrityCheck = value }
    fun saveRenderer(value: String) = save { config.renderer = value }
    fun saveDriver(value: String) = save { config.driver = value }
    fun saveGraphicsApi(value: GraphicsApi?) = save { config.graphicsApi = value }
    fun saveControl(value: String) = save { config.control = value }
    fun saveRuntime(value: String) = save { config.javaRuntime = value }
    fun saveRam(value: Int) = save { config.ramAllocation = value }
    fun followGlobalRam() = save { config.ramAllocation = -1 }
    fun saveCustomInfo(value: String) = save { config.customInfo = value }
    fun saveJvmArgs(value: String) = save { config.jvmArgs = value }
    fun saveServerIp(value: String) = save { config.serverIp = value }
    fun saveVibrationKind(value: VibrationHandler.VibrateKind) = save { config.touchVibrateKind = value }
    fun saveVibrationDuration(value: Int) = save { config.touchVibrateDuration = value }

    fun previewVibration(context: Context, duration: Int, kind: VibrationHandler.VibrateKind) {
        context.getSystemService(Vibrator::class.java)?.let {
            VibrationHandler.vibrate(it, duration, kind)
        }
    }

    private inline fun save(change: VersionConfig.() -> Unit) {
        config.change()
        runCatching { config.saveWithThrowable() }
            .onFailure { error ->
                submitError(
                    ErrorViewModel.ThrowableMessage(
                        title = androidText(R.string.versions_config_failed_to_save),
                        message = androidText(error.message ?: error.toString())
                    )
                )
            }
    }
}
