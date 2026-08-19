import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"
with open(file_path, "r") as f:
    text = f.read()

new_state = """    private val _geminiCategoryItems = MutableStateFlow<List<com.example.ui.screens.DiscoveryItem>>(emptyList())
    val geminiCategoryItems: StateFlow<List<com.example.ui.screens.DiscoveryItem>> = _geminiCategoryItems.asStateFlow()

    fun fetchGeminiCategoryItems(category: String, inventorySummary: String) {
        viewModelScope.launch {
            _isDiscoveryLoading.value = true
            try {
                val prompt = "You are curating the '" + category + "' section for a media library app.\\n" +
                    "Based on the user's active inventory below, or using your own knowledge of public domain/popular media, \\n" +
                    "generate a list of 10 items (mix of books, audiobooks, and music) that perfectly fit the '" + category + "' category.\\n\\n" +
                    "User Inventory:\\n" + inventorySummary + "\\n\\n" +
                    "Return ONLY valid JSON matching this schema:\\n" +
                    "{\\n" +
                    "  \\"items\\": [\\n" +
                    "    {\\n" +
                    "      \\"title\\": \\"Item Title\\",\\n" +
                    "      \\"creator\\": \\"Author or Artist\\",\\n" +
                    "      \\"mediaType\\": \\"BOOK\\" | \\"AUDIOBOOK\\" | \\"MUSIC\\",\\n" +
                    "      \\"genre\\": \\"Genre Name\\",\\n" +
                    "      \\"description\\": \\"Short description of why it fits this category.\\",\\n" +
                    "      \\"reason\\": \\"AI curation note\\",\\n" +
                    "      \\"coverUrl\\": \\"A placeholder image URL (e.g. Unsplash URL) or empty\\"\\n" +
                    "    }\\n" +
                    "  ]\\n" +
                    "}\\n" +
                    "Do not use markdown formatting. Just raw JSON."

                val request = com.example.GenerateContentRequest(
                    contents = listOf(com.example.Content(parts = listOf(com.example.Part(text = prompt))))
                )
                val response = com.example.RetrofitClient.service.generateContent(com.example.BuildConfig.GEMINI_API_KEY, request)
                var jsonStr = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                
                jsonStr = jsonStr.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                
                val moshi = com.squareup.moshi.Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(com.example.ui.screens.GeminiDiscoveryResponse::class.java)
                val geminiRes = adapter.fromJson(jsonStr)
                
                if (geminiRes != null) {
                    val newItems = geminiRes.items.mapIndexed { index, item ->
                        com.example.ui.screens.DiscoveryItem(
                            id = "gemini_${category.replace(" ", "_")}_$index",
                            title = item.title,
                            creator = item.creator,
                            mediaType = when (item.mediaType.uppercase()) {
                                "AUDIOBOOK" -> com.example.ui.screens.DiscoveryMediaType.AUDIOBOOK
                                "MUSIC" -> com.example.ui.screens.DiscoveryMediaType.MUSIC
                                else -> com.example.ui.screens.DiscoveryMediaType.BOOK
                            },
                            genre = item.genre,
                            coverUrl = item.coverUrl.ifBlank { "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&q=80" },
                            description = item.description,
                            tag = "✨ AI Curated",
                            durationOrPages = "N/A",
                            format = "Digital",
                            gradient = listOf(androidx.compose.ui.graphics.Color(0xFF311B92), androidx.compose.ui.graphics.Color(0xFF7C4DFF))
                        )
                    }
                    _geminiCategoryItems.value = newItems
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isDiscoveryLoading.value = false
            }
        }
    }
"""

text = text.replace("    private val _discoveryError = MutableStateFlow<String?>(null)", "    private val _discoveryError = MutableStateFlow<String?>(null)\n" + new_state)

with open(file_path, "w") as f:
    f.write(text)
