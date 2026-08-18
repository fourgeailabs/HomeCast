package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
    onNavigateToSettings: () -> Unit
) {
    val allBooks by viewModel.allBooks.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val recents by viewModel.recents.collectAsState()
    val servers by viewModel.servers.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val filteredBooks = remember(allBooks, searchQuery) {
        if (searchQuery.isBlank()) allBooks
        else allBooks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.author.contains(searchQuery, ignoreCase = true) ||
            it.seriesName.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Audiobooks", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                if (servers.any { it.type == "audiobookshelf" }) {
                    IconButton(onClick = {
                        val server = servers.firstOrNull { it.type == "audiobookshelf" }
                        if (server != null) viewModel.syncServer(server)
                    }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync", tint = AccentTeal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search title, author, series...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
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
                    unfocusedContainerColor = SurfaceGlass
                )
            )
        }

        if (allBooks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(AccentTeal.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Book, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(32.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No Audiobooks Synced", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Connect your personal Audiobookshelf server in Settings to stream and sync your complete collection.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onNavigateToSettings,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect Audiobookshelf")
                        }
                    }
                }
            }
        } else {
            // Recently Played
            if (recents.isNotEmpty() && searchQuery.isBlank()) {
                item {
                    Text("Continue Listening", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(recents) { book ->
                            BookCard(
                                book = book,
                                onClick = {
                                    viewModel.playAudiobook(book)
                                    onBookClick(book)
                                }
                            )
                        }
                    }
                }
            }

            // Favorites
            if (favorites.isNotEmpty() && searchQuery.isBlank()) {
                item {
                    Text("Favorites", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(favorites) { book ->
                            BookCard(
                                book = book,
                                onClick = {
                                    viewModel.playAudiobook(book)
                                    onBookClick(book)
                                }
                            )
                        }
                    }
                }
            }

            // All Audiobooks List
            item {
                Text(
                    if (searchQuery.isNotBlank()) "Search Results (${filteredBooks.size})" else "All Audiobooks (${allBooks.size})",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(filteredBooks, key = { it.id }) { book ->
                BookListItem(
                    book = book,
                    onClick = {
                        viewModel.playAudiobook(book)
                        onBookClick(book)
                    },
                    onFavoriteToggle = { viewModel.toggleFavorite(book) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun BookCard(book: Audiobook, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(140.dp).height(210.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (book.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = "Cover for ${book.title}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(AccentTeal.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(book.title.take(1), fontSize = 48.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(8.dp)
            ) {
                Text(book.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(book.author, color = Color.LightGray, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun BookListItem(
    book: Audiobook,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceGlass)
            .clickable { onClick() }
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (book.coverUrl.isNotBlank()) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = "Cover for ${book.title}",
                modifier = Modifier.size(84.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.size(84.dp).background(AccentTeal.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(book.title.take(1), fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(book.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
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
    }
}
