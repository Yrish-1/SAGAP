package com.example.sagap.domain

fun timeAgo(timestamp: Long): String {
    val minutes = ((System.currentTimeMillis() - timestamp) / 60_000).toInt()
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        else -> "${minutes / 60} h ago"
    }
}