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

    @Query("SELECT * FROM ebooks WHERE id = :id LIMIT 1")
    suspend fun getEBookById(id: String): EBook?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEBooks(ebooks: List<EBook>)

    @Query("DELETE FROM ebooks WHERE serverId = :serverId")
    suspend fun deleteEBooksByServer(serverId: String)

    @Query("UPDATE ebooks SET progressPercent = :progressPercent, lastRead = :timestamp WHERE id = :id")
    suspend fun updateEBookProgress(id: String, progressPercent: Int, timestamp: Long)

    @Query("UPDATE ebooks SET progressPercent = :progressPercent, totalPages = :totalPages, lastRead = :timestamp WHERE id = :id")
    suspend fun updateEBookProgressAndPages(id: String, progressPercent: Int, totalPages: Int, timestamp: Long)

    // Public Domain Sources
    @Query("SELECT * FROM public_domain_sources ORDER BY isDefault DESC, dateAdded ASC")
    fun getAllPublicDomainSources(): Flow<List<PublicDomainSource>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPublicDomainSource(source: PublicDomainSource)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPublicDomainSources(sources: List<PublicDomainSource>)

    @Query("UPDATE public_domain_sources SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun togglePublicDomainSource(id: String, isEnabled: Boolean)

    @Query("DELETE FROM public_domain_sources WHERE id = :id")
    suspend fun deletePublicDomainSource(id: String)

    // Local Folder Configurations
    @Query("SELECT * FROM local_folders ORDER BY mediaType ASC, displayName ASC")
    fun getAllLocalFolders(): Flow<List<LocalFolderConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalFolder(folder: LocalFolderConfig)

    @Query("DELETE FROM local_folders WHERE id = :id")
    suspend fun deleteLocalFolder(id: String)

    @Query("UPDATE local_folders SET fileCount = :count, lastScanned = :timestamp WHERE id = :id")
    suspend fun updateFolderScanStatus(id: String, count: Int, timestamp: Long)
}
