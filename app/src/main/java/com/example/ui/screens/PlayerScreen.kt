package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.data.PlaybackState
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.PlayButtonColor
import com.example.ui.theme.PlayButtonIcon
import com.example.ui.theme.SurfaceGlass
import java.util.Locale

@Composable
fun PlayerScreen(
    viewModel: MainViewModel,
    onNavigateToLibrary: () -> Unit
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val book = playbackState.currentAudiobook
    val track = playbackState.currentMusicTrack

    if (book == null && track == null) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Headphones,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Media Playing", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Select an audiobook from your library or a music track from Plex to begin listening.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToLibrary,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) {
                        Text("Browse Library")
                    }
                }
            }
        }
        return
    }

    val title = book?.title ?: track?.title ?: "Unknown"
    val subtitle = book?.let { "${it.author}${if (it.narrator.isNotBlank()) " • Narrated by ${it.narrator}" else ""}" }
        ?: track?.let { "${it.artist} • ${it.album}" } ?: ""
    val coverUrl = book?.coverUrl ?: track?.coverUrl ?: ""

    var dominantColor by remember { mutableStateOf(if (book != null) AccentTeal else AccentIndigo) }
    var vibrantColor by remember { mutableStateOf(if (book != null) AccentIndigo else AccentTeal) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSleepTimerMenu by remember { mutableStateOf(false) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(dominantColor.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.15f),
                        radius = size.width * 0.9f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(vibrantColor.copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.85f),
                        radius = size.width * 0.9f
                    )
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Cover Image Card
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(SurfaceGlass),
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
                        modifier = Modifier.fillMaxSize().background(dominantColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(title.take(1), fontSize = 96.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                if (book?.seriesName?.isNotBlank() == true) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            book.seriesName,
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Slider
            Slider(
                value = progressFraction,
                onValueChange = { sliderDraggingPosition = it },
                onValueChangeFinished = {
                    sliderDraggingPosition?.let { fraction ->
                        viewModel.seekTo((fraction * durationMs).toLong())
                        sliderDraggingPosition = null
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = vibrantColor,
                    activeTrackColor = dominantColor,
                    inactiveTrackColor = SurfaceGlass
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatTime(currentPositionMs),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatTime(durationMs),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed Button
                Box {
                    TextButton(onClick = { showSpeedMenu = true }) {
                        Text(
                            "${playbackState.playbackSpeed}x",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false }
                    ) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
                            DropdownMenuItem(
                                text = { Text("${speed}x") },
                                onClick = {
                                    viewModel.setSpeed(speed)
                                    showSpeedMenu = false
                                }
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.skipBackward(10) },
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(SurfaceGlass)
                    ) {
                        Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s")
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(PlayButtonColor),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                tint = PlayButtonIcon,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.skipForward(30) },
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(SurfaceGlass)
                    ) {
                        Icon(Icons.Default.Forward30, contentDescription = "Fast Forward 30s")
                    }
                }

                // Sleep Timer Button
                Box {
                    IconButton(onClick = { showSleepTimerMenu = true }) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "Sleep Timer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
