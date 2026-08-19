import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"
with open(file_path, "r") as f:
    text = f.read()

# Since I just blindly replaced, let me do it robustly

text = re.sub(
r"    fun playAudiobook\(book: Audiobook, playlist: List<Audiobook>\? = null\) \{.*?\n        \}",
r"""    fun playAudiobook(book: Audiobook, playlist: List<Audiobook>? = null) {
        playbackManager.playAudiobook(book, playlist ?: allBooks.value)
        viewModelScope.launch {
            repository.updateProgress(book.id, book.progress)
        }
    }""", text, flags=re.DOTALL)

text = re.sub(
r"    fun playMusicTrack\(track: MusicTrack, playlist: List<MusicTrack>\? = null\) \{.*?\n        \}",
r"""    fun playMusicTrack(track: MusicTrack, playlist: List<MusicTrack>? = null) {
        playbackManager.playMusicTrack(track, playlist ?: allMusic.value)
        viewModelScope.launch {
            repository.updateMusicLastPlayed(track.id)
        }
    }""", text, flags=re.DOTALL)


with open(file_path, "w") as f:
    f.write(text)
