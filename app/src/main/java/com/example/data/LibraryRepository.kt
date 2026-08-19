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
    val servers: Flow<List<ServerConfig>> = secureConfigManager.serversFlow

    suspend fun insertBook(book: Audiobook) = dao.insertBook(book)
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
    }

    suspend fun syncAudiobooks(server: ServerConfig): Result<Int> {
        val result = AudiobookshelfClient.fetchAudiobooks(server.hostUrl, server.apiKey, server.id)
        return if (result.isSuccess) {
            val books = result.getOrNull() ?: emptyList()
            if (books.isNotEmpty()) {
                dao.deleteBooksByServer(server.id)
                dao.insertBooks(books)
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
                dao.deleteMusicByServer(server.id)
                dao.insertMusicTracks(tracks)
            }
            secureConfigManager.saveServer(server.copy(isConnected = true, lastSyncTime = System.currentTimeMillis()))
            Result.success(tracks.size)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        }
    }
}
