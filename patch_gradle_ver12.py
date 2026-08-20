import re

file_path = "app/build.gradle.kts"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace('versionCode = 24', 'versionCode = 25')
text = text.replace('versionName = "3.3"', 'versionName = "3.3.0"')

with open(file_path, "w") as f:
    f.write(text)
