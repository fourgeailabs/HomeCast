package com.example.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
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
    val sleepTimerMinutes: Int = 0,
    val errorMessage: String? = null
)

class PlaybackManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var currentPlaylist: List<MusicTrack> = emptyList()
    private var currentAudiobookList: List<Audiobook> = emptyList()

    private var isPrepared = false

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (isPrepared && player.isPlaying) {
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

    fun setPlaylist(tracks: List<MusicTrack>) {
        currentPlaylist = tracks
    }

    fun setAudiobookList(books: List<Audiobook>) {
        currentAudiobookList = books
    }

    fun playAudiobook(book: Audiobook, playlist: List<Audiobook>? = null) {
        if (playlist != null) currentAudiobookList = playlist
        _playbackState.value = _playbackState.value.copy(
            currentAudiobook = book,
            currentMusicTrack = null,
            currentPosition = book.progress,
            duration = if (book.duration > 0) book.duration * 1000L else 0L,
            isPlaying = true,
            errorMessage = null
        )

        if (book.streamUrl.isNotBlank()) {
            startStream(book.streamUrl, book.progress, isAudiobook = true, fallbackCandidate = book)
        } else {
            handler.removeCallbacks(updateProgressRunnable)
            handler.post(updateProgressRunnable)
        }
    }

    fun playMusicTrack(track: MusicTrack, playlist: List<MusicTrack>? = null) {
        if (playlist != null) currentPlaylist = playlist
        _playbackState.value = _playbackState.value.copy(
            currentMusicTrack = track,
            currentAudiobook = null,
            currentPosition = 0L,
            duration = track.duration,
            isPlaying = true,
            errorMessage = null
        )

        if (track.streamUrl.isNotBlank()) {
            startStream(track.streamUrl, 0L, isAudiobook = false)
        } else {
            handler.removeCallbacks(updateProgressRunnable)
            handler.post(updateProgressRunnable)
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
            // If already played > 3 seconds, restart current track
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

    private fun startStream(
        url: String,
        startPositionMs: Long,
        isAudiobook: Boolean = false,
        fallbackCandidate: Audiobook? = null,
        attempt: Int = 1
    ) {
        try {
            handler.removeCallbacks(updateProgressRunnable)
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(if (isAudiobook) AudioAttributes.CONTENT_TYPE_SPEECH else AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                // Build custom headers for authenticated self-hosted audio streaming
                val headers = mutableMapOf<String, String>()
                headers["User-Agent"] = "HomeCast-Android/2.2"

                // Extract token from url query if present and inject into Authorization header
                val uri = Uri.parse(url)
                val tokenParam = uri.getQueryParameter("token") ?: uri.getQueryParameter("apiKey") ?: uri.getQueryParameter("X-Plex-Token")
                if (!tokenParam.isNullOrBlank()) {
                    headers["Authorization"] = "Bearer $tokenParam"
                }

                try {
                    setDataSource(context, uri, headers)
                } catch (e: Exception) {
                    // Fallback to simple string dataSource
                    setDataSource(url)
                }

                prepareAsync()
                setOnPreparedListener { mp ->
                    isPrepared = true
                    if (startPositionMs > 0 && startPositionMs < mp.duration) {
                        mp.seekTo(startPositionMs.toInt())
                    }
                    mp.start()
                    setPlaybackSpeed(_playbackState.value.playbackSpeed)
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = true,
                        duration = mp.duration.toLong(),
                        errorMessage = null
                    )
                    handler.post(updateProgressRunnable)
                }
                setOnCompletionListener { mp ->
                    _playbackState.value = _playbackState.value.copy(isPlaying = false, currentPosition = if(isPrepared) mp.duration.toLong() else 0L)
                    skipNextTrack()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("PlaybackManager", "MediaPlayer error: what=$what extra=$extra on url $url")
                    isPrepared = false
                    
                    // If first attempt failed on audiobook, try fallback download endpoint
                    if (isAudiobook && fallbackCandidate != null && attempt == 1) {
                        val originalUrl = fallbackCandidate.streamUrl
                        val fallbackUrl = when {
                            originalUrl.contains("/file/") -> originalUrl.replace(Regex("/file/[^?]+"), "/download")
                            originalUrl.contains("/play") -> originalUrl.replace("/play", "/download")
                            else -> originalUrl.replace("/download", "/play")
                        }
                        if (fallbackUrl != originalUrl) {
                            Log.i("PlaybackManager", "Retrying with fallback stream url: $fallbackUrl")
                            startStream(fallbackUrl, startPositionMs, isAudiobook, fallbackCandidate, attempt = 2)
                            return@setOnErrorListener true
                        }
                    }

                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        errorMessage = "Audio playback error ($what, $extra)"
                    )
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Failed to start stream: ${e.message}", e)
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                errorMessage = e.localizedMessage
            )
        }
    }

    fun togglePlayPause() {
        val current = _playbackState.value
        mediaPlayer?.let { player ->
            try {
                if (isPrepared) {
                    if (player.isPlaying) {
                        player.pause()
                        _playbackState.value = current.copy(isPlaying = false)
                    } else {
                        player.start()
                        _playbackState.value = current.copy(isPlaying = true)
                        handler.post(updateProgressRunnable)
                    }
                } else {
                     _playbackState.value = current.copy(isPlaying = !current.isPlaying)
                }
            } catch (e: Exception) {
                Log.e("PlaybackManager", "Error toggling play/pause", e)
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
