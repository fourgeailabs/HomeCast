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
fun EpisodeDetailScreen(
    viewModel: MainViewModel,
    showId: String,
    seasonNumber: Int,
    episodeNumber: Int,
    onBack: () -> Unit,
    onOpenPerson: (personName: String) -> Unit,
    onNavigateEpisode: (episodeNumber: Int) -> Unit
) {
    val shows by viewModel.plexShows.collectAsState()
    val show = remember(showId, shows) { shows.firstOrNull { it.id == showId } }
    val season = remember(show, seasonNumber) { show?.seasons?.firstOrNull { it.seasonNumber == seasonNumber } }
    val episode = remember(season, episodeNumber) { season?.episodes?.firstOrNull { it.episodeNumber == episodeNumber } }

    var isPlayingVideo by remember { mutableStateOf(false) }

    val showTitle = show?.title ?: "TV Show"
    val epTitle = episode?.title ?: "Episode $episodeNumber"
    val summary = episode?.summary.takeIf { !it.isNullOrBlank() } ?: season?.summary ?: show?.summary ?: ""
    val coverUrl = episode?.coverUrl.takeIf { !it.isNullOrBlank() } ?: season?.coverUrl ?: show?.coverUrl ?: ""
    val bannerUrl = show?.bannerUrl.takeIf { !it.isNullOrBlank() } ?: coverUrl
    val airDate = episode?.airDate ?: ""
    val duration = episode?.duration ?: 0L

    val epCast = episode?.cast.takeIf { !it.isNullOrEmpty() } ?: season?.cast ?: show?.cast ?: emptyList()
    val epDirectors = episode?.directors ?: emptyList()
    val epWriters = episode?.writers ?: emptyList()
    val epProducers = episode?.producers ?: emptyList()

    val allEpisodes = season?.episodes ?: emptyList()
    val prevEp = allEpisodes.firstOrNull { it.episodeNumber == episodeNumber - 1 }
    val nextEp = allEpisodes.firstOrNull { it.episodeNumber == episodeNumber + 1 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "S${seasonNumber}E${episodeNumber} • $epTitle",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = showTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
            // 1. Episode Hero Backdrop
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    if (coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = epTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
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

                    // Overlay Info
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = AccentIndigo,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Season $seasonNumber, Episode $episodeNumber",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            if (airDate.isNotBlank()) {
                                Text(
                                    text = airDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }

                            if (duration > 0L) {
                                val mins = duration / 60000
                                Text(
                                    text = "• ${mins} min",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Text(
                            text = epTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // 2. Play Episode Button
            if (episode != null && episode.videoUrl.isNotBlank()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Button(
                            onClick = {
                                isPlayingVideo = true
                                viewModel.recordRecentProgram(
                                    id = episode.id,
                                    programType = "EPISODE",
                                    title = episode.title,
                                    subtitle = "$showTitle • S${episode.seasonNumber}E${episode.episodeNumber}",
                                    coverUrl = coverUrl,
                                    bannerUrl = bannerUrl,
                                    duration = episode.duration,
                                    progress = episode.progress,
                                    streamUrl = episode.videoUrl,
                                    ratingKey = episode.ratingKey,
                                    serverId = episode.serverId,
                                    showTitle = showTitle,
                                    seasonNumber = episode.seasonNumber,
                                    episodeNumber = episode.episodeNumber
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
                            Text("Play Episode", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Episode Biography & Summary
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
                                text = "Episode Synopsis & Overview",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = if (summary.isNotBlank()) summary else "No synopsis available for this episode from Plex metadata.",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // 4. Granular Episode Cast & Creative Team
            if (epCast.isNotEmpty() || epDirectors.isNotEmpty() || epWriters.isNotEmpty() || epProducers.isNotEmpty()) {
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
                                text = "Episode Cast & People",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        if (epCast.isNotEmpty()) {
                            CastRowSection(
                                sectionTitle = "Actors & Characters",
                                members = epCast,
                                onPersonClick = { onOpenPerson(it.name) }
                            )
                        }

                        if (epDirectors.isNotEmpty()) {
                            CastRowSection(
                                sectionTitle = "Directed By",
                                members = epDirectors,
                                onPersonClick = { onOpenPerson(it.name) }
                            )
                        }

                        if (epWriters.isNotEmpty()) {
                            CastRowSection(
                                sectionTitle = "Written By",
                                members = epWriters,
                                onPersonClick = { onOpenPerson(it.name) }
                            )
                        }

                        if (epProducers.isNotEmpty()) {
                            CastRowSection(
                                sectionTitle = "Produced By",
                                members = epProducers,
                                onPersonClick = { onOpenPerson(it.name) }
                            )
                        }
                    }
                }
            }

            // 5. Episode Navigation (Previous / Next Episode)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (prevEp != null) {
                        OutlinedButton(
                            onClick = { onNavigateEpisode(prevEp.episodeNumber) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ep ${prevEp.episodeNumber}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    if (nextEp != null) {
                        Button(
                            onClick = { onNavigateEpisode(nextEp.episodeNumber) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo.copy(alpha = 0.25f), contentColor = AccentIndigo)
                        ) {
                            Text("Next Ep ${nextEp.episodeNumber}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }

        if (isPlayingVideo && episode != null) {
            VideoPlayerDialog(
                videoUrl = episode.videoUrl,
                title = episode.title,
                subtitle = "$showTitle • S${episode.seasonNumber}E${episode.episodeNumber}",
                onDismiss = { isPlayingVideo = false }
            )
        }
    }
}
