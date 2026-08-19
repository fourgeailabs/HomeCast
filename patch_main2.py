import re

file_path = "app/src/main/java/com/example/ui/screens/MainScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace(
    """                composable(Routes.Library) {
                    LibraryScreen(
                        viewModel = viewModel,
                        onBookClick = {
                            isPlayerSlidUp = true
                        },
                        onNavigateToSettings = { navController.navigate(Routes.Settings) }
                    )
                }""",
    """                composable(Routes.Library) {
                    LibraryScreen(
                        viewModel = viewModel,
                        onBookClick = {
                            isPlayerSlidUp = true
                        },
                        onNavigateToSettings = { navController.navigate(Routes.Settings) },
                        onNavigateToDetails = { title, creator, type ->
                            navController.navigate(Routes.MediaDetail(title, creator, type))
                        },
                        onNavigateToCreator = { name ->
                            navController.navigate(Routes.CreatorDetail(name))
                        }
                    )
                }"""
)

with open(file_path, "w") as f:
    f.write(text)
