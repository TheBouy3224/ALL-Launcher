package com.movtery.zalithlauncher.all.download

import com.movtery.zalithlauncher.all.ui.AllTheme

import com.movtery.zalithlauncher.all.ui.AllColors
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
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


/** Light value kept exactly as before; the dark value is used only in Midnight Luna (the switch is AllTheme). */
private fun dl(light: Color, dark: Color): Color = if (AllTheme.isDark) dark else light

private val DownloadSurface: Color get() = AllColors.WindowBody
private val DownloadPanel: Color get() = AllColors.Card
private val DownloadRail: Color get() = AllColors.MenuRight
private val DownloadBorder: Color get() = AllColors.Border
private val DownloadText: Color get() = AllColors.Text
private val DownloadMuted: Color get() = dl(Color(0xFF69665D), Color(0xFF9AA6BC))
private val DownloadAccent: Color get() = AllColors.Blue
private val DownloadAccentPale: Color get() = AllColors.BluePale
private val DownloadDanger: Color get() = AllColors.Danger

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
                //install / download status: always ONE line (never squeezes the page); the per-step bars open on demand
                var detailsOpen by remember { mutableStateOf(false) }
                //while an install runs the pinned InstallingBar at the bottom is the only status (no duplicate line up here)
                if (!state.busy) {
                    state.progressMessage?.takeIf { it.startsWith("Installed ") }?.let {
                        Text("✓ " + stringResource(R.string.all_dl_installed_done, it.removePrefix("Installed ")), color = DownloadAccent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                    }
                    if (state.runningTaskIds.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.all_dl_bg_tasks, state.tasks.size), color = DownloadAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(
                            stringResource(if (detailsOpen) R.string.all_dl_hide_details else R.string.all_dl_show_details), color = DownloadMuted, fontSize = 12.sp,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { detailsOpen = !detailsOpen }.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
                if (detailsOpen) {
                    if (state.busy && state.installerTasks.isNotEmpty()) Box(Modifier.padding(horizontal = 8.dp)) { InstallerProgress(state.installerTasks) }
                    if (state.runningTaskIds.isNotEmpty()) Box(Modifier.padding(horizontal = 8.dp)) { TaskPanel(state.tasks, state.trackedAssetTaskIds, onCancel = viewModel::cancelTask) }
                }

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
                    "Minecraft" to v.platformGameVersion().let { g -> if (g.size <= 3) g.joinToString() else g.takeLast(2).joinToString() + "  +${g.size - 2}" },
                    v.platformLoaders().joinToString { it.toString() }.takeIf { it.isNotBlank() }?.let { stringResource(R.string.all_dl_loader) to it },
                    stringResource(R.string.all_dl_plan_source) to (PlatformLabel(v.platform()) + "  ·  " + v.platformReleaseType().toString()),
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
            viewModel = viewModel,
            version = categoryState.selectedVersion,
            project = categoryState.project?.platformTitle().orEmpty(),
            name = state.modpackInstallName,
            takenNames = remember(state.installedVersions) { state.installedVersions.map { it.getVersionName() }.toSet() },
            onName = viewModel::updateModpackName,
            onDismiss = viewModel::closeModpackInstall,
            onConfirm = viewModel::installModpack
        )
    }
    state.installError?.let { error ->
        //plain-language first line; the raw message stays available underneath for bug reports
        val failedFiles = if (error.startsWith("Failed downloads:")) error.lines().drop(1).count { it.isNotBlank() } else 0
        com.movtery.zalithlauncher.all.ui.AllDialog(
            title = stringResource(R.string.all_dl_error_dialog_title),
            onDismissRequest = viewModel::dismissInstallError,
            kind = com.movtery.zalithlauncher.all.ui.AllDialogKind.Error,
            actions = {
                AllButton(stringResource(R.string.generic_close), viewModel::dismissInstallError, compact = true)
                if (state.installRetryable) AllButton(stringResource(R.string.all_dl_retry), viewModel::retryGameInstall, style = AllButtonStyle.Primary, compact = true)
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (failedFiles > 0) stringResource(R.string.all_dl_error_files, failedFiles) else stringResource(R.string.all_dl_error_generic),
                    color = DownloadText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                )
                var details by remember { mutableStateOf(false) }
                Text(
                    stringResource(if (details) R.string.all_dl_hide_details else R.string.all_dl_show_details), color = DownloadMuted, fontSize = 12.sp,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { details = !details }.padding(vertical = 6.dp)
                )
                if (details) Text(error, color = DownloadMuted, fontSize = 10.sp, modifier = Modifier.heightIn(max = 140.dp).verticalScroll(rememberScrollState()))
            }
        }
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
    Box(Modifier.fillMaxWidth().height(1.dp).background(dl(Color(0xFFE5E7EE), Color(0xFF34415A))))
}

