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

@Entity(tableName = "recent_programs")
data class RecentProgramEntity(
    @PrimaryKey val id: String,
    val programType: String, // "MOVIE", "TV_SHOW", "EPISODE", "MUSIC"
    val title: String,
    val subtitle: String = "",
    val coverUrl: String = "",
    val bannerUrl: String = "",
    val duration: Long = 0L, // in milliseconds
    val progress: Long = 0L, // in milliseconds
    val lastPlayed: Long = System.currentTimeMillis(),
    val streamUrl: String = "",
    val ratingKey: String = "",
    val serverId: String = "",
    val showTitle: String = "",
    val seasonNumber: Int = 0,
    val episodeNumber: Int = 0
)

data class PlexCastMember(
    val id: String = "",
    val name: String,
    val role: String = "Actor", // e.g., "Actor", "Director", "Writer", "Producer", "Executive Producer", "Cinematographer"
    val character: String = "",
    val thumbUrl: String = "",
    val bio: String = ""
)

data class PlexEpisodeItem(
    val id: String,
    val ratingKey: String,
    val showTitle: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val summary: String = "",
    val duration: Long = 0L, // ms
    val airDate: String = "",
    val coverUrl: String = "",
    val videoUrl: String = "",
    val serverId: String = "",
    val directors: List<PlexCastMember> = emptyList(),
    val writers: List<PlexCastMember> = emptyList(),
    val cast: List<PlexCastMember> = emptyList(),
    val producers: List<PlexCastMember> = emptyList(),
    val lastPlayed: Long = 0L,
    val progress: Long = 0L
)

data class PlexSeasonItem(
    val id: String,
    val ratingKey: String,
    val showTitle: String,
    val seasonNumber: Int,
    val title: String,
    val summary: String = "",
    val coverUrl: String = "",
    val episodeCount: Int = 0,
    val episodes: List<PlexEpisodeItem> = emptyList(),
    val cast: List<PlexCastMember> = emptyList()
)

data class PlexShowItem(
    val id: String,
    val ratingKey: String,
    val title: String,
    val originalTitle: String = "",
    val summary: String = "",
    val year: Int? = null,
    val rating: Float? = null,
    val contentRating: String = "", // e.g. "TV-MA", "TV-14", "PG-13"
    val studio: String = "",
    val genres: List<String> = emptyList(),
    val coverUrl: String = "",
    val bannerUrl: String = "",
    val serverId: String = "",
    val seasons: List<PlexSeasonItem> = emptyList(),
    val cast: List<PlexCastMember> = emptyList(),
    val directors: List<PlexCastMember> = emptyList(),
    val producers: List<PlexCastMember> = emptyList(),
    val writers: List<PlexCastMember> = emptyList(),
    val cinematographers: List<PlexCastMember> = emptyList(),
    val similarTitles: List<String> = emptyList(),
    val lastPlayed: Long = 0L,
    val leafCount: Int = 0,
    val childCount: Int = 0,
    val addedAt: Long = 0L
)

data class PlexMovieItem(
    val id: String,
    val ratingKey: String,
    val title: String,
    val originalTitle: String = "",
    val tagline: String = "",
    val summary: String = "",
    val year: Int? = null,
    val rating: Float? = null,
    val contentRating: String = "", // e.g. "PG-13", "R", "PG"
    val duration: Long = 0L, // ms
    val studio: String = "",
    val genres: List<String> = emptyList(),
    val coverUrl: String = "",
    val bannerUrl: String = "",
    val videoUrl: String = "",
    val serverId: String = "",
    val cast: List<PlexCastMember> = emptyList(),
    val directors: List<PlexCastMember> = emptyList(),
    val writers: List<PlexCastMember> = emptyList(),
    val producers: List<PlexCastMember> = emptyList(),
    val cinematographers: List<PlexCastMember> = emptyList(),
    val similarTitles: List<String> = emptyList(),
    val lastPlayed: Long = 0L,
    val progress: Long = 0L,
    val addedAt: Long = 0L
)


