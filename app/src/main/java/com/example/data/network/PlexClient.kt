package com.example.data.network

import android.util.Log
import com.example.data.MusicTrack
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object PlexClient {
    private const val TAG = "PlexClient"
    const val CLIENT_ID = "HomeCast-Android-Client"
    private const val PRODUCT_NAME = "HomeCast"
    private const val VERSION = "1.8"

    private val client: OkHttpClient by lazy {
        OptimizedNetworkEngine.client
    }

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    /**
     * Sanitizes and normalizes any user-entered Plex server URL.
     * Auto-appends :32400 for local IP addresses if port is omitted, and strips
     * copied web client paths (/web, /web/index.html, etc.).
     */
    fun normalizeUrl(url: String): String {
        var clean = url.trim()
        if (clean.isBlank()) return ""

        // Strip query params or hash anchors
        if (clean.contains("?")) clean = clean.substringBefore("?")
        if (clean.contains("#")) clean = clean.substringBefore("#")

        // Auto-prepend scheme if missing
        if (!clean.startsWith("http://", ignoreCase = true) && !clean.startsWith("https://", ignoreCase = true)) {
            val isRemoteDomain = clean.contains(".") && !clean.matches(Regex("^[0-9.]+(?::[0-9]+)?$"))
            clean = if (isRemoteDomain) "https://$clean" else "http://$clean"
        }

        clean = clean.trimEnd('/')

        // Strip common web UI suffixes if pasted directly from browser address bar
        val suffixesToStrip = listOf(
            "/web/index.html",
            "/web",
            "/manage",
            "/library/sections",
            "/library",
            "/status"
        )
        for (suffix in suffixesToStrip) {
            if (clean.endsWith(suffix, ignoreCase = true)) {
                clean = clean.substring(0, clean.length - suffix.length).trimEnd('/')
            }
        }

        // If it's an IP address without a port, default to Plex port 32400
        val hostPart = clean.substringAfter("://")
        if (!hostPart.contains(":") && (hostPart.matches(Regex("^[0-9.]+$")) || hostPart.equals("localhost", ignoreCase = true))) {
            clean = "$clean:32400"
        }

        return clean
    }

    private fun buildStandardHeaders(builder: Request.Builder, token: String) {
        builder.addHeader("Accept", "application/json")
        builder.addHeader("X-Plex-Product", PRODUCT_NAME)
        builder.addHeader("X-Plex-Version", VERSION)
        builder.addHeader("X-Plex-Client-Identifier", CLIENT_ID)
        builder.addHeader("X-Plex-Platform", "Android")
        builder.addHeader("X-Plex-Device", "Android")
        builder.addHeader("X-Plex-Device-Name", "HomeCast Android")
        if (token.isNotBlank()) {
            builder.addHeader("X-Plex-Token", token.trim())
        }
    }

    /**
     * Creates a Plex PIN for web authorization (plex.tv/link).
     * Uses strong=false to generate a clean 4-character code for plex.tv/link.
     */
    suspend fun createPin(): Result<PlexPinResponse> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://plex.tv/api/v2/pins?strong=false")
            .post("".toRequestBody())
            .apply { buildStandardHeaders(this, "") }
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to create PIN: HTTP ${response.code}"))
            }

            // Dual parsing: JSONObject + Moshi
            var pinCode: String? = null
            var pinId: Long? = null
            try {
                val json = org.json.JSONObject(body)
                if (json.has("code") && !json.isNull("code")) pinCode = json.getString("code")
                if (json.has("id") && !json.isNull("id")) pinId = json.getLong("id")
            } catch (_: Exception) {}

            if (pinCode != null && pinId != null) {
                Result.success(PlexPinResponse(id = pinId, code = pinCode.uppercase()))
            } else {
                val adapter = moshi.adapter(PlexPinResponse::class.java)
                val pin = adapter.fromJson(body)
                if (pin?.code != null && pin.id != null) {
                    Result.success(pin.copy(code = pin.code.uppercase()))
                } else {
                    Result.failure(Exception("Invalid PIN response from Plex"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if the PIN has been claimed by the user on plex.tv/link.
     */
    suspend fun checkPin(pinId: Long): Result<String?> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://plex.tv/api/v2/pins/$pinId")
            .get()
            .apply { buildStandardHeaders(this, "") }
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}"))
            }

            var token: String? = null
            try {
                val json = org.json.JSONObject(body)
                if (json.has("authToken") && !json.isNull("authToken")) {
                    token = json.getString("authToken")
                } else if (json.has("auth_token") && !json.isNull("auth_token")) {
                    token = json.getString("auth_token")
                }
            } catch (_: Exception) {}

            if (token.isNullOrBlank()) {
                val adapter = moshi.adapter(PlexPinResponse::class.java)
                val pin = adapter.fromJson(body)
                token = pin?.resolvedAuthToken
            }

            Result.success(token?.takeIf { it.isNotBlank() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Direct sign-in using Plex username/email & password to retrieve authToken directly.
     */
    suspend fun loginWithCredentials(usernameOrEmail: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        if (usernameOrEmail.isBlank() || password.isBlank()) {
            return@withContext Result.failure(Exception("Username/email and password cannot be empty."))
        }

        val formBody = FormBody.Builder()
            .add("user[login]", usernameOrEmail.trim())
            .add("user[password]", password)
            .add("login", usernameOrEmail.trim())
            .add("password", password)
            .build()

        val request = Request.Builder()
            .url("https://plex.tv/users/sign_in.json")
            .post(formBody)
            .apply { buildStandardHeaders(this, "") }
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                if (response.code == 401) {
                    return@withContext Result.failure(Exception("Invalid Plex username or password."))
                }
                return@withContext Result.failure(Exception("Plex sign-in failed: HTTP ${response.code}"))
            }

            var token: String? = null
            try {
                val json = org.json.JSONObject(body)
                if (json.has("user")) {
                    val userObj = json.getJSONObject("user")
                    if (userObj.has("authToken") && !userObj.isNull("authToken")) {
                        token = userObj.getString("authToken")
                    } else if (userObj.has("authentication_token") && !userObj.isNull("authentication_token")) {
                        token = userObj.getString("authentication_token")
                    }
                }
                if (token.isNullOrBlank()) {
                    if (json.has("authToken") && !json.isNull("authToken")) {
                        token = json.getString("authToken")
                    } else if (json.has("auth_token") && !json.isNull("auth_token")) {
                        token = json.getString("auth_token")
                    }
                }
            } catch (_: Exception) {}

            if (!token.isNullOrBlank()) {
                Result.success(token)
            } else {
                Result.failure(Exception("Sign-in succeeded but no authentication token was returned by Plex."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches all Plex Media Servers linked to the user's Plex account via plex.tv/api/v2/resources.
     * Probes and resolves the best reachable connection (local LAN IP, secure plex.direct, or remote)
     * so the user NEVER has to manually enter an IP address or port!
     */
    suspend fun fetchAccountServers(authToken: String): Result<List<DiscoveredPlexServer>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://plex.tv/api/v2/resources?includeHttps=1&includeRelay=1")
            .get()
            .apply { buildStandardHeaders(this, authToken) }
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch Plex servers: HTTP ${response.code}"))
            }

            val listType = Types.newParameterizedType(List::class.java, PlexDevice::class.java)
            val adapter = moshi.adapter<List<PlexDevice>>(listType)
            val devices = adapter.fromJson(body) ?: emptyList()

            // Filter all media servers accessible to this account (owned or shared)
            val servers = devices.filter {
                it.provides?.contains("server", ignoreCase = true) == true
            }
            if (servers.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            val discoveredList = mutableListOf<DiscoveredPlexServer>()

            for (server in servers) {
                val serverName = server.name ?: "Plex Server"
                val serverToken = if (!server.accessToken.isNullOrBlank()) server.accessToken else authToken
                val connections = server.connections ?: emptyList()

                // Generate candidate URIs from connections
                val candidateUris = mutableListOf<String>()
                for (conn in connections) {
                    if (!conn.uri.isNullOrBlank()) {
                        candidateUris.add(conn.uri)
                    }
                    if (!conn.address.isNullOrBlank() && conn.port != null) {
                        val httpUri = "http://${conn.address}:${conn.port}"
                        val httpsUri = "https://${conn.address}:${conn.port}"
                        if (!candidateUris.contains(httpUri)) candidateUris.add(httpUri)
                        if (!candidateUris.contains(httpsUri)) candidateUris.add(httpsUri)
                    }
                }

                // Sort candidate URIs: local LAN first (e.g. 192.168.x.x:32400), then local HTTPS, then remote, then relay
                val sortedCandidates = candidateUris.distinct().sortedWith(
                    compareByDescending<String> { it.contains("192.168.") || it.contains("10.") || it.contains("172.") }
                        .thenByDescending { it.startsWith("http://") && !it.contains("relay.plex.services") }
                        .thenByDescending { it.startsWith("https://") && !it.contains("relay.plex.services") }
                        .thenBy { it.contains("relay.plex.services") }
                )

                // Concurrent parallel connection probing across candidates for instant response
                val activeUri = findFastestReachableUri(sortedCandidates, serverToken)
                val isLocal = activeUri.contains("192.168.") || activeUri.contains("10.") || activeUri.contains("172.") || activeUri.contains("localhost")

                if (activeUri.isNotBlank()) {
                    discoveredList.add(
                        DiscoveredPlexServer(
                            name = serverName,
                            clientIdentifier = server.clientIdentifier ?: serverName,
                            token = serverToken,
                            preferredUri = activeUri,
                            isLocal = isLocal,
                            allConnections = connections,
                            candidateUris = sortedCandidates,
                            owned = server.owned != false,
                            isReachable = true
                        )
                    )
                }
            }

            Result.success(discoveredList)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching account servers", e)
            Result.failure(e)
        }
    }

    /**
     * Concurrently probes candidate URIs in parallel with a tight timeout, returning as soon as a working address is found.
     */
    private suspend fun findFastestReachableUri(candidateUris: List<String>, token: String): String = coroutineScope {
        if (candidateUris.isEmpty()) return@coroutineScope ""
        if (candidateUris.size == 1) return@coroutineScope candidateUris.first()

        val jobs = candidateUris.map { uri ->
            async(Dispatchers.IO) {
                if (testConnectionQuick(uri, token)) uri else null
            }
        }

        val results = jobs.awaitAll()
        results.firstOrNull { !it.isNullOrBlank() } ?: candidateUris.first()
    }

    fun testConnectionQuick(serverUrl: String, token: String): Boolean {
        val root = normalizeUrl(serverUrl)
        if (root.isBlank()) return false
        val cleanToken = token.trim()
        val probeClient = client.newBuilder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()

        // 1. Try /library/sections with token
        if (cleanToken.isNotBlank()) {
            try {
                val req = Request.Builder()
                    .url("$root/library/sections?X-Plex-Token=$cleanToken")
                    .get()
                    .apply { buildStandardHeaders(this, cleanToken) }
                    .build()
                val res = probeClient.newCall(req).execute()
                val code = res.code
                res.close()
                if (code in 200..299) return true
            } catch (_: Exception) {}
        }

        // 2. Fallback to /identity probe
        return try {
            val req = Request.Builder()
                .url("$root/identity")
                .get()
                .apply { buildStandardHeaders(this, cleanToken) }
                .build()
            val res = probeClient.newCall(req).execute()
            val code = res.code
            res.close()
            code in 200..299 || code == 401
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Tests connection to Plex server across all candidate endpoints concurrently in parallel.
     */
    suspend fun testConnection(serverUrl: String, token: String, candidateUrls: List<String> = emptyList()): Result<Boolean> = coroutineScope {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            return@coroutineScope Result.failure(Exception("X-Plex-Token cannot be empty. Please sign in with your Plex account."))
        }

        val allCandidates = (listOf(serverUrl) + candidateUrls)
            .map { normalizeUrl(it) }
            .filter { it.isNotBlank() }
            .distinct()

        if (allCandidates.isEmpty()) {
            return@coroutineScope Result.failure(Exception("Server URL cannot be empty."))
        }

        val deferreds = allCandidates.map { root ->
            async(Dispatchers.IO) {
                testSingleCandidate(root, cleanToken)
            }
        }

        val results = deferreds.awaitAll()
        if (results.any { it }) {
            Result.success(true)
        } else {
            Result.failure(Exception("Unable to connect to Plex server at provided address(es)."))
        }
    }

    private fun testSingleCandidate(root: String, token: String): Boolean {
        val candidateEndpoints = listOf(
            "$root/library/sections",
            "$root/library/sections?X-Plex-Token=$token"
        )
        val fastClient = client.newBuilder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()

        for (url in candidateEndpoints) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .apply { buildStandardHeaders(this, token) }
                    .build()
                val response = fastClient.newCall(request).execute()
                val isSuccess = response.isSuccessful
                response.close()
                if (isSuccess) return true
            } catch (_: Exception) {}
        }
        return false
    }

    /**
     * Fetches all music tracks across music libraries in Plex.
     * Automatically attempts alternate candidate URLs if the primary URL is unreachable.
     */
    suspend fun fetchMusicTracks(
        serverUrl: String,
        token: String,
        serverId: String,
        candidateUrls: List<String> = emptyList()
    ): Result<List<MusicTrack>> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            return@withContext Result.failure(Exception("X-Plex-Token is missing. Please sign into your Plex account."))
        }

        val allCandidates = (listOf(serverUrl) + candidateUrls)
            .map { normalizeUrl(it) }
            .filter { it.isNotBlank() }
            .distinct()

        var workingRoot = ""
        var sections = emptyList<PlexDirectory>()
        var lastError: Exception? = null

        // Step 1: Find working candidate URL and fetch library sections
        for (candidate in allCandidates) {
            try {
                val secReq = Request.Builder()
                    .url("$candidate/library/sections?X-Plex-Token=$cleanToken")
                    .get()
                    .apply { buildStandardHeaders(this, cleanToken) }
                    .build()

                val secRes = client.newCall(secReq).execute()
                val secBody = secRes.body?.string() ?: ""

                if (secRes.isSuccessful && secBody.isNotBlank()) {
                    val secAdapter = moshi.adapter(PlexSectionsResponse::class.java)
                    val parsed = secAdapter.fromJson(secBody)
                    val dirs = parsed?.mediaContainer?.directory ?: emptyList()
                    if (dirs.isNotEmpty()) {
                        workingRoot = candidate
                        sections = dirs
                        break
                    } else {
                        workingRoot = candidate
                        sections = emptyList()
                        break
                    }
                } else {
                    lastError = Exception("Plex section fetch returned HTTP ${secRes.code}")
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        if (workingRoot.isBlank()) {
            return@withContext Result.failure(lastError ?: Exception("Could not connect to Plex server library."))
        }

        // Filter for music/audio sections or fallback to all sections
        val musicSections = sections.filter { dir ->
            val type = dir.type?.lowercase() ?: ""
            val title = dir.title?.lowercase() ?: ""
            type == "artist" || type == "music" || type == "audio" || type == "track" ||
                title.contains("music") || title.contains("song") || title.contains("audio") || title.contains("track")
        }.ifEmpty {
            sections
        }

        val tracksList = mutableListOf<MusicTrack>()

        // Step 2: Fetch tracks from each music section
        for (sec in musicSections) {
            val key = (sec.key ?: "").removePrefix("/library/sections/").trim()
            if (key.isBlank()) continue

            // Try type=10 (tracks) first
            var metadataItems: List<PlexTrackMetadata> = emptyList()
            val queryUrls = listOf(
                "$workingRoot/library/sections/$key/all?type=10&X-Plex-Token=$cleanToken",
                "$workingRoot/library/sections/$key/all?X-Plex-Token=$cleanToken"
            )

            for (tracksUrl in queryUrls) {
                try {
                    val tracksReq = Request.Builder()
                        .url(tracksUrl)
                        .get()
                        .apply { buildStandardHeaders(this, cleanToken) }
                        .build()

                    val tracksRes = client.newCall(tracksReq).execute()
                    if (tracksRes.isSuccessful) {
                        val body = tracksRes.body?.string() ?: ""
                        val tracksAdapter = moshi.adapter(PlexTracksResponse::class.java)
                        val items = tracksAdapter.fromJson(body)?.mediaContainer?.metadata ?: emptyList()
                        if (items.isNotEmpty()) {
                            metadataItems = items
                            break
                        }
                    }
                } catch (_: Exception) {}
            }

            for (item in metadataItems) {
                val ratingKey = item.ratingKey ?: item.key?.substringAfterLast("/") ?: continue
                val partKey = item.media?.firstOrNull()?.part?.firstOrNull()?.key ?: ""
                val streamUrl = if (partKey.isNotBlank()) {
                    val cleanPart = if (partKey.startsWith("/")) partKey else "/$partKey"
                    "$workingRoot$cleanPart?X-Plex-Token=$cleanToken"
                } else {
                    ""
                }

                val thumbPath = item.thumb ?: item.parentThumb ?: item.grandparentThumb ?: ""
                val coverUrl = if (thumbPath.isNotBlank()) {
                    val cleanThumb = if (thumbPath.startsWith("/")) thumbPath else "/$thumbPath"
                    "$workingRoot$cleanThumb?X-Plex-Token=$cleanToken"
                } else {
                    ""
                }

                val genreTag = item.genreList?.firstOrNull()?.tag?.takeIf { it.isNotBlank() } ?: "Music"
                val trackIndex = item.index ?: (tracksList.size + 1)
                val trackTitle = item.title?.takeIf { it.isNotBlank() } ?: "Track $trackIndex"
                val artistName = item.grandparentTitle?.takeIf { it.isNotBlank() }
                    ?: item.parentTitle?.takeIf { it.isNotBlank() }
                    ?: sec.title?.takeIf { it.isNotBlank() }
                    ?: "Plex Artist"
                val albumName = item.parentTitle?.takeIf { it.isNotBlank() }
                    ?: sec.title?.takeIf { it.isNotBlank() }
                    ?: "Plex Album"

                tracksList.add(
                    MusicTrack(
                        id = "plex_${serverId}_$ratingKey",
                        title = trackTitle,
                        artist = artistName,
                        album = albumName,
                        coverUrl = coverUrl,
                        duration = item.duration ?: 0L,
                        serverId = serverId,
                        streamUrl = streamUrl,
                        ratingKey = ratingKey,
                        genre = genreTag,
                        trackNumber = trackIndex
                    )
                )
            }
        }

        // Step 3: Global server-wide fallback search if section queries found 0 tracks
        if (tracksList.isEmpty()) {
            val globalQueries = listOf(
                "$workingRoot/library/all?type=10&X-Plex-Token=$cleanToken",
                "$workingRoot/hubs/search?query=&type=10&X-Plex-Token=$cleanToken"
            )

            for (globalUrl in globalQueries) {
                try {
                    val tracksReq = Request.Builder()
                        .url(globalUrl)
                        .get()
                        .apply { buildStandardHeaders(this, cleanToken) }
                        .build()

                    val tracksRes = client.newCall(tracksReq).execute()
                    if (tracksRes.isSuccessful) {
                        val body = tracksRes.body?.string() ?: ""
                        val tracksAdapter = moshi.adapter(PlexTracksResponse::class.java)
                        val items = tracksAdapter.fromJson(body)?.mediaContainer?.metadata ?: emptyList()

                        for (item in items) {
                            val ratingKey = item.ratingKey ?: item.key?.substringAfterLast("/") ?: continue
                            val partKey = item.media?.firstOrNull()?.part?.firstOrNull()?.key ?: ""
                            val streamUrl = if (partKey.isNotBlank()) {
                                val cleanPart = if (partKey.startsWith("/")) partKey else "/$partKey"
                                "$workingRoot$cleanPart?X-Plex-Token=$cleanToken"
                            } else {
                                ""
                            }

                            val thumbPath = item.thumb ?: item.parentThumb ?: item.grandparentThumb ?: ""
                            val coverUrl = if (thumbPath.isNotBlank()) {
                                val cleanThumb = if (thumbPath.startsWith("/")) thumbPath else "/$thumbPath"
                                "$workingRoot$cleanThumb?X-Plex-Token=$cleanToken"
                            } else {
                                ""
                            }

                            val genreTag = item.genreList?.firstOrNull()?.tag?.takeIf { it.isNotBlank() } ?: "Music"
                            val trackIndex = item.index ?: (tracksList.size + 1)
                            val trackTitle = item.title?.takeIf { it.isNotBlank() } ?: "Track $trackIndex"
                            val artistName = item.grandparentTitle?.takeIf { it.isNotBlank() }
                                ?: item.parentTitle?.takeIf { it.isNotBlank() }
                                ?: "Plex Artist"
                            val albumName = item.parentTitle?.takeIf { it.isNotBlank() } ?: "Plex Album"

                            tracksList.add(
                                MusicTrack(
                                    id = "plex_${serverId}_$ratingKey",
                                    title = trackTitle,
                                    artist = artistName,
                                    album = albumName,
                                    coverUrl = coverUrl,
                                    duration = item.duration ?: 0L,
                                    serverId = serverId,
                                    streamUrl = streamUrl,
                                    ratingKey = ratingKey,
                                    genre = genreTag,
                                    trackNumber = trackIndex
                                )
                            )
                        }

                        if (tracksList.isNotEmpty()) break
                    }
                } catch (_: Exception) {}
            }
        }

        Result.success(tracksList)
    }

    /**
     * Fetches Movies and TV Shows/Episodes from Plex server library sections.
     */
    suspend fun fetchVideoItems(
        serverUrl: String,
        token: String = "",
        serverId: String = "",
        candidateUrls: List<String> = emptyList()
    ): Result<List<PlexVideoItem>> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        val allCandidates = (listOf(serverUrl) + candidateUrls)
            .map { normalizeUrl(it) }
            .filter { it.isNotBlank() }
            .distinct()

        if (allCandidates.isEmpty()) {
            return@withContext Result.failure(Exception("Server URL is empty."))
        }

        var workingRoot = ""
        var sections: List<PlexDirectory> = emptyList()

        for (root in allCandidates) {
            val secReqUrl = "$root/library/sections?X-Plex-Token=$cleanToken"
            try {
                val secReq = Request.Builder()
                    .url(secReqUrl)
                    .get()
                    .apply { buildStandardHeaders(this, cleanToken) }
                    .build()

                val secRes = client.newCall(secReq).execute()
                if (secRes.isSuccessful) {
                    val secBody = secRes.body?.string() ?: ""
                    val secAdapter = moshi.adapter(PlexSectionsResponse::class.java)
                    val parsed = secAdapter.fromJson(secBody)?.mediaContainer?.directory ?: emptyList()
                    if (parsed.isNotEmpty()) {
                        workingRoot = root
                        sections = parsed
                        break
                    }
                }
            } catch (_: Exception) {}
        }

        if (workingRoot.isBlank()) {
            return@withContext Result.failure(Exception("Could not connect to Plex server library."))
        }

        val videoSections = sections.filter { dir ->
            val type = dir.type?.lowercase() ?: ""
            type == "movie" || type == "show"
        }.ifEmpty { sections }

        val videoList = mutableListOf<PlexVideoItem>()

        for (sec in videoSections) {
            val key = (sec.key ?: "").removePrefix("/library/sections/").trim()
            if (key.isBlank()) continue
            val secType = sec.type?.lowercase() ?: ""

            val queryUrls = when {
                secType == "movie" -> listOf(
                    "$workingRoot/library/sections/$key/all?type=1&X-Plex-Token=$cleanToken",
                    "$workingRoot/library/sections/$key/all?X-Plex-Token=$cleanToken"
                )
                secType == "show" -> listOf(
                    "$workingRoot/library/sections/$key/all?type=4&X-Plex-Token=$cleanToken",
                    "$workingRoot/library/sections/$key/all?X-Plex-Token=$cleanToken"
                )
                else -> listOf(
                    "$workingRoot/library/sections/$key/all?X-Plex-Token=$cleanToken"
                )
            }

            var items: List<PlexTrackMetadata> = emptyList()
            for (qUrl in queryUrls) {
                try {
                    val req = Request.Builder()
                        .url(qUrl)
                        .get()
                        .apply { buildStandardHeaders(this, cleanToken) }
                        .build()
                    val res = client.newCall(req).execute()
                    if (res.isSuccessful) {
                        val body = res.body?.string() ?: ""
                        val adapter = moshi.adapter(PlexTracksResponse::class.java)
                        val parsed = adapter.fromJson(body)?.mediaContainer?.metadata ?: emptyList()
                        if (parsed.isNotEmpty()) {
                            items = parsed
                            break
                        }
                    }
                } catch (_: Exception) {}
            }

            for (item in items) {
                val ratingKey = item.ratingKey ?: item.key?.substringAfterLast("/") ?: continue
                val partKey = item.media?.firstOrNull()?.part?.firstOrNull()?.key ?: ""
                val videoUrl = if (partKey.isNotBlank()) {
                    val cleanPart = if (partKey.startsWith("/")) partKey else "/$partKey"
                    "$workingRoot$cleanPart?X-Plex-Token=$cleanToken"
                } else ""

                val thumbPath = item.thumb ?: item.parentThumb ?: item.grandparentThumb ?: ""
                val coverUrl = if (thumbPath.isNotBlank()) {
                    val cleanThumb = if (thumbPath.startsWith("/")) thumbPath else "/$thumbPath"
                    "$workingRoot$cleanThumb?X-Plex-Token=$cleanToken"
                } else ""

                val artPath = item.art ?: ""
                val bannerUrl = if (artPath.isNotBlank()) {
                    val cleanArt = if (artPath.startsWith("/")) artPath else "/$artPath"
                    "$workingRoot$cleanArt?X-Plex-Token=$cleanToken"
                } else ""

                val itemType = item.type?.lowercase() ?: if (secType == "movie") "movie" else "episode"
                val title = item.title?.takeIf { it.isNotBlank() } ?: "Plex Video"
                val showTitle = item.grandparentTitle?.takeIf { it.isNotBlank() }
                    ?: sec.title?.takeIf { it.isNotBlank() }
                    ?: "Plex Show"

                val seasonEp = if (item.parentIndex != null && item.index != null) {
                    "S${item.parentIndex}E${item.index}"
                } else if (item.parentTitle != null) {
                    item.parentTitle
                } else ""

                val genreTag = item.genreList?.firstOrNull()?.tag?.takeIf { it.isNotBlank() }
                    ?: if (itemType == "movie") "Movie" else "TV Show"

                videoList.add(
                    PlexVideoItem(
                        id = "plex_vid_${serverId}_$ratingKey",
                        title = title,
                        type = itemType,
                        showTitle = showTitle,
                        seasonEpisodeLabel = seasonEp,
                        summary = item.summary ?: "",
                        year = item.year ?: item.parentYear,
                        duration = item.duration ?: 0L,
                        coverUrl = coverUrl,
                        bannerUrl = bannerUrl,
                        videoUrl = videoUrl,
                        ratingKey = ratingKey,
                        genre = genreTag,
                        serverId = serverId
                    )
                )
            }
        }

        if (videoList.isEmpty()) {
            val globalUrls = listOf(
                "$workingRoot/library/all?type=1&X-Plex-Token=$cleanToken",
                "$workingRoot/library/all?type=4&X-Plex-Token=$cleanToken"
            )
            for (gUrl in globalUrls) {
                try {
                    val req = Request.Builder()
                        .url(gUrl)
                        .get()
                        .apply { buildStandardHeaders(this, cleanToken) }
                        .build()
                    val res = client.newCall(req).execute()
                    if (res.isSuccessful) {
                        val body = res.body?.string() ?: ""
                        val adapter = moshi.adapter(PlexTracksResponse::class.java)
                        val items = adapter.fromJson(body)?.mediaContainer?.metadata ?: emptyList()
                        for (item in items) {
                            val ratingKey = item.ratingKey ?: continue
                            val partKey = item.media?.firstOrNull()?.part?.firstOrNull()?.key ?: ""
                            val videoUrl = if (partKey.isNotBlank()) {
                                val cleanPart = if (partKey.startsWith("/")) partKey else "/$partKey"
                                "$workingRoot$cleanPart?X-Plex-Token=$cleanToken"
                            } else ""
                            val thumbPath = item.thumb ?: item.parentThumb ?: ""
                            val coverUrl = if (thumbPath.isNotBlank()) {
                                val cleanThumb = if (thumbPath.startsWith("/")) thumbPath else "/$thumbPath"
                                "$workingRoot$cleanThumb?X-Plex-Token=$cleanToken"
                            } else ""

                            videoList.add(
                                PlexVideoItem(
                                    id = "plex_vid_${serverId}_$ratingKey",
                                    title = item.title ?: "Plex Video",
                                    type = item.type?.lowercase() ?: "movie",
                                    showTitle = item.grandparentTitle ?: "",
                                    seasonEpisodeLabel = if (item.parentIndex != null && item.index != null) "S${item.parentIndex}E${item.index}" else "",
                                    summary = item.summary ?: "",
                                    year = item.year,
                                    duration = item.duration ?: 0L,
                                    coverUrl = coverUrl,
                                    videoUrl = videoUrl,
                                    ratingKey = ratingKey,
                                    serverId = serverId
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        Result.success(videoList)
    }

    /**
     * Executes comprehensive diagnostic checks against the Plex server URL,
     * verifying network reachability, identity endpoint, token authorization,
     * and music libraries discovery.
     */
    suspend fun diagnoseConnection(
        serverUrl: String,
        token: String = ""
    ): PlexDiagnosticResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val logs = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        val normalizedUrl = normalizeUrl(serverUrl)

        logs.add("1. Sanitized Plex URL: '$normalizedUrl'")
        if (normalizedUrl.isBlank()) {
            return@withContext PlexDiagnosticResult(
                isReachable = false,
                testedUrl = serverUrl,
                httpStatusCode = null,
                success = false,
                statusMessage = "Plex URL is empty.",
                latencyMs = 0L,
                diagnosticLog = listOf("Error: Please provide a Plex Server IP or URL."),
                recommendations = listOf("Enter your Plex server IP, e.g. http://192.168.1.100:32400")
            )
        }

        var isReachable = false
        var httpStatusCode: Int? = null
        var musicSectionsCount = 0
        var tracksCount = 0
        val cleanToken = token.trim()

        // Step 1: Probe unauthenticated Plex Identity endpoint (/identity)
        logs.add("2. Testing server reachability via /identity endpoint...")
        try {
            val idReq = Request.Builder()
                .url("$normalizedUrl/identity")
                .get()
                .apply { buildStandardHeaders(this, "") }
                .build()

            val idRes = client.newCall(idReq).execute()
            httpStatusCode = idRes.code
            isReachable = true
            logs.add("   -> Server responded to /identity with HTTP ${idRes.code}")
        } catch (e: Exception) {
            logs.add("   -> /identity probe note: ${e.message}")
        }

        // Step 2: Test Token Authorization
        if (cleanToken.isNotBlank()) {
            logs.add("3. Verifying X-Plex-Token against /library/sections...")
            try {
                val secReq = Request.Builder()
                    .url("$normalizedUrl/library/sections?X-Plex-Token=$cleanToken")
                    .get()
                    .apply { buildStandardHeaders(this, cleanToken) }
                    .build()

                val secRes = client.newCall(secReq).execute()
                httpStatusCode = secRes.code
                isReachable = true

                if (secRes.isSuccessful) {
                    val secBody = secRes.body?.string() ?: ""
                    val secAdapter = moshi.adapter(PlexSectionsResponse::class.java)
                    val sections = secAdapter.fromJson(secBody)?.mediaContainer?.directory ?: emptyList()
                    val musicSections = sections.filter { it.type == "artist" }
                    musicSectionsCount = musicSections.size
                    logs.add("   -> Token AUTHENTICATED! Found $musicSectionsCount Music Library sections.")

                    if (musicSections.isNotEmpty()) {
                        val sampleSec = musicSections.first()
                        val sampleReq = Request.Builder()
                            .url("$normalizedUrl/library/sections/${sampleSec.key}/all?type=10&X-Plex-Token=$cleanToken")
                            .get()
                            .apply { buildStandardHeaders(this, cleanToken) }
                            .build()
                        val sampleRes = client.newCall(sampleReq).execute()
                        if (sampleRes.isSuccessful) {
                            val tracksAdapter = moshi.adapter(PlexTracksResponse::class.java)
                            val parsedTracks = tracksAdapter.fromJson(sampleRes.body?.string() ?: "")?.mediaContainer?.metadata ?: emptyList()
                            tracksCount = parsedTracks.size
                            logs.add("   -> Verified tracks retrieval: Sample library contains $tracksCount tracks.")
                        }
                    }
                } else if (secRes.code == 401) {
                    logs.add("   -> HTTP 401 Unauthorized: Plex token is invalid or expired.")
                    recommendations.add("Your Plex Token is invalid or was rejected. Use 'Sign In via Plex PIN' to get an official valid token automatically.")
                    recommendations.add("If finding token manually: Open Plex Web -> Play any track -> Click '...' -> View Info -> View XML -> Copy the token at the end of the URL ('X-Plex-Token=...').")
                } else {
                    logs.add("   -> Server returned HTTP ${secRes.code}")
                    recommendations.add("Server returned HTTP ${secRes.code}. Check that the Plex server is running and accessible.")
                }
            } catch (e: Exception) {
                logs.add("   -> Token verification failed: ${e.message}")
            }
        } else {
            logs.add("3. No X-Plex-Token provided.")
            recommendations.add("Enter your X-Plex-Token or use 'Sign In with Plex PIN' below to authenticate.")
        }

        val latency = System.currentTimeMillis() - startTime
        val success = isReachable && httpStatusCode == 200

        val statusMessage = when {
            success -> "Connected to Plex successfully! ($musicSectionsCount music libraries found, ${latency}ms latency)"
            httpStatusCode == 401 -> "HTTP 401: Token invalid or expired."
            isReachable -> "Plex server reached, but returned HTTP $httpStatusCode."
            else -> "Cannot reach Plex server at '$normalizedUrl'."
        }

        PlexDiagnosticResult(
            isReachable = isReachable,
            testedUrl = normalizedUrl,
            httpStatusCode = httpStatusCode,
            success = success,
            statusMessage = statusMessage,
            latencyMs = latency,
            musicSectionsFound = musicSectionsCount,
            totalTracksFound = tracksCount,
            diagnosticLog = logs,
            recommendations = recommendations
        )
    }
}
