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
 * ALL-owned first-run setup wizard (Windows XP look) over the existing unpack items and tasks.
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

    BoxWithConstraints(Modifier.fillMaxSize().background(XpDesktop).padding(6.dp)) {
        val wide = maxWidth >= 600.dp
        Column(
            Modifier.fillMaxSize()
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .border(2.dp, XpBorder, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(XpBody)
        ) {
            XpTitleBar(stringResource(R.string.all_setup_title))
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(Modifier.widthIn(max = if (wide) 640.dp else 480.dp).fillMaxWidth()) {
                    Column(
                        Modifier.weight(1f, fill = false).fillMaxWidth().padding(8.dp)
                            .background(Color.White).border(1.dp, Color(0xFF7F9DB9)).padding(10.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (phase) {
                            Phase.WELCOME -> Welcome(finished, total, stringResource(if (finished > 0) R.string.all_setup_resume else R.string.all_setup_install))
                            else -> Progress(phase, finished, total, failed, running, errors)
                        }
                        if (phase != Phase.WELCOME) {
                            Text(
                                (if (showDetails) "▾ " else "▸ ") + stringResource(R.string.all_setup_details),
                                color = XpBorder, fontSize = 14.sp,
                                modifier = Modifier.clickable { detailsOpen = !detailsOpen }.padding(vertical = 4.dp)
                            )
                            if (showDetails) Details(items, states, errors)
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (phase) {
                            Phase.WELCOME -> XpButton(
                                stringResource(if (finished > 0) R.string.all_setup_resume else R.string.all_setup_install),
                                onClick = start, default = true, enabled = total > 0
                            )
                            Phase.RUNNING -> XpButton(
                                stringResource(if (cancelRequested) R.string.all_setup_cancelling else R.string.all_setup_cancel),
                                onClick = { cancelRequested = true; started = false; onCancel() },
                                enabled = !cancelRequested && running
                            )
                            Phase.FAILED -> {
                                XpButton(stringResource(R.string.all_setup_cancel), onClick = {
                                    items.forEach { it.updateError(null) }
                                    started = false
                                })
                                XpButton(stringResource(R.string.all_setup_retry), onClick = start, default = true)
                            }
                            Phase.DONE -> XpButton(stringResource(R.string.all_setup_finish), onClick = {}, default = true, enabled = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Welcome(finished: Int, total: Int, action: String) {
    Text(stringResource(R.string.all_setup_welcome_title), color = XpBorder, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    Text(stringResource(R.string.all_setup_welcome_body), color = XpInk, fontSize = 16.sp)
    if (finished in 1 until total) {
        Text(stringResource(R.string.all_setup_welcome_partial, finished, total), color = XpMuted, fontSize = 15.sp)
    }
    Text(
        listOf(stringResource(R.string.all_setup_step_check), stringResource(R.string.all_setup_step_install), stringResource(R.string.all_setup_step_ready)).joinToString("  ▸  "),
        color = XpBorder, fontWeight = FontWeight.Bold, fontSize = 15.sp
    )
    Text(stringResource(R.string.all_setup_welcome_hint, action), color = XpMuted, fontSize = 14.sp)
}

@Composable
private fun Progress(phase: Phase, finished: Int, total: Int, failed: Int, running: Boolean, errors: List<String?>) {
    val done = phase == Phase.DONE
    if (done) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("\u2714", color = XpOk, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp)
        Text(stringResource(R.string.all_setup_ready_title), color = XpOk, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
    //the big "ready" header already says it; keep the finished page short so it fits a landscape phone
    if (!done) {
        Step(stringResource(R.string.all_setup_step_check), StepMark.DONE)
        Step(
            stringResource(R.string.all_setup_step_install),
            when {
                done -> StepMark.DONE
                phase == Phase.FAILED -> StepMark.FAILED
                else -> StepMark.ACTIVE
            }
        )
        Step(stringResource(R.string.all_setup_step_ready), if (done) StepMark.DONE else StepMark.TODO)
    }

    XpProgressBar(finished.toFloat() / total.coerceAtLeast(1), animate = running)
    Text(stringResource(R.string.all_setup_progress, finished, total), color = XpMuted, fontSize = 14.sp)

    when (phase) {
        Phase.RUNNING -> Text(stringResource(R.string.all_setup_installing_body), color = XpInk, fontSize = 16.sp)
        Phase.FAILED -> {
            Text(stringResource(R.string.all_setup_failed, failed), color = XpError, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(stringResource(when (classifySetupError(errors.firstOrNull { it != null })) {
                SetupErrorKind.NO_SPACE -> R.string.all_setup_err_space
                SetupErrorKind.CANNOT_WRITE -> R.string.all_setup_err_write
                SetupErrorKind.OTHER -> R.string.all_setup_err_other
            }), color = XpInk, fontSize = 16.sp)
        }
        Phase.DONE -> Text(stringResource(R.string.all_setup_done), color = XpOk, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Phase.WELCOME -> Unit
    }
}

private enum class StepMark(val glyph: String, val color: Color) {
    DONE("✔", XpOk), ACTIVE("▶", XpBorder), FAILED("✖", XpError), TODO("○", XpMuted)
}

@Composable
private fun Step(label: String, mark: StepMark) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(mark.glyph, color = mark.color, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.width(24.dp))
        Text(
            label, color = if (mark == StepMark.TODO) XpMuted else XpInk, fontSize = 16.sp,
            fontWeight = if (mark == StepMark.ACTIVE) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/** XP block progress bar: filled blocks = real finished fraction; while working one extra block group sweeps over the rest */
@Composable
private fun XpProgressBar(fraction: Float, animate: Boolean) {
    val sweep = rememberInfiniteTransition(label = "xp-sweep").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "xp-sweep-x"
    ).value
    Canvas(
        Modifier.fillMaxWidth().height(20.dp)
            .clip(RoundedCornerShape(3.dp)).background(Color.White)
            .border(1.dp, Color(0xFF686868), RoundedCornerShape(3.dp))
    ) {
        val block = 10.dp.toPx()
        val gap = 2.dp.toPx()
        val pad = 2.dp.toPx()
        val slot = block + gap
        val capacity = ((size.width - pad * 2) / slot).toInt().coerceAtLeast(1)
        val filled = (fraction.coerceIn(0f, 1f) * capacity).toInt()
        val h = size.height - pad * 2
        fun draw(i: Int) = drawRoundRect(
            XpGreen, Offset(pad + i * slot, pad), Size(block, h), CornerRadius(1.5.dp.toPx())
        )
        for (i in 0 until filled) draw(i)
        if (animate && filled < capacity) {
            val span = capacity - filled
            val head = filled + (sweep * (span + 3)).toInt()
            for (k in 0..2) { val i = head - k; if (i in filled until capacity) draw(i) }
        }
    }
}

@Composable
private fun Details(items: List<InstallableItem>, states: List<InstallableItem.State>, errors: List<String?>) {
    Column(
        Modifier.fillMaxWidth().heightIn(max = 220.dp).background(Color(0xFFFAFAF5))
            .border(1.dp, Color(0xFFD0D0C8)).padding(8.dp),
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
