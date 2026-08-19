import re

file_path = "app/src/main/java/com/example/ui/screens/EBooksScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace(
    "fun EBooksScreen(\n    viewModel: MainViewModel,\n    onOpenEBook: (EBookData) -> Unit,\n    onOpenComic: (ComicData) -> Unit,\n    onNavigateToSettings: () -> Unit\n) {",
    "fun EBooksScreen(\n    viewModel: MainViewModel,\n    onOpenEBook: (EBookData) -> Unit,\n    onOpenComic: (ComicData) -> Unit,\n    onNavigateToSettings: () -> Unit,\n    onNavigateToDetails: (String, String, String) -> Unit = {_,_,_->},\n    onNavigateToCreator: (String) -> Unit = {}\n) {"
)

# In BookCard3Column, when clicked, call onNavigateToDetails
text = text.replace(
    "fun BookCard3Column(\n    book: BookshelfItem,\n    onClick: () -> Unit\n) {",
    "fun BookCard3Column(\n    book: BookshelfItem,\n    onClick: () -> Unit,\n    onAuthorClick: (String) -> Unit = {}\n) {"
)
text = text.replace(
    """        Text(
            book.authorOrArtist,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )""",
    """        Text(
            book.authorOrArtist,
            fontSize = 9.sp,
            color = AccentTeal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable { onAuthorClick(book.authorOrArtist) }
        )"""
)

# And in EBooksScreen, pass the new parameters to BookCard3Column
text = text.replace(
    "BookCard3Column(book = book, onClick = { openItem(book) })",
    """BookCard3Column(book = book, onClick = { onNavigateToDetails(book.title, book.authorOrArtist, "BOOK") }, onAuthorClick = onNavigateToCreator)"""
)

# And in BookshelfBookItem
text = text.replace(
    "fun BookshelfBookItem(\n    book: BookshelfItem,\n    onClick: () -> Unit\n) {",
    "fun BookshelfBookItem(\n    book: BookshelfItem,\n    onClick: () -> Unit,\n    onAuthorClick: (String) -> Unit = {}\n) {"
)
text = text.replace(
    """        Text(
            book.authorOrArtist,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )""",
    """        Text(
            book.authorOrArtist,
            fontSize = 10.sp,
            color = AccentTeal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable { onAuthorClick(book.authorOrArtist) }
        )"""
)
text = text.replace(
    "BookshelfBookItem(book = book, onClick = { onItemClick(book) })",
    """BookshelfBookItem(book = book, onClick = { onItemClick(book) }, onAuthorClick = onNavigateToCreator)"""
)

text = text.replace(
    "onItemClick = { openItem(it) }",
    "onItemClick = { onNavigateToDetails(it.title, it.authorOrArtist, \"BOOK\") }"
)

with open(file_path, "w") as f:
    f.write(text)
