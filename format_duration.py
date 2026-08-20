import re

file_path = "app/src/main/java/com/example/ui/screens/LibraryScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

# Replace undefined formatDuration with fun formatDuration or inline it
# Wait, let's just find where formatDuration is. It says Unresolved reference.
text = text.replace("formatDuration(", "com.example.utils.formatDuration(")

# The previous compile error also complained about "WhiteVariant" in MusicScreen
file_path_music = "app/src/main/java/com/example/ui/screens/MusicScreen.kt"
with open(file_path_music, "r") as f:
    text_music = f.read()

text_music = text_music.replace("MaterialTheme.colorScheme.WhiteVariant", "Color(0x80FFFFFF)")

with open(file_path_music, "w") as f:
    f.write(text_music)

with open(file_path, "w") as f:
    f.write(text)
