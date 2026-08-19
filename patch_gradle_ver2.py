import re

file_path = "app/build.gradle.kts"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace('versionCode = 14', 'versionCode = 15')
text = text.replace('versionName = "2.3"', 'versionName = "2.4"')

with open(file_path, "w") as f:
    f.write(text)
