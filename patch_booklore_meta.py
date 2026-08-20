import re

file_path = "app/src/main/java/com/example/data/network/BookloreClient.kt"
with open(file_path, "r") as f:
    text = f.read()

replacement_classes = """@JsonClass(generateAdapter = true)
data class BookloreBook(
    val id: String,
    val title: String? = null,
    val name: String? = null,
    val metadata: BookloreMetadata? = null,
    
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
data class BookloreMetadata(
    val authors: List<String>? = null,
    val description: String? = null,
    val pageCount: Int? = null,
    val categories: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class KomgaMedia("""

text = re.sub(r"@JsonClass\(generateAdapter = true\)\ndata class BookloreBook\([\s\S]*?@JsonClass\(generateAdapter = true\)\ndata class KomgaMedia\(", replacement_classes, text)

mapping = """                                        val ebooks = bookList.map { book ->
                                            val authorsList = book.metadata?.authors ?: emptyList()
                                            val authorName = if (authorsList.isNotEmpty()) authorsList.joinToString(", ") else book.author ?: book.writer ?: "Unknown"
                                            val categoriesList = book.metadata?.categories ?: emptyList()
                                            val genreName = if (categoriesList.isNotEmpty()) categoriesList.first() else book.genre ?: "Unknown"
                                            
                                            EBook(
                                                id = book.id,
                                                title = book.title ?: book.name ?: "Unknown",
                                                author = authorName,
                                                coverUrl = book.coverUrl ?: if (endpoint.contains("books") || endpoint == "") "$normalizedUrl/api/v1/books/${book.id}/thumbnail" else "",
                                                serverId = serverId,
                                                genre = genreName,
                                                description = book.metadata?.description ?: book.description ?: book.summary ?: "",
                                                totalPages = book.metadata?.pageCount ?: book.totalPages ?: book.media?.pagesCount ?: 0,
                                                isComic = book.isComic ?: (genreName == "Comic" || genreName == "Manga")
                                            )
                                        }"""

text = re.sub(r"                                        val ebooks = bookList\.map \{ book ->\n                                            EBook\([\s\S]*?\n                                            \)\n                                        \}", mapping, text)

mapping2 = """                                        val ebooks = books.map { book ->
                                            val authorsList = book.metadata?.authors ?: emptyList()
                                            val authorName = if (authorsList.isNotEmpty()) authorsList.joinToString(", ") else book.author ?: book.writer ?: "Unknown"
                                            val categoriesList = book.metadata?.categories ?: emptyList()
                                            val genreName = if (categoriesList.isNotEmpty()) categoriesList.first() else book.genre ?: "Unknown"
                                            
                                            EBook(
                                                id = book.id,
                                                title = book.title ?: book.name ?: "Unknown",
                                                author = authorName,
                                                coverUrl = book.coverUrl ?: if (endpoint.contains("books") || endpoint == "") "$normalizedUrl/api/v1/books/${book.id}/thumbnail" else "",
                                                serverId = serverId,
                                                genre = genreName,
                                                description = book.metadata?.description ?: book.description ?: book.summary ?: "",
                                                totalPages = book.metadata?.pageCount ?: book.totalPages ?: book.media?.pagesCount ?: 0,
                                                isComic = book.isComic ?: (genreName == "Comic" || genreName == "Manga")
                                            )
                                        }"""

text = re.sub(r"                                        val ebooks = books\.map \{ book ->\n                                            EBook\([\s\S]*?\n                                            \)\n                                        \}", mapping2, text)

with open(file_path, "w") as f:
    f.write(text)
