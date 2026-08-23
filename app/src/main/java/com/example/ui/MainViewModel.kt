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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private val _publicDomainBooks = MutableStateFlow<List<ArchiveDoc>>(emptyList())
    val publicDomainBooks = _publicDomainBooks.asStateFlow()

    private val _publicDomainAudiobooks = MutableStateFlow<List<ArchiveDoc>>(emptyList())
    val publicDomainAudiobooks = _publicDomainAudiobooks.asStateFlow()

    private val _publicDomainMusic = MutableStateFlow<List<ArchiveDoc>>(emptyList())
    val publicDomainMusic = _publicDomainMusic.asStateFlow()

    private val _resolvedDurations = MutableStateFlow<Map<String, Long>>(emptyMap())
    val resolvedDurations = _resolvedDurations.asStateFlow()

    private val _isCleaningUp = MutableStateFlow(false)
    val isCleaningUp = _isCleaningUp.asStateFlow()

    private val _isLocatingCovers = MutableStateFlow(false)
    val isLocatingCovers = _isLocatingCovers.asStateFlow()

    val initialRoute: String
    val initialEbooksSource: Int
    val initialAudiobooksSource: Int
    val initialMusicSource: Int

    init {
        val sharedPrefs = application.getSharedPreferences("playback_prefs", android.content.Context.MODE_PRIVATE)
        val lastType = sharedPrefs.getString("last_media_type", null)
        val lastId = sharedPrefs.getString("last_media_id", null)
        val lastTitle = sharedPrefs.getString("last_media_title", null)
        val lastCreator = sharedPrefs.getString("last_media_creator", null)
        val lastCover = sharedPrefs.getString("last_media_cover", "") ?: ""
        val lastSource = sharedPrefs.getInt("last_media_source", 0)

        if (lastType != null && lastId != null && lastTitle != null && lastCreator != null) {
            when (lastType) {
                "AUDIOBOOK" -> {
                    initialRoute = "library"
                    initialAudiobooksSource = lastSource
                    initialEbooksSource = 0
                    initialMusicSource = 0
                    val book = Audiobook(
                        id = lastId,
                        title = lastTitle,
                        author = lastCreator,
                        coverUrl = lastCover,
                        duration = 3600L,
                        serverId = if (lastSource == 1) "pd_server" else "personal",
                        streamUrl = ""
                    )
                    playbackManager.setInitialAudiobook(book)
                }
                "MUSIC" -> {
                    initialRoute = "music"
                    initialAudiobooksSource = 0
                    initialEbooksSource = 0
                    initialMusicSource = lastSource
                    val track = MusicTrack(
                        id = lastId,
                        title = lastTitle,
                        artist = lastCreator,
                        album = "Last Played",
                        coverUrl = lastCover,
                        duration = 180000L,
                        serverId = if (lastSource == 1) "pd_server" else "personal",
                        streamUrl = ""
                    )
                    playbackManager.setInitialMusicTrack(track)
                }
                "BOOK" -> {
                    initialRoute = "ebooks"
                    initialEbooksSource = lastSource
                    initialAudiobooksSource = 0
                    initialMusicSource = 0
                }
                else -> {
                    initialRoute = "library"
                    initialEbooksSource = 0
                    initialAudiobooksSource = 0
                    initialMusicSource = 0
                }
            }
        } else {
            initialRoute = "library"
            initialEbooksSource = 0
            initialAudiobooksSource = 0
            initialMusicSource = 0
        }

        viewModelScope.launch {
            // Fetch multiple varied collections for E-Books, Audiobooks, and Music!
            val booksList = mutableListOf<ArchiveDoc>()
            booksList.addAll(ArchiveOrgClient.fetchPublicDomain("collection:(gutenberg)"))
            booksList.addAll(ArchiveOrgClient.fetchPublicDomain("collection:(smithsonian) AND mediatype:(texts)"))
            booksList.addAll(ArchiveOrgClient.fetchPublicDomain("collection:(opened_publications) AND mediatype:(texts)"))
            _publicDomainBooks.value = booksList.distinctBy { it.identifier }

            val audiobooksList = mutableListOf<ArchiveDoc>()
            audiobooksList.addAll(ArchiveOrgClient.fetchPublicDomain("collection:(librivoxaudio)"))
            audiobooksList.addAll(ArchiveOrgClient.fetchPublicDomain("collection:(audio_bookspoetry)"))
            _publicDomainAudiobooks.value = audiobooksList.distinctBy { it.identifier }

            val musicList = mutableListOf<ArchiveDoc>()
            musicList.addAll(ArchiveOrgClient.fetchPublicDomain("collection:(78rpm) AND (subject:(jazz) OR subject:(blues) OR subject:(classic))"))
            musicList.addAll(ArchiveOrgClient.fetchPublicDomain("collection:(etree) AND (subject:(concert) OR subject:(acoustic) OR subject:(live))"))
            musicList.addAll(ArchiveOrgClient.fetchPublicDomain("collection:(netlabels) AND (subject:(electronic) OR subject:(ambient) OR subject:(synth))"))
            _publicDomainMusic.value = musicList.distinctBy { it.identifier }

            // Launch background resolution for audiobooks duration
            launch(Dispatchers.IO) {
                try {
                    val semaphore = Semaphore(3)
                    _publicDomainAudiobooks.collect { docs ->
                        if (docs.isNotEmpty()) {
                            kotlinx.coroutines.delay(2000)
                            docs.forEach { doc ->
                                launch {
                                    try {
                                        semaphore.withPermit {
                                            val files = ArchiveOrgClient.fetchFilesForIdentifier(doc.identifier)
                                            val mp3Files = files.filter { it.name.endsWith(".mp3", ignoreCase = true) }
                                            val totalLength = mp3Files.sumOf { it.length }.toLong()
                                            if (totalLength > 0) {
                                                val estimatedDurationSeconds = (totalLength / 10000L).coerceAtLeast(300L)
                                                _resolvedDurations.value = _resolvedDurations.value + (doc.identifier to estimatedDurationSeconds)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // ignore
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            
            // Check if database is empty of seeded items
            val hasEBooks = database.libraryDao().getAllEBooks().firstOrNull()?.isNotEmpty() ?: false
            val hasAudiobooks = database.libraryDao().getAllBooks().firstOrNull()?.isNotEmpty() ?: false
            val hasMusic = database.libraryDao().getAllMusic().firstOrNull()?.isNotEmpty() ?: false

            if (!hasEBooks && !hasAudiobooks && !hasMusic) {
                // Seed database with messy items that Gemini will beautifully format and locate covers for!
                val seedEBooks = listOf(
                    com.example.data.EBook(
                        id = "ebook_seeded_1",
                        title = "The Time Machine",
                        author = "Wells, H. G. (Herbert George), 1866-1946",
                        coverUrl = "placeholder",
                        serverId = "demo_server",
                        genre = "/bonevolume7ghost0000smit_r6d2",
                        description = "A Victorian scientist constructs a machine that travels through fourth-dimensional spacetime into the far future year 802,701 AD.",
                        totalPages = 180,
                        progressPercent = 65
                    ),
                    com.example.data.EBook(
                        id = "ebook_seeded_2",
                        title = "The Great Gatsby",
                        author = "Fitzgerald, F. Scott (Francis Scott), 1896-1940",
                        coverUrl = "placeholder",
                        serverId = "demo_server",
                        genre = "/bonevolume7ghost0000smit_r6d2",
                        description = "A classic novel exploring themes of wealth, love, and the disillusionment of the American Dream.",
                        totalPages = 190,
                        progressPercent = 12
                    ),
                    com.example.data.EBook(
                        id = "ebook_seeded_3",
                        title = "The Art of War",
                        author = "Sun Tzu, 5th cent. B.C.",
                        coverUrl = "placeholder",
                        serverId = "demo_server",
                        genre = "Various",
                        description = "Ancient Chinese military treatise attributed to Sun Tzu, devoted to strategic thinking and tactics.",
                        totalPages = 112,
                        progressPercent = 100
                    ),
                    com.example.data.EBook(
                        id = "ebook_seeded_4",
                        title = "Frankenstein",
                        author = "Shelley, Mary Wollstonecraft, 1797-1851",
                        coverUrl = "placeholder",
                        serverId = "demo_server",
                        genre = "Uncategorized",
                        description = "Victor Frankenstein creates a sentient creature in an unorthodox scientific experiment with haunting consequences.",
                        totalPages = 280,
                        progressPercent = 30
                    ),
                    com.example.data.EBook(
                        id = "ebook_seeded_5",
                        title = "Metamorphosis",
                        author = "Kafka, Franz, 1883-1924",
                        coverUrl = "placeholder",
                        serverId = "demo_server",
                        genre = "/bonevolume7ghost0000smit_r6d2",
                        description = "Gregor Samsa awakens one morning to find himself transformed into a monstrous insect.",
                        totalPages = 128,
                        progressPercent = 50
                    )
                )

                val seedAudiobooks = listOf(
                    com.example.data.Audiobook(
                        id = "audiobook_seeded_1",
                        title = "The Adventures of Sherlock Holmes",
                        author = "Doyle, Arthur Conan, Sir, 1859-1930",
                        coverUrl = "placeholder",
                        duration = 3600L, // Corrected duration in seconds
                        serverId = "demo_server",
                        genre = "/bonevolume7ghost0000smit_r6d2",
                        narrator = "LibriVox Narrator",
                        streamUrl = "https://archive.org/download/sherlock_holmes_adventures_64kb_librivox/sherlock_holmes_adventures_01_doyle_64kb.mp3"
                    ),
                    com.example.data.Audiobook(
                        id = "audiobook_seeded_2",
                        title = "Dracula",
                        author = "Stoker, Bram, 1847-1912",
                        coverUrl = "placeholder",
                        duration = 5400L, // Corrected duration in seconds
                        serverId = "demo_server",
                        genre = "Various",
                        narrator = "LibriVox Narrator",
                        streamUrl = "https://archive.org/download/dracula_librivox/dracula_01_stoker_64kb.mp3"
                    )
                )

                val seedMusic = listOf(
                    com.example.data.MusicTrack(
                        id = "music_seeded_1",
                        title = "Synthwave Sunset",
                        artist = "Retro Synth Waves",
                        album = "Neon Horizon",
                        coverUrl = "placeholder",
                        duration = 180000L,
                        serverId = "demo_server",
                        streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3",
                        genre = "https://some-url.com/.txt"
                    ),
                    com.example.data.MusicTrack(
                        id = "music_seeded_2",
                        title = "Acoustic Reflection",
                        artist = "Warm Acoustic Guitar Trio",
                        album = "Reflections",
                        coverUrl = "placeholder",
                        duration = 240000L,
                        serverId = "demo_server",
                        streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3",
                        genre = "Various"
                    )
                )

                database.libraryDao().insertEBooks(seedEBooks)
                database.libraryDao().insertBooks(seedAudiobooks)
                database.libraryDao().insertMusicTracks(seedMusic)
            }
            
            // Automatically clean authors/genres and locate beautiful cover-art URLs immediately!
            performDailyDynamicMenuAndCategoryCleanup()
        }
    }

    fun checkAndTriggerDailyCleanupIfNeeded() {
        viewModelScope.launch {
            val lastCleanup = secureConfigManager.getLastCleanupDate()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            if (lastCleanup != todayStr) {
                val success = performDailyDynamicMenuAndCategoryCleanup()
                if (success) {
                    secureConfigManager.saveLastCleanupDate(todayStr)
                }
            }
        }
    }

    val recommendations = _recommendations.asStateFlow()

    val _isDiscoveryLoading = MutableStateFlow(false)
    val isDiscoveryLoading = _isDiscoveryLoading.asStateFlow()

    val _discoveryError = MutableStateFlow<String?>(null)
    private val _geminiCategoryItems = MutableStateFlow<List<com.example.ui.screens.DiscoveryItem>>(emptyList())
    val geminiCategoryItems: StateFlow<List<com.example.ui.screens.DiscoveryItem>> = _geminiCategoryItems.asStateFlow()


    fun categorizeBooksWithAI() {
        viewModelScope.launch {
            val books = allEBooks.value
            if (books.isEmpty()) return@launch
            
            // We batch them to avoid huge prompts
            val batch = books.take(30)
            val titles = batch.joinToString("\n") { it.id + ":::" + it.title + " by " + it.author }
            
            val prompt = "Here is a list of books and comics with IDs, titles, and authors.\n" +
                "Please categorize each into a single, high-level, precise genre (e.g., 'Sci-Fi', 'Fantasy', 'Cyberpunk', 'Manga', 'Superhero Comic', 'Non-Fiction').\n" +
                "Return ONLY a raw JSON array matching this schema:\n" +
                "[\n" +
                "  {\n" +
                "    \"id\": \"book id\",\n" +
                "    \"genre\": \"assigned genre\"\n" +
                "  }\n" +
                "]\n\n" +
                "List:\n" + titles

            try {
                val request = com.example.GenerateContentRequest(
                    contents = listOf(com.example.Content(parts = listOf(com.example.Part(text = prompt))))
                )
                val response = com.example.RetrofitClient.service.generateContent(com.example.BuildConfig.GEMINI_API_KEY, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                if (rawText.isNotBlank()) {
                    val jsonStr = rawText.substringAfter("[").substringBeforeLast("]")
                    val jsonArray = org.json.JSONArray("[" + jsonStr + "]")
                    
                    val updatedBooks = mutableListOf<com.example.data.EBook>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.optString("id")
                        val genre = obj.optString("genre")
                        
                        val book = batch.find { it.id == id }
                        if (book != null && genre.isNotBlank()) {
                            updatedBooks.add(book.copy(genre = genre))
                        }
                    }
                    if (updatedBooks.isNotEmpty()) {
                        database.libraryDao().insertEBooks(updatedBooks)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun extractJsonString(rawText: String): String {
        val startIndex = rawText.indexOfAny(listOf("{", "["))
        val endIndex = rawText.lastIndexOfAny(listOf("}", "]"))
        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            rawText.substring(startIndex, endIndex + 1)
        } else {
            rawText
        }
    }

    fun triggerManualDailyCleanup() {
        viewModelScope.launch {
            performDailyDynamicMenuAndCategoryCleanup()
        }
    }

    fun triggerManualCoverLocation() {
        viewModelScope.launch {
            locateMissingCoverArtWithAI()
        }
    }

    suspend fun performDailyDynamicMenuAndCategoryCleanup(): Boolean {
        _isCleaningUp.value = true
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val ebooks = database.libraryDao().getAllEBooks().firstOrNull() ?: emptyList()
                val audiobooks = database.libraryDao().getAllBooks().firstOrNull() ?: emptyList()
                val music = database.libraryDao().getAllMusic().firstOrNull() ?: emptyList()

                if (ebooks.isEmpty() && audiobooks.isEmpty() && music.isEmpty()) {
                    return@withContext false
                }

                val itemsText = java.lang.StringBuilder()
                ebooks.forEach { itemsText.append("${it.id} | EBOOK | ${it.title} | ${it.author} | ${it.genre}\n") }
                audiobooks.forEach { itemsText.append("${it.id} | AUDIOBOOK | ${it.title} | ${it.author} | ${it.genre}\n") }
                music.forEach { itemsText.append("${it.id} | MUSIC | ${it.title} | ${it.artist} | ${it.genre}\n") }

                val sdfDay = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
                val dayOfWeekString = sdfDay.format(Date())

                val prompt = """
                    You are the HomeCast Media Optimizer AI.
                    Today's Date/Day context is: $dayOfWeekString.
                    Your task is to:
                    1. Generate exactly 5-6 fresh, beautiful, high-level, unique thematic categories for today (e.g. "Cosmic Journeys", "Vintage Classics", "Retro Synth Beats", "Philosophical Strategy", "Neon Cyberpunk", "Mindful Resonance"). These must vary on a daily basis (use today's date context to make them different).
                    2. Review the list of active library items below.
                    3. Categorize each item into ONE of these 5-6 newly generated categories based on its title and author/artist.
                    4. Clean up raw library catalog author/artist names into clean, reader-friendly, natural human names. For example:
                       - 'Smith, Jeff, 1960 Feb...' should become 'Jeff Smith'
                       - 'Wells, H. G. (Herbert George), 1866-1946' should become 'H.G. Wells'
                       - 'Sun Tzu, 5th cent. B.C.' should become 'Sun Tzu'
                       - 'Fitzgerald, F. Scott (Francis Scott), 1896-1940' should become 'F. Scott Fitzgerald'
                       - Remove any birth/death dates, raw catalog parentheticals, and trailing commas/periods.
                    5. Ensure that NO categories resemble raw directories, folder paths, URLs, or file extensions (e.g., no '/bonevolume...', no 'gutenberg', no '.txt', no 'Various', no 'Uncategorized', no 'https://...'). Everything must be beautifully categorized in proper English.
                    
                    Active Items:
                    $itemsText
                    
                    Return ONLY valid JSON matching this exact schema:
                    {
                      "categories": ["Category 1", "Category 2", "Category 3", "Category 4", "Category 5", "Category 6"],
                      "items": [
                        {
                          "id": "item ID",
                          "type": "EBOOK" | "AUDIOBOOK" | "MUSIC",
                          "cleanedAuthor": "Clean Human Name",
                          "genre": "Assigned Category"
                        }
                      ]
                    }
                    Do not wrap in markdown or code blocks. Just raw JSON.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                )
                val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                if (rawText.isBlank()) return@withContext false

                val cleanedJsonStr = extractJsonString(rawText)
                val rootObj = org.json.JSONObject(cleanedJsonStr)
                val itemsArray = rootObj.optJSONArray("items") ?: org.json.JSONArray()

                val updatedEbooks = mutableListOf<com.example.data.EBook>()
                val updatedAudiobooks = mutableListOf<com.example.data.Audiobook>()
                val updatedMusic = mutableListOf<com.example.data.MusicTrack>()

                for (i in 0 until itemsArray.length()) {
                    val obj = itemsArray.getJSONObject(i)
                    val id = obj.optString("id")
                    val type = obj.optString("type")
                    val cleanedAuthor = obj.optString("cleanedAuthor")
                    val genre = obj.optString("genre")

                    if (id.isBlank() || genre.isBlank()) continue

                    when (type.uppercase()) {
                        "EBOOK" -> {
                            val original = ebooks.find { it.id == id }
                            if (original != null) {
                                updatedEbooks.add(original.copy(
                                    author = cleanedAuthor.ifBlank { original.author },
                                    genre = genre
                                ))
                            }
                        }
                        "AUDIOBOOK" -> {
                            val original = audiobooks.find { it.id == id }
                            if (original != null) {
                                updatedAudiobooks.add(original.copy(
                                    author = cleanedAuthor.ifBlank { original.author },
                                    genre = genre
                                ))
                            }
                        }
                        "MUSIC" -> {
                            val original = music.find { it.id == id }
                            if (original != null) {
                                updatedMusic.add(original.copy(
                                    artist = cleanedAuthor.ifBlank { original.artist },
                                    genre = genre
                                ))
                            }
                        }
                    }
                }

                if (updatedEbooks.isNotEmpty()) database.libraryDao().insertEBooks(updatedEbooks)
                if (updatedAudiobooks.isNotEmpty()) database.libraryDao().insertBooks(updatedAudiobooks)
                if (updatedMusic.isNotEmpty()) database.libraryDao().insertMusicTracks(updatedMusic)

                // Trigger cover art location for newly seeded/inserted items
                viewModelScope.launch {
                    locateMissingCoverArtWithAI()
                }

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
                _isCleaningUp.value = false
            }
        }
    }

    suspend fun locateMissingCoverArtWithAI(): Boolean {
        _isLocatingCovers.value = true
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val ebooks = database.libraryDao().getAllEBooks().firstOrNull() ?: emptyList()
                val audiobooks = database.libraryDao().getAllBooks().firstOrNull() ?: emptyList()
                val music = database.libraryDao().getAllMusic().firstOrNull() ?: emptyList()

                val missingEbooks = ebooks.filter { 
                    it.coverUrl.isBlank() || 
                    it.coverUrl.contains("photo-1544716278-ca5e3f4abd8c") || 
                    it.coverUrl.startsWith("http://localhost") ||
                    it.coverUrl == "placeholder"
                }
                val missingAudiobooks = audiobooks.filter { 
                    it.coverUrl.isBlank() || 
                    it.coverUrl.contains("photo-1544716278-ca5e3f4abd8c") || 
                    it.coverUrl == "placeholder"
                }
                val missingMusic = music.filter { 
                    it.coverUrl.isBlank() || 
                    it.coverUrl.contains("photo-1544716278-ca5e3f4abd8c") || 
                    it.coverUrl == "placeholder"
                }

                if (missingEbooks.isEmpty() && missingAudiobooks.isEmpty() && missingMusic.isEmpty()) {
                    return@withContext true
                }

                val itemsText = java.lang.StringBuilder()
                missingEbooks.take(15).forEach { itemsText.append("${it.id} | EBOOK | ${it.title} | ${it.author}\n") }
                missingAudiobooks.take(15).forEach { itemsText.append("${it.id} | AUDIOBOOK | ${it.title} | ${it.author}\n") }
                missingMusic.take(15).forEach { itemsText.append("${it.id} | MUSIC | ${it.title} | ${it.artist}\n") }

                val prompt = """
                    You are the HomeCast Media Cover Locator AI.
                    The items below are missing high-quality cover art. Your task is to provide a highly relevant, high-quality, beautiful public image URL for each item's cover.
                    
                    Instructions for choosing URLs:
                    1. Use specific, beautiful, and thematic image URLs from Unsplash, curated to match the title or mood of the media (e.g. cyberpunk, classic books, acoustic guitar, dark space scenery, synthwave elements).
                    2. Example high-quality Unsplash images:
                       - Sci-Fi: 'https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=500'
                       - Tech/Cyberpunk: 'https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=500'
                       - Fantasy/Adventure: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500'
                       - Classic Books: 'https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=500'
                       - Music/Synth: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500'
                       - Audiobooks: 'https://images.unsplash.com/photo-1484704849700-f032a568e944?w=500'
                    3. Return ONLY valid JSON matching this exact schema:
                    [
                      {
                        "id": "item ID",
                        "type": "EBOOK" | "AUDIOBOOK" | "MUSIC",
                        "coverUrl": "Direct image URL"
                      }
                    ]
                    Do not wrap in markdown or code blocks. Just raw JSON.
                    
                    Missing Cover Items:
                    $itemsText
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                )
                val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                if (rawText.isBlank()) return@withContext false

                val cleanedJsonStr = extractJsonString(rawText)
                val itemsArray = org.json.JSONArray(cleanedJsonStr)

                val updatedEbooks = mutableListOf<com.example.data.EBook>()
                val updatedAudiobooks = mutableListOf<com.example.data.Audiobook>()
                val updatedMusic = mutableListOf<com.example.data.MusicTrack>()

                for (i in 0 until itemsArray.length()) {
                    val obj = itemsArray.getJSONObject(i)
                    val id = obj.optString("id")
                    val type = obj.optString("type")
                    val coverUrl = obj.optString("coverUrl")

                    if (id.isBlank() || coverUrl.isBlank()) continue

                    when (type.uppercase()) {
                        "EBOOK" -> {
                            val original = ebooks.find { it.id == id }
                            if (original != null) {
                                updatedEbooks.add(original.copy(coverUrl = coverUrl))
                            }
                        }
                        "AUDIOBOOK" -> {
                            val original = audiobooks.find { it.id == id }
                            if (original != null) {
                                updatedAudiobooks.add(original.copy(coverUrl = coverUrl))
                            }
                        }
                        "MUSIC" -> {
                            val original = music.find { it.id == id }
                            if (original != null) {
                                updatedMusic.add(original.copy(coverUrl = coverUrl))
                            }
                        }
                    }
                }

                if (updatedEbooks.isNotEmpty()) database.libraryDao().insertEBooks(updatedEbooks)
                if (updatedAudiobooks.isNotEmpty()) database.libraryDao().insertBooks(updatedAudiobooks)
                if (updatedMusic.isNotEmpty()) database.libraryDao().insertMusicTracks(updatedMusic)

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
                _isLocatingCovers.value = false
            }
        }
    }

    fun fetchGeminiCategoryItems(category: String, sourceStr: String) {
        viewModelScope.launch {
            _isDiscoveryLoading.value = true
            try {
                val inventory = if (sourceStr.contains("private")) {
                    val b = allBooks.value.take(20).joinToString { it.title + " by " + it.author }
                    val e = allEBooks.value.take(20).joinToString { it.title + " by " + it.author }
                    val m = allMusic.value.take(20).joinToString { it.title + " by " + it.artist }
                    "Audiobooks: $b\nEBooks: $e\nMusic: $m"
                } else {
                    val pb = _publicDomainBooks.value.take(20).joinToString { it.title + " by " + (it.creator ?: "Unknown") }
                    val pa = _publicDomainAudiobooks.value.take(20).joinToString { it.title + " by " + (it.creator ?: "Unknown") }
                    "Public Domain Books: $pb\nPublic Domain Audiobooks: $pa"
                }
                val prompt = "You are curating the '" + category + "' section for a media library app.\n" +
                    "Based on the user's active inventory below, or using your own knowledge of public domain/popular media, \n" +
                    "generate a list of 10 items (mix of books, audiobooks, and music) that perfectly fit the '" + category + "' category.\n\n" +
                    "User Inventory:\n" + inventory + "\n\n" +
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
                            title = item.title,
                            creator = item.creator,
                            mediaType = when (item.mediaType.uppercase()) {
                                "AUDIOBOOK" -> com.example.ui.screens.DiscoveryMediaType.AUDIOBOOK
                                "MUSIC" -> com.example.ui.screens.DiscoveryMediaType.MUSIC
                                else -> com.example.ui.screens.DiscoveryMediaType.BOOK
                            },
                            genre = item.genre,
                            coverUrl = item.coverUrl.ifBlank { "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&q=80" },
                            description = item.description
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
                // Fetch JWT from Booklore login endpoint
                val loginResult = com.example.data.network.BookloreClient.login(hostUrl, username, password)
                if (loginResult.isFailure) {
                    _serverOpState.value = ServerOperationState.Error("Login failed: ${loginResult.exceptionOrNull()?.message}")
                    return@launch
                }
                val token = loginResult.getOrNull() ?: ""
                
                val serverId = "booklore_${System.currentTimeMillis()}"
                
                val server = ServerConfig(
                    id = serverId,
                    type = "booklore",
                    name = name,
                    hostUrl = hostUrl,
                    apiKey = token, // Store the JWT token
                    username = username,
                    password = password,
                    isConnected = true,
                    lastSyncTime = System.currentTimeMillis()
                )
                
                // Try initial sync
                val syncResult = repository.syncBooklore(server)
                if (syncResult.isSuccess) {
                    repository.addOrUpdateServer(server)
                    _serverOpState.value = ServerOperationState.Success(
                        "Connected to Booklore server '${server.name}' successfully! Synced ${syncResult.getOrNull()} books."
                    )
                } else {
                    _serverOpState.value = ServerOperationState.Error("Failed to connect or sync: ${syncResult.exceptionOrNull()?.message}")
                }
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
                // Run AI dynamic cleanup and category optimization on the new items!
                launch {
                    try {
                        performDailyDynamicMenuAndCategoryCleanup()
                        locateMissingCoverArtWithAI()
                    } catch (e: Exception) {
                        android.util.Log.e("MainViewModel", "AI post-sync cleanup failed", e)
                    }
                }
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
    fun saveLastMediaPlayed(type: String, id: String, title: String, creator: String, coverUrl: String, source: Int) {
        val sharedPrefs = getApplication<android.app.Application>().getSharedPreferences("playback_prefs", android.content.Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putString("last_media_type", type)
            .putString("last_media_id", id)
            .putString("last_media_title", title)
            .putString("last_media_creator", creator)
            .putString("last_media_cover", coverUrl)
            .putInt("last_media_source", source)
            .apply()
    }

    fun saveLastEBookRead(id: String, title: String, author: String, coverUrl: String) {
        val source = if (id.startsWith("ebook_seeded") || id.contains("pd_") || id.startsWith("txt_") || id.startsWith("guten_")) 1 else 0
        saveLastMediaPlayed("BOOK", id, title, author, coverUrl, source)
    }

    fun playAudiobook(book: Audiobook, playlist: List<Audiobook>? = null) {
        playbackManager.playAudiobook(book, playlist ?: allBooks.value)
        viewModelScope.launch {
            repository.updateProgress(book.id, book.progress)
        }
        val source = if (book.serverId == "demo_server" || book.serverId == "pd_server") 1 else 0
        saveLastMediaPlayed("AUDIOBOOK", book.id, book.title, book.author, book.coverUrl, source)
    }

    fun playAudiobookWithResolution(book: Audiobook, playlist: List<Audiobook>? = null) {
        viewModelScope.launch {
            var finalUrl = book.streamUrl
            if (book.serverId == "pd_server" && (finalUrl.isBlank() || finalUrl.contains("_64kb.mp3"))) {
                val resolved = resolveArchiveOrgStreamUrl(book.id, ".mp3")
                if (resolved.isNotBlank()) {
                    finalUrl = resolved
                }
            }
            val resolvedBook = book.copy(streamUrl = finalUrl)
            val resolvedPlaylist = playlist?.map { b ->
                if (b.id == book.id) resolvedBook else b
            }
            playAudiobook(resolvedBook, resolvedPlaylist)
        }
    }

    fun playMusicTrack(track: MusicTrack, playlist: List<MusicTrack>? = null) {
        playbackManager.playMusicTrack(track, playlist ?: allMusic.value)
        viewModelScope.launch {
            repository.updateMusicLastPlayed(track.id)
        }
        val source = if (track.serverId == "demo_server" || track.serverId == "pd_server") 1 else 0
        saveLastMediaPlayed("MUSIC", track.id, track.title, track.artist, track.coverUrl, source)
    }

    fun playMusicTrackWithResolution(track: MusicTrack, playlist: List<MusicTrack>? = null) {
        viewModelScope.launch {
            var finalUrl = track.streamUrl
            if (track.serverId == "pd_server" && (finalUrl.isBlank() || finalUrl.contains(".mp3"))) {
                val resolved = resolveArchiveOrgStreamUrl(track.id, ".mp3")
                if (resolved.isNotBlank()) {
                    finalUrl = resolved
                }
            }
            val resolvedTrack = track.copy(streamUrl = finalUrl)
            val resolvedPlaylist = playlist?.map { t ->
                if (t.id == track.id) resolvedTrack else t
            }
            playMusicTrack(resolvedTrack, resolvedPlaylist)
        }
    }

    suspend fun resolveArchiveOrgStreamUrl(identifier: String, extension: String): String {
        val files = ArchiveOrgClient.fetchFilesForIdentifier(identifier)
        val matchingFile = files.firstOrNull { it.name.endsWith(extension, ignoreCase = true) }?.name
        return if (matchingFile != null) {
            val encodedFile = java.net.URLEncoder.encode(matchingFile, "UTF-8")
                .replace("+", "%20")
                .replace("%2F", "/")
                .replace("%3A", ":")
            "https://archive.org/download/$identifier/$encodedFile"
        } else {
            ""
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