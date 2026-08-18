package com.example.data.network

import com.example.data.Audiobook
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object AudiobookshelfClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private fun normalizeUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        return clean.trimEnd('/')
    }

    suspend fun login(baseUrl: String, username: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        val root = normalizeUrl(baseUrl)
        val cleanUsername = username.trim()

        val requestAdapter = moshi.adapter(AbsLoginRequest::class.java)
        val jsonPayload = requestAdapter.toJson(AbsLoginRequest(cleanUsername, password))
        val body = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())

        // Try standard /login endpoint first, with fallback to /api/login if /login returns 404
        val endpoints = listOf("$root/login", "$root/api/login")
        var lastError: Exception? = null

        for (loginUrl in endpoints) {
            val request = Request.Builder()
                .url(loginUrl)
                .post(body)
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.code == 401) {
                    var detailedMsg = "Invalid username or password"
                    try {
                        val parsed = moshi.adapter(AbsLoginResponse::class.java).fromJson(responseBody)
                        if (!parsed?.error.isNullOrBlank()) {
                            detailedMsg = parsed?.error ?: detailedMsg
                        }
                    } catch (_: Exception) {}
                    return@withContext Result.failure(
                        Exception("HTTP 401 Unauthorized: $detailedMsg. Please verify your credentials or use an API Token from Audiobookshelf (Settings > Users > API Keys).")
                    )
                }

                if (response.code == 404 && loginUrl == endpoints.first()) {
                    // Try next endpoint
                    continue
                }

                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("HTTP ${response.code}: ${responseBody.ifBlank { response.message }}")
                    )
                }

                val adapter = moshi.adapter(AbsLoginResponse::class.java)
                val parsed = adapter.fromJson(responseBody)
                val token = parsed?.user?.token ?: parsed?.token

                if (!token.isNullOrBlank()) {
                    return@withContext Result.success(token)
                } else {
                    return@withContext Result.failure(Exception("No token returned by server in login response."))
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        Result.failure(lastError ?: Exception("Failed to reach Audiobookshelf login endpoint at $root"))
    }

    suspend fun testConnection(baseUrl: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val root = normalizeUrl(baseUrl)
        val testUrl = "$root/api/libraries"

        val request = Request.Builder()
            .url(testUrl)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Server returned status ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAudiobooks(baseUrl: String, token: String, serverId: String): Result<List<Audiobook>> = withContext(Dispatchers.IO) {
        val root = normalizeUrl(baseUrl)
        try {
            // 1. Get Libraries
            val libReq = Request.Builder()
                .url("$root/api/libraries")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            val libRes = client.newCall(libReq).execute()
            if (!libRes.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch libraries: HTTP ${libRes.code}"))
            }

            val libAdapter = moshi.adapter(AbsLibrariesResponse::class.java)
            val libraries = libAdapter.fromJson(libRes.body?.string() ?: "")?.libraries ?: emptyList()
            val bookLibraries = libraries.filter { it.mediaType == null || it.mediaType == "book" }

            val allAudiobooks = mutableListOf<Audiobook>()

            // 2. Fetch items for each book library
            for (lib in bookLibraries) {
                val itemsReq = Request.Builder()
                    .url("$root/api/libraries/${lib.id}/items")
                    .addHeader("Authorization", "Bearer $token")
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
                            "$root/api/items/${item.id}/cover?token=$token"
                        } else {
                            ""
                        }

                        // Stream url for Audiobookshelf item
                        val streamUrl = "$root/api/items/${item.id}/play?token=$token"

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
}
