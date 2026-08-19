import re

file_path = "app/src/main/java/com/example/ui/screens/MainScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace(
    """                        PlayerScreen(
                            viewModel = viewModel,
                            onCollapse = { isPlayerSlidUp = false }
                        )""",
    """                        PlayerScreen(
                            viewModel = viewModel,
                            onCollapse = { isPlayerSlidUp = false },
                            onNavigateToCreator = { name ->
                                navController.navigate(Routes.CreatorDetail(name))
                            },
                            onNavigateToMedia = { title, creator, type ->
                                navController.navigate(Routes.MediaDetail(title, creator, type))
                            }
                        )"""
)

with open(file_path, "w") as f:
    f.write(text)
