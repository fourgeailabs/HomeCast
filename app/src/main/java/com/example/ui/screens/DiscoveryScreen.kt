package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.android.gms.location.LocationServices

@Composable
fun DiscoveryScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var location by remember { mutableStateOf<Location?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasLocationPermission = granted
        }
    )

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                    location = loc
                }
            } catch (e: SecurityException) {
                // Ignore
            }
        }
    }

    val recommendations by viewModel.recommendations.collectAsState()
    val isLoading by viewModel.isDiscoveryLoading.collectAsState()
    val error by viewModel.discoveryError.collectAsState()

    var customPrompt by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Popular Audiobooks") }

    val categories = listOf(
        "Popular Audiobooks",
        "Sci-Fi & Fantasy",
        "Mindset & Focus",
        "Mystery & Crime",
        "Acoustic & Chill Music",
        "Epic Soundtracks",
        "Nearby Culture"
    )

    // Initial load on first render if recommendations are empty
    LaunchedEffect(Unit) {
        if (recommendations.isEmpty()) {
            viewModel.fetchDiscoveryRecommendations("Recommend top trending audiobooks and standout music albums")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("AI Discovery", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                "Discover new audiobooks and music tailored by Gemini AI",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Custom Search / Prompt Field
        item {
            OutlinedTextField(
                value = customPrompt,
                onValueChange = { customPrompt = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ask Gemini (e.g. 'Gripping mystery audiobooks with great narrators')") },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (customPrompt.isNotBlank()) {
                                viewModel.fetchDiscoveryRecommendations(customPrompt, location)
                            }
                        },
                        enabled = customPrompt.isNotBlank() && !isLoading
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Search AI", tint = AccentTeal)
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

        // Category Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCategory = category
                            if (category == "Nearby Culture") {
                                if (!hasLocationPermission) {
                                    launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                } else {
                                    viewModel.fetchDiscoveryRecommendations("Recommend audiobooks and albums inspired by local culture and geography", location)
                                }
                            } else {
                                viewModel.fetchDiscoveryRecommendations("Recommend exceptional items in the category: $category", location)
                            }
                        },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentTeal.copy(alpha = 0.25f),
                            selectedLabelColor = AccentTeal
                        )
                    )
                }
            }
        }

        // Location Banner if category is Nearby Culture and not granted
        if (selectedCategory == "Nearby Culture" && !hasLocationPermission) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = AccentTeal)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Location Permission", fontWeight = FontWeight.Bold)
                            Text("Enable location to get regional & geographical recommendations.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = { launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }) {
                            Text("Enable")
                        }
                    }
                }
            }
        }

        // Recommendations List
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentTeal)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Gemini is curating recommendations...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            }
        } else if (error != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Error fetching recommendations", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(error ?: "", fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.fetchDiscoveryRecommendations("Recommend top trending audiobooks and music", location) }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        } else {
            items(recommendations) { itemText ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentTeal.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(22.dp))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(itemText, fontSize = 14.sp, color = TextPrimary, lineHeight = 20.sp)
                        }

                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(itemText))
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy title", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
