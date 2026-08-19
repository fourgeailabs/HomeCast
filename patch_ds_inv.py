import re

file_path = "app/src/main/java/com/example/ui/screens/DiscoveryScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

replacement_func = """
    val inventorySummary = remember(allDiscoverableItems) {
        allDiscoverableItems.take(50).joinToString("; ") { "${it.title} by ${it.creator} (${it.mediaType})" }
    }
"""

text = text.replace("    val allDiscoverableItems = remember(dynamicAudiobooks, dynamicEBooks, dynamicMusic, publicDomainAudio, publicDomainBooks, publicDomainMusic) {\n        dynamicAudiobooks + dynamicEBooks + dynamicMusic + publicDomainAudio + publicDomainBooks + publicDomainMusic\n    }",
"    val allDiscoverableItems = remember(dynamicAudiobooks, dynamicEBooks, dynamicMusic, publicDomainAudio, publicDomainBooks, publicDomainMusic) {\n        dynamicAudiobooks + dynamicEBooks + dynamicMusic + publicDomainAudio + publicDomainBooks + publicDomainMusic\n    }\n" + replacement_func)

text = text.replace(
    """viewModel.fetchGeminiCategoryItems("New Releases", "")""",
    """viewModel.fetchGeminiCategoryItems("New Releases", inventorySummary)"""
)
text = text.replace(
    """viewModel.fetchGeminiCategoryItems("Noteworthy", "")""",
    """viewModel.fetchGeminiCategoryItems("Noteworthy", inventorySummary)"""
)
text = text.replace(
    """viewModel.fetchGeminiCategoryItems("Popular", "")""",
    """viewModel.fetchGeminiCategoryItems("Popular", inventorySummary)"""
)
text = text.replace(
    """viewModel.fetchGeminiCategoryItems("Sagas & Epics", "")""",
    """viewModel.fetchGeminiCategoryItems("Sagas & Epics", inventorySummary)"""
)

with open(file_path, "w") as f:
    f.write(text)
