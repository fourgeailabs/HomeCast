import re

file_path = "app/src/main/java/com/example/ui/screens/MainScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

# Replace Discovery label
text = text.replace('label = { Text("Discovery", maxLines = 1) }', 'label = { Text("Discover", maxLines = 1) }')

# Replace saveState = true with saveState = false in bottom bar
text = text.replace('popUpTo(navController.graph.startDestinationId) { saveState = true }', 'popUpTo(navController.graph.startDestinationId) { saveState = false }')

# Replace restoreState = true with restoreState = false in bottom bar
text = text.replace('restoreState = true', 'restoreState = false')

with open(file_path, "w") as f:
    f.write(text)
