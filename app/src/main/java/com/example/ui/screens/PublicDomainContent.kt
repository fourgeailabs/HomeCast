package com.example.ui.screens

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.util.Log

object PublicDomainContentFetcher {
    private val client = OkHttpClient()

    suspend fun fetchTextContent(url: String): List<BookChapter> = withContext(Dispatchers.IO) {
        try {
            if (url.isBlank()) return@withContext emptyList()
            
            var fullText: String? = null
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                fullText = response.body?.string()
            }
            
            // Auto fallback for Archive.org text format variations
            if (fullText == null && url.contains("_djvu.txt")) {
                val fallbackUrl = url.replace("_djvu.txt", ".txt")
                val fallbackReq = Request.Builder()
                    .url(fallbackUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                    .build()
                val fallbackResp = client.newCall(fallbackReq).execute()
                if (fallbackResp.isSuccessful) {
                    fullText = fallbackResp.body?.string()
                }
            }

            if (fullText == null && url.contains(".txt")) {
                val fallbackUrl = url.replace(".txt", "_djvu.txt")
                val fallbackReq = Request.Builder()
                    .url(fallbackUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                    .build()
                val fallbackResp = client.newCall(fallbackReq).execute()
                if (fallbackResp.isSuccessful) {
                    fullText = fallbackResp.body?.string()
                }
            }

            if (fullText.isNullOrBlank()) {
                return@withContext emptyList()
            }
            
            // Simple logic: split into chapters by "CHAPTER" or just chunk it
            val chunks = fullText.split("CHAPTER").filter { it.isNotBlank() }
            
            val chapters = mutableListOf<BookChapter>()
            if (chunks.size > 1) {
                // Ignore the first chunk as it's usually preamble
                for (i in 1 until chunks.size) {
                    val chapterText = chunks[i].trim()
                    val lines = chapterText.split("\n\n").map { it.trim().replace("\n", " ") }.filter { it.isNotBlank() }
                    chapters.add(
                        BookChapter(
                            title = "Chapter $i",
                            startPage = i * 10,
                            paragraphs = lines.take(200) // Increase limits
                        )
                    )
                }
            } else {
                // Just chunk it arbitrarily
                val lines = fullText.split("\n\n").map { it.trim().replace("\n", " ") }.filter { it.isNotBlank() }
                val chunkSize = 50
                val chunkedLines = lines.chunked(chunkSize)
                for (i in chunkedLines.indices) {
                    chapters.add(
                        BookChapter(
                            title = "Part ${i+1}",
                            startPage = i * 5,
                            paragraphs = chunkedLines[i]
                        )
                    )
                }
            }
            return@withContext chapters
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
}
