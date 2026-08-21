package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.border
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.MusicTrack
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder
import java.util.Locale

enum class MusicNavTab {
    SHELVES, GENRES, ARTISTS, ALBUMS, SONGS
}

data class GenreItem(
    val name: String,
    val imageUrl: String = "",
    val gradient: List<Color>,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
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

@Composable
fun MusicScreen(
    viewModel: MainViewModel,
    onTrackClick: (MusicTrack) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val allMusic by viewModel.allMusic.collectAsState()
    val recentMusic by viewModel.recentMusic.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(MusicNavTab.SHELVES) }
    var selectedSource by remember { mutableIntStateOf(0) } // 0 = Personal, 1 = Public Domain

    val publicDomainMusic = remember {
        listOf(
            MusicTrack(
                id = "pd_track_1",
                title = "Acoustic Chill",
                artist = "Various Artists",
                album = "Public Domain Sounds",
                duration = 2700000L,
                coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&q=80",
                serverId = "pd_server",
                streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__Fly_Paper.mp3",
                trackNumber = 1,
                genre = "Acoustic"
            )
        )
    }

    val currentMusic = remember(allMusic, selectedSource) {
        if (selectedSource == 0) allMusic else publicDomainMusic
    }

    // Navigation Stack for File Structure Drilldown
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var selectedArtist by remember { mutableStateOf<ArtistGroup?>(null) }
    var selectedAlbum by remember { mutableStateOf<AlbumGroup?>(null) }

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
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Music",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = Color.White
                )
                Text(
                    if (currentMusic.isNotEmpty()) "${currentMusic.size} tracks • ${albumGroups.size} albums" else "Your Personal Music Cloud",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceGlass)
                    .border(1.dp, SurfaceGlassBorder, CircleShape)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AccentIndigo)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        androidx.compose.material3.TabRow(
            selectedTabIndex = selectedSource,
            containerColor = Color.Transparent,
            indicator = { tabPositions ->
                androidx.compose.material3.TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedSource]),
                    color = AccentIndigo
                )
            }
        ) {
            listOf("Personal Library", "Public Domain").forEachIndexed { index, title ->
                androidx.compose.material3.Tab(
                    selected = selectedSource == index,
                    onClick = { selectedSource = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedSource == index) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    selectedContentColor = AccentIndigo,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Persistent Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search songs, artists, albums, genres...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = AccentIndigo) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceGlass,
                unfocusedContainerColor = SurfaceGlass,
                focusedBorderColor = AccentIndigo.copy(alpha = 0.8f),
                unfocusedBorderColor = SurfaceGlassBorder
            )
        )

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

        // Active Drill-Down Check: If an album is selected, display the Songs Screen with square album art at top center!
        if (selectedAlbum != null) {
            AlbumSongsScreen(
                album = selectedAlbum!!,
                isPlaying = playbackState.isPlaying,
                currentTrackId = playbackState.currentMusicTrack?.id,
                onBack = { selectedAlbum = null },
                onTrackSelected = { track ->
                    viewModel.playbackManager.playMusicTrack(track)
                    onTrackClick(track)
                },
                onPlayAll = {
                    val first = selectedAlbum!!.tracks.firstOrNull()
                    if (first != null) {
                        viewModel.playbackManager.playMusicTrack(first)
                        onTrackClick(first)
                    }
                }
            )
            return
        }

        // Active Drill-Down: If an artist is selected
        if (selectedArtist != null) {
            ArtistDetailScreen(
                artist = selectedArtist!!,
                onBack = { selectedArtist = null },
                onAlbumClick = { album -> selectedAlbum = album },
                onTrackClick = { track ->
                    viewModel.playbackManager.playMusicTrack(track)
                    onTrackClick(track)
                }
            )
            return
        }

        // Active Drill-Down: If a genre is selected
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

        // Global Search Active View
        if (searchQuery.isNotBlank()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        "Search Results (${filteredTracks.size} songs, ${filteredAlbums.size} albums)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (filteredAlbums.isNotEmpty()) {
                    item {
                        Text("Albums", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AccentIndigo)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(filteredAlbums) { album ->
                                AlbumCard(
                                    album = album,
                                    onClick = { selectedAlbum = album }
                                )
                            }
                        }
                    }
                }

                item {
                    Text("Songs", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AccentIndigo)
                }

                items(filteredTracks, key = { it.id }) { track ->
                    MusicTrackRowItem(
                        track = track,
                        isPlaying = playbackState.currentMusicTrack?.id == track.id && playbackState.isPlaying,
                        onClick = {
                            viewModel.playbackManager.playMusicTrack(track)
                            onTrackClick(track)
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
            return
        }

        // Hierarchy Navigation Tabs: Shelves, Genres, Artists, Albums, Songs
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
                    allTracks = allMusic,
                    recentTracks = recentMusic,
                    albumGroups = albumGroups,
                    artistGroups = artistGroups,
                    playbackState = playbackState,
                    onAlbumClick = { selectedAlbum = it },
                    onTrackClick = { track ->
                        viewModel.playbackManager.playMusicTrack(track)
                        onTrackClick(track)
                    }
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
                    items(allMusic, key = { it.id }) { track ->
                        MusicTrackRowItem(
                            track = track,
                            isPlaying = playbackState.currentMusicTrack?.id == track.id && playbackState.isPlaying,
                            onClick = {
                                viewModel.playbackManager.playMusicTrack(track)
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
// ALBUM SONGS SCREEN (Album image in a square shape at the top center with rounded corners)
// -----------------------------------------------------------------------------
@Composable
fun AlbumSongsScreen(
    album: AlbumGroup,
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
                    "${album.tracks.size} Songs • ${album.genre}",
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
                            val shuffled = album.tracks.shuffled().firstOrNull()
                            if (shuffled != null) onTrackSelected(shuffled)
                        },
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

        // Tracklist
        items(album.tracks) { track ->
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
// MUSIC SHELVES VIEW (Slide to the side, scroll up and down for curated options)
// -----------------------------------------------------------------------------
@Composable
fun MusicShelvesView(
    allTracks: List<MusicTrack>,
    recentTracks: List<MusicTrack>,
    albumGroups: List<AlbumGroup>,
    artistGroups: List<ArtistGroup>,
    playbackState: com.example.data.PlaybackState,
    onAlbumClick: (AlbumGroup) -> Unit,
    onTrackClick: (MusicTrack) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Shelf: Continue Listening / Recent Grooves
        if (recentTracks.isNotEmpty()) {
            item {
                ShelfHeader(
                    title = "Recent Grooves",
                    subtitle = "Pick up where you left off",
                    icon = Icons.Default.History,
                    iconTint = AccentIndigo
                )
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

        // 2. Shelf: Featured & New Albums
        if (albumGroups.isNotEmpty()) {
            item {
                ShelfHeader(
                    title = "New Releases & Albums",
                    subtitle = "Slide sideways to explore full albums",
                    badge = "NEW",
                    icon = Icons.Default.Album,
                    iconTint = Color(0xFFFF9800)
                )
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

        // 3. Shelf: Top Artists
        if (artistGroups.isNotEmpty()) {
            item {
                ShelfHeader(
                    title = "Standout Artists",
                    subtitle = "Artists in your personal Plex collection",
                    icon = Icons.Default.Person,
                    iconTint = AccentTeal
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(artistGroups, key = { "art_${it.name}" }) { artist ->
                        ArtistShelfCard(
                            artist = artist,
                            onClick = {
                                val firstAlb = artist.albums.firstOrNull()
                                if (firstAlb != null) onAlbumClick(firstAlb)
                            }
                        )
                    }
                }
            }
        }

        // 4. Shelf: Noteworthy Tracks & Curated Mixes
        item {
            ShelfHeader(
                title = "Curated Mixes & Audio Tracks",
                subtitle = "Popular selections tailored for you",
                icon = Icons.Default.LibraryMusic,
                iconTint = AccentIndigo
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(allTracks.take(12), key = { "mix_${it.id}" }) { track ->
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
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                genre.description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
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
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceGlass)
            .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(AccentIndigo.copy(alpha = 0.4f), AccentTeal.copy(alpha = 0.3f)))),
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

            // Playing Indicator
            if (isPlaying) {
                Surface(
                    color = AccentIndigo,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Playing", tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("PLAYING", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(track.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(album.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
// DRILLDOWN DETAIL SCREENS
// -----------------------------------------------------------------------------
@Composable
fun ArtistDetailScreen(
    artist: ArtistGroup,
    onBack: () -> Unit,
    onAlbumClick: (AlbumGroup) -> Unit,
    onTrackClick: (MusicTrack) -> Unit
) {
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
                Text(artist.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                modifier = Modifier.fillMaxWidth()
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
                        if (artist.coverUrl.isNotBlank()) {
                            AsyncImage(
                                model = artist.coverUrl,
                                contentDescription = artist.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(36.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(artist.name, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${artist.albums.size} Albums in collection", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text("${albums.size} albums • 3-Column Choices", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // 3-in-a-Row Top Down Grid for Genre choices
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp, top = 8.dp),
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
