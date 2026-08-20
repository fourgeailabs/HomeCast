import re

file_path = "app/src/main/java/com/example/data/network/BookloreClient.kt"
with open(file_path, "r") as f:
    text = f.read()

replacement = """    suspend fun login(hostUrl: String, username: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = hostUrl.trimEnd('/')
            val url = "$normalizedUrl/api/v1/auth/login"
            
            // Build JSON safely
            val jsonBody = org.json.JSONObject()
            jsonBody.put("username", username)
            jsonBody.put("password", password)
            
            val requestBody = okhttp3.RequestBody.create("application/json".toMediaType(), jsonBody.toString())
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
                
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string()
                if (bodyString != null) {
                    try {
                        val json = org.json.JSONObject(bodyString)
                        val token = json.optString("accessToken", "")
                        if (token.isNotEmpty()) {
                            return@withContext Result.success(token)
                        }
                    } catch (e: Exception) {
                        return@withContext Result.failure(Exception("Failed to parse login response"))
                    }
                }
            }
            return@withContext Result.failure(Exception("HTTP ${response.code}: Login failed"))
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }"""

text = re.sub(r"    suspend fun login\([\s\S]*?\}\n    \}", replacement, text)

with open(file_path, "w") as f:
    f.write(text)
