package com.movtery.zalithlauncher.all.launchersettings

import android.graphics.BitmapFactory
import android.graphics.Movie
import androidx.compose.ui.graphics.asAndroidBitmap
import com.movtery.zalithlauncher.ui.control.mouse.CursorAnimLimits
import com.movtery.zalithlauncher.ui.control.mouse.CursorHotspot
import com.movtery.zalithlauncher.ui.control.mouse.aniHotspotPercent
import com.movtery.zalithlauncher.ui.control.mouse.curAsIco
import com.movtery.zalithlauncher.ui.control.mouse.curInfo
import com.movtery.zalithlauncher.ui.control.mouse.decodeAniFrames
import com.movtery.zalithlauncher.ui.control.mouse.decodeCursorFrames
import com.movtery.zalithlauncher.ui.control.mouse.parseAni

/** Result of checking an imported cursor: an error, or OK with the hotspot the file itself carries (.ani / .cur) */
class CursorCheck(val error: CursorImportError?, val hotspot: CursorHotspot? = null)

/**
 * Full check of an imported cursor before it is staged. Everything is decoded once here, so a file that passes is known to
 * render; a file that fails never reaches the cursor folder.
 */
@Suppress("DEPRECATION")
fun checkCursorBytes(bytes: ByteArray): CursorCheck {
    if (bytes.size > CURSOR_MAX_FILE_BYTES) return CursorCheck(CursorImportError.TOO_LARGE)
    val format = sniffCursorFormat(bytes) ?: return CursorCheck(CursorImportError.UNSUPPORTED)
    if (format == CursorFormat.ANI) {
        val ani = parseAni(bytes) ?: return CursorCheck(CursorImportError.CORRUPT)
        val frames = decodeAniFrames(bytes) ?: return CursorCheck(CursorImportError.ANIMATION_TOO_BIG)
        frames.bitmaps.distinct().forEach { it.asAndroidBitmap().recycle() }
        return CursorCheck(null, aniHotspotPercent(ani))
    }
    if (format == CursorFormat.CUR) {
        val info = curInfo(bytes) ?: return CursorCheck(CursorImportError.CORRUPT)
        if (info.width > CURSOR_MAX_STATIC_SIDE || info.height > CURSOR_MAX_STATIC_SIDE) return CursorCheck(CursorImportError.TOO_BIG_SIDE)
        val bmp = BitmapFactory.decodeByteArray(curAsIco(bytes), 0, bytes.size) ?: return CursorCheck(CursorImportError.CORRUPT)
        bmp.recycle()
        return CursorCheck(null, CursorHotspot(info.hotspotX * 100 / info.width, info.hotspotY * 100 / info.height))
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return CursorCheck(CursorImportError.CORRUPT)
    if (bounds.outWidth > CURSOR_MAX_STATIC_SIDE || bounds.outHeight > CURSOR_MAX_STATIC_SIDE) return CursorCheck(CursorImportError.TOO_BIG_SIDE)
    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return CursorCheck(CursorImportError.CORRUPT)
    bmp.recycle()
    if (format == CursorFormat.GIF) {
        val movie = Movie.decodeByteArray(bytes, 0, bytes.size) ?: return CursorCheck(CursorImportError.CORRUPT)
        if (movie.duration() > 0) {
            if (movie.width() > CursorAnimLimits.MAX_SIDE || movie.height() > CursorAnimLimits.MAX_SIDE) return CursorCheck(CursorImportError.ANIMATION_TOO_BIG)
            val frames = decodeCursorFrames(bytes) ?: return CursorCheck(CursorImportError.ANIMATION_TOO_BIG)
            frames.bitmaps.forEach { it.asAndroidBitmap().recycle() }
        }
    }
    return CursorCheck(null)
}

