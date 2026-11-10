package com.movtery.zalithlauncher.all.music

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

private const val TAG = "AllMusicPlayer"

data class MusicUiState(
    val tracks: List<MusicTrack> = emptyList(),
    val currentIndex: Int = -1,
    val playing: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: MusicRepeat = MusicRepeat.OFF,
    val volume: Float = 0.6f,
    val failedTitle: String? = null
)

/**
 * Music from the phone's files, played in the game process and controlled from the in-game quick menu.
 *
 * - MediaPlayer decodes in the system media service, not on a game thread, and nothing here runs per frame.
 * - Audio focus is deliberately never requested, so Android does not duck or stop the game's own sound.
 * - The volume applies to this player only; the game's sound and the phone's media volume are not changed.
 *
 * Call from the main thread (Compose callbacks and MediaPlayer listeners on a Looper thread).
 */
object AllMusicPlayer {
    private val _state = MutableStateFlow(MusicUiState())
    val state: StateFlow<MusicUiState> = _state.asStateFlow()

    private var appContext: Context? = null
    private var queue = MusicQueue()
    private var player: MediaPlayer? = null
    private var prepared = false
    private var wantPlaying = false
    private var volume = 0.6f
    private var failuresInARow = 0

    private fun stateFile(context: Context) = File(context.filesDir, "all_music/playlist.json")

    fun init(context: Context) {
        if (appContext != null) return
        val app = context.applicationContext
        appContext = app
        val saved = decodeMusicState(runCatching { stateFile(app).takeIf { it.isFile }?.readText() }.getOrNull())
        queue = MusicQueue(saved)
        volume = saved.volume
        publish()
    }

    /** Tracks picked in the system file picker. The read grant is kept so the playlist still works next launch. */
    fun addUris(uris: List<Uri>) {
        val app = appContext ?: return
        val picked = uris.mapNotNull { uri ->
            runCatching { app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                .onFailure { Logger.warning(TAG, "No lasting permission for a picked file: ${it.message}") }
            MusicTrack(uri.toString(), displayName(app, uri))
        }
        val wasEmpty = queue.current == null
        if (queue.add(picked) > 0) {
            persist()
            if (wasEmpty) loadCurrent(false)
        }
        publish()
    }

    fun togglePlay() {
        if (queue.current == null) return
        if (wantPlaying) {
            wantPlaying = false
            if (prepared) runCatching { player?.pause() }
        } else {
            wantPlaying = true
            failuresInARow = 0
            if (player != null && prepared) runCatching { player?.start() } else if (player == null) loadCurrent(true)
        }
        publish()
    }

    fun next() { if (queue.next()) { failuresInARow = 0; loadCurrent(wantPlaying); persist() }; publish() }
    fun previous() { if (queue.previous()) { failuresInARow = 0; loadCurrent(wantPlaying); persist() }; publish() }

    fun playIndex(index: Int) {
        queue.select(index)
        failuresInARow = 0
        loadCurrent(true)
        persist()
        publish()
    }

    fun remove(index: Int) {
        val wasCurrent = queue.remove(index)
        if (wasCurrent) {
            if (queue.current == null) { wantPlaying = false; releasePlayer() } else loadCurrent(wantPlaying)
        }
        persist()
        publish()
    }

    fun toggleShuffle() { queue.toggleShuffle(); persist(); publish() }
    fun cycleRepeat() { queue.cycleRepeat(); persist(); publish() }

    /** Live while dragging; call [saveSettings] when the drag ends. */
    fun setVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        runCatching { player?.setVolume(volume, volume) }
        publish()
    }

    fun saveSettings() = persist()

    /** Position for the open music page only. Nothing polls this while the menu is closed. */
    fun positionMs(): Pair<Int, Int> = if (!prepared) 0 to 0 else runCatching {
        val p = player ?: return 0 to 0
        p.currentPosition to p.duration.coerceAtLeast(0)
    }.getOrDefault(0 to 0)

    /** The game screen is closing: music must not outlive it. */
    fun release() {
        wantPlaying = false
        releasePlayer()
        persist()
        publish()
    }

    private fun loadCurrent(playWhenReady: Boolean) {
        val app = appContext ?: return
        val track = queue.current ?: return
        wantPlaying = playWhenReady
        releasePlayer()
        val mp = MediaPlayer()
        player = mp
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            )
            mp.setVolume(volume, volume)
            mp.setOnPreparedListener {
                if (player !== it) return@setOnPreparedListener
                prepared = true
                _state.value = _state.value.copy(failedTitle = null)
                if (wantPlaying) it.start()
                publish()
            }
            mp.setOnCompletionListener {
                if (player !== it) return@setOnCompletionListener
                failuresInARow = 0
                if (queue.advanceAfterCompletion()) loadCurrent(true)
                else {
                    //end of the list: back to the first track, stopped, like a press of Play starts over
                    wantPlaying = false
                    queue.select(queue.tracks.indices.firstOrNull() ?: -1)
                    releasePlayer()
                }
                persist()
                publish()
            }
            mp.setOnErrorListener { failed, what, extra ->
                if (player === failed) onTrackFailed(track, "error $what/$extra")
                true
            }
            mp.setDataSource(app, Uri.parse(track.uri))
            mp.prepareAsync()
        } catch (e: Exception) {
            onTrackFailed(track, e.message ?: e.javaClass.simpleName)
        }
    }

    /** A file that was moved, deleted or cannot be decoded is skipped; it never reaches the game or crashes it. */
    private fun onTrackFailed(track: MusicTrack, reason: String) {
        Logger.warning(TAG, "Cannot play a track: $reason")
        releasePlayer()
        failuresInARow++
        val keepGoing = wantPlaying && failuresInARow < queue.tracks.size
        _state.value = _state.value.copy(failedTitle = track.title)
        if (keepGoing && queue.next()) loadCurrent(true) else wantPlaying = false
        publish()
    }

    private fun releasePlayer() {
        val old = player
        player = null
        prepared = false
        runCatching { old?.release() }
    }

    private fun publish() {
        _state.value = MusicUiState(queue.tracks, queue.currentIndex, wantPlaying, queue.shuffle, queue.repeat, volume, _state.value.failedTitle)
    }

    private fun persist() {
        val app = appContext ?: return
        runCatching {
            val file = stateFile(app)
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(queue.toSaved(volume).encode())
            if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
        }.onFailure { Logger.warning(TAG, "Cannot save the playlist: ${it.message}") }
    }

    private fun displayName(context: Context, uri: Uri): String {
        val name = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull()
        return (name ?: uri.lastPathSegment ?: "?").substringBeforeLast('.')
    }
}
