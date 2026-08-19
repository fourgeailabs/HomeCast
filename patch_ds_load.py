import re

file_path = "app/src/main/java/com/example/ui/screens/DiscoveryScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

replacement = """            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isLoading) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = AccentTeal)
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("Gemini AI is curating this category...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }"""

text = text.replace("""            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {""", replacement)

with open(file_path, "w") as f:
    f.write(text)
