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
import com.example.data.PublicDomainPodcastsCatalog
import com.example.data.network.PodcastClient
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()

    var selectedSection by remember { mutableIntStateOf(1) } // 0: Personal, 1: Public Catalog & Search
    var personalFilter by remember { mutableIntStateOf(0) } // 0: All Personal, 1: Local Device, 2: Personal Server
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val servers by viewModel.servers.collectAsState()

    var publicChannels by remember { mutableStateOf(PublicDomainPodcastsCatalog.curatedPodcasts) }
    var isLoadingPublic by remember { mutableStateOf(false) }
    var expandedChannelId by remember { mutableStateOf<String?>(null) }
    var channelEpisodesMap by remember { mutableStateOf<Map<String, List<PodcastEpisode>>>(emptyMap()) }
    var loadingEpisodesChannelId by remember { mutableStateOf<String?>(null) }

    // Subscribed / Saved personal channels
    var subscribedChannelIds by remember { mutableStateOf(setOf<String>()) }

    // Live search & category filter for public feeds
    LaunchedEffect(searchQuery, selectedCategory, selectedSection) {
        if (selectedSection == 1) {
            isLoadingPublic = true
            val results = PodcastClient.searchPodcasts(searchQuery, selectedCategory)
            publicChannels = results
            isLoadingPublic = false
        }
    }

    // Dynamic Personal Podcast Feeds
    val basePersonalChannels = remember(servers, subscribedChannelIds) {
        val defaultPersonal = listOf(
            PodcastChannel(
                id = "pod_tech",
                title = "Silicon Valley Tech Daily",
                publisher = "FourgeAI Labs Audio",
                coverUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&q=80",
                description = "Your morning brief on software engineering, cloud computing, and AI developments.",
                category = "Science & Tech",
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
                category = "History & Culture",
                serverId = if (servers.isNotEmpty()) servers.first().id else "local",
                episodes = listOf(
                    PodcastEpisode("pod_ep2", "Chapter 12: The Renaissance Builders", "Echoes of History", "Archive Cultural Media", 3400L, "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3", "https://images.unsplash.com/photo-1461360370896-922624d12aa1?w=600&q=80", "2 days ago", "How architectural revolutions sparked artistic enlightenment.")
                )
            )
        )
        val addedFromPublic = PublicDomainPodcastsCatalog.curatedPodcasts.filter { subscribedChannelIds.contains(it.id) }
        defaultPersonal + addedFromPublic
    }

    // Filter personal feeds by switch
    val filteredPersonalChannels = remember(basePersonalChannels, personalFilter, searchQuery) {
        basePersonalChannels.filter { channel ->
            val matchesFilter = when (personalFilter) {
                1 -> channel.serverId == "local" || channel.serverId.isBlank()
                2 -> channel.serverId != "local" && channel.serverId.isNotBlank()
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() || channel.title.contains(searchQuery, ignoreCase = true) || channel.description.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }.distinctBy { "${it.title.lowercase().trim()}___${it.publisher.lowercase().trim()}" }
    }

    val displayChannels = if (selectedSection == 0) filteredPersonalChannels else publicChannels

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

                // If Public Directory, display Category Filter Chips
                if (selectedSection == 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(PublicDomainPodcastsCatalog.categories) { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentTeal,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color.Black.copy(alpha = 0.25f),
                                    labelColor = Color.White
                                )
                            )
                        }
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
                            Text("Fresh episodes curated from your subscribed feeds & global directories", fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
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

            if (isLoadingPublic) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentTeal)
                    }
                }
            }

            // Podcast Channels List
            items(displayChannels, key = { it.id }) { channel ->
                val isExpanded = expandedChannelId == channel.id
                val isSubscribed = subscribedChannelIds.contains(channel.id)
                val episodesList = channelEpisodesMap[channel.id] ?: channel.episodes

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newExpanded = if (isExpanded) null else channel.id
                                    expandedChannelId = newExpanded
                                    if (newExpanded != null && !channelEpisodesMap.containsKey(channel.id)) {
                                        scope.launch {
                                            loadingEpisodesChannelId = channel.id
                                            val fetched = PodcastClient.fetchChannelEpisodes(channel)
                                            channelEpisodesMap = channelEpisodesMap + (channel.id to fetched)
                                            loadingEpisodesChannelId = null
                                        }
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(channel.coverUrl).crossfade(true).build(),
                                contentDescription = channel.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(channel.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    Surface(color = AccentIndigo.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                        Text(channel.category, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AccentTeal, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                Text(channel.publisher, fontSize = 12.sp, color = AccentTeal, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(channel.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = if (isExpanded) 4 else 2, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
                            }

                            IconButton(onClick = {
                                subscribedChannelIds = if (isSubscribed) subscribedChannelIds - channel.id else subscribedChannelIds + channel.id
                            }) {
                                Icon(
                                    imageVector = if (isSubscribed) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Subscribe",
                                    tint = if (isSubscribed) AccentTeal else Color.Gray
                                )
                            }
                        }

                        if (loadingEpisodesChannelId == channel.id) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentTeal, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fetching full broadcast episodes...", fontSize = 11.sp, color = AccentTeal)
                            }
                        } else if (episodesList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = SurfaceGlassBorder)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Episodes (${episodesList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (isExpanded) "Show Less" else "Expand All", fontSize = 11.sp, color = AccentTeal, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                                    expandedChannelId = if (isExpanded) null else channel.id
                                })
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            val showCount = if (isExpanded) episodesList.size else 2
                            episodesList.take(showCount).forEach { ep ->
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
