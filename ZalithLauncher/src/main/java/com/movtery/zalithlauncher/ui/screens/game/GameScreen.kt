/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.movtery.inputmap.keycodes.ControlEventKeycode
import com.movtery.inputmap.keycodes.LwjglGlfwKeycode
import com.movtery.inputmap.keycodes.OPEN_CHAT
import com.movtery.inputmap.keycodes.OPEN_CHAT_VALUE
import com.movtery.layer_controller.event.ClickEvent
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.bridge.CURSOR_DISABLED
import com.movtery.zalithlauncher.bridge.ZLBridgeStates
import com.movtery.zalithlauncher.bridge.ZLNativeInvoker
import com.movtery.zalithlauncher.game.input.LWJGLCharSender
import com.movtery.zalithlauncher.game.keycodes.mapToKeycode
import com.movtery.zalithlauncher.game.launch.handler.GameHandler
import com.movtery.zalithlauncher.game.sdl.SdlBridge
import com.movtery.zalithlauncher.game.sdl.SdlTextSender
import com.movtery.zalithlauncher.game.support.touch_controller.touchControllerInputModifier
import com.movtery.zalithlauncher.game.support.touch_controller.touchControllerTouchModifier
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.ui.androidText
import com.movtery.zalithlauncher.setting.enums.isLauncherInDarkTheme
import com.movtery.zalithlauncher.setting.enums.toAction
import com.movtery.zalithlauncher.terracotta.Terracotta
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.MenuState
import com.movtery.zalithlauncher.ui.components.rememberBoxSize
import com.movtery.zalithlauncher.ui.control.MinecraftHotbar
import com.movtery.zalithlauncher.ui.control.event.launcherEvent
import com.movtery.zalithlauncher.ui.control.event.lwjglEvent
import com.movtery.zalithlauncher.ui.control.gamepad.GamepadKeyListener
import com.movtery.zalithlauncher.ui.control.gamepad.GamepadOnActionListener
import com.movtery.zalithlauncher.ui.control.gamepad.GamepadStickMovementListener
import com.movtery.zalithlauncher.ui.control.gamepad.SimpleGamepadCapture
import com.movtery.zalithlauncher.ui.control.gyroscope.GyroscopeReader
import com.movtery.zalithlauncher.ui.control.gyroscope.isGyroscopeAvailable
import com.movtery.zalithlauncher.ui.control.hotbarPercentage
import com.movtery.zalithlauncher.ui.control.input.TextInputMode
import com.movtery.zalithlauncher.ui.control.mouse.SwitchableMouseLayout
import com.movtery.zalithlauncher.ui.screens.game.elements.DraggableGameBall
import com.movtery.zalithlauncher.ui.screens.game.elements.ForceCloseOperation
import com.movtery.zalithlauncher.ui.screens.game.elements.GamepadModePromptDialog
import com.movtery.zalithlauncher.ui.screens.game.elements.LogBox
import com.movtery.zalithlauncher.ui.screens.game.elements.LogState
import com.movtery.zalithlauncher.ui.screens.game.elements.SendKeycodeOperation
import com.movtery.zalithlauncher.ui.screens.game.elements.SendKeycodeState
import com.movtery.zalithlauncher.ui.screens.game.multiplayer.TerracottaOperation
import com.movtery.zalithlauncher.ui.screens.game.multiplayer.rememberTerracottaViewModel
import com.movtery.zalithlauncher.utils.currentGameDisplayLayout
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import com.movtery.zalithlauncher.viewmodel.GamepadViewModel
import com.movtery.zalithlauncher.viewmodel.sendToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.lwjgl.glfw.CallbackBridge
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "GameScreen"

