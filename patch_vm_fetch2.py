import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"
with open(file_path, "r") as f:
    text = f.read()

# Make sure fetchDiscoveryRecommendations is available
text = re.sub(
    r"private fun fetchDiscoveryRecommendations\(\)",
    "fun fetchDiscoveryRecommendations()", text
)

with open(file_path, "w") as f:
    f.write(text)
