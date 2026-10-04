package com.movtery.zalithlauncher.all.mods

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import com.movtery.zalithlauncher.all.ui.AlertDialog
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.bridge.AllSubmitError
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import org.apache.commons.io.FileUtils

/**
 * ALL 自己的模组窗口内容（Computer Theme）。
 *
 * 这一层完全是 ALL 的界面：不嵌入、也不包一层 Zalith 的模组管理页。
 * 数据与操作走 [AllModsViewModel] → [AllModsService] → Zalith 既有的模组读取与
 * .disabled 启用/禁用语义；导入走 Zalith 既有的导入任务（见 [rememberAllModsImportAction]）。
 *
 * 外壳（标题栏、拖动、最小化、焦点、UI 缩放）仍由 AllDesktopScreen 提供，本文件只管内容区，
 * 因此以后要换视觉只要改这里。
 */

private val AllModsSurface = Color(0xFFF3F1E8)
private val AllModsCard = Color(0xFFFFFFFF)
private val AllModsCardOff = Color(0xFFEDEBE3)
private val AllModsBorder = Color(0xFFB9B4A2)
private val AllModsText = Color(0xFF1B1B1B)
private val AllModsDim = Color(0xFF6B675C)
private val AllModsAccent = Color(0xFF316AC5)
private val AllModsOn = Color(0xFF2E7D32)
private val AllModsOff = Color(0xFF9A6B00)
private val AllModsDanger = Color(0xFFB3261E)
private val AllModsWarnBg = Color(0xFFFFF3D6)
private val AllModsWarnText = Color(0xFF7A4B00)

@Composable
fun AllModsWindow(
    version: Version,
    onOpenDownload: () -> Unit,
    onError: AllSubmitError,
    modifier: Modifier = Modifier
) {
    val modsDir = remember(version) { VersionFolders.MOD.getDir(version.getGameDir()) }
    val hasModLoader = remember(version) {
        version.getVersionInfo()?.loaderInfo?.loader?.isLoader == true
    }
    //状态放 ViewModel：最小化会把内容移出组合，放 remember 里每次还原都要重读所有 jar
    val viewModel: AllModsViewModel = viewModel(key = "ALL_Mods_" + version.toString()) {
        AllModsViewModel(modsDir)
    }

    //窗口每次打开/还原时对一次文件数量，只有真的变了才重读
    LaunchedEffect(Unit) { viewModel.loadIfChanged() }

    var pendingDelete by remember { mutableStateOf<AllModItem?>(null) }
    val importMods = rememberAllModsImportAction(
        modsDir = modsDir,
        onError = onError,
        onImported = { viewModel.load() }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AllModsSurface)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 8.dp)
        ) {
            AllModsSearchField(
                value = viewModel.filter,
                onValueChange = { viewModel.filter = it },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            AllModsButton(
                text = stringResource(R.string.generic_refresh),
                onClick = { viewModel.load() }
            )
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
            AllModsButton(
                text = stringResource(R.string.generic_download),
                onClick = onOpenDownload,
                background = Color(0xFFE8EDF7),
                contentColor = AllModsAccent,
                borderColor = AllModsAccent
            )
            AllModsButton(
                text = stringResource(R.string.generic_import),
                onClick = importMods
            )
            AllModsCountChip(
                text = stringResource(R.string.generic_enable) + " " + viewModel.enabledCount,
                color = AllModsOn
            )
            AllModsCountChip(
                text = stringResource(R.string.generic_disable) + " " + viewModel.disabledCount,
                color = AllModsOff
            )
        }

        if (!hasModLoader) {
            AllModsNotice(text = stringResource(R.string.mods_manage_no_loader))
        }

        when {
            viewModel.loading && viewModel.items.isEmpty() -> AllModsLoading()

            viewModel.visibleItems.isEmpty() -> AllModsEmpty()

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
                    .padding(top = 6.dp)
            ) {
                items(viewModel.visibleItems, key = { it.fileName }) { item ->
                    AllModRow(
                        item = item,
                        onToggle = { viewModel.toggle(item) },
                        onDelete = { pendingDelete = item }
                    )
                }
            }
        }
    }

    pendingDelete?.let { item ->
        AllModDeleteDialog(
            item = item,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                viewModel.delete(item)
                pendingDelete = null
            }
        )
    }
}

@Composable
private fun AllModRow(
    item: AllModItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (item.enabled) AllModsCard else AllModsCardOff)
            .border(1.dp, AllModsBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        AllModStateDot(enabled = item.enabled)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = AllModsText,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.fileName,
                color = AllModsDim,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val meta = AllModMetaLine(item)
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    color = AllModsDim,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        AllModsButton(
            text = stringResource(if (item.enabled) R.string.generic_disable else R.string.generic_enable),
            onClick = onToggle,
            background = if (item.enabled) Color(0xFFE8EDF7) else Color(0xFFE7F3E7),
            contentColor = if (item.enabled) AllModsAccent else AllModsOn,
            borderColor = if (item.enabled) AllModsAccent else AllModsOn
        )
        Spacer(Modifier.width(6.dp))
        AllModsButton(
            text = stringResource(R.string.generic_delete),
            onClick = onDelete,
            background = Color(0xFFF7E9E7),
            contentColor = AllModsDanger,
            borderColor = AllModsDanger
        )
    }
}

/** 模组版本 / 加载器 / 大小，空的项直接省略 */
@Composable
private fun AllModMetaLine(item: AllModItem): String = listOfNotNull(
    item.version?.takeIf { it.isNotBlank() }
        ?.let { stringResource(R.string.mods_manage_version, it) },
    item.loaderName.takeIf { it.isNotBlank() },
    FileUtils.byteCountToDisplaySize(item.sizeBytes)
).joinToString(" · ")

/** 启用=绿点，禁用=橙点，一眼能分辨，不依赖文字 */
@Composable
private fun AllModStateDot(enabled: Boolean) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(if (enabled) AllModsOn else AllModsOff)
    )
}

@Composable
private fun AllModsSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = AllModsText, fontSize = 14.sp),
        cursorBrush = SolidColor(AllModsAccent),
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White)
            .border(1.dp, AllModsBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 13.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.generic_search),
                        color = AllModsDim,
                        fontSize = 14.sp
                    )
                }
                inner()
            }
        }
    )
}

@Composable
private fun AllModsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = AllModsCard,
    contentColor: Color = AllModsText,
    borderColor: Color = AllModsBorder
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun AllModsCountChip(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AllModsCard)
            .border(1.dp, color, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 12.dp)
    )
}

@Composable
private fun AllModsNotice(text: String) {
    Text(
        text = text,
        color = AllModsWarnText,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(AllModsWarnBg)
            .padding(10.dp)
    )
}

@Composable
private fun AllModsLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AllModsAccent)
            Spacer(Modifier.height(10.dp))
            Text(text = stringResource(R.string.generic_loading), color = AllModsDim)
        }
    }
}

@Composable
private fun AllModsEmpty() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = stringResource(R.string.mods_manage_no_mods), color = AllModsDim)
    }
}

@Composable
private fun AllModDeleteDialog(
    item: AllModItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.generic_delete)) },
        text = { Text(text = stringResource(R.string.mods_manage_delete_warning, item.name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.generic_confirm), color = AllModsDanger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.generic_cancel))
            }
        }
    )
}
