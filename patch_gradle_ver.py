import re

file_path = "app/build.gradle.kts"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace('versionCode = 13', 'versionCode = 14')
text = text.replace('versionName = "2.2"', 'versionName = "2.3"')

with open(file_path, "w") as f:
    f.write(text)
