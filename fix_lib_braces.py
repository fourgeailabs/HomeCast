import re

file_path = "app/src/main/java/com/example/ui/screens/LibraryScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace("Icons.AutoMirrored.Filled.ArrowBack", "Icons.Default.ArrowBack")

# The problem is that the added `Column` block doesn't have a closing brace before `else if (allBooks.isEmpty())`
# Wait, let's see how I replaced `box_repl`.
text = text.replace("} // Ends LazyColumn\n    } // Ends else\n} // Ends main Column\n} // Ends LibraryScreen Composable\n", "} // Ends LazyColumn\n    } // Ends else\n} // Ends LibraryScreen Composable\n")

with open(file_path, "w") as f:
    f.write(text)
