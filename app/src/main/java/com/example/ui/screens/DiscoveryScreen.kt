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

    // Selected E-Reader book preview state
    var selectedBookForReading by remember { mutableStateOf<DiscoveryItem?>(null) }
    var bookmarkMessage by remember { mutableStateOf<String?>(null) }

    // Curated Static Shelves Data
    val trendingAudiobooks = remember {
        listOf(
            DiscoveryItem(
                id = "disc_ab_1",
                title = "Project Hail Mary",
                creator = "Andy Weir",
                mediaType = DiscoveryMediaType.AUDIOBOOK,
                genre = "Sci-Fi & Space",
                coverUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&q=80",
                description = "A lone astronaut must save the earth from an extinction-level catastrophe with the help of an unexpected alien ally.",
                tag = "⭐ 4.9 Top Narrated",
                durationOrPages = "16h 10m",
                format = "AUDIOBOOK",
                gradient = listOf(Color(0xFF0D47A1), Color(0xFF00E5FF))
            ),
            DiscoveryItem(
                id = "disc_ab_2",
                title = "Atomic Habits",
                creator = "James Clear",
                mediaType = DiscoveryMediaType.AUDIOBOOK,
                genre = "Mindset & Growth",
                coverUrl = "https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=600&q=80",
                description = "Tiny changes yield remarkable results. An actionable guide to breaking bad habits and building extraordinary systems.",
                tag = "🔥 #1 Bestseller",
                durationOrPages = "5h 35m",
                format = "AUDIOBOOK",
                gradient = listOf(Color(0xFFE65100), Color(0xFFFFB74D))
            ),
            DiscoveryItem(
                id = "disc_ab_3",
                title = "Dune",
                creator = "Frank Herbert",
                mediaType = DiscoveryMediaType.AUDIOBOOK,
                genre = "Epic Saga",
                coverUrl = "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=600&q=80",
                description = "Set on the desert planet Arrakis, Dune is the story of Paul Atreides and the battle for the universe's most vital resource: spice.",
                tag = "✨ Masterpiece",
                durationOrPages = "21h 02m",
                format = "AUDIOBOOK",
                gradient = listOf(Color(0xFFBF360C), Color(0xFFFF8A65))
            ),
            DiscoveryItem(
                id = "disc_ab_4",
                title = "The Silent Patient",
                creator = "Alex Michaelides",
                mediaType = DiscoveryMediaType.AUDIOBOOK,
                genre = "Psychological Thriller",
                coverUrl = "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=600&q=80",
                description = "Alicia Berenson’s life is seemingly perfect until she shoots her husband five times in the face and never speaks another word.",
                tag = "🔪 Suspense Pick",
                durationOrPages = "8h 43m",
                format = "AUDIOBOOK",
                gradient = listOf(Color(0xFF263238), Color(0xFF78909C))
            ),
            DiscoveryItem(
                id = "disc_ab_5",
                title = "Deep Work",
                creator = "Cal Newport",
                mediaType = DiscoveryMediaType.AUDIOBOOK,
                genre = "Productivity",
                coverUrl = "https://images.unsplash.com/photo-1517842645767-c639042777db?w=600&q=80",
                description = "Rules for focused success in a distracted world. Master difficult information and produce better results in less time.",
                tag = "💡 Focus Guide",
                durationOrPages = "7h 44m",
                format = "AUDIOBOOK",
                gradient = listOf(Color(0xFF1B5E20), Color(0xFF66BB6A))
            )
        )
    }

    val sciFiSagas = remember {
        listOf(
            DiscoveryItem(
                id = "disc_ab_sf1",
                title = "Hyperion",
                creator = "Dan Simmons",
                mediaType = DiscoveryMediaType.AUDIOBOOK,
                genre = "Space Opera",
                coverUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&q=80",
                description = "On the world of Hyperion, beyond the reach of galactic law, waits a creature called the Shrike. Seven pilgrims embark on a final voyage.",
                tag = "🚀 Hugo Award",
                durationOrPages = "20h 45m",
                format = "AUDIOBOOK",
                gradient = listOf(Color(0xFF4A148C), Color(0xFFAB47BC))
            ),
            DiscoveryItem(
                id = "disc_ab_sf2",
                title = "Neuromancer",
                creator = "William Gibson",
                mediaType = DiscoveryMediaType.AUDIOBOOK,
                genre = "Cyberpunk",
                coverUrl = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=600&q=80",
                description = "Case was the sharpest data-thief in the matrix until the wrong people caught him. The definitive cyberpunk classic.",
                tag = "⚡ Cyber Classic",
                durationOrPages = "10h 15m",
                format = "AUDIOBOOK",
                gradient = listOf(Color(0xFF006064), Color(0xFF00E5FF))
            ),
            DiscoveryItem(
                id = "disc_ab_sf3",
                title = "The Three-Body Problem",
                creator = "Cixin Liu",
                mediaType = DiscoveryMediaType.AUDIOBOOK,
                genre = "Hard Sci-Fi",
                coverUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=600&q=80",
                description = "Set against the backdrop of China's Cultural Revolution, a secret military project sends signals into space to establish contact with aliens.",
                tag = "🌌 Epic Trilogy",
                durationOrPages = "13h 26m",
                format = "AUDIOBOOK",
                gradient = listOf(Color(0xFF004D40), Color(0xFF26A69A))
            )
        )
    }

    val featuredAlbums = remember {
        listOf(
            DiscoveryItem(
                id = "disc_mu_1",
                title = "Random Access Memories",
                creator = "Daft Punk",
                mediaType = DiscoveryMediaType.MUSIC,
                genre = "Electronic & Funk",
                coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&q=80",
                description = "A sonic journey through vintage modular synthesizers, live disco session musicians, and timeless groove anthems.",
                tag = "🏆 Album of the Year",
                durationOrPages = "13 Tracks",
                format = "LOSSLESS",
                gradient = listOf(Color(0xFFE65100), Color(0xFFFFD54F))
            ),
            DiscoveryItem(
                id = "disc_mu_2",
                title = "In Rainbows",
                creator = "Radiohead",
                mediaType = DiscoveryMediaType.MUSIC,
                genre = "Art Rock & Ambient",
                coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80",
                description = "Warm analog textures, intricate rhythmic tapestries, and haunting melodic poetry recorded in an English country manor.",
                tag = "✨ Iconic Sound",
                durationOrPages = "10 Tracks",
                format = "LOSSLESS",
                gradient = listOf(Color(0xFFB71C1C), Color(0xFFFF8A80))
            ),
            DiscoveryItem(
                id = "disc_mu_3",
                title = "Interstellar Soundtrack",
                creator = "Hans Zimmer",
                mediaType = DiscoveryMediaType.MUSIC,
                genre = "Cinematic Score",
                coverUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&q=80",
                description = "Magnificent pipe organ arrangements and cosmic synthesizer sweeps celebrating courage and dimensions beyond time.",
                tag = "🎹 Master Orchestral",
                durationOrPages = "16 Tracks",
                format = "ORIGINAL SCORE",
                gradient = listOf(Color(0xFF1A237E), Color(0xFF5C6BC0))
            ),
            DiscoveryItem(
                id = "disc_mu_4",
                title = "Midnight City Synthwaves",
                creator = "M83 & Retro Waves",
                mediaType = DiscoveryMediaType.MUSIC,
                genre = "Synthwave & Dream Pop",
                coverUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&q=80",
                description = "Nostalgic neon synth leads, driving basslines, and dreamy reverberations inspired by late-night coastal highways.",
                tag = "🌙 Late Night Vibes",
                durationOrPages = "14 Tracks",
                format = "HI-RES",
                gradient = listOf(Color(0xFF4A148C), Color(0xFFEA80FC))
            )
        )
    }

    val acousticChillMusic = remember {
        listOf(
            DiscoveryItem(
                id = "disc_mu_ch1",
                title = "Sunday Morning Acoustic",
                creator = "Various Acoustic Artists",
                mediaType = DiscoveryMediaType.MUSIC,
                genre = "Acoustic & Folk",
                coverUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=600&q=80",
                description = "Gentle fingerpicked guitars, mellow cello harmonies, and soothing coffee-shop melodies for relaxed reading and coding.",
                tag = "☕ Coffee & Chill",
                durationOrPages = "18 Tracks",
                format = "ALBUM",
                gradient = listOf(Color(0xFF3E2723), Color(0xFF8D6E63))
            ),
            DiscoveryItem(
                id = "disc_mu_ch2",
                title = "Rainy Window Lo-Fi Beats",
                creator = "ChilledCow Labs",
                mediaType = DiscoveryMediaType.MUSIC,
                genre = "Lo-Fi Hip Hop",
                coverUrl = "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=600&q=80",
                description = "Tape-saturated electric piano chords, vinyl crackle, and steady downtempo beats crafted for deep focus.",
                tag = "🎧 Focus Loop",
                durationOrPages = "22 Tracks",
                format = "EP",
                gradient = listOf(Color(0xFF263238), Color(0xFF80CBC4))
            ),
            DiscoveryItem(
                id = "disc_mu_ch3",
                title = "Quiet Piano Nocturnes",
                creator = "Ludovico Einaudi & Olafur Arnalds",
                mediaType = DiscoveryMediaType.MUSIC,
                genre = "Modern Classical",
                coverUrl = "https://images.unsplash.com/photo-1520523839898-5071270409fb?w=600&q=80",
                description = "Delicate upright piano with felt dampers, minimal strings, and contemplative ambient room tone.",
                tag = "🌿 Mindful Sound",
                durationOrPages = "12 Tracks",
                format = "HI-RES",
                gradient = listOf(Color(0xFF004D40), Color(0xFF80CBC4))
            )
        )
    }

    // Curated Books for the E-Reader Aspect!
    val bestsellingEBooks = remember {
        listOf(
            DiscoveryItem(
                id = "disc_bk_1",
                title = "Tomorrow, and Tomorrow, and Tomorrow",
                creator = "Gabrielle Zevin",
                mediaType = DiscoveryMediaType.BOOK,
                genre = "Literary Fiction",
                coverUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&q=80",
                description = "A dazzling and intricately imagined novel about two friends, often in love but never lovers, who become creative partners in video game design.",
                tag = "📖 E-Reader Ready",
                durationOrPages = "416 Pages",
                format = "EPUB",
                excerpt = "On a freezing December day, Sam Masur exits a subway car onto the crowded platform of Cambridge's Harvard Square and sees, through the surging throng of commuters, Sadie Green. He calls her name, but she does not hear. Sadie is leaning against a pillar, gazing down at a portable gaming system, her dark hair tucked behind a wool beanie.\n\nWhen they finally meet, years of unsaid memories rush forward. Game design was their language, a world where rebirth and replay were always one coin away.",
                gradient = listOf(Color(0xFF00695C), Color(0xFF4DB6AC))
            ),
            DiscoveryItem(
                id = "disc_bk_2",
                title = "Klara and the Sun",
                creator = "Kazuo Ishiguro",
                mediaType = DiscoveryMediaType.BOOK,
                genre = "Speculative Fiction",
                coverUrl = "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&q=80",
                description = "From the Nobel Prize winner comes a thrilling book that asks: What does it mean to love, through the eyes of an unforgettable Artificial Friend.",
                tag = "✨ Nobel Laureate",
                durationOrPages = "320 Pages",
                format = "EPUB",
                excerpt = "When we were new, Rosa and I were mid-store, on the side of the magazine table, and could see through more than half of the front window. So we saw the outside very well—the office workers hurrying by, the taxis, the beggars, and the glorious nourishment of the Sun streaming across the pavement.\n\nI watched the people closely, noting how their eyes held secret shadows even when their mouths smiled.",
                gradient = listOf(Color(0xFFF57F17), Color(0xFFFFEE58))
            ),
            DiscoveryItem(
                id = "disc_bk_3",
                title = "1984",
                creator = "George Orwell",
                mediaType = DiscoveryMediaType.BOOK,
                genre = "Dystopian Classic",
                coverUrl = "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=600&q=80",
                description = "The definitive dystopian prophecy of surveillance, censorship, doublespeak, and the struggle of human independence against Big Brother.",
                tag = "🏛️ Timeless Classic",
                durationOrPages = "328 Pages",
                format = "EPUB / PDF",
                excerpt = "It was a bright cold day in April, and the clocks were striking thirteen. Winston Smith, his chin nuzzled into his breast in an effort to escape the vile wind, slipped quickly through the glass doors of Victory Mansions, though not quickly enough to prevent a swirl of gritty dust from entering along with him.\n\nThe hallway smelt of boiled cabbage and old rag mats. At one end of it a coloured poster, too large for indoor display, had been tacked to the wall.",
                gradient = listOf(Color(0xFF212121), Color(0xFF757575))
            ),
            DiscoveryItem(
                id = "disc_bk_4",
                title = "Snow Crash",
                creator = "Neal Stephenson",
                mediaType = DiscoveryMediaType.BOOK,
                genre = "Cyberpunk & Tech",
                coverUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&q=80",
                description = "In reality, Hiro Protagonist delivers pizza for Uncle Enzo's CosoNostra Pizza Inc. In the Metaverse, he's a warrior prince investigating a dangerous digital virus.",
                tag = "⚡ Metaverse Origin",
                durationOrPages = "480 Pages",
                format = "EPUB",
                excerpt = "The Deliverator belongs to an elite order, a hallowed subculture. In the old days, they had the Pony Express and the samurai. Now they have the Deliverator.\n\nThe Deliverator's car has enough potential energy packed into its batteries to fire a pound of bacon into the Asteroid Belt. He never delivers late.",
                gradient = listOf(Color(0xFF311B92), Color(0xFF7C4DFF))
            ),
            DiscoveryItem(
                id = "disc_bk_5",
                title = "Meditations",
                creator = "Marcus Aurelius",
                mediaType = DiscoveryMediaType.BOOK,
                genre = "Philosophy & Stoicism",
                coverUrl = "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=600&q=80",
                description = "Private reflections and timeless stoic wisdom from the Roman Emperor on resilience, clarity of mind, virtue, and purpose.",
                tag = "📜 Stoic Wisdom",
                durationOrPages = "256 Pages",
                format = "EPUB / PDF",
                excerpt = "When you arise in the morning, think of what a precious privilege it is to be alive—to breathe, to think, to enjoy, to love.\n\nSay to yourself in the early morning: I shall meet today ungrateful, violent, treacherous, envious, uncharitable men. All of the ignorance of real good and ill.",
                gradient = listOf(Color(0xFF4E342E), Color(0xFFA1887F))
            )
        )
    }

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

        // 2. Category Filter Chips: All, Audiobooks, Music, Books (E-Reader), Regional
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
