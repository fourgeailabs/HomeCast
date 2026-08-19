import re

file_path = "app/src/main/java/com/example/PlaybackService.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace("PlaybackManager.setPlayer(player, mediaSession)", "PlaybackManager.setPlayer(player, mediaSession!!)")

with open(file_path, "w") as f:
    f.write(text)
