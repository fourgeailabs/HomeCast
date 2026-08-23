package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Calendar

data class MusicMix(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val category: String,
    val gradientColors: List<Color>,
    val icon: ImageVector,
    val coverUrl: String,
    val tracks: List<MusicTrack>
)

object MusicMixGenerator {

    /**
     * Generates a personalized "For You" mix by learning from the user's listening history,
     * favorite artists, genres, and recent plays.
     */
    fun generateForYouMix(
        allTracks: List<MusicTrack>,
        recentTracks: List<MusicTrack>
    ): MusicMix {
        if (allTracks.isEmpty()) {
            return MusicMix(
                id = "mix_for_you",
                title = "For You",
                subtitle = "Made for your musical journey",
                description = "AI learning your taste from listening history",
                category = "Personalized",
                gradientColors = listOf(Color(0xFF6366F1), Color(0xFFEC4899)),
                icon = Icons.Default.AutoAwesome,
                coverUrl = "",
                tracks = emptyList()
            )
        }

        val favoriteArtists = recentTracks
            .map { it.artist.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }

        val favoriteGenres = recentTracks
            .map { it.genre.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }

        val forYouTracks = mutableListOf<MusicTrack>()

        // 1. Add top recent tracks
        forYouTracks.addAll(recentTracks.take(6))

        // 2. Add tracks from top artists
        for (artist in favoriteArtists.take(3)) {
            val artistTracks = allTracks.filter { it.artist.equals(artist, ignoreCase = true) }
            forYouTracks.addAll(artistTracks.shuffled().take(3))
        }

        // 3. Add tracks from top genres
        for (genre in favoriteGenres.take(2)) {
            val genreTracks = allTracks.filter { it.genre.contains(genre, ignoreCase = true) }
            forYouTracks.addAll(genreTracks.shuffled().take(3))
        }

        // 4. Fill with smart library recommendations if small
        if (forYouTracks.size < 15) {
            val remaining = allTracks.filter { t -> forYouTracks.none { it.id == t.id } }.shuffled()
            forYouTracks.addAll(remaining.take(15 - forYouTracks.size))
        }

        val finalTracks = forYouTracks.distinctBy { it.id }
        val topArtistName = favoriteArtists.firstOrNull() ?: allTracks.firstOrNull()?.artist ?: "your library"
        val firstCover = finalTracks.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl ?: ""

        return MusicMix(
            id = "mix_for_you",
            title = "For You",
            subtitle = if (recentTracks.isNotEmpty()) "Inspired by $topArtistName & your listening history" else "Daily personalized discovery blend",
            description = "AI learned blend adapting to your listening patterns, favorite artists, and genres.",
            category = "Made For You",
            gradientColors = listOf(Color(0xFF6366F1), Color(0xFFEC4899)),
            icon = Icons.Default.AutoAwesome,
            coverUrl = firstCover,
            tracks = finalTracks
        )
    }

