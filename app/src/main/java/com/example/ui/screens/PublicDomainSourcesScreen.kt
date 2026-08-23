package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PublicDomainSource
import com.example.data.VerificationResult
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicDomainSourcesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val sources by viewModel.publicDomainSources.collectAsState()
    val isVerifying by viewModel.isVerifyingSource.collectAsState()
    val verificationResult by viewModel.verificationResult.collectAsState()

    var inputUrl by remember { mutableStateOf("") }
    var inputName by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Public Domain Sources", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Manage open-access media catalogs & feeds", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Source", tint = AccentTeal)
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
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("AI Source Verification Engine", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Automated URL validation, feed repair & type mapping", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Type or paste any open repository website or archive collection. HomeCast AI analyzes the catalog structure, fixes broken protocols/paths, detects supported media types, and requests your confirmation before adding.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Custom Public Domain Source")
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Configured Catalogs (${sources.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            if (sources.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No sources configured", fontWeight = FontWeight.Medium)
                            Text("Tap 'Add Custom Public Domain Source' to connect a catalog.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(sources, key = { it.id }) { source ->
                    PublicDomainSourceItem(
                        source = source,
                        onToggle = { viewModel.togglePublicDomainSource(source.id, it) },
                        onDelete = { viewModel.deletePublicDomainSource(source.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Add Source Input Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isVerifying) {
                    showAddDialog = false
                    inputUrl = ""
                    inputName = ""
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = AccentTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Public Domain Source", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Enter the website or catalog link. The AI will inspect and repair the endpoint before connecting.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = { Text("Website or Catalog URL") },
                        placeholder = { Text("e.g., standardebooks.org or archive.org/details/...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Custom Name (Optional)") },
                        placeholder = { Text("e.g., Standard Ebooks Vault") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isVerifying) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AccentTeal)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("AI analyzing and repairing source...", fontSize = 13.sp, color = AccentTeal)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputUrl.isNotBlank()) {
                            viewModel.verifyPublicDomainSource(inputUrl, inputName.ifBlank { null })
                        }
                    },
                    enabled = inputUrl.isNotBlank() && !isVerifying,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Verify with AI")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        inputUrl = ""
                        inputName = ""
                    },
                    enabled = !isVerifying
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // AI Verification Confirmation Modal
    if (verificationResult != null) {
        val result = verificationResult!!
        AlertDialog(
            onDismissRequest = { viewModel.clearVerificationResult() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (result.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (result.isValid) AccentTeal else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Verification Complete", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (result.requiresCorrection) "HomeCast AI repaired the URL and verified catalog access:"
                        else "HomeCast AI successfully verified the media source:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Source Name: ${result.sourceName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (result.requiresCorrection) {
                                Text("Original: ${result.originalUrl}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("AI Corrected: ${result.correctedUrl}", fontSize = 12.sp, color = AccentTeal, fontWeight = FontWeight.SemiBold)
                            } else {
                                Text("Verified URL: ${result.correctedUrl}", fontSize = 12.sp, color = AccentTeal)
                            }
                            Text("Media Formats: ${result.mediaTypes.joinToString(", ")}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Text(
                        result.explanation,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    Text(
                        "Would you like to confirm and activate this source in your public catalog?",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmAndSaveVerifiedSource(result)
                        showAddDialog = false
                        inputUrl = ""
                        inputName = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm & Add Source")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearVerificationResult() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PublicDomainSourceItem(
    source: PublicDomainSource,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceGlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AccentIndigo.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                source.mediaTypes.contains("AUDIOBOOK") -> Icons.Default.Headphones
                                source.mediaTypes.contains("MUSIC") -> Icons.Default.MusicNote
                                source.mediaTypes.contains("COMIC") -> Icons.Default.MenuBook
                                else -> Icons.Default.Book
                            },
                            contentDescription = null,
                            tint = AccentIndigo,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                source.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (source.isDefault) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AccentTeal.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        "Curated",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentTeal,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            source.verifiedUrl,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Switch(
                    checked = source.isEnabled,
                    onCheckedChange = onToggle
                )
            }

            if (source.aiExplanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    source.aiExplanation,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    source.mediaTypes.split(",").forEach { type ->
                        val cleanType = type.trim()
                        if (cleanType.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    cleanType,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                if (!source.isDefault) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
