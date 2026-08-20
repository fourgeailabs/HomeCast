import re

file_path = "app/src/main/java/com/example/ui/screens/EBooksScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

# Add selectedCollection state
state_target = "    var searchQuery by remember { mutableStateOf(\"\") }"
state_repl = """    var searchQuery by remember { mutableStateOf("") }
    var selectedCollection by remember { mutableStateOf<Pair<String, List<BookshelfItem>>?>(null) }"""
text = text.replace(state_target, state_repl)

# Inside GlassBookshelfRow, add onClick to the header row
row_target = """fun GlassBookshelfRow(
    shelfTitle: String,
    badge: String,
    badgeColor: Color,
    items: List<BookshelfItem>,
    onItemClick: (BookshelfItem) -> Unit,
    onNavigateToCreator: (String) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier"""

row_repl = """fun GlassBookshelfRow(
    shelfTitle: String,
    badge: String,
    badgeColor: Color,
    items: List<BookshelfItem>,
    onItemClick: (BookshelfItem) -> Unit,
    onNavigateToCreator: (String) -> Unit = {},
    onHeaderClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.clickable { onHeaderClick() }"""

text = text.replace(row_target, row_repl)

# Find where GlassBookshelfRow is called and add onHeaderClick = { selectedCollection = Pair("Title", list) }
text = re.sub(
    r'GlassBookshelfRow\(\s*shelfTitle = "(.*?)",\s*badge = "(.*?)",\s*badgeColor = (.*?),\s*items = (.*?),\s*onItemClick = (.*?),',
    r'GlassBookshelfRow(\n                                shelfTitle = "\1",\n                                badge = "\2",\n                                badgeColor = \3,\n                                items = \4,\n                                onItemClick = \5,\n                                onHeaderClick = { selectedCollection = Pair("\1", \4) },',
    text
)
# Special case for variable shelfTitle
text = re.sub(
    r'GlassBookshelfRow\(\s*shelfTitle = genre,\s*badge = "COLLECTION",\s*badgeColor = Color\(0xFFF59E0B\),\s*items = (.*?),\s*onItemClick = (.*?),',
    r'GlassBookshelfRow(\n                                shelfTitle = genre,\n                                badge = "COLLECTION",\n                                badgeColor = Color(0xFFF59E0B),\n                                items = \1,\n                                onItemClick = \2,\n                                onHeaderClick = { selectedCollection = Pair(genre, \1) },',
    text
)


# Add UI for selectedCollection
box_target = "        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {"
box_repl = """        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (selectedCollection != null) {
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
                            BookshelfBookItem(
                                book = book,
                                onClick = { openItem(book) },
                                onAuthorClick = onNavigateToCreator
                            )
                        }
                    }
                }
            } else"""

text = text.replace(box_target, box_repl)

# We need to add the closing brace for the else branch
closing_target = """        if (selectedBookForReading != null) {
            val book = selectedBookForReading!!"""
closing_repl = """        }
        if (selectedBookForReading != null) {
            val book = selectedBookForReading!!"""
text = text.replace(closing_target, closing_repl)

with open(file_path, "w") as f:
    f.write(text)
