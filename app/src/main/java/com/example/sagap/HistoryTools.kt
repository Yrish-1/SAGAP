package com.example.sagap

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// The filter chips on the History screen: "All 6", "Good+ 4", "Needs attention 2"
enum class HistoryFilter { ALL, GOOD_PLUS, NEEDS_ATTENTION }

// The three numbers in the dark summary card (counts for the whole week, before the chip filter)
data class HistoryStats(val total: Int, val goodOrBetter: Int, val needAttention: Int)

// "Today · 8 November" followed by that day's tests
data class DayGroup(val label: String, val tests: List<TestResult>)

data class HistoryView(
    val weekStartMillis: Long,
    val weekLabel: String,       // "This week · 2–8 November"
    val stats: HistoryStats,
    val groups: List<DayGroup>   // newest day first, already filtered by the chip
)

fun isGoodOrBetter(r: TestResult): Boolean = r.rating == "Good" || r.rating == "Excellent"

object HistoryTools {

    private fun startOfDay(ms: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun addDays(ms: Long, days: Int): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        c.add(Calendar.DAY_OF_YEAR, days)
        return c.timeInMillis
    }

    // Monday 00:00 of the week that contains [now]
    fun weekStart(now: Long = System.currentTimeMillis()): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = startOfDay(now)
        val daysSinceMonday = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0 ... Sunday = 6
        c.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
        return c.timeInMillis
    }

    // Move to another week: shiftWeek(start, -1) = previous week
    fun shiftWeek(weekStartMillis: Long, weeks: Int): Long = addDays(weekStartMillis, weeks * 7)

    fun weekLabel(weekStartMillis: Long, now: Long = System.currentTimeMillis()): String {
        val loc = Locale.getDefault()
        val endMillis = addDays(weekStartMillis, 6)
        val s = Calendar.getInstance().apply { timeInMillis = weekStartMillis }
        val e = Calendar.getInstance().apply { timeInMillis = endMillis }
        val range = if (s.get(Calendar.MONTH) == e.get(Calendar.MONTH)) {
            "${SimpleDateFormat("d", loc).format(s.time)}–${SimpleDateFormat("d MMMM", loc).format(e.time)}"
        } else {
            "${SimpleDateFormat("d MMM", loc).format(s.time)}–${SimpleDateFormat("d MMM", loc).format(e.time)}"
        }
        val isThisWeek = weekStartMillis == weekStart(now)
        return (if (isThisWeek) "This week" else "Week") + " · " + range
    }

    private fun dayLabel(dayStartMillis: Long, now: Long): String {
        val loc = Locale.getDefault()
        val today = startOfDay(now)
        val date = java.util.Date(dayStartMillis)
        return when (dayStartMillis) {
            today -> "Today · " + SimpleDateFormat("d MMMM", loc).format(date)
            addDays(today, -1) -> "Yesterday · " + SimpleDateFormat("d MMMM", loc).format(date)
            else -> SimpleDateFormat("EEEE · d MMMM", loc).format(date)
        }
    }

    // Everything the History screen needs for one week and one chip
    fun view(
        context: Context,
        weekStartMillis: Long = weekStart(),
        filter: HistoryFilter = HistoryFilter.ALL,
        now: Long = System.currentTimeMillis()
    ): HistoryView {
        val endMillis = addDays(weekStartMillis, 7)
        val weekTests = ResultStore.load(context)
            .filter { it.timeMillis >= weekStartMillis && it.timeMillis < endMillis }
            .sortedByDescending { it.timeMillis }

        val good = weekTests.count { isGoodOrBetter(it) }
        val stats = HistoryStats(weekTests.size, good, weekTests.size - good)

        val shown = when (filter) {
            HistoryFilter.ALL -> weekTests
            HistoryFilter.GOOD_PLUS -> weekTests.filter { isGoodOrBetter(it) }
            HistoryFilter.NEEDS_ATTENTION -> weekTests.filter { !isGoodOrBetter(it) }
        }
        val groups = shown
            .groupBy { startOfDay(it.timeMillis) }
            .entries
            .sortedByDescending { it.key }
            .map { DayGroup(dayLabel(it.key, now), it.value) }

        return HistoryView(weekStartMillis, weekLabel(weekStartMillis, now), stats, groups)
    }
}
