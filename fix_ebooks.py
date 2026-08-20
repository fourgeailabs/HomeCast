import re

file_path = "app/src/main/java/com/example/ui/screens/EBooksScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

# Fix regex botch in Graphic Novels
text = text.replace(
    'onItemClick = { onNavigateToDetails(it.title,                                onHeaderClick = { selectedCollection = Pair("Graphic Novels & Manga", comics) }, it.authorOrArtist, "BOOK") },',
    'onItemClick = { onNavigateToDetails(it.title, it.authorOrArtist, "BOOK") },\n                                onHeaderClick = { selectedCollection = Pair("Graphic Novels & Manga", comics) },'
)

# Fix regex botch in Dynamic Genres
text = text.replace(
    'onItemClick = { onNavigateToDetails(it.title,                                onHeaderClick = { selectedCollection = Pair(genre, books) }, it.authorOrArtist, "BOOK") },',
    'onItemClick = { onNavigateToDetails(it.title, it.authorOrArtist, "BOOK") },\n                                onHeaderClick = { selectedCollection = Pair(genre, books) },'
)

# Fix regex botch in Currently Reading
text = text.replace(
    'onItemClick = { onNavigateToDetails(it.title,                                onHeaderClick = { selectedCollection = Pair("Currently Reading", currentlyReading) }, it.authorOrArtist, "BOOK") },',
    'onItemClick = { onNavigateToDetails(it.title, it.authorOrArtist, "BOOK") },\n                                onHeaderClick = { selectedCollection = Pair("Currently Reading", currentlyReading) },'
)

# Replace any others I might have missed
text = re.sub(
    r'onItemClick = \{ onNavigateToDetails\(it\.title,\s*onHeaderClick = \{ selectedCollection = Pair\("(.*?)", (.*?)\) \}, it\.authorOrArtist, "BOOK"\) \},',
    r'onItemClick = { onNavigateToDetails(it.title, it.authorOrArtist, "BOOK") },\n                                onHeaderClick = { selectedCollection = Pair("\1", \2) },',
    text
)

with open(file_path, "w") as f:
    f.write(text)
