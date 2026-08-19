import re

file_path = "app/src/main/java/com/example/data/LibraryRepository.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace(
    "suspend fun toggleFavorite(",
    "fun toggleFavorite("
)

with open(file_path, "w") as f:
    f.write(text)
