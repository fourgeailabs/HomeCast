import re

file_path = "app/src/main/java/com/example/ui/screens/MainScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

target = """                composable(Routes.Settings) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onThemeToggle = onThemeToggle
                    )
                }
            }"""

replacement = """                composable(Routes.Settings) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onThemeToggle = onThemeToggle
                    )
                }
                composable(
                    route = "media_detail/{title}/{creator}/{type}"
                ) { backStackEntry ->
                    val title = backStackEntry.arguments?.getString("title") ?: ""
                    val creator = backStackEntry.arguments?.getString("creator") ?: ""
                    val type = backStackEntry.arguments?.getString("type") ?: ""
                    
                    MediaDetailScreen(
                        viewModel = viewModel,
                        title = title,
                        creator = creator,
                        type = type,
                        onBack = { navController.popBackStack() },
                        onCreatorClick = { navController.navigate(Routes.CreatorDetail(it)) },
                        onPlayReadClick = { /* Handled contextually */ }
                    )
                }
                composable(
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
                }
            }"""

if target in text:
    text = text.replace(target, replacement)
    with open(file_path, "w") as f:
        f.write(text)
    print("Success")
else:
    print("Target not found")
