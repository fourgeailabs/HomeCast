package com.example.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class LocalScanResult(
    val audiobooks: List<Audiobook> = emptyList(),
    val ebooks: List<EBook> = emptyList(),
    val musicTracks: List<MusicTrack> = emptyList()
)

object LocalMediaScanner {

    private val AUDIO_EXTENSIONS = setOf("mp3", "m4b", "flac", "aac", "wav", "m4a", "ogg", "opus")
    private val EBOOK_EXTENSIONS = setOf("epub", "pdf", "txt", "mobi")
    private val COMIC_EXTENSIONS = setOf("cbz", "cbr", "zip")

    suspend fun scanFolder(context: Context, folder: LocalFolderConfig): LocalScanResult {
        return withContext(Dispatchers.IO) {
            val audiobooks = mutableListOf<Audiobook>()
            val ebooks = mutableListOf<EBook>()
            val musicTracks = mutableListOf<MusicTrack>()

            try {
                if (folder.folderPath.startsWith("content://")) {
                    val treeUri = Uri.parse(folder.folderPath)
                    val documentTree = DocumentFile.fromTreeUri(context, treeUri)
                    if (documentTree != null && documentTree.isDirectory) {
                        scanDocumentTree(documentTree, folder, audiobooks, ebooks, musicTracks)
                    }
                } else {
                    val file = File(folder.folderPath)
                    if (file.exists() && file.isDirectory) {
                        scanFileDirectory(file, folder, audiobooks, ebooks, musicTracks)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            LocalScanResult(
                audiobooks = audiobooks,
                ebooks = ebooks,
                musicTracks = musicTracks
            )
        }
    }

    private fun scanDocumentTree(
        dir: DocumentFile,
        folder: LocalFolderConfig,
        audiobooks: MutableList<Audiobook>,
        ebooks: MutableList<EBook>,
        musicTracks: MutableList<MusicTrack>
    ) {
        val files = dir.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                scanDocumentTree(file, folder, audiobooks, ebooks, musicTracks)
            } else if (file.isFile) {
                val name = file.name ?: "Unknown"
                val extension = name.substringAfterLast('.', "").lowercase()
                val uriString = file.uri.toString()
                val (rawTitle, rawArtist) = parseNameAndArtist(name)

                when (folder.mediaType.uppercase()) {
                    "AUDIOBOOK" -> {
                        if (extension in AUDIO_EXTENSIONS) {
                            val id = "local_ab_${folder.id}_${name.hashCode().toUInt()}"
                            audiobooks.add(
                                Audiobook(
                                    id = id,
                                    title = rawTitle,
                                    author = rawArtist.ifBlank { "Local Audio" },
                                    coverUrl = "",
                                    duration = 1800L,
                                    serverId = "local_device",
                                    streamUrl = uriString,
                                    genre = "Audiobook"
                                )
                            )
                        }
                    }
                    "EBOOK" -> {
                        if (extension in EBOOK_EXTENSIONS || extension in COMIC_EXTENSIONS) {
                            val isComic = extension in COMIC_EXTENSIONS || name.contains("comic", true) || name.contains("manga", true)
                            val id = "local_eb_${folder.id}_${name.hashCode().toUInt()}"
                            ebooks.add(
                                EBook(
                                    id = id,
                                    title = rawTitle,
                                    author = rawArtist.ifBlank { "Local Author" },
                                    coverUrl = "",
                                    serverId = "local_device",
                                    genre = if (isComic) "Comic" else "E-Book",
                                    description = "Local media document stored in ${folder.displayName}",
                                    totalPages = 100,
                                    downloadUrl = uriString,
                                    isComic = isComic
                                )
                            )
                        }
                    }
                    "MUSIC" -> {
                        if (extension in AUDIO_EXTENSIONS) {
                            val id = "local_mu_${folder.id}_${name.hashCode().toUInt()}"
                            musicTracks.add(
                                MusicTrack(
                                    id = id,
                                    title = rawTitle,
                                    artist = rawArtist.ifBlank { "Local Artist" },
                                    album = dir.name ?: folder.displayName,
                                    coverUrl = "",
                                    duration = 210000L,
                                    serverId = "local_device",
                                    streamUrl = uriString,
                                    genre = "Music"
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun scanFileDirectory(
        dir: File,
        folder: LocalFolderConfig,
        audiobooks: MutableList<Audiobook>,
        ebooks: MutableList<EBook>,
        musicTracks: MutableList<MusicTrack>
    ) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                scanFileDirectory(file, folder, audiobooks, ebooks, musicTracks)
            } else if (file.isFile) {
                val name = file.name
                val extension = file.extension.lowercase()
                val path = file.absolutePath
                val (rawTitle, rawArtist) = parseNameAndArtist(name)

                when (folder.mediaType.uppercase()) {
                    "AUDIOBOOK" -> {
                        if (extension in AUDIO_EXTENSIONS) {
                            val id = "local_ab_${folder.id}_${name.hashCode().toUInt()}"
                            audiobooks.add(
                                Audiobook(
                                    id = id,
                                    title = rawTitle,
                                    author = rawArtist.ifBlank { "Local Audio" },
                                    coverUrl = "",
                                    duration = 1800L,
                                    serverId = "local_device",
                                    streamUrl = path,
                                    genre = "Audiobook"
                                )
                            )
                        }
                    }
                    "EBOOK" -> {
                        if (extension in EBOOK_EXTENSIONS || extension in COMIC_EXTENSIONS) {
                            val isComic = extension in COMIC_EXTENSIONS || name.contains("comic", true) || name.contains("manga", true)
                            val id = "local_eb_${folder.id}_${name.hashCode().toUInt()}"
                            ebooks.add(
                                EBook(
                                    id = id,
                                    title = rawTitle,
                                    author = rawArtist.ifBlank { "Local Author" },
                                    coverUrl = "",
                                    serverId = "local_device",
                                    genre = if (isComic) "Comic" else "E-Book",
                                    description = "Local file from ${folder.displayName}",
                                    totalPages = 100,
                                    downloadUrl = path,
                                    isComic = isComic
                                )
                            )
                        }
                    }
                    "MUSIC" -> {
                        if (extension in AUDIO_EXTENSIONS) {
                            val id = "local_mu_${folder.id}_${name.hashCode().toUInt()}"
                            musicTracks.add(
                                MusicTrack(
                                    id = id,
                                    title = rawTitle,
                                    artist = rawArtist.ifBlank { "Local Artist" },
                                    album = dir.name ?: folder.displayName,
                                    coverUrl = "",
                                    duration = 210000L,
                                    serverId = "local_device",
                                    streamUrl = path,
                                    genre = "Music"
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun parseNameAndArtist(filename: String): Pair<String, String> {
        val nameWithoutExt = filename.substringBeforeLast('.')
        return if (nameWithoutExt.contains(" - ")) {
            val parts = nameWithoutExt.split(" - ", limit = 2)
            Pair(parts[1].trim(), parts[0].trim())
        } else if (nameWithoutExt.contains(" by ", ignoreCase = true)) {
            val parts = nameWithoutExt.split(Regex("(?i) by "), limit = 2)
            Pair(parts[0].trim(), parts[1].trim())
        } else {
            val cleaned = nameWithoutExt.replace('_', ' ').replace('-', ' ').trim()
            Pair(cleaned, "")
        }
    }
}
