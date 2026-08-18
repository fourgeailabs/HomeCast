package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.Audiobook
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.PlayButtonColor
import com.example.ui.theme.PlayButtonIcon
import com.example.ui.theme.SurfaceGlass

@Composable
fun PlayerScreen(
    book: Audiobook?,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit
) {
    if (book == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No book selected")
        }
        return
    }

    var progress by remember { mutableStateOf(0.45f) }
    var showSleepTimerMenu by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableStateOf(1.0f) }

    var dominantColor by remember { mutableStateOf(AccentIndigo) }
    var vibrantColor by remember { mutableStateOf(AccentTeal) }

    val context = LocalContext.current
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(book.coverUrl.ifBlank { null }) // Use null to trigger placeholder if blank
            .allowHardware(false)
            .build()
    )

    val state = painter.state
    LaunchedEffect(state) {
        if (state is AsyncImagePainter.State.Success) {
            val bitmap = state.result.drawable.toBitmap()
            Palette.from(bitmap).generate { palette ->
                palette?.dominantSwatch?.rgb?.let { dominantColor = Color(it) }
                palette?.vibrantSwatch?.rgb?.let { vibrantColor = Color(it) }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(dominantColor.copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.1f),
                        radius = size.width * 0.9f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(vibrantColor.copy(alpha = 0.2f), Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.9f),
                        radius = size.width * 0.9f
                    )
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(SurfaceGlass),
                contentAlignment = Alignment.Center
            ) {
                if (book.coverUrl.isNotBlank()) {
                    androidx.compose.foundation.Image(
                        painter = painter,
                        contentDescription = "Cover for ${book.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(book.title.take(1), fontSize = 120.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                Box(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)).padding(12.dp)
                ) {
                     Text("Chapter 12: Erid", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(book.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("${book.author} • Narrator", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            
            Spacer(modifier = Modifier.height(24.dp))

            Slider(
                value = progress,
                onValueChange = { progress = it },
                onValueChangeFinished = { onSeek((progress * book.duration).toLong()) },
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
                Text("04:12:05", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                Text("-08:45:12", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) 
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { currentSpeed = if (currentSpeed == 1.0f) 1.25f else if (currentSpeed == 1.25f) 1.5f else 1.0f }) {
                    Text("${currentSpeed}x", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(
                        onClick = { /* TODO rewind */ },
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
                        IconButton(onClick = onPlayPause, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = PlayButtonIcon,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { /* TODO fast forward */ },
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(SurfaceGlass)
                    ) {
                        Icon(Icons.Default.Forward30, contentDescription = "Fast Forward 30s")
                    }
                }
                
                Box {
                    IconButton(onClick = { showSleepTimerMenu = true }) {
                        Icon(Icons.Default.Timer, contentDescription = "Sleep Timer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(
                        expanded = showSleepTimerMenu,
                        onDismissRequest = { showSleepTimerMenu = false }
                    ) {
                        DropdownMenuItem(text = { Text("15 Minutes") }, onClick = { showSleepTimerMenu = false })
                        DropdownMenuItem(text = { Text("30 Minutes") }, onClick = { showSleepTimerMenu = false })
                        DropdownMenuItem(text = { Text("End of Chapter") }, onClick = { showSleepTimerMenu = false })
                    }
                }
            }
        }
    }
}
