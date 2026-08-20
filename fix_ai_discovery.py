import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"
with open(file_path, "r") as f:
    text = f.read()

target = """    fun fetchGeminiCategoryItems(category: String, inventorySummary: String) {
        viewModelScope.launch {
            _isDiscoveryLoading.value = true
            try {
                val prompt = "You are curating the '" + category + "' section for a media library app.\\n" +
                    "Based on the user's active inventory below, or using your own knowledge of public domain/popular media, \\n" +
                    "generate a list of 10 items (mix of books, audiobooks, and music) that perfectly fit the '" + category + "' category.\\n\\n" +
                    "User Inventory:\\n" + inventorySummary + "\\n\\n" +"""

repl = """    fun fetchGeminiCategoryItems(category: String, sourceStr: String) {
        viewModelScope.launch {
            _isDiscoveryLoading.value = true
            try {
                val inventory = if (sourceStr.contains("private")) {
                    val b = _allBooks.value.take(20).joinToString { it.title + " by " + it.author }
                    val e = _allEBooks.value.take(20).joinToString { it.title + " by " + it.author }
                    val m = _allMusic.value.take(20).joinToString { it.title + " by " + it.artist }
                    "Audiobooks: $b\\nEBooks: $e\\nMusic: $m"
                } else {
                    val pb = _publicDomainBooks.value.take(20).joinToString { it.title + " by " + it.author }
                    val pa = _publicDomainAudiobooks.value.take(20).joinToString { it.title + " by " + it.author }
                    "Public Domain Books: $pb\\nPublic Domain Audiobooks: $pa"
                }
                val prompt = "You are curating the '" + category + "' section for a media library app.\\n" +
                    "Based on the user's active inventory below, or using your own knowledge of public domain/popular media, \\n" +
                    "generate a list of 10 items (mix of books, audiobooks, and music) that perfectly fit the '" + category + "' category.\\n\\n" +
                    "User Inventory:\\n" + inventory + "\\n\\n" +"""

text = text.replace(target, repl)

with open(file_path, "w") as f:
    f.write(text)
