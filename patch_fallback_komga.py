import re

file_path = "app/src/main/java/com/example/data/network/BookloreClient.kt"
with open(file_path, "r") as f:
    text = f.read()

replacement = """                                        val ebooks = books.map { book ->
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
                                        }"""

text = re.sub(r"                                        val ebooks = books\.map \{ book ->\n                                            EBook\(\n                                                id = book\.id,\n                                                title = book\.title,\n                                                author = book\.author,\n                                                coverUrl = book\.coverUrl \?\: \"\",\n                                                serverId = serverId,\n                                                genre = book\.genre \?\: \"Unknown\",\n                                                description = book\.description \?\: \"\",\n                                                totalPages = book\.totalPages \?\: 0,\n                                                isComic = book\.isComic \?\: false\n                                            \)\n                                        \}", replacement, text)

with open(file_path, "w") as f:
    f.write(text)
