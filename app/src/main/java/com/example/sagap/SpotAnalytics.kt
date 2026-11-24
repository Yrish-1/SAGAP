package com.example.sagap

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val RECENT_WINDOW_MS = 24L * 60 * 60 * 1000     // map averages: last 24 hours (as in the wireframe)
const val REPORT_WINDOW_MS = 24L * 60 * 60 * 1000     // reports counted: last 24 hours
const val HAZARD_HIDE_COUNT = 3                       // this many hazard reports put a spot on safety hold
const val DEAD_ZONE_RED_COUNT = 3                     // this many dead-zone reports make a spot red

// Map color from a grade (no grade = no recent data = gray)
fun statusOf(grade: Grade?): SpotStatus = when (grade) {
    null -> SpotStatus.GRAY
    Grade.POOR -> SpotStatus.RED
    Grade.FAIR -> SpotStatus.YELLOW
    Grade.GOOD, Grade.EXCELLENT -> SpotStatus.GREEN
}

// Headline on the result screen
fun headlineFor(grade: Grade): String = when (grade) {
    Grade.EXCELLENT, Grade.GOOD -> "You're good to go"
    Grade.FAIR -> "Usable, with limits"
    Grade.POOR -> "Not a good spot right now"
}

// The "What works here?" card: one line per use case
fun whatWorks(mbps: Double, pingMs: Long, rssi: Int? = null): List<UseCaseAdvice> {
    val ping = pingMs
    val cls = when {
        mbps >= UseCase.VIDEO_CLASS.minMbps && ping <= UseCase.VIDEO_CLASS.maxPingMs ->
            UseCaseAdvice(UseCase.VIDEO_CLASS, Fit.GOOD_FIT, "2–5 Mbps is typical video-class context.")
        mbps >= 2 ->
            UseCaseAdvice(UseCase.VIDEO_CLASS, Fit.LIMITED, "May lag. Turn off your camera if it stutters.")
        else ->
            UseCaseAdvice(UseCase.VIDEO_CLASS, Fit.NOT_RECOMMENDED, "Too slow for a video class.")
    }
    val exam = if (mbps >= UseCase.EXAM.minMbps && ping <= UseCase.EXAM.maxPingMs) {
        UseCaseAdvice(UseCase.EXAM, Fit.CHECK_STABILITY, "Run Exam Mode first; speed is not a guarantee.")
    } else {
        UseCaseAdvice(UseCase.EXAM, Fit.NOT_RECOMMENDED, "Too weak for an exam here. Try another spot.")
    }
    val download = when {
        mbps >= UseCase.DOWNLOAD.minMbps ->
            UseCaseAdvice(UseCase.DOWNLOAD, Fit.GOOD_FIT, "Large files should download comfortably.")
        mbps >= 2 ->
            UseCaseAdvice(UseCase.DOWNLOAD, Fit.LIMITED, "Large files will be slow.")
        else ->
            UseCaseAdvice(UseCase.DOWNLOAD, Fit.NOT_RECOMMENDED, "Too slow for large downloads.")
    }
    val gaming = when {
        mbps >= UseCase.GAMING.minMbps && ping <= UseCase.GAMING.maxPingMs ->
            UseCaseAdvice(UseCase.GAMING, Fit.GOOD_FIT, "$ping ms ping; watch for spikes or packet loss.")
        ping <= 100 ->
            UseCaseAdvice(UseCase.GAMING, Fit.LIMITED, "$ping ms ping; you may notice lag.")
        else ->
            UseCaseAdvice(UseCase.GAMING, Fit.NOT_RECOMMENDED, "High ping will cause lag.")
    }
    val browsing = when {
        mbps >= 5 ->
            UseCaseAdvice(UseCase.BROWSING, Fit.GOOD_FIT, "Plenty of bandwidth for everyday pages.")
        mbps >= UseCase.BROWSING.minMbps ->
            UseCaseAdvice(UseCase.BROWSING, Fit.LIMITED, "Pages will load, but slowly.")
        else ->
            UseCaseAdvice(UseCase.BROWSING, Fit.NOT_RECOMMENDED, "Too slow for comfortable browsing.")
    }
    return listOf(cls, exam, download, gaming, browsing)
}

