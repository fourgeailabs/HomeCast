package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.border
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Audiobook
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    onBookClick: (Audiobook) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDetails: (String, String, String) -> Unit = {_,_,_->},
    onNavigateToCreator: (String) -> Unit = {}
) {
    val allBooks by viewModel.allBooks.collectAsState()
    val archiveAudiobooks by viewModel.publicDomainAudiobooks.collectAsState()
    val resolvedDurations by viewModel.resolvedDurations.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val recents by viewModel.recents.collectAsState()
    val servers by viewModel.servers.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var isGridView by remember { mutableStateOf(false) }
    var selectedCollection by remember { mutableStateOf<Pair<String, List<com.example.data.Audiobook>>?>(null) }
    var selectedSource by remember { mutableIntStateOf(viewModel.initialAudiobooksSource) } // 0 = Personal, 1 = Public Domain

    val publicDomainAudiobooks = remember(archiveAudiobooks, resolvedDurations) {
        archiveAudiobooks.map { doc ->
            val coverUrl = "https://archive.org/services/img/${doc.identifier}"
            val title = doc.title ?: "Unknown Title"
            val author = when (doc.creator) {
                is List<*> -> (doc.creator as List<*>).firstOrNull()?.toString() ?: "Unknown Author"
                is String -> doc.creator
                else -> "Unknown Author"
            }
            com.example.data.Audiobook(
                id = doc.identifier,
                title = title,
                author = author,
                duration = resolvedDurations[doc.identifier] ?: 0L,
                coverUrl = coverUrl,
                serverId = "pd_server",
                streamUrl = "https://archive.org/download/${doc.identifier}/${doc.identifier}_64kb.mp3", // best effort
                narrator = "Archive.org",
                genre = "Classic",
            )
        }
    }


    val currentBooks = remember(allBooks, publicDomainAudiobooks, selectedSource) {
        if (selectedSource == 0) {
            allBooks.filter { it.serverId != "demo_server" && it.serverId != "pd_server" }
        } else {
            val localPD = allBooks.filter { it.serverId == "demo_server" || it.serverId == "pd_server" }
            val fetched = publicDomainAudiobooks.filter { f -> localPD.none { l -> l.title.equals(f.title, ignoreCase = true) } }
            localPD + fetched
        }
    }

    val genreList = remember(currentBooks) {
        val extracted = currentBooks.map { it.genre.trim() }.filter { it.isNotBlank() }.distinct()
        if (extracted.isNotEmpty()) listOf("All") + extracted else listOf("All", "Sci-Fi", "Fantasy", "Mystery", "Non-Fiction", "Classics")
    }


    val newArrivals = currentBooks.sortedByDescending { it.id }.take(10)
    val popularBooks = currentBooks.shuffled().take(5)
    val noteworthyBooks = currentBooks.filter { it.genre == "Classics" || it.genre == "Sci-Fi" }
    val seriesBooks = currentBooks.filter { it.genre == "Fantasy" || it.genre == "Sci-Fi" }

    val filteredBooks = remember(currentBooks, searchQuery, selectedGenre) {
        currentBooks.filter { book ->
            val matchesGenre = selectedGenre == null || selectedGenre == "All" || book.genre.equals(selectedGenre, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() || book.title.contains(searchQuery, ignoreCase = true) || book.author.contains(searchQuery, ignoreCase = true)
            matchesGenre && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Audiobooks",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = Color.White
                )
                Text(
                    if (currentBooks.isNotEmpty()) "${currentBooks.size} titles in your bookshelf" else "Your Audiobookshelf Library",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { isGridView = !isGridView },
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceGlass, CircleShape)
                ) {
                    Icon(
                        if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle View",
                        tint = AccentTeal
                    )
                }
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceGlass, CircleShape)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AccentTeal)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedSource == 0,
                onClick = { selectedSource = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Personal Library")
            }
            SegmentedButton(
                selected = selectedSource == 1,
                onClick = { selectedSource = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Public Domain")
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search title, author, series, narrator...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = AccentTeal) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceGlass,
                unfocusedContainerColor = SurfaceGlass,
                focusedBorderColor = AccentTeal.copy(alpha = 0.8f),
                unfocusedBorderColor = SurfaceGlassBorder
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Genre Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            genreList.forEach { genre ->
                val isSelected = (selectedGenre == genre) || (selectedGenre == null && genre == "All")
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedGenre = if (genre == "All") null else genre
                    },
                    label = { Text(genre, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentTeal.copy(alpha = 0.25f),
                        selectedLabelColor = AccentTeal
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) AccentTeal else SurfaceGlassBorder
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3-COLUMN CHOICES VIEW WHEN GENRE SELECTED OR GRID TOGGLED OR SEARCH ACTIVE
        if (selectedCollection != null) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    IconButton(onClick = { selectedCollection = null }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text(selectedCollection!!.first, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(selectedCollection!!.second, key = { it.id }) { book ->
                        Audiobook3ColumnCard(
                            book = book,
                            isPlaying = playbackState.currentAudiobook?.id == book.id && playbackState.isPlaying,
                            onClick = {
                                viewModel.playAudiobookWithResolution(book)
                                onBookClick(book)
                            }
                        )
                    }
                }
            }
        } else if (selectedGenre != null || isGridView || searchQuery.isNotBlank()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${selectedGenre ?: if (searchQuery.isNotBlank()) "Search Results" else "All Titles"} (${filteredBooks.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedGenre != null) {
                        TextButton(onClick = { selectedGenre = null }) {
                            Text("Show Shelves", color = AccentTeal, fontSize = 12.sp)
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredBooks, key = { it.id }) { book ->
                        Audiobook3ColumnCard(
                            book = book,
                            isPlaying = playbackState.currentAudiobook?.id == book.id && playbackState.isPlaying,
                            onClick = {
                                viewModel.playAudiobookWithResolution(book)
                                onBookClick(book)
                            }
                        )
                    }
                } // Ends LazyVerticalGrid
            } // Ends Column
        } else if (currentBooks.isEmpty()) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(AccentTeal.copy(alpha = 0.3f), AccentIndigo.copy(alpha = 0.3f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (selectedSource == 0) Icons.Default.AutoStories else Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        if (selectedSource == 0) "Your Bookshelf is Empty" else "No Public Domain Audiobooks",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (selectedSource == 0) {
                            "Connect your personal Audiobookshelf server to sync and stream your full library on sliding shelves."
                        } else {
                            "Checking public domain archives. Please ensure you have an active internet connection."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    if (selectedSource == 0) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateToSettings,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect Audiobookshelf", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            // Horizontal sliding shelves
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

            // 1. Shelf: Continue Listening
            if (recents.isNotEmpty()) {
                item {
                    ShelfHeader(
                        title = "Continue Listening",
                        subtitle = "Pick up where you left off",
                        icon = Icons.Default.PlayCircleFilled,
                        iconTint = AccentTeal
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(recents, key = { "recent_${it.id}" }) { book ->
                            AudiobookShelfCard(
                                book = book,
                                showProgress = true,
                                isPlaying = playbackState.currentAudiobook?.id == book.id && playbackState.isPlaying,
                                onClick = {
                                    viewModel.playAudiobookWithResolution(book)
                                    onBookClick(book)
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(book) }
                            )
                        }
                    }
                }
            }

            // 2. Shelf: New & Recently Added
            if (newArrivals.isNotEmpty()) {
                item {
                    ShelfHeader(
                        title = "New Arrivals",
                        subtitle = "Freshly added to your library",
                        badge = "NEW",
                        icon = Icons.Default.NewReleases,
                        iconTint = Color(0xFFFFB74D)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(newArrivals, key = { "new_${it.id}" }) { book ->
                            AudiobookShelfCard(
                                book = book,
                                tag = "New",
                                onClick = {
                                    viewModel.playAudiobookWithResolution(book)
                                    onBookClick(book)
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(book) }
                            )
                        }
                    }
                }
            }

            // 3. Shelf: Popular & Regional Highlights
            if (popularBooks.isNotEmpty()) {
                item {
                    ShelfHeader(
                        title = "Popular & Trending",
                        subtitle = "Most engaging listens in your region",
                        icon = Icons.Default.TrendingUp,
                        iconTint = AccentIndigo
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(popularBooks, key = { "pop_${it.id}" }) { book ->
                            AudiobookShelfCard(
                                book = book,
                                tag = "Popular",
                                onClick = {
                                    viewModel.playAudiobookWithResolution(book)
                                    onBookClick(book)
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(book) }
                            )
                        }
                    }
                }
            }

            // 4. Shelf: Noteworthy & Editor's Choice
            if (noteworthyBooks.isNotEmpty()) {
                item {
                    ShelfHeader(
                        title = "Noteworthy Masterpieces",
                        subtitle = "Critically acclaimed & memorable narratives",
                        icon = Icons.Default.Star,
                        iconTint = Color(0xFFFFD54F)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(noteworthyBooks, key = { "noteworthy_${it.id}" }) { book ->
                            AudiobookShelfCard(
                                book = book,
                                onClick = {
                                    viewModel.playAudiobookWithResolution(book)
                                    onBookClick(book)
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(book) }
                            )
                        }
                    }
                }
            }

            // 5. Shelf: Series & Epic Sagas
            if (seriesBooks.isNotEmpty()) {
                item {
                    ShelfHeader(
                        title = "Series & Sagas",
                        subtitle = "Multi-volume sagas and deep universes",
                        icon = Icons.Default.Bookmarks,
                        iconTint = AccentTeal
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(seriesBooks, key = { "series_${it.id}" }) { book ->
                            AudiobookShelfCard(
                                book = book,
                                tag = book.seriesName.take(16),
                                onClick = {
                                    viewModel.playAudiobookWithResolution(book)
                                    onBookClick(book)
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(book) }
                            )
                        }
                    }
                }
            }

            // 6. Shelf: Favorites
            if (favorites.isNotEmpty()) {
                item {
                    ShelfHeader(
                        title = "Your Favorites",
                        subtitle = "Pinned audiobooks",
                        icon = Icons.Default.Favorite,
                        iconTint = Color(0xFFEF5350)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(favorites, key = { "fav_${it.id}" }) { book ->
                            AudiobookShelfCard(
                                book = book,
                                onClick = {
                                    viewModel.playAudiobookWithResolution(book)
                                    onBookClick(book)
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(book) }
                            )
                        }
                    }
                }
            }

            // 7. Shelf: All Audiobooks Collection
            item {
                ShelfHeader(
                    title = "All Audiobooks",
                    subtitle = "Browse your entire library (${allBooks.size})",
                    icon = Icons.Default.ViewCarousel,
                    iconTint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(allBooks, key = { "all_${it.id}" }) { book ->
                        AudiobookShelfCard(
                            book = book,
                            onClick = {
                                viewModel.playAudiobookWithResolution(book)
                                onBookClick(book)
                            },
                            onFavoriteToggle = { viewModel.toggleFavorite(book) }
                        )
                    }
                }
            } // Ends item (All Audiobooks)

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        } // Ends LazyColumn
    } // Ends else
} // Ends main Column
} // Ends LibraryScreen Composable

