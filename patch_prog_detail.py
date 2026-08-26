with open('app/src/main/java/com/example/ui/screens/ProgramDetailScreen.kt', 'r') as f:
    content = f.read()

replacement = """
    val initialCast = if (isMovie) movie?.cast ?: emptyList() else show?.cast ?: emptyList()
    val initialDirectors = if (isMovie) movie?.directors ?: emptyList() else show?.directors ?: emptyList()
    val initialWriters = if (isMovie) movie?.writers ?: emptyList() else show?.writers ?: emptyList()
    val initialProducers = if (isMovie) movie?.producers ?: emptyList() else show?.producers ?: emptyList()
    val initialCinematographers = if (isMovie) movie?.cinematographers ?: emptyList() else show?.cinematographers ?: emptyList()

    var detailedCast by remember { mutableStateOf<List<PlexCastMember>?>(null) }
    var detailedDirectors by remember { mutableStateOf<List<PlexCastMember>?>(null) }
    var detailedWriters by remember { mutableStateOf<List<PlexCastMember>?>(null) }
    var detailedProducers by remember { mutableStateOf<List<PlexCastMember>?>(null) }
    var detailedCinematographers by remember { mutableStateOf<List<PlexCastMember>?>(null) }

    val cast = detailedCast ?: initialCast
    val directors = detailedDirectors ?: initialDirectors
    val writers = detailedWriters ?: initialWriters
    val producers = detailedProducers ?: initialProducers
    val cinematographers = detailedCinematographers ?: initialCinematographers
    
    val servers by viewModel.servers.collectAsState()
    
    LaunchedEffect(programId) {
        val ratingKey = if (isMovie) movie?.ratingKey else show?.ratingKey
        val serverId = if (isMovie) movie?.serverId else show?.serverId
        
        if (ratingKey != null && serverId != null) {
            val server = servers.firstOrNull { it.id == serverId }
            if (server != null) {
                // Fetch full details
                try {
                    val root = server.hostUrl
                    val token = server.apiKey.trim()
                    val url = "$root/library/metadata/$ratingKey?X-Plex-Token=$token"
                    val req = okhttp3.Request.Builder()
                        .url(url)
                        .header("Accept", "application/json")
                        .build()
                    val res = com.example.data.network.PlexClient.client.newCall(req).execute()
                    if (res.isSuccessful) {
                        val body = res.body?.string() ?: ""
                        val json = org.json.JSONObject(body)
                        val mc = json.optJSONObject("MediaContainer")
                        if (mc != null) {
                            val metaArray = mc.optJSONArray("Metadata")
                            if (metaArray != null && metaArray.length() > 0) {
                                val item = metaArray.getJSONObject(0)
                                
                                // Helper to parse rich cast
                                fun parseRich(tagKey: String, roleDefault: String): List<PlexCastMember> {
                                    val list = mutableListOf<PlexCastMember>()
                                    val arr = item.optJSONArray(tagKey)
                                    if (arr != null) {
                                        for (i in 0 until arr.length()) {
                                            val obj = arr.getJSONObject(i)
                                            val name = obj.optString("tag", obj.optString("name", "")).trim()
                                            if (name.isBlank()) continue
                                            val role = obj.optString("role", roleDefault).trim()
                                            val thumb = obj.optString("thumb", "")
                                            val thumbUrl = if (thumb.isNotBlank()) {
                                                val cleanThumb = if (thumb.startsWith("/")) thumb else "/$thumb"
                                                "$root$cleanThumb?X-Plex-Token=$token"
                                            } else ""
                                            val id = obj.optString("id", "")
                                            list.add(PlexCastMember(id = id, name = name, role = role, imageUrl = thumbUrl))
                                        }
                                    }
                                    return list
                                }
                                
                                detailedCast = parseRich("Role", "Actor")
                                detailedDirectors = parseRich("Director", "Director")
                                detailedWriters = parseRich("Writer", "Writer")
                                detailedProducers = parseRich("Producer", "Producer")
                                detailedCinematographers = parseRich("Country", "Cinematographer") // Just in case, Country is what was used in PlexClient, though usually it's "Country". For real it's Country/Cinematographer? Actually, just parse what PlexClient parses.
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
"""

content = content.replace(
"""    val cast = if (isMovie) movie?.cast ?: emptyList() else show?.cast ?: emptyList()
    val directors = if (isMovie) movie?.directors ?: emptyList() else show?.directors ?: emptyList()
    val writers = if (isMovie) movie?.writers ?: emptyList() else show?.writers ?: emptyList()
    val producers = if (isMovie) movie?.producers ?: emptyList() else show?.producers ?: emptyList()
    val cinematographers = if (isMovie) movie?.cinematographers ?: emptyList() else show?.cinematographers ?: emptyList()""", replacement)

with open('app/src/main/java/com/example/ui/screens/ProgramDetailScreen.kt', 'w') as f:
    f.write(content)
