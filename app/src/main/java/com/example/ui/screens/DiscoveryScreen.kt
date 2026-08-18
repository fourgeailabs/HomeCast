package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import com.example.RetrofitClient
import com.example.GenerateContentRequest
import com.example.Content
import com.example.Part
import com.example.BuildConfig
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DiscoveryScreen() {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasLocationPermission = granted
        }
    )

    var location by remember { mutableStateOf<Location?>(null) }
    var recommendations by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                    location = loc
                }
            } catch (e: SecurityException) {
                error = "Permission denied."
            }
        }
    }

    LaunchedEffect(location) {
        if (location != null) {
            isLoading = true
            scope.launch {
                try {
                    val prompt = "Based on the geographic coordinates: Latitude ${location?.latitude}, Longitude ${location?.longitude}, identify the general area/city and recommend 3 audiobooks that fit the interests or culture of people in this area. Just return a bulleted list with book titles and authors, no extra text."
                    val request = GenerateContentRequest(
                        contents = listOf(Content(parts = listOf(Part(text = prompt))))
                    )
                    val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
                    val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                    recommendations = text.split("\n").filter { it.isNotBlank() }
                } catch (e: Exception) {
                    error = "Failed to fetch recommendations: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Social Discovery", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (!hasLocationPermission) {
            Button(
                onClick = { launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = Color.White)
            ) {
                Text("Enable Location for Local Recommendations")
            }
        } else {
            if (location == null) {
                Text("Locating...", color = TextSecondary)
            } else {
                Text("Recommendations near you:", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AccentTeal)
                Spacer(modifier = Modifier.height(16.dp))
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = AccentTeal)
                } else if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recommendations) { rec ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceGlass)
                                    .border(1.dp, SurfaceGlassBorder, RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(AccentTeal.copy(alpha = 0.2f)).border(1.dp, AccentTeal.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📍", fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(rec, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
