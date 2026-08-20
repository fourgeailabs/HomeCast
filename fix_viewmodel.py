import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"
with open(file_path, "r") as f:
    text = f.read()

# Fix isSuccessful and body()
target_ai_req = r'''val response = com\.example\.RetrofitClient\.service\.generateContent\(com\.example\.BuildConfig\.GEMINI_API_KEY, request\)
                if \(response\.isSuccessful\) \{
                    val rawText = response\.body\(\)\?\.candidates\?\.firstOrNull\(\)\?\.content\?\.parts\?\.firstOrNull\(\)\?\.text \?\: ""'''
repl_ai_req = '''val response = com.example.RetrofitClient.service.generateContent(com.example.BuildConfig.GEMINI_API_KEY, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                if (rawText.isNotBlank()) {'''
text = re.sub(target_ai_req, repl_ai_req, text)

# Fix creator map
text = text.replace("it.title + \" by \" + it.author", "it.title + \" by \" + (it.creator ?: \"Unknown\")")

# Fix DiscoveryItem instantiation
target_disc_item = r'''com\.example\.ui\.screens\.DiscoveryItem\(
                            id = "gemini_.*?",
                            title = item\.title,
                            creator = item\.creator,
                            mediaType = when \(item\.mediaType\.uppercase\(\)\) \{
                                "AUDIOBOOK" -> com\.example\.ui\.screens\.DiscoveryMediaType\.AUDIOBOOK
                                "MUSIC" -> com\.example\.ui\.screens\.DiscoveryMediaType\.MUSIC
                                else -> com\.example\.ui\.screens\.DiscoveryMediaType\.BOOK
                            \},
                            genre = item\.genre,
                            coverUrl = item\.coverUrl\.ifBlank \{ "https://images\.unsplash\.com/photo-1544716278-ca5e3f4abd8c\?w=600&q=80" \},
                            description = item\.description,
                            tag = "✨ AI Curated",
                            durationOrPages = "N/A",
                            format = "Digital",
                            gradient = listOf\(androidx\.compose\.ui\.graphics\.Color\(0xFF311B92\), androidx\.compose\.ui\.graphics\.Color\(0xFF7C4DFF\)\)
                        \)'''
repl_disc_item = '''com.example.ui.screens.DiscoveryItem(
                            title = item.title,
                            creator = item.creator,
                            mediaType = when (item.mediaType.uppercase()) {
                                "AUDIOBOOK" -> com.example.ui.screens.DiscoveryMediaType.AUDIOBOOK
                                "MUSIC" -> com.example.ui.screens.DiscoveryMediaType.MUSIC
                                else -> com.example.ui.screens.DiscoveryMediaType.BOOK
                            },
                            genre = item.genre,
                            coverUrl = item.coverUrl.ifBlank { "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&q=80" },
                            description = item.description
                        )'''
text = re.sub(target_disc_item, repl_disc_item, text, flags=re.DOTALL)

with open(file_path, "w") as f:
    f.write(text)