// Saves a test made with the old simple test functions
fun saveTestResult(context: Context, spotName: String, mbps: Double, pingMs: Long): TestResult {
    val r = TestResult(spotName, mbps, pingMs, rating(mbps, pingMs), System.currentTimeMillis())
    ResultStore.add(context, r)
    return r
}

object SpotAnalytics {

    private fun hourOf(timeMillis: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = timeMillis
        return c.get(Calendar.HOUR_OF_DAY)
    }

    // Data for every spot. The map, the location list, and the admin screen all use this.
    fun summaries(context: Context, now: Long = System.currentTimeMillis()): List<SpotSummary> {
        val results = ResultStore.load(context)
        val reports = ReportStore.load(context)
        val holds = SpotRepository.holdNames(context)

        return CAMPUS_SPOTS.mapIndexed { index, spot ->
            val recent = results.filter { it.spot == spot.name && now - it.timeMillis <= RECENT_WINDOW_MS }
            val avgMbps = if (recent.isEmpty()) null else recent.map { it.mbps }.average()
            val avgPing = if (recent.isEmpty()) null else recent.map { it.pingMs.toDouble() }.average()
            val rssiList = recent.mapNotNull { it.rssi }
            val avgRssi = if (rssiList.isEmpty()) null else rssiList.average()
            val grade = if (avgMbps != null && avgPing != null) overallGrade(avgMbps, avgPing, avgRssi) else null

            val spotReports = reports.filter { it.spotName == spot.name && now - it.timeMillis <= REPORT_WINDOW_MS }
            val dead = spotReports.count { it.type == ReportType.DEAD_ZONE }
            val hazard = spotReports.count { it.type == ReportType.HAZARD }

            val status = when {
                grade != null -> statusOf(grade)
                dead >= DEAD_ZONE_RED_COUNT -> SpotStatus.RED
                else -> SpotStatus.GRAY
            }

            // Safety rules: a spot can only be selected or recommended if none of these apply
            val onHold = spot.name in holds || hazard >= HAZARD_HIDE_COUNT
            val reason: String? = when {
                onHold -> "Safety hold: unavailable"
                !SpotRepository.isOpenAtHour(spot, hourOf(now)) -> "Closed now"
                else -> null
            }

            SpotSummary(
                spot = spot,
                number = index + 1,
                status = status,
                grade = grade,
                avgMbps = avgMbps,
                avgPingMs = avgPing,
                avgRssi = avgRssi,
                testCount = recent.size,
                lastTestMillis = recent.maxOfOrNull { it.timeMillis },
                available = reason == null,
                onSafetyHold = onHold,
                unavailableReason = reason,
                deadZoneReports = dead,
                hazardReports = hazard
            )
        }
    }

    // "Best recent match" card on the map screen: best grade first, then fastest
    fun bestOverall(context: Context, now: Long = System.currentTimeMillis()): SpotSummary? {
        return summaries(context, now)
            .filter { it.available && it.grade != null }
            .sortedWith(
                compareByDescending<SpotSummary> { it.grade?.ordinal ?: -1 }
                    .thenByDescending { it.avgMbps ?: 0.0 }
            )
            .firstOrNull()
    }

    // "Best spot for..." : only available spots with recent tests that meet the need
    fun bestSpots(
        context: Context,
        useCase: UseCase,
        limit: Int = 3,
        now: Long = System.currentTimeMillis()
    ): List<SpotSummary> {
        return summaries(context, now)
            .filter { s ->
                val mbps = s.avgMbps
                val ping = s.avgPingMs
                s.available && mbps != null && ping != null &&
                    mbps >= useCase.minMbps && ping <= useCase.maxPingMs
            }
            .sortedWith(
                compareByDescending<SpotSummary> { it.avgMbps ?: 0.0 }
                    .thenBy { it.avgPingMs ?: Double.MAX_VALUE }
            )
            .take(limit)
    }

