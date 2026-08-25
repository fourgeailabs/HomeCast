package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.Content
import com.example.GenerateContentRequest
import com.example.Part
import com.example.RetrofitClient
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class PersonalizedRecommendation(
    val id: String = "",
    val title: String = "",
    val creator: String = "",
    val mediaType: String = "MUSIC", // MUSIC, AUDIOBOOK, EBOOK, COMIC, MOVIE, SHOW
    val genre: String = "",
    val vibeTag: String = "", // e.g. "Late Night Focus", "Acoustic Warmth", "Cosmic Wonder"
    val becauseYouPlayed: String = "", // e.g. "Because you recently played 'Clair de Lune'"
    val aiRationale: String = "", // e.g. "Shares lush impressionist harmonies and calming dynamic texture."
    val suggestedAction: String = "PLAY", // PLAY, LISTEN, READ, WATCH
    val coverUrl: String = "",
    val streamOrReadUrl: String = "",
    val durationSeconds: Long = 0L,
    val totalPages: Int = 150
)

@JsonClass(generateAdapter = true)
data class GeminiRecommendationResponse(
    val recommendations: List<PersonalizedRecommendation> = emptyList()
)

object GeminiPersonalizedEngine {
    private const val TAG = "GeminiPersonalized"

    suspend fun generateRecommendations(
        recentMusic: List<MusicTrack>,
        recentBooks: List<Audiobook>,
        recentPrograms: List<RecentProgramEntity>,
        allMusic: List<MusicTrack>,
        allBooks: List<Audiobook>,
        allEBooks: List<EBook>,
        plexMovies: List<PlexMovieItem> = emptyList(),
        plexShows: List<PlexShowItem> = emptyList()
    ): List<PersonalizedRecommendation> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        // Collect user's recent listening/viewing history
        val recentMusicSummary = recentMusic.take(6).joinToString("; ") { "'${it.title}' by ${it.artist} (${it.genre})" }
        val recentBooksSummary = recentBooks.take(6).joinToString("; ") { "'${it.title}' by ${it.author} (${it.genre})" }
        val recentProgramsSummary = recentPrograms.take(6).joinToString("; ") { "'${it.title}' (${it.programType})" }

        val hasHistory = recentMusicSummary.isNotBlank() || recentBooksSummary.isNotBlank() || recentProgramsSummary.isNotBlank()

        // Available library snapshot to ground recommendations
        val libraryMusicSnippet = allMusic.shuffled().take(12).joinToString("; ") { "${it.title} by ${it.artist} [${it.genre}]" }
        val libraryBooksSnippet = allBooks.shuffled().take(8).joinToString("; ") { "${it.title} by ${it.author} [${it.genre}]" }
        val libraryEBooksSnippet = allEBooks.shuffled().take(8).joinToString("; ") { "${it.title} by ${it.author} [${it.genre}]" }
        val libraryMoviesSnippet = plexMovies.shuffled().take(6).joinToString("; ") { "${it.title} (${it.year ?: ""})" }

        if (apiKey.isBlank()) {
            return@withContext generateFallbackRecommendations(
                recentMusic, recentBooks, recentPrograms, allMusic, allBooks, allEBooks, plexMovies
            )
        }

