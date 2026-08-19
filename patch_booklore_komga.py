import re

file_path = "app/src/main/java/com/example/data/network/BookloreClient.kt"
with open(file_path, "r") as f:
    text = f.read()

# Add a `content` list to BookloreResponse for Komga API compatibility
replacement = """@JsonClass(generateAdapter = true)
data class BookloreResponse(
    val books: List<BookloreBook>? = null,
    val content: List<BookloreBook>? = null
)"""

text = re.sub(r"@JsonClass\(generateAdapter = true\)\ndata class BookloreResponse\(\n    val books: List<BookloreBook>\? = null\n\)", replacement, text)

# Update the parsing logic
replacement2 = """                                    val bookList = bookloreResponse?.books ?: bookloreResponse?.content
                                    if (bookList != null) {
                                        val ebooks = bookList.map { book ->
                                            EBook(
                                                id = book.id,
                                                title = book.title ?: book.name ?: "Unknown",
                                                author = book.author ?: book.writer ?: "Unknown",
                                                coverUrl = book.coverUrl ?: if (endpoint.contains("books")) "$normalizedUrl/api/v1/books/${book.id}/thumbnail" else "",
                                                serverId = serverId,
                                                genre = book.genre ?: "Unknown",
                                                description = book.description ?: book.summary ?: "",
                                                totalPages = book.totalPages ?: book.media?.pagesCount ?: 0,
                                                isComic = book.isComic ?: true
                                            )
                                        }
                                        return@withContext Result.success(ebooks)
                                    }"""

text = re.sub(r"                                    if \(bookloreResponse\?\.books != null\) \{\n                                        val ebooks = bookloreResponse\.books\.map \{ book ->\n                                            EBook\([\s\S]*?\n                                            \)\n                                        \}\n                                        return@withContext Result\.success\(ebooks\)\n                                    \}", replacement2, text)

# Add Komga book properties
replacement3 = """@JsonClass(generateAdapter = true)
data class BookloreBook(
    val id: String,
    val title: String? = null,
    val name: String? = null,
    val author: String? = null,
    val writer: String? = null,
    val coverUrl: String? = null,
    val genre: String? = null,
    val description: String? = null,
    val summary: String? = null,
    val totalPages: Int? = null,
    val isComic: Boolean? = null,
    val media: KomgaMedia? = null
)

@JsonClass(generateAdapter = true)
data class KomgaMedia(
    val pagesCount: Int? = null
)"""

text = re.sub(r"@JsonClass\(generateAdapter = true\)\ndata class BookloreBook\([\s\S]*?val isComic: Boolean\? = null\n\)", replacement3, text)

with open(file_path, "w") as f:
    f.write(text)
