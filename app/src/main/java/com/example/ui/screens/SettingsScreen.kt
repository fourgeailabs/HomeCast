package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ServerConfig
import com.example.data.network.AbsDiagnosticResult
import com.example.data.network.PlexDiagnosticResult
import com.example.ui.MainViewModel
import com.example.ui.ServerOperationState
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.LocalThemeMode
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder
import java.text.SimpleDateFormat
import java.util.*

enum class SettingsSubMenu {
    SERVERS,
    PUBLIC_DOMAIN_SOURCES,
    LOCAL_FOLDERS
}

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onThemeToggle: (Boolean) -> Unit
) {
    var currentSubMenu by remember { mutableStateOf<SettingsSubMenu?>(null) }

    when (currentSubMenu) {
        SettingsSubMenu.SERVERS -> {
            BackHandler { currentSubMenu = null }
            ServerConnectionsSubScreen(
                viewModel = viewModel,
                onBack = { currentSubMenu = null }
            )
        }
        SettingsSubMenu.PUBLIC_DOMAIN_SOURCES -> {
            BackHandler { currentSubMenu = null }
            PublicDomainSourcesScreen(
                viewModel = viewModel,
                onBack = { currentSubMenu = null }
            )
        }
        SettingsSubMenu.LOCAL_FOLDERS -> {
            BackHandler { currentSubMenu = null }
            LocalFoldersScreen(
                viewModel = viewModel,
                onBack = { currentSubMenu = null }
            )
        }
        null -> {
            MainSettingsMenu(
                viewModel = viewModel,
                onThemeToggle = onThemeToggle,
                onNavigateToSubMenu = { currentSubMenu = it }
            )
        }
    }
}

@Composable
private fun MainSettingsMenu(
    viewModel: MainViewModel,
    onThemeToggle: (Boolean) -> Unit,
    onNavigateToSubMenu: (SettingsSubMenu) -> Unit
) {
    val isDarkTheme = LocalThemeMode.current
    val servers by viewModel.servers.collectAsState()
    val publicDomainSources by viewModel.publicDomainSources.collectAsState()
    val localFolders by viewModel.localFolders.collectAsState()
    val serverOpState by viewModel.serverOpState.collectAsState()
    val hasSilentBackup by viewModel.hasSilentBackup.collectAsState()
    val context = LocalContext.current
    var showWhatsNewDialog by remember { mutableStateOf(false) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportBackup(context, it) }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importBackup(context, it) }
    }

    LaunchedEffect(serverOpState) {
        when (serverOpState) {
            is ServerOperationState.Success -> {
                Toast.makeText(context, (serverOpState as ServerOperationState.Success).message, Toast.LENGTH_LONG).show()
                viewModel.resetServerOpState()
            }
            is ServerOperationState.Error -> {
                Toast.makeText(context, (serverOpState as ServerOperationState.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetServerOpState()
            }
            else -> {}
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Settings & Connections", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }

        if (servers.isEmpty() && hasSilentBackup) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Found Automatic Settings Backup!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "We detected a settings backup in your public Downloads folder. Tap below to automatically restore all your server configurations, passwords, and preferences in 1-click.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.restoreFromSilentBackup() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore Settings Now")
                        }
                    }
                }
            }
        }

        // Submenus Section
        item {
            Text("Media & Server Configuration", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        item {
            SettingsSubMenuEntryCard(
                title = "Server Connections",
                subtitle = "${servers.size} personal server(s) configured",
                description = "Manage Audiobookshelf, Plex, Booklore, Komga, Kavita & Jellyfin connections",
                icon = Icons.Default.Dns,
                iconTint = AccentTeal,
                onClick = { onNavigateToSubMenu(SettingsSubMenu.SERVERS) }
            )
        }

        item {
            SettingsSubMenuEntryCard(
                title = "Public Domain Sources",
                subtitle = "${publicDomainSources.count { it.isEnabled }} active catalog feed(s)",
                description = "Add or change open-access sources with automated AI verification & URL repair",
                icon = Icons.Default.Language,
                iconTint = AccentIndigo,
                onClick = { onNavigateToSubMenu(SettingsSubMenu.PUBLIC_DOMAIN_SOURCES) }
            )
        }

        item {
            SettingsSubMenuEntryCard(
                title = "Local Device Folders",
                subtitle = "${localFolders.size} storage directory(s) imported",
                description = "Import Audiobooks, E-Books & Music from local storage with AI cover & bio enrichment",
                icon = Icons.Default.FolderSpecial,
                iconTint = Color(0xFFE6A23C),
                onClick = { onNavigateToSubMenu(SettingsSubMenu.LOCAL_FOLDERS) }
            )
        }

        // App Preferences
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Preferences", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        item {
            // Theme Setting
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceGlass
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceGlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Theme",
                            tint = AccentTeal
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Dark Mode", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Switch(checked = isDarkTheme, onCheckedChange = { onThemeToggle(it) })
                }
            }
        }

        // Backup & Restore settings card
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Backup & Restore Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceGlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Preserve Your Settings Offline",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Export all your server connections, logins, API tokens, and preferences to a secure local file. If you reinstall or update the app, simply import this file to restore everything instantly.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { exportBackupLauncher.launch("homecast_backup.json") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { importBackupLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                        ) {
                            Icon(Icons.Default.RestorePage, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("AI Magic Optimizer", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceGlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Intelligent Library Curation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Optimize and refine your media library automatically or on-demand using Google Gemini API.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val isCleaningUp by viewModel.isCleaningUp.collectAsState()
                    val isLocatingCovers by viewModel.isLocatingCovers.collectAsState()

                    Button(
                        onClick = { viewModel.triggerManualDailyCleanup() },
                        enabled = !isCleaningUp,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                    ) {
                        if (isCleaningUp) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Curating Library...")
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Refresh Daily Menus & Authors")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.triggerManualCoverLocation() },
                        enabled = !isLocatingCovers,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) {
                        if (isLocatingCovers) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Locating Cover Art...")
                        } else {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan & Locate Missing Covers")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("About", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceGlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("HomeCast", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("Version ${com.example.BuildConfig.VERSION_NAME}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Created by ", fontSize = 14.sp)
                        Text(
                            text = "FourgeAI LABS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentIndigo,
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://github.com/fourgeailabs")
                            }
                        )
                    }
                    Text("© 2026", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("AI-Powered Features", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "✨ Public Domain Source Verifier\nInspects and fixes open-access media links using Gemini before adding.\n\n" +
                        "✨ Local Media Scanner & Enricher\nRetrieves high-res cover art and author/artist biographies for offline storage files.\n\n" +
                        "✨ Dynamic Daily Menus & Themes\nRotates category bookshelves automatically at midnight to keep collections fresh daily.\n\n" +
                        "✨ Intelligent Author Cleaning\nRefines messy catalog indexing into clean human author names.\n\n" +
                        "✨ AI Discovery Blends\nGenerate bespoke thematic playlists and discovery mixes in the Discovery tab.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { showWhatsNewDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                    ) {
                        Icon(Icons.Default.NewReleases, contentDescription = "What's New")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("What's New in this Update")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://github.com/fourgeailabs/HomeCast") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.Code, contentDescription = "GitHub")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View on GitHub")
                    }
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showWhatsNewDialog) {
        WhatsNewDialog(onDismiss = { showWhatsNewDialog = false })
    }
}

