import re

file_path = "app/src/main/java/com/example/ui/screens/LibraryScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace(
    "fun LibraryScreen(\n    viewModel: MainViewModel,\n    onBookClick: (Audiobook) -> Unit,\n    onNavigateToSettings: () -> Unit\n) {",
    "fun LibraryScreen(\n    viewModel: MainViewModel,\n    onBookClick: (Audiobook) -> Unit,\n    onNavigateToSettings: () -> Unit,\n    onNavigateToDetails: (String, String, String) -> Unit = {_,_,_->},\n    onNavigateToCreator: (String) -> Unit = {}\n) {"
)

# For Audiobook3ColumnCard
text = text.replace(
    "fun Audiobook3ColumnCard(\n    book: Audiobook,\n    onClick: () -> Unit\n) {",
    "fun Audiobook3ColumnCard(\n    book: Audiobook,\n    onClick: () -> Unit,\n    onAuthorClick: (String) -> Unit = {}\n) {"
)
text = text.replace(
    """        Text(
            book.author,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )""",
    """        Text(
            book.author,
            fontSize = 9.sp,
            color = AccentTeal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable { onAuthorClick(book.author) }
        )"""
)
text = text.replace(
    "Audiobook3ColumnCard(book = book, onClick = {",
    "Audiobook3ColumnCard(book = book, onAuthorClick = onNavigateToCreator, onClick = {\n                            onNavigateToDetails(book.title, book.author, \"AUDIOBOOK\")\n                            //"
)

# For BookShelfRowItem
text = text.replace(
    "fun BookShelfRowItem(\n    book: Audiobook,\n    onClick: () -> Unit\n) {",
    "fun BookShelfRowItem(\n    book: Audiobook,\n    onClick: () -> Unit,\n    onAuthorClick: (String) -> Unit = {}\n) {"
)
text = text.replace(
    """                Text(
                    book.author,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )""",
    """                Text(
                    book.author,
                    fontSize = 13.sp,
                    color = AccentTeal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onAuthorClick(book.author) }
                )"""
)
text = text.replace(
    "BookShelfRowItem(book = book, onClick = {",
    "BookShelfRowItem(book = book, onAuthorClick = onNavigateToCreator, onClick = {\n                            onNavigateToDetails(book.title, book.author, \"AUDIOBOOK\")\n                            //"
)

# For AudiobookShelfCard
text = text.replace(
    "fun AudiobookShelfCard(\n    book: Audiobook,\n    onClick: () -> Unit\n) {",
    "fun AudiobookShelfCard(\n    book: Audiobook,\n    onClick: () -> Unit,\n    onAuthorClick: (String) -> Unit = {}\n) {"
)
text = text.replace(
    """        Text(
            book.author,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )""",
    """        Text(
            book.author,
            fontSize = 10.sp,
            color = AccentTeal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable { onAuthorClick(book.author) }
        )"""
)
text = text.replace(
    "AudiobookShelfCard(book = book, onClick = {",
    "AudiobookShelfCard(book = book, onAuthorClick = onNavigateToCreator, onClick = {\n                            onNavigateToDetails(book.title, book.author, \"AUDIOBOOK\")\n                            //"
)

with open(file_path, "w") as f:
    f.write(text)
