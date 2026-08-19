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

    val trendingAudiobooks = if (selectedSource == DiscoverySourceTab.PERSONAL) dynamicAudiobooks else publicDomainAudio
    val sciFiSagas = publicDomainBooks
    val featuredAlbums = if (selectedSource == DiscoverySourceTab.PERSONAL) dynamicMusic else publicDomainMusic
    val acousticChillMusic = publicDomainMusic
    val bestsellingEBooks = publicDomainBooks

    // Initial load on first render if recommendations are empty
    LaunchedEffect(Unit) {
        if (recommendations.isEmpty()) {
            viewModel.fetchDiscoveryRecommendations(
                "Recommend top trending audiobooks, standout music albums, and popular e-books for readers and listeners"
            )
        }
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
                        "AI Discovery Hub",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        "Audiobooks, Music & E-Books curated with Gemini AI",
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

        // 2. Source Selection (Personal vs Public Domain)
        item {
            TabRow(
                selectedTabIndex = selectedSource.ordinal,
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    if (selectedSource.ordinal < tabPositions.size) {
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSource.ordinal]),
                            color = AccentTeal
                        )
                    }
                },
                divider = { Divider(color = SurfaceGlassBorder) }
            ) {
                Tab(
                    selected = selectedSource == DiscoverySourceTab.PERSONAL,
                    onClick = { selectedSource = DiscoverySourceTab.PERSONAL },
                    text = { Text("Personal Collection", fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = AccentTeal,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Tab(
                    selected = selectedSource == DiscoverySourceTab.PUBLIC_DOMAIN,
                    onClick = { selectedSource = DiscoverySourceTab.PUBLIC_DOMAIN },
                    text = { Text("Public Domain Library", fontWeight = FontWeight.SemiBold) },
                    selectedContentColor = AccentTeal,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 3. Category Filter Chips: All, Audiobooks, Music, Books (E-Reader), Regional
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(DiscoveryCategoryTab.values()) { tab ->
                    val isSelected = selectedTab == tab
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedTab = tab
                            when (tab) {
                                DiscoveryCategoryTab.ALL -> {
                                    viewModel.fetchDiscoveryRecommendations("Recommend exceptional audiobooks, music albums, and books", location)
                                }
                                DiscoveryCategoryTab.AUDIOBOOKS -> {
                                    viewModel.fetchDiscoveryRecommendations("Recommend top trending audiobooks with great narrators", location, "audiobooks")
                                }
                                DiscoveryCategoryTab.MUSIC -> {
                                    viewModel.fetchDiscoveryRecommendations("Recommend standout music albums across various genres", location, "music")
                                }
                                DiscoveryCategoryTab.BOOKS -> {
                                    viewModel.fetchDiscoveryRecommendations("Recommend captivating e-books, novels, and literature for readers", location, "books")
                                }
                                DiscoveryCategoryTab.REGIONAL -> {
                                    if (!hasLocationPermission) {
                                        launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    } else {
                                        viewModel.fetchDiscoveryRecommendations("Recommend audiobooks, music albums, and books inspired by regional culture and geography", location)
                                    }
                                }
                            }
                        },
                        label = {
                            Text(
                                when (tab) {
                                    DiscoveryCategoryTab.ALL -> "All Media"
                                    DiscoveryCategoryTab.AUDIOBOOKS -> "Audiobooks"
                                    DiscoveryCategoryTab.MUSIC -> "Music"
                                    DiscoveryCategoryTab.BOOKS -> "Books (E-Reader)"
                                    DiscoveryCategoryTab.REGIONAL -> "Nearby Culture"
                                },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                when (tab) {
                                    DiscoveryCategoryTab.ALL -> Icons.Default.TravelExplore
                                    DiscoveryCategoryTab.AUDIOBOOKS -> Icons.Default.Headphones
                                    DiscoveryCategoryTab.MUSIC -> Icons.Default.MusicNote
                                    DiscoveryCategoryTab.BOOKS -> Icons.Default.AutoStories
                                    DiscoveryCategoryTab.REGIONAL -> Icons.Default.LocationOn
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentTeal.copy(alpha = 0.25f),
                            selectedLabelColor = AccentTeal,
                            selectedLeadingIconColor = AccentTeal
                        ),
                        shape = RoundedCornerShape(12.dp)
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
                                    viewModel.playMusicTrack(matchingTrack)
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

        // 4. VERTICAL SHELF SECTIONS (Audiobooks, Music, Books)

        // --- AUDIOBOOKS SHELVES ---
        if (selectedTab == DiscoveryCategoryTab.ALL || selectedTab == DiscoveryCategoryTab.AUDIOBOOKS) {
            // Shelf: Trending Audiobooks
            item {
                ShelfHeader(
                    title = "Trending Audiobooks",
                    subtitle = "Slide sideways for top-rated narratives & epics",
                    badge = "AUDIOBOOK",
                    icon = Icons.Default.Headphones,
                    iconTint = AccentTeal
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(trendingAudiobooks, key = { it.id }) { item ->
                        DiscoveryMediaCard(
                            item = item,
                            isPlaying = playbackState.currentAudiobook?.title?.equals(item.title, ignoreCase = true) == true && playbackState.isPlaying,
                            onPrimaryClick = {
                                val local = allBooks.firstOrNull { it.title.equals(item.title, ignoreCase = true) }
                                if (local != null) {
                                    viewModel.playAudiobook(local)
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
                                }
                                onMediaSelected()
                            }
                        )
                    }
                }
            }

            // Shelf: Sci-Fi & Speculative Sagas
            item {
                ShelfHeader(
                    title = "Sci-Fi & Cyberpunk Sagas",
                    subtitle = "Deep universes, distant galaxies & tech futures",
                    icon = Icons.Default.RocketLaunch,
                    iconTint = Color(0xFF80D8FF)
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(sciFiSagas, key = { it.id }) { item ->
                        DiscoveryMediaCard(
                            item = item,
                            isPlaying = playbackState.currentAudiobook?.title?.equals(item.title, ignoreCase = true) == true && playbackState.isPlaying,
                            onPrimaryClick = {
                                val demoBook = Audiobook(
                                    id = item.id,
                                    title = item.title,
                                    author = item.creator,
                                    coverUrl = item.coverUrl,
                                    duration = 38000L,
                                    serverId = "discovery_demo",
                                    streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3",
                                    narrator = "Full Cast Audio",
                                    seriesName = item.genre
                                )
                                viewModel.playAudiobook(demoBook)
                                onMediaSelected()
                            }
                        )
                    }
                }
            }
        }

        // --- MUSIC SHELVES ---
        if (selectedTab == DiscoveryCategoryTab.ALL || selectedTab == DiscoveryCategoryTab.MUSIC) {
            // Shelf: Featured Music Albums
            item {
                ShelfHeader(
                    title = "Featured Music Albums",
                    subtitle = "Slide sideways to discover iconic soundscapes",
                    badge = "MUSIC",
                    icon = Icons.Default.Album,
                    iconTint = AccentIndigo
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(featuredAlbums, key = { it.id }) { item ->
                        DiscoveryMediaCard(
                            item = item,
                            isPlaying = playbackState.currentMusicTrack?.album?.equals(item.title, ignoreCase = true) == true && playbackState.isPlaying,
                            onPrimaryClick = {
                                val local = allMusic.firstOrNull { it.album.equals(item.title, ignoreCase = true) }
                                if (local != null) {
                                    viewModel.playMusicTrack(local)
                                } else {
                                    val demoTrack = MusicTrack(
                                        id = item.id,
                                        title = item.title,
                                        artist = item.creator,
                                        album = item.title,
                                        coverUrl = item.coverUrl,
                                        duration = 240000L,
                                        serverId = "discovery_music",
                                        streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverwritten_Role_Playing_Game.mp3",
                                        genre = item.genre,
                                        trackNumber = 1
                                    )
                                    viewModel.playMusicTrack(demoTrack)
                                }
                                onMediaSelected()
                            }
                        )
                    }
                }
            }

            // Shelf: Acoustic & Lo-Fi Chill
            item {
                ShelfHeader(
                    title = "Acoustic & Lo-Fi Chill",
                    subtitle = "Mellow frequencies for relaxing, reading, and deep focus",
                    icon = Icons.Default.Spa,
                    iconTint = Color(0xFF4DB6AC)
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(acousticChillMusic, key = { it.id }) { item ->
                        DiscoveryMediaCard(
                            item = item,
                            isPlaying = playbackState.currentMusicTrack?.title?.equals(item.title, ignoreCase = true) == true && playbackState.isPlaying,
                            onPrimaryClick = {
                                val demoTrack = MusicTrack(
                                    id = item.id,
                                    title = item.title,
                                    artist = item.creator,
                                    album = "Acoustic & Chill Discovery",
                                    coverUrl = item.coverUrl,
                                    duration = 190000L,
                                    serverId = "discovery_music",
                                    streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverwritten_Role_Playing_Game.mp3",
                                    genre = item.genre,
                                    trackNumber = 1
                                )
                                viewModel.playMusicTrack(demoTrack)
                                onMediaSelected()
                            }
                        )
                    }
                }
            }
        }

        // --- BOOKS (E-READER READY) SHELVES ---
        if (selectedTab == DiscoveryCategoryTab.ALL || selectedTab == DiscoveryCategoryTab.BOOKS) {
            // Shelf: Bestselling E-Books & Novels
            item {
                ShelfHeader(
                    title = "Bestselling E-Books",
                    subtitle = "Tap any book to open the E-Reader preview & chapter excerpt",
                    badge = "E-READER",
                    icon = Icons.Default.AutoStories,
                    iconTint = Color(0xFFFFB300)
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(bestsellingEBooks, key = { it.id }) { bookItem ->
                        DiscoveryMediaCard(
                            item = bookItem,
                            isBook = true,
                            onPrimaryClick = {
                                selectedBookForReading = bookItem
                            }
                        )
                    }
                }
            }

            // Shelf: E-Reader Showcase & Literature
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedBookForReading = bestsellingEBooks.first() }
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(listOf(AccentIndigo, AccentTeal))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("E-Reader Aspect Hub", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Color(0xFFFFB300).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "EPUB / PDF",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB300)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Preview typography, customizable reading canvas, and excerpts ahead of full reader sync.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = AccentTeal)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
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
            EBookData(
                id = book.id,
                title = book.title,
                author = book.creator,
                totalChapters = 4,
                chapters = listOf(
                    BookChapter(
                        title = "Chapter 1: The Beginning",
                        startPage = 1,
                        paragraphs = listOf(
                            book.excerpt.ifBlank { "It was a bright cold day in April, and the clocks were striking thirteen. The air smelled of vintage paper and quiet serenity." },
                            "Every passage was rendered with crystalline clarity upon the display. The gentle curve of the pages responded to each gesture, turning with fluid grace.",
                            "In that quiet sanctuary, the reader flicked a thumb against the right edge of the screen, advancing effortlessly into the next scene.",
                            "With personalized typography, custom margins, and warm sepia tones, the digital words felt as intimate as a bound heirloom volume."
                        )
                    ),
                    BookChapter(
                        title = "Chapter 2: The Archive",
                        startPage = 2,
                        paragraphs = listOf(
                            "The library had stood for ages, guarding stories from every era.",
                            "Audiobooks, music albums, and literature rested in complete harmony within the home server vault, accessible anywhere without restriction.",
                            "As darkness fell, the true black OLED canvas offered deep contrast and comfort for long nocturnal reading sessions."
                        )
                    ),
                    BookChapter(
                        title = "Chapter 3: Odyssey",
                        startPage = 3,
                        paragraphs = listOf(
                            "Every story is an expedition across stars and minds.",
                            "With interactive bookmarks, reading speed estimates, and chapter navigation, the entire catalog came alive at the touch of a finger."
                        )
                    )
                )
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
