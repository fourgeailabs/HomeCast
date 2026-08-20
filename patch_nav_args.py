import re

file_path = "app/src/main/java/com/example/ui/screens/MainScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

# Make sure imports exist
if "import androidx.navigation.navArgument" not in text:
    text = text.replace("import androidx.navigation.compose.rememberNavController", "import androidx.navigation.compose.rememberNavController\nimport androidx.navigation.navArgument\nimport androidx.navigation.NavType")

# Replace composable block for media_detail
target_media = """                composable(
                    route = "media_detail?title={title}&creator={creator}&type={type}"
                ) { backStackEntry ->"""

replacement_media = """                composable(
                    route = "media_detail?title={title}&creator={creator}&type={type}",
                    arguments = listOf(
                        navArgument("title") { type = NavType.StringType; defaultValue = "Unknown" },
                        navArgument("creator") { type = NavType.StringType; defaultValue = "Unknown" },
                        navArgument("type") { type = NavType.StringType; defaultValue = "BOOK" }
                    )
                ) { backStackEntry ->"""

text = text.replace(target_media, replacement_media)

# Replace composable block for creator_detail
target_creator = """                composable(
                    route = "creator_detail?name={name}"
                ) { backStackEntry ->"""

replacement_creator = """                composable(
                    route = "creator_detail?name={name}",
                    arguments = listOf(
                        navArgument("name") { type = NavType.StringType; defaultValue = "Unknown" }
                    )
                ) { backStackEntry ->"""

text = text.replace(target_creator, replacement_creator)

with open(file_path, "w") as f:
    f.write(text)
