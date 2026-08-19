import re

file_path = "app/src/main/java/com/example/ui/screens/PlayerScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace("viewModel.setSpeed(speed)", "viewModel.playbackManager.setPlaybackSpeed(speed)")
text = text.replace("viewModel.seekTo(newPos)", "viewModel.playbackManager.seekTo(newPos)")
text = text.replace("viewModel.skipPreviousTrack()", "viewModel.playbackManager.skipPreviousTrack()")
text = text.replace("viewModel.skipBackward()", "viewModel.playbackManager.skipBackward()")
text = text.replace("viewModel.togglePlayPause()", "viewModel.playbackManager.togglePlayPause()")
text = text.replace("viewModel.skipForward()", "viewModel.playbackManager.skipForward()")
text = text.replace("viewModel.skipNextTrack()", "viewModel.playbackManager.skipNextTrack()")

with open(file_path, "w") as f:
    f.write(text)
