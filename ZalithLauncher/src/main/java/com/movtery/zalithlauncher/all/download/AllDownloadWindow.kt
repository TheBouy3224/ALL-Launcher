package com.movtery.zalithlauncher.all.download

import com.movtery.zalithlauncher.all.ui.AllButton
import com.movtery.zalithlauncher.all.ui.allStyleForColor
import com.movtery.zalithlauncher.all.ui.AllButtonStyle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.movtery.zalithlauncher.all.ui.AlertDialog
import androidx.compose.material3.Button
import com.movtery.zalithlauncher.all.ui.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import com.movtery.zalithlauncher.all.ui.DropdownMenu
import com.movtery.zalithlauncher.all.ui.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import com.movtery.zalithlauncher.all.ui.OutlinedTextField
import androidx.compose.material3.Text
import com.movtery.zalithlauncher.all.ui.TextButton
import com.movtery.zalithlauncher.all.ui.AllListRow
import com.movtery.zalithlauncher.all.ui.LocalAllCompact
import androidx.compose.runtime.Composable
import com.movtery.zalithlauncher.ui.toAndroidString
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

/** ALL-owned download presentation (Download V5, XP Luna prototype). Search/install behavior is delegated to shared Zalith services/tasks. */
@Composable
fun AllDownloadWindow(
    initialCategory: AllDownloadCategory = AllDownloadCategory.GAME,
    session: Int = 0,
    targetVersionName: String? = null,
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
    val backEnabled = state.installConfirmationOpen || state.assetConfirmOpen || state.gameInstallConfirmOpen ||
        (state.category != AllDownloadCategory.GAME && categoryState.route != AllDownloadRoute.BROWSE)

    //关闭后重开（session 变了）清掉旧搜索并加载推荐；最小化/还原保持原状态
    androidx.compose.runtime.LaunchedEffect(initialCategory, session) {
        viewModel.openWindow(initialCategory, session)
    }
    androidx.compose.runtime.LaunchedEffect(targetVersionName) {
        viewModel.setPreferredTarget(targetVersionName)
    }
    //the game page always shows a Minecraft version: the newest release is picked first (the user changes it in the dropdown)
    androidx.compose.runtime.LaunchedEffect(state.category, state.selectedGameVersion == null, state.gameVersions.size) {
        if (state.category == AllDownloadCategory.GAME && state.selectedGameVersion == null) {
            state.gameVersions.firstOrNull { it.type == MinecraftVersion.Type.Release }?.let(viewModel::chooseGameVersion)
        }
    }

    BackHandler(enabled = backEnabled) { viewModel.back() }

    BoxWithConstraints(modifier.fillMaxSize().background(DownloadSurface).padding(6.dp)) {
        val iconRail = maxWidth < 720.dp
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CategoryRail(selected = state.category, onSelect = viewModel::selectCategory, iconOnly = iconRail, onRefresh = { viewModel.refreshTargets() })
            //workspace: header -> content -> fixed footer (prototype .workspace)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(7.dp))
                    .background(DownloadPanel)
                    .border(1.dp, DownloadBorder, RoundedCornerShape(7.dp))
            ) {
                if (state.busy) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = DownloadAccent)
                        Spacer(Modifier.width(8.dp))
                        Text(state.progressMessage ?: stringResource(R.string.all_dl_installing), color = DownloadAccent, maxLines = 1, modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::cancelInstall) { Text(stringResource(R.string.generic_cancel), color = DownloadDanger) }
                    }
                } else state.progressMessage?.takeIf { it.startsWith("Installed ") }?.let {
                    Text("✓ $it", color = DownloadAccent, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
                if (state.busy && state.installerTasks.isNotEmpty()) Box(Modifier.padding(horizontal = 8.dp)) { InstallerProgress(state.installerTasks) }
                if (state.runningTaskIds.isNotEmpty()) Box(Modifier.padding(horizontal = 8.dp)) { TaskPanel(state.tasks, state.trackedAssetTaskIds, onCancel = viewModel::cancelTask) }

                if (state.category == AllDownloadCategory.GAME) {
                    GamePage(state, viewModel)
                } else {
                    AssetPage(state, categoryState, viewModel, onSearch = {
                        keyboard?.hide()
                        if (state.category == AllDownloadCategory.BY_ID) viewModel.searchById() else viewModel.search()
                    })
                }
            }
        }
    }

    if (state.gameInstallConfirmOpen) {
        SummaryDialog(
            title = stringResource(R.string.all_dl_confirm_game_title),
            rows = listOfNotNull(
                stringResource(R.string.all_dl_plan_target) to state.gameInstallName.trim() + "  (" + stringResource(R.string.all_dl_plan_new_instance) + ")",
                "Minecraft" to (state.selectedGameVersion?.version?.id.orEmpty() + "  " + state.selectedGameVersion?.type?.toString().orEmpty()),
                stringResource(R.string.all_dl_loader) to state.gameLoader.title,
                stringResource(R.string.all_dl_loader_version) to (if (state.gameLoader == AllGameLoader.VANILLA) stringResource(R.string.all_dl_not_needed) else state.selectedLoaderVersion?.getAddonVersion().orEmpty()),
                ("Fabric API" to (state.loaderApiVersion?.getAddonVersion() ?: stringResource(R.string.all_dl_bundle_none))).takeIf { state.gameLoader == AllGameLoader.FABRIC },
                ("Mod Menu" to (state.modMenuVersion?.platformVersion() ?: stringResource(R.string.all_dl_bundle_none))).takeIf { state.gameLoader == AllGameLoader.FABRIC }
            ),
            warning = state.selectedLoaderVersion?.takeIf { state.gameLoader != AllGameLoader.VANILLA && AllLoaderCatalog.isPreRelease(it) }?.let { stringResource(R.string.all_dl_plan_prerelease) },
            note = stringResource(R.string.all_dl_plan_note),
            onBack = viewModel::cancelGameInstallConfirm,
            onConfirm = viewModel::installGame
        )
    }
    if (state.assetConfirmOpen) {
        categoryState.selectedVersion?.let { v ->
            val targets = state.installedVersions.filter { it.getVersionName() in state.targetVersionNames }
            val required = v.platformDependencies().count { it.type.name.equals("REQUIRED", true) }
            SummaryDialog(
                title = stringResource(R.string.all_dl_confirm_asset_title, categoryTitle(state.category)),
                rows = listOfNotNull(
                    stringResource(R.string.all_dl_plan_project) to categoryState.project?.platformTitle().orEmpty(),
                    stringResource(R.string.all_dl_plan_chosen_version) to v.platformDisplayName(),
                    stringResource(R.string.all_dl_plan_file_name) to v.platformFileName(),
                    "Minecraft" to v.platformGameVersion().joinToString(),
                    v.platformLoaders().joinToString { it.toString() }.takeIf { it.isNotBlank() }?.let { stringResource(R.string.all_dl_loader) to it },
                    stringResource(R.string.all_dl_plan_release) to v.platformReleaseType().toString(),
                    stringResource(R.string.all_dl_plan_source) to PlatformLabel(v.platform()),
                    stringResource(R.string.all_dl_plan_target) to targets.joinToString("\n") { targetLabel(it) }
                ),
                warning = if (required > 0) stringResource(R.string.all_dl_required_deps, required) else null,
                note = null,
                onBack = viewModel::cancelAssetInstall,
                onConfirm = viewModel::confirmAssetInstall
            )
        }
    }
    if (state.installConfirmationOpen) {
        ModpackInstallDialog(
            version = categoryState.selectedVersion,
            project = categoryState.project?.platformTitle().orEmpty(),
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
            text = { Text(stringResource(R.string.all_dl_mobile_data)) },
            confirmButton = { TextButton(onClick = { viewModel.respondToMobileData(true) }) { Text(stringResource(R.string.generic_confirm)) } },
            dismissButton = { TextButton(onClick = { viewModel.respondToMobileData(false) }) { Text(stringResource(R.string.generic_cancel)) } }
        )
    }
}

