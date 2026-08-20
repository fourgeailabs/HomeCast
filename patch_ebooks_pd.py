import re

file_path = "app/src/main/java/com/example/ui/screens/EBooksScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

# Add public domain books collect
collect_target = "    val allEBooks by viewModel.allEBooks.collectAsState()"
collect_repl = """    val allEBooks by viewModel.allEBooks.collectAsState()
    val archiveBooks by viewModel.publicDomainBooks.collectAsState()"""

text = text.replace(collect_target, collect_repl)

# Replace sampleBookshelfItems with archiveBooks map
sample_target = """        } else {
            // Keep public domain sample books with full 20-chapter simulations
            sampleBookshelfItems
        }"""
sample_repl = """        } else {
            archiveBooks.map { doc ->
                val coverUrl = "https://archive.org/services/img/${doc.identifier}"
                val title = doc.title ?: "Unknown Title"
                val author = when (doc.creator) {
                    is List<*> -> doc.creator.firstOrNull()?.toString() ?: "Unknown Author"
                    is String -> doc.creator
                    else -> "Unknown Author"
                }
                val desc = when (doc.description) {
                    is List<*> -> doc.description.firstOrNull()?.toString() ?: ""
                    is String -> doc.description
                    else -> ""
                }
                BookshelfItem(
                    id = doc.identifier,
                    title = title,
                    authorOrArtist = author,
                    publicDomainUrl = "https://archive.org/download/${doc.identifier}/${doc.identifier}_djvu.txt", // best effort
                    coverUrl = coverUrl,
                    genre = "Classic",
                    description = desc
                )
            }
        }"""
text = text.replace(sample_target, sample_repl)

with open(file_path, "w") as f:
    f.write(text)
