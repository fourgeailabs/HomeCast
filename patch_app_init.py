import re

file_path = "app/src/main/java/com/example/MyApplication.kt"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace(
    "class MyApplication : Application() {",
    """class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val intent = android.content.Intent(this, PlaybackService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }"""
)

with open(file_path, "w") as f:
    f.write(text)
