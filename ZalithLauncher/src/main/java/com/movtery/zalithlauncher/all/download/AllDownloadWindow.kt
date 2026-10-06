package com.movtery.zalithlauncher.all.download

import com.movtery.zalithlauncher.all.ui.AllButton
import com.movtery.zalithlauncher.all.ui.allStyleForColor
import com.movtery.zalithlauncher.all.ui.AllButtonStyle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import com.movtery.zalithlauncher.all.bridge.AllAssetIcon
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.movtery.zalithlauncher.all.ui.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import com.movtery.zalithlauncher.all.ui.OutlinedTextField
import androidx.compose.material3.Text
import com.movtery.zalithlauncher.all.ui.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.all.bridge.AllSubmitError
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformDependencyType
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersion
import kotlinx.coroutines.flow.StateFlow

private val DownloadSurface = Color(0xFFF3F1E8)
private val DownloadPanel = Color(0xFFFFFFFF)
private val DownloadRail = Color(0xFFD3E5FA)
private val DownloadBorder = Color(0xFFB9B4A2)
private val DownloadText = Color(0xFF1B1B1B)
private val DownloadMuted = Color(0xFF69665D)
private val DownloadAccent = Color(0xFF316AC5)
private val DownloadAccentPale = Color(0xFFE8EDF7)
private val DownloadDanger = Color(0xFFB3261E)

