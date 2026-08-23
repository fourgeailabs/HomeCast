package com.example

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.data.PlaybackManager
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val tokenInterceptor = okhttp3.Interceptor { chain ->
            val original = chain.request()
            val url = original.url
            val tokenParam = url.queryParameter("token") 
                ?: url.queryParameter("apiKey") 
                ?: url.queryParameter("access_token")
                ?: url.queryParameter("X-Plex-Token")

            val requestBuilder = original.newBuilder()
                .header("User-Agent", "HomeCast-Android/5.04")
                .header("Accept", "*/*")

            if (!tokenParam.isNullOrBlank()) {
                val cleanToken = if (tokenParam.startsWith("Bearer ", ignoreCase = true)) {
                    tokenParam.substring(7).trim()
                } else tokenParam.trim()

                if (original.header("Authorization") == null) {
                    requestBuilder.header("Authorization", "Bearer $cleanToken")
                }
                if (original.header("x-auth-token") == null) {
                    requestBuilder.header("x-auth-token", cleanToken)
                }
                if (original.header("X-Plex-Token") == null && (url.host.contains("plex", ignoreCase = true) || url.port == 32400 || url.queryParameter("X-Plex-Token") != null)) {
                    requestBuilder.header("X-Plex-Token", cleanToken)
                }
            }

            chain.proceed(requestBuilder.build())
        }

        // Create a permissive OkHttpClient for ExoPlayer to stream from personal servers with self-signed SSL or reverse proxies
        val permissiveOkHttpClient = try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())

            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .addInterceptor(tokenInterceptor)
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
        } catch (_: Exception) {
            OkHttpClient.Builder()
                .addInterceptor(tokenInterceptor)
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
        }

        val httpDataSourceFactory = OkHttpDataSource.Factory(permissiveOkHttpClient)
            .setUserAgent("HomeCast-Android/5.0")

        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaSession = MediaSession.Builder(this, player).build()
        PlaybackManager.setPlayer(player, mediaSession!!)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.player?.release()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
