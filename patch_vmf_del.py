import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"
with open(file_path, "r") as f:
    text = f.read()

text = re.sub(r"    fun skipNextTrack\(\) \{.*?\n    \}", "", text, flags=re.DOTALL)
text = re.sub(r"    fun skipPreviousTrack\(\) \{.*?\n    \}", "", text, flags=re.DOTALL)
text = re.sub(r"    fun togglePlayPause\(\) \{.*?\n    \}", "", text, flags=re.DOTALL)
text = re.sub(r"    fun seekTo\(positionMs: Long\) \{.*?\n    \}", "", text, flags=re.DOTALL)
text = re.sub(r"    fun skipForward\(seconds: Int = 30\) \{.*?\n    \}", "", text, flags=re.DOTALL)
text = re.sub(r"    fun skipBackward\(seconds: Int = 10\) \{.*?\n    \}", "", text, flags=re.DOTALL)
text = re.sub(r"    fun setSpeed\(speed: Float\) \{.*?\n    \}", "", text, flags=re.DOTALL)

with open(file_path, "w") as f:
    f.write(text)
