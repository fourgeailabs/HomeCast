package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder
import com.example.ui.theme.TextSecondary

object Routes {
    const val Library = "library"
    const val Music = "music"
    const val Player = "player"
    const val Discovery = "discovery"
    const val Settings = "settings"
}

@Composable
fun MainScreen(
    onThemeToggle: (Boolean) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val playbackState by viewModel.playbackState.collectAsState()
    val activeBook = playbackState.currentAudiobook
    val activeTrack = playbackState.currentMusicTrack
    val hasActiveMedia = (activeBook != null || activeTrack != null) && currentDestination != Routes.Player

    fun isSelected(route: String): Boolean {
        return currentDestination == route
    }

    val navColors = NavigationBarItemDefaults.colors(
        selectedIconColor = AccentIndigo,
        selectedTextColor = AccentIndigo,
        unselectedIconColor = TextSecondary.copy(alpha = 0.6f),
        unselectedTextColor = TextSecondary.copy(alpha = 0.6f),
        indicatorColor = Color.Transparent
    )

    Scaffold(
        bottomBar = {
            Column {
                // Persistent Mini-Player when navigating between screens
                AnimatedVisibility(
                    visible = hasActiveMedia,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    val mediaTitle = activeBook?.title ?: activeTrack?.title ?: "Playing"
                    val mediaSubtitle = activeBook?.author ?: activeTrack?.artist ?: ""
                    val mediaCover = activeBook?.coverUrl ?: activeTrack?.coverUrl ?: ""

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { navController.navigate(Routes.Player) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (mediaCover.isNotBlank()) {
                            AsyncImage(
                                model = mediaCover,
                                contentDescription = mediaTitle,
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentIndigo.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (activeBook != null) Icons.Default.Book else Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = AccentIndigo
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(mediaTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                            Text(mediaSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
                        }

                        IconButton(onClick = { viewModel.togglePlayPause() }) {
                            Icon(
                                if (playbackState.isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                tint = AccentIndigo,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                    modifier = Modifier.drawBehind {
                        drawLine(
                            color = SurfaceGlassBorder,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1f
                        )
                    }
                ) {
                    NavigationBarItem(
                        selected = isSelected(Routes.Library),
                        onClick = { navController.navigate(Routes.Library) {
                            popUpTo(Routes.Library) { inclusive = true }
                        } },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Audiobooks") },
                        label = { Text("Audiobooks", maxLines = 1) },
                        colors = navColors
                    )
                    NavigationBarItem(
                        selected = isSelected(Routes.Music),
                        onClick = { navController.navigate(Routes.Music) {
                            popUpTo(Routes.Library)
                        } },
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = "Music") },
                        label = { Text("Music", maxLines = 1) },
                        colors = navColors
                    )
                    NavigationBarItem(
                        selected = isSelected(Routes.Player),
                        onClick = { navController.navigate(Routes.Player) {
                            popUpTo(Routes.Library)
                        } },
                        icon = { Icon(Icons.Default.PlayCircle, contentDescription = "Player") },
                        label = { Text("Player", maxLines = 1) },
                        colors = navColors
                    )
                    NavigationBarItem(
                        selected = isSelected(Routes.Discovery),
                        onClick = { navController.navigate(Routes.Discovery) {
                            popUpTo(Routes.Library)
                        } },
                        icon = { Icon(Icons.Default.TravelExplore, contentDescription = "Discovery") },
                        label = { Text("Discovery", maxLines = 1) },
                        colors = navColors
                    )
                    NavigationBarItem(
                        selected = isSelected(Routes.Settings),
                        onClick = { navController.navigate(Routes.Settings) {
                            popUpTo(Routes.Library)
                        } },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", maxLines = 1) },
                        colors = navColors
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Library,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.Library) {
                LibraryScreen(
                    viewModel = viewModel,
                    onBookClick = { navController.navigate(Routes.Player) },
                    onNavigateToSettings = { navController.navigate(Routes.Settings) }
                )
            }
            composable(Routes.Music) {
                MusicScreen(
                    viewModel = viewModel,
                    onTrackClick = { navController.navigate(Routes.Player) },
                    onNavigateToSettings = { navController.navigate(Routes.Settings) }
                )
            }
            composable(Routes.Player) {
                PlayerScreen(
                    viewModel = viewModel,
                    onNavigateToLibrary = { navController.navigate(Routes.Library) }
                )
            }
            composable(Routes.Discovery) {
                DiscoveryScreen(viewModel = viewModel)
            }
            composable(Routes.Settings) {
                SettingsScreen(
                    viewModel = viewModel,
                    onThemeToggle = onThemeToggle
                )
            }
        }
    }
}
