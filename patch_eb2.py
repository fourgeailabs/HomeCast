import re

file_path = "app/src/main/java/com/example/ui/screens/EBooksScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace(
    "fun GlassBookshelfRow(\n    shelfTitle: String,\n    badge: String,\n    badgeColor: Color,\n    items: List<BookshelfItem>,\n    onItemClick: (BookshelfItem) -> Unit\n) {",
    "fun GlassBookshelfRow(\n    shelfTitle: String,\n    badge: String,\n    badgeColor: Color,\n    items: List<BookshelfItem>,\n    onItemClick: (BookshelfItem) -> Unit,\n    onNavigateToCreator: (String) -> Unit = {}\n) {"
)

text = text.replace(
    "onItemClick = { onNavigateToDetails(it.title, it.authorOrArtist, \"BOOK\") }",
    "onItemClick = { onNavigateToDetails(it.title, it.authorOrArtist, \"BOOK\") },\n                            onNavigateToCreator = onNavigateToCreator"
)

with open(file_path, "w") as f:
    f.write(text)
