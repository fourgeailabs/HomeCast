import re

file_path = "app/src/main/java/com/example/data/network/BookloreClient.kt"
with open(file_path, "r") as f:
    text = f.read()

login_method = """
    suspend fun login(hostUrl: String, username: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = hostUrl.trimEnd('/')
            val url = "$normalizedUrl/api/v1/auth/login"
            
            val jsonBody = "{\\"username\\":\\"$username\\", \\"password\\":\\"$password\\"}"
            val requestBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json"), jsonBody)
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
                
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string()
                if (bodyString != null) {
                    // Quick regex or moshi parse for accessToken
                    val matcher = java.util.regex.Pattern.compile("\\"accessToken\\"\\s*:\\s*\\"([^\"]+)\\"").matcher(bodyString)
                    if (matcher.find()) {
                        return@withContext Result.success(matcher.group(1) ?: "")
                    }
                }
            }
            return@withContext Result.failure(Exception("HTTP ${response.code}: Login failed"))
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
"""

text = text.replace("object BookloreClient {", "object BookloreClient {" + login_method)

with open(file_path, "w") as f:
    f.write(text)
