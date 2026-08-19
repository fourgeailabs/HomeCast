import re

file_path = "app/src/main/java/com/example/ui/screens/EReaderScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

replacement = """data class EBookData(
    val id: String,
    val title: String,
    val author: String,
    val totalChapters: Int,
    val chapters: List<BookChapter>,
    val publicDomainUrl: String? = null
)"""
text = re.sub(r"data class EBookData\([^)]+\)", replacement, text)

with open(file_path, "w") as f:
    f.write(text)
