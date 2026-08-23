package com.example.data

import android.content.Context
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
import android.content.Intent

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

    private var currentPlaylist: List<MusicTrack> = emptyList()
    private var currentAudiobookList: List<Audiobook> = emptyList()

    private var isPrepared = false
    private val handler = Handler(Looper.getMainLooper())

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            player?.let { p ->
                val state = p.playbackState
                if (state == Player.STATE_READY || state == Player.STATE_BUFFERING || p.isPlaying) {
                    val pos = p.currentPosition
                    val dur = p.duration.coerceAtLeast(0L)
                    _playbackState.value = _playbackState.value.copy(
                        currentPosition = pos,
                        duration = if (dur > 0L) dur else _playbackState.value.duration,
                        isPlaying = p.isPlaying
                    )
                }
            }
            handler.postDelayed(this, 500)
        }
    }

    init {
        val intent = Intent(context, com.example.PlaybackService::class.java)
        context.startService(intent)
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
                    errorMessage = "Playback error: ${error.message}"
                )
            }
        })
    }

    fun playAudiobook(book: Audiobook, list: List<Audiobook> = emptyList()) {
        currentAudiobookList = if (list.isNotEmpty()) list else listOf(book)
        _playbackState.value = _playbackState.value.copy(
            currentAudiobook = book,
            currentMusicTrack = null,
            currentPosition = 0L,
            duration = book.duration * 1000L,
            errorMessage = null
        )
        
        val meta = MediaMetadata.Builder()
            .setTitle(book.title)
            .setArtist(book.author)
            .setArtworkUri(Uri.parse(book.coverUrl))
            .build()
            
        startStream(book.streamUrl, meta, book.progress, true, book)
    }

    fun playMusicTrack(track: MusicTrack, playlist: List<MusicTrack> = emptyList()) {
        currentPlaylist = if (playlist.isNotEmpty()) playlist else listOf(track)
        _playbackState.value = _playbackState.value.copy(
            currentMusicTrack = track,
            currentAudiobook = null,
            currentPosition = 0L,
            duration = track.duration,
            errorMessage = null
        )
        
        val meta = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(Uri.parse(track.coverUrl))
            .build()
            
        startStream(track.streamUrl, meta, 0L, false)
    }

    private fun startStream(
        url: String,
        metadata: MediaMetadata,
        startPositionMs: Long,
        isAudiobook: Boolean = false,
        fallbackCandidate: Audiobook? = null
    ) {
        try {
            val uri = Uri.parse(url)
            val finalUrl = if (url.isBlank()) {
                "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3"
            } else url

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(finalUrl)
                .setMediaId(finalUrl)
                .setMediaMetadata(metadata)

            val tokenParam = uri.getQueryParameter("token") ?: uri.getQueryParameter("apiKey") ?: uri.getQueryParameter("X-Plex-Token")
            
            // Note: Since we are using MediaItem directly, ExoPlayer will fetch using its DefaultDataSource.
            // For custom headers, we usually need a DefaultHttpDataSource.Factory, but this works for basic URLs.
            // If the token is in the query, it should work fine without headers.

            val mediaItem = mediaItemBuilder.build()
            player?.setMediaItem(mediaItem)
            player?.prepare()
            if (startPositionMs > 0) {
                player?.seekTo(startPositionMs)
            }
            player?.play()
            setPlaybackSpeed(_playbackState.value.playbackSpeed)
            isPrepared = false // Will be set true in listener
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Failed to start stream", e)
        }
    }

    fun skipNextTrack() {
        val currentTrack = _playbackState.value.currentMusicTrack
        val currentBook = _playbackState.value.currentAudiobook

        if (currentTrack != null && currentPlaylist.isNotEmpty()) {
            val idx = currentPlaylist.indexOfFirst { it.id == currentTrack.id }
            if (idx != -1 && idx < currentPlaylist.size - 1) {
                playMusicTrack(currentPlaylist[idx + 1])
            } else if (currentPlaylist.isNotEmpty()) {
                playMusicTrack(currentPlaylist.first())
            }
        } else if (currentBook != null && currentAudiobookList.isNotEmpty()) {
            val idx = currentAudiobookList.indexOfFirst { it.id == currentBook.id }
            if (idx != -1 && idx < currentAudiobookList.size - 1) {
                playAudiobook(currentAudiobookList[idx + 1])
            } else {
                skipForward(30)
            }
        } else {
            skipForward(30)
        }
    }

    fun skipPreviousTrack() {
        val currentTrack = _playbackState.value.currentMusicTrack
        val currentBook = _playbackState.value.currentAudiobook

        if (currentTrack != null && currentPlaylist.isNotEmpty()) {
            if (_playbackState.value.currentPosition > 3000L) {
                seekTo(0L)
            } else {
                val idx = currentPlaylist.indexOfFirst { it.id == currentTrack.id }
                if (idx > 0) {
                    playMusicTrack(currentPlaylist[idx - 1])
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
                    playAudiobook(currentAudiobookList[idx - 1])
                } else {
                    seekTo(0L)
                }
            }
        } else {
            skipBackward(10)
        }
    }

    fun togglePlayPause() {
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

    fun stop() {
        player?.stop()
        player?.clearMediaItems()
        _playbackState.value = _playbackState.value.copy(
            currentAudiobook = null,
            currentMusicTrack = null,
            isPlaying = false,
            currentPosition = 0L,
            duration = 0L
        )
    }

    fun setInitialAudiobook(book: Audiobook) {
        _playbackState.value = _playbackState.value.copy(
            currentAudiobook = book,
            currentMusicTrack = null,
            isPlaying = false,
            currentPosition = 0L,
            duration = book.duration * 1000L
        )
    }

    fun setInitialMusicTrack(track: MusicTrack) {
        _playbackState.value = _playbackState.value.copy(
            currentMusicTrack = track,
            currentAudiobook = null,
            isPlaying = false,
            currentPosition = 0L,
            duration = track.duration
        )
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(currentPosition = positionMs)
    }

    fun skipForward(seconds: Int = 30) {
        player?.let { p ->
            val d = if (p.duration > 0) p.duration else _playbackState.value.duration
            val newPos = (p.currentPosition + (seconds * 1000L)).coerceAtMost(d)
            seekTo(newPos)
        }
    }

    fun skipBackward(seconds: Int = 10) {
        player?.let { p ->
            val newPos = (p.currentPosition - (seconds * 1000L)).coerceAtLeast(0L)
            seekTo(newPos)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackState.value = _playbackState.value.copy(playbackSpeed = speed)
        player?.setPlaybackSpeed(speed)
    }

    fun release() {
        handler.removeCallbacks(updateProgressRunnable)
        player?.stop()
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
