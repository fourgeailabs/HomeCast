import re

file_path = "app/build.gradle.kts"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace('versionCode = 16', 'versionCode = 17')
text = text.replace('versionName = "2.5"', 'versionName = "2.6"')

with open(file_path, "w") as f:
    f.write(text)
