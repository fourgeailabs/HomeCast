import re

file_path = "app/src/main/java/com/example/ui/screens/LibraryScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

# Replace <TODO> manually based on title
text = text.replace('Pair("Continue Listening", <TODO>)', 'Pair("Continue Listening", recents)')
text = text.replace('Pair("New Releases", <TODO>)', 'Pair("New Releases", currentBooks.sortedByDescending { it.id }.take(10))')
text = text.replace('Pair("Trending in your area", <TODO>)', 'Pair("Trending in your area", currentBooks.shuffled().take(5))')
text = text.replace('Pair("Award Winners", <TODO>)', 'Pair("Award Winners", currentBooks.filter { it.genre == "Classics" || it.genre == "Sci-Fi" })')
text = text.replace('Pair("Epic Series", <TODO>)', 'Pair("Epic Series", currentBooks.filter { it.genre == "Fantasy" || it.genre == "Sci-Fi" })')
text = text.replace('Pair("Favorites", <TODO>)', 'Pair("Favorites", favorites)')

# Add UI for selectedCollection
box_target = "            if (isGridView) {"
box_repl = """            if (selectedCollection != null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        IconButton(onClick = { selectedCollection = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Text(selectedCollection!!.first, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(selectedCollection!!.second) { book ->
                            BookGridItem(
                                book = book,
                                onClick = { onNavigateToDetails(book.title, book.author, "AUDIOBOOK") }
                            )
                        }
                    }
                }
            } else if (isGridView) {"""
text = text.replace(box_target, box_repl)

with open(file_path, "w") as f:
    f.write(text)
