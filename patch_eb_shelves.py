import re

file_path = "app/src/main/java/com/example/ui/screens/EBooksScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

# Replace the hardcoded shelves with dynamic ones, or at least add a fallback shelf for all other books.

shelves_replacement = """                    // Shelf 1: Currently Reading
                    val currentlyReading = filteredItems.filter { it.progressPercent in 1..99 }
                    if (currentlyReading.isNotEmpty()) {
                        item {
                            GlassBookshelfRow(
                                shelfTitle = "Currently Reading",
                                badge = "RECENT",
                                badgeColor = AccentTeal,
                                items = currentlyReading,
                                onItemClick = { onNavigateToDetails(it.title, it.authorOrArtist, "BOOK") },
                                onNavigateToCreator = onNavigateToCreator
                            )
                        }
                    }

                    // Shelf 2: Graphic Novels & Manga
                    val comics = filteredItems.filter { it.isComic }
                    if (comics.isNotEmpty()) {
                        item {
                            GlassBookshelfRow(
                                shelfTitle = "Graphic Novels & Manga",
                                badge = "GEMINI SMART ZOOM",
                                badgeColor = AccentIndigo,
                                items = comics,
                                onItemClick = { onNavigateToDetails(it.title, it.authorOrArtist, "BOOK") },
                                onNavigateToCreator = onNavigateToCreator
                            )
                        }
                    }

                    // Dynamic Genres
                    val specificGenres = listOf("Sci-Fi", "Cyberpunk", "Classic", "Philosophy")
                    val otherBooks = filteredItems.filter { !it.isComic && it.progressPercent !in 1..99 }
                    
                    val groupedByGenre = otherBooks.groupBy { it.genre.takeIf { g -> g.isNotBlank() } ?: "Uncategorized" }
                    
                    groupedByGenre.forEach { (genre, books) ->
                        item {
                            GlassBookshelfRow(
                                shelfTitle = genre,
                                badge = "COLLECTION",
                                badgeColor = Color(0xFFF59E0B),
                                items = books,
                                onItemClick = { onNavigateToDetails(it.title, it.authorOrArtist, "BOOK") },
                                onNavigateToCreator = onNavigateToCreator
                            )
                        }
                    }"""

text = re.sub(
r"                    // Shelf 1: Currently Reading & Favorites\n                    item \{\n                        GlassBookshelfRow\([\s\S]*?item \{\n                        GlassBookshelfRow\(\n                            shelfTitle = \"Classics & Philosophy\",\n                            badge = \"PUBLIC DOMAIN\",\n                            badgeColor = Color\(0xFF10B981\),\n                            items = filteredItems.filter \{ it.genre in listOf\(\"Classic\", \"Philosophy\"\) \},\n                            onItemClick = \{ onNavigateToDetails\(it.title, it.authorOrArtist, \"BOOK\"\) \},\n                            onNavigateToCreator = onNavigateToCreator\n                        \)\n                    \}",
shelves_replacement, text)

with open(file_path, "w") as f:
    f.write(text)
