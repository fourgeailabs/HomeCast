import re

file_path = "app/src/main/java/com/example/ui/screens/DiscoveryScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

models = """
enum class DiscoveryMediaType {
    BOOK, AUDIOBOOK, MUSIC
}

data class DiscoveryItem(
    val title: String,
    val creator: String,
    val genre: String,
    val description: String,
    val mediaType: DiscoveryMediaType,
    val coverUrl: String
)

"""

text = text.replace("import com.example.ui.theme.SurfaceGlassBorder\n", "import com.example.ui.theme.SurfaceGlassBorder\n" + models)

with open(file_path, "w") as f:
    f.write(text)
