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

package com.movtery.zalithlauncher.ui.screens.main

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.BuildKeys
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.activities.CrashType
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.ScalingActionButton
import com.movtery.zalithlauncher.ui.components.verticalScrollWithBar
import com.movtery.zalithlauncher.ui.theme.backgroundColor
import com.movtery.zalithlauncher.ui.theme.onBackgroundColor

@Composable
fun ErrorScreen(
    crashType: CrashType,
    shareLogs: Boolean = true,
    canUpload: Boolean = false,
    canRestart: Boolean = true,
    /** 崩溃之后的诊断文案，第一行是标题；为空时页面与过去完全一致 */
    diagnosisLines: List<String> = emptyList(),
    /** 主要修复动作（安装依赖并修复）的文案；为 null 时不显示 */
    repairActionLabel: String? = null,
    onRepairActionClick: () -> Unit = {},
    /** 备用修复动作（临时禁用）的文案；为 null 时不显示任何备用动作 */
    fixActionLabel: String? = null,
    onFixActionClick: () -> Unit = {},
    onShareLogsClick: () -> Unit = {},
    onUploadClick: () -> Unit = {},
    onRestartClick: () -> Unit = {},
    onExitClick: () -> Unit = {},
    onOrientationChanged: (Int) -> Unit = {},
    details: String = "",
    onCopyLog: (() -> Unit)? = null,
    body: @Composable ColumnScope.() -> Unit
) {
    if (AllSettings.launcherInterfaceMode.state == com.movtery.zalithlauncher.setting.enums.InterfaceMode.ALL_DESKTOP) {
        com.movtery.zalithlauncher.all.overlays.AllErrorScreen(
            title = if (crashType == CrashType.LAUNCHER_CRASH) stringResource(R.string.crash_launcher_title, "ALL Launcher") else "ALL Launcher",
            type = stringResource(R.string.crash_type, stringResource(crashType.textRes)),
            diagnosisLines = diagnosisLines, details = details, shareLogs = shareLogs, canUpload = canUpload, canRestart = canRestart,
            repairActionLabel = repairActionLabel, onRepair = onRepairActionClick,
            fixActionLabel = fixActionLabel, onFix = onFixActionClick, onShare = onShareLogsClick, onUpload = onUploadClick,
            onRestart = onRestartClick, onExit = onExitClick, onCopyLog = onCopyLog, body = body)
        return
    }
    //获取方向信息，展示两套不同的UI
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        ErrorScreenLandscape(
            crashType = crashType,
            repairActionLabel = repairActionLabel,
            onRepairActionClick = onRepairActionClick,
            shareLogs = shareLogs,
            canUpload = canUpload,
            canRestart = canRestart,
            diagnosisLines = diagnosisLines,
            fixActionLabel = fixActionLabel,
            onFixActionClick = onFixActionClick,
            onShareLogsClick = onShareLogsClick,
            onUploadClick = onUploadClick,
            onRestartClick = onRestartClick,
            onExitClick = onExitClick,
            onRotateClick = {
                onOrientationChanged(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            },
            body = body
        )
    } else {
        ErrorScreenPortrait(
            crashType = crashType,
            repairActionLabel = repairActionLabel,
            onRepairActionClick = onRepairActionClick,
            shareLogs = shareLogs,
            canUpload = canUpload,
            canRestart = canRestart,
            diagnosisLines = diagnosisLines,
            fixActionLabel = fixActionLabel,
            onFixActionClick = onFixActionClick,
            onShareLogsClick = onShareLogsClick,
            onUploadClick = onUploadClick,
            onRestartClick = onRestartClick,
            onExitClick = onExitClick,
            onRotateClick = {
                onOrientationChanged(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
            },
            body = body
        )
    }
}

/**
 * 崩溃页面（横屏页面）
 */
@Composable
private fun ErrorScreenLandscape(
    crashType: CrashType,
    repairActionLabel: String?,
    onRepairActionClick: () -> Unit,
    shareLogs: Boolean,
    canUpload: Boolean,
    canRestart: Boolean,
    diagnosisLines: List<String>,
    fixActionLabel: String?,
    onFixActionClick: () -> Unit,
    onShareLogsClick: () -> Unit,
    onUploadClick: () -> Unit,
    onRestartClick: () -> Unit,
    onExitClick: () -> Unit,
    onRotateClick: () -> Unit,
    body: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            val text = when (crashType) {
                //在启动器崩溃的时候，显示一个较为严重的标题
                CrashType.LAUNCHER_CRASH -> stringResource(R.string.crash_launcher_title, BuildKeys.LAUNCHER_NAME)
                //游戏运行崩溃了，大概和启动器关系不大，仅展示应用标题
                CrashType.GAME_CRASH -> BuildKeys.LAUNCHER_NAME
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = text)
                //旋转竖屏
                IconButton(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    onClick = onRotateClick
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fullscreen_exit),
                        contentDescription = null
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
        containerColor = backgroundColor(),
        contentColor = onBackgroundColor(),
        contentWindowInsets = if (AllSettings.launcherFullScreen.state) {
            WindowInsets()
        } else {
            WindowInsets.safeContent.only(WindowInsetsSides.Horizontal)
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ErrorContent(
                modifier = Modifier.weight(7f),
                diagnosisLines = diagnosisLines,
                body = body
            )

            ActionLayout(
                modifier = Modifier.weight(3f),
                crashType = crashType,
                repairActionLabel = repairActionLabel,
                onRepairActionClick = onRepairActionClick,
                shareLogs = shareLogs,
                canUpload = canUpload,
                canRestart = canRestart,
                fixActionLabel = fixActionLabel,
                onFixActionClick = onFixActionClick,
                onShareLogsClick = onShareLogsClick,
                onUploadClick = onUploadClick,
                onRestartClick = onRestartClick,
                onExitClick = onExitClick
            )
        }
    }
}