private class GameViewModel(
    private val version: Version,
    private val onChangeTextInputMode: (TextInputMode?) -> Unit
) : ViewModel() {
    /** 游戏菜单操作状态 */
    var gameMenuState by mutableStateOf(MenuState.NONE)
    /** 游戏菜单-控制设置区域Tab选择的索引 */
    var controlMenuTabIndex by mutableIntStateOf(0)
    /** 强制关闭弹窗操作状态 */
    var forceCloseState by mutableStateOf<ForceCloseOperation>(ForceCloseOperation.None)
    /** 发送键值操作状态 */
    var sendKeycodeState by mutableStateOf<SendKeycodeState>(SendKeycodeState.None)
    /** 被控制布局层标记为仅滑动的指针列表 */
    var moveOnlyPointers = mutableSetOf<PointerId>()
    /** 鼠标触摸指针处理层占用指针列表 */
    var occupiedPointers = mutableSetOf<PointerId>()

    /** 游戏内帧率状态 */
    var gameFps by mutableIntStateOf(0)
        private set
    /** Active screen refresh rate (Hz) and frames the game window drew in the last second; 0 = not measured */
    var refreshHz by mutableIntStateOf(0)
        private set
    var windowFps by mutableIntStateOf(0)
        private set
    private var fpsJob: Job? = null
    private var activeStats: com.movtery.zalithlauncher.all.unlockfps.AllDisplayStats? = null
    /** 开始帧率捕获 */
    fun startFpsCapture(stats: com.movtery.zalithlauncher.all.unlockfps.AllDisplayStats? = null) {
        //开启一个新的协程，每秒更新一次帧率数据
        stats?.start()
        activeStats = stats
        fpsJob =viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                runCatching {
                    ensureActive()
                }.onFailure {
                    break
                }
                gameFps = CallbackBridge.getCurrentFps()
                if (stats != null) { //same 1 s tick, no extra polling
                    refreshHz = Math.round(stats.hz())
                    windowFps = stats.takeWindowFrames()
                    if (com.movtery.zalithlauncher.BuildConfig.DEBUG) android.util.Log.i("ALLHUD", "fps=$gameFps hz=$refreshHz win=$windowFps") //debug builds only
                }
                delay(1000L.milliseconds)
            }
        }
    }
    /** 停止帧率捕获 */
    fun stopFpsCapture() {
        fpsJob?.cancel()
        fpsJob = null
        activeStats?.stop()
        activeStats = null
    }

    var editorRefresh by mutableIntStateOf(0)
    fun reloadControls() { editorRefresh++ }

    /** 是否正在编辑布局 */
    var isEditingLayout by mutableStateOf(false)
        private set

    /** ALL: open the BOUY Studio over the running game (inside this process; leaving it goes through [exitControlEditor]) */
    fun startStudio() {
        if (!isEditingLayout) {
            clearState()
            isEditingLayout = true
        }
    }

    /** 虚拟鼠标滚动事件处理 */
    val mouseScrollUpEvent = MouseScrollEvent(viewModelScope, 1.0)
    val mouseScrollDownEvent = MouseScrollEvent(viewModelScope, -1.0)

    /** 游戏内消息发送器 */
    val gameTextSender = GameTextSender(viewModelScope)


    /** 处理控制布局类点击事件 */
    fun onKeyEvent(event: ClickEvent, pressed: Boolean) {
        val key = event.key
        when (event.type) {
            ClickEvent.Type.Key -> {
                lwjglEvent(
                    eventKey = key,
                    isMouse = key.startsWith("GLFW_MOUSE_", false),
                    isPressed = pressed
                )
            }
            ClickEvent.Type.LauncherEvent -> {
                launcherEvent(
                    eventKey = key,
                    isPressed = pressed,
                    onSwitchIME = { onChangeTextInputMode(null) },
                    onSwitchMenu = { switchMenu() },
                    onSingleScrollUp = { mouseScrollUpEvent.scrollSingle() },
                    onSingleScrollDown = { mouseScrollDownEvent.scrollSingle() },
                    onLongScrollUp = { mouseScrollUpEvent.scrollLongPress() },
                    onLongScrollUpCancel = { mouseScrollUpEvent.cancel() },
                    onLongScrollDown = { mouseScrollDownEvent.scrollLongPress() },
                    onLongScrollDownCancel = { mouseScrollDownEvent.cancel() }
                )
            }
            ClickEvent.Type.SendText -> {
                //游戏内文本发送事件
                if (pressed) {
                    val text = event.key
                    val inGame = ZLBridgeStates.cursorMode.value == CURSOR_DISABLED
                    gameTextSender.send(GameTextSender.Data(text, inGame))
                }
                return
            }
            else -> return
        }
    }


    /**
     * ALL: leave the BOUY Studio (the layer reloads the saved layout through [editorRefresh])
     */
    fun exitControlEditor() {
        viewModelScope.launch(Dispatchers.Main) {
            if (isEditingLayout) {
                isEditingLayout = false
                editorRefresh++
            }
        }
    }

    /**
     * 切换游戏菜单
     */
    fun switchMenu() {
        this.gameMenuState = this.gameMenuState.next()
    }

    /**
     * 清除所有游戏状态
     */
    fun clearState() {
        mouseScrollUpEvent.cancel()
        mouseScrollDownEvent.cancel()
        gameTextSender.cancel()
        onChangeTextInputMode(TextInputMode.DISABLE)
        moveOnlyPointers.clear()
        occupiedPointers.clear()
    }


    override fun onCleared() {
        clearState()
    }
}

