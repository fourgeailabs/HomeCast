import re

file_path = "app/src/main/java/com/example/ui/screens/EBooksScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

target = """                IconButton(
                    onClick = { isGridView = !isGridView },
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceGlass, CircleShape)
                ) {
                    Icon(
                        if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle View",
                        tint = AccentIndigo
                    )
                }"""

repl = """                IconButton(
                    onClick = { viewModel.categorizeBooksWithAI() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceGlass, CircleShape)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Categorize", tint = AccentIndigo)
                }
""" + target

text = text.replace(target, repl)

with open(file_path, "w") as f:
    f.write(text)
