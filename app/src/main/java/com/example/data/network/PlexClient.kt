package com.example.data.network

import android.util.Log
import com.example.data.*
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

    fun optJsonList(parent: org.json.JSONObject, key: String): List<org.json.JSONObject> {
        if (!parent.has(key) || parent.isNull(key)) return emptyList()
        val list = mutableListOf<org.json.JSONObject>()
        val item = parent.get(key)
        if (item is org.json.JSONArray) {
            for (i in 0 until item.length()) {
                val obj = item.optJSONObject(i)
                if (obj != null) list.add(obj)
            }
        } else if (item is org.json.JSONObject) {
            list.add(item)
        }
        return list
    }

    fun parseJsonArrayOrObjectList(jsonStr: String, rootKey: String = "MediaContainer", listKey: String): List<org.json.JSONObject> {
        val trimmed = jsonStr.trim()
        if (trimmed.isBlank()) return emptyList()

        // 1. If response is XML, parse XML nodes into JSONObjects
        if (trimmed.startsWith("<")) {
            return when {
                listKey.equals("Directory", ignoreCase = true) -> parsePlexXmlToDirectoryList(trimmed)
                listKey.equals("Metadata", ignoreCase = true) || listKey.equals("Video", ignoreCase = true) || listKey.equals("Track", ignoreCase = true) -> parsePlexXmlToMetadataList(trimmed)
                listKey.equals("Device", ignoreCase = true) || listKey.equals("Server", ignoreCase = true) -> parsePlexXmlToDeviceList(trimmed)
                else -> emptyList()
            }
        }

        // 2. Parse JSON
        try {
            if (trimmed.startsWith("[")) {
                val array = org.json.JSONArray(trimmed)
                val list = mutableListOf<org.json.JSONObject>()
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i)
                    if (item != null) list.add(item)
                }
                return list
            }

            val rootObj = org.json.JSONObject(trimmed)
            val container = if (rootObj.has(rootKey)) rootObj.optJSONObject(rootKey) else rootObj
            if (container != null) {
                val list = optJsonList(container, listKey)
                if (list.isNotEmpty()) return list
                // Try lowercase or alternative key casing
                val altKey = if (listKey.equals("Directory", ignoreCase = true)) "directory"
                else if (listKey.equals("Metadata", ignoreCase = true)) "metadata"
                else if (listKey.equals("Device", ignoreCase = true)) "device"
                else listKey
                return optJsonList(container, altKey)
            }
        } catch (_: Exception) {}
        return emptyList()
    }

    private fun parsePlexXmlToDirectoryList(xmlStr: String): List<org.json.JSONObject> {
        val list = mutableListOf<org.json.JSONObject>()
        try {
            val parser = android.util.Xml.newPullParser()
            parser.setInput(java.io.StringReader(xmlStr))
            var eventType = parser.eventType
            while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG) {
                    val tagName = parser.name
                    if (tagName.equals("Directory", ignoreCase = true) || tagName.equals("Server", ignoreCase = true) || tagName.equals("Location", ignoreCase = true)) {
                        val obj = org.json.JSONObject()
                        for (i in 0 until parser.attributeCount) {
                            obj.put(parser.getAttributeName(i), parser.getAttributeValue(i))
                        }
                        list.add(obj)
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return list
    }

    private fun parsePlexXmlToMetadataList(xmlStr: String): List<org.json.JSONObject> {
        val list = mutableListOf<org.json.JSONObject>()
        try {
            val parser = android.util.Xml.newPullParser()
            parser.setInput(java.io.StringReader(xmlStr))
            var eventType = parser.eventType
            var currentItem: org.json.JSONObject? = null
            var mediaList = mutableListOf<org.json.JSONObject>()
            var genreList = mutableListOf<org.json.JSONObject>()
            var currentMedia: org.json.JSONObject? = null
            var partList = mutableListOf<org.json.JSONObject>()

            while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG) {
                    val tagName = parser.name
                    when {
                        tagName.equals("Metadata", ignoreCase = true) || tagName.equals("Track", ignoreCase = true) ||
                            tagName.equals("Video", ignoreCase = true) || tagName.equals("Photo", ignoreCase = true) ||
                            tagName.equals("Directory", ignoreCase = true) -> {
                            currentItem = org.json.JSONObject()
                            for (i in 0 until parser.attributeCount) {
                                currentItem.put(parser.getAttributeName(i), parser.getAttributeValue(i))
                            }
                            mediaList = mutableListOf()
                            genreList = mutableListOf()
                        }
                        tagName.equals("Media", ignoreCase = true) -> {
                            currentMedia = org.json.JSONObject()
                            for (i in 0 until parser.attributeCount) {
                                currentMedia.put(parser.getAttributeName(i), parser.getAttributeValue(i))
                            }
                            partList = mutableListOf()
                        }
                        tagName.equals("Part", ignoreCase = true) -> {
                            val partObj = org.json.JSONObject()
                            for (i in 0 until parser.attributeCount) {
                                partObj.put(parser.getAttributeName(i), parser.getAttributeValue(i))
                            }
                            partList.add(partObj)
                        }
                        tagName.equals("Genre", ignoreCase = true) -> {
                            val genreObj = org.json.JSONObject()
                            for (i in 0 until parser.attributeCount) {
                                genreObj.put(parser.getAttributeName(i), parser.getAttributeValue(i))
                            }
                            genreList.add(genreObj)
                        }
                    }
                } else if (eventType == org.xmlpull.v1.XmlPullParser.END_TAG) {
                    val tagName = parser.name
                    when {
                        tagName.equals("Media", ignoreCase = true) -> {
                            if (currentMedia != null) {
                                currentMedia.put("Part", org.json.JSONArray(partList))
                                mediaList.add(currentMedia)
                                currentMedia = null
                            }
                        }
                        tagName.equals("Metadata", ignoreCase = true) || tagName.equals("Track", ignoreCase = true) ||
                            tagName.equals("Video", ignoreCase = true) || tagName.equals("Photo", ignoreCase = true) ||
                            tagName.equals("Directory", ignoreCase = true) -> {
                            if (currentItem != null) {
                                if (mediaList.isNotEmpty()) currentItem.put("Media", org.json.JSONArray(mediaList))
                                if (genreList.isNotEmpty()) currentItem.put("Genre", org.json.JSONArray(genreList))
                                list.add(currentItem)
                                currentItem = null
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return list
    }

    private fun parsePlexXmlToDeviceList(xmlStr: String): List<org.json.JSONObject> {
        val list = mutableListOf<org.json.JSONObject>()
        try {
            val parser = android.util.Xml.newPullParser()
            parser.setInput(java.io.StringReader(xmlStr))
            var eventType = parser.eventType
            var currentDevice: org.json.JSONObject? = null
            var connList = mutableListOf<org.json.JSONObject>()

            while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG) {
                    val tagName = parser.name
                    when {
                        tagName.equals("Device", ignoreCase = true) || tagName.equals("Server", ignoreCase = true) -> {
                            currentDevice = org.json.JSONObject()
                            for (i in 0 until parser.attributeCount) {
                                currentDevice.put(parser.getAttributeName(i), parser.getAttributeValue(i))
                            }
                            connList = mutableListOf()
                        }
                        tagName.equals("Connection", ignoreCase = true) -> {
                            val connObj = org.json.JSONObject()
                            for (i in 0 until parser.attributeCount) {
                                connObj.put(parser.getAttributeName(i), parser.getAttributeValue(i))
                            }
                            connList.add(connObj)
                        }
                    }
                } else if (eventType == org.xmlpull.v1.XmlPullParser.END_TAG) {
                    val tagName = parser.name
                    if (tagName.equals("Device", ignoreCase = true) || tagName.equals("Server", ignoreCase = true)) {
                        if (currentDevice != null) {
                            if (connList.isNotEmpty()) {
                                currentDevice.put("connections", org.json.JSONArray(connList))
                                currentDevice.put("Connection", org.json.JSONArray(connList))
                            }
                            list.add(currentDevice)
                            currentDevice = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return list
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
     * Fetches all Plex Media Servers linked to the user's Plex account via plex.tv/api/v2/resources or fallback plex.tv/pms/resources.
     * Probes and resolves the best reachable connection (local LAN IP, secure plex.direct, or remote)
     * so the user NEVER has to manually enter an IP address or port!
     */
    suspend fun fetchAccountServers(authToken: String): Result<List<DiscoveredPlexServer>> = withContext(Dispatchers.IO) {
        val cleanToken = authToken.trim()
        if (cleanToken.isBlank()) {
            return@withContext Result.failure(Exception("Auth token is empty."))
        }

        val resourceUrls = listOf(
            "https://plex.tv/api/v2/resources?includeHttps=1&includeRelay=1",
            "https://plex.tv/pms/resources?includeHttps=1&includeRelay=1"
        )

        var deviceJsonList = emptyList<org.json.JSONObject>()
        var lastErr: Exception? = null

        for (resUrl in resourceUrls) {
            try {
                val request = Request.Builder()
                    .url(resUrl)
                    .get()
                    .apply { buildStandardHeaders(this, cleanToken) }
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                if (response.isSuccessful && body.isNotBlank()) {
                    val parsed = parseJsonArrayOrObjectList(body, "MediaContainer", "Device")
                        .ifEmpty { parseJsonArrayOrObjectList(body, "MediaContainer", "Server") }
                    if (parsed.isNotEmpty()) {
                        deviceJsonList = parsed
                        break
                    }
                } else {
                    lastErr = Exception("Plex resources returned HTTP ${response.code}")
                }
            } catch (e: Exception) {
                lastErr = e
            }
        }

        try {
            val discoveredList = mutableListOf<DiscoveredPlexServer>()

            for (device in deviceJsonList) {
                val provides = device.optString("provides", "")
                if (provides.isNotBlank() && !provides.contains("server", ignoreCase = true)) continue

                val serverName = device.optString("name", "Plex Server").ifBlank { "Plex Server" }
                val clientIdentifier = device.optString("clientIdentifier", serverName)
                val serverToken = device.optString("accessToken", cleanToken).ifBlank { cleanToken }
                val owned = device.optBoolean("owned", true)

                val connJsonList = optJsonList(device, "connections")
                    .ifEmpty { optJsonList(device, "Connection") }
                    .ifEmpty { optJsonList(device, "connection") }

                val candidateUris = mutableListOf<String>()
                for (conn in connJsonList) {
                    val uri = conn.optString("uri", "")
                    if (uri.isNotBlank()) candidateUris.add(uri)
                    val address = conn.optString("address", "")
                    val port = if (conn.has("port")) conn.optInt("port") else 32400
                    if (address.isNotBlank()) {
                        val httpUri = "http://$address:$port"
                        val httpsUri = "https://$address:$port"
                        if (!candidateUris.contains(httpUri)) candidateUris.add(httpUri)
                        if (!candidateUris.contains(httpsUri)) candidateUris.add(httpsUri)
                    }
                }

                // If device itself has host/address properties
                val devAddress = device.optString("address", device.optString("publicAddress", ""))
                val devPort = if (device.has("port")) device.optInt("port") else 32400
                if (devAddress.isNotBlank()) {
                    val httpUri = "http://$devAddress:$devPort"
                    val httpsUri = "https://$devAddress:$devPort"
                    if (!candidateUris.contains(httpUri)) candidateUris.add(httpUri)
                    if (!candidateUris.contains(httpsUri)) candidateUris.add(httpsUri)
                }

                val sortedCandidates = candidateUris.distinct().sortedWith(
                    compareByDescending<String> { it.contains("192.168.") || it.contains("10.") || it.contains("172.") }
                        .thenByDescending { it.startsWith("http://") && !it.contains("relay.plex.services") }
                        .thenByDescending { it.startsWith("https://") && !it.contains("relay.plex.services") }
                        .thenBy { it.contains("relay.plex.services") }
                )

                val activeUri = findFastestReachableUri(sortedCandidates, serverToken).ifBlank { sortedCandidates.firstOrNull() ?: "" }
                val isLocal = activeUri.contains("192.168.") || activeUri.contains("10.") || activeUri.contains("172.") || activeUri.contains("localhost")

                if (activeUri.isNotBlank()) {
                    discoveredList.add(
                        DiscoveredPlexServer(
                            name = serverName,
                            clientIdentifier = clientIdentifier,
                            token = serverToken,
                            preferredUri = activeUri,
                            isLocal = isLocal,
                            candidateUris = sortedCandidates,
                            owned = owned,
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
     * Concurrently probes candidate URIs in parallel, returning as soon as a working address is found.
     */
    suspend fun findFastWorkingUri(candidateUris: List<String>, token: String): String = coroutineScope {
        if (candidateUris.isEmpty()) return@coroutineScope ""
        val cleaned = candidateUris.map { normalizeUrl(it) }.filter { it.isNotBlank() }.distinct()
        if (cleaned.size <= 1) return@coroutineScope cleaned.firstOrNull() ?: ""

        val jobs = cleaned.map { uri ->
            async(Dispatchers.IO) {
                if (testConnectionQuick(uri, token)) uri else null
            }
        }

        val results = jobs.awaitAll()
        results.firstOrNull { !it.isNullOrBlank() } ?: cleaned.first()
    }

    private suspend fun findFastestReachableUri(candidateUris: List<String>, token: String): String = findFastWorkingUri(candidateUris, token)

    fun testConnectionQuick(serverUrl: String, token: String): Boolean {
        val root = normalizeUrl(serverUrl)
        if (root.isBlank()) return false
        val cleanToken = token.trim()
        val probeClient = client.newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
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
            "$root/library/sections?X-Plex-Token=$token",
            "$root/library/sections",
            "$root/identity?X-Plex-Token=$token",
            "$root/identity",
            "$root/?X-Plex-Token=$token"
        )
        val fastClient = client.newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        for (url in candidateEndpoints) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .apply { buildStandardHeaders(this, token) }
                    .build()
                val response = fastClient.newCall(request).execute()
                val isSuccess = response.isSuccessful || response.code == 401
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
        var directoryList = emptyList<org.json.JSONObject>()
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
                    val parsedDirs = parseJsonArrayOrObjectList(secBody, "MediaContainer", "Directory")
                    workingRoot = candidate
                    directoryList = parsedDirs
                    if (parsedDirs.isNotEmpty()) {
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

        val musicSections = directoryList.filter { dir ->
            val type = dir.optString("type", "").lowercase()
            val title = dir.optString("title", "").lowercase()
            type == "artist" || type == "music" || type == "audio" || type == "track" || type == "album" ||
                title.contains("music") || title.contains("song") || title.contains("audio") || title.contains("track") || title.contains("book") || title.contains("podcast")
        }.ifEmpty { directoryList }

        val tracksList = mutableListOf<MusicTrack>()

        // Step 2: Fetch tracks from each section
        for (sec in musicSections) {
            val key = sec.optString("key", "").removePrefix("/library/sections/").trim()
            if (key.isBlank()) continue
            val secTitle = sec.optString("title", "Music")

            var metadataItems: List<org.json.JSONObject> = emptyList()
            val queryUrls = listOf(
                "$workingRoot/library/sections/$key/allLeaves?X-Plex-Token=$cleanToken",
                "$workingRoot/library/sections/$key/all?type=10&X-Plex-Token=$cleanToken",
                "$workingRoot/library/sections/$key/all?X-Plex-Token=$cleanToken",
                "$workingRoot/library/sections/$key/search?type=10&X-Plex-Token=$cleanToken"
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
                        val items = parseJsonArrayOrObjectList(body, "MediaContainer", "Metadata")
                            .ifEmpty { parseJsonArrayOrObjectList(body, "MediaContainer", "Track") }
                            .ifEmpty { parseJsonArrayOrObjectList(body, "MediaContainer", "Directory") }
                        if (items.isNotEmpty()) {
                            metadataItems = items
                            break
                        }
                    }
                } catch (_: Exception) {}
            }

            for (item in metadataItems) {
                val ratingKey = item.optString("ratingKey", item.optString("key", "").substringAfterLast("/"))
                if (ratingKey.isBlank()) continue

                var mediaList = optJsonList(item, "Media")
                var partList = if (mediaList.isNotEmpty()) optJsonList(mediaList.first(), "Part") else emptyList()

                // If this is a container (Artist or Album) without direct Media, attempt to fetch its children/leaves
                val itemType = item.optString("type", "").lowercase()
                if (partList.isEmpty() && (itemType == "artist" || itemType == "album")) {
                    try {
                        val leafUrl = "$workingRoot/library/metadata/$ratingKey/allLeaves?X-Plex-Token=$cleanToken"
                        val leafReq = Request.Builder().url(leafUrl).get().apply { buildStandardHeaders(this, cleanToken) }.build()
                        val leafRes = client.newCall(leafReq).execute()
                        if (leafRes.isSuccessful) {
                            val leafBody = leafRes.body?.string() ?: ""
                            val leaves = parseJsonArrayOrObjectList(leafBody, "MediaContainer", "Metadata")
                                .ifEmpty { parseJsonArrayOrObjectList(leafBody, "MediaContainer", "Track") }
                            for (leaf in leaves) {
                                val leafKey = leaf.optString("ratingKey", leaf.optString("key", "").substringAfterLast("/"))
                                if (leafKey.isBlank()) continue
                                val leafMedia = optJsonList(leaf, "Media")
                                val leafPart = if (leafMedia.isNotEmpty()) optJsonList(leafMedia.first(), "Part") else emptyList()
                                val leafPartKey = leafPart.firstOrNull()?.optString("key", "") ?: ""
                                val streamUrl = if (leafPartKey.isNotBlank()) {
                                    val cleanPart = if (leafPartKey.startsWith("/")) leafPartKey else "/$leafPartKey"
                                    "$workingRoot$cleanPart?X-Plex-Token=$cleanToken"
                                } else ""

                                val thumbPath = leaf.optString("thumb", leaf.optString("parentThumb", leaf.optString("grandparentThumb", item.optString("thumb", ""))))
                                val coverUrl = if (thumbPath.isNotBlank()) {
                                    val cleanThumb = if (thumbPath.startsWith("/")) thumbPath else "/$thumbPath"
                                    "$workingRoot$cleanThumb?X-Plex-Token=$cleanToken"
                                } else ""

                                val genreList = optJsonList(leaf, "Genre").ifEmpty { optJsonList(item, "Genre") }
                                val genreTag = genreList.firstOrNull()?.optString("tag", "")?.takeIf { it.isNotBlank() } ?: "Music"
                                val trackIndex = if (leaf.has("index")) leaf.optInt("index") else (tracksList.size + 1)
                                val trackTitle = leaf.optString("title", "Track $trackIndex")
                                val artistName = leaf.optString("grandparentTitle", leaf.optString("originalTitle", leaf.optString("parentTitle", item.optString("title", secTitle)))).ifBlank { "Plex Artist" }
                                val albumName = leaf.optString("parentTitle", item.optString("title", secTitle)).ifBlank { "Plex Album" }
                                val duration = leaf.optLong("duration", 0L)

                                tracksList.add(
                                    MusicTrack(
                                        id = "plex_${serverId}_$leafKey",
                                        title = trackTitle,
                                        artist = artistName,
                                        album = albumName,
                                        coverUrl = coverUrl,
                                        duration = duration,
                                        serverId = serverId,
                                        streamUrl = streamUrl,
                                        ratingKey = leafKey,
                                        genre = genreTag,
                                        trackNumber = trackIndex
                                    )
                                )
                            }
                            continue
                        }
                    } catch (_: Exception) {}
                }

                val partKey = partList.firstOrNull()?.optString("key", "") ?: ""
                val streamUrl = if (partKey.isNotBlank()) {
                    val cleanPart = if (partKey.startsWith("/")) partKey else "/$partKey"
                    "$workingRoot$cleanPart?X-Plex-Token=$cleanToken"
                } else ""

                val thumbPath = item.optString("thumb", item.optString("parentThumb", item.optString("grandparentThumb", "")))
                val coverUrl = if (thumbPath.isNotBlank()) {
                    val cleanThumb = if (thumbPath.startsWith("/")) thumbPath else "/$thumbPath"
                    "$workingRoot$cleanThumb?X-Plex-Token=$cleanToken"
                } else ""

                val genreList = optJsonList(item, "Genre")
                val genreTag = genreList.firstOrNull()?.optString("tag", "")?.takeIf { it.isNotBlank() } ?: "Music"
                val trackIndex = if (item.has("index")) item.optInt("index") else (tracksList.size + 1)
                val trackTitle = item.optString("title", "Track $trackIndex")
                val artistName = item.optString("grandparentTitle", item.optString("originalTitle", item.optString("parentTitle", secTitle))).ifBlank { "Plex Artist" }
                val albumName = item.optString("parentTitle", secTitle).ifBlank { "Plex Album" }
                val duration = item.optLong("duration", 0L)

                tracksList.add(
                    MusicTrack(
                        id = "plex_${serverId}_$ratingKey",
                        title = trackTitle,
                        artist = artistName,
                        album = albumName,
                        coverUrl = coverUrl,
                        duration = duration,
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
                "$workingRoot/library/all?X-Plex-Token=$cleanToken"
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
                        val items = parseJsonArrayOrObjectList(body, "MediaContainer", "Metadata")

                        for (item in items) {
                            val ratingKey = item.optString("ratingKey", item.optString("key", "").substringAfterLast("/"))
                            if (ratingKey.isBlank()) continue

                            val mediaList = optJsonList(item, "Media")
                            val firstMedia = mediaList.firstOrNull()
                            val partList = if (firstMedia != null) optJsonList(firstMedia, "Part") else emptyList()
                            val partKey = partList.firstOrNull()?.optString("key", "") ?: ""

                            val streamUrl = if (partKey.isNotBlank()) {
                                val cleanPart = if (partKey.startsWith("/")) partKey else "/$partKey"
                                "$workingRoot$cleanPart?X-Plex-Token=$cleanToken"
                            } else ""

                            val thumbPath = item.optString("thumb", item.optString("parentThumb", item.optString("grandparentThumb", "")))
                            val coverUrl = if (thumbPath.isNotBlank()) {
                                val cleanThumb = if (thumbPath.startsWith("/")) thumbPath else "/$thumbPath"
                                "$workingRoot$cleanThumb?X-Plex-Token=$cleanToken"
                            } else ""

                            val genreList = optJsonList(item, "Genre")
                            val genreTag = genreList.firstOrNull()?.optString("tag", "")?.takeIf { it.isNotBlank() } ?: "Music"
                            val trackIndex = if (item.has("index")) item.optInt("index") else (tracksList.size + 1)
                            val trackTitle = item.optString("title", "Track $trackIndex")
                            val artistName = item.optString("grandparentTitle", item.optString("originalTitle", item.optString("parentTitle", "Plex Artist"))).ifBlank { "Plex Artist" }
                            val albumName = item.optString("parentTitle", "Plex Album").ifBlank { "Plex Album" }
                            val duration = item.optLong("duration", 0L)

                            tracksList.add(
                                MusicTrack(
                                    id = "plex_${serverId}_$ratingKey",
                                    title = trackTitle,
                                    artist = artistName,
                                    album = albumName,
                                    coverUrl = coverUrl,
                                    duration = duration,
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

    private fun parseCastList(
        item: org.json.JSONObject,
        tagKey: String,
        defaultRole: String,
        workingRoot: String,
        cleanToken: String
    ): List<PlexCastMember> {
        val list = optJsonList(item, tagKey)
        if (list.isEmpty()) return emptyList()
        return list.mapNotNull { obj ->
            val name = obj.optString("tag", obj.optString("name", "")).trim()
            if (name.isBlank()) return@mapNotNull null
            val character = obj.optString("role", "").trim()
            val thumbPath = obj.optString("thumb", "")
            val thumbUrl = if (thumbPath.isNotBlank()) {
                val clean = if (thumbPath.startsWith("/")) thumbPath else "/$thumbPath"
                "$workingRoot$clean?X-Plex-Token=$cleanToken"
            } else ""
            val personId = obj.optString("id", "")
            PlexCastMember(
                id = personId,
                name = name,
                role = if (character.isNotBlank()) "Actor" else defaultRole,
                character = character,
                thumbUrl = thumbUrl
            )
        }
    }

    /**
     * Fetches Movies with full cast, directors, writers, producers, summaries, cover/banner art.
     */
    suspend fun fetchRichMovies(
        serverUrl: String,
        token: String = "",
        serverId: String = "",
        candidateUrls: List<String> = emptyList()
    ): Result<List<PlexMovieItem>> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        val allCandidates = (listOf(serverUrl) + candidateUrls)
            .map { normalizeUrl(it) }
            .filter { it.isNotBlank() }
            .distinct()

        if (allCandidates.isEmpty()) {
            return@withContext Result.failure(Exception("Server URL is empty."))
        }

        var workingRoot = ""
        var directoryList = emptyList<org.json.JSONObject>()

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
                    val parsedDirs = parseJsonArrayOrObjectList(secBody, "MediaContainer", "Directory")
                    workingRoot = root
                    directoryList = parsedDirs
                    if (parsedDirs.isNotEmpty()) break
                }
            } catch (_: Exception) {}
        }

        if (workingRoot.isBlank()) {
            return@withContext Result.failure(Exception("Could not connect to Plex server library."))
        }

        val movieSections = directoryList.filter { dir ->
            val type = dir.optString("type", "").lowercase()
            type == "movie"
        }.ifEmpty { directoryList }

        val movieList = mutableListOf<PlexMovieItem>()

        for (sec in movieSections) {
            val key = sec.optString("key", "").removePrefix("/library/sections/").trim()
            if (key.isBlank()) continue

            val queryUrls = listOf(
                "$workingRoot/library/sections/$key/all?type=1&includeGuids=1&X-Plex-Token=$cleanToken",
                "$workingRoot/library/sections/$key/all?X-Plex-Token=$cleanToken"
            )

            var items: List<org.json.JSONObject> = emptyList()
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
                        val parsed = parseJsonArrayOrObjectList(body, "MediaContainer", "Metadata")
                        if (parsed.isNotEmpty()) {
                            items = parsed
                            break
                        }
                    }
                } catch (_: Exception) {}
            }

            for (item in items) {
                val ratingKey = item.optString("ratingKey", item.optString("key", "").substringAfterLast("/"))
                if (ratingKey.isBlank()) continue

                val mediaList = optJsonList(item, "Media")
                val firstMedia = mediaList.firstOrNull()
                val partList = if (firstMedia != null) optJsonList(firstMedia, "Part") else emptyList()
                val partKey = partList.firstOrNull()?.optString("key", "") ?: ""

                val videoUrl = if (partKey.isNotBlank()) {
                    val cleanPart = if (partKey.startsWith("/")) partKey else "/$partKey"
                    "$workingRoot$cleanPart?X-Plex-Token=$cleanToken"
                } else ""

                val thumbPath = item.optString("thumb", item.optString("parentThumb", ""))
                val coverUrl = if (thumbPath.isNotBlank()) {
                    val cleanThumb = if (thumbPath.startsWith("/")) thumbPath else "/$thumbPath"
                    "$workingRoot$cleanThumb?X-Plex-Token=$cleanToken"
                } else ""

                val artPath = item.optString("art", "")
                val bannerUrl = if (artPath.isNotBlank()) {
                    val cleanArt = if (artPath.startsWith("/")) artPath else "/$artPath"
                    "$workingRoot$cleanArt?X-Plex-Token=$cleanToken"
                } else ""

                val title = item.optString("title", "Movie").ifBlank { "Movie" }
                val originalTitle = item.optString("originalTitle", "")
                val tagline = item.optString("tagline", "")
                val summary = item.optString("summary", "")
                val yearVal = if (item.has("year")) item.optInt("year") else null
                val ratingVal = if (item.has("rating")) item.optDouble("rating").toFloat() else null
                val contentRating = item.optString("contentRating", "")
                val studio = item.optString("studio", "")
                val duration = item.optLong("duration", 0L)

                val genreList = optJsonList(item, "Genre").mapNotNull { it.optString("tag", "").takeIf { g -> g.isNotBlank() } }
                val cast = parseCastList(item, "Role", "Actor", workingRoot, cleanToken)
                val directors = parseCastList(item, "Director", "Director", workingRoot, cleanToken)
                val writers = parseCastList(item, "Writer", "Writer", workingRoot, cleanToken)
                val producers = parseCastList(item, "Producer", "Producer", workingRoot, cleanToken)
                val cinematographers = parseCastList(item, "Country", "Cinematographer", workingRoot, cleanToken)

                val similarTitles = optJsonList(item, "Similar").mapNotNull { it.optString("tag", "").takeIf { s -> s.isNotBlank() } }

                movieList.add(
                    PlexMovieItem(
                        id = "plex_movie_${serverId}_$ratingKey",
                        ratingKey = ratingKey,
                        title = title,
                        originalTitle = originalTitle,
                        tagline = tagline,
                        summary = summary,
                        year = yearVal,
                        rating = ratingVal,
                        contentRating = contentRating,
                        duration = duration,
                        studio = studio,
                        genres = genreList.ifEmpty { listOf("Movie") },
                        coverUrl = coverUrl,
                        bannerUrl = bannerUrl,
                        videoUrl = videoUrl,
                        serverId = serverId,
                        cast = cast,
                        directors = directors,
                        writers = writers,
                        producers = producers,
                        cinematographers = cinematographers,
                        similarTitles = similarTitles
                    )
                )
            }
        }

        Result.success(movieList)
    }

    /**
     * Fetches TV Shows with granular seasons and episodes including episode biographies and episode cast/crew.
     */
    suspend fun fetchRichShows(
        serverUrl: String,
        token: String = "",
        serverId: String = "",
        candidateUrls: List<String> = emptyList()
    ): Result<List<PlexShowItem>> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        val allCandidates = (listOf(serverUrl) + candidateUrls)
            .map { normalizeUrl(it) }
            .filter { it.isNotBlank() }
            .distinct()

        if (allCandidates.isEmpty()) {
            return@withContext Result.failure(Exception("Server URL is empty."))
        }

        var workingRoot = ""
        var directoryList = emptyList<org.json.JSONObject>()

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
                    val parsedDirs = parseJsonArrayOrObjectList(secBody, "MediaContainer", "Directory")
                    workingRoot = root
                    directoryList = parsedDirs
                    if (parsedDirs.isNotEmpty()) break
                }
            } catch (_: Exception) {}
        }

        if (workingRoot.isBlank()) {
            return@withContext Result.failure(Exception("Could not connect to Plex server library."))
        }

        val showSections = directoryList.filter { dir ->
            val type = dir.optString("type", "").lowercase()
            type == "show"
        }.ifEmpty { directoryList }

        val showList = mutableListOf<PlexShowItem>()

        for (sec in showSections) {
            val key = sec.optString("key", "").removePrefix("/library/sections/").trim()
            if (key.isBlank()) continue

            val queryUrls = listOf(
                "$workingRoot/library/sections/$key/all?type=2&includeGuids=1&X-Plex-Token=$cleanToken",
                "$workingRoot/library/sections/$key/all?X-Plex-Token=$cleanToken"
            )

            var items: List<org.json.JSONObject> = emptyList()
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
                        val parsed = parseJsonArrayOrObjectList(body, "MediaContainer", "Metadata")
                        if (parsed.isNotEmpty()) {
                            items = parsed
                            break
                        }
                    }
                } catch (_: Exception) {}
            }

            for (showItem in items) {
                val showRatingKey = showItem.optString("ratingKey", showItem.optString("key", "").substringAfterLast("/"))
                if (showRatingKey.isBlank()) continue

                val showTitle = showItem.optString("title", "TV Show").ifBlank { "TV Show" }
                val originalTitle = showItem.optString("originalTitle", "")
                val summary = showItem.optString("summary", "")
                val yearVal = if (showItem.has("year")) showItem.optInt("year") else null
                val ratingVal = if (showItem.has("rating")) showItem.optDouble("rating").toFloat() else null
                val contentRating = showItem.optString("contentRating", "")
                val studio = showItem.optString("studio", "")

                val thumbPath = showItem.optString("thumb", "")
                val coverUrl = if (thumbPath.isNotBlank()) {
                    val cleanThumb = if (thumbPath.startsWith("/")) thumbPath else "/$thumbPath"
                    "$workingRoot$cleanThumb?X-Plex-Token=$cleanToken"
                } else ""

                val artPath = showItem.optString("art", "")
                val bannerUrl = if (artPath.isNotBlank()) {
                    val cleanArt = if (artPath.startsWith("/")) artPath else "/$artPath"
                    "$workingRoot$cleanArt?X-Plex-Token=$cleanToken"
                } else ""

                val genreList = optJsonList(showItem, "Genre").mapNotNull { it.optString("tag", "").takeIf { g -> g.isNotBlank() } }
                val showCast = parseCastList(showItem, "Role", "Actor", workingRoot, cleanToken)
                val showDirectors = parseCastList(showItem, "Director", "Director", workingRoot, cleanToken)
                val showProducers = parseCastList(showItem, "Producer", "Producer", workingRoot, cleanToken)
                val showWriters = parseCastList(showItem, "Writer", "Writer", workingRoot, cleanToken)

                // Fetch granular all leaves (episodes) for this show
                val episodesList = mutableListOf<PlexEpisodeItem>()
                val leavesUrl = "$workingRoot/library/metadata/$showRatingKey/allLeaves?X-Plex-Token=$cleanToken"
                try {
                    val epReq = Request.Builder()
                        .url(leavesUrl)
                        .get()
                        .apply { buildStandardHeaders(this, cleanToken) }
                        .build()
                    val epRes = client.newCall(epReq).execute()
                    if (epRes.isSuccessful) {
                        val epBody = epRes.body?.string() ?: ""
                        val epMetadata = parseJsonArrayOrObjectList(epBody, "MediaContainer", "Metadata")
                        for (ep in epMetadata) {
                            val epRatingKey = ep.optString("ratingKey", "")
                            if (epRatingKey.isBlank()) continue

                            val seasonNum = if (ep.has("parentIndex")) ep.optInt("parentIndex") else 1
                            val epNum = if (ep.has("index")) ep.optInt("index") else 1
                            val epTitle = ep.optString("title", "Episode $epNum")
                            val epSummary = ep.optString("summary", "")
                            val epDuration = ep.optLong("duration", 0L)
                            val epAirDate = ep.optString("originallyAvailableAt", "")

                            val epMediaList = optJsonList(ep, "Media")
                            val epFirstMedia = epMediaList.firstOrNull()
                            val epPartList = if (epFirstMedia != null) optJsonList(epFirstMedia, "Part") else emptyList()
                            val epPartKey = epPartList.firstOrNull()?.optString("key", "") ?: ""
                            val epVideoUrl = if (epPartKey.isNotBlank()) {
                                val clean = if (epPartKey.startsWith("/")) epPartKey else "/$epPartKey"
                                "$workingRoot$clean?X-Plex-Token=$cleanToken"
                            } else ""

                            val epThumb = ep.optString("thumb", "")
                            val epCoverUrl = if (epThumb.isNotBlank()) {
                                val clean = if (epThumb.startsWith("/")) epThumb else "/$epThumb"
                                "$workingRoot$clean?X-Plex-Token=$cleanToken"
                            } else coverUrl

                            val epDirectors = parseCastList(ep, "Director", "Director", workingRoot, cleanToken)
                            val epWriters = parseCastList(ep, "Writer", "Writer", workingRoot, cleanToken)
                            val epCast = parseCastList(ep, "Role", "Actor", workingRoot, cleanToken)
                            val epProducers = parseCastList(ep, "Producer", "Producer", workingRoot, cleanToken)

                            episodesList.add(
                                PlexEpisodeItem(
                                    id = "plex_ep_${serverId}_$epRatingKey",
                                    ratingKey = epRatingKey,
                                    showTitle = showTitle,
                                    seasonNumber = seasonNum,
                                    episodeNumber = epNum,
                                    title = epTitle,
                                    summary = epSummary,
                                    duration = epDuration,
                                    airDate = epAirDate,
                                    coverUrl = epCoverUrl,
                                    videoUrl = epVideoUrl,
                                    serverId = serverId,
                                    directors = epDirectors,
                                    writers = epWriters,
                                    cast = epCast,
                                    producers = epProducers
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}

                // Group episodes into seasons
                val seasonsGrouped = episodesList.groupBy { it.seasonNumber }
                val seasonsList = seasonsGrouped.map { (seasonNum, eps) ->
                    PlexSeasonItem(
                        id = "plex_season_${serverId}_${showRatingKey}_$seasonNum",
                        ratingKey = "${showRatingKey}_$seasonNum",
                        showTitle = showTitle,
                        seasonNumber = seasonNum,
                        title = if (seasonNum == 0) "Specials" else "Season $seasonNum",
                        summary = "Season $seasonNum featuring ${eps.size} episode(s).",
                        coverUrl = coverUrl,
                        episodeCount = eps.size,
                        episodes = eps.sortedBy { it.episodeNumber },
                        cast = showCast
                    )
                }.sortedBy { it.seasonNumber }

                showList.add(
                    PlexShowItem(
                        id = "plex_show_${serverId}_$showRatingKey",
                        ratingKey = showRatingKey,
                        title = showTitle,
                        originalTitle = originalTitle,
                        summary = summary,
                        year = yearVal,
                        rating = ratingVal,
                        contentRating = contentRating,
                        studio = studio,
                        genres = genreList.ifEmpty { listOf("TV Series") },
                        coverUrl = coverUrl,
                        bannerUrl = bannerUrl,
                        serverId = serverId,
                        seasons = seasonsList,
                        cast = showCast,
                        directors = showDirectors,
                        producers = showProducers,
                        writers = showWriters
                    )
                )
            }
        }

        Result.success(showList)
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
        val moviesRes = fetchRichMovies(serverUrl, token, serverId, candidateUrls)
        val showsRes = fetchRichShows(serverUrl, token, serverId, candidateUrls)

        val list = mutableListOf<PlexVideoItem>()

        moviesRes.getOrNull()?.forEach { movie ->
            list.add(
                PlexVideoItem(
                    id = movie.id,
                    title = movie.title,
                    type = "movie",
                    showTitle = "",
                    seasonEpisodeLabel = "",
                    summary = movie.summary,
                    year = movie.year,
                    duration = movie.duration,
                    coverUrl = movie.coverUrl,
                    bannerUrl = movie.bannerUrl,
                    videoUrl = movie.videoUrl,
                    ratingKey = movie.ratingKey,
                    genre = movie.genres.firstOrNull() ?: "Movie",
                    serverId = movie.serverId
                )
            )
        }

        showsRes.getOrNull()?.forEach { show ->
            show.seasons.forEach { season ->
                season.episodes.forEach { ep ->
                    list.add(
                        PlexVideoItem(
                            id = ep.id,
                            title = ep.title,
                            type = "episode",
                            showTitle = show.title,
                            seasonEpisodeLabel = "S${ep.seasonNumber}E${ep.episodeNumber}",
                            summary = ep.summary,
                            year = show.year,
                            duration = ep.duration,
                            coverUrl = ep.coverUrl,
                            bannerUrl = show.bannerUrl,
                            videoUrl = ep.videoUrl,
                            ratingKey = ep.ratingKey,
                            genre = show.genres.firstOrNull() ?: "TV Show",
                            serverId = show.serverId
                        )
                    )
                }
            }
        }

        Result.success(list)
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
