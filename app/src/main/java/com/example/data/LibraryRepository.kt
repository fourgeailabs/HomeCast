package com.example.data

import com.example.data.network.AudiobookshelfClient
import com.example.data.network.PlexClient
import kotlinx.coroutines.flow.Flow

class LibraryRepository(private val dao: LibraryDao, private val secureConfigManager: SecureConfigManager) {
    val allBooks: Flow<List<Audiobook>> = dao.getAllBooks()
    val favorites: Flow<List<Audiobook>> = dao.getFavorites()
    val recents: Flow<List<Audiobook>> = dao.getRecents()
    val allMusic: Flow<List<MusicTrack>> = dao.getAllMusic()
    val recentMusic: Flow<List<MusicTrack>> = dao.getRecentMusic()
    val allEBooks: Flow<List<EBook>> = dao.getAllEBooks()
    val servers: Flow<List<ServerConfig>> = secureConfigManager.serversFlow
    val allPublicDomainSources: Flow<List<PublicDomainSource>> = dao.getAllPublicDomainSources()
    val allLocalFolders: Flow<List<LocalFolderConfig>> = dao.getAllLocalFolders()
    val recentPrograms: Flow<List<RecentProgramEntity>> = dao.getAllRecentPrograms()

    suspend fun insertRecentProgram(program: RecentProgramEntity) = dao.insertRecentProgram(program)
    suspend fun updateProgramProgress(id: String, progress: Long) = dao.updateProgramProgress(id, progress, System.currentTimeMillis())
    suspend fun deleteRecentProgram(id: String) = dao.deleteRecentProgram(id)

    suspend fun insertBook(book: Audiobook) = dao.insertBook(book)
    suspend fun insertBooks(books: List<Audiobook>) = dao.insertBooks(books)
    suspend fun insertMusicTracks(tracks: List<MusicTrack>) = dao.insertMusicTracks(tracks)
    suspend fun insertEBooks(ebooks: List<EBook>) = dao.insertEBooks(ebooks)
    
    suspend fun deleteBooksByServer(serverId: String) = dao.deleteBooksByServer(serverId)
    suspend fun deleteMusicByServer(serverId: String) = dao.deleteMusicByServer(serverId)
    suspend fun deleteEBooksByServer(serverId: String) = dao.deleteEBooksByServer(serverId)

    // Public Domain Sources
    suspend fun addPublicDomainSource(source: PublicDomainSource) = dao.insertPublicDomainSource(source)
    suspend fun addPublicDomainSources(sources: List<PublicDomainSource>) = dao.insertPublicDomainSources(sources)
    suspend fun togglePublicDomainSource(id: String, isEnabled: Boolean) = dao.togglePublicDomainSource(id, isEnabled)
    suspend fun deletePublicDomainSource(id: String) = dao.deletePublicDomainSource(id)

    // Local Folders
    suspend fun addLocalFolder(folder: LocalFolderConfig) = dao.insertLocalFolder(folder)
    suspend fun deleteLocalFolder(id: String) = dao.deleteLocalFolder(id)
    suspend fun updateFolderScanStatus(id: String, count: Int, timestamp: Long) = dao.updateFolderScanStatus(id, count, timestamp)
    suspend fun updateProgress(id: String, progress: Long) {
        dao.updateProgress(id, progress, System.currentTimeMillis())
    }
    suspend fun toggleFavorite(book: Audiobook) {
        dao.setFavorite(book.id, !book.isFavorite)
    }

    suspend fun updateMusicLastPlayed(id: String) {
        dao.updateMusicLastPlayed(id, System.currentTimeMillis())
    }

    suspend fun addOrUpdateServer(server: ServerConfig) {
        secureConfigManager.saveServer(server)
    }

    suspend fun removeServer(serverId: String) {
        secureConfigManager.removeServer(serverId)
        dao.deleteBooksByServer(serverId)
        dao.deleteMusicByServer(serverId)
        dao.deleteEBooksByServer(serverId)
    }

    suspend fun syncAudiobooks(server: ServerConfig): Result<Int> {
        val result = AudiobookshelfClient.fetchAudiobooks(server.hostUrl, server.apiKey, server.id)
        return if (result.isSuccess) {
            val books = result.getOrNull() ?: emptyList()
            if (books.isNotEmpty()) {
                val sanitizedBooks = books.map { book ->
                    book.copy(
                        author = com.example.utils.sanitizeAuthorName(book.author),
                        genre = com.example.utils.sanitizeGenreName(book.genre, book.title, "")
                    )
                }
                dao.deleteBooksByServer(server.id)
                dao.insertBooks(sanitizedBooks)
            }
            secureConfigManager.saveServer(server.copy(isConnected = true, lastSyncTime = System.currentTimeMillis()))
            Result.success(books.size)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        }
    }

    suspend fun syncPlex(server: ServerConfig, candidateUrls: List<String> = emptyList()): Result<Int> {
        val result = PlexClient.fetchMusicTracks(server.hostUrl, server.apiKey, server.id, candidateUrls)
        return if (result.isSuccess) {
            val tracks = result.getOrNull() ?: emptyList()
            if (tracks.isNotEmpty()) {
                val sanitizedTracks = tracks.map { track ->
                    track.copy(
                        artist = com.example.utils.sanitizeAuthorName(track.artist),
                        genre = com.example.utils.sanitizeGenreName(track.genre, track.title, "")
                    )
                }
                dao.deleteMusicByServer(server.id)
                dao.insertMusicTracks(sanitizedTracks)
            }
            secureConfigManager.saveServer(server.copy(isConnected = true, lastSyncTime = System.currentTimeMillis()))
            Result.success(tracks.size)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        }
    }

    suspend fun syncBooklore(server: ServerConfig): Result<Int> {
        val result = com.example.data.network.BookloreClient.fetchBooks(server.hostUrl, server.apiKey, server.id)
        return if (result.isSuccess) {
            val books = result.getOrNull() ?: emptyList()
            if (books.isNotEmpty()) {
                val sanitizedBooks = books.map { book ->
                    book.copy(
                        author = com.example.utils.sanitizeAuthorName(book.author),
                        genre = com.example.utils.sanitizeGenreName(book.genre, book.title, book.description)
                    )
                }
                dao.deleteEBooksByServer(server.id)
                dao.insertEBooks(sanitizedBooks)
            }
            secureConfigManager.saveServer(server.copy(isConnected = true, lastSyncTime = System.currentTimeMillis()))
            Result.success(books.size)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        }
    }
}
