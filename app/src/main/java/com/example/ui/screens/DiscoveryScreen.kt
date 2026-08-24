package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder

val AccentCoral = Color(0xFFFF6B6B)

enum class DiscoveryMediaType {
    BOOK, AUDIOBOOK, MUSIC, COMIC
}

data class DiscoveryItem(
    val title: String,
    val creator: String,
    val genre: String,
    val description: String,
    val mediaType: DiscoveryMediaType,
    val coverUrl: String,
    val streamOrReadUrl: String = "",
    val durationSeconds: Long = 0L,
    val totalPages: Int = 150
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    viewModel: MainViewModel,
    onNavigateToDetails: (String, String, String) -> Unit = { _, _, _ -> },
    onOpenEBook: (EBookData) -> Unit = {},
    onOpenComic: (ComicData) -> Unit = {},
    onAudiobookClick: () -> Unit = {},
    onMusicClick: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSource by remember { mutableIntStateOf(0) } // 0 = Personal, 1 = Public Domain
    var mediaTypeFilter by remember { mutableStateOf("ALL") } // ALL, AUDIOBOOK, BOOK, MUSIC, COMIC
    
    val isLoading by viewModel.isDiscoveryLoading.collectAsState()
    val geminiCategoryItems by viewModel.geminiCategoryItems.collectAsState()
    val conciergeResult by viewModel.conciergeResult.collectAsState()

    val allBooks by viewModel.allBooks.collectAsState()
    val allEBooks by viewModel.allEBooks.collectAsState()
    val allMusic by viewModel.allMusic.collectAsState()
    val recentMusic by viewModel.recentMusic.collectAsState()
    val recents by viewModel.recents.collectAsState()

    val dynamicMixes = remember(allMusic, recentMusic) {
        MusicMixGenerator.generateDynamicMixes(allMusic, recentMusic, 0)
    }

    val sampleMoods = remember {
        listOf(
            MusicMoodsCatalog.allMoods[0], // Chillout Lounge
            MusicMoodsCatalog.allMoods[1], // Lo-Fi Study Beats
            MusicMoodsCatalog.allMoods[5], // Deep Focus
            MusicMoodsCatalog.allMoods[10], // High Voltage
            MusicMoodsCatalog.allMoods[20], // Cyberpunk Noir
            MusicMoodsCatalog.allMoods[30], // 80s Synth Highway
            MusicMoodsCatalog.allMoods[40], // Epic Cinematic
            MusicMoodsCatalog.allMoods[50], // Coffeehouse Acoustic
            MusicMoodsCatalog.allMoods[70], // Cosmic Stargazing
            MusicMoodsCatalog.allMoods[80], // Neo-Classical Piano
            MusicMoodsCatalog.allMoods[90]  // Sunset Drive
        )
    }

    val privatePrompts = listOf(
        "🎧 Cyberpunk Coding",
        "☕ Rainy Day Lo-Fi",
        "⚡ High Voltage Workout",
        "🌌 Late Night Synthwave",
        "🧘 Deep Meditation",
        "📚 Epic Fantasy Blend",
        "🌙 Bedtime Audiobooks",
        "🚗 80s Road Trip"
    )

    val publicDomainPrompts = listOf(
        "🏛️ Ancient Stoic Philosophy",
        "🕵️ Sherlock Holmes Mysteries",
        "🚀 Golden Age Sci-Fi",
        "🎻 Baroque & Classical Focus",
        "📻 Old Time Radio Thrillers",
        "🧛 Gothic Horror Masterpieces",
        "📚 Victorian Literature",
        "🎷 1920s Speakeasy Jazz"
    )

    val currentPrompts = if (selectedSource == 0) privatePrompts else publicDomainPrompts

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Explore,
                    contentDescription = null,
                    tint = AccentTeal,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "Discover & Blends",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        if (selectedSource == 0) "AI Mixes, Audiobooks, E-Books & 100+ Moods" else "Classic Literature, LibriVox, Comics & Archives",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = "Preferences", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Source Switcher (Private Library vs Public Domain)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedSource == 0) AccentIndigo else Color.Transparent)
                    .clickable { 
                        selectedSource = 0
                        mediaTypeFilter = "ALL"
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Private Library",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedSource == 1) AccentTeal else Color.Transparent)
                    .clickable { 
                        selectedSource = 1
                        mediaTypeFilter = "ALL"
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Public Domain",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (selectedSource == 1) Color.Black else Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Media Type Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filterOptions = if (selectedSource == 0) {
                listOf(
                    "ALL" to "All Media",
                    "AUDIOBOOK" to "🎧 Audiobooks",
                    "BOOK" to "📖 E-Books",
                    "MUSIC" to "🎵 Music & Mixes"
                )
            } else {
                listOf(
                    "ALL" to "All Archives",
                    "BOOK" to "📖 Classic Books",
                    "AUDIOBOOK" to "🎧 Dramatic Audio",
                    "COMIC" to "🎨 Golden Comics",
                    "MUSIC" to "🎵 Orchestral Music"
                )
            }

            filterOptions.forEach { (type, label) ->
                val isSelected = mediaTypeFilter == type
                FilterChip(
                    selected = isSelected,
                    onClick = { mediaTypeFilter = type },
                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (selectedSource == 0) AccentIndigo else AccentTeal,
                        selectedLabelColor = Color.White,
                        containerColor = SurfaceGlass
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = SurfaceGlassBorder,
                        selectedBorderColor = Color.Transparent,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // AI Search & Prompt Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { 
                Text(
                    if (selectedSource == 0) "Ask AI to curate any vibe or blend from your library..." else "Ask AI to search classic literature, audiobooks & archives...",
                    fontSize = 13.sp
                ) 
            },
            leadingIcon = { 
                Icon(
                    Icons.Default.AutoAwesome, 
                    contentDescription = "AI", 
                    tint = if (selectedSource == 0) AccentIndigo else AccentTeal 
                ) 
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = {
                        val sourceStr = if (selectedSource == 0) "the user's private library" else "public domain archives"
                        viewModel.fetchGeminiCategoryItems(searchQuery, sourceStr)
                        viewModel.runMediaConcierge(searchQuery)
                    }) {
                        Icon(Icons.Default.Send, contentDescription = "Generate", tint = if (selectedSource == 0) AccentIndigo else AccentTeal)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceGlass,
                unfocusedContainerColor = SurfaceGlass,
                focusedBorderColor = if (selectedSource == 0) AccentIndigo.copy(alpha = 0.8f) else AccentTeal.copy(alpha = 0.8f),
                unfocusedBorderColor = SurfaceGlassBorder
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = if (selectedSource == 0) AccentIndigo else AccentTeal)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "AI is curating your personalized collection...", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else if (geminiCategoryItems.isNotEmpty()) {
            // Display AI Concierge Narrative Summary if available
            conciergeResult?.explanation?.let { conciergeText ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentIndigo.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentIndigo.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "Concierge AI",
                            tint = AccentTeal,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "AI Media Concierge Insight",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AccentTeal
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                conciergeText,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // Display AI-Generated Results Grid with direct action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("AI Recommendations (${geminiCategoryItems.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                TextButton(onClick = { 
                    searchQuery = ""
                    viewModel.fetchGeminiCategoryItems("", "")
                    viewModel.clearMediaConcierge()
                }) {
                    Text("Clear Results", color = if (selectedSource == 0) AccentIndigo else AccentTeal, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(geminiCategoryItems) { item ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(16.dp))
                    ) {
                        Column {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                AsyncImage(
                                    model = item.coverUrl,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1.2f)
                                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                )
                                Surface(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            when (item.mediaType) {
                                                DiscoveryMediaType.AUDIOBOOK -> Icons.Default.Headphones
                                                DiscoveryMediaType.MUSIC -> Icons.Default.MusicNote
                                                DiscoveryMediaType.COMIC -> Icons.Default.AutoStories
                                                else -> Icons.Default.MenuBook
                                            },
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            item.mediaType.name,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    item.title, 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 13.sp, 
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
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    item.description,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 13.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        item.genre, 
                                        fontSize = 10.sp, 
                                        color = if (selectedSource == 0) AccentIndigo else AccentTeal, 
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = {
                                            when (item.mediaType) {
                                                DiscoveryMediaType.AUDIOBOOK -> {
                                                    val book = Audiobook(
                                                        id = "disc_ab_${item.title.hashCode()}",
                                                        title = item.title,
                                                        author = item.creator,
                                                        coverUrl = item.coverUrl,
                                                        duration = item.durationSeconds.takeIf { it > 0 } ?: 3600L,
                                                        serverId = "pd_server",
                                                        streamUrl = item.streamOrReadUrl
                                                    )
                                                    viewModel.playbackManager.playAudiobook(book)
                                                    onAudiobookClick()
                                                }
                                                DiscoveryMediaType.MUSIC -> {
                                                    val track = MusicTrack(
                                                        id = "disc_mus_${item.title.hashCode()}",
                                                        title = item.title,
                                                        artist = item.creator,
                                                        album = "AI Discovery Mix",
                                                        coverUrl = item.coverUrl,
                                                        duration = item.durationSeconds.takeIf { it > 0 } ?: 240000L,
                                                        serverId = "pd_server",
                                                        streamUrl = item.streamOrReadUrl
                                                    )
                                                    viewModel.playMusicTrackWithResolution(track)
                                                    onMusicClick()
                                                }
                                                DiscoveryMediaType.BOOK -> {
                                                    val ebook = EBookData(
                                                        id = "disc_eb_${item.title.hashCode()}",
                                                        title = item.title,
                                                        author = item.creator,
                                                        downloadUrl = item.streamOrReadUrl,
                                                        publicDomainUrl = item.streamOrReadUrl
                                                    )
                                                    onOpenEBook(ebook)
                                                }
                                                DiscoveryMediaType.COMIC -> {
                                                    val comic = ComicData(
                                                        id = "disc_com_${item.title.hashCode()}",
                                                        title = item.title,
                                                        writer = item.creator,
                                                        artist = item.creator,
                                                        coverUrl = item.coverUrl,
                                                        downloadUrl = item.streamOrReadUrl,
                                                        pageCount = item.totalPages
                                                    )
                                                    onOpenComic(comic)
                                                }
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selectedSource == 0) AccentIndigo else AccentTeal
                                        ),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(
                                            when (item.mediaType) {
                                                DiscoveryMediaType.BOOK -> "Read"
                                                DiscoveryMediaType.COMIC -> "Comic"
                                                DiscoveryMediaType.AUDIOBOOK -> "Listen"
                                                DiscoveryMediaType.MUSIC -> "Play"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Rich Discovery Feed (Differentiated for Private Library vs Public Domain)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                // 1. Suggested AI Prompt Chips
                item {
                    Column {
                        Text(
                            "AI Prompt Inspirations", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 13.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(currentPrompts) { prompt ->
                                Surface(
                                    modifier = Modifier.clickable {
                                        searchQuery = prompt
                                        val sourceStr = if (selectedSource == 0) "the user's private library" else "public domain archives"
                                        viewModel.fetchGeminiCategoryItems(prompt, sourceStr)
                                        viewModel.runMediaConcierge(prompt)
                                    },
                                    color = SurfaceGlass,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceGlassBorder)
                                ) {
                                    Text(
                                        prompt,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedSource == 0) {
                    // ==========================================
                    // --- PRIVATE LIBRARY DISCOVERY CONTENT ---
                    // ==========================================

                    // Hero "For You" / Daily Curated Blend Spotlight Card
                    item {
                        val forYouMix = dynamicMixes.firstOrNull { it.id == "mix_for_you" } 
                            ?: dynamicMixes.firstOrNull()
                        val heroGradient = forYouMix?.gradientColors ?: listOf(AccentIndigo, AccentTeal)
                        val trackCount = forYouMix?.tracks?.size ?: allMusic.size

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(10.dp, RoundedCornerShape(22.dp))
                                .clip(RoundedCornerShape(22.dp))
                                .background(Brush.linearGradient(heroGradient))
                                .clickable {
                                    if (forYouMix != null && forYouMix.tracks.isNotEmpty()) {
                                        viewModel.playMusicTrackWithResolution(forYouMix.tracks.first(), forYouMix.tracks)
                                        onMusicClick()
                                    } else if (allMusic.isNotEmpty()) {
                                        viewModel.playMusicTrackWithResolution(allMusic.first(), allMusic)
                                        onMusicClick()
                                    }
                                }
                                .padding(18.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            "MADE FOR YOU",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Play Blend", tint = Color.Black, modifier = Modifier.size(24.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    forYouMix?.title ?: "Personal Media Blend",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = Color.White
                                )

                                Text(
                                    forYouMix?.subtitle ?: "Your personalized dynamic blend from your connected servers and audio history.",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "$trackCount tracks • ${allBooks.size} Audiobooks • ${allEBooks.size} E-Books",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    // Shelf 1: "Continue Your Journey" (In-Progress Audiobooks & Recent Tracks)
                    val continueListening = recents.take(6)
                    if (continueListening.isNotEmpty() && (mediaTypeFilter == "ALL" || mediaTypeFilter == "AUDIOBOOK")) {
                        item {
                            Column {
                                ShelfHeader(title = "Continue Your Journey", subtitle = "Pick up where you left off")
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    items(continueListening) { book ->
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                            modifier = Modifier
                                                .width(160.dp)
                                                .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(16.dp))
                                                .clickable {
                                                    viewModel.playbackManager.playAudiobook(book)
                                                    onAudiobookClick()
                                                }
                                        ) {
                                            Column {
                                                AsyncImage(
                                                    model = book.coverUrl,
                                                    contentDescription = book.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(130.dp)
                                                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                                )
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(book.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(book.author, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Resume", fontSize = 11.sp, color = AccentTeal, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Shelf 2: Dynamic Time-of-Day & Style Mixes Carousel
                    if (dynamicMixes.size > 1 && (mediaTypeFilter == "ALL" || mediaTypeFilter == "MUSIC")) {
                        item {
                            Column {
                                ShelfHeader(title = "AI Dynamic Mixes", subtitle = "Mixes that evolve throughout the day")
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    items(dynamicMixes.drop(1)) { mix ->
                                        Box(
                                            modifier = Modifier
                                                .width(170.dp)
                                                .height(130.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(Brush.linearGradient(mix.gradientColors))
                                                .clickable {
                                                    val first = mix.tracks.firstOrNull()
                                                    if (first != null) {
                                                        viewModel.playMusicTrackWithResolution(first, mix.tracks)
                                                        onMusicClick()
                                                    }
                                                }
                                                .padding(12.dp)
                                        ) {
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                Text(
                                                    mix.category.uppercase(),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White.copy(alpha = 0.8f)
                                                )
                                                Spacer(modifier = Modifier.weight(1f))
                                                Text(
                                                    mix.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    mix.subtitle,
                                                    fontSize = 10.sp,
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    "${mix.tracks.size} songs",
                                                    fontSize = 9.sp,
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Shelf 3: Featured Audiobooks
                    val featuredAudiobooks = allBooks.take(10)
                    if (featuredAudiobooks.isNotEmpty() && (mediaTypeFilter == "ALL" || mediaTypeFilter == "AUDIOBOOK")) {
                        item {
                            Column {
                                ShelfHeader(title = "Featured Audiobooks", subtitle = "From your synced audiobook collection")
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    items(featuredAudiobooks) { book ->
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                            modifier = Modifier
                                                .width(140.dp)
                                                .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(16.dp))
                                                .clickable {
                                                    onNavigateToDetails(book.title, book.author, "AUDIOBOOK")
                                                }
                                        ) {
                                            Column {
                                                AsyncImage(
                                                    model = book.coverUrl,
                                                    contentDescription = book.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(130.dp)
                                                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                                )
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text(book.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(book.author, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.playbackManager.playAudiobook(book)
                                                            onAudiobookClick()
                                                        },
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .align(Alignment.End)
                                                    ) {
                                                        Icon(Icons.Default.PlayCircleFilled, contentDescription = "Play", tint = AccentTeal)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Shelf 4: Top E-Books & Graphic Reads
                    val featuredEBooks = allEBooks.take(10)
                    if (featuredEBooks.isNotEmpty() && (mediaTypeFilter == "ALL" || mediaTypeFilter == "BOOK")) {
                        item {
                            Column {
                                ShelfHeader(title = "Top Digital E-Books", subtitle = "From your synced reading library")
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    items(featuredEBooks) { ebook ->
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                            modifier = Modifier
                                                .width(140.dp)
                                                .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(16.dp))
                                                .clickable {
                                                    val ebookData = EBookData(
                                                        id = ebook.id,
                                                        title = ebook.title,
                                                        author = ebook.author,
                                                        downloadUrl = ebook.downloadUrl,
                                                        publicDomainUrl = ebook.downloadUrl
                                                    )
                                                    onOpenEBook(ebookData)
                                                }
                                        ) {
                                            Column {
                                                AsyncImage(
                                                    model = ebook.coverUrl,
                                                    contentDescription = ebook.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(130.dp)
                                                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                                )
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text(ebook.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(ebook.author, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("${ebook.totalPages}p", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("Read", fontSize = 10.sp, color = AccentIndigo, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Shelf 5: 100+ Moods & Vibe Explorer (Interactive from 100-mood catalog)
                    if (mediaTypeFilter == "ALL" || mediaTypeFilter == "MUSIC") {
                        item {
                            Column {
                                ShelfHeader(title = "Explore 100+ Moods & Vibes", subtitle = "Soundscapes & vibes matched to your state of mind")
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(sampleMoods) { mood ->
                                        Box(
                                            modifier = Modifier
                                                .width(150.dp)
                                                .height(100.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Brush.linearGradient(mood.gradient))
                                                .clickable {
                                                    val matching = MusicMoodsCatalog.filterTracksForMood(mood, allMusic)
                                                    val first = matching.firstOrNull()
                                                    if (first != null) {
                                                        viewModel.playMusicTrackWithResolution(first, matching)
                                                        onMusicClick()
                                                    }
                                                }
                                                .padding(12.dp)
                                        ) {
                                            Column(modifier = Modifier.align(Alignment.TopStart)) {
                                                Text(
                                                    mood.name,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp,
                                                    color = Color.White,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    mood.group,
                                                    fontSize = 10.sp,
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    maxLines = 1
                                                )
                                            }

                                            Icon(
                                                mood.icon,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.4f),
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .align(Alignment.BottomEnd)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Fallback / Server Setup Card if Library is completely empty
                    if (allBooks.isEmpty() && allEBooks.isEmpty() && allMusic.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(20.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Connect Your Media Servers", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Sync with Audiobookshelf, Booklore, or Plex to stream your complete library, or switch to the Public Domain tab to browse thousands of free classic masterworks.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = onNavigateToSettings,
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Connect Server", fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = { selectedSource = 1 },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Explore Public Domain", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                } else {
                    // ==========================================
                    // --- PUBLIC DOMAIN DISCOVERY CONTENT ---
                    // ==========================================

                    // Hero Showcase: Timeless Masterpiece Card (H.G. Wells / Beethoven / Conan Doyle)
                    item {
                        val heroClassic = PublicDomainCatalog.curatedAudiobooks.firstOrNull() 
                            ?: PublicDomainCatalog.curatedEBooks.first()
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(10.dp, RoundedCornerShape(22.dp))
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(AccentTeal.copy(alpha = 0.85f), Color(0xFF1E3A8A))
                                    )
                                )
                                .clickable {
                                    val book = Audiobook(
                                        id = heroClassic.id,
                                        title = heroClassic.title,
                                        author = heroClassic.authorOrCreator,
                                        coverUrl = heroClassic.coverUrl,
                                        duration = heroClassic.durationSeconds,
                                        serverId = "pd_server",
                                        streamUrl = heroClassic.streamOrReadUrl
                                    )
                                    viewModel.playbackManager.playAudiobook(book)
                                    onAudiobookClick()
                                }
                                .padding(18.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            "FEATURED MASTERPIECE",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Headphones, contentDescription = "Listen", tint = Color.Black, modifier = Modifier.size(22.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    heroClassic.title,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 19.sp,
                                    color = Color.White
                                )

                                Text(
                                    "by ${heroClassic.authorOrCreator} • Full LibriVox Dramatic Recording",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    heroClassic.description,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // Shelf 1: "Immortal Classic E-Books" (Frankenstein, Gatsby, Dracula, Dorian Gray, etc.)
                    if (mediaTypeFilter == "ALL" || mediaTypeFilter == "BOOK") {
                        item {
                            Column {
                                ShelfHeader(title = "Immortal Classic E-Books", subtitle = "Project Gutenberg & Smithsonian timeless editions")
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    items(PublicDomainCatalog.curatedEBooks) { item ->
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                            modifier = Modifier
                                                .width(145.dp)
                                                .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(16.dp))
                                                .clickable {
                                                    val ebookData = EBookData(
                                                        id = item.id,
                                                        title = item.title,
                                                        author = item.authorOrCreator,
                                                        downloadUrl = item.streamOrReadUrl,
                                                        publicDomainUrl = item.streamOrReadUrl
                                                    )
                                                    onOpenEBook(ebookData)
                                                }
                                        ) {
                                            Column {
                                                AsyncImage(
                                                    model = item.coverUrl,
                                                    contentDescription = item.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(140.dp)
                                                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                                )
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(item.authorOrCreator, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(item.genre, fontSize = 9.sp, color = AccentTeal, fontWeight = FontWeight.Bold)
                                                        Surface(
                                                            color = AccentTeal.copy(alpha = 0.2f),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                "Read",
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = AccentTeal
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Shelf 2: "Dramatic Audiobooks & LibriVox Classics"
                    if (mediaTypeFilter == "ALL" || mediaTypeFilter == "AUDIOBOOK") {
                        item {
                            Column {
                                ShelfHeader(title = "Dramatic Audiobooks", subtitle = "LibriVox full unabridged voice performances")
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    items(PublicDomainCatalog.curatedAudiobooks) { item ->
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                            modifier = Modifier
                                                .width(155.dp)
                                                .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(16.dp))
                                                .clickable {
                                                    val book = Audiobook(
                                                        id = item.id,
                                                        title = item.title,
                                                        author = item.authorOrCreator,
                                                        coverUrl = item.coverUrl,
                                                        duration = item.durationSeconds,
                                                        serverId = "pd_server",
                                                        streamUrl = item.streamOrReadUrl
                                                    )
                                                    viewModel.playbackManager.playAudiobook(book)
                                                    onAudiobookClick()
                                                }
                                        ) {
                                            Column {
                                                AsyncImage(
                                                    model = item.coverUrl,
                                                    contentDescription = item.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(130.dp)
                                                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                                )
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(item.authorOrCreator, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("${item.durationSeconds / 60}m", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Icon(Icons.Default.PlayCircleFilled, contentDescription = "Play", tint = AccentTeal, modifier = Modifier.size(20.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Shelf 3: "Golden Age Comics & Illustrated Tales"
                    if (mediaTypeFilter == "ALL" || mediaTypeFilter == "COMIC") {
                        item {
                            Column {
                                ShelfHeader(title = "Golden Age Comics", subtitle = "Vintage illustrated stories from Internet Archive")
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    items(PublicDomainCatalog.curatedComics) { comic ->
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                            modifier = Modifier
                                                .width(150.dp)
                                                .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(16.dp))
                                                .clickable {
                                                    val comicData = ComicData(
                                                        id = comic.id,
                                                        title = comic.title,
                                                        writer = comic.authorOrCreator,
                                                        artist = comic.authorOrCreator,
                                                        coverUrl = comic.coverUrl,
                                                        downloadUrl = comic.streamOrReadUrl,
                                                        pageCount = comic.totalPages
                                                    )
                                                    onOpenComic(comicData)
                                                }
                                        ) {
                                            Column {
                                                AsyncImage(
                                                    model = comic.coverUrl,
                                                    contentDescription = comic.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(140.dp)
                                                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                                )
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(comic.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(comic.authorOrCreator, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("${comic.totalPages} pages", fontSize = 9.sp, color = AccentCoral)
                                                        Surface(
                                                            color = AccentCoral.copy(alpha = 0.2f),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                "Comic",
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = AccentCoral
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Shelf 4: "Masterpiece Classical & Archive Recordings"
                    if (mediaTypeFilter == "ALL" || mediaTypeFilter == "MUSIC") {
                        item {
                            Column {
                                ShelfHeader(title = "Masterpiece Classical & Archive Recordings", subtitle = "Beethoven, Debussy, Chopin, Scott Joplin, Mozart & Bach")
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    items(PublicDomainCatalog.curatedMusic) { trackItem ->
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                            modifier = Modifier
                                                .width(160.dp)
                                                .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(16.dp))
                                                .clickable {
                                                    val track = MusicTrack(
                                                        id = trackItem.id,
                                                        title = trackItem.title,
                                                        artist = trackItem.authorOrCreator,
                                                        album = "Public Domain Masterpieces",
                                                        coverUrl = trackItem.coverUrl,
                                                        duration = trackItem.durationSeconds * 1000L,
                                                        serverId = "pd_server",
                                                        streamUrl = trackItem.streamOrReadUrl
                                                    )
                                                    viewModel.playMusicTrackWithResolution(track)
                                                    onMusicClick()
                                                }
                                        ) {
                                            Column {
                                                AsyncImage(
                                                    model = trackItem.coverUrl,
                                                    contentDescription = trackItem.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(120.dp)
                                                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                                )
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(trackItem.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(trackItem.authorOrCreator, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("${trackItem.durationSeconds / 60}m", fontSize = 10.sp, color = AccentTeal)
                                                        Icon(Icons.Default.PlayCircleFilled, contentDescription = "Play", tint = AccentTeal, modifier = Modifier.size(20.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Shelf 5: Curated Historical Theme Clusters
                    item {
                        Column {
                            ShelfHeader(title = "Curated Historical Collections", subtitle = "Explore by era, philosophy, and theme")
                            Spacer(modifier = Modifier.height(10.dp))
                            val themes = listOf(
                                Triple("🚀 Sci-Fi Pioneers", "Wells, Verne, Shelley & early speculative imagination", listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))),
                                Triple("🕵️ Victorian Mystery", "Sherlock Holmes, Edgar Allan Poe & detective logic", listOf(Color(0xFF232526), Color(0xFF414345))),
                                Triple("🏛️ Ancient Philosophy", "Marcus Aurelius, Sun Tzu, Epictetus & Machiavelli", listOf(Color(0xFF4A00E0), Color(0xFF8E2DE2))),
                                Triple("🎷 Roaring 20s Jazz", "Scott Joplin ragtime, 78rpm blues & Charleston rhythm", listOf(Color(0xFFFF512F), Color(0xFFDD2476))),
                                Triple("🌊 High Seas Adventure", "Moby Dick, Treasure Island & Call of the Wild", listOf(Color(0xFF136a8a), Color(0xFF267871)))
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                themes.forEach { (themeTitle, themeDesc, themeColors) ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Brush.linearGradient(themeColors))
                                            .clickable {
                                                searchQuery = themeTitle
                                                viewModel.fetchGeminiCategoryItems(themeTitle, "public domain archives")
                                            }
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(themeTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                                Text(themeDesc, fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShelfHeader(title: String, subtitle: String) {
    Column {
        Text(
            title, 
            fontWeight = FontWeight.Bold, 
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            subtitle, 
            fontSize = 11.sp, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
