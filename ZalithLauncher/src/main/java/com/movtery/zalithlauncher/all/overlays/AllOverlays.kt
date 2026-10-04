package com.movtery.zalithlauncher.all.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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

private val OvSurface = Color(0xFFF3F1E8)
private val OvCard = Color(0xFFFFFFFF)
private val OvBorder = Color(0xFFB9B4A2)
private val OvText = Color(0xFF1B1B1B)
private val OvDim = Color(0xFF6B675C)
private val OvAccent = Color(0xFF316AC5)
private val OvDanger = Color(0xFFB3261E)

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

/** ALL-styled replacement for Zalith's TitleTaskFlowDialog: the launch / install phase list with a Cancel button. */
@Composable
fun AllTaskFlowDialog(title: String, tasks: List<TitledTask>, onCancel: () -> Unit) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnClickOutside = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(OvSurface)
                .border(1.dp, OvBorder, RoundedCornerShape(8.dp))
        ) {
            Text(
                title, color = Color.White, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OvAccent)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
            LazyColumn(
                modifier = Modifier.heightIn(max = 260.dp).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(tasks) { t ->
                    val progress by t.task.progress.collectAsStateWithLifecycle()
                    val message by t.task.message.collectAsStateWithLifecycle()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(OvCard)
                            .border(1.dp, OvBorder, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(resolveAndroidString(t.title).text, color = OvText, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        message?.let { Text(resolveAndroidString(it).text, color = OvDim, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                        if (progress < 0f) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = OvAccent)
                        else LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), color = OvAccent)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(110.dp)
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(OvCard)
                        .border(1.dp, OvDanger, RoundedCornerShape(4.dp))
                        .clickable(onClick = onCancel)
                ) { Text(stringResource(R.string.generic_cancel), color = OvDanger, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
