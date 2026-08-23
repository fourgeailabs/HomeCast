package com.example

import android.app.Application
import android.content.Intent
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import okhttp3.OkHttpClient

class MyApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            }
            .build()
    }
}