/** ALL-owned download presentation. Search/install behavior is delegated to shared Zalith services/tasks. */
@Composable
fun AllDownloadWindow(
    initialCategory: AllDownloadCategory = AllDownloadCategory.GAME,
    submitError: AllSubmitError,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: AllDownloadViewModel = viewModel(key = "ALL_Download") {
        AllDownloadViewModel(context, submitError)
    }
    val state = viewModel.state
    val categoryState = viewModel.current()
    val keyboard = LocalSoftwareKeyboardController.current
    val backEnabled = state.targetChooserOpen || state.installConfirmationOpen ||
        categoryState.route != AllDownloadRoute.BROWSE

    androidx.compose.runtime.LaunchedEffect(initialCategory) {
        viewModel.selectCategory(initialCategory)
    }

    BackHandler(enabled = backEnabled) { viewModel.back() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DownloadSurface)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(5.dp))
                .background(DownloadPanel)
                .border(1.dp, DownloadBorder, RoundedCornerShape(5.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("⬇  ดาวน์โหลด", color = DownloadText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${categoryTitle(state.category)}  ·  ${state.gameVersionName.ifBlank { "เลือกเวอร์ชันเกม" }}",
                    color = DownloadMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = { viewModel.refreshTargets() }) {
                Text(stringResource(R.string.generic_refresh))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryRail(selected = state.category, onSelect = viewModel::selectCategory)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(5.dp))
                    .background(DownloadPanel)
                    .border(1.dp, DownloadBorder, RoundedCornerShape(5.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (state.busy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = DownloadAccent)
                        Spacer(Modifier.width(8.dp))
                        Text(state.progressMessage ?: "กำลังติดตั้ง…", color = DownloadAccent, maxLines = 1)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = viewModel::cancelInstall) {
                            Text(stringResource(R.string.generic_cancel), color = DownloadDanger)
                        }
                    }
                }
                else state.progressMessage?.takeIf { it.startsWith("Installed ") }?.let { AllDownloadMessage("✓ $it", DownloadAccent) }
                if (state.runningTaskIds.isNotEmpty()) {
                    TaskPanel(state.tasks, state.trackedAssetTaskIds, onCancel = viewModel::cancelTask)
                }
                when {
                    state.category == AllDownloadCategory.GAME && state.selectedGameVersion != null -> GameVersionDetails(
                        version = state.selectedGameVersion!!,
                        targetName = state.gameInstallName,
                        onTargetName = viewModel::updateGameInstallName,
                        onBack = viewModel::back,
                        onInstall = viewModel::installGame,
                        installedVersions = state.installedVersions
                    )
                    state.category == AllDownloadCategory.GAME -> GameBrowser(
                        versions = state.gameVersions,
                        filter = categoryState.query,
                        onFilter = viewModel::updateQuery,
                        onSelect = viewModel::chooseGameVersion
                    )
                    categoryState.route == AllDownloadRoute.PROJECT -> ProjectDetails(
                        category = state.category,
                        project = categoryState.project,
                        versions = categoryState.versions,
                        selectedVersion = categoryState.selectedVersion,
                        targets = state.installedVersions,
                        loading = categoryState.loading,
                        error = categoryState.error,
                        onBack = viewModel::back,
                        onSelectVersion = viewModel::selectVersion,
                        onInstall = {
                            if (state.category == AllDownloadCategory.MODPACK) viewModel.openModpackInstall()
                            else viewModel.openTargetChooser()
                        }
                    )
                    else -> SearchBrowser(
                        category = state.category,
                        categoryState = categoryState,
                        onQuery = viewModel::updateQuery,
                        onPlatform = viewModel::selectPlatform,
                        onSearch = {
                            keyboard?.hide()
                            if (state.category == AllDownloadCategory.BY_ID) viewModel.searchById()
                            else viewModel.search()
                        },
                        onSearchById = viewModel::searchById,
                        onSelect = viewModel::openProject,
                        onNextPage = { viewModel.search(categoryState.pageIndex + 1) },
                        onPreviousPage = { viewModel.search((categoryState.pageIndex - 1).coerceAtLeast(0)) },
                        onSelectIdClass = viewModel::selectIdClass
                    )
                }
            }
        }
    }

    if (state.targetChooserOpen) {
        InstallTargetDialog(
            versions = state.installedVersions,
            selectedNames = state.targetVersionNames,
            onToggle = viewModel::toggleTarget,
            onDismiss = viewModel::closeTargetChooser,
            onConfirm = viewModel::confirmAssetInstall
        )
    }
    if (state.installConfirmationOpen) {
        ModpackInstallDialog(
            name = state.modpackInstallName,
            onName = viewModel::updateModpackName,
            onDismiss = viewModel::closeModpackInstall,
            onConfirm = viewModel::installModpack
        )
    }
    state.installError?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::dismissInstallError,
            title = { Text(stringResource(R.string.generic_error)) },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = viewModel::dismissInstallError) { Text(stringResource(R.string.generic_close)) } }
        )
    }
    if (state.mobileDataConfirmationOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.respondToMobileData(false) },
            title = { Text(stringResource(R.string.download_modpack_install_title)) },
            text = { Text("Modpack install may use mobile data and download many files. Continue?") },
            confirmButton = {
                TextButton(onClick = { viewModel.respondToMobileData(true) }) {
                    Text(stringResource(R.string.generic_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.respondToMobileData(false) }) {
                    Text(stringResource(R.string.generic_cancel))
                }
            }
        )
    }
}

@Composable
private fun CategoryRail(selected: AllDownloadCategory, onSelect: (AllDownloadCategory) -> Unit) {
    Column(
        modifier = Modifier
            .width(142.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(5.dp))
            .background(DownloadRail)
            .border(1.dp, DownloadBorder, RoundedCornerShape(5.dp))
            .verticalScroll(rememberScrollState())
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AllDownloadCategory.entries.forEach { category ->
            val active = category == selected
            Text(
                text = categoryTitle(category),
                color = if (active) Color.White else DownloadText,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (active) DownloadAccent else Color.Transparent)
                    .clickable { onSelect(category) }
                    .padding(horizontal = 9.dp, vertical = 11.dp)
            )
        }
    }
}

