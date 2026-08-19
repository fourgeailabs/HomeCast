import re

file_path = "app/src/main/java/com/example/ui/screens/SettingsScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

# Update the default hostUrl in BookloreConfigCard to point to 6060 just as a hint, since they use 6060.
text = text.replace('var hostUrl by remember { mutableStateOf("http://10.70.14.2:8080") }', 'var hostUrl by remember { mutableStateOf("http://10.70.14.2:6060") }')

with open(file_path, "w") as f:
    f.write(text)
