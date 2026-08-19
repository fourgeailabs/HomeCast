package com.example.data.network

import android.util.Log
import com.example.data.MusicTrack
import com.squareup.moshi.Moshi
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
     */
    suspend fun createPin(): Result<PlexPinResponse> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://plex.tv/api/v2/pins?strong=true")
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
                Result.success(pin)
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
     * Tests connection to Plex server with both header and query param token fallbacks.
     */
    suspend fun testConnection(serverUrl: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val root = normalizeUrl(serverUrl)
        if (root.isBlank()) {
            return@withContext Result.failure(Exception("Server URL cannot be empty."))
        }
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            return@withContext Result.failure(Exception("X-Plex-Token cannot be empty. Enter your Plex token or sign in via Plex PIN."))
        }

        // Test with headers and query parameters
        val candidateUrls = listOf(
            "$root/library/sections",
            "$root/library/sections?X-Plex-Token=$cleanToken"
        )

        var lastError: Exception? = null
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
                    return@withContext Result.failure(
                        Exception("HTTP 401 Unauthorized: The Plex token is invalid or unauthorized for $root. Please verify your X-Plex-Token or use 'Sign In with Plex PIN'.")
                    )
                } else {
                    lastError = Exception("Plex returned HTTP ${response.code} from $url")
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        Result.failure(lastError ?: Exception("Unable to connect to Plex at $root"))
    }

    /**
     * Fetches all music tracks across artist music libraries in Plex.
     */
    suspend fun fetchMusicTracks(serverUrl: String, token: String, serverId: String): Result<List<MusicTrack>> = withContext(Dispatchers.IO) {
        val root = normalizeUrl(serverUrl)
        val cleanToken = token.trim()

        try {
            // 1. Get Library sections
            val secReq = Request.Builder()
                .url("$root/library/sections?X-Plex-Token=$cleanToken")
                .get()
                .apply { buildStandardHeaders(this, cleanToken) }
                .build()

            val secRes = client.newCall(secReq).execute()
            val secBody = secRes.body?.string() ?: ""

            if (!secRes.isSuccessful) {
                if (secRes.code == 401) {
                    return@withContext Result.failure(Exception("HTTP 401 Unauthorized: Plex token was rejected."))
                }
                return@withContext Result.failure(Exception("Failed to load Plex sections: HTTP ${secRes.code}"))
            }

            val secAdapter = moshi.adapter(PlexSectionsResponse::class.java)
            val sections = secAdapter.fromJson(secBody)?.mediaContainer?.directory ?: emptyList()
            val musicSections = sections.filter { it.type == "artist" }

            val tracksList = mutableListOf<MusicTrack>()

            // 2. For each music section, fetch all tracks (type=10)
            for (sec in musicSections) {
                val tracksReq = Request.Builder()
                    .url("$root/library/sections/${sec.key}/all?type=10&X-Plex-Token=$cleanToken")
                    .get()
                    .apply { buildStandardHeaders(this, cleanToken) }
                    .build()

                val tracksRes = client.newCall(tracksReq).execute()
                if (tracksRes.isSuccessful) {
                    val tracksAdapter = moshi.adapter(PlexTracksResponse::class.java)
                    val metadata = tracksAdapter.fromJson(tracksRes.body?.string() ?: "")?.mediaContainer?.metadata ?: emptyList()

                    for (item in metadata) {
                        val partKey = item.media?.firstOrNull()?.part?.firstOrNull()?.key ?: ""
                        val streamUrl = if (partKey.isNotBlank()) {
                            "$root$partKey?X-Plex-Token=$cleanToken"
                        } else {
                            ""
                        }

                        val thumbPath = item.thumb ?: item.parentThumb ?: item.grandparentThumb
                        val coverUrl = if (!thumbPath.isNullOrBlank()) {
                            "$root$thumbPath?X-Plex-Token=$cleanToken"
                        } else {
                            ""
                        }

                        val genreTag = item.genreList?.firstOrNull()?.tag?.takeIf { it.isNotBlank() } ?: "Music"
                        val trackIndex = item.index ?: (tracksList.size + 1)

                        tracksList.add(
                            MusicTrack(
                                id = "plex_${item.ratingKey}",
                                title = item.title,
                                artist = item.grandparentTitle ?: "Unknown Artist",
                                album = item.parentTitle ?: "Unknown Album",
                                coverUrl = coverUrl,
                                duration = item.duration ?: 0L,
                                serverId = serverId,
                                streamUrl = streamUrl,
                                ratingKey = item.ratingKey,
                                genre = genreTag,
                                trackNumber = trackIndex
                            )
                        )
                    }
                }
            }

            Result.success(tracksList)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