@Composable
fun ShelfHeader(
    title: String,
    subtitle: String,
    badge: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: Color = AccentTeal,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (badge != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = iconTint.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            badge,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconTint
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "Slide to view more",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun AudiobookShelfCard(
    book: Audiobook,
    tag: String? = null,
    showProgress: Boolean = false,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onAuthorClick: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        // Shelf Cover with 3D Book Elevation & Badge
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(210.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceGlass)
                .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(16.dp))
        ) {
            if (book.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(AccentTeal.copy(alpha = 0.4f), AccentIndigo.copy(alpha = 0.6f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Book,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            book.title.take(1),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Top overlay: Tag & Favorite heart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (tag != null) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            tag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { onFavoriteToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (book.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (book.isFavorite) Color.Red else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Playing Indicator or Duration Pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                if (isPlaying) {
                    Surface(
                        color = AccentTeal,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = "Playing",
                                tint = Color.Black,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("PLAYING", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                        }
                    }
                } else if (book.duration > 0) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            com.example.utils.formatDuration(book.duration),
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Progress bar if in progress
            if (showProgress && book.duration > 0 && book.progress > 0) {
                val progressFraction = (book.progress.toFloat() / (book.duration * 1000f)).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter),
                    color = AccentTeal,
                    trackColor = Color.Black.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title and Author
        Text(
            book.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            book.author,
            fontSize = 11.sp,
            color = AccentTeal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { onAuthorClick(book.author) }
        )
    }
}

@Composable
fun BookShelfRowItem(
    book: Audiobook,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onAuthorClick: (String) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceGlass)
            .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (book.coverUrl.isNotBlank()) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(AccentTeal.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(book.title.take(1), fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(book.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(book.author, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, maxLines = 1)
            if (book.seriesName.isNotBlank()) {
                Text(book.seriesName, color = AccentTeal, fontSize = 11.sp, maxLines = 1)
            }
        }

        IconButton(onClick = onFavoriteToggle) {
            Icon(
                if (book.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (book.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        IconButton(onClick = onClick) {
            Icon(
                if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                contentDescription = "Play",
                tint = AccentTeal,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}



@Composable
fun Audiobook3ColumnCard(
    book: Audiobook,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onAuthorClick: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceGlass)
            .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(AccentTeal.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (book.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = AccentTeal,
                    modifier = Modifier.size(32.dp)
                )
            }

            if (isPlaying) {
                Surface(
                    color = AccentTeal,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = "Playing",
                        tint = Color.White,
                        modifier = Modifier.padding(3.dp).size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            book.title,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Text(
            book.author,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
