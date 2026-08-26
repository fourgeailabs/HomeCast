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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object PlexClient {
    private const val TAG = "PlexClient"
    const val CLIENT_ID = "HomeCast-Android-Client"
    private const val PRODUCT_NAME = "HomeCast"
    private const val VERSION = "1.8"

    private val client: OkHttpClient by lazy {
        OptimizedNetworkEngine.client
    }

    // Fast candidate probing client with 2.5s connection timeout for instant parallel server checks
    private val fastProbeClient: OkHttpClient by lazy {
        client.newBuilder()
            .connectTimeout(2500, TimeUnit.MILLISECONDS)
            .readTimeout(3500, TimeUnit.MILLISECONDS)
            .build()
    }

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Thread-safe cache of verified active working server roots: serverId -> workingRootUrl
    private val verifiedHostMap = ConcurrentHashMap<String, String>()

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
        builder.addHeader("X-Plex-Container-Start", "0")
        builder.addHeader("X-Plex-Container-Size", "10000")
        if (token.isNotBlank()) {
            builder.addHeader("X-Plex-Token", token.trim())
        }
    }

    fun optJsonList(parent: org.json.JSONObject, key: String): List<org.json.JSONObject> {
        if (!parent.has(key) || parent.isNull(key)) return emptyList()
        val list = mutableListOf<org.json.JSONObject>()
        val opt = parent.opt(key)
        if (opt is org.json.JSONArray) {
            for (i in 0 until opt.length()) {
                val elem = opt.optJSONObject(i)
                if (elem != null) list.add(elem)
            }
        } else if (opt is org.json.JSONObject) {
            list.add(opt)
        }
        return list
    }

    fun parseJsonArrayOrObjectList(raw: String, containerKey: String, listKey: String): List<org.json.JSONObject> {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return emptyList()

        if (trimmed.startsWith("<")) {
            return if (listKey.equals("Directory", ignoreCase = true) || listKey.equals("Server", ignoreCase = true)) {
                parsePlexXmlToDirectoryList(trimmed)
            } else {
                parsePlexXmlToMetadataList(trimmed)
            }
        }

        try {
            val root = org.json.JSONObject(trimmed)
            val container = if (root.has(containerKey)) root.optJSONObject(containerKey) else root
            if (container != null) {
                val direct = optJsonList(container, listKey)
                if (direct.isNotEmpty()) return direct

                val altKey = if (listKey.isNotEmpty()) listKey.take(1).lowercase() + listKey.substring(1) else listKey
                val altList = optJsonList(container, altKey)
                if (altList.isNotEmpty()) return altList

                // Check common alternative media list keys
                if (listKey.equals("Metadata", ignoreCase = true) || listKey.equals("Video", ignoreCase = true) || listKey.equals("Track", ignoreCase = true) || listKey.equals("Directory", ignoreCase = true)) {
                    val videoList = optJsonList(container, "Video").ifEmpty { optJsonList(container, "video") }
                    if (videoList.isNotEmpty()) return videoList
                    val dirList = optJsonList(container, "Directory").ifEmpty { optJsonList(container, "directory") }
                    if (dirList.isNotEmpty()) return dirList
                    val trackList = optJsonList(container, "Track").ifEmpty { optJsonList(container, "track") }
                    if (trackList.isNotEmpty()) return trackList
                    val metaList = optJsonList(container, "Metadata").ifEmpty { optJsonList(container, "metadata") }
                    if (metaList.isNotEmpty()) return metaList
                }
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
            var roleList = mutableListOf<org.json.JSONObject>()
            var directorList = mutableListOf<org.json.JSONObject>()
            var writerList = mutableListOf<org.json.JSONObject>()
            var producerList = mutableListOf<org.json.JSONObject>()
            var crewList = mutableListOf<org.json.JSONObject>()
            var similarList = mutableListOf<org.json.JSONObject>()
            var countryList = mutableListOf<org.json.JSONObject>()
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
                            roleList = mutableListOf()
                            directorList = mutableListOf()
                            writerList = mutableListOf()
                            producerList = mutableListOf()
                            crewList = mutableListOf()
                            similarList = mutableListOf()
                            countryList = mutableListOf()
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
                            val obj = org.json.JSONObject()
                            for (i in 0 until parser.attributeCount) obj.put(parser.getAttributeName(i), parser.getAttributeValue(i))
                            genreList.add(obj)
                        }
                        tagName.equals("Role", ignoreCase = true) -> {
                            val obj = org.json.JSONObject()
                            for (i in 0 until parser.attributeCount) obj.put(parser.getAttributeName(i), parser.getAttributeValue(i))
                            roleList.add(obj)
                        }
                        tagName.equals("Director", ignoreCase = true) -> {
                            val obj = org.json.JSONObject()
                            for (i in 0 until parser.attributeCount) obj.put(parser.getAttributeName(i), parser.getAttributeValue(i))
                            directorList.add(obj)
                        }
                        tagName.equals("Writer", ignoreCase = true) -> {
                            val obj = org.json.JSONObject()
                            for (i in 0 until parser.attributeCount) obj.put(parser.getAttributeName(i), parser.getAttributeValue(i))
                            writerList.add(obj)
                        }
                        tagName.equals("Producer", ignoreCase = true) -> {
                            val obj = org.json.JSONObject()
                            for (i in 0 until parser.attributeCount) obj.put(parser.getAttributeName(i), parser.getAttributeValue(i))
                            producerList.add(obj)
                        }
                        tagName.equals("Crew", ignoreCase = true) -> {
                            val obj = org.json.JSONObject()
                            for (i in 0 until parser.attributeCount) obj.put(parser.getAttributeName(i), parser.getAttributeValue(i))
                            crewList.add(obj)
                        }
                        tagName.equals("Similar", ignoreCase = true) -> {
                            val obj = org.json.JSONObject()
                            for (i in 0 until parser.attributeCount) obj.put(parser.getAttributeName(i), parser.getAttributeValue(i))
                            similarList.add(obj)
                        }
                        tagName.equals("Country", ignoreCase = true) -> {
                            val obj = org.json.JSONObject()
                            for (i in 0 until parser.attributeCount) obj.put(parser.getAttributeName(i), parser.getAttributeValue(i))
                            countryList.add(obj)
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
                                if (roleList.isNotEmpty()) currentItem.put("Role", org.json.JSONArray(roleList))
                                if (directorList.isNotEmpty()) currentItem.put("Director", org.json.JSONArray(directorList))
                                if (writerList.isNotEmpty()) currentItem.put("Writer", org.json.JSONArray(writerList))
                                if (producerList.isNotEmpty()) currentItem.put("Producer", org.json.JSONArray(producerList))
                                if (crewList.isNotEmpty()) currentItem.put("Crew", org.json.JSONArray(crewList))
                                if (similarList.isNotEmpty()) currentItem.put("Similar", org.json.JSONArray(similarList))
                                if (countryList.isNotEmpty()) currentItem.put("Country", org.json.JSONArray(countryList))
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

            if (token != null) {
                Result.success(token)
            } else {
                val adapter = moshi.adapter(PlexPinCheckResponse::class.java)
                val pin = adapter.fromJson(body)
                Result.success(pin?.authToken)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Authenticates directly with Plex credentials if needed.
     */
    suspend fun loginWithCredentials(usernameOrEmail: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("user[login]", usernameOrEmail.trim())
                .add("user[password]", password)
                .build()

            val request = Request.Builder()
                .url("https://plex.tv/users/sign_in.json")
                .post(formBody)
                .apply { buildStandardHeaders(this, "") }
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Plex Sign-In failed: HTTP ${response.code}"))
            }

            val json = org.json.JSONObject(body)
            val userObj = json.optJSONObject("user")
            val token = userObj?.optString("authToken", userObj.optString("authentication_token", "")) ?: ""
            if (token.isNotBlank()) {
                Result.success(token)
            } else {
                Result.failure(Exception("No authentication token in response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun diagnoseConnection(
        serverUrl: String,
        token: String = "",
        candidateUrls: List<String> = emptyList()
    ): PlexDiagnosticResult = runComprehensivePlexDiagnostics(serverUrl, token, candidateUrls)

    /**
     * Fast Probe across candidate URLs for a Plex Server.
     */
    private fun probeCandidate(rootUrl: String, token: String): Pair<String, List<org.json.JSONObject>> {
        val secReqUrl = "$rootUrl/library/sections?X-Plex-Token=$token"
        try {
            val req = Request.Builder()
                .url(secReqUrl)
                .get()
                .apply { buildStandardHeaders(this, token) }
                .build()

            val res = fastProbeClient.newCall(req).execute()
            if (res.isSuccessful) {
                val body = res.body?.string() ?: ""
                val parsedDirs = parseJsonArrayOrObjectList(body, "MediaContainer", "Directory")
                return Pair(rootUrl, parsedDirs)
            }
        } catch (_: Throwable) {}
        return Pair("", emptyList())
    }

    /**
     * High-speed parallel resolver that finds the active working root URL for a Plex server in milliseconds.
     */
    suspend fun resolveWorkingPlexUrl(
        serverUrl: String,
        token: String,
        serverId: String = "",
        candidateUrls: List<String> = emptyList()
    ): Pair<String, List<org.json.JSONObject>> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        val allCandidates = (listOf(serverUrl) + candidateUrls)
            .map { normalizeUrl(it) }
            .filter { it.isNotBlank() }
            .distinct()

        if (allCandidates.isEmpty()) {
            return@withContext Pair("", emptyList())
        }

        // 1. Fast cache hit check
        if (serverId.isNotBlank()) {
            val cachedHost = verifiedHostMap[serverId]
            if (!cachedHost.isNullOrBlank()) {
                val probe = probeCandidate(cachedHost, cleanToken)
                if (probe.first.isNotBlank()) {
                    return@withContext probe
                }
            }
        }

        // 2. Parallel probing across all candidate endpoints with fast racing
        val probeDeferreds = coroutineScope {
            allCandidates.map { candidate ->
                async {
                    probeCandidate(candidate, cleanToken)
                }
            }
        }

        val results = probeDeferreds.awaitAll()
        val successful = results.firstOrNull { it.first.isNotBlank() }

        if (successful != null) {
            if (serverId.isNotBlank()) {
                verifiedHostMap[serverId] = successful.first
            }
            return@withContext successful
        }

        // 3. Fallback to Plex account servers lookup if direct candidates failed
        if (cleanToken.isNotBlank()) {
            try {
                val serversRes = fetchAccountServers(cleanToken)
                val servers = serversRes.getOrNull() ?: emptyList()
                val target = servers.firstOrNull { it.clientIdentifier == serverId } ?: servers.firstOrNull()
                if (target != null && target.candidateUris.isNotEmpty()) {
                    val cloudCandidates = target.candidateUris.map { normalizeUrl(it) }.filter { it.isNotBlank() }.distinct()
                    val cloudResults = coroutineScope {
                        cloudCandidates.map { c ->
                            async { probeCandidate(c, cleanToken) }
                        }.awaitAll()
                    }
                    val cloudSuccess = cloudResults.firstOrNull { it.first.isNotBlank() }
                    if (cloudSuccess != null) {
                        if (serverId.isNotBlank()) {
                            verifiedHostMap[serverId] = cloudSuccess.first
                        }
                        return@withContext cloudSuccess
                    }
                }
            } catch (_: Throwable) {}
        }

        Pair("", emptyList())
    }

    /**
     * Fetches all Plex Media Servers associated with the user's account token from plex.tv/api/v2/resources.
     */
    suspend fun fetchAccountServers(token: String): Result<List<PlexServerResource>> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        val endpoints = listOf(
            "https://plex.tv/api/v2/resources?includeHttps=1&includeRelay=1&X-Plex-Token=$cleanToken",
            "https://plex.tv/pms/resources?includeHttps=1&includeRelay=1&X-Plex-Token=$cleanToken"
        )

        for (endpoint in endpoints) {
            val request = Request.Builder()
                .url(endpoint)
                .get()
                .apply { buildStandardHeaders(this, cleanToken) }
                .build()

            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                if (response.isSuccessful && body.isNotBlank()) {
                    val rawDevices = parseJsonArrayOrObjectList(body, "MediaContainer", "Device")
                        .ifEmpty { parseJsonArrayOrObjectList(body, "MediaContainer", "Server") }

                    val servers = mutableListOf<PlexServerResource>()
                    for (dev in rawDevices) {
                        val provides = dev.optString("provides", "")
                        if (!provides.contains("server", ignoreCase = true) && provides.isNotBlank()) continue

                        val name = dev.optString("name", "Plex Media Server")
                        val clientIdentifier = dev.optString("clientIdentifier", dev.optString("machineIdentifier", dev.optString("accessToken", "")))
                        val accessToken = dev.optString("accessToken", cleanToken).ifBlank { cleanToken }
                        val owned = dev.optBoolean("owned", true)

                        val connections = optJsonList(dev, "connections").ifEmpty { optJsonList(dev, "Connection") }
                        val parsedConns = mutableListOf<PlexConnection>()
                        val candidateUris = mutableListOf<String>()

                        for (conn in connections) {
                            val uri = conn.optString("uri", "")
                            val address = conn.optString("address", "")
                            val port = conn.optInt("port", 32400)
                            val protocol = conn.optString("protocol", if (uri.startsWith("https")) "https" else "http")
                            val local = conn.optBoolean("local", false)
                            val relay = conn.optBoolean("relay", false)

                            if (uri.isNotBlank()) candidateUris.add(uri)
                            if (address.isNotBlank()) {
                                candidateUris.add("$protocol://$address:$port")
                                if (protocol == "https") candidateUris.add("http://$address:$port")
                            }

                            parsedConns.add(
                                PlexConnection(
                                    uri = if (uri.isNotBlank()) uri else "$protocol://$address:$port",
                                    address = address,
                                    port = port,
                                    protocol = protocol,
                                    local = local,
                                    relay = relay
                                )
                            )
                        }

                        if (candidateUris.isNotEmpty()) {
                            servers.add(
                                PlexServerResource(
                                    name = name,
                                    clientIdentifier = clientIdentifier,
                                    accessToken = accessToken,
                                    owned = owned,
                                    connections = parsedConns,
                                    candidateUris = candidateUris.distinct()
                                )
                            )
                        }
                    }

                    if (servers.isNotEmpty()) {
                        return@withContext Result.success(servers)
                    }
                }
            } catch (_: Exception) {}
        }

        Result.failure(Exception("No reachable Plex servers found in account resources."))
    }

    suspend fun fetchPlexServers(token: String): Result<List<PlexServerResource>> = fetchAccountServers(token)

    /**
     * Fetches sections from a server URL.
     */
    suspend fun fetchSections(
        serverUrl: String,
        token: String = "",
        serverId: String = "",
        candidateUrls: List<String> = emptyList()
    ): Result<List<PlexSection>> = withContext(Dispatchers.IO) {
        val (workingRoot, directoryList) = resolveWorkingPlexUrl(serverUrl, token, serverId, candidateUrls)
        if (workingRoot.isBlank()) {
            return@withContext Result.failure(Exception("Could not connect to Plex server at $serverUrl"))
        }

        val sections = directoryList.mapNotNull { dir ->
            val key = dir.optString("key", "").removePrefix("/library/sections/").trim()
            val title = dir.optString("title", "")
            val type = dir.optString("type", "")
            if (key.isNotBlank() && title.isNotBlank()) {
                PlexSection(key = key, title = title, type = type)
            } else null
        }

        Result.success(sections)
    }

    suspend fun testServerConnection(
        serverUrl: String,
        token: String = "",
        serverId: String = "",
        candidateUrls: List<String> = emptyList()
    ): Boolean = withContext(Dispatchers.IO) {
        val (workingRoot, _) = resolveWorkingPlexUrl(serverUrl, token, serverId, candidateUrls)
        workingRoot.isNotBlank()
    }

    /**
     * Parses cast and crew with roles, characters, thumbnails, and IDs.
     */
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
            val thumbPath = obj.optString("thumb", obj.optString("photo", obj.optString("picture", "")))
            val thumbUrl = if (thumbPath.isNotBlank()) {
                if (thumbPath.startsWith("http://") || thumbPath.startsWith("https://")) {
                    thumbPath
                } else {
                    val clean = if (thumbPath.startsWith("/")) thumbPath else "/$thumbPath"
                    "$workingRoot$clean?X-Plex-Token=$cleanToken"
                }
            } else ""
            val personId = obj.optString("id", "")
            val role = if (defaultRole.equals("Actor", ignoreCase = true) || defaultRole.equals("Role", ignoreCase = true)) {
                "Actor"
            } else {
                defaultRole
            }
            PlexCastMember(
                id = personId,
                name = name,
                role = role,
                character = character,
                thumbUrl = thumbUrl
            )
        }
    }

    /**
     * Blazing fast parallel fetch of movies with full cast, directors, writers, producers, and summaries.
     */
    suspend fun fetchRichMovies(
        serverUrl: String,
        token: String = "",
        serverId: String = "",
        candidateUrls: List<String> = emptyList()
    ): Result<List<PlexMovieItem>> = withContext(Dispatchers.IO) {
        try {
            val cleanToken = token.trim()
            val (workingRoot, directoryList) = resolveWorkingPlexUrl(serverUrl, cleanToken, serverId, candidateUrls)

            if (workingRoot.isBlank()) {
                return@withContext Result.failure(Exception("Could not connect to Plex server library."))
            }

            val movieSections = directoryList.filter { dir ->
                val type = dir.optString("type", "").lowercase()
                val title = dir.optString("title", "").lowercase()
                type == "movie" || type == "film" || type == "video" || type == "home_video" ||
                    title.contains("movie") || title.contains("film") || title.contains("cinema")
            }.ifEmpty {
                directoryList.filter { dir ->
                    val type = dir.optString("type", "").lowercase()
                    type != "artist" && type != "music" && type != "audio" && type != "photo" && type != "show"
                }
            }.ifEmpty { directoryList }

            // Concurrent execution for all movie sections!
            val sectionResults = coroutineScope {
                movieSections.map { sec ->
                    async {
                        val key = sec.optString("key", "").removePrefix("/library/sections/").trim()
                        if (key.isBlank()) return@async emptyList<PlexMovieItem>()

                        val items = fetchSectionItems(workingRoot, key, "type=1", cleanToken)
                        val secMovies = mutableListOf<PlexMovieItem>()

                        for (item in items) {
                            val ratingKey = item.optString("ratingKey", item.optString("key", "").substringAfterLast("/"))
                            if (ratingKey.isBlank()) continue

                            val itemType = item.optString("type", "").lowercase()
                            if (itemType == "show" || itemType == "artist" || itemType == "track") continue

                            val mediaList = optJsonList(item, "Media")
                            val firstMedia = mediaList.firstOrNull()
                            val partList = if (firstMedia != null) optJsonList(firstMedia, "Part") else emptyList()
                            val partKey = partList.firstOrNull()?.optString("key", "") ?: ""

                            val videoUrl = if (partKey.isNotBlank()) {
                                val cleanPart = if (partKey.startsWith("/")) partKey else "/$partKey"
                                "$workingRoot$cleanPart?X-Plex-Token=$cleanToken"
                            } else {
                                "$workingRoot/video/:/transcode/universal/start.m3u8?path=/library/metadata/$ratingKey&mediaIndex=0&partIndex=0&protocol=hls&fastSeek=1&directPlay=1&directStream=1&X-Plex-Token=$cleanToken"
                            }

                            val thumbPath = item.optString("thumb", item.optString("parentThumb", ""))
                            val coverUrl = if (thumbPath.isNotBlank()) {
                                val cleanThumb = if (thumbPath.startsWith("/")) thumbPath else "/$thumbPath"
                                "$workingRoot$cleanThumb?X-Plex-Token=$cleanToken"
                            } else ""

                            val artPath = item.optString("art", item.optString("parentArt", ""))
                            val bannerUrl = if (artPath.isNotBlank()) {
                                val cleanArt = if (artPath.startsWith("/")) artPath else "/$artPath"
                                "$workingRoot$cleanArt?X-Plex-Token=$cleanToken"
                            } else coverUrl

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
                                .ifEmpty { parseCastList(item, "Actor", "Actor", workingRoot, cleanToken) }
                                .distinctBy { "${it.name}_${it.character}" }
                            val directors = parseCastList(item, "Director", "Director", workingRoot, cleanToken)
                                .distinctBy { "${it.name}_${it.character}" }
                            val writers = parseCastList(item, "Writer", "Writer", workingRoot, cleanToken)
                                .distinctBy { "${it.name}_${it.character}" }
                            val producers = parseCastList(item, "Producer", "Producer", workingRoot, cleanToken)
                                .distinctBy { "${it.name}_${it.character}" }
                            val cinematographers = parseCastList(item, "Crew", "Cinematographer", workingRoot, cleanToken)
                                .distinctBy { "${it.name}_${it.character}" }

                            val similarTitles = optJsonList(item, "Similar").mapNotNull { it.optString("tag", "").takeIf { s -> s.isNotBlank() } }
                            val addedAtVal = item.optLong("addedAt", 0L)

                            secMovies.add(
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
                                    similarTitles = similarTitles,
                                    addedAt = addedAtVal
                                )
                            )
                        }
                        secMovies
                    }
                }.awaitAll()
            }

            val allMovies = sectionResults.flatten().distinctBy { it.id }
            Result.success(allMovies)
        } catch (t: Throwable) {
            Log.e(TAG, "fetchRichMovies error: ${t.message}", t)
            Result.failure(Exception(t.message ?: "Failed to fetch rich movies", t))
        }
    }

    /**
     * Blazing fast parallel fetch of TV shows with granular seasons, episodes, cast, directors, and writers.
     */
    suspend fun fetchRichShows(
        serverUrl: String,
        token: String = "",
        serverId: String = "",
        candidateUrls: List<String> = emptyList()
    ): Result<List<PlexShowItem>> = withContext(Dispatchers.IO) {
        try {
            val cleanToken = token.trim()
            val (workingRoot, directoryList) = resolveWorkingPlexUrl(serverUrl, cleanToken, serverId, candidateUrls)

            if (workingRoot.isBlank()) {
                return@withContext Result.failure(Exception("Could not connect to Plex server library."))
            }

            val showSections = directoryList.filter { dir ->
                val type = dir.optString("type", "").lowercase()
                val title = dir.optString("title", "").lowercase()
                type == "show" || type == "tv" || type == "series" || type == "episode" ||
                    title.contains("show") || title.contains("tv") || title.contains("series") || title.contains("season") || title.contains("anime")
            }.ifEmpty {
                directoryList.filter { dir ->
                    val type = dir.optString("type", "").lowercase()
                    type != "artist" && type != "music" && type != "audio" && type != "photo" && type != "movie"
                }
            }.ifEmpty { directoryList }

            val sectionResults = coroutineScope {
                showSections.map { sec ->
                    async {
                        val key = sec.optString("key", "").removePrefix("/library/sections/").trim()
                        if (key.isBlank()) return@async emptyList<PlexShowItem>()

                        // Concurrently fetch show items & section episodes!
                        val (showItems, sectionEpisodesMap) = coroutineScope {
                            val showsDeferred = async { fetchSectionItems(workingRoot, key, "type=2", cleanToken) }
                            val episodesDeferred = async { fetchSectionEpisodesMap(workingRoot, key, cleanToken, serverId) }
                            Pair(showsDeferred.await(), episodesDeferred.await())
                        }

                        val secShows = mutableListOf<PlexShowItem>()

                        for (showItem in showItems) {
                            val showRatingKey = showItem.optString("ratingKey", showItem.optString("key", "").substringAfterLast("/"))
                            if (showRatingKey.isBlank()) continue

                            val itemType = showItem.optString("type", "").lowercase()
                            if (itemType == "movie" || itemType == "artist" || itemType == "track") continue

                            val showTitle = showItem.optString("title", "TV Show").ifBlank { "TV Show" }
                            val originalTitle = showItem.optString("originalTitle", "")
                            val summary = showItem.optString("summary", "")
                            val yearVal = if (showItem.has("year")) showItem.optInt("year") else null
                            val ratingVal = if (showItem.has("rating")) showItem.optDouble("rating").toFloat() else null
                            val contentRating = showItem.optString("contentRating", "")
                            val studio = showItem.optString("studio", "")
                            val addedAtVal = showItem.optLong("addedAt", 0L)
                            val rawLeafCount = showItem.optInt("leafCount", 0)
                            val rawChildCount = showItem.optInt("childCount", 0)

                            val thumbPath = showItem.optString("thumb", "")
                            val coverUrl = if (thumbPath.isNotBlank()) {
                                val cleanThumb = if (thumbPath.startsWith("/")) thumbPath else "/$thumbPath"
                                "$workingRoot$cleanThumb?X-Plex-Token=$cleanToken"
                            } else ""

                            val artPath = showItem.optString("art", "")
                            val bannerUrl = if (artPath.isNotBlank()) {
                                val cleanArt = if (artPath.startsWith("/")) artPath else "/$artPath"
                                "$workingRoot$cleanArt?X-Plex-Token=$cleanToken"
                            } else coverUrl

                            val genreList = optJsonList(showItem, "Genre").mapNotNull { it.optString("tag", "").takeIf { g -> g.isNotBlank() } }
                            val showCast = parseCastList(showItem, "Role", "Actor", workingRoot, cleanToken)
                                .ifEmpty { parseCastList(showItem, "Actor", "Actor", workingRoot, cleanToken) }
                                .distinctBy { "${it.name}_${it.character}" }
                            val showDirectors = parseCastList(showItem, "Director", "Director", workingRoot, cleanToken)
                                .distinctBy { "${it.name}_${it.character}" }
                            val showProducers = parseCastList(showItem, "Producer", "Producer", workingRoot, cleanToken)
                                .distinctBy { "${it.name}_${it.character}" }
                            val showWriters = parseCastList(showItem, "Writer", "Writer", workingRoot, cleanToken)
                                .distinctBy { "${it.name}_${it.character}" }
                            val showCrew = parseCastList(showItem, "Crew", "Crew", workingRoot, cleanToken)
                                .distinctBy { "${it.name}_${it.character}" }

                            // Match pre-fetched bulk episodes
                            val matchedEps = (sectionEpisodesMap[showRatingKey] ?: sectionEpisodesMap[showTitle] ?: emptyList()).distinctBy { it.ratingKey }
                            val episodesList = matchedEps.toMutableList()

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
                                    episodes = eps.sortedBy { it.episodeNumber }.distinctBy { it.id },
                                    cast = showCast
                                )
                            }.sortedBy { it.seasonNumber }.distinctBy { it.id }

                            val finalLeafCount = if (rawLeafCount > 0) rawLeafCount else if (episodesList.isNotEmpty()) episodesList.size else 0
                            val finalChildCount = if (rawChildCount > 0) rawChildCount else if (seasonsList.isNotEmpty()) seasonsList.size else 1

                            secShows.add(
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
                                    writers = showWriters,
                                    leafCount = finalLeafCount,
                                    childCount = finalChildCount,
                                    addedAt = addedAtVal
                                )
                            )
                        }
                        secShows
                    }
                }.awaitAll()
            }

            val allShows = sectionResults.flatten().distinctBy { it.id }
            Result.success(allShows)
        } catch (t: Throwable) {
            Log.e(TAG, "fetchRichShows error: ${t.message}", t)
            Result.failure(Exception(t.message ?: "Failed to fetch rich shows", t))
        }
    }

    /**
     * Efficiently fetches items for a section using standard single URL query with fast fallback.
     */
    private fun fetchSectionItems(
        workingRoot: String,
        key: String,
        typeParam: String,
        cleanToken: String
    ): List<org.json.JSONObject> {
        val queryUrls = listOf(
            "$workingRoot/library/sections/$key/all?$typeParam&includeGuids=1&X-Plex-Token=$cleanToken",
            "$workingRoot/library/sections/$key/all?$typeParam&X-Plex-Token=$cleanToken",
            "$workingRoot/library/sections/$key/all?X-Plex-Token=$cleanToken"
        )

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
                        .ifEmpty { parseJsonArrayOrObjectList(body, "MediaContainer", "Directory") }
                        .ifEmpty { parseJsonArrayOrObjectList(body, "MediaContainer", "Video") }
                        .ifEmpty { parseJsonArrayOrObjectList(body, "MediaContainer", "Show") }
                    if (parsed.isNotEmpty()) {
                        return parsed
                    }
                }
            } catch (_: Throwable) {}
        }
        return emptyList()
    }

    /**
     * Fetches bulk episodes for a TV section and groups them in memory.
     */
    private fun fetchSectionEpisodesMap(
        workingRoot: String,
        key: String,
        cleanToken: String,
        serverId: String
    ): Map<String, List<PlexEpisodeItem>> {
        val map = mutableMapOf<String, MutableList<PlexEpisodeItem>>()
        val bulkEpisodeUrls = listOf(
            "$workingRoot/library/sections/$key/all?type=4&X-Plex-Token=$cleanToken",
            "$workingRoot/library/sections/$key/allLeaves?X-Plex-Token=$cleanToken"
        )

        for (epUrl in bulkEpisodeUrls) {
            try {
                val epReq = Request.Builder()
                    .url(epUrl)
                    .get()
                    .apply { buildStandardHeaders(this, cleanToken) }
                    .build()
                val epRes = client.newCall(epReq).execute()
                if (epRes.isSuccessful) {
                    val epBody = epRes.body?.string() ?: ""
                    val epMetadata = parseJsonArrayOrObjectList(epBody, "MediaContainer", "Metadata")
                        .ifEmpty { parseJsonArrayOrObjectList(epBody, "MediaContainer", "Video") }

                    for (ep in epMetadata) {
                        val epRatingKey = ep.optString("ratingKey", "")
                        if (epRatingKey.isBlank()) continue

                        val grandParentKey = ep.optString("grandparentRatingKey", "")
                        val grandParentTitle = ep.optString("grandparentTitle", "")
                        val grandParentPathKey = ep.optString("grandparentKey", "").substringAfterLast("/")
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
                        } else {
                            "$workingRoot/video/:/transcode/universal/start.m3u8?path=/library/metadata/$epRatingKey&mediaIndex=0&partIndex=0&protocol=hls&fastSeek=1&directPlay=1&directStream=1&X-Plex-Token=$cleanToken"
                        }

                        val epThumb = ep.optString("thumb", "")
                        val epCoverUrl = if (epThumb.isNotBlank()) {
                            val clean = if (epThumb.startsWith("/")) epThumb else "/$epThumb"
                            "$workingRoot$clean?X-Plex-Token=$cleanToken"
                        } else ""

                        val epDirectors = parseCastList(ep, "Director", "Director", workingRoot, cleanToken)
                            .distinctBy { "${it.name}_${it.character}" }
                        val epWriters = parseCastList(ep, "Writer", "Writer", workingRoot, cleanToken)
                            .distinctBy { "${it.name}_${it.character}" }
                        val epCast = parseCastList(ep, "Role", "Actor", workingRoot, cleanToken)
                            .distinctBy { "${it.name}_${it.character}" }
                        val epProducers = parseCastList(ep, "Producer", "Producer", workingRoot, cleanToken)
                            .distinctBy { "${it.name}_${it.character}" }

                        val epItem = PlexEpisodeItem(
                            id = "plex_ep_${serverId}_$epRatingKey",
                            ratingKey = epRatingKey,
                            showTitle = ep.optString("grandparentTitle", ""),
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

                        if (grandParentKey.isNotBlank()) {
                            map.getOrPut(grandParentKey) { mutableListOf() }.add(epItem)
                        }
                        if (grandParentTitle.isNotBlank()) {
                            map.getOrPut(grandParentTitle) { mutableListOf() }.add(epItem)
                        }
                        if (grandParentPathKey.isNotBlank() && grandParentPathKey != grandParentKey) {
                            map.getOrPut(grandParentPathKey) { mutableListOf() }.add(epItem)
                        }
                    }
                    if (map.isNotEmpty()) break
                }
            } catch (_: Throwable) {}
        }
        return map
    }

    /**
     * Blazing fast parallel fetch of music tracks.
     */
    suspend fun fetchMusicTracks(
        serverUrl: String,
        token: String = "",
        serverId: String = "",
        candidateUrls: List<String> = emptyList()
    ): Result<List<MusicTrack>> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        val (workingRoot, directoryList) = resolveWorkingPlexUrl(serverUrl, cleanToken, serverId, candidateUrls)

        if (workingRoot.isBlank()) {
            return@withContext Result.failure(Exception("Could not connect to Plex server."))
        }

        val musicSections = directoryList.filter { dir ->
            val type = dir.optString("type", "").lowercase()
            type == "artist" || type == "music" || type == "audio"
        }.ifEmpty { directoryList }

        val tracksResults = coroutineScope {
            musicSections.map { sec ->
                async {
                    val key = sec.optString("key", "").removePrefix("/library/sections/").trim()
                    val secTitle = sec.optString("title", "Music")
                    if (key.isBlank()) return@async emptyList<MusicTrack>()

                    val items = fetchSectionItems(workingRoot, key, "type=10", cleanToken)
                    val list = mutableListOf<MusicTrack>()

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
                        val trackIndex = if (item.has("index")) item.optInt("index") else (list.size + 1)
                        val trackTitle = item.optString("title", "Track $trackIndex")
                        val artistName = item.optString("grandparentTitle", item.optString("originalTitle", item.optString("parentTitle", secTitle))).ifBlank { "Plex Artist" }
                        val albumName = item.optString("parentTitle", secTitle).ifBlank { "Plex Album" }
                        val duration = item.optLong("duration", 0L)

                        list.add(
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
                    list
                }
            }.awaitAll()
        }

        Result.success(tracksResults.flatten().distinctBy { it.id })
    }

    /**
     * Blazing fast parallel fetch of audiobooks.
     */
    suspend fun fetchAudiobooks(
        serverUrl: String,
        token: String = "",
        serverId: String = "",
        candidateUrls: List<String> = emptyList()
    ): Result<List<Audiobook>> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        val (workingRoot, directoryList) = resolveWorkingPlexUrl(serverUrl, cleanToken, serverId, candidateUrls)

        if (workingRoot.isBlank()) {
            return@withContext Result.failure(Exception("Could not connect to Plex server."))
        }

        val audiobookSections = directoryList.filter { dir ->
            val title = dir.optString("title", "").lowercase()
            title.contains("audiobook") || title.contains("book") || title.contains("spoken")
        }.ifEmpty {
            directoryList.filter { dir ->
                val type = dir.optString("type", "").lowercase()
                type == "artist" || type == "music" || type == "audio"
            }
        }

        val bookResults = coroutineScope {
            audiobookSections.map { sec ->
                async {
                    val key = sec.optString("key", "").removePrefix("/library/sections/").trim()
                    if (key.isBlank()) return@async emptyList<Audiobook>()

                    val items = fetchSectionItems(workingRoot, key, "type=9", cleanToken) // type 9 = albums
                        .ifEmpty { fetchSectionItems(workingRoot, key, "type=10", cleanToken) }

                    val list = mutableListOf<Audiobook>()

                    for (item in items) {
                        val ratingKey = item.optString("ratingKey", item.optString("key", "").substringAfterLast("/"))
                        if (ratingKey.isBlank()) continue

                        val title = item.optString("title", "Audiobook").ifBlank { "Audiobook" }
                        val author = item.optString("parentTitle", item.optString("originalTitle", "Plex Author")).ifBlank { "Plex Author" }
                        val summary = item.optString("summary", "")

                        val thumbPath = item.optString("thumb", item.optString("parentThumb", ""))
                        val coverUrl = if (thumbPath.isNotBlank()) {
                            val cleanThumb = if (thumbPath.startsWith("/")) thumbPath else "/$thumbPath"
                            "$workingRoot$cleanThumb?X-Plex-Token=$cleanToken"
                        } else ""

                        val duration = item.optLong("duration", 0L)

                        list.add(
                            Audiobook(
                                id = "plex_book_${serverId}_$ratingKey",
                                title = title,
                                author = author,
                                duration = duration,
                                coverUrl = coverUrl,
                                serverId = serverId,
                                streamUrl = "$workingRoot/library/metadata/$ratingKey?X-Plex-Token=$cleanToken"
                            )
                        )
                    }
                    list
                }
            }.awaitAll()
        }

        Result.success(bookResults.flatten().distinctBy { it.id })
    }

    suspend fun findFastWorkingUri(candidateUrls: List<String>, token: String): String {
        if (candidateUrls.isEmpty()) return ""
        val (workingRoot, _) = resolveWorkingPlexUrl(candidateUrls.first(), token, candidateUrls = candidateUrls)
        return workingRoot
    }

    /**
     * Runs comprehensive diagnostic tests on the Plex server and candidate paths.
     */
    suspend fun runComprehensivePlexDiagnostics(
        serverUrl: String,
        token: String = "",
        candidateUrls: List<String> = emptyList()
    ): PlexDiagnosticResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val normalizedUrl = normalizeUrl(serverUrl)
        val cleanToken = token.trim()
        val logs = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        var isReachable = false
        var httpStatusCode = 0
        var musicSectionsCount = 0
        var tracksCount = 0

        logs.add("1. Probing primary candidate URL '$normalizedUrl'...")
        val (workingRoot, directoryList) = resolveWorkingPlexUrl(normalizedUrl, cleanToken, candidateUrls = candidateUrls)

        if (workingRoot.isNotBlank()) {
            isReachable = true
            httpStatusCode = 200
            logs.add("   -> Connection established to verified host '$workingRoot' with HTTP 200.")
            val musicSections = directoryList.filter { dir ->
                val type = dir.optString("type", "").lowercase()
                type == "artist" || type == "music" || type == "audio"
            }
            musicSectionsCount = musicSections.size
            logs.add("   -> Discovered ${directoryList.size} libraries total, $musicSectionsCount music/audio libraries.")
        } else {
            logs.add("   -> Could not connect to any candidate endpoint.")
            recommendations.add("Check that your Plex Media Server is active on your local network or reachable via Remote Access.")
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

data class PlexPinCheckResponse(
    val authToken: String? = null
)

data class PlexServerResource(
    val name: String,
    val clientIdentifier: String,
    val accessToken: String,
    val owned: Boolean = true,
    val connections: List<PlexConnection> = emptyList(),
    val candidateUris: List<String> = emptyList()
) {
    val preferredUri: String
        get() = candidateUris.firstOrNull { !it.contains("plex.direct") && !it.contains("relay") }
            ?: candidateUris.firstOrNull() ?: ""
    val token: String
        get() = accessToken
}

data class PlexSection(
    val key: String,
    val title: String,
    val type: String
)

