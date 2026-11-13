package com.example.sagap.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun fmtMbps(value: Double): String = "%.1f".format(Locale.US, value)

fun timeAgo(timestamp: Long): String {
    val minutes = ((System.currentTimeMillis() - timestamp) / 60_000).toInt()
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        else -> "${minutes / 60} h ago"
    }
}

private fun startOfDay(ts: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = ts
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun daysAgo(ts: Long): Int =
    ((startOfDay(System.currentTimeMillis()) - startOfDay(ts)) / 86_400_000L).toInt()

private fun fmt(pattern: String, ts: Long): String =
    SimpleDateFormat(pattern, Locale.ENGLISH).format(Date(ts))

fun dayWord(ts: Long): String = when (daysAgo(ts)) {
    0 -> "Today"
    1 -> "Yesterday"
    else -> fmt("EEEE", ts)
}

fun dayLabel(ts: Long): String = "${dayWord(ts)} · ${fmt("d MMMM", ts)}"

fun clockTime(ts: Long): String = fmt("HH:mm", ts)

fun dayAndTime(ts: Long): String = "${dayWord(ts)}, ${clockTime(ts)}"

fun last7DaysLabel(): String {
    val end = System.currentTimeMillis()
    val start = end - 6 * 86_400_000L
    val sameMonth = fmt("MMMM", start) == fmt("MMMM", end)
    return if (sameMonth) "${fmt("d", start)}–${fmt("d MMMM", end)}"
    else "${fmt("d MMM", start)} – ${fmt("d MMM", end)}"
}
