package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.LocalMediaMetadataProvider
import com.example.data.network.ArchiveDoc
import com.example.ui.MainViewModel
import com.example.ui.components.MediaCoverArt
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder

data class CarouselItem(
    val title: String,
    val creator: String,
    val type: String,
    val coverUrl: String,
    val genre: String = "Classic"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    viewModel: MainViewModel,
    title: String,
    creator: String,
    type: String, // "BOOK", "AUDIOBOOK", "MUSIC"
    onBack: () -> Unit,
    onCreatorClick: (String) -> Unit,
    onPlayReadClick: () -> Unit,
    onNavigateToDetails: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    var details by remember(title, creator) { 
        mutableStateOf(LocalMediaMetadataProvider.getFallbackDetails(title, creator, type)) 
    }
    var isAIEnhancing by remember { mutableStateOf(true) }

    val allBooks by viewModel.allBooks.collectAsState()
    val allEBooks by viewModel.allEBooks.collectAsState()
    val allMusic by viewModel.allMusic.collectAsState()
    val pdBooks by viewModel.publicDomainBooks.collectAsState()
    val pdAudiobooks by viewModel.publicDomainAudiobooks.collectAsState()
    val pdMusic by viewModel.publicDomainMusic.collectAsState()

    // Helper to extract ArchiveDoc creator name
    fun getDocCreator(doc: ArchiveDoc): String {
        return when (val cr = doc.creator) {
            is List<*> -> cr.firstOrNull()?.toString() ?: "Unknown"
            is String -> cr
            else -> "Unknown"
        }
    }

    LaunchedEffect(title, creator) {
        isAIEnhancing = true
        try {
            val result = viewModel.fetchDetailsWithGemini(title, creator, type)
            if (result.isNotEmpty()) {
                details = result
            }
        } catch (e: Exception) {
            // Keep fallback
        } finally {
            isAIEnhancing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (type == "BOOK") "About Book" else if (type == "AUDIOBOOK") "About Audiobook" else "Track Details", 
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ) 
                },
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
            
            // Get other books/tracks by the SAME creator
            val otherItems = remember(allBooks, allEBooks, allMusic, pdBooks, pdAudiobooks, pdMusic, creator, title, type) {
                val list = mutableListOf<CarouselItem>()
                when (type) {
                    "BOOK" -> {
                        allEBooks.filter {
                            it.author.contains(creator, ignoreCase = true) && !it.title.equals(title, ignoreCase = true)
                        }.forEach {
                            list.add(CarouselItem(it.title, it.author, "BOOK", it.coverUrl, it.genre))
                        }
                        pdBooks.filter { doc ->
                            getDocCreator(doc).contains(creator, ignoreCase = true) && !(doc.title ?: "").equals(title, ignoreCase = true)
                        }.forEach { doc ->
                            list.add(CarouselItem(doc.title ?: "Untitled", getDocCreator(doc), "BOOK", "https://archive.org/services/img/${doc.identifier}", "Classic"))
                        }
                    }
                    "AUDIOBOOK" -> {
                        allBooks.filter {
                            it.author.contains(creator, ignoreCase = true) && !it.title.equals(title, ignoreCase = true)
                        }.forEach {
                            list.add(CarouselItem(it.title, it.author, "AUDIOBOOK", it.coverUrl, it.genre))
                        }
                        pdAudiobooks.filter { doc ->
                            getDocCreator(doc).contains(creator, ignoreCase = true) && !(doc.title ?: "").equals(title, ignoreCase = true)
                        }.forEach { doc ->
                            list.add(CarouselItem(doc.title ?: "Untitled", getDocCreator(doc), "AUDIOBOOK", "https://archive.org/services/img/${doc.identifier}", "Classic"))
                        }
                    }
                    "MUSIC" -> {
                        allMusic.filter {
                            it.artist.contains(creator, ignoreCase = true) && !it.title.equals(title, ignoreCase = true)
                        }.forEach {
                            list.add(CarouselItem(it.title, it.artist, "MUSIC", it.coverUrl, it.genre))
                        }
                        pdMusic.filter { doc ->
                            getDocCreator(doc).contains(creator, ignoreCase = true) && !(doc.title ?: "").equals(title, ignoreCase = true)
                        }.forEach { doc ->
                            list.add(CarouselItem(doc.title ?: "Untitled", getDocCreator(doc), "MUSIC", "https://archive.org/services/img/${doc.identifier}", "Classic"))
                        }
                    }
                }
                list.distinctBy { it.title.lowercase() }
            }

            // Get similar books/tracks based on matching genre/category
            val currentGenre = data["genre"] ?: data["assignedGenre"] ?: "Classic"
            val similarItems = remember(allBooks, allEBooks, allMusic, pdBooks, pdAudiobooks, pdMusic, currentGenre, title, type) {
                val list = mutableListOf<CarouselItem>()
                when (type) {
                    "BOOK" -> {
                        allEBooks.filter {
                            (it.genre.contains(currentGenre, ignoreCase = true) || currentGenre.contains(it.genre, ignoreCase = true)) && !it.title.equals(title, ignoreCase = true)
                        }.forEach {
                            list.add(CarouselItem(it.title, it.author, "BOOK", it.coverUrl, it.genre))
                        }
                        pdBooks.filter { doc ->
                            val desc = doc.description?.toString() ?: ""
                            (desc.contains(currentGenre, ignoreCase = true) || (doc.title ?: "").contains(currentGenre, ignoreCase = true)) && !(doc.title ?: "").equals(title, ignoreCase = true)
                        }.take(10).forEach { doc ->
                            list.add(CarouselItem(doc.title ?: "Untitled", getDocCreator(doc), "BOOK", "https://archive.org/services/img/${doc.identifier}", "Classic"))
                        }
                        if (list.isEmpty()) {
                            pdBooks.filter { !(it.title ?: "").equals(title, ignoreCase = true) }.take(8).forEach { doc ->
                                list.add(CarouselItem(doc.title ?: "Untitled", getDocCreator(doc), "BOOK", "https://archive.org/services/img/${doc.identifier}", "Classic"))
                            }
                        }
                    }
                    "AUDIOBOOK" -> {
                        allBooks.filter {
                            (it.genre.contains(currentGenre, ignoreCase = true) || currentGenre.contains(it.genre, ignoreCase = true)) && !it.title.equals(title, ignoreCase = true)
                        }.forEach {
                            list.add(CarouselItem(it.title, it.author, "AUDIOBOOK", it.coverUrl, it.genre))
                        }
                        pdAudiobooks.filter { doc ->
                            val desc = doc.description?.toString() ?: ""
                            (desc.contains(currentGenre, ignoreCase = true) || (doc.title ?: "").contains(currentGenre, ignoreCase = true)) && !(doc.title ?: "").equals(title, ignoreCase = true)
                        }.take(10).forEach { doc ->
                            list.add(CarouselItem(doc.title ?: "Untitled", getDocCreator(doc), "AUDIOBOOK", "https://archive.org/services/img/${doc.identifier}", "Classic"))
                        }
                        if (list.isEmpty()) {
                            pdAudiobooks.filter { !(it.title ?: "").equals(title, ignoreCase = true) }.take(8).forEach { doc ->
                                list.add(CarouselItem(doc.title ?: "Untitled", getDocCreator(doc), "AUDIOBOOK", "https://archive.org/services/img/${doc.identifier}", "Classic"))
                            }
                        }
                    }
                    "MUSIC" -> {
                        allMusic.filter {
                            (it.genre.contains(currentGenre, ignoreCase = true) || currentGenre.contains(it.genre, ignoreCase = true)) && !it.title.equals(title, ignoreCase = true)
                        }.forEach {
                            list.add(CarouselItem(it.title, it.artist, "MUSIC", it.coverUrl, it.genre))
                        }
                        pdMusic.filter { doc ->
                            val desc = doc.description?.toString() ?: ""
                            (desc.contains(currentGenre, ignoreCase = true) || (doc.title ?: "").contains(currentGenre, ignoreCase = true)) && !(doc.title ?: "").equals(title, ignoreCase = true)
                        }.take(10).forEach { doc ->
                            list.add(CarouselItem(doc.title ?: "Untitled", getDocCreator(doc), "MUSIC", "https://archive.org/services/img/${doc.identifier}", "Classic"))
                        }
                        if (list.isEmpty()) {
                            pdMusic.filter { !(it.title ?: "").equals(title, ignoreCase = true) }.take(8).forEach { doc ->
                                list.add(CarouselItem(doc.title ?: "Untitled", getDocCreator(doc), "MUSIC", "https://archive.org/services/img/${doc.identifier}", "Classic"))
                            }
                        }
                    }
                }
                list.distinctBy { it.title.lowercase() }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Hero Section
                Row(modifier = Modifier.fillMaxWidth()) {
                    MediaCoverArt(
                        title = title,
                        authorOrArtist = creator,
                        coverUrl = data["coverUrl"],
                        genre = currentGenre,
                        isBookAspectRatio = type != "MUSIC",
                        modifier = Modifier
                            .width(135.dp)
                            .aspectRatio(if (type == "MUSIC") 1f else 0.68f)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title, 
                            fontSize = 22.sp, 
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 26.sp
                        )
                        
                        Text(
                            text = creator, 
                            fontSize = 15.sp, 
                            fontWeight = FontWeight.SemiBold,
                            color = AccentTeal,
                            modifier = Modifier.clickable { onCreatorClick(creator) }
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star, 
                                contentDescription = "Rating", 
                                tint = Color(0xFFFFB300), 
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = data["rating"] ?: "4.9/5", 
                                fontSize = 13.sp, 
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Text(
                            text = "Edition: ${data["publisher"] ?: "Public Domain Library"}", 
                            fontSize = 12.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Button(
                            onClick = onPlayReadClick,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(
                                if (type == "BOOK") Icons.Default.MenuBook else Icons.Default.PlayArrow, 
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (type == "BOOK") "Read Book" else if (type == "AUDIOBOOK") "Listen to Audiobook" else "Play Track",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Synopsis Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Synopsis & Analysis", 
                                fontSize = 17.sp, 
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (isAIEnhancing) {
                                Text("AI Enhanced", fontSize = 11.sp, color = AccentTeal)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = data["bio"] ?: "No synopsis available.", 
                            fontSize = 14.sp, 
                            lineHeight = 22.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                // Other books by creator
                if (otherItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(28.dp))
                    MediaCarousel(
                        title = if (type == "BOOK") "Other works by $creator" else if (type == "AUDIOBOOK") "Audiobooks by $creator" else "Tracks by $creator",
                        items = otherItems,
                        onClick = { item ->
                            onNavigateToDetails(item.title, item.creator, item.type)
                        }
                    )
                }

                // Similar books on server
                if (similarItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(28.dp))
                    MediaCarousel(
                        title = if (type == "BOOK") "Similar books in the server" else if (type == "AUDIOBOOK") "Similar audiobooks in the server" else "Similar tracks",
                        items = similarItems,
                        onClick = { item ->
                            onNavigateToDetails(item.title, item.creator, item.type)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                data["website"]?.takeIf { it.isNotBlank() && it != "N/A" }?.let { url ->
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = AccentTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Author Biography on Wikipedia", color = AccentTeal)
                    }
                }
            }
        }
    }
}

@Composable
fun MediaCarousel(
    title: String,
    items: List<CarouselItem>,
    onClick: (CarouselItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(items) { item ->
                Column(
                    modifier = Modifier
                        .width(110.dp)
                        .clickable { onClick(item) }
                ) {
                    MediaCoverArt(
                        title = item.title,
                        authorOrArtist = item.creator,
                        coverUrl = item.coverUrl,
                        genre = item.genre,
                        isBookAspectRatio = item.type != "MUSIC",
                        cornerRadius = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(if (item.type == "MUSIC") 1f else 0.68f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        item.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        item.creator,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
