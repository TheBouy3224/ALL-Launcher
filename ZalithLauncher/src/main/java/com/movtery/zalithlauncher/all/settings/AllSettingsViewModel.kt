package com.movtery.zalithlauncher.all.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movtery.zalithlauncher.all.bridge.AllSubmitError
import com.movtery.zalithlauncher.game.control.ControlManager
import com.movtery.zalithlauncher.game.support.touch_controller.VibrationHandler
import com.movtery.zalithlauncher.game.version.installed.GraphicsApi
import com.movtery.zalithlauncher.game.version.installed.SettingState
import com.movtery.zalithlauncher.game.version.installed.Version
import kotlinx.coroutines.launch

/** ALL-owned transient UI state; all values are read/written through VersionConfig and AllSettings. */
class AllSettingsViewModel(
    version: Version,
    submitError: AllSubmitError
) : ViewModel() {
    val service = AllSettingsService(version, submitError)

    var state by mutableStateOf(AllSettingsState(
        ramDraft = service.config.ramAllocation.takeIf { it >= 256 }
            ?: service.effectiveRam()
    ))
        private set

    init {
        viewModelScope.launch {
            ControlManager.dataList.collect { data ->
                state = state.copy(controlOptions = service.controls(data))
            }
        }
    }

    fun selectSection(section: AllSettingsSection) {
        state = state.copy(section = section)
    }

    fun changed() {
        state = state.copy(revision = state.revision + 1)
    }

    fun saveIsolation(value: SettingState) = update { service.saveIsolation(value) }
    fun saveIntegrity(value: SettingState) = update { service.saveIntegrity(value) }
    fun saveRenderer(value: String) = update { service.saveRenderer(value) }
    fun saveDriver(value: String) = update { service.saveDriver(value) }
    fun saveGraphicsApi(value: GraphicsApi?) = update { service.saveGraphicsApi(value) }
    fun saveControl(value: String) = update { service.saveControl(value) }
    fun saveRuntime(value: String) = update { service.saveRuntime(value) }
    fun saveCustomInfo(value: String) = update { service.saveCustomInfo(value) }
    fun saveJvmArgs(value: String) = update { service.saveJvmArgs(value) }
    fun saveServerIp(value: String) = update { service.saveServerIp(value) }
    fun saveVibrationKind(value: VibrationHandler.VibrateKind) = update { service.saveVibrationKind(value) }
    fun saveVibrationDuration(value: Int) = update { service.saveVibrationDuration(value) }

    fun updateRamDraft(value: Int) {
        state = state.copy(ramDraft = value)
    }

    fun saveRam(value: Int) {
        service.saveRam(value)
        state = state.copy(revision = state.revision + 1, ramDraft = service.config.ramAllocation)
    }

    fun followGlobalRam() {
        service.followGlobalRam()
        state = state.copy(
            revision = state.revision + 1,
            ramDraft = service.effectiveRam()
        )
    }

    fun showMicrophoneCheck() {
        state = state.copy(microphoneOpen = true, microphoneLevel = 0.0)
    }

    fun closeMicrophoneCheck() {
        state = state.copy(microphoneOpen = false, microphoneLevel = 0.0)
    }

    fun updateMicrophoneLevel(value: Double) {
        state = state.copy(microphoneLevel = value)
    }

    private inline fun update(change: () -> Unit) {
        change()
        changed()
    }
}
