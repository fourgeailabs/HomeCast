import re

file_path = "app/build.gradle.kts"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace('versionCode = 22', 'versionCode = 23')
text = text.replace('versionName = "3.1"', 'versionName = "3.2"')

with open(file_path, "w") as f:
    f.write(text)