        val prompt = """
            You are the HomeCast Gemini AI Personal Media Curator.
            Generate 6 to 8 highly personalized, intelligent media recommendations tailored directly to the user's taste and recently played media history.

            User's Recently Played Media History:
            - Recently Played Music: ${recentMusicSummary.ifBlank { "None yet (New listener)" }}
            - Recently Listened Audiobooks: ${recentBooksSummary.ifBlank { "None yet" }}
            - Recently Watched Movies / Series: ${recentProgramsSummary.ifBlank { "None yet" }}

            Active Connected User Library & Catalog Highlights:
            - Available Music: ${libraryMusicSnippet.ifBlank { "Debussy Clair de Lune; Miles Davis Autumn Leaves; Daft Punk Touch; Chopin Nocturne" }}
            - Available Audiobooks: ${libraryBooksSnippet.ifBlank { "The Time Machine by H.G. Wells; Sherlock Holmes by Arthur Conan Doyle; Frankenstein by Mary Shelley" }}
            - Available E-Books: ${libraryEBooksSnippet.ifBlank { "Pride and Prejudice by Jane Austen; Meditations by Marcus Aurelius; Dracula by Bram Stoker" }}
            - Available Movies/Shows: ${libraryMoviesSnippet.ifBlank { "Metropolis (1927); Night of the Living Dead; Voyage to the Moon" }}

            Instructions:
            1. Suggest 6-8 diverse recommendations across Music, Audiobooks, E-Books, and Movies.
            2. For each recommendation, reference a specific recent item in "becauseYouPlayed" (e.g., "Because you recently played 'Clair de Lune' by Claude Debussy" or "Because you love atmospheric sci-fi").
            3. Provide a thoughtful 1-2 sentence "aiRationale" explaining the musical, literary, or stylistic connection.
            4. Include a concise "vibeTag" (e.g. "Late Night Focus", "Sci-Fi Mystery", "Acoustic Warmth", "High Voltage").
            5. Return ONLY a valid JSON object matching this exact schema:
            {
              "recommendations": [
                {
                  "title": "Title of Media",
                  "creator": "Artist, Author, or Director",
                  "mediaType": "MUSIC", // Must be one of: MUSIC, AUDIOBOOK, EBOOK, COMIC, MOVIE, SHOW
                  "genre": "Genre name",
                  "vibeTag": "Vibe / Mood Name",
                  "becauseYouPlayed": "Because you listened to / watched ...",
                  "aiRationale": "Why this is recommended for your taste...",
                  "suggestedAction": "PLAY", // One of: PLAY, LISTEN, READ, WATCH
                  "coverUrl": "Representative high-quality artwork image URL (e.g. Unsplash / Wikimedia) or leave empty"
                }
              ]
            }
            Do not wrap with markdown blocks. Return valid JSON only.
        """.trimIndent()

