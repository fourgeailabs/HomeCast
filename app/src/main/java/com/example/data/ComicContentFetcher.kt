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

import com.example.data.network.OptimizedNetworkEngine

object ComicContentFetcher {
    private const val TAG = "ComicContentFetcher"

    private val client: OkHttpClient get() = OptimizedNetworkEngine.client

    /**
     * Resolves real comic pages (either extracted from CBZ/ZIP, from Komga/Kavita/Audiobookshelf server,
     * from Archive.org files/pages, or local directories/archives).
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

            // Case 2: Local Storage File or Directory
            val localPath = when {
                downloadUrl.startsWith("/") || downloadUrl.startsWith("file://") -> downloadUrl.removePrefix("file://")
                coverUrl.startsWith("/") || coverUrl.startsWith("file://") -> coverUrl.removePrefix("file://")
                else -> ""
            }
            if (localPath.isNotBlank()) {
                val localFile = File(localPath)
                if (localFile.isDirectory) {
                    val loaded = loadExtractedPagesFromDir(localFile)
                    if (loaded.isNotEmpty()) return@withContext loaded
                } else if (localFile.exists() && (localPath.endsWith(".cbz", true) || localPath.endsWith(".zip", true))) {
                    localFile.inputStream().use { stream ->
                        val extracted = extractFromInputStream(context, comicId, stream)
                        if (extracted.isNotEmpty()) return@withContext extracted
                    }
                }
            }

            // Case 3: Archive.org Identifier Resolution & Extraction
            val archiveId = extractArchiveIdentifier(coverUrl, downloadUrl, comicId)
            if (archiveId.isNotBlank()) {
                Log.d(TAG, "Fetching Archive.org files for identifier: $archiveId")
                val files = com.example.data.network.ArchiveOrgClient.fetchFilesForIdentifier(archiveId)
                
                // 3a. Look for actual CBZ or ZIP archive file on Archive.org
                val cbzFile = files.firstOrNull { 
                    it.name.endsWith(".cbz", ignoreCase = true) || 
                    it.name.endsWith(".zip", ignoreCase = true) 
                }
                if (cbzFile != null) {
                    val cbzUrl = "https://archive.org/download/$archiveId/${cbzFile.name}"
                    val extracted = downloadAndExtractCbz(context, comicId, cbzUrl, serverApiKey)
                    if (extracted.isNotEmpty()) return@withContext extracted
                }

                // 3b. Look for direct page image files in Archive.org metadata
                val imageFiles = files.filter { file ->
                    val name = file.name.lowercase()
                    (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".jp2")) &&
                    !name.contains("_thumb") && !name.contains("_small")
                }.sortedWith(Comparator { a, b ->
                    extractNumber(a.name).compareTo(extractNumber(b.name))
                })

                if (imageFiles.isNotEmpty()) {
                    imageFiles.forEachIndexed { index, file ->
                        pages.add(
                            ComicPage(
                                pageNumber = index + 1,
                                fullPageArtUrl = "https://archive.org/download/$archiveId/${file.name}",
                                pageTitle = "Page ${index + 1}"
                            )
                        )
                    }
                    return@withContext pages
                }

                // 3c. Archive.org Book/Comic Reader Page Stream fallback
                val targetPages = if (pageCount > 0) pageCount else 32
                for (i in 0 until targetPages) {
                    pages.add(
                        ComicPage(
                            pageNumber = i + 1,
                            fullPageArtUrl = "https://archive.org/download/$archiveId/page/n$i.jpg",
                            pageTitle = "Page ${i + 1}"
                        )
                    )
                }
                if (pages.isNotEmpty()) return@withContext pages
            }

            // Case 4: Audiobookshelf / Direct Remote Stream
            if (downloadUrl.isNotBlank() && (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://"))) {
                val reqBuilder = Request.Builder().url(downloadUrl)
                if (serverApiKey.isNotBlank()) {
                    reqBuilder.header("Authorization", if (serverApiKey.startsWith("Bearer ")) serverApiKey else "Bearer $serverApiKey")
                }
                val resp = client.newCall(reqBuilder.build()).execute()
                if (resp.isSuccessful && resp.body != null) {
                    val contentType = resp.header("Content-Type") ?: ""
                    if (contentType.contains("zip", true) || contentType.contains("comic", true) || contentType.contains("octet-stream", true) || downloadUrl.contains(".cbz", true) || downloadUrl.contains(".zip", true)) {
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

        // Fallback: Return cover URL if available
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

    private fun extractArchiveIdentifier(coverUrl: String, downloadUrl: String, comicId: String): String {
        val candidates = listOf(downloadUrl, coverUrl)
        for (url in candidates) {
            if (url.contains("archive.org")) {
                val id = url.substringAfter("img/")
                    .substringAfter("details/")
                    .substringAfter("download/")
                    .substringBefore("/")
                    .substringBefore("?")
                    .trim()
                if (id.isNotBlank() && !id.contains(".")) return id
            }
        }
        if (comicId.isNotBlank() && !comicId.contains("/") && !comicId.contains("\\") && !comicId.startsWith("http")) {
            return comicId
        }
        return ""
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
                    if (!entry.isDirectory && (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".jp2"))) {
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
            file.isFile && (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".jp2"))
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

