package com.movtery.zalithlauncher.ui.screens.content

import java.io.File

/**
 * 实例卡片上的“已安装模组”数量：只统计 mods 目录顶层的 .jar 与 .jar.disabled，不统计子目录与嵌套 jar。
 */
internal fun countInstalledModJars(gameDir: File): Int =
    File(gameDir, "mods").listFiles { f -> f.name.endsWith(".jar") || f.name.endsWith(".jar.disabled") }?.size ?: 0