/** Prototype .work-footer: what will happen (2 lines) + a way back + "Review before install", never scrolled away */
@Composable
private fun PageFooter(summary: String, detail: String, secondary: String, onSecondary: () -> Unit, secondaryEnabled: Boolean, primaryEnabled: Boolean, onPrimary: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(dl(Color(0xFFF6F9FF), Color(0xFF1E2A3F)), dl(Color(0xFFE6EEFB), Color(0xFF182334)))))
            .border(1.dp, dl(Color(0xFFB8C3D4), Color(0xFF34415A)))
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
        Box(Modifier.size(20.dp).clip(CircleShape).background(dl(Color(0xFF2564C6), Color(0xFF6FA8FF))), contentAlignment = Alignment.Center) {
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

private val PillGreen: Pair<Color, Color> get() = dl(Color(0xFF286C26), Color(0xFF7BD389)) to dl(Color(0xFFE5F6DF), Color(0xFF1E3A24))
private val PillOrange: Pair<Color, Color> get() = dl(Color(0xFF986213), Color(0xFFF0C27A)) to dl(Color(0xFFFFF0DA), Color(0xFF3D2E17))
private val PillRed: Pair<Color, Color> get() = dl(Color(0xFFA23131), Color(0xFFFF8A8A)) to dl(Color(0xFFFFF0F0), Color(0xFF3D2020))
private val PillGrey: Pair<Color, Color> get() = dl(Color(0xFF667080), Color(0xFF9AA6BC)) to dl(Color(0xFFF2F3F5), Color(0xFF2A3446))

@Composable
private fun RadioDot(on: Boolean) {
    Box(
        Modifier.size(18.dp).clip(CircleShape).border(2.dp, if (on) dl(Color(0xFF2163BC), Color(0xFF6FA8FF)) else dl(Color(0xFF8A9CB5), Color(0xFF4A6289)), CircleShape),
        contentAlignment = Alignment.Center
    ) { if (on) Box(Modifier.size(8.dp).clip(CircleShape).background(dl(Color(0xFF2163BC), Color(0xFF6FA8FF)))) }
}

private fun loaderIcon(loader: AllGameLoader): Int = when (loader) {
    AllGameLoader.VANILLA -> R.drawable.img_old_grass_block
    AllGameLoader.FABRIC -> R.drawable.img_loader_fabric
    AllGameLoader.QUILT -> R.drawable.img_loader_quilt
    AllGameLoader.NEOFORGE -> R.drawable.img_loader_neoforge
    AllGameLoader.FORGE -> R.drawable.img_anvil
    AllGameLoader.OPTIFINE -> R.drawable.img_loader_optifine
}

/** Download V2 card: thin border, soft radius, no nested cards. Every download page builds on this. */
@Composable
private fun DlCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(dl(Color(0xFFFBFAF6), Color(0xFF1C2535)))
            .border(1.dp, dl(Color(0xFFDAD6C8), Color(0xFF34415A)), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp), content = content
    )
}