/** Prototype .work-top: page title + the steps in one line */
@Composable
private fun PageHeader(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(title, color = DownloadText, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = DownloadMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5E7EE)))
}

/** Prototype .work-footer: what will happen (2 lines) + a way back + "Review before install", never scrolled away */
@Composable
private fun PageFooter(summary: String, detail: String, secondary: String, onSecondary: () -> Unit, secondaryEnabled: Boolean, primaryEnabled: Boolean, onPrimary: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFFF6F9FF), Color(0xFFE6EEFB))))
            .border(1.dp, Color(0xFFB8C3D4))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(summary, color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, color = DownloadMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AllButton(secondary, onSecondary, compact = true, enabled = secondaryEnabled)
        AllButton(stringResource(R.string.all_dl_review), onPrimary, style = AllButtonStyle.Primary, compact = true, enabled = primaryEnabled)
    }
}

@Composable
private fun StepTitle(n: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(Color(0xFF2564C6)), contentAlignment = Alignment.Center) {
            Text("$n", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Text(text, color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Pill(text: String, fg: Color, bg: Color) {
    Text(
        text, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(bg).padding(horizontal = 6.dp, vertical = 1.dp)
    )
}

private val PillGreen = Color(0xFF286C26) to Color(0xFFE5F6DF)
private val PillOrange = Color(0xFF986213) to Color(0xFFFFF0DA)
private val PillRed = Color(0xFFA23131) to Color(0xFFFFF0F0)
private val PillGrey = Color(0xFF667080) to Color(0xFFF2F3F5)

@Composable
private fun RadioDot(on: Boolean) {
    Box(
        Modifier.size(18.dp).clip(CircleShape).border(2.dp, if (on) Color(0xFF2163BC) else Color(0xFF8A9CB5), CircleShape),
        contentAlignment = Alignment.Center
    ) { if (on) Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2163BC))) }
}

private fun loaderIcon(loader: AllGameLoader): Int = when (loader) {
    AllGameLoader.VANILLA -> R.drawable.img_old_grass_block
    AllGameLoader.FABRIC -> R.drawable.img_loader_fabric
    AllGameLoader.QUILT -> R.drawable.img_loader_quilt
    AllGameLoader.NEOFORGE -> R.drawable.img_loader_neoforge
    AllGameLoader.FORGE -> R.drawable.img_anvil
}

/** Light XP panel used by the game page (owner's final mockup) */
@Composable
private fun GamePanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(7.dp)).background(Color(0xFFF8F7F2)).border(1.dp, Color(0xFFCFCCC0), RoundedCornerShape(7.dp)).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp), content = content
    )
}

