/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */
package com.movtery.zalithlauncher.all.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.ui.AllColors
import com.movtery.zalithlauncher.all.ui.AllDialog
import com.movtery.zalithlauncher.all.ui.AllDialogKind
import com.movtery.zalithlauncher.game.version.mod.FamilyChangeKind
import com.movtery.zalithlauncher.game.version.mod.FamilyPreviewItem

/**
 * Repair UI owned by ALL (Crash page and Mods window share it): Planning → Preview → Confirm → Applying → Success / Error / No fix.
 *
 * Presentation only. The plan, the staged files, the transaction, the hash checks and the rollback are ModDoctor's;
 * this file only shows them and hands the user's decision back through the callbacks.
 */
sealed interface AllRepairUiState {
    /** Reading the mods and the provider; nothing is changed. */
    data object Planning : AllRepairUiState

    /** One plan for the whole mod set (every JAR that will be installed / upgraded / downgraded), or plain text lines for the rule paths. */
    data class Preview(
        val title: String,
        val items: List<FamilyPreviewItem> = emptyList(),
        val lines: List<String> = emptyList(),
        val hasPrerelease: Boolean = false,
        val confirmText: String,
    ) : AllRepairUiState

    /** Backup, install, hash check — not dismissable. */
    data object Applying : AllRepairUiState

    data class Result(
        val kind: AllRepairResultKind,
        val title: String,
        val body: String,
        /** Optional second action next to Close (e.g. "Review again", "Disable temporarily"). */
        val extraLabel: String? = null,
    ) : AllRepairUiState
}

enum class AllRepairResultKind { Success, Error, NoFix }

@Composable
fun AllRepairHost(
    state: AllRepairUiState?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onClose: () -> Unit,
    onExtra: () -> Unit = {},
) {
    when (state) {
        null -> Unit
        AllRepairUiState.Planning -> AllDialog(
            title = stringResource(R.string.all_repair_planning_title),
            onDismissRequest = onCancel,
            actions = { DlgButton(stringResource(R.string.generic_cancel), onCancel) },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(28.dp), color = AllColors.Blue, strokeWidth = 3.dp)
                Text(stringResource(R.string.all_repair_planning_body), color = AllColors.Text, fontSize = 14.sp)
            }
        }
        is AllRepairUiState.Preview -> AllDialog(
            title = state.title,
            onDismissRequest = onCancel,
            kind = if (state.hasPrerelease) AllDialogKind.Warning else AllDialogKind.Info,
            actions = {
                DlgButton(stringResource(R.string.generic_cancel), onCancel)
                DlgButton(state.confirmText, onConfirm, AllColors.Blue)
            },
        ) { PreviewContent(state) }
        AllRepairUiState.Applying -> AllDialog(
            title = stringResource(R.string.all_repair_applying_title),
            onDismissRequest = {},
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(28.dp), color = AllColors.Blue, strokeWidth = 3.dp)
                Text(stringResource(R.string.all_repair_applying_body), color = AllColors.Text, fontSize = 14.sp)
            }
        }
        is AllRepairUiState.Result -> AllDialog(
            title = state.title,
            onDismissRequest = onClose,
            kind = when (state.kind) {
                AllRepairResultKind.Success -> AllDialogKind.Info
                AllRepairResultKind.Error -> AllDialogKind.Error
                AllRepairResultKind.NoFix -> AllDialogKind.Warning
            },
            actions = {
                state.extraLabel?.let { DlgButton(it, onExtra) }
                DlgButton(stringResource(R.string.generic_close), onClose, AllColors.Blue)
            },
        ) { Text(state.body, color = AllColors.Text, fontSize = 14.sp) }
    }
}

@Composable
private fun PreviewContent(state: AllRepairUiState.Preview) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.hasPrerelease) {
            Text(
                stringResource(R.string.all_repair_beta_warning),
                color = AllColors.Warning, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(AllColors.BluePale)
                    .border(1.dp, AllColors.Warning, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            )
        }
        state.items.forEach { ChangeCard(it) }
        state.lines.forEach { Text(it, color = AllColors.Text, fontSize = 13.sp) }
        if (state.items.isNotEmpty()) {
            Text(stringResource(R.string.all_repair_note_untouched), color = AllColors.TextDim, fontSize = 12.sp)
            Text(stringResource(R.string.all_repair_note_backup), color = AllColors.TextDim, fontSize = 12.sp)
            Text(stringResource(R.string.all_repair_note_atomic), color = AllColors.TextDim, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ChangeCard(item: FamilyPreviewItem) {
    val label = stringResource(
        when (item.kind) {
            FamilyChangeKind.INSTALL -> R.string.all_repair_kind_install
            FamilyChangeKind.UPGRADE -> R.string.all_repair_kind_upgrade
            FamilyChangeKind.DOWNGRADE -> R.string.all_repair_kind_downgrade
            FamilyChangeKind.SWITCH -> R.string.all_repair_kind_switch
            FamilyChangeKind.DISABLE -> R.string.all_repair_kind_disable
            FamilyChangeKind.RESTORE -> R.string.all_repair_kind_restore
        }
    )
    val accent = when (item.kind) {
        FamilyChangeKind.INSTALL -> AllColors.Green
        FamilyChangeKind.UPGRADE -> AllColors.Blue
        FamilyChangeKind.DOWNGRADE -> AllColors.Warning
        FamilyChangeKind.SWITCH -> AllColors.TextDim
        FamilyChangeKind.DISABLE -> AllColors.Danger
        FamilyChangeKind.RESTORE -> AllColors.Green
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(AllColors.Card)
            .border(1.dp, AllColors.Border, RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.clip(RoundedCornerShape(4.dp)).background(accent).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(label, color = androidx.compose.ui.graphics.Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            if (item.prerelease) {
                Box(Modifier.clip(RoundedCornerShape(4.dp)).background(AllColors.Warning).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(stringResource(R.string.all_repair_beta_tag), color = androidx.compose.ui.graphics.Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(item.modId, color = AllColors.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            if (item.fromVersion.isNullOrBlank()) item.toVersion.orEmpty() else "${item.fromVersion}  →  ${item.toVersion.orEmpty()}",
            color = AllColors.Text, fontSize = 13.sp
        )
        Text(
            when {
                item.fromFile.isBlank() -> item.toFile
                item.toFile.isBlank() -> item.fromFile
                else -> "${item.fromFile}  →  ${item.toFile}"
            },
            color = AllColors.TextDim, fontSize = 11.sp
        )
        if (item.fromFile.isNotBlank()) Text(stringResource(R.string.all_repair_old_kept), color = AllColors.TextDim, fontSize = 11.sp)
        item.reasons.forEach { Text(stringResource(R.string.crash_repair_family_reason, it), color = AllColors.Text, fontSize = 12.sp) }
    }
}
