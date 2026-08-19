import re

file_path = "app/src/main/java/com/example/ui/screens/EReaderScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

replacement = """@Composable
fun EReaderScreen(
    eBook: EBookData,
    onClose: () -> Unit,
    onSwitchToComic: (() -> Unit)? = null
) {
    var chapters by remember { mutableStateOf(eBook.chapters) }
    var isLoading by remember { mutableStateOf(eBook.publicDomainUrl != null) }

    LaunchedEffect(eBook) {
        if (eBook.publicDomainUrl != null) {
            val fetched = PublicDomainContentFetcher.fetchTextContent(eBook.publicDomainUrl)
            if (fetched.isNotEmpty()) {
                chapters = fetched
            } else {
                chapters = listOf(BookChapter("Fetch Failed", 0, listOf("Failed to load content from ${eBook.publicDomainUrl}")))
            }
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFDFBF7)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = AccentTeal)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Downloading book from Public Domain archive...", color = Color.Black)
            }
        }
        return
    }

    val coroutineScope = rememberCoroutineScope()"""

text = re.sub(r"@Composable\nfun EReaderScreen\(\n    eBook: EBookData,\n    onClose: \(\) -> Unit,\n    onSwitchToComic: \(\(\) -> Unit\)\? = null\n\) \{\n    val coroutineScope = rememberCoroutineScope\(\)", replacement, text)

# Now fix chapters
text = text.replace("eBook.chapters", "chapters")

with open(file_path, "w") as f:
    f.write(text)