@Composable
private fun PanelTitle(icon: Int?, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        icon?.let { androidx.compose.foundation.Image(painterResource(it), null, Modifier.size(20.dp)) }
        Text(text, color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Download > Game, owner's final mockup: header (title, instance name, Clear, Review & install), Minecraft version + loader row,
 * loader version list + extra options. Fabric bundles Fabric API + Mod Menu (versions that really fit, looked up live); no other
 * loader gets extra mods. No bottom bar - only the ALL taskbar below.
 */
@Composable
private fun ColumnScope.GamePage(state: AllDownloadState, viewModel: AllDownloadViewModel) {
    val version = state.selectedGameVersion
    val loader = state.gameLoader
    val nameTaken = state.installedVersions.any { it.getVersionName() == state.gameInstallName }
    val fabric = loader == AllGameLoader.FABRIC
    val bundleReady = !fabric || (!state.loaderLoading && !state.modMenuLoading)
    val ready = !state.busy && version != null && state.gameInstallName.isNotBlank() && !nameTaken && bundleReady &&
        (loader == AllGameLoader.VANILLA || state.selectedLoaderVersion != null)

    //header
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.foundation.Image(painterResource(R.drawable.img_old_grass_block), null, Modifier.size(40.dp))
        Column(Modifier.weight(0.9f)) {
            Text(stringResource(R.string.all_dl_game_title), color = DownloadText, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, maxLines = 1)
            Text(stringResource(R.string.all_dl_game_subtitle2), color = DownloadMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.all_dl_install_name), color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color.White)
                    .border(1.dp, if (nameTaken) DownloadDanger else Color(0xFF9DB5D4), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = state.gameInstallName, onValueChange = viewModel::updateGameInstallName, singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = DownloadText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f)
                )
                if (state.gameInstallName.isNotEmpty()) Text("⊗", color = DownloadMuted, fontSize = 15.sp, modifier = Modifier.clickable { viewModel.updateGameInstallName("") }.padding(start = 6.dp))
            }
            Text(
                if (nameTaken) stringResource(R.string.all_dl_already_installed) else stringResource(R.string.all_dl_name_example),
                color = if (nameTaken) DownloadDanger else DownloadMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        AllButton(stringResource(R.string.all_dl_clear_short), { viewModel.selectGameLoader(AllGameLoader.VANILLA) }, enabled = loader != AllGameLoader.VANILLA)
        AllButton(stringResource(R.string.all_dl_review_install), viewModel::requestGameInstall, style = AllButtonStyle.Success, enabled = ready)
    }

    Column(Modifier.weight(1f).fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        //Minecraft version + loader
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GamePanel(Modifier.weight(0.75f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.foundation.Image(painterResource(R.drawable.img_old_grass_block), null, Modifier.size(38.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(stringResource(R.string.all_dl_mc_version), color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        var menu by remember { mutableStateOf(false) }
                        var includeTest by remember { mutableStateOf(false) }
                        Box {
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color.White).border(1.dp, Color(0xFF9DB5D4), RoundedCornerShape(6.dp))
                                    .clickable { menu = true }.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(version?.version?.id ?: stringResource(R.string.generic_loading), color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                Text("▾", color = DownloadText, fontWeight = FontWeight.Bold)
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text((if (includeTest) "☑ " else "☐ ") + stringResource(R.string.all_dl_show_test_versions)) }, onClick = { includeTest = !includeTest })
                                state.gameVersions.asSequence().filter { it.type in visibleGameTypes(includeTest) }.take(150).forEach { v ->
                                    DropdownMenuItem(text = { Text("${v.version.id}  ·  ${v.type}") }, onClick = { menu = false; viewModel.chooseGameVersion(v) })
                                }
                            }
                        }
                    }
                }
            }
            GamePanel(Modifier.weight(1.6f)) {
                PanelTitle(R.drawable.img_old_grass_block, stringResource(R.string.all_dl_loader))
                val offered = state.supportedLoaders ?: listOf(AllGameLoader.VANILLA)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    AllGameLoader.entries.forEach { option ->
                        val on = loader == option
                        val enabled = option in offered
                        Row(
                            Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                                .background(if (on) Color(0xFFDDEAFE) else if (enabled) Color.White else Color(0xFFEFEFEC))
                                .border(if (on) 2.dp else 1.dp, if (on) Color(0xFF3A80DC) else Color(0xFFB7BCC6), RoundedCornerShape(6.dp))
                                .clickable(enabled = enabled) { viewModel.selectGameLoader(option) }
                                .padding(horizontal = 4.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
                        ) {
                            androidx.compose.foundation.Image(painterResource(loaderIcon(option)), null, Modifier.size(18.dp), alpha = if (enabled) 1f else 0.35f)
                            Spacer(Modifier.width(5.dp))
                            Text(option.title, color = if (on) Color(0xFF1758AF) else if (enabled) DownloadText else DownloadMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
                if (state.supportedLoaders == null) Text(stringResource(R.string.all_dl_loader_probing), color = DownloadMuted, fontSize = 10.sp)
            }
        }
        //loader version list + extra options
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GamePanel(Modifier.weight(1.3f).fillMaxHeight()) {
                PanelTitle(loaderIcon(loader), if (loader == AllGameLoader.VANILLA) "Vanilla" else stringResource(R.string.all_dl_loader_versions_title, loader.title))
                val list = state.loaderVersions
                Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(5.dp)).background(Color.White).border(1.dp, Color(0xFFD5DCE7), RoundedCornerShape(5.dp))) {
                    when {
                        loader == AllGameLoader.VANILLA -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("✓", color = Color(0xFF2E7D32), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.all_dl_vanilla_hint), color = DownloadMuted, fontSize = 12.sp)
                        }
                        list == null -> AllDownloadLoading()
                        else -> {
                            val (stable, unstable) = remember(list) { AllLoaderCatalog.splitStable(list) }
                            LazyColumn(Modifier.fillMaxSize()) {
                                items(stable + unstable, key = { it.getAddonVersion() }) { lv ->
                                    val chosen = state.selectedLoaderVersion?.getAddonVersion() == lv.getAddonVersion()
                                    val isStable = lv in stable
                                    Row(
                                        Modifier.fillMaxWidth().background(if (chosen) Color(0xFFDDEAFE) else Color.White)
                                            .clickable { viewModel.selectLoaderVersion(lv) }.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        RadioDot(chosen)
                                        Text(lv.getAddonVersion(), color = DownloadText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        val (fg, bg) = if (isStable) PillGreen else PillOrange
                                        Pill(if (isStable) "Stable" else "Beta", fg, bg)
                                    }
                                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEDF0F5)))
                                }
                            }
                        }
                    }
                }
            }
            GamePanel(Modifier.weight(1f).fillMaxHeight()) {
                PanelTitle(null, "⚙  " + stringResource(R.string.all_dl_step_options))
                if (fabric) {
                    Text(stringResource(R.string.all_dl_fabric_bundle_hint), color = DownloadMuted, fontSize = 11.sp)
                    BundleRow(
                        R.drawable.img_loader_fabric, "Fabric API",
                        when {
                            state.loaderLoading -> stringResource(R.string.all_dl_checking_version)
                            state.loaderApiVersion != null -> state.loaderApiVersion.getAddonVersion()
                            else -> null
                        }
                    )
                    BundleRow(
                        R.drawable.img_old_grass_block, "Mod Menu",
                        when {
                            state.modMenuLoading -> stringResource(R.string.all_dl_checking_version)
                            state.modMenuVersion != null -> state.modMenuVersion.platformVersion()
                            else -> null
                        }
                    )
                } else {
                    Text(
                        stringResource(if (loader == AllGameLoader.VANILLA) R.string.all_dl_vanilla_no_mods else R.string.all_dl_no_auto_mods, loader.title),
                        color = DownloadMuted, fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/** One bundled mod: name and the version that will be installed; red text when no fitting version exists (it is skipped) */
@Composable
private fun BundleRow(icon: Int, name: String, versionText: String?) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(Color.White).border(1.dp, Color(0xFFC9D6EA), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.foundation.Image(painterResource(icon), null, Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(versionText ?: stringResource(R.string.all_dl_bundle_none), color = if (versionText != null) DownloadMuted else DownloadDanger, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (versionText != null) Pill(stringResource(R.string.all_dl_bundle_included), PillGreen.first, PillGreen.second)
    }
}

private fun categoryIllustration(category: AllDownloadCategory): Int = when (category) {
    AllDownloadCategory.MODPACK -> R.drawable.img_old_grass_block
    AllDownloadCategory.MOD -> R.drawable.img_old_grass_block
    AllDownloadCategory.RESOURCE_PACK -> R.drawable.img_old_grass_block
    AllDownloadCategory.SHADER -> R.drawable.img_old_grass_block
    AllDownloadCategory.WORLD -> R.drawable.img_old_grass_block
    else -> R.drawable.img_old_grass_block
}

/** "recommended" = the newest release that fits (marked only - the user still picks the row) */
private fun recommendedOf(versions: List<PlatformVersion>, ok: (PlatformVersion) -> Boolean): PlatformVersion? =
    versions.firstOrNull { ok(it) && it.platformReleaseType().toString().equals("release", true) } ?: versions.firstOrNull(ok)

/**
 * Mods / modpacks / resource packs / shaders / worlds / by ID (owner's "perfect" mods mockup): header with the main button top right,
 * search + results on the left, a simple install pane on the right (provider, project, target, versions). The user always picks the
 * version; nothing is pre-selected. No bottom bar - only the ALL taskbar below.
 */
@Composable
private fun ColumnScope.AssetPage(state: AllDownloadState, cs: AllDownloadCategoryState, viewModel: AllDownloadViewModel, onSearch: () -> Unit) {
    val category = state.category
    val modpack = category == AllDownloadCategory.MODPACK
    val isById = category == AllDownloadCategory.BY_ID
    val targets = state.installedVersions.filter { it.getVersionName() in state.targetVersionNames }
    val v = cs.selectedVersion
    val canInstall = v != null && viewModel.isCompatible(v) && !state.busy && (modpack || targets.isNotEmpty())

    //header: title + main button (top right)
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.foundation.Image(painterResource(categoryIllustration(category)), null, Modifier.size(38.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (isById) stringResource(R.string.all_dl_by_id_title) else stringResource(R.string.all_dl_install_cat, categoryTitle(category)),
                color = DownloadText, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, maxLines = 1
            )
            Text(
                v?.let { stringResource(R.string.all_dl_chosen_line, cs.project?.platformTitle().orEmpty(), it.platformDisplayName()) }
                    ?: stringResource(R.string.all_dl_asset_subtitle2),
                color = if (v != null) DownloadAccent else DownloadMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        AllButton(
            when {
                modpack -> stringResource(R.string.all_dl_install_as_new)
                isById -> stringResource(R.string.all_install)
                else -> stringResource(R.string.all_dl_install_cat_short, categoryTitle(category))
            },
            { if (modpack) viewModel.openModpackInstall() else viewModel.requestAssetInstall() },
            style = AllButtonStyle.Success, enabled = canInstall
        )
    }

    Row(Modifier.weight(1f).fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        //left: search + results
        Column(Modifier.weight(1.05f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp)).background(Color.White).border(1.dp, Color(0xFF9DB5D4), RoundedCornerShape(7.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(painterResource(R.drawable.ic_search), null, tint = DownloadAccent, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f)) {
                    if (cs.query.isEmpty()) Text(stringResource(if (isById) R.string.all_dl_id_hint else R.string.all_dl_search_hint_short), color = DownloadMuted, fontSize = 14.sp, maxLines = 1)
                    androidx.compose.foundation.text.BasicTextField(
                        value = cs.query, onValueChange = viewModel::updateQuery, singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = DownloadText, fontSize = 14.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (cs.query.isNotEmpty()) Text("✕", color = DownloadMuted, fontSize = 15.sp, modifier = Modifier.clickable { viewModel.updateQuery("") }.padding(horizontal = 2.dp))
                if (isById) MenuButton(cs.idSearchClass.name, listOf(PlatformClasses.MOD, PlatformClasses.MOD_PACK, PlatformClasses.RESOURCE_PACK, PlatformClasses.SHADERS).map { it.name to { viewModel.selectIdClass(it) } })
                AllButton(stringResource(R.string.generic_search), onSearch, compact = true, style = AllButtonStyle.Primary, enabled = cs.query.isNotBlank())
            }
            Column(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(7.dp)).background(Color.White).border(1.dp, Color(0xFFCFCCC0), RoundedCornerShape(7.dp))) {
                if (cs.projects.isNotEmpty() || cs.pageIndex > 0) Row(Modifier.fillMaxWidth().background(Color(0xFFF6F7FA)).padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.all_dl_results), color = DownloadMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    PageArrow(R.drawable.ic_arrow_left_rounded, { viewModel.search((cs.pageIndex - 1).coerceAtLeast(0)) }, enabled = cs.pageIndex > 0)
                    Text("${cs.pageIndex + 1}/${cs.totalPages.coerceAtLeast(1)}", color = DownloadText, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 3.dp))
                    PageArrow(R.drawable.ic_arrow_right_rounded, { viewModel.search(cs.pageIndex + 1) }, enabled = cs.pageIndex + 1 < cs.totalPages)
                }
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    if (cs.loading && cs.projects.isEmpty()) item { AllDownloadStateCard(null, stringResource(R.string.all_dl_loading_title), stringResource(R.string.all_dl_loading_body)) }
                    cs.error?.takeIf { cs.project == null }?.let { e ->
                        item { AllDownloadStateCard(R.drawable.ic_warning_filled, stringResource(R.string.all_dl_error_title), e, tint = DownloadDanger, actionText = stringResource(R.string.all_dl_retry), onAction = onSearch) }
                    }
                    items(cs.projects, key = { "${it.platform.name}/${it.projectId}" }) { item ->
                        val selected = cs.project != null && cs.project.platformTitle() == item.title
                        CompactProjectRow(item, selected, onClick = { viewModel.openProject(item) })
                    }
                    if (cs.projects.isEmpty() && !cs.loading && cs.error == null) item {
                        AllDownloadStateCard(R.drawable.ic_search, stringResource(if (cs.searched) R.string.all_dl_no_results_title else R.string.all_dl_empty_title), stringResource(if (cs.searched) R.string.all_dl_no_results_body else R.string.all_dl_search_hint))
                    }
                }
            }
        }
        //right: provider + simple install pane
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Platform.entries.sortedBy { if (it == Platform.MODRINTH) 0 else 1 }.forEach { p ->
                    val on = cs.platform == p
                    Row(
                        Modifier.weight(1f).clip(RoundedCornerShape(7.dp)).background(if (on) Color(0xFFDDEAFE) else Color.White)
                            .border(if (on) 2.dp else 1.dp, if (on) Color(0xFF3A80DC) else Color(0xFFB7BCC6), RoundedCornerShape(7.dp))
                            .clickable { viewModel.selectPlatform(p) }.padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
                    ) {
                        if (p == Platform.CURSEFORGE) androidx.compose.foundation.Image(painterResource(R.drawable.img_platform_curseforge), null, Modifier.size(18.dp).padding(end = 2.dp))
                        Text(PlatformLabel(p), color = if (on) Color(0xFF1758AF) else DownloadText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(7.dp)).background(Color.White).border(1.dp, Color(0xFFCFCCC0), RoundedCornerShape(7.dp)).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val project = cs.project
                if (project == null && !cs.loading) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("⇩", color = Color(0xFF3672C9), fontSize = 30.sp)
                        Text(stringResource(R.string.all_dl_pick_project_short), color = Color(0xFF2A466A), fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Text(stringResource(R.string.all_dl_three_steps), color = DownloadMuted, fontSize = 11.sp)
                    }
                    return@Column
                }
                //project
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    project?.platformIconUrl()?.takeIf { it.isNotBlank() }?.let { AllAssetIcon(modifier = Modifier.clip(RoundedCornerShape(7.dp)), size = 40.dp, iconUrl = it) }
                    Column(Modifier.weight(1f)) {
                        Text(project?.platformTitle().orEmpty(), color = DownloadText, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(project?.platformSummary().orEmpty(), color = DownloadMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (category == AllDownloadCategory.SHADER) Text(stringResource(R.string.all_dl_shader_needs), color = Color(0xFF875D0F), fontSize = 11.sp)
                //target
                if (modpack) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.all_dl_new_game_filter), color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val mcs = state.gameVersions.filter { it.type == MinecraftVersion.Type.Release }.take(60).map { it.version.id }
                        MenuButton(state.modpackMc?.let { "MC $it" } ?: stringResource(R.string.all_dl_any_mc), (listOf<String?>(null) + mcs).map { mc -> (mc?.let { "MC $it" } ?: "-") to { viewModel.setModpackFilter(mc, state.modpackLoader) } })
                        MenuButton(state.modpackLoader ?: stringResource(R.string.all_dl_any_loader), listOf(null, "Fabric", "Quilt", "Forge", "NeoForge").map { l -> (l ?: "-") to { viewModel.setModpackFilter(state.modpackMc, l) } })
                    }
                } else {
                    Text(stringResource(R.string.all_dl_target_game), color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    var open by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                                .background(Color.White).border(1.dp, if (targets.isEmpty()) DownloadDanger else Color(0xFF9DB5D4), RoundedCornerShape(6.dp))
                                .clickable(enabled = state.installedVersions.isNotEmpty()) { open = true }.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                targets.firstOrNull()?.let { targetLabel(it) } ?: stringResource(if (state.installedVersions.isEmpty()) R.string.all_dl_no_instance else R.string.all_dl_choose_target_game),
                                color = if (targets.isEmpty()) DownloadDanger else DownloadText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                            )
                            Text("▾", color = DownloadText, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                            state.installedVersions.forEach { iv -> DropdownMenuItem(text = { Text(targetLabel(iv)) }, onClick = { open = false; viewModel.selectTarget(iv.getVersionName()) }) }
                        }
                    }
                }
                //versions
                var compatibleOnly by remember(project) { mutableStateOf(true) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.all_dl_installable_versions), color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(
                        (if (compatibleOnly) "☑ " else "☐ ") + stringResource(R.string.all_dl_compatible_only), color = DownloadMuted, fontSize = 11.sp,
                        modifier = Modifier.clickable { compatibleOnly = !compatibleOnly }.padding(4.dp)
                    )
                }
                if (cs.error != null && project != null) Text(cs.error, color = DownloadDanger, fontSize = 11.sp, maxLines = 2)
                val all = cs.versions.take(150)
                val recommended = remember(all, state.targetVersionNames, state.modpackMc, state.modpackLoader) { recommendedOf(all) { viewModel.isCompatible(it) } }
                val shown = all.filter { !compatibleOnly || viewModel.isCompatible(it) }
                Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(6.dp)).border(1.dp, Color(0xFFD5DCE7), RoundedCornerShape(6.dp))) {
                    when {
                        cs.loading -> AllDownloadLoading()
                        shown.isEmpty() -> Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(stringResource(R.string.all_dl_no_fitting_version), color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                stringResource(if (!modpack && targets.isEmpty()) R.string.all_dl_pick_target_short else R.string.all_dl_no_fitting_short),
                                color = DownloadMuted, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                        else -> LazyColumn(Modifier.fillMaxSize()) {
                            items(shown, key = { "${it.platform().name}/${it.platformId()}" }) { pv ->
                                val ok = viewModel.isCompatible(pv)
                                val chosen = pv.platformId() == v?.platformId()
                                Row(
                                    Modifier.fillMaxWidth().background(if (chosen) Color(0xFFDDEAFE) else Color.White)
                                        .clickable(enabled = ok) { viewModel.selectVersion(pv) }.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioDot(chosen)
                                    Text(
                                        pv.platformVersion().ifBlank { pv.platformDisplayName() }, color = if (ok) DownloadText else DownloadMuted,
                                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                                    )
                                    if (pv == recommended) Pill(stringResource(R.string.all_dl_loader_recommended), PillGreen.first, PillGreen.second)
                                    if (!ok) Pill(stringResource(R.string.all_dl_does_not_fit), PillRed.first, PillRed.second)
                                    else {
                                        val type = pv.platformReleaseType().toString()
                                        val stable = type.contains("release", true)
                                        Pill(if (stable) stringResource(R.string.all_dl_stable_short) else type.lowercase().replaceFirstChar { it.uppercase() }, Color(0xFF2762A9), Color(0xFFE8EFFC))
                                    }
                                }
                                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEDF0F5)))
                            }
                        }
                    }
                }
                v?.let { Text(listOf(it.platformGameVersion().take(4).joinToString(), it.platformLoaders().joinToString { l -> l.toString() }).filter { s -> s.isNotBlank() }.joinToString("  ·  "), color = DownloadMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

/** One result row: icon, name, one-line description, provider chip */
@Composable
private fun CompactProjectRow(item: AllDownloadProject, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(if (selected) Color(0xFFDDEAFE) else Color.White)
            .border(if (selected) 2.dp else 0.dp, if (selected) Color(0xFF3A80DC) else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFFF2F2EE)), contentAlignment = Alignment.Center) {
            if (item.iconUrl.isNullOrBlank()) Text(item.title.take(1).uppercase(), color = DownloadAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            else AllAssetIcon(size = 40.dp, iconUrl = item.iconUrl)
        }
        Column(Modifier.weight(1f)) {
            Text(item.title, color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.description, color = DownloadMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("›", color = DownloadMuted, fontSize = 20.sp)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEDF0F5)))
}

