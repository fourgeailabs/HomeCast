package com.example.data.network

import android.util.Log
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@JsonClass(generateAdapter = true)
data class ArchiveSearchResponse(
    val response: ArchiveResponseBody? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveResponseBody(
    val docs: List<ArchiveDoc>? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveDoc(
    val identifier: String,
    val title: String? = null,
    val creator: Any? = null, // can be string or list of strings
    val description: Any? = null // can be string or list of strings
)

object ArchiveOrgClient {
    private val client = OkHttpClient.Builder().build()
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    suspend fun fetchPublicDomain(collection: String): List<ArchiveDoc> = withContext(Dispatchers.IO) {
        try {
            val url = "https://archive.org/advancedsearch.php?q=collection:($collection)&fl[]=identifier,title,creator,description&sort[]=downloads+desc&rows=30&page=1&output=json"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                val adapter = moshi.adapter(ArchiveSearchResponse::class.java)
                val searchResponse = adapter.fromJson(body)
                return@withContext searchResponse?.response?.docs ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e("ArchiveOrg", "Error fetching archive org", e)
        }
        emptyList()
    }
}
