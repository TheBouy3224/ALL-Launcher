package com.movtery.zalithlauncher.all.accounts

/**
 * Easter egg that unlocks offline accounts: [needed] taps on the ALL logo, each at most [windowMs] after the previous one.
 * Pure logic so it can be unit tested; the window persists the result in AllSettings.allOfflineUnlocked.
 */
class OfflineUnlockTaps(private val needed: Int = 7, private val windowMs: Long = 3_000L) {
    private var count = 0
    private var last = Long.MIN_VALUE

    /** @return true on the tap that completes the sequence (the counter then starts over) */
    fun tap(nowMs: Long): Boolean {
        count = if (last != Long.MIN_VALUE && nowMs - last <= windowMs) count + 1 else 1
        last = nowMs
        if (count >= needed) { count = 0; return true }
        return false
    }
}

/** Offline accounts are shown to people who unlocked them, or who already own one (an upgrade must not hide their account tools) */
fun offlineOptionVisible(unlocked: Boolean, hasLocalAccount: Boolean): Boolean = unlocked || hasLocalAccount
