package com.movtery.zalithlauncher.all.unlockfps

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.Window
import java.util.concurrent.atomic.AtomicInteger

/**
 * Cheap display facts for the in-game FPS readout. No extra polling: the existing 1 s FPS tick calls [hz] and
 * [takeWindowFrames]. Three different numbers, never mixed:
 *  - game FPS: frames Minecraft renders (native counter, already shown by the HUD);
 *  - refresh rate: Display.getRefreshRate(), the mode Android currently runs the screen at;
 *  - window frames/s: frames the game window finished drawing (FrameMetrics), a proxy for frames handed to the
 *    compositor. It is compared against SurfaceFlinger's own presented-frame count in the handoff before being trusted.
 */
class AllDisplayStats(private val activity: Activity) {
    private val frames = AtomicInteger(0)
    private var thread: HandlerThread? = null
    private val listener = Window.OnFrameMetricsAvailableListener { _, _, _ -> frames.incrementAndGet() }

    fun start() {
        if (thread != null) return
        val t = HandlerThread("all-frame-stats").also { it.start() }
        thread = t
        frames.set(0)
        activity.window.addOnFrameMetricsAvailableListener(listener, Handler(t.looper))
    }

    fun stop() {
        thread ?: return
        runCatching { activity.window.removeOnFrameMetricsAvailableListener(listener) }
        thread?.quitSafely()
        thread = null
    }

    /** Active screen refresh rate in Hz as Android reports it right now. */
    fun hz(): Float =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) activity.display?.refreshRate ?: 0f
        else @Suppress("DEPRECATION") activity.windowManager.defaultDisplay.refreshRate

    /** Frames drawn by the game window since the previous call (call once per second). */
    fun takeWindowFrames(): Int = frames.getAndSet(0)
}