@Composable
fun SettingsSubMenuEntryCard(
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceGlassBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, fontSize = 12.sp, color = iconTint, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerConnectionsSubScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val servers by viewModel.servers.collectAsState()
    val serverOpState by viewModel.serverOpState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Server Connections", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Manage Audiobookshelf, Plex, Booklore, Komga & Kavita", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Active Connected Servers
            if (servers.isNotEmpty()) {
                item {
                    Text("Configured Servers (${servers.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                items(servers) { server ->
                    ServerItemCard(
                        server = server,
                        onSync = { viewModel.syncServer(server) },
                        onDelete = { viewModel.removeServer(server.id) },
                        isLoading = serverOpState is ServerOperationState.Loading
                    )
                }
            } else {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No Personal Servers Added Yet", fontWeight = FontWeight.Medium)
                            Text("Configure an Audiobookshelf, Plex, or Booklore server below.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Add New Server Connection", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            // Audiobookshelf Config Card
            item {
                AudiobookshelfConfigCard(
                    viewModel = viewModel,
                    isLoading = serverOpState is ServerOperationState.Loading,
                    serverOpState = serverOpState,
                    onConnect = { name, url, token, username, password ->
                        viewModel.saveAndConnectAudiobookshelf(name, url, token, username, password)
                    }
                )
            }

            // Plex Config Card
            item {
                PlexConfigCard(
                    viewModel = viewModel,
                    isLoading = serverOpState is ServerOperationState.Loading,
                    onConnect = { name, url, token ->
                        viewModel.saveAndConnectPlexDirect(name, url, token)
                    }
                )
            }

            // Booklore Config Card
            item {
                BookloreConfigCard(
                    isLoading = serverOpState is ServerOperationState.Loading,
                    onConnect = { name, url, username, password ->
                        viewModel.saveAndConnectBooklore(name, url, username, password)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ServerItemCard(
    server: ServerConfig,
    onSync: () -> Unit,
    onDelete: () -> Unit,
    isLoading: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (server.type == "audiobookshelf") Icons.Default.Book else Icons.Default.MusicNote,
                        contentDescription = server.type,
                        tint = if (server.type == "audiobookshelf") AccentTeal else AccentIndigo
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(server.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(server.hostUrl, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                AssistChip(
                    onClick = {},
                    label = { Text(if (server.isConnected) "Connected" else "Offline", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Status",
                            tint = if (server.isConnected) Color.Green else Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }

            if (server.lastSyncTime > 0) {
                val formattedTime = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(server.lastSyncTime))
                Text(
                    "Last synced: $formattedTime",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDelete, enabled = !isLoading) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(onClick = onSync, enabled = !isLoading) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sync Library")
                }
            }
        }
    }
}

@Composable
fun AudiobookshelfConfigCard(
    viewModel: MainViewModel,
    isLoading: Boolean,
    serverOpState: ServerOperationState,
    onConnect: (String, String, String, String, String) -> Unit
) {
    var serverName by remember { mutableStateOf("AudioBookShelf") }
    var hostUrl by remember { mutableStateOf("http://10.70.14.2:13378") }
    var token by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("ecollins") }
    var password by remember { mutableStateOf("") }
    var useTokenAuth by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var connectionMode by remember { mutableStateOf("local") }

    val diagnosticResult by viewModel.diagnosticResult.collectAsState()
    val isDiagnosing by viewModel.isDiagnosing.collectAsState()
    var showDiagnosticDialog by remember { mutableStateOf(false) }

    var isExpanded by remember { mutableStateOf(false) }
    
    LaunchedEffect(diagnosticResult) {
        if (diagnosticResult != null) {
            showDiagnosticDialog = true
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Book, contentDescription = "Audiobookshelf", tint = AccentTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Audiobookshelf Server", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Audiobooks, Podcasts & E-books", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { showHelp = !showHelp }) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "Help", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

            AnimatedVisibility(visible = showHelp) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Audiobookshelf Remote & Login Guide:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = AccentTeal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "1. Remote / HTTPS Access: Enter your remote domain (e.g., https://abs.yourdomain.com) or Tailscale IP. HomeCast supports HTTPS, reverse proxies, and self-signed certificates.\n" +
                            "2. API Token (Direct & Fast): If username/password fails due to Single Sign-On (SSO), Cloudflare Access, or 2FA, use an API Token. In Audiobookshelf: go to Settings -> Users -> click your user -> copy API Key / Token.\n" +
                            "3. Diagnostic Mode: Tap 'Test & Diagnose' below to check reachability, latency, and pinpoint exact connection issues.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Connection Mode Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = connectionMode == "local",
                    onClick = {
                        connectionMode = "local"
                        if (hostUrl.startsWith("https://") || hostUrl.isBlank()) {
                            hostUrl = "http://10.70.14.2:13378"
                        }
                    },
                    label = { Text("Local LAN", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentTeal.copy(alpha = 0.25f),
                        selectedLabelColor = AccentTeal
                    ),
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = connectionMode == "remote",
                    onClick = {
                        connectionMode = "remote"
                        if (hostUrl.startsWith("http://10.") || hostUrl.startsWith("http://192.")) {
                            hostUrl = "https://"
                        }
                    },
                    label = { Text("Remote / HTTPS", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentIndigo.copy(alpha = 0.25f),
                        selectedLabelColor = AccentIndigo
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = serverName,
                onValueChange = { serverName = it },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = hostUrl,
                onValueChange = { hostUrl = it },
                label = { Text(if (connectionMode == "remote") "Remote Domain URL (e.g. https://abs.domain.com)" else "Server URL (e.g. http://10.70.14.2:13378)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = useTokenAuth, onCheckedChange = { useTokenAuth = it })
                Text("Use API Token instead of Username/Password", fontSize = 13.sp)
            }

            if (useTokenAuth) {
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("API Token / Key") },
                    placeholder = { Text("Paste Bearer token or API key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            } else {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        autoCorrect = false
                    ),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dual Buttons: Test & Diagnose / Save & Connect
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.diagnoseAudiobookshelf(
                            baseUrl = hostUrl.trim(),
                            username = if (!useTokenAuth) username.trim() else "",
                            password = if (!useTokenAuth) password else "",
                            token = if (useTokenAuth) token.trim() else ""
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isDiagnosing && !isLoading && hostUrl.isNotBlank()
                ) {
                    if (isDiagnosing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test & Diagnose", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        onConnect(
                            serverName.trim(),
                            hostUrl.trim(),
                            token.trim(),
                            username.trim(),
                            password
                        )
                    },
                    modifier = Modifier.weight(1.3f),
                    enabled = !isLoading && !isDiagnosing && hostUrl.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connecting...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.Link, contentDescription = "Connect", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Connect", fontSize = 12.sp)
                    }
                }
            }
            }
            }
        }
    }

    // Diagnostic Results Dialog
    if (showDiagnosticDialog && diagnosticResult != null) {
        val result = diagnosticResult!!
        AlertDialog(
            onDismissRequest = {
                showDiagnosticDialog = false
                viewModel.clearDiagnosticResult()
            },
            icon = {
                Icon(
                    imageVector = when {
                        result.success -> Icons.Default.CheckCircle
                        result.isReachable -> Icons.Default.Warning
                        else -> Icons.Default.Error
                    },
                    contentDescription = null,
                    tint = when {
                        result.success -> Color(0xFF4CAF50)
                        result.isReachable -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (result.success) "Connection Verified" else "Diagnostic Report",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = result.statusMessage,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (result.success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Tested URL: ${result.testedUrl}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("Latency: ${result.latencyMs} ms", fontSize = 12.sp)
                            if (result.httpStatusCode != null) {
                                Text("HTTP Status Code: ${result.httpStatusCode}", fontSize = 12.sp)
                            }
                            if (result.librariesFound > 0) {
                                Text("Libraries Accessible: ${result.librariesFound}", fontSize = 12.sp, color = AccentTeal)
                            }
                        }
                    }

                    if (result.diagnosticLog.isNotEmpty()) {
                        Text("Diagnostic Logs:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            color = Color.Black.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                for (logLine in result.diagnosticLog) {
                                    Text(
                                        text = logLine,
                                        fontSize = 11.sp,
                                        color = if (logLine.contains("SUCCESS") || logLine.contains("VALID")) Color(0xFF81C784) else Color(0xFFE0E0E0),
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    if (result.recommendations.isNotEmpty()) {
                        Text("Suggested Fixes:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentIndigo)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (tip in result.recommendations) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("• ", fontSize = 12.sp, color = AccentIndigo)
                                    Text(tip, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (result.success) {
                    Button(
                        onClick = {
                            val activeTok = result.resolvedToken ?: token.trim()
                            showDiagnosticDialog = false
                            viewModel.clearDiagnosticResult()
                            onConnect(
                                serverName.trim(),
                                result.testedUrl,
                                activeTok,
                                username.trim(),
                                password
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) {
                        Text("Save & Connect Now")
                    }
                } else {
                    Button(
                        onClick = {
                            showDiagnosticDialog = false
                            viewModel.clearDiagnosticResult()
                        }
                    ) {
                        Text("Close")
                    }
                }
            },
            dismissButton = {
                if (result.success) {
                    TextButton(onClick = {
                        showDiagnosticDialog = false
                        viewModel.clearDiagnosticResult()
                    }) {
                        Text("Dismiss")
                    }
                }
            }
        )
    }
}

@Composable
fun PlexConfigCard(
    viewModel: MainViewModel,
    isLoading: Boolean,
    onConnect: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var serverName by remember { mutableStateOf("My Plex Server") }
    var hostUrl by remember { mutableStateOf("http://192.168.1.100:32400") }
    var plexToken by remember { mutableStateOf("") }
    var showManualSetup by remember { mutableStateOf(false) }
    var connectionMode by remember { mutableStateOf("local") }

    val plexDiagnosticResult by viewModel.plexDiagnosticResult.collectAsState()
    val isDiagnosingPlex by viewModel.isDiagnosingPlex.collectAsState()
    val plexPinCode by viewModel.plexPinCode.collectAsState()
    val plexAuthToken by viewModel.plexAuthToken.collectAsState()
    val isRequestingPin by viewModel.isRequestingPin.collectAsState()
    val isPollingPin by viewModel.isPollingPin.collectAsState()
    val isDiscoveringPlexServers by viewModel.isDiscoveringPlexServers.collectAsState()
    val discoveredPlexServers by viewModel.discoveredPlexServers.collectAsState()
    val showServerPicker by viewModel.showServerPicker.collectAsState()

    var showDiagnosticDialog by remember { mutableStateOf(false) }

    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(plexDiagnosticResult) {
        if (plexDiagnosticResult != null) {
            showDiagnosticDialog = true
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, contentDescription = "Plex", tint = AccentIndigo)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Plex Server", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Music & Audio Streaming", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))

            // Primary 1-Tap Cloud Account Sign-In Card
            Surface(
                color = AccentIndigo.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentIndigo.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Zero-Config Account Login",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AccentIndigo
                        )
                    }

                    Text(
                        "Sign in once in your browser. HomeCast automatically discovers your owned Plex Media Server and auto-searches and syncs your music library directly.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = { viewModel.requestPlexPin() },
                        enabled = !isRequestingPin && !isDiscoveringPlexServers && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isRequestingPin || isDiscoveringPlexServers) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isRequestingPin) "Generating Code..." else "Discovering Servers...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign In with Plex Account", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Discovered Servers Quick List (if available)
            if (discoveredPlexServers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text("Discovered Owned Servers on Your Account:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (server in discoveredPlexServers) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(server.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = if (server.isLocal) AccentTeal.copy(alpha = 0.2f) else AccentIndigo.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (server.isLocal) "Local LAN" else "Remote / Secure",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (server.isLocal) AccentTeal else AccentIndigo,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Button(
                                    onClick = { viewModel.connectDiscoveredPlexServer(server) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Connect", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Collapsible Manual / Advanced IP & Token Configuration
            TextButton(
                onClick = { showManualSetup = !showManualSetup },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Advanced: Manual IP & Token Setup",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (showManualSetup) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = showManualSetup) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    // Quick Connection Mode Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = connectionMode == "local",
                            onClick = {
                                connectionMode = "local"
                                if (hostUrl.startsWith("https://") || hostUrl.isBlank()) {
                                    hostUrl = "http://192.168.1.100:32400"
                                }
                            },
                            label = { Text("Local LAN (:32400)", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentIndigo.copy(alpha = 0.25f),
                                selectedLabelColor = AccentIndigo
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = connectionMode == "remote",
                            onClick = {
                                connectionMode = "remote"
                                if (hostUrl.startsWith("http://192.") || hostUrl.startsWith("http://10.")) {
                                    hostUrl = "https://"
                                }
                            },
                            label = { Text("Remote / HTTPS", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentTeal.copy(alpha = 0.25f),
                                selectedLabelColor = AccentTeal
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = serverName,
                        onValueChange = { serverName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = hostUrl,
                        onValueChange = { hostUrl = it },
                        label = { Text("Plex URL (e.g. http://192.168.1.100:32400)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = plexToken,
                        onValueChange = { plexToken = it },
                        label = { Text("X-Plex-Token") },
                        placeholder = { Text("Enter manual token or use account login above") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.diagnosePlex(
                                    serverUrl = hostUrl.trim(),
                                    token = plexToken.trim()
                                )
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isDiagnosingPlex && !isLoading && hostUrl.isNotBlank()
                        ) {
                            if (isDiagnosingPlex) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test & Diagnose", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                onConnect(serverName.trim(), hostUrl.trim(), plexToken.trim())
                            },
                            modifier = Modifier.weight(1.3f),
                            enabled = !isLoading && !isDiagnosingPlex && hostUrl.isNotBlank() && plexToken.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connecting...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Link, contentDescription = "Connect", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save & Connect", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            }
            }
        }
    }

    // Plex Server Picker Dialog (if user has multiple servers on their Plex account)
    if (showServerPicker && discoveredPlexServers.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissServerPicker() },
            icon = {
                Icon(Icons.Default.Dns, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Select Plex Server", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "We found ${discoveredPlexServers.size} owned servers linked to your Plex account (shared servers excluded). Choose which one to connect:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    for (server in discoveredPlexServers) {
                        Surface(
                            onClick = { viewModel.connectDiscoveredPlexServer(server) },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentIndigo.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(server.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        text = if (server.isLocal) "⚡ Local LAN (Fastest)" else "🌐 Remote / Direct",
                                        fontSize = 11.sp,
                                        color = if (server.isLocal) AccentTeal else AccentIndigo
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = "Select", tint = AccentIndigo)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.dismissServerPicker() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Plex PIN Link Flow Dialog
    if (plexPinCode != null) {
        val code = plexPinCode ?: ""
        AlertDialog(
            onDismissRequest = { viewModel.dismissPlexPin() },
            icon = {
                Icon(Icons.Default.VpnKey, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Link Plex Account", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Enter this 4-character code at plex.tv/link to authorize:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    // 4-Letter Digit Display
                    if (code.length == 4) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (char in code) {
                                Surface(
                                    color = AccentIndigo.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentIndigo),
                                    modifier = Modifier.size(width = 46.dp, height = 54.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = char.toString(),
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentIndigo
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            color = AccentIndigo.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentIndigo)
                        ) {
                            Text(
                                text = code,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentIndigo,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                        }
                    }

                    // Quick Copy & Browser Links
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(code))
                                Toast.makeText(context, "Code '$code' copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Code", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val linkUrl = "https://plex.tv/link?code=$code"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                            modifier = Modifier.weight(1.3f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open plex.tv/link", fontSize = 12.sp)
                        }
                    }

                    // Auto-sync helper message
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isPollingPin) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = AccentTeal)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = "Auto-checking plex.tv approval in the background...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.checkPlexPinStatus { receivedToken ->
                            plexToken = receivedToken
                        }
                    },
                    enabled = !isPollingPin,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                ) {
                    Text("Check Manually")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPlexPin() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Plex Diagnostic Results Dialog
    if (showDiagnosticDialog && plexDiagnosticResult != null) {
        val result = plexDiagnosticResult!!
        AlertDialog(
            onDismissRequest = {
                showDiagnosticDialog = false
                viewModel.clearPlexDiagnosticResult()
            },
            icon = {
                Icon(
                    imageVector = when {
                        result.success -> Icons.Default.CheckCircle
                        result.isReachable -> Icons.Default.Warning
                        else -> Icons.Default.Error
                    },
                    contentDescription = null,
                    tint = when {
                        result.success -> Color(0xFF4CAF50)
                        result.isReachable -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (result.success) "Plex Connection Verified" else "Plex Diagnostic Report",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = result.statusMessage,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (result.success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Tested URL: ${result.testedUrl}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("Latency: ${result.latencyMs} ms", fontSize = 12.sp)
                            if (result.httpStatusCode != null) {
                                Text("HTTP Status Code: ${result.httpStatusCode}", fontSize = 12.sp)
                            }
                            if (result.musicSectionsFound > 0) {
                                Text("Music Libraries Found: ${result.musicSectionsFound}", fontSize = 12.sp, color = AccentTeal)
                            }
                            if (result.totalTracksFound > 0) {
                                Text("Tracks Sampled: ${result.totalTracksFound}", fontSize = 12.sp, color = AccentIndigo)
                            }
                        }
                    }

                    if (result.diagnosticLog.isNotEmpty()) {
                        Text("Diagnostic Logs:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            color = Color.Black.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                for (logLine in result.diagnosticLog) {
                                    Text(
                                        text = logLine,
                                        fontSize = 11.sp,
                                        color = if (logLine.contains("AUTHENTICATED") || logLine.contains("SUCCESS")) Color(0xFF81C784) else Color(0xFFE0E0E0),
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    if (result.recommendations.isNotEmpty()) {
                        Text("How to Fix:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentIndigo)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (tip in result.recommendations) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("• ", fontSize = 12.sp, color = AccentIndigo)
                                    Text(tip, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (result.success) {
                    Button(
                        onClick = {
                            showDiagnosticDialog = false
                            viewModel.clearPlexDiagnosticResult()
                            onConnect(serverName.trim(), result.testedUrl, plexToken.trim())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                    ) {
                        Text("Save & Connect Now")
                    }
                } else {
                    Button(
                        onClick = {
                            showDiagnosticDialog = false
                            viewModel.clearPlexDiagnosticResult()
                        }
                    ) {
                        Text("Close")
                    }
                }
            },
            dismissButton = {
                if (result.success) {
                    TextButton(onClick = {
                        showDiagnosticDialog = false
                        viewModel.clearPlexDiagnosticResult()
                    }) {
                        Text("Dismiss")
                    }
                }
            }
        )
    }
}

@Composable
fun BookloreConfigCard(
    isLoading: Boolean,
    onConnect: (String, String, String, String) -> Unit
) {
    var serverName by remember { mutableStateOf("Booklore") }
    var hostUrl by remember { mutableStateOf("http://10.70.14.2:6060") }
    var username by remember { mutableStateOf("ecollins") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, contentDescription = "Booklore", tint = AccentIndigo)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Booklore Server", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("E-Books & Comics", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = serverName,
                onValueChange = { serverName = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = hostUrl,
                onValueChange = { hostUrl = it },
                label = { Text("Server URL (http://ip:port)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrect = false
                ),
                trailingIcon = {
                    val image = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(image, "Toggle password visibility")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onConnect(serverName.trim(), hostUrl.trim(), username.trim(), password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && hostUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Connecting...", fontSize = 14.sp)
                } else {
                    Icon(Icons.Default.Link, contentDescription = "Connect", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Connect to Booklore", fontSize = 14.sp)
                }
            }
            }
            }
        }
    }
}

@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    val updates = remember {
        listOf(
            UpdateNotice(
                version = "4.8.0",
                date = "August 2026",
                highlights = listOf(
                    "Granular Page-by-Page E-Reader Navigation: Transformed the e-reader experience so every swipe or tap turns one individual page rather than advancing entire chapters. Chapters seamlessly transition only when the reader reaches the final page of a chapter.",
                    "Automatic Reading Progress Preservation: The e-reader automatically records and saves the exact chapter and page upon every page turn, screen navigation, or app exit, instantly restoring your position when reopened.",
                    "Universal Media Progress in JSON Backup: All reading progress (E-Books and Comics) and playback progress (Audiobooks and Music) are persisted directly into the portable homecast_backup.json offline file alongside server configurations and settings.",
                    "Cross-Session Comic Page Memory: Comic and Manga reader now saves and restores the exact page index across app launches."
                )
            ),
            UpdateNotice(
                version = "4.7.0",
                date = "August 2026",
                highlights = listOf(
                    "Dedicated Server Connections Submenu: Organized all personal media servers (Audiobookshelf, Plex, Booklore, Komga, Kavita, Jellyfin) into a streamlined sub-screen with live diagnostics and quick connection setup.",
                    "Public Domain Sources & AI Verification: Added a dedicated sub-menu to change, add, or customize public domain media repositories. Enter any website or catalog URL, and HomeCast's Gemini AI will inspect the endpoint, fix broken links/protocols, detect supported media types, and present a confirmation card before saving.",
                    "Local Storage Folders Submenu: Easily select or input device folders for Audiobooks, E-Books/Comics, and Music. Files automatically import and appear in the Personal Library tab for each media type.",
                    "AI Media Cover Art & Biography Enrichment: Gemini AI scans local media files to correct messy file tags, locate high-resolution cover art, and retrieve authentic literary/musical biographies."
                )
            ),
            UpdateNotice(
                version = "4.6.1",
                date = "August 2026",
                highlights = listOf(
                    "Authentic Comic & Manga Page Engine: Fully replaced AI summaries in the comic reader with direct image streaming from Komga, Kavita, Archive.org, and CBZ/ZIP archives. Comics now load their actual original graphical pages.",
                    "Direct E-Book Text Stream Reader: E-books now stream genuine unabridged text chapters from Booklore, Project Gutenberg, and EPUB files without AI summary fallbacks.",
                    "Dark Mode Typography Polish: Upgraded all media titles, author names, and descriptions to ensure crisp contrast across both dark and light modes.",
                    "Audiobooks Switcher UI Alignment: Replicated the polished segmented switcher buttons from Books and Music onto the Audiobooks screen.",
                    "Clean Bookshelf Header: Simplified the e-book header to 'Bookshelf' for a clean, distraction-free reading experience."
                )
            ),
            UpdateNotice(
                version = "4.6.0",
                date = "August 2026",
                highlights = listOf(
                    "Unified TabRow Switcher UI/UX: Replicated the sleek personal library & public domain switcher button tab bar from Books and Music across the Audiobooks screen for seamless visual and behavioral consistency.",
                    "Relabeled Bookshelf Header: Renamed 'Glass Bookshelf' to 'Bookshelf' with crisp display typography and enhanced contrast.",
                    "Resilient Cover Art Engine (MediaCoverArt): Replaced raw image loaders across all shelves with high-contrast, gold-embossed fallback covers. Book covers now load reliably and never display empty or broken boxes.",
                    "Massive Public Domain Catalog Expansion: Preloaded hundreds of iconic public domain books, audiobooks, and music masterworks across Sci-Fi, Cyberpunk, Fantasy, Philosophy, Mystery, and Classics.",
                    "Instant Zero-Delay Fallbacks for Details & Creators: LocalMediaMetadataProvider guarantees instant rendering of media biographies, creator profiles, ratings, and recommendations even when offline or before AI responses arrive.",
                    "Comprehensive Dark Mode Contrast Fixes: Fixed all media titles, album names, and author links across all screens to render in high-contrast white and vibrant accents."
                )
            ),
            UpdateNotice(
                version = "4.5.3",
                date = "August 2026",
                highlights = listOf(
                    "AI Personal Library Categorization: Rewrote synchronization pipelines to automatically run client-side author and genre sanitizers before insertion into the local database. This guarantees elegant, folder-free categories (e.g. cleaning folder directories and raw formats into English classics) even if the server is offline or the Gemini API is unavailable.",
                    "Interactive Media Details Screen: Tapping any audiobook or ebook from any shelf now opens a fully-fledged, contextual details pane featuring the media synopsis/biography, author description, publisher info, rating, and dynamic horizontal carousels.",
                    "Personal & Public Domain Cross-Recommendations: The new details screen dynamically computes other matching books or tracks on your personal servers, alongside classic recommended matches from public domain archives, creating a unified browsing experience.",
                    "Resolved Ebook Loading & Playback: Corrected the media resolution triggers inside the main e-reader for local server e-books, ensuring personal Booklore files load instantly and smoothly."
                )
            ),
            UpdateNotice(
                version = "4.5.2",
                date = "August 2026",
                highlights = listOf(
                    "Signature Bypass Settings Migration (Dual-Path): Solved the Android package installation/signature collision issue completely! If you are migrating from a previous GitHub version or are forced to do a clean uninstall/reinstall due to conflicting debug keystores, HomeCast now supports an absolute, signature-free, storage-independent backup mechanism.",
                    "Offline Storage Access Framework (SAF) Export/Import: Users can now click 'Export Backup' inside the settings menu to save their encrypted server configs, passwords, and playback state into a portable .json backup file anywhere (local downloads, Google Drive, SD card). Selecting 'Import Backup' restores everything instantly in 1-click.",
                    "Automatic Auto-Backup Detection: On any modification, connection profiles are auto-saved to public Downloads (/sdcard/Download/homecast_backup.json). On fresh reinstalls, if the database is unconfigured, Settings presents a prominent 1-click prompt to auto-restore all connections immediately."
                )
            ),
            UpdateNotice(
                version = "4.5.1",
                date = "August 2026",
                highlights = listOf(
                    "Automatic Settings Preservation & Cloud Backup: Explicitly configured native Android Auto Backup and modern Cloud Data Extraction rules (backup_rules.xml and data_extraction_rules.xml). This guarantees that all configuration files and Room SQLite database assets are preserved during updates, reinstalls, or device-to-device transfers.",
                    "Fragile Data Retention Support: Fully integrated android:hasFragileUserData=\"true\", ensuring that if a user manually uninstalls the app on modern Android versions, they are offered an OS-level checkbox to seamlessly preserve their settings, configurations, and reading history for subsequent reinstalls."
                )
            ),
            UpdateNotice(
                version = "4.5.0",
                date = "August 2026",
                highlights = listOf(
                    "Unified Mini-Player and Custom Stop Controls: Added a direct 'Stop' button to the sliding player controls. This halts ExoPlayer playback, collapses the player screen, and dismisses the mini-player completely. The mini-player now incorporates a sleek, non-interactive visual seek bar overlay that utilizes theme-matching gradients to reflect real-time playback progress.",
                    "Dynamic Startup Navigation & Tab Presets: On launch, HomeCast loads the exact media section (Library, Music, or Ebooks) and initializes the view's data source filter according to the media's origin.",
                    "Premium Adaptive Icon Compatibility: Re-architected launcher icon vector drawables to move the multi-stop gradient (Cyan to Magenta) into a full-bleed background layer.",
                    "AI-Powered Personal Server Categorization: Expanded the Gemini-backed automated dynamic categorization and curation engines to process personal server files, organizing them into gorgeous dynamic shelves alongside public domain media.",
                    "Keyboard Password Input Auto-Spacing Fix: Integrated dedicated KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrect = false) to prevent mobile keyboards (like Gboard) from inserting automatic spaces when typing special characters.",
                    "Enhanced E-Book Loading Resilience: Resolved text parsing and file loading crashes across Project Gutenberg collections, ensuring stable page rendering.",
                    "Seamless Update Integration: Incremented build configuration to versionCode 37 and versionName \"4.5.0\" to eliminate installer conflicts."
                )
            ),
            UpdateNotice(
                version = "4.3.2",
                date = "July 2026",
                highlights = listOf(
                    "Resolved Audiobook Duration Display: Solved the pervasive 1-hour default duration display bug on public domain audiobook cards. Fallbacks are now set to 0L and background worker is throttled.",
                    "Fixed Password Input Auto-Spacing: Added dedicated KeyboardOptions to Server Settings fields to disable auto-spacing entirely.",
                    "Embedded Keystore Restoration: Configured dynamically restored debug.keystore to guarantee identical signing certificates across all build environments."
                )
            ),
            UpdateNotice(
                version = "4.3.0",
                date = "July 2026",
                highlights = listOf(
                    "Clickable Creator Bio Detail Navigation: Transition the user directly to the Google Gemini-powered Creator Detail screen on clicking author names.",
                    "Dynamic Public Domain Library Matching: Cross-reference personal files with public domain records for matching shelves in bios.",
                    "On-Demand Full Music Album Resolution: Fetch and load entire tracklists from Archive.org files API.",
                    "Reactive Background Audiobook Durations: Background worker queries Archive.org files and updates duration badges in real-time."
                )
            )
        )
    }

    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NewReleases,
                    contentDescription = null,
                    tint = AccentIndigo,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("What's New in HomeCast", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Explore recent updates and features added to your app by FourgeAI LABS.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                updates.forEachIndexed { index, update ->
                    val isExpanded = expandedIndex == index
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else SurfaceGlass
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedIndex = if (isExpanded) null else index
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Version ${update.version}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isExpanded) AccentIndigo else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        update.date,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = if (isExpanded) AccentIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    update.highlights.forEach { highlight ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text("• ", fontWeight = FontWeight.Bold, color = AccentIndigo)
                                            Text(
                                                text = highlight,
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
            ) {
                Text("Awesome")
            }
        }
    )
}

data class UpdateNotice(
    val version: String,
    val date: String,
    val highlights: List<String>
)
