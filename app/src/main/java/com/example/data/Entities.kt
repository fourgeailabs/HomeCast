package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audiobooks")
data class Audiobook(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val duration: Long, // in seconds or milliseconds
    val progress: Long = 0L,
    val isFavorite: Boolean = false,
    val lastPlayed: Long = 0L,
    val serverId: String,
    val isDownloaded: Boolean = false,
    val streamUrl: String = "",
    val seriesName: String = "",
    val narrator: String = "",
    val genre: String = "Various"
)

@Entity(tableName = "music_tracks")
data class MusicTrack(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String,
    val duration: Long, // in milliseconds
    val serverId: String,
    val streamUrl: String,
    val ratingKey: String = "",
    val lastPlayed: Long = 0L,
    val genre: String = "Various",
    val trackNumber: Int = 1
)

@Entity(tableName = "ebooks")
data class EBook(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val serverId: String,
    val genre: String = "Various",
    val description: String = "",
    val totalPages: Int = 0,
    val progressPercent: Int = 0,
    val lastRead: Long = 0L,
    val downloadUrl: String = "",
    val isComic: Boolean = false
)

data class ServerConfig(
    val id: String, // e.g. "abs_server" or "plex_server" or uuid
    val name: String,
    val type: String, // "audiobookshelf", "plex", "booklore"
    val hostUrl: String,
    val apiKey: String = "",
    val username: String = "",
    val password: String = "",
    val isConnected: Boolean = false,
    val lastSyncTime: Long = 0L
)

@Entity(tableName = "public_domain_sources")
data class PublicDomainSource(
    @PrimaryKey val id: String,
    val name: String,
    val originalUrl: String,
    val verifiedUrl: String,
    val mediaTypes: String, // e.g. "AUDIOBOOK,EBOOK,MUSIC,COMIC"
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false,
    val aiExplanation: String = "",
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_folders")
data class LocalFolderConfig(
    @PrimaryKey val id: String,
    val mediaType: String, // "AUDIOBOOK", "EBOOK", "MUSIC"
    val folderPath: String, // DocumentTree URI string or file path
    val displayName: String,
    val isEnabled: Boolean = true,
    val fileCount: Int = 0,
    val lastScanned: Long = 0L
)

