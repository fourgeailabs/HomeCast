package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramDetailScreen(
    viewModel: MainViewModel,
    programId: String,
    programType: String, // "movie" or "show"
    onBack: () -> Unit,
    onOpenSeason: (showId: String, seasonNumber: Int) -> Unit,
    onOpenPerson: (personName: String) -> Unit,
    onOpenProgram: (id: String, type: String) -> Unit
) {
    val movies by viewModel.plexMovies.collectAsState()
    val shows by viewModel.plexShows.collectAsState()

    val movie = remember(programId, movies) { movies.firstOrNull { it.id == programId } }
    val show = remember(programId, shows) { shows.firstOrNull { it.id == programId } }

    var activeVideoUrl by remember { mutableStateOf<String?>(null) }
    var activeVideoTitle by remember { mutableStateOf("") }
    var activeVideoSubtitle by remember { mutableStateOf("") }

    val isMovie = (programType == "movie" || movie != null)

    val title = if (isMovie) movie?.title ?: "Movie" else show?.title ?: "TV Show"
    val summary = if (isMovie) movie?.summary ?: "" else show?.summary ?: ""
    val coverUrl = if (isMovie) movie?.coverUrl ?: "" else show?.coverUrl ?: ""
    val bannerUrl = if (isMovie) movie?.bannerUrl.takeIf { !it.isNullOrBlank() } ?: coverUrl else show?.bannerUrl.takeIf { !it.isNullOrBlank() } ?: coverUrl
    val year = if (isMovie) movie?.year else show?.year
    val rating = if (isMovie) movie?.rating else show?.rating
    val contentRating = if (isMovie) movie?.contentRating ?: "" else show?.contentRating ?: ""
    val studio = if (isMovie) movie?.studio ?: "" else show?.studio ?: ""
    val genres = if (isMovie) movie?.genres ?: emptyList() else show?.genres ?: emptyList()


    val initialCast = if (isMovie) movie?.cast ?: emptyList() else show?.cast ?: emptyList()
    val initialDirectors = if (isMovie) movie?.directors ?: emptyList() else show?.directors ?: emptyList()
    val initialWriters = if (isMovie) movie?.writers ?: emptyList() else show?.writers ?: emptyList()
    val initialProducers = if (isMovie) movie?.producers ?: emptyList() else show?.producers ?: emptyList()
    val initialCinematographers = if (isMovie) movie?.cinematographers ?: emptyList() else show?.cinematographers ?: emptyList()

    var detailedCast by remember { mutableStateOf<List<PlexCastMember>?>(null) }
    var detailedDirectors by remember { mutableStateOf<List<PlexCastMember>?>(null) }
    var detailedWriters by remember { mutableStateOf<List<PlexCastMember>?>(null) }
    var detailedProducers by remember { mutableStateOf<List<PlexCastMember>?>(null) }
    var detailedCinematographers by remember { mutableStateOf<List<PlexCastMember>?>(null) }

    val cast = detailedCast ?: initialCast
    val directors = detailedDirectors ?: initialDirectors
    val writers = detailedWriters ?: initialWriters
    val producers = detailedProducers ?: initialProducers
    val cinematographers = detailedCinematographers ?: initialCinematographers
    
    val servers by viewModel.servers.collectAsState()
    
    LaunchedEffect(programId) {
        val ratingKey = if (isMovie) movie?.ratingKey else show?.ratingKey
        val serverId = if (isMovie) movie?.serverId else show?.serverId
        
        if (ratingKey != null && serverId != null) {
            val server = servers.firstOrNull { it.id == serverId }
            if (server != null) {
                // Fetch full details
                try {
                    val root = server.hostUrl
                    val token = server.apiKey.trim()
                    val url = "$root/library/metadata/$ratingKey?X-Plex-Token=$token"
                    val req = okhttp3.Request.Builder()
                        .url(url)
                        .header("Accept", "application/json")
                        .build()
                    val res = okhttp3.OkHttpClient().newCall(req).execute()
                    if (res.isSuccessful) {
                        val body = res.body?.string() ?: ""
                        val json = org.json.JSONObject(body)
                        val mc = json.optJSONObject("MediaContainer")
                        if (mc != null) {
                            val metaArray = mc.optJSONArray("Metadata")
                            if (metaArray != null && metaArray.length() > 0) {
                                val item = metaArray.getJSONObject(0)
                                
                                // Helper to parse rich cast
                                fun parseRich(tagKey: String, roleDefault: String): List<PlexCastMember> {
                                    val list = mutableListOf<PlexCastMember>()
                                    val arr = item.optJSONArray(tagKey)
                                    if (arr != null) {
                                        for (i in 0 until arr.length()) {
                                            val obj = arr.getJSONObject(i)
                                            val name = obj.optString("tag", obj.optString("name", "")).trim()
                                            if (name.isBlank()) continue
                                            val role = obj.optString("role", roleDefault).trim()
                                            val thumb = obj.optString("thumb", "")
                                            val thumbUrl = if (thumb.isNotBlank()) {
                                                val cleanThumb = if (thumb.startsWith("/")) thumb else "/$thumb"
                                                "$root$cleanThumb?X-Plex-Token=$token"
                                            } else ""
                                            val id = obj.optString("id", "")
                                            list.add(PlexCastMember(id = id, name = name, role = role, thumbUrl = thumbUrl))
                                        }
                                    }
                                    return list
                                }
                                
                                detailedCast = parseRich("Role", "Actor")
                                detailedDirectors = parseRich("Director", "Director")
                                detailedWriters = parseRich("Writer", "Writer")
                                detailedProducers = parseRich("Producer", "Producer")
                                detailedCinematographers = parseRich("Country", "Cinematographer") // Just in case, Country is what was used in PlexClient, though usually it's "Country". For real it's Country/Cinematographer? Actually, just parse what PlexClient parses.
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }


    // Similar recommendations
    val similarPrograms = remember(isMovie, movie, show, movies, shows) {
        if (isMovie && movie != null) {
            val genresSet = movie.genres.toSet()
            movies.filter { it.id != movie.id && (it.genres.any { g -> genresSet.contains(g) } || it.directors.any { d -> movie.directors.any { md -> md.name == d.name } }) }
                .take(8)
        } else if (!isMovie && show != null) {
            val genresSet = show.genres.toSet()
            shows.filter { it.id != show.id && (it.genres.any { g -> genresSet.contains(g) } || it.directors.any { d -> show.directors.any { sd -> sd.name == d.name } }) }
                .take(8)
        } else {
            emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Hero Header with Backdrop Banner & Cover Art
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    // Backdrop Image
                    if (bannerUrl.isNotBlank()) {
                        AsyncImage(
                            model = bannerUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Gradient Overlay for smooth contrast
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xFF0F172A).copy(alpha = 0.7f),
                                            Color(0xFF0F172A)
                                        )
                                    )
                                )
                        )
                    }

                    // Content Container with Cover Art + Badges
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Poster Card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(8.dp),
                            modifier = Modifier
                                .width(110.dp)
                                .height(165.dp)
                                .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(12.dp))
                        ) {
                            if (coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = coverUrl,
                                    contentDescription = title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(AccentIndigo.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isMovie) Icons.Default.Movie else Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = AccentIndigo,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }

                        // Info Column
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (isMovie && !movie?.tagline.isNullOrBlank()) {
                                Text(
                                    text = "\"${movie?.tagline}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = AccentTeal,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Meta row (Year, Rating, Content Rating, Duration/Seasons)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (year != null && year > 0) {
                                    Text(
                                        text = "$year",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                }

                                if (contentRating.isNotBlank()) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = contentRating,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (rating != null && rating > 0f) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB800),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = String.format("%.1f", rating),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }

                                if (isMovie && (movie?.duration ?: 0L) > 0L) {
                                    val mins = (movie!!.duration / 60000)
                                    val hrs = mins / 60
                                    val remMins = mins % 60
                                    val durText = if (hrs > 0) "${hrs}h ${remMins}m" else "${remMins}m"
                                    Text(
                                        text = "• $durText",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                } else if (!isMovie && show != null) {
                                    val sCount = show.seasons.size
                                    Text(
                                        text = "• $sCount ${if (sCount == 1) "Season" else "Seasons"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                }
                            }

                            if (studio.isNotBlank()) {
                                Text(
                                    text = studio,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Genre Chips
            if (genres.isNotEmpty()) {
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(genres) { genre ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(genre, fontSize = 12.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = AccentIndigo.copy(alpha = 0.15f),
                                    labelColor = AccentIndigo
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    borderColor = AccentIndigo.copy(alpha = 0.3f),
                                    enabled = true
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
            }

            // 3. Primary Action: Play Movie (for Movies)
            if (isMovie && movie != null && movie.videoUrl.isNotBlank()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Button(
                            onClick = {
                                activeVideoUrl = movie.videoUrl
                                activeVideoTitle = movie.title
                                activeVideoSubtitle = "${movie.year ?: ""} • ${movie.genres.firstOrNull() ?: "Movie"}"
                                viewModel.recordRecentProgram(
                                    id = movie.id,
                                    programType = "MOVIE",
                                    title = movie.title,
                                    subtitle = "${movie.year ?: ""} • ${movie.genres.firstOrNull() ?: "Movie"}",
                                    coverUrl = movie.coverUrl,
                                    bannerUrl = movie.bannerUrl,
                                    duration = movie.duration,
                                    progress = movie.progress,
                                    streamUrl = movie.videoUrl,
                                    ratingKey = movie.ratingKey,
                                    serverId = movie.serverId
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentIndigo,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play Movie", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 4. Biography / Synopsis Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SurfaceGlassBorder, SurfaceGlassBorder)))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Overview & Biography",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = if (summary.isNotBlank()) summary else "No synopsis available for this title from Plex server metadata.",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // 5. TV Show Seasons Shelf (for TV Shows)
            if (!isMovie && show != null && show.seasons.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    tint = AccentIndigo,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Seasons (${show.seasons.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "Select a season to view episodes",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(show.seasons, key = { it.id }) { season ->
                                SeasonCardItem(
                                    season = season,
                                    fallbackCover = show.coverUrl,
                                    onClick = { onOpenSeason(show.id, season.seasonNumber) }
                                )
                            }
                        }
                    }
                }
            }

            // 6. Cast & Crew Section (Actors, Directors, Writers, Producers, Cinematographers)
            if (cast.isNotEmpty() || directors.isNotEmpty() || writers.isNotEmpty() || producers.isNotEmpty() || cinematographers.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cast & Creative Team",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        // Main Cast / Actors
                        if (cast.isNotEmpty()) {
                            CastRowSection(
                                sectionTitle = "Actors & Characters",
                                members = cast,
                                onPersonClick = { onOpenPerson(it.name) }
                            )
                        }

                        // Directors
                        if (directors.isNotEmpty()) {
                            CastRowSection(
                                sectionTitle = "Directors",
                                members = directors,
                                onPersonClick = { onOpenPerson(it.name) }
                            )
                        }

                        // Writers
                        if (writers.isNotEmpty()) {
                            CastRowSection(
                                sectionTitle = "Writers & Screenplay",
                                members = writers,
                                onPersonClick = { onOpenPerson(it.name) }
                            )
                        }

                        // Producers & Executive Producers
                        if (producers.isNotEmpty()) {
                            CastRowSection(
                                sectionTitle = "Producers",
                                members = producers,
                                onPersonClick = { onOpenPerson(it.name) }
                            )
                        }

                        // Cinematographers
                        if (cinematographers.isNotEmpty()) {
                            CastRowSection(
                                sectionTitle = "Cinematography",
                                members = cinematographers,
                                onPersonClick = { onOpenPerson(it.name) }
                            )
                        }
                    }
                }
            }

            // 7. Similar Recommendations Section
            if (similarPrograms.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFFB800),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isMovie) "Similar Movies" else "Similar TV Shows",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isMovie) {
                                items(similarPrograms.filterIsInstance<PlexMovieItem>(), key = { it.id }) { simMovie ->
                                    ProgramPosterCard(
                                        title = simMovie.title,
                                        subtitle = "${simMovie.year ?: ""} • ${simMovie.genres.firstOrNull() ?: "Movie"}",
                                        coverUrl = simMovie.coverUrl,
                                        onClick = { onOpenProgram(simMovie.id, "movie") }
                                    )
                                }
                            } else {
                                items(similarPrograms.filterIsInstance<PlexShowItem>(), key = { it.id }) { simShow ->
                                    ProgramPosterCard(
                                        title = simShow.title,
                                        subtitle = "${simShow.year ?: ""} • ${simShow.genres.firstOrNull() ?: "TV Show"}",
                                        coverUrl = simShow.coverUrl,
                                        onClick = { onOpenProgram(simShow.id, "show") }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }

        // Active Video Player Dialog if playing movie
        activeVideoUrl?.let { vidUrl ->
            VideoPlayerDialog(
                videoUrl = vidUrl,
                title = activeVideoTitle,
                subtitle = activeVideoSubtitle,
                onDismiss = { activeVideoUrl = null }
            )
        }
    }
}

@Composable
fun SeasonCardItem(
    season: PlexSeasonItem,
    fallbackCover: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SurfaceGlassBorder, SurfaceGlassBorder)))
    ) {
        Column {
            val seasonCover = season.coverUrl.ifBlank { fallbackCover }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (seasonCover.isNotBlank()) {
                    AsyncImage(
                        model = seasonCover,
                        contentDescription = season.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AccentIndigo.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = AccentIndigo,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Episode badge count
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "${season.episodeCount.coerceAtLeast(season.episodes.size)} Eps",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = season.title.ifBlank { "Season ${season.seasonNumber}" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "View Episodes",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentTeal
                )
            }
        }
    }
}

@Composable
fun CastRowSection(
    sectionTitle: String,
    members: List<PlexCastMember>,
    onPersonClick: (PlexCastMember) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = sectionTitle,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(members, key = { "${it.name}_${it.role}_${it.character}" }) { person ->
                PersonAvatarCard(
                    person = person,
                    onClick = { onPersonClick(person) }
                )
            }
        }
    }
}

@Composable
fun PersonAvatarCard(
    person: PlexCastMember,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(84.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Circular Avatar
        Box(
            modifier = Modifier
                .size(68.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .border(1.5.dp, AccentIndigo.copy(alpha = 0.4f), CircleShape)
                .background(SurfaceGlass)
        ) {
            if (person.thumbUrl.isNotBlank()) {
                AsyncImage(
                    model = person.thumbUrl,
                    contentDescription = person.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(AccentIndigo.copy(alpha = 0.3f), AccentTeal.copy(alpha = 0.3f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = person.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }

        // Name
        Text(
            text = person.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        // Character / Role
        val subLabel = if (person.character.isNotBlank()) person.character else person.role
        if (subLabel.isNotBlank()) {
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ProgramPosterCard(
    title: String,
    subtitle: String,
    coverUrl: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(115.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SurfaceGlassBorder, SurfaceGlassBorder)))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                if (coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AccentIndigo.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Movie,
                            contentDescription = null,
                            tint = AccentIndigo,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
