package com.movtery.zalithlauncher.utils.microphone

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.compose.runtime.mutableStateOf

/**
 * "Mic to live stream": plays the microphone back as app audio (USAGE_GAME), so a screen/live recorder that captures
 * app audio (TikTok game live) hears the player while the game holds the mic for Simple Voice Chat. Headphones needed.
 * ponytail: plain PCM pass-through, no echo cancel; add AcousticEchoCanceler if speaker use matters.
 */
object MicLoopback {
    val enabled = mutableStateOf(false)
    /** false = the voice plays on the phone speaker, not the headphones, so the player does not hear themself (recorders still get it) */
    val monitor = mutableStateOf(false)
    @Volatile private var track: AudioTrack? = null
    @Volatile private var speaker: AudioDeviceInfo? = null

    fun setMonitor(on: Boolean) { monitor.value = on; track?.preferredDevice = if (on) null else speaker }
    @Volatile private var thread: Thread? = null

    @SuppressLint("MissingPermission") //only offered after the mic permission was granted for voice chat
    fun set(context: Context, on: Boolean) {
        enabled.value = on
        if (!on) { thread?.interrupt(); thread = null; return }
        if (thread != null) return
        thread = Thread {
            val rate = 48000
            val inSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val rec = runCatching { AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, inSize * 2) }.getOrNull()
            if (rec == null || rec.state != AudioRecord.STATE_INITIALIZED) { rec?.release(); enabled.value = false; thread = null; return@Thread }
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(inSize * 2).setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY).build()
            speaker = context.getSystemService(AudioManager::class.java).getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            this.track = track
            track.preferredDevice = if (monitor.value) null else speaker
            val buf = ShortArray(inSize / 2)
            try {
                rec.startRecording(); track.play()
                while (!Thread.currentThread().isInterrupted) {
                    val n = rec.read(buf, 0, buf.size)
                    if (n > 0) track.write(buf, 0, n)
                }
            } finally {
                runCatching { rec.stop() }; rec.release()
                this.track = null
                runCatching { track.stop() }; track.release()
            }
        }.apply { name = "MicLoopback"; isDaemon = true; start() }
    }
}
