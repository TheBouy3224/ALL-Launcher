package com.movtery.zalithlauncher.all.files

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.hilt.lifecycle.viewmodel.HiltViewModelFactory
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.filemanager.logic.entry.FmEntry
import com.movtery.zalithlauncher.filemanager.logic.ops.ConflictResolution
import com.movtery.zalithlauncher.filemanager.viewmodel.DialogIntent
import com.movtery.zalithlauncher.filemanager.viewmodel.FileManagerViewModel
import com.movtery.zalithlauncher.filemanager.viewmodel.FmInitState
import com.movtery.zalithlauncher.game.version.installed.Version
import java.nio.file.Path
import java.nio.file.Paths
import org.apache.commons.io.FileUtils

/** ALL-owned Files window content. All disk operations remain in the existing Zalith file backend. */
private val FilesSurface = Color(0xFFF3F1E8)
private val FilesPanel = Color(0xFFFFFFFF)
private val FilesPanelMuted = Color(0xFFEAE8DF)
private val FilesLine = Color(0xFFB9B4A2)
private val FilesText = Color(0xFF1B1B1B)
private val FilesMuted = Color(0xFF69665D)
private val FilesBlue = Color(0xFF316AC5)
private val FilesGreen = Color(0xFF2E7D32)
private val FilesRed = Color(0xFFB3261E)
private val FilesErrorBg = Color(0xFFFFE9E7)

@Composable
fun AllFilesWindow(
    version: Version,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rootPath = remember(version) { version.getGameDir().absolutePath }
    val backend = rememberFileManagerBackend(context, rootPath)
    val allViewModel: AllFilesViewModel = viewModel(key = "ALL_Files_" + rootPath) {
        AllFilesViewModel(backend)
    }
    val allState by allViewModel.state.collectAsState()
    val backendState = allState.backend

    var createFolderOpen by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FmEntry?>(null) }
    var deleteTarget by remember { mutableStateOf<FmEntry?>(null) }
    var folderName by remember { mutableStateOf("") }
    var renameName by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) allViewModel.importFiles(uris)
    }

    BackHandler(enabled = backendState.canBack ||
        backendState.dialogIntent is DialogIntent.PasteConflict) {
        if (backendState.dialogIntent is DialogIntent.PasteConflict) {
            allViewModel.resolvePasteConflict(ConflictResolution.SKIP)
        } else {
            allViewModel.consumeBack()
        }
    }

    LaunchedEffect(backendState.snackbar) {
        if (backendState.snackbar != null) {
            kotlinx.coroutines.delay(3500)
            allViewModel.consumeSnackbar()
        }
    }
    LaunchedEffect(allState.errorMessage) {
        if (allState.errorMessage != null) {
            kotlinx.coroutines.delay(4500)
            allViewModel.consumeError()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FilesSurface)
            .padding(8.dp)
    ) {
        AllFilesBreadcrumb(
            root = backendState.rawList?.rootDir,
            current = backendState.currentDir,
            onNavigate = allViewModel::navigate
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            AllFilesButton(
                text = "↑  " + stringResource(R.string.generic_back),
                enabled = backendState.canBack,
                onClick = { allViewModel.goParent() }
            )
            AllFilesButton(
                text = stringResource(R.string.generic_refresh),
                onClick = allViewModel::refresh
            )
            AllFilesButton(
                text = "+",
                onClick = {
                    folderName = ""
                    createFolderOpen = true
                }
            )
            AllFilesButton(
                text = stringResource(R.string.generic_import),
                onClick = { filePicker.launch("*/*") }
            )
            AllFilesButton(
                text = stringResource(R.string.generic_paste),
                enabled = backendState.clipboard != null,
                onClick = allViewModel::paste
            )
            Text(
                text = "${backendState.folderCount} folders · ${backendState.fileCount} files",
                color = FilesMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        val message = allState.errorMessage ?: backendState.snackbar?.text
        if (message != null) {
            Snackbar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                containerColor = if (allState.errorMessage != null) FilesErrorBg else FilesPanelMuted
            ) {
                Text(message, color = FilesText)
            }
        }

        when {
            allState.init is FmInitState.Pending -> AllFilesLoading()
            allState.init is FmInitState.Failed -> AllFilesMessage(
                text = (allState.init as FmInitState.Failed).message,
                color = FilesRed
            )
            backendState.refreshing && backendState.rawList == null -> AllFilesLoading()
            backendState.rawList == null -> AllFilesMessage(
                text = stringResource(R.string.fm_empty_dir),
                color = FilesMuted
            )
            backendState.visibleEntries.isEmpty() -> AllFilesMessage(
                text = stringResource(R.string.fm_empty_dir),
                color = FilesMuted
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(backendState.visibleEntries, key = { it.path.toString() }) { entry ->
                    AllFilesRow(
                        entry = entry,
                        onOpen = { if (entry.isDirectory) allViewModel.enter(entry) },
                        onCopy = { allViewModel.copy(entry) },
                        onMove = { allViewModel.move(entry) },
                        onRename = {
                            renameName = entry.name
                            renameTarget = entry
                        },
                        onDelete = { deleteTarget = entry }
                    )
                }
            }
        }
    }

    if (createFolderOpen) {
        AllFilesInputDialog(
            title = stringResource(R.string.fm_new_name),
            value = folderName,
            onValueChange = { folderName = it },
            onDismiss = { createFolderOpen = false },
            onConfirm = {
                allViewModel.createFolder(folderName) { success ->
                    if (success) createFolderOpen = false
                }
            }
        )
    }

    renameTarget?.let { entry ->
        AllFilesInputDialog(
            title = stringResource(R.string.generic_rename),
            value = renameName,
            onValueChange = { renameName = it },
            onDismiss = { renameTarget = null },
            onConfirm = {
                allViewModel.rename(entry, renameName) { renameTarget = null }
            }
        )
    }

    deleteTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.generic_delete)) },
            text = { Text(stringResource(R.string.fm_move_to_trash) + ": " + entry.name) },
            confirmButton = {
                TextButton(onClick = {
                    allViewModel.deleteToTrash(entry)
                    deleteTarget = null
                }) {
                    Text(stringResource(R.string.generic_delete), color = FilesRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.generic_cancel))
                }
            }
        )
    }

    (backendState.dialogIntent as? DialogIntent.PasteConflict)?.let { conflict ->
        val item = conflict.request.conflicts.getOrNull(conflict.currentIndex)
        AlertDialog(
            onDismissRequest = { allViewModel.resolvePasteConflict(ConflictResolution.SKIP) },
            title = { Text(stringResource(R.string.fm_conflict_title)) },
            text = { Text(item?.let { "${it.source.fileName} → ${it.existing.fileName}" }.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { allViewModel.resolvePasteConflict(ConflictResolution.KEEP_BOTH) }) {
                    Text(stringResource(R.string.fm_conflict_keep_both))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { allViewModel.resolvePasteConflict(ConflictResolution.OVERWRITE) }) {
                        Text(stringResource(R.string.fm_conflict_overwrite), color = FilesRed)
                    }
                    TextButton(onClick = { allViewModel.resolvePasteConflict(ConflictResolution.SKIP) }) {
                        Text(stringResource(R.string.generic_cancel))
                    }
                }
            }
        )
    }
}

