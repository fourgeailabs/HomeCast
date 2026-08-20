import re

file_path = "app/build.gradle.kts"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace('versionCode = 18', 'versionCode = 19')
text = text.replace('versionName = "2.7"', 'versionName = "2.8"')

with open(file_path, "w") as f:
    f.write(text)
