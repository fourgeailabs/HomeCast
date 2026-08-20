import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"
with open(file_path, "r") as f:
    text = f.read()

categorize_func = """
    fun categorizeBooksWithAI() {
        viewModelScope.launch {
            val books = _allEBooks.value
            if (books.isEmpty()) return@launch
            
            // We batch them to avoid huge prompts
            val batch = books.take(30)
            val titles = batch.joinToString("\\n") { it.id + ":::" + it.title + " by " + it.author }
            
            val prompt = "Here is a list of books and comics with IDs, titles, and authors.\\n" +
                "Please categorize each into a single, high-level, precise genre (e.g., 'Sci-Fi', 'Fantasy', 'Cyberpunk', 'Manga', 'Superhero Comic', 'Non-Fiction').\\n" +
                "Return ONLY a raw JSON array matching this schema:\\n" +
                "[\\n" +
                "  {\\n" +
                "    \\"id\\": \\"book id\\",\\n" +
                "    \\"genre\\": \\"assigned genre\\"\\n" +
                "  }\\n" +
                "]\\n\\n" +
                "List:\\n" + titles

            try {
                val request = com.example.GenerateContentRequest(
                    contents = listOf(com.example.Content(parts = listOf(com.example.Part(text = prompt))))
                )
                val response = geminiApiService.generateContent(request)
                if (response.isSuccessful) {
                    val rawText = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                    val jsonStr = rawText.substringAfter("[").substringBeforeLast("]")
                    val jsonArray = org.json.JSONArray("[" + jsonStr + "]")
                    
                    val updatedBooks = mutableListOf<com.example.data.EBook>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.optString("id")
                        val genre = obj.optString("genre")
                        
                        val book = batch.find { it.id == id }
                        if (book != null && genre.isNotBlank()) {
                            updatedBooks.add(book.copy(genre = genre))
                        }
                    }
                    if (updatedBooks.isNotEmpty()) {
                        libraryDao.insertEBooks(updatedBooks)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
"""

text = text.replace("    fun fetchGeminiCategoryItems", categorize_func + "\n    fun fetchGeminiCategoryItems")

with open(file_path, "w") as f:
    f.write(text)
