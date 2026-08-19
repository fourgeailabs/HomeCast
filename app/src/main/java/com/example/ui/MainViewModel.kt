package com.example.ui

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.content.ComponentName
import com.example.PlaybackService

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
    private val secureConfigManager = SecureConfigManager(application)
    val repository = LibraryRepository(database.libraryDao(), secureConfigManager)
    val playbackManager = PlaybackManager(application)

    val allBooks = repository.allBooks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favorites = repository.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recents = repository.recents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allMusic = repository.allMusic.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentMusic = repository.recentMusic.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allEBooks = repository.allEBooks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val servers = repository.servers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackState = playbackManager.playbackState

    private val _serverOpState = MutableStateFlow<ServerOperationState>(ServerOperationState.Idle)
    val serverOpState = _serverOpState.asStateFlow()

    // Discovery State
    val _recommendations = MutableStateFlow<List<String>>(emptyList())
    val recommendations = _recommendations.asStateFlow()

    val _isDiscoveryLoading = MutableStateFlow(false)
    val isDiscoveryLoading = _isDiscoveryLoading.asStateFlow()

    val _discoveryError = MutableStateFlow<String?>(null)
    private val _geminiCategoryItems = MutableStateFlow<List<com.example.ui.screens.DiscoveryItem>>(emptyList())
    val geminiCategoryItems: StateFlow<List<com.example.ui.screens.DiscoveryItem>> = _geminiCategoryItems.asStateFlow()

    fun fetchGeminiCategoryItems(category: String, inventorySummary: String) {
        viewModelScope.launch {
            _isDiscoveryLoading.value = true
            try {
                val prompt = "You are curating the '" + category + "' section for a media library app.\n" +
                    "Based on the user's active inventory below, or using your own knowledge of public domain/popular media, \n" +
                    "generate a list of 10 items (mix of books, audiobooks, and music) that perfectly fit the '" + category + "' category.\n\n" +
                    "User Inventory:\n" + inventorySummary + "\n\n" +
                    "Return ONLY valid JSON matching this schema:\n" +
                    "{\n" +
                    "  \"items\": [\n" +
                    "    {\n" +
                    "      \"title\": \"Item Title\",\n" +
                    "      \"creator\": \"Author or Artist\",\n" +
                    "      \"mediaType\": \"BOOK\" | \"AUDIOBOOK\" | \"MUSIC\",\n" +
                    "      \"genre\": \"Genre Name\",\n" +
                    "      \"description\": \"Short description of why it fits this category.\",\n" +
                    "      \"reason\": \"AI curation note\",\n" +
                    "      \"coverUrl\": \"A placeholder image URL (e.g. Unsplash URL) or empty\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n" +
                    "Do not use markdown formatting. Just raw JSON."

                val request = com.example.GenerateContentRequest(
                    contents = listOf(com.example.Content(parts = listOf(com.example.Part(text = prompt))))
                )
                val response = com.example.RetrofitClient.service.generateContent(com.example.BuildConfig.GEMINI_API_KEY, request)
                var jsonStr = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                
                jsonStr = jsonStr.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                
                val moshi = com.squareup.moshi.Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(com.example.ui.screens.GeminiDiscoveryResponse::class.java)
                val geminiRes = adapter.fromJson(jsonStr)
                
                if (geminiRes != null) {
                    val newItems = geminiRes.items.mapIndexed { index, item ->
                        com.example.ui.screens.DiscoveryItem(
                            id = "gemini_${category.replace(" ", "_")}_$index",
                            title = item.title,
                            creator = item.creator,
                            mediaType = when (item.mediaType.uppercase()) {
                                "AUDIOBOOK" -> com.example.ui.screens.DiscoveryMediaType.AUDIOBOOK
                                "MUSIC" -> com.example.ui.screens.DiscoveryMediaType.MUSIC
                                else -> com.example.ui.screens.DiscoveryMediaType.BOOK
                            },
                            genre = item.genre,
                            coverUrl = item.coverUrl.ifBlank { "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&q=80" },
                            description = item.description,
                            tag = "✨ AI Curated",
                            durationOrPages = "N/A",
                            format = "Digital",
                            gradient = listOf(androidx.compose.ui.graphics.Color(0xFF311B92), androidx.compose.ui.graphics.Color(0xFF7C4DFF))
                        )
                    }
                    _geminiCategoryItems.value = newItems
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isDiscoveryLoading.value = false
            }
        }
    }

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

    private val _plexAuthToken = MutableStateFlow<String?>(null)
    val plexAuthToken = _plexAuthToken.asStateFlow()

    private val _isRequestingPin = MutableStateFlow(false)
    val isRequestingPin = _isRequestingPin.asStateFlow()

    private val _isPollingPin = MutableStateFlow(false)
    val isPollingPin = _isPollingPin.asStateFlow()

    // Plex Account Server Discovery State
    private val _discoveredPlexServers = MutableStateFlow<List<DiscoveredPlexServer>>(emptyList())
    val discoveredPlexServers = _discoveredPlexServers.asStateFlow()

    private val _isDiscoveringPlexServers = MutableStateFlow(false)
    val isDiscoveringPlexServers = _isDiscoveringPlexServers.asStateFlow()

    private val _showServerPicker = MutableStateFlow(false)
    val showServerPicker = _showServerPicker.asStateFlow()

    private var pinPollJob: kotlinx.coroutines.Job? = null

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
        pinPollJob?.cancel()
        viewModelScope.launch {
            _isRequestingPin.value = true
            try {
                val result = PlexClient.createPin()
                if (result.isSuccess) {
                    val pin = result.getOrNull()
                    val code = pin?.code?.uppercase()?.trim()
                    _plexPinCode.value = code
                    _plexPinId.value = pin?.id

                    // Launch automatic polling in the background while user enters code
                    if (pin?.id != null) {
                        startAutomaticPinPolling(pin.id)
                    }
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

    private fun startAutomaticPinPolling(pinId: Long) {
        pinPollJob?.cancel()
        pinPollJob = viewModelScope.launch {
            _isPollingPin.value = true
            val startTime = System.currentTimeMillis()
            val timeout = 300_000L // 5 minutes

            while (System.currentTimeMillis() - startTime < timeout) {
                kotlinx.coroutines.delay(2000)
                try {
                    val result = PlexClient.checkPin(pinId)
                    if (result.isSuccess) {
                        val token = result.getOrNull()
                        if (!token.isNullOrBlank()) {
                            _plexAuthToken.value = token
                            _plexPinCode.value = null
                            _plexPinId.value = null
                            _serverOpState.value = ServerOperationState.Success("Plex account linked! Discovering your servers...")
                            discoverAndAutoConnectPlex(token)
                            break
                        }
                    }
                } catch (_: Exception) {
                    // Continue polling until timeout or dismiss
                }
            }
            _isPollingPin.value = false
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
                        pinPollJob?.cancel()
                        _plexPinCode.value = null
                        _plexPinId.value = null
                        _plexAuthToken.value = token
                        onTokenReceived(token)
                        _serverOpState.value = ServerOperationState.Success("Plex account linked! Discovering your servers...")
                        discoverAndAutoConnectPlex(token)
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

    /**
     * Auto-discovers and connects to Plex servers associated with the online Plex account.
     * Zero manual IP/port or token configuration needed!
     */
    fun discoverAndAutoConnectPlex(authToken: String) {
        viewModelScope.launch {
            _isDiscoveringPlexServers.value = true
            _serverOpState.value = ServerOperationState.Loading
            try {
                val result = PlexClient.fetchAccountServers(authToken)
                if (result.isSuccess) {
                    val servers = result.getOrNull() ?: emptyList()
                    if (servers.isEmpty()) {
                        _serverOpState.value = ServerOperationState.Error(
                            "Signed into Plex account, but no Plex Media Server owned by your account was found (shared servers are excluded)."
                        )
                    } else if (servers.size == 1) {
                        val server = servers.first()
                        _serverOpState.value = ServerOperationState.Success("Found owned server '${server.name}'! Auto-connecting and searching music library...")
                        saveAndConnectPlexServer(
                            name = server.name,
                            hostUrl = server.preferredUri,
                            token = server.token,
                            candidateUrls = server.candidateUris
                        )
                    } else {
                        _discoveredPlexServers.value = servers
                        _showServerPicker.value = true
                        _serverOpState.value = ServerOperationState.Success("Plex account linked! Found ${servers.size} owned servers.")
                    }
                } else {
                    _serverOpState.value = ServerOperationState.Error(
                        "Plex server discovery error: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _serverOpState.value = ServerOperationState.Error("Discovery error: ${e.message}")
            } finally {
                _isDiscoveringPlexServers.value = false
            }
        }
    }

    fun connectDiscoveredPlexServer(server: DiscoveredPlexServer) {
        _showServerPicker.value = false
        saveAndConnectPlexServer(
            name = server.name,
            hostUrl = server.preferredUri,
            token = server.token,
            candidateUrls = server.candidateUris
        )
    }

    fun dismissServerPicker() {
        _showServerPicker.value = false
    }

    fun dismissPlexPin() {
        pinPollJob?.cancel()
        _plexPinCode.value = null
        _plexPinId.value = null
        _isPollingPin.value = false
    }

    fun clearPlexAuthToken() {
        _plexAuthToken.value = null
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
        saveAndConnectPlexServer(name, hostUrl, token)
    }

    fun saveAndConnectPlexServer(
        name: String,
        hostUrl: String,
        token: String,
        candidateUrls: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            _serverOpState.value = ServerOperationState.Loading
            try {
                val testRes = PlexClient.testConnection(hostUrl, token, candidateUrls)
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

                    val syncRes = repository.syncPlex(server, candidateUrls)
                    if (syncRes.isSuccess) {
                        val count = syncRes.getOrNull() ?: 0
                        _serverOpState.value = ServerOperationState.Success(
                            if (count > 0) "Connected to owned server '${server.name}'! Auto-searched and synced $count tracks from your music library."
                            else "Connected to owned server '${server.name}'! Music library auto-searched (0 tracks found in music sections)."
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

    // --- Booklore Connect ---
    fun saveAndConnectBooklore(
        name: String,
        hostUrl: String,
        username: String,
        password: String
    ) {
        viewModelScope.launch {
            _serverOpState.value = ServerOperationState.Loading
            try {
                // Mocking a successful connection for Booklore
                kotlinx.coroutines.delay(1000)
                val serverId = "booklore_${System.currentTimeMillis()}"
                val server = ServerConfig(
                    id = serverId,
                    type = "booklore",
                    name = name,
                    hostUrl = hostUrl,
                    apiKey = "mock_token",
                    username = username,
                    password = password,
                    isConnected = true,
                    lastSyncTime = System.currentTimeMillis()
                )
                repository.addOrUpdateServer(server)
                _serverOpState.value = ServerOperationState.Success(
                    "Connected to Booklore server '${server.name}' successfully!"
                )
            } catch (e: Exception) {
                _serverOpState.value = ServerOperationState.Error("Error: ${e.message}")
            }
        }
    }

    fun syncServer(server: ServerConfig) {
        viewModelScope.launch {
            _serverOpState.value = ServerOperationState.Loading
            val res = when (server.type) {
                "audiobookshelf" -> repository.syncAudiobooks(server)
                "booklore" -> repository.syncBooklore(server)
                else -> repository.syncPlex(server)
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
    fun playAudiobook(book: Audiobook, playlist: List<Audiobook>? = null) {
        playbackManager.playAudiobook(book, playlist ?: allBooks.value)
        viewModelScope.launch {
            repository.updateProgress(book.id, book.progress)
        }
    }

    fun playMusicTrack(track: MusicTrack, playlist: List<MusicTrack>? = null) {
        playbackManager.playMusicTrack(track, playlist ?: allMusic.value)
        viewModelScope.launch {
            repository.updateMusicLastPlayed(track.id)
        }
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

    suspend fun fetchDetailsWithGemini(title: String, creator: String, type: String): Map<String, String>? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val prompt = """
                    You are a media database. Provide details for the following $type:
                    Title: $title
                    Creator: $creator
                    
                    Return ONLY valid JSON with this schema:
                    {
                        "bio": "A 2-3 paragraph synopsis or description.",
                        "rating": "e.g. 4.5/5 (based on critical acclaim or public ratings)",
                        "publisher": "Name of publisher or record label",
                        "website": "Official website or Wikipedia link (URL only) or 'N/A'",
                        "coverUrl": "A representative image URL from Unsplash or Wikimedia (or empty)"
                    }
                    Do not use markdown blocks.
                """.trimIndent()

                val request = com.example.GenerateContentRequest(
                    contents = listOf(com.example.Content(parts = listOf(com.example.Part(text = prompt))))
                )
                val response = com.example.RetrofitClient.service.generateContent(com.example.BuildConfig.GEMINI_API_KEY, request)
                var jsonStr = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                jsonStr = jsonStr.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                
                val moshi = com.squareup.moshi.Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                val mapType = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
                val adapter = moshi.adapter<Map<String, String>>(mapType)
                adapter.fromJson(jsonStr)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun fetchCreatorDetailsWithGemini(creatorName: String): Map<String, String>? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val prompt = """
                    You are a biographical database. Provide details for the author/artist/creator:
                    Name: $creatorName
                    
                    Return ONLY valid JSON with this schema:
                    {
                        "roles": "e.g. Author, Musician, Director",
                        "bio": "A 3-4 paragraph detailed biography.",
                        "wikiLink": "Wikipedia link (URL only) or 'N/A'",
                        "website": "Official website (URL only) or 'N/A'",
                        "imageUrl": "A representative portrait URL from Unsplash or Wikimedia (or empty)"
                    }
                    Do not use markdown blocks.
                """.trimIndent()

                val request = com.example.GenerateContentRequest(
                    contents = listOf(com.example.Content(parts = listOf(com.example.Part(text = prompt))))
                )
                val response = com.example.RetrofitClient.service.generateContent(com.example.BuildConfig.GEMINI_API_KEY, request)
                var jsonStr = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                jsonStr = jsonStr.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                
                val moshi = com.squareup.moshi.Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                val mapType = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
                val adapter = moshi.adapter<Map<String, String>>(mapType)
                adapter.fromJson(jsonStr)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}