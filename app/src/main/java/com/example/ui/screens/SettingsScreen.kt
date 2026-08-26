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
    var showAiNoticeDialog by remember { mutableStateOf(false) }

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
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = AccentIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Disclaimer: A Plex Pass subscription is required for remote access outside your home network for Plex media services.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showAiNoticeDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Notice", tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Capabilities & Features Notice", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
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

    if (showAiNoticeDialog) {
        AiFeaturesNoticeDialog(onDismiss = { showAiNoticeDialog = false })
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

    var plexLoginUsername by remember { mutableStateOf("") }
    var plexLoginPassword by remember { mutableStateOf("") }
    var plexAuthMode by remember { mutableStateOf("web") } // "web" or "direct"

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

            // Primary Cloud Account Sign-In Card
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Plex Account Login",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = AccentIndigo
                            )
                        }
                    }

                    // Mode Toggle (Browser 1-Tap vs Direct User/Pass)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = plexAuthMode == "web",
                            onClick = { plexAuthMode = "web" },
                            label = { Text("🌐 1-Tap Browser Auth", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentIndigo.copy(alpha = 0.25f),
                                selectedLabelColor = AccentIndigo
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = plexAuthMode == "direct",
                            onClick = { plexAuthMode = "direct" },
                            label = { Text("🔑 Username & Password", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentTeal.copy(alpha = 0.25f),
                                selectedLabelColor = AccentTeal
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (plexAuthMode == "web") {
                        Text(
                            "Sign in once via your browser. HomeCast auto-discovers your owned Plex Media Server and syncs your music and video libraries directly.",
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
                                Text("Sign In with Plex Web", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        Text(
                            "Enter your Plex account credentials directly to sign in and auto-connect your servers.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = plexLoginUsername,
                            onValueChange = { plexLoginUsername = it },
                            label = { Text("Plex Username or Email", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = plexLoginPassword,
                            onValueChange = { plexLoginPassword = it },
                            label = { Text("Plex Password", fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (plexLoginUsername.isNotBlank() && plexLoginPassword.isNotBlank()) {
                                    viewModel.loginWithPlexCredentials(plexLoginUsername, plexLoginPassword)
                                } else {
                                    Toast.makeText(context, "Please enter both username/email and password.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isRequestingPin && !isDiscoveringPlexServers && !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isRequestingPin || isDiscoveringPlexServers) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Signing in...", fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign In Directly", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
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
                        "We found ${discoveredPlexServers.size} servers linked to your Plex account. Choose which one to connect:",
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

                    // 1-Tap Browser Auth Button
                    Button(
                        onClick = {
                            val authUrl = "https://app.plex.tv/auth#?clientID=${com.example.data.network.PlexClient.CLIENT_ID}&code=$code&context%5Bdevice%5D%5Bproduct%5D=HomeCast&context%5Bdevice%5D%5Bplatform%5D=Android"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("1-Tap Open Plex Web Sign-In", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Secondary Copy Code & Manual Link Options
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
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Code", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val linkUrl = "https://plex.tv/link?code=$code"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1.2f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("plex.tv/link", fontSize = 11.sp)
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
                version = "6.03.02",
                date = "August 2026",
                highlights = listOf(
                    "High-Speed Parallel Plex Data Loading: Optimized Plex server probing and concurrent section queries to load entire movie and TV libraries in seconds, matching native Plex speeds.",
                    "Complete Cast & Crew Biography Mapping: Seamlessly maps actors, directors, writers, producers, and cinematographers with full role descriptions and authentic portrait images sourced directly from Plex metadata or IMDb suggestions.",
                    "Zero-Lag Avatar Caching & Pre-Fetching: Integrated instant local cache resolution and background bulk pre-fetching for cast and crew portraits, eliminating UI stutter and loading delays.",
                    "Full Creative Team Support in Creator Profiles: Tapping any director, writer, producer, or cinematographer opens their rich biography with high-resolution portraits, curated filmographies, and matched media."
                )
            ),
            UpdateNotice(
                version = "6.03.01",
                date = "August 2026",
                highlights = listOf(
                    "Crash Prevention & Resilient Video Loading: Hardened Plex movie and show parsing with bulletproof error boundaries, safe nullable duration handling, and complete collection deduplication to ensure zero crashes on media loading.",
                    "Optimized Parallel Queries: Enhanced multi-candidate server queries with isolated async error handling preventing network dropouts from interrupting library display.",
                    "Enhanced 20-Item Sub-Screen Stability: Verified seamless rendering of 'Recently Added' and 'Recent Releases' 3-column poster grid sub-screens capped at 20 items without layout conflicts."
                )
            ),
            UpdateNotice(
                version = "6.03.00",
                date = "August 2026",
                highlights = listOf(
                    "High-Speed Bulk Fetching & Concurrent Loading: Overhauled Plex movie and TV show fetching to fetch all library items and episodes in high-speed parallel bulk queries, drastically reducing loading times.",
                    "Dedicated Interactive 'Recently Added' & 'Recent Releases' Sub-Screens: Added full-screen dedicated sub-views capped at the top 20 most recent uploads and releases for both Movies and TV Shows with responsive 3-column poster grids.",
                    "Authentic 4-Tab Plex Media Hub: Built sleek multi-tab browsing (Recommended, Browse, Playlists, Categories) with 3-column poster grids, search filter bar, dynamic sorting (Title, Year, Rating, Date Added), and colorful 2-column genre tiles matching reference design.",
                    "Seamless Media Navigation: Interactive clickables on every poster, banner, category card, and action button leading directly to media detail views with instant HD playback."
                )
            ),
            UpdateNotice(
                version = "6.02.00",
                date = "August 2026",
                highlights = listOf(
                    "Plex Video Library Fetch & Pagination Resolution: Re-architected Plex movie and TV show queries to include X-Plex-Container-Size headers (10,000 items) and exhaustive section scans to load thousands of movies and multi-season TV shows without truncation.",
                    "Accurate Real-Time Library Item Counts: Browse Libraries cards dynamically report exact live counts for all TV shows and movies directly from your connected Plex media server.",
                    "Comprehensive Cast & Creative Team Suite: Shows, movies, and episodes display full cast and crew lists (Actors, Directors, Writers, Producers, Cinematographers) matching native Plex client depth.",
                    "Universal Cast & Crew Portrait Art: Enhanced portrait loader resolves high-resolution photos for all actors and creative crew members directly from Plex metadata and authentic internet archives.",
                    "Theme-Consistent Sleek Progress Bars: Polished video resume and playback progress indicators with modern cyan/indigo gradient styling matching the app's established theme."
                )
            ),
            UpdateNotice(
                version = "6.01.00",
                date = "August 2026",
                highlights = listOf(
                    "Gemini AI Personalized Recommendations: New dedicated discovery section powered by Gemini AI that analyzes your recently played tracks, audiobooks, books, and movies to generate personalized cross-media recommendations.",
                    "Interactive AI Rationale & Vibe Chips: Tap the info icon on any recommendation to explore why Gemini recommended it, the inspired media item, and the aesthetic vibe with 1-tap playback/reading.",
                    "Concurrently Probed Multi-Candidate Plex Connection: Resolved Plex server connection and login issues by testing all local and remote candidate endpoints in parallel to lock onto the fastest responsive host with token persistence.",
                    "Full Multi-Candidate Plex Video & Music Sync: Concurrently fetches rich movie catalogs, multi-season TV shows, and music libraries across all accessible server endpoints upon connecting."
                )
            ),
            UpdateNotice(
                version = "6.00.00",
                date = "August 2026",
                highlights = listOf(
                    "Major Milestone Release (v6.00.00): Full multimedia hub transformation with complete Movies, TV Shows, Seasons, and Episodes streaming and metadata suite.",
                    "Rich Program, Season & Episode Biography Hierarchy: Movies and TV Shows now open directly to high-definition overview screens with synopsis, rating badges, genre tags, and cast & crew credits instead of auto-playing.",
                    "Granular Multi-Level Cast & Crew: Cast and crew members are granularly partitioned at every hierarchy level (Show, Season, and Episode). Explore actors, directors, producers, and writers specific to each episode and season.",
                    "Interactive Plex & IMDb Biography Integration: Tapping on any actor, director, or creator opens their full profile featuring authentic biographies (sourcing Plex first, with fallback to Wikipedia/IMDb), portrait photos, and filmography shelves.",
                    "Categorized Media Shelves & Recently Played Shelf: Media landing screen organizes content into dedicated 'Most Recently Played' resume cards with live progress bars, category filter pills (All, Movies, TV Shows), and separate Movie and Show horizontal shelves.",
                    "Instant In-Screen Video Player Dialog: Playback is triggered on-demand via the 'Play Movie' or episode play buttons without interrupting navigation history."
                )
            ),
            UpdateNotice(
                version = "5.16.00",
                date = "August 2026",
                highlights = listOf(
                    "Universal Plex XML & JSON Multi-Format Parser: Fully integrated native XML parser and JSON parser for Plex endpoints, supporting XML and JSON MediaContainer, Directory, Metadata, Track, and Device nodes.",
                    "Deep Nested Track & Album Traversal: Automatically extracts leaf tracks from artist and album nodes via allLeaves and child queries, ensuring complete library population regardless of default section view.",
                    "Safe Archive.org JSON Validation: Added pre-parsing JSON validation in ArchiveOrgClient, preventing JSONExceptions on non-JSON metadata responses.",
                    "Resilient Auto-Connect & Library Sync: Server configurations and sync pipelines now gracefully save and probe all candidate URLs in parallel with improved timeouts."
                )
            ),
            UpdateNotice(
                version = "5.15.00",
                date = "August 2026",
                highlights = listOf(
                    "Universal Single & Multi-Object Plex JSON Parser: Dual-mode JSON object and array parsing for fetchAccountServers, fetchMusicTracks, and fetchVideoItems in PlexClient.",
                    "Instant 1-Tap Auth & Direct Credentials: Sign into Plex via browser link or direct username/password credentials with automatic server discovery."
                )
            ),
            UpdateNotice(
                version = "5.14.00",
                date = "August 2026",
                highlights = listOf(
                    "1-Tap Browser Auth & Direct Link: Integrated official 1-tap browser auth link (app.plex.tv/auth) allowing instant authorization via Google, Apple, or Plex web login with auto-claimed PIN codes.",
                    "Direct Plex Credentials Sign-In: Added dedicated tab for direct Plex Username/Email & Password sign-in, obtaining authToken directly without leaving the app."
                )
            ),
            UpdateNotice(
                version = "5.13.00",
                date = "August 2026",
                highlights = listOf(
                    "Full Plex Server Video Library Support: Added full support for personal Plex Movies and TV Show episodes alongside music tracks.",
                    "Dedicated Movies & Shows Media Tab: Featuring poster artwork, year/season metadata, and 1-tap playback in embedded HD VideoPlayerDialog.",
                    "Remote Access Plex Pass Disclaimer: Added explicit notice in the About section of Settings stating that a Plex Pass subscription is required for remote access outside your home network."
                )
            ),
            UpdateNotice(
                version = "5.12.00",
                date = "August 2026",
                highlights = listOf(
                    "Resolved Audio & Video Media Playback Stream Engine: Fixed ExoPlayer network request headers for signed URLs (Google Cloud Storage, Archive.org, LibriVox, RSS feeds, and CDN endpoints).",
                    "Strict Single-Server Labeling: Dynamically evaluates connected servers and strictly displays EITHER 'Plex Library' or 'Jellyfin Library'."
                )
            ),
            UpdateNotice(
                version = "5.11.00",
                date = "August 2026",
                highlights = listOf(
                    "High-Performance Embedded Video Player for Podcasts & Video Media: Integrated Media3 PlayerView and ExoPlayer for high-throughput video streaming.",
                    "Dynamic Plex & Jellyfin Server Tab Labeling: Automatically detects connected personal servers and updates section labels.",
                    "AI Capabilities & Notice Accordion Menu in About: Full-featured dropdown menu inside About detailing all 9 intelligent AI features built by FourgeAI LABS."
                )
            ),
            UpdateNotice(
                version = "5.10.00",
                date = "August 2026",
                highlights = listOf(
                    "Expanded Public Domain Podcast Catalog: Added a comprehensive curated catalog with 30+ top public domain, open-access, and public broadcasting audio series.",
                    "Live iTunes Podcast Search API: Real-time discovery across thousands of global audio feeds, independent creator broadcasts, and top podcasts.",
                    "Live RSS & iTunes Episode Extractor: Automatically parses XML feeds and iTunes episode metadata to extract live audio enclosures, titles, and release dates.",
                    "Interactive Category Filtering: Browse feeds by Old Time Radio, Science & Tech, History & Culture, Philosophy & Books, News & Ideas, Audio Serials, and Indie & Community.",
                    "Personal Podcast Subscriptions: Bookmark and save public feeds directly into your Personal Podcasts collection."
                )
            ),
            UpdateNotice(
                version = "5.09.00",
                date = "August 2026",
                highlights = listOf(
                    "AI Capabilities & Features Dropdown Notice: Added an interactive accordion dialog in the About section detailing all 9 AI capabilities built into HomeCast.",
                    "Restored Comic Archive & Page Streaming Engine: Upgraded comic identifier parsing, multi-tier archive file extraction, direct Archive.org page image streams, and local CBZ/ZIP directory support.",
                    "Coil User-Agent Header Injection: Ensured high-resolution remote comic pages and Archive.org images load seamlessly without server blockages.",
                    "Enhanced Comic Reader Responsiveness: Seamless Western LTR, Manga RTL, and Webtoon vertical reading modes with multi-touch zoom and guided panel transitions."
                )
            ),
            UpdateNotice(
                version = "5.08.00",
                date = "August 2026",
                highlights = listOf(
                    "Story So Far AI Summarizer: Generates instant context summaries for long audiobooks, e-books, and podcasts without spoilers.",
                    "24/7 AI Companion Assistant: In-context conversational helper on player and e-reader screens that answers questions tailored strictly to your current position.",
                    "AI Media Concierge: Curates custom media blends and delivers narrative recommendations in the Discovery feed.",
                    "Smart Sleep Timer & Sleep Assistant: Intelligent sleep timer with custom audio fade-out and AI-generated bedtime prompts.",
                    "Dynamic Ambient Soundscape Synthesizer: Generates real-time ambient background audio (Rainfall, Fireplace, Ocean Waves, Cafe Ambient, Forest Birds, Cosmic Drone) synthesized directly on-device using PCM AudioTracks for immersive reading or listening.",
                    "Stylized Quote Card Generator: Transforms book excerpts and bookmarks into beautifully designed quote cards with customizable color palettes, typography, and background patterns."
                )
            ),
            UpdateNotice(
                version = "5.07.00",
                date = "August 2026",
                highlights = listOf(
                    "High-Performance App Responsiveness: Resolved background UI thread locks and startup AI cleanup loops, eliminating app sluggishness.",
                    "Restored Music & Podcast Playback: Fixed audio resolution logic and ExoPlayer initialization queues, ensuring podcasts and music play instantly.",
                    "Seamless Backup Server Reconnection: Restoring settings or silent backups now automatically reloads server configurations and immediately triggers server reconnects."
                )
            ),
            UpdateNotice(
                version = "5.06.00",
                date = "August 2026",
                highlights = listOf(
                    "Mini-Player Forehead Space Optimization: Shrunk top padding and outer margins on the mini-player card across all screens to eliminate gaps above the playback bar.",
                    "Seamless Content-to-Player Alignment: Removed excessive bottom scroll padding across Audiobooks, Music, Bookshelf, Podcasts, and Discover screens for flush alignment directly against the mini-player."
                )
            ),
            UpdateNotice(
                version = "5.05.00",
                date = "August 2026",
                highlights = listOf(
                    "Unified Glassmorphic UI & Layout System: Applied the sleek visual style of the Podcasts screen across Audiobooks, Music, Bookshelf, and Discover screens.",
                    "Sleek Pill Segmented Switchers: Modernized all personal/public tab switchers with pill-shaped segmented controls and responsive color highlights.",
                    "Consistent Dark Glass Canvas: Polished all top app bars, search inputs, and filter chips for a consistent dark glass design."
                )
            ),
            UpdateNotice(
                version = "5.04.00",
                date = "August 2026",
                highlights = listOf(
                    "New Podcasts Section: Added a primary Podcasts tab to bottom navigation featuring personal server feeds and curated public directories (PlayPodcast.net, RSS.com, GetPodcast).",
                    "Smart System Back Navigation: Resolved back button behavior so pressing back smoothly pops navigation history rather than resetting to personal audiobooks.",
                    "Artist Bio Overhaul: Added persistent popular tracks dropdown with right-aligned play buttons and chronological album display (newest to oldest left to right).",
                    "AI-Guided Cinematic Comic Zoom: Panel-by-panel guided zoom with energetic spring transitions and smart comic archive detection.",
                    "Solid Player Canvas: Opaque dark background canvas prevents lower screens from bleeding through full-screen player transitions."
                )
            ),
            UpdateNotice(
                version = "5.03.00",
                date = "August 2026",
                highlights = listOf(
                    "Authentic Internet Creator Biographies: Replaced generic templated AI text with verified, multi-paragraph biographies sourced live from Wikipedia's official REST API, Wikidata, and comprehensive historical archives.",
                    "Genuine Internet Portrait Photos: Creator profiles now display authentic high-resolution portraits fetched directly from Wikimedia Commons and official archives.",
                    "Direct IMDb & Wikipedia Integration: Each creator profile features one-tap action buttons linking directly to their official Wikipedia articles, IMDb filmography/credits, and Internet Archive catalogs.",
                    "Curated Historical Mastermind Encyclopedia: Integrated zero-latency offline biographies and verified portraits for classic novelists, spoken-word authors, philosophers, and classical composers (H.G. Wells, Mary Shelley, Jane Austen, Arthur Conan Doyle, Edgar Allan Poe, Beethoven, Mozart, Bach, Chopin, and more).",
                    "Creator Spotlight & Media Navigation: Added creator jump-cards from media detail views, letting you explore all related e-books, audiobooks, and music tracks by the author."
                )
            ),
            UpdateNotice(
                version = "5.02.00",
                date = "August 2026",
                highlights = listOf(
                    "Personal Server E-Book Loading Resolution: Fixed e-book loading from personal servers (Booklore, Komga, Audiobookshelf) with an enhanced multi-endpoint fallback engine, token extraction, Bearer authentication headers, and permissive SSL handling for self-hosted certificates.",
                    "Robust E-Reader Text & EPUB Parsing: Upgraded the EPUB and text stream parser to comprehensively unpack multi-directory EPUB archives, clean HTML/XML entities, and naturally order chapters for smooth reading.",
                    "Zero-Crash Audiobook Playback Transition: Resolved playback crashes when switching between audiobooks on personal servers. ExoPlayer operations and progress updates are now strictly thread-isolated on the main loop with resilient error recovery and OkHttp data streaming.",
                    "Optimized Media Progress Synchronization: Eliminated background race conditions and UI thread contention during audio playback and track switching."
                )
            ),
            UpdateNotice(
                version = "5.01.00",
                date = "August 2026",
                highlights = listOf(
                    "Fully Populated Dual-Side Discovery: Completely resolved the empty Discover tab on both Private Library and Public Domain sides with rich, dynamic feeds and interactive carousels.",
                    "Public Domain Masterpiece Showcase: Discover timeless classic literature (Frankenstein, Great Gatsby, Dracula, Dorian Gray, Art of War), full LibriVox dramatic audiobooks (Sherlock Holmes, Dracula, Alice in Wonderland), Golden Age vintage comics (Little Nemo, Planet Comics, Krazy Kat, Shazam), and masterwork classical/archive recordings (Beethoven, Debussy, Vivaldi, Chopin, Joplin, Mozart, Bach) with 1-tap reading and playback.",
                    "Curated Historical Theme Clusters: Dive into thematic eras including Sci-Fi Pioneers, Victorian Mystery, Ancient Philosophy, Roaring 20s Jazz, and High Seas Adventures.",
                    "Private Library Multi-Media Showcase: Experience the AI 'For You' Daily Blend, 'Continue Your Journey' resume shelf, dynamic time-of-day mixes, top digital e-books, featured audiobooks, and the interactive 100+ moods explorer carousel.",
                    "Quick Media Type Filter Pills: Easily toggle between All Media, Audiobooks, E-Books & Comics, and Music & Mixes on demand.",
                    "Direct Action Discovery Cards: AI recommendations and search results now feature immediate 1-tap 'Read Now', 'Comic', 'Listen', and 'Play' buttons with instant playback and reader transitions."
                )
            ),
            UpdateNotice(
                version = "5.0.0",
                date = "August 2026",
                highlights = listOf(
                    "Clickable Music Category Navigation: Replaced single-row side-scrolling restrictions across all music shelves. Tapping category headers (Recent Grooves, New Releases, Featured Artists, AI Mixes, All Songs) now opens a dedicated full-screen view with full grid/list layout, Play All, and Shuffle controls.",
                    "100+ Distinct Stylized Moods: Added a new 'Moods' tab to the music hierarchy matching the visual design of the Genres screen. Explore over 100 moods organized into 10 curated vibe categories (Chillout, Lo-Fi, Deep Focus, Cyberpunk, 80s Synth, High Energy, Cinematic, etc.) with custom gradients, vector iconography, and track matching.",
                    "AI Listening History 'For You' Mix: The AI engine continuously analyzes your playback history and frequently played artists/genres to curate a bespoke 'For You' daily playlist.",
                    "Dynamic Time-of-Day & Style Mixes: Automatic dynamic mix generation that adapts multiple times a day (Morning Awakening, Midday Focus, Golden Hour, Midnight Low-End, Heavy Rotation, Deep Cuts, and Genre Fusion).",
                    "Dynamic AI Category Shuffling: Mix up and remix your category shelves on demand or throughout the day with the one-tap remix button.",
                    "Revamped Discovery & Home Page: Transformed the home explore experience with interactive prompt suggestion chips, live 'For You' music showcase cards, and instant mood exploration carousels."
                )
            ),
            UpdateNotice(
                version = "4.9.1",
                date = "August 2026",
                highlights = listOf(
                    "Resilient Personal Media Loading: Fixed library filtering so personal Audiobooks, E-Books, and Music always load reliably across connected servers, local storage folders, and starter collections without getting masked.",
                    "Automated Background Server Sync & Folder Scanning: HomeCast now automatically initiates background sync for all connected Audiobookshelf, Plex, and Booklore servers and scans local media folders upon app launch and state changes.",
                    "One-Tap Header Sync & Refresh: Added a dedicated Refresh/Sync button to Audiobooks, E-Books, and Music screen headers for immediate manual synchronization with animated feedback.",
                    "Full Backup Payload Preservation: Enhanced silent and exported JSON backup routines to safeguard local folder profiles, public domain sources, reading bookmarks, and granular playback positions."
                )
            ),
            UpdateNotice(
                version = "4.9.0",
                date = "August 2026",
                highlights = listOf(
                    "True Screen-Budget Dynamic Pagination: Completely fixed the e-reader pagination engine. Chapters are now split into true screen-fitting pages based on font size and line spacing, allowing smooth page-by-page reading instead of single-page chapters.",
                    "Universal Bookmarks for E-Books & Audiobooks: Added a complete bookmarking system. In the E-Reader and Audio/Music Player, tap the bookmark icon to save your current page or timestamp with preview excerpts and notes.",
                    "Interactive Bookmarks & Last Spot Drawer: Browse all saved bookmarks and instantly resume reading or listening from your last spot with a single tap.",
                    "Instant JSON & Room Progress Synchronization: Every page turn, scrub, and bookmark is immediately written to local Room storage and exported to the portable JSON backup."
                )
            ),
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

data class AiFeatureItem(
    val title: String,
    val category: String,
    val description: String,
    val highlights: List<String>
)

@Composable
fun AiFeaturesNoticeDialog(onDismiss: () -> Unit) {
    val aiFeatures = remember {
        listOf(
            AiFeatureItem(
                title = "Story So Far AI Summarizer",
                category = "Reading & Listening Progress AI",
                description = "Provides instant chapter-level and context-aware story recaps for long audiobooks, e-books, and podcast episodes without spoiling upcoming events.",
                highlights = listOf(
                    "Generates spoiler-free recaps matching your exact chapter or playback timestamp.",
                    "Available on both the full-screen player and e-reader HUD menus.",
                    "Keeps track of complex character arcs, plot twists, and key lore."
                )
            ),
            AiFeatureItem(
                title = "24/7 In-Context AI Companion Assistant",
                category = "Interactive Media Assistant",
                description = "Conversational assistant integrated directly into player and e-reader screens that answers questions tailored strictly to what you are currently reading or listening to.",
                highlights = listOf(
                    "Clarifies difficult vocabulary, historical setting details, or character relationships.",
                    "Provides instant answers without leaving your current book or track.",
                    "Respects privacy and operates with context isolation."
                )
            ),
            AiFeatureItem(
                title = "AI Media Concierge & Bespoke Blends",
                category = "Discovery & Personalization",
                description = "Curates tailored media recommendation blends and delivers narrative commentary in the Discovery feed based on your taste and listening history.",
                highlights = listOf(
                    "Generates custom thematic mixes and media pairings.",
                    "Explains why each recommended book, comic, or album matches your vibe.",
                    "Supports interactive prompt chips for custom genre exploration."
                )
            ),
            AiFeatureItem(
                title = "Public Domain Endpoint Verifier & Link Repair",
                category = "Catalog & Network Intelligence",
                description = "Automatically inspects, verifies, and repairs open-access catalog feeds and archive endpoints before adding them to your home library.",
                highlights = listOf(
                    "Performs live URL health checks and automatic protocol repairs.",
                    "Detects supported media formats (CBZ, EPUB, MP3, FLAC) from raw feeds.",
                    "Guarantees broken or dead catalog links are fixed before saving."
                )
            ),
            AiFeatureItem(
                title = "Local Media Scanner & Bio Enricher",
                category = "Library Curation & Enrichment",
                description = "Scans imported device folders and connected servers to retrieve high-resolution cover art, clean messy folder tags, and fetch authentic biographies.",
                highlights = listOf(
                    "Sources verified author portraits and multi-paragraph biographies live from Wikipedia and historical archives.",
                    "Cleans ugly directory folder names into proper English titles and creator names.",
                    "Automatically matches local audiobooks and e-books with public domain catalogs."
                )
            ),
            AiFeatureItem(
                title = "Dynamic Midnight Bookshelf & Mood Rotator",
                category = "Automated Daily Curation",
                description = "Automatically reorganizes category bookshelves, featured masterworks, and 100+ vibe moods daily at midnight to keep your collection fresh.",
                highlights = listOf(
                    "Rotates featured authors and genre shelves every night.",
                    "Adapts 'For You' mixes based on time of day (Morning, Midday, Midnight).",
                    "Supports 1-tap manual category remixing in preferences."
                )
            ),
            AiFeatureItem(
                title = "Smart Sleep Assistant & Auto-Fade",
                category = "Playback & Audio Intelligence",
                description = "Intelligent sleep timer featuring smooth exponential volume fade-out curves and soothing AI-generated bedtime prompts for night listening.",
                highlights = listOf(
                    "Gradually decreases audio volume over 15, 30, 45, or 60 minutes.",
                    "Displays relaxing bedtime prompts tailored for night reading.",
                    "Automatically pauses ExoPlayer playback when timer expires."
                )
            ),
            AiFeatureItem(
                title = "Dynamic Ambient Soundscape Synthesizer",
                category = "Atmospheric Audio Engine",
                description = "Synthesizes real-time ambient background audio (Rainfall, Fireplace, Ocean Waves, Cafe Ambient, Forest Birds, Cosmic Drone) on-device using PCM AudioTracks.",
                highlights = listOf(
                    "Auto-detects story mood from current page text to select matching ambience.",
                    "Generates zero-latency procedural audio without streaming network data.",
                    "Independent background volume control overlaid with main player."
                )
            ),
            AiFeatureItem(
                title = "Stylized Quote Card Generator",
                category = "Social Sharing & Creative Tooling",
                description = "Transforms book excerpts and saved bookmarks into shareable quote cards with customizable color palettes, typography, and background patterns.",
                highlights = listOf(
                    "Converts highlights or book passages into visual cards.",
                    "Customizable card themes, font pairings, and canvas styles.",
                    "Includes author attribution and book title formatting."
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
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = AccentTeal,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("AI Capabilities & Features", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Explore the intelligent AI models and smart features built into HomeCast by FourgeAI LABS.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                aiFeatures.forEachIndexed { index, feature ->
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        feature.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isExpanded) AccentTeal else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        feature.category,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AccentIndigo
                                    )
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = if (isExpanded) AccentTeal else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = feature.description,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    feature.highlights.forEach { highlight ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text("✦ ", fontWeight = FontWeight.Bold, color = AccentTeal)
                                            Text(
                                                text = highlight,
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp,
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
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
            ) {
                Text("Got It", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}
