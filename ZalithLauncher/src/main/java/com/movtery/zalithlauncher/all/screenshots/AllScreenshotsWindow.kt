package com.movtery.zalithlauncher.all.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File

/**
 * ALL screenshots window (PHASE 3.3c).
 *
 * Same rules as Zalith's screenshots page: only .png files in the screenshots folder, thumbnails loaded
 * by Coil straight from the file, delete via FileUtils. Tapping a thumbnail enlarges it in a dialog
 * (no new image subsystem). Zalith's export-to-gallery lives in its own private ViewModel code and is
 * not duplicated here, so there is no export yet.
 */

private val ShotSurface = Color(0xFFF3F1E8)
private val ShotCard = Color(0xFFFFFFFF)
private val ShotBorder = Color(0xFFB9B4A2)
private val ShotText = Color(0xFF1B1B1B)
private val ShotDim = Color(0xFF6B675C)
private val ShotAccent = Color(0xFF316AC5)
private val ShotDanger = Color(0xFFB3261E)

class AllScreenshotsViewModel(private val dir: File) : ViewModel() {
    var items by mutableStateOf<List<File>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
    private var loadedCount = -1

    private fun fileCount() = dir.listFiles()?.size ?: 0

    fun load() {
        loadedCount = fileCount()
        loading = true
        error = null
        viewModelScope.launch {
            items = withContext(Dispatchers.IO) {
                dir.listFiles { f -> f.isFile && f.extension.equals("png", true) }.orEmpty()
                    .sortedByDescending { it.lastModified() }
            }
            loading = false
        }
    }

    fun loadIfChanged() {
        if (loadedCount != fileCount()) load()
    }

    fun delete(file: File) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { FileUtils.deleteQuietly(file) }
            if (!ok) error = file.name
            load()
        }
    }
}

@Composable
fun AllScreenshotsWindow(version: Version, modifier: Modifier = Modifier) {
    val dir = remember(version) { VersionFolders.SCREENSHOTS.getDir(version.getGameDir()) }
    val viewModel: AllScreenshotsViewModel = viewModel(key = "ALL_Screenshots_" + version.toString()) {
        AllScreenshotsViewModel(dir)
    }
    LaunchedEffect(Unit) { viewModel.loadIfChanged() }

    var preview by remember { mutableStateOf<File?>(null) }
    var pendingDelete by remember { mutableStateOf<File?>(null) }

    Column(modifier = modifier.fillMaxSize().background(ShotSurface)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            ShotButton(stringResource(R.string.generic_refresh)) { viewModel.load() }
            Spacer(Modifier.width(10.dp))
            Text("${viewModel.items.size}", color = ShotDim, fontSize = 12.sp)
        }
        viewModel.error?.let {
            Text(
                stringResource(R.string.generic_error) + ": " + it,
                color = ShotDanger, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
        }
        when {
            viewModel.loading && viewModel.items.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = ShotAccent)
            }
            viewModel.items.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(stringResource(R.string.screenshots_manage_no_screenshots), color = ShotDim)
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(viewModel.items, key = { it.name }) { file ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ShotCard)
                            .border(1.dp, ShotBorder, RoundedCornerShape(6.dp))
                            .clickable { preview = file }
                    ) {
                        AsyncImage(
                            model = file,
                            contentDescription = file.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            file.name,
                            color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color(0x99000000))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }

    preview?.let { file ->
        AlertDialog(
            onDismissRequest = { preview = null },
            confirmButton = { TextButton(onClick = { preview = null }) { Text(stringResource(R.string.generic_close)) } },
            dismissButton = {
                TextButton(onClick = { pendingDelete = file; preview = null }) {
                    Text(stringResource(R.string.generic_delete), color = ShotDanger)
                }
            },
            text = {
                Column {
                    AsyncImage(
                        model = file,
                        contentDescription = file.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)
                    )
                    Text(file.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                    Text(FileUtils.byteCountToDisplaySize(file.length()), fontSize = 11.sp, color = ShotDim)
                }
            }
        )
    }

    pendingDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.generic_delete)) },
            text = { Text(file.name) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(file); pendingDelete = null }) {
                    Text(stringResource(R.string.generic_confirm), color = ShotDanger)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.generic_cancel)) } }
        )
    }
}

@Composable
private fun ShotButton(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(ShotCard)
            .border(1.dp, ShotBorder, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
    ) { Text(text, color = ShotText, fontWeight = FontWeight.Bold, maxLines = 1) }
}