@Composable
private fun DlCardTitle(icon: Int?, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        icon?.let { androidx.compose.foundation.Image(painterResource(it), null, Modifier.size(22.dp)) }
        Text(text, color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A tappable value box with a drop-down arrow (min 44 dp high: touch first). [content] is the closed-state label. */
@Composable
private fun DlField(modifier: Modifier = Modifier, enabled: Boolean = true, error: Boolean = false, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(8.dp)).background(AllColors.Card)
            .border(1.dp, if (error) DownloadDanger else dl(Color(0xFFB3C3DB), Color(0xFF3A5A8A)), RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
        Text("▾", color = if (enabled) DownloadText else DownloadMuted, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/** Status chip: green = good, grey = neutral */
@Composable
private fun DlChip(text: String, good: Boolean) {
    val (fg, bg) = if (good) PillGreen else PillGrey
    Row(
        Modifier.clip(RoundedCornerShape(16.dp)).background(bg).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (good) Text("✓", color = fg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(text, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun LoaderButton(loader: AllGameLoader, on: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(9.dp))
            .background(if (on) dl(Color(0xFFDDEAFE), Color(0xFF24406B)) else if (enabled) AllColors.Card else dl(Color(0xFFEFEFEC), Color(0xFF1A2230)))
            .border(if (on) 2.dp else 1.dp, if (on) dl(Color(0xFF3A80DC), Color(0xFF6FA8FF)) else dl(Color(0xFFC3C8D2), Color(0xFF34415A)), RoundedCornerShape(9.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
    ) {
        androidx.compose.foundation.Image(painterResource(loaderIcon(loader)), null, Modifier.size(24.dp), alpha = if (enabled) 1f else 0.35f)
        Spacer(Modifier.width(7.dp))
        Text(
            loader.title, color = if (on) dl(Color(0xFF1758AF), Color(0xFF9CC4FF)) else if (enabled) DownloadText else DownloadMuted,
            fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, softWrap = false
        )
    }
}

/** characters Windows/Android file systems refuse in a folder name; the install name becomes the instance folder */
private const val INSTALL_NAME_BAD_CHARS = "\\/:*?\"<>|"

/**
 * Download > Game (Download V2): version -> loader (+ its version) -> name -> Install.
 * Wide windows (landscape phones) use two columns so the name field is on screen without scrolling:
 * left = header, Minecraft version, installation name; right = loader + loader version. Narrow windows use one scrolling column
 * (version, name, loader). The green Install button is pinned at the bottom. All choices come from the existing view model /
 * installer; a loader with no version for the chosen Minecraft version is greyed and cannot be chosen.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.GamePage(state: AllDownloadState, viewModel: AllDownloadViewModel) {
    val ctxForBar = LocalContext.current
    val version = state.selectedGameVersion
    val loader = state.gameLoader
    val mc = version?.version?.id
    val name = state.gameInstallName
    val nameTaken = state.installedVersions.any { it.getVersionName() == name.trim() }
    val nameBad = name.isNotEmpty() && (name.any { it in INSTALL_NAME_BAD_CHARS } || name.trimEnd().endsWith("."))
    val fabric = loader == AllGameLoader.FABRIC
    val bundleReady = !fabric || (!state.loaderLoading && !state.modMenuLoading)
    val ready = !state.busy && version != null && name.isNotBlank() && !nameTaken && !nameBad && bundleReady &&
        (loader == AllGameLoader.VANILLA || state.selectedLoaderVersion != null)
    val offered = state.supportedLoaders

    val label: @Composable (String) -> Unit = { Text(it, color = DownloadMuted, fontSize = 12.sp, maxLines = 1) }

    val versionField: @Composable (Modifier) -> Unit = { m ->
        Column(m, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            label(stringResource(R.string.all_dl_lbl_version))
            var menu by remember { mutableStateOf(false) }
            var includeTest by remember { mutableStateOf(false) }
            Box {
                DlField(Modifier.fillMaxWidth(), onClick = { menu = true }) {
                    Text(version?.version?.id ?: stringResource(R.string.generic_loading), color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
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

    val nameField: @Composable (Modifier) -> Unit = { m ->
        Column(m, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            label(stringResource(R.string.all_dl_lbl_name))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp)).background(AllColors.Card)
                    .border(1.dp, if (nameTaken || nameBad) DownloadDanger else dl(Color(0xFFB3C3DB), Color(0xFF34415A)), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = name, onValueChange = viewModel::updateGameInstallName, singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = DownloadText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(DownloadAccent),
                    modifier = Modifier.weight(1f)
                )
                if (name.isNotEmpty()) Text(
                    "✕", color = DownloadMuted, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.size(36.dp).clip(CircleShape).clickable { viewModel.updateGameInstallName("") }.padding(top = 7.dp)
                )
            }
            if (nameTaken || nameBad) Text(
                stringResource(if (nameTaken) R.string.all_dl_already_installed else R.string.all_dl_name_invalid),
                color = DownloadDanger, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
    }

    //loader pills in one row; the loader-version picker joins the row only when a loader needs one
    val loaderSection: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            label(stringResource(R.string.all_dl_lbl_loader))
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AllGameLoader.entries.forEach { option ->
                    val enabled = option == AllGameLoader.VANILLA || (offered != null && option in offered)
                    val on = loader == option
                    Text(
                        option.title, color = if (on) Color.White else if (enabled) DownloadText else DownloadMuted.copy(alpha = 0.5f),
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1,
                        modifier = Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(20.dp))
                            .background(if (on) DownloadAccent else Color.Transparent)
                            .border(1.dp, if (on) DownloadAccent else dl(Color(0xFFC3C8D2), Color(0xFF2C3754)), RoundedCornerShape(20.dp))
                            .clickable(enabled = enabled) { viewModel.selectGameLoader(option) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
                if (loader != AllGameLoader.VANILLA) {
                    val list = state.loaderVersions
                    val chosen = state.selectedLoaderVersion
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        DlField(Modifier.widthIn(min = 200.dp), enabled = !list.isNullOrEmpty(), onClick = { menu = true }) {
                            Text(chosen?.let { "${loader.title} ${it.getAddonVersion()}" } ?: stringResource(R.string.generic_loading), color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            chosen?.let { c ->
                                if (AllLoaderCatalog.isRecommended(c)) Pill(stringResource(R.string.all_dl_loader_recommended), PillGreen.first, PillGreen.second)
                                else if (AllLoaderCatalog.isPreRelease(c)) Pill("Beta", PillOrange.first, PillOrange.second)
                            }
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            val (stable, unstable) = remember(list) { AllLoaderCatalog.splitStable(list.orEmpty()) }
                            val recommendedText = stringResource(R.string.all_dl_loader_recommended)
                            (stable + unstable).take(200).forEach { lv ->
                                val tag = when { AllLoaderCatalog.isRecommended(lv) -> "  ·  $recommendedText"; lv in unstable -> "  ·  Beta"; else -> "" }
                                DropdownMenuItem(text = { Text(lv.getAddonVersion() + tag) }, onClick = { menu = false; viewModel.selectLoaderVersion(lv) })
                            }
                        }
                    }
                }
            }
            if (fabric) androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (state.loaderLoading || state.modMenuLoading) DlChip(stringResource(R.string.all_dl_checking_version), false)
                else {
                    if (state.loaderApiVersion != null) DlChip(stringResource(R.string.all_dl_api_recommended, "Fabric API"), true)
                    else DlChip(stringResource(R.string.all_dl_api_none, "Fabric API"), false)
                    if (state.modMenuVersion != null) DlChip("Mod Menu", true)
                }
            }
            val hints = mutableListOf<String>()
            if (loader == AllGameLoader.VANILLA) hints += stringResource(R.string.all_dl_no_loader_needed)
            if (loader == AllGameLoader.OPTIFINE) hints += stringResource(R.string.all_dl_optifine_note)
            if (offered == null) hints += stringResource(R.string.all_dl_loader_probing)
            else {
                val missing = AllGameLoader.entries.filter { it != AllGameLoader.VANILLA && it !in offered }
                if (missing.isNotEmpty()) hints += stringResource(R.string.all_dl_loader_unsupported, mc.orEmpty(), missing.joinToString { it.title })
            }
            if (hints.isNotEmpty()) Text(hints.joinToString("  ·  "), color = DownloadMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }

    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
        val wide = maxWidth >= 640.dp
        //while installing the page is dimmed and ignores touches (the bar below says what is happening)
        if (state.busy) Box(Modifier.fillMaxSize().zIndex(1f).background(dl(Color(0x99F8F7F2), Color(0x99141B26))).pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            if (wide) Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                versionField(Modifier.weight(1f)); nameField(Modifier.weight(1f))
            } else { versionField(Modifier); nameField(Modifier) }
            loaderSection()
        }
    }

    //pinned install button; the installing bar takes its place while an install runs
    Box(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 8.dp)) {
        if (state.busy) InstallingBar(state.progressMessage ?: name, state.installerTasks.take(2).joinToString("  ·  ") { it.title.toAndroidString(ctxForBar) }, state.installerTasks.map { it.task.progress }, viewModel::cancelInstall)
        else com.movtery.zalithlauncher.all.ui.AllButtonBox(viewModel::requestGameInstall, Modifier.fillMaxWidth().heightIn(min = 52.dp), style = AllButtonStyle.Primary, enabled = ready, fill = true) {
            Icon(painterResource(R.drawable.ic_download), null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(mc?.let { stringResource(R.string.all_dl_install_mc, it) } ?: stringResource(R.string.all_install), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * The unmistakable "installing" state: spinner, what is being installed, the current steps, one overall bar with a percentage
 * and (for game installs) Cancel. It replaces the Install button while a game/modpack install runs, so nobody taps Install twice
 * or wonders whether anything is happening.
 */
@Composable
private fun InstallingBar(title: String, steps: String, flows: List<kotlinx.coroutines.flow.StateFlow<Float>>, onCancel: (() -> Unit)?) {
    val overall by remember(flows) {
        if (flows.isEmpty()) kotlinx.coroutines.flow.flowOf(-1f)
        else kotlinx.coroutines.flow.combine(flows) { arr -> arr.filter { it >= 0f }.let { known -> if (known.isEmpty()) -1f else known.average().toFloat() } }
    }.collectAsState(initial = -1f)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(dl(Color(0xFFE3EEFF), Color(0xFF1F3354)))
            .border(2.dp, dl(Color(0xFF3A80DC), Color(0xFF6FA8FF)), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp, color = DownloadAccent)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.all_dl_installing_name, title), color = DownloadText, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (steps.isNotBlank()) Text(steps, color = DownloadMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (overall >= 0f) Text("${(overall * 100).toInt().coerceIn(0, 100)}%", color = DownloadAccent, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            onCancel?.let { c -> TextButton(onClick = c) { Text(stringResource(R.string.generic_cancel), color = DownloadDanger, fontWeight = FontWeight.Bold) } }
        }
        if (overall >= 0f) LinearProgressIndicator(progress = { overall.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = DownloadAccent)
        else LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = DownloadAccent)
        Text(stringResource(R.string.all_dl_installing_hint), color = DownloadMuted, fontSize = 11.sp)
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
    val ctxForBar = LocalContext.current
    val category = state.category
    val modpack = category == AllDownloadCategory.MODPACK
    val isById = category == AllDownloadCategory.BY_ID
    val targets = state.installedVersions.filter { it.getVersionName() in state.targetVersionNames }
    val v = cs.selectedVersion
    val canInstall = !state.busy && if (modpack) v != null && viewModel.isCompatible(v) else cs.versions.isNotEmpty()
    val project0 = cs.project

    val inProject = cs.route == AllDownloadRoute.PROJECT
    var pickGame by remember { mutableStateOf(false) }
    //"advanced": the user's own version pick; used for a game only when it fits that game
    var manual by remember(cs.project) { mutableStateOf<PlatformVersion?>(null) }
    var advanced by remember(cs.project) { mutableStateOf(false) }
    //modpacks are installed as a new game: the newest fitting version is pre-marked so the button works without the version list
    if (modpack && inProject) androidx.compose.runtime.LaunchedEffect(cs.versions, state.modpackMc, state.modpackLoader) {
        if (cs.selectedVersion == null) recommendedOf(cs.versions.take(150)) { viewModel.isCompatible(it) }?.let(viewModel::selectVersion)
    }

    //top bar: search + provider in one row
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (inProject) {
            Text(
                "‹  " + stringResource(R.string.all_dl_back_results), color = DownloadAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(AllColors.Card).border(1.dp, dl(Color(0xFFB3C3DB), Color(0xFF3A5A8A)), RoundedCornerShape(10.dp))
                    .clickable { viewModel.back() }.padding(horizontal = 14.dp, vertical = 8.dp)
            )
            Spacer(Modifier.weight(1f))
        } else Row(
            Modifier.weight(1f).heightIn(min = 46.dp).clip(RoundedCornerShape(10.dp)).background(AllColors.Card).border(1.dp, dl(Color(0xFFB3C3DB), Color(0xFF3A5A8A)), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 2.dp),
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
        //provider (segmented)
        Row(Modifier.clip(RoundedCornerShape(10.dp)).background(AllColors.Card).border(1.dp, dl(Color(0xFFC3C8D2), Color(0xFF34415A)), RoundedCornerShape(10.dp)).padding(3.dp)) {
            Platform.entries.sortedBy { if (it == Platform.MODRINTH) 0 else 1 }.forEach { p ->
                val on = cs.platform == p
                Row(
                    Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(8.dp)).background(if (on) dl(Color(0xFFDDEAFE), Color(0xFF24406B)) else Color.Transparent)
                        .clickable { viewModel.selectPlatform(p) }.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(painterResource(if (p == Platform.MODRINTH) R.drawable.img_platform_modrinth else R.drawable.img_platform_curseforge), null, Modifier.size(18.dp).padding(end = 4.dp))
                    Text(PlatformLabel(p), color = if (on) dl(Color(0xFF1758AF), Color(0xFF9CC4FF)) else DownloadText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        val project = cs.project
        if (!inProject) Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            //results: full width, two columns when there is room
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                androidx.compose.foundation.lazy.grid.GridCells.Adaptive(300.dp), Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val wide: androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> androidx.compose.foundation.lazy.grid.GridItemSpan = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }
                if (cs.loading && cs.projects.isEmpty()) item(span = wide) { AllDownloadStateCard(null, stringResource(R.string.all_dl_loading_title), stringResource(R.string.all_dl_loading_body)) }
                cs.error?.let { e -> item(span = wide) { Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AllDownloadStateCard(R.drawable.ic_warning_filled, stringResource(R.string.all_dl_error_title), friendlyError(e), tint = DownloadDanger, actionText = stringResource(R.string.all_dl_retry), onAction = onSearch)
                    ErrorDetail(e)
                } } }
                items(cs.projects.size, key = { "${cs.projects[it].platform.name}/${cs.projects[it].projectId}" }) { i ->
                    val item = cs.projects[i]
                    ResultCard(item) { viewModel.openProject(item) }
                }
                if (cs.projects.isEmpty() && !cs.loading && cs.error == null) item(span = wide) {
                    if (isById) Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔗", fontSize = 34.sp)
                        Text(stringResource(R.string.all_dl_id_empty_title), color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(stringResource(R.string.all_dl_id_empty_body), color = DownloadMuted, fontSize = 12.sp)
                    } else AllDownloadStateCard(R.drawable.ic_search, stringResource(if (cs.searched) R.string.all_dl_no_results_title else R.string.all_dl_empty_title), stringResource(if (cs.searched) R.string.all_dl_no_results_body else R.string.all_dl_search_hint))
                }
            }
            if (cs.projects.isNotEmpty() || cs.pageIndex > 0) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                PageArrow(stringResource(R.string.all_dl_prev_page), { viewModel.search((cs.pageIndex - 1).coerceAtLeast(0)) }, enabled = cs.pageIndex > 0)
                Text(stringResource(R.string.all_dl_page_of, cs.pageIndex + 1, cs.totalPages.coerceAtLeast(1)), color = DownloadText, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp))
                PageArrow(stringResource(R.string.all_dl_next_page), { viewModel.search(cs.pageIndex + 1) }, enabled = cs.pageIndex + 1 < cs.totalPages)
            }
        } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            //project detail: minimal - icon, name, facts, description; the game is chosen when Install is pressed
            if (project == null) { AllDownloadLoading(); cs.error?.let { Text(friendlyError(it), color = DownloadDanger, fontSize = 12.sp); ErrorDetail(it) }; return@Column }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(dl(Color(0xFFDDEAFE), Color(0xFF24406B))), contentAlignment = Alignment.Center) {
                    val icon = project.platformIconUrl()
                    if (icon.isNullOrBlank()) Text(project.platformTitle().take(1).uppercase(), color = DownloadAccent, fontWeight = FontWeight.Bold, fontSize = 32.sp)
                    else AllAssetIcon(modifier = Modifier.clip(RoundedCornerShape(12.dp)), size = 80.dp, iconUrl = icon)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(project.platformTitle(), color = DownloadText, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val all = cs.versions
                    val facts = listOfNotNull(
                        PlatformLabel(cs.platform),
                        all.flatMap { it.platformLoaders().map { l -> l.toString() } }.distinct().take(4).joinToString().takeIf { it.isNotBlank() },
                        all.flatMap { it.platformGameVersion().toList() }.distinct().let { g -> if (g.isEmpty()) null else if (g.size == 1) "MC ${g[0]}" else "MC ${g.last()} – ${g.first()}" }
                    )
                    Text(facts.joinToString("  ·  "), color = DownloadMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(project.platformSummary().orEmpty(), color = DownloadText, fontSize = 14.sp, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    if (cs.loading) Text(stringResource(R.string.generic_loading), color = DownloadMuted, fontSize = 12.sp)
                    cs.error?.let { Text(friendlyError(it), color = DownloadDanger, fontSize = 12.sp, maxLines = 2); ErrorDetail(it) }
                    if (!modpack && cs.versions.isNotEmpty()) Text(
                        (if (advanced) "▾ " else "▸ ") + stringResource(R.string.all_dl_advanced_versions) + ((if (modpack) cs.selectedVersion else manual)?.let { "  ·  " + it.platformVersion().ifBlank { it.platformDisplayName() } } ?: ""),
                        color = DownloadAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { advanced = !advanced }.padding(vertical = 8.dp)
                    )
                }
            }
            //screenshots from the platform (every category); tap = full screen
            val shots = remember(project) { project.platformScreenshots() }
            if (shots.isNotEmpty()) {
                var full by remember { mutableStateOf<String?>(null) }
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(shots.size) { i ->
                        coil3.compose.AsyncImage(
                            model = shots[i].imageUrl, contentDescription = shots[i].title, contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.size(220.dp, 124.dp).clip(RoundedCornerShape(10.dp)).background(DownloadSurface).clickable { full = shots[i].imageUrl }
                        )
                    }
                }
                full?.let { url ->
                    androidx.compose.ui.window.Dialog({ full = null }, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
                        Box(Modifier.fillMaxSize().background(Color(0xE6000000)).clickable { full = null }, contentAlignment = Alignment.Center) {
                            coil3.compose.AsyncImage(model = url, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(16.dp))
                        }
                    }
                }
            }
            //advanced: every version; the pick is used for a game only if it fits that game (modpacks: the filter above)
            if (advanced) Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, dl(Color(0xFFD5DCE7), Color(0xFF34415A)), RoundedCornerShape(10.dp))) {
                val all = cs.versions.take(150)
                val rec = remember(all) { recommendedOf(all) { true } }
                all.forEach { pv ->
                    val chosen = pv.platformId() == (if (modpack) cs.selectedVersion else manual)?.platformId()
                    val ok = !modpack || viewModel.isCompatible(pv)
                    Row(
                        Modifier.fillMaxWidth().background(if (chosen) dl(Color(0xFFDDEAFE), Color(0xFF24406B)) else AllColors.Card)
                            .clickable(enabled = ok) { if (modpack) viewModel.selectVersion(pv) else manual = if (chosen) null else pv }.heightIn(min = 44.dp).padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioDot(chosen)
                        Text(pv.platformVersion().ifBlank { pv.platformDisplayName() }, color = if (ok) DownloadText else DownloadMuted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(pv.platformGameVersion().take(3).joinToString(), color = DownloadMuted, fontSize = 11.sp, maxLines = 1)
                        if (pv == rec) Pill(stringResource(R.string.all_dl_loader_recommended), PillGreen.first, PillGreen.second)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(dl(Color(0xFFEDF0F5), Color(0xFF2A3446))))
                }
            }
        }
    }

    //"Install into which game?" - every installed game, fitting ones first, each says which file it gets
    if (pickGame && project0 != null) {
        val games = state.installedVersions
        val plan = remember(games, cs.versions, manual) { games.map { g -> g to viewModel.versionFor(g, manual) }.sortedBy { if (it.second == null) 1 else 0 } }
        var chosen by remember { mutableStateOf(plan.firstOrNull { it.second != null && it.first.getVersionName() == state.preferredTargetName }?.first) }
        com.movtery.zalithlauncher.all.ui.AllDialog(
            title = stringResource(R.string.all_dl_install_into, project0.platformTitle()),
            onDismissRequest = { pickGame = false },
            actions = {
                AllButton(stringResource(R.string.generic_cancel), { pickGame = false }, compact = true)
                AllButton(stringResource(R.string.all_install), { chosen?.let { viewModel.installInto(it, manual) }; pickGame = false }, style = AllButtonStyle.Success, compact = true, enabled = chosen != null)
            }
        ) {
            Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (games.isEmpty()) Text(stringResource(R.string.all_dl_no_instance), color = DownloadMuted, fontSize = 13.sp)
                plan.forEach { (g, file) ->
                    val on = chosen == g
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (on) dl(Color(0xFFEEF4FE), Color(0xFF24406B)) else AllColors.Card)
                            .border(if (on) 2.dp else 1.dp, if (on) dl(Color(0xFF3A80DC), Color(0xFF6FA8FF)) else dl(Color(0xFFDAD6C8), Color(0xFF34415A)), RoundedCornerShape(10.dp))
                            .clickable(enabled = file != null) { chosen = g }.heightIn(min = 52.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🎮", fontSize = 18.sp, color = if (file != null) DownloadText else DownloadMuted)
                        Column(Modifier.weight(1f)) {
                            Text(g.getVersionName(), color = if (file != null) DownloadText else DownloadMuted, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val info = g.getVersionInfo()
                            val mcLoader = "${info?.minecraftVersion ?: "?"} · ${info?.loaderInfo?.loader?.takeIf { it.isLoader || it.displayName == "OptiFine" }?.displayName ?: "Vanilla"}"
                            Text(
                                file?.let { stringResource(R.string.all_dl_will_install, it.platformVersion().ifBlank { it.platformDisplayName() }) + "  ·  " + mcLoader }
                                    ?: if (state.category == AllDownloadCategory.SHADER && !viewModel.hasShaderLoader(g)) stringResource(R.string.all_dl_needs_shader_loader, mcLoader)
                                    else stringResource(R.string.all_dl_no_version_for, mcLoader),
                                color = DownloadMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (file != null) Pill(stringResource(R.string.all_dl_fits), PillGreen.first, PillGreen.second)
                        else Pill(stringResource(R.string.all_dl_does_not_fit), PillRed.first, PillRed.second)
                    }
                }
            }
        }
    }

    //pinned main button (same place and look as the Game page); a running install shows the installing bar instead
    Box(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 10.dp)) {
        if (state.busy) InstallingBar(state.progressMessage ?: cs.project?.platformTitle().orEmpty(), state.installerTasks.take(2).joinToString("  ·  ") { it.title.toAndroidString(ctxForBar) }, state.installerTasks.map { it.task.progress }, viewModel::cancelInstall)
        else if (state.runningTaskIds.isNotEmpty()) InstallingBar(
            stringResource(R.string.all_dl_bg_tasks, state.tasks.size), "",
            state.tasks.takeLast(4).map { it.progress }, null
        )
        else if (inProject) com.movtery.zalithlauncher.all.ui.AllButtonBox(
            { if (modpack) viewModel.openModpackInstall() else pickGame = true },
            Modifier.fillMaxWidth().heightIn(min = 52.dp), style = AllButtonStyle.Success, enabled = canInstall, fill = true
        ) {
            Icon(painterResource(R.drawable.ic_download), null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    modpack -> stringResource(R.string.all_dl_install_as_new)
                    isById -> stringResource(R.string.all_install)
                    else -> stringResource(R.string.all_dl_install_cat_short, categoryTitle(category))
                },
                fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** One search result as a card: icon, name, one-line description, downloads */
@Composable
private fun ResultCard(item: AllDownloadProject, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AllColors.Card).border(1.dp, dl(Color(0xFFDAD6C8), Color(0xFF34415A)), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).heightIn(min = 64.dp).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(dl(Color(0xFFDDEAFE), Color(0xFF24406B))), contentAlignment = Alignment.Center) {
            if (item.iconUrl.isNullOrBlank()) Text(item.title.take(1).uppercase(), color = DownloadAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            else AllAssetIcon(size = 44.dp, iconUrl = item.iconUrl)
        }
        Column(Modifier.weight(1f)) {
            Text(item.title, color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOf(item.description, shortCount(item.downloads)).filter { it.isNotBlank() }.joinToString("  ·  "), color = DownloadMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun shortCount(n: Long): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0).replace(".0M", "M")
    n >= 1_000 -> "${n / 1_000}K"
    n > 0 -> n.toString()
    else -> ""
}

/** One result row: icon, name, one-line description, provider chip */
@Composable
private fun CompactProjectRow(item: AllDownloadProject, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(if (selected) dl(Color(0xFFDDEAFE), Color(0xFF24406B)) else AllColors.Card)
            .border(if (selected) 2.dp else 0.dp, if (selected) dl(Color(0xFF3A80DC), Color(0xFF6FA8FF)) else Color.Transparent)
            .clickable(onClick = onClick).heightIn(min = 60.dp).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(7.dp)).background(dl(Color(0xFFF2F2EE), Color(0xFF222B3A))), contentAlignment = Alignment.Center) {
            if (item.iconUrl.isNullOrBlank()) Text(item.title.take(1).uppercase(), color = DownloadAccent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            else AllAssetIcon(size = 40.dp, iconUrl = item.iconUrl)
        }
        Column(Modifier.weight(1f)) {
            Text(item.title, color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(item.description, color = DownloadMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("›", color = DownloadMuted, fontSize = 20.sp)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(dl(Color(0xFFEDF0F5), Color(0xFF2A3446))))
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

/** Minimal confirm sheet: title, one tidy list of what will happen, a one-line note, two buttons. No coloured header, no divider lines. */
@Composable
private fun SummaryDialog(title: String, rows: List<Pair<String, String>>, warning: String?, note: String?, onBack: () -> Unit, onConfirm: () -> Unit) {
    com.movtery.zalithlauncher.all.ui.AllDialog(
        title = "",
        onDismissRequest = onBack,
        actions = {
            AllButton(stringResource(R.string.all_dl_back_to_edit), onBack, compact = true)
            AllButton(stringResource(R.string.all_install), onConfirm, style = AllButtonStyle.Success, compact = true)
        }
    ) {
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.foundation.Image(painterResource(R.drawable.img_old_grass_block), null, Modifier.size(30.dp))
                Text(title, color = DownloadText, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(dl(Color(0xFFFBFAF6), Color(0xFF1C2535)))
                    .border(1.dp, dl(Color(0xFFDAD6C8), Color(0xFF34415A)), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
            ) { rows.forEach { (label, value) -> SummaryLine(label, value) } }
            warning?.let {
                Text(it, color = dl(Color(0xFF875D0F), Color(0xFFF0C27A)), fontSize = 12.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(dl(Color(0xFFFFF9E8), Color(0xFF3D2E17))).padding(8.dp))
            }
            note?.let { Text(it, color = DownloadMuted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Text(label, color = DownloadMuted, fontSize = 12.sp, modifier = Modifier.weight(0.38f))
        Text(value, color = DownloadText, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.weight(0.62f))
    }
}

@Composable
private fun CategoryRail(selected: AllDownloadCategory, onSelect: (AllDownloadCategory) -> Unit, iconOnly: Boolean = false, onRefresh: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .width(if (iconOnly) 56.dp else 170.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AllDownloadCategory.entries.forEach { category ->
            val active = category == selected
            val shape = RoundedCornerShape(10.dp)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 46.dp).clip(shape)
                    .background(if (active) dl(Color(0xFFE6F0FF), Color(0xFF24406B)) else AllColors.Card)
                    .border(if (active) 2.dp else 1.dp, if (active) dl(Color(0xFF3A80DC), Color(0xFF6FA8FF)) else DownloadBorder, shape)
                    .clickable { onSelect(category) }.padding(horizontal = if (iconOnly) 0.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (iconOnly) Arrangement.Center else Arrangement.spacedBy(10.dp)
            ) {
                Icon(painterResource(categoryIcon(category)), null, tint = if (active) dl(DownloadAccent, Color(0xFF9CC4FF)) else DownloadMuted, modifier = Modifier.size(24.dp))
                //one line per label; on narrow windows only the icon is shown
                if (!iconOnly) Text(
                    text = categoryTitle(category),
                    color = if (active) dl(DownloadAccent, Color(0xFF9CC4FF)) else DownloadText,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 15.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis
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
private fun PageArrow(label: String, onClick: () -> Unit, enabled: Boolean) {
    Text(
        label, color = if (enabled) DownloadAccent else DownloadBorder, fontWeight = FontWeight.Bold, fontSize = 14.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) AllColors.Card else DownloadSurface)
            .border(1.dp, DownloadBorder, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = if (LocalAllCompact.current) 6.dp else 9.dp)
    )
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

/** "name (1.20.1 · Fabric)" of an installed game (Vanilla when it has no loader) */
private fun targetLabel(v: Version): String {
    val info = v.getVersionInfo()
    val mc = info?.minecraftVersion ?: v.getVersionName()
    val loader = info?.loaderInfo?.loader?.takeIf { it.isLoader || it.displayName == "OptiFine" }?.displayName?.takeIf { it.isNotBlank() } ?: "Vanilla"
    return "${v.getVersionName()} ($mc · $loader)"
}

internal fun visibleGameTypes(includeTest: Boolean): Set<MinecraftVersion.Type> =
    if (includeTest) MinecraftVersion.Type.values().toSet() else setOf(MinecraftVersion.Type.Release)

/** Progress of the existing installer's phases (game files, loader, ...), one bar per running step. */
@Composable
private fun InstallerProgress(tasks: List<com.movtery.zalithlauncher.coroutine.TitledTask>) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = 130.dp)
            .clip(RoundedCornerShape(5.dp)).background(dl(Color(0xFFEAF2FA), Color(0xFF1F3354))).border(1.dp, DownloadBorder, RoundedCornerShape(5.dp)).padding(7.dp),
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
    viewModel: AllDownloadViewModel,
    version: PlatformVersion?,
    project: String,
    name: String,
    takenNames: Set<String>,
    onName: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val taken = name.trim() in takenNames
    val bad = name.isNotEmpty() && (name.any { it in INSTALL_NAME_BAD_CHARS } || name.trimEnd().endsWith("."))
    com.movtery.zalithlauncher.all.ui.AllDialog(
        title = "",
        onDismissRequest = onDismiss,
        actions = {
            AllButton(stringResource(R.string.generic_cancel), onDismiss, compact = true)
            AllButton(stringResource(R.string.all_install), onConfirm, style = AllButtonStyle.Success, compact = true, enabled = version != null && viewModel.isCompatible(version) && name.isNotBlank() && !taken && !bad)
        }
    ) {
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.foundation.Image(painterResource(R.drawable.img_old_grass_block), null, Modifier.size(30.dp))
                Text(project, color = DownloadText, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            ModpackPicker(viewModel)
            Text(stringResource(R.string.all_dl_install_name), color = DownloadMuted, fontSize = 11.sp)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(8.dp)).background(AllColors.Card)
                    .border(1.dp, if (taken || bad) DownloadDanger else dl(Color(0xFFB3C3DB), Color(0xFF3A5A8A)), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = name, onValueChange = onName, singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = DownloadText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(DownloadAccent), modifier = Modifier.weight(1f)
                )
            }
            Text(
                when { taken -> stringResource(R.string.all_dl_already_installed); bad -> stringResource(R.string.all_dl_name_invalid); else -> stringResource(R.string.all_dl_new_instance_name) },
                color = if (taken || bad) DownloadDanger else DownloadMuted, fontSize = 11.sp
            )
            Text(stringResource(R.string.download_modpack_warning1), color = DownloadMuted, fontSize = 11.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
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
            .background(dl(Color(0xFFEAF2FA), Color(0xFF1F3354)))
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

/** Modpack install choices: Minecraft version and loader (only what the pack offers), then the pack version that fits them */
@Composable
private fun ModpackPicker(viewModel: AllDownloadViewModel) {
    val state = viewModel.state
    val cs = viewModel.current()
    //one row: Minecraft | loader | pack version, so the name field below stays on screen
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val known = state.gameVersions.map { it.version.id }
        val mcs = remember(cs.versions, known) { cs.versions.flatMap { it.platformGameVersion().toList() }.distinct().sortedBy { known.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } } }
        val loaders = remember(cs.versions) {
            val all = cs.versions.flatMap { v -> v.platformLoaders().map { it.toString().lowercase() } }.toSet()
            listOf("fabric" to "Fabric", "quilt" to "Quilt", "forge" to "Forge", "neoforge" to "NeoForge").filter { it.first in all }.map { it.second }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Minecraft", color = DownloadMuted, fontSize = 11.sp)
            var open by remember { mutableStateOf(false) }
            Box {
                DlField(Modifier.fillMaxWidth(), enabled = mcs.isNotEmpty(), onClick = { open = true }) {
                    Text(state.modpackMc ?: stringResource(R.string.all_dl_any_mc), color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    val any = stringResource(R.string.all_dl_any_mc)
                    (listOf<String?>(null) + mcs).forEach { mc -> DropdownMenuItem(text = { Text(mc ?: any) }, onClick = { open = false; viewModel.setModpackFilter(mc, state.modpackLoader) }) }
                }
            }
        }
        if (loaders.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.all_dl_loader_short), color = DownloadMuted, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                loaders.forEach { l ->
                    val on = loaders.size == 1 || state.modpackLoader.equals(l, true)
                    Text(
                        l, color = if (on) dl(Color(0xFF1758AF), Color(0xFF9CC4FF)) else DownloadText, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (on) dl(Color(0xFFDDEAFE), Color(0xFF24406B)) else AllColors.Card)
                            .border(1.dp, if (on) dl(Color(0xFF3A80DC), Color(0xFF6FA8FF)) else dl(Color(0xFFDAD6C8), Color(0xFF34415A)), RoundedCornerShape(8.dp))
                            .clickable(enabled = loaders.size > 1) { viewModel.setModpackFilter(state.modpackMc, if (state.modpackLoader.equals(l, true)) null else l) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.all_dl_pack_version), color = DownloadMuted, fontSize = 11.sp)
            var open by remember { mutableStateOf(false) }
            val fits = cs.versions.take(150).filter { viewModel.isCompatible(it) }
            val rec = remember(fits) { recommendedOf(fits) { true } }
            Box {
                DlField(Modifier.fillMaxWidth(), enabled = fits.isNotEmpty(), onClick = { open = true }) {
                    Text(cs.selectedVersion?.let { it.platformVersion().ifBlank { it.platformDisplayName() } } ?: "-", color = DownloadText, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (cs.selectedVersion != null && cs.selectedVersion?.platformId() == rec?.platformId()) Pill(stringResource(R.string.all_dl_loader_recommended), PillGreen.first, PillGreen.second)
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    fits.forEach { v -> DropdownMenuItem(text = { Text(v.platformVersion().ifBlank { v.platformDisplayName() } + "  ·  " + v.platformGameVersion().joinToString()) }, onClick = { open = false; viewModel.selectVersion(v) }) }
                }
            }
        }
    }
}

/** Plain Thai for a raw platform/HTTP error; the raw text stays visible in [ErrorDetail] and in logcat for bug reports */
@Composable
private fun friendlyError(raw: String): String = stringResource(
    when {
        Regex("""\b404\b""").containsMatchIn(raw) -> R.string.all_dl_err_not_found
        Regex("""\b(401|403)\b""").containsMatchIn(raw) -> R.string.all_dl_err_denied
        Regex("""\b429\b""").containsMatchIn(raw) -> R.string.all_dl_err_rate
        Regex("""\b5\d\d\b""").containsMatchIn(raw) -> R.string.all_dl_err_server
        Regex("(?i)unable to resolve|unknownhost|timeout|timed out|connect|network|socket").containsMatchIn(raw) -> R.string.all_dl_err_network
        else -> R.string.all_dl_err_generic
    }
)

@Composable
private fun ErrorDetail(raw: String) {
    androidx.compose.runtime.LaunchedEffect(raw) { android.util.Log.w("ALLDownload", raw) }
    Text(raw, color = DownloadMuted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
}
