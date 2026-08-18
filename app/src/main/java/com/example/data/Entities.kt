package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audiobooks")
data class Audiobook(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val duration: Long,
    val progress: Long,
    val isFavorite: Boolean = false,
    val lastPlayed: Long = 0L,
    val serverId: String,
    val isDownloaded: Boolean = false
)

@Entity(tableName = "servers")
data class ServerConfig(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "audiobookshelf" or "plex"
    val localIp: String,
    val externalIp: String,
    val apiKey: String,
    val username: String = ""
)
