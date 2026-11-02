package com.movtery.zalithlauncher.ui.screens.content

import com.movtery.zalithlauncher.all.ui.AllBrushes

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import com.movtery.zalithlauncher.all.bridge.AllBackStackViewModel
import com.movtery.zalithlauncher.all.bridge.AllDeleteVersionDialog
import com.movtery.zalithlauncher.all.download.AllDownloadCategory
import com.movtery.zalithlauncher.all.download.AllDownloadWindow
import com.movtery.zalithlauncher.all.files.AllFilesWindow
import com.movtery.zalithlauncher.all.settings.AllSettingsWindow
import com.movtery.zalithlauncher.all.about.AllAboutWindow
import com.movtery.zalithlauncher.all.accounts.AllAccountsWindow
import com.movtery.zalithlauncher.all.instances.AllInstancesWindow
import com.movtery.zalithlauncher.all.overlays.AllTasksWindow
import com.movtery.zalithlauncher.all.ui.AllApp
import com.movtery.zalithlauncher.all.ui.AllAppEnv
import com.movtery.zalithlauncher.all.ui.AllAppRegistry
import com.movtery.zalithlauncher.all.ui.AllDesktopIcon
import com.movtery.zalithlauncher.all.ui.AllTaskChip
import com.movtery.zalithlauncher.all.ui.AllTaskbar
import com.movtery.zalithlauncher.all.ui.allDesktopIconCell
import com.movtery.zalithlauncher.all.ui.AllMotion
import com.movtery.zalithlauncher.all.ui.LocalAllCompact
import com.movtery.zalithlauncher.all.ui.LocalAllWindowFocused
import com.movtery.zalithlauncher.all.ui.AllStartEntry
import com.movtery.zalithlauncher.all.ui.AllStartMenu
import com.movtery.zalithlauncher.all.ui.AllStartSection
import com.movtery.zalithlauncher.all.ui.AllWindow
import com.movtery.zalithlauncher.all.overlays.AllWindowRequests
import com.movtery.zalithlauncher.all.launchersettings.AllLauncherSettingsWindow
import com.movtery.zalithlauncher.all.mods.AllModsWindow
import com.movtery.zalithlauncher.all.resourcepacks.AllResourcePacksWindow
import com.movtery.zalithlauncher.all.screenshots.AllScreenshotsWindow
import com.movtery.zalithlauncher.all.servers.AllServersWindow
import com.movtery.zalithlauncher.all.shaders.AllShadersWindow
import com.movtery.zalithlauncher.all.worlds.AllWorldsWindow
import com.movtery.zalithlauncher.all.bridge.AllSubmitError
import com.movtery.zalithlauncher.all.bridge.AllTaskDialog
import com.movtery.zalithlauncher.all.bridge.AllToast
import com.movtery.zalithlauncher.all.bridge.AllVersionIcon
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.movtery.zalithlauncher.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.FlowRow
import com.movtery.zalithlauncher.all.bridge.allHasCustomIcon
import com.movtery.zalithlauncher.all.bridge.allResetVersionIcon
import com.movtery.zalithlauncher.all.bridge.allSetVersionIcon
import com.movtery.zalithlauncher.all.ui.AllDimens
import com.movtery.zalithlauncher.all.ui.AllColors
import com.movtery.zalithlauncher.all.ui.AllTheme
import com.movtery.zalithlauncher.all.ui.AllButtonStyle
import com.movtery.zalithlauncher.all.ui.AllButton
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.path.getGameHome
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.InterfaceMode
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val XpBlue: Brush get() = AllBrushes.Title
private val XpGreen: Brush get() = AllBrushes.Green
private val XpGreenPressed = Brush.verticalGradient(listOf(Color(0xFF2E8B2E), Color(0xFF1F6B1F)))
private val XpTaskbar: Brush get() = AllBrushes.Taskbar
private val XpWindowBody: Color get() = AllColors.WindowBody
private val XpText: Color get() = AllColors.Text
private val LabelShadow = Shadow(Color(0xCC000000), Offset(1f, 1.5f), 3f)

/**
 * ALL Desktop：经典主屏幕之外的另一种外观，实例像桌面快捷方式。
 * 只负责显示与导航，启动、账号、版本全部走与经典界面相同的现有路径。
 */
