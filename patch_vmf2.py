import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace(
    "fun playAudiobook(book: Audiobook, playlist: List<Audiobook>? = null) {",
    "fun playAudiobook(book: Audiobook, playlist: List<Audiobook>? = null) {\n        playbackManager.playAudiobook(book, playlist ?: allBooks.value)"
)

text = text.replace(
    "fun playMusicTrack(track: MusicTrack, playlist: List<MusicTrack>? = null) {",
    "fun playMusicTrack(track: MusicTrack, playlist: List<MusicTrack>? = null) {\n        playbackManager.playMusicTrack(track, playlist ?: allMusic.value)"
)

# Fix earlier patch mistake where we completely deleted the playback calls by accident
text = text.replace("""    fun playAudiobook(book: Audiobook, playlist: List<Audiobook>? = null) {
        playbackManager.playAudiobook(book, playlist ?: allBooks.value)
        viewModelScope.launch {
            repository.updateProgress(book.id, book.progress)
            
        }
    }""", """    fun playAudiobook(book: Audiobook, playlist: List<Audiobook>? = null) {
        playbackManager.playAudiobook(book, playlist ?: allBooks.value)
        viewModelScope.launch {
            repository.updateProgress(book.id, book.progress)
        }
    }""")

# Read again
with open(file_path, "w") as f:
    f.write(text)
