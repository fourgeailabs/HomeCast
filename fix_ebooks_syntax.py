import re

file_path = "app/src/main/java/com/example/ui/screens/EBooksScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

# Fix the broken onItemClick block
text = re.sub(
    r'onItemClick = \{ onNavigateToDetails\(it\.title,\s*onHeaderClick = \{ selectedCollection = Pair\((.*?),\s*(.*?)\) \},\s*it\.authorOrArtist, "BOOK"\) \},',
    r'onItemClick = { onNavigateToDetails(it.title, it.authorOrArtist, "BOOK") },\n                                onHeaderClick = { selectedCollection = Pair(\1, \2) },',
    text
)

with open(file_path, "w") as f:
    f.write(text)
