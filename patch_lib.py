import re

file_path = "app/src/main/java/com/example/ui/screens/LibraryScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace("viewModel.toggleFavorite", "viewModel.repository.toggleFavorite")

with open(file_path, "w") as f:
    f.write(text)

file_path = "app/src/main/java/com/example/ui/screens/DiscoveryScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace("viewModel.playMusicTrack", "viewModel.playbackManager.playMusicTrack")

with open(file_path, "w") as f:
    f.write(text)

file_path = "app/src/main/java/com/example/ui/screens/MusicScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace("viewModel.playMusicTrack", "viewModel.playbackManager.playMusicTrack")

with open(file_path, "w") as f:
    f.write(text)
