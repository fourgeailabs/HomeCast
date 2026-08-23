package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder
import com.example.data.Audiobook
import com.example.data.MusicTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorDetailScreen(
    viewModel: MainViewModel,
    creatorName: String,
    onBack: () -> Unit,
    onReadEBook: (EBookData) -> Unit = {},
    onPlayAudiobook: (Audiobook) -> Unit = {},
    onPlayMusicTrack: (MusicTrack) -> Unit = {}
) {
    val context = LocalContext.current
    var details by remember { mutableStateOf<Map<String, String>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val allMusic by viewModel.allMusic.collectAsState()
    val pdMusic by viewModel.publicDomainMusic.collectAsState()
    val allBooks = viewModel.allBooks.collectAsState().value
    val pdBooks by viewModel.publicDomainBooks.collectAsState()
    val pdAudiobooks by viewModel.publicDomainAudiobooks.collectAsState()
    val localEBooks by viewModel.allEBooks.collectAsState()
    val resolvedDurations by viewModel.resolvedDurations.collectAsState()

    LaunchedEffect(creatorName) {
        isLoading = true
        val result = viewModel.fetchCreatorDetailsWithGemini(creatorName)
        details = result
        isLoading = false
    }

    // Filter matching E-Books
    val combinedEBooks = remember(creatorName, localEBooks, pdBooks) {
        val localMatch = localEBooks.filter { it.author.contains(creatorName, ignoreCase = true) }
            .map { e ->
                EBookData(
                    id = e.id,
                    title = e.title,
                    author = e.author,
                    totalChapters = 0,
                    chapters = emptyList(),
                    publicDomainUrl = e.serverId
                )
            }
        val pdMatch = pdBooks.filter { doc ->
            val creatorStr = when (val creator = doc.creator) {
                is List<*> -> creator.firstOrNull()?.toString() ?: ""
                is String -> creator
                else -> ""
            }
            creatorStr.contains(creatorName, ignoreCase = true)
        }.map { doc ->
            EBookData(
                id = doc.identifier,
                title = doc.title ?: "E-Book",
                author = creatorName,
                totalChapters = 0,
                chapters = emptyList(),
                publicDomainUrl = "https://archive.org/download/${doc.identifier}/${doc.identifier}_djvu.txt"
            )
        }
        (localMatch + pdMatch).distinctBy { it.title.lowercase().trim() }
    }

    // Filter matching Audiobooks
    val combinedAudiobooks = remember(creatorName, allBooks, pdAudiobooks, resolvedDurations) {
        val localMatch = allBooks.filter { it.author.contains(creatorName, ignoreCase = true) }
        val pdMatch = pdAudiobooks.filter { doc ->
            val creatorStr = when (val creator = doc.creator) {
                is List<*> -> creator.firstOrNull()?.toString() ?: ""
                is String -> creator
                else -> ""
            }
            creatorStr.contains(creatorName, ignoreCase = true)
        }.map { doc ->
            Audiobook(
                id = doc.identifier,
                title = doc.title ?: "Audiobook",
                author = creatorName,
                duration = resolvedDurations[doc.identifier] ?: 3600L,
                coverUrl = "https://archive.org/services/img/${doc.identifier}",
                serverId = "pd_server",
                streamUrl = "https://archive.org/download/${doc.identifier}/${doc.identifier}_64kb.mp3",
                narrator = "Archive.org",
                genre = "Classic"
            )
        }
        (localMatch + pdMatch).distinctBy { it.title.lowercase().trim() }
    }

    // Filter matching Music tracks
    val combinedMusic = remember(creatorName, allMusic, pdMusic) {
        val localMatch = allMusic.filter { it.artist.contains(creatorName, ignoreCase = true) }
        val pdMatch = pdMusic.filter { doc ->
            val creatorStr = when (val creator = doc.creator) {
                is List<*> -> creator.firstOrNull()?.toString() ?: ""
                is String -> creator
                else -> ""
            }
            creatorStr.contains(creatorName, ignoreCase = true) || (doc.title ?: "").contains(creatorName, ignoreCase = true)
        }.map { doc ->
            val titleStr = doc.title ?: "Archive Music"
            MusicTrack(
                id = doc.identifier,
                title = titleStr,
                artist = creatorName,
                album = "Archive.org Classics",
                coverUrl = "https://archive.org/services/img/${doc.identifier}",
                duration = 180000L,
                serverId = "pd_server",
                streamUrl = "",
                genre = "Classics",
                trackNumber = 1
            )
        }
        (localMatch + pdMatch).distinctBy { it.title.lowercase().trim() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Creator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AccentTeal)
            } else if (details != null) {
                val data = details!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = data["imageUrl"] ?: "",
                        contentDescription = creatorName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(SurfaceGlass)
                            .border(1.5.dp, SurfaceGlassBorder, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(creatorName, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(data["roles"] ?: "", fontSize = 13.sp, color = AccentTeal, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text(
                        data["bio"] ?: "No biography available.", 
                        fontSize = 15.sp, 
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        data["wikiLink"]?.takeIf { it.isNotBlank() && it != "N/A" }?.let { url ->
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Wikipedia", fontSize = 12.sp)
                            }
                        }
                        
                        data["website"]?.takeIf { it.isNotBlank() && it != "N/A" }?.let { url ->
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Website", fontSize = 12.sp)
                            }
                        }
                    }

                    // E-Books Shelf
                    CreatorItemShelf(
                        title = "E-Books in Public Domain",
                        items = combinedEBooks,
                        icon = Icons.Default.Book,
                        onItemClick = onReadEBook
                    ) { item ->
                        CreatorCard(title = item.title, coverUrl = "")
                    }

                    // Audiobooks Shelf
                    CreatorItemShelf(
                        title = "Audiobooks in Public Domain",
                        items = combinedAudiobooks,
                        icon = Icons.Default.Headphones,
                        onItemClick = onPlayAudiobook
                    ) { item ->
                        CreatorCard(title = item.title, coverUrl = item.coverUrl)
                    }

                    // Music Tracks Shelf
                    CreatorItemShelf(
                        title = "Music in Public Domain",
                        items = combinedMusic,
                        icon = Icons.Default.MusicNote,
                        onItemClick = onPlayMusicTrack
                    ) { item ->
                        CreatorCard(title = item.title, coverUrl = item.coverUrl)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            } else {
                Text("Failed to load details.", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun <T> CreatorItemShelf(
    title: String,
    items: List<T>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onItemClick: (T) -> Unit,
    cardContent: @Composable (T) -> Unit
) {
    if (items.isNotEmpty()) {
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items) { item ->
                Box(modifier = Modifier.clickable { onItemClick(item) }) {
                    cardContent(item)
                }
            }
        }
    }
}

@Composable
fun CreatorCard(title: String, coverUrl: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        modifier = Modifier
            .width(130.dp)
            .height(180.dp)
            .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (coverUrl.isNotBlank()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentTeal.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Book, contentDescription = null, tint = AccentTeal.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                maxLines = 2,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
