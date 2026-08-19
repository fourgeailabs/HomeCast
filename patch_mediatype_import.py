import re

file_path = "app/src/main/java/com/example/data/network/BookloreClient.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace("import okhttp3.Request\n", "import okhttp3.Request\nimport okhttp3.MediaType.Companion.toMediaType\n")

with open(file_path, "w") as f:
    f.write(text)
