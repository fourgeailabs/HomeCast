import re

file_path = "app/build.gradle.kts"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace('versionCode = 19', 'versionCode = 20')
text = text.replace('versionName = "2.8"', 'versionName = "2.9"')

with open(file_path, "w") as f:
    f.write(text)