@Composable
fun AllDesktopScreen(
    backStackViewModel: AllBackStackViewModel,
    onLaunchGame: (Version?) -> Unit,
    onImportModpack: (android.net.Uri) -> Unit,
    onCheckVulkan: (Version) -> Unit,
    showToast: (AllToast) -> Unit,
    submitError: AllSubmitError,
) {
    registerAllApps
    val versions by VersionsManager.versions.collectAsStateWithLifecycle()
    val currentVersion by VersionsManager.currentVersion.collectAsStateWithLifecycle()
    val account by AccountsManager.currentAccountFlow.collectAsStateWithLifecycle()
    val tasks by TaskSystem.tasksFlow.collectAsStateWithLifecycle()

    var importWarning by remember { mutableStateOf(false) }
    val packPicker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri -> uri?.let(onImportModpack) }
    if (importWarning) com.movtery.zalithlauncher.all.overlays.AllAlertDialog(
        title = stringResource(R.string.generic_tip), text = stringResource(R.string.import_modpack_tip) + "\n\n" +
            com.movtery.zalithlauncher.game.download.modpack.platform.PackPlatform.entries.joinToString { it.identifier } + "\n\n" +
            stringResource(R.string.download_modpack_warning1) + "\n\n" + stringResource(R.string.download_modpack_warning2) + "\n\n" +
            stringResource(R.string.download_modpack_warning3),
        onConfirm = { importWarning = false; packPicker.launch("*/*") }, onDismiss = { importWarning = false })
    var openVersion by remember { mutableStateOf<Version?>(null) }
    val renameScope = androidx.compose.runtime.rememberCoroutineScope()
    var minimized by remember { mutableStateOf(false) }
    var startOpen by remember { mutableStateOf(false) }
    var wallpaperOpen by remember { mutableStateOf(false) }
    var uiScaleOpen by remember { mutableStateOf(false) }
    var settingsVersion by remember { mutableStateOf<Version?>(null) }
    var settingsMinimized by remember { mutableStateOf(false) }
    var modsVersion by remember { mutableStateOf<Version?>(null) }
    var modsMinimized by remember { mutableStateOf(false) }
    var packsVersion by remember { mutableStateOf<Version?>(null) }
    var packsMinimized by remember { mutableStateOf(false) }
    //Shaders / Worlds / Screenshots / Servers 共用的实例级浮动窗口：id -> 实例；最小化状态按 id 记录
    val extras = remember { androidx.compose.runtime.mutableStateMapOf<String, Version>() }
    val extraMin = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }
    val extraShown = remember { androidx.compose.runtime.mutableStateMapOf<String, Version>() }
    //registry apps (AllAppRegistry): window open / minimized flags by app id
    val appOpen = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }
    val appMin = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }
    var instOpen by remember { mutableStateOf(false) }
    var instMinimized by remember { mutableStateOf(false) }
    var lsOpen by remember { mutableStateOf(false) }
    var lsMinimized by remember { mutableStateOf(false) }
    var accountsOpen by remember { mutableStateOf(false) }
    var accountsMinimized by remember { mutableStateOf(false) }
    var filesVersion by remember { mutableStateOf<Version?>(null) }
    var filesMinimized by remember { mutableStateOf(false) }
    var downloadOpen by remember { mutableStateOf(false) }
    var downloadMinimized by remember { mutableStateOf(false) }
    var downloadInitialCategory by remember { mutableStateOf(AllDownloadCategory.GAME) }
    var downloadTargetName by remember { mutableStateOf<String?>(null) }
    //关闭时递增：重新打开 Download 会清掉旧搜索；最小化不改变它
    var downloadSession by remember { mutableIntStateOf(0) }
    //开始菜单的全局工具先选实例：MODS / FILES，null = 不显示选择器
    var pickerFor by remember { mutableStateOf<String?>(null) }
    val windowStack = remember { com.movtery.zalithlauncher.all.ui.AllWindowStack(WINDOW_INSTANCE) }
    var front by windowStack::front
    var deleteTarget by remember { mutableStateOf<Version?>(null) }
    var deleteTask by remember { mutableStateOf<Pair<Int, suspend () -> Unit>?>(null) }
    //窗口拖动位置放在窗口外：最小化会把窗口内容移出组合，位置放里面会在还原时丢失。关闭后/换实例再打开时回到居中
    val winOffsets = remember { mutableMapOf<String, androidx.compose.runtime.MutableState<IntOffset>>() }
    fun offsetOf(id: String) = winOffsets.getOrPut(id) { mutableStateOf(IntOffset.Zero) }

    //下载窗口由 ALL 自己的状态驱动；category 为 null 时保持上次的分类
    //targetName 来自实例窗口（如 Mods）时，安装默认指向那个实例；其余入口回到当前启用的实例
    fun openDownloadWindow(category: AllDownloadCategory?, targetName: String? = null) {
        if (!downloadOpen) offsetOf(WINDOW_DOWNLOAD).value = IntOffset.Zero
        category?.let { downloadInitialCategory = it }
        downloadTargetName = targetName
        downloadOpen = true
        downloadMinimized = false
        front = WINDOW_DOWNLOAD
    }
    val openFilesWindow: (Version) -> Unit = { version ->
        if (filesVersion != version) offsetOf(WINDOW_FILES).value = IntOffset.Zero
        filesVersion = version
        filesMinimized = false
        front = WINDOW_FILES
    }
    val openModsWindow: (Version) -> Unit = { version ->
        if (modsVersion != version) offsetOf(WINDOW_MODS).value = IntOffset.Zero
        modsVersion = version
        modsMinimized = false
        front = WINDOW_MODS
    }

    val openPacksWindow: (Version) -> Unit = { version ->
        if (packsVersion != version) offsetOf(WINDOW_PACKS).value = IntOffset.Zero
        packsVersion = version
        packsMinimized = false
        front = WINDOW_PACKS
    }
    val openExtraWindow: (String, Version) -> Unit = { id, version ->
        if (extras[id] != version) offsetOf(id).value = IntOffset.Zero
        extras[id] = version
        extraMin[id] = false
        front = id
    }
    val openApp: (String) -> Unit = { id ->
        if (appOpen[id] != true) offsetOf(id).value = IntOffset.Zero
        appOpen[id] = true
        appMin[id] = false
        front = id
    }
    val openInstancesWindow: () -> Unit = {
        if (!instOpen) offsetOf(WINDOW_INSTANCES).value = IntOffset.Zero
        instOpen = true
        instMinimized = false
        front = WINDOW_INSTANCES
    }
    //which tab the launcher-settings window shows when it is opened (0 = first); the "Touch controls" shortcut opens the Controls tab
    var lsTab by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val openLauncherSettingsWindow: (Int) -> Unit = { tab ->
        lsTab = tab
        if (!lsOpen) offsetOf(WINDOW_LSETTINGS).value = IntOffset.Zero
        lsOpen = true
        lsMinimized = false
        front = WINDOW_LSETTINGS
    }
    val openAccountsWindow: () -> Unit = {
        if (!accountsOpen) offsetOf(WINDOW_ACCOUNTS).value = IntOffset.Zero
        accountsOpen = true
        accountsMinimized = false
        front = WINDOW_ACCOUNTS
    }

    //pre-launch flows (no account / no version) ask the desktop to open the matching ALL window
    LaunchedEffect(Unit) {
        AllWindowRequests.flow.collect { w ->
            when (w) {
                AllWindowRequests.ACCOUNTS -> openAccountsWindow()
                AllWindowRequests.INSTANCES -> openInstancesWindow()
            }
        }
    }

    val shown = mapOf(
        WINDOW_INSTANCE to (openVersion != null && !minimized),
        WINDOW_SETTINGS to (settingsVersion != null && !settingsMinimized),
        WINDOW_MODS to (modsVersion != null && !modsMinimized),
        WINDOW_PACKS to (packsVersion != null && !packsMinimized),
        WINDOW_FILES to (filesVersion != null && !filesMinimized),
        WINDOW_DOWNLOAD to (downloadOpen && !downloadMinimized),
        WINDOW_ACCOUNTS to (accountsOpen && !accountsMinimized),
        WINDOW_LSETTINGS to (lsOpen && !lsMinimized),
        WINDOW_INSTANCES to (instOpen && !instMinimized)
    ) + EXTRA_WINDOWS.associate { it.id to (extras[it.id] != null && extraMin[it.id] != true) } +
        AllAppRegistry.all().associate { it.id to (appOpen[it.id] == true && appMin[it.id] != true) }
    //实际显示在最上面的窗口：焦点窗口可见就是它，否则按固定顺序取最后一个可见的
    val topShown = windowStack.order.lastOrNull { shown[it] == true }
        ?: listOf(WINDOW_INSTANCE, WINDOW_SETTINGS, WINDOW_MODS, WINDOW_FILES, WINDOW_DOWNLOAD, WINDOW_PACKS, WINDOW_ACCOUNTS, WINDOW_LSETTINGS, WINDOW_INSTANCES, *EXTRA_WINDOWS.map { it.id }.toTypedArray(), *AllAppRegistry.all().map { it.id }.toTypedArray()).lastOrNull { shown[it] == true }

    //桌面主栈只有 LauncherMain，返回键会直接结束 Activity；有开始菜单/窗口时先收起它们。
    //注册在窗口内容之前，窗口内的嵌套导航（如下载子页）仍优先处理返回
    BackHandler(enabled = startOpen || topShown != null) {
        when {
            startOpen -> startOpen = false
            topShown == WINDOW_INSTANCE -> minimized = true
            topShown == WINDOW_SETTINGS -> settingsMinimized = true
            topShown == WINDOW_MODS -> modsMinimized = true
            topShown == WINDOW_PACKS -> packsMinimized = true
            topShown == WINDOW_ACCOUNTS -> accountsMinimized = true
            topShown == WINDOW_LSETTINGS -> lsMinimized = true
            topShown == WINDOW_INSTANCES -> instMinimized = true
            topShown != null && AllAppRegistry.find(topShown) != null -> appMin[topShown] = true
            topShown != null && EXTRA_WINDOWS.any { it.id == topShown } -> extraMin[topShown] = true
            topShown == WINDOW_FILES -> filesMinimized = true
            topShown == WINDOW_DOWNLOAD -> downloadMinimized = true
        }
    }

    //nothing open: Back no longer ends the app on the first press; a second press within 2 s does
    val backContext = androidx.compose.ui.platform.LocalContext.current
    val backAgainText = stringResource(R.string.all_desktop_back_again)
    var lastBackAt by remember { mutableStateOf(0L) }
    BackHandler(enabled = !startOpen && topShown == null) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastBackAt < 2000L) {
            //LocalContext may be a ContextWrapper around the Activity
            var c: android.content.Context? = backContext
            while (c is android.content.ContextWrapper && c !is android.app.Activity) c = c.baseContext
            (c as? android.app.Activity)?.finish()
        }
        else { lastBackAt = now; android.widget.Toast.makeText(backContext, backAgainText, android.widget.Toast.LENGTH_SHORT).show() }
    }

    val uiScale = AllSettings.allDesktopUiScale.state / 100f
    val baseDensity = LocalDensity.current
    val cfg = androidx.compose.ui.platform.LocalConfiguration.current
    CompositionLocalProvider(
        LocalAllCompact provides (cfg.screenWidthDp / uiScale < 600f || cfg.screenHeightDp / uiScale < 400f),
        LocalDensity provides Density(
            density = baseDensity.density * uiScale,
            fontScale = baseDensity.fontScale
        )
    ) {
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(
        primary = com.movtery.zalithlauncher.all.ui.AllColors.Blue, onPrimary = Color.White,
        primaryContainer = com.movtery.zalithlauncher.all.ui.AllColors.BluePale, onPrimaryContainer = com.movtery.zalithlauncher.all.ui.AllColors.Blue,
        surface = com.movtery.zalithlauncher.all.ui.AllColors.WindowBody, onSurface = com.movtery.zalithlauncher.all.ui.AllColors.Text,
        surfaceContainer = Color.White, surfaceContainerHigh = Color.White, outline = com.movtery.zalithlauncher.all.ui.AllColors.Border
    )) {
    Column(modifier = Modifier.fillMaxSize()) {
        var uiCompact by remember { mutableStateOf(false) }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            uiCompact = maxWidth < 600.dp
            Wallpaper()

            if (versions.isEmpty()) {
                Text(
                    text = stringResource(R.string.all_desktop_no_instances),
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color(0x99000000), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                )
            }

            val iconCell = allDesktopIconCell(maxWidth)
            LazyHorizontalGrid(
                rows = GridCells.FixedSize(iconCell.second + 46.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(versions, key = { it.getVersionName() }) { version ->
                    AllDesktopIcon(
                        label = version.getVersionName(), selected = version == openVersion,
                        cellWidth = iconCell.first, iconSize = iconCell.second,
                        icon = { m -> AllVersionIcon(version = version, modifier = m) }
                    ) {
                        if (openVersion != version) offsetOf(WINDOW_INSTANCE).value = IntOffset.Zero
                        openVersion = version
                        minimized = false
                        startOpen = false
                        front = WINDOW_INSTANCE
                    }
                }
            }

            val area = IntSize(constraints.maxWidth, constraints.maxHeight)
            //最小化/关闭时保留上一次的实例，让退场动画有内容可画
            var shownVersion by remember { mutableStateOf<Version?>(null) }
            if (openVersion != null) shownVersion = openVersion
            val instanceLayer: @Composable () -> Unit = {
                androidx.compose.animation.AnimatedVisibility(
                    visible = openVersion != null && !minimized,
                    enter = AllMotion.windowEnter,
                    exit = AllMotion.windowExit,
                    modifier = Modifier.fillMaxSize()
                ) {
                    shownVersion?.let { version ->
                        InstanceWindow(
                            version = version,
                            accountName = account?.username,
                            area = area,
                            offsetState = offsetOf(WINDOW_INSTANCE),
                            onClose = { openVersion = null },
                            onMinimize = { minimized = true },
                            onPlay = { onLaunchGame(version) },
                            //在桌面上打开浮动模组窗口，不离开桌面；经典界面仍走原来的整页模组管理
                            onMods = { openModsWindow(version) },
                            //在桌面上打开浮动设置窗口，不离开桌面；经典界面仍走原来的整页设置
                            onSettings = {
                                if (settingsVersion != version) offsetOf(WINDOW_SETTINGS).value = IntOffset.Zero
                                settingsVersion = version
                                settingsMinimized = false
                                front = WINDOW_SETTINGS
                            },
                            //在桌面上打开浮动文件窗口，根目录就是这个实例的游戏目录；经典界面仍打开原来的文件管理器
                            onFiles = { openFilesWindow(version) },
                            onResourcePacks = { openPacksWindow(version) },
                            onExtra = { id -> openExtraWindow(id, version) },
                            onDelete = { deleteTarget = version },
                            onIconError = submitError,
                            onRename = { newName ->
                                val old = version
                                renameScope.launch {
                                    withContext(kotlinx.coroutines.Dispatchers.IO) { VersionsManager.renameVersion(old, newName) }
                                    val renamed = kotlinx.coroutines.withTimeoutOrNull(8000) {
                                        VersionsManager.versions.first { l -> l.any { it.getVersionName() == newName } }
                                    }?.firstOrNull { it.getVersionName() == newName }
                                    //windows hold the old Version object: close them, reopen the instance window on the renamed one
                                    if (settingsVersion == old) settingsVersion = null
                                    if (modsVersion == old) modsVersion = null
                                    if (packsVersion == old) packsVersion = null
                                    if (filesVersion == old) filesVersion = null
                                    EXTRA_WINDOWS.forEach { w -> if (extras[w.id] == old) extras.remove(w.id) }
                                    if (openVersion == old) openVersion = renamed
                                }
                            },
                            onFocus = { front = WINDOW_INSTANCE }
                        )
                    }
                }
            }
            var shownSettings by remember { mutableStateOf<Version?>(null) }
            if (settingsVersion != null) shownSettings = settingsVersion
            val settingsLayer: @Composable () -> Unit = {
                androidx.compose.animation.AnimatedVisibility(
                    visible = settingsVersion != null && !settingsMinimized,
                    enter = AllMotion.windowEnter,
                    exit = AllMotion.windowExit,
                    modifier = Modifier.fillMaxSize()
                ) {
                    shownSettings?.let { version ->
                        //key(version)：换实例时整窗重建，读的是这个实例自己的配置
                        androidx.compose.runtime.key(version) {
                            InstanceSettingsWindow(
                                version = version,
                                area = area,
                                offsetState = offsetOf(WINDOW_SETTINGS),
                                onClose = { settingsVersion = null },
                                onMinimize = { settingsMinimized = true },
                                onFocus = { front = WINDOW_SETTINGS },
                                onCheckVulkan = onCheckVulkan,
                                showToast = showToast,
                                submitError = submitError
                            )
                        }
                    }
                }
            }
            var shownMods by remember { mutableStateOf<Version?>(null) }
            if (modsVersion != null) shownMods = modsVersion
            val modsLayer: @Composable () -> Unit = {
                androidx.compose.animation.AnimatedVisibility(
                    visible = modsVersion != null && !modsMinimized,
                    enter = AllMotion.windowEnter,
                    exit = AllMotion.windowExit,
                    modifier = Modifier.fillMaxSize()
                ) {
                    shownMods?.let { version ->
                        //key(version)：换实例时整窗重建，列表来自这个实例自己的 mods 目录
                        androidx.compose.runtime.key(version) {
                            ModsWindow(
                                version = version,
                                area = area,
                                offsetState = offsetOf(WINDOW_MODS),
                                onClose = { modsVersion = null },
                                onMinimize = { modsMinimized = true },
                                onFocus = { front = WINDOW_MODS },
                                swapToDownload = { openDownloadWindow(AllDownloadCategory.MOD, version.getVersionName()) },
                                submitError = submitError
                            )
                        }
                    }
                }
            }
            var shownFiles by remember { mutableStateOf<Version?>(null) }
            if (filesVersion != null) shownFiles = filesVersion
            val filesLayer: @Composable () -> Unit = {
                androidx.compose.animation.AnimatedVisibility(
                    visible = filesVersion != null && !filesMinimized,
                    enter = AllMotion.windowEnter,
                    exit = AllMotion.windowExit,
                    modifier = Modifier.fillMaxSize()
                ) {
                    shownFiles?.let { version ->
                        androidx.compose.runtime.key(version) {
                            FilesWindow(
                                version = version,
                                area = area,
                                offsetState = offsetOf(WINDOW_FILES),
                                onClose = { filesVersion = null },
                                onMinimize = { filesMinimized = true },
                                onFocus = { front = WINDOW_FILES }
                            )
                        }
                    }
                }
            }
            val downloadLayer: @Composable () -> Unit = {
                androidx.compose.animation.AnimatedVisibility(
                    visible = downloadOpen && !downloadMinimized,
                    enter = AllMotion.windowEnter,
                    exit = AllMotion.windowExit,
                    modifier = Modifier.fillMaxSize()
                ) {
                    DownloadWindow(
                        area = area,
                        offsetState = offsetOf(WINDOW_DOWNLOAD),
                        initialCategory = downloadInitialCategory,
                        targetName = downloadTargetName,
                        session = downloadSession,
                        onClose = { downloadOpen = false; downloadSession++ },
                        onMinimize = { downloadMinimized = true },
                        onFocus = { front = WINDOW_DOWNLOAD },
                        submitError = submitError
                    )
                }
            }
            //获得焦点的窗口画在最上面，其余按固定顺序在下面
            var shownPacks by remember { mutableStateOf<Version?>(null) }
            if (packsVersion != null) shownPacks = packsVersion
            val packsLayer: @Composable () -> Unit = {
                androidx.compose.animation.AnimatedVisibility(
                    visible = packsVersion != null && !packsMinimized,
                    enter = AllMotion.windowEnter,
                    exit = AllMotion.windowExit,
                    modifier = Modifier.fillMaxSize()
                ) {
                    shownPacks?.let { version ->
                        androidx.compose.runtime.key(version) {
                            FloatingWindow(
                                title = "🎨 " + stringResource(R.string.resource_pack_manage) + " — " + version.getVersionName(),
                                area = area,
                                offsetState = offsetOf(WINDOW_PACKS),
                                dragKey = version,
                                onClose = { packsVersion = null },
                                onMinimize = { packsMinimized = true },
                                onFocus = { front = WINDOW_PACKS }
                            ) {
                                AllResourcePacksWindow(
                                    version = version,
                                    onOpenDownload = { openDownloadWindow(AllDownloadCategory.RESOURCE_PACK, version.getVersionName()) },
                                    onError = submitError,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
            val appLayers = AllAppRegistry.all().map { app ->
                app.id to @Composable {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = appOpen[app.id] == true && appMin[app.id] != true,
                        enter = AllMotion.windowEnter,
                        exit = AllMotion.windowExit,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        FloatingWindow(
                            title = app.emoji + " " + stringResource(app.titleRes),
                            area = area,
                            offsetState = offsetOf(app.id),
                            dragKey = app.id,
                            onClose = { appOpen[app.id] = false },
                            onMinimize = { appMin[app.id] = true },
                            onFocus = { front = app.id }
                        ) {
                            app.content(AllAppEnv(showToast, submitError))
                        }
                    }
                }
            }
            val instLayer: @Composable () -> Unit = {
                androidx.compose.animation.AnimatedVisibility(
                    visible = instOpen && !instMinimized,
                    enter = AllMotion.windowEnter,
                    exit = AllMotion.windowExit,
                    modifier = Modifier.fillMaxSize()
                ) {
                    FloatingWindow(
                        title = "\uD83D\uDCE6 " + stringResource(R.string.all_instances_title),
                        area = area,
                        offsetState = offsetOf(WINDOW_INSTANCES),
                        dragKey = WINDOW_INSTANCES,
                        onClose = { instOpen = false },
                        onMinimize = { instMinimized = true },
                        onFocus = { front = WINDOW_INSTANCES }
                    ) {
                        AllInstancesWindow(
                            onImport = { importWarning = true },
                            onOpenDownload = { openDownloadWindow(it) },
                            onError = submitError,
                            modifier = Modifier.fillMaxSize(),
                            onPlay = { onLaunchGame(it) },
                            onOpen = { v ->
                                if (openVersion != v) offsetOf(WINDOW_INSTANCE).value = IntOffset.Zero
                                openVersion = v
                                minimized = false
                                startOpen = false
                                front = WINDOW_INSTANCE
                            },
                            onMods = openModsWindow,
                        )
                    }
                }
            }
            val lsLayer: @Composable () -> Unit = {
                androidx.compose.animation.AnimatedVisibility(
                    visible = lsOpen && !lsMinimized,
                    enter = AllMotion.windowEnter,
                    exit = AllMotion.windowExit,
                    modifier = Modifier.fillMaxSize()
                ) {
                    FloatingWindow(
                        title = "\u2699 " + stringResource(R.string.all_settings_launcher_title),
                        area = area,
                        offsetState = offsetOf(WINDOW_LSETTINGS),
                        dragKey = WINDOW_LSETTINGS,
                        onClose = { lsOpen = false },
                        onMinimize = { lsMinimized = true },
                        onFocus = { front = WINDOW_LSETTINGS }
                    ) {
                        AllLauncherSettingsWindow(onError = submitError, modifier = Modifier.fillMaxSize(), initialTab = lsTab)
                    }
                }
            }
            val accountsLayer: @Composable () -> Unit = {
                androidx.compose.animation.AnimatedVisibility(
                    visible = accountsOpen && !accountsMinimized,
                    enter = AllMotion.windowEnter,
                    exit = AllMotion.windowExit,
                    modifier = Modifier.fillMaxSize()
                ) {
                    FloatingWindow(
                        title = "\uD83D\uDC64 " + stringResource(R.string.all_desktop_account),
                        area = area,
                        offsetState = offsetOf(WINDOW_ACCOUNTS),
                        dragKey = WINDOW_ACCOUNTS,
                        onClose = { accountsOpen = false },
                        onMinimize = { accountsMinimized = true },
                        onFocus = { front = WINDOW_ACCOUNTS },
                        maxWidth = Dp.Infinity
                    ) {
                        AllAccountsWindow(modifier = Modifier.fillMaxSize(), submitError = submitError)
                    }
                }
            }
            val extraLayers = EXTRA_WINDOWS.map { w ->
                w.id to @Composable {
                    extras[w.id]?.let { extraShown[w.id] = it }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = extras[w.id] != null && extraMin[w.id] != true,
                        enter = AllMotion.windowEnter,
                        exit = AllMotion.windowExit,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        extraShown[w.id]?.let { version ->
                            androidx.compose.runtime.key(version) {
                                FloatingWindow(
                                    title = w.emoji + " " + stringResource(w.titleRes) + " \u2014 " + version.getVersionName(),
                                    area = area,
                                    offsetState = offsetOf(w.id),
                                    dragKey = version,
                                    onClose = { extras.remove(w.id) },
                                    onMinimize = { extraMin[w.id] = true },
                                    onFocus = { front = w.id }
                                ) {
                                    ExtraWindowContent(
                                        id = w.id,
                                        version = version,
                                        onOpenDownload = { c -> openDownloadWindow(c, version.getVersionName()) },
                                        submitError = submitError
                                    )
                                }
                            }
                        }
                    }
                }
            }
            val layers = listOf(WINDOW_INSTANCE to instanceLayer, WINDOW_SETTINGS to settingsLayer, WINDOW_MODS to modsLayer, WINDOW_FILES to filesLayer, WINDOW_DOWNLOAD to downloadLayer, WINDOW_PACKS to packsLayer, WINDOW_ACCOUNTS to accountsLayer, WINDOW_LSETTINGS to lsLayer, WINDOW_INSTANCES to instLayer) + extraLayers + appLayers
            //同一个调用点 + key(id)：换焦点时 Compose 只移动窗口，不会重建（否则位置和窗口内状态会丢）
            //最近聚焦的在上：按聚焦栈排序（从未聚焦过的排最下面），key(id) 保证只移动不重建
            layers.sortedBy { windowStack.rank(it.first) }.forEach { (id, layer) ->
                androidx.compose.runtime.key(id) { CompositionLocalProvider(LocalAllWindowFocused provides (topShown == id)) { layer() } }
            }

            if (startOpen) {
                // 点空白处关闭开始菜单
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(indication = null, interactionSource = null) { startOpen = false }
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = startOpen,
                enter = fadeIn(tween(120)) + slideInVertically(tween(160)) { it / 4 },
                exit = fadeOut(tween(100)) + slideOutVertically(tween(120)) { it / 4 },
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                val startEntries = run {
                    val pinned = AllStartSection.PINNED; val system = AllStartSection.SYSTEM
                    fun e(id: String, label: String, icon: Int, sec: AllStartSection, action: () -> Unit) = AllStartEntry(id, label, icon, sec, action)
                    listOfNotNull(
                        e("home", stringResource(R.string.all_desktop_home), R.drawable.ic_home_filled, pinned) { openVersion = null },
                        e("download", stringResource(R.string.generic_download), R.drawable.ic_download, pinned) { openDownloadWindow(null) },
                        if (versions.isNotEmpty()) e("mods", stringResource(R.string.all_desktop_mods), R.drawable.ic_extension_outlined, AllStartSection.TOOLS) { pickerFor = WINDOW_MODS } else null,
                        e("files", stringResource(R.string.all_desktop_files), R.drawable.ic_folder_outlined, AllStartSection.TOOLS) { pickerFor = WINDOW_FILES },
                        e("instances", stringResource(R.string.all_instances_title), R.drawable.ic_dashboard_outlined, pinned) { openInstancesWindow() },
                        e("accounts", stringResource(R.string.all_desktop_account), R.drawable.ic_person_outlined, system) { openAccountsWindow() },
                        e("settings", stringResource(R.string.generic_setting), R.drawable.ic_settings_filled, system) { openLauncherSettingsWindow(0) },
                        e("controls", stringResource(R.string.all_settings_tab_controls), R.drawable.ic_touch_app_filled, system) { openLauncherSettingsWindow(4) },
                        e("wallpaper", stringResource(R.string.all_desktop_wallpaper), R.drawable.ic_build_outlined, system) { wallpaperOpen = true },
                        e("uiscale", stringResource(R.string.all_desktop_ui_scale), R.drawable.ic_text_format, system) { uiScaleOpen = true },
                    ) + AllAppRegistry.all().filter { it.inStartMenu }.map { app ->
                        e("app:" + app.id, stringResource(app.titleRes), app.iconRes ?: R.drawable.ic_info_outlined, if (app.pinned) pinned else system) { openApp(app.id) }
                    }
                }
                AllStartMenu(
                    userName = account?.username ?: "ALL Launcher",
                    entries = startEntries,
                    maxMenuHeight = maxHeight,
                    onClose = { startOpen = false }
                )
            }

            if (wallpaperOpen) WallpaperDialog(onDismiss = { wallpaperOpen = false })
            if (uiScaleOpen) UiScaleDialog(onDismiss = { uiScaleOpen = false })

            pickerFor?.let { target ->
                InstancePicker(
                    versions = versions,
                    icon = if (target == WINDOW_MODS) "🧩" else "📁",
                    onDismiss = { pickerFor = null },
                    onPick = { version ->
                        pickerFor = null
                        if (target == WINDOW_MODS) openModsWindow(version) else openFilesWindow(version)
                    }
                )
            }

            //删除复用经典界面的确认对话框与任务执行方式，不另写删除逻辑
            deleteTarget?.let { target ->
                AllDeleteVersionDialog(
                    version = target,
                    onDismissRequest = { deleteTarget = null },
                    onConfirm = { title, task ->
                        deleteTarget = null
                        deleteTask = title to task
                    },
                    onVersionDeleted = {
                        if (openVersion == target) {
                            openVersion = null
                            minimized = false
                        }
                        if (settingsVersion == target) settingsVersion = null
                        if (modsVersion == target) modsVersion = null
                        if (packsVersion == target) packsVersion = null
                        EXTRA_WINDOWS.forEach { w -> if (extras[w.id] == target) extras.remove(w.id) }
                        if (filesVersion == target) filesVersion = null
                    }
                )
            }
            deleteTask?.let { (title, task) ->
                AllTaskDialog(
                    title = stringResource(title),
                    task = task,
                    onDismiss = { deleteTask = null }
                )
            }
        }

        AllTaskbar(
            compact = uiCompact,
            taskLabel = stringResource(R.string.all_desktop_tasks, tasks.size),
            startOpen = startOpen,
            onStart = { startOpen = !startOpen },
            chips = run {
                val downloadLabel = stringResource(R.string.generic_download)
                val accountsLabel = stringResource(R.string.all_desktop_account)
                val lsLabel = stringResource(R.string.all_settings_launcher_title)
                val instLabel = stringResource(R.string.all_instances_title)
                val appLabels = AllAppRegistry.all().map { it to stringResource(it.titleRes) }
                //任务栏按钮：窗口在最前 → 按下态，点一下最小化；在后面 → 点一下提到最前；已最小化 → 点一下还原
                fun chip(id: String, v: Version?, prefix: String, isMin: Boolean, setMin: (Boolean) -> Unit) = v?.let {
                    val isFront = topShown == id
                    AllTaskChip(prefix + it.getVersionName(), isFront, icon = { AllVersionIcon(version = it, modifier = Modifier.size(24.dp)) }) {
                        when {
                            isMin -> { setMin(false); front = id }
                            isFront -> setMin(true)
                            else -> front = id
                        }
                    }
                }
                listOfNotNull(
                    chip(WINDOW_INSTANCE, openVersion, "", minimized) { minimized = it },
                    chip(WINDOW_SETTINGS, settingsVersion, "⚙ ", settingsMinimized) { settingsMinimized = it },
                    chip(WINDOW_MODS, modsVersion, "🧩 ", modsMinimized) { modsMinimized = it },
                    chip(WINDOW_FILES, filesVersion, "📁 ", filesMinimized) { filesMinimized = it },
                    chip(WINDOW_PACKS, packsVersion, "🎨 ", packsMinimized) { packsMinimized = it },
                    *EXTRA_WINDOWS.map { w -> chip(w.id, extras[w.id], w.emoji + " ", extraMin[w.id] == true) { extraMin[w.id] = it } }.toTypedArray(),
                    if (instOpen) {
                        val isFront = topShown == WINDOW_INSTANCES
                        AllTaskChip("\uD83D\uDCE6 " + instLabel, isFront) {
                            when {
                                instMinimized -> { instMinimized = false; front = WINDOW_INSTANCES }
                                isFront -> instMinimized = true
                                else -> front = WINDOW_INSTANCES
                            }
                        }
                    } else null,
                    if (lsOpen) {
                        val isFront = topShown == WINDOW_LSETTINGS
                        AllTaskChip("\u2699 " + lsLabel, isFront) {
                            when {
                                lsMinimized -> { lsMinimized = false; front = WINDOW_LSETTINGS }
                                isFront -> lsMinimized = true
                                else -> front = WINDOW_LSETTINGS
                            }
                        }
                    } else null,
                    if (accountsOpen) {
                        val isFront = topShown == WINDOW_ACCOUNTS
                        AllTaskChip("\uD83D\uDC64 " + accountsLabel, isFront) {
                            when {
                                accountsMinimized -> { accountsMinimized = false; front = WINDOW_ACCOUNTS }
                                isFront -> accountsMinimized = true
                                else -> front = WINDOW_ACCOUNTS
                            }
                        }
                    } else null,
                    *appLabels.map { (app, label) ->
                        if (appOpen[app.id] == true) {
                            val isFront = topShown == app.id
                            AllTaskChip(app.emoji + " " + label, isFront) {
                                when {
                                    appMin[app.id] == true -> { appMin[app.id] = false; front = app.id }
                                    isFront -> appMin[app.id] = true
                                    else -> front = app.id
                                }
                            }
                        } else null
                    }.toTypedArray(),
                    null
                )
            },
            taskCount = tasks.size,
            onTasks = { openApp("tasks") },
            downloadLabel = stringResource(R.string.generic_download),
            downloadActive = downloadOpen && !downloadMinimized && topShown == WINDOW_DOWNLOAD,
            onDownload = {
                when {
                    !downloadOpen -> openDownloadWindow(null)
                    downloadMinimized -> { downloadMinimized = false; front = WINDOW_DOWNLOAD }
                    topShown == WINDOW_DOWNLOAD -> downloadMinimized = true
                    else -> front = WINDOW_DOWNLOAD
                }
            }
        )
    }
    }
    }
}

/**
 * 默认内置 All XP；用户选过图片时读持久授权的 Uri，读不到就自动回落到 All XP。
 * 绘制的天空+草坡垫在最底层，作为最后的兜底。
 */
@Composable
private fun Wallpaper() {
    val custom = AllSettings.allDesktopWallpaper.state
    //flat navy ground; a wallpaper the user picked still goes on top of it
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0E1626)))
    if (custom.isNotBlank()) {
        AsyncImage(
            model = Uri.parse(custom),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun WallpaperDialog(onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            //拿持久读取权限，重启后仍能读到；拿不到就不保存，避免下次读不到
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }.onSuccess { AllSettings.allDesktopWallpaper.save(uri.toString()) }
            onDismiss()
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x55000000))
            .clickable(indication = null, interactionSource = null, onClick = onDismiss)
    ) {
        XpWindowFrame(
            title = stringResource(R.string.all_desktop_wallpaper),
            onClose = onDismiss,
            modifier = Modifier.width(360.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                XpButton(
                    text = stringResource(R.string.all_desktop_wallpaper_default),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        AllSettings.allDesktopWallpaper.save("")
                        onDismiss()
                    }
                )
                XpButton(
                    text = stringResource(R.string.all_desktop_wallpaper_pick),
                    brush = XpGreen,
                    textColor = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { picker.launch(arrayOf("image/*")) }
                )
            }
        }
    }
}

