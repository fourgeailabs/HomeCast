package com.example.data

import android.content.Context
import android.util.Log
import com.example.ui.screens.ComicPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

object ComicContentFetcher {
    private const val TAG = "ComicContentFetcher"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Resolves real comic pages (either extracted from CBZ/ZIP, from Komga/Kavita/Audiobookshelf server,
     * or from Archive.org image lists).
     */
    suspend fun fetchComicPages(
        context: Context,
        comicId: String,
        title: String,
        downloadUrl: String = "",
        coverUrl: String = "",
        pageCount: Int = 0,
        serverId: String = "",
        serverHostUrl: String = "",
        serverApiKey: String = ""
    ): List<ComicPage> = withContext(Dispatchers.IO) {
        val pages = mutableListOf<ComicPage>()

        try {
            // Case 1: Komga / Kavita / Booklore server page streaming
            if (serverId.isNotBlank() && (serverHostUrl.contains("komga") || serverHostUrl.contains("kavita") || serverHostUrl.contains("booklore") || downloadUrl.contains("/api/v1/books/"))) {
                val normalizedHost = serverHostUrl.trimEnd('/')
                val rawToken = if (serverApiKey.startsWith("Bearer ")) serverApiKey.substring(7) else serverApiKey
                val authParam = if (rawToken.isNotBlank() && !rawToken.startsWith("Basic ")) "?token=$rawToken" else ""
                
                val count = if (pageCount > 0) pageCount else 24
                for (i in 1..count) {
                    val pageUrl = "$normalizedHost/api/v1/books/$comicId/pages/$i$authParam"
                    pages.add(
                        ComicPage(
                            pageNumber = i,
                            fullPageArtUrl = pageUrl,
                            pageTitle = "Page $i"
                        )
                    )
                }
                if (pages.isNotEmpty()) {
                    return@withContext pages
                }
            }

            // Case 2: CBZ / ZIP Archive Download & Local Extraction
            val archiveUrl = when {
                downloadUrl.isNotBlank() -> downloadUrl
                coverUrl.contains("archive.org") -> {
                    val identifier = coverUrl.substringAfter("img/").substringBefore("/")
                    "https://archive.org/download/$identifier/${identifier}.cbz"
                }
                else -> ""
            }

            if (archiveUrl.isNotBlank() && (archiveUrl.endsWith(".cbz", ignoreCase = true) || archiveUrl.endsWith(".zip", ignoreCase = true) || archiveUrl.contains("/download"))) {
                val extractedPages = downloadAndExtractCbz(context, comicId, archiveUrl, serverApiKey)
                if (extractedPages.isNotEmpty()) {
                    return@withContext extractedPages
                }
            }

            // Case 3: Archive.org image pages lookup
            if (coverUrl.contains("archive.org")) {
                val identifier = coverUrl.substringAfter("img/").substringBefore("/")
                val files = com.example.data.network.ArchiveOrgClient.fetchFilesForIdentifier(identifier)
                val imageFiles = files.filter { 
                    it.name.endsWith(".jpg", ignoreCase = true) || 
                    it.name.endsWith(".jpeg", ignoreCase = true) || 
                    it.name.endsWith(".png", ignoreCase = true) ||
                    it.name.endsWith(".webp", ignoreCase = true)
                }.sortedBy { it.name }

                if (imageFiles.isNotEmpty()) {
                    imageFiles.forEachIndexed { index, file ->
                        pages.add(
                            ComicPage(
                                pageNumber = index + 1,
                                fullPageArtUrl = "https://archive.org/download/$identifier/${file.name}",
                                pageTitle = "Page ${index + 1}"
                            )
                        )
                    }
                    return@withContext pages
                }
            }

            // Case 4: Audiobookshelf / Server Item file endpoint
            if (downloadUrl.isNotBlank()) {
                val reqBuilder = Request.Builder().url(downloadUrl)
                if (serverApiKey.isNotBlank()) {
                    reqBuilder.header("Authorization", if (serverApiKey.startsWith("Bearer ")) serverApiKey else "Bearer $serverApiKey")
                }
                val resp = client.newCall(reqBuilder.build()).execute()
                if (resp.isSuccessful && resp.body != null) {
                    val contentType = resp.header("Content-Type") ?: ""
                    if (contentType.contains("zip", true) || contentType.contains("comic", true) || contentType.contains("octet-stream", true)) {
                        val extracted = extractFromInputStream(context, comicId, resp.body!!.byteStream())
                        if (extracted.isNotEmpty()) {
                            return@withContext extracted
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch comic pages for $title", e)
        }

        // Fallback: If only cover is available, return cover as Page 1
        if (coverUrl.isNotBlank()) {
            val count = if (pageCount > 0) pageCount else 1
            for (i in 1..count) {
                pages.add(
                    ComicPage(
                        pageNumber = i,
                        fullPageArtUrl = coverUrl,
                        pageTitle = "Page $i"
                    )
                )
            }
        }

        pages
    }

    private fun downloadAndExtractCbz(
        context: Context,
        comicId: String,
        url: String,
        apiKey: String
    ): List<ComicPage> {
        val cacheDir = File(context.cacheDir, "comics/$comicId")
        if (cacheDir.exists() && cacheDir.listFiles()?.isNotEmpty() == true) {
            return loadExtractedPagesFromDir(cacheDir)
        }
        cacheDir.mkdirs()

        val reqBuilder = Request.Builder().url(url)
        if (apiKey.isNotBlank()) {
            reqBuilder.header("Authorization", if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey")
        }

        val response = client.newCall(reqBuilder.build()).execute()
        if (!response.isSuccessful || response.body == null) {
            return emptyList()
        }

        return extractFromInputStream(context, comicId, response.body!!.byteStream())
    }

    private fun extractFromInputStream(
        context: Context,
        comicId: String,
        inputStream: java.io.InputStream
    ): List<ComicPage> {
        val cacheDir = File(context.cacheDir, "comics/$comicId")
        cacheDir.mkdirs()

        try {
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase()
                    if (!entry.isDirectory && (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp"))) {
                        val sanitizedName = File(entry.name).name
                        val outputFile = File(cacheDir, sanitizedName)
                        FileOutputStream(outputFile).use { out ->
                            zip.copyTo(out)
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Zip extraction error for comic $comicId", e)
        }

        return loadExtractedPagesFromDir(cacheDir)
    }

    private fun loadExtractedPagesFromDir(cacheDir: File): List<ComicPage> {
        val imageFiles = cacheDir.listFiles { file ->
            val name = file.name.lowercase()
            file.isFile && (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp"))
        }?.sortedWith(Comparator { a, b ->
            extractNumber(a.name).compareTo(extractNumber(b.name))
        }) ?: emptyList()

        return imageFiles.mapIndexed { index, file ->
            ComicPage(
                pageNumber = index + 1,
                fullPageArtUrl = file.absolutePath,
                pageTitle = "Page ${index + 1}"
            )
        }
    }

    private fun extractNumber(name: String): Int {
        val digits = name.filter { it.isDigit() }
        return digits.toIntOrNull() ?: 0
    }
}
