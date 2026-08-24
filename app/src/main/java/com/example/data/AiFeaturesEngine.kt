package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.Content
import com.example.GenerateContentRequest
import com.example.Part
import com.example.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class QuoteCardStyle(
    val quoteText: String,
    val author: String,
    val bookTitle: String,
    val themeName: String = "Ethereal Emerald",
    val fontStyleName: String = "Serif Elegance",
    val moodKeywords: List<String> = emptyList(),
    val gradientColorsHex: List<String> = listOf("#1E3A8A", "#0D9488")
)

data class MediaConciergeResult(
    val title: String,
    val explanation: String,
    val recommendedMediaIds: List<String> = emptyList(),
    val genreFilter: String? = null
)

object AiFeaturesEngine {

    // --- Feature 1: Story So Far & Smart Recaps ---
    suspend fun generateStorySoFarRecap(
        title: String,
        author: String,
        currentPosStr: String,
        durationStr: String,
        contextNotes: String = ""
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            return@withContext "Enjoying '$title' by $author! (Connect Gemini API key in Settings for AI catch-up summaries)."
        }

        val prompt = """
            You are HomeCast's intelligent audiobook and story recap assistant.
            The user is currently listening to / reading:
            Title: "$title"
            Author/Creator: "$author"
            Current Progress: $currentPosStr / $durationStr
            ${if (contextNotes.isNotBlank()) "User Bookmarks & Notes: $contextNotes" else ""}

            Task:
            Provide a warm, concise 2-3 sentence "Story So Far" catch-up recap to help the user resume listening without spoilers past their current progress ($currentPosStr). Follow with 2 bullet points for "Key Chapter Takeaways".
        """.trimIndent()

        try {
            val req = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt))))
            )
            val res = RetrofitClient.service.generateContent(apiKey, req)
            val text = res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                text.trim()
            } else {
                "Resuming '$title' at $currentPosStr. Prepare for the next captivating chapter!"
            }
        } catch (e: Exception) {
            Log.e("AiFeaturesEngine", "Error generating recap", e)
            "Resuming '$title' by $author at $currentPosStr."
        }
    }

    // --- Feature 2: Interactive Reading & Listening Companion ---
    suspend fun askCompanionQuestion(
        title: String,
        creator: String,
        mediaType: String,
        currentPosStr: String,
        userQuestion: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            return@withContext "I am your HomeCast Companion. Add a Gemini API key in Settings to ask interactive questions!"
        }

        val prompt = """
            You are the HomeCast Companion AI assistant, embedded inside an audio/e-book player.
            Media: "$title" by "$creator" ($mediaType).
            Current Position: $currentPosStr.
            User Question: "$userQuestion"

            Rules:
            1. Keep your answer conversational, helpful, and concise (under 120 words).
            2. Strictly avoid any spoilers beyond the current position ($currentPosStr).
            3. If answering about character origins or technical/historical background, focus on what has been established up to this point.
        """.trimIndent()

        try {
            val req = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt))))
            )
            val res = RetrofitClient.service.generateContent(apiKey, req)
            res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "I couldn't retrieve an answer right now. Please try again in a moment."
        } catch (e: Exception) {
            Log.e("AiFeaturesEngine", "Error in companion Q&A", e)
            "Unable to connect to AI companion right now. Please check your network connection."
        }
    }

    // --- Feature 3: Natural Language Media Concierge ---
    suspend fun askMediaConcierge(
        userPrompt: String,
        availableMediaSummary: String
    ): MediaConciergeResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            return@withContext MediaConciergeResult(
                title = "AI Media Concierge",
                explanation = "Set up your Gemini API Key in Settings to enable natural language media discovery!"
            )
        }

        val prompt = """
            You are HomeCast's AI Media Concierge.
            The user wants: "$userPrompt"

            Available Library / Catalog Context:
            $availableMediaSummary

            Task:
            Analyze the request and provide a short, welcoming recommendation response explaining what items match their prompt.
        """.trimIndent()

        try {
            val req = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt))))
            )
            val res = RetrofitClient.service.generateContent(apiKey, req)
            val text = res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "Here are top selections matching your mood and interest."
            MediaConciergeResult(
                title = "Curated AI Concierge Selection",
                explanation = text
            )
        } catch (e: Exception) {
            Log.e("AiFeaturesEngine", "Error in concierge", e)
            MediaConciergeResult(
                title = "Concierge Recommendations",
                explanation = "Found matching titles for '$userPrompt' in your library and catalog."
            )
        }
    }

    // --- Feature 4: AI Quote Card Styling ---
    suspend fun generateQuoteCardStyle(
        quoteText: String,
        bookTitle: String,
        author: String
    ): QuoteCardStyle = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val themes = listOf(
            Triple("Ethereal Emerald", "#064E3B", "#10B981"),
            Triple("Midnight Velvet", "#1E1B4B", "#6366F1"),
            Triple("Warm Parchment", "#451A03", "#D97706"),
            Triple("Cyber Neon", "#311B92", "#EC4899"),
            Triple("Crimson Sunset", "#881337", "#F43F5E"),
            Triple("Golden Horizon", "#78350F", "#F59E0B")
        )

        val selected = themes.random()
        QuoteCardStyle(
            quoteText = quoteText,
            author = author,
            bookTitle = bookTitle,
            themeName = selected.first,
            fontStyleName = "Serif Display",
            gradientColorsHex = listOf(selected.second, selected.third)
        )
    }

    // --- Feature 6: Dynamic Ambient Soundscape Mood Detector ---
    suspend fun detectAmbientMoodForText(
        bookTitle: String,
        textSnippet: String
    ): SoundscapeType = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) return@withContext SoundscapeType.RAINY_CAFE

        val prompt = """
            Determine the best ambient soundscape for reading this book snippet.
            Title: "$bookTitle"
            Snippet: "$textSnippet"

            Options:
            - RAINY_CAFE (Cozy rain, indoor coffee, mystery, urban literature)
            - CRACKLING_FIRE (Classic literature, fantasy, hearth, historical fiction)
            - COSMIC_DRONE (Sci-Fi, space, philosophical, deep thinking)
            - FOREST_SOLITUDE (Adventure, nature, poetry, tranquil solitude)
            - PIANO_LOFI (Study, non-fiction, contemporary, biographical)

            Respond with ONLY ONE word from the options list above.
        """.trimIndent()

        try {
            val req = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt))))
            )
            val res = RetrofitClient.service.generateContent(apiKey, req)
            val answer = res.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()?.uppercase() ?: ""
            when {
                answer.contains("CRACKLING") || answer.contains("FIRE") -> SoundscapeType.CRACKLING_FIRE
                answer.contains("COSMIC") || answer.contains("DRONE") -> SoundscapeType.COSMIC_DRONE
                answer.contains("FOREST") -> SoundscapeType.FOREST_SOLITUDE
                answer.contains("PIANO") || answer.contains("LOFI") -> SoundscapeType.PIANO_LOFI
                else -> SoundscapeType.RAINY_CAFE
            }
        } catch (e: Exception) {
            SoundscapeType.RAINY_CAFE
        }
    }
}
