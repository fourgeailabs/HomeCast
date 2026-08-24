package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.Audiobook
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder

data class PodcastChannel(
    val id: String,
    val title: String,
    val publisher: String,
    val coverUrl: String,
    val description: String,
    val category: String = "General",
    val feedUrl: String = "",
    val serverId: String = "local",
    val isPublic: Boolean = false,
    val episodes: List<PodcastEpisode> = emptyList()
)

data class PodcastEpisode(
    val id: String,
    val title: String,
    val podcastTitle: String,
    val publisher: String,
    val durationSeconds: Long = 1800L,
    val audioUrl: String,
    val coverUrl: String,
    val publishDate: String = "Recent",
    val description: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastsScreen(
    viewModel: MainViewModel,
    onEpisodeClick: (PodcastEpisode) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    var selectedSection by remember { mutableIntStateOf(0) } // 0: Personal, 1: Public Directory
    var personalFilter by remember { mutableIntStateOf(0) } // 0: All Personal, 1: Local Device, 2: Personal Server
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val servers by viewModel.servers.collectAsState()

    // Curated Public Podcast Directories requested by user
    val publicDirectories = remember {
        listOf(
            PodcastChannel(
                id = "pub_playpodcast",
                title = "PlayPodcast.net Directory",
                publisher = "PlayPodcast Network",
                coverUrl = "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=600&q=80",
                description = "Discover trending independent podcasts, audio series, and daily news.",
                category = "Directory",
                feedUrl = "https://www.playpodcast.net/",
                isPublic = true,
                episodes = listOf(
                    PodcastEpisode("ep_play1", "The Tech Tomorrow Show #142", "PlayPodcast.net Directory", "PlayPodcast Network", 2400L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=600&q=80", "Today", "Daily breakthroughs in AI and consumer hardware."),
                    PodcastEpisode("ep_play2", "Mindset & Performance Digest", "PlayPodcast.net Directory", "PlayPodcast Network", 1800L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=600&q=80", "Yesterday", "Cognitive psychology for modern creators.")
                )
            ),
            PodcastChannel(
                id = "pub_rss_community",
                title = "RSS.com Community Showcase",
                publisher = "RSS.com Podcasting",
                coverUrl = "https://images.unsplash.com/photo-1478737270239-2f02b77fc618?w=600&q=80",
                description = "Global creator community featuring stories, culture, and indie broadcasts.",
                category = "Community",
                feedUrl = "https://rss.com/community/",
                isPublic = true,
                episodes = listOf(
                    PodcastEpisode("ep_rss1", "Indie Creator Stories Vol. 8", "RSS.com Community Showcase", "RSS.com Podcasting", 2100L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1478737270239-2f02b77fc618?w=600&q=80", "3 days ago", "How independent podcasters built active listener communities."),
                    PodcastEpisode("ep_rss2", "Acoustic & Ambient Soundscapes", "RSS.com Community Showcase", "RSS.com Podcasting", 3600L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1478737270239-2f02b77fc618?w=600&q=80", "5 days ago", "Immersive audio journeys recorded around the globe.")
                )
            ),
            PodcastChannel(
                id = "pub_getpodcast",
                title = "GetPodcast Global Charts",
                publisher = "GetPodcast Platform",
                coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80",
                description = "Top charting global audio programs, investigative journalism, and science.",
                category = "Global Charts",
                feedUrl = "https://getpodcast.com/",
                isPublic = true,
                episodes = listOf(
                    PodcastEpisode("ep_get1", "The Science of Deep Focus", "GetPodcast Global Charts", "GetPodcast Platform", 2800L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80", "This Week", "Neuroscience-backed tools for sustained attention."),
                    PodcastEpisode("ep_get2", "Cosmic Wonders & Astrophysics", "GetPodcast Global Charts", "GetPodcast Platform", 3200L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80", "Last Week", "Exploring deep space telemetry and exoplanets.")
                )
            )
        )
    }

    // Dynamic Personal Podcast Feeds
    val personalChannels = remember(servers) {
        listOf(
            PodcastChannel(
                id = "pod_tech",
                title = "Silicon Valley Tech Daily",
                publisher = "FourgeAI Labs Audio",
                coverUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&q=80",
                description = "Your morning brief on software engineering, cloud computing, and AI developments.",
                category = "Technology",
                serverId = "local",
                episodes = listOf(
                    PodcastEpisode("pod_ep1", "Ep. 88: Generative Models & On-Device AI", "Silicon Valley Tech Daily", "FourgeAI Labs Audio", 2100L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&q=80", "Today", "An in-depth look at Android neural acceleration.")
                )
            ),
            PodcastChannel(
                id = "pod_history",
                title = "Echoes of History",
                publisher = "Archive Cultural Media",
                coverUrl = "https://images.unsplash.com/photo-1461360370896-922624d12aa1?w=600&q=80",
                description = "Uncovering the pivotal moments, thinkers, and movements that shaped human civilization.",
                category = "History",
                serverId = if (servers.isNotEmpty()) servers.first().id else "local",
                episodes = listOf(
                    PodcastEpisode("pod_ep2", "Chapter 12: The Renaissance Builders", "Echoes of History", "Archive Cultural Media", 3400L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1461360370896-922624d12aa1?w=600&q=80", "2 days ago", "How architectural revolutions sparked artistic enlightenment.")
                )
            )
        )
    }

    // Filter personal feeds by switch
    val filteredPersonalChannels = remember(personalChannels, personalFilter, searchQuery) {
        personalChannels.filter { channel ->
            val matchesFilter = when (personalFilter) {
                1 -> channel.serverId == "local" || channel.serverId.isBlank()
                2 -> channel.serverId != "local" && channel.serverId.isNotBlank()
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() || channel.title.contains(searchQuery, ignoreCase = true) || channel.description.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }.distinctBy { "${it.title.lowercase().trim()}___${it.publisher.lowercase().trim()}" }
    }

    val displayChannels = if (selectedSection == 0) filteredPersonalChannels else publicDirectories

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceGlass)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Podcasts, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Podcasts", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("Audio series, feeds & RSS broadcasts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Podcast Server Settings", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search podcasts & episodes...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AccentTeal) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                        focusedBorderColor = AccentIndigo,
                        unfocusedBorderColor = SurfaceGlassBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Primary Segmented Tab Switch: Personal vs Public
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
                            .background(if (selectedSection == 0) AccentIndigo else Color.Transparent)
                            .clickable { selectedSection = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Personal Podcasts", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedSection == 1) AccentTeal else Color.Transparent)
                            .clickable { selectedSection = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Public Feeds", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (selectedSection == 1) Color.Black else Color.White)
                    }
                }

                // If Personal Section, display Local vs Personal Server filter switch
                if (selectedSection == 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filterLabels = listOf("All Personal", "Local Device", "Personal Server")
                        filterLabels.forEachIndexed { idx, label ->
                            val isSelected = personalFilter == idx
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) AccentIndigo.copy(alpha = 0.25f) else Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AccentIndigo else SurfaceGlassBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { personalFilter = idx }
                            ) {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) AccentIndigo else Color.Gray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                // AI Podcast Mix Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(AccentIndigo, AccentTeal)))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(color = Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(6.dp)) {
                                Text("AI CURATED BLEND", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Daily Podcast Briefing", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.White)
                            Text("Fresh episodes curated from your subscribed feeds & directories", fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                        }

                        IconButton(
                            onClick = {
                                val firstEp = displayChannels.flatMap { it.episodes }.firstOrNull()
                                if (firstEp != null) {
                                    val ab = Audiobook(
                                        id = firstEp.id,
                                        title = firstEp.title,
                                        author = firstEp.publisher,
                                        coverUrl = firstEp.coverUrl,
                                        duration = firstEp.durationSeconds,
                                        serverId = "podcast",
                                        streamUrl = firstEp.audioUrl
                                    )
                                    viewModel.playAudiobookWithResolution(ab)
                                    onEpisodeClick(firstEp)
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play Mix", tint = Color.Black, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }

            // Podcast Channels List
            items(displayChannels, key = { it.id }) { channel ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(channel.coverUrl).crossfade(true).build(),
                                contentDescription = channel.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(channel.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(channel.publisher, fontSize = 12.sp, color = AccentTeal, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(channel.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
                            }
                        }

                        if (channel.episodes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = SurfaceGlassBorder)
                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Recent Episodes", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))

                            channel.episodes.forEach { ep ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val ab = Audiobook(
                                                id = ep.id,
                                                title = ep.title,
                                                author = ep.publisher,
                                                coverUrl = ep.coverUrl,
                                                duration = ep.durationSeconds,
                                                serverId = channel.serverId,
                                                streamUrl = ep.audioUrl
                                            )
                                            viewModel.playAudiobookWithResolution(ab)
                                            onEpisodeClick(ep)
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ep.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${ep.publishDate} • ${ep.durationSeconds / 60} mins", fontSize = 10.sp, color = Color.Gray)
                                    }

                                    IconButton(
                                        onClick = {
                                            val ab = Audiobook(
                                                id = ep.id,
                                                title = ep.title,
                                                author = ep.publisher,
                                                coverUrl = ep.coverUrl,
                                                duration = ep.durationSeconds,
                                                serverId = channel.serverId,
                                                streamUrl = ep.audioUrl
                                            )
                                            viewModel.playAudiobookWithResolution(ab)
                                            onEpisodeClick(ep)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.PlayCircleFilled, contentDescription = "Play", tint = AccentIndigo, modifier = Modifier.size(26.dp))
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
