package com.example.ui

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.*
import com.example.data.*
import com.example.data.network.AudiobookshelfClient
import com.example.data.network.PlexClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class ServerOperationState {
    object Idle : ServerOperationState()
    object Loading : ServerOperationState()
    data class Success(val message: String) : ServerOperationState()
    data class Error(val message: String) : ServerOperationState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = LibraryRepository(database.libraryDao())
    val playbackManager = PlaybackManager(application)

    val allBooks = repository.allBooks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favorites = repository.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recents = repository.recents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allMusic = repository.allMusic.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentMusic = repository.recentMusic.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val servers = repository.servers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackState = playbackManager.playbackState

    private val _serverOpState = MutableStateFlow<ServerOperationState>(ServerOperationState.Idle)
    val serverOpState = _serverOpState.asStateFlow()

    // Discovery State
    private val _recommendations = MutableStateFlow<List<String>>(emptyList())
    val recommendations = _recommendations.asStateFlow()

    private val _isDiscoveryLoading = MutableStateFlow(false)
    val isDiscoveryLoading = _isDiscoveryLoading.asStateFlow()

    private val _discoveryError = MutableStateFlow<String?>(null)
    val discoveryError = _discoveryError.asStateFlow()

    // --- Audiobookshelf Connect & Sync ---
    fun saveAndConnectAudiobookshelf(
        name: String,
        hostUrl: String,
        token: String,
        username: String = "",
        password: String = ""
    ) {
        viewModelScope.launch {
            _serverOpState.value = ServerOperationState.Loading
            try {
                var activeToken = token.trim()
                if (activeToken.isBlank() && username.isNotBlank() && password.isNotBlank()) {
                    val loginRes = AudiobookshelfClient.login(hostUrl, username, password)
                    if (loginRes.isSuccess) {
                        activeToken = loginRes.getOrNull() ?: ""
                    } else {
                        _serverOpState.value = ServerOperationState.Error(
                            "Login failed: ${loginRes.exceptionOrNull()?.message}"
                        )
                        return@launch
                    }
                }

                val testRes = AudiobookshelfClient.testConnection(hostUrl, activeToken)
                if (testRes.isSuccess) {
                    val server = ServerConfig(
                        id = "abs_${System.currentTimeMillis()}",
                        name = name.ifBlank { "Audiobookshelf" },
                        type = "audiobookshelf",
                        hostUrl = hostUrl.trim(),
                        apiKey = activeToken,
                        username = username,
                        isConnected = true,
                        lastSyncTime = System.currentTimeMillis()
                    )
                    repository.addOrUpdateServer(server)

                    // Sync Library
                    val syncRes = repository.syncAudiobooks(server)
                    if (syncRes.isSuccess) {
                        _serverOpState.value = ServerOperationState.Success(
                            "Connected! Synced ${syncRes.getOrNull()} audiobooks."
                        )
                    } else {
                        _serverOpState.value = ServerOperationState.Success(
                            "Server connected, but library sync returned: ${syncRes.exceptionOrNull()?.message}"
                        )
                    }
                } else {
                    _serverOpState.value = ServerOperationState.Error(
                        "Connection test failed: ${testRes.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _serverOpState.value = ServerOperationState.Error("Error: ${e.message}")
            }
        }
    }

    // --- Plex Connect & Sync ---
    fun saveAndConnectPlexDirect(name: String, hostUrl: String, token: String) {
        viewModelScope.launch {
            _serverOpState.value = ServerOperationState.Loading
            try {
                val testRes = PlexClient.testConnection(hostUrl, token)
                if (testRes.isSuccess) {
                    val server = ServerConfig(
                        id = "plex_${System.currentTimeMillis()}",
                        name = name.ifBlank { "Plex Music" },
                        type = "plex",
                        hostUrl = hostUrl.trim(),
                        apiKey = token.trim(),
                        isConnected = true,
                        lastSyncTime = System.currentTimeMillis()
                    )
                    repository.addOrUpdateServer(server)

                    val syncRes = repository.syncPlex(server)
                    if (syncRes.isSuccess) {
                        _serverOpState.value = ServerOperationState.Success(
                            "Connected! Synced ${syncRes.getOrNull()} tracks from Plex."
                        )
                    } else {
                        _serverOpState.value = ServerOperationState.Success(
                            "Plex server connected, but music sync returned: ${syncRes.exceptionOrNull()?.message}"
                        )
                    }
                } else {
                    _serverOpState.value = ServerOperationState.Error(
                        "Plex connection failed: ${testRes.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _serverOpState.value = ServerOperationState.Error("Error: ${e.message}")
            }
        }
    }

    fun syncServer(server: ServerConfig) {
        viewModelScope.launch {
            _serverOpState.value = ServerOperationState.Loading
            val res = if (server.type == "audiobookshelf") {
                repository.syncAudiobooks(server)
            } else {
                repository.syncPlex(server)
            }
            if (res.isSuccess) {
                _serverOpState.value = ServerOperationState.Success("Synced ${res.getOrNull()} items successfully.")
            } else {
                _serverOpState.value = ServerOperationState.Error("Sync failed: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun removeServer(serverId: String) {
        viewModelScope.launch {
            repository.removeServer(serverId)
            _serverOpState.value = ServerOperationState.Success("Server removed.")
        }
    }

    fun resetServerOpState() {
        _serverOpState.value = ServerOperationState.Idle
    }

    // --- Playback Controls ---
    fun playAudiobook(book: Audiobook) {
        viewModelScope.launch {
            repository.updateProgress(book.id, book.progress)
            playbackManager.playAudiobook(book)
        }
    }

    fun playMusicTrack(track: MusicTrack) {
        viewModelScope.launch {
            repository.updateMusicLastPlayed(track.id)
            playbackManager.playMusicTrack(track)
        }
    }

    fun togglePlayPause() {
        playbackManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playbackManager.seekTo(positionMs)
    }

    fun setSpeed(speed: Float) {
        playbackManager.setPlaybackSpeed(speed)
    }

    fun skipForward(seconds: Int = 30) {
        playbackManager.skipForward(seconds)
    }

    fun skipBackward(seconds: Int = 10) {
        playbackManager.skipBackward(seconds)
    }

    fun toggleFavorite(book: Audiobook) {
        viewModelScope.launch {
            repository.toggleFavorite(book)
        }
    }

    // --- Gemini Discovery ---
    fun fetchDiscoveryRecommendations(prompt: String, location: Location? = null) {
        viewModelScope.launch {
            _isDiscoveryLoading.value = true
            _discoveryError.value = null
            try {
                val fullPrompt = if (location != null) {
                    "$prompt (User Location: Lat ${location.latitude}, Lng ${location.longitude}). Return 4-5 diverse audiobook and music recommendations with title, creator/author, and a short 1-line reason why."
                } else {
                    "$prompt. Return 4-5 diverse audiobook and music recommendations with title, creator/author, and a short 1-line reason why."
                }

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = fullPrompt))))
                )
                val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val lines = text.split("\n")
                    .map { it.trim().removePrefix("*").removePrefix("-").trim() }
                    .filter { it.isNotBlank() }
                _recommendations.value = lines
            } catch (e: Exception) {
                _discoveryError.value = "Unable to fetch AI recommendations: ${e.message}"
            } finally {
                _isDiscoveryLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}
