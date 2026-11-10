package com.movtery.zalithlauncher.all.fg;

/**
 * Runtime channel to the Frame Generation module in libpojavexec (same process as the game window).
 * The native side keeps the user mode in memory only; the persistent choice is AllSettings.frameGeneration.
 * Every call is safe when the library or the symbol is missing (older native build): it just reports "unavailable".
 */
public final class AllFgBridge {
    /** native status: 0 unavailable/failed, 1 off (user), 2 on but the adaptive gate is idle, 3 generating, 4 on, waiting for the first frame */
    public static final int UNAVAILABLE = 0, OFF = 1, ON_IDLE = 2, GENERATING = 3, STARTING = 4;

    private AllFgBridge() {}

    private static native void nativeSetMode(int on);
    private static native int nativeStatus();
    private static native void nativeSetHz(float hz);

    public static void setMode(boolean on) {
        try { nativeSetMode(on ? 1 : 0); } catch (Throwable ignored) { }
    }

    /** the display rate that is active now (Unlock FPS can change it while playing); the adaptive gate compares real fps with it */
    public static void setDisplayHz(float hz) {
        try { nativeSetHz(hz); } catch (Throwable ignored) { }
    }

    public static int status() {
        try { return nativeStatus(); } catch (Throwable t) { return UNAVAILABLE; }
    }
}
