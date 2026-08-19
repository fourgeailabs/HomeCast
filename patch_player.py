import re

file_path = "app/src/main/java/com/example/ui/screens/PlayerScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace(
    "fun PlayerScreen(\n    viewModel: MainViewModel,\n    onCollapse: () -> Unit\n) {",
    "fun PlayerScreen(\n    viewModel: MainViewModel,\n    onCollapse: () -> Unit,\n    onNavigateToCreator: (String) -> Unit = {},\n    onNavigateToMedia: (String, String, String) -> Unit = {_,_,_->}\n) {"
)

# Text for title and creator
text = text.replace(
    """                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = authorOrArtist,
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )""",
    """                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { 
                        onCollapse()
                        onNavigateToMedia(title, authorOrArtist, if (playbackState.currentMusicTrack != null) "MUSIC" else "AUDIOBOOK")
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = authorOrArtist,
                    fontSize = 18.sp,
                    color = AccentTeal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { 
                        onCollapse()
                        onNavigateToCreator(authorOrArtist)
                    }
                )"""
)

# Album name (if any)
text = text.replace(
    """                        Text(
                            text = album,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )""",
    """                        Text(
                            text = album,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { 
                                onCollapse()
                                onNavigateToMedia(album, authorOrArtist, "MUSIC")
                            }
                        )"""
)

with open(file_path, "w") as f:
    f.write(text)
