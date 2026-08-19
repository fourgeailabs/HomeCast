import re

file_path = "app/src/main/java/com/example/ui/screens/LibraryScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace(
    """fun Audiobook3ColumnCard(
    book: Audiobook,
    isPlaying: Boolean,
    onClick: () -> Unit
) {""",
    """fun Audiobook3ColumnCard(
    book: Audiobook,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onAuthorClick: (String) -> Unit = {}
) {"""
)

with open(file_path, "w") as f:
    f.write(text)
