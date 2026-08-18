package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM audiobooks ORDER BY lastPlayed DESC")
    fun getAllBooks(): Flow<List<Audiobook>>

    @Query("SELECT * FROM audiobooks WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<Audiobook>>

    @Query("SELECT * FROM audiobooks ORDER BY lastPlayed DESC LIMIT 10")
    fun getRecents(): Flow<List<Audiobook>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Audiobook)

    @Query("UPDATE audiobooks SET progress = :progress, lastPlayed = :timestamp WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Long, timestamp: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(serverConfig: ServerConfig)

    @Query("SELECT * FROM servers")
    fun getServers(): Flow<List<ServerConfig>>
}
