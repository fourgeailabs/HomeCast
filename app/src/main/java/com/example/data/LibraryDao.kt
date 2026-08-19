package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    // Audiobooks
    @Query("SELECT * FROM audiobooks ORDER BY lastPlayed DESC, title ASC")
    fun getAllBooks(): Flow<List<Audiobook>>

    @Query("SELECT * FROM audiobooks WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavorites(): Flow<List<Audiobook>>

    @Query("SELECT * FROM audiobooks WHERE lastPlayed > 0 ORDER BY lastPlayed DESC LIMIT 15")
    fun getRecents(): Flow<List<Audiobook>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<Audiobook>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Audiobook)

    @Query("DELETE FROM audiobooks WHERE serverId = :serverId")
    suspend fun deleteBooksByServer(serverId: String)

    @Query("UPDATE audiobooks SET progress = :progress, lastPlayed = :timestamp WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Long, timestamp: Long)

    @Query("UPDATE audiobooks SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    // Music Tracks (Plex)
    @Query("SELECT * FROM music_tracks ORDER BY artist ASC, album ASC, title ASC")
    fun getAllMusic(): Flow<List<MusicTrack>>

    @Query("SELECT * FROM music_tracks WHERE lastPlayed > 0 ORDER BY lastPlayed DESC LIMIT 15")
    fun getRecentMusic(): Flow<List<MusicTrack>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMusicTracks(tracks: List<MusicTrack>)

    @Query("DELETE FROM music_tracks WHERE serverId = :serverId")
    suspend fun deleteMusicByServer(serverId: String)

    @Query("UPDATE music_tracks SET lastPlayed = :timestamp WHERE id = :id")
    suspend fun updateMusicLastPlayed(id: String, timestamp: Long)

    // EBooks
    @Query("SELECT * FROM ebooks ORDER BY lastRead DESC, title ASC")
    fun getAllEBooks(): Flow<List<EBook>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEBooks(ebooks: List<EBook>)

    @Query("DELETE FROM ebooks WHERE serverId = :serverId")
    suspend fun deleteEBooksByServer(serverId: String)
}
