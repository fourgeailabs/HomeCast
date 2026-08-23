package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder

data class BookshelfItem(
    val id: String,
    val title: String,
    val authorOrArtist: String,
    val publicDomainUrl: String? = null,
    val coverUrl: String,
    val genre: String,
    val isComic: Boolean = false,
    val progressPercent: Int = 0,
    val pageCount: Int = 320,
    val rating: Float = 4.8f,
    val description: String = ""
)

val sampleBookshelfItems = listOf(
    BookshelfItem(
        id = "ebook_1",
        publicDomainUrl = "https://www.gutenberg.org/cache/epub/35/pg35.txt",
        title = "The Time Machine",
        authorOrArtist = "H.G. Wells",
        coverUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500&auto=format&fit=crop&q=60",
        genre = "Sci-Fi",
        progressPercent = 65,
        pageCount = 180,
        description = "A Victorian scientist constructs a machine that travels through fourth-dimensional spacetime into the far future year 802,701 AD."
    ),
    BookshelfItem(
        id = "ebook_2",
        publicDomainUrl = "https://www.gutenberg.org/cache/epub/84/pg84.txt",
        title = "Frankenstein",
        authorOrArtist = "Mary Shelley",
        coverUrl = "https://images.unsplash.com/photo-1532012164546-f432f2e37b29?w=500&auto=format&fit=crop&q=60",
        genre = "Classic",
        progressPercent = 30,
        pageCount = 280,
        description = "Victor Frankenstein creates a sentient creature in an unorthodox scientific experiment with haunting consequences."
    ),
    BookshelfItem(
        id = "ebook_3",
        title = "Neuromancer",
        authorOrArtist = "William Gibson",
        coverUrl = "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=500&auto=format&fit=crop&q=60",
        genre = "Cyberpunk",
        progressPercent = 88,
        pageCount = 271,
        description = "Case, a washed-up computer hacker hired by a mysterious employer, is contracted for the ultimate digital hack into the Matrix."
    ),
    BookshelfItem(
        id = "comic_1",
        title = "Neon Vanguard #1",
        authorOrArtist = "Marcus Drake • Kenji Sato",
        coverUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&auto=format&fit=crop&q=60",
        genre = "Comics",
        isComic = true,
        progressPercent = 45,
        pageCount = 32,
        description = "In Neo-Kyoto 2088, rogue cyber-enforcers uncover an omnipotent rogue artificial intelligence governing the skyline."
    ),
    BookshelfItem(
        id = "ebook_4",
        publicDomainUrl = "https://www.gutenberg.org/cache/epub/132/pg132.txt",
        title = "The Art of War",
        authorOrArtist = "Sun Tzu",
        coverUrl = "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=500&auto=format&fit=crop&q=60",
        genre = "Philosophy",
        progressPercent = 100,
        pageCount = 112,
        description = "Ancient Chinese military treatise attributed to Sun Tzu, devoted to strategic thinking and tactics."
    ),
    BookshelfItem(
        id = "comic_2",
        title = "Solaris Odyssey #4",
        authorOrArtist = "Elena Vance • Roy Zhang",
        coverUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=500&auto=format&fit=crop&q=60",
        genre = "Comics",
        isComic = true,
        progressPercent = 15,
        pageCount = 28,
        description = "Deep space explorers approach an oceanic sentient planet exhibiting reality-bending gravitational anomalies."
    ),
    BookshelfItem(
        id = "ebook_5",
        publicDomainUrl = "https://www.gutenberg.org/cache/epub/64317/pg64317.txt",
        title = "The Great Gatsby",
        authorOrArtist = "F. Scott Fitzgerald",
        coverUrl = "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=500&auto=format&fit=crop&q=60",
        genre = "Classic",
        progressPercent = 12,
        pageCount = 190,
        description = "A tragic story of Jay Gatsby, a self-made millionaire, and his pursuit of Daisy Buchanan."
    ),
    BookshelfItem(
        id = "ebook_6",
        publicDomainUrl = "https://www.gutenberg.org/cache/epub/5200/pg5200.txt",
        title = "Metamorphosis",
        authorOrArtist = "Franz Kafka",
        coverUrl = "https://images.unsplash.com/photo-1495640388970-05e83744d18e?w=500&auto=format&fit=crop&q=60",
        genre = "Philosophy",
        progressPercent = 50,
        pageCount = 128,
        description = "Gregor Samsa awakens one morning to find himself transformed into a monstrous insect."
    ),
    BookshelfItem(
        id = "comic_3",
        title = "Cyber Ronin: Eclipse",
        authorOrArtist = "Akira Takahashi",
        coverUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=500&auto=format&fit=crop&q=60",
        genre = "Comics",
        isComic = true,
        progressPercent = 90,
        pageCount = 44,
        description = "A lone samurai in a neon-drenched dystopia fights to protect the last biological memory vault."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EBooksScreen(
    viewModel: MainViewModel,
    onOpenEBook: (EBookData) -> Unit,
    onOpenComic: (ComicData) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDetails: (String, String, String) -> Unit = {_,_,_->},
    onNavigateToCreator: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCollection by remember { mutableStateOf<Pair<String, List<BookshelfItem>>?>(null) }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var isGridViewOpen by remember { mutableStateOf(false) }
    var selectedSource by remember { mutableIntStateOf(viewModel.initialEbooksSource) } // 0 = Personal, 1 = Public Domain

    val allEBooks by viewModel.allEBooks.collectAsState()
    val archiveBooks by viewModel.publicDomainBooks.collectAsState()
    
    val currentBookshelfItems = remember(allEBooks, archiveBooks, selectedSource) {
        if (selectedSource == 0) {
            allEBooks.filter { it.serverId != "demo_server" && it.serverId != "pd_server" }.map { ebook ->
                BookshelfItem(
                    id = ebook.id,
                    title = ebook.title,
                    authorOrArtist = ebook.author,
                    coverUrl = ebook.coverUrl,
                    genre = ebook.genre,
                    isComic = ebook.isComic,
                    progressPercent = ebook.progressPercent,
                    pageCount = ebook.totalPages,
                    description = ebook.description
                )
            }
        } else {
            val localPD = allEBooks.filter { it.serverId == "demo_server" || it.serverId == "pd_server" }.map { ebook ->
                BookshelfItem(
                    id = ebook.id,
                    title = ebook.title,
                    authorOrArtist = ebook.author,
                    coverUrl = ebook.coverUrl,
                    genre = ebook.genre,
                    isComic = ebook.isComic,
                    progressPercent = ebook.progressPercent,
                    pageCount = ebook.totalPages,
                    description = ebook.description
                )
            }
            val fetched = archiveBooks.map { doc ->
                val coverUrl = "https://archive.org/services/img/${doc.identifier}"
                val title = doc.title ?: "Unknown Title"
                val author = when (doc.creator) {
                    is List<*> -> doc.creator.firstOrNull()?.toString() ?: "Unknown Author"
                    is String -> doc.creator
                    else -> "Unknown Author"
                }
                val desc = when (doc.description) {
                    is List<*> -> doc.description.firstOrNull()?.toString() ?: ""
                    is String -> doc.description
                    else -> ""
                }
                BookshelfItem(
                    id = doc.identifier,
                    title = title,
                    authorOrArtist = author,
                    publicDomainUrl = "https://archive.org/download/${doc.identifier}/${doc.identifier}_djvu.txt", // best effort
                    coverUrl = coverUrl,
                    genre = "Classic",
                    description = desc
                )
            }.filter { f -> localPD.none { l -> l.title.equals(f.title, ignoreCase = true) } }
            localPD + fetched
        }
    }

    val genres = remember(currentBookshelfItems) {
        val extracted = currentBookshelfItems.map { it.genre.trim() }.filter { it.isNotBlank() }.distinct()
        if (extracted.isNotEmpty()) listOf("All") + extracted else listOf("All", "Sci-Fi", "Classic", "Comics", "Cyberpunk", "Philosophy")
    }

    val filteredItems = remember(searchQuery, selectedGenre, currentBookshelfItems) {
        currentBookshelfItems.filter { item ->
            val matchesGenre = selectedGenre == null || selectedGenre == "All" || item.genre.equals(selectedGenre, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() || item.title.contains(searchQuery, ignoreCase = true) || item.authorOrArtist.contains(searchQuery, ignoreCase = true)
            matchesGenre && matchesSearch
        }
    }

    fun openItem(item: BookshelfItem) {
        if (item.isComic) {
            val sampleComic = ComicData(
                id = item.id,
                title = item.title,
                series = item.title,
                issueNumber = "#1",
                writer = item.authorOrArtist.split("•").firstOrNull()?.trim() ?: item.authorOrArtist,
                artist = item.authorOrArtist.split("•").lastOrNull()?.trim() ?: "Unknown",
                coverUrl = item.coverUrl,
                pages = listOf(
                    ComicPage(
                        pageNumber = 1,
                        fullPageArtUrl = item.coverUrl,
                        pageTitle = "Prologue",
                        frames = listOf(
                            ComicFrame(
                                id = "f1",
                                frameNumber = 1,
                                title = "Establishing Shot - Neo Metropolis",
                                speaker = "NARRATOR",
                                dialogue = "Rain fell in sheets over the shimmering spires of Neo-Tokyo. 2088.",
                                sfx = "SHSHHH...",
                                gradientColors = listOf(Color(0xFF1E1B4B), Color(0xFF0F172A))
                            ),
                            ComicFrame(
                                id = "f2",
                                frameNumber = 2,
                                title = "Rooftop Infiltration",
                                speaker = "KAI",
                                dialogue = "Visual sensors calibrated. Perimeter security is bypassed.",
                                sfx = "CLIK-WHIRR",
                                gradientColors = listOf(Color(0xFF0C4A6E), Color(0xFF0284C7))
                            ),
                            ComicFrame(
                                id = "f3",
                                frameNumber = 3,
                                title = "Action Clash",
                                speaker = "CYBER ENFORCER",
                                dialogue = "Halt! You are trespassing on secure sector 9!",
                                sfx = "KRAAA-KOOOM!!",
                                gradientColors = listOf(Color(0xFF7F1D1D), Color(0xFFEA580C))
                            )
                        )
                    )
                )
            )
            onOpenComic(sampleComic)
        } else {
            val sampleEBook = EBookData(
                id = item.id,
                title = item.title,
                author = item.authorOrArtist,
                totalChapters = 0,
                chapters = emptyList(),
                publicDomainUrl = item.publicDomainUrl
            )
            onOpenEBook(sampleEBook)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Bookshelf",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color = Color.White
                    )
                    Text(
                        "E-Books • Graphic Novels • Manga",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { isGridViewOpen = !isGridViewOpen },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceGlass)
                            .border(1.dp, SurfaceGlassBorder, CircleShape)
                    ) {
                        Icon(
                            if (isGridViewOpen) Icons.Default.ViewAgenda else Icons.Default.GridView,
                            contentDescription = "Toggle Grid / Shelf View",
                            tint = AccentTeal
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceGlass)
                            .border(1.dp, SurfaceGlassBorder, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = AccentTeal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            androidx.compose.material3.TabRow(
                selectedTabIndex = selectedSource,
                containerColor = Color.Transparent,
                indicator = { tabPositions ->
                    androidx.compose.material3.TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedSource]),
                        color = AccentIndigo
                    )
                }
            ) {
                listOf("Personal Library", "Public Domain").forEachIndexed { index, title ->
                    androidx.compose.material3.Tab(
                        selected = selectedSource == index,
                        onClick = { selectedSource = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedSource == index) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        selectedContentColor = AccentIndigo,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search and Genre Filters
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceGlass),
                placeholder = { Text("Search books, graphic novels, manga...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AccentTeal) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentTeal,
                    unfocusedBorderColor = SurfaceGlassBorder
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Genre Chips Horizontal Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genres.forEach { genre ->
                    val isSelected = (selectedGenre == genre) || (selectedGenre == null && genre == "All")
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedGenre = if (genre == "All") null else genre
                        },
                        label = {
                            Text(
                                genre,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
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

            // Main Content: 3-Column Top-Down Grid OR Frosted Glass Bookshelf Planks
            if (isGridViewOpen || selectedGenre != null) {
                // 3-COLUMN TOP DOWN SCROLL VIEW AS REQUESTED
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${selectedGenre ?: "All Choices"} (${filteredItems.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filteredItems) { book ->
                            BookCard3Column(book = book, onClick = { onNavigateToDetails(book.title, book.authorOrArtist, "BOOK") }, onAuthorClick = onNavigateToCreator)
                        }
                    }
                }
            } else {
                // FROSTED GLASS BOOKSHELF STYLIZED VIEW
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    // Shelf 1: Currently Reading
                    val currentlyReading = filteredItems.filter { it.progressPercent in 1..99 }
                    if (currentlyReading.isNotEmpty()) {
                        item {
                            GlassBookshelfRow(
                                shelfTitle = "Currently Reading",
                                badge = "RECENT",
                                badgeColor = AccentTeal,
                                items = currentlyReading,
                                onItemClick = { onNavigateToDetails(it.title, it.authorOrArtist, "BOOK") },
                                onHeaderClick = { selectedCollection = Pair("Currently Reading", currentlyReading) },
                                onNavigateToCreator = onNavigateToCreator
                            )
                        }
                    }

                    // Shelf 2: Graphic Novels & Manga
                    val comics = filteredItems.filter { it.isComic }
                    if (comics.isNotEmpty()) {
                        item {
                            GlassBookshelfRow(
                                shelfTitle = "Graphic Novels & Manga",
                                badge = "GEMINI SMART ZOOM",
                                badgeColor = AccentIndigo,
                                items = comics,
                                onItemClick = { onNavigateToDetails(it.title, it.authorOrArtist, "BOOK") },
                                onHeaderClick = { selectedCollection = Pair("Graphic Novels & Manga", comics) },
                                onNavigateToCreator = onNavigateToCreator
                            )
                        }
                    }

                    // Dynamic Genres
                    val specificGenres = listOf("Sci-Fi", "Cyberpunk", "Classic", "Philosophy")
                    val otherBooks = filteredItems.filter { !it.isComic && it.progressPercent !in 1..99 }
                    
                    val groupedByGenre = otherBooks.groupBy { it.genre.takeIf { g -> g.isNotBlank() } ?: "Uncategorized" }
                    
                    groupedByGenre.forEach { (genre, books) ->
                        item {
                            GlassBookshelfRow(
                                shelfTitle = genre,
                                badge = "COLLECTION",
                                badgeColor = Color(0xFFF59E0B),
                                items = books,
                                onItemClick = { onNavigateToDetails(it.title, it.authorOrArtist, "BOOK") },
                                onHeaderClick = { selectedCollection = Pair(genre, books) },
                                onNavigateToCreator = onNavigateToCreator
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassBookshelfRow(
    shelfTitle: String,
    badge: String,
    badgeColor: Color,
    items: List<BookshelfItem>,
    onItemClick: (BookshelfItem) -> Unit,
    onNavigateToCreator: (String) -> Unit = {},
    onHeaderClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.clickable { onHeaderClick() }
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                shelfTitle,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                color = badgeColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
            ) {
                Text(
                    badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // Horizontal book row resting on translucent 3D glass shelf
        Box(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(items) { book ->
                    BookshelfBookItem(book = book, onClick = { onItemClick(book) }, onAuthorClick = onNavigateToCreator)
                }
            }

            // Frosted Glass Shelf Plank at the bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.3f),
                                SurfaceGlass,
                                Color.Black.copy(alpha = 0.4f)
                            )
                        )
                    )
                    .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(4.dp))
                    .shadow(8.dp)
            )
        }
    }
}

@Composable
fun BookshelfBookItem(
    book: BookshelfItem,
    onClick: () -> Unit,
    onAuthorClick: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .width(115.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(115.dp)
                .height(160.dp)
                .shadow(12.dp, RoundedCornerShape(10.dp))
        ) {
            com.example.ui.components.MediaCoverArt(
                title = book.title,
                authorOrArtist = book.authorOrArtist,
                coverUrl = book.coverUrl,
                genre = book.genre,
                isBookAspectRatio = true,
                cornerRadius = 10.dp,
                modifier = Modifier.fillMaxSize()
            )

            if (book.isComic) {
                Surface(
                    color = AccentIndigo.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(topStart = 10.dp, bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        "COMIC",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (book.progressPercent > 0) {
                LinearProgressIndicator(
                    progress = { book.progressPercent / 100f },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp),
                    color = AccentTeal,
                    trackColor = Color.Black.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            book.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Text(
            book.authorOrArtist,
            fontSize = 10.sp,
            color = AccentTeal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable { onAuthorClick(book.authorOrArtist) }
        )
    }
}

@Composable
fun BookCard3Column(
    book: BookshelfItem,
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
                .aspectRatio(0.68f)
        ) {
            com.example.ui.components.MediaCoverArt(
                title = book.title,
                authorOrArtist = book.authorOrArtist,
                coverUrl = book.coverUrl,
                genre = book.genre,
                isBookAspectRatio = true,
                cornerRadius = 8.dp,
                modifier = Modifier.fillMaxSize()
            )

            if (book.isComic) {
                Surface(
                    color = AccentIndigo,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                ) {
                    Text(
                        "COMIC",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            if (book.progressPercent > 0) {
                LinearProgressIndicator(
                    progress = { book.progressPercent / 100f },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                    color = AccentTeal,
                    trackColor = Color.Black.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            book.title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Text(
            book.authorOrArtist,
            fontSize = 9.sp,
            color = AccentTeal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable { onAuthorClick(book.authorOrArtist) }
        )
    }
}
