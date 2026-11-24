package com.movtery.zalithlauncher.all.launchersettings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.ui.AllButton
import com.movtery.zalithlauncher.game.plugin.natives.NativePluginManager
import com.movtery.zalithlauncher.notification.NotificationManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.MirrorSourceType
import com.movtery.zalithlauncher.terracotta.Terracotta
import com.movtery.zalithlauncher.ui.screens.content.MultiplayerOperation
import com.movtery.zalithlauncher.ui.screens.content.MultiplayerOperation as MpOp
import com.movtery.zalithlauncher.utils.file.shareFile

/** Game tab: download sources (same AUTO / official / mirror setting the Classic screen had). */
internal fun LazyListScope.mirrorRows() {
    item {
        ChoiceRow(stringResource(R.string.settings_launcher_mirror_game_source_title), MirrorSourceType.entries, AllSettings.gameDownloadSource.state,
            { stringResource(it.textRes) }, { AllSettings.gameDownloadSource.save(it) })
    }
    item {
        ChoiceRow(stringResource(R.string.settings_launcher_mirror_asset_platform_source_title), MirrorSourceType.entries, AllSettings.assetPlatformSource.state,
            { stringResource(it.textRes) }, { AllSettings.assetPlatformSource.save(it) })
    }
}

/**
 * Game tab: Terracotta (P2P room over EasyTier + VPN). Only the missing switch lives here: the notice / notification dialogs are the Classic
 * screen's own [MultiplayerOperation], and create / join room is the in-game menu's existing Terracotta page (shown once this is on).
 */
internal fun LazyListScope.multiplayerRows() {
    item { MultiplayerCard() }
}

@Composable
private fun MultiplayerCard() {
    val context = LocalContext.current
    var op by remember { mutableStateOf<MpOp>(MpOp.None) }
    com.movtery.zalithlauncher.all.ui.AllMaterialTheme {
        MultiplayerOperation(
            operation = op, onChange = { op = it },
            onNoticeRead = {
                AllSettings.terracottaNoticeVer.save(Terracotta.TERRACOTTA_USER_NOTICE_VERSION)
                op = if (!NotificationManager.checkNotificationEnabled(context)) MpOp.WarningNotification else MpOp.None
            },
            onNoticeRefused = { AllSettings.enableTerracotta.save(false); op = MpOp.None }
        )
    }
    val on = AllSettings.enableTerracotta.state
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SwitchRow(stringResource(R.string.all_section_multiplayer), stringResource(R.string.all_mp_hint), on) { v ->
            AllSettings.enableTerracotta.save(v)
            if (v) when {
                AllSettings.terracottaNoticeVer.getValue() < Terracotta.TERRACOTTA_USER_NOTICE_VERSION -> op = MpOp.Notice
                !NotificationManager.checkNotificationEnabled(context) -> op = MpOp.WarningNotification
            }
        }
        if (on) {
            SwitchRow(stringResource(R.string.terracotta_custom_note_list), null, AllSettings.enableTerracottaNodes.state) { AllSettings.enableTerracottaNodes.save(it) }
            if (AllSettings.enableTerracottaNodes.state) GlobalTextRow(stringResource(R.string.terracotta_custom_note_list), stringResource(R.string.terracotta_custom_note_list_hint), AllSettings.terracottaNodes)
            Card {
                AllButton(stringResource(R.string.terracotta_export_log_share), {
                    val log = PathManager.FILE_TERRACOTTA_LOG
                    if (log.exists()) shareFile(context, log) else Toast.makeText(context, R.string.terracotta_export_log_share_null, Toast.LENGTH_SHORT).show()
                })
            }
        }
    }
}

/** Game tab, troubleshooting: launcher log retention + native-library plugin on/off (only listed when plugins are installed). */
internal fun LazyListScope.advancedRows() {
    item { StepRow(stringResource(R.string.settings_launcher_log_retention_days_title), AllSettings.launcherLogRetentionDays, 1, "d") }
    item {
        val plugins = remember { NativePluginManager.getPlugins() }
        val off = AllSettings.disableNativeLibPlugins.state
        if (plugins.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            plugins.forEach { p ->
                SwitchRow(p.displayName, stringResource(R.string.settings_game_native_lib_plugin_title), p.packageName !in off) { on ->
                    AllSettings.disableNativeLibPlugins.save(if (on) off - p.packageName else off + p.packageName)
                }
            }
        }
    }
}
