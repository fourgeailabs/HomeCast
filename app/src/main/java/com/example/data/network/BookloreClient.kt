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
data class KomgaMedia(
    val pagesCount: Int? = null
)

object BookloreClient {
    private const val TAG = "BookloreClient"
    
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    suspend fun login(hostUrl: String, username: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = hostUrl.trimEnd('/')
            val url = "$normalizedUrl/api/v1/auth/login"
            
            val jsonBody = "{\"username\":\"$username\", \"password\":\"$password\"}"
            val requestBody = okhttp3.RequestBody.create("application/json".toMediaType(), jsonBody)
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
                
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string()
                if (bodyString != null) {
                    val tokenKey = "\"accessToken\":\""
                    val idx = bodyString.indexOf(tokenKey)
                    if (idx != -1) {
                        val start = idx + tokenKey.length
                        val end = bodyString.indexOf("\"", start)
                        if (end != -1) {
                            return@withContext Result.success(bodyString.substring(start, end))
                        }
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
                                            EBook(
                                                id = book.id,
                                                title = book.title ?: book.name ?: "Unknown",
                                                author = book.author ?: book.writer ?: "Unknown",
                                                coverUrl = book.coverUrl ?: if (endpoint.contains("books")) "$normalizedUrl/api/v1/books/${book.id}/thumbnail" else "",
                                                serverId = serverId,
                                                genre = book.genre ?: "Unknown",
                                                description = book.description ?: book.summary ?: "",
                                                totalPages = book.totalPages ?: book.media?.pagesCount ?: 0,
                                                isComic = book.isComic ?: true
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
                                            EBook(
                                                id = book.id,
                                                title = book.title ?: book.name ?: "Unknown",
                                                author = book.author ?: book.writer ?: "Unknown",
                                                coverUrl = book.coverUrl ?: if (endpoint.contains("books")) "$normalizedUrl/api/v1/books/${book.id}/thumbnail" else "",
                                                serverId = serverId,
                                                genre = book.genre ?: "Unknown",
                                                description = book.description ?: book.summary ?: "",
                                                totalPages = book.totalPages ?: book.media?.pagesCount ?: 0,
                                                isComic = book.isComic ?: true
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
