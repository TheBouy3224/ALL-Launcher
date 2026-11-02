package com.movtery.zalithlauncher.ui.screens.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import com.movtery.zalithlauncher.all.ui.XpChip
import com.movtery.zalithlauncher.all.ui.XpBlue
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.ui.*
import com.movtery.zalithlauncher.components.InstallableItem

private enum class Phase { WELCOME, RUNNING, FAILED, DONE }

/**
 * ALL-owned first-run setup wizard (V3 flat navy, 2 columns in landscape) over the existing unpack items and tasks.
 * Progress is the real number of finished components; nothing here installs anything by itself.
 */
@Composable
internal fun AllUnpackScreen(items: List<InstallableItem>, onStart: () -> Unit, onCancel: () -> Unit) {
    val states = items.map { it.state.collectAsStateWithLifecycle().value }
    val errors = items.map { it.error.collectAsStateWithLifecycle().value }
    var started by rememberSaveable { mutableStateOf(false) }
    var cancelRequested by rememberSaveable { mutableStateOf(false) }
    var detailsOpen by rememberSaveable { mutableStateOf(false) }

    //items that cannot be installed on this device do not count
    val countable = items.indices.filter { states[it] != InstallableItem.State.NOT_EXISTS }
    val total = countable.size
    val finished = countable.count { states[it] == InstallableItem.State.FINISHED }
    val running = countable.any { states[it] == InstallableItem.State.RUNNING }
    val failed = countable.count { errors[it] != null }
    val allDone = total > 0 && finished == total

    if (!running && cancelRequested) cancelRequested = false
    val phase = when {
        allDone -> Phase.DONE
        running -> Phase.RUNNING
        failed > 0 -> Phase.FAILED
        started -> Phase.RUNNING
        else -> Phase.WELCOME
    }
    val showDetails = detailsOpen || phase == Phase.FAILED

    val start = {
        started = true
        onStart()
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(XpBody)) {
        val wide = maxWidth >= 600.dp
        val actionLabel = stringResource(if (finished > 0) R.string.all_setup_resume else R.string.all_setup_install)

        //left: brand + where we are
        val leftPart: @Composable () -> Unit = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(com.movtery.zalithlauncher.R.drawable.img_all_logo), "ALL", Modifier.size(56.dp))
                if (phase == Phase.WELCOME) Welcome(finished, total)
                else Progress(phase, finished, total, failed, running, errors)
            }
        }

        //right: what happens / progress, details and the actions
        val rightPart: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (phase == Phase.WELCOME) {
                    WelcomeSteps(actionLabel)
                } else {
                    XpProgressBar(finished.toFloat() / total.coerceAtLeast(1), animate = running)
                    Text(stringResource(R.string.all_setup_progress, finished, total), color = XpMuted, fontSize = 13.sp)
                    ProgressNote(phase, failed, errors)
                    Text(
                        (if (showDetails) "▾ " else "▸ ") + stringResource(R.string.all_setup_details),
                        color = XpBorder, fontSize = 13.sp,
                        modifier = Modifier.clickable { detailsOpen = !detailsOpen }.padding(vertical = 4.dp)
                    )
                    if (showDetails) Details(items, states, errors)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (phase) {
                        Phase.WELCOME -> XpButton(actionLabel, onClick = start, default = true, enabled = total > 0, modifier = Modifier.weight(1f))
                        Phase.RUNNING -> XpButton(
                            stringResource(if (cancelRequested) R.string.all_setup_cancelling else R.string.all_setup_cancel),
                            onClick = { cancelRequested = true; started = false; onCancel() },
                            enabled = !cancelRequested && running, modifier = Modifier.weight(1f)
                        )
                        Phase.FAILED -> {
                            XpButton(stringResource(R.string.all_setup_cancel), onClick = {
                                items.forEach { it.updateError(null) }
                                started = false
                            }, modifier = Modifier.weight(1f))
                            XpButton(stringResource(R.string.all_setup_retry), onClick = start, default = true, modifier = Modifier.weight(1f))
                        }
                        Phase.DONE -> XpButton(stringResource(R.string.all_setup_finish), onClick = {}, default = true, enabled = false, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (wide) Row(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) { leftPart() }
            Box(Modifier.weight(1.2f)) { rightPart() }
        } else Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)
        ) { leftPart(); rightPart() }
    }
}

