package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.network.OptimizedNetworkEngine

class MyApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        OptimizedNetworkEngine.initialize(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient { OptimizedNetworkEngine.client }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 25% available app memory for instant image rendering
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_image_cache"))
                    .maxSizeBytes(250 * 1024 * 1024L) // 250MB image disk cache
                    .build()
            }
            .respectCacheHeaders(false) // Force cache cover arts even if server headers omit explicit max-age
            .build()
    }
}


