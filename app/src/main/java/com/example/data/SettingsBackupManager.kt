package com.example.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

data class BackupPayload(
    val version: Int = 1,
    val backupDate: Long = System.currentTimeMillis(),
    val servers: List<ServerConfig> = emptyList(),
    val lastPlaybackPrefs: Map<String, String> = emptyMap()
)

class SettingsBackupManager(private val context: Context) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(BackupPayload::class.java)

    // Define the public silent backup location in the public Downloads directory
    private fun getSilentBackupFile(): File {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadDir, "homecast_backup.json")
    }

    // Export payload to a SAF Uri
    fun exportToUri(uri: Uri, servers: List<ServerConfig>): Boolean {
        try {
            val playbackPrefs = context.getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)
            val prefsMap = mutableMapOf<String, String>()
            playbackPrefs.all.forEach { (key, value) ->
                if (value != null) {
                    prefsMap[key] = value.toString()
                }
            }

            val payload = BackupPayload(
                servers = servers,
                lastPlaybackPrefs = prefsMap
            )
            val json = adapter.toJson(payload)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray(Charsets.UTF_8))
            }
            
            // Also try to update the silent automatic backup
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
