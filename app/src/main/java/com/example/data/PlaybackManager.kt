package com.example.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackState(
    val currentAudiobook: Audiobook? = null,
    val currentMusicTrack: MusicTrack? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerMinutes: Int = 0
)

class PlaybackManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    val pos = player.currentPosition.toLong()
                    val dur = if (player.duration > 0) player.duration.toLong() else _playbackState.value.duration
                    _playbackState.value = _playbackState.value.copy(
                        currentPosition = pos,
                        duration = dur,
                        isPlaying = true
                    )
                }
            }
            handler.postDelayed(this, 500)
        }
    }

    fun playAudiobook(book: Audiobook) {
        _playbackState.value = _playbackState.value.copy(
            currentAudiobook = book,
            currentMusicTrack = null,
            currentPosition = book.progress,
            duration = if (book.duration > 0) book.duration * 1000L else 0L,
            isPlaying = true
        )

        if (book.streamUrl.isNotBlank()) {
            startStream(book.streamUrl, book.progress)
        } else {
            // Simulated playback progress for offline/preview mode
            handler.removeCallbacks(updateProgressRunnable)
            handler.post(updateProgressRunnable)
        }
    }

    fun playMusicTrack(track: MusicTrack) {
        _playbackState.value = _playbackState.value.copy(
            currentMusicTrack = track,
            currentAudiobook = null,
            currentPosition = 0L,
            duration = track.duration,
            isPlaying = true
        )

        if (track.streamUrl.isNotBlank()) {
            startStream(track.streamUrl, 0L)
        } else {
            handler.removeCallbacks(updateProgressRunnable)
            handler.post(updateProgressRunnable)
        }
    }

    private fun startStream(url: String, startPositionMs: Long) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener { mp ->
                    if (startPositionMs > 0 && startPositionMs < mp.duration) {
                        mp.seekTo(startPositionMs.toInt())
                    }
                    mp.start()
                    setPlaybackSpeed(_playbackState.value.playbackSpeed)
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = true,
                        duration = mp.duration.toLong()
                    )
                    handler.post(updateProgressRunnable)
                }
                setOnCompletionListener { mp ->
                    _playbackState.value = _playbackState.value.copy(isPlaying = false, currentPosition = mp.duration.toLong())
                }
                setOnErrorListener { _, _, _ ->
                    _playbackState.value = _playbackState.value.copy(isPlaying = false)
                    true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePlayPause() {
        val current = _playbackState.value
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _playbackState.value = current.copy(isPlaying = false)
            } else {
                player.start()
                _playbackState.value = current.copy(isPlaying = true)
                handler.post(updateProgressRunnable)
            }
        } ?: run {
            _playbackState.value = current.copy(isPlaying = !current.isPlaying)
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { player ->
            player.seekTo(positionMs.toInt())
        }
        _playbackState.value = _playbackState.value.copy(currentPosition = positionMs)
    }

    fun skipForward(seconds: Int = 30) {
        val newPos = (_playbackState.value.currentPosition + (seconds * 1000L)).coerceAtMost(_playbackState.value.duration)
        seekTo(newPos)
    }

    fun skipBackward(seconds: Int = 10) {
        val newPos = (_playbackState.value.currentPosition - (seconds * 1000L)).coerceAtLeast(0L)
        seekTo(newPos)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackState.value = _playbackState.value.copy(playbackSpeed = speed)
        mediaPlayer?.let { player ->
            try {
                player.playbackParams = player.playbackParams.setSpeed(speed)
            } catch (e: Exception) {
                // Ignore if not supported on older API
            }
        }
    }

    fun release() {
        handler.removeCallbacks(updateProgressRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
