import re

file_path = "app/src/main/java/com/example/ui/screens/MainScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace("viewModel.togglePlayPause()", "viewModel.playbackManager.togglePlayPause()")

with open(file_path, "w") as f:
    f.write(text)
