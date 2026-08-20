import re

file_path = "app/build.gradle.kts"
with open(file_path, "r") as f:
    text = f.read()

text = text.replace('versionCode = 23', 'versionCode = 24')
text = text.replace('versionName = "3.2"', 'versionName = "3.3"')

with open(file_path, "w") as f:
    f.write(text)