    /**
     * Generates dynamic AI mixes and categories that rotate multiple times a day
     * based on time-of-day slots, user history, and custom shuffle seeds.
     */
    fun generateDynamicMixes(
        allTracks: List<MusicTrack>,
        recentTracks: List<MusicTrack>,
        shuffleSeed: Int = 0
    ): List<MusicMix> {
        if (allTracks.isEmpty()) return emptyList()

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        // Rotate 4 times a day: early morning (5-11), afternoon (12-16), evening (17-21), night (22-4)
        val timeSlotIndex = when (hour) {
            in 5..11 -> 0  // Morning
            in 12..16 -> 1 // Afternoon
            in 17..21 -> 2 // Evening / Sunset
            else -> 3      // Late Night
        }

        val mixes = mutableListOf<MusicMix>()

        // 1. FOR YOU MIX (Personalized AI from listening history)
        val forYou = generateForYouMix(allTracks, recentTracks)
        mixes.add(forYou)

        // 2. TIME-OF-DAY HERO MIX (Rotates throughout the day)
        val timeOfDayMix = when (timeSlotIndex) {
            0 -> {
                val morningTracks = allTracks.filter {
                    val g = it.genre.lowercase()
                    g.contains("acoustic") || g.contains("pop") || g.contains("folk") || g.contains("jazz")
                }.ifEmpty { allTracks.shuffled().take(12) }
                MusicMix(
                    id = "mix_time_morning_${dayOfYear}_$shuffleSeed",
                    title = "Morning Awakening",
                    subtitle = "Fresh acoustic, light grooves & sunrise energy",
                    description = "Uplifting tempo and acoustic clarity to start your day with focus and positivity.",
                    category = "Time of Day",
                    gradientColors = listOf(Color(0xFFFFB300), Color(0xFFFF7043)),
                    icon = Icons.Default.WbSunny,
                    coverUrl = morningTracks.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl ?: "",
                    tracks = morningTracks
                )
            }
            1 -> {
                val afternoonTracks = allTracks.filter {
                    val g = it.genre.lowercase()
                    g.contains("rock") || g.contains("electronic") || g.contains("dance") || g.contains("pop")
                }.ifEmpty { allTracks.shuffled().take(12) }
                MusicMix(
                    id = "mix_time_afternoon_${dayOfYear}_$shuffleSeed",
                    title = "Midday Momentum",
                    subtitle = "High-tempo anthems & productive flow",
                    description = "Keep your midday momentum rolling with driving beats and energetic hooks.",
                    category = "Time of Day",
                    gradientColors = listOf(Color(0xFF00ACC1), Color(0xFF3949AB)),
                    icon = Icons.Default.Speed,
                    coverUrl = afternoonTracks.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl ?: "",
                    tracks = afternoonTracks
                )
            }
            2 -> {
                val sunsetTracks = allTracks.filter {
                    val g = it.genre.lowercase()
                    g.contains("jazz") || g.contains("soul") || g.contains("r&b") || g.contains("ambient")
                }.ifEmpty { allTracks.shuffled().take(12) }
                MusicMix(
                    id = "mix_time_sunset_${dayOfYear}_$shuffleSeed",
                    title = "Golden Hour Unwind",
                    subtitle = "Smooth chords, sunset warmth & relaxed tempos",
                    description = "Mellow textures to transition from work into evening relaxation.",
                    category = "Time of Day",
                    gradientColors = listOf(Color(0xFFFF5722), Color(0xFF9C27B0)),
                    icon = Icons.Default.WbTwilight,
                    coverUrl = sunsetTracks.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl ?: "",
                    tracks = sunsetTracks
                )
            }
            else -> {
                val nightTracks = allTracks.filter {
                    val g = it.genre.lowercase()
                    g.contains("ambient") || g.contains("lo-fi") || g.contains("electronic") || g.contains("classical")
                }.ifEmpty { allTracks.shuffled().take(12) }
                MusicMix(
                    id = "mix_time_night_${dayOfYear}_$shuffleSeed",
                    title = "Midnight Low-End",
                    subtitle = "Deep bass, lo-fi textures & late night haze",
                    description = "Intimate after-hours rhythms and hypnotic ambiance for nocturnal minds.",
                    category = "Time of Day",
                    gradientColors = listOf(Color(0xFF311B92), Color(0xFF000000)),
                    icon = Icons.Default.Nightlife,
                    coverUrl = nightTracks.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl ?: "",
                    tracks = nightTracks
                )
            }
        }
        mixes.add(timeOfDayMix)

        // 3. HEAVY ROTATION MIX
        if (recentTracks.isNotEmpty()) {
            val heavyTracks = recentTracks.take(12)
            mixes.add(
                MusicMix(
                    id = "mix_heavy_rotation",
                    title = "Heavy Rotation",
                    subtitle = "Your most played tracks on repeat",
                    description = "The anthems and grooves you keep coming back to.",
                    category = "Your Taste",
                    gradientColors = listOf(Color(0xFFE91E63), Color(0xFFFF5722)),
                    icon = Icons.Default.Repeat,
                    coverUrl = heavyTracks.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl ?: "",
                    tracks = heavyTracks
                )
            )
        }

        // 4. DEEP CUTS & DISCOVERY MIX (Tracks not played recently)
        val deepCuts = allTracks.filter { t -> recentTracks.none { r -> r.id == t.id } }.shuffled().take(15)
        if (deepCuts.isNotEmpty()) {
            mixes.add(
                MusicMix(
                    id = "mix_deep_cuts_$shuffleSeed",
                    title = "Deep Cuts & Hidden Gems",
                    subtitle = "Rediscover overlooked tracks in your library",
                    description = "Unplayed and rare gems waiting in your personal archive.",
                    category = "Discovery",
                    gradientColors = listOf(Color(0xFF00B4D8), Color(0xFF7209B7)),
                    icon = Icons.Default.Explore,
                    coverUrl = deepCuts.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl ?: "",
                    tracks = deepCuts
                )
            )
        }

        // 5. ARTIST SPOTLIGHT RADIO (Centered around user's top artist)
        val topArtist = recentTracks.firstOrNull()?.artist ?: allTracks.firstOrNull()?.artist ?: ""
        if (topArtist.isNotBlank()) {
            val artistTracks = allTracks.filter { it.artist.contains(topArtist, ignoreCase = true) }
            val relatedTracks = allTracks.filter { !it.artist.contains(topArtist, ignoreCase = true) }.shuffled().take(8)
            val combined = (artistTracks + relatedTracks).distinctBy { it.id }
            mixes.add(
                MusicMix(
                    id = "mix_artist_radio_$shuffleSeed",
                    title = "$topArtist & Similar",
                    subtitle = "Artist spotlight & sonic companions",
                    description = "A curated journey through the soundscapes of $topArtist and kindred artists.",
                    category = "Artist Radio",
                    gradientColors = listOf(Color(0xFF8E24AA), Color(0xFF3F51B5)),
                    icon = Icons.Default.Radio,
                    coverUrl = combined.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl ?: "",
                    tracks = combined
                )
            )
        }

        // 6. GENRE FUSION / CLASH MIX (Mix two distinct genres)
        val distinctGenres = allTracks.map { it.genre.trim() }.filter { it.isNotBlank() }.distinct()
        if (distinctGenres.size >= 2) {
            val g1 = distinctGenres[0]
            val g2 = distinctGenres[1]
            val g1Tracks = allTracks.filter { it.genre.equals(g1, ignoreCase = true) }.shuffled().take(6)
            val g2Tracks = allTracks.filter { it.genre.equals(g2, ignoreCase = true) }.shuffled().take(6)
            val fusion = (g1Tracks + g2Tracks).shuffled()
            mixes.add(
                MusicMix(
                    id = "mix_genre_fusion_$shuffleSeed",
                    title = "$g1 × $g2 Blend",
                    subtitle = "Cross-genre dynamic collision",
                    description = "An eclectic sonic crossover weaving two distinct musical worlds.",
                    category = "AI Fusion",
                    gradientColors = listOf(Color(0xFF00E676), Color(0xFF00B0FF)),
                    icon = Icons.Default.MergeType,
                    coverUrl = fusion.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl ?: "",
                    tracks = fusion
                )
            )
        }

        // 7. CHILLOUT LOUNGE MIX
        val chillTracks = allTracks.filter {
            val g = it.genre.lowercase()
            g.contains("ambient") || g.contains("jazz") || g.contains("acoustic") || g.contains("chill") || g.contains("lo-fi")
        }.ifEmpty { allTracks.shuffled().take(10) }
        mixes.add(
            MusicMix(
                id = "mix_chillout_$shuffleSeed",
                title = "Chillout Lounge",
                subtitle = "Laid back soul, calm textures & easy grooves",
                description = "Velveteen tempos designed for unwinding and deep relaxation.",
                category = "Vibe Mix",
                gradientColors = listOf(Color(0xFF4DB6AC), Color(0xFF5C6BC0)),
                icon = Icons.Default.Nightlight,
                coverUrl = chillTracks.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl ?: "",
                tracks = chillTracks
            )
        )

        // 8. HIGH VOLTAGE ANTHEMS MIX
        val highEnergyTracks = allTracks.filter {
            val g = it.genre.lowercase()
            g.contains("rock") || g.contains("metal") || g.contains("dance") || g.contains("electronic") || g.contains("hip hop")
        }.ifEmpty { allTracks.shuffled().take(10) }
        mixes.add(
            MusicMix(
                id = "mix_high_voltage_$shuffleSeed",
                title = "High Voltage Energy",
                subtitle = "Fast tempos, power chords & driving rhythms",
                description = "Pure adrenaline fuel for workouts, road trips, and peak motivation.",
                category = "Vibe Mix",
                gradientColors = listOf(Color(0xFFFF1744), Color(0xFFFF6D00)),
                icon = Icons.Default.ElectricBolt,
                coverUrl = highEnergyTracks.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl ?: "",
                tracks = highEnergyTracks
            )
        )

        return mixes
    }
}
