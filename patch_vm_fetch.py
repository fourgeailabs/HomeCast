import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"
with open(file_path, "r") as f:
    text = f.read()

# Make sure discovery states are accessible
text = text.replace(
    "private val _isDiscoveryLoading = MutableStateFlow(false)",
    "val _isDiscoveryLoading = MutableStateFlow(false)"
)
text = text.replace(
    "private val _discoveryError = MutableStateFlow<String?>(null)",
    "val _discoveryError = MutableStateFlow<String?>(null)"
)
text = text.replace(
    "private val _recommendations = MutableStateFlow<List<String>>(emptyList())",
    "val _recommendations = MutableStateFlow<List<String>>(emptyList())"
)

with open(file_path, "w") as f:
    f.write(text)