/** A compact XP drop-down button */
@Composable
private fun MenuButton(label: String, options: List<Pair<String, () -> Unit>>, highlight: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    Box {
        AllButton("$label ▾", { open = true }, compact = true, style = if (highlight) AllButtonStyle.Danger else AllButtonStyle.Neutral, enabled = options.isNotEmpty())
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (text, action) -> DropdownMenuItem(text = { Text(text) }, onClick = { open = false; action() }) }
        }
    }
}

/** Prototype confirm dialog: label left, value right; "Back to edit" / "Confirm" */
@Composable
private fun SummaryDialog(title: String, rows: List<Pair<String, String>>, warning: String?, note: String?, onBack: () -> Unit, onConfirm: () -> Unit) {
    com.movtery.zalithlauncher.all.ui.AllDialog(
        title = title,
        onDismissRequest = onBack,
        actions = {
            AllButton(stringResource(R.string.all_dl_back_to_edit), onBack, compact = true)
            AllButton(stringResource(R.string.all_dl_confirm_install), onConfirm, style = AllButtonStyle.Primary, compact = true)
        }
    ) {
        Column {
            rows.forEach { (label, value) -> SummaryLine(label, value) }
            warning?.let {
                Text(it, color = Color(0xFF875D0F), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(5.dp)).background(Color(0xFFFFF9E8)).border(1.dp, Color(0xFFEDDDA9), RoundedCornerShape(5.dp)).padding(8.dp))
            }
            note?.let { Text(it, color = DownloadMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp)) }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = DownloadMuted, fontSize = 12.sp, modifier = Modifier.weight(0.38f))
        Text(value, color = DownloadText, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.weight(0.62f))
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFECEDF1)))
}

