import re

file_path = "app/src/main/java/com/example/data/network/BookloreClient.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace('okhttp3.MediaType.get("application/json")', '"application/json".toMediaType()')

with open(file_path, "w") as f:
    f.write(text)
