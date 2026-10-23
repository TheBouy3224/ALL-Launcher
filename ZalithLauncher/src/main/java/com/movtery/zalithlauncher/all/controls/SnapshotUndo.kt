package com.movtery.zalithlauncher.all.controls

/**
 * Undo V1 for the touch-control editor: whole-layout snapshots, grouped into one step per finished edit.
 *
 * The editor calls [observe] on a timer with two consecutive samples of the layout. An edit counts as finished when both samples are
 * equal (nothing moved for one interval) and differ from the last stable state; the last stable state is then pushed on the stack.
 * A drag, a resize or a slider is therefore one step, and add/delete/binding changes are one step each. [undo] returns the state to
 * restore: the last stable state when an unfinished edit is pending, otherwise the previous step.
 * Pure logic (no Android, no Compose) so it is unit tested; the editor applies the returned snapshot to the observable layout.
 */
class SnapshotUndo<T : Any>(private val limit: Int = 30) {
    private val stack = ArrayDeque<T>()
    private var stable: T? = null

    /** Number of steps that can still be undone (a pending unfinished edit is not counted) */
    val size: Int get() = stack.size

    fun observe(previous: T, current: T) {
        val s = stable
        if (s == null) { stable = current; return }
        if (previous == current && current != s) {
            stack.addLast(s)
            if (stack.size > limit) stack.removeFirst()
            stable = current
        }
    }

    /** @return the state to restore, or null when there is nothing to undo */
    fun undo(current: T): T? {
        val s = stable ?: return null
        if (current != s) return s
        val prev = stack.removeLastOrNull() ?: return null
        stable = prev
        return prev
    }

    fun canUndo(current: T): Boolean = stable?.let { current != it || stack.isNotEmpty() } ?: false
}
