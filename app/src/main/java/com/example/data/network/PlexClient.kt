package com.example.data.network

import android.util.Log
import com.example.data.MusicTrack
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private const val CLIENT_ID = "HomeCast-Android-Client"
    private const val PRODUCT_NAME = "HomeCast"
    private const val VERSION = "1.8"

    private val client: OkHttpClient by lazy {
        try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())

            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create permissive SSL client, falling back to standard client", e)
            OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .build()
        }
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

            val adapter = moshi.adapter(PlexPinResponse::class.java)
            val pin = adapter.fromJson(body)
            if (pin?.code != null && pin.id != null) {
                // Ensure code is formatted cleanly (4 uppercase characters)
                Result.success(pin.copy(code = pin.code.uppercase()))
            } else {
                Result.failure(Exception("Invalid PIN response from Plex"))
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

            val adapter = moshi.adapter(PlexPinResponse::class.java)
            val pin = adapter.fromJson(body)
            Result.success(pin?.authToken)
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

            // Filter for media servers
            val servers = devices.filter { it.provides?.contains("server", ignoreCase = true) == true }
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

                // Probe candidate connections to find active working URI
                var activeUri = ""
                var isLocal = false
                for (uri in sortedCandidates) {
                    if (testConnectionQuick(uri, serverToken)) {
                        activeUri = uri
                        isLocal = uri.contains("192.168.") || uri.contains("10.") || uri.contains("172.") || uri.contains("localhost")
                        break
                    }
                }

                // If fast probing didn't match immediately, fallback to the first candidate URI
                if (activeUri.isBlank() && sortedCandidates.isNotEmpty()) {
                    activeUri = sortedCandidates.first()
                    isLocal = activeUri.contains("192.168.") || activeUri.contains("10.") || activeUri.contains("172.")
                }

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
                            isReachable = activeUri.isNotBlank()
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

    fun testConnectionQuick(serverUrl: String, token: String): Boolean {
        val root = normalizeUrl(serverUrl)
        if (root.isBlank()) return false
        val cleanToken = token.trim()
        val probeClient = client.newBuilder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
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
     * Tests connection to Plex server with both header and query param token fallbacks.
     */
    suspend fun testConnection(serverUrl: String, token: String, candidateUrls: List<String> = emptyList()): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            return@withContext Result.failure(Exception("X-Plex-Token cannot be empty. Please sign in with your Plex account."))
        }

        val allCandidates = (listOf(serverUrl) + candidateUrls)
            .map { normalizeUrl(it) }
            .filter { it.isNotBlank() }
            .distinct()

        if (allCandidates.isEmpty()) {
            return@withContext Result.failure(Exception("Server URL cannot be empty."))
        }

        var lastError: Exception? = null
        for (root in allCandidates) {
            val candidateUrls = listOf(
                "$root/library/sections",
                "$root/library/sections?X-Plex-Token=$cleanToken"
            )

            for (url in candidateUrls) {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .apply { buildStandardHeaders(this, cleanToken) }
                    .build()

                try {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        return@withContext Result.success(true)
                    } else if (response.code == 401) {
                        lastError = Exception("HTTP 401 Unauthorized: Plex token is invalid for $root.")
                    } else {
                        lastError = Exception("Plex returned HTTP ${response.code} from $url")
                    }
                } catch (e: Exception) {
                    lastError = e
                }
            }
        }

        Result.failure(lastError ?: Exception("Unable to connect to Plex."))
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

        Result.success(tracksList)
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