@Composable
private fun rememberFileManagerBackend(context: Context, rootPath: String): FileManagerViewModel {
    val owner = checkNotNull(LocalViewModelStoreOwner.current)
    val defaults = owner as HasDefaultViewModelProviderFactory
    return viewModel(
        viewModelStoreOwner = owner,
        key = "all_desktop_files_" + rootPath,
        factory = HiltViewModelFactory(context, defaults.defaultViewModelProviderFactory),
        extras = MutableCreationExtras(defaults.defaultViewModelCreationExtras).apply {
            set(DEFAULT_ARGS_KEY, bundleOf(FileManagerViewModel.KEY_ROOT_PATH to rootPath))
        }
    )
}

@Composable
private fun AllFilesBreadcrumb(root: Path?, current: Path?, onNavigate: (Path) -> Unit) {
    val path = current ?: root
    if (path == null) return
    val rootPath = root ?: path.root
    val segments = buildList {
        add(rootPath)
        var next = rootPath
        path.toAbsolutePath().normalize().forEach { segment ->
            next = next.resolve(segment)
            if (next.startsWith(rootPath) && next != rootPath) add(next)
        }
    }.distinct()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(5.dp))
            .background(FilesPanel)
            .border(1.dp, FilesLine, RoundedCornerShape(5.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(
            text = stringResource(R.string.all_desktop_files) + "  ·  " + path.fileName?.toString().orEmpty().ifEmpty { path.toString() },
            color = FilesText,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            segments.forEachIndexed { index, segment ->
                if (index > 0) Text("  ›  ", color = FilesMuted)
                Text(
                    text = segment.fileName?.toString().orEmpty().ifEmpty { segment.toString() },
                    color = if (segment == path) FilesText else FilesBlue,
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.clickable { onNavigate(segment) }
                )
            }
        }
        Text(
            text = path.toString(),
            color = FilesMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AllFilesRow(
    entry: FmEntry,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(5.dp))
            .background(FilesPanel)
            .border(1.dp, FilesLine, RoundedCornerShape(5.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = if (entry.isDirectory) "📁" else "▤",
            fontSize = 24.sp,
            modifier = Modifier.width(38.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                color = FilesText,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (entry.isDirectory) stringResource(R.string.fm_property_folders)
                    else "${FileUtils.byteCountToDisplaySize(entry.size)} · ${entry.path.fileName.toString().substringAfterLast('.', "file").uppercase()}",
                color = FilesMuted,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        if (entry.writable) {
            AllFilesAction(stringResource(R.string.generic_copy), onCopy)
            AllFilesAction(stringResource(R.string.fm_cut), onMove)
            AllFilesAction(stringResource(R.string.generic_rename), onRename)
            AllFilesAction(stringResource(R.string.generic_delete), onDelete, color = FilesRed)
        }
    }
}

@Composable
private fun AllFilesAction(text: String, onClick: () -> Unit, color: Color = FilesBlue) {
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = Modifier
            .padding(start = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(FilesPanelMuted)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    )
}

@Composable
private fun AllFilesButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Text(
        text = text,
        color = if (enabled) FilesText else FilesMuted.copy(alpha = 0.5f),
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (enabled) FilesPanel else FilesPanelMuted)
            .border(1.dp, FilesLine, RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    )
}

@Composable
private fun AllFilesInputDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = FilesText, fontSize = 16.sp),
                cursorBrush = SolidColor(FilesBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .background(FilesPanelMuted, RoundedCornerShape(4.dp))
                    .padding(12.dp)
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.generic_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.generic_cancel)) } }
    )
}

@Composable
private fun AllFilesLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = FilesBlue)
    }
}

@Composable
private fun AllFilesMessage(text: String, color: Color) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, color = color, modifier = Modifier.padding(20.dp))
    }
}
