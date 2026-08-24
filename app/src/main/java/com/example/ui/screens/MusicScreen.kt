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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import java.util.Locale

enum class MusicNavTab {
    SHELVES, MOODS, GENRES, ARTISTS, ALBUMS, SONGS
}

data class GenreItem(
    val name: String,
    val imageUrl: String = "",
    val gradient: List<Color>,
    val icon: ImageVector,
    val description: String
)

data class AlbumGroup(
    val title: String,
    val artist: String,
    val coverUrl: String,
    val genre: String,
    val tracks: List<MusicTrack>
)

data class ArtistGroup(
    val name: String,
    val coverUrl: String,
    val albums: List<AlbumGroup>,
    val tracks: List<MusicTrack>
)

data class CategoryViewData(
    val title: String,
    val subtitle: String,
    val description: String = "",
    val gradient: List<Color> = listOf(AccentIndigo, AccentTeal),
    val icon: ImageVector? = null,
    val coverUrl: String = "",
    val tracks: List<MusicTrack>
)

@Composable
fun MusicScreen(
    viewModel: MainViewModel,
    onTrackClick: (MusicTrack) -> Unit,
    onNavigateToSettings: () -> Unit,
    onArtistClick: ((String) -> Unit)? = null
) {
    val allMusic by viewModel.allMusic.collectAsState()
    val recentMusic by viewModel.recentMusic.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val isSyncing by viewModel.isSyncingPersonalMedia.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val personalTabLabel = remember(servers) {
        val types = servers.map { it.type.lowercase() }
        when {
            types.any { it.contains("plex") } && types.any { it.contains("jellyfin") } -> "Plex / Jellyfin"
            types.any { it.contains("plex") } -> "Plex"
            types.any { it.contains("jellyfin") } -> "Jellyfin"
            types.any { it.contains("audiobookshelf") } -> "Audiobookshelf"
            else -> "Plex / Jellyfin"
        }
    }
    var selectedTab by remember { mutableStateOf(MusicNavTab.SHELVES) }
    var selectedSource by remember { mutableIntStateOf(viewModel.initialMusicSource) } // 0 = Personal, 1 = Public Domain

    val archiveMusic by viewModel.publicDomainMusic.collectAsState()

    val publicDomainMusic = remember(archiveMusic) {
        archiveMusic.map { doc ->
            val coverUrl = "https://archive.org/services/img/${doc.identifier}"
            val title = doc.title ?: "Unknown Track"
            val artist = when (doc.creator) {
                is List<*> -> (doc.creator as List<*>).firstOrNull()?.toString() ?: "Various Artists"
                is String -> doc.creator
                else -> "Various Artists"
            }
            val desc = when (doc.description) {
                is List<*> -> doc.description.firstOrNull()?.toString() ?: ""
                is String -> doc.description
                else -> ""
            }.lowercase()

            val assignedGenre = when {
                desc.contains("jazz") || desc.contains("blues") || artist.lowercase().contains("jazz") -> "Jazz & Blues"
                desc.contains("classical") || desc.contains("piano") || desc.contains("orchestra") || artist.lowercase().contains("classical") -> "Classical & Soundtracks"
                desc.contains("electronic") || desc.contains("ambient") || desc.contains("synth") -> "Electronic & Dance"
                desc.contains("rock") || desc.contains("metal") || desc.contains("guitar") -> "Rock & Alternative"
                desc.contains("hip hop") || desc.contains("rap") || desc.contains("r&b") -> "Hip Hop & R&B"
                else -> "Jazz & Blues"
            }

            MusicTrack(
                id = doc.identifier,
                title = title,
                artist = artist,
                album = "Archive.org Classics",
                coverUrl = coverUrl.takeIf { doc.identifier.isNotBlank() } ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80",
                duration = 180000L,
                serverId = "pd_server",
                streamUrl = "",
                genre = assignedGenre,
                trackNumber = 1
            )
        }
    }

    val currentMusic = remember(allMusic, publicDomainMusic, selectedSource, servers) {
        if (selectedSource == 0) {
            val serverOrLocal = allMusic.filter { it.serverId != "demo_server" && it.serverId != "pd_server" }
            if (serverOrLocal.isNotEmpty() || servers.isNotEmpty()) {
                serverOrLocal
            } else {
                allMusic.filter { it.serverId != "pd_server" }
            }
        } else {
            val localPD = allMusic.filter { it.serverId == "demo_server" || it.serverId == "pd_server" }
            val fetched = publicDomainMusic.filter { f -> localPD.none { l -> l.title.equals(f.title, ignoreCase = true) } }
            localPD + fetched
        }
    }

    // Dynamic AI category shuffle state
    var shuffleSeed by remember { mutableIntStateOf(0) }
    val dynamicMixes = remember(currentMusic, recentMusic, shuffleSeed) {
        MusicMixGenerator.generateDynamicMixes(currentMusic, recentMusic, shuffleSeed)
    }

    // Navigation Stack for File Structure & Category Drilldown
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var selectedArtist by remember { mutableStateOf<ArtistGroup?>(null) }
    var selectedAlbum by remember { mutableStateOf<AlbumGroup?>(null) }
    var selectedMood by remember { mutableStateOf<MoodItem?>(null) }
    var selectedMix by remember { mutableStateOf<MusicMix?>(null) }
    var selectedCategoryView by remember { mutableStateOf<CategoryViewData?>(null) }

    var isFetchingAlbumTracks by remember { mutableStateOf(false) }
    var resolvedAlbumTracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }

    LaunchedEffect(selectedAlbum) {
        val album = selectedAlbum
        if (album != null && album.tracks.any { it.serverId == "pd_server" }) {
            isFetchingAlbumTracks = true
            try {
                val firstTrack = album.tracks.first()
                val identifier = firstTrack.id.split("___").first()
                val files = com.example.data.network.ArchiveOrgClient.fetchFilesForIdentifier(identifier)
                val mp3Files = files.filter { it.name.endsWith(".mp3", ignoreCase = true) }.sortedBy { it.name }
                if (mp3Files.isNotEmpty()) {
                    resolvedAlbumTracks = mp3Files.mapIndexed { index, fileInfo ->
                        val fileName = fileInfo.name
                        val cleanTitle = fileName
                            .replace(".mp3", "", ignoreCase = true)
                            .replace(Regex("^\\d+\\s*-\\s*"), "")
                            .replace("_", " ")
                            .trim()

                        val encodedFile = java.net.URLEncoder.encode(fileName, "UTF-8")
                            .replace("+", "%20")
                            .replace("%2F", "/")
                            .replace("%3A", ":")

                        MusicTrack(
                            id = "${identifier}___${fileName}",
                            title = cleanTitle,
                            artist = album.artist,
                            album = album.title,
                            coverUrl = album.coverUrl,
                            duration = (fileInfo.length * 1000).toLong().takeIf { it > 0 } ?: 180000L,
                            serverId = "pd_server",
                            streamUrl = "https://archive.org/download/$identifier/$encodedFile",
                            genre = album.genre,
                            trackNumber = index + 1
                        )
                    }
                } else {
                    resolvedAlbumTracks = album.tracks
                }
            } catch (e: Exception) {
                resolvedAlbumTracks = album.tracks
            } finally {
                isFetchingAlbumTracks = false
            }
        } else {
            resolvedAlbumTracks = album?.tracks ?: emptyList()
            isFetchingAlbumTracks = false
        }
    }

    // Pre-calculated groupings
    val albumGroups = remember(currentMusic) {
        currentMusic.groupBy { "${it.artist}___${it.album}" }
            .map { (_, tracks) ->
                val first = tracks.first()
                AlbumGroup(
                    title = first.album.ifBlank { "Unknown Album" },
                    artist = first.artist.ifBlank { "Unknown Artist" },
                    coverUrl = tracks.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl ?: "",
                    genre = first.genre.ifBlank { "Various" },
                    tracks = tracks.sortedBy { it.trackNumber }
                )
            }.sortedBy { it.title }
    }

    val artistGroups = remember(albumGroups, currentMusic) {
        currentMusic.groupBy { it.artist.ifBlank { "Unknown Artist" } }
            .map { (artistName, tracks) ->
                val artistAlbums = albumGroups.filter { it.artist == artistName }
                val cover = tracks.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl ?: ""
                ArtistGroup(
                    name = artistName,
                    coverUrl = cover,
                    albums = artistAlbums,
                    tracks = tracks
                )
            }.sortedBy { it.name }
    }

    val genreList = remember(currentMusic) {
        val standardGenres = listOf(
            GenreItem(
                name = "Rock & Alternative",
                gradient = listOf(Color(0xFFE53935), Color(0xFF8E24AA)),
                icon = Icons.Default.ElectricBolt,
                description = "Guitars, Riffs & Anthems"
            ),
            GenreItem(
                name = "Electronic & Dance",
                gradient = listOf(Color(0xFF00ACC1), Color(0xFF3949AB)),
                icon = Icons.Default.GraphicEq,
                description = "Synths, Beats & Basslines"
            ),
            GenreItem(
                name = "Pop & Vocal",
                gradient = listOf(Color(0xFFFF4081), Color(0xFFFF9100)),
                icon = Icons.Default.Mic,
                description = "Melodic Hooks & Energy"
            ),
            GenreItem(
                name = "Hip Hop & R&B",
                gradient = listOf(Color(0xFFFFB300), Color(0xFFD81B60)),
                icon = Icons.Default.SpeakerGroup,
                description = "Flows, Grooves & Beats"
            ),
            GenreItem(
                name = "Jazz & Blues",
                gradient = listOf(Color(0xFF5D4037), Color(0xFF8D6E63)),
                icon = Icons.Default.Piano,
                description = "Soulful Horns & Improvisation"
            ),
            GenreItem(
                name = "Classical & Soundtracks",
                gradient = listOf(Color(0xFF1E88E5), Color(0xFF26A69A)),
                icon = Icons.Default.TheaterComedy,
                description = "Orchestras & Cinematic Themes"
            ),
            GenreItem(
                name = "Acoustic & Folk",
                gradient = listOf(Color(0xFF43A047), Color(0xFF00897B)),
                icon = Icons.Default.Park,
                description = "Unplugged, Warm & Intimate"
            ),
            GenreItem(
                name = "Ambient & Lo-Fi",
                gradient = listOf(Color(0xFF5C6BC0), Color(0xFF7E57C2)),
                icon = Icons.Default.Nightlight,
                description = "Calm Textures & Focus"
            )
        )
        val databaseGenres = currentMusic.map { it.genre.trim() }.filter { it.isNotBlank() }.distinct()
        val extraGenres = databaseGenres.filter { dbGenre ->
            standardGenres.none { it.name.equals(dbGenre, ignoreCase = true) }
        }.map { dbGenre ->
            GenreItem(
                name = dbGenre,
                gradient = listOf(Color(0xFF3F51B5), Color(0xFF00BCD4)),
                icon = Icons.Default.Audiotrack,
                description = "AI Curated Rhythm Blend"
            )
        }
        standardGenres + extraGenres
    }

    // Filtered tracks for global search
    val filteredTracks = remember(currentMusic, searchQuery) {
        if (searchQuery.isBlank()) currentMusic
        else currentMusic.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true) ||
            it.album.contains(searchQuery, ignoreCase = true) ||
            it.genre.contains(searchQuery, ignoreCase = true)
        }
    }

    // Filtered albums for global search
    val filteredAlbums = remember(albumGroups, searchQuery) {
        if (searchQuery.isBlank()) albumGroups
        else albumGroups.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true) ||
            it.genre.contains(searchQuery, ignoreCase = true)
        }
    }

    // Main Layout
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
                    Icons.Default.MusicVideo,
                    contentDescription = null,
                    tint = AccentIndigo,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "Music & Video",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        if (currentMusic.isNotEmpty()) "${currentMusic.size} tracks • ${albumGroups.size} albums • Plex/Jellyfin ready" else "Your Personal Media & Video Cloud",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Quick Remix AI Categories Button
                IconButton(
                    onClick = { shuffleSeed++ },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = "Remix AI Categories", tint = Color.White)
                }

                IconButton(
                    onClick = { viewModel.refreshPersonalMedia() },
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = AccentIndigo,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh & Sync", tint = Color.White)
                    }
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Persistent Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            placeholder = { Text("Search songs, artists, albums, genres...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = AccentTeal) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Black.copy(alpha = 0.2f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                focusedBorderColor = AccentIndigo,
                unfocusedBorderColor = SurfaceGlassBorder
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Segmented Primary Pill Tab Switch
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
                    .clickable { selectedSource = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$personalTabLabel Library",
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
                    .clickable { selectedSource = 1 }
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

        Spacer(modifier = Modifier.height(12.dp))

        // Empty Server State
        if (currentMusic.isEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(AccentIndigo.copy(alpha = 0.3f), AccentTeal.copy(alpha = 0.3f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (selectedSource == 0) Icons.Default.MusicNote else Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = AccentIndigo,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                if (selectedSource == 0) "No Music Synced" else "No Public Domain Music",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (selectedSource == 0) {
                                    "Connect your personal Plex server with your URL & Token to stream your library with full genre, artist, and album hierarchy."
                                } else {
                                    "Checking public domain audio tracks. Please ensure you have an active internet connection."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                            if (selectedSource == 0) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = onNavigateToSettings,
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Connect Plex Server", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
            return
        }

        // Active Drill-Down Check: If a specific category view is open (No single row restriction!)
        if (selectedCategoryView != null) {
            val catData = selectedCategoryView!!
            MusicCategoryDetailScreen(
                title = catData.title,
                subtitle = catData.subtitle,
                description = catData.description,
                gradient = catData.gradient,
                icon = catData.icon,
                coverUrl = catData.coverUrl,
                tracks = catData.tracks,
                currentTrackId = playbackState.currentMusicTrack?.id,
                isPlaying = playbackState.isPlaying,
                onBack = { selectedCategoryView = null },
                onTrackClick = { track ->
                    viewModel.playMusicTrackWithResolution(track, catData.tracks)
                    onTrackClick(track)
                },
                onPlayAll = {
                    val first = catData.tracks.firstOrNull()
                    if (first != null) {
                        viewModel.playMusicTrackWithResolution(first, catData.tracks)
                        onTrackClick(first)
                    }
                },
                onShuffle = {
                    val shuffled = catData.tracks.shuffled()
                    val first = shuffled.firstOrNull()
                    if (first != null) {
                        viewModel.playMusicTrackWithResolution(first, shuffled)
                        onTrackClick(first)
                    }
                }
            )
            return
        }

        // Active Drill-Down Check: If an AI Mix is selected
        if (selectedMix != null) {
            val mix = selectedMix!!
            MusicCategoryDetailScreen(
                title = mix.title,
                subtitle = mix.subtitle,
                description = mix.description,
                gradient = mix.gradientColors,
                icon = mix.icon,
                coverUrl = mix.coverUrl,
                tracks = mix.tracks,
                currentTrackId = playbackState.currentMusicTrack?.id,
                isPlaying = playbackState.isPlaying,
                onBack = { selectedMix = null },
                onTrackClick = { track ->
                    viewModel.playMusicTrackWithResolution(track, mix.tracks)
                    onTrackClick(track)
                },
                onPlayAll = {
                    val first = mix.tracks.firstOrNull()
                    if (first != null) {
                        viewModel.playMusicTrackWithResolution(first, mix.tracks)
                        onTrackClick(first)
                    }
                },
                onShuffle = {
                    val shuffled = mix.tracks.shuffled()
                    val first = shuffled.firstOrNull()
                    if (first != null) {
                        viewModel.playMusicTrackWithResolution(first, shuffled)
                        onTrackClick(first)
                    }
                }
            )
            return
        }

        // Active Drill-Down Check: If a Mood is selected
        if (selectedMood != null) {
            val mood = selectedMood!!
            val matchingTracks = remember(mood, currentMusic) {
                MusicMoodsCatalog.filterTracksForMood(mood, currentMusic)
            }
            MusicCategoryDetailScreen(
                title = mood.name,
                subtitle = mood.group,
                description = mood.description,
                gradient = mood.gradient,
                icon = mood.icon,
                coverUrl = matchingTracks.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl ?: "",
                tracks = matchingTracks,
                currentTrackId = playbackState.currentMusicTrack?.id,
                isPlaying = playbackState.isPlaying,
                onBack = { selectedMood = null },
                onTrackClick = { track ->
                    viewModel.playMusicTrackWithResolution(track, matchingTracks)
                    onTrackClick(track)
                },
                onPlayAll = {
                    val first = matchingTracks.firstOrNull()
                    if (first != null) {
                        viewModel.playMusicTrackWithResolution(first, matchingTracks)
                        onTrackClick(first)
                    }
                },
                onShuffle = {
                    val shuffled = matchingTracks.shuffled()
                    val first = shuffled.firstOrNull()
                    if (first != null) {
                        viewModel.playMusicTrackWithResolution(first, shuffled)
                        onTrackClick(first)
                    }
                }
            )
            return
        }

        // Active Drill-Down Check: If an album is selected
        if (selectedAlbum != null) {
            AlbumSongsScreen(
                album = selectedAlbum!!,
                tracks = resolvedAlbumTracks,
                isFetching = isFetchingAlbumTracks,
                isPlaying = playbackState.isPlaying,
                currentTrackId = playbackState.currentMusicTrack?.id,
                onBack = { selectedAlbum = null },
                onTrackSelected = { track ->
                    viewModel.playMusicTrackWithResolution(track, resolvedAlbumTracks)
                    onTrackClick(track)
                },
                onPlayAll = {
                    val first = resolvedAlbumTracks.firstOrNull()
                    if (first != null) {
                        viewModel.playMusicTrackWithResolution(first, resolvedAlbumTracks)
                        onTrackClick(first)
                    }
                }
            )
            return
        }

        // Active Drill-Down Check: If an artist is selected
        if (selectedArtist != null) {
            ArtistDetailScreen(
                artist = selectedArtist!!,
                onBack = { selectedArtist = null },
                onAlbumClick = { album -> selectedAlbum = album },
                onTrackClick = { track ->
                    viewModel.playMusicTrackWithResolution(track)
                    onTrackClick(track)
                },
                onViewBio = { artistName ->
                    onArtistClick?.invoke(artistName)
                }
            )
            return
        }

        // Active Drill-Down Check: If a genre is selected
        if (selectedGenre != null) {
            GenreDetailScreen(
                genreName = selectedGenre!!,
                albums = albumGroups.filter {
                    it.genre.contains(selectedGenre!!, ignoreCase = true) ||
                    selectedGenre == "All" ||
                    it.title.contains(selectedGenre!!, ignoreCase = true)
                }.ifEmpty { albumGroups.take(6) },
                artists = artistGroups.take(6),
                onBack = { selectedGenre = null },
                onAlbumClick = { album -> selectedAlbum = album },
                onArtistClick = { artist -> selectedArtist = artist }
            )
            return
        }

        // Global Search View
        if (searchQuery.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredAlbums.isNotEmpty()) {
                    item {
                        Text("Albums (${filteredAlbums.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filteredAlbums) { album ->
                                AlbumShelfCard(album = album, onClick = { selectedAlbum = album })
                            }
                        }
                    }
                }

                item {
                    Text("Tracks (${filteredTracks.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                items(filteredTracks, key = { it.id }) { track ->
                    MusicTrackRowItem(
                        track = track,
                        isPlaying = playbackState.currentMusicTrack?.id == track.id && playbackState.isPlaying,
                        onClick = {
                            viewModel.playMusicTrackWithResolution(track)
                            onTrackClick(track)
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
            return
        }

        // Hierarchy Navigation Tabs: Shelves, Moods, Genres, Artists, Albums, Songs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            items(MusicNavTab.values()) { tab ->
                val isSelected = selectedTab == tab
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedTab = tab },
                    label = {
                        Text(
                            when (tab) {
                                MusicNavTab.SHELVES -> "Shelves"
                                MusicNavTab.MOODS -> "Moods (100)"
                                MusicNavTab.GENRES -> "Genres"
                                MusicNavTab.ARTISTS -> "Artists"
                                MusicNavTab.ALBUMS -> "Albums"
                                MusicNavTab.SONGS -> "All Songs"
                            },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Icon(
                            when (tab) {
                                MusicNavTab.SHELVES -> Icons.Default.ViewCarousel
                                MusicNavTab.MOODS -> Icons.Default.Mood
                                MusicNavTab.GENRES -> Icons.Default.Category
                                MusicNavTab.ARTISTS -> Icons.Default.Person
                                MusicNavTab.ALBUMS -> Icons.Default.Album
                                MusicNavTab.SONGS -> Icons.Default.Audiotrack
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentIndigo.copy(alpha = 0.25f),
                        selectedLabelColor = AccentIndigo,
                        selectedLeadingIconColor = AccentIndigo
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Tab Content
        when (selectedTab) {
            MusicNavTab.SHELVES -> {
                MusicShelvesView(
                    allTracks = currentMusic,
                    recentTracks = recentMusic,
                    dynamicMixes = dynamicMixes,
                    albumGroups = albumGroups,
                    artistGroups = artistGroups,
                    playbackState = playbackState,
                    onMixClick = { selectedMix = it },
                    onAlbumClick = { selectedAlbum = it },
                    onArtistClick = { selectedArtist = it },
                    onOpenCategory = { cat -> selectedCategoryView = cat },
                    onRemixAI = { shuffleSeed++ },
                    onTrackClick = { track ->
                        viewModel.playMusicTrackWithResolution(track)
                        onTrackClick(track)
                    }
                )
            }

            MusicNavTab.MOODS -> {
                MoodsGridView(
                    allTracks = currentMusic,
                    onMoodClick = { mood -> selectedMood = mood }
                )
            }

            MusicNavTab.GENRES -> {
                GenresGridView(
                    genres = genreList,
                    onGenreClick = { genre -> selectedGenre = genre.name }
                )
            }

            MusicNavTab.ARTISTS -> {
                ArtistsGridView(
                    artists = artistGroups,
                    onArtistClick = { artist -> selectedArtist = artist }
                )
            }

            MusicNavTab.ALBUMS -> {
                AlbumsGridView(
                    albums = albumGroups,
                    onAlbumClick = { album -> selectedAlbum = album }
                )
            }

            MusicNavTab.SONGS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentMusic, key = { it.id }) { track ->
                        MusicTrackRowItem(
                            track = track,
                            isPlaying = playbackState.currentMusicTrack?.id == track.id && playbackState.isPlaying,
                            onClick = {
                                viewModel.playMusicTrackWithResolution(track)
                                onTrackClick(track)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// MUSIC SHELVES VIEW (With Clickable Headers opening to full category view!)
// -----------------------------------------------------------------------------
@Composable
fun MusicShelvesView(
    allTracks: List<MusicTrack>,
    recentTracks: List<MusicTrack>,
    dynamicMixes: List<MusicMix>,
    albumGroups: List<AlbumGroup>,
    artistGroups: List<ArtistGroup>,
    playbackState: PlaybackState,
    onMixClick: (MusicMix) -> Unit,
    onAlbumClick: (AlbumGroup) -> Unit,
    onArtistClick: (ArtistGroup) -> Unit,
    onOpenCategory: (CategoryViewData) -> Unit,
    onRemixAI: () -> Unit,
    onTrackClick: (MusicTrack) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. AI Dynamic Mixes & "For You" Section
        if (dynamicMixes.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOpenCategory(
                                CategoryViewData(
                                    title = "AI Dynamic Mixes",
                                    subtitle = "Personalized mixes & time-of-day categories",
                                    description = "Intelligently updated multiple times a day from your listening history and acoustic algorithms.",
                                    gradient = listOf(Color(0xFF6366F1), Color(0xFFEC4899)),
                                    icon = Icons.Default.AutoAwesome,
                                    tracks = dynamicMixes.flatMap { it.tracks }.distinctBy { it.id }
                                )
                            )
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Made For You & AI Mixes", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFEC4899).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "AI",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFEC4899)
                                    )
                                }
                            }
                            Text("Updated multiple times a day", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("See All", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentIndigo)
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(dynamicMixes, key = { it.id }) { mix ->
                        MusicMixShelfCard(
                            mix = mix,
                            onClick = { onMixClick(mix) },
                            onPlayClick = {
                                val first = mix.tracks.firstOrNull()
                                if (first != null) onTrackClick(first)
                            }
                        )
                    }
                }
            }
        }

        // 2. Shelf: Continue Listening / Recent Grooves (CLICKABLE HEADER TO OPEN FULL CATEGORY)
        if (recentTracks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOpenCategory(
                                CategoryViewData(
                                    title = "Recent Grooves",
                                    subtitle = "All recently played tracks",
                                    description = "Your complete playback history and recent audio spins.",
                                    gradient = listOf(AccentIndigo, AccentTeal),
                                    icon = Icons.Default.History,
                                    tracks = recentTracks
                                )
                            )
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Recent Grooves", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Pick up where you left off", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("See All (${recentTracks.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentIndigo)
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(recentTracks, key = { "recent_${it.id}" }) { track ->
                        MusicTrackShelfCard(
                            track = track,
                            isPlaying = playbackState.currentMusicTrack?.id == track.id && playbackState.isPlaying,
                            onClick = { onTrackClick(track) }
                        )
                    }
                }
            }
        }

        // 3. Shelf: Featured & New Albums (CLICKABLE HEADER TO OPEN FULL CATEGORY)
        if (albumGroups.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOpenCategory(
                                CategoryViewData(
                                    title = "All Albums & Releases",
                                    subtitle = "${albumGroups.size} albums in your collection",
                                    description = "Browse full albums, LPs, EPs and single releases across your entire collection.",
                                    gradient = listOf(Color(0xFFFF9800), Color(0xFFE91E63)),
                                    icon = Icons.Default.Album,
                                    tracks = albumGroups.flatMap { it.tracks }
                                )
                            )
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Album, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("New Releases & Albums", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFFF9800).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "NEW",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                            }
                            Text("Slide sideways or tap to view all", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("See All (${albumGroups.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentIndigo)
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(albumGroups, key = { "alb_${it.title}_${it.artist}" }) { album ->
                        AlbumShelfCard(
                            album = album,
                            onClick = { onAlbumClick(album) }
                        )
                    }
                }
            }
        }

        // 4. Shelf: Top Artists (CLICKABLE HEADER TO OPEN FULL ARTISTS)
        if (artistGroups.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOpenCategory(
                                CategoryViewData(
                                    title = "Featured Artists",
                                    subtitle = "${artistGroups.size} artists in your library",
                                    description = "All standout artists and musical creators in your synced collection.",
                                    gradient = listOf(AccentTeal, AccentIndigo),
                                    icon = Icons.Default.Person,
                                    tracks = artistGroups.flatMap { it.tracks }
                                )
                            )
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Standout Artists", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Artists in your personal collection", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("See All (${artistGroups.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentIndigo)
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(artistGroups, key = { "art_${it.name}" }) { artist ->
                        ArtistShelfCard(
                            artist = artist,
                            onClick = { onArtistClick(artist) }
                        )
                    }
                }
            }
        }

        // 5. Shelf: Curated Mixes & Audio Tracks (CLICKABLE HEADER TO OPEN FULL ALL TRACKS)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpenCategory(
                            CategoryViewData(
                                title = "Curated Library Tracks",
                                subtitle = "${allTracks.size} total audio tracks",
                                description = "The entire comprehensive track catalogue available in your cloud library.",
                                gradient = listOf(Color(0xFF8E24AA), Color(0xFF1E88E5)),
                                icon = Icons.Default.LibraryMusic,
                                tracks = allTracks
                            )
                        )
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Curated Mixes & Audio Tracks", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Popular selections tailored for you", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("See All (${allTracks.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentIndigo)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(allTracks.take(15), key = { "mix_${it.id}" }) { track ->
                    MusicTrackShelfCard(
                        track = track,
                        isPlaying = playbackState.currentMusicTrack?.id == track.id && playbackState.isPlaying,
                        onClick = { onTrackClick(track) }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(28.dp)) }
    }
}

// -----------------------------------------------------------------------------
// MUSIC MIX SHELF CARD (AI mixes & For You hero card)
// -----------------------------------------------------------------------------
@Composable
fun MusicMixShelfCard(
    mix: MusicMix,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(180.dp)
            .height(140.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(mix.gradientColors))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        mix.category.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { onPlayClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play Mix", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                mix.title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                mix.subtitle,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${mix.tracks.size} tracks",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// MOODS GRID VIEW (100 Distinct Stylized Moods matching Genres view design)
// -----------------------------------------------------------------------------
@Composable
fun MoodsGridView(
    allTracks: List<MusicTrack>,
    onMoodClick: (MoodItem) -> Unit
) {
    var selectedGroup by remember { mutableStateOf("All") }
    var moodSearchQuery by remember { mutableStateOf("") }

    val filteredMoods = remember(selectedGroup, moodSearchQuery) {
        MusicMoodsCatalog.allMoods.filter { mood ->
            val matchesGroup = selectedGroup == "All" || mood.group.equals(selectedGroup, ignoreCase = true)
            val matchesSearch = moodSearchQuery.isBlank() ||
                mood.name.contains(moodSearchQuery, ignoreCase = true) ||
                mood.description.contains(moodSearchQuery, ignoreCase = true) ||
                mood.keywords.any { it.contains(moodSearchQuery, ignoreCase = true) }
            matchesGroup && matchesSearch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Mood Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            items(MusicMoodsCatalog.moodGroups) { group ->
                val isSelected = selectedGroup == group
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedGroup = group },
                    label = { Text(group, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentIndigo.copy(alpha = 0.25f),
                        selectedLabelColor = AccentIndigo
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Moods 2-Column Responsive Grid (Styled exactly like Genres Screen)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(filteredMoods, key = { it.id }) { mood ->
                MoodCard(
                    mood = mood,
                    trackCount = MusicMoodsCatalog.filterTracksForMood(mood, allTracks).size,
                    onClick = { onMoodClick(mood) }
                )
            }
        }
    }
}

@Composable
fun MoodCard(
    mood: MoodItem,
    trackCount: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(mood.gradient))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Text(
                mood.name,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = Color.White,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                mood.description,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "$trackCount songs",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Icon(
            mood.icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier
                .size(44.dp)
                .align(Alignment.BottomEnd)
        )
    }
}

// -----------------------------------------------------------------------------
// MUSIC CATEGORY DETAIL SCREEN (Full grid/list view for any category or mood!)
// -----------------------------------------------------------------------------
@Composable
fun MusicCategoryDetailScreen(
    title: String,
    subtitle: String,
    description: String = "",
    gradient: List<Color> = listOf(AccentIndigo, AccentTeal),
    icon: ImageVector? = null,
    coverUrl: String = "",
    tracks: List<MusicTrack>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onTrackClick: (MusicTrack) -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    var isGridView by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back Navigation & Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceGlass)
                            .border(1.dp, SurfaceGlassBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Category View", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                // Grid / List toggle
                IconButton(
                    onClick = { isGridView = !isGridView },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SurfaceGlass)
                        .border(1.dp, SurfaceGlassBorder, CircleShape)
                ) {
                    Icon(
                        if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle View",
                        tint = AccentIndigo
                    )
                }
            }
        }

        // Hero Category Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(gradient))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (icon != null) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                subtitle,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            description,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${tracks.size} tracks in full category",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.75f)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onPlayAll,
                                enabled = tracks.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play All", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = onShuffle,
                                enabled = tracks.isNotEmpty(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Shuffle", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Full Tracks Catalog (Scrollable without side-scrolling restrictions)
        if (tracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tracks available in this category.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (isGridView) {
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 2000.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tracks, key = { it.id }) { track ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceGlass)
                                .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(14.dp))
                                .clickable { onTrackClick(track) }
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AccentIndigo.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (track.coverUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = track.coverUrl,
                                        contentDescription = track.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(36.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(track.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        } else {
            items(tracks, key = { it.id }) { track ->
                MusicTrackRowItem(
                    track = track,
                    isPlaying = currentTrackId == track.id && isPlaying,
                    onClick = { onTrackClick(track) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(28.dp)) }
    }
}

// -----------------------------------------------------------------------------
// GENRES GRID VIEW
// -----------------------------------------------------------------------------
@Composable
fun GenresGridView(
    genres: List<GenreItem>,
    onGenreClick: (GenreItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(genres) { genre ->
            GenreCard(genre = genre, onClick = { onGenreClick(genre) })
        }
    }
}

@Composable
fun GenreCard(genre: GenreItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(genre.gradient))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Text(
                genre.name,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = Color.White,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                genre.description,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 2,
                lineHeight = 14.sp
            )
        }

        Icon(
            genre.icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier
                .size(44.dp)
                .align(Alignment.BottomEnd)
        )
    }
}

// -----------------------------------------------------------------------------
// ARTISTS GRID VIEW
// -----------------------------------------------------------------------------
@Composable
fun ArtistsGridView(
    artists: List<ArtistGroup>,
    onArtistClick: (ArtistGroup) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(artists, key = { it.name }) { artist ->
            ArtistGridCard(artist = artist, onClick = { onArtistClick(artist) })
        }
    }
}

@Composable
fun ArtistGridCard(artist: ArtistGroup, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceGlass)
            .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(AccentTeal.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            if (artist.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = artist.coverUrl,
                    contentDescription = artist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(artist.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, textAlign = TextAlign.Center)
        Text(
            "${artist.albums.size} Albums • ${artist.tracks.size} Songs",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// -----------------------------------------------------------------------------
// ALBUMS GRID VIEW
// -----------------------------------------------------------------------------
@Composable
fun AlbumsGridView(
    albums: List<AlbumGroup>,
    onAlbumClick: (AlbumGroup) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(albums, key = { "${it.title}_${it.artist}" }) { album ->
            AlbumCard(album = album, onClick = { onAlbumClick(album) })
        }
    }
}

@Composable
fun AlbumCard(album: AlbumGroup, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .shadow(6.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceGlass)
                .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (album.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = album.coverUrl,
                    contentDescription = album.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AccentIndigo.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Album, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(48.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(album.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(album.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// -----------------------------------------------------------------------------
// SHELF CARDS (Track, Album, Artist)
// -----------------------------------------------------------------------------
@Composable
fun MusicTrackShelfCard(
    track: MusicTrack,
    isPlaying: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .shadow(6.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceGlass)
                .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(16.dp))
        ) {
            if (track.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(AccentIndigo.copy(alpha = 0.5f), AccentTeal.copy(alpha = 0.3f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Audiotrack, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }

            if (isPlaying) {
                Surface(
                    color = AccentIndigo,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Playing", tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("PLAYING", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(track.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(track.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun AlbumShelfCard(album: AlbumGroup, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .shadow(6.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceGlass)
                .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(16.dp))
        ) {
            if (album.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = album.coverUrl,
                    contentDescription = album.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AccentIndigo.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Album, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(44.dp))
                }
            }

            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            ) {
                Text(
                    "${album.tracks.size} tracks",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(album.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(album.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ArtistShelfCard(artist: ArtistGroup, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(AccentTeal.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            if (artist.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = artist.coverUrl,
                    contentDescription = artist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(44.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(artist.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, textAlign = TextAlign.Center)
        Text("${artist.albums.size} albums", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MusicTrackRowItem(
    track: MusicTrack,
    isPlaying: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceGlass)
            .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (track.coverUrl.isNotBlank()) {
            AsyncImage(
                model = track.coverUrl,
                contentDescription = track.title,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentIndigo.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = AccentIndigo)
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${track.artist} • ${track.album}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onClick) {
            Icon(
                if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                contentDescription = "Play",
                tint = AccentIndigo,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

// -----------------------------------------------------------------------------
// ALBUM SONGS SCREEN (Album image in a square shape at the top center with rounded corners)
// -----------------------------------------------------------------------------
@Composable
fun AlbumSongsScreen(
    album: AlbumGroup,
    tracks: List<MusicTrack>,
    isFetching: Boolean,
    isPlaying: Boolean,
    currentTrackId: String?,
    onBack: () -> Unit,
    onTrackSelected: (MusicTrack) -> Unit,
    onPlayAll: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Back Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SurfaceGlass)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Album Details", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Top Center Square Album Image with Rounded Corners
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .shadow(16.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceGlass)
                        .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (album.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = album.coverUrl,
                            contentDescription = "Cover for ${album.title}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(AccentIndigo.copy(alpha = 0.5f), AccentTeal.copy(alpha = 0.4f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Album,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Album Info
                Text(
                    album.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    album.artist,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentIndigo,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${tracks.size} Songs • ${album.genre}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Play All & Shuffle
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPlayAll,
                        enabled = tracks.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play Album", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val shuffled = tracks.shuffled().firstOrNull()
                            if (shuffled != null) onTrackSelected(shuffled)
                        },
                        enabled = tracks.isNotEmpty(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        item {
            HorizontalDivider(
                color = SurfaceGlassBorder,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        if (isFetching) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentTeal)
                }
            }
        }

        // Tracklist
        items(tracks) { track ->
            val isCurrent = currentTrackId == track.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isCurrent) AccentIndigo.copy(alpha = 0.15f) else SurfaceGlass)
                    .clickable { onTrackSelected(track) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${track.trackNumber}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) AccentIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(28.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        track.title,
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = if (isCurrent) AccentIndigo else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        track.artist,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                if (track.duration > 0) {
                    Text(
                        formatMillis(track.duration),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { onTrackSelected(track) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (isCurrent && isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                        contentDescription = "Play",
                        tint = AccentIndigo,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(28.dp)) }
    }
}

// -----------------------------------------------------------------------------
// DRILLDOWN DETAIL SCREENS (Artist & Genre)
// -----------------------------------------------------------------------------
@Composable
fun ArtistDetailScreen(
    artist: ArtistGroup,
    onBack: () -> Unit,
    onAlbumClick: (AlbumGroup) -> Unit,
    onTrackClick: (MusicTrack) -> Unit,
    onViewBio: ((String) -> Unit)? = null
) {
    var artistBio by remember { mutableStateOf<com.example.data.network.CreatorBioData?>(null) }
    var isLoadingBio by remember { mutableStateOf(true) }

    LaunchedEffect(artist.name) {
        isLoadingBio = true
        artistBio = com.example.data.network.InternetCreatorBioFetcher.getCreatorBio(artist.name)
        isLoadingBio = false
    }

    val bioData = artistBio
    val bioText = bioData?.bio ?: ""
    val bioImage = bioData?.imageUrl ?: ""

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SurfaceGlass)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(artist.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (onViewBio != null) {
                    Button(
                        onClick = { onViewBio(artist.name) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Artist Bio", fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewBio?.invoke(artist.name) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(AccentIndigo.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val heroImage = if (bioImage.isNotBlank()) bioImage else artist.coverUrl
                        if (heroImage.isNotBlank()) {
                            AsyncImage(
                                model = heroImage,
                                contentDescription = artist.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(36.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(artist.name, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${artist.albums.size} Albums in collection", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (onViewBio != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tap to view full biography & archives →", fontSize = 12.sp, color = AccentTeal, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // Artist Biography Banner / Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass.copy(alpha = 0.85f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Artist Biography", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        if (onViewBio != null) {
                            TextButton(
                                onClick = { onViewBio(artist.name) },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Full Bio →", fontSize = 13.sp, color = AccentTeal)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoadingBio) {
                        Text(
                            "Loading artist biography...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (bioText.isNotBlank()) {
                        Text(
                            bioText.take(280) + if (bioText.length > 280) "..." else "",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            lineHeight = 18.sp
                        )
                    } else {
                        Text(
                            "Discover rich biography, discography history, Wikidata, and Internet Archive recordings for ${artist.name}.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text("Albums", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        items(artist.albums) { album ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceGlass)
                    .clickable { onAlbumClick(album) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = album.coverUrl,
                    contentDescription = album.title,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(album.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("${album.tracks.size} Tracks", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
            }
        }

        item { Spacer(modifier = Modifier.height(28.dp)) }
    }
}

@Composable
fun GenreDetailScreen(
    genreName: String,
    albums: List<AlbumGroup>,
    artists: List<ArtistGroup>,
    onBack: () -> Unit,
    onAlbumClick: (AlbumGroup) -> Unit,
    onArtistClick: (ArtistGroup) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp)
    ) {
        // Back Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SurfaceGlass)
                    .border(1.dp, SurfaceGlassBorder, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(genreName, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("${albums.size} albums", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 12.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(albums, key = { "${it.title}_${it.artist}" }) { album ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceGlass)
                        .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(12.dp))
                        .clickable { onAlbumClick(album) }
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentIndigo.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (album.coverUrl.isNotBlank()) {
                            AsyncImage(
                                model = album.coverUrl,
                                contentDescription = album.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Album,
                                contentDescription = null,
                                tint = AccentIndigo,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        album.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        album.artist,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
