import re

file_path = "app/src/main/java/com/example/ui/screens/EBooksScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace(
    "data class BookshelfItem(\n    val id: String,\n    val title: String,\n    val authorOrArtist: String,",
    "data class BookshelfItem(\n    val id: String,\n    val title: String,\n    val authorOrArtist: String,\n    val publicDomainUrl: String? = null,"
)

# Update the sample items
urls = {
    "ebook_1": "https://www.gutenberg.org/cache/epub/35/pg35.txt",
    "ebook_2": "https://www.gutenberg.org/cache/epub/84/pg84.txt",
    "ebook_3": "", # Not public domain? Wait, Neuromancer is not PD. Let's make it simulated or just empty url.
    "ebook_4": "https://www.gutenberg.org/cache/epub/132/pg132.txt",
    "ebook_5": "https://www.gutenberg.org/cache/epub/64317/pg64317.txt", # Gatsby is PD in US!
    "ebook_6": "https://www.gutenberg.org/cache/epub/5200/pg5200.txt"
}

for k, v in urls.items():
    if v:
        text = text.replace(
            f'id = "{k}",',
            f'id = "{k}",\n        publicDomainUrl = "{v}",'
        )

with open(file_path, "w") as f:
    f.write(text)
