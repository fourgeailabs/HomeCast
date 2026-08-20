import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace("it.title + \" by \" + (it.creator ?: \"Unknown\")", "it.title + \" by \" + it.author")

# But wait, ArchiveDoc needs it.creator
text = text.replace("val pb = _publicDomainBooks.value.take(20).joinToString { it.title + \" by \" + it.author }", "val pb = _publicDomainBooks.value.take(20).joinToString { it.title + \" by \" + (it.creator ?: \"Unknown\") }")
text = text.replace("val pa = _publicDomainAudiobooks.value.take(20).joinToString { it.title + \" by \" + it.author }", "val pa = _publicDomainAudiobooks.value.take(20).joinToString { it.title + \" by \" + (it.creator ?: \"Unknown\") }")

with open(file_path, "w") as f:
    f.write(text)
