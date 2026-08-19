import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace("playbackManager.setAudiobookList(playlist ?: allBooks.value)", "")
text = text.replace("playbackManager.setPlaylist(playlist ?: allMusic.value)", "")

with open(file_path, "w") as f:
    f.write(text)
