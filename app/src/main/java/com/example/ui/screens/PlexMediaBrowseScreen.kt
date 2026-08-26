package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PlexMovieItem
import com.example.data.PlexShowItem
import com.example.data.network.PlexVideoItem
import com.example.data.RecentProgramEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.*

enum class MediaBrowseSection {
    RECOMMENDED,
    BROWSE,
    PLAYLISTS,
    CATEGORIES
}

enum class MediaDetailSubPage {
    NONE,
    RECENTLY_ADDED_TV,
    RECENTLY_ADDED_MOVIES,
    RECENT_RELEASES_TV,
    RECENT_RELEASES_MOVIES,
    CONTINUE_WATCHING,
    CATEGORY_FILTER
}

val PLEX_CATEGORIES = listOf(
    "Action" to Color(0xFF8B2635),
    "Action/Adventure" to Color(0xFF9E4733),
    "Adventure" to Color(0xFFC06C42),
    "Animation" to Color(0xFF2E6F62),
    "Anime" to Color(0xFF9C3D6E),
    "Biography" to Color(0xFF4A5859),
    "Children" to Color(0xFF386641),
    "Comedy" to Color(0xFFD4813A),
    "Crime" to Color(0xFF3D3B4E),
    "Documentary" to Color(0xFF4A6B6C),
    "Drama" to Color(0xFF6B4D57),
    "Family" to Color(0xFF437C90),
    "Fantasy" to Color(0xFF5E4973),
    "Food" to Color(0xFFA25B3E),
    "Game Show" to Color(0xFF367B63),
    "History" to Color(0xFF7A5C43),
    "Horror" to Color(0xFF4B2E39),
    "Martial Arts" to Color(0xFF8C3A27),
    "Mini-Series" to Color(0xFF3F5E78),
    "Music" to Color(0xFF734B6D),
    "Musical" to Color(0xFF944852),
    "Mystery" to Color(0xFF2C4251),
    "News" to Color(0xFF485665),
    "Reality" to Color(0xFFB3673B),
    "Romance" to Color(0xFF9C4153),
    "Sci-Fi & Fantasy" to Color(0xFF3A506B),
    "Science Fiction" to Color(0xFF1F4E5B),
    "Short" to Color(0xFF5A6650),
    "Soap" to Color(0xFF854763),
    "Sport" to Color(0xFF2C5E43),
    "Suspense" to Color(0xFF3F3B4D),
    "Talk" to Color(0xFF6A5837),
    "Talk Show" to Color(0xFF566246),
    "Thriller" to Color(0xFF6E2D3B),
    "Travel" to Color(0xFF396B75),
    "War" to Color(0xFF5E503F),
    "War & Politics" to Color(0xFF495867),
    "Western" to Color(0xFF8C5338)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlexMediaBrowseScreen(
    viewModel: MainViewModel,
    initialMediaType: String = "SHOWS", // "SHOWS" or "MOVIES"
    onOpenProgram: (id: String, type: String) -> Unit,
    onPlayVideo: (PlexVideoItem) -> Unit,
    onSwitchToMusic: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val movies by viewModel.plexMovies.collectAsState()
    val shows by viewModel.plexShows.collectAsState()
    val recentPrograms by viewModel.recentPrograms.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val isFetching by viewModel.isFetchingPlexVideos.collectAsState()

    var activeMediaType by remember { mutableStateOf(initialMediaType) } // "SHOWS", "MOVIES", "MUSIC"
    var activeTab by remember { mutableStateOf(MediaBrowseSection.RECOMMENDED) }
    var activeSubPage by remember { mutableStateOf(MediaDetailSubPage.NONE) }
    var selectedCategoryName by remember { mutableStateOf("") }

    // Top Dropdown Menu state
    var showTypeMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    var selectedSortOption by remember { mutableStateOf("Title") } // "Title", "Year", "Rating", "Recently Added"
    var selectedStatusOption by remember { mutableStateOf("All") } // "All", "Unwatched", "Watched", "In Progress"

    val connectedPlexServer = servers.firstOrNull { it.type == "plex" }
    val serverDisplayName = connectedPlexServer?.name?.ifBlank { "Collins Media Server" } ?: "Collins Media Server"

    // Sub-page Back Navigation Handler
    if (activeSubPage != MediaDetailSubPage.NONE) {
        SubPageContainer(
            subPage = activeSubPage,
            categoryName = selectedCategoryName,
            movies = movies,
            shows = shows,
            recentPrograms = recentPrograms,
            onBack = { activeSubPage = MediaDetailSubPage.NONE },
            onOpenProgram = onOpenProgram
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // TOP HEADER BAR
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Dropdown Selector for Media Library
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showTypeMenu = true }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                                .testTag("media_type_dropdown_button")
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when (activeMediaType) {
                                            "MOVIES" -> "Movies"
                                            "SHOWS" -> "TV Shows"
                                            else -> "Music"
                                        },
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Library",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = serverDisplayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showTypeMenu,
                            onDismissRequest = { showTypeMenu = false },
                            modifier = Modifier.background(SurfaceGlass)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Tv, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("TV Shows", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                },
                                onClick = {
                                    activeMediaType = "SHOWS"
                                    showTypeMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Movie, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Movies", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                },
                                onClick = {
                                    activeMediaType = "MOVIES"
                                    showTypeMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Music", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                },
                                onClick = {
                                    showTypeMenu = false
                                    onSwitchToMusic()
                                }
                            )
                        }
                    }

                    // Right Actions: Search, Cast, Watchlist, Account
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isSearchActive = !isSearchActive },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        IconButton(
                            onClick = { /* Cast dialog */ },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.Cast, contentDescription = "Cast", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        IconButton(
                            onClick = { activeSubPage = MediaDetailSubPage.CONTINUE_WATCHING },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.BookmarkBorder, contentDescription = "Watchlist", tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AccentIndigo)
                                .clickable { /* Profile info */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("P", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }

                // Optional Search Bar
                AnimatedVisibility(visible = isSearchActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search titles, genres, actors...", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentIndigo,
                            unfocusedBorderColor = SurfaceGlassBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // TAB ROW: Recommended | Browse | Playlists | Categories
                val tabs = if (activeMediaType == "MOVIES") {
                    listOf(MediaBrowseSection.RECOMMENDED, MediaBrowseSection.BROWSE, MediaBrowseSection.PLAYLISTS, MediaBrowseSection.CATEGORIES)
                } else {
                    listOf(MediaBrowseSection.RECOMMENDED, MediaBrowseSection.BROWSE, MediaBrowseSection.CATEGORIES)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEach { tab ->
                        val isSelected = activeTab == tab
                        val tabTitle = when (tab) {
                            MediaBrowseSection.RECOMMENDED -> "Recommended"
                            MediaBrowseSection.BROWSE -> "Browse"
                            MediaBrowseSection.PLAYLISTS -> "Playlists"
                            MediaBrowseSection.CATEGORIES -> "Categories"
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFF2A2E3D) else Color.Transparent,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A5568)) else null,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { activeTab = tab }
                                .testTag("tab_${tabTitle.lowercase()}")
                        ) {
                            Text(
                                text = tabTitle,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color.Gray,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // TAB CONTENT
        Box(modifier = Modifier.fillMaxSize()) {
            when (activeTab) {
                MediaBrowseSection.RECOMMENDED -> {
                    RecommendedTabView(
                        activeMediaType = activeMediaType,
                        movies = movies,
                        shows = shows,
                        recentPrograms = recentPrograms,
                        onOpenProgram = onOpenProgram,
                        onNavigateSubPage = { activeSubPage = it },
                        onNavigateCategory = { cat ->
                            selectedCategoryName = cat
                            activeSubPage = MediaDetailSubPage.CATEGORY_FILTER
                        }
                    )
                }
                MediaBrowseSection.BROWSE -> {
                    BrowseTabView(
                        activeMediaType = activeMediaType,
                        movies = movies,
                        shows = shows,
                        searchQuery = searchQuery,
                        selectedSort = selectedSortOption,
                        selectedStatus = selectedStatusOption,
                        onSortChange = { selectedSortOption = it },
                        onStatusChange = { selectedStatusOption = it },
                        onOpenProgram = onOpenProgram
                    )
                }
                MediaBrowseSection.CATEGORIES -> {
                    CategoriesTabView(
                        onSelectCategory = { cat ->
                            selectedCategoryName = cat
                            activeSubPage = MediaDetailSubPage.CATEGORY_FILTER
                        }
                    )
                }
                MediaBrowseSection.PLAYLISTS -> {
                    PlaylistsTabView(
                        movies = movies,
                        onOpenProgram = onOpenProgram
                    )
                }
            }
        }
    }
}

@Composable
fun RecommendedTabView(
    activeMediaType: String,
    movies: List<PlexMovieItem>,
    shows: List<PlexShowItem>,
    recentPrograms: List<RecentProgramEntity>,
    onOpenProgram: (id: String, type: String) -> Unit,
    onNavigateSubPage: (MediaDetailSubPage) -> Unit,
    onNavigateCategory: (String) -> Unit
) {
    val isMovies = activeMediaType == "MOVIES"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
    ) {
        // 1. CONTINUE WATCHING SECTION
        if (recentPrograms.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateSubPage(MediaDetailSubPage.CONTINUE_WATCHING) }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Continue Watching", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "View All", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(recentPrograms, key = { it.id }) { prog ->
                            RecentProgramCard(
                                program = prog,
                                onClick = { onOpenProgram(prog.id, prog.programType) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // 2. RECENTLY ADDED SECTION (CLICKABLE BUTTON HEADER NAVIGATES TO 20-ITEM PAGE)
        item {
            val title = if (isMovies) "Recently Added in Movies" else "Recently Added in TV Shows"
            val targetSubPage = if (isMovies) MediaDetailSubPage.RECENTLY_ADDED_MOVIES else MediaDetailSubPage.RECENTLY_ADDED_TV
            val itemsList = if (isMovies) {
                movies.sortedByDescending { it.addedAt }.take(20)
            } else {
                shows.sortedByDescending { it.addedAt }.take(20)
            }

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateSubPage(targetSubPage) }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("recently_added_header_button"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "View all recently added", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (itemsList.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        if (isMovies) {
                            items(itemsList.filterIsInstance<PlexMovieItem>(), key = { it.id }) { movie ->
                                MovieShelfCard(movie = movie, onClick = { onOpenProgram(movie.id, "movie") })
                            }
                        } else {
                            items(itemsList.filterIsInstance<PlexShowItem>(), key = { it.id }) { show ->
                                ShowShelfCard(show = show, onClick = { onOpenProgram(show.id, "show") })
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceGlass)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No media recently added. Syncing library...", color = Color.Gray, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 3. RECENT RELEASES SECTION (CLICKABLE BUTTON HEADER NAVIGATES TO RECENT RELEASES PAGE)
        item {
            val title = if (isMovies) "Recent Releases in Movies" else "Recent Releases in TV Shows"
            val targetSubPage = if (isMovies) MediaDetailSubPage.RECENT_RELEASES_MOVIES else MediaDetailSubPage.RECENT_RELEASES_TV
            val itemsList = if (isMovies) {
                movies.sortedByDescending { it.year ?: 0 }.take(20)
            } else {
                shows.sortedByDescending { it.year ?: 0 }.take(20)
            }

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateSubPage(targetSubPage) }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("recent_releases_header_button"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "View all recent releases", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (itemsList.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        if (isMovies) {
                            items(itemsList.filterIsInstance<PlexMovieItem>(), key = { it.id }) { movie ->
                                MovieShelfCard(movie = movie, onClick = { onOpenProgram(movie.id, "movie") })
                            }
                        } else {
                            items(itemsList.filterIsInstance<PlexShowItem>(), key = { it.id }) { show ->
                                ShowShelfCard(show = show, onClick = { onOpenProgram(show.id, "show") })
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 4. HERO BANNER
        val heroItem = if (isMovies) movies.firstOrNull { it.bannerUrl.isNotBlank() } ?: movies.firstOrNull()
                       else shows.firstOrNull { it.bannerUrl.isNotBlank() } ?: shows.firstOrNull()

        if (heroItem != null) {
            item {
                val title = if (heroItem is PlexMovieItem) heroItem.title else (heroItem as PlexShowItem).title
                val bannerUrl = if (heroItem is PlexMovieItem) heroItem.bannerUrl.ifBlank { heroItem.coverUrl } else (heroItem as PlexShowItem).bannerUrl.ifBlank { (heroItem as PlexShowItem).coverUrl }
                val summary = if (heroItem is PlexMovieItem) heroItem.summary else (heroItem as PlexShowItem).summary
                val year = if (heroItem is PlexMovieItem) heroItem.year else (heroItem as PlexShowItem).year
                val genres = if (heroItem is PlexMovieItem) heroItem.genres else (heroItem as PlexShowItem).genres
                val type = if (heroItem is PlexMovieItem) "movie" else "show"
                val id = if (heroItem is PlexMovieItem) heroItem.id else (heroItem as PlexShowItem).id

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenProgram(id, type) }
                ) {
                    AsyncImage(
                        model = bannerUrl,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f)),
                                    startY = 100f
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            title.uppercase(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val metaList = mutableListOf<String>()
                        if (year != null) metaList.add("$year")
                        if (genres.isNotEmpty()) metaList.add(genres.first())
                        Text(
                            metaList.joinToString(" • "),
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            summary,
                            fontSize = 12.sp,
                            color = Color(0xFFD1D5DB),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(42.dp)
                                .clickable { onOpenProgram(id, type) }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play Now", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 5. BROWSE CATEGORIES PILLS
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Popular Categories", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val featuredCats = listOf("Action", "Animation", "Comedy", "Crime", "Drama", "Documentary", "Sci-Fi & Fantasy", "Horror", "Thriller")
                    items(featuredCats) { cat ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceGlass,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceGlassBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onNavigateCategory(cat) }
                        ) {
                            Text(
                                text = cat,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BrowseTabView(
    activeMediaType: String,
    movies: List<PlexMovieItem>,
    shows: List<PlexShowItem>,
    searchQuery: String,
    selectedSort: String,
    selectedStatus: String,
    onSortChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onOpenProgram: (id: String, type: String) -> Unit
) {
    val isMovies = activeMediaType == "MOVIES"
    var showSortDropdown by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }

    // Filter and sort items
    val filteredMovies = remember(movies, searchQuery, selectedSort) {
        var list = movies
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            list = list.filter { it.title.lowercase().contains(q) || it.genres.any { g -> g.lowercase().contains(q) } }
        }
        when (selectedSort) {
            "Year (Newest)" -> list.sortedByDescending { it.year ?: 0 }
            "Year (Oldest)" -> list.sortedBy { it.year ?: 9999 }
            "Rating" -> list.sortedByDescending { it.rating ?: 0f }
            "Recently Added" -> list.sortedByDescending { it.addedAt }
            else -> list.sortedBy { it.title }
        }
    }

    val filteredShows = remember(shows, searchQuery, selectedSort) {
        var list = shows
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            list = list.filter { it.title.lowercase().contains(q) || it.genres.any { g -> g.lowercase().contains(q) } }
        }
        when (selectedSort) {
            "Year (Newest)" -> list.sortedByDescending { it.year ?: 0 }
            "Year (Oldest)" -> list.sortedBy { it.year ?: 9999 }
            "Rating" -> list.sortedByDescending { it.rating ?: 0f }
            "Recently Added" -> list.sortedByDescending { it.addedAt }
            else -> list.sortedBy { it.title }
        }
    }

    val totalCount = if (isMovies) filteredMovies.size else filteredShows.size

    Column(modifier = Modifier.fillMaxSize()) {
        // FILTER & SORT BAR (Matching Screenshot 1)
        Surface(
            color = Color(0xFF1E212B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Filter Pills: Type pill, Status pill, Sort pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Type Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF2E3440),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF434C5E)),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                if (isMovies) "Movies" else "TV Shows",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Status Pill (All, Unwatched, Watched)
                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2E3440),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF434C5E)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showStatusDropdown = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    selectedStatus,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = showStatusDropdown,
                            onDismissRequest = { showStatusDropdown = false },
                            modifier = Modifier.background(SurfaceGlass)
                        ) {
                            listOf("All", "Unwatched", "Watched", "In Progress").forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st, color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        onStatusChange(st)
                                        showStatusDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Sort Pill
                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2E3440),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF434C5E)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showSortDropdown = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    selectedSort,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = showSortDropdown,
                            onDismissRequest = { showSortDropdown = false },
                            modifier = Modifier.background(SurfaceGlass)
                        ) {
                            listOf("Title", "Recently Added", "Year (Newest)", "Year (Oldest)", "Rating").forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s, color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        onSortChange(s)
                                        showSortDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Right Total Count Badge with up/down indicator (e.g. "507 ⇅")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2E3440))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        "$totalCount",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.SwapVert,
                        contentDescription = "Count indicator",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 3-COLUMN POSTER GRID (Matching Screenshot 1)
        if (totalCount > 0) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isMovies) {
                    items(filteredMovies, key = { it.id }) { movie ->
                        MovieGridPosterCard(
                            movie = movie,
                            onClick = { onOpenProgram(movie.id, "movie") }
                        )
                    }
                } else {
                    items(filteredShows, key = { it.id }) { show ->
                        ShowGridPosterCard(
                            show = show,
                            onClick = { onOpenProgram(show.id, "show") }
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (isMovies) Icons.Default.Movie else Icons.Default.Tv,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No items found", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Try adjusting your search or sync settings.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun MovieGridPosterCard(
    movie: PlexMovieItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("movie_grid_item_${movie.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF181818))
        ) {
            if (movie.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = movie.coverUrl,
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Movie,
                    contentDescription = null,
                    tint = AccentIndigo,
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                )
            }

            // Top-right Year or Rating badge
            movie.year?.let { y ->
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .padding(5.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Text(
                        "$y",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = movie.title,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
            lineHeight = 15.sp
        )
        Text(
            text = if (movie.year != null) "${movie.year}" else "Movie",
            fontSize = 11.sp,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ShowGridPosterCard(
    show: PlexShowItem,
    onClick: () -> Unit
) {
    val displayEpisodeCount = if (show.leafCount > 0) show.leafCount
                              else if (show.seasons.isNotEmpty()) show.seasons.sumOf { it.episodes.size }
                              else 0

    val displaySeasonCount = if (show.childCount > 0) show.childCount
                             else if (show.seasons.isNotEmpty()) show.seasons.size
                             else 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("show_grid_item_${show.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF181818))
        ) {
            if (show.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = show.coverUrl,
                    contentDescription = show.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Tv,
                    contentDescription = null,
                    tint = AccentTeal,
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                )
            }

            // Top-right Episode Count Badge (Matching Screenshot 1 e.g. "8", "139", "103")
            if (displayEpisodeCount > 0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .padding(5.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Text(
                        "$displayEpisodeCount",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = show.title,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
            lineHeight = 15.sp
        )
        Text(
            text = if (displaySeasonCount > 1) "$displaySeasonCount seasons"
                   else if (displayEpisodeCount > 0) "$displayEpisodeCount episodes"
                   else if (show.year != null) "${show.year}"
                   else "TV Show",
            fontSize = 11.sp,
            color = Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CategoriesTabView(
    onSelectCategory: (String) -> Unit
) {
    // 2-COLUMN CATEGORY TILES (Matching Screenshots 2 & 3)
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(PLEX_CATEGORIES, key = { it.first }) { (catName, catColor) ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = catColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectCategory(catName) }
                    .testTag("category_card_$catName")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = catName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistsTabView(
    movies: List<PlexMovieItem>,
    onOpenProgram: (id: String, type: String) -> Unit
) {
    val playlists = listOf(
        "Top Rated Masterpieces" to movies.filter { (it.rating ?: 0f) >= 7.5f },
        "Recent Additions" to movies.sortedByDescending { it.addedAt }.take(15),
        "Sci-Fi Classics" to movies.filter { it.genres.any { g -> g.contains("Sci", ignoreCase = true) } },
        "Comedy Hits" to movies.filter { it.genres.any { g -> g.contains("Comedy", ignoreCase = true) } }
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
    ) {
        items(playlists) { (pName, pItems) ->
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = pName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Text(
                    text = "${pItems.size} items",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (pItems.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(pItems, key = { it.id }) { movie ->
                            MovieShelfCard(movie = movie, onClick = { onOpenProgram(movie.id, "movie") })
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceGlass)
                            .padding(12.dp)
                    ) {
                        Text("No items in this playlist yet.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Dedicated SubPage Container for "Recently Added", "Recent Releases", "Continue Watching", and "Category Items".
 * Each page fills NO MORE THAN 20 items for recently added media type as explicitly required!
 */
@Composable
fun SubPageContainer(
    subPage: MediaDetailSubPage,
    categoryName: String,
    movies: List<PlexMovieItem>,
    shows: List<PlexShowItem>,
    recentPrograms: List<RecentProgramEntity>,
    onBack: () -> Unit,
    onOpenProgram: (id: String, type: String) -> Unit
) {
    val title = when (subPage) {
        MediaDetailSubPage.RECENTLY_ADDED_TV -> "Recently Added in TV Shows"
        MediaDetailSubPage.RECENTLY_ADDED_MOVIES -> "Recently Added in Movies"
        MediaDetailSubPage.RECENT_RELEASES_TV -> "Recent Releases in TV Shows"
        MediaDetailSubPage.RECENT_RELEASES_MOVIES -> "Recent Releases in Movies"
        MediaDetailSubPage.CONTINUE_WATCHING -> "Continue Watching"
        MediaDetailSubPage.CATEGORY_FILTER -> categoryName
        MediaDetailSubPage.NONE -> ""
    }

    val subtitle = when (subPage) {
        MediaDetailSubPage.RECENTLY_ADDED_TV -> "20 Most Recent Uploads to Server"
        MediaDetailSubPage.RECENTLY_ADDED_MOVIES -> "20 Most Recent Uploads to Server"
        MediaDetailSubPage.RECENT_RELEASES_TV -> "Latest TV Show Releases"
        MediaDetailSubPage.RECENT_RELEASES_MOVIES -> "Latest Movie Releases"
        MediaDetailSubPage.CONTINUE_WATCHING -> "In-progress programs"
        MediaDetailSubPage.CATEGORY_FILTER -> "Browsing category: $categoryName"
        MediaDetailSubPage.NONE -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // TOP NAVIGATION BAR
        Surface(
            color = Color(0xFF1E212B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("subpage_back_button")) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // CONTENT GRID
        when (subPage) {
            MediaDetailSubPage.RECENTLY_ADDED_TV -> {
                // Maximum 20 most recent
                val recent20Shows = shows.sortedByDescending { it.addedAt }.take(20)
                Poster3ColumnGrid(
                    items = recent20Shows,
                    isMovie = false,
                    onOpenProgram = onOpenProgram
                )
            }
            MediaDetailSubPage.RECENTLY_ADDED_MOVIES -> {
                // Maximum 20 most recent
                val recent20Movies = movies.sortedByDescending { it.addedAt }.take(20)
                Poster3ColumnGrid(
                    items = recent20Movies,
                    isMovie = true,
                    onOpenProgram = onOpenProgram
                )
            }
            MediaDetailSubPage.RECENT_RELEASES_TV -> {
                val releasesShows = shows.sortedByDescending { it.year ?: 0 }.take(40)
                Poster3ColumnGrid(
                    items = releasesShows,
                    isMovie = false,
                    onOpenProgram = onOpenProgram
                )
            }
            MediaDetailSubPage.RECENT_RELEASES_MOVIES -> {
                val releasesMovies = movies.sortedByDescending { it.year ?: 0 }.take(40)
                Poster3ColumnGrid(
                    items = releasesMovies,
                    isMovie = true,
                    onOpenProgram = onOpenProgram
                )
            }
            MediaDetailSubPage.CONTINUE_WATCHING -> {
                if (recentPrograms.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(recentPrograms, key = { it.id }) { prog ->
                            RecentProgramCard(
                                program = prog,
                                onClick = { onOpenProgram(prog.id, prog.programType) }
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No continue watching items yet.", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
            MediaDetailSubPage.CATEGORY_FILTER -> {
                val q = categoryName.lowercase()
                val matchedShows = shows.filter { it.genres.any { g -> g.lowercase().contains(q) } }
                val matchedMovies = movies.filter { it.genres.any { g -> g.lowercase().contains(q) } }

                if (matchedShows.isNotEmpty() || matchedMovies.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 120.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(matchedShows, key = { it.id }) { show ->
                            ShowGridPosterCard(show = show, onClick = { onOpenProgram(show.id, "show") })
                        }
                        items(matchedMovies, key = { it.id }) { movie ->
                            MovieGridPosterCard(movie = movie, onClick = { onOpenProgram(movie.id, "movie") })
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No media found for category '$categoryName'", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
            MediaDetailSubPage.NONE -> {}
        }
    }
}

@Composable
fun Poster3ColumnGrid(
    items: List<Any>,
    isMovie: Boolean,
    onOpenProgram: (id: String, type: String) -> Unit
) {
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No items found.", color = Color.Gray, fontSize = 14.sp)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (isMovie) {
            items(items.filterIsInstance<PlexMovieItem>(), key = { it.id }) { movie ->
                MovieGridPosterCard(movie = movie, onClick = { onOpenProgram(movie.id, "movie") })
            }
        } else {
            items(items.filterIsInstance<PlexShowItem>(), key = { it.id }) { show ->
                ShowGridPosterCard(show = show, onClick = { onOpenProgram(show.id, "show") })
            }
        }
    }
}
