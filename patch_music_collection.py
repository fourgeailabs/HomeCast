import re

file_path = "app/src/main/java/com/example/ui/screens/MusicScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

# Add selectedCollection state
state_target = "    var isGridView by remember { mutableStateOf(false) }"
state_repl = """    var isGridView by remember { mutableStateOf(false) }
    var selectedCollection by remember { mutableStateOf<Pair<String, List<com.example.data.MusicTrack>>?>(null) }"""
text = text.replace(state_target, state_repl)

# ShelfHeader onClick
text = re.sub(
    r'MusicShelfHeader\(\s*title = "(.*?)",\s*subtitle = "(.*?)",\s*icon = (.*?),\s*iconTint = (.*?)\n\s*\)',
    r'MusicShelfHeader(\n                        title = "\1",\n                        subtitle = "\2",\n                        icon = \3,\n                        iconTint = \4,\n                        onClick = { selectedCollection = Pair("\1", <TODO>) }\n                    )',
    text
)
# We have a <TODO> that we need to replace with the actual list for that section.
text = text.replace('Pair("Jump Back In", <TODO>)', 'Pair("Jump Back In", recents)')
text = text.replace('Pair("Recently Added", <TODO>)', 'Pair("Recently Added", currentMusic.sortedByDescending { it.id }.take(15))')
text = text.replace('Pair("Your Mixes & Radio", <TODO>)', 'Pair("Your Mixes & Radio", currentMusic.shuffled().take(10))')
text = text.replace('Pair("Top Tracks", <TODO>)', 'Pair("Top Tracks", currentMusic.filter { it.genre == "Pop" || it.genre == "Electronic" })')
text = text.replace('Pair("Albums", <TODO>)', 'Pair("Albums", currentMusic)')

# Add UI for selectedCollection
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
                    items(selectedCollection!!.second, key = { it.id }) { track ->
                        Music3ColumnCard(
                            track = track,
                            isPlaying = playbackState.currentTrack?.id == track.id && playbackState.isPlaying,
                            onClick = {
                                viewModel.playMusic(track)
                            }
                        )
                    }
                }
            }
        } else if (selectedGenre != null || isGridView || searchQuery.isNotBlank()) {"""
text = text.replace(box_target, box_repl)

# Fix Public Domain music to pull from Archive as well? The user asked for it. 
# But let's first get this fixed and compiling.

with open(file_path, "w") as f:
    f.write(text)
