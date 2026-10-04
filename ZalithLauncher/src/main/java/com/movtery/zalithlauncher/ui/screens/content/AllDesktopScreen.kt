package com.movtery.zalithlauncher.ui.screens.content

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.path.getGameHome
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.InterfaceMode
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val XpBlue = Brush.verticalGradient(listOf(Color(0xFF3A8DFF), Color(0xFF0B5CD8), Color(0xFF0842A8)))
private val XpGreen = Brush.verticalGradient(listOf(Color(0xFF5CC85C), Color(0xFF2E8B2E)))
private val XpGreenPressed = Brush.verticalGradient(listOf(Color(0xFF2E8B2E), Color(0xFF1F6B1F)))
private val XpTaskbar = Brush.verticalGradient(listOf(Color(0xFF3F7FE6), Color(0xFF1F57C3), Color(0xFF1848A8)))
private val XpWindowBody = Color(0xFFF3F1E8)
private val XpText = Color(0xFF1B1B1B)
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
    var tasksOpen by remember { mutableStateOf(false) }
    var tasksMinimized by remember { mutableStateOf(false) }
    var instOpen by remember { mutableStateOf(false) }
    var instMinimized by remember { mutableStateOf(false) }
    var lsOpen by remember { mutableStateOf(false) }
    var lsMinimized by remember { mutableStateOf(false) }
    var accountsOpen by remember { mutableStateOf(false) }
    var accountsMinimized by remember { mutableStateOf(false) }
    var aboutOpen by remember { mutableStateOf(false) }
    var aboutMinimized by remember { mutableStateOf(false) }
    var filesVersion by remember { mutableStateOf<Version?>(null) }
    var filesMinimized by remember { mutableStateOf(false) }
    var downloadOpen by remember { mutableStateOf(false) }
    var downloadMinimized by remember { mutableStateOf(false) }
    var downloadInitialCategory by remember { mutableStateOf(AllDownloadCategory.GAME) }
    //开始菜单的全局工具先选实例：MODS / FILES，null = 不显示选择器
    var pickerFor by remember { mutableStateOf<String?>(null) }
    var front by remember { mutableStateOf(WINDOW_INSTANCE) }
    var deleteTarget by remember { mutableStateOf<Version?>(null) }
    var deleteTask by remember { mutableStateOf<Pair<Int, suspend () -> Unit>?>(null) }
    //窗口拖动位置放在窗口外：最小化会把窗口内容移出组合，位置放里面会在还原时丢失。关闭后/换实例再打开时回到居中
    val winOffsets = remember { mutableMapOf<String, androidx.compose.runtime.MutableState<IntOffset>>() }
    fun offsetOf(id: String) = winOffsets.getOrPut(id) { mutableStateOf(IntOffset.Zero) }

    //下载窗口由 ALL 自己的状态驱动；category 为 null 时保持上次的分类
    val openDownloadWindow: (AllDownloadCategory?) -> Unit = { category ->
        if (!downloadOpen) offsetOf(WINDOW_DOWNLOAD).value = IntOffset.Zero
        category?.let { downloadInitialCategory = it }
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
    val openTasksWindow: () -> Unit = {
        if (!tasksOpen) offsetOf(WINDOW_TASKS).value = IntOffset.Zero
        tasksOpen = true
        tasksMinimized = false
        front = WINDOW_TASKS
    }
    val openInstancesWindow: () -> Unit = {
        if (!instOpen) offsetOf(WINDOW_INSTANCES).value = IntOffset.Zero
        instOpen = true
        instMinimized = false
        front = WINDOW_INSTANCES
    }
    val openLauncherSettingsWindow: () -> Unit = {
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
    val openAboutWindow: () -> Unit = {
        if (!aboutOpen) offsetOf(WINDOW_ABOUT).value = IntOffset.Zero
        aboutOpen = true
        aboutMinimized = false
        front = WINDOW_ABOUT
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
        WINDOW_ABOUT to (aboutOpen && !aboutMinimized),
        WINDOW_FILES to (filesVersion != null && !filesMinimized),
        WINDOW_DOWNLOAD to (downloadOpen && !downloadMinimized),
        WINDOW_ACCOUNTS to (accountsOpen && !accountsMinimized),
        WINDOW_LSETTINGS to (lsOpen && !lsMinimized),
        WINDOW_INSTANCES to (instOpen && !instMinimized),
        WINDOW_TASKS to (tasksOpen && !tasksMinimized)
    ) + EXTRA_WINDOWS.associate { it.id to (extras[it.id] != null && extraMin[it.id] != true) }
    //实际显示在最上面的窗口：焦点窗口可见就是它，否则按固定顺序取最后一个可见的
    val topShown = if (shown[front] == true) front
        else listOf(WINDOW_INSTANCE, WINDOW_SETTINGS, WINDOW_MODS, WINDOW_FILES, WINDOW_DOWNLOAD, WINDOW_PACKS, WINDOW_ABOUT, WINDOW_ACCOUNTS, WINDOW_LSETTINGS, WINDOW_INSTANCES, WINDOW_TASKS, *EXTRA_WINDOWS.map { it.id }.toTypedArray()).lastOrNull { shown[it] == true }

    //桌面主栈只有 LauncherMain，返回键会直接结束 Activity；有开始菜单/窗口时先收起它们。
    //注册在窗口内容之前，窗口内的嵌套导航（如下载子页）仍优先处理返回
    BackHandler(enabled = startOpen || topShown != null) {
        when {
            startOpen -> startOpen = false
            topShown == WINDOW_INSTANCE -> minimized = true
            topShown == WINDOW_SETTINGS -> settingsMinimized = true
            topShown == WINDOW_MODS -> modsMinimized = true
            topShown == WINDOW_PACKS -> packsMinimized = true
            topShown == WINDOW_ABOUT -> aboutMinimized = true
            topShown == WINDOW_ACCOUNTS -> accountsMinimized = true
            topShown == WINDOW_LSETTINGS -> lsMinimized = true
            topShown == WINDOW_INSTANCES -> instMinimized = true
            topShown == WINDOW_TASKS -> tasksMinimized = true
            topShown != null && EXTRA_WINDOWS.any { it.id == topShown } -> extraMin[topShown] = true
            topShown == WINDOW_FILES -> filesMinimized = true
            topShown == WINDOW_DOWNLOAD -> downloadMinimized = true
        }
    }

    val uiScale = AllSettings.allDesktopUiScale.state / 100f
    val baseDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = baseDensity.density * uiScale,
            fontScale = baseDensity.fontScale
        )
    ) {
    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
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

            LazyHorizontalGrid(
                rows = GridCells.Adaptive(118.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(versions, key = { it.getVersionName() }) { version ->
                    DesktopIcon(version = version, selected = version == openVersion) {
                        if (openVersion != version) offsetOf(WINDOW_INSTANCE).value = IntOffset.Zero
                        openVersion = version
                        minimized = false
                        startOpen = false
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
                    enter = fadeIn(tween(140)) + scaleIn(WindowPopSpring, initialScale = 0.9f),
                    exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.92f),
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
                    enter = fadeIn(tween(140)) + scaleIn(WindowPopSpring, initialScale = 0.9f),
                    exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.92f),
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
                    enter = fadeIn(tween(140)) + scaleIn(WindowPopSpring, initialScale = 0.9f),
                    exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.92f),
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
                                swapToDownload = { openDownloadWindow(AllDownloadCategory.MOD) },
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
                    enter = fadeIn(tween(140)) + scaleIn(WindowPopSpring, initialScale = 0.9f),
                    exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.92f),
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
                    enter = fadeIn(tween(140)) + scaleIn(WindowPopSpring, initialScale = 0.9f),
                    exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.92f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    DownloadWindow(
                        area = area,
                        offsetState = offsetOf(WINDOW_DOWNLOAD),
                        initialCategory = downloadInitialCategory,
                        onClose = { downloadOpen = false },
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
                    enter = fadeIn(tween(140)) + scaleIn(WindowPopSpring, initialScale = 0.9f),
                    exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.92f),
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
                                    onOpenDownload = { openDownloadWindow(AllDownloadCategory.RESOURCE_PACK) },
                                    onError = submitError,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
            val aboutLayer: @Composable () -> Unit = {
                androidx.compose.animation.AnimatedVisibility(
                    visible = aboutOpen && !aboutMinimized,
                    enter = fadeIn(tween(140)) + scaleIn(WindowPopSpring, initialScale = 0.9f),
                    exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.92f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    FloatingWindow(
                        title = "ℹ " + stringResource(R.string.about_launcher_title),
                        area = area,
                        offsetState = offsetOf(WINDOW_ABOUT),
                        dragKey = WINDOW_ABOUT,
                        onClose = { aboutOpen = false },
                        onMinimize = { aboutMinimized = true },
                        onFocus = { front = WINDOW_ABOUT }
                    ) {
                        AllAboutWindow(showToast = showToast, modifier = Modifier.fillMaxSize())
                    }
                }
            }
            val tasksLayer: @Composable () -> Unit = {
                androidx.compose.animation.AnimatedVisibility(
                    visible = tasksOpen && !tasksMinimized,
                    enter = fadeIn(tween(140)) + scaleIn(WindowPopSpring, initialScale = 0.9f),
                    exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.92f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    FloatingWindow(
                        title = "\u23F3 " + stringResource(R.string.all_tasks_title),
                        area = area,
                        offsetState = offsetOf(WINDOW_TASKS),
                        dragKey = WINDOW_TASKS,
                        onClose = { tasksOpen = false },
                        onMinimize = { tasksMinimized = true },
                        onFocus = { front = WINDOW_TASKS }
                    ) {
                        AllTasksWindow(modifier = Modifier.fillMaxSize())
                    }
                }
            }
            val instLayer: @Composable () -> Unit = {
                androidx.compose.animation.AnimatedVisibility(
                    visible = instOpen && !instMinimized,
                    enter = fadeIn(tween(140)) + scaleIn(WindowPopSpring, initialScale = 0.9f),
                    exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.92f),
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
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            val lsLayer: @Composable () -> Unit = {
                androidx.compose.animation.AnimatedVisibility(
                    visible = lsOpen && !lsMinimized,
                    enter = fadeIn(tween(140)) + scaleIn(WindowPopSpring, initialScale = 0.9f),
                    exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.92f),
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
                        AllLauncherSettingsWindow(onError = submitError, modifier = Modifier.fillMaxSize())
                    }
                }
            }
            val accountsLayer: @Composable () -> Unit = {
                androidx.compose.animation.AnimatedVisibility(
                    visible = accountsOpen && !accountsMinimized,
                    enter = fadeIn(tween(140)) + scaleIn(WindowPopSpring, initialScale = 0.9f),
                    exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.92f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    FloatingWindow(
                        title = "\uD83D\uDC64 " + stringResource(R.string.all_desktop_account),
                        area = area,
                        offsetState = offsetOf(WINDOW_ACCOUNTS),
                        dragKey = WINDOW_ACCOUNTS,
                        onClose = { accountsOpen = false },
                        onMinimize = { accountsMinimized = true },
                        onFocus = { front = WINDOW_ACCOUNTS }
                    ) {
                        AllAccountsWindow(modifier = Modifier.fillMaxSize())
                    }
                }
            }
            val extraLayers = EXTRA_WINDOWS.map { w ->
                w.id to @Composable {
                    extras[w.id]?.let { extraShown[w.id] = it }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = extras[w.id] != null && extraMin[w.id] != true,
                        enter = fadeIn(tween(140)) + scaleIn(WindowPopSpring, initialScale = 0.9f),
                        exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.92f),
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
                                        onOpenDownload = { c -> openDownloadWindow(c) },
                                        submitError = submitError
                                    )
                                }
                            }
                        }
                    }
                }
            }
            val layers = listOf(WINDOW_INSTANCE to instanceLayer, WINDOW_SETTINGS to settingsLayer, WINDOW_MODS to modsLayer, WINDOW_FILES to filesLayer, WINDOW_DOWNLOAD to downloadLayer, WINDOW_PACKS to packsLayer, WINDOW_ABOUT to aboutLayer, WINDOW_ACCOUNTS to accountsLayer, WINDOW_LSETTINGS to lsLayer, WINDOW_INSTANCES to instLayer, WINDOW_TASKS to tasksLayer) + extraLayers
            //同一个调用点 + key(id)：换焦点时 Compose 只移动窗口，不会重建（否则位置和窗口内状态会丢）
            (layers.filter { it.first != front } + layers.filter { it.first == front }).forEach { (id, layer) ->
                androidx.compose.runtime.key(id) { layer() }
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
                StartMenu(
                    accountName = account?.username,
                    hasCurrentVersion = versions.isNotEmpty(),
                    maxMenuHeight = maxHeight,
                    onItem = { startOpen = false },
                    onDesktop = { openVersion = null },
                    onDownload = { openDownloadWindow(null) },
                    onMods = { pickerFor = WINDOW_MODS },
                    onFiles = { pickerFor = WINDOW_FILES },
                    onAccount = openAccountsWindow,
                    onInstances = openInstancesWindow,
                    onTasks = openTasksWindow,
                    onSettings = openLauncherSettingsWindow,
                    onWallpaper = { wallpaperOpen = true },
                    onUiScale = { uiScaleOpen = true },
                    onAbout = openAboutWindow,
                    onClassic = { AllSettings.launcherInterfaceMode.save(InterfaceMode.CLASSIC) }
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

        Taskbar(
            startOpen = startOpen,
            onStart = { startOpen = !startOpen },
            chips = run {
                val downloadLabel = stringResource(R.string.generic_download)
                val aboutLabel = stringResource(R.string.about_launcher_title)
                val accountsLabel = stringResource(R.string.all_desktop_account)
                val lsLabel = stringResource(R.string.all_settings_launcher_title)
                val instLabel = stringResource(R.string.all_instances_title)
                val tasksLabel = stringResource(R.string.all_tasks_title)
                //任务栏按钮：窗口在最前 → 按下态，点一下最小化；在后面 → 点一下提到最前；已最小化 → 点一下还原
                fun chip(id: String, v: Version?, prefix: String, isMin: Boolean, setMin: (Boolean) -> Unit) = v?.let {
                    val isFront = topShown == id
                    TaskChip(it, prefix + it.getVersionName(), isFront) {
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
                    if (tasksOpen) {
                        val isFront = topShown == WINDOW_TASKS
                        TaskChip(null, "\u23F3 " + tasksLabel, isFront) {
                            when {
                                tasksMinimized -> { tasksMinimized = false; front = WINDOW_TASKS }
                                isFront -> tasksMinimized = true
                                else -> front = WINDOW_TASKS
                            }
                        }
                    } else null,
                    if (instOpen) {
                        val isFront = topShown == WINDOW_INSTANCES
                        TaskChip(null, "\uD83D\uDCE6 " + instLabel, isFront) {
                            when {
                                instMinimized -> { instMinimized = false; front = WINDOW_INSTANCES }
                                isFront -> instMinimized = true
                                else -> front = WINDOW_INSTANCES
                            }
                        }
                    } else null,
                    if (lsOpen) {
                        val isFront = topShown == WINDOW_LSETTINGS
                        TaskChip(null, "\u2699 " + lsLabel, isFront) {
                            when {
                                lsMinimized -> { lsMinimized = false; front = WINDOW_LSETTINGS }
                                isFront -> lsMinimized = true
                                else -> front = WINDOW_LSETTINGS
                            }
                        }
                    } else null,
                    if (accountsOpen) {
                        val isFront = topShown == WINDOW_ACCOUNTS
                        TaskChip(null, "\uD83D\uDC64 " + accountsLabel, isFront) {
                            when {
                                accountsMinimized -> { accountsMinimized = false; front = WINDOW_ACCOUNTS }
                                isFront -> accountsMinimized = true
                                else -> front = WINDOW_ACCOUNTS
                            }
                        }
                    } else null,
                    if (aboutOpen) {
                        val isFront = topShown == WINDOW_ABOUT
                        TaskChip(null, "ℹ " + aboutLabel, isFront) {
                            when {
                                aboutMinimized -> { aboutMinimized = false; front = WINDOW_ABOUT }
                                isFront -> aboutMinimized = true
                                else -> front = WINDOW_ABOUT
                            }
                        }
                    } else null,
                    if (downloadOpen) {
                        val isFront = topShown == WINDOW_DOWNLOAD
                        TaskChip(null, "⬇ " + downloadLabel, isFront) {
                            when {
                                downloadMinimized -> { downloadMinimized = false; front = WINDOW_DOWNLOAD }
                                isFront -> downloadMinimized = true
                                else -> front = WINDOW_DOWNLOAD
                            }
                        }
                    } else null
                )
            },
            taskCount = tasks.size,
            onTasks = openTasksWindow
        )
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
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF2C6FD6), Color(0xFF6FA8F0), Color(0xFFBFDDF7))))
        drawOval(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF6DBE45), Color(0xFF3F8F2A)),
                startY = size.height * 0.62f, endY = size.height
            ),
            topLeft = Offset(-size.width * 0.25f, size.height * 0.62f),
            size = Size(size.width * 1.5f, size.height * 0.9f)
        )
    }
    val fallback = painterResource(R.drawable.all_desktop_wallpaper)
    //等比裁切、底部对齐：保留草坡和人物，只裁掉天空
    AsyncImage(
        model = custom.ifBlank { null }?.let(Uri::parse) ?: R.drawable.all_desktop_wallpaper,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alignment = Alignment.BottomCenter,
        placeholder = fallback,
        error = fallback,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun WallpaperDialog(onDismiss: () -> Unit) {
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
private fun DesktopIcon(version: Version, selected: Boolean, onOpen: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(90), label = "iconPress")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(104.dp)
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0x553A8DFF) else Color.Transparent)
            .border(1.dp, if (selected) Color(0xAAFFFFFF) else Color.Transparent, RoundedCornerShape(10.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onOpen)
            .padding(6.dp)
    ) {
        AllVersionIcon(version = version, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = version.getVersionName(),
            color = Color.White,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, shadow = LabelShadow),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .background(Color(0x33000000), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
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
) {
    Column(
        modifier = modifier
            .shadow(18.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(XpWindowBody)
            .border(2.dp, Color(0xFF0842A8), RoundedCornerShape(10.dp))
            .clickable(indication = null, interactionSource = null, onClick = onFocus)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .background(XpBlue)
                .then(titleModifier)
                .padding(start = 10.dp, end = 4.dp)
        ) {
            icon?.let {
                it()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = title,
                color = Color.White,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, shadow = LabelShadow),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            onMinimize?.let { TitleButton(text = "–", color = Color(0xFF2F6FE0), onClick = it) }
            TitleButton(text = "✕", color = Color(0xFFE0442E), onClick = onClose)
        }
        content()
    }
}

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
    onFocus: () -> Unit = {},
) {
    var offset by offsetState
    var windowSize by remember { mutableStateOf(IntSize.Zero) }
    val info = version.getVersionInfo()
    val maxWidth = with(LocalDensity.current) { area.width.toDp() }

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
        ) {
            // 封面区：用现有图标和配色做出层次，不新增封面系统
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0xFFDCE8FB), Color(0xFFF3F1E8))))
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFB8C7E0), RoundedCornerShape(12.dp))
                ) {
                    AllVersionIcon(version = version, modifier = Modifier.size(56.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    InfoLine(stringResource(R.string.all_desktop_version), info?.minecraftVersion)
                    InfoLine(
                        stringResource(R.string.all_desktop_loader),
                        info?.loaderInfo?.let { "${it.loader.displayName} ${it.version}" }
                    )
                    InfoLine(stringResource(R.string.all_desktop_account), accountName)
                    // 没有持久化的健康状态：如实说明检查发生在启动时，不做后台扫描
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "🩺 " + stringResource(R.string.all_desktop_check_on_launch),
                        color = Color(0xFF555555),
                        fontSize = 12.sp
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp)
            ) {
                XpButton(
                    text = "▶  " + stringResource(R.string.all_desktop_play),
                    brush = XpGreen,
                    textColor = Color.White,
                    borderColor = Color(0xFF1F6B1F),
                    modifier = Modifier.weight(0.95f),
                    onClick = onPlay
                )
                XpButton(stringResource(R.string.all_desktop_mods), modifier = Modifier.weight(0.55f), onClick = onMods)
                XpButton(stringResource(R.string.generic_setting), modifier = Modifier.weight(0.95f), onClick = onSettings)
                XpButton(stringResource(R.string.all_desktop_files), modifier = Modifier.weight(0.55f), onClick = onFiles)
                XpButton(
                    stringResource(R.string.resource_pack_manage),
                    modifier = Modifier.weight(1.5f),
                    onClick = onResourcePacks
                )
            }

            EXTRA_WINDOWS.chunked(2).forEach { pair ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                ) {
                    pair.forEach { w ->
                        XpButton(w.emoji + " " + stringResource(w.titleRes), modifier = Modifier.weight(1f), onClick = { onExtra(w.id) })
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            //删除单独放一行、远离「开始游戏」，点下去只会打开确认对话框
            XpButton(
                text = "ลบ",
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 16.dp, bottom = 14.dp)
                    .width(136.dp),
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Text(text = "$label: $value", color = XpText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun TitleButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .size(38.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
    ) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.Bold)
    }
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
            .padding(horizontal = 8.dp)
    ) {
        Text(text = text, color = textColor, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun StartMenu(
    accountName: String?,
    hasCurrentVersion: Boolean,
    maxMenuHeight: Dp,
    onItem: () -> Unit,
    onDesktop: () -> Unit,
    onDownload: () -> Unit,
    onMods: () -> Unit,
    onFiles: () -> Unit,
    onAccount: () -> Unit,
    onInstances: () -> Unit,
    onTasks: () -> Unit,
    onSettings: () -> Unit,
    onWallpaper: () -> Unit,
    onUiScale: () -> Unit,
    onAbout: () -> Unit,
    onClassic: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(start = 6.dp, bottom = 4.dp)
            .fillMaxWidth(0.5f)
            .widthIn(min = 360.dp, max = 480.dp)
            .heightIn(max = maxMenuHeight)
            .verticalScroll(rememberScrollState())
            .shadow(18.dp, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .border(2.dp, Color(0xFF0842A8), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .clickable(indication = null, interactionSource = null) {}
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(XpBlue)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(2.dp, Color(0xFFF0B044), RoundedCornerShape(8.dp))
            ) {
                Icon(painterResource(R.drawable.ic_person_outlined), null, tint = Color(0xFF0B5CD8))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                accountName ?: "ALL Launcher",
                color = Color.White,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, shadow = LabelShadow)
            )
        }

        val item: @Composable (Int, String, () -> Unit) -> Unit = { icon, text, action ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onItem(); action() }
                    .heightIn(min = 46.dp)
                    .padding(horizontal = 12.dp)
            ) {
                Icon(painterResource(icon), null, tint = Color(0xFF0B5CD8), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(text, color = XpText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // 左栏：常用；右栏：系统（XP 开始菜单的浅蓝右栏）
            Column(
                modifier = Modifier
                    .weight(0.59f).fillMaxHeight()
                    .background(Color.White)
                    .padding(6.dp)
            ) {
                item(R.drawable.ic_home_filled, stringResource(R.string.all_desktop_home), onDesktop)
                item(R.drawable.ic_download, stringResource(R.string.generic_download), onDownload)
                if (hasCurrentVersion) item(R.drawable.ic_extension_outlined, stringResource(R.string.all_desktop_mods), onMods)
                item(R.drawable.ic_folder_outlined, stringResource(R.string.all_desktop_files), onFiles)
            }
            Column(
                modifier = Modifier
                    .weight(0.41f).fillMaxHeight()
                    .background(Color(0xFFD3E5FA))
                    .border(1.dp, Color(0xFF95BDEE))
                    .padding(6.dp)
            ) {
                item(R.drawable.ic_person_outlined, stringResource(R.string.all_desktop_account), onAccount)
                item(R.drawable.ic_dashboard_outlined, stringResource(R.string.all_instances_title), onInstances)
                item(R.drawable.ic_list_down, stringResource(R.string.all_tasks_title), onTasks)
                item(R.drawable.ic_settings_filled, stringResource(R.string.generic_setting), onSettings)
                item(R.drawable.ic_build_outlined, stringResource(R.string.all_desktop_wallpaper), onWallpaper)
                item(R.drawable.ic_text_format, stringResource(R.string.all_desktop_ui_scale), onUiScale)
                item(R.drawable.ic_info_outlined, stringResource(R.string.settings_tab_info_about), onAbout)
            }
        }

        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .background(XpBlue)
                .padding(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onItem(); onClassic() }
                    .heightIn(min = 44.dp)
                    .padding(horizontal = 12.dp)
            ) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE0442E))
                        .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.all_desktop_switch_classic), color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun Taskbar(
    startOpen: Boolean,
    onStart: () -> Unit,
    chips: List<TaskChip>,
    taskCount: Int,
    onTasks: () -> Unit,
) {
    var clock by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            clock = format.format(Date())
            delay(15_000)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(XpTaskbar)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxHeight()
                .shadow(6.dp, RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp))
                .clip(RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp))
                .background(if (startOpen) XpGreenPressed else XpGreen)
                .clickable(onClick = onStart)
                .padding(horizontal = 20.dp)
        ) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(Color.White))
            Spacer(Modifier.width(8.dp))
            Text(
                "ALL",
                color = Color.White,
                style = TextStyle(fontWeight = FontWeight.Bold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 20.sp, shadow = LabelShadow)
            )
        }

        //开着的窗口：在最前时按下态（深色、粗体），在后面或最小化时浮起态（浅色）
        //按钮区占用 ALL 按钮与托盘之间的剩余宽度，放不下时横向滑动，按钮本身不缩小；最前的窗口自动滚到可见处
        val chipsState = rememberLazyListState()
        val frontIndex = chips.indexOfFirst { it.front }
        LaunchedEffect(frontIndex, chips.size) {
            if (frontIndex >= 0) chipsState.animateScrollToItem(frontIndex)
        }
        LazyRow(
            state = chipsState,
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(end = 8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            items(chips.size) { i ->
            val chip = chips[i]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .widthIn(max = 220.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (chip.front) Color(0xFF1A4BA8) else Color(0xFF3C81F3))
                    .border(1.dp, if (chip.front) Color(0xFF0E3380) else Color(0xFF8CB6FA), RoundedCornerShape(6.dp))
                    .clickable(onClick = chip.onClick)
                    .padding(horizontal = 12.dp)
            ) {
                chip.version?.let { AllVersionIcon(version = it, modifier = Modifier.size(24.dp)) }
                Spacer(Modifier.width(6.dp))
                Text(
                    chip.label,
                    color = Color.White,
                    fontWeight = if (chip.front) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxHeight()
                .background(Brush.verticalGradient(listOf(Color(0xFF14A0F0), Color(0xFF0F7FD8))))
                .border(1.dp, Color(0xFF0B5CD8))
                .padding(horizontal = 18.dp)
        ) {
            //已有的任务列表里有任务时才显示，不做新的监控
            if (taskCount > 0) {
                //桌面隐藏了经典顶栏，点这里展开/收起共用的任务菜单（进度与取消）
                Text(
                    stringResource(R.string.all_desktop_tasks, taskCount),
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .clickable {
                            onTasks()
                        }
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                )
            }
            Text(clock, color = Color.White, fontSize = 16.sp)
        }
    }
}

private class TaskChip(val version: Version?, val label: String, val front: Boolean, val onClick: () -> Unit)

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
                .width(minOf(760.dp, maxW - 24.dp))
                .height(maxH - 24.dp)
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
private const val WINDOW_ABOUT = "about"
private const val WINDOW_ACCOUNTS = "accounts"
private const val WINDOW_LSETTINGS = "lsettings"
private const val WINDOW_INSTANCES = "instances"
private const val WINDOW_TASKS = "tasks"

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
    content: @Composable () -> Unit
) {
    var offset by offsetState
    var windowSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val maxW = with(density) { area.width.toDp() }
    val maxH = with(density) { area.height.toDp() }

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
                .width(minOf(900.dp, maxW - 24.dp))
                .height(maxH - 24.dp)
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
                .width(minOf(900.dp, maxW - 24.dp))
                .height(maxH - 24.dp)
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
                .width(minOf(1000.dp, maxW - 24.dp))
                .height(maxH - 24.dp)
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
                .width(minOf(1100.dp, maxW - 24.dp))
                .height(maxH - 24.dp)
        ) {
            AllDownloadWindow(
                initialCategory = initialCategory,
                submitError = submitError,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
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
