package com.example.sagap.data.repository

import com.example.sagap.data.model.Grade
import com.example.sagap.data.model.Spot
import com.example.sagap.data.model.TestResult
import com.example.sagap.domain.RatingEngine
import kotlin.math.roundToInt

/** Pure functions over a list of results. The list will come from Firestore later. */
object SpotRepository {

    private const val DAY_MS = 24 * 60 * 60 * 1000L

    fun recent(results: List<TestResult>, spotId: Int): List<TestResult> =
        results.filter { it.spotId == spotId }.sortedByDescending { it.timestamp }

    fun latest(results: List<TestResult>, spotId: Int): TestResult? =
        recent(results, spotId).firstOrNull()

    private fun last24h(results: List<TestResult>, spotId: Int): List<TestResult> =
        recent(results, spotId).filter { System.currentTimeMillis() - it.timestamp <= DAY_MS }

    fun testCount24h(results: List<TestResult>, spotId: Int): Int = last24h(results, spotId).size

    /** Average of the last 24 h for a spot, or null if there is no recent data. */
    fun average(results: List<TestResult>, spotId: Int): TestResult? {
        val list = last24h(results, spotId)
        if (list.isEmpty()) return null
        return TestResult(
            spotId = spotId,
            downloadMbps = list.map { it.downloadMbps }.average(),
            pingMs = list.map { it.pingMs }.average().roundToInt(),
            signalDbm = list.map { it.signalDbm }.average().roundToInt(),
            timestamp = list.first().timestamp
        )
    }

    fun gradeOf(avg: TestResult?): Grade =
        if (avg == null) Grade.NO_DATA
        else RatingEngine.grade(avg.downloadMbps, avg.pingMs, avg.signalDbm)

    /** Best open spot: highest grade, then fastest. Safety-hold spots are never recommended. */
    fun best(spots: List<Spot>, results: List<TestResult>): Pair<Spot, TestResult>? =
        spots
            .filter { !it.onHold }
            .mapNotNull { s -> average(results, s.id)?.let { s to it } }
            .minWithOrNull(
                compareBy<Pair<Spot, TestResult>>(
                    { gradeOf(it.second).ordinal },
                    { -it.second.downloadMbps }
                )
            )
}
