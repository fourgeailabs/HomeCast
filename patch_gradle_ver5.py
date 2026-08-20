import re

file_path = "app/build.gradle.kts"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace('versionCode = 17', 'versionCode = 18')
text = text.replace('versionName = "2.6"', 'versionName = "2.7"')

with open(file_path, "w") as f:
    f.write(text)
