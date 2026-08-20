import re
file_path = "app/src/main/java/com/example/ui/screens/LibraryScreen.kt"
with open(file_path, "r") as f:
    text = f.read()

target = r'''private fun com.example.utils.formatDuration\(seconds: Long\): String \{
    val hrs = seconds / 3600
    val mins = \(seconds % 3600\) / 60
    return if \(hrs > 0\) "\$\{hrs\}h \$\{mins\}m" else "\$\{mins\}m"
\}'''

text = re.sub(target, "", text, flags=re.DOTALL)
with open(file_path, "w") as f:
    f.write(text)
