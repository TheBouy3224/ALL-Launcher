package com.movtery.zalithlauncher.all.overlays

import com.movtery.zalithlauncher.all.ui.AllColors
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ui.screens.content.readLog
import com.movtery.zalithlauncher.utils.copyText
import com.movtery.zalithlauncher.utils.file.shareFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Uses the existing bounded log reader, clipboard and Android share actions. Save exports the original file via SAF. */
@Composable
fun AllLogViewDialog(file: File, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var content by remember(file) { mutableStateOf<String?>(null) }
    var status by remember(file) { mutableStateOf<String?>(null) }
    val saved = stringResource(R.string.all_log_saved)
    val copied = stringResource(R.string.all_log_copied)
    val failed = stringResource(R.string.all_log_failed)
    LaunchedEffect(file) {
        runCatching { withContext(Dispatchers.IO) { readLog(file) } }
            .onSuccess { content = it }.onFailure { status = "$failed: ${it.message}" }
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) scope.launch {
            runCatching { withContext(Dispatchers.IO) {
                requireNotNull(context.contentResolver.openOutputStream(uri)).use { output -> file.inputStream().use { it.copyTo(output) } }
            } }.onSuccess { status = saved }.onFailure { status = "$failed: ${it.message}" }
        }
    }
    DlgFrame(file.name, onClose) {
        SelectionContainer {
            Text(content ?: "…", color = AllColors.Text, modifier = Modifier.fillMaxWidth().heightIn(max = 210.dp).verticalScroll(rememberScrollState()))
        }
        status?.let { Text(it, color = AllColors.Text) }
        Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DlgButton(stringResource(R.string.generic_copy), {
                runCatching { copyText(file.name, content, context, showToast = false) }.onSuccess { status = copied }.onFailure { status = "$failed: ${it.message}" }
            }, enabled = content != null)
            DlgButton(stringResource(R.string.generic_save), { save.launch(file.name) })
            DlgButton(stringResource(R.string.crash_share_logs), {
                runCatching { shareFile(context, file) }.onFailure { status = "$failed: ${it.message}" }
            })
            DlgButton(stringResource(R.string.generic_close), onClose)
        }
    }
}
