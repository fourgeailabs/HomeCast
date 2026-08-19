import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"
with open(file_path, "r") as f:
    text = f.read()

new_functions = """
    suspend fun fetchDetailsWithGemini(title: String, creator: String, type: String): Map<String, String>? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val prompt = \"\"\"
                    You are a media database. Provide details for the following $type:
                    Title: $title
                    Creator: $creator
                    
                    Return ONLY valid JSON with this schema:
                    {
                        "bio": "A 2-3 paragraph synopsis or description.",
                        "rating": "e.g. 4.5/5 (based on critical acclaim or public ratings)",
                        "publisher": "Name of publisher or record label",
                        "website": "Official website or Wikipedia link (URL only) or 'N/A'",
                        "coverUrl": "A representative image URL from Unsplash or Wikimedia (or empty)"
                    }
                    Do not use markdown blocks.
                \"\"\".trimIndent()

                val request = com.example.GenerateContentRequest(
                    contents = listOf(com.example.Content(parts = listOf(com.example.Part(text = prompt))))
                )
                val response = com.example.RetrofitClient.service.generateContent(com.example.BuildConfig.GEMINI_API_KEY, request)
                var jsonStr = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                jsonStr = jsonStr.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                
                val moshi = com.squareup.moshi.Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                val mapType = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
                val adapter = moshi.adapter<Map<String, String>>(mapType)
                adapter.fromJson(jsonStr)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun fetchCreatorDetailsWithGemini(creatorName: String): Map<String, String>? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val prompt = \"\"\"
                    You are a biographical database. Provide details for the author/artist/creator:
                    Name: $creatorName
                    
                    Return ONLY valid JSON with this schema:
                    {
                        "roles": "e.g. Author, Musician, Director",
                        "bio": "A 3-4 paragraph detailed biography.",
                        "wikiLink": "Wikipedia link (URL only) or 'N/A'",
                        "website": "Official website (URL only) or 'N/A'",
                        "imageUrl": "A representative portrait URL from Unsplash or Wikimedia (or empty)"
                    }
                    Do not use markdown blocks.
                \"\"\".trimIndent()

                val request = com.example.GenerateContentRequest(
                    contents = listOf(com.example.Content(parts = listOf(com.example.Part(text = prompt))))
                )
                val response = com.example.RetrofitClient.service.generateContent(com.example.BuildConfig.GEMINI_API_KEY, request)
                var jsonStr = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                jsonStr = jsonStr.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                
                val moshi = com.squareup.moshi.Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
                val mapType = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
                val adapter = moshi.adapter<Map<String, String>>(mapType)
                adapter.fromJson(jsonStr)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}"""

# Replace the very last closing brace
text = text.rsplit("}", 1)[0] + new_functions

with open(file_path, "w") as f:
    f.write(text)
