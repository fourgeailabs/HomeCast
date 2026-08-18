package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Audiobookshelf Models
@JsonClass(generateAdapter = true)
data class AbsLoginRequest(
    val username: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class AbsLoginResponse(
    val user: AbsUser? = null,
    val token: String? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class AbsUser(
    val id: String? = null,
    val username: String? = null,
    val token: String? = null
)

@JsonClass(generateAdapter = true)
data class AbsLibrariesResponse(
    val libraries: List<AbsLibrary>? = null
)

@JsonClass(generateAdapter = true)
data class AbsLibrary(
    val id: String,
    val name: String,
    val mediaType: String? = null
)

@JsonClass(generateAdapter = true)
data class AbsItemsResponse(
    val results: List<AbsItem>? = null
)

@JsonClass(generateAdapter = true)
data class AbsItem(
    val id: String,
    val media: AbsMedia? = null
)

@JsonClass(generateAdapter = true)
data class AbsMedia(
    val metadata: AbsMetadata? = null,
    val duration: Double? = null,
    val coverPath: String? = null,
    val audioFiles: List<AbsAudioFile>? = null
)

@JsonClass(generateAdapter = true)
data class AbsMetadata(
    val title: String? = null,
    val authorName: String? = null,
    val seriesName: String? = null,
    val narratorName: String? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class AbsAudioFile(
    val index: Int? = null,
    val ino: String? = null,
    val duration: Double? = null
)

// Plex Models
@JsonClass(generateAdapter = true)
data class PlexPinResponse(
    val id: Long? = null,
    val code: String? = null,
    val authToken: String? = null,
    val expiresAt: String? = null
)

@JsonClass(generateAdapter = true)
data class PlexSectionsResponse(
    @Json(name = "MediaContainer") val mediaContainer: PlexMediaContainer? = null
)

@JsonClass(generateAdapter = true)
data class PlexTracksResponse(
    @Json(name = "MediaContainer") val mediaContainer: PlexTracksContainer? = null
)

@JsonClass(generateAdapter = true)
data class PlexMediaContainer(
    val size: Int? = null,
    @Json(name = "Directory") val directory: List<PlexDirectory>? = null
)

@JsonClass(generateAdapter = true)
data class PlexDirectory(
    val key: String,
    val title: String,
    val type: String // "artist" or "movie" or "show"
)

@JsonClass(generateAdapter = true)
data class PlexTracksContainer(
    val size: Int? = null,
    @Json(name = "Metadata") val metadata: List<PlexTrackMetadata>? = null
)

@JsonClass(generateAdapter = true)
data class PlexTrackMetadata(
    val ratingKey: String,
    val key: String? = null,
    val title: String,
    val grandparentTitle: String? = null, // Artist
    val parentTitle: String? = null, // Album
    val thumb: String? = null,
    val parentThumb: String? = null,
    val grandparentThumb: String? = null,
    val duration: Long? = null,
    val index: Int? = null,
    val parentYear: Int? = null,
    @Json(name = "Genre") val genreList: List<PlexTagItem>? = null,
    @Json(name = "Media") val media: List<PlexMediaItem>? = null
)

@JsonClass(generateAdapter = true)
data class PlexTagItem(
    val tag: String? = null
)

@JsonClass(generateAdapter = true)
data class PlexMediaItem(
    @Json(name = "Part") val part: List<PlexPartItem>? = null
)

@JsonClass(generateAdapter = true)
data class PlexPartItem(
    val id: Long? = null,
    val key: String? = null,
    val duration: Long? = null
)
