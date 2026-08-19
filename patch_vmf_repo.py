import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace(
    "private val repository = LibraryRepository",
    "val repository = LibraryRepository"
)

with open(file_path, "w") as f:
    f.write(text)
