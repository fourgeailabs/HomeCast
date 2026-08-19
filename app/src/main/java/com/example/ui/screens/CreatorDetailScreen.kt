package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
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
fun CreatorDetailScreen(
    viewModel: MainViewModel,
    creatorName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var details by remember { mutableStateOf<Map<String, String>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(creatorName) {
        isLoading = true
        val result = viewModel.fetchCreatorDetailsWithGemini(creatorName)
        details = result
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", fontWeight = FontWeight.Bold) },
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
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = data["imageUrl"] ?: "",
                        contentDescription = creatorName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(SurfaceGlass)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(creatorName, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(data["roles"] ?: "", fontSize = 14.sp, color = AccentTeal)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        data["bio"] ?: "No biography available.", 
                        fontSize = 15.sp, 
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    data["wikiLink"]?.takeIf { it.isNotBlank() && it != "N/A" }?.let { url ->
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
                            Text("Wikipedia Page")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
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
                            Text("Official Website")
                        }
                    }
                }
            } else {
                Text("Failed to load details.", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
