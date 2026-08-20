import re

file_path = "app/src/main/java/com/example/ui/screens/MainScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

target = """                composable(Routes.EBooks) {
                    EBooksScreen(
                        viewModel = viewModel,
                        onOpenEBook = { book ->
                            activeEBook = book
                        },
                        onOpenComic = { comic ->
                            activeComic = comic
                        },
                        onNavigateToSettings = { navController.navigate(Routes.Settings) }
                    )
                }"""

replacement = """                composable(Routes.EBooks) {
                    EBooksScreen(
                        viewModel = viewModel,
                        onOpenEBook = { book ->
                            activeEBook = book
                        },
                        onOpenComic = { comic ->
                            activeComic = comic
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

if target in text:
    text = text.replace(target, replacement)
    with open(file_path, "w") as f:
        f.write(text)
    print("Success")
else:
    print("Target not found")
