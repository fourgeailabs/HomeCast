import re

file_path = "app/src/main/java/com/example/ui/screens/MainScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

target = """                composable(
                    route = "creator_detail/{name}"
                ) { backStackEntry ->
                    val name = backStackEntry.arguments?.getString("name") ?: ""
                    CreatorDetailScreen(
                        viewModel = viewModel,
                        name = name,
                        onBack = { navController.popBackStack() },
                        onMediaClick = { mediaTitle, mediaType ->
                            navController.navigate(Routes.MediaDetail(mediaTitle, name, mediaType))
                        }
                    )
                }"""

replacement = """                composable(
                    route = "creator_detail/{name}"
                ) { backStackEntry ->
                    val name = backStackEntry.arguments?.getString("name") ?: ""
                    CreatorDetailScreen(
                        viewModel = viewModel,
                        creatorName = name,
                        onBack = { navController.popBackStack() }
                    )
                }"""

if target in text:
    text = text.replace(target, replacement)
    with open(file_path, "w") as f:
        f.write(text)
    print("Success")
else:
    print("Target not found")