@Composable
private fun categoryTitle(category: AllDownloadCategory): String = when (category) {
    AllDownloadCategory.GAME -> "เกม"
    AllDownloadCategory.MODPACK -> "มอดแพ็ก"
    AllDownloadCategory.MOD -> stringResource(R.string.mods_manage)
    AllDownloadCategory.RESOURCE_PACK -> "รีซอร์สแพ็ก"
    AllDownloadCategory.SHADER -> "เชเดอร์แพ็ก"
    AllDownloadCategory.WORLD -> "โลก"
    AllDownloadCategory.BY_ID -> "ค้นหาด้วย ID"
}

@Composable
private fun ColumnScope.SearchBrowser(
    category: AllDownloadCategory,
    categoryState: AllDownloadCategoryState,
    onQuery: (String) -> Unit,
    onPlatform: (Platform) -> Unit,
    onSearch: () -> Unit,
    onSearchById: () -> Unit,
    onSelect: (AllDownloadProject) -> Unit,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    onSelectIdClass: (PlatformClasses) -> Unit
) {
    var providerMenu by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }
    val isById = category == AllDownloadCategory.BY_ID
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = categoryState.query,
            onValueChange = onQuery,
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text(if (isById) "Project ID" else stringResource(R.string.generic_search)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )
        Box {
            AllDownloadButton(PlatformLabel(categoryState.platform), { providerMenu = true })
            DropdownMenu(expanded = providerMenu, onDismissRequest = { providerMenu = false }) {
                Platform.entries.forEach { provider ->
                    DropdownMenuItem(text = { Text(PlatformLabel(provider)) }, onClick = {
                        providerMenu = false
                        onPlatform(provider)
                    })
                }
            }
        }
        if (isById) {
            Box {
                AllDownloadButton(categoryState.idSearchClass.name, { typeMenu = true })
                DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                    listOf(PlatformClasses.MOD, PlatformClasses.MOD_PACK, PlatformClasses.RESOURCE_PACK, PlatformClasses.SHADERS)
                        .forEach { platformClass ->
                            DropdownMenuItem(text = { Text(platformClass.name) }, onClick = {
                                typeMenu = false
                                onSelectIdClass(platformClass)
                            })
                        }
                }
            }
        }
        AllDownloadButton(stringResource(R.string.generic_search), onSearch, enabled = categoryState.query.isNotBlank())
    }

    if (categoryState.error != null) AllDownloadMessage(categoryState.error, DownloadDanger)
    if (categoryState.loading) AllDownloadLoading()

    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(categoryState.projects, key = { "${it.platform.name}/${it.projectId}" }) { item ->
            ProjectCard(item = item, onClick = { onSelect(item) })
        }
        if (categoryState.projects.isEmpty() && !categoryState.loading && categoryState.error == null) {
            item { AllDownloadMessage(if (categoryState.searched) "ไม่พบผลลัพธ์" else "ค้นหาโปรเจกต์จากผู้ให้บริการที่เลือก", DownloadMuted) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AllDownloadButton("‹", onPreviousPage, enabled = categoryState.pageIndex > 0)
                Text(
                    text = "${categoryState.pageIndex + 1} / ${categoryState.totalPages.coerceAtLeast(1)}",
                    color = DownloadMuted,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Spacer(Modifier.weight(1f))
                AllDownloadButton("›", onNextPage, enabled = categoryState.pageIndex + 1 < categoryState.totalPages)
            }
        }
    }
}

