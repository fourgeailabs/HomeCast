package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.Content
import com.example.GenerateContentRequest
import com.example.Part
import com.example.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object LocalMediaEnricher {

    suspend fun enrichMediaItems(
        audiobooks: List<Audiobook>,
        ebooks: List<EBook>,
        musicTracks: List<MusicTrack>,
        dao: LibraryDao
    ): Int {
        return withContext(Dispatchers.IO) {
            var totalEnriched = 0

            // 1. Enrich Local E-Books & Comics
            if (ebooks.isNotEmpty()) {
                val chunks = ebooks.chunked(6)
                for (chunk in chunks) {
                    val promptBuilder = StringBuilder()
                    promptBuilder.append("You are the HomeCast Local Media Metadata & Biography AI.\n")
                    promptBuilder.append("Given the following local book/comic files, please:\n")
                    promptBuilder.append("1. Fix and clean the raw title into proper title-cased English (e.g., 'the_time_machine_wells.epub' -> 'The Time Machine').\n")
                    promptBuilder.append("2. Extract or locate the true Author/Artist name (e.g. 'H.G. Wells').\n")
                    promptBuilder.append("3. Locate or write a rich, compelling 2-3 sentence literary synopsis / biography of the book.\n")
                    promptBuilder.append("4. Provide a high-quality cover art image URL or verified OpenLibrary/Internet Archive cover URL if known (otherwise return a clean OpenLibrary ISBN/title search image URL, e.g., 'https://covers.openlibrary.org/b/id/10523366-L.jpg' or leave empty).\n")
                    promptBuilder.append("5. Assign a high-level genre (e.g. 'Sci-Fi', 'Classic', 'Fantasy', 'Comic', 'Manga').\n\n")
                    promptBuilder.append("Input Items:\n")
                    chunk.forEach { item ->
                        promptBuilder.append("ID: ${item.id} | Raw Title: ${item.title} | Current Author: ${item.author}\n")
                    }
                    promptBuilder.append("\nReturn ONLY a valid JSON array of objects with keys: id, cleanedTitle, cleanedAuthor, synopsis, genre, coverUrl.")

                    try {
                        val request = GenerateContentRequest(
                            contents = listOf(Content(parts = listOf(Part(text = promptBuilder.toString()))))
                        )
                        val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
                        val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                        if (rawText.isNotBlank()) {
                            val cleanJson = rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                            val sIdx = cleanJson.indexOf("[")
                            val eIdx = cleanJson.lastIndexOf("]")
                            if (sIdx != -1 && eIdx != -1) {
                                val jsonArr = org.json.JSONArray(cleanJson.substring(sIdx, eIdx + 1))
                                val updated = mutableListOf<EBook>()
                                for (i in 0 until jsonArr.length()) {
                                    val obj = jsonArr.getJSONObject(i)
                                    val id = obj.optString("id")
                                    val original = chunk.find { it.id == id }
                                    if (original != null) {
                                        val title = obj.optString("cleanedTitle", original.title)
                                        val author = obj.optString("cleanedAuthor", original.author)
                                        val synopsis = obj.optString("synopsis", original.description)
                                        val genre = obj.optString("genre", original.genre)
                                        val cover = obj.optString("coverUrl", original.coverUrl)
                                        updated.add(
                                            original.copy(
                                                title = title.ifBlank { original.title },
                                                author = author.ifBlank { original.author },
                                                description = synopsis.ifBlank { original.description },
                                                genre = genre.ifBlank { original.genre },
                                                coverUrl = if (cover.isNotBlank()) cover else original.coverUrl
                                            )
                                        )
                                        totalEnriched++
                                    }
                                }
                                if (updated.isNotEmpty()) {
                                    dao.insertEBooks(updated)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("LocalMediaEnricher", "E-Book enrichment failed", e)
                    }
                }
            }

            // 2. Enrich Local Audiobooks
            if (audiobooks.isNotEmpty()) {
                val chunks = audiobooks.chunked(6)
                for (chunk in chunks) {
                    val promptBuilder = StringBuilder()
                    promptBuilder.append("You are the HomeCast Local Audio Metadata & Biography AI.\n")
                    promptBuilder.append("Given the following local audiobook audio files, please:\n")
                    promptBuilder.append("1. Fix and clean the raw title into proper title-cased English.\n")
                    promptBuilder.append("2. Extract or locate the true Author/Narrator name.\n")
                    promptBuilder.append("3. Locate or write a rich, compelling 2-sentence synopsis / author biography.\n")
                    promptBuilder.append("4. Provide a high-quality cover art image URL if known.\n")
                    promptBuilder.append("5. Assign a high-level genre.\n\n")
                    promptBuilder.append("Input Items:\n")
                    chunk.forEach { item ->
                        promptBuilder.append("ID: ${item.id} | Raw Title: ${item.title} | Current Author: ${item.author}\n")
                    }
                    promptBuilder.append("\nReturn ONLY a valid JSON array of objects with keys: id, cleanedTitle, cleanedAuthor, narrator, genre, coverUrl.")

                    try {
                        val request = GenerateContentRequest(
                            contents = listOf(Content(parts = listOf(Part(text = promptBuilder.toString()))))
                        )
                        val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
                        val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                        if (rawText.isNotBlank()) {
                            val cleanJson = rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                            val sIdx = cleanJson.indexOf("[")
                            val eIdx = cleanJson.lastIndexOf("]")
                            if (sIdx != -1 && eIdx != -1) {
                                val jsonArr = org.json.JSONArray(cleanJson.substring(sIdx, eIdx + 1))
                                val updated = mutableListOf<Audiobook>()
                                for (i in 0 until jsonArr.length()) {
                                    val obj = jsonArr.getJSONObject(i)
                                    val id = obj.optString("id")
                                    val original = chunk.find { it.id == id }
                                    if (original != null) {
                                        val title = obj.optString("cleanedTitle", original.title)
                                        val author = obj.optString("cleanedAuthor", original.author)
                                        val narrator = obj.optString("narrator", original.narrator)
                                        val genre = obj.optString("genre", original.genre)
                                        val cover = obj.optString("coverUrl", original.coverUrl)
                                        updated.add(
                                            original.copy(
                                                title = title.ifBlank { original.title },
                                                author = author.ifBlank { original.author },
                                                narrator = narrator.ifBlank { original.narrator },
                                                genre = genre.ifBlank { original.genre },
                                                coverUrl = if (cover.isNotBlank()) cover else original.coverUrl
                                            )
                                        )
                                        totalEnriched++
                                    }
                                }
                                if (updated.isNotEmpty()) {
                                    dao.insertBooks(updated)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("LocalMediaEnricher", "Audiobook enrichment failed", e)
                    }
                }
            }

            // 3. Enrich Local Music
            if (musicTracks.isNotEmpty()) {
                val chunks = musicTracks.chunked(8)
                for (chunk in chunks) {
                    val promptBuilder = StringBuilder()
                    promptBuilder.append("You are the HomeCast Local Music Tag & Biography AI.\n")
                    promptBuilder.append("Given the following local music audio tracks, please:\n")
                    promptBuilder.append("1. Clean the track title, removing track numbers, extensions, and rip labels.\n")
                    promptBuilder.append("2. Clean or locate the true Artist/Band name.\n")
                    promptBuilder.append("3. Clean or locate the Album name.\n")
                    promptBuilder.append("4. Assign a clean musical genre (e.g. 'Rock', 'Electronic', 'Jazz', 'Classical', 'Hip-Hop', 'Pop').\n")
                    promptBuilder.append("5. Provide a cover art URL if known.\n\n")
                    promptBuilder.append("Input Items:\n")
                    chunk.forEach { item ->
                        promptBuilder.append("ID: ${item.id} | Raw Title: ${item.title} | Current Artist: ${item.artist} | Current Album: ${item.album}\n")
                    }
                    promptBuilder.append("\nReturn ONLY a valid JSON array of objects with keys: id, cleanedTitle, cleanedArtist, cleanedAlbum, genre, coverUrl.")

                    try {
                        val request = GenerateContentRequest(
                            contents = listOf(Content(parts = listOf(Part(text = promptBuilder.toString()))))
                        )
                        val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
                        val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                        if (rawText.isNotBlank()) {
                            val cleanJson = rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                            val sIdx = cleanJson.indexOf("[")
                            val eIdx = cleanJson.lastIndexOf("]")
                            if (sIdx != -1 && eIdx != -1) {
                                val jsonArr = org.json.JSONArray(cleanJson.substring(sIdx, eIdx + 1))
                                val updated = mutableListOf<MusicTrack>()
                                for (i in 0 until jsonArr.length()) {
                                    val obj = jsonArr.getJSONObject(i)
                                    val id = obj.optString("id")
                                    val original = chunk.find { it.id == id }
                                    if (original != null) {
                                        val title = obj.optString("cleanedTitle", original.title)
                                        val artist = obj.optString("cleanedArtist", original.artist)
                                        val album = obj.optString("cleanedAlbum", original.album)
                                        val genre = obj.optString("genre", original.genre)
                                        val cover = obj.optString("coverUrl", original.coverUrl)
                                        updated.add(
                                            original.copy(
                                                title = title.ifBlank { original.title },
                                                artist = artist.ifBlank { original.artist },
                                                album = album.ifBlank { original.album },
                                                genre = genre.ifBlank { original.genre },
                                                coverUrl = if (cover.isNotBlank()) cover else original.coverUrl
                                            )
                                        )
                                        totalEnriched++
                                    }
                                }
                                if (updated.isNotEmpty()) {
                                    dao.insertMusicTracks(updated)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("LocalMediaEnricher", "Music enrichment failed", e)
                    }
                }
            }

            totalEnriched
        }
    }
}
