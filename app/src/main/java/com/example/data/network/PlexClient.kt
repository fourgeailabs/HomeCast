package com.example.data.network

import com.example.data.MusicTrack
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object PlexClient {
    private const val CLIENT_ID = "HomeCast-Android-Client"
    private const val PRODUCT_NAME = "HomeCast"

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

    suspend fun createPin(): Result<PlexPinResponse> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://plex.tv/api/v2/pins?strong=true")
            .post("".toRequestBody())
            .addHeader("Accept", "application/json")
            .addHeader("X-Plex-Product", PRODUCT_NAME)
            .addHeader("X-Plex-Client-Identifier", CLIENT_ID)
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

    suspend fun checkPin(pinId: Long): Result<String?> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://plex.tv/api/v2/pins/$pinId")
            .get()
            .addHeader("Accept", "application/json")
            .addHeader("X-Plex-Product", PRODUCT_NAME)
            .addHeader("X-Plex-Client-Identifier", CLIENT_ID)
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

    suspend fun testConnection(serverUrl: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val root = normalizeUrl(serverUrl)
        val request = Request.Builder()
            .url("$root/library/sections")
            .get()
            .addHeader("Accept", "application/json")
            .addHeader("X-Plex-Token", token)
            .addHeader("X-Plex-Client-Identifier", CLIENT_ID)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Plex returned HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchMusicTracks(serverUrl: String, token: String, serverId: String): Result<List<MusicTrack>> = withContext(Dispatchers.IO) {
        val root = normalizeUrl(serverUrl)
        try {
            // 1. Get Library sections
            val secReq = Request.Builder()
                .url("$root/library/sections")
                .get()
                .addHeader("Accept", "application/json")
                .addHeader("X-Plex-Token", token)
                .addHeader("X-Plex-Client-Identifier", CLIENT_ID)
                .build()

            val secRes = client.newCall(secReq).execute()
            if (!secRes.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to load sections: HTTP ${secRes.code}"))
            }

            val secAdapter = moshi.adapter(PlexSectionsResponse::class.java)
            val sections = secAdapter.fromJson(secRes.body?.string() ?: "")?.mediaContainer?.directory ?: emptyList()
            val musicSections = sections.filter { it.type == "artist" }

            val tracksList = mutableListOf<MusicTrack>()

            // 2. For each music section, fetch all tracks (type=10)
            for (sec in musicSections) {
                val tracksReq = Request.Builder()
                    .url("$root/library/sections/${sec.key}/all?type=10")
                    .get()
                    .addHeader("Accept", "application/json")
                    .addHeader("X-Plex-Token", token)
                    .addHeader("X-Plex-Client-Identifier", CLIENT_ID)
                    .build()

                val tracksRes = client.newCall(tracksReq).execute()
                if (tracksRes.isSuccessful) {
                    val tracksAdapter = moshi.adapter(PlexTracksResponse::class.java)
                    val metadata = tracksAdapter.fromJson(tracksRes.body?.string() ?: "")?.mediaContainer?.metadata ?: emptyList()

                    for (item in metadata) {
                        val partKey = item.media?.firstOrNull()?.part?.firstOrNull()?.key ?: ""
                        val streamUrl = if (partKey.isNotBlank()) {
                            "$root$partKey?X-Plex-Token=$token"
                        } else {
                            ""
                        }

                        val thumbPath = item.thumb ?: item.parentThumb ?: item.grandparentThumb
                        val coverUrl = if (!thumbPath.isNullOrBlank()) {
                            "$root$thumbPath?X-Plex-Token=$token"
                        } else {
                            ""
                        }

                        val genreTag = item.genreList?.firstOrNull()?.tag?.takeIf { it.isNotBlank() } ?: "Various"
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
}
