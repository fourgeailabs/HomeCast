import re

file_path = "app/src/main/java/com/example/data/PlaybackManager.kt"
with open(file_path, "r") as f:
    text = f.read()

replacement = """package com.example.data

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PlaybackState(
    val currentAudiobook: Audiobook? = null,
    val currentMusicTrack: MusicTrack? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

class PlaybackManager(private val context: Context) {
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var player: ExoPlayer? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    init {
        initializePlayer()
    }

    private fun initializePlayer() {
        if (player == null) {
            player = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _playbackState.update { it.copy(isPlaying = isPlaying) }
                        if (isPlaying) {
                            startProgressTracking()
                        } else {
                            progressJob?.cancel()
                        }
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            _playbackState.update { it.copy(durationMs = duration.coerceAtLeast(0)) }
                        } else if (state == Player.STATE_ENDED) {
                            _playbackState.update { it.copy(isPlaying = false, positionMs = 0) }
                        }
                    }
                })
            }
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch {
            while (isActive) {
                player?.let { p ->
                    _playbackState.update { it.copy(positionMs = p.currentPosition.coerceAtLeast(0)) }
                }
                delay(1000)
            }
        }
    }

    fun playAudiobook(book: Audiobook) {
        _playbackState.update { 
            it.copy(currentAudiobook = book, currentMusicTrack = null, positionMs = 0, durationMs = book.duration * 1000L) 
        }
        playUrl(book.streamUrl)
    }

    fun playMusicTrack(track: MusicTrack) {
        _playbackState.update { 
            it.copy(currentMusicTrack = track, currentAudiobook = null, positionMs = 0, durationMs = track.duration) 
        }
        playUrl(track.streamUrl)
    }

    private fun playUrl(url: String) {
        try {
            initializePlayer()
            val finalUrl = if (url.isBlank()) {
                // If it's a simulated pd track without a real url
                "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3"
            } else url

            val mediaItem = MediaItem.fromUri(finalUrl)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Error playing media", e)
        }
    }

    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
        _playbackState.update { it.copy(positionMs = positionMs) }
    }
    
    fun skipForward() {
        player?.let {
            val newPos = (it.currentPosition + 30000).coerceAtMost(it.duration)
            it.seekTo(newPos)
        }
    }
    
    fun skipBackward() {
        player?.let {
            val newPos = (it.currentPosition - 15000).coerceAtLeast(0)
            it.seekTo(newPos)
        }
    }

    fun release() {
        progressJob?.cancel()
        player?.release()
        player = null
        coroutineScope.cancel()
    }
}
"""

text = replacement

with open(file_path, "w") as f:
    f.write(text)
