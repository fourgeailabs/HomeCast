import re

file_path = "app/src/main/java/com/example/ui/screens/LibraryScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace("viewModel.repository.toggleFavorite(", "viewModel.toggleFavorite(")

with open(file_path, "w") as f:
    f.write(text)
