package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ServerConfig
import com.example.ui.MainViewModel
import com.example.ui.ServerOperationState
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.LocalThemeMode
import com.example.ui.theme.SurfaceGlass
import com.example.ui.theme.SurfaceGlassBorder
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onThemeToggle: (Boolean) -> Unit
) {
    val isDarkTheme = LocalThemeMode.current
    val servers by viewModel.servers.collectAsState()
    val serverOpState by viewModel.serverOpState.collectAsState()
    val context = LocalContext.current

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

        item {
            // Theme Setting
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceGlass
                ),
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

        // Active Connected Servers
        if (servers.isNotEmpty()) {
            item {
                Text("Connected Servers", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            items(servers) { server ->
                ServerItemCard(
                    server = server,
                    onSync = { viewModel.syncServer(server) },
                    onDelete = { viewModel.removeServer(server.id) },
                    isLoading = serverOpState is ServerOperationState.Loading
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Add New Server", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Audiobookshelf Config Card
        item {
            AudiobookshelfConfigCard(
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
                isLoading = serverOpState is ServerOperationState.Loading,
                onConnect = { name, url, token ->
                    viewModel.saveAndConnectPlexDirect(name, url, token)
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
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
                    Icon(Icons.Default.Book, contentDescription = "Audiobookshelf", tint = AccentTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Audiobookshelf Server", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Audiobooks, Podcasts & E-books", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

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
                            "Authentication & 401 Help:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = AccentTeal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "1. Check for typos or casing in your username and password.\n" +
                            "2. If your server uses SSO, OIDC, or strict API policies, check 'Use API Token'.\n" +
                            "3. To get an API Token: Open Audiobookshelf Web UI -> Settings -> Users -> Click your user or API Keys -> Copy Token/API Key.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                label = { Text("Server URL (e.g. http://10.70.14.2:13378)") },
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
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && hostUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connecting & Syncing...")
                } else {
                    Icon(Icons.Default.Link, contentDescription = "Connect")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save & Connect Audiobookshelf")
                }
            }
        }
    }
}

@Composable
fun PlexConfigCard(
    isLoading: Boolean,
    onConnect: (String, String, String) -> Unit
) {
    var serverName by remember { mutableStateOf("My Plex Server") }
    var hostUrl by remember { mutableStateOf("http://192.168.1.") }
    var plexToken by remember { mutableStateOf("") }
    var showTokenHelp by remember { mutableStateOf(false) }

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
                    Icon(Icons.Default.MusicNote, contentDescription = "Plex", tint = AccentIndigo)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Plex Server", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Music & Audio Streaming", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                IconButton(onClick = { showTokenHelp = !showTokenHelp }) {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Help", tint = MaterialTheme.colorScheme.primary)
                }
            }

            AnimatedVisibility(visible = showTokenHelp) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        "To find your Plex Token:\n1. Open Plex Web and inspect XML on any media item\n2. Or check your Plex account settings for X-Plex-Token\n3. Enter the token along with your Plex local/remote URL.",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onConnect(serverName, hostUrl, plexToken)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && hostUrl.isNotBlank() && plexToken.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connecting & Syncing...")
                } else {
                    Icon(Icons.Default.Link, contentDescription = "Connect")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save & Connect Plex")
                }
            }
        }
    }
}
