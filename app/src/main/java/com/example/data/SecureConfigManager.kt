package com.example.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.ServerConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SecureConfigManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_server_config.xml",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, ServerConfig::class.java)
    private val adapter = moshi.adapter<List<ServerConfig>>(listType)

    private val _serversFlow = MutableStateFlow<List<ServerConfig>>(emptyList())
    val serversFlow: StateFlow<List<ServerConfig>> = _serversFlow.asStateFlow()

    init {
        loadServers()
    }

    private fun loadServers() {
        val json = sharedPreferences.getString("servers", null)
        if (json != null) {
            try {
                val servers = adapter.fromJson(json) ?: emptyList()
                _serversFlow.value = servers
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reloadServers() {
        val json = sharedPreferences.getString("servers", null)
        if (json != null) {
            try {
                val servers = adapter.fromJson(json) ?: emptyList()
                _serversFlow.value = servers
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            _serversFlow.value = emptyList()
        }
    }

    fun saveServer(server: ServerConfig) {
        val currentServers = _serversFlow.value.toMutableList()
        val index = currentServers.indexOfFirst { it.id == server.id }
        if (index != -1) {
            currentServers[index] = server
        } else {
            currentServers.add(server)
        }
        
        val json = adapter.toJson(currentServers)
        sharedPreferences.edit().putString("servers", json).apply()
        _serversFlow.value = currentServers
    }

    fun removeServer(serverId: String) {
        val currentServers = _serversFlow.value.filter { it.id != serverId }
        val json = adapter.toJson(currentServers)
        sharedPreferences.edit().putString("servers", json).apply()
        _serversFlow.value = currentServers
    }

    fun getLastCleanupDate(): String? {
        return sharedPreferences.getString("last_cleanup_date", null)
    }

    fun saveLastCleanupDate(dateStr: String) {
        sharedPreferences.edit().putString("last_cleanup_date", dateStr).apply()
    }
}
