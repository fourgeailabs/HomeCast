package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.Audiobook
import com.example.data.MusicTrack
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.android.gms.location.LocationServices

enum class DiscoveryCategoryTab {
    ALL, AUDIOBOOKS, MUSIC, BOOKS, REGIONAL
}

enum class DiscoveryMediaType {
    AUDIOBOOK, MUSIC, BOOK
}

data class DiscoveryItem(
    val id: String,
    val title: String,
    val creator: String,
    val mediaType: DiscoveryMediaType,
    val genre: String = "",
    val coverUrl: String = "",
    val description: String = "",
    val tag: String? = null,
    val durationOrPages: String = "",
    val format: String = "DIGITAL",
    val excerpt: String = "",
    val gradient: List<Color> = listOf(AccentIndigo, AccentTeal)
)

enum class DiscoverySourceTab {
    PERSONAL, PUBLIC_DOMAIN
}

@Composable
fun DiscoveryScreen(
    viewModel: MainViewModel,
    onMediaSelected: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var location by remember { mutableStateOf<Location?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasLocationPermission = granted
        }
    )

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                    location = loc
                }
            } catch (e: SecurityException) {
                // Ignore
            }
        }
    }

    val recommendations by viewModel.recommendations.collectAsState()
    val isLoading by viewModel.isDiscoveryLoading.collectAsState()
    val error by viewModel.discoveryError.collectAsState()
    val geminiCategoryItems by viewModel.geminiCategoryItems.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val allBooks by viewModel.allBooks.collectAsState()
    val allMusic by viewModel.allMusic.collectAsState()

    var customPrompt by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(DiscoveryCategoryTab.ALL) }
    var selectedSource by remember { mutableStateOf(DiscoverySourceTab.PERSONAL) }

    // Selected E-Reader book preview state
    var selectedBookForReading by remember { mutableStateOf<DiscoveryItem?>(null) }
    var bookmarkMessage by remember { mutableStateOf<String?>(null) }

    // Select dynamic content from real servers
    val dynamicEBooks = viewModel.allEBooks.collectAsState().value.map { book ->
        DiscoveryItem(
            id = "disc_bk_${book.id}",
            title = book.title,
            creator = book.author,
            mediaType = DiscoveryMediaType.BOOK,
            genre = book.genre,
            coverUrl = book.coverUrl.ifEmpty { "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&q=80" },
            description = book.description.ifEmpty { "From your connected Booklore server" },
            tag = "📚 E-Book",
            durationOrPages = "${book.totalPages} Pages",
            format = "EPUB",
            gradient = listOf(Color(0xFF311B92), Color(0xFF7C4DFF))
        )
    }.take(10)

    val dynamicAudiobooks = allBooks.map { book ->
        DiscoveryItem(
            id = "disc_ab_${book.id}",
            title = book.title,
            creator = book.author,
            mediaType = DiscoveryMediaType.AUDIOBOOK,
            genre = "Library Audiobook",
            coverUrl = book.coverUrl.ifEmpty { "https://images.unsplash.com/photo-1589998059171-988d887df646?w=600&q=80" },
            description = "From your connected server",
            tag = "🎧 Server Sync",
            durationOrPages = "${book.duration / 60}m",
            format = "AUDIOBOOK",
            gradient = listOf(Color(0xFF0D47A1), Color(0xFF00E5FF))
        )
    }.take(10)

    val dynamicMusic = allMusic.map { track ->
        DiscoveryItem(
            id = "disc_m_${track.id}",
            title = track.title,
            creator = track.artist,
            mediaType = DiscoveryMediaType.MUSIC,
            genre = track.genre,
            coverUrl = track.coverUrl.ifEmpty { "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80" },
            description = "From your connected server",
            tag = "🎵 Track",
            durationOrPages = "${track.duration / 60000}m",
            format = "FLAC/MP3",
            gradient = listOf(Color(0xFF004D40), Color(0xFF1DE9B6))
        )
    }.take(10)

    // Free/Public Domain Content (LibriVox, Gutenberg)
    val publicDomainBooks = remember {
        listOf(
            DiscoveryItem(
                id = "disc_bk_pd_1",
                title = "Frankenstein",
                creator = "Mary Shelley",
                mediaType = DiscoveryMediaType.BOOK,
                genre = "Classic Horror",
                coverUrl = "https://images.unsplash.com/photo-1532012197267-da84d127e765?w=600&q=80",
                description = "Public domain classic from Project Gutenberg. A scientist creates life with terrifying consequences.",
                tag = "🏛️ Public Domain",
                durationOrPages = "280 Pages",
                format = "EPUB",
                excerpt = "I am by birth a Genevese, and my family is one of the most distinguished of that republic.",
                gradient = listOf(Color(0xFF212121), Color(0xFF757575))
            ),
            DiscoveryItem(
                id = "disc_bk_pd_2",
                title = "Pride and Prejudice",
                creator = "Jane Austen",
                mediaType = DiscoveryMediaType.BOOK,
                genre = "Romance Classic",
                coverUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&q=80",
                description = "Public domain classic. The romantic clash between the opinionated Elizabeth and her proud beau, Mr. Darcy.",
                tag = "🏛️ Public Domain",
                durationOrPages = "432 Pages",
                format = "EPUB",
                excerpt = "It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife.",
                gradient = listOf(Color(0xFF4E342E), Color(0xFFA1887F))
            ),
            DiscoveryItem(
                id = "disc_bk_pd_3",
                title = "The Time Machine",
                creator = "H. G. Wells",
                mediaType = DiscoveryMediaType.BOOK,
                genre = "Sci-Fi Classic",
                coverUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&q=80",
                description = "Public domain classic. A Victorian scientist travels to the year 802,701 AD.",
                tag = "🏛️ Public Domain",
                durationOrPages = "118 Pages",
                format = "EPUB",
                excerpt = "The Time Traveller (for so it will be convenient to speak of him) was expounding a recondite matter to us.",
                gradient = listOf(Color(0xFF311B92), Color(0xFF7C4DFF))
            )
        )
    }

    val publicDomainAudio = remember {
        listOf(
            DiscoveryItem(
                id = "disc_ab_pd_1",
                title = "The Art of War",
                creator = "Sun Tzu",
                mediaType = DiscoveryMediaType.AUDIOBOOK,
                genre = "Philosophy",
                coverUrl = "https://images.unsplash.com/photo-1545041793-272e50529d84?w=600&q=80",
                description = "Classic ancient Chinese military treatise.",
                tag = "🏛️ Public Domain",
                durationOrPages = "1h 12m",
                format = "AUDIOBOOK",
                gradient = listOf(Color(0xFFB71C1C), Color(0xFFFF5252))
            )
        )
    }

    val publicDomainMusic = remember {
        listOf(
            DiscoveryItem(
                id = "disc_m_pd_1",
                title = "Acoustic & Chill Discovery",
                creator = "Various Artists",
                mediaType = DiscoveryMediaType.MUSIC,
                genre = "Acoustic",
                coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80",
                description = "Public domain acoustic tracks for focus and relaxation.",
                tag = "🏛️ Public Domain",
                durationOrPages = "45m",
                format = "MP3",
                gradient = listOf(Color(0xFF004D40), Color(0xFF1DE9B6))
            )
        )
    }

    var activeCategory by remember { mutableStateOf<String?>(null) }
    
    val allDiscoverableItems = remember(dynamicAudiobooks, dynamicEBooks, dynamicMusic, publicDomainAudio, publicDomainBooks, publicDomainMusic) {
        dynamicAudiobooks + dynamicEBooks + dynamicMusic + publicDomainAudio + publicDomainBooks + publicDomainMusic
    }

    val inventorySummary = remember(allDiscoverableItems) {
        allDiscoverableItems.take(50).joinToString("; ") { "${it.title} by ${it.creator} (${it.mediaType})" }
    }


    val categoryNew = remember(allDiscoverableItems) { allDiscoverableItems.shuffled().take(12) }
    val categoryNoteworthy = remember(allDiscoverableItems) { allDiscoverableItems.shuffled().take(12) }
    val categoryPopular = remember(allDiscoverableItems) { allDiscoverableItems.shuffled().take(12) }
    val categorySagas = remember(allDiscoverableItems) { allDiscoverableItems.filter { it.genre.contains("Sci-Fi") || it.genre.contains("Comics") || it.genre.contains("Classic") }.take(12) }

    // Initial load on first render if recommendations are empty
    LaunchedEffect(Unit) {
        if (recommendations.isEmpty()) {
            viewModel.fetchDiscoveryRecommendations(
                "Recommend top trending audiobooks, standout music albums, and popular e-books for readers and listeners"
            )
        }
    }

    if (activeCategory != null) {
        val itemsToShow = geminiCategoryItems

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { activeCategory = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = activeCategory ?: "Category",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isLoading) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = AccentTeal)
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("Gemini AI is curating this category...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                items(itemsToShow, key = { it.id }) { item ->
                    DiscoveryMediaCard(
                        item = item,
                        isPlaying = playbackState.currentAudiobook?.title?.equals(item.title, ignoreCase = true) == true && playbackState.isPlaying,
                        onPrimaryClick = {
                            val localBook = allBooks.firstOrNull { it.title.equals(item.title, ignoreCase = true) }
                            val localTrack = allMusic.firstOrNull { it.title.equals(item.title, ignoreCase = true) }
                            if (localBook != null) {
                                viewModel.playAudiobook(localBook)
                                onMediaSelected()
                            } else if (localTrack != null) {
                                viewModel.playbackManager.playMusicTrack(localTrack)
                                onMediaSelected()
                            } else if (item.mediaType == DiscoveryMediaType.BOOK) {
                                selectedBookForReading = item
                            } else {
                                val demoBook = Audiobook(
                                    id = item.id,
                                    title = item.title,
                                    author = item.creator,
                                    coverUrl = item.coverUrl,
                                    duration = 36000L,
                                    serverId = "discovery_demo",
                                    streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3",
                                    narrator = "Discovery Narrator",
                                    seriesName = item.genre
                                )
                                viewModel.playAudiobook(demoBook)
                                onMediaSelected()
                            }
                        }
                    )
                }
            }
        }
        
        // E-reader overlay needs to be placed at the top level
        if (selectedBookForReading != null) {
            val book = selectedBookForReading!!
            var isViewingAsComic by remember {
                mutableStateOf(
                    book.genre.contains("Comic", ignoreCase = true) ||
                    book.genre.contains("Cyberpunk", ignoreCase = true) ||
                    book.genre.contains("Tech", ignoreCase = true)
                )
            }

            val eBookData = remember(book) {
                val generatedChapters = (1..20).map { chapterNum ->
                    BookChapter(
                        title = "Chapter $chapterNum",
                        startPage = chapterNum * 5,
                        paragraphs = listOf(
                            book.excerpt.ifBlank { "The sun hung low over the horizon, casting long, stylized shadows across the metallic landscape." },
                            "This is a substantially longer representation of a book chapter. In a fully connected Booklore environment, this content would be streamed directly from your server.",
                            "As the protagonist navigated the labyrinthine streets, the neon signs buzzed with a low electric hum. The air tasted of ozone and rain.",
                            "Far above, transport ships painted streaks of fire against the bruised purple sky. They were leaving the atmosphere, bound for colonies on Mars and beyond.",
                            "Every step felt heavier than the last, weighed down not just by gravity, but by the burden of secrets carried in the cybernetic drive implanted at the base of the skull.",
                            "To be continued in the next sequence...",
                            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
                            "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
                        )
                    )
                }
                EBookData(
                    id = book.id,
                    title = book.title,
                    author = book.creator,
                    totalChapters = generatedChapters.size,
                    chapters = generatedChapters
                )
            }

            val sampleComic = remember(book) {
                ComicData(
                    id = "comic_${book.id}",
                    title = book.title,
                    series = if (book.genre.contains("Cyberpunk", ignoreCase = true)) "Cyberpunk: Neon Horizon" else "Chronicles of the Cosmos",
                    issueNumber = "01",
                    writer = book.creator,
                    artist = "Master Illustrator",
                    coverUrl = book.coverUrl,
                    pages = listOf(
                        ComicPage(
                            pageNumber = 1,
                            pageTitle = "Prologue: High Orbit",
                            frames = listOf(
                                ComicFrame(
                                    id = "f1",
                                    frameNumber = 1,
                                    title = "Approach Vector",
                                    speaker = "Commander Vex",
                                    dialogue = "All telemetry streams are synchronized. Home server link established.",
                                    sfx = "HUMMMM...",
                                    gradientColors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                                )
                            )
                        )
                    )
                )
            }

            if (isViewingAsComic) {
                ComicReaderScreen(
                    comic = sampleComic,
                    onClose = { selectedBookForReading = null },
                    onSwitchToNovel = { isViewingAsComic = false }
                )
            } else {
                EReaderScreen(
                    eBook = eBookData,
                    onClose = { selectedBookForReading = null },
                    onSwitchToComic = { isViewingAsComic = true }
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Header Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Discover",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        "Curated categories & Gemini AI insights",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val activeType = when (selectedTab) {
                                DiscoveryCategoryTab.AUDIOBOOKS -> "audiobooks"
                                DiscoveryCategoryTab.MUSIC -> "music"
                                DiscoveryCategoryTab.BOOKS -> "books"
                                else -> null
                            }
                            viewModel.fetchDiscoveryRecommendations(
                                if (customPrompt.isNotBlank()) customPrompt else "Recommend outstanding trending audiobooks, music albums, and bestselling e-books",
                                location,
                                activeType
                            )
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceGlass)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Refresh Recommendations", tint = AccentTeal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar & AI Prompt
            OutlinedTextField(
                value = customPrompt,
                onValueChange = { customPrompt = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ask Gemini (e.g. 'Sci-fi e-books & ambient synth albums')") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = AccentTeal) },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (customPrompt.isNotBlank()) {
                                val activeType = when (selectedTab) {
                                    DiscoveryCategoryTab.AUDIOBOOKS -> "audiobooks"
                                    DiscoveryCategoryTab.MUSIC -> "music"
                                    DiscoveryCategoryTab.BOOKS -> "books"
                                    else -> null
                                }
                                viewModel.fetchDiscoveryRecommendations(customPrompt, location, activeType)
                            }
                        },
                        enabled = customPrompt.isNotBlank() && !isLoading
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Submit AI Query", tint = if (customPrompt.isNotBlank()) AccentTeal else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceGlass,
                    unfocusedContainerColor = SurfaceGlass,
                    focusedBorderColor = AccentTeal.copy(alpha = 0.8f),
                    unfocusedBorderColor = SurfaceGlassBorder
                )
            )
        }

        // Large Visual Category Clickables
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CategoryCard(
                        title = "New Releases",
                        icon = Icons.Default.NewReleases,
                        color = AccentTeal,
                        modifier = Modifier.weight(1f).aspectRatio(1.2f),
                        onClick = { 
                            activeCategory = "New Releases"
                            viewModel.fetchGeminiCategoryItems("New Releases", inventorySummary)
                        }
                    )
                    CategoryCard(
                        title = "Noteworthy",
                        icon = Icons.Default.Star,
                        color = Color(0xFFFFB300),
                        modifier = Modifier.weight(1f).aspectRatio(1.2f),
                        onClick = { 
                            activeCategory = "Noteworthy"
                            viewModel.fetchGeminiCategoryItems("Noteworthy", inventorySummary)
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CategoryCard(
                        title = "Popular",
                        icon = Icons.Default.TrendingUp,
                        color = AccentIndigo,
                        modifier = Modifier.weight(1f).aspectRatio(1.2f),
                        onClick = { 
                            activeCategory = "Popular"
                            viewModel.fetchGeminiCategoryItems("Popular", inventorySummary)
                        }
                    )
                    CategoryCard(
                        title = "Sagas & Epics",
                        icon = Icons.Default.AutoStories,
                        color = Color(0xFFE91E63),
                        modifier = Modifier.weight(1f).aspectRatio(1.2f),
                        onClick = { 
                            activeCategory = "Sagas & Epics"
                            viewModel.fetchGeminiCategoryItems("Sagas & Epics", inventorySummary)
                        }
                    )
                }
            }
        }

        // Location Permission Banner
        if (selectedTab == DiscoveryCategoryTab.REGIONAL && !hasLocationPermission) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Regional Discovery", fontWeight = FontWeight.Bold)
                            Text("Enable location to discover stories and music tailored to your region.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Enable")
                        }
                    }
                }
            }
        }

        // 3. Gemini AI Curated Output Shelf
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceGlass)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentTeal, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "Gemini AI is curating recommendations across audiobooks, music, and books...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else if (recommendations.isNotEmpty()) {
            item {
                ShelfHeader(
                    title = "AI Curated Selections",
                    subtitle = "Real-time generative intelligence for your library",
                    badge = "GEMINI AI",
                    icon = Icons.Default.AutoAwesome,
                    iconTint = AccentTeal
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(recommendations) { itemText ->
                        AiRecommendationShelfCard(
                            recommendation = itemText,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(itemText))
                            },
                            onSearchOrPlay = {
                                // Match against local audiobooks or music, or initiate demo playback
                                val matchingBook = allBooks.firstOrNull { itemText.contains(it.title, ignoreCase = true) }
                                val matchingTrack = allMusic.firstOrNull { itemText.contains(it.title, ignoreCase = true) }

                                if (matchingBook != null) {
                                    viewModel.playAudiobook(matchingBook)
                                    onMediaSelected()
                                } else if (matchingTrack != null) {
                                    viewModel.playbackManager.playMusicTrack(matchingTrack)
                                    onMediaSelected()
                                } else {
                                    // Treat as recommended book or audio preview
                                    selectedBookForReading = DiscoveryItem(
                                        id = "ai_rec_${System.currentTimeMillis()}",
                                        title = itemText.substringBefore("-").trim().ifBlank { itemText.take(30) },
                                        creator = itemText.substringAfter("-").substringBefore(":").trim().ifBlank { "Gemini Pick" },
                                        mediaType = DiscoveryMediaType.BOOK,
                                        genre = "AI Curated",
                                        description = itemText,
                                        tag = "✨ Gemini Pick",
                                        durationOrPages = "320 Pages",
                                        format = "EPUB / DIGITAL",
                                        excerpt = "Recommended by Gemini AI:\n\n$itemText\n\nThis title has been highlighted for its rich themes, exceptional writing style, and acclaim among readers and listeners.",
                                        gradient = listOf(AccentTeal, AccentIndigo)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

    }


    // -------------------------------------------------------------------------
    // KINDLE E-READER & COMIC FRAME-BY-FRAME VIEWER
    // -------------------------------------------------------------------------
    if (selectedBookForReading != null) {
        val book = selectedBookForReading!!
        var isViewingAsComic by remember {
            mutableStateOf(
                book.genre.contains("Comic", ignoreCase = true) ||
                book.genre.contains("Cyberpunk", ignoreCase = true) ||
                book.genre.contains("Tech", ignoreCase = true)
            )
        }

        val eBookData = remember(book) {
            val generatedChapters = (1..20).map { chapterNum ->
                BookChapter(
                    title = "Chapter $chapterNum",
                    startPage = chapterNum * 5,
                    paragraphs = listOf(
                        book.excerpt.ifBlank { "The sun hung low over the horizon, casting long, stylized shadows across the metallic landscape." },
                        "This is a substantially longer representation of a book chapter. In a fully connected Booklore environment, this content would be streamed directly from your server.",
                        "As the protagonist navigated the labyrinthine streets, the neon signs buzzed with a low electric hum. The air tasted of ozone and rain.",
                        "Far above, transport ships painted streaks of fire against the bruised purple sky. They were leaving the atmosphere, bound for colonies on Mars and beyond.",
                        "Every step felt heavier than the last, weighed down not just by gravity, but by the burden of secrets carried in the cybernetic drive implanted at the base of the skull.",
                        "To be continued in the next sequence...",
                        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
                        "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
                    )
                )
            }
            EBookData(
                id = book.id,
                title = book.title,
                author = book.creator,
                totalChapters = generatedChapters.size,
                chapters = generatedChapters
            )
        }

        val sampleComic = remember(book) {
            ComicData(
                id = "comic_${book.id}",
                title = book.title,
                series = if (book.genre.contains("Cyberpunk", ignoreCase = true)) "Cyberpunk: Neon Horizon" else "Chronicles of the Cosmos",
                issueNumber = "01",
                writer = book.creator,
                artist = "Master Illustrator",
                coverUrl = book.coverUrl,
                pages = listOf(
                    ComicPage(
                        pageNumber = 1,
                        pageTitle = "Prologue: High Orbit",
                        frames = listOf(
                            ComicFrame(
                                id = "f1",
                                frameNumber = 1,
                                title = "Approach Vector",
                                speaker = "Commander Vex",
                                dialogue = "All telemetry streams are synchronized. Home server link established.",
                                sfx = "HUMMMM...",
                                gradientColors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                            ),
                            ComicFrame(
                                id = "f2",
                                frameNumber = 2,
                                title = "Atmospheric Entry",
                                speaker = "AI Core",
                                dialogue = "Warning: Approaching cloud ceiling. Engaging guided visual thrusters!",
                                sfx = "BOOOM!",
                                gradientColors = listOf(Color(0xFF311042), Color(0xFF831843))
                            ),
                            ComicFrame(
                                id = "f3",
                                frameNumber = 3,
                                title = "City Lights Below",
                                speaker = "Vex",
                                dialogue = "Look down there... countless server hubs shining like galaxies.",
                                sfx = "CRACKLE",
                                gradientColors = listOf(Color(0xFF064E3B), Color(0xFF065F46))
                            ),
                            ComicFrame(
                                id = "f4",
                                frameNumber = 4,
                                title = "Safe Landing",
                                speaker = "Navigator",
                                dialogue = "Touchdown confirmed. The HomeCast terminal is operational.",
                                sfx = "CLICK-WHIRR",
                                gradientColors = listOf(Color(0xFF1E293B), Color(0xFF0284C7))
                            )
                        )
                    ),
                    ComicPage(
                        pageNumber = 2,
                        pageTitle = "Chapter 1: The Secret Vault",
                        frames = listOf(
                            ComicFrame(
                                id = "f5",
                                frameNumber = 1,
                                title = "The Ancient Data Chamber",
                                speaker = "Scholar",
                                dialogue = "This archive holds the legendary audiobooks and graphic novels of our time.",
                                sfx = "SHHHH",
                                gradientColors = listOf(Color(0xFF451A03), Color(0xFF78350F))
                            ),
                            ComicFrame(
                                id = "f6",
                                frameNumber = 2,
                                title = "Igniting the Playback Core",
                                speaker = "Vex",
                                dialogue = "Turn the page. Let the journey continue!",
                                sfx = "FLASH!",
                                gradientColors = listOf(Color(0xFF14532D), Color(0xFF166534))
                            )
                        )
                    )
                )
            )
        }

        Dialog(
            onDismissRequest = { selectedBookForReading = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            if (isViewingAsComic) {
                ComicReaderScreen(
                    comic = sampleComic,
                    onClose = { selectedBookForReading = null },
                    onSwitchToNovel = { isViewingAsComic = false }
                )
            } else {
                EReaderScreen(
                    eBook = eBookData,
                    onClose = { selectedBookForReading = null },
                    onSwitchToComic = { isViewingAsComic = true }
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// DISCOVERY SHELF CARD (Audiobooks, Music, Books)
// -----------------------------------------------------------------------------
@Composable
fun DiscoveryMediaCard(
    item: DiscoveryItem,
    isBook: Boolean = false,
    isPlaying: Boolean = false,
    onPrimaryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onPrimaryClick() }
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(if (item.mediaType == DiscoveryMediaType.MUSIC) 140.dp else 210.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceGlass)
        ) {
            if (item.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = item.coverUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(item.gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (item.mediaType) {
                            DiscoveryMediaType.AUDIOBOOK -> Icons.Default.Headphones
                            DiscoveryMediaType.MUSIC -> Icons.Default.Album
                            DiscoveryMediaType.BOOK -> Icons.Default.AutoStories
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Top Tag Overlay
            if (item.tag != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        item.tag,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        maxLines = 1
                    )
                }
            }

            // Bottom Right Action / Duration / Format
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            ) {
                if (isPlaying) {
                    Surface(
                        color = AccentTeal,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = "Playing", tint = Color.Black, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("PLAYING", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                } else if (item.durationOrPages.isNotBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            item.durationOrPages,
                            fontSize = 9.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title and Creator
        Text(
            item.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            item.creator,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// -----------------------------------------------------------------------------
// AI RECOMMENDATION SHELF CARD
// -----------------------------------------------------------------------------
@Composable
fun AiRecommendationShelfCard(
    recommendation: String,
    onCopy: () -> Unit,
    onSearchOrPlay: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        modifier = Modifier
            .width(260.dp)
            .height(130.dp)
            .clickable { onSearchOrPlay() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentTeal.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy text",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                recommendation,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Explore / Preview", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentTeal)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun CategoryCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
