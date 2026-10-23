package com.movtery.zalithlauncher.all.launchersettings

import java.io.File

/**
 * File side of the ALL Cursor tab. The cursor images stay where Zalith keeps them (DIR_MOUSE_POINTER/<shape>.image, read by
 * MousePointer); this only adds a staging copy and one backup so that nothing the user had is lost:
 *  - Import writes to `.staging/<name>` after validation; the live file is untouched until Apply.
 *  - Apply keeps the previous live file as `<name>.bak` and swaps the new one in through a temp file.
 *  - Restore swaps `<name>.bak` back. Cancel only deletes the staged copy.
 */
enum class CursorFormat { PNG, GIF, WEBP, JPEG, ANI, CUR }

enum class CursorImportError { TOO_LARGE, UNSUPPORTED, CORRUPT, TOO_BIG_SIDE, ANIMATION_TOO_BIG }

const val CURSOR_MAX_FILE_BYTES = 2 * 1024 * 1024
const val CURSOR_MAX_STATIC_SIDE = 512

fun sniffCursorFormat(bytes: ByteArray): CursorFormat? {
    fun at(i: Int, vararg b: Int) = bytes.size >= i + b.size && b.indices.all { bytes[i + it] == b[it].toByte() }
    return when {
        at(0, 0x89, 0x50, 0x4E, 0x47) -> CursorFormat.PNG
        at(0, 0x47, 0x49, 0x46, 0x38) -> CursorFormat.GIF
        at(0, 0x52, 0x49, 0x46, 0x46) && at(8, 0x57, 0x45, 0x42, 0x50) -> CursorFormat.WEBP
        at(0, 0x52, 0x49, 0x46, 0x46) && at(8, 0x41, 0x43, 0x4F, 0x4E) -> CursorFormat.ANI
        at(0, 0xFF, 0xD8, 0xFF) -> CursorFormat.JPEG
        at(0, 0x00, 0x00, 0x02, 0x00) || at(0, 0x00, 0x00, 0x01, 0x00) -> CursorFormat.CUR
        else -> null
    }
}

class CursorStore(private val live: File) {
    private val dir: File get() = live.parentFile!!
    val staged: File get() = File(File(dir, ".staging"), live.name)
    val backup: File get() = File(dir, live.name + ".bak")

    fun hasCustom() = live.isFile
    fun hasStaged() = staged.isFile
    fun hasBackup() = backup.isFile

    fun stage(bytes: ByteArray) {
        staged.parentFile!!.mkdirs()
        staged.writeBytes(bytes)
    }

    fun discard() {
        staged.delete()
    }

    /** @param toDefault true = the user chose Reset: the live file goes to the backup and the built-in cursor is used */
    fun apply(toDefault: Boolean) {
        if (toDefault) {
            if (live.isFile) {
                live.copyTo(backup, overwrite = true)
                live.delete()
            }
            discard()
            return
        }
        if (!staged.isFile) return
        if (live.isFile) live.copyTo(backup, overwrite = true)
        val tmp = File(dir, live.name + ".tmp")
        staged.copyTo(tmp, overwrite = true)
        live.delete()
        if (!tmp.renameTo(live)) {
            //put the previous cursor back rather than leave nothing
            if (backup.isFile) backup.copyTo(live, overwrite = true)
            tmp.delete()
            error("Could not replace the cursor file")
        }
        discard()
    }

    /** Swap the previous cursor (backup) and the current one; doing it twice gets the current one back */
    fun restore() {
        if (!backup.isFile) return
        val tmp = File(dir, live.name + ".swap")
        val hadLive = live.isFile
        if (hadLive) live.copyTo(tmp, overwrite = true)
        backup.copyTo(live, overwrite = true)
        if (hadLive) {
            tmp.copyTo(backup, overwrite = true)
            tmp.delete()
        } else {
            backup.delete()
        }
    }
}
