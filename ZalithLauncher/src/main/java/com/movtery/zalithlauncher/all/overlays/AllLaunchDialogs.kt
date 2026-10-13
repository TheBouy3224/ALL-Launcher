package com.movtery.zalithlauncher.all.overlays

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.ui.AllButton
import com.movtery.zalithlauncher.all.ui.AllButtonStyle
import com.movtery.zalithlauncher.all.ui.AllDialog
import com.movtery.zalithlauncher.all.ui.AllDialogKind
import com.movtery.zalithlauncher.game.version.mod.ModDoctorProblem
import com.movtery.zalithlauncher.game.version.mod.ModDoctorReport
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * ALL-styled presentation of the pre-launch dialogs (PHASE 3.7b).
 *
 * Only presentation: the state machine (LaunchGameOperation), its callbacks, ModDoctor and the launcher are Zalith's and
 * untouched; every dialog here is handed the same texts and the same onConfirm / onDismiss lambdas the Classic
 * dialog gets.
 */

/** Window requests from Zalith-side flows (e.g. "no account" / "no version" during launch) to the ALL Desktop. */
object AllWindowRequests {
    const val INSTANCES = "instances"
    const val ACCOUNTS = "accounts"
    val flow = MutableSharedFlow<String>(extraBufferCapacity = 4)
    fun open(window: String) { flow.tryEmit(window) }
}

private val DlgSurface = Color(0xFFF3F1E8)
private val DlgCard = Color(0xFFFFFFFF)
private val DlgBorder = Color(0xFFB9B4A2)
private val DlgText = Color(0xFF1B1B1B)
private val DlgAccent = Color(0xFF316AC5)
private val DlgDanger = Color(0xFFB3261E)

@Composable
internal fun DlgFrame(title: String, onDismissRequest: () -> Unit, kind: AllDialogKind = AllDialogKind.Info, content: @Composable () -> Unit) =
    AllDialog(title, onDismissRequest, kind) { content() }

@Composable
internal fun DlgButton(text: String, onClick: () -> Unit, color: Color = DlgText, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val style = when (color) {
        DlgAccent -> AllButtonStyle.Primary
        DlgDanger -> AllButtonStyle.Danger
        else -> AllButtonStyle.Neutral
    }
    AllButton(text, onClick, modifier, style, compact = true, enabled = enabled)
}

/** Same contract as SimpleAlertDialog: [onConfirm] == null gives a single confirm button that calls [onDismiss]. */
@Composable
fun AllAlertDialog(
    title: String,
    text: String,
    confirmText: String = stringResource(R.string.generic_confirm),
    dismissText: String = stringResource(R.string.generic_cancel),
    onConfirm: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    kind: AllDialogKind = AllDialogKind.Info
) {
    AllDialog(
        title, onDismissRequest = onDismiss, kind = kind,
        actions = {
            if (onConfirm != null) DlgButton(dismissText, onDismiss)
            DlgButton(confirmText, onConfirm ?: onDismiss, DlgAccent)
        }
    ) { Text(text, color = DlgText, fontSize = 14.sp) }
}

