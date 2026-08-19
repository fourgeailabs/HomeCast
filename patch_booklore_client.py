import re

file_path = "app/src/main/java/com/example/data/network/BookloreClient.kt"
with open(file_path, "r") as f:
    text = f.read()

# Fix BookloreClient to use the passed api key properly.
# If authString starts with Basic, it's basic auth. Otherwise Bearer.
replacement = """            if (apiKey.isNotBlank()) {
                if (apiKey.startsWith("Basic ")) {
                    requestBuilder.header("Authorization", apiKey)
                } else {
                    requestBuilder.header("Authorization", "Bearer $apiKey")
                }
            }"""

text = text.replace("""            if (apiKey.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $apiKey")
            }""", replacement)

with open(file_path, "w") as f:
    f.write(text)
