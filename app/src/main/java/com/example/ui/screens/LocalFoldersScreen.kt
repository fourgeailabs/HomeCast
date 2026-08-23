package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocalFolderConfig
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalFoldersScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val localFolders by viewModel.localFolders.collectAsState()
    val isScanning by viewModel.isScanningFolders.collectAsState()
    val isEnriching by viewModel.isEnrichingLocalMedia.collectAsState()
    val scanMessage by viewModel.folderScanMessage.collectAsState()
    val context = LocalContext.current

    var selectedMediaTypeForAdd by remember { mutableStateOf("AUDIOBOOK") }
    var showAddDialog by remember { mutableStateOf(false) }
    var manualPathInput by remember { mutableStateOf("") }
    var manualNameInput by remember { mutableStateOf("") }

    // SAF Folder Tree Picker
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not supported
            }
            val folderName = uri.lastPathSegment?.substringAfterLast(':') ?: "Local Storage"
            viewModel.addLocalFolder(
                mediaType = selectedMediaTypeForAdd,
                folderPath = uri.toString(),
                displayName = manualNameInput.ifBlank { folderName }
            )
            showAddDialog = false
            manualNameInput = ""
            manualPathInput = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Local Device Folders", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Import offline audio, e-books & music from storage", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.scanAllLocalFolders() }, enabled = !isScanning) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan All Folders", tint = AccentTeal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Media Intelligence & Scan Banner
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceGlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentTeal.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Local Media & AI Metadata Engine", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Scans local storage & enriches with covers and bios", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Add directories from your device storage for Audiobooks, E-Books & Comics, or Music. All discovered files automatically appear in your Personal Library tab. HomeCast AI can retrieve missing cover art, look up author/artist biographies, and clean messy filenames.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.scanAllLocalFolders() },
                                enabled = !isScanning && !isEnriching && localFolders.isNotEmpty(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isScanning) "Scanning..." else "Scan Folders", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { viewModel.enrichLocalMediaWithAI() },
                                enabled = !isScanning && !isEnriching,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isEnriching) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isEnriching) "Enriching..." else "AI Enrich", fontSize = 12.sp)
                            }
                        }

                        if (!scanMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                scanMessage ?: "",
                                fontSize = 12.sp,
                                color = AccentTeal,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Media Categories
            val mediaTypes = listOf(
                Triple("AUDIOBOOK", "Audiobooks Folders", Icons.Default.Headphones),
                Triple("EBOOK", "E-Books & Comics Folders", Icons.Default.Book),
                Triple("MUSIC", "Music Folders", Icons.Default.MusicNote)
            )

            mediaTypes.forEach { (typeKey, typeTitle, typeIcon) ->
                val typeFolders = localFolders.filter { it.mediaType.equals(typeKey, ignoreCase = true) }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(typeIcon, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(typeTitle, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }
                        TextButton(
                            onClick = {
                                selectedMediaTypeForAdd = typeKey
                                showAddDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentIndigo)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Folder", color = AccentIndigo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (typeFolders.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceGlass.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedMediaTypeForAdd = typeKey
                                        showAddDialog = true
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("No folder selected for $typeTitle", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("Tap to select or enter a storage folder", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(typeFolders, key = { it.id }) { folder ->
                        LocalFolderItemCard(
                            folder = folder,
                            isScanning = isScanning,
                            onRescan = { viewModel.scanFolder(folder) },
                            onDelete = { viewModel.deleteLocalFolder(folder) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Add Folder Dialog
    if (showAddDialog) {
        val typeLabel = when (selectedMediaTypeForAdd) {
            "AUDIOBOOK" -> "Audiobooks"
            "EBOOK" -> "E-Books & Comics"
            else -> "Music"
        }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                manualNameInput = ""
                manualPathInput = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = AccentTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add $typeLabel Folder", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Choose a folder using the system directory browser or type a custom directory path.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = manualNameInput,
                        onValueChange = { manualNameInput = it },
                        label = { Text("Display Name") },
                        placeholder = { Text("e.g. My $typeLabel Collection") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { folderPickerLauncher.launch(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Browse Device Storage")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Or enter manual file path:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = manualPathInput,
                        onValueChange = { manualPathInput = it },
                        label = { Text("Absolute Storage Path") },
                        placeholder = { Text("e.g. /storage/emulated/0/$typeLabel") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Storage Presets shortcuts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Audiobooks", "Books", "Music", "Download").forEach { preset ->
                            OutlinedButton(
                                onClick = { manualPathInput = "/storage/emulated/0/$preset" },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(preset, fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualPathInput.isNotBlank()) {
                            viewModel.addLocalFolder(
                                mediaType = selectedMediaTypeForAdd,
                                folderPath = manualPathInput.trim(),
                                displayName = manualNameInput.ifBlank { manualPathInput.substringAfterLast('/') }
                            )
                            showAddDialog = false
                            manualNameInput = ""
                            manualPathInput = ""
                        }
                    },
                    enabled = manualPathInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                ) {
                    Text("Add Path")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        manualNameInput = ""
                        manualPathInput = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LocalFolderItemCard(
    folder: LocalFolderConfig,
    isScanning: Boolean,
    onRescan: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    val lastScanStr = if (folder.lastScanned > 0) sdf.format(Date(folder.lastScanned)) else "Never scanned"

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceGlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            folder.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            folder.folderPath,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRescan, enabled = !isScanning, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Sync, contentDescription = "Rescan", tint = AccentIndigo, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "${folder.fileCount} items discovered",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    "Last scan: $lastScanStr",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
