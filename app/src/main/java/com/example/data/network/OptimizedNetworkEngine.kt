package com.example.data.network

import android.content.Context
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Optimized high-performance network engine for HomeCast.
 * Features:
 * - 120MB HTTP Disk Cache
 * - Aggressive socket reuse with 32 idle connection pool (10 min keepalive)
 * - HTTP/2 protocol negotiation
 * - Permissive SSL for self-hosted LAN servers (Plex, Jellyfin, Audiobookshelf, etc.)
 * - Sub-millisecond In-Memory LRU API Response Caching for fast UI rendering
 */
object OptimizedNetworkEngine {
    private const val TAG = "OptimizedNetworkEngine"

    private var appContext: Context? = null

    // In-memory LRU response cache for instant API queries (up to 200 entries)
    private val memoryCache = LruCache<String, CacheEntry>(200)

    data class CacheEntry(
        val data: String,
        val timestampMs: Long,
        val ttlMs: Long
    )

    fun initialize(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    val client: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()

        // 1. Configure SSL for self-hosted / reverse proxies / self-signed certs
        try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())
            builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up permissive SSL", e)
        }

        // 2. HTTP Disk Cache (120MB)
        appContext?.let { ctx ->
            try {
                val cacheDir = File(ctx.cacheDir, "homecast_http_cache")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                builder.cache(Cache(cacheDir, 120 * 1024 * 1024L))
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing OkHttp Disk Cache", e)
            }
        }

        // 3. Aggressive connection pooling (32 idle connections held for 10 minutes)
        builder.connectionPool(ConnectionPool(32, 10, TimeUnit.MINUTES))

        // 4. Protocols: HTTP/2 with fallback to HTTP/1.1
        builder.protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))

        // 5. High-speed performance timeouts
        builder.connectTimeout(6, TimeUnit.SECONDS)
        builder.readTimeout(10, TimeUnit.SECONDS)
        builder.writeTimeout(10, TimeUnit.SECONDS)
        builder.followRedirects(true)
        builder.followSslRedirects(true)

        // 6. User-Agent Header Interceptor
        builder.addInterceptor { chain ->
            val orig = chain.request()
            val req = orig.newBuilder()
                .header("User-Agent", "HomeCast-FastEngine/5.15 (Android; High-Performance)")
                .build()
            chain.proceed(req)
        }

        builder.build()
    }

    /**
     * Executes HTTP GET with sub-millisecond memory cache lookup.
     * If valid in cache, returns immediately without network hit.
     */
    suspend fun getCachedOrFetch(
        url: String,
        ttlMs: Long = 300_000L, // 5 minutes default TTL
        forceRefresh: Boolean = false
    ): String? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            val cached = memoryCache.get(url)
            if (cached != null && (now - cached.timestampMs) < cached.ttlMs) {
                return@withContext cached.data
            }
        }

        try {
            val req = Request.Builder().url(url).build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful && resp.body != null) {
                val bodyStr = resp.body!!.string()
                memoryCache.put(url, CacheEntry(bodyStr, now, ttlMs))
                return@withContext bodyStr
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching URL: $url", e)
        }

        // Fallback to stale memory cache if available on failure
        memoryCache.get(url)?.data
    }

    fun clearCache() {
        memoryCache.evictAll()
    }
}