        try {
            val req = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt))))
            )
            val res = RetrofitClient.service.generateContent(apiKey, req)
            var jsonStr = res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            jsonStr = jsonStr.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

            if (jsonStr.isNotBlank()) {
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(GeminiRecommendationResponse::class.java)
                val parsed = adapter.fromJson(jsonStr)
                val recs = parsed?.recommendations ?: emptyList()

                if (recs.isNotEmpty()) {
                    // Enrich with local stream URLs & high quality covers if matched in library
                    return@withContext recs.mapIndexed { index, rec ->
                        enrichRecommendation(rec, index, allMusic, allBooks, allEBooks, plexMovies)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini recommendation request failed, falling back", e)
        }

        // Fallback rule-based personalized generator
        generateFallbackRecommendations(
            recentMusic, recentBooks, recentPrograms, allMusic, allBooks, allEBooks, plexMovies
        )
    }

    private fun enrichRecommendation(
        rec: PersonalizedRecommendation,
        index: Int,
        allMusic: List<MusicTrack>,
        allBooks: List<Audiobook>,
        allEBooks: List<EBook>,
        plexMovies: List<PlexMovieItem>
    ): PersonalizedRecommendation {
        val uniqueId = "gemini_rec_${index}_${rec.title.hashCode()}"

        // Match against user's actual library items to enable direct instant playback
        when (rec.mediaType.uppercase()) {
            "MUSIC" -> {
                val matched = allMusic.firstOrNull { 
                    it.title.contains(rec.title, ignoreCase = true) || rec.title.contains(it.title, ignoreCase = true) ||
                    (it.artist.contains(rec.creator, ignoreCase = true) && it.artist.isNotBlank())
                }
                if (matched != null) {
                    return rec.copy(
                        id = uniqueId,
                        streamOrReadUrl = matched.streamUrl,
                        coverUrl = if (rec.coverUrl.isNotBlank()) rec.coverUrl else matched.coverUrl,
                        durationSeconds = matched.duration / 1000
                    )
                }
            }
            "AUDIOBOOK" -> {
                val matched = allBooks.firstOrNull { 
                    it.title.contains(rec.title, ignoreCase = true) || rec.title.contains(it.title, ignoreCase = true) ||
                    it.author.contains(rec.creator, ignoreCase = true)
                }
                if (matched != null) {
                    return rec.copy(
                        id = uniqueId,
                        streamOrReadUrl = matched.streamUrl,
                        coverUrl = if (rec.coverUrl.isNotBlank()) rec.coverUrl else matched.coverUrl,
                        durationSeconds = matched.duration
                    )
                }
            }
            "EBOOK" -> {
                val matched = allEBooks.firstOrNull {
                    it.title.contains(rec.title, ignoreCase = true) || rec.title.contains(it.title, ignoreCase = true) ||
                    it.author.contains(rec.creator, ignoreCase = true)
                }
                if (matched != null) {
                    return rec.copy(
                        id = uniqueId,
                        streamOrReadUrl = matched.downloadUrl,
                        coverUrl = if (rec.coverUrl.isNotBlank()) rec.coverUrl else matched.coverUrl,
                        totalPages = matched.totalPages
                    )
                }
            }
            "MOVIE", "SHOW" -> {
                val matched = plexMovies.firstOrNull {
                    it.title.contains(rec.title, ignoreCase = true) || rec.title.contains(it.title, ignoreCase = true)
                }
                if (matched != null) {
                    return rec.copy(
                        id = uniqueId,
                        streamOrReadUrl = matched.videoUrl,
                        coverUrl = if (rec.coverUrl.isNotBlank()) rec.coverUrl else matched.coverUrl,
                        durationSeconds = matched.duration / 1000
                    )
                }
            }
        }

        // Fallback default covers for visual aesthetics
        val defaultCover = when (rec.mediaType.uppercase()) {
            "MUSIC" -> "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
            "AUDIOBOOK" -> "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80"
            "EBOOK" -> "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&auto=format&fit=crop&q=80"
            "COMIC" -> "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600&auto=format&fit=crop&q=80"
            else -> "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&auto=format&fit=crop&q=80"
        }

        return rec.copy(
            id = uniqueId,
            coverUrl = rec.coverUrl.ifBlank { defaultCover }
        )
    }

    private fun generateFallbackRecommendations(
        recentMusic: List<MusicTrack>,
        recentBooks: List<Audiobook>,
        recentPrograms: List<RecentProgramEntity>,
        allMusic: List<MusicTrack>,
        allBooks: List<Audiobook>,
        allEBooks: List<EBook>,
        plexMovies: List<PlexMovieItem>
    ): List<PersonalizedRecommendation> {
        val list = mutableListOf<PersonalizedRecommendation>()

        // 1. Music Recommendation based on recent music
        val lastTrack = recentMusic.firstOrNull()
        if (lastTrack != null) {
            val sameGenreTrack = allMusic.firstOrNull { it.genre.equals(lastTrack.genre, ignoreCase = true) && it.id != lastTrack.id }
                ?: allMusic.firstOrNull { it.id != lastTrack.id }
            
            if (sameGenreTrack != null) {
                list.add(
                    PersonalizedRecommendation(
                        id = "rec_mus_${sameGenreTrack.id}",
                        title = sameGenreTrack.title,
                        creator = sameGenreTrack.artist,
                        mediaType = "MUSIC",
                        genre = sameGenreTrack.genre,
                        vibeTag = "Harmonic Sync",
                        becauseYouPlayed = "Because you played '${lastTrack.title}' by ${lastTrack.artist}",
                        aiRationale = "Matches the rhythmic cadence, genre texture, and mood of your recent listening session.",
                        suggestedAction = "PLAY",
                        coverUrl = sameGenreTrack.coverUrl.ifBlank { "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600" },
                        streamOrReadUrl = sameGenreTrack.streamUrl,
                        durationSeconds = sameGenreTrack.duration / 1000
                    )
                )
            }
        }

        // 2. Audiobook Recommendation based on recent audiobooks
        val lastBook = recentBooks.firstOrNull()
        if (lastBook != null) {
            val relatedBook = allBooks.firstOrNull { it.id != lastBook.id }
            if (relatedBook != null) {
                list.add(
                    PersonalizedRecommendation(
                        id = "rec_ab_${relatedBook.id}",
                        title = relatedBook.title,
                        creator = relatedBook.author,
                        mediaType = "AUDIOBOOK",
                        genre = relatedBook.genre,
                        vibeTag = "Immersive Narrative",
                        becauseYouPlayed = "Because you listened to '${lastBook.title}' by ${lastBook.author}",
                        aiRationale = "Deepens your journey into immersive storytelling with rich pacing and thematic resonance.",
                        suggestedAction = "LISTEN",
                        coverUrl = relatedBook.coverUrl.ifBlank { "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600" },
                        streamOrReadUrl = relatedBook.streamUrl,
                        durationSeconds = relatedBook.duration
                    )
                )
            }
        }

        // 3. E-Book Curated Pick
        val firstEBook = allEBooks.firstOrNull()
        if (firstEBook != null) {
            list.add(
                PersonalizedRecommendation(
                    id = "rec_eb_${firstEBook.id}",
                    title = firstEBook.title,
                    creator = firstEBook.author,
                    mediaType = "EBOOK",
                    genre = firstEBook.genre,
                    vibeTag = "Deep Reading",
                    becauseYouPlayed = if (lastBook != null) "Complements '${lastBook.title}'" else "Curated for mindful focus",
                    aiRationale = "A timeless literary work curated to enrich your personal digital library.",
                    suggestedAction = "READ",
                    coverUrl = firstEBook.coverUrl.ifBlank { "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600" },
                    streamOrReadUrl = firstEBook.downloadUrl,
                    totalPages = firstEBook.totalPages
                )
            )
        }

        // 4. Movie / Cinema Pick
        val firstMovie = plexMovies.firstOrNull()
        if (firstMovie != null) {
            list.add(
                PersonalizedRecommendation(
                    id = "rec_mov_${firstMovie.id}",
                    title = firstMovie.title,
                    creator = firstMovie.directors.firstOrNull()?.name ?: "Director",
                    mediaType = "MOVIE",
                    genre = firstMovie.genres.firstOrNull() ?: "Cinema",
                    vibeTag = "Cinematic Night",
                    becauseYouPlayed = "Synced from your Plex Media Server",
                    aiRationale = "Full-length cinematic experience ready to stream in high fidelity.",
                    suggestedAction = "WATCH",
                    coverUrl = firstMovie.coverUrl,
                    streamOrReadUrl = firstMovie.videoUrl,
                    durationSeconds = firstMovie.duration / 1000
                )
            )
        }

        // Curated Public Domain classics if library is still fresh
        if (list.size < 4) {
            list.add(
                PersonalizedRecommendation(
                    id = "rec_pd_1",
                    title = "Clair de Lune",
                    creator = "Claude Debussy",
                    mediaType = "MUSIC",
                    genre = "Classical & Impressionism",
                    vibeTag = "Peaceful Reflection",
                    becauseYouPlayed = "Recommended by Gemini for tranquility",
                    aiRationale = "Gentle, atmospheric piano textures designed for relaxation, creative work, and focus.",
                    suggestedAction = "PLAY",
                    coverUrl = "https://images.unsplash.com/photo-1520523839898-507127054976?w=600",
                    streamOrReadUrl = "https://ia800301.us.archive.org/15/items/78_clair-de-lune_victor-symphony-orchestra-charles-oconnell-debussy_gbia0001858a/01%20Clair%20De%20Lune.mp3",
                    durationSeconds = 300L
                )
            )
            list.add(
                PersonalizedRecommendation(
                    id = "rec_pd_2",
                    title = "The Time Machine",
                    creator = "H.G. Wells",
                    mediaType = "AUDIOBOOK",
                    genre = "Classic Sci-Fi",
                    vibeTag = "Time Traveler",
                    becauseYouPlayed = "Top recommended science fiction classic",
                    aiRationale = "Groundbreaking sci-fi journey to the year 802,701 AD exploring the Eloi and Morlocks.",
                    suggestedAction = "LISTEN",
                    coverUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600",
                    streamOrReadUrl = "https://ia800201.us.archive.org/34/items/time_machine_0710_librivox/timemachine_01_wells.mp3",
                    durationSeconds = 3600L
                )
            )
            list.add(
                PersonalizedRecommendation(
                    id = "rec_pd_3",
                    title = "Meditations",
                    creator = "Marcus Aurelius",
                    mediaType = "EBOOK",
                    genre = "Philosophy & Wisdom",
                    vibeTag = "Stoic Calm",
                    becauseYouPlayed = "Essential reading for mental clarity",
                    aiRationale = "Private journal reflections of the Roman Emperor on resilience, mindfulness, and purpose.",
                    suggestedAction = "READ",
                    coverUrl = "https://images.unsplash.com/photo-1506880018603-83d5b814b5a6?w=600",
                    streamOrReadUrl = "https://www.gutenberg.org/ebooks/2680.epub.images",
                    totalPages = 220
                )
            )
        }

        return list
    }
}
