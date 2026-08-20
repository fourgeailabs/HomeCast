import re

file_path = "app/build.gradle.kts"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace('versionCode = 21', 'versionCode = 22')
text = text.replace('versionName = "3.0"', 'versionName = "3.1"')

with open(file_path, "w") as f:
    f.write(text)