@Composable
private fun ProjectCard(item: AllDownloadProject, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(DownloadPanel)
            .border(1.dp, DownloadBorder, RoundedCornerShape(5.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(5.dp)).background(DownloadRail), contentAlignment = Alignment.Center) {
            // Zalith's Coil-backed icon (cache, shimmer, error fallback); letter only when the provider gave no URL
            if (item.iconUrl.isNullOrBlank()) Text(item.title.take(1).uppercase(), color = DownloadAccent, fontWeight = FontWeight.Bold)
            else AllAssetIcon(size = 48.dp, iconUrl = item.iconUrl)
        }
        Column(Modifier.weight(1f)) {
            Text(item.title, color = DownloadText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.description, color = DownloadMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${PlatformLabel(item.platform)} · ${item.author} · ${item.downloads}", color = DownloadAccent, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ColumnScope.ProjectDetails(
    category: AllDownloadCategory,
    project: com.movtery.zalithlauncher.game.download.assets.platform.PlatformProject?,
    versions: List<PlatformVersion>,
    selectedVersion: PlatformVersion?,
    targets: List<Version>,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSelectVersion: (PlatformVersion) -> Unit,
    onInstall: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AllDownloadButton(stringResource(R.string.generic_back), onBack)
        project?.platformIconUrl()?.takeIf { it.isNotBlank() }?.let {
            AllAssetIcon(modifier = Modifier.clip(RoundedCornerShape(5.dp)), size = 36.dp, iconUrl = it)
        }
        Text(project?.platformTitle().orEmpty(), color = DownloadText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f))
        Text(project?.platform()?.name.orEmpty(), color = DownloadAccent, fontSize = 12.sp)
    }
    if (error != null) AllDownloadMessage(error, DownloadDanger)
    if (loading) AllDownloadLoading()
    project?.let {
        Text(it.platformSummary().orEmpty(), color = DownloadMuted, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Text("${it.platformAuthor().orEmpty()} · ${it.platformDownloadCount()} downloads", color = DownloadMuted, fontSize = 11.sp)
    }
    if (versions.isEmpty() && !loading) {
        AllDownloadMessage("ไม่มีเวอร์ชันที่เข้ากันได้สำหรับรายการนี้", DownloadMuted)
    }
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        items(versions.take(80), key = { "${it.platform().name}/${it.platformId()}" }) { fileVersion ->
            val selected = fileVersion.platformId() == selectedVersion?.platformId()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (selected) DownloadAccentPale else DownloadPanel)
                    .border(1.dp, if (selected) DownloadAccent else DownloadBorder, RoundedCornerShape(5.dp))
                    .clickable { onSelectVersion(fileVersion) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(fileVersion.platformDisplayName(), color = DownloadText, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        text = "${fileVersion.platformVersion()} · ${fileVersion.platformReleaseType()} · ${fileVersion.platformGameVersion().joinToString()}",
                        color = DownloadMuted,
                        fontSize = 11.sp,
                        maxLines = 2
                    )
                    val loaders = fileVersion.platformLoaders().joinToString { it.toString() }
                    if (loaders.isNotBlank()) Text(loaders, color = DownloadAccent, fontSize = 11.sp)
                    val required = fileVersion.platformDependencies().count { it.type.name.equals("REQUIRED", true) }
                    if (required > 0) Text("Required dependencies: $required", color = DownloadMuted, fontSize = 11.sp)
                }
                Text(PlatformLabel(fileVersion.platform()), color = DownloadAccent, fontSize = 11.sp)
            }
        }
    }
    selectedVersion?.let { selected ->
        SettingActionCard(
            title = if (category == AllDownloadCategory.MODPACK) "Install as a new instance" else "Install to selected instance(s)",
            detail = "${selected.platformFileName()} · ${targets.size} installed target(s)",
            action = onInstall
        )
    }
}

