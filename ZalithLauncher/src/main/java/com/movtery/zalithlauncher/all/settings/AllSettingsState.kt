package com.movtery.zalithlauncher.all.settings

import com.movtery.zalithlauncher.game.version.installed.VersionConfig

/** Categories and UI-only option state owned by ALL; persisted values remain in Zalith config. */
enum class AllSettingsSection {
    VERSION,
    GAME,
    SUPPORT
}

data class AllSettingsOption(
    val id: String,
    val title: String,
    val summary: String? = null
)

data class AllSettingsState(
    val section: AllSettingsSection = AllSettingsSection.VERSION,
    val revision: Int = 0,
    val controlOptions: List<AllSettingsOption> = emptyList(),
    val ramDraft: Int = 0,
    val microphoneOpen: Boolean = false,
    val microphoneLevel: Double = 0.0
)
