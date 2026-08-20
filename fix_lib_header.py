import re

file_path = "app/src/main/java/com/example/ui/screens/LibraryScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

header = """    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        // Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Audiobooks",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = Color.White
                )
                Text(
                    if (currentBooks.isNotEmpty()) "${currentBooks.size} titles in your bookshelf" else "Your Audiobookshelf Library",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { isGridView = !isGridView },
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceGlass, CircleShape)
                ) {
                    Icon(
                        if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle View",
                        tint = AccentTeal
                    )
                }
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .background(SurfaceGlass, CircleShape)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AccentTeal)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedSource == 0,
                onClick = { selectedSource = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Personal Library")
            }
            SegmentedButton(
                selected = selectedSource == 1,
                onClick = { selectedSource = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Public Domain")
            }
        }
"""

text = text.replace("    val filteredBooks = remember(currentBooks, searchQuery, selectedGenre) {", header + "\n    val filteredBooks = remember(currentBooks, searchQuery, selectedGenre) {")

with open(file_path, "w") as f:
    f.write(text)
