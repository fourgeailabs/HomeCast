import re

for file_path in ["app/src/main/java/com/example/ui/screens/LibraryScreen.kt", "app/src/main/java/com/example/ui/screens/MusicScreen.kt"]:
    with open(file_path, "r") as f:
        text = f.read()
    
    text = re.sub(
        r'Text\(\n\s*"Audiobooks",\n\s*fontSize = 28\.sp,\n\s*fontWeight = FontWeight\.ExtraBold,\n\s*letterSpacing = \(-0\.5\)\.sp\n\s*\)',
        'Text(\n                    "Audiobooks",\n                    fontSize = 28.sp,\n                    fontWeight = FontWeight.ExtraBold,\n                    letterSpacing = (-0.5).sp,\n                    color = MaterialTheme.colorScheme.onSurface\n                )',
        text
    )
    
    text = re.sub(
        r'Text\(\n\s*"Music",\n\s*fontSize = 28\.sp,\n\s*fontWeight = FontWeight\.ExtraBold,\n\s*letterSpacing = \(-0\.5\)\.sp\n\s*\)',
        'Text(\n                    "Music",\n                    fontSize = 28.sp,\n                    fontWeight = FontWeight.ExtraBold,\n                    letterSpacing = (-0.5).sp,\n                    color = MaterialTheme.colorScheme.onSurface\n                )',
        text
    )

    with open(file_path, "w") as f:
        f.write(text)
