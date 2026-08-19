import re

file_path = "app/build.gradle.kts"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace('versionCode = 15', 'versionCode = 16')
text = text.replace('versionName = "2.4"', 'versionName = "2.5"')

with open(file_path, "w") as f:
    f.write(text)
