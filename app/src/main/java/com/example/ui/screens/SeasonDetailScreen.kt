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
fun SeasonDetailScreen(
    viewModel: MainViewModel,
    showId: String,
    seasonNumber: Int,
    onBack: () -> Unit,
    onOpenEpisode: (showId: String, seasonNumber: Int, episodeNumber: Int) -> Unit,
    onOpenPerson: (personName: String) -> Unit
) {
    val shows by viewModel.plexShows.collectAsState()
    val show = remember(showId, shows) { shows.firstOrNull { it.id == showId } }
    val season = remember(show, seasonNumber) { show?.seasons?.firstOrNull { it.seasonNumber == seasonNumber } }

    var activeVideoUrl by remember { mutableStateOf<String?>(null) }
    var activeVideoTitle by remember { mutableStateOf("") }
    var activeVideoSubtitle by remember { mutableStateOf("") }

    val showTitle = show?.title ?: "TV Show"
    val seasonTitle = season?.title.takeIf { !it.isNullOrBlank() } ?: "Season $seasonNumber"
    val summary = season?.summary.takeIf { !it.isNullOrBlank() } ?: show?.summary ?: ""
    val coverUrl = season?.coverUrl.takeIf { !it.isNullOrBlank() } ?: show?.coverUrl ?: ""
    val bannerUrl = show?.bannerUrl.takeIf { !it.isNullOrBlank() } ?: coverUrl
    val episodes = season?.episodes ?: emptyList()
    val seasonCast = season?.cast.takeIf { !it.isNullOrEmpty() } ?: show?.cast ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = seasonTitle,
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
            // 1. Hero Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    if (bannerUrl.isNotBlank()) {
                        AsyncImage(
                            model = bannerUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
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

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(8.dp),
                            modifier = Modifier
                                .width(95.dp)
                                .height(140.dp)
                                .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(12.dp))
                        ) {
                            if (coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = coverUrl,
                                    contentDescription = seasonTitle,
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
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = showTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentTeal
                            )
                            Text(
                                text = seasonTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${episodes.size} Episodes • Season $seasonNumber",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // 2. Season Synopsis / Biography
            if (summary.isNotBlank()) {
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
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = AccentTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Season Summary",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 20.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // 3. Episodes List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            tint = AccentIndigo,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Episodes (${episodes.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Tap to view full details & cast",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }            // 4. Episodes Rows
            items(episodes.distinctBy { it.id }) { ep ->
                EpisodeRowItem(
                    episode = ep,
                    fallbackCover = coverUrl,
                    onPlayClick = {
                        activeVideoUrl = ep.videoUrl
                        activeVideoTitle = ep.title
                        activeVideoSubtitle = "$showTitle • S${ep.seasonNumber}E${ep.episodeNumber}"
                        viewModel.recordRecentProgram(
                            id = ep.id,
                            programType = "EPISODE",
                            title = ep.title,
                            subtitle = "$showTitle • S${ep.seasonNumber}E${ep.episodeNumber}",
                            coverUrl = ep.coverUrl.ifBlank { coverUrl },
                            bannerUrl = bannerUrl,
                            duration = ep.duration,
                            progress = ep.progress,
                            streamUrl = ep.videoUrl,
                            ratingKey = ep.ratingKey,
                            serverId = ep.serverId,
                            showTitle = showTitle,
                            seasonNumber = ep.seasonNumber,
                            episodeNumber = ep.episodeNumber
                        )
                    },
                    onCardClick = {
                        onOpenEpisode(showId, ep.seasonNumber, ep.episodeNumber)
                    }
                )
            }

            // 5. Season Cast & Creative Team
            if (seasonCast.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                text = "Season Cast & Characters",
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
                            items(seasonCast.distinctBy { "${it.name}_${it.character}" }) { person ->
                                PersonAvatarCard(
                                    person = person,
                                    onClick = { onOpenPerson(person.name) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }

        // Video Player Dialog
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
fun EpisodeRowItem(
    episode: PlexEpisodeItem,
    fallbackCover: String,
    onPlayClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onCardClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(SurfaceGlassBorder, SurfaceGlassBorder)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Episode Thumbnail with Play Action Overlay
            val epCover = episode.coverUrl.ifBlank { fallbackCover }
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(75.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                if (epCover.isNotBlank()) {
                    AsyncImage(
                        model = epCover,
                        contentDescription = episode.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Play Button overlay
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .background(AccentIndigo.copy(alpha = 0.85f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Duration badge
                if (episode.duration > 0L) {
                    val mins = episode.duration / 60000
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(topStart = 4.dp),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Text(
                            text = "${mins}m",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Episode Info & Excerpt Summary
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Episode ${episode.episodeNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal
                    )
                    if (episode.airDate.isNotBlank()) {
                        Text(
                            text = episode.airDate,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (episode.summary.isNotBlank()) {
                    Text(
                        text = episode.summary,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Granular Episode Cast preview tag
                if (episode.cast.isNotEmpty() || episode.directors.isNotEmpty()) {
                    val firstPerson = episode.cast.firstOrNull() ?: episode.directors.firstOrNull()
                    if (firstPerson != null) {
                        Text(
                            text = "Featuring: ${firstPerson.name}${if (episode.cast.size > 1) " +${episode.cast.size - 1} more" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = AccentIndigo.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