@Composable
private fun CategoryRail(selected: AllDownloadCategory, onSelect: (AllDownloadCategory) -> Unit, iconOnly: Boolean = false, onRefresh: () -> Unit = {}) {
    val compact = iconOnly
    Column(
        modifier = Modifier
            .width(if (iconOnly) 52.dp else 156.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(5.dp))
            .background(DownloadRail)
            .border(1.dp, DownloadBorder, RoundedCornerShape(5.dp))
            .verticalScroll(rememberScrollState())
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (!iconOnly) Text(stringResource(R.string.all_dl_rail_title), color = DownloadMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        AllDownloadCategory.entries.forEach { category ->
            val active = category == selected
            AllListRow(selected = active, onClick = { onSelect(category) }) {
                Icon(
                    painterResource(categoryIcon(category)), null,
                    tint = if (active) DownloadAccent else DownloadMuted,
                    modifier = Modifier.size(if (compact) 15.dp else 18.dp)
                )
                Spacer(Modifier.width(if (compact) 4.dp else 6.dp))
                //แต่ละป้ายต้องอยู่บรรทัดเดียว; on narrow windows only the icon is shown (prototype < 720 dp)
                if (!iconOnly) Text(
                    text = categoryTitle(category),
                    color = if (active) DownloadAccent else DownloadText,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!iconOnly) AllButton(stringResource(R.string.generic_refresh), onRefresh, compact = true, modifier = Modifier.fillMaxWidth())
    }
}

private fun categoryIcon(category: AllDownloadCategory): Int = when (category) {
    AllDownloadCategory.GAME -> R.drawable.ic_sports_esports_filled
    AllDownloadCategory.MODPACK -> R.drawable.ic_folder_zip_filled
    AllDownloadCategory.MOD -> R.drawable.ic_extension_outlined
    AllDownloadCategory.RESOURCE_PACK -> R.drawable.ic_image_outlined
    AllDownloadCategory.SHADER -> R.drawable.ic_lightbulb
    AllDownloadCategory.WORLD -> R.drawable.ic_public
    AllDownloadCategory.BY_ID -> R.drawable.ic_dns_outlined
}

@Composable
private fun categoryTitle(category: AllDownloadCategory): String = when (category) {
    AllDownloadCategory.GAME -> stringResource(R.string.all_dl_cat_game)
    AllDownloadCategory.MODPACK -> stringResource(R.string.all_dl_cat_modpack)
    AllDownloadCategory.MOD -> stringResource(R.string.mods_manage)
    AllDownloadCategory.RESOURCE_PACK -> stringResource(R.string.all_dl_cat_resource_pack)
    AllDownloadCategory.SHADER -> stringResource(R.string.all_dl_cat_shader)
    AllDownloadCategory.WORLD -> stringResource(R.string.all_dl_cat_world)
    AllDownloadCategory.BY_ID -> stringResource(R.string.all_dl_cat_by_id)
}

@Composable
private fun PageArrow(icon: Int, onClick: () -> Unit, enabled: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(if (LocalAllCompact.current) 24.dp else 28.dp)
            .clip(CircleShape)
            .background(if (enabled) Color.White else DownloadSurface)
            .border(1.dp, DownloadBorder, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Icon(painterResource(icon), null, tint = if (enabled) DownloadAccent else DownloadBorder, modifier = Modifier.size(if (LocalAllCompact.current) 16.dp else 20.dp))
    }
}

/** 空状态 / 无结果 / 加载 / 错误 的统一卡片：图标 + 标题 + 说明，错误时可直接重试 */
@Composable
private fun AllDownloadStateCard(
    icon: Int?,
    title: String,
    body: String,
    tint: Color = DownloadMuted,
    actionText: String? = null,
    onAction: () -> Unit = {}
) {
    val compactUi = com.movtery.zalithlauncher.all.ui.LocalAllCompact.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compactUi) 2.dp else 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (compactUi) 4.dp else 18.dp, bottom = if (compactUi) 2.dp else 12.dp, start = 16.dp, end = 16.dp)
    ) {
        if (icon != null) Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(if (compactUi) 32.dp else 52.dp).clip(CircleShape).background(DownloadRail)
        ) { Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(if (compactUi) 16.dp else 26.dp)) }
        else CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.5.dp, color = DownloadAccent)
        Text(title, color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(body, color = DownloadMuted, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (actionText != null) Spacer(Modifier.height(4.dp))
        if (actionText != null) AllDownloadButton(actionText, onAction)
    }
}

