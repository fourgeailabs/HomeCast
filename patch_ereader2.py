import re

file_path = "app/src/main/java/com/example/ui/screens/EReaderScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace("var chapters by remember { mutableStateOf(chapters) }", "var chapters by remember { mutableStateOf(eBook.chapters) }")

with open(file_path, "w") as f:
    f.write(text)