    // Spot detail screen: 24-hour average, download trend, and recent tests
    fun spotDetail(context: Context, spotName: String, now: Long = System.currentTimeMillis()): SpotDetail? {
        val summary = summaries(context, now).firstOrNull { it.spot.name == spotName } ?: return null
        val tests = ResultStore.load(context)
            .filter { it.spot == spotName && now - it.timeMillis <= RECENT_WINDOW_MS }
            .sortedBy { it.timeMillis }
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val hourly = tests
            .groupBy { it.timeMillis / 3_600_000L }
            .map { (bucket, list) ->
                val start = bucket * 3_600_000L
                HourlyPoint(start, fmt.format(Date(start)), list.map { it.mbps }.average())
            }
            .sortedBy { it.hourStartMillis }
        return SpotDetail(summary, hourly, tests.reversed().take(5))
    }

    // Exam Mode (quick version): judge the latest speed test
    fun examCheck(context: Context, currentSpotName: String, mbps: Double, pingMs: Long): ExamAdvice {
        val need = UseCase.EXAM
        if (mbps >= need.minMbps && pingMs <= need.maxPingMs) {
            return ExamAdvice(true, "Your connection looks fast enough for an online exam.", null)
        }
        val better = bestSpots(context, need, limit = 5).firstOrNull { it.spot.name != currentSpotName }
        val message = if (better != null) {
            "Your current spot is not good enough for an exam. Try ${better.spot.name} instead."
        } else {
            "Your current spot is not good enough for an exam, and no other approved spot has good recent tests."
        }
        return ExamAdvice(false, message, better)
    }

    // Exam Mode (stability version): uses the result of runStabilityCheck()
    fun examAdvice(context: Context, currentSpotName: String, report: StabilityReport): ExamAdvice {
        if (report.stable) return ExamAdvice(true, report.summary, null)
        val better = bestSpots(context, UseCase.EXAM, limit = 5).firstOrNull { it.spot.name != currentSpotName }
        val message = if (better != null) {
            "${report.summary} Try ${better.spot.name} instead."
        } else {
            "${report.summary} No other approved spot has good recent tests."
        }
        return ExamAdvice(false, message, better)
    }

    // Admin: weakest spots first (spots without recent tests go last)
    fun adminRanking(context: Context): List<SpotSummary> {
        return summaries(context).sortedBy { it.avgMbps ?: Double.MAX_VALUE }
    }

    // Saves a dead-zone or hazard report on the phone (the email is built in ReportEmail.kt)
    fun report(context: Context, spotName: String, type: ReportType) {
        ReportStore.add(context, spotName, type)
    }

    // Admin actions
    fun placeOnHold(context: Context, spotName: String) = SpotRepository.setHold(context, spotName, true)

    fun releaseHold(context: Context, spotName: String) {
        SpotRepository.setHold(context, spotName, false)
        ReportStore.clearForSpot(context, spotName) // reviewed, so its reports are cleared
    }

    // Plain-text dump of everything, for testing without a UI
    fun debugDump(context: Context): String {
        val sb = StringBuilder()
        sb.appendLine("== Spots (24 h averages) ==")
        summaries(context).forEach { s ->
            val mbps = s.avgMbps?.let { "%.1f".format(it) } ?: "-"
            val ping = s.avgPingMs?.let { "%.0f".format(it) } ?: "-"
            val sig = s.avgRssi?.let { "%.0f".format(it) } ?: "-"
            val why = s.unavailableReason?.let { " ($it)" } ?: ""
            sb.appendLine(
                "${s.number}. ${s.spot.name}: ${s.status} ${s.grade?.label ?: ""} | $mbps Mbps, $ping ms, $sig dBm | " +
                    "tests=${s.testCount} | available=${s.available}$why | dead=${s.deadZoneReports} hazard=${s.hazardReports}"
            )
        }
        sb.appendLine()
        sb.appendLine("Best overall: ${bestOverall(context)?.spot?.name ?: "none"}")
        UseCase.values().forEach { u ->
            val names = bestSpots(context, u).joinToString { it.spot.name }
            sb.appendLine("Best for ${u.label}: ${if (names.isEmpty()) "none" else names}")
        }
        return sb.toString()
    }
}
