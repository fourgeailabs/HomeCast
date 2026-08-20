import re

file_path = "app/src/main/java/com/example/data/network/BookloreClient.kt"
with open(file_path, "r") as f:
    text = f.read()

# Replace the coverUrl generation in block 1
mapping1_regex = r"                                            // Determine cover url based on Booklore vs Komga\n                                            val cover = book\.coverUrl \?\: if \(normalizedUrl\.contains\(\"komga\"\) \|\| endpoint\.contains\(\"komga\"\)\) \{\n                                                \"\$normalizedUrl/api/v1/books/\$\{book\.id\}/thumbnail\"\n                                            \} else \{\n                                                \"\$normalizedUrl/api/v1/media/book/\$\{book\.id\}/cover\"\n                                            \}"
mapping1_replacement = """                                            // Determine cover url based on Booklore vs Komga
                                            var rawToken = apiKey
                                            if (rawToken.startsWith("Bearer ")) {
                                                rawToken = rawToken.substring(7)
                                            }
                                            val queryParam = if (rawToken.isNotBlank() && !rawToken.startsWith("Basic ")) "?token=$rawToken" else ""
                                            val cover = book.coverUrl ?: if (normalizedUrl.contains("komga") || endpoint.contains("komga")) {
                                                "$normalizedUrl/api/v1/books/${book.id}/thumbnail"
                                            } else {
                                                "$normalizedUrl/api/v1/media/book/${book.id}/cover$queryParam"
                                            }"""
text = re.sub(mapping1_regex, mapping1_replacement, text)

# Replace the coverUrl generation in block 2
mapping2_regex = r"                                            val cover = book\.coverUrl \?\: if \(normalizedUrl\.contains\(\"komga\"\) \|\| endpoint\.contains\(\"komga\"\)\) \{\n                                                \"\$normalizedUrl/api/v1/books/\$\{book\.id\}/thumbnail\"\n                                            \} else \{\n                                                \"\$normalizedUrl/api/v1/media/book/\$\{book\.id\}/cover\"\n                                            \}"
text = re.sub(mapping2_regex, mapping1_replacement, text)

with open(file_path, "w") as f:
    f.write(text)