@Composable
private fun ProjectCard(item: AllDownloadProject, selected: Boolean = false, onClick: () -> Unit) {
    AllListRow(onClick = onClick, selected = selected, modifier = Modifier.heightIn(min = 60.dp)) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(Color.White)
                .border(1.dp, Color(0xFFB8C7E0), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Zalith's Coil-backed icon (cache, shimmer, error fallback); letter only when the provider gave no URL
            if (item.iconUrl.isNullOrBlank()) Text(item.title.take(1).uppercase(), color = DownloadAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            else AllAssetIcon(size = 46.dp, iconUrl = item.iconUrl)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.title, color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.description, color = DownloadMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                Text(
                    PlatformLabel(item.platform), color = DownloadAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(DownloadAccentPale).padding(horizontal = 6.dp, vertical = 1.dp)
                )
                Text(item.author, color = DownloadMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Text(item.downloads.toString(), color = DownloadMuted, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

/** "name (1.20.1 · Fabric)" of an installed game (Vanilla when it has no loader) */
private fun targetLabel(v: Version): String {
    val info = v.getVersionInfo()
    val mc = info?.minecraftVersion ?: v.getVersionName()
    val loader = info?.loaderInfo?.loader?.takeIf { it.isLoader }?.displayName?.takeIf { it.isNotBlank() } ?: "Vanilla"
    return "${v.getVersionName()} ($mc · $loader)"
}

/** 默认只显示正式版；打开“显示测试版本”后才包含快照、远古 Beta/Alpha、愚人节版等 */
internal fun visibleGameTypes(includeTest: Boolean): Set<MinecraftVersion.Type> =
    if (includeTest) MinecraftVersion.Type.values().toSet() else setOf(MinecraftVersion.Type.Release)

/** Progress of the existing installer's phases (game files, loader, ...), one bar per running step. */
@Composable
private fun InstallerProgress(tasks: List<com.movtery.zalithlauncher.coroutine.TitledTask>) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = 130.dp)
            .clip(RoundedCornerShape(5.dp)).background(Color(0xFFEAF2FA)).border(1.dp, DownloadBorder, RoundedCornerShape(5.dp)).padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            tasks.forEach { titled ->
                val progress by titled.task.progress.collectAsState()
                Text(titled.title.toAndroidString(context), color = DownloadText, fontSize = 11.sp, maxLines = 1)
                if (progress >= 0f) LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = DownloadAccent)
                else LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = DownloadAccent)
            }
        }
    }
}

