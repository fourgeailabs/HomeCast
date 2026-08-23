package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
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
    val errorMessage: String? = null
)

class PlaybackManager(private val context: Context) {

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    var onProgressUpdate: ((audiobook: Audiobook?, musicTrack: MusicTrack?, positionMs: Long, durationMs: Long) -> Unit)? = null

    private var currentPlaylist: List<MusicTrack> = emptyList()
    private var currentAudiobookList: List<Audiobook> = emptyList()

    private var isPrepared = false
    private val handler = Handler(Looper.getMainLooper())

    private var lastSavedPos = 0L

    private fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                action()
            } catch (e: Exception) {
                Log.e("PlaybackManager", "Error in main thread action", e)
            }
        } else {
            handler.post {
                try {
                    action()
                } catch (e: Exception) {
                    Log.e("PlaybackManager", "Error in posted main thread action", e)
                }
            }
        }
    }

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            try {
                player?.let { p ->
                    val state = p.playbackState
                    if (state == Player.STATE_READY || state == Player.STATE_BUFFERING || p.isPlaying) {
                        val pos = p.currentPosition.coerceAtLeast(0L)
                        val dur = p.duration.coerceAtLeast(0L)
                        _playbackState.value = _playbackState.value.copy(
                            currentPosition = pos,
                            duration = if (dur > 0L) dur else _playbackState.value.duration,
                            isPlaying = p.isPlaying
                        )
                        
                        // Throttle saving progress to every 2 seconds or significant seek
                        if (Math.abs(pos - lastSavedPos) > 2000L || !p.isPlaying) {
                            lastSavedPos = pos
                            onProgressUpdate?.invoke(
                                _playbackState.value.currentAudiobook,
                                _playbackState.value.currentMusicTrack,
                                pos,
                                dur
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("PlaybackManager", "Error in progress update loop", e)
            }
            handler.postDelayed(this, 500)
        }
    }

    init {
        try {
            val intent = Intent(context, com.example.PlaybackService::class.java)
            context.startService(intent)
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Failed to start PlaybackService", e)
        }
        handler.post(updateProgressRunnable)
        
        // Polling to wait for player
        handler.post(object : Runnable {
            override fun run() {
                if (player != null) {
                    setupPlayer()
                } else {
                    handler.postDelayed(this, 100)
                }
            }
        })
    }
    
    private fun setupPlayer() {
        try {
            player?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                }

                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        isPrepared = true
                        val dur = player?.duration?.coerceAtLeast(0L) ?: 0L
                        _playbackState.value = _playbackState.value.copy(
                            duration = if (dur > 0L) dur else _playbackState.value.duration,
                            errorMessage = null
                        )
                    } else if (state == Player.STATE_ENDED) {
                        _playbackState.value = _playbackState.value.copy(isPlaying = false, currentPosition = player?.duration ?: 0L)
                        skipNextTrack()
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e("PlaybackManager", "ExoPlayer error", error)
                    isPrepared = false
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        errorMessage = "Stream error: ${error.localizedMessage ?: "Source unavailable"}"
                    )
                }
            })
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Failed to setup player listener", e)
        }
    }

    fun playAudiobook(book: Audiobook, list: List<Audiobook> = emptyList()) {
        runOnMainThread {
            currentAudiobookList = if (list.isNotEmpty()) list else listOf(book)
            _playbackState.value = _playbackState.value.copy(
                currentAudiobook = book,
                currentMusicTrack = null,
                currentPosition = 0L,
                duration = if (book.duration > 0) book.duration * 1000L else 0L,
                errorMessage = null
            )
            
            val metaBuilder = MediaMetadata.Builder()
                .setTitle(book.title.ifBlank { "Untitled Audiobook" })
                .setArtist(book.author.ifBlank { "Unknown Author" })

            if (book.coverUrl.isNotBlank()) {
                try {
                    metaBuilder.setArtworkUri(Uri.parse(book.coverUrl))
                } catch (_: Exception) {}
            }
                
            startStream(book.streamUrl, metaBuilder.build(), book.progress, true, book)
        }
    }

    fun playMusicTrack(track: MusicTrack, playlist: List<MusicTrack> = emptyList()) {
        runOnMainThread {
            currentPlaylist = if (playlist.isNotEmpty()) playlist else listOf(track)
            _playbackState.value = _playbackState.value.copy(
                currentMusicTrack = track,
                currentAudiobook = null,
                currentPosition = 0L,
                duration = track.duration,
                errorMessage = null
            )
            
            val metaBuilder = MediaMetadata.Builder()
                .setTitle(track.title.ifBlank { "Untitled Track" })
                .setArtist(track.artist.ifBlank { "Unknown Artist" })
                .setAlbumTitle(track.album.ifBlank { "Unknown Album" })

            if (track.coverUrl.isNotBlank()) {
                try {
                    metaBuilder.setArtworkUri(Uri.parse(track.coverUrl))
                } catch (_: Exception) {}
            }
                
            startStream(track.streamUrl, metaBuilder.build(), 0L, false)
        }
    }

    private fun startStream(
        url: String,
        metadata: MediaMetadata,
        startPositionMs: Long,
        isAudiobook: Boolean = false,
        fallbackCandidate: Audiobook? = null
    ) {
        try {
            val finalUrl = if (url.isBlank()) {
                "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3"
            } else url

            val mediaItem = MediaItem.Builder()
                .setUri(finalUrl)
                .setMediaId(finalUrl)
                .setMediaMetadata(metadata)
                .build()

            // Safely stop previous playback before loading next
            try {
                player?.stop()
                player?.clearMediaItems()
            } catch (e: Exception) {
                Log.w("PlaybackManager", "Error resetting previous player state", e)
            }

            player?.setMediaItem(mediaItem)
            player?.prepare()
            if (startPositionMs > 0) {
                try {
                    player?.seekTo(startPositionMs)
                } catch (e: Exception) {
                    Log.w("PlaybackManager", "Initial seek to $startPositionMs failed", e)
                }
            }
            player?.play()
            setPlaybackSpeed(_playbackState.value.playbackSpeed)
            isPrepared = false // Will be set true in listener
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Failed to start stream", e)
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                errorMessage = "Failed to stream: ${e.message}"
            )
        }
    }

    fun skipNextTrack() {
        runOnMainThread {
            val currentTrack = _playbackState.value.currentMusicTrack
            val currentBook = _playbackState.value.currentAudiobook

            if (currentTrack != null && currentPlaylist.isNotEmpty()) {
                val idx = currentPlaylist.indexOfFirst { it.id == currentTrack.id }
                if (idx != -1 && idx < currentPlaylist.size - 1) {
                    playMusicTrack(currentPlaylist[idx + 1], currentPlaylist)
                } else if (currentPlaylist.isNotEmpty()) {
                    playMusicTrack(currentPlaylist.first(), currentPlaylist)
                }
            } else if (currentBook != null && currentAudiobookList.isNotEmpty()) {
                val idx = currentAudiobookList.indexOfFirst { it.id == currentBook.id }
                if (idx != -1 && idx < currentAudiobookList.size - 1) {
                    playAudiobook(currentAudiobookList[idx + 1], currentAudiobookList)
                } else {
                    skipForward(30)
                }
            } else {
                skipForward(30)
            }
        }
    }

    fun skipPreviousTrack() {
        runOnMainThread {
            val currentTrack = _playbackState.value.currentMusicTrack
            val currentBook = _playbackState.value.currentAudiobook

            if (currentTrack != null && currentPlaylist.isNotEmpty()) {
                if (_playbackState.value.currentPosition > 3000L) {
                    seekTo(0L)
                } else {
                    val idx = currentPlaylist.indexOfFirst { it.id == currentTrack.id }
                    if (idx > 0) {
                        playMusicTrack(currentPlaylist[idx - 1], currentPlaylist)
                    } else {
                        seekTo(0L)
                    }
                }
            } else if (currentBook != null && currentAudiobookList.isNotEmpty()) {
                if (_playbackState.value.currentPosition > 5000L) {
                    seekTo(0L)
                } else {
                    val idx = currentAudiobookList.indexOfFirst { it.id == currentBook.id }
                    if (idx > 0) {
                        playAudiobook(currentAudiobookList[idx - 1], currentAudiobookList)
                    } else {
                        seekTo(0L)
                    }
                }
            } else {
                skipBackward(10)
            }
        }
    }

    fun togglePlayPause() {
        runOnMainThread {
            player?.let { p ->
                val state = p.playbackState
                if (state == Player.STATE_IDLE || p.mediaItemCount == 0) {
                    val book = _playbackState.value.currentAudiobook
                    val track = _playbackState.value.currentMusicTrack
                    if (book != null) {
                        playAudiobook(book, currentAudiobookList)
                    } else if (track != null) {
                        playMusicTrack(track, currentPlaylist)
                    }
                } else {
                    if (p.isPlaying) p.pause() else p.play()
                }
            }
        }
    }

    fun stop() {
        runOnMainThread {
            try {
                player?.stop()
                player?.clearMediaItems()
            } catch (e: Exception) {
                Log.w("PlaybackManager", "Error stopping player", e)
            }
            _playbackState.value = _playbackState.value.copy(
                currentAudiobook = null,
                currentMusicTrack = null,
                isPlaying = false,
                currentPosition = 0L,
                duration = 0L
            )
        }
    }

    fun setInitialAudiobook(book: Audiobook) {
        runOnMainThread {
            _playbackState.value = _playbackState.value.copy(
                currentAudiobook = book,
                currentMusicTrack = null,
                isPlaying = false,
                currentPosition = 0L,
                duration = book.duration * 1000L
            )
        }
    }

    fun setInitialMusicTrack(track: MusicTrack) {
        runOnMainThread {
            _playbackState.value = _playbackState.value.copy(
                currentMusicTrack = track,
                currentAudiobook = null,
                isPlaying = false,
                currentPosition = 0L,
                duration = track.duration
            )
        }
    }

    fun seekTo(positionMs: Long) {
        runOnMainThread {
            try {
                player?.seekTo(positionMs)
                _playbackState.value = _playbackState.value.copy(currentPosition = positionMs)
            } catch (e: Exception) {
                Log.w("PlaybackManager", "Seek to $positionMs failed", e)
            }
        }
    }

    fun skipForward(seconds: Int = 30) {
        runOnMainThread {
            player?.let { p ->
                val d = if (p.duration > 0) p.duration else _playbackState.value.duration
                val newPos = (p.currentPosition + (seconds * 1000L)).coerceAtMost(d)
                seekTo(newPos)
            }
        }
    }

    fun skipBackward(seconds: Int = 10) {
        runOnMainThread {
            player?.let { p ->
                val newPos = (p.currentPosition - (seconds * 1000L)).coerceAtLeast(0L)
                seekTo(newPos)
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        runOnMainThread {
            _playbackState.value = _playbackState.value.copy(playbackSpeed = speed)
            try {
                player?.setPlaybackSpeed(speed)
            } catch (e: Exception) {
                Log.w("PlaybackManager", "Failed to set playback speed", e)
            }
        }
    }

    fun release() {
        handler.removeCallbacks(updateProgressRunnable)
        runOnMainThread {
            try {
                player?.stop()
            } catch (e: Exception) {
                Log.w("PlaybackManager", "Error releasing player", e)
            }
        }
    }
    
    companion object {
        var player: ExoPlayer? = null
        var mediaSession: MediaSession? = null
        fun setPlayer(p: ExoPlayer, ms: MediaSession) {
            player = p
            mediaSession = ms
        }
    }
}
