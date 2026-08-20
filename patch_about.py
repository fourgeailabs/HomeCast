import re

file_path = "app/src/main/java/com/example/ui/screens/SettingsScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

target = """        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}"""

replacement = """        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text("About", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Booklore", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("Version ${com.example.BuildConfig.VERSION_NAME}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Made by FourgeAI LABS", fontSize = 14.sp)
                    Text("© 2026", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    OutlinedButton(
                        onClick = { uriHandler.openUri("https://github.com/booklore-app/booklore") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.Code, contentDescription = "GitHub")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View on GitHub")
                    }
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}"""

if target in text:
    text = text.replace(target, replacement)
    with open(file_path, "w") as f:
        f.write(text)
    print("Success")
else:
    print("Target not found")
