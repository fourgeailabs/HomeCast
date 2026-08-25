package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.navigation.navArgument
import androidx.navigation.NavType
import coil.compose.AsyncImage
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.LocalThemeMode
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

object Routes {
    const val Library = "library"
    const val Music = "music"
    const val EBooks = "ebooks"
    const val Podcasts = "podcasts"
    const val Player = "player"
    const val Discovery = "discovery"
    const val Settings = "settings"
    
    fun MediaDetail(title: String, creator: String, type: String) = "media_detail?title=${android.net.Uri.encode(title.ifEmpty { "Unknown" })}&creator=${android.net.Uri.encode(creator.ifEmpty { "Unknown" })}&type=$type"
    fun CreatorDetail(name: String) = "creator_detail?name=${android.net.Uri.encode(name.ifEmpty { "Unknown" })}"
    fun ProgramDetail(id: String, type: String) = "program_detail?id=${android.net.Uri.encode(id)}&type=${android.net.Uri.encode(type)}"
    fun SeasonDetail(showId: String, seasonNumber: Int) = "season_detail?showId=${android.net.Uri.encode(showId)}&seasonNumber=$seasonNumber"
    fun EpisodeDetail(showId: String, seasonNumber: Int, episodeNumber: Int) = "episode_detail?showId=${android.net.Uri.encode(showId)}&seasonNumber=$seasonNumber&episodeNumber=$episodeNumber"
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

    val scope = rememberCoroutineScope()
    var isPlayerSlidUp by remember { mutableStateOf(false) }
    var activeEBook by remember { mutableStateOf<EBookData?>(null) }
    var activeComic by remember { mutableStateOf<ComicData?>(null) }
    var activeVideoUrl by remember { mutableStateOf<String?>(null) }
    var activeVideoTitle by remember { mutableStateOf("") }
    var activeVideoSubtitle by remember { mutableStateOf("") }

