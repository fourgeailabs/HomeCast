import re

file_path = "app/src/main/java/com/example/ui/screens/MainScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

# Add detail routes
routes_update = """    object Music : Routes("music")
    object EBooks : Routes("ebooks")
    object Settings : Routes("settings")
    object MediaDetail : Routes("media_detail/{title}/{creator}/{type}") {
        fun createRoute(title: String, creator: String, type: String) = "media_detail/${Uri.encode(title)}/${Uri.encode(creator)}/$type"
    }
    object CreatorDetail : Routes("creator_detail/{name}") {
        fun createRoute(name: String) = "creator_detail/${Uri.encode(name)}"
    }"""

text = text.replace(
    """    object Music : Routes("music")
    object EBooks : Routes("ebooks")
    object Settings : Routes("settings")""",
    routes_update
)

nav_entries = """                composable(Routes.EBooks.route) {
                    EBooksScreen(
                        viewModel = viewModel,
                        onOpenEBook = { book ->
                            selectedBookForReading = book
                        },
                        onOpenComic = { comic ->
                            selectedComicForReading = comic
                        },
                        onNavigateToSettings = { navController.navigate(Routes.Settings.route) },
                        onNavigateToDetails = { title, creator, type ->
                            navController.navigate(Routes.MediaDetail.createRoute(title, creator, type))
                        },
                        onNavigateToCreator = { name ->
                            navController.navigate(Routes.CreatorDetail.createRoute(name))
                        }
                    )
                }
                
                composable(
                    route = Routes.MediaDetail.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("title") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("creator") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("type") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val title = Uri.decode(backStackEntry.arguments?.getString("title") ?: "")
                    val creator = Uri.decode(backStackEntry.arguments?.getString("creator") ?: "")
                    val type = backStackEntry.arguments?.getString("type") ?: "BOOK"
                    
                    MediaDetailScreen(
                        viewModel = viewModel,
                        title = title,
                        creator = creator,
                        type = type,
                        onBack = { navController.popBackStack() },
                        onCreatorClick = { name ->
                            navController.navigate(Routes.CreatorDetail.createRoute(name))
                        },
                        onPlayReadClick = {
                            if (type == "MUSIC" || type == "AUDIOBOOK") {
                                // Simple play logic handled mostly on other screens, 
                                // but we could tell VM to search and play it.
                            } else {
                                // Note: full implementation of read click would link to the actual book.
                                // Because we are keeping it simple, we just pop back for now or show a toast.
                                navController.popBackStack()
                            }
                        }
                    )
                }

                composable(
                    route = Routes.CreatorDetail.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("name") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val name = Uri.decode(backStackEntry.arguments?.getString("name") ?: "")
                    CreatorDetailScreen(
                        viewModel = viewModel,
                        creatorName = name,
                        onBack = { navController.popBackStack() }
                    )
                }"""

# Replace just EBooksScreen for now to avoid complexity of replacing all screens
# Wait, it's safer to just inject at the bottom of the NavHost.

inject = """
                composable(
                    route = Routes.MediaDetail.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("title") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("creator") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("type") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val title = Uri.decode(backStackEntry.arguments?.getString("title") ?: "")
                    val creator = Uri.decode(backStackEntry.arguments?.getString("creator") ?: "")
                    val type = backStackEntry.arguments?.getString("type") ?: "BOOK"
                    
                    MediaDetailScreen(
                        viewModel = viewModel,
                        title = title,
                        creator = creator,
                        type = type,
                        onBack = { navController.popBackStack() },
                        onCreatorClick = { name ->
                            navController.navigate(Routes.CreatorDetail.createRoute(name))
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
                                    // Normally we would launch EBookReader, but the reader is managed by state on MainScreen.
                                    // A robust implementation would hoist that state.
                                }
                            }
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = Routes.CreatorDetail.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("name") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val name = Uri.decode(backStackEntry.arguments?.getString("name") ?: "")
                    CreatorDetailScreen(
                        viewModel = viewModel,
                        creatorName = name,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
"""

text = text.replace("            }\n\n            AnimatedVisibility(", inject + "\n            AnimatedVisibility(")

# add Uri import
text = text.replace("import android.widget.Toast", "import android.widget.Toast\nimport android.net.Uri")

with open(file_path, "w") as f:
    f.write(text)
