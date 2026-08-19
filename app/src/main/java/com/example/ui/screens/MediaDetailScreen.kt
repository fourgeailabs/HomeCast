package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailScreen(
    viewModel: MainViewModel,
    title: String,
    creator: String,
    type: String, // "BOOK", "AUDIOBOOK", "MUSIC"
    onBack: () -> Unit,
    onCreatorClick: (String) -> Unit,
    onPlayReadClick: () -> Unit
) {
    val context = LocalContext.current
    var details by remember { mutableStateOf<Map<String, String>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(title, creator) {
        isLoading = true
        val result = viewModel.fetchDetailsWithGemini(title, creator, type)
        details = result
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details", fontWeight = FontWeight.Bold) },
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
                        .padding(20.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = data["coverUrl"] ?: "",
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(120.dp)
                                .aspectRatio(if (type == "MUSIC") 1f else 0.68f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceGlass)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                creator, 
                                fontSize = 16.sp, 
                                color = AccentTeal,
                                modifier = Modifier.clickable { onCreatorClick(creator) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(data["rating"] ?: "N/A", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Publisher: ${data["publisher"] ?: "Unknown"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onPlayReadClick,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(if (type == "BOOK") Icons.Default.MenuBook else Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (type == "BOOK") "Read" else "Play")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Synopsis", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(data["bio"] ?: "No synopsis available.", fontSize = 14.sp, lineHeight = 20.sp)

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
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Visit Official Website")
                        }
                    }
                }
            } else {
                Text("Failed to load details.", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
