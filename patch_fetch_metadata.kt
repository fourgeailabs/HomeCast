    suspend fun fetchDetailedMetadata(
        serverUrl: String,
        token: String,
        serverId: String,
        ratingKey: String,
        candidateUrls: List<String> = emptyList()
    ): Result<org.json.JSONObject> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        val allCandidates = (listOf(serverUrl) + candidateUrls)
            .map { normalizeUrl(it) }
            .filter { it.isNotBlank() }
            .distinct()
        
        for (root in allCandidates) {
            val qUrl = "$root/library/metadata/$ratingKey?X-Plex-Token=$cleanToken"
            try {
                val req = Request.Builder()
                    .url(qUrl)
                    .get()
                    .apply { buildStandardHeaders(this, cleanToken) }
                    .build()
                val res = client.newCall(req).execute()
                if (res.isSuccessful) {
                    val body = res.body?.string() ?: ""
                    val parsed = parseJsonArrayOrObjectList(body, "MediaContainer", "Metadata")
                    if (parsed.isNotEmpty()) {
                        val item = parsed.first()
                        item.put("_workingRoot", root)
                        return@withContext Result.success(item)
                    }
                }
            } catch (_: Exception) {}
        }
        Result.failure(Exception("Failed to fetch detailed metadata"))
    }
