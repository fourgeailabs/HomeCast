import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"
with open(file_path, "r") as f:
    text = f.read()

fav_func = """    fun toggleFavorite(itemId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(itemId)
        }
    }
"""

if "fun toggleFavorite(" not in text:
    text = text.replace("    // --- Booklore Sync ---", fav_func + "\n    // --- Booklore Sync ---")

with open(file_path, "w") as f:
    f.write(text)
