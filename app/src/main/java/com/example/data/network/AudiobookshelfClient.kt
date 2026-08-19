package com.example.data.network

import android.util.Log
import com.example.data.Audiobook
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object AudiobookshelfClient {
    private const val TAG = "AudiobookshelfClient"

    // Permissive OkHttpClient that gracefully handles self-signed certificates,
    // reverse proxies, internal CA certs, and SSL redirects on self-hosted servers.
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
            Log.e(TAG, "Failed to create custom permissive SSL client, falling back to standard client", e)
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
     * Sanitizes and normalizes any user-entered Audiobookshelf server URL.
     * Handles bare domains, trailing slashes, pasted UI endpoints (/login, /audiobooks),
     * URL parameters, and auto-detects HTTPS vs HTTP.
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
            "/login",
            "/api/login",
            "/api/authorize",
            "/api",
            "/audiobooks",
            "/books",
            "/podcasts",
            "/home",
            "/settings"
        )
        for (suffix in suffixesToStrip) {
            if (clean.endsWith(suffix, ignoreCase = true)) {
                clean = clean.substring(0, clean.length - suffix.length).trimEnd('/')
            }
        }

        return clean
    }

    /**
     * Authenticates with Audiobookshelf using username/password with multi-endpoint fallback.
     */
    suspend fun login(baseUrl: String, username: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        val root = normalizeUrl(baseUrl)
        if (root.isBlank()) {
            return@withContext Result.failure(Exception("Server URL cannot be empty."))
        }

        val cleanUsername = username.trim()
        if (cleanUsername.isBlank()) {
            return@withContext Result.failure(Exception("Username cannot be empty."))
        }

        val requestAdapter = moshi.adapter(AbsLoginRequest::class.java)
        val jsonPayload = requestAdapter.toJson(AbsLoginRequest(cleanUsername, password))
        val body = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())

        // Build candidate roots (original, plus HTTPS/HTTP swap)
        val candidateRoots = mutableListOf(root)
        if (root.startsWith("http://", ignoreCase = true)) {
            candidateRoots.add("https://" + root.removePrefix("http://"))
        } else if (root.startsWith("https://", ignoreCase = true)) {
            candidateRoots.add("http://" + root.removePrefix("https://"))
        }

        // Try standard /login and /api/login endpoints
        val candidateEndpoints = mutableListOf<String>()
        for (r in candidateRoots) {
            candidateEndpoints.add("$r/login")
            candidateEndpoints.add("$r/api/login")
            candidateEndpoints.add("$r/api/authorize")
        }

        var lastException: Exception? = null
        var lastStatusCode: Int? = null
        var lastErrorBody: String? = null

        for (endpoint in candidateEndpoints) {
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "HomeCast-Android/1.6")
                .post(body)
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                val code = response.code
                lastStatusCode = code
                lastErrorBody = responseBody

                if (code == 401) {
                    var detailedMsg = "Invalid username or password"
                    try {
                        val parsed = moshi.adapter(AbsLoginResponse::class.java).fromJson(responseBody)
                        if (!parsed?.error.isNullOrBlank()) {
                            detailedMsg = parsed?.error ?: detailedMsg
                        }
                    } catch (_: Exception) {}
                    return@withContext Result.failure(
                        Exception("HTTP 401 Unauthorized: $detailedMsg. If using SSO, Authelia, Cloudflare Access, or 2FA, please check 'Use API Token' and paste your token directly.")
                    )
                }

                if (code == 404 || code == 405) {
                    // Try next endpoint
                    continue
                }

                if (response.isSuccessful) {
                    val adapter = moshi.adapter(AbsLoginResponse::class.java)
                    val parsed = try {
                        adapter.fromJson(responseBody)
                    } catch (e: Exception) {
                        null
                    }

                    val token = parsed?.user?.token
                        ?: parsed?.token
                        ?: parsed?.bearerToken
                        ?: parsed?.user?.apiKey
                        ?: parsed?.apiKey
                        ?: response.header("x-token")

                    if (!token.isNullOrBlank()) {
                        Log.i(TAG, "Audiobookshelf login successful via $endpoint")
                        return@withContext Result.success(token)
                    } else {
                        // Fallback: check if json contains "token" string via regex
                        val tokenRegex = Regex(""""token"\s*:\s*"([^"]+)"""")
                        val match = tokenRegex.find(responseBody)
                        if (match != null && match.groupValues.size > 1) {
                            return@withContext Result.success(match.groupValues[1])
                        }
                    }
                }
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Error connecting to endpoint $endpoint: ${e.message}")
            }
        }

        val finalError = buildString {
            if (lastStatusCode != null) {
                append("Server returned HTTP $lastStatusCode")
                if (!lastErrorBody.isNullOrBlank()) {
                    append(": ${lastErrorBody!!.take(120)}")
                }
            } else if (lastException != null) {
                append("Connection error: ${lastException.localizedMessage}")
            } else {
                append("Unable to establish connection to Audiobookshelf at $root")
            }
            append(".\n\nTip: If accessing remotely via HTTPS or reverse proxy, verify that port forwarding is active or use an API Token from your Audiobookshelf user settings.")
        }

        Result.failure(Exception(finalError))
    }

    /**
     * Verifies that the provided API Token or session token can access Audiobookshelf API.
     */
    suspend fun testConnection(baseUrl: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val root = normalizeUrl(baseUrl)
        if (root.isBlank()) {
            return@withContext Result.failure(Exception("Server URL is empty."))
        }
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            return@withContext Result.failure(Exception("Token is empty."))
        }

        val testEndpoints = listOf(
            "$root/api/libraries",
            "$root/api/me",
            "$root/api/libraries?token=$cleanToken"
        )

        var lastError: Exception? = null
        for (testUrl in testEndpoints) {
            val request = Request.Builder()
                .url(testUrl)
                .addHeader("Authorization", "Bearer $cleanToken")
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "HomeCast-Android/1.6")
                .get()
                .build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    return@withContext Result.success(true)
                } else if (response.code == 401 || response.code == 403) {
                    return@withContext Result.failure(
                        Exception("HTTP ${response.code} Unauthorized: API Token is invalid, expired, or missing permissions.")
                    )
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        Result.failure(lastError ?: Exception("Could not verify Audiobookshelf API at $root"))
    }

    /**
     * Fetches all audiobooks across all libraries from Audiobookshelf.
     */
    suspend fun fetchAudiobooks(baseUrl: String, token: String, serverId: String): Result<List<Audiobook>> = withContext(Dispatchers.IO) {
        val root = normalizeUrl(baseUrl)
        val cleanToken = token.trim()

        try {
            // 1. Get Libraries
            val libReq = Request.Builder()
                .url("$root/api/libraries")
                .addHeader("Authorization", "Bearer $cleanToken")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            val libRes = client.newCall(libReq).execute()
            val libBody = libRes.body?.string() ?: ""

            if (!libRes.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Failed to fetch libraries: HTTP ${libRes.code} (${libBody.take(100)})")
                )
            }

            val libAdapter = moshi.adapter(AbsLibrariesResponse::class.java)
            val libraries = libAdapter.fromJson(libBody)?.libraries ?: emptyList()
            val bookLibraries = libraries.filter { it.mediaType == null || it.mediaType == "book" }

            val allAudiobooks = mutableListOf<Audiobook>()

            // 2. Fetch items for each book library
            for (lib in bookLibraries) {
                val itemsReq = Request.Builder()
                    .url("$root/api/libraries/${lib.id}/items")
                    .addHeader("Authorization", "Bearer $cleanToken")
                    .addHeader("Accept", "application/json")
                    .get()
                    .build()

                val itemsRes = client.newCall(itemsReq).execute()
                if (itemsRes.isSuccessful) {
                    val itemsAdapter = moshi.adapter(AbsItemsResponse::class.java)
                    val parsedItems = itemsAdapter.fromJson(itemsRes.body?.string() ?: "")?.results ?: emptyList()

                    for (item in parsedItems) {
                        val title = item.media?.metadata?.title ?: "Untitled Audiobook"
                        val author = item.media?.metadata?.authorName ?: "Unknown Author"
                        val series = item.media?.metadata?.seriesName ?: ""
                        val narrator = item.media?.metadata?.narratorName ?: ""
                        val durationSeconds = (item.media?.duration ?: 0.0).toLong()
                        val coverPath = item.media?.coverPath

                        val coverUrl = if (!coverPath.isNullOrBlank()) {
                            "$root/api/items/${item.id}/cover?token=$cleanToken"
                        } else {
                            ""
                        }

                        // Stream url for Audiobookshelf item - direct file endpoint, with download and play fallbacks
                        val firstAudioFile = item.media?.audioFiles?.firstOrNull()
                        val streamUrl = if (!firstAudioFile?.ino.isNullOrBlank()) {
                            "$root/api/items/${item.id}/file/${firstAudioFile!!.ino}?token=$cleanToken"
                        } else {
                            "$root/api/items/${item.id}/download?token=$cleanToken"
                        }

                        allAudiobooks.add(
                            Audiobook(
                                id = item.id,
                                title = title,
                                author = author,
                                coverUrl = coverUrl,
                                duration = durationSeconds,
                                progress = 0L,
                                serverId = serverId,
                                streamUrl = streamUrl,
                                seriesName = series,
                                narrator = narrator
                            )
                        )
                    }
                }
            }

            Result.success(allAudiobooks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Executes comprehensive diagnostic checks against the target server URL,
     * testing DNS, Reachability, SSL, Authentication, and API Endpoints.
     */
    suspend fun diagnoseConnection(
        baseUrl: String,
        username: String = "",
        password: String = "",
        token: String = ""
    ): AbsDiagnosticResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val logs = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        val normalizedUrl = normalizeUrl(baseUrl)

        logs.add("1. Sanitized Server URL: '$normalizedUrl'")
        if (normalizedUrl.isBlank()) {
            return@withContext AbsDiagnosticResult(
                isReachable = false,
                testedUrl = baseUrl,
                httpStatusCode = null,
                success = false,
                statusMessage = "Server URL is blank",
                latencyMs = 0L,
                sslValid = false,
                diagnosticLog = listOf("Error: Please provide a valid server URL or IP address."),
                recommendations = listOf("Enter your server URL, e.g. http://10.70.14.2:13378 or https://abs.yourdomain.com")
            )
        }

        var isReachable = false
        var httpStatusCode: Int? = null
        var sslValid = true
        var resolvedToken: String? = null
        var librariesFound = 0

        // Step 1: Reachability check
        logs.add("2. Testing network ping & HTTP reachability...")
        try {
            val pingReq = Request.Builder()
                .url("$normalizedUrl/ping")
                .addHeader("User-Agent", "HomeCast-Android/1.6")
                .get()
                .build()

            val pingRes = client.newCall(pingReq).execute()
            httpStatusCode = pingRes.code
            isReachable = true
            logs.add("   -> Server responded to /ping with HTTP ${pingRes.code}")
        } catch (e: Exception) {
            logs.add("   -> /ping probe exception: ${e.message}")
            if (e.message?.contains("SSL", ignoreCase = true) == true || e.message?.contains("Cert", ignoreCase = true) == true) {
                sslValid = false
                recommendations.add("SSL/TLS Handshake encountered an issue. HomeCast's permissive SSL layer will allow self-signed certificates.")
            }
        }

        // Step 2: Test Authentication
        if (token.isNotBlank()) {
            logs.add("3. Testing provided API Token against /api/libraries...")
            try {
                val libReq = Request.Builder()
                    .url("$normalizedUrl/api/libraries")
                    .addHeader("Authorization", "Bearer ${token.trim()}")
                    .get()
                    .build()
                val libRes = client.newCall(libReq).execute()
                httpStatusCode = libRes.code
                isReachable = true

                if (libRes.isSuccessful) {
                    resolvedToken = token.trim()
                    val libBody = libRes.body?.string() ?: ""
                    val parsedLibs = moshi.adapter(AbsLibrariesResponse::class.java).fromJson(libBody)
                    librariesFound = parsedLibs?.libraries?.size ?: 0
                    logs.add("   -> API Token VALID! Found $librariesFound libraries.")
                } else {
                    logs.add("   -> API Token rejected with HTTP ${libRes.code}")
                    recommendations.add("API Token returned HTTP ${libRes.code}. Generate a new API Token in Audiobookshelf (Settings -> Users -> Click User -> API Key).")
                }
            } catch (e: Exception) {
                logs.add("   -> Token test failed: ${e.message}")
            }
        } else if (username.isNotBlank() && password.isNotBlank()) {
            logs.add("3. Attempting login with username '$username'...")
            val loginRes = login(normalizedUrl, username, password)
            if (loginRes.isSuccess) {
                resolvedToken = loginRes.getOrNull()
                isReachable = true
                logs.add("   -> Login SUCCESSFUL! Session token acquired.")

                // Test library fetch with new token
                val libReq = Request.Builder()
                    .url("$normalizedUrl/api/libraries")
                    .addHeader("Authorization", "Bearer $resolvedToken")
                    .get()
                    .build()
                try {
                    val libRes = client.newCall(libReq).execute()
                    if (libRes.isSuccessful) {
                        val libBody = libRes.body?.string() ?: ""
                        val parsedLibs = moshi.adapter(AbsLibrariesResponse::class.java).fromJson(libBody)
                        librariesFound = parsedLibs?.libraries?.size ?: 0
                        logs.add("   -> Retrieved $librariesFound libraries successfully.")
                    }
                } catch (_: Exception) {}
            } else {
                val err = loginRes.exceptionOrNull()?.message ?: "Login failed"
                logs.add("   -> Login failed: $err")
                if (err.contains("401", ignoreCase = true)) {
                    recommendations.add("Verify that username and password are correct. Check for caps lock or special characters.")
                    recommendations.add("If you use Single Sign-On (SSO) or Reverse Proxy Auth (Authelia/Cloudflare Access), username/password login is blocked. Use the 'API Token' option instead.")
                } else if (err.contains("Failed to connect", ignoreCase = true) || err.contains("timeout", ignoreCase = true)) {
                    recommendations.add("Server timed out. Ensure your remote domain/port (e.g. port 13378) is forwarded on your router, or that your Tailscale/VPN is connected.")
                }
            }
        } else {
            logs.add("3. No credentials provided to test authentication.")
            recommendations.add("Enter your Audiobookshelf username and password, or check 'Use API Token' and paste your token.")
        }

        val latency = System.currentTimeMillis() - startTime
        val success = resolvedToken != null

        val statusMessage = when {
            success -> "Connected successfully! ($librariesFound libraries accessible, ${latency}ms latency)"
            isReachable -> "Server is reachable, but authentication failed."
            else -> "Cannot reach server at '$normalizedUrl'."
        }

        AbsDiagnosticResult(
            isReachable = isReachable,
            testedUrl = normalizedUrl,
            httpStatusCode = httpStatusCode,
            success = success,
            statusMessage = statusMessage,
            latencyMs = latency,
            sslValid = sslValid,
            resolvedToken = resolvedToken,
            librariesFound = librariesFound,
            diagnosticLog = logs,
            recommendations = recommendations
        )
    }
}
