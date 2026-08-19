package com.example.ui

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.*
import com.example.data.*
import com.example.data.network.*
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

    // Diagnostic State
    private val _diagnosticResult = MutableStateFlow<AbsDiagnosticResult?>(null)
    val diagnosticResult = _diagnosticResult.asStateFlow()

    private val _isDiagnosing = MutableStateFlow(false)
    val isDiagnosing = _isDiagnosing.asStateFlow()

    private val _plexDiagnosticResult = MutableStateFlow<PlexDiagnosticResult?>(null)
    val plexDiagnosticResult = _plexDiagnosticResult.asStateFlow()

    private val _isDiagnosingPlex = MutableStateFlow(false)
    val isDiagnosingPlex = _isDiagnosingPlex.asStateFlow()

    // Plex PIN Link State
    private val _plexPinCode = MutableStateFlow<String?>(null)
    val plexPinCode = _plexPinCode.asStateFlow()

    private val _plexPinId = MutableStateFlow<Long?>(null)
    val plexPinId = _plexPinId.asStateFlow()

    private val _isRequestingPin = MutableStateFlow(false)
    val isRequestingPin = _isRequestingPin.asStateFlow()

    private val _isPollingPin = MutableStateFlow(false)
    val isPollingPin = _isPollingPin.asStateFlow()

    fun diagnoseAudiobookshelf(baseUrl: String, username: String = "", password: String = "", token: String = "") {
        viewModelScope.launch {
            _isDiagnosing.value = true
            try {
                val report = AudiobookshelfClient.diagnoseConnection(baseUrl, username, password, token)
                _diagnosticResult.value = report
            } catch (e: Exception) {
                _diagnosticResult.value = AbsDiagnosticResult(
                    isReachable = false,
                    testedUrl = baseUrl,
                    httpStatusCode = null,
                    success = false,
                    statusMessage = "Diagnostic error: ${e.message}",
                    latencyMs = 0,
                    sslValid = false,
                    diagnosticLog = listOf("Unexpected error: ${e.message}")
                )
            } finally {
                _isDiagnosing.value = false
            }
        }
    }

    fun clearDiagnosticResult() {
        _diagnosticResult.value = null
    }

    fun diagnosePlex(serverUrl: String, token: String = "") {
        viewModelScope.launch {
            _isDiagnosingPlex.value = true
            try {
                val report = PlexClient.diagnoseConnection(serverUrl, token)
                _plexDiagnosticResult.value = report
            } catch (e: Exception) {
                _plexDiagnosticResult.value = PlexDiagnosticResult(
                    isReachable = false,
                    testedUrl = serverUrl,
                    httpStatusCode = null,
                    success = false,
                    statusMessage = "Diagnostic error: ${e.message}",
                    latencyMs = 0,
                    diagnosticLog = listOf("Unexpected error: ${e.message}")
                )
            } finally {
                _isDiagnosingPlex.value = false
            }
        }
    }

    fun clearPlexDiagnosticResult() {
        _plexDiagnosticResult.value = null
    }

    fun requestPlexPin() {
        viewModelScope.launch {
            _isRequestingPin.value = true
            try {
                val result = PlexClient.createPin()
                if (result.isSuccess) {
                    val pin = result.getOrNull()
                    _plexPinCode.value = pin?.code
                    _plexPinId.value = pin?.id
                } else {
                    _serverOpState.value = ServerOperationState.Error("PIN creation failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _serverOpState.value = ServerOperationState.Error("Error: ${e.message}")
            } finally {
                _isRequestingPin.value = false
            }
        }
    }

    fun checkPlexPinStatus(onTokenReceived: (String) -> Unit) {
        val pinId = _plexPinId.value ?: return
        viewModelScope.launch {
            _isPollingPin.value = true
            try {
                val result = PlexClient.checkPin(pinId)
                if (result.isSuccess) {
                    val token = result.getOrNull()
                    if (!token.isNullOrBlank()) {
                        _plexPinCode.value = null
                        _plexPinId.value = null
                        onTokenReceived(token)
                        _serverOpState.value = ServerOperationState.Success("Plex account linked successfully!")
                    } else {
                        _serverOpState.value = ServerOperationState.Error("PIN not claimed yet on plex.tv/link. Please verify in your browser.")
                    }
                } else {
                    _serverOpState.value = ServerOperationState.Error("PIN check error: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _serverOpState.value = ServerOperationState.Error("Error checking PIN: ${e.message}")
            } finally {
                _isPollingPin.value = false
            }
        }
    }

    fun dismissPlexPin() {
        _plexPinCode.value = null
        _plexPinId.value = null
    }

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

    // --- Gemini Discovery (Grounding strictly on Active Server Media) ---
    fun fetchDiscoveryRecommendations(prompt: String, location: Location? = null, mediaType: String? = null) {
        viewModelScope.launch {
            _isDiscoveryLoading.value = true
            _discoveryError.value = null
            try {
                val currentBooks = allBooks.value
                val currentMusic = allMusic.value

                val booksSummary = if (currentBooks.isNotEmpty()) {
                    currentBooks.take(25).joinToString("; ") { "${it.title} by ${it.author}" }
                } else {
                    "No audiobooks synced yet"
                }

                val musicSummary = if (currentMusic.isNotEmpty()) {
                    currentMusic.take(25).joinToString("; ") { "${it.title} by ${it.artist} (${it.album})" }
                } else {
                    "No music tracks synced yet"
                }

                val locationHint = if (location != null) " (Context: Near Lat ${location.latitude}, Lng ${location.longitude})" else ""

                val fullPrompt = """
                    You are the HomeCast Personal Media Assistant.
                    The user has explicitly specified: ONLY generate recommendations and tailored queues BASED DIRECTLY ON THE MEDIA ACTIVELY PRESENT ON THEIR CONNECTED SERVERS.

                    Active Connected Server Inventory:
                    - Active Audiobooks: $booksSummary
                    - Active Music Collection: $musicSummary

                    User Request / Mood / Prompt: "$prompt"$locationHint
                    Requested Media Focus: ${mediaType ?: "All active media"}

                    Instructions:
                    1. Select and recommend 4-6 items or combinations ONLY from the active server items listed above.
                    2. Provide personalized reasons why each item fits the requested mood or listening session.
                    3. Return each recommendation on its own line in the exact format: 'Title - Creator: Reason/Highlight' (e.g. 'Project Hail Mary - Andy Weir: Outstanding immersive sci-fi narration with gripping stakes').
                    4. If the user's active inventory is currently empty, advise syncing Audiobookshelf or Plex in Settings and recommend setting up their server.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = fullPrompt))))
                )
                val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val lines = text.split("\n")
                    .map { it.trim().removePrefix("*").removePrefix("-").trim() }
                    .filter { it.isNotBlank() && it.length > 3 }
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