/**
 * 鼠标滚轮事件管理
 * @param offset 滚轮滚动距离
 */
private class MouseScrollEvent(
    private val scope: CoroutineScope,
    private val offset: Double
) {
    private var mouseScrollJob: Job? = null

    /**
     * 取消滚动事件，并重置状态
     */
    fun cancel() {
        mouseScrollJob?.cancel()
        mouseScrollJob = null
    }

    /**
     * 单击响应一次滚轮滚动事件
     */
    fun scrollSingle() {
        CallbackBridge.sendScroll(0.0, offset)
    }

    /**
     * 长按不间断触发滚轮滚动事件
     */
    fun scrollLongPress() {
        mouseScrollJob?.cancel()
        mouseScrollJob = scope.launch {
            while (true) {
                try {
                    ensureActive()
                    CallbackBridge.sendScroll(0.0, offset)
                    delay(50L.milliseconds)
                } catch (_: Exception) {
                    break
                }
            }
            mouseScrollJob = null
        }
    }
}

/**
 * 游戏内消息发送器
 */
private class GameTextSender(private val scope: CoroutineScope) {
    /**
     * @param text 要发送的文本
     * @param inGame 当前是否处于游戏内，如果在游戏中，则会尝试打开聊天栏
     */
    data class Data(
        val text: String,
        val inGame: Boolean
    )

    private var messageChannel: Channel<Data>? = null
    private var job: Job? = null

    fun cancel() {
        job?.cancel()
        messageChannel?.close()
        messageChannel = null
        job = null
    }

    /**
     * 尝试向游戏发送文本（排队发送）
     */
    fun send(data: Data) {
        if (job?.isActive != true || messageChannel == null) {
            job?.cancel()
            messageChannel?.close()

            messageChannel = Channel(Channel.UNLIMITED)
            job = scope.launch {
                messageChannel?.let { channel ->
                    for ((text, inGame) in channel) {
                        sendMessage(text, inGame)
                    }
                }
            }
        }

        messageChannel?.trySend(data)
    }

    private suspend fun sendMessage(text: String, inGame: Boolean) {
        withContext(Dispatchers.Main) {
            //only the SDL render path (MC 26.3+) submits through SDL; a gamepad-only SDL init keeps the GLFW bridge (upstream 362d7322)
            val useSdl = SdlBridge.sdlEnabled && SdlBridge.isSdlRenderActive()

            fun sendText() {
                for (ch in text) {
                    if (useSdl) {
                        SdlTextSender.sendChar(ch)
                    } else {
                        LWJGLCharSender.sendChar(ch)
                    }
                }
            }

            if (inGame) {
                //根据options.txt中的配置，找到打开聊天栏的键
                //如果找不到，则忽略这次事件
                mapToKeycode(OPEN_CHAT, OPEN_CHAT_VALUE)?.let { openChat ->
                    if (useSdl) {
                        SdlTextSender.sendKey(openChat)
                        //wait until the game activates text input; text sent before that is dropped by SDL
                        if (SdlTextSender.awaitTextInputActive()) {
                            sendText()
                            delay(50L.milliseconds)
                            SdlTextSender.sendEnter()
                        }
                    } else {
                        CallbackBridge.sendKeyPress(openChat)
                        delay(50L.milliseconds)
                        sendText()
                        delay(50L.milliseconds)
                        LWJGLCharSender.sendEnter()
                    }
                }
            } else {
                //如果当前不在游戏内，则直接发送文本
                sendText()
            }
        }
    }
}

@Composable
private fun rememberGameViewModel(
    version: Version,
    onChangeTextInputMode: (TextInputMode?) -> Unit
) = viewModel(
    key = version.toString()
) {
    GameViewModel(version, onChangeTextInputMode)
}


