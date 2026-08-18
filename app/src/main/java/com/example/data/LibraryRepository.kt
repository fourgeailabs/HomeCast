package com.example.data

import kotlinx.coroutines.flow.Flow

class LibraryRepository(private val dao: LibraryDao) {
    val allBooks: Flow<List<Audiobook>> = dao.getAllBooks()
    val favorites: Flow<List<Audiobook>> = dao.getFavorites()
    val recents: Flow<List<Audiobook>> = dao.getRecents()
    val servers: Flow<List<ServerConfig>> = dao.getServers()

    suspend fun insertBook(book: Audiobook) = dao.insertBook(book)
    suspend fun updateProgress(id: String, progress: Long) {
        dao.updateProgress(id, progress, System.currentTimeMillis())
    }
    suspend fun addServer(server: ServerConfig) = dao.insertServer(server)
}
