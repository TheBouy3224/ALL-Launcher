package com.movtery.zalithlauncher.all.overlays

import com.movtery.zalithlauncher.all.ui.AllColors
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.coroutine.TitledTask
import com.movtery.zalithlauncher.ui.resolveAndroidString
import com.movtery.zalithlauncher.utils.file.formatFileSize

/**
 * ALL-owned task / progress presentation (PHASE 3.7).
 *
 * Both composables only READ the existing task system ([TaskSystem.tasksFlow], [Task] progress / message / speed flows,
 * [TitledTask] flows of GameInstaller & friends) and call [TaskSystem.cancelTask] / the caller's onCancel.
 * Nothing about scheduling, execution or cancellation semantics is changed.
 */

private val OvSurface: Color get() = AllColors.WindowBody
private val OvCard: Color get() = AllColors.Card
private val OvBorder: Color get() = AllColors.Border
private val OvText: Color get() = AllColors.Text
private val OvDim: Color get() = AllColors.TextDim
private val OvAccent: Color get() = AllColors.Blue
private val OvDanger: Color get() = AllColors.Danger

/** Content of the "Tasks" window on the ALL Desktop: every running background task with progress and cancel. */
@Composable
fun AllTasksWindow(modifier: Modifier = Modifier) {
    val tasks by TaskSystem.tasksFlow.collectAsStateWithLifecycle()
    if (tasks.isEmpty()) {
        Box(modifier.fillMaxSize().background(OvSurface), Alignment.Center) {
            Text(stringResource(R.string.all_tasks_empty), color = OvDim)
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().background(OvSurface).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(tasks, key = { it.id }) { task -> TaskRow(task) { TaskSystem.cancelTask(task.id) } }
    }
}

@Composable
private fun TaskRow(task: Task, onCancel: () -> Unit) {
    val progress by task.progress.collectAsStateWithLifecycle()
    val message by task.message.collectAsStateWithLifecycle()
    val rate by task.rateBytesPerSec.collectAsStateWithLifecycle()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(OvCard)
            .border(1.dp, OvBorder, RoundedCornerShape(6.dp))
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message?.let { resolveAndroidString(it).text } ?: task.id,
                color = OvText, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            if (progress < 0f) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), color = OvAccent)
            } else {
                LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), color = OvAccent)
            }
            rate?.let { Text(formatFileSize(it) + "/s", color = OvDim, fontSize = 11.sp) }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(start = 8.dp)
                .heightIn(min = 40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(OvCard)
                .border(1.dp, OvDanger, RoundedCornerShape(4.dp))
                .clickable(onClick = onCancel)
                .padding(horizontal = 12.dp)
        ) { Text(stringResource(R.string.generic_cancel), color = OvDanger, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
    }
}

/**
 * ALL V3 replacement for Zalith's TitleTaskFlowDialog: the launch / install phase list with a Cancel button.
 * Everything shown is read from the real tasks: stage (waiting / running / done), message, progress (a bar only while running;
 * a percentage only when the task reports one, otherwise an indeterminate bar) and download speed (only when reported).
 */
@Composable
fun AllTaskFlowDialog(title: String, tasks: List<TitledTask>, onCancel: () -> Unit, logOutput: com.movtery.zalithlauncher.coroutine.TaskLogOutput? = null) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false)) {
        val shape = RoundedCornerShape(14.dp)
        //centred on the window the launch was started from (same gap left and right), not on the whole screen
        val win = com.movtery.zalithlauncher.all.ui.AllFocusedWindowBounds.rect
        val density = androidx.compose.ui.platform.LocalDensity.current
        val maxW = win?.let { with(density) { (it.width.toDp() - 32.dp).coerceIn(260.dp, 380.dp) } } ?: 380.dp
        //the dialog window does not start at the screen origin (display cutout / insets): convert the window rect into its coordinates
        var origin by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
        val local = win?.let { r -> origin?.let { r.translate(-it.x, -it.y) } }
        Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.localToScreen(androidx.compose.ui.geometry.Offset.Zero) }) {
        Column(
            modifier = Modifier
                .then(if (local == null) Modifier.align(Alignment.Center).padding(horizontal = 16.dp) else Modifier.layout { m, c ->
                    val p = m.measure(c.copy(minWidth = 0, minHeight = 0))
                    layout(c.maxWidth, c.maxHeight) {
                        p.place(
                            (local.center.x - p.width / 2f).toInt().coerceIn(0, (c.maxWidth - p.width).coerceAtLeast(0)),
                            (local.center.y - p.height / 2f).toInt().coerceIn(0, (c.maxHeight - p.height).coerceAtLeast(0))
                        )
                    }
                })
                .widthIn(max = maxW)
                .fillMaxWidth()
                .clip(shape)
                .background(OvSurface)
                .border(1.dp, OvBorder, shape)
        ) {
            Text(
                title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth().background(Color(0xFF223049)).padding(horizontal = 14.dp, vertical = 12.dp)
            )
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false).heightIn(max = 300.dp).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks) { t ->
                    val progress by t.task.progress.collectAsStateWithLifecycle()
                    val message by t.task.message.collectAsStateWithLifecycle()
                    val stage by t.task.stage.collectAsStateWithLifecycle()
                    val rate by t.task.rateBytesPerSec.collectAsStateWithLifecycle()
                    val done = stage == com.movtery.zalithlauncher.coroutine.TaskStage.COMPLETED
                    val running = stage == com.movtery.zalithlauncher.coroutine.TaskStage.RUNNING
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(OvCard).padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (done) "✔" else if (running) "▶" else "○",
                                color = if (done) AllColors.Ok else if (running) AllColors.BlueLight else OvDim,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.width(24.dp)
                            )
                            Text(
                                resolveAndroidString(t.title).text,
                                color = if (stage == com.movtery.zalithlauncher.coroutine.TaskStage.PREPARING) OvDim else OvText,
                                fontWeight = if (running) FontWeight.Medium else FontWeight.Normal, fontSize = 14.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                            )
                            if (running && progress >= 0f) Text("${(progress.coerceIn(0f, 1f) * 100).toInt()}%", color = OvDim, fontSize = 12.sp)
                        }
                        if (running) {
                            message?.let { Text(resolveAndroidString(it).text, color = OvDim, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 24.dp, top = 2.dp)) }
                            if (progress < 0f) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(5.dp).clip(RoundedCornerShape(3.dp)), color = OvAccent, trackColor = Color(0xFF30415F))
                            else LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(5.dp).clip(RoundedCornerShape(3.dp)), color = OvAccent, trackColor = Color(0xFF30415F))
                            rate?.let { Text(formatFileSize(it) + "/s", color = OvDim, fontSize = 11.sp, modifier = Modifier.padding(start = 24.dp, top = 4.dp)) }
                        }
                    }
                }
            }
            logOutput?.let { output ->
                val lines by output.lines.collectAsStateWithLifecycle()
                if (lines.isNotEmpty()) {
                    Text(resolveAndroidString(output.title).text, color = OvText, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp))
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 100.dp).padding(horizontal = 14.dp)) {
                        items(lines) { Text(it, color = OvDim, fontSize = 11.sp) }
                    }
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp)
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF232B3D))
                    .clickable(onClick = onCancel)
            ) { Text(stringResource(R.string.generic_cancel), color = OvDanger, fontWeight = FontWeight.Medium, fontSize = 15.sp) }
        }
        }
    }
}
