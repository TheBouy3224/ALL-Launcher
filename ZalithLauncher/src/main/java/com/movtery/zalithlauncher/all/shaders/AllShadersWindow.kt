package com.movtery.zalithlauncher.all.shaders

import androidx.compose.ui.graphics.Brush

import com.movtery.zalithlauncher.all.ui.AllColors
import com.movtery.zalithlauncher.all.ui.AllListRow
import androidx.compose.ui.draw.alpha
import com.movtery.zalithlauncher.all.ui.AllButton
import com.movtery.zalithlauncher.all.ui.allStyleForColor
import com.movtery.zalithlauncher.all.ui.AllButtonStyle
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import com.movtery.zalithlauncher.all.ui.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import com.movtery.zalithlauncher.all.ui.TextButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.bridge.AllSubmitError
import com.movtery.zalithlauncher.all.bridge.rememberAllUriImportTaskBuilder
import com.movtery.zalithlauncher.contract.extensionToMimeType
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.ShaderPackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File

/**
 * ALL 自己的光影包窗口内容（PHASE 3.3a）；只列 .zip 文件，与 Zalith 的光影包页规则一致。
 *
 * 读取与解析沿用 Zalith 的 [parseResourcePack] / [ShaderPackInfo]（pack.mcmeta、pack.png、有效性判断），
 * 删除与经典页相同（FileUtils），导入走 Zalith 既有的导入任务（zip 后缀校验、错误上报）。
 * Zalith 的资源包页没有“启用/禁用”（启用状态存在游戏的 options.txt，没有后端），所以这里也没有。
 * 外壳（标题栏、拖动、最小化、焦点）由 AllDesktopScreen 提供。
 */

private val PackSurface: Color get() = AllColors.WindowBody
private val PackCard: Color get() = AllColors.Card
private val PackBorder: Color get() = AllColors.Border
private val PackText: Color get() = AllColors.Text
private val PackDim: Color get() = AllColors.TextDim
private val PackAccent: Color get() = AllColors.Blue
private val PackDanger: Color get() = AllColors.Danger

private const val PACK_EXTENSION = "zip"

/** 状态放 ViewModel：窗口最小化会把内容移出组合，放 remember 里还原时就要重新解析所有包 */
class AllShadersViewModel(private val packDir: File) : ViewModel() {
    var items by mutableStateOf<List<ShaderPackInfo>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
    var filter by mutableStateOf("")

    private var loadedCount = -1

    val visibleItems: List<ShaderPackInfo>
        get() {
            val keyword = filter.trim()
            return items.filter {
                (keyword.isEmpty() || it.file.name.contains(keyword, ignoreCase = true))
            }
        }

    private fun fileCount() = packDir.listFiles()?.size ?: 0

    fun load() {
        loadedCount = fileCount()
        loading = true
        error = null
        viewModelScope.launch {
            items = withContext(Dispatchers.IO) {
                packDir.listFiles().orEmpty().filter { it.isFile && it.extension.equals("zip", true) }.map { ShaderPackInfo(it, FileUtils.sizeOf(it)) }.sortedBy { it.file.name }
            }
            loading = false
        }
    }

    /** 打开/还原窗口时只在目录内容数量变化时重读 */
    fun loadIfChanged() {
        if (loadedCount != fileCount()) load()
    }

    fun delete(item: ShaderPackInfo) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { FileUtils.deleteQuietly(item.file) }
            if (!ok) error = item.file.name
            load()
        }
    }
}

@Composable
fun AllShadersWindow(
    version: Version,
    onOpenDownload: () -> Unit,
    onError: AllSubmitError,
    modifier: Modifier = Modifier
) {
    val packDir = remember(version) { VersionFolders.SHADERS.getDir(version.getGameDir()) }
    val viewModel: AllShadersViewModel = viewModel(key = "ALL_Shaders_" + version.toString()) {
        AllShadersViewModel(packDir)
    }
    LaunchedEffect(Unit) { viewModel.loadIfChanged() }

    var pendingDelete by remember { mutableStateOf<ShaderPackInfo?>(null) }

    val importBuilder = rememberAllUriImportTaskBuilder(
        id = "ContentManager.Shaders.Import",
        targetDir = packDir,
        checkExtension = listOf(PACK_EXTENSION),
        submitError = onError,
        onImported = { viewModel.load() }
    )
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) TaskSystem.submitTask(importBuilder(uris))
    }

    Column(modifier = modifier.fillMaxSize().background(PackSurface)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 8.dp)
        ) {
            SearchField(viewModel.filter, { viewModel.filter = it }, Modifier.weight(1f))
            Spacer(Modifier.width(6.dp))
            PackButton(stringResource(R.string.generic_refresh), onClick = { viewModel.load() })
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 6.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            PackButton(stringResource(R.string.generic_download), onOpenDownload, AllColors.BluePale, PackAccent, PackAccent)
            PackButton(stringResource(R.string.generic_import), onClick = { picker.launch(PACK_EXTENSION.extensionToMimeType()) })
            Text("${viewModel.visibleItems.size}/${viewModel.items.size}", color = PackDim, fontSize = 12.sp)
        }

        viewModel.error?.let {
            Text(
                text = stringResource(R.string.generic_error) + ": " + it,
                color = PackDanger,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(top = 6.dp)
            )
        }

        when {
            viewModel.loading && viewModel.items.isEmpty() -> CenterBox {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PackAccent)
                    Spacer(Modifier.size(10.dp))
                    Text(stringResource(R.string.generic_loading), color = PackDim)
                }
            }
            viewModel.items.isEmpty() -> CenterBox { Text(stringResource(R.string.shader_pack_manage_no_packs), color = PackDim) }
            viewModel.visibleItems.isEmpty() -> CenterBox { Text(stringResource(R.string.generic_no_matching_items), color = PackDim) }
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).padding(top = 6.dp)) {
                items(viewModel.visibleItems, key = { it.file.name }) { pack ->
                    PackRow(pack) { pendingDelete = pack }
                }
            }
        }
    }

    pendingDelete?.let { pack ->
        AlertDialog(
            destructive = true,
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.generic_delete)) },
            text = { Text(stringResource(R.string.shader_pack_manage_delete_warning, pack.file.name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(pack); pendingDelete = null }) {
                    Text(stringResource(R.string.generic_confirm), color = PackDanger)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.generic_cancel)) } }
        )
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun PackRow(pack: ShaderPackInfo, onDelete: () -> Unit) {
    AllListRow(modifier = Modifier.padding(bottom = 6.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(pack.file.nameWithoutExtension, color = PackText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(pack.file.name + " · " + FileUtils.byteCountToDisplaySize(pack.fileSize), color = PackDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        PackButton(stringResource(R.string.generic_delete), onDelete, Color(0xFFF7E9E7), PackDanger, PackDanger)
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = PackText, fontSize = 14.sp),
        cursorBrush = SolidColor(PackAccent),
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(AllColors.Card)
            .border(1.dp, PackBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 13.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(stringResource(R.string.generic_search), color = PackDim, fontSize = 14.sp)
                inner()
            }
        }
    )
}

@Composable
private fun PackButton(
    text: String,
    onClick: () -> Unit,
    background: Color = PackCard,
    contentColor: Color = PackText,
    borderColor: Color = PackBorder
) = AllButton(text, onClick, style = allStyleForColor(contentColor), compact = true)
