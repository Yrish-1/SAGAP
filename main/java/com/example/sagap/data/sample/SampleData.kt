package com.example.sagap.data.sample

import com.example.sagap.data.model.Spot
import com.example.sagap.data.model.TestResult

object SampleData {

    val spots = listOf(
        Spot(1, "Einstein Building",     mapX = 0.18f, mapY = 0.20f),
        Spot(2, "CICS Building",         mapX = 0.50f, mapY = 0.20f),
        Spot(3, "Sparta Gymnasium",      mapX = 0.85f, mapY = 0.55f),
        Spot(4, "Student Center Lounge", mapX = 0.17f, mapY = 0.48f),
        Spot(5, "Cafeteria",             mapX = 0.50f, mapY = 0.75f),
        Spot(6, "KIST Park",             mapX = 0.17f, mapY = 0.76f),
        Spot(7, "COE Building", onHold = true, mapX = 0.85f, mapY = 0.28f),
        Spot(8, "CET Building",          mapX = 0.85f, mapY = 0.80f)
    )

    private fun minutesAgo(m: Int) = System.currentTimeMillis() - m * 60_000L

    // CET (id 8) has no results on purpose, so it shows as "No recent data"
    val results = listOf(
        TestResult(1, 18.6, 32, -61, minutesAgo(5)),
        TestResult(2, 42.8, 18, -52, minutesAgo(3)),
        TestResult(3, 24.2, 28, -59, minutesAgo(8)),
        TestResult(4,  6.8, 92, -70, minutesAgo(6)),
        TestResult(5,  1.4, 186, -81, minutesAgo(2)),
        TestResult(6, 15.3, 39, -64, minutesAgo(12))
    )
}