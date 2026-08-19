import re

file_path = "app/src/main/java/com/example/ui/screens/PlayerScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

# Make sure togglePlayPause is correctly replaced
text = text.replace("viewModel.playbackManager.playbackManager.togglePlayPause()", "viewModel.playbackManager.togglePlayPause()")
text = text.replace("viewModel.playbackManager.playbackManager.seekTo", "viewModel.playbackManager.seekTo")

with open(file_path, "w") as f:
    f.write(text)
