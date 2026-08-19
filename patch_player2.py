import re

file_path = "app/src/main/java/com/example/ui/screens/PlayerScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace("viewModel.skipBackward(10)", "viewModel.playbackManager.skipBackward(10)")
text = text.replace("viewModel.skipForward(30)", "viewModel.playbackManager.skipForward(30)")

with open(file_path, "w") as f:
    f.write(text)
