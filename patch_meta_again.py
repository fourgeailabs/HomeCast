import re

file_path = "app/src/main/java/com/example/data/network/BookloreClient.kt"
with open(file_path, "r") as f:
    text = f.read()

# Replace BookloreMetadata
replacement_meta = """@JsonClass(generateAdapter = true)
data class BookloreMetadata(
    val title: String? = null,
    val authors: List<String>? = null,
    val description: String? = null,
    val pageCount: Int? = null,
    val categories: List<String>? = null
)"""
text = re.sub(r"@JsonClass\(generateAdapter = true\)\ndata class BookloreMetadata\([\s\S]*?val categories: List<String>\? = null\n\)", replacement_meta, text)

# Replace the first ebooks mapping block
mapping1_regex = r"                                        val ebooks = bookList\.map \{ book ->\n[\s\S]*?\n                                        \}"
mapping1_replacement = """                                        val ebooks = bookList.map { book ->
                                            val authorsList = book.metadata?.authors ?: emptyList()
                                            val authorName = if (authorsList.isNotEmpty()) authorsList.joinToString(", ") else book.author ?: book.writer ?: "Unknown"
                                            val categoriesList = book.metadata?.categories ?: emptyList()
                                            val genreName = if (categoriesList.isNotEmpty()) categoriesList.first() else book.genre ?: "Unknown"
                                            
                                            // Determine cover url based on Booklore vs Komga
                                            val cover = book.coverUrl ?: if (normalizedUrl.contains("komga") || endpoint.contains("komga")) {
                                                "$normalizedUrl/api/v1/books/${book.id}/thumbnail"
                                            } else {
                                                "$normalizedUrl/api/v1/media/book/${book.id}/cover"
                                            }
                                            
                                            EBook(
                                                id = book.id,
                                                title = book.metadata?.title ?: book.title ?: book.name ?: "Unknown",
                                                author = authorName,
                                                coverUrl = cover,
                                                serverId = serverId,
                                                genre = genreName,
                                                description = book.metadata?.description ?: book.description ?: book.summary ?: "",
                                                totalPages = book.metadata?.pageCount ?: book.totalPages ?: book.media?.pagesCount ?: 0,
                                                isComic = book.isComic ?: (genreName == "Comic" || genreName == "Manga")
                                            )
                                        }"""
text = re.sub(mapping1_regex, mapping1_replacement, text)

# Replace the second ebooks mapping block
mapping2_regex = r"                                        val ebooks = books\.map \{ book ->\n[\s\S]*?\n                                        \}"
mapping2_replacement = """                                        val ebooks = books.map { book ->
                                            val authorsList = book.metadata?.authors ?: emptyList()
                                            val authorName = if (authorsList.isNotEmpty()) authorsList.joinToString(", ") else book.author ?: book.writer ?: "Unknown"
                                            val categoriesList = book.metadata?.categories ?: emptyList()
                                            val genreName = if (categoriesList.isNotEmpty()) categoriesList.first() else book.genre ?: "Unknown"
                                            
                                            val cover = book.coverUrl ?: if (normalizedUrl.contains("komga") || endpoint.contains("komga")) {
                                                "$normalizedUrl/api/v1/books/${book.id}/thumbnail"
                                            } else {
                                                "$normalizedUrl/api/v1/media/book/${book.id}/cover"
                                            }
                                            
                                            EBook(
                                                id = book.id,
                                                title = book.metadata?.title ?: book.title ?: book.name ?: "Unknown",
                                                author = authorName,
                                                coverUrl = cover,
                                                serverId = serverId,
                                                genre = genreName,
                                                description = book.metadata?.description ?: book.description ?: book.summary ?: "",
                                                totalPages = book.metadata?.pageCount ?: book.totalPages ?: book.media?.pagesCount ?: 0,
                                                isComic = book.isComic ?: (genreName == "Comic" || genreName == "Manga")
                                            )
                                        }"""
text = re.sub(mapping2_regex, mapping2_replacement, text)

with open(file_path, "w") as f:
    f.write(text)
