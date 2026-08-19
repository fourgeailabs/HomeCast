import re

file_path = "app/src/main/java/com/example/ui/screens/EBooksScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

replacement = """        } else {
            val sampleEBook = EBookData(
                id = item.id,
                title = item.title,
                author = item.authorOrArtist,
                totalChapters = 0,
                chapters = emptyList(),
                publicDomainUrl = item.publicDomainUrl
            )
            onOpenEBook(sampleEBook)
        }"""

text = re.sub(r"        \} else \{\n            val generatedChapters.*?val sampleEBook = EBookData\([^)]+\)\n            onOpenEBook\(sampleEBook\)\n        \}", replacement, text, flags=re.DOTALL)

with open(file_path, "w") as f:
    f.write(text)
