package com.movtery.zalithlauncher.all.unlockfps

import android.app.Activity
import android.os.Build
import android.provider.Settings
import android.view.Display

import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.setting.AllSettings
import java.io.File

/**
 * Unlock FPS: asks Android to run the game window at the panel's highest display mode.
 *
 * Why it is needed (TECNO LI9, Android 15, measured): the game is drawn into a TextureView, so the
 * Surface(SurfaceTexture).setFrameRate vote never reaches SurfaceFlinger (that surface has no SurfaceFlinger layer),
 * and HiOS votes the window layer at 90 Hz "Exact" for non-whitelisted apps even though peak_refresh_rate=120 and the
 * 120 Hz mode is allowed. The window therefore asks for the highest mode through public window/view APIs. Android
 * still clamps the request to the user's own refresh-rate setting; no system setting is written.
 *
 * Independent of Frame Generation, ALL Render and any renderer. Off by default (opt-in).
 */
object AllUnlockFps {
    /** What was requested and what Android reports right now; never an invented number. */
    data class Status(
        val requestedModeId: Int,
        val requestedHz: Float,
        val activeHz: Float,
        val systemPeakHz: Float?,
        val supportedHz: List<Float>
    )

    /** Whether Unlock FPS is requested (setting; debug builds can override with a test file). */
    private data class Requests(val on: Boolean)

    private fun displayOf(activity: Activity): Display? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) activity.display
        else @Suppress("DEPRECATION") activity.windowManager.defaultDisplay

    /** Highest-refresh mode with the current resolution, or null when there is nothing to choose. */
    private fun bestMode(display: Display): Display.Mode? {
        val cur = display.mode
        return display.supportedModes
            .filter { it.physicalWidth == cur.physicalWidth && it.physicalHeight == cur.physicalHeight }
            .maxByOrNull { it.refreshRate }
    }

    /** Read-only: Android's user peak refresh-rate setting if the device exposes it (null otherwise). */
    private fun systemPeak(activity: Activity): Float? = runCatching {
        Settings.System.getFloat(activity.contentResolver, "peak_refresh_rate").takeIf { it > 0f }
    }.getOrNull()

    fun status(activity: Activity): Status? {
        val display = displayOf(activity) ?: return null
        val best = bestMode(display) ?: return null
        val requested = activity.window.attributes.preferredDisplayModeId
        return Status(
            requestedModeId = requested,
            requestedHz = display.supportedModes.firstOrNull { it.modeId == requested }?.refreshRate ?: 0f,
            activeHz = display.refreshRate,
            systemPeakHz = systemPeak(activity),
            supportedHz = display.supportedModes.filter { it.physicalWidth == best.physicalWidth }.map { it.refreshRate }.distinct().sorted()
        )
    }

    private fun requests(activity: Activity): Requests {
        var r = Requests(AllSettings.unlockFps.getValue())
        if (BuildConfig.DEBUG) runCatching { //test switch: <app files>/ALL_UFPS_TEST.txt "on=0|1" (overrides the setting while testing)
            val text = File(activity.getExternalFilesDir(null), "ALL_UFPS_TEST.txt").takeIf { it.isFile }?.readText() ?: return@runCatching
            Regex("on=([01])").find(text)?.groupValues?.get(1)?.let { r = Requests(it == "1") }
        }
        return r
    }

    /**
     * Call from the game activity. Ablation on TECNO (see handoff): preferredRefreshRate alone flips the window vote
     * from 90 Hz Exact to 120 Hz; preferredDisplayModeId and View.requestedFrameRate neither suffice nor are needed,
     * so only the one public request that works is made. Cleared (0) when off, so toggling restores Android's behaviour.
     */
    fun apply(activity: Activity) {
        val r = requests(activity)
        val display = displayOf(activity) ?: return
        val best = bestMode(display) ?: return
        val lp = activity.window.attributes
        val want = if (r.on) best.refreshRate else 0f
        @Suppress("DEPRECATION")
        if (lp.preferredRefreshRate != want) { lp.preferredRefreshRate = want; activity.window.attributes = lp }
    }

    /** Debug builds only: re-apply every 2 s so ablation switches can change while the game runs. */
    fun watchDebug(activity: Activity) {
        if (!BuildConfig.DEBUG) return
        val view = activity.window.decorView
        view.postDelayed(object : Runnable {
            override fun run() {
                if (activity.isDestroyed || activity.isFinishing) return
                apply(activity)
                view.postDelayed(this, 2000)
            }
        }, 2000)
    }
}
