package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.data.MediaBookmark
import com.example.data.MediaProgress
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: MainViewModel,
    onNavigateToLibrary: () -> Unit = {},
    onCollapse: (() -> Unit)? = null,
    onArtistClick: (String) -> Unit = {}
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val book = playbackState.currentAudiobook
    val track = playbackState.currentMusicTrack

    if (book == null && track == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceGlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(AccentTeal.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Headphones,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text("No Media Playing", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Select an audiobook from your library or a music track from Plex to begin listening.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            onCollapse?.invoke()
                            onNavigateToLibrary()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Browse Library", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    val title = book?.title ?: track?.title ?: "Unknown Title"
    val subtitle = book?.let {
        "${it.author}${if (it.narrator.isNotBlank()) " • Narrated by ${it.narrator}" else ""}"
    } ?: track?.let {
        "${it.artist} • ${it.album}"
    } ?: ""
    val coverUrl = book?.coverUrl ?: track?.coverUrl ?: ""
    val isAudiobook = book != null

    var dominantColor by remember { mutableStateOf(if (isAudiobook) AccentTeal else AccentIndigo) }
    var vibrantColor by remember { mutableStateOf(if (isAudiobook) AccentIndigo else AccentTeal) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSleepTimerMenu by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var bookmarkNoteText by remember { mutableStateOf("") }
    var audioBookmarks by remember { mutableStateOf<List<MediaBookmark>>(emptyList()) }
    var lastSavedSpot by remember { mutableStateOf<MediaProgress?>(null) }

    val mediaId = book?.id ?: track?.id ?: ""
    fun refreshAudioBookmarks() {
        if (mediaId.isNotBlank()) {
            audioBookmarks = viewModel.getBookmarks(mediaId)
            lastSavedSpot = if (isAudiobook) viewModel.loadAudiobookProgress(mediaId) else viewModel.loadMusicProgress(mediaId)
        }
    }

    LaunchedEffect(mediaId) {
        refreshAudioBookmarks()
    }

    val context = LocalContext.current
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(coverUrl.ifBlank { null })
            .allowHardware(false)
            .build()
    )

    val imageState = painter.state
    LaunchedEffect(imageState) {
        if (imageState is AsyncImagePainter.State.Success) {
            val bitmap = imageState.result.drawable.toBitmap()
            Palette.from(bitmap).generate { palette ->
                palette?.dominantSwatch?.rgb?.let { dominantColor = Color(it) }
                palette?.vibrantSwatch?.rgb?.let { vibrantColor = Color(it) }
            }
        }
    }

    val durationMs = if (playbackState.duration > 0) playbackState.duration else 1L
    val currentPositionMs = playbackState.currentPosition.coerceIn(0L, durationMs)
    var sliderDraggingPosition by remember { mutableStateOf<Float?>(null) }

    val progressFraction = sliderDraggingPosition ?: (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

    val coroutineScope = rememberCoroutineScope()
    val dragOffsetY = remember { Animatable(0f) }

    // Full screen Frosted Glass Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragOffsetY.value.roundToInt().coerceAtLeast(0)) }
            .pointerInput(onCollapse) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            if (dragOffsetY.value > 140f) {
                                onCollapse?.invoke()
                                dragOffsetY.snapTo(0f)
                            } else {
                                dragOffsetY.animateTo(0f, spring(stiffness = 500f))
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            dragOffsetY.animateTo(0f, spring(stiffness = 500f))
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (dragAmount > 0 || dragOffsetY.value > 0) {
                            coroutineScope.launch {
                                dragOffsetY.snapTo((dragOffsetY.value + dragAmount).coerceAtLeast(0f))
                            }
                            change.consume()
                        }
                    }
                )
            }
    ) {
        // Frosted Glass Blurred Cover Background
        if (coverUrl.isNotBlank()) {
            coil.compose.AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(80.dp, edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)
            )
            // Dark glass overlay to ensure text is readable
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                dominantColor.copy(alpha = 0.6f),
                                Color(0xFF0F172A).copy(alpha = 0.85f),
                                Color(0xFF020617).copy(alpha = 0.95f)
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                dominantColor.copy(alpha = 0.45f),
                                vibrantColor.copy(alpha = 0.25f),
                                Color.Transparent,
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (onCollapse != null) {
                        IconButton(
                            onClick = onCollapse,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SurfaceGlass)
                                .border(1.dp, SurfaceGlassBorder, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Minimize Player",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Drag Indicator Pill
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                            .clickable { onCollapse?.invoke() }
                    )

                    Surface(
                        color = (if (isAudiobook) AccentTeal else AccentIndigo).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, (if (isAudiobook) AccentTeal else AccentIndigo).copy(alpha = 0.4f)),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text(
                            if (isAudiobook) "AUDIOBOOK" else "MUSIC",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            color = if (isAudiobook) AccentTeal else AccentIndigo,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Center Cover Artwork (Larger, dropped slightly, elegant glass frame & shadow)
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .shadow(24.dp, RoundedCornerShape(24.dp), ambientColor = dominantColor, spotColor = dominantColor)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceGlass)
                    .border(1.5.dp, SurfaceGlassBorder, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (coverUrl.isNotBlank()) {
                    androidx.compose.foundation.Image(
                        painter = painter,
                        contentDescription = "Cover for $title",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(dominantColor.copy(alpha = 0.6f), vibrantColor.copy(alpha = 0.5f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isAudiobook) Icons.Default.Book else Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(90.dp)
                        )
                    }
                }

                if (book?.seriesName?.isNotBlank() == true) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            book.seriesName,
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Section: Title, Seek Bar, and All Player Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title and Subtitle
                Text(
                    title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                val artistText = book?.author ?: track?.artist ?: "Unknown Artist"
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (artistText.isNotBlank() && artistText != "Unknown Artist") {
                                onCollapse?.invoke()
                                onArtistClick(artistText)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = AccentIndigo,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = "View Artist Info",
                        tint = AccentIndigo,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Seek Bar
                Slider(
                    value = progressFraction,
                    onValueChange = { sliderDraggingPosition = it },
                    onValueChangeFinished = {
                        sliderDraggingPosition?.let { fraction ->
                            viewModel.playbackManager.seekTo((fraction * durationMs).toLong())
                            sliderDraggingPosition = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = vibrantColor,
                        activeTrackColor = dominantColor,
                        inactiveTrackColor = SurfaceGlassBorder
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatTime(currentPositionMs),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        formatTime(durationMs),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Primary Controls: Previous Track, Rewind 10s, Big Play/Pause, Fast-Forward 30s, Next/Skip Track
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Track Button
                    IconButton(
                        onClick = { viewModel.playbackManager.skipPreviousTrack() },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(SurfaceGlass)
                            .border(1.dp, SurfaceGlassBorder, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Rewind 10s Button
                    IconButton(
                        onClick = { viewModel.playbackManager.skipBackward(10) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SurfaceGlass)
                            .border(1.dp, SurfaceGlassBorder, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Stop Button
                    IconButton(
                        onClick = {
                            viewModel.playbackManager.stop()
                            onCollapse?.invoke()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SurfaceGlass)
                            .border(1.dp, SurfaceGlassBorder, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop Playback",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Main Glowing Play/Pause Action Button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(16.dp, CircleShape, ambientColor = dominantColor, spotColor = vibrantColor)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(dominantColor, vibrantColor)
                                )
                            )
                            .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { viewModel.playbackManager.togglePlayPause() },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    // Fast Forward 30s Button
                    IconButton(
                        onClick = { viewModel.playbackManager.skipForward(30) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SurfaceGlass)
                            .border(1.dp, SurfaceGlassBorder, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Forward30,
                            contentDescription = "Fast Forward 30s",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Next / Skip Track Button
                    IconButton(
                        onClick = { viewModel.playbackManager.skipNextTrack() },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(SurfaceGlass)
                            .border(1.dp, SurfaceGlassBorder, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Secondary Settings Row (Speed, Sleep Timer, Favorite)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed Menu
                    Box {
                        Surface(
                            color = SurfaceGlass,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceGlassBorder),
                            modifier = Modifier.clickable { showSpeedMenu = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "${playbackState.playbackSpeed}x",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text("${speed}x") },
                                    onClick = {
                                        viewModel.playbackManager.setPlaybackSpeed(speed)
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Error indicator if any
                    if (playbackState.errorMessage != null) {
                        Text(
                            playbackState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                    }

                    // Bookmarks & Last Spot Button
                    Surface(
                        color = SurfaceGlass,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceGlassBorder),
                        modifier = Modifier.clickable {
                            refreshAudioBookmarks()
                            showBookmarksSheet = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (audioBookmarks.isNotEmpty()) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmarks",
                                modifier = Modifier.size(16.dp),
                                tint = if (audioBookmarks.isNotEmpty()) vibrantColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (audioBookmarks.isNotEmpty()) "Bookmarks (${audioBookmarks.size})" else "Bookmark",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Sleep Timer Menu
                    Box {
                        Surface(
                            color = SurfaceGlass,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceGlassBorder),
                            modifier = Modifier.clickable { showSleepTimerMenu = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = "Sleep Timer", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Timer",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showSleepTimerMenu,
                            onDismissRequest = { showSleepTimerMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("Off") }, onClick = { showSleepTimerMenu = false })
                            DropdownMenuItem(text = { Text("15 Minutes") }, onClick = { showSleepTimerMenu = false })
                            DropdownMenuItem(text = { Text("30 Minutes") }, onClick = { showSleepTimerMenu = false })
                            DropdownMenuItem(text = { Text("45 Minutes") }, onClick = { showSleepTimerMenu = false })
                            DropdownMenuItem(text = { Text("60 Minutes") }, onClick = { showSleepTimerMenu = false })
                        }
                    }
                }
            }
        }
    }

    // Auto-save audio progress on position change and screen exit
    LaunchedEffect(currentPositionMs) {
        if (mediaId.isNotBlank() && currentPositionMs > 500L) {
            val progressPct = ((currentPositionMs.toFloat() / durationMs.toFloat()) * 100).toInt().coerceIn(0, 100)
            if (isAudiobook) {
                viewModel.saveAudiobookProgress(
                    id = mediaId,
                    title = title,
                    author = subtitle,
                    positionMs = currentPositionMs,
                    durationMs = durationMs
                )
            } else {
                viewModel.saveMusicProgress(
                    id = mediaId,
                    title = title,
                    artist = subtitle,
                    positionMs = currentPositionMs,
                    durationMs = durationMs
                )
            }
        }
    }

    DisposableEffect(mediaId) {
        onDispose {
            if (mediaId.isNotBlank() && currentPositionMs > 500L) {
                if (isAudiobook) {
                    viewModel.saveAudiobookProgress(
                        id = mediaId,
                        title = title,
                        author = subtitle,
                        positionMs = currentPositionMs,
                        durationMs = durationMs
                    )
                } else {
                    viewModel.saveMusicProgress(
                        id = mediaId,
                        title = title,
                        artist = subtitle,
                        positionMs = currentPositionMs,
                        durationMs = durationMs
                    )
                }
            }
        }
    }

    // --- AUDIO BOOKMARKS & LAST SPOT MODAL BOTTOM SHEET ---
    if (showBookmarksSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBookmarksSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isAudiobook) "Audiobook Bookmarks" else "Music Track Bookmarks",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = {
                        val positionStr = formatTime(currentPositionMs)
                        val note = bookmarkNoteText.ifBlank { "Bookmark at $positionStr" }
                        val newBookmark = MediaBookmark(
                            mediaId = mediaId,
                            mediaType = if (isAudiobook) "AUDIOBOOK" else "MUSIC",
                            title = title,
                            positionMs = currentPositionMs,
                            excerpt = note,
                            createdAt = System.currentTimeMillis()
                        )
                        viewModel.saveBookmark(newBookmark)
                        bookmarkNoteText = ""
                        refreshAudioBookmarks()
                    }) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = "Add Current Time Bookmark", tint = vibrantColor)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Quick Add Bookmark Card with Note Input
                Surface(
                    color = SurfaceGlass,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceGlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Current Spot: ${formatTime(currentPositionMs)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = vibrantColor
                            )
                            Button(
                                onClick = {
                                    val positionStr = formatTime(currentPositionMs)
                                    val note = bookmarkNoteText.ifBlank { "Bookmark at $positionStr" }
                                    val newBookmark = MediaBookmark(
                                        mediaId = mediaId,
                                        mediaType = if (isAudiobook) "AUDIOBOOK" else "MUSIC",
                                        title = title,
                                        positionMs = currentPositionMs,
                                        excerpt = note,
                                        createdAt = System.currentTimeMillis()
                                    )
                                    viewModel.saveBookmark(newBookmark)
                                    bookmarkNoteText = ""
                                    refreshAudioBookmarks()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = vibrantColor),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save Spot", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bookmarkNoteText,
                            onValueChange = { bookmarkNoteText = it },
                            placeholder = { Text("Add optional note (e.g. Favorite quote)", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Last Listened Spot Quick Resume Card
                val lastSpot = lastSavedSpot
                if (lastSpot != null && lastSpot.currentPosition > 0L) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = vibrantColor.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, vibrantColor.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.playbackManager.seekTo(lastSpot.currentPosition)
                                showBookmarksSheet = false
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(vibrantColor.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null, tint = vibrantColor, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Last Listened Spot (${lastSpot.progressPercent}%)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Position: " + formatTime(lastSpot.currentPosition),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    viewModel.playbackManager.seekTo(lastSpot.currentPosition)
                                    showBookmarksSheet = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = vibrantColor),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Resume", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 3. Saved Audio Bookmarks List
                Text(
                    "Saved Bookmarks (${audioBookmarks.size})",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (audioBookmarks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No bookmarks saved for this audio yet.\nTap 'Save Spot' above to bookmark your favorite moment.",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(audioBookmarks) { bookmark ->
                            val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(bookmark.createdAt))
                            Surface(
                                color = SurfaceGlass,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceGlassBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.playbackManager.seekTo(bookmark.positionMs)
                                            showBookmarksSheet = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = vibrantColor, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    formatTime(bookmark.positionMs),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp,
                                                    color = vibrantColor
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    dateStr,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                            if (bookmark.excerpt.isNotBlank()) {
                                                Text(
                                                    bookmark.excerpt,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    IconButton(onClick = {
                                        viewModel.deleteBookmark(bookmark.id)
                                        refreshAudioBookmarks()
                                    }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Bookmark", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
