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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */
package com.movtery.zalithlauncher.game.launch

import java.io.File
import java.util.Properties

/**
 * Tells a normal game exit from "the game process vanished".
 *
 * The game runs in its own `:game` process. A normal exit (the game's own exit hook → [com.movtery.zalithlauncher.bridge.ZLNativeInvoker.jvmExit],
 * a crash page, or "quit") calls [end] first; an exit that bypasses that path leaves the marker behind. The launcher process looks at it
 * when it is shown again ([takeUnexpectedEnd]): a marker whose process is no longer alive means "ended without an exit report".
 *
 * This only records THAT the exit was not reported. It never guesses a cause (memory, kill, crash…): the cause is unknown.
 */
object GameSessionMarker {
    data class Session(val pid: Int, val instance: String, val startedAtMillis: Long, val logPath: String)

    private fun markerFile(filesDir: File) = File(filesDir, "game_session.properties")

    /** Called in the game process once the instance, the log file and the process are known. */
    fun begin(filesDir: File, instance: String, logPath: String, pid: Int = android.os.Process.myPid(), now: Long = System.currentTimeMillis()) {
        runCatching {
            val p = Properties().apply {
                setProperty("pid", pid.toString()); setProperty("instance", instance)
                setProperty("startedAt", now.toString()); setProperty("logPath", logPath)
            }
            markerFile(filesDir).outputStream().use { p.store(it, null) }
        }
    }

    /** Called on every reported exit (normal, non-zero code or signal handled by the launcher). */
    fun end(filesDir: File) {
        runCatching { markerFile(filesDir).delete() }
    }

    /**
     * The session that ended without being reported, or null.
     * [isAlive] tells whether that pid is still the game process (injectable for tests).
     */
    fun takeUnexpectedEnd(filesDir: File, isAlive: (Int) -> Boolean = ::isGameProcessAlive): Session? {
        val file = markerFile(filesDir)
        if (!file.isFile) return null
        val session = runCatching {
            val p = Properties().apply { file.inputStream().use { load(it) } }
            Session(p.getProperty("pid").toInt(), p.getProperty("instance", ""), p.getProperty("startedAt", "0").toLong(), p.getProperty("logPath", ""))
        }.getOrNull()
        if (session == null) {
            runCatching { file.delete() }
            return null
        }
        //still running: leave the marker alone
        if (isAlive(session.pid)) return null
        runCatching { file.delete() }
        return session
    }

    /** `/proc/<pid>/cmdline` ends with ":game" for the game process of this app. */
    private fun isGameProcessAlive(pid: Int): Boolean = runCatching {
        File("/proc/$pid/cmdline").readText().trimEnd('\u0000').endsWith(":game")
    }.getOrDefault(false)
}