@Composable
fun GameScreen(
    version: Version,
    gameHandler: GameHandler,
    showGameInfo: Boolean,
    onInfoBoxClose: () -> Unit,
    logState: LogState,
    onLogStateChange: (LogState) -> Unit,
    textInputMode: TextInputMode,
    isTouchProxyEnabled: Boolean,
    onInputAreaRectUpdated: (IntRect?) -> Unit,
    getAccountName: () -> String?,
    eventViewModel: EventViewModel,
    gamepadViewModel: GamepadViewModel,
) {
    val context = LocalContext.current
    val viewModel = rememberGameViewModel(version) { mode ->
        eventViewModel.sendEvent(EventViewModel.Event.Game.SwitchIme(mode))
    }
    val cursorMode by ZLBridgeStates.cursorMode.collectAsStateWithLifecycle()
    val isGrabbing = remember(cursorMode) {
        cursorMode == CURSOR_DISABLED
    }
    val terracottaViewModel = rememberTerracottaViewModel(
        keyTag = gameHandler.toString() + "_Terracotta",
        gameHandler = gameHandler,
        eventViewModel = eventViewModel,
        getUserName = getAccountName
    )

    LaunchedEffect(viewModel.isEditingLayout, viewModel.gameMenuState) {
        //向VMActivity同步状态，编辑控制布局或打开游戏菜单时，不会继续处理按键事件
        val allowKeyHandle = !viewModel.isEditingLayout && viewModel.gameMenuState != MenuState.SHOW
        eventViewModel.sendEvent(EventViewModel.Event.Game.KeyHandle(allowKeyHandle))
    }

    SendKeycodeOperation(
        operation = viewModel.sendKeycodeState,
        onChange = { viewModel.sendKeycodeState = it },
        lifecycleScope = viewModel.viewModelScope
    )

    ForceCloseOperation(
        operation = viewModel.forceCloseState,
        onChange = { viewModel.forceCloseState = it },
        onForceClose = {
            Terracotta.setWaiting(false)
            ZLNativeInvoker.jvmExit(0, false)
        },
        text = stringResource(R.string.game_menu_option_force_close_text)
    )


    TerracottaOperation(
        viewModel = terracottaViewModel,
        onShowToast = { text, duration ->
            eventViewModel.sendToast(text, duration)
        }
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val screenSize = rememberBoxSize()

        if (!viewModel.isEditingLayout) {
            if (AllSettings.gamepadControl.state) {
                GamepadOnActionListener(
                    gamepadViewModel = gamepadViewModel,
                    onAction = {}
                )
            }

            if (AllSettings.gamepadControl.state && gamepadViewModel.gamepadEngaged) {
                //手柄事件监听
                GamepadKeyListener(
                    gamepadViewModel = gamepadViewModel,
                    isGrabbing = isGrabbing,
                    onKeyEvent = { events, pressed ->
                        events.forEach { event ->
                            viewModel.onKeyEvent(event, pressed)
                        }
                    }
                )

                //手柄摇杆控制移动事件监听
                GamepadStickMovementListener(
                    gamepadViewModel = gamepadViewModel,
                    isGrabbing = isGrabbing,
                    onKeyEvent = { event, pressed ->
                        viewModel.onKeyEvent(event, pressed)
                    }
                )
            }

            //控制布局层
            //ALL BOUY: the new sprite control layer (replaces the old ControlBoxLayout; same occupied-pointer contract with the camera layer)
            val bouyLayout = remember(viewModel.editorRefresh) { com.movtery.zalithlauncher.all.bouy.BouyStore.load(context) }
            com.movtery.zalithlauncher.all.bouy.BouyControls(
                layout = bouyLayout,
                isGrabbing = isGrabbing,
                opacity = (AllSettings.controlsOpacity.state.toFloat() / 100f).coerceIn(0f, 1f),
                onEvent = { e, pressed -> viewModel.onKeyEvent(e, pressed) },
                isOccupied = { viewModel.occupiedPointers.contains(it) },
                onOccupied = { viewModel.occupiedPointers.add(it) },
                onRelease = { viewModel.occupiedPointers.remove(it); viewModel.moveOnlyPointers.remove(it) },
                onMoveOnly = { viewModel.moveOnlyPointers.add(it) },
                modifier = Modifier.fillMaxSize()
            ) {
                //虚拟鼠标控制层
                MouseControlLayout(
                    isTouchProxyEnabled = isTouchProxyEnabled,
                    modifier = Modifier.fillMaxSize(),
                    cursorMode = cursorMode,
                    screenSize = screenSize,
                    onInputAreaRectUpdated = onInputAreaRectUpdated,
                    textInputMode = textInputMode,
                    isMoveOnlyPointer = { viewModel.moveOnlyPointers.contains(it) },
                    onOccupiedPointer = { viewModel.occupiedPointers.add(it) },
                    onReleasePointer = {
                        viewModel.occupiedPointers.remove(it)
                        viewModel.moveOnlyPointers.remove(it)
                    },
                    onMouseMoved = {},
                    onTouch = {},
                    gamepadViewModel = gamepadViewModel.takeIf { AllSettings.gamepadControl.state }
                )
            }

            //物品栏触发层
            val gameDisplayLayout = currentGameDisplayLayout(screenSize)
            MinecraftHotbar(
                screenSize = screenSize,
                rule = AllSettings.hotbarRule.state,
                widthPercentage = AllSettings.hotbarWidth.state.hotbarPercentage(),
                heightPercentage = AllSettings.hotbarHeight.state.hotbarPercentage(),
                sendKeycode = { keycode ->
                    CallbackBridge.sendKeyPress(keycode)
                },
                isGrabbing = isGrabbing,
                displayOffset = gameDisplayLayout.offset,
                onOccupiedPointer = { viewModel.occupiedPointers.add(it) },
                onReleasePointer = { viewModel.occupiedPointers.remove(it) }
            )
        }

        //陀螺仪控制
        val isGyroscopeAvailable = remember(context) {
            isGyroscopeAvailable(context = context)
        }
        if (isGrabbing && isGyroscopeAvailable && AllSettings.gyroscopeControl.state) {
            GyroscopeReader(
                xEvent = { delta ->
                    CallbackBridge.sendCursorDelta(if (AllSettings.gyroscopeInvertX.state) -delta else delta, 0f)
                },
                yEvent = { delta ->
                    CallbackBridge.sendCursorDelta(0f, if (AllSettings.gyroscopeInvertY.state) delta else -delta)
                },
                sampleRate = AllSettings.gyroscopeSampleRate.state,
                smoothing = AllSettings.gyroscopeSmoothing.state,
                smoothingWindow = AllSettings.gyroscopeSmoothingWindow.state,
                sensitivity = AllSettings.gyroscopeSensitivity.state / 100f
            )
        }

        GameInfoBox(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(all = 16.dp),
            versionName = version.getVersionName(),
            versionInfo = version.getVersionInfo()?.getInfoString(),
            visible = showGameInfo,
            onClose = onInfoBoxClose,
            icon = { com.movtery.zalithlauncher.all.bridge.AllVersionIcon(version = version, modifier = Modifier.size(32.dp)) }
        )

        LogBox(
            enableLog = !viewModel.isEditingLayout && logState.value,
            onClose = {
                onLogStateChange(LogState.CLOSE)
            },
            modifier = Modifier.fillMaxSize()
        )

        //ALL: XP Luna quick menu (UI rework of GameMenuSubscreen, which stays in the source); same callbacks and settings
        com.movtery.zalithlauncher.all.game.AllGameMenu(
            state = viewModel.gameMenuState,
            gamepadViewModel = gamepadViewModel,
            closeScreen = { viewModel.gameMenuState = MenuState.HIDE },
            onForceCloseConfirmed = {
                Terracotta.setWaiting(false)
                ZLNativeInvoker.jvmExit(0, false)
            },
            onSwitchLog = { onLogStateChange(logState.next()) },
            enableTerracotta = AllSettings.enableTerracotta.state,
            onOpenTerracottaMenu = { terracottaViewModel.openMenu() },
            onRefreshWindowSize = { eventViewModel.sendEvent(EventViewModel.Event.Game.RefreshSize) },
            onInputMethod = {
                eventViewModel.sendEvent(EventViewModel.Event.Game.SwitchIme(null))
            },
            onFullKeyboard = { viewModel.sendKeycodeState = SendKeycodeState.ShowDialog },
            onOpenStudio = { viewModel.startStudio() },
            onProfileSwitched = { viewModel.reloadControls() },
            onShowToast = { text, duration ->
                eventViewModel.sendToast(text, duration)
            }
        )

        if (AllSettings.gamepadControl.state) {
            //手柄事件捕获层
            SimpleGamepadCapture(
                gamepadViewModel = gamepadViewModel
            )
        }

        //手柄输入模式选择询问
        GamepadModePromptDialog(
            visible = gamepadViewModel.modePromptVisible,
            onConfirm = { mode ->
                gamepadViewModel.confirmModePrompt(mode)
            }
        )

        if (viewModel.isEditingLayout) {
            //ALL BOUY Studio (the only control editor)
            com.movtery.zalithlauncher.all.ui.AllMaterialTheme {
                com.movtery.zalithlauncher.all.bouy.BouyStudio(remember { com.movtery.zalithlauncher.all.bouy.BouyStore.load(context) }, onExit = { viewModel.exitControlEditor() })
            }
        } else {
            if (AllSettings.showMenuBall.state) {
                //在这里根据设置决定是否启用帧率捕获协程
                val showFps = AllSettings.showFPS.state
                val hudActivity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
                DisposableEffect(showFps) {
                    if (showFps) viewModel.startFpsCapture(hudActivity?.let { com.movtery.zalithlauncher.all.unlockfps.AllDisplayStats(it) })
                    onDispose {
                        viewModel.stopFpsCapture()
                    }
                }

                val gameFps: Int? = if (showFps) {
                    viewModel.gameFps
                } else {
                    null
                }

                DraggableGameBall(
                    position = AllSettings.menuBallPos.state,
                    onPositionChanged = {
                        AllSettings.menuBallPos.updateState(it)
                    },
                    onSavePos = {
                        AllSettings.menuBallPos.save()
                    },
                    gameFps = gameFps,
                    refreshHz = if (showFps) viewModel.refreshHz else 0,
                    windowFps = if (showFps) viewModel.windowFps else 0,
                    showMemory = AllSettings.showMemory.state,
                    opened = viewModel.gameMenuState == MenuState.SHOW,
                    alpha = AllSettings.menuBallOpacity.state / 100f,
                    onClick = {
                        viewModel.switchMenu()
                    }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        eventViewModel.events
            .filterIsInstance<EventViewModel.Event.Game>()
            .collect { event ->
                when (event) {
                    is EventViewModel.Event.Game.OnBack -> {
                        if (viewModel.isEditingLayout) {
                            //ALL BOUY Studio handles Back itself (BackHandler: try mode / unsaved changes)
                        } else if (!AllSettings.showMenuBall.getValue()) {
                            viewModel.switchMenu()
                        } else {
                            //按下返回键
                            val event = ClickEvent(
                                type = ClickEvent.Type.Key,
                                key = ControlEventKeycode.GLFW_KEY_ESCAPE
                            )
                            viewModel.onKeyEvent(event, true)
                            delay(10L.milliseconds)
                            viewModel.onKeyEvent(event, false)
                        }
                    }
                    is EventViewModel.Event.Game.OnResume -> {
                        viewModel.clearState()
                    }
                    else -> { /*忽略*/ }
                }
            }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GameInfoBox(
    versionName: String,
    versionInfo: String?,
    visible: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {},
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        //ALL V3: small flat navy card. Only data we really have: the game's icon / name / version info; the bar is indeterminate (no fake percentage)
        val shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
        val dim = androidx.compose.ui.graphics.Color(0xFF8C97AE)
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .shadow(8.dp, shape)
                .background(androidx.compose.ui.graphics.Color(0xFF161B28), shape)
                .border(1.dp, androidx.compose.ui.graphics.Color(0xFF2D3548), shape)
                .clip(shape)
                .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Box(Modifier.size(40.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp)).background(androidx.compose.ui.graphics.Color(0xFF232B3D)), contentAlignment = Alignment.Center) { icon() }
                Column(Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.all_game_entering), color = dim, fontSize = 11.sp, maxLines = 1)
                    Text(
                        text = versionName, color = androidx.compose.ui.graphics.Color.White, fontSize = 15.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    versionInfo?.let { info ->
                        Text(text = info, color = dim, fontSize = 11.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
                IconButton(modifier = Modifier.size(36.dp), onClick = onClose) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(R.drawable.ic_close),
                        tint = dim,
                        contentDescription = stringResource(R.string.generic_close)
                    )
                }
            }
            androidx.compose.material3.LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(end = 10.dp).height(5.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)),
                color = androidx.compose.ui.graphics.Color(0xFF2F6FED),
                trackColor = androidx.compose.ui.graphics.Color(0xFF30415F)
            )
        }
    }
}

