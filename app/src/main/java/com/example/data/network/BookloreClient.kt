package com.example.data.network

import android.util.Log
import com.example.data.EBook
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BookloreResponse(
    val books: List<BookloreBook>? = null,
    val content: List<BookloreBook>? = null
)

@JsonClass(generateAdapter = true)
data class BookloreBook(
    val id: String,
    val title: String? = null,
    val name: String? = null,
    val metadata: BookloreMetadata? = null,
    
    val author: String? = null,
    val writer: String? = null,
    val coverUrl: String? = null,
    val genre: String? = null,
    val description: String? = null,
    val summary: String? = null,
    val totalPages: Int? = null,
    val isComic: Boolean? = null,
    val media: KomgaMedia? = null
)

@JsonClass(generateAdapter = true)
data class BookloreMetadata(
    val title: String? = null,
    val authors: List<String>? = null,
    val description: String? = null,
    val pageCount: Int? = null,
    val categories: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class KomgaMedia(
    val pagesCount: Int? = null
)

object BookloreClient {
    private const val TAG = "BookloreClient"
    
    private val client: OkHttpClient by lazy {
        OptimizedNetworkEngine.client
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    suspend fun login(hostUrl: String, username: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = hostUrl.trimEnd('/')
            val url = "$normalizedUrl/api/v1/auth/login"
            
            // Build JSON safely
            val jsonBody = org.json.JSONObject()
            jsonBody.put("username", username)
            jsonBody.put("password", password)
            
            val requestBody = okhttp3.RequestBody.create("application/json".toMediaType(), jsonBody.toString())
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
                
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string()
                if (bodyString != null) {
                    try {
                        val json = org.json.JSONObject(bodyString)
                        val token = json.optString("accessToken", "")
                        if (token.isNotEmpty()) {
                            return@withContext Result.success(token)
                        }
                    } catch (e: Exception) {
                        return@withContext Result.failure(Exception("Failed to parse login response"))
                    }
                }
            }
            return@withContext Result.failure(Exception("HTTP ${response.code}: Login failed"))
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    suspend fun fetchBooks(hostUrl: String, apiKey: String, serverId: String): Result<List<EBook>> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = hostUrl.trimEnd('/')
            
            // Allow testing different endpoints
            val endpoints = listOf(
                "/api/v1/library/books", 
                "/api/books",
                "/api/v2/opds/catalog",
                "/opds/v1.2/catalog",
                "/api/v1/books",
                "" // Fallback to root if they mapped it directly
            )
            
            var lastException: Exception? = null
            var lastErrorString: String? = null
            
            for (endpoint in endpoints) {
                try {
                    val currentUrl = "$normalizedUrl$endpoint"
                    val requestBuilder = Request.Builder().url(currentUrl)
                    
                    if (apiKey.isNotBlank() && !apiKey.equals("Basic Og==")) { // Don't send empty basic auth
                        if (apiKey.startsWith("Basic ")) {
                            requestBuilder.header("Authorization", apiKey)
                        } else {
                            requestBuilder.header("Authorization", "Bearer $apiKey")
                        }
                    }
                    val request = requestBuilder.build()
                    val response = client.newCall(request).execute()
                    
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string()?.trim()
                        if (bodyString != null) {
                            try {
                                if (bodyString.startsWith("{")) {
                                    val adapter = moshi.adapter(BookloreResponse::class.java)
                                    val bookloreResponse = adapter.fromJson(bodyString)
                                    
                                    val bookList = bookloreResponse?.books ?: bookloreResponse?.content
                                    if (bookList != null) {
                                        val ebooks = bookList.map { book ->
                                            val authorsList = book.metadata?.authors ?: emptyList()
                                            val authorName = if (authorsList.isNotEmpty()) authorsList.joinToString(", ") else book.author ?: book.writer ?: "Unknown"
                                            val categoriesList = book.metadata?.categories ?: emptyList()
                                            val genreName = if (categoriesList.isNotEmpty()) categoriesList.first() else book.genre ?: "Unknown"
                                            
                                            // Determine cover url based on Booklore vs Komga
                                            var rawToken = apiKey
                                            if (rawToken.startsWith("Bearer ")) {
                                                rawToken = rawToken.substring(7)
                                            }
                                            val queryParam = if (rawToken.isNotBlank() && !rawToken.startsWith("Basic ")) "?token=$rawToken" else ""
                                            val cover = book.coverUrl ?: if (normalizedUrl.contains("komga") || endpoint.contains("komga")) {
                                                "$normalizedUrl/api/v1/books/${book.id}/pages/1"
                                            } else {
                                                "$normalizedUrl/api/v1/media/book/${book.id}/cover$queryParam"
                                            }
                                            
                                            val download = if (normalizedUrl.contains("komga") || endpoint.contains("komga")) {
                                                "$normalizedUrl/api/v1/books/${book.id}/file"
                                            } else {
                                                "$normalizedUrl/api/v1/media/book/${book.id}/file$queryParam"
                                            }
                                            
                                            EBook(
                                                id = book.id,
                                                title = book.metadata?.title ?: book.title ?: book.name ?: "Unknown",
                                                author = authorName,
                                                coverUrl = cover,
                                                serverId = serverId,
                                                genre = genreName,
                                                description = book.metadata?.description ?: book.description ?: book.summary ?: "",
                                                totalPages = book.metadata?.pageCount ?: book.totalPages ?: book.media?.pagesCount ?: 0,
                                                downloadUrl = download,
                                                isComic = book.isComic ?: (genreName.contains("comic", true) || genreName.contains("manga", true))
                                            )
                                        }
                                        return@withContext Result.success(ebooks)
                                    }
                                } else if (bodyString.startsWith("[")) {
                                    // Maybe it's a direct array?
                                    val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, BookloreBook::class.java)
                                    val adapter: com.squareup.moshi.JsonAdapter<List<BookloreBook>> = moshi.adapter(listType)
                                    val books = adapter.fromJson(bodyString)
                                    if (books != null) {
                                        val ebooks = books.map { book ->
                                            val authorsList = book.metadata?.authors ?: emptyList()
                                            val authorName = if (authorsList.isNotEmpty()) authorsList.joinToString(", ") else book.author ?: book.writer ?: "Unknown"
                                            val categoriesList = book.metadata?.categories ?: emptyList()
                                            val genreName = if (categoriesList.isNotEmpty()) categoriesList.first() else book.genre ?: "Unknown"
                                            
                                            // Determine cover url based on Booklore vs Komga
                                            var rawToken = apiKey
                                            if (rawToken.startsWith("Bearer ")) {
                                                rawToken = rawToken.substring(7)
                                            }
                                            val queryParam = if (rawToken.isNotBlank() && !rawToken.startsWith("Basic ")) "?token=$rawToken" else ""
                                            val cover = book.coverUrl ?: if (normalizedUrl.contains("komga") || endpoint.contains("komga")) {
                                                "$normalizedUrl/api/v1/books/${book.id}/pages/1"
                                            } else {
                                                "$normalizedUrl/api/v1/media/book/${book.id}/cover$queryParam"
                                            }
                                            
                                            val download = if (normalizedUrl.contains("komga") || endpoint.contains("komga")) {
                                                "$normalizedUrl/api/v1/books/${book.id}/file"
                                            } else {
                                                "$normalizedUrl/api/v1/media/book/${book.id}/file$queryParam"
                                            }

                                            EBook(
                                                id = book.id,
                                                title = book.metadata?.title ?: book.title ?: book.name ?: "Unknown",
                                                author = authorName,
                                                coverUrl = cover,
                                                serverId = serverId,
                                                genre = genreName,
                                                description = book.metadata?.description ?: book.description ?: book.summary ?: "",
                                                totalPages = book.metadata?.pageCount ?: book.totalPages ?: book.media?.pagesCount ?: 0,
                                                downloadUrl = download,
                                                isComic = book.isComic ?: (genreName.contains("comic", true) || genreName.contains("manga", true))
                                            )
                                        }
                                        return@withContext Result.success(ebooks)
                                    }
                                }
                            } catch (e: Exception) {
                                lastException = e
                                lastErrorString = "JSON Parse Error on $endpoint"
                            }
                        }
                    } else {
                        lastErrorString = "HTTP ${response.code} on $endpoint"
                    }
                } catch (e: Exception) {
                    lastException = e
                    lastErrorString = e.message
                }
            }
            
            return@withContext Result.failure(lastException ?: Exception("Connection failed: $lastErrorString"))
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Booklore books", e)
            return@withContext Result.failure(e)
        }
    }
}
