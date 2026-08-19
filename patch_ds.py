import re

file_path = "app/src/main/java/com/example/ui/screens/DiscoveryScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

# Add geminiCategoryItems
text = text.replace(
    "val error by viewModel.discoveryError.collectAsState()",
    "val error by viewModel.discoveryError.collectAsState()\n    val geminiCategoryItems by viewModel.geminiCategoryItems.collectAsState()"
)

# Update activeCategory to clear items/fetch
text = text.replace(
"""                    CategoryCard(
                        title = "New Releases",
                        icon = Icons.Default.NewReleases,
                        color = AccentTeal,
                        modifier = Modifier.weight(1f).aspectRatio(1.2f),
                        onClick = { activeCategory = "New Releases" }
                    )""",
"""                    CategoryCard(
                        title = "New Releases",
                        icon = Icons.Default.NewReleases,
                        color = AccentTeal,
                        modifier = Modifier.weight(1f).aspectRatio(1.2f),
                        onClick = { 
                            activeCategory = "New Releases"
                            viewModel.fetchGeminiCategoryItems("New Releases", "")
                        }
                    )"""
)

text = text.replace(
"""                    CategoryCard(
                        title = "Noteworthy",
                        icon = Icons.Default.Star,
                        color = Color(0xFFFFB300),
                        modifier = Modifier.weight(1f).aspectRatio(1.2f),
                        onClick = { activeCategory = "Noteworthy" }
                    )""",
"""                    CategoryCard(
                        title = "Noteworthy",
                        icon = Icons.Default.Star,
                        color = Color(0xFFFFB300),
                        modifier = Modifier.weight(1f).aspectRatio(1.2f),
                        onClick = { 
                            activeCategory = "Noteworthy"
                            viewModel.fetchGeminiCategoryItems("Noteworthy", "")
                        }
                    )"""
)

text = text.replace(
"""                    CategoryCard(
                        title = "Popular",
                        icon = Icons.Default.TrendingUp,
                        color = AccentIndigo,
                        modifier = Modifier.weight(1f).aspectRatio(1.2f),
                        onClick = { activeCategory = "Popular" }
                    )""",
"""                    CategoryCard(
                        title = "Popular",
                        icon = Icons.Default.TrendingUp,
                        color = AccentIndigo,
                        modifier = Modifier.weight(1f).aspectRatio(1.2f),
                        onClick = { 
                            activeCategory = "Popular"
                            viewModel.fetchGeminiCategoryItems("Popular", "")
                        }
                    )"""
)

text = text.replace(
"""                    CategoryCard(
                        title = "Sagas & Epics",
                        icon = Icons.Default.AutoStories,
                        color = Color(0xFFE91E63),
                        modifier = Modifier.weight(1f).aspectRatio(1.2f),
                        onClick = { activeCategory = "Sagas" }
                    )""",
"""                    CategoryCard(
                        title = "Sagas & Epics",
                        icon = Icons.Default.AutoStories,
                        color = Color(0xFFE91E63),
                        modifier = Modifier.weight(1f).aspectRatio(1.2f),
                        onClick = { 
                            activeCategory = "Sagas & Epics"
                            viewModel.fetchGeminiCategoryItems("Sagas & Epics", "")
                        }
                    )"""
)

# Update itemsToShow
text = text.replace(
"""        val itemsToShow = when (activeCategory) {
            "New Releases" -> categoryNew
            "Noteworthy" -> categoryNoteworthy
            "Popular" -> categoryPopular
            "Sagas" -> categorySagas
            else -> allDiscoverableItems
        }""",
"""        val itemsToShow = geminiCategoryItems"""
)

with open(file_path, "w") as f:
    f.write(text)
