import re

file_path = "app/src/main/java/com/example/ui/screens/LibraryScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace(
    """fun BookShelfRowItem(
    book: Audiobook,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {""",
    """fun BookShelfRowItem(
    book: Audiobook,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onAuthorClick: (String) -> Unit = {}
) {"""
)

with open(file_path, "w") as f:
    f.write(text)
