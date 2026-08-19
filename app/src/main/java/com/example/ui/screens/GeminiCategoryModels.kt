package com.example.ui.screens

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiDiscoveryResponse(
    val items: List<GeminiDiscoveryItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GeminiDiscoveryItem(
    val title: String = "",
    val creator: String = "",
    val mediaType: String = "",
    val genre: String = "",
    val description: String = "",
    val reason: String = "",
    val coverUrl: String = ""
)
