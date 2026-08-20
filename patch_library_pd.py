import re

file_path = "app/src/main/java/com/example/ui/screens/LibraryScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

collect_target = "    val allBooks by viewModel.allBooks.collectAsState()"
collect_repl = """    val allBooks by viewModel.allBooks.collectAsState()
    val archiveAudiobooks by viewModel.publicDomainAudiobooks.collectAsState()"""
text = text.replace(collect_target, collect_repl)

pd_target = r"    val publicDomainAudiobooks = remember \{\s*listOf\([\s\S]*?            \)\s*\}\s*\}"
pd_repl = """    val publicDomainAudiobooks = remember(archiveAudiobooks) {
        archiveAudiobooks.map { doc ->
            val coverUrl = "https://archive.org/services/img/${doc.identifier}"
            val title = doc.title ?: "Unknown Title"
            val author = when (doc.creator) {
                is List<*> -> (doc.creator as List<*>).firstOrNull()?.toString() ?: "Unknown Author"
                is String -> doc.creator
                else -> "Unknown Author"
            }
            com.example.data.Audiobook(
                id = doc.identifier,
                title = title,
                author = author,
                duration = 3600000L,
                coverUrl = coverUrl,
                serverId = "pd_server",
                streamUrl = "https://archive.org/download/${doc.identifier}/${doc.identifier}_64kb.mp3", // best effort
                narrator = "Archive.org",
                genre = "Classic",
                lastPlayedPosition = 0L,
                isDownloaded = false
            )
        }
    }"""
text = re.sub(pd_target, pd_repl, text)

with open(file_path, "w") as f:
    f.write(text)
