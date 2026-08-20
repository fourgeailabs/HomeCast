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
