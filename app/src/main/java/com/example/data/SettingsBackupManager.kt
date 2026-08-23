package com.example.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

data class MediaProgress(
    val id: String,
    val type: String, // "EBOOK", "COMIC", "AUDIOBOOK", "MUSIC"
    val title: String,
    val creator: String = "",
    val currentPosition: Long = 0L, // ms for audio/music
    val currentChapter: Int = 0,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val progressPercent: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class BackupPayload(
    val version: Int = 2,
    val backupDate: Long = System.currentTimeMillis(),
    val servers: List<ServerConfig> = emptyList(),
    val publicDomainSources: List<PublicDomainSource> = emptyList(),
    val localFolders: List<LocalFolderConfig> = emptyList(),
    val mediaProgressList: List<MediaProgress> = emptyList(),
    val lastPlaybackPrefs: Map<String, String> = emptyMap()
)

class SettingsBackupManager(private val context: Context) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(BackupPayload::class.java)

    // Internal private backup file guaranteed to always succeed
    private fun getInternalBackupFile(): File {
        return File(context.filesDir, "homecast_backup.json")
    }

    // Public silent backup location in the public Downloads directory
    private fun getSilentBackupFile(): File {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadDir, "homecast_backup.json")
    }

    // Save individual media progress (EBook, Comic, Audiobook, Music) to JSON file
    @Synchronized
    fun saveMediaProgress(progress: MediaProgress, servers: List<ServerConfig> = emptyList()) {
        try {
            val currentPayload = loadCurrentPayload()
            val updatedList = currentPayload.mediaProgressList.filter { it.id != progress.id }.toMutableList()
            updatedList.add(progress)

            val playbackPrefs = context.getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)
            val editor = playbackPrefs.edit()
            editor.putInt("ebook_page_${progress.id}", progress.currentPage)
            editor.putInt("ebook_chapter_${progress.id}", progress.currentChapter)
            editor.putInt("ebook_progress_${progress.id}", progress.progressPercent)
            editor.putLong("media_pos_${progress.id}", progress.currentPosition)
            editor.putString("last_media_id", progress.id)
            editor.putString("last_media_type", progress.type)
            editor.putString("last_media_title", progress.title)
            editor.putString("last_media_creator", progress.creator)
            editor.apply()

            val prefsMap = mutableMapOf<String, String>()
            playbackPrefs.all.forEach { (key, value) ->
                if (value != null) prefsMap[key] = value.toString()
            }

            val finalServers = if (servers.isNotEmpty()) servers else currentPayload.servers

            val updatedPayload = currentPayload.copy(
                backupDate = System.currentTimeMillis(),
                servers = finalServers,
                mediaProgressList = updatedList,
                lastPlaybackPrefs = prefsMap
            )

            val json = adapter.toJson(updatedPayload)

            // Write internal guaranteed file
            val internalFile = getInternalBackupFile()
            internalFile.writeText(json, Charsets.UTF_8)

            // Attempt public Downloads write
            try {
                val publicFile = getSilentBackupFile()
                publicFile.parentFile?.mkdirs()
                publicFile.writeText(json, Charsets.UTF_8)
                android.util.Log.d("SettingsBackupManager", "JSON backup updated at ${publicFile.absolutePath} for ${progress.title} (Page ${progress.currentPage + 1})")
            } catch (e: Exception) {
                // Scoped storage fallback
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsBackupManager", "Failed to save media progress to JSON", e)
        }
    }

    // Retrieve saved progress for a specific media item from JSON / prefs
    fun getMediaProgress(id: String): MediaProgress? {
        try {
            val payload = loadCurrentPayload()
            val match = payload.mediaProgressList.firstOrNull { it.id == id }
            if (match != null) return match

            // Fallback to shared preferences
            val playbackPrefs = context.getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)
            val page = playbackPrefs.getInt("ebook_page_$id", -1)
            val chapter = playbackPrefs.getInt("ebook_chapter_$id", -1)
            val progressPercent = playbackPrefs.getInt("ebook_progress_$id", 0)
            val pos = playbackPrefs.getLong("media_pos_$id", 0L)
            if (page >= 0 || chapter >= 0 || pos > 0) {
                return MediaProgress(
                    id = id,
                    type = "MEDIA",
                    title = id,
                    currentPosition = pos,
                    currentChapter = if (chapter >= 0) chapter else 0,
                    currentPage = if (page >= 0) page else 0,
                    progressPercent = progressPercent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // Load current active payload from internal or public storage
    fun loadCurrentPayload(): BackupPayload {
        try {
            val internalFile = getInternalBackupFile()
            if (internalFile.exists() && internalFile.length() > 0) {
                val json = internalFile.readText(Charsets.UTF_8)
                val payload = adapter.fromJson(json)
                if (payload != null) return payload
            }

            val publicFile = getSilentBackupFile()
            if (publicFile.exists() && publicFile.length() > 0) {
                val json = publicFile.readText(Charsets.UTF_8)
                val payload = adapter.fromJson(json)
                if (payload != null) return payload
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return BackupPayload()
    }

    // Export payload to a SAF Uri
    fun exportToUri(uri: Uri, servers: List<ServerConfig>): Boolean {
        try {
            val currentPayload = loadCurrentPayload()
            val playbackPrefs = context.getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)
            val prefsMap = mutableMapOf<String, String>()
            playbackPrefs.all.forEach { (key, value) ->
                if (value != null) {
                    prefsMap[key] = value.toString()
                }
            }

            val payload = currentPayload.copy(
                backupDate = System.currentTimeMillis(),
                servers = servers,
                lastPlaybackPrefs = prefsMap
            )
            val json = adapter.toJson(payload)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray(Charsets.UTF_8))
            }
            
            // Also update the silent automatic backup
            saveSilentBackup(servers, prefsMap)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // Import payload from a SAF Uri
    fun importFromUri(uri: Uri, secureConfigManager: SecureConfigManager): Boolean {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: return false

            val payload = adapter.fromJson(json) ?: return false

            // Save servers
            payload.servers.forEach { server ->
                secureConfigManager.saveServer(server)
            }

            // Save preferences
            val playbackPrefs = context.getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)
            val editor = playbackPrefs.edit()
            payload.lastPlaybackPrefs.forEach { (key, value) ->
                editor.putString(key, value)
            }
            editor.apply()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // Silently save a backup to /sdcard/Download/homecast_backup.json
    fun saveSilentBackup(servers: List<ServerConfig>, prefsMap: Map<String, String> = emptyMap()) {
        try {
            val file = getSilentBackupFile()
            file.parentFile?.mkdirs()

            val actualPrefsMap = if (prefsMap.isEmpty()) {
                val playbackPrefs = context.getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)
                val map = mutableMapOf<String, String>()
                playbackPrefs.all.forEach { (key, value) ->
                    if (value != null) {
                        map[key] = value.toString()
                    }
                }
                map
            } else {
                prefsMap
            }

            val payload = BackupPayload(
                servers = servers,
                lastPlaybackPrefs = actualPrefsMap
            )
            val json = adapter.toJson(payload)
            file.writeText(json, Charsets.UTF_8)
            android.util.Log.d("SettingsBackupManager", "Silent backup auto-saved to ${file.absolutePath}")
        } catch (e: Exception) {
            // Silently skip if write permission is restricted on modern Scoped Storage
            android.util.Log.w("SettingsBackupManager", "Silent auto-backup skipped/failed: ${e.message}")
        }
    }

    // Check if a silent backup is available in public Downloads
    fun hasSilentBackup(): Boolean {
        return try {
            val file = getSilentBackupFile()
            file.exists() && file.length() > 0
        } catch (e: Exception) {
            false
        }
    }

    // Load servers silently from the silent backup file
    fun loadSilentBackup(secureConfigManager: SecureConfigManager): Boolean {
        try {
            val file = getSilentBackupFile()
            if (!file.exists()) return false

            val json = file.readText(Charsets.UTF_8)
            val payload = adapter.fromJson(json) ?: return false

            // Save servers
            payload.servers.forEach { server ->
                secureConfigManager.saveServer(server)
            }

            // Save preferences
            val playbackPrefs = context.getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)
            val editor = playbackPrefs.edit()
            payload.lastPlaybackPrefs.forEach { (key, value) ->
                editor.putString(key, value)
            }
            editor.apply()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