@Composable
private fun UiScaleDialog(onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    val selectedScale = AllSettings.allDesktopUiScale.state
    val options = listOf(75, 90, 100, 110, 125, 150)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x55000000))
            .clickable(indication = null, interactionSource = null, onClick = onDismiss)
    ) {
        XpWindowFrame(
            title = stringResource(R.string.all_desktop_ui_scale),
            onClose = onDismiss,
            modifier = Modifier.width(360.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                options.chunked(3).forEach { rowOptions ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowOptions.forEach { percent ->
                            val selected = percent == selectedScale
                            XpButton(
                                text = "$percent%",
                                brush = if (selected) XpBlue else Brush.verticalGradient(listOf(Color.White, Color(0xFFE3E0D5))),
                                textColor = if (selected) Color.White else XpText,
                                modifier = Modifier.weight(1f),
                                onClick = { AllSettings.allDesktopUiScale.save(percent) }
                            )
                        }
                    }
                }
                Text(stringResource(R.string.all_desktop_ui_scale_hint), color = XpText)
            }
        }
    }
}

@Composable
private fun XpWindowFrame(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onMinimize: (() -> Unit)? = null,
    titleModifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    onFocus: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) = AllWindow(title, onClose, modifier, onMinimize, titleModifier, icon, onFocus, content)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun InstanceWindow(
    version: Version,
    accountName: String?,
    area: IntSize,
    offsetState: androidx.compose.runtime.MutableState<IntOffset>,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onPlay: () -> Unit,
    onMods: () -> Unit,
    onSettings: () -> Unit,
    onFiles: () -> Unit,
    onResourcePacks: () -> Unit,
    onExtra: (String) -> Unit,
    onDelete: () -> Unit,
    onIconError: AllSubmitError,
    onRename: (String) -> Unit,
    onFocus: () -> Unit = {},
) {
    var offset by offsetState
    var windowSize by remember { mutableStateOf(IntSize.Zero) }
    val info = version.getVersionInfo()
    val maxWidth = with(LocalDensity.current) { area.width.toDp() }
    val maxHeightDp = with(LocalDensity.current) { area.height.toDp() }
    val windowWidthDp = minOf(480.dp, maxWidth - 32.dp)
    var iconDialog by remember { mutableStateOf(false) }
    var renameDialog by remember { mutableStateOf(false) }
    if (renameDialog) com.movtery.zalithlauncher.all.instances.AllNameDialog(
        title = stringResource(R.string.versions_manage_rename_version), initial = version.getVersionName(), allowSame = true,
        onDismiss = { renameDialog = false }, onConfirm = { name -> renameDialog = false; if (name != version.getVersionName()) onRename(name) }
    )
    //窗口打开期间 mods 目录可能被修复、启用或删除，因此定时重新统计，避免卡片显示旧数量
    val modCount by produceState(initialValue = countInstalledModJars(version.getGameDir()), version) {
        while (true) {
            delay(2_000)
            value = withContext(Dispatchers.IO) { countInstalledModJars(version.getGameDir()) }
        }
    }
    if (iconDialog) InstanceIconDialog(version = version, onDismiss = { iconDialog = false }, onError = onIconError)

    //窗口居中放置，拖动时限制在桌面范围内，不让它被拖出屏幕
    fun clamp(o: IntOffset): IntOffset {
        val limitX = ((area.width - windowSize.width) / 2).coerceAtLeast(0)
        val limitY = ((area.height - windowSize.height) / 2).coerceAtLeast(0)
        return IntOffset(o.x.coerceIn(-limitX, limitX), o.y.coerceIn(-limitY, limitY))
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        XpWindowFrame(
            title = version.getVersionName(),
            onClose = onClose,
            onMinimize = onMinimize,
            icon = { AllVersionIcon(version = version, modifier = Modifier.size(26.dp)) },
            onFocus = onFocus,
            titleModifier = Modifier.pointerInput(version) {
                detectDragGestures(onDragStart = { onFocus() }) { change, drag ->
                    change.consume()
                    offset = clamp(IntOffset(offset.x + drag.x.roundToInt(), offset.y + drag.y.roundToInt()))
                }
            },
            modifier = Modifier
                .wobbly(offset, ::clamp)
                .onSizeChanged { windowSize = it }
                .width(minOf(480.dp, maxWidth - 32.dp))
                .heightIn(max = maxHeightDp - 16.dp)
        ) {
            //body scrolls when the window is taller than the desktop (compact density / large UI scale)
            Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).background(InstBg)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                // header card: icon (tap = icon dialog) + name (pencil = rename) + two quiet facts lines
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(InstCard).padding(10.dp)
                ) {
                    Box(modifier = Modifier.size(60.dp).clickable { iconDialog = true }) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(InstChip)
                        ) { AllVersionIcon(version = version, modifier = Modifier.size(40.dp)) }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.align(Alignment.BottomEnd).size(22.dp).clip(CircleShape).background(InstBlue).border(2.dp, InstCard, CircleShape)
                        ) { Icon(painterResource(R.drawable.ic_edit_filled), stringResource(R.string.all_inst_icon_edit), tint = Color.White, modifier = Modifier.size(11.dp)) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(version.getVersionName(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(AllDimens.Target).clip(CircleShape).clickable { renameDialog = true }
                            ) { Icon(painterResource(R.drawable.ic_edit_outlined), stringResource(R.string.generic_rename), tint = InstDim, modifier = Modifier.size(16.dp)) }
                        }
                        val loader = info?.loaderInfo?.let { "${it.loader.displayName} ${it.version}" }
                        Text(listOfNotNull(info?.minecraftVersion?.let { "Minecraft $it" }, loader).joinToString(" · "), color = InstDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        // only real data: account, installed mod count, and the honest note that Mod Doctor runs at launch
                        Text(listOfNotNull(accountName?.takeIf { it.isNotBlank() }, stringResource(R.string.all_inst_mods_count, modCount), stringResource(R.string.all_desktop_check_on_launch)).joinToString(" · "), color = InstDim, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }

                // main actions: Play (the one blue button) + Mods + Settings
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    InstBtn(stringResource(R.string.all_inst_play_game), R.drawable.ic_play_arrow_filled, Modifier.weight(1.6f), primary = true, onClick = onPlay)
                    InstBtn(stringResource(R.string.all_desktop_mods), R.drawable.ic_extension_outlined, Modifier.weight(1f), onClick = onMods)
                    InstBtn(stringResource(R.string.generic_setting), R.drawable.ic_settings_filled, Modifier.weight(1f), onClick = onSettings)
                }

                // tools: tiles (files, packs, shaders, shots, servers, worlds); narrow windows reflow to 2 columns
                Text(stringResource(R.string.all_inst_section_tools), color = InstDim, fontSize = 11.sp, modifier = Modifier.padding(start = 2.dp))
                val toolIcons = mapOf("shaders" to R.drawable.ic_lightbulb, "shots" to R.drawable.ic_photo_library_outlined, "servers" to R.drawable.ic_dns_outlined, "worlds" to R.drawable.ic_public)
                val tools = buildList<Pair<String, Pair<Int, () -> Unit>>> {
                    add(stringResource(R.string.all_desktop_files) to (R.drawable.ic_folder_outlined to onFiles))
                    add(stringResource(R.string.resource_pack_manage) to (R.drawable.ic_folder_zip_outlined to onResourcePacks))
                    EXTRA_WINDOWS.forEach { w -> add(stringResource(w.titleRes) to (toolIcons.getValue(w.id) to { onExtra(w.id) })) }
                }
                val gridCols = if (windowWidthDp >= 330.dp) 3 else 2
                FlowRow(
                    maxItemsInEachRow = gridCols,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tools.forEach { (label, ia) -> InstTile(label, ia.first, Modifier.weight(1f), ia.second) }
                    // pad the last row so tiles keep equal width
                    repeat((gridCols - tools.size % gridCols) % gridCols) { Spacer(Modifier.weight(1f)) }
                }

                // delete: quiet red text at the bottom; still asks for confirmation
                Row(
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onDelete).heightIn(min = 36.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_delete_outlined), null, tint = InstDanger, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.generic_delete), color = InstDanger, fontSize = 13.sp)
                }
            }
            }
        }
    }
}

