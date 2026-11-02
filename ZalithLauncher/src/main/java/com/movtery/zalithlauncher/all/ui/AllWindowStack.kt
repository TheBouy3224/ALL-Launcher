package com.movtery.zalithlauncher.all.ui

import androidx.compose.runtime.mutableStateListOf

/**
 * Most-recently-focused-last list of window ids. The Desktop keeps one of these next to its per-window open/minimized flags:
 * focusing a window moves it to the end, so the stacking order of every window (not just the front one) follows use,
 * and after the front window is minimized or closed the previous one is the next on top.
 */
class AllWindowStack(initial: String) {
    val order = mutableStateListOf(initial)

    var front: String
        get() = order.lastOrNull() ?: ""
        set(id) {
            if (order.lastOrNull() == id) return
            order.remove(id)
            order.add(id)
        }

    /** Position of [id] in the stack (higher = closer to the front); ids never focused sort behind everything. */
    fun rank(id: String): Int = order.indexOf(id)
}