    // Intercept back button if player or reader is expanded
    BackHandler(enabled = isPlayerSlidUp || activeEBook != null || activeComic != null || activeVideoUrl != null) {
        when {
            activeVideoUrl != null -> activeVideoUrl = null
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
                        val progressFraction = if (playbackState.duration > 0) {
                            (playbackState.currentPosition.toFloat() / playbackState.duration.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 10.dp, top = 0.dp, bottom = 4.dp)
                                .shadow(12.dp, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .background(SurfaceGlass)
                                .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(16.dp))
                                .clickable { isPlayerSlidUp = true }
                        ) {
                            // Elegant gradient seek bar attached to the top
                            val gradientColors = if (isAudiobook) {
                                listOf(AccentTeal, AccentIndigo)
                            } else {
                                listOf(AccentIndigo, AccentTeal)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(Color.White.copy(alpha = 0.1f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFraction)
                                        .fillMaxHeight()
                                        .background(Brush.horizontalGradient(gradientColors))
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 4.dp),
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

                            IconButton(onClick = { viewModel.playbackManager.togglePlayPause() }) {
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
                }

                NavigationBar(
                        containerColor = SurfaceGlass,
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
                                    popUpTo(navController.graph.startDestinationId) { saveState = false }
                                    launchSingleTop = true
                                    restoreState = false
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
                                    popUpTo(navController.graph.startDestinationId) { saveState = false }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            },
                            icon = { Icon(Icons.Default.MusicVideo, contentDescription = "Media") },
                            label = { Text("Media", maxLines = 1) },
                            colors = navColors
                        )
                        NavigationBarItem(
                            selected = isSelected(Routes.EBooks),
                            onClick = {
                                isPlayerSlidUp = false
                                navController.navigate(Routes.EBooks) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = false }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            },
                            icon = { Icon(Icons.Default.MenuBook, contentDescription = "E-Books") },
                            label = { Text("E-Books", maxLines = 1) },
                            colors = navColors
                        )
                        NavigationBarItem(
                            selected = isSelected(Routes.Podcasts),
                            onClick = {
                                isPlayerSlidUp = false
                                navController.navigate(Routes.Podcasts) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = false }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            },
                            icon = { Icon(Icons.Default.Podcasts, contentDescription = "Podcasts") },
                            label = { Text("Podcasts", maxLines = 1) },
                            colors = navColors
                        )
                        NavigationBarItem(
                            selected = isSelected(Routes.Discovery),
                            onClick = {
                                isPlayerSlidUp = false
                                navController.navigate(Routes.Discovery) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = false }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            },
                            icon = { Icon(Icons.Default.TravelExplore, contentDescription = "Discovery") },
                            label = { Text("Discover", maxLines = 1) },
                            colors = navColors
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = viewModel.initialRoute,
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
                        onNavigateToSettings = { navController.navigate(Routes.Settings) },
                        onArtistClick = { artistName ->
                            navController.navigate(Routes.CreatorDetail(artistName))
                        },
                        onOpenProgram = { id, type ->
                            navController.navigate(Routes.ProgramDetail(id, type))
                        }
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
                        onNavigateToSettings = { navController.navigate(Routes.Settings) },
                        onNavigateToDetails = { title, creator, type ->
                            navController.navigate(Routes.MediaDetail(title, creator, type))
                        },
                        onNavigateToCreator = { name ->
                            navController.navigate(Routes.CreatorDetail(name))
                        }
                    )
                }
                composable(Routes.Podcasts) {
                    PodcastsScreen(
                        viewModel = viewModel,
                        onEpisodeClick = {
                            isPlayerSlidUp = true
                        },
                        onNavigateToSettings = {
                            navController.navigate(Routes.Settings)
                        }
                    )
                }
                composable(Routes.Player) {
                    PlayerScreen(
                        viewModel = viewModel,
                        onNavigateToLibrary = { navController.navigate(Routes.Library) },
                        onCollapse = { isPlayerSlidUp = false },
                        onArtistClick = { artistName ->
                            navController.navigate(Routes.CreatorDetail(artistName))
                        }
                    )
                }
                composable(Routes.Discovery) {
                    DiscoveryScreen(
                        viewModel = viewModel,
                        onNavigateToDetails = { title, creator, type ->
                            navController.navigate(Routes.MediaDetail(title, creator, type))
                        },
                        onOpenEBook = { book ->
                            activeEBook = book
                        },
                        onOpenComic = { comic ->
                            activeComic = comic
                        },
                        onAudiobookClick = {
                            isPlayerSlidUp = true
                        },
                        onMusicClick = {
                            isPlayerSlidUp = true
                        },
                        onNavigateToSettings = {
                            navController.navigate(Routes.Settings)
                        }
                    )
                }
                composable(Routes.Settings) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onThemeToggle = onThemeToggle
                    )
                }
                composable(
                    route = "media_detail?title={title}&creator={creator}&type={type}",
                    arguments = listOf(
                        navArgument("title") { type = NavType.StringType; defaultValue = "Unknown" },
                        navArgument("creator") { type = NavType.StringType; defaultValue = "Unknown" },
                        navArgument("type") { type = NavType.StringType; defaultValue = "BOOK" }
                    )
                ) { backStackEntry ->
                    val title = backStackEntry.arguments?.getString("title") ?: ""
                    val creator = backStackEntry.arguments?.getString("creator") ?: ""
                    val type = backStackEntry.arguments?.getString("type") ?: ""
                    
                    MediaDetailScreen(
                        viewModel = viewModel,
                        title = title,
                        creator = creator,
                        type = type,
                        onBack = { navController.popBackStack() },
                        onCreatorClick = { navController.navigate(Routes.CreatorDetail(it)) },
                        onPlayReadClick = {
                            if (type == "BOOK" || type == "COMIC") {
                                val localEBook = viewModel.allEBooks.value.firstOrNull { it.title.equals(title, ignoreCase = true) }
                                val server = localEBook?.let { ebook -> viewModel.servers.value.firstOrNull { it.id == ebook.serverId } }
                                val isComic = type == "COMIC" || (localEBook?.isComic == true)

                                if (isComic) {
                                    activeComic = ComicData(
                                        id = localEBook?.id ?: title.lowercase().replace(" ", "_"),
                                        title = title,
                                        series = title,
                                        writer = creator,
                                        coverUrl = localEBook?.coverUrl ?: "https://archive.org/services/img/${title.lowercase().replace(" ", "_")}",
                                        downloadUrl = localEBook?.downloadUrl ?: "",
                                        pageCount = localEBook?.totalPages ?: 0,
                                        serverHostUrl = server?.hostUrl ?: "",
                                        serverApiKey = server?.apiKey ?: ""
                                    )
                                } else if (localEBook != null && localEBook.serverId != "demo_server" && localEBook.serverId != "pd_server") {
                                    activeEBook = EBookData(
                                        id = localEBook.id,
                                        title = localEBook.title,
                                        author = localEBook.author,
                                        totalChapters = 0,
                                        chapters = emptyList(),
                                        downloadUrl = localEBook.downloadUrl,
                                        publicDomainUrl = localEBook.downloadUrl,
                                        serverHostUrl = server?.hostUrl ?: "",
                                        serverApiKey = server?.apiKey ?: ""
                                    )
                                } else {
                                    val standardGutenbergUrls = mapOf(
                                        "The Time Machine" to "https://www.gutenberg.org/cache/epub/35/pg35.txt",
                                        "Frankenstein" to "https://www.gutenberg.org/cache/epub/84/pg84.txt",
                                        "The Art of War" to "https://www.gutenberg.org/cache/epub/132/pg132.txt",
                                        "The Great Gatsby" to "https://www.gutenberg.org/cache/epub/64317/pg64317.txt",
                                        "Metamorphosis" to "https://www.gutenberg.org/cache/epub/5200/pg5200.txt"
                                    )
                                    val matchedUrl = standardGutenbergUrls[title]
                                    if (matchedUrl != null) {
                                        activeEBook = EBookData(
                                            id = title.lowercase().replace(" ", "_"),
                                            title = title,
                                            author = creator,
                                            totalChapters = 0,
                                            chapters = emptyList(),
                                            publicDomainUrl = matchedUrl
                                        )
                                    } else {
                                        val doc = viewModel.publicDomainBooks.value.firstOrNull { (it.title ?: "").contains(title, ignoreCase = true) }
                                        val identifier = doc?.identifier ?: title.lowercase().replace(" ", "_")
                                        scope.launch {
                                            val files = com.example.data.network.ArchiveOrgClient.fetchFilesForIdentifier(identifier)
                                            val matchingTxt = files.firstOrNull { it.name.endsWith("_djvu.txt", ignoreCase = true) || it.name.endsWith(".txt", ignoreCase = true) }?.name
                                            val txtUrl = if (matchingTxt != null) {
                                                "https://archive.org/download/$identifier/$matchingTxt"
                                            } else {
                                                "https://archive.org/download/$identifier/${identifier}_djvu.txt"
                                            }
                                            activeEBook = EBookData(
                                                id = identifier,
                                                title = title,
                                                author = creator,
                                                totalChapters = 0,
                                                chapters = emptyList(),
                                                publicDomainUrl = txtUrl
                                            )
                                        }
                                    }
                                }
                            } else if (type == "AUDIOBOOK") {
                                val localAudiobook = viewModel.allBooks.value.firstOrNull { it.title.equals(title, ignoreCase = true) }
                                if (localAudiobook != null) {
                                    viewModel.playAudiobookWithResolution(localAudiobook)
                                    isPlayerSlidUp = true
                                } else {
                                    val doc = viewModel.publicDomainAudiobooks.value.firstOrNull { (it.title ?: "").contains(title, ignoreCase = true) }
                                    val id = doc?.identifier ?: title.lowercase().replace(" ", "_")
                                    val audiobook = com.example.data.Audiobook(
                                        id = id,
                                        title = title,
                                        author = creator,
                                        duration = 0L,
                                        coverUrl = "https://archive.org/services/img/$id",
                                        serverId = "pd_server",
                                        streamUrl = "https://archive.org/download/$id/${id}_64kb.mp3"
                                    )
                                    viewModel.playAudiobookWithResolution(audiobook)
                                    isPlayerSlidUp = true
                                }
                            } else if (type == "MUSIC") {
                                val localTrack = viewModel.allMusic.value.firstOrNull { it.title.equals(title, ignoreCase = true) }
                                if (localTrack != null) {
                                    viewModel.playMusicTrackWithResolution(localTrack)
                                    isPlayerSlidUp = true
                                } else {
                                    val doc = viewModel.publicDomainMusic.value.firstOrNull { (it.title ?: "").contains(title, ignoreCase = true) }
                                    val id = doc?.identifier ?: title.lowercase().replace(" ", "_")
                                    val track = com.example.data.MusicTrack(
                                        id = id,
                                        title = title,
                                        artist = creator,
                                        album = "Archive.org Classics",
                                        coverUrl = "https://archive.org/services/img/$id",
                                        duration = 180000L,
                                        serverId = "pd_server",
                                        streamUrl = ""
                                    )
                                    viewModel.playMusicTrackWithResolution(track)
                                    isPlayerSlidUp = true
                                }
                            }
                        },
                        onNavigateToDetails = { newTitle, newCreator, newType ->
                            navController.navigate(Routes.MediaDetail(newTitle, newCreator, newType))
                        }
                    )
                }
                composable(
                    route = "creator_detail?name={name}",
                    arguments = listOf(
                        navArgument("name") { type = NavType.StringType; defaultValue = "Unknown" }
                    )
                ) { backStackEntry ->
                    val name = backStackEntry.arguments?.getString("name") ?: ""
                    CreatorDetailScreen(
                        viewModel = viewModel,
                        creatorName = name,
                        onBack = { navController.popBackStack() },
                        onReadEBook = { activeEBook = it },
                        onPlayAudiobook = { book ->
                            viewModel.playAudiobookWithResolution(book)
                            isPlayerSlidUp = true
                        },
                        onPlayMusicTrack = { track ->
                            viewModel.playMusicTrackWithResolution(track)
                            isPlayerSlidUp = true
                        },
                        onOpenProgram = { id, type ->
                            navController.navigate(Routes.ProgramDetail(id, type))
                        }
                    )
                }

                composable(
                    route = "program_detail?id={id}&type={type}",
                    arguments = listOf(
                        navArgument("id") { type = NavType.StringType; defaultValue = "" },
                        navArgument("type") { type = NavType.StringType; defaultValue = "movie" }
                    )
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: ""
                    val type = backStackEntry.arguments?.getString("type") ?: "movie"
                    ProgramDetailScreen(
                        viewModel = viewModel,
                        programId = id,
                        programType = type,
                        onBack = { navController.popBackStack() },
                        onOpenSeason = { showId, seasonNum ->
                            navController.navigate(Routes.SeasonDetail(showId, seasonNum))
                        },
                        onOpenPerson = { personName ->
                            navController.navigate(Routes.CreatorDetail(personName))
                        },
                        onOpenProgram = { progId, progType ->
                            navController.navigate(Routes.ProgramDetail(progId, progType))
                        }
                    )
                }

                composable(
                    route = "season_detail?showId={showId}&seasonNumber={seasonNumber}",
                    arguments = listOf(
                        navArgument("showId") { type = NavType.StringType; defaultValue = "" },
                        navArgument("seasonNumber") { type = NavType.IntType; defaultValue = 1 }
                    )
                ) { backStackEntry ->
                    val showId = backStackEntry.arguments?.getString("showId") ?: ""
                    val seasonNumber = backStackEntry.arguments?.getInt("seasonNumber") ?: 1
                    SeasonDetailScreen(
                        viewModel = viewModel,
                        showId = showId,
                        seasonNumber = seasonNumber,
                        onBack = { navController.popBackStack() },
                        onOpenEpisode = { sId, sNum, epNum ->
                            navController.navigate(Routes.EpisodeDetail(sId, sNum, epNum))
                        },
                        onOpenPerson = { personName ->
                            navController.navigate(Routes.CreatorDetail(personName))
                        }
                    )
                }

                composable(
                    route = "episode_detail?showId={showId}&seasonNumber={seasonNumber}&episodeNumber={episodeNumber}",
                    arguments = listOf(
                        navArgument("showId") { type = NavType.StringType; defaultValue = "" },
                        navArgument("seasonNumber") { type = NavType.IntType; defaultValue = 1 },
                        navArgument("episodeNumber") { type = NavType.IntType; defaultValue = 1 }
                    )
                ) { backStackEntry ->
                    val showId = backStackEntry.arguments?.getString("showId") ?: ""
                    val seasonNumber = backStackEntry.arguments?.getInt("seasonNumber") ?: 1
                    val episodeNumber = backStackEntry.arguments?.getInt("episodeNumber") ?: 1
                    EpisodeDetailScreen(
                        viewModel = viewModel,
                        showId = showId,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        onBack = { navController.popBackStack() },
                        onOpenPerson = { personName ->
                            navController.navigate(Routes.CreatorDetail(personName))
                        },
                        onNavigateEpisode = { nextEpNum ->
                            navController.navigate(Routes.EpisodeDetail(showId, seasonNumber, nextEpNum))
                        }
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
                onCollapse = { isPlayerSlidUp = false },
                onArtistClick = { artistName ->
                    navController.navigate(Routes.CreatorDetail(artistName))
                }
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
                    onClose = { activeEBook = null },
                    viewModel = viewModel
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
                    onClose = { activeComic = null },
                    viewModel = viewModel
                )
            }
        }

        // Full-screen Video Player Dialog for Movies, Shows, and Episodes
        activeVideoUrl?.let { url ->
            com.example.ui.components.VideoPlayerDialog(
                videoUrl = url,
                title = activeVideoTitle,
                subtitle = activeVideoSubtitle,
                onDismiss = { activeVideoUrl = null }
            )
        }
    }
}
