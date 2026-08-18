package com.example.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.Audiobook
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.SurfaceGlassBorder

object Routes {
    const val Library = "library"
    const val Music = "music"
    const val Player = "player"
    const val Discovery = "discovery"
    const val Settings = "settings"
}

@Composable
fun MainScreen(onThemeToggle: (Boolean) -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Library,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.Library) {
                val mockBooks = listOf(
                    Audiobook("1", "Project Hail Mary", "Andy Weir", "https://covers.openlibrary.org/b/title/Project%20Hail%20Mary-L.jpg", 10000, 5000, true, 0L, "server1", isDownloaded = true),
                    Audiobook("2", "Dark Matter", "Blake Crouch", "https://covers.openlibrary.org/b/title/Dark%20Matter-L.jpg", 8000, 0, false, 0L, "server1", isDownloaded = false),
                    Audiobook("3", "Dune", "Frank Herbert", "https://covers.openlibrary.org/b/title/Dune-L.jpg", 15000, 0, true, 0L, "server1", isDownloaded = false)
                )
                LibraryScreen(
                    books = mockBooks,
                    onBookClick = { navController.navigate(Routes.Player) }
                )
            }
            composable(Routes.Music) {
                MusicScreen()
            }
            composable(Routes.Player) {
                PlayerScreen(
                    book = Audiobook("1", "Project Hail Mary", "Andy Weir", "", 10000, 5000, true, 0L, "server1"),
                    isPlaying = false,
                    onPlayPause = {},
                    onSeek = {}
                )
            }
            composable(Routes.Discovery) {
                DiscoveryScreen()
            }
            composable(Routes.Settings) {
                SettingsScreen(onThemeToggle = onThemeToggle)
            }
        }
    }
}