/** Presentation of ModDoctor's report. Buttons and their callbacks are exactly Classic's ModProblemsDialog. */
@Composable
fun AllModProblemsDialog(
    report: ModDoctorReport,
    problemText: (Context, ModDoctorProblem) -> String,
    onCancel: () -> Unit,
    onFix: () -> Unit,
    onLaunchAnyway: () -> Unit
) {
    val context = LocalContext.current
    var fixing by remember(report) { mutableStateOf(false) }
    //advisories only: no cancel action, both usable actions continue the launch (same rule as Classic)
    val advisoryOnly = report.problems.isEmpty() && report.advisories.isNotEmpty()
    DlgFrame(
        kind = if (advisoryOnly) AllDialogKind.Info else AllDialogKind.Warning,
        title = stringResource(if (advisoryOnly) R.string.mod_doctor_advisory_title else R.string.mod_doctor_title),
        onDismissRequest = { if (!fixing) { if (advisoryOnly) onLaunchAnyway() else onCancel() } }
    ) {
        Text(
            stringResource(
                if (advisoryOnly) R.string.mod_doctor_advisory_summary else R.string.mod_doctor_summary,
                if (advisoryOnly) report.advisories.size else report.problems.size
            ),
            color = DlgText, fontSize = 14.sp
        )
        Column(
            modifier = Modifier
                .padding(top = 8.dp)
                .heightIn(max = 220.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            report.problems.forEach { Text(problemText(context, it), color = DlgText, fontSize = 13.sp) }
            report.advisories.forEach { Text(problemText(context, it), color = DlgText, fontSize = 13.sp) }
            report.groupPlan?.let { GroupPlanPreview(it) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            if (!advisoryOnly) DlgButton(stringResource(R.string.generic_cancel), { if (!fixing) onCancel() }, modifier = Modifier.fillMaxWidth())
            if (report.hasFixableProblems) {
                DlgButton(
                    stringResource(if (fixing) R.string.mod_doctor_fixing else R.string.mod_doctor_fix),
                    { if (!fixing) { fixing = true; onFix() } },
                    DlgAccent, Modifier.fillMaxWidth()
                )
            }
            DlgButton(stringResource(R.string.generic_anyway), { if (!fixing) onLaunchAnyway() }, DlgDanger, Modifier.fillMaxWidth())
        }
    }
}

/** แสดงแผนซ่อมกลุ่มแบบอ่านอย่างเดียว: ไม่มีปุ่มที่ใช้แผนนี้ */
@Composable
private fun GroupPlanPreview(plan: com.movtery.zalithlauncher.game.version.mod.GroupPlan) {
    val statusText = when (plan.status) {
        com.movtery.zalithlauncher.game.version.mod.GroupPlanStatus.NO_CHANGE -> stringResource(R.string.all_plan_no_change)
        com.movtery.zalithlauncher.game.version.mod.GroupPlanStatus.UNVERIFIED ->
            stringResource(R.string.all_plan_unverified, plan.notes.lastOrNull().orEmpty())
        com.movtery.zalithlauncher.game.version.mod.GroupPlanStatus.BLOCKED_PACK_OWNED -> stringResource(R.string.all_plan_blocked)
        com.movtery.zalithlauncher.game.version.mod.GroupPlanStatus.PLAN -> null
    }
    Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.all_plan_title), color = DlgText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        plan.causes.forEach { Text(stringResource(R.string.all_plan_cause, it), color = DlgText, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        if (statusText != null) Text(statusText, color = DlgText, fontSize = 12.sp)
        if (plan.dependencyInstalls.isNotEmpty()) {
            Text(stringResource(R.string.all_plan_dep_title), color = DlgText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            plan.dependencyInstalls.forEach { install ->
                Text(
                    stringResource(
                        R.string.all_plan_dep_install,
                        install.dependencyIds.joinToString(),
                        install.requiredBy.joinToString(),
                        install.versionName ?: "-",
                    ),
                    color = DlgText, fontSize = 11.sp
                )
            }
        }
        if (plan.changes.isNotEmpty()) {
            Text(stringResource(R.string.all_plan_changes_title), color = DlgText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        plan.changes.forEach { change ->
            Text(
                stringResource(R.string.all_plan_change, change.modId, change.fromVersion ?: "-", change.toVersion),
                color = DlgText, fontSize = 12.sp
            )
            Text(stringResource(R.string.all_plan_reason, change.reason), color = DlgText, fontSize = 11.sp)
        }
        if (plan.changes.isNotEmpty() || plan.dependencyInstalls.isNotEmpty()) {
            val conf = when (plan.confidence) {
                com.movtery.zalithlauncher.game.version.mod.GroupPlanConfidence.HIGH -> R.string.all_plan_conf_high
                com.movtery.zalithlauncher.game.version.mod.GroupPlanConfidence.MEDIUM -> R.string.all_plan_conf_medium
                com.movtery.zalithlauncher.game.version.mod.GroupPlanConfidence.UNVERIFIED -> R.string.all_plan_conf_unverified
            }
            Text(stringResource(R.string.all_plan_confidence, stringResource(conf)), color = DlgText, fontSize = 11.sp)
        }
        plan.risks.forEach { Text(it, color = DlgText, fontSize = 11.sp) }
        if (plan.notes.isNotEmpty()) {
            Text(stringResource(R.string.all_plan_notes_title), color = DlgText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        plan.notes.forEach { Text(it, color = DlgText, fontSize = 11.sp) }
        if (plan.suggestions.isNotEmpty()) {
            Text(stringResource(R.string.all_plan_suggestions_title), color = DlgText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.all_plan_suggestions_note), color = DlgText, fontSize = 11.sp)
            plan.suggestions.forEach {
                Text(stringResource(R.string.all_plan_change, it.modId, it.fromVersion ?: "-", it.toVersion), color = DlgText, fontSize = 11.sp)
            }
        }
        Text(stringResource(R.string.all_plan_plan_only), color = DlgText, fontSize = 11.sp)
    }
}