private val InstBg = Color(0xFF161B28)
private val InstCard = Color(0xFF1B2232)
private val InstChip = Color(0xFF232B3D)
private val InstBlue = Color(0xFF2F6FED)
private val InstDim = Color(0xFF8C97AE)
private val InstDanger = Color(0xFFFF6B6B)

/** Flat dark action button for the instance window; [primary] = the blue Play button. */
@Composable
private fun InstBtn(text: String, icon: Int, modifier: Modifier, primary: Boolean = false, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(if (primary) InstBlue else InstChip).clickable(onClick = onClick).heightIn(min = 48.dp).padding(horizontal = 8.dp)
    ) {
        Icon(painterResource(icon), null, tint = if (primary) Color.White else Color(0xFFD5DBE8), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = if (primary) Color.White else Color(0xFFD5DBE8), fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun InstTile(text: String, icon: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(InstCard).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Icon(painterResource(icon), null, tint = Color(0xFFD5DBE8), modifier = Modifier.size(20.dp))
        Text(text, color = Color(0xFFD5DBE8), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
    }
}

/** ALL dialog: pick an image (Android system picker), restore the default icon, or cancel. Cancelling the picker changes nothing. */
@Composable
private fun InstanceIconDialog(version: Version, onDismiss: () -> Unit, onError: AllSubmitError) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val title = stringResource(R.string.all_inst_icon_title)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val err = withContext(kotlinx.coroutines.Dispatchers.IO) { allSetVersionIcon(context, version, uri) }
            if (err != null) onError(com.movtery.zalithlauncher.viewmodel.ErrorViewModel.ThrowableMessage(
                com.movtery.zalithlauncher.ui.androidText(title), com.movtery.zalithlauncher.ui.androidText(err.message ?: err.toString())))
        }
        onDismiss()
    }
    com.movtery.zalithlauncher.all.ui.AllDialog(title, onDismiss, actions = {
        AllButton(stringResource(R.string.generic_cancel), onDismiss, Modifier.weight(1f), fill = true)
    }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AllDimens.Gap), modifier = Modifier.fillMaxWidth()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(84.dp).shadow(3.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp)).background(Color.White)
                    .border(1.dp, Color(0xFFB8C7E0), RoundedCornerShape(14.dp))
            ) { AllVersionIcon(version = version, modifier = Modifier.size(64.dp)) }
            AllButton(stringResource(R.string.all_inst_icon_pick), { picker.launch("image/*") }, Modifier.fillMaxWidth(), style = AllButtonStyle.Primary, fill = true)
            AllButton(stringResource(R.string.all_inst_icon_reset), { allResetVersionIcon(version); onDismiss() }, Modifier.fillMaxWidth(), enabled = allHasCustomIcon(version), fill = true)
        }
    }
}

