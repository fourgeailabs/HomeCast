import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace(
    "class MainViewModel : ViewModel() {",
    "class MainViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {"
)
text = text.replace(
    "private val playbackManager = PlaybackManager()",
    "private val playbackManager = PlaybackManager(application)"
)

with open(file_path, "w") as f:
    f.write(text)