@Preview(showBackground = false)
@Composable
private fun PreviewGameInfoBox() {
    MaterialExpressiveTheme {
        GameInfoBox(
            versionName = "1.21.11",
            versionInfo = "1.21.11",
            visible = true,
            onClose = {}
        )
    }
}

/**
 * 鼠标控制层
 * @param isTouchProxyEnabled 是否启用控制代理（TouchController模组支持）
 * @param cursorMode 当前鼠标模式
 * @param textInputMode 输入法状态
 * @param isMoveOnlyPointer 检查指针是否被标记为仅处理滑动事件
 * @param onOccupiedPointer 标记指针已被占用
 * @param onReleasePointer 标记指针已被释放
 * @param onMouseMoved 实体鼠标操作时回调
 * @param onTouch 手指触摸操作鼠标层时回调
 */
@Composable
private fun MouseControlLayout(
    isTouchProxyEnabled: Boolean,
    modifier: Modifier = Modifier,
    cursorMode: Int,
    screenSize: IntSize,
    onInputAreaRectUpdated: (IntRect?) -> Unit,
    textInputMode: TextInputMode,
    isMoveOnlyPointer: (PointerId) -> Boolean,
    onOccupiedPointer: (PointerId) -> Unit,
    onReleasePointer: (PointerId) -> Unit,
    onMouseMoved: () -> Unit,
    onTouch: () -> Unit,
    gamepadViewModel: GamepadViewModel?
) {
    Box(
        modifier = modifier
            .then(
                if (isTouchProxyEnabled) {
                    Modifier
                        .touchControllerTouchModifier(
                            screenSize = screenSize
                        )
                        .touchControllerInputModifier(
                            screenSize = screenSize,
                            onInputAreaRectUpdated = onInputAreaRectUpdated,
                        )
                } else Modifier
            )
    ) {

        val capturedSpeedFactor = AllSettings.mouseCaptureSensitivity.state / 100f
        val capturedTapMouseAction = AllSettings.gestureTapMouseAction.state.toAction()
        val capturedLongPressMouseAction = AllSettings.gestureLongPressMouseAction.state.toAction()

        SwitchableMouseLayout(
            modifier = Modifier.fillMaxSize(),
            screenSize = screenSize,
            cursorMode = cursorMode,
            onTouch = onTouch,
            onMouse = onMouseMoved,
            gamepadViewModel = gamepadViewModel,
            onTap = { position ->
                val gamePosition = currentGameDisplayLayout(screenSize).mapToGame(position)
                CallbackBridge.putMouseEventWithCoords(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), gamePosition.x, gamePosition.y)
            },
            onCapturedTap = {
                if (AllSettings.gestureControl.state) {
                    CallbackBridge.putMouseEvent(capturedTapMouseAction)
                }
            },
            onLongPress = {
                CallbackBridge.putMouseEvent(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), true)
            },
            onLongPressEnd = {
                CallbackBridge.putMouseEvent(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), false)
            },
            onCapturedLongPress = {
                if (AllSettings.gestureControl.state) {
                    CallbackBridge.putMouseEvent(capturedLongPressMouseAction, true)
                }
            },
            onCapturedLongPressEnd = {
                if (AllSettings.gestureControl.state) {
                    CallbackBridge.putMouseEvent(capturedLongPressMouseAction, false)
                }
            },
            onPointerMove = { pos ->
                pos.sendPosition(screenSize)
            },
            onCapturedMove = { delta ->
                CallbackBridge.sendCursorDelta(
                    delta.x * capturedSpeedFactor,
                    delta.y * capturedSpeedFactor
                )
            },
            onMouseScroll = { scroll ->
                CallbackBridge.sendScroll(scroll.x.toDouble(), scroll.y.toDouble())
            },
            onMouseButton = { button, pressed ->
                val code = LWJGLCharSender.getMouseButton(button) ?: return@SwitchableMouseLayout
                CallbackBridge.sendMouseButton(code.toInt(), pressed)
            },
            isMoveOnlyPointer = isMoveOnlyPointer,
            onOccupiedPointer = onOccupiedPointer,
            onReleasePointer = onReleasePointer,
            enableScrollGesture = AllSettings.gestureControl.state,
            onScrollGesture = { scroll ->
                CallbackBridge.sendScroll(scroll.x.toDouble(), scroll.y.toDouble())
            }
        )
    }
}

private fun Offset.sendPosition(screenSize: IntSize) {
    val gamePosition = currentGameDisplayLayout(screenSize).mapToGame(this)
    CallbackBridge.sendCursorPos(gamePosition.x, gamePosition.y)
}
