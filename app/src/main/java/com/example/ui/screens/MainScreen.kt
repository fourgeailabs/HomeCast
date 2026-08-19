package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.LocalThemeMode
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder
import com.example.ui.theme.TextSecondary

object Routes {
    const val Library = "library"
    const val Music = "music"
    const val EBooks = "ebooks"
    const val Player = "player"
    const val Discovery = "discovery"
    const val Settings = "settings"
    
    fun MediaDetail(title: String, creator: String, type: String) = "media_detail/${android.net.Uri.encode(title)}/${android.net.Uri.encode(creator)}/$type"
    fun CreatorDetail(name: String) = "creator_detail/${android.net.Uri.encode(name)}"

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
    val hasActiveMedia = (activeBook != null || activeTrack != null)

    var isPlayerSlidUp by remember { mutableStateOf(false) }
    var activeEBook by remember { mutableStateOf<EBookData?>(null) }
    var activeComic by remember { mutableStateOf<ComicData?>(null) }

    // Intercept back button if player or reader is expanded
    BackHandler(enabled = isPlayerSlidUp || activeEBook != null || activeComic != null) {
        when {
            activeComic != null -> activeComic = null
            activeEBook != null -> activeEBook = null
            isPlayerSlidUp -> isPlayerSlidUp = false
        }
    }

    fun isSelected(route: String): Boolean {
        return currentDestination == route && !isPlayerSlidUp && activeEBook == null && activeComic == null
    }

    val navColors = NavigationBarItemDefaults.colors(
        selectedIconColor = AccentIndigo,
        selectedTextColor = AccentIndigo,
        unselectedIconColor = TextSecondary.copy(alpha = 0.6f),
        unselectedTextColor = TextSecondary.copy(alpha = 0.6f),
        indicatorColor = Color.Transparent
    )

    val isDarkTheme = LocalThemeMode.current
    val globalBackgroundBrush = remember(isDarkTheme) {
        if (isDarkTheme) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0F172A), // Deep Slate
                    Color(0xFF1E1B4B), // Deep Indigo
                    Color(0xFF090B10)  // Deep Black
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFF8FAFC),
                    Color(0xFFE0E7FF),
                    Color(0xFFF1F5F9)
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(globalBackgroundBrush)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Column {
                    // Persistent Mini-Player when browsing shelves/hierarchy
                    AnimatedVisibility(
                        visible = hasActiveMedia && !isPlayerSlidUp,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        val mediaTitle = activeBook?.title ?: activeTrack?.title ?: "Now Playing"
                        val mediaSubtitle = activeBook?.author ?: activeTrack?.artist ?: ""
                        val mediaCover = activeBook?.coverUrl ?: activeTrack?.coverUrl ?: ""
                        val isAudiobook = activeBook != null

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .shadow(8.dp, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        if (isAudiobook) {
                                            listOf(AccentTeal.copy(alpha = 0.25f), MaterialTheme.colorScheme.surfaceVariant)
                                        } else {
                                            listOf(AccentIndigo.copy(alpha = 0.25f), MaterialTheme.colorScheme.surfaceVariant)
                                        }
                                    )
                                )
                                .clickable { isPlayerSlidUp = true }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (mediaCover.isNotBlank()) {
                                AsyncImage(
                                    model = mediaCover,
                                    contentDescription = mediaTitle,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isAudiobook) AccentTeal.copy(alpha = 0.3f) else AccentIndigo.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isAudiobook) Icons.Default.Book else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (isAudiobook) AccentTeal else AccentIndigo
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(mediaTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(mediaSubtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            IconButton(onClick = { viewModel.togglePlayPause() }) {
                                Icon(
                                    if (playbackState.isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                    tint = if (isAudiobook) AccentTeal else AccentIndigo,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            IconButton(onClick = { isPlayerSlidUp = true }) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Expand Player",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
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
                            onClick = {
                                isPlayerSlidUp = false
                                navController.navigate(Routes.Library) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Audiobooks") },
                            label = { Text("Audiobooks", maxLines = 1) },
                            colors = navColors
                        )
                        NavigationBarItem(
                            selected = isSelected(Routes.Music),
                            onClick = {
                                isPlayerSlidUp = false
                                navController.navigate(Routes.Music) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.MusicNote, contentDescription = "Music") },
                            label = { Text("Music", maxLines = 1) },
                            colors = navColors
                        )
                        NavigationBarItem(
                            selected = isSelected(Routes.EBooks),
                            onClick = {
                                isPlayerSlidUp = false
                                navController.navigate(Routes.EBooks) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.MenuBook, contentDescription = "E-Books") },
                            label = { Text("E-Books", maxLines = 1) },
                            colors = navColors
                        )
                        NavigationBarItem(
                            selected = isSelected(Routes.Discovery),
                            onClick = {
                                isPlayerSlidUp = false
                                navController.navigate(Routes.Discovery) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.TravelExplore, contentDescription = "Discovery") },
                            label = { Text("Discovery", maxLines = 1) },
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
                        onBookClick = {
                            isPlayerSlidUp = true
                        },
                        onNavigateToSettings = { navController.navigate(Routes.Settings) },
                        onNavigateToDetails = { title, creator, type ->
                            navController.navigate(Routes.MediaDetail(title, creator, type))
                        },
                        onNavigateToCreator = { name ->
                            navController.navigate(Routes.CreatorDetail(name))
                        }
                    )
                }
                composable(Routes.Music) {
                    MusicScreen(
                        viewModel = viewModel,
                        onTrackClick = {
                            isPlayerSlidUp = true
                        },
                        onNavigateToSettings = { navController.navigate(Routes.Settings) }
                    )
                }
                composable(Routes.EBooks) {
                    EBooksScreen(
                        viewModel = viewModel,
                        onOpenEBook = { book ->
                            activeEBook = book
                        },
                        onOpenComic = { comic ->
                            activeComic = comic
                        },
                        onNavigateToSettings = { navController.navigate(Routes.Settings) }
                    )
                }
                composable(Routes.Player) {
                    PlayerScreen(
                        viewModel = viewModel,
                        onNavigateToLibrary = { navController.navigate(Routes.Library) },
                        onCollapse = { isPlayerSlidUp = false }
                    )
                }
                composable(Routes.Discovery) {
                    DiscoveryScreen(
                        viewModel = viewModel,
                        onMediaSelected = { isPlayerSlidUp = true }
                    )
                }
                composable(Routes.Settings) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onThemeToggle = onThemeToggle
                    )
                }
            }
        }

        // Sliding Full-Screen Player Transition
        AnimatedVisibility(
            visible = isPlayerSlidUp,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerScreen(
                viewModel = viewModel,
                onNavigateToLibrary = { isPlayerSlidUp = false },
                onCollapse = { isPlayerSlidUp = false }
            )
        }

        // Full-screen Kindle-Like E-Reader with Paper Texture & Glass HUD
        AnimatedVisibility(
            visible = activeEBook != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            activeEBook?.let { book ->
                EReaderScreen(
                    eBook = book,
                    onClose = { activeEBook = null }
                )
            }
        }

        // Full-screen Comic & Manga Reader with Guided Smart Panel Zoom
        AnimatedVisibility(
            visible = activeComic != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            activeComic?.let { comic ->
                ComicReaderScreen(
                    comic = comic,
                    onClose = { activeComic = null }
                )
            }
        }
    }
}
