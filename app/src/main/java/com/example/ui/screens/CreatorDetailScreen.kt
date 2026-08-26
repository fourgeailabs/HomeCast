package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.Audiobook
import com.example.data.LocalMediaMetadataProvider
import com.example.data.MusicTrack
import com.example.data.network.InternetCreatorBioFetcher
import com.example.ui.MainViewModel
import com.example.ui.components.MediaCoverArt
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorDetailScreen(
    viewModel: MainViewModel,
    creatorName: String,
    onBack: () -> Unit,
    onReadEBook: (EBookData) -> Unit = {},
    onPlayAudiobook: (Audiobook) -> Unit = {},
    onPlayMusicTrack: (MusicTrack) -> Unit = {},
    onOpenProgram: (id: String, type: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var details by remember(creatorName) {
        mutableStateOf(LocalMediaMetadataProvider.getFallbackCreatorDetails(creatorName))
    }
    var isLoadingBio by remember(creatorName) { mutableStateOf(true) }

    val allMusic by viewModel.allMusic.collectAsState()
    val pdMusic by viewModel.publicDomainMusic.collectAsState()
    val allBooks = viewModel.allBooks.collectAsState().value
    val pdBooks by viewModel.publicDomainBooks.collectAsState()
    val pdAudiobooks by viewModel.publicDomainAudiobooks.collectAsState()
    val localEBooks by viewModel.allEBooks.collectAsState()
    val resolvedDurations by viewModel.resolvedDurations.collectAsState()
    val plexMovies by viewModel.plexMovies.collectAsState()
    val plexShows by viewModel.plexShows.collectAsState()

    // Filter matching Movies
    val matchedMovies = remember(creatorName, plexMovies) {
        plexMovies.filter { movie ->
            movie.cast.any { it.name.contains(creatorName, ignoreCase = true) } ||
            movie.directors.any { it.name.contains(creatorName, ignoreCase = true) } ||
            movie.writers.any { it.name.contains(creatorName, ignoreCase = true) } ||
            movie.producers.any { it.name.contains(creatorName, ignoreCase = true) } ||
            movie.cinematographers.any { it.name.contains(creatorName, ignoreCase = true) }
        }
    }

    // Filter matching Shows
    val matchedShows = remember(creatorName, plexShows) {
        plexShows.filter { show ->
            show.cast.any { it.name.contains(creatorName, ignoreCase = true) } ||
            show.directors.any { it.name.contains(creatorName, ignoreCase = true) } ||
            show.writers.any { it.name.contains(creatorName, ignoreCase = true) } ||
            show.producers.any { it.name.contains(creatorName, ignoreCase = true) } ||
            show.seasons.any { s ->
                s.cast.any { it.name.contains(creatorName, ignoreCase = true) } ||
                s.episodes.any { ep ->
                    ep.cast.any { it.name.contains(creatorName, ignoreCase = true) } ||
                    ep.directors.any { it.name.contains(creatorName, ignoreCase = true) } ||
                    ep.writers.any { it.name.contains(creatorName, ignoreCase = true) } ||
                    ep.producers.any { it.name.contains(creatorName, ignoreCase = true) }
                }
            }
        }
    }

    LaunchedEffect(creatorName) {
        isLoadingBio = true
        try {
            // First check if Plex provides cast member info / thumbnail
            val plexPerson = (plexMovies.flatMap { it.cast + it.directors + it.writers + it.producers + it.cinematographers } +
                    plexShows.flatMap { it.cast + it.directors + it.writers + it.producers + it.seasons.flatMap { s -> s.episodes.flatMap { ep -> ep.cast + ep.directors + ep.writers + ep.producers } } })
                .firstOrNull { it.name.equals(creatorName, ignoreCase = true) }

            val bioData = InternetCreatorBioFetcher.getCreatorBio(creatorName)
            val updatedMap = bioData.toMap().toMutableMap()
            if (plexPerson != null && plexPerson.thumbUrl.isNotBlank()) {
                updatedMap["image"] = plexPerson.thumbUrl
            }
            if (plexPerson != null && plexPerson.role.isNotBlank()) {
                val existingRoles = updatedMap["roles"] ?: ""
                updatedMap["roles"] = if (existingRoles.isNotBlank()) "${plexPerson.role} • $existingRoles" else plexPerson.role
            }
            details = updatedMap
            
            // If bio is brief, attempt enriched background fetch
            if (bioData.bio.length < 100) {
                val enriched = viewModel.fetchCreatorDetailsWithGemini(creatorName)
                if (enriched.isNotEmpty() && (enriched["bio"]?.length ?: 0) > bioData.bio.length) {
                    val finalMap = enriched.toMutableMap()
                    if (plexPerson != null && plexPerson.thumbUrl.isNotBlank()) {
                        finalMap["image"] = plexPerson.thumbUrl
                    }
                    details = finalMap
                }
            }
        } catch (_: Exception) {
            // Keep fallback
        } finally {
            isLoadingBio = false
        }
    }

    // Filter matching E-Books
    val combinedEBooks = remember(creatorName, localEBooks, pdBooks) {
        val localMatch = localEBooks.filter { it.author.contains(creatorName, ignoreCase = true) }
            .map { e ->
                EBookData(
                    id = e.id,
                    title = e.title,
                    author = e.author,
                    totalChapters = 0,
                    chapters = emptyList(),
                    publicDomainUrl = e.serverId
                )
            }
        val pdMatch = pdBooks.filter { doc ->
            val creatorStr = when (val creator = doc.creator) {
                is List<*> -> creator.firstOrNull()?.toString() ?: ""
                is String -> creator
                else -> ""
            }
            creatorStr.contains(creatorName, ignoreCase = true)
        }.map { doc ->
            EBookData(
                id = doc.identifier,
                title = doc.title ?: "E-Book",
                author = creatorName,
                totalChapters = 0,
                chapters = emptyList(),
                publicDomainUrl = "https://archive.org/download/${doc.identifier}/${doc.identifier}_djvu.txt"
            )
        }
        (localMatch + pdMatch).distinctBy { it.title.lowercase().trim() }
    }

    // Filter matching Audiobooks
    val combinedAudiobooks = remember(creatorName, allBooks, pdAudiobooks, resolvedDurations) {
        val localMatch = allBooks.filter { it.author.contains(creatorName, ignoreCase = true) }
        val pdMatch = pdAudiobooks.filter { doc ->
            val creatorStr = when (val creator = doc.creator) {
                is List<*> -> creator.firstOrNull()?.toString() ?: ""
                is String -> creator
                else -> ""
            }
            creatorStr.contains(creatorName, ignoreCase = true)
        }.map { doc ->
            Audiobook(
                id = doc.identifier,
                title = doc.title ?: "Audiobook",
                author = creatorName,
                duration = resolvedDurations[doc.identifier] ?: 0L,
                coverUrl = "https://archive.org/services/img/${doc.identifier}",
                serverId = "pd_server",
                streamUrl = "https://archive.org/download/${doc.identifier}/${doc.identifier}_64kb.mp3",
                narrator = "Archive.org",
                genre = "Classic"
            )
        }
        (localMatch + pdMatch).distinctBy { it.title.lowercase().trim() }
    }

    // Filter matching Music tracks
    val combinedMusic = remember(creatorName, allMusic, pdMusic) {
        val localMatch = allMusic.filter { it.artist.contains(creatorName, ignoreCase = true) }
        val pdMatch = pdMusic.filter { doc ->
            val creatorStr = when (val creator = doc.creator) {
                is List<*> -> creator.firstOrNull()?.toString() ?: ""
                is String -> creator
                else -> ""
            }
            creatorStr.contains(creatorName, ignoreCase = true) || (doc.title ?: "").contains(creatorName, ignoreCase = true)
        }.map { doc ->
            val titleStr = doc.title ?: "Archive Music"
            MusicTrack(
                id = doc.identifier,
                title = titleStr,
                artist = creatorName,
                album = "Archive.org Classics",
                coverUrl = "https://archive.org/services/img/${doc.identifier}",
                duration = 180000L,
                serverId = "pd_server",
                streamUrl = "",
                genre = "Classics",
                trackNumber = 1
            )
        }
        (localMatch + pdMatch).distinctBy { it.title.lowercase().trim() }
    }

    val imageUrl = details["imageUrl"] ?: ""
    val roles = details["roles"] ?: "Author & Creator"
    val bio = details["bio"] ?: "Biographical details are loading..."
    val wikiLink = details["wikiLink"] ?: ""
    val imdbLink = details["imdbLink"] ?: ""
    val archiveLink = details["website"] ?: ""
    val sourceName = details["source"] ?: "Wikipedia • IMDb • Wikimedia"
    val isVerified = details["isVerified"] == "true" || imageUrl.isNotBlank() || wikiLink.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Creator", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Creator Portrait with Real Internet Photo & Backdrop Gradient
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(AccentTeal.copy(alpha = 0.35f), Color(0xFF1E293B))
                            )
                        )
                        .border(3.dp, AccentTeal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = creatorName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Creator Name
                Text(
                    text = creatorName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Roles & Era
                Text(
                    text = roles,
                    fontSize = 13.sp,
                    color = AccentTeal,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Verified Internet Bio Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isVerified) Color(0xFF0F766E).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, 
                        if (isVerified) AccentTeal.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isVerified) Icons.Default.Verified else Icons.Default.Public,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isVerified) "Verified Internet Profile" else "Digital Media Archive",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons: IMDb, Plex, Wikipedia, Archive.org, Copy
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    val finalImdbLink = if (imdbLink.isNotBlank() && imdbLink != "N/A") {
                        imdbLink
                    } else {
                        "https://www.imdb.com/find/?q=${Uri.encode(creatorName)}&s=nm"
                    }

                    // IMDb Circular Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalImdbLink))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF5C518),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("IMDb", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Black)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("IMDb", fontSize = 10.sp, color = Color.Gray)
                    }

                    // Wikipedia Circular Button
                    if (wikiLink.isNotBlank() && wikiLink != "N/A") {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(wikiLink))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .size(46.dp)
                                    .border(1.dp, SurfaceGlassBorder, CircleShape)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.MenuBook, contentDescription = "Wikipedia", tint = AccentTeal, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Wiki", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    // Archive.org Circular Button
                    val finalArchiveLink = if (archiveLink.isNotBlank() && archiveLink != "N/A") {
                        archiveLink
                    } else {
                        "https://archive.org/search.php?query=${Uri.encode(creatorName)}"
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalArchiveLink))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier
                                .size(46.dp)
                                .border(1.dp, SurfaceGlassBorder, CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CloudDownload, contentDescription = "Archive", tint = AccentTeal, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Archive", fontSize = 10.sp, color = Color.Gray)
                    }

                    // Copy Bio Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable {
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Creator Bio", "$creatorName\n\n$bio")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Biography copied to clipboard", Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {}
                            }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier
                                .size(46.dp)
                                .border(1.dp, SurfaceGlassBorder, CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Copy", fontSize = 10.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Biography Card with Real Internet Biography
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceGlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = AccentTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Biography & Legacy",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            if (isLoadingBio) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("AI Sanity Check...", fontSize = 10.sp, color = AccentTeal)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = AccentTeal,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Multi-paragraph biography text
                        val bioParagraphs = bio.split("\n\n").filter { it.isNotBlank() }
                        if (bioParagraphs.isEmpty()) {
                            Text(
                                text = bio,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        } else {
                            bioParagraphs.forEachIndexed { index, para ->
                                Text(
                                    text = para.trim(),
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                                if (index < bioParagraphs.size - 1) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Source citation inside card
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Source: $sourceName",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                            if (imageUrl.isNotBlank()) {
                                Text(
                                    text = "Verified Creator Profile",
                                    fontSize = 11.sp,
                                    color = AccentTeal.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // POPULAR TRACKS DROPDOWN (STUCK ON OPEN BY DEFAULT WITH PLAY BUTTON ON RIGHT)
                var isPopularTracksOpen by remember { mutableStateOf(true) }
                val popularTracks = remember(combinedMusic) {
                    if (combinedMusic.isNotEmpty()) {
                        combinedMusic.take(10)
                    } else {
                        emptyList()
                    }
                }

                if (popularTracks.isNotEmpty()) {
                    Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceGlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isPopularTracksOpen = !isPopularTracksOpen },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Popular Tracks (${popularTracks.size})", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                            IconButton(onClick = { isPopularTracksOpen = !isPopularTracksOpen }) {
                                Icon(
                                    if (isPopularTracksOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle Popular Tracks",
                                    tint = AccentTeal
                                )
                            }
                        }

                        if (isPopularTracksOpen) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = SurfaceGlassBorder)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Scrollable container for popular tracks
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                popularTracks.forEachIndexed { index, track ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = 0.2f))
                                            .clickable { onPlayMusicTrack(track) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentTeal, modifier = Modifier.width(24.dp))
                                            Column {
                                                Text(track.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(track.album.ifBlank { "Popular Single" }, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }

                                        // Play Button on the Right
                                        IconButton(
                                            onClick = { onPlayMusicTrack(track) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.PlayCircleFilled, contentDescription = "Play Track", tint = AccentTeal, modifier = Modifier.size(28.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ALBUMS SECTION BELOW TOP 10 SONGS (NEWEST TO OLDEST LEFT TO RIGHT)
                val albumsChronological = remember(combinedMusic) {
                    combinedMusic
                        .groupBy { it.album.ifBlank { "Singles" } }
                        .map { (albumTitle, tracks) ->
                            val yearVal = tracks.firstOrNull()?.lastPlayed ?: 0L
                            val cover = tracks.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl ?: imageUrl
                            Triple(albumTitle, yearVal, cover)
                        }
                        .sortedByDescending { it.second } // Newest to oldest!
                }

                if (albumsChronological.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Album, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Albums & Discography (Newest to Oldest)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(albumsChronological) { (albumTitle, yearVal, cover) ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                                modifier = Modifier
                                    .width(130.dp)
                                    .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(16.dp))
                                    .clickable {
                                        val firstTrack = combinedMusic.firstOrNull { it.album == albumTitle }
                                        if (firstTrack != null) onPlayMusicTrack(firstTrack)
                                    }
                            ) {
                                Column {
                                    MediaCoverArt(
                                        title = albumTitle,
                                        authorOrArtist = creatorName,
                                        coverUrl = cover,
                                        isBookAspectRatio = false,
                                        cornerRadius = 12.dp,
                                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                                    )
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(albumTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(if (yearVal > 0) "$yearVal Release" else "Album", fontSize = 10.sp, color = AccentTeal, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Movies Shelf
                if (matchedMovies.isNotEmpty()) {
                    CreatorItemShelf(
                        title = "Movies Featuring $creatorName (${matchedMovies.size})",
                        items = matchedMovies,
                        icon = Icons.Default.Movie,
                        onItemClick = { onOpenProgram(it.id, "movie") }
                    ) { item ->
                        Column(modifier = Modifier.width(115.dp)) {
                            MediaCoverArt(
                                title = item.title,
                                authorOrArtist = creatorName,
                                coverUrl = item.coverUrl,
                                isBookAspectRatio = true,
                                cornerRadius = 8.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.68f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.title,
                                maxLines = 1,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (item.year != null && item.year > 0) "${item.year}" else "Movie",
                                fontSize = 10.sp,
                                color = AccentTeal
                            )
                        }
                    }
                }

                // TV Shows Shelf
                if (matchedShows.isNotEmpty()) {
                    CreatorItemShelf(
                        title = "TV Shows Featuring $creatorName (${matchedShows.size})",
                        items = matchedShows,
                        icon = Icons.Default.Tv,
                        onItemClick = { onOpenProgram(it.id, "show") }
                    ) { item ->
                        Column(modifier = Modifier.width(115.dp)) {
                            MediaCoverArt(
                                title = item.title,
                                authorOrArtist = creatorName,
                                coverUrl = item.coverUrl,
                                isBookAspectRatio = true,
                                cornerRadius = 8.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.68f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.title,
                                maxLines = 1,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                            Text(
                                text = "${item.seasons.size} Seasons",
                                fontSize = 10.sp,
                                color = AccentIndigo
                            )
                        }
                    }
                }

                // E-Books Shelf
                CreatorItemShelf(
                    title = "E-Books & Literature (${combinedEBooks.size})",
                    items = combinedEBooks,
                    icon = Icons.Default.Book,
                    onItemClick = onReadEBook
                ) { item ->
                    Column(modifier = Modifier.width(115.dp)) {
                        MediaCoverArt(
                            title = item.title,
                            authorOrArtist = creatorName,
                            coverUrl = null,
                            isBookAspectRatio = true,
                            cornerRadius = 8.dp,
                            modifier = Modifier.fillMaxWidth().aspectRatio(0.68f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.title,
                            maxLines = 1,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }

                // Audiobooks Shelf
                CreatorItemShelf(
                    title = "Audiobooks (${combinedAudiobooks.size})",
                    items = combinedAudiobooks,
                    icon = Icons.Default.Headphones,
                    onItemClick = onPlayAudiobook
                ) { item ->
                    Column(modifier = Modifier.width(115.dp)) {
                        MediaCoverArt(
                            title = item.title,
                            authorOrArtist = creatorName,
                            coverUrl = item.coverUrl,
                            isBookAspectRatio = true,
                            cornerRadius = 8.dp,
                            modifier = Modifier.fillMaxWidth().aspectRatio(0.68f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.title,
                            maxLines = 1,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }

                // Music Tracks Shelf
                CreatorItemShelf(
                    title = "Music Tracks (${combinedMusic.size})",
                    items = combinedMusic,
                    icon = Icons.Default.MusicNote,
                    onItemClick = onPlayMusicTrack
                ) { item ->
                    Column(modifier = Modifier.width(115.dp)) {
                        MediaCoverArt(
                            title = item.title,
                            authorOrArtist = creatorName,
                            coverUrl = item.coverUrl,
                            isBookAspectRatio = false,
                            cornerRadius = 8.dp,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.title,
                            maxLines = 1,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun <T> CreatorItemShelf(
    title: String,
    items: List<T>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onItemClick: (T) -> Unit,
    cardContent: @Composable (T) -> Unit
) {
    if (items.isNotEmpty()) {
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items) { item ->
                Box(modifier = Modifier.clickable { onItemClick(item) }) {
                    cardContent(item)
                }
            }
        }
    }
}
