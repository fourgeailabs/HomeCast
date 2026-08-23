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

    suspend fun fetchPublicDomain(query: String): List<ArchiveDoc> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://archive.org/advancedsearch.php?q=$encodedQuery&fl[]=identifier,title,creator,description&sort[]=downloads+desc&rows=40&page=1&output=json"
            val request = Request.Builder().url(url).build()
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

    suspend fun fetchFilesForIdentifier(identifier: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://archive.org/metadata/$identifier/files"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                val jsonObject = org.json.JSONObject(body)
                val filesArray = jsonObject.optJSONArray("result") ?: return@withContext emptyList()
                val list = mutableListOf<String>()
                for (i in 0 until filesArray.length()) {
                    val fileObj = filesArray.optJSONObject(i)
                    val name = fileObj?.optString("name") ?: ""
                    if (name.isNotBlank()) {
                        list.add(name)
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
