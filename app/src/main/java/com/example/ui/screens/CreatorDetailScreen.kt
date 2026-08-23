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
import com.example.data.Audiobook
import com.example.data.LocalMediaMetadataProvider
import com.example.data.MusicTrack
import com.example.ui.MainViewModel
import com.example.ui.components.MediaCoverArt
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder

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
    var details by remember(creatorName) {
        mutableStateOf(LocalMediaMetadataProvider.getFallbackCreatorDetails(creatorName))
    }

    val allMusic by viewModel.allMusic.collectAsState()
    val pdMusic by viewModel.publicDomainMusic.collectAsState()
    val allBooks = viewModel.allBooks.collectAsState().value
    val pdBooks by viewModel.publicDomainBooks.collectAsState()
    val pdAudiobooks by viewModel.publicDomainAudiobooks.collectAsState()
    val localEBooks by viewModel.allEBooks.collectAsState()
    val resolvedDurations by viewModel.resolvedDurations.collectAsState()

    LaunchedEffect(creatorName) {
        try {
            val result = viewModel.fetchCreatorDetailsWithGemini(creatorName)
            if (result.isNotEmpty()) {
                details = result
            }
        } catch (e: Exception) {
            // Keep fallback
        }
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
                duration = resolvedDurations[doc.identifier] ?: 0L,
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
                title = { Text("About Creator", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) { 
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) 
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            val data = details
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(AccentTeal.copy(alpha = 0.2f))
                        .border(2.dp, AccentTeal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person, 
                        contentDescription = null, 
                        tint = AccentTeal, 
                        modifier = Modifier.size(54.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = creatorName, 
                    fontSize = 24.sp, 
                    fontWeight = FontWeight.ExtraBold, 
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = data["roles"] ?: "Master Creator", 
                    fontSize = 13.sp, 
                    color = AccentTeal, 
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            "Biography", 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            data["bio"] ?: "No biography available.", 
                            fontSize = 14.sp, 
                            lineHeight = 22.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
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
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wikipedia", fontSize = 12.sp, color = AccentTeal)
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
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Archive", fontSize = 12.sp, color = AccentTeal)
                        }
                    }
                }

                // E-Books Shelf
                CreatorItemShelf(
                    title = "E-Books & Literature",
                    items = combinedEBooks,
                    icon = Icons.Default.Book,
                    onItemClick = onReadEBook
                ) { item ->
                    Column(modifier = Modifier.width(115.dp)) {
                        MediaCoverArt(
                            title = item.title,
                            authorOrArtist = creatorName,
                            coverUrl = null,
                            isBookAspectRatio = true,
                            cornerRadius = 8.dp,
                            modifier = Modifier.fillMaxWidth().aspectRatio(0.68f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.title,
                            maxLines = 1,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }

                // Audiobooks Shelf
                CreatorItemShelf(
                    title = "Audiobooks",
                    items = combinedAudiobooks,
                    icon = Icons.Default.Headphones,
                    onItemClick = onPlayAudiobook
                ) { item ->
                    Column(modifier = Modifier.width(115.dp)) {
                        MediaCoverArt(
                            title = item.title,
                            authorOrArtist = creatorName,
                            coverUrl = item.coverUrl,
                            isBookAspectRatio = true,
                            cornerRadius = 8.dp,
                            modifier = Modifier.fillMaxWidth().aspectRatio(0.68f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.title,
                            maxLines = 1,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }

                // Music Tracks Shelf
                CreatorItemShelf(
                    title = "Music Tracks",
                    items = combinedMusic,
                    icon = Icons.Default.MusicNote,
                    onItemClick = onPlayMusicTrack
                ) { item ->
                    Column(modifier = Modifier.width(115.dp)) {
                        MediaCoverArt(
                            title = item.title,
                            authorOrArtist = creatorName,
                            coverUrl = item.coverUrl,
                            isBookAspectRatio = false,
                            cornerRadius = 8.dp,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.title,
                            maxLines = 1,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
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
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
