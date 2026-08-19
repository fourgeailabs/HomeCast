import re

file_path = "app/src/main/AndroidManifest.xml"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace(
    '<application',
    '<application\n        android:name=".MyApplication"'
)

with open(file_path, "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/MyApplication.kt", "w") as f:
    f.write("""package com.example

import android.app.Application
import android.content.Intent
import android.os.Build

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
""")