@Composable
private fun InstSection(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
        Text(title, color = AllColors.BlueDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).height(1.dp).background(AllColors.Border.copy(alpha = 0.7f)))
    }
}

@Composable
private fun InfoLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Text(text = "$label: $value", color = XpText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun XpButton(
    text: String,
    modifier: Modifier = Modifier,
    brush: Brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFE3E0D5))),
    textColor: Color = XpText,
    borderColor: Color = Color(0xFF7A8BA8),
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(80), label = "buttonPress")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(brush)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp)
    ) {
        Text(text = text, color = textColor, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/**
 * 实例设置浮动窗口：窗口壳由 ALL Desktop 管理，内容由 ALL Settings 自己呈现。
 */
@Composable
private fun InstanceSettingsWindow(
    version: Version,
    area: IntSize,
    offsetState: androidx.compose.runtime.MutableState<IntOffset>,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onFocus: () -> Unit,
    onCheckVulkan: (Version) -> Unit,
    showToast: (AllToast) -> Unit,
    submitError: AllSubmitError,
) {
    var offset by offsetState
    var windowSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val maxW = with(density) { area.width.toDp() }
    val maxH = with(density) { area.height.toDp() }
    val margin = if (com.movtery.zalithlauncher.all.ui.LocalAllCompact.current) com.movtery.zalithlauncher.all.ui.AllDimens.WindowMarginCompact else com.movtery.zalithlauncher.all.ui.AllDimens.WindowMargin / 2

    fun clamp(o: IntOffset): IntOffset {
        val limitX = ((area.width - windowSize.width) / 2).coerceAtLeast(0)
        val limitY = ((area.height - windowSize.height) / 2).coerceAtLeast(0)
        return IntOffset(o.x.coerceIn(-limitX, limitX), o.y.coerceIn(-limitY, limitY))
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        XpWindowFrame(
            title = "⚙ " + stringResource(R.string.generic_setting) + " — " + version.getVersionName(),
            onClose = onClose,
            onMinimize = onMinimize,
            onFocus = onFocus,
            titleModifier = Modifier.pointerInput(version) {
                detectDragGestures(onDragStart = { onFocus() }) { change, drag ->
                    change.consume()
                    offset = clamp(IntOffset(offset.x + drag.x.roundToInt(), offset.y + drag.y.roundToInt()))
                }
            },
            modifier = Modifier
                .wobbly(offset, ::clamp)
                .onSizeChanged { windowSize = it }
                .width(minOf(760.dp, maxW - margin * 2))
                .height(maxH - margin * 2)
        ) {
            AllSettingsWindow(
                version = version,
                onCheckVulkan = { onCheckVulkan(version) },
                showToast = showToast,
                submitError = submitError,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )
        }
    }
}

private const val WINDOW_INSTANCE = "instance"
private const val WINDOW_SETTINGS = "settings"
private const val WINDOW_MODS = "mods"
private const val WINDOW_FILES = "files"
private const val WINDOW_DOWNLOAD = "download"
private const val WINDOW_PACKS = "packs"
private const val WINDOW_ACCOUNTS = "accounts"
private const val WINDOW_LSETTINGS = "lsettings"
private const val WINDOW_INSTANCES = "instances"

/** Global ALL apps (AllAppRegistry): Desktop generates window, chip, Start entry and Back handling for each. */
private val registerAllApps: Unit = run {
    AllAppRegistry.register(AllApp("tasks", "\u23F3", R.string.all_tasks_title, R.drawable.ic_list_down) { AllTasksWindow(modifier = Modifier.fillMaxSize()) })
    AllAppRegistry.register(AllApp("about", "\u2139", R.string.about_launcher_title, R.drawable.ic_info_outlined) { env -> AllAboutWindow(showToast = env.showToast, modifier = Modifier.fillMaxSize()) })
}

/** 实例级浮动窗口清单：新增一个窗口 = 这里加一行 + [ExtraWindowContent] 加一个分支 */
private class ExtraWindow(val id: String, val emoji: String, val titleRes: Int)

private val EXTRA_WINDOWS = listOf(
    ExtraWindow("shaders", "\uD83C\uDF1F", R.string.shader_pack_manage),
    ExtraWindow("shots", "\uD83D\uDDBC", R.string.screenshots_manage),
    ExtraWindow("servers", "\uD83D\uDDA5", R.string.servers_list),
    ExtraWindow("worlds", "\uD83C\uDF0D", R.string.saves_manage),
)

@Composable
private fun ExtraWindowContent(
    id: String,
    version: Version,
    onOpenDownload: (AllDownloadCategory) -> Unit,
    submitError: AllSubmitError
) {
    when (id) {
        "shaders" -> AllShadersWindow(
            version = version,
            onOpenDownload = { onOpenDownload(AllDownloadCategory.SHADER) },
            onError = submitError,
            modifier = Modifier.fillMaxSize()
        )
        "shots" -> AllScreenshotsWindow(version = version, modifier = Modifier.fillMaxSize())
        "servers" -> AllServersWindow(version = version, modifier = Modifier.fillMaxSize())
        "worlds" -> AllWorldsWindow(
            version = version,
            onOpenDownload = { onOpenDownload(AllDownloadCategory.WORLD) },
            onError = submitError,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/** Generic floating window: same shell as the Mods window (title bar, drag, minimize, focus); content supplied by the caller */
@Composable
private fun FloatingWindow(
    title: String,
    area: IntSize,
    offsetState: androidx.compose.runtime.MutableState<IntOffset>,
    dragKey: Any,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onFocus: () -> Unit,
    //widest the window may get; the 3-column accounts window uses the whole desktop width
    maxWidth: Dp = 900.dp,
    content: @Composable () -> Unit
) {
    var offset by offsetState
    var windowSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val maxW = with(density) { area.width.toDp() }
    val maxH = with(density) { area.height.toDp() }
    val margin = if (com.movtery.zalithlauncher.all.ui.LocalAllCompact.current) com.movtery.zalithlauncher.all.ui.AllDimens.WindowMarginCompact else com.movtery.zalithlauncher.all.ui.AllDimens.WindowMargin / 2

    fun clamp(o: IntOffset): IntOffset {
        val limitX = ((area.width - windowSize.width) / 2).coerceAtLeast(0)
        val limitY = ((area.height - windowSize.height) / 2).coerceAtLeast(0)
        return IntOffset(o.x.coerceIn(-limitX, limitX), o.y.coerceIn(-limitY, limitY))
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        XpWindowFrame(
            title = title,
            onClose = onClose,
            onMinimize = onMinimize,
            onFocus = onFocus,
            titleModifier = Modifier.pointerInput(dragKey) {
                detectDragGestures(onDragStart = { onFocus() }) { change, drag ->
                    change.consume()
                    offset = clamp(IntOffset(offset.x + drag.x.roundToInt(), offset.y + drag.y.roundToInt()))
                }
            },
            modifier = Modifier
                .wobbly(offset, ::clamp)
                .onSizeChanged { windowSize = it }
                .width(minOf(maxWidth, maxW - margin * 2))
                .height(maxH - margin * 2)
        ) {
            content()
        }
    }
}

/**
 * 模组浮动窗口：这里只提供窗口外壳（标题栏、拖动、最小化、焦点），
 * 内容区完全是 ALL 自己的 [AllModsWindow]，不再嵌入 Zalith 的模组管理页。
 */
@Composable
private fun ModsWindow(
    version: Version,
    area: IntSize,
    offsetState: androidx.compose.runtime.MutableState<IntOffset>,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onFocus: () -> Unit,
    swapToDownload: () -> Unit,
    submitError: AllSubmitError,
) {
    var offset by offsetState
    var windowSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val maxW = with(density) { area.width.toDp() }
    val maxH = with(density) { area.height.toDp() }
    val margin = if (com.movtery.zalithlauncher.all.ui.LocalAllCompact.current) com.movtery.zalithlauncher.all.ui.AllDimens.WindowMarginCompact else com.movtery.zalithlauncher.all.ui.AllDimens.WindowMargin / 2

    fun clamp(o: IntOffset): IntOffset {
        val limitX = ((area.width - windowSize.width) / 2).coerceAtLeast(0)
        val limitY = ((area.height - windowSize.height) / 2).coerceAtLeast(0)
        return IntOffset(o.x.coerceIn(-limitX, limitX), o.y.coerceIn(-limitY, limitY))
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        XpWindowFrame(
            title = "🧩 " + stringResource(R.string.all_desktop_mods) + " — " + version.getVersionName(),
            onClose = onClose,
            onMinimize = onMinimize,
            onFocus = onFocus,
            titleModifier = Modifier.pointerInput(version) {
                detectDragGestures(onDragStart = { onFocus() }) { change, drag ->
                    change.consume()
                    offset = clamp(IntOffset(offset.x + drag.x.roundToInt(), offset.y + drag.y.roundToInt()))
                }
            },
            modifier = Modifier
                .wobbly(offset, ::clamp)
                .onSizeChanged { windowSize = it }
                .width(minOf(900.dp, maxW - margin * 2))
                .height(maxH - margin * 2)
        ) {
            AllModsWindow(
                version = version,
                onOpenDownload = swapToDownload,
                onError = submitError,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 文件浮动窗口：外壳由 ALL 提供，内容由 ALL 自己绘制；后端沿用 Zalith 文件管理能力。
 */
@Composable
private fun FilesWindow(
    version: Version,
    area: IntSize,
    offsetState: androidx.compose.runtime.MutableState<IntOffset>,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onFocus: () -> Unit,
) {
    var offset by offsetState
    var windowSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val maxW = with(density) { area.width.toDp() }
    val maxH = with(density) { area.height.toDp() }
    val margin = if (com.movtery.zalithlauncher.all.ui.LocalAllCompact.current) com.movtery.zalithlauncher.all.ui.AllDimens.WindowMarginCompact else com.movtery.zalithlauncher.all.ui.AllDimens.WindowMargin / 2
    fun clamp(o: IntOffset): IntOffset {
        val limitX = ((area.width - windowSize.width) / 2).coerceAtLeast(0)
        val limitY = ((area.height - windowSize.height) / 2).coerceAtLeast(0)
        return IntOffset(o.x.coerceIn(-limitX, limitX), o.y.coerceIn(-limitY, limitY))
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        XpWindowFrame(
            title = "📁 " + stringResource(R.string.all_desktop_files) + " — " + version.getVersionName(),
            onClose = onClose,
            onMinimize = onMinimize,
            onFocus = onFocus,
            titleModifier = Modifier.pointerInput(version) {
                detectDragGestures(onDragStart = { onFocus() }) { change, drag ->
                    change.consume()
                    offset = clamp(IntOffset(offset.x + drag.x.roundToInt(), offset.y + drag.y.roundToInt()))
                }
            },
            modifier = Modifier
                .wobbly(offset, ::clamp)
                .onSizeChanged { windowSize = it }
                .width(minOf(1000.dp, maxW - margin * 2))
                .height(maxH - margin * 2)
        ) {
            AllFilesWindow(
                version = version,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 开始菜单的全局工具（模组 / 文件）先选实例：列表来自 VersionsManager 的真实实例，不猜当前实例。
 */
@Composable
private fun InstancePicker(
    versions: List<Version>,
    icon: String,
    onDismiss: () -> Unit,
    onPick: (Version) -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x55000000))
            .clickable(indication = null, interactionSource = null, onClick = onDismiss)
    ) {
        XpWindowFrame(
            title = icon + " " + stringResource(R.string.all_desktop_pick_instance),
            onClose = onDismiss,
            modifier = Modifier
                .width(420.dp)
                .heightIn(max = 420.dp)
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(versions.size) { i ->
                    val version = versions[i]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onPick(version) }
                            .padding(horizontal = 10.dp)
                    ) {
                        AllVersionIcon(version = version, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(version.getVersionName(), color = XpText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/**
 * Download floating shell: ALL owns navigation/presentation; shared Zalith provider and installer services remain underneath.
 */
@Composable
private fun DownloadWindow(
    area: IntSize,
    offsetState: androidx.compose.runtime.MutableState<IntOffset>,
    initialCategory: AllDownloadCategory,
    targetName: String?,
    session: Int,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onFocus: () -> Unit,
    submitError: AllSubmitError,
) {
    var offset by offsetState
    var windowSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val maxW = with(density) { area.width.toDp() }
    val maxH = with(density) { area.height.toDp() }
    val margin = if (com.movtery.zalithlauncher.all.ui.LocalAllCompact.current) com.movtery.zalithlauncher.all.ui.AllDimens.WindowMarginCompact else com.movtery.zalithlauncher.all.ui.AllDimens.WindowMargin / 2

    fun clamp(o: IntOffset): IntOffset {
        val limitX = ((area.width - windowSize.width) / 2).coerceAtLeast(0)
        val limitY = ((area.height - windowSize.height) / 2).coerceAtLeast(0)
        return IntOffset(o.x.coerceIn(-limitX, limitX), o.y.coerceIn(-limitY, limitY))
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        XpWindowFrame(
            title = "⬇ " + stringResource(R.string.generic_download),
            onClose = onClose,
            onMinimize = onMinimize,
            onFocus = onFocus,
            titleModifier = Modifier.pointerInput(Unit) {
                detectDragGestures(onDragStart = { onFocus() }) { change, drag ->
                    change.consume()
                    offset = clamp(IntOffset(offset.x + drag.x.roundToInt(), offset.y + drag.y.roundToInt()))
                }
            },
            modifier = Modifier
                .wobbly(offset, ::clamp)
                .onSizeChanged { windowSize = it }
                //Download V4: the whole desktop area above the taskbar (the taskbar stays visible and usable)
                .width(maxW)
                .height(maxH)
        ) {
            AllDownloadWindow(
                initialCategory = initialCategory,
                targetVersionName = targetName,
                session = session,
                submitError = submitError,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** 还原/打开窗口时轻微回弹的缩放 */
private val WindowPopSpring = spring<Float>(dampingRatio = 0.62f, stiffness = 520f)

/** 拖动窗口的弹簧：看得出弹性，但不夸张 */
private val WobbleSpring = spring<Offset>(dampingRatio = 0.58f, stiffness = 650f)

/**
 * Wobbly / Bouncy Window V1：拖动逻辑与夹紧不变，只让显示位置用弹簧跟随目标位置，
 * 松手后轻微过冲再回正；按水平速度轻微倾斜。只在布局/绘制阶段读取动画值，不做变形着色器，也没有轮询。
 * 显示位置同样经过 [clamp]，因此回弹不会把窗口带出桌面。
 */
@Composable
private fun Modifier.wobbly(target: IntOffset, clamp: (IntOffset) -> IntOffset): Modifier {
    val anim = remember { Animatable(Offset(target.x.toFloat(), target.y.toFloat()), Offset.VectorConverter) }
    LaunchedEffect(target) {
        anim.animateTo(Offset(target.x.toFloat(), target.y.toFloat()), WobbleSpring)
    }
    return this
        .offset { clamp(IntOffset(anim.value.x.roundToInt(), anim.value.y.roundToInt())) }
        .graphicsLayer {
            rotationZ = (anim.velocity.x / 1100f).coerceIn(-2.5f, 2.5f)
        }
}
