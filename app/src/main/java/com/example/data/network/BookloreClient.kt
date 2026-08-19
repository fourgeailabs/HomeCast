package com.example.data.network

import android.util.Log
import com.example.data.EBook
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BookloreResponse(
    val books: List<BookloreBook>? = null
)

@JsonClass(generateAdapter = true)
data class BookloreBook(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String? = null,
    val genre: String? = null,
    val description: String? = null,
    val totalPages: Int? = null,
    val isComic: Boolean? = null
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

    suspend fun fetchBooks(hostUrl: String, apiKey: String, serverId: String): Result<List<EBook>> = withContext(Dispatchers.IO) {
        try {
            val url = "${hostUrl.trimEnd('/')}/api/v1/library/books"
            val requestBuilder = Request.Builder().url(url)
            
            if (apiKey.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $apiKey")
            }

            val request = requestBuilder.build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val bodyString = response.body?.string()
                if (bodyString != null) {
                    val adapter = moshi.adapter(BookloreResponse::class.java)
                    val bookloreResponse = adapter.fromJson(bodyString)
                    
                    val ebooks = bookloreResponse?.books?.map { book ->
                        EBook(
                            id = book.id,
                            title = book.title,
                            author = book.author,
                            coverUrl = book.coverUrl ?: "",
                            serverId = serverId,
                            genre = book.genre ?: "Unknown",
                            description = book.description ?: "",
                            totalPages = book.totalPages ?: 0,
                            isComic = book.isComic ?: false
                        )
                    } ?: emptyList()
                    
                    return@withContext Result.success(ebooks)
                } else {
                    return@withContext Result.failure(Exception("Empty response body"))
                }
            } else {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Booklore books", e)
            return@withContext Result.failure(e)
        }
    }
}
