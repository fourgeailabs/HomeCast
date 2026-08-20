import re

file_path = "app/src/main/java/com/example/ui/screens/MusicScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

target = """                Text(
                    "Music",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )"""
repl = """                Text(
                    "Music",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = Color.White
                )"""
text = text.replace(target, repl)

# Also fix the icon tints that were originally Color.White
text = text.replace("tint = MaterialTheme.colorScheme.onSurface", "tint = Color.White")

with open(file_path, "w") as f:
    f.write(text)
