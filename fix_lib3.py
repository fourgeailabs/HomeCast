import re

file_path = "app/src/main/java/com/example/ui/screens/LibraryScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

# Add selectedCollection state
state_target = "    var isGridView by remember { mutableStateOf(false) }"
state_repl = """    var isGridView by remember { mutableStateOf(false) }
    var selectedCollection by remember { mutableStateOf<Pair<String, List<com.example.data.Audiobook>>?>(null) }"""
text = text.replace(state_target, state_repl)

# Re-add newArrivals, popularBooks, noteworthyBooks, seriesBooks that were lost when the bad regex hit
lost_code = """
    val newArrivals = currentBooks.sortedByDescending { it.id }.take(10)
    val popularBooks = currentBooks.shuffled().take(5)
    val noteworthyBooks = currentBooks.filter { it.genre == "Classics" || it.genre == "Sci-Fi" }
    val seriesBooks = currentBooks.filter { it.genre == "Fantasy" || it.genre == "Sci-Fi" }
"""
text = text.replace("    val filteredBooks = remember(currentBooks, searchQuery, selectedGenre) {", lost_code + "\n    val filteredBooks = remember(currentBooks, searchQuery, selectedGenre) {")

with open(file_path, "w") as f:
    f.write(text)