@Composable
private fun ColumnScope.GameBrowser(
    versions: List<MinecraftVersion>,
    filter: String,
    onFilter: (String) -> Unit,
    onSelect: (MinecraftVersion) -> Unit
) {
    OutlinedTextField(
        value = filter,
        onValueChange = onFilter,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Minecraft version") },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
    )
    val matches = versions.asSequence()
        .filter { filter.isBlank() || it.version.id.contains(filter.trim(), ignoreCase = true) }
        .take(120)
        .toList()
    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(matches, key = { it.version.id }) { version ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(DownloadPanel)
                    .border(1.dp, DownloadBorder, RoundedCornerShape(5.dp))
                    .clickable { onSelect(version) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(version.version.id, color = DownloadText, fontWeight = FontWeight.Bold)
                    Text(version.type.toString(), color = DownloadMuted, fontSize = 11.sp)
                }
                Text("Install", color = DownloadAccent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GameVersionDetails(
    version: MinecraftVersion,
    targetName: String,
    onTargetName: (String) -> Unit,
    onBack: () -> Unit,
    onInstall: () -> Unit,
    installedVersions: List<Version>
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AllDownloadButton(stringResource(R.string.generic_back), onBack)
        Text(version.version.id, color = DownloadText, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(version.type.toString(), color = DownloadAccent)
    }
    AllDownloadMessage("Install official Minecraft version to the launcher game directory.", DownloadMuted)
    if (installedVersions.any { it.getVersionName() == targetName }) {
        AllDownloadMessage("This version name is already installed.", DownloadDanger)
    }
    OutlinedTextField(
        value = targetName,
        onValueChange = onTargetName,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Installed version name") }
    )
    AllDownloadButton(
        text = "Install game version",
        onClick = onInstall,
        enabled = targetName.isNotBlank() && installedVersions.none { it.getVersionName() == targetName }
    )
}

@Composable
private fun InstallTargetDialog(
    versions: List<Version>,
    selectedNames: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose install target") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                versions.forEach { version ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onToggle(version.getVersionName()) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = version.getVersionName() in selectedNames, onCheckedChange = { onToggle(version.getVersionName()) })
                        Column {
                            Text(version.getVersionName(), color = DownloadText)
                            Text(version.getVersionInfo()?.loaderInfo?.loader?.displayName.orEmpty(), color = DownloadMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm, enabled = selectedNames.isNotEmpty()) { Text("Install") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.generic_cancel)) } }
    )
}

@Composable
private fun ModpackInstallDialog(
    name: String,
    onName: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Install modpack") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.download_modpack_warning1), color = DownloadMuted, fontSize = 12.sp)
                OutlinedTextField(value = name, onValueChange = onName, singleLine = true, label = { Text("New instance name") })
            }
        },
        confirmButton = { TextButton(onClick = onConfirm, enabled = name.isNotBlank()) { Text("Install") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.generic_cancel)) } }
    )
}

@Composable
private fun SettingActionCard(title: String, detail: String, action: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(5.dp))
            .background(DownloadAccentPale)
            .border(1.dp, DownloadAccent, RoundedCornerShape(5.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = DownloadText, fontWeight = FontWeight.Bold)
            Text(detail, color = DownloadMuted, fontSize = 11.sp)
        }
        AllDownloadButton(stringResource(R.string.generic_download), action)
    }
}

@Composable
private fun TaskPanel(tasks: List<Task>, tracked: Set<String>, onCancel: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 150.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFFEAF2FA))
            .border(1.dp, DownloadBorder, RoundedCornerShape(5.dp))
            .padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Background tasks · ${tasks.size}", color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            tasks.takeLast(4).forEach { task ->
                val progress by task.progress.collectAsState()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.id, color = DownloadMuted, fontSize = 10.sp, maxLines = 1, modifier = Modifier.weight(1f))
                    if (task.id in tracked) {
                        TextButton(onClick = { onCancel(task.id) }) {
                            Text(stringResource(R.string.generic_cancel), color = DownloadDanger, fontSize = 11.sp)
                        }
                    }
                }
                if (progress >= 0f) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = DownloadAccent
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = DownloadAccent)
                }
            }
        }
    }
}

@Composable
private fun AllDownloadButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) = AllButton(text, onClick, style = AllButtonStyle.Primary, compact = true, enabled = enabled)

@Composable
private fun AllDownloadMessage(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(5.dp))
            .background(DownloadSurface)
            .padding(10.dp)
    )
}

@Composable
private fun AllDownloadLoading() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = DownloadAccent)
        Text(stringResource(R.string.generic_loading), color = DownloadMuted)
    }
}

private fun PlatformLabel(platform: Platform): String = when (platform) {
    Platform.MODRINTH -> "Modrinth"
    Platform.CURSEFORGE -> "CurseForge"
}
