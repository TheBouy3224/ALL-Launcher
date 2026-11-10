package com.movtery.zalithlauncher.all.music

import com.google.gson.Gson
import kotlin.random.Random

data class MusicTrack(val uri: String = "", val title: String = "")

enum class MusicRepeat { OFF, ALL, ONE }

/** What is saved between game launches. Playback never starts by itself from this. */
data class MusicSavedState(
    val tracks: List<MusicTrack> = emptyList(),
    val index: Int = -1,
    val shuffle: Boolean = false,
    val repeat: MusicRepeat = MusicRepeat.OFF,
    val volume: Float = 0.6f
)

fun MusicSavedState.encode(): String = Gson().toJson(this)

/** Unreadable or partial data gives a safe state; a damaged file must never stop the game menu from opening. */
fun decodeMusicState(text: String?): MusicSavedState {
    if (text.isNullOrBlank()) return MusicSavedState()
    val raw = runCatching { Gson().fromJson(text, RawState::class.java) }.getOrNull() ?: return MusicSavedState()
    val tracks = raw.tracks.orEmpty().mapNotNull { track ->
        track?.uri?.takeIf { it.isNotBlank() }?.let { MusicTrack(it, track.title.orEmpty()) }
    }
    return MusicSavedState(
        tracks = tracks,
        index = (raw.index ?: -1).takeIf { it in tracks.indices } ?: if (tracks.isEmpty()) -1 else 0,
        shuffle = raw.shuffle == true,
        repeat = raw.repeat ?: MusicRepeat.OFF,
        volume = (raw.volume ?: 0.6f).takeIf { !it.isNaN() }?.coerceIn(0f, 1f) ?: 0.6f
    )
}

//nullable on purpose: Gson leaves absent fields null even when the Kotlin type says otherwise
private class RawState(
    val tracks: List<RawTrack?>? = null,
    val index: Int? = null,
    val shuffle: Boolean? = null,
    val repeat: MusicRepeat? = null,
    val volume: Float? = null
)
private class RawTrack(val uri: String? = null, val title: String? = null)

/** Playlist order and position rules. No Android types, so every rule is unit tested. */
class MusicQueue(saved: MusicSavedState = MusicSavedState(), private val random: Random = Random.Default) {
    private val list = saved.tracks.toMutableList()
    private var order: List<Int> = list.indices.toList()
    private var position = 0

    val tracks: List<MusicTrack> get() = list.toList()
    var repeat: MusicRepeat = saved.repeat
    var shuffle: Boolean = saved.shuffle
        private set

    /** Index into [tracks], or -1 when empty. */
    val currentIndex: Int get() = order.getOrElse(position) { -1 }
    val current: MusicTrack? get() = list.getOrNull(currentIndex)

    init {
        rebuildOrder(saved.index.takeIf { it in list.indices } ?: if (list.isEmpty()) -1 else 0)
    }

    /** Adds tracks not already present. Returns how many were added. */
    fun add(newTracks: List<MusicTrack>): Int {
        val keep = currentIndex
        val known = list.mapTo(HashSet()) { it.uri }
        val fresh = newTracks.filter { known.add(it.uri) }
        list += fresh
        rebuildOrder(if (keep >= 0) keep else if (list.isEmpty()) -1 else 0)
        return fresh.size
    }

    /** Removes one track. Returns true when it was the current one. */
    fun remove(index: Int): Boolean {
        if (index !in list.indices) return false
        val keep = currentIndex
        val wasCurrent = index == keep
        list.removeAt(index)
        val next = when {
            list.isEmpty() -> -1
            wasCurrent -> keep.coerceAtMost(list.lastIndex)
            index < keep -> keep - 1
            else -> keep
        }
        rebuildOrder(next)
        return wasCurrent
    }

    fun select(index: Int) {
        if (index in list.indices) rebuildOrder(index)
    }

    fun toggleShuffle() {
        shuffle = !shuffle
        rebuildOrder(currentIndex)
    }

    fun cycleRepeat() {
        repeat = MusicRepeat.entries[(repeat.ordinal + 1) % MusicRepeat.entries.size]
    }

    /** Next button: always moves, wrapping at the end, even with repeat ONE. Returns false when empty. */
    fun next(): Boolean {
        if (list.isEmpty()) return false
        position = (position + 1) % order.size
        return true
    }

    /** Previous button: always moves, wrapping at the start. Returns false when empty. */
    fun previous(): Boolean {
        if (list.isEmpty()) return false
        position = (position - 1 + order.size) % order.size
        return true
    }

    /** A track finished by itself. Returns false when playback should stop. */
    fun advanceAfterCompletion(): Boolean {
        if (list.isEmpty()) return false
        if (repeat == MusicRepeat.ONE) return true
        if (position == order.lastIndex && repeat == MusicRepeat.OFF) return false
        position = (position + 1) % order.size
        return true
    }

    fun toSaved(volume: Float) = MusicSavedState(list.toList(), currentIndex, shuffle, repeat, volume)

    /** The given track becomes the current one; with shuffle it goes first and the others are mixed behind it. */
    private fun rebuildOrder(currentTrack: Int) {
        order = if (shuffle && currentTrack >= 0) {
            listOf(currentTrack) + list.indices.filter { it != currentTrack }.shuffled(random)
        } else list.indices.toList()
        position = order.indexOf(currentTrack).coerceAtLeast(0)
    }
}