@Composable
private fun ModpackInstallDialog(
    version: PlatformVersion?,
    project: String,
    name: String,
    onName: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    com.movtery.zalithlauncher.all.ui.AllDialog(
        title = stringResource(R.string.all_dl_install_modpack),
        onDismissRequest = onDismiss,
        actions = {
            AllButton(stringResource(R.string.all_dl_back_to_edit), onDismiss, compact = true)
            AllButton(stringResource(R.string.all_dl_confirm_install), onConfirm, style = AllButtonStyle.Primary, compact = true, enabled = name.isNotBlank())
        }
    ) {
        Column {
            SummaryLine(stringResource(R.string.all_dl_plan_project), project)
            version?.let { v ->
                SummaryLine(stringResource(R.string.all_dl_plan_chosen_version), v.platformDisplayName())
                SummaryLine("Minecraft", v.platformGameVersion().joinToString())
                v.platformLoaders().joinToString { it.toString() }.takeIf { it.isNotBlank() }?.let { SummaryLine(stringResource(R.string.all_dl_loader), it) }
                SummaryLine(stringResource(R.string.all_dl_plan_release), v.platformReleaseType().toString())
                SummaryLine(stringResource(R.string.all_dl_plan_source), PlatformLabel(v.platform()))
            }
            Text(stringResource(R.string.download_modpack_warning1), color = DownloadMuted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 6.dp))
            OutlinedTextField(value = name, onValueChange = onName, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.all_dl_new_instance_name)) })
        }
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
        Text(stringResource(R.string.all_dl_bg_tasks, tasks.size), color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
