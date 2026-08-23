package com.example.utils

fun formatDuration(durationSeconds: Long): String {
    val h = durationSeconds / 3600
    val m = (durationSeconds % 3600) / 60
    val s = durationSeconds % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}

fun sanitizeAuthorName(raw: String?): String {
    if (raw.isNullOrBlank()) return "Unknown"
    // Handle specific typical cases first
    var text = raw.trim()
    
    // Check if the name contains birth/death years or trailing commas/dashes
    text = text.replace(Regex("\\d{4}-\\d{4}"), "")
               .replace(Regex("\\d{4}-"), "")
               .replace(Regex("\\(\\d+-\\d+\\)"), "")
               .replace(Regex("\\(\\d+-\\s*\\)"), "")
               .trim()
    
    // If it contains a comma and is "Last, First" style, convert to "First Last"
    val parts = text.split(",").map { it.trim() }.filter { it.isNotBlank() }
    if (parts.size >= 2) {
        val last = parts[0]
        val first = parts[1]
        // If the second part isn't a date/year residue, combine
        if (!first.matches(Regex(".*\\d.*"))) {
            text = "$first $last"
        } else {
            text = last
        }
    }
    
    // Final clean up of special characters, brackets, and extra spaces
    var cleaned = text.replace(Regex("[\\(\\)\\[\\]\\.\\,]"), " ")
                      .replace(Regex("\\s+"), " ")
                      .trim()
                      
    if (cleaned.endsWith("-")) {
        cleaned = cleaned.substring(0, cleaned.length - 1).trim()
    }
    return cleaned.ifBlank { "Unknown" }
}

fun sanitizeGenreName(genre: String?, title: String?, description: String?): String {
    val raw = genre?.lowercase() ?: ""
    if (raw.contains("/") || raw.contains("://") || raw.contains("gutenberg") || raw.contains(".txt") || raw.contains(".mp3") || raw.contains("various") || raw.contains("uncategorized") || raw.isBlank() || raw == "unknown") {
        val text = ((title ?: "") + " " + (description ?: "")).lowercase()
        return when {
            text.contains("manga") || text.contains("comic") || text.contains("graphic novel") || text.contains("bonevolume") -> "Manga & Comics"
            text.contains("cyberpunk") || text.contains("neon") || text.contains("cyberspace") -> "Cyberpunk"
            text.contains("sci-fi") || text.contains("science fiction") || text.contains("space") || text.contains("galaxy") || text.contains("orbit") || text.contains("time machine") || text.contains("future") -> "Sci-Fi"
            text.contains("philosophy") || text.contains("philosophical") || text.contains("think") || text.contains("wisdom") || text.contains("sun tzu") -> "Philosophy"
            text.contains("horror") || text.contains("ghost") || text.contains("dracula") || text.contains("vampire") || text.contains("monster") || text.contains("frankenstein") || text.contains("creature") || text.contains("dark") || text.contains("haunting") -> "Horror"
            text.contains("classic") || text.contains("literature") || text.contains("novel") || text.contains("tragedy") || text.contains("gatsby") -> "Classic"
            text.contains("mystery") || text.contains("detective") || text.contains("holmes") || text.contains("watson") || text.contains("crime") -> "Mystery"
            else -> "Classic"
        }
    }
    return genre!!.trim().split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
