package com.example.sagap

// Overall quality grade. The LOWEST of speed, ping and signal sets the grade.
enum class Grade(val label: String) {
    POOR("Poor"),
    FAIR("Fair"),
    GOOD("Good"),
    EXCELLENT("Excellent")
}

// Marker color on the map. GRAY = no recent data.
enum class SpotStatus { GREEN, YELLOW, RED, GRAY }

// What the student can report (the email goes to the SAGAP team)
enum class ReportType(val label: String) {
    DEAD_ZONE("Dead zone / weak Wi-Fi"),
    HAZARD("Unsafe or inaccessible spot"),
    APP_PROBLEM("Problem with the app"),
    SUGGESTION("Suggestion or recommendation")
}

// What the student wants to do. Change the numbers if your thresholds change.
enum class UseCase(val label: String, val minMbps: Double, val maxPingMs: Long) {
    VIDEO_CLASS("Online class", 3.0, 120L),
    EXAM("Online exam", 5.0, 100L),
    DOWNLOAD("File download", 5.0, Long.MAX_VALUE),
    GAMING("Gaming", 5.0, 60L),
    BROWSING("Casual browsing", 1.0, 400L)
}

enum class Fit(val label: String) {
    GOOD_FIT("Good fit"),
    CHECK_STABILITY("Check stability"),
    LIMITED("Limited"),
    NOT_RECOMMENDED("Not recommended")
}

// One line of the "What works here?" card
data class UseCaseAdvice(val useCase: UseCase, val fit: Fit, val note: String)

data class Spot(
    val name: String,
    val building: String,
    val startsOnSafetyHold: Boolean = false, // starts as a campus safety hold (admin can change it)
    val openHour: Int = 0,                   // opening hours: 0 to 24 means always open
    val closeHour: Int = 24
)

data class Report(val spotName: String, val type: ReportType, val timeMillis: Long)

// Everything the UI needs to draw one spot (map marker, list row, detail screen)
data class SpotSummary(
    val spot: Spot,
    val number: Int,                 // marker number on the map (1, 2, 3...)
    val status: SpotStatus,
    val grade: Grade?,               // null = no recent tests
    val avgMbps: Double?,
    val avgPingMs: Double?,
    val avgRssi: Double?,
    val testCount: Int,              // tests in the last 24 hours
    val lastTestMillis: Long?,       // for "updated 3 min ago"
    val available: Boolean,          // false = safety hold or closed: cannot be selected or recommended
    val onSafetyHold: Boolean,
    val unavailableReason: String?,
    val deadZoneReports: Int,
    val hazardReports: Int
)

data class ExamAdvice(val ok: Boolean, val message: String, val suggestion: SpotSummary?)

data class HourlyPoint(val hourStartMillis: Long, val label: String, val avgMbps: Double)

// Data for the spot detail screen
data class SpotDetail(
    val summary: SpotSummary,
    val hourly: List<HourlyPoint>,    // download trend
    val recentTests: List<TestResult> // newest first
)
