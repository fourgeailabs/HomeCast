import re

file_path = "app/src/main/java/com/example/ui/screens/LibraryScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

missing_code = """
    val currentBooks = remember(allBooks, publicDomainAudiobooks, selectedSource) {
        if (selectedSource == 0) allBooks else publicDomainAudiobooks
    }

    val genreList = remember(currentBooks) {
        val extracted = currentBooks.map { it.genre.trim() }.filter { it.isNotBlank() }.distinct()
        if (extracted.isNotEmpty()) listOf("All") + extracted else listOf("All", "Sci-Fi", "Fantasy", "Mystery", "Non-Fiction", "Classics")
    }

    val filteredBooks = remember(currentBooks, searchQuery, selectedGenre) {
        currentBooks.filter { book ->
            val matchesGenre = selectedGenre == null || selectedGenre == "All" || book.genre.equals(selectedGenre, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() || book.title.contains(searchQuery, ignoreCase = true) || book.author.contains(searchQuery, ignoreCase = true)
            matchesGenre && matchesSearch
        }
    }
"""

text = text.replace("        Spacer(modifier = Modifier.height(14.dp))", missing_code + "\n        Spacer(modifier = Modifier.height(14.dp))")

# Also let's fix the selectedCollection logic that failed to apply
# Wait, let's look for `if (selectedGenre != null || isGridView || searchQuery.isNotBlank()) {`
box_target = "        if (selectedGenre != null || isGridView || searchQuery.isNotBlank()) {"
box_repl = """        if (selectedCollection != null) {
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(selectedCollection!!.second, key = { it.id }) { book ->
                        Audiobook3ColumnCard(
                            book = book,
                            isPlaying = playbackState.currentAudiobook?.id == book.id && playbackState.isPlaying,
                            onClick = {
                                viewModel.playAudiobook(book)
                                onBookClick(book)
                            }
                        )
                    }
                }
            }
        } else if (selectedGenre != null || isGridView || searchQuery.isNotBlank()) {"""
text = text.replace(box_target, box_repl)

with open(file_path, "w") as f:
    f.write(text)
