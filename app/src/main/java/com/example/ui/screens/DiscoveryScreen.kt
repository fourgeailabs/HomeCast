package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder

enum class DiscoveryMediaType {
    BOOK, AUDIOBOOK, MUSIC
}

data class DiscoveryItem(
    val title: String,
    val creator: String,
    val genre: String,
    val description: String,
    val mediaType: DiscoveryMediaType,
    val coverUrl: String
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    viewModel: MainViewModel,
    onNavigateToDetails: (String, String, String) -> Unit = { _, _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSource by remember { mutableIntStateOf(0) } // 0 = Personal, 1 = Public Domain
    val isLoading by viewModel.isDiscoveryLoading.collectAsState()
    val geminiCategoryItems by viewModel.geminiCategoryItems.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp)) {
        Text(
            "Discover",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text("AI-Generated Mixes & Blends", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedSource == 0,
                onClick = { selectedSource = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Private Library")
            }
            SegmentedButton(
                selected = selectedSource == 1,
                onClick = { selectedSource = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Public Domain")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ask AI to create a mix (e.g. Cyberpunk Noir)...") },
            leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = AccentIndigo) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = {
                        val sourceStr = if (selectedSource == 0) "the user's private library" else "public domain archives"
                        viewModel.fetchGeminiCategoryItems(searchQuery, "Use $sourceStr for recommendations.")
                    }) {
                        Icon(Icons.Default.Send, contentDescription = "Generate", tint = AccentIndigo)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceGlass,
                unfocusedContainerColor = SurfaceGlass,
                focusedBorderColor = AccentIndigo.copy(alpha = 0.8f),
                unfocusedBorderColor = SurfaceGlassBorder
            )
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentIndigo)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI is curating your blend...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (geminiCategoryItems.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(geminiCategoryItems) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                        modifier = Modifier.fillMaxWidth().clickable {
                            val detailType = when (item.mediaType) {
                                DiscoveryMediaType.BOOK -> "BOOK"
                                DiscoveryMediaType.AUDIOBOOK -> "AUDIOBOOK"
                                DiscoveryMediaType.MUSIC -> "MUSIC"
                            }
                            onNavigateToDetails(item.title, item.creator, detailType)
                        }
                    ) {
                        Column {
                            AsyncImage(
                                model = item.coverUrl,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            )
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.creator, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        when(item.mediaType) {
                                            DiscoveryMediaType.AUDIOBOOK -> Icons.Default.Headphones
                                            DiscoveryMediaType.MUSIC -> Icons.Default.MusicNote
                                            else -> Icons.Default.MenuBook
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = AccentIndigo
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(item.genre, fontSize = 10.sp, color = AccentIndigo, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Search for a mood, genre, or vibe to get an AI curated mix.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}