/**
 * 崩溃页面（竖屏版本）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrorScreenPortrait(
    crashType: CrashType,
    repairActionLabel: String?,
    onRepairActionClick: () -> Unit,
    shareLogs: Boolean,
    canUpload: Boolean,
    canRestart: Boolean,
    diagnosisLines: List<String>,
    fixActionLabel: String?,
    onFixActionClick: () -> Unit,
    onShareLogsClick: () -> Unit,
    onUploadClick: () -> Unit,
    onRestartClick: () -> Unit,
    onExitClick: () -> Unit,
    onRotateClick: () -> Unit,
    body: @Composable ColumnScope.() -> Unit
) {
    //控制下拉菜单的显示状态
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = {
                    //应用标题
                    Text(text = BuildKeys.LAUNCHER_NAME)
                },
                actions = {
                    //旋转横屏
                    IconButton(
                        onClick = onRotateClick
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_mobile_rotate_filled),
                            contentDescription = null
                        )
                    }

                    Row {
                        IconButton(
                            onClick = { showMenu = true }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = stringResource(R.string.generic_more)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (canUpload) {
                                DropdownMenuItem(
                                    text = {
                                        MarqueeText(text = stringResource(R.string.crash_link_share_button))
                                    },
                                    onClick = {
                                        showMenu = false
                                        onUploadClick()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    MarqueeText(text = stringResource(R.string.crash_share_logs))
                                },
                                onClick = {
                                    showMenu = false
                                    onShareLogsClick()
                                },
                                enabled = shareLogs
                            )
                            if (canRestart) {
                                DropdownMenuItem(
                                    text = {
                                        MarqueeText(text = stringResource(R.string.crash_restart))
                                    },
                                    onClick = {
                                        showMenu = false
                                        onRestartClick()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    MarqueeText(text = stringResource(R.string.crash_exit))
                                },
                                onClick = {
                                    showMenu = false
                                    onExitClick()
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScrollWithBar(rememberScrollState())
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            //崩溃类型
            Text(
                text = stringResource(
                    R.string.crash_type,
                    stringResource(crashType.textRes)
                )
            )

            if (crashType == CrashType.LAUNCHER_CRASH) {
                //仅在启动器崩溃时，才显示这行略显严重的文本
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.crash_launcher_title, BuildKeys.LAUNCHER_NAME)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    content = body
                )
            } else {
                body()
            }

            if (diagnosisLines.isNotEmpty()) {
                DiagnosisBlock(lines = diagnosisLines)
            }

            //诊断给出的动作直接放在文案下面：用户不该为了修一件事去菜单里找；主要动作在前，备用动作在后
            repairActionLabel?.let { label ->
                ScalingActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRepairActionClick
                ) {
                    MarqueeText(text = label)
                }
            }
            fixActionLabel?.let { label ->
                ScalingActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onFixActionClick
                ) {
                    MarqueeText(text = label)
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    modifier: Modifier = Modifier,
    diagnosisLines: List<String> = emptyList(),
    body: @Composable ColumnScope.() -> Unit
) {
    BackgroundCard(
        modifier = modifier,
        influencedByBackground = false,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScrollWithBar(state = rememberScrollState())
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                body()
                if (diagnosisLines.isNotEmpty()) {
                    DiagnosisBlock(lines = diagnosisLines)
                }
            }
        )
    }
}

/**
 * 崩溃诊断的文案块。第一行当标题（数据文件只管自己是谁，说什么在 Kotlin 里）。
 * 没有诊断时这个函数根本不进组合树，页面和以前一样。
 */
@Composable
private fun DiagnosisBlock(lines: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = lines.first(),
            style = MaterialTheme.typography.titleSmall
        )
        lines.drop(1).forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ActionLayout(
    modifier: Modifier = Modifier,
    crashType: CrashType,
    shareLogs: Boolean,
    canUpload: Boolean,
    canRestart: Boolean,
    repairActionLabel: String? = null,
    onRepairActionClick: () -> Unit = {},
    fixActionLabel: String? = null,
    onFixActionClick: () -> Unit = {},
    onShareLogsClick: () -> Unit = {},
    onUploadClick: () -> Unit = {},
    onRestartClick: () -> Unit = {},
    onExitClick: () -> Unit = {}
) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = stringResource(
                    R.string.crash_type,
                    stringResource(crashType.textRes)
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom)
        ) {
            repairActionLabel?.let { label ->
                ScalingActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRepairActionClick
                ) {
                    MarqueeText(text = label)
                }
            }
            fixActionLabel?.let { label ->
                ScalingActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onFixActionClick
                ) {
                    MarqueeText(text = label)
                }
            }
            if (canUpload) {
                ScalingActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onUploadClick
                ) {
                    MarqueeText(text = stringResource(R.string.crash_link_share_button))
                }
            }
            if (shareLogs) {
                ScalingActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onShareLogsClick
                ) {
                    MarqueeText(text = stringResource(R.string.crash_share_logs))
                }
            }
            if (canRestart) {
                ScalingActionButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRestartClick
                ) {
                    MarqueeText(text = stringResource(R.string.crash_restart))
                }
            }
            ScalingActionButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onExitClick
            ) {
                MarqueeText(text = stringResource(R.string.crash_exit))
            }
        }
    }
}

@Preview
@Composable
private fun PreviewErrorScreen() {
    MaterialExpressiveTheme {
        ErrorScreen(
            crashType = CrashType.LAUNCHER_CRASH,
            shareLogs = true,
            canUpload = true,
            canRestart = true,
            onShareLogsClick = {},
            onUploadClick = {},
            onRestartClick = {},
            onExitClick = {},
            body = {}
        )
    }
}