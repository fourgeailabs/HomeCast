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

@JsonClass(generateAdapter = true)
data class ArchiveFile(
    val name: String,
    val length: Double = 0.0
)

object ArchiveOrgClient {
    private val client = OptimizedNetworkEngine.client
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    suspend fun fetchPublicDomain(query: String): List<ArchiveDoc> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://archive.org/advancedsearch.php?q=$encodedQuery&fl[]=identifier,title,creator,description&sort[]=downloads+desc&rows=250&page=1&output=json"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                val adapter = moshi.adapter(ArchiveSearchResponse::class.java)
                val searchResponse = adapter.fromJson(body)
                return@withContext searchResponse?.response?.docs ?: emptyList()
            }
        } catch (e: Exception) {
            Log.e("ArchiveOrg", "Error fetching archive org with query: $query", e)
        }
        emptyList()
    }

    suspend fun fetchFilesForIdentifier(identifier: String): List<ArchiveFile> = withContext(Dispatchers.IO) {
        try {
            val url = "https://archive.org/metadata/$identifier/files"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                val jsonObject = org.json.JSONObject(body)
                val filesArray = jsonObject.optJSONArray("result") ?: return@withContext emptyList()
                val list = mutableListOf<ArchiveFile>()
                for (i in 0 until filesArray.length()) {
                    val fileObj = filesArray.optJSONObject(i)
                    val name = fileObj?.optString("name") ?: ""
                    val lengthStr = fileObj?.optString("length") ?: "0.0"
                    val length = lengthStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        list.add(ArchiveFile(name, length))
                    }
                }
                return@withContext list
            }
        } catch (e: Exception) {
            Log.e("ArchiveOrg", "Error fetching files for $identifier", e)
        }
        emptyList()
    }
}
