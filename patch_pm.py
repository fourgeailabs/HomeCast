import re

file_path = "app/src/main/java/com/example/ui/PlaybackManager.kt"
with open(file_path, "r") as f:
    text = f.read()

# I need to completely replace PlaybackManager to use context to get MediaController and play.
# Let's see what is inside PlaybackManager right now.
