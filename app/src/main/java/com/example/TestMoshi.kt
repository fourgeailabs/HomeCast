package com.example

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TestBook(
    val id: String,
    val title: String? = null
)

fun testMoshiParser() {
    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    val adapter = moshi.adapter(TestBook::class.java)
    try {
        val b = adapter.fromJson("{\"id\": 123, \"title\": \"Hello\"}")
        println("Success: $b")
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}
