import re

file_path = "app/src/main/java/com/example/ui/screens/LibraryScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

target = """        } // Ends LazyColumn
    } // Ends else
} // Ends LibraryScreen Composable"""

repl = """        } // Ends LazyColumn
    } // Ends else
} // Ends main Column
} // Ends LibraryScreen Composable"""

text = text.replace(target, repl)

with open(file_path, "w") as f:
    f.write(text)
