package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.OfflinePin
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
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder
import com.example.ui.theme.AccentIndigo

@Composable
fun LibraryScreen(
    books: List<Audiobook>,
    onBookClick: (Audiobook) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text("Recently Played", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(books) { book ->
                    BookCard(book, onClick = { onBookClick(book) })
                }
            }
        }
        item {
            Text("Favorites", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(books.filter { it.isFavorite }) { book ->
                    BookCard(book, onClick = { onBookClick(book) })
                }
            }
        }
        item {
            Text("New and Noteworthy", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(books.reversed()) { book ->
                    BookCard(book, onClick = { onBookClick(book) })
                }
            }
        }
        item {
            Text("All Audiobooks", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        items(books) { book ->
            BookListItem(book, onClick = { onBookClick(book) })
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun BookCard(book: Audiobook, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(140.dp).height(200.dp).clickable { onClick() },
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
                    modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(book.title.take(1), fontSize = 48.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp)
            ) {
                Text(book.title, color = Color.White, fontSize = 14.sp, maxLines = 1)
                Text(book.author, color = Color.LightGray, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun BookListItem(book: Audiobook, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceGlass).clickable { onClick() }.padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (book.coverUrl.isNotBlank()) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = "Cover for ${book.title}",
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.size(80.dp).background(Color.Gray.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                 Text(book.title.take(1), fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(book.title, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(book.author, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Icon(
            imageVector = if (book.isDownloaded) Icons.Default.OfflinePin else Icons.Default.DownloadForOffline,
            contentDescription = "Download",
            tint = if (book.isDownloaded) AccentIndigo else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
