import re

file_path = "app/src/main/java/com/example/ui/MainViewModel.kt"
with open(file_path, "r") as f:
    text = f.read()

# I had double braces because of my regex earlier
text = text.replace("""        }
    }
    }""", """        }
    }""")

with open(file_path, "w") as f:
    f.write(text)
