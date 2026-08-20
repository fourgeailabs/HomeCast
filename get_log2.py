import subprocess
result = subprocess.run(["gradle", ":app:compileDebugKotlin"], capture_output=True, text=True)
print(result.stderr)
print(result.stdout)