@Composable
private fun Welcome(finished: Int, total: Int) {
    Text(stringResource(R.string.all_setup_welcome_title), color = Color.White, fontWeight = FontWeight.Medium, fontSize = 18.sp, textAlign = TextAlign.Center)
    Text(stringResource(R.string.all_setup_welcome_body), color = XpMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
    if (finished in 1 until total) {
        Text(stringResource(R.string.all_setup_welcome_partial, finished, total), color = XpMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun WelcomeSteps(action: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(XpCard).padding(horizontal = 14.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(R.string.all_setup_step_check, R.string.all_setup_step_install, R.string.all_setup_step_ready).forEachIndexed { i, res ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${i + 1}", color = XpBorder, fontSize = 14.sp)
                Text(stringResource(res), color = XpInk, fontSize = 14.sp)
            }
        }
    }
    Text(stringResource(R.string.all_setup_welcome_hint, action), color = XpMuted, fontSize = 12.sp)
}

/** Left side while installing: the three steps (or the big "ready" mark). */
@Composable
private fun Progress(phase: Phase, finished: Int, total: Int, failed: Int, running: Boolean, errors: List<String?>) {
    val done = phase == Phase.DONE
    if (done) Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("\u2714", color = XpOk, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp)
        Text(stringResource(R.string.all_setup_ready_title), color = XpOk, fontWeight = FontWeight.Medium, fontSize = 18.sp)
    } else Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(XpCard).padding(horizontal = 14.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Step(stringResource(R.string.all_setup_step_check), StepMark.DONE)
        Step(stringResource(R.string.all_setup_step_install), if (phase == Phase.FAILED) StepMark.FAILED else StepMark.ACTIVE)
        Step(stringResource(R.string.all_setup_step_ready), StepMark.TODO)
    }
}

/** Right side note under the bar: the waiting text, or the failure (red chip) with the reason. */
@Composable
private fun ProgressNote(phase: Phase, failed: Int, errors: List<String?>) {
    when (phase) {
        Phase.RUNNING -> Text(stringResource(R.string.all_setup_installing_body), color = XpMuted, fontSize = 12.sp)
        Phase.FAILED -> Text(
            stringResource(R.string.all_setup_failed, failed) + " " + stringResource(when (classifySetupError(errors.firstOrNull { it != null })) {
                SetupErrorKind.NO_SPACE -> R.string.all_setup_err_space
                SetupErrorKind.CANNOT_WRITE -> R.string.all_setup_err_write
                SetupErrorKind.OTHER -> R.string.all_setup_err_other
            }),
            color = XpError, fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF3A2128)).padding(horizontal = 12.dp, vertical = 9.dp)
        )
        Phase.DONE -> Text(stringResource(R.string.all_setup_done), color = XpOk, fontSize = 13.sp)
        Phase.WELCOME -> Unit
    }
}

private enum class StepMark(val glyph: String, val color: Color) {
    DONE("✔", XpOk), ACTIVE("▶", XpBorder), FAILED("✖", XpError), TODO("○", XpMuted)
}

@Composable
private fun Step(label: String, mark: StepMark) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(mark.glyph, color = mark.color, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.width(24.dp))
        Text(
            label, color = if (mark == StepMark.TODO) XpMuted else XpInk, fontSize = 14.sp,
            fontWeight = if (mark == StepMark.ACTIVE) FontWeight.Medium else FontWeight.Normal
        )
    }
}

/** Flat progress bar: the filled part is the real finished fraction; while working, a small indeterminate bar shows it is alive. */
@Composable
private fun XpProgressBar(fraction: Float, animate: Boolean) {
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(5.dp)).background(XpChip)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(XpBlue))
    }
}

@Composable
private fun Details(items: List<InstallableItem>, states: List<InstallableItem.State>, errors: List<String?>) {
    Column(
        Modifier.fillMaxWidth().heightIn(max = 220.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF232B3D)).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEachIndexed { i, item ->
            val message by item.task.taskMessage.collectAsStateWithLifecycle()
            val label = when (states[i]) {
                InstallableItem.State.NOT_STARTED -> stringResource(R.string.all_startup_not_installed)
                InstallableItem.State.PENDING -> stringResource(R.string.all_startup_update_required)
                InstallableItem.State.RUNNING -> stringResource(R.string.all_startup_unpacking)
                InstallableItem.State.FINISHED -> stringResource(R.string.all_startup_ready)
                InstallableItem.State.NOT_EXISTS -> stringResource(R.string.all_startup_unavailable)
            }
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.name, color = XpInk, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(
                        label, fontSize = 14.sp,
                        color = when { errors[i] != null -> XpError; states[i] == InstallableItem.State.FINISHED -> XpOk; else -> XpMuted }
                    )
                }
                errors[i]?.let {
                    Text(stringResource(when (classifySetupError(it)) {
                        SetupErrorKind.NO_SPACE -> R.string.all_setup_item_err_space
                        SetupErrorKind.CANNOT_WRITE -> R.string.all_setup_item_err_write
                        SetupErrorKind.OTHER -> R.string.all_setup_item_err_other
                    }), color = XpError, fontSize = 13.sp)
                    Text(it, color = XpMuted, fontSize = 11.sp)
                }
                if (states[i] == InstallableItem.State.RUNNING) message?.let { Text(it, color = XpMuted, fontSize = 12.sp) }
            }
        }
    }
}
