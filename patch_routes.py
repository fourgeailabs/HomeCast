import re

file_path = "app/src/main/java/com/example/ui/screens/MainScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

new_routes = """    const val Discovery = "discovery"
    const val Settings = "settings"
    
    fun MediaDetail(title: String, creator: String, type: String) = "media_detail/${android.net.Uri.encode(title)}/${android.net.Uri.encode(creator)}/$type"
    fun CreatorDetail(name: String) = "creator_detail/${android.net.Uri.encode(name)}"
"""
text = text.replace('    const val Discovery = "discovery"\n    const val Settings = "settings"', new_routes)

inject = """
                composable(
                    route = "media_detail/{title}/{creator}/{type}",
                    arguments = listOf(
                        androidx.navigation.navArgument("title") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("creator") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("type") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val title = android.net.Uri.decode(backStackEntry.arguments?.getString("title") ?: "")
                    val creator = android.net.Uri.decode(backStackEntry.arguments?.getString("creator") ?: "")
                    val type = backStackEntry.arguments?.getString("type") ?: "BOOK"
                    
                    MediaDetailScreen(
                        viewModel = viewModel,
                        title = title,
                        creator = creator,
                        type = type,
                        onBack = { navController.popBackStack() },
                        onCreatorClick = { name ->
                            navController.navigate(Routes.CreatorDetail(name))
                        },
                        onPlayReadClick = {
                            if (type == "MUSIC") {
                                val track = viewModel.allMusic.value.firstOrNull { it.title.equals(title, ignoreCase = true) }
                                if (track != null) viewModel.playMusicTrack(track)
                            } else if (type == "AUDIOBOOK") {
                                val book = viewModel.allBooks.value.firstOrNull { it.title.equals(title, ignoreCase = true) }
                                if (book != null) viewModel.playAudiobook(book)
                            } else {
                                val ebook = viewModel.allEBooks.value.firstOrNull { it.title.equals(title, ignoreCase = true) }
                                if (ebook != null) {
                                    // Not hoisting selectedBookForReading, just returning to ebook screen
                                }
                            }
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = "creator_detail/{name}",
                    arguments = listOf(
                        androidx.navigation.navArgument("name") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val name = android.net.Uri.decode(backStackEntry.arguments?.getString("name") ?: "")
                    CreatorDetailScreen(
                        viewModel = viewModel,
                        creatorName = name,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
"""

text = text.replace("            }\n\n            AnimatedVisibility(", inject + "\n            AnimatedVisibility(")

with open(file_path, "w") as f:
    f.write(text)
