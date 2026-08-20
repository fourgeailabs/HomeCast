import re

file_path = "app/build.gradle.kts"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace('versionCode = 20', 'versionCode = 21')
text = text.replace('versionName = "2.9"', 'versionName = "3.0"')

with open(file_path, "w") as f:
    f.write(text)
