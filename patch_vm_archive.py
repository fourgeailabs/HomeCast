import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"
with open(file_path, "r") as f:
    text = f.read()

# Make sure imports are present
if "import com.example.data.network.ArchiveOrgClient" not in text:
    text = text.replace("import com.example.data.network.BookloreClient", "import com.example.data.network.BookloreClient\nimport com.example.data.network.ArchiveOrgClient\nimport com.example.data.network.ArchiveDoc")

var_target = "    val _recommendations = MutableStateFlow<List<String>>(emptyList())"
var_replacement = """    val _recommendations = MutableStateFlow<List<String>>(emptyList())

    private val _publicDomainBooks = MutableStateFlow<List<ArchiveDoc>>(emptyList())
    val publicDomainBooks = _publicDomainBooks.asStateFlow()

    private val _publicDomainAudiobooks = MutableStateFlow<List<ArchiveDoc>>(emptyList())
    val publicDomainAudiobooks = _publicDomainAudiobooks.asStateFlow()

    init {
        viewModelScope.launch {
            _publicDomainBooks.value = ArchiveOrgClient.fetchPublicDomain("gutenberg")
            _publicDomainAudiobooks.value = ArchiveOrgClient.fetchPublicDomain("librivoxaudio")
        }
    }"""
text = text.replace(var_target, var_replacement)

with open(file_path, "w") as f:
    f.write(text)
