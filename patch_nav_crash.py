import re

file_path = "app/src/main/java/com/example/ui/screens/MainScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

# Replace Routes
text = re.sub(
    r'fun MediaDetail\(title: String, creator: String, type: String\) = "media_detail/\$\{android\.net\.Uri\.encode\(title\)\}/\$\{android\.net\.Uri\.encode\(creator\)\}/\$type"',
    r'fun MediaDetail(title: String, creator: String, type: String) = "media_detail?title=${android.net.Uri.encode(title.ifEmpty { "Unknown" })}&creator=${android.net.Uri.encode(creator.ifEmpty { "Unknown" })}&type=$type"',
    text
)

text = re.sub(
    r'fun CreatorDetail\(name: String\) = "creator_detail/\$\{android\.net\.Uri\.encode\(name\)\}"',
    r'fun CreatorDetail(name: String) = "creator_detail?name=${android.net.Uri.encode(name.ifEmpty { "Unknown" })}"',
    text
)

# Replace composables in NavHost
text = re.sub(
    r'route = "media_detail/\{title\}/\{creator\}/\{type\}"',
    r'route = "media_detail?title={title}&creator={creator}&type={type}"',
    text
)

text = re.sub(
    r'route = "creator_detail/\{name\}"',
    r'route = "creator_detail?name={name}"',
    text
)

with open(file_path, "w") as f:
    f.write(text)
