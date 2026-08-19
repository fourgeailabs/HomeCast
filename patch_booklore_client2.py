import re

file_path = "app/src/main/java/com/example/data/network/BookloreClient.kt"
with open(file_path, "r") as f:
    text = f.read()

# Add a fallback OPDS endpoint just in case it's actually OPDS standard xml, or kavita/komga using specific routes
replacement = """    suspend fun fetchBooks(hostUrl: String, apiKey: String, serverId: String): Result<List<EBook>> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = hostUrl.trimEnd('/')
            // Try standard API first
            var url = "$normalizedUrl/api/v1/library/books"
            
            // Allow testing different endpoints
            val endpoints = listOf(
                "/api/v1/library/books", 
                "/api/books",
                "/api/v2/opds/catalog",
                "/opds/v1.2/catalog",
                "/api/v1/books"
            )
            
            var lastException: Exception? = null
            
            for (endpoint in endpoints) {
                try {
                    val currentUrl = "$normalizedUrl$endpoint"
                    val requestBuilder = Request.Builder().url(currentUrl)
                    
                    if (apiKey.isNotBlank()) {
                        if (apiKey.startsWith("Basic ")) {
                            requestBuilder.header("Authorization", apiKey)
                        } else {
                            requestBuilder.header("Authorization", "Bearer $apiKey")
                        }
                    }
                    val request = requestBuilder.build()
                    val response = client.newCall(request).execute()
                    
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string()
                        if (bodyString != null && bodyString.startsWith("{")) {
                            val adapter = moshi.adapter(BookloreResponse::class.java)
                            val bookloreResponse = adapter.fromJson(bodyString)
                            
                            if (bookloreResponse?.books != null) {
                                val ebooks = bookloreResponse.books.map { book ->
                                    EBook(
                                        id = book.id,
                                        title = book.title,
                                        author = book.author,
                                        coverUrl = book.coverUrl ?: "",
                                        serverId = serverId,
                                        genre = book.genre ?: "Unknown",
                                        description = book.description ?: "",
                                        totalPages = book.totalPages ?: 0,
                                        isComic = book.isComic ?: false
                                    )
                                }
                                return@withContext Result.success(ebooks)
                            }
                        }
                    }
                } catch (e: Exception) {
                    lastException = e
                }
            }
            
            return@withContext Result.failure(lastException ?: Exception("Failed to find valid Booklore API endpoint. Supported: /api/v1/library/books"))
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Booklore books", e)
            return@withContext Result.failure(e)
        }
    }"""

text = re.sub(r"    suspend fun fetchBooks\([\s\S]*?\}\n    \}", replacement, text)

with open(file_path, "w") as f:
    f.write(text)
