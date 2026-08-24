package com.example.data.network

import android.util.Log
import com.example.data.PublicDomainPodcastsCatalog
import com.example.ui.screens.PodcastChannel
import com.example.ui.screens.PodcastEpisode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object PodcastClient {
    private const val TAG = "PodcastClient"

    private val okHttpClient = OptimizedNetworkEngine.client

    private fun isVideoUrl(url: String, rawType: String = ""): Boolean {
        val lowerUrl = url.lowercase()
        val lowerType = rawType.lowercase()
        return lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".m4v") || lowerUrl.endsWith(".webm") ||
                lowerUrl.endsWith(".mov") || lowerUrl.endsWith(".mkv") || lowerUrl.contains("/video/") ||
                lowerType.contains("video") || lowerType.contains("mp4")
    }

    /**
     * Live search across iTunes Public Podcasts, Archive.org Audio Podcasts,
     * and the curated Public Domain Podcasts catalog.
     */
    suspend fun searchPodcasts(
        query: String = "",
        category: String = "All"
    ): List<PodcastChannel> = withContext(Dispatchers.IO) {
        val results = mutableListOf<PodcastChannel>()

        // 1. Filter local curated catalog
        val curatedFiltered = PublicDomainPodcastsCatalog.curatedPodcasts.filter { item ->
            val matchesCat = (category == "All" || item.category.equals(category, ignoreCase = true) || item.category.contains(category, ignoreCase = true))
            val matchesQuery = query.isBlank() || item.title.contains(query, ignoreCase = true) || item.description.contains(query, ignoreCase = true) || item.publisher.contains(query, ignoreCase = true)
            matchesCat && matchesQuery
        }
        results.addAll(curatedFiltered)

        // 2. Query iTunes Podcast API if search query is provided or category is selected
        val searchTerm = when {
            query.isNotBlank() -> query
            category != "All" -> category
            else -> ""
        }

        if (searchTerm.isNotBlank()) {
            val itunesPodcasts = searchITunesPodcasts(searchTerm)
            results.addAll(itunesPodcasts)

            val archivePodcasts = searchArchiveOrgPodcasts(searchTerm)
            results.addAll(archivePodcasts)
        }

        results.distinctBy { "${it.title.lowercase().trim()}___${it.publisher.lowercase().trim()}" }
    }

    /**
     * Queries iTunes Public Podcast Search API.
     * Endpoint: https://itunes.apple.com/search?media=podcast&limit=30&term=...
     */
    private fun searchITunesPodcasts(term: String): List<PodcastChannel> {
        val list = mutableListOf<PodcastChannel>()
        try {
            val encoded = URLEncoder.encode(term, "UTF-8")
            val url = "https://itunes.apple.com/search?media=podcast&limit=30&term=$encoded"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful && resp.body != null) {
                val jsonStr = resp.body!!.string()
                val root = JSONObject(jsonStr)
                val resultsArray = root.optJSONArray("results") ?: return emptyList()

                for (i in 0 until resultsArray.length()) {
                    val obj = resultsArray.getJSONObject(i)
                    val collectionId = obj.optLong("collectionId", 0L)
                    val title = obj.optString("collectionName", obj.optString("trackName", "Podcast"))
                    val artist = obj.optString("artistName", "Public Broadcaster")
                    val coverUrl = obj.optString("artworkUrl600", obj.optString("artworkUrl100", ""))
                    val feedUrl = obj.optString("feedUrl", "itunes:$collectionId")
                    val primaryGenre = obj.optString("primaryGenreName", "General")

                    if (title.isNotBlank() && coverUrl.isNotBlank()) {
                        list.add(
                            PodcastChannel(
                                id = "itunes_$collectionId",
                                title = title,
                                publisher = artist,
                                coverUrl = coverUrl,
                                description = "Category: $primaryGenre • Live Feed from iTunes Podcast Directory",
                                category = primaryGenre,
                                feedUrl = feedUrl,
                                isPublic = true,
                                episodes = listOf(
                                    PodcastEpisode(
                                        id = "itunes_ep_${collectionId}_1",
                                        title = "Episode 1 • $title",
                                        podcastTitle = title,
                                        publisher = artist,
                                        durationSeconds = 2400L,
                                        audioUrl = feedUrl,
                                        coverUrl = coverUrl,
                                        publishDate = "Recent Broadcast",
                                        description = "Tap to listen to latest audio stream from $title."
                                    )
                                )
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching iTunes podcasts for $term", e)
        }
        return list
    }

    /**
     * Queries Archive.org Audio/Podcast Search API.
     */
    private fun searchArchiveOrgPodcasts(term: String): List<PodcastChannel> {
        val list = mutableListOf<PodcastChannel>()
        try {
            val encoded = URLEncoder.encode(term, "UTF-8")
            val url = "https://archive.org/advancedsearch.php?q=mediatype:audio+AND+(subject:podcast+OR+subject:radio+OR+collection:librivoxaudio)+AND+title:$encoded&fl[]=identifier,title,creator,description,downloads,coverUrl&rows=20&output=json"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "HomeCast-PodcastClient/1.0")
                .build()

            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful && resp.body != null) {
                val jsonStr = resp.body!!.string()
                val root = JSONObject(jsonStr)
                val docs = root.optJSONObject("response")?.optJSONArray("docs") ?: return emptyList()

                for (i in 0 until docs.length()) {
                    val doc = docs.getJSONObject(i)
                    val id = doc.optString("identifier", "")
                    val title = doc.optString("title", "Archive Audio")
                    val creator = doc.optString("creator", "Public Domain")
                    val desc = doc.optString("description", "Public domain audio recording from Internet Archive.")

                    if (id.isNotBlank() && title.isNotBlank()) {
                        val coverUrl = "https://archive.org/services/img/$id"
                        val streamUrl = "https://archive.org/download/$id/${id}.mp3"

                        list.add(
                            PodcastChannel(
                                id = "archive_pod_$id",
                                title = title,
                                publisher = creator,
                                coverUrl = coverUrl,
                                description = desc.take(150),
                                category = "Archive Radio",
                                feedUrl = "https://archive.org/details/$id",
                                isPublic = true,
                                episodes = listOf(
                                    PodcastEpisode(
                                        id = "archive_ep_$id",
                                        title = title,
                                        podcastTitle = title,
                                        publisher = creator,
                                        durationSeconds = 2100L,
                                        audioUrl = streamUrl,
                                        coverUrl = coverUrl,
                                        publishDate = "Public Domain Archive",
                                        description = desc
                                    )
                                )
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching Archive.org podcasts for $term", e)
        }
        return list
    }

    /**
     * Fetches live episodes from RSS XML feed or iTunes Lookup API.
     */
    suspend fun fetchChannelEpisodes(channel: PodcastChannel): List<PodcastEpisode> = withContext(Dispatchers.IO) {
        if (channel.episodes.size > 2) {
            return@withContext channel.episodes
        }

        val fetched = mutableListOf<PodcastEpisode>()

        // 1. If iTunes collection ID
        if (channel.id.startsWith("itunes_")) {
            val collectionId = channel.id.substringAfter("itunes_")
            val itunesEps = fetchITunesEpisodes(collectionId, channel)
            if (itunesEps.isNotEmpty()) return@withContext itunesEps
        }

        // 2. If valid HTTP RSS Feed URL
        if (channel.feedUrl.startsWith("http://") || channel.feedUrl.startsWith("https://")) {
            val rssEps = parseRssFeedEpisodes(channel.feedUrl, channel)
            if (rssEps.isNotEmpty()) return@withContext rssEps
        }

        // Fallback to existing episodes
        channel.episodes
    }

    private fun fetchITunesEpisodes(collectionId: String, channel: PodcastChannel): List<PodcastEpisode> {
        val list = mutableListOf<PodcastEpisode>()
        try {
            val url = "https://itunes.apple.com/lookup?id=$collectionId&entity=podcastEpisode&limit=30"
            val req = Request.Builder().url(url).build()
            val resp = okHttpClient.newCall(req).execute()

            if (resp.isSuccessful && resp.body != null) {
                val jsonStr = resp.body!!.string()
                val root = JSONObject(jsonStr)
                val results = root.optJSONArray("results") ?: return emptyList()

                for (i in 0 until results.length()) {
                    val obj = results.getJSONObject(i)
                    if (obj.optString("wrapperType") == "podcastEpisode") {
                        val epId = obj.optString("trackId", "$i")
                        val title = obj.optString("trackName", "Episode ${i + 1}")
                        val streamUrl = obj.optString("episodeUrl", obj.optString("previewUrl", ""))
                        val durationMs = obj.optLong("trackTimeMillis", 1800000L)
                        val desc = obj.optString("description", "")
                        val dateStr = obj.optString("releaseDate", "Recent").take(10)

                        if (streamUrl.isNotBlank()) {
                            val isVid = isVideoUrl(streamUrl, obj.optString("episodeContentType", ""))
                            list.add(
                                PodcastEpisode(
                                    id = "itunes_ep_${collectionId}_$epId",
                                    title = title,
                                    podcastTitle = channel.title,
                                    publisher = channel.publisher,
                                    durationSeconds = durationMs / 1000L,
                                    audioUrl = streamUrl,
                                    coverUrl = channel.coverUrl,
                                    publishDate = dateStr,
                                    description = desc,
                                    isVideo = isVid
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching iTunes episodes for $collectionId", e)
        }
        return list
    }

    private fun parseRssFeedEpisodes(feedUrl: String, channel: PodcastChannel): List<PodcastEpisode> {
        val list = mutableListOf<PodcastEpisode>()
        try {
            val req = Request.Builder().url(feedUrl).header("User-Agent", "Mozilla/5.0").build()
            val resp = okHttpClient.newCall(req).execute()
            if (resp.isSuccessful && resp.body != null) {
                val xmlStr = resp.body!!.string()
                val itemRegex = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
                val titleRegex = Regex("<title>(.*?)</title>")
                val enclosureRegex = Regex("<enclosure[^>]+url=[\"']([^\"']+)[\"'](?:[^>]*type=[\"']([^\"']+)[\"'])?")
                val pubDateRegex = Regex("<pubDate>(.*?)</pubDate>")

                var count = 0
                for (match in itemRegex.findAll(xmlStr)) {
                    if (count >= 25) break
                    val itemText = match.groupValues[1]
                    val title = titleRegex.find(itemText)?.groupValues?.get(1)?.replace("<![CDATA[", "")?.replace("]]>", "")?.trim() ?: "Episode ${count + 1}"
                    val enclosureMatch = enclosureRegex.find(itemText)
                    val audioUrl = enclosureMatch?.groupValues?.get(1) ?: ""
                    val mimeType = enclosureMatch?.groupValues?.getOrNull(2) ?: ""
                    val pubDate = pubDateRegex.find(itemText)?.groupValues?.get(1)?.take(16) ?: "Recent"

                    if (audioUrl.isNotBlank()) {
                        val isVid = isVideoUrl(audioUrl, mimeType)
                        list.add(
                            PodcastEpisode(
                                id = "rss_ep_${channel.id}_$count",
                                title = title,
                                podcastTitle = channel.title,
                                publisher = channel.publisher,
                                durationSeconds = 1800L,
                                audioUrl = audioUrl,
                                coverUrl = channel.coverUrl,
                                publishDate = pubDate,
                                description = "From ${channel.title} feed",
                                isVideo = isVid
                            )
                        )
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing RSS feed $feedUrl", e)
        }
        return list
    }
}
