package com.example.sagap.data.sample

import com.example.sagap.data.model.Spot
import com.example.sagap.data.model.TestResult

/** Demo data only. Later, repositories will read this from Firestore instead. */
object SampleData {

    const val WIFI_NAME = "BatStateU CICS"
    const val WIFI_DETAILS = "5 GHz · AP LIB-02 · Link speed 144 Mbps"

    val spots = listOf(
        Spot(1, "Einstein Building", mapX = 0.18f, mapY = 0.20f),
        Spot(2, "CICS Building", mapX = 0.50f, mapY = 0.20f),
        Spot(3, "Sparta Gymnasium", mapX = 0.85f, mapY = 0.55f),
        Spot(4, "Student Center Lounge", mapX = 0.17f, mapY = 0.48f),
        Spot(5, "Cafeteria", mapX = 0.50f, mapY = 0.78f),
        Spot(6, "KIST Park", mapX = 0.17f, mapY = 0.80f),
        Spot(7, "COE Building", onHold = true, mapX = 0.85f, mapY = 0.25f),
        Spot(8, "CET Building", mapX = 0.85f, mapY = 0.82f)
    )

    private fun ago(minutes: Int): Long = System.currentTimeMillis() - minutes * 60_000L

    private fun t(spotId: Int, mbps: Double, ping: Int, dbm: Int, minutesAgo: Int) =
        TestResult(spotId, mbps, ping, dbm, ago(minutesAgo))

    // Campus-wide sample tests from the last 24 h (CET has none on purpose: "No recent data")
    val results = listOf(
        t(1, 18.6, 32, -61, 5), t(1, 17.9, 35, -62, 40), t(1, 19.2, 30, -60, 75),
        t(2, 42.8, 18, -52, 3), t(2, 40.1, 20, -53, 30), t(2, 44.0, 17, -51, 65),
        t(3, 24.2, 28, -59, 8), t(3, 22.8, 31, -60, 50), t(3, 25.9, 27, -58, 90),
        t(4, 6.8, 92, -70, 6), t(4, 7.4, 88, -69, 45), t(4, 5.9, 101, -72, 80),
        t(5, 1.4, 186, -81, 2), t(5, 1.9, 170, -79, 35), t(5, 1.1, 205, -83, 70),
        t(6, 15.3, 39, -64, 12), t(6, 14.1, 42, -65, 55), t(6, 16.0, 37, -63, 95)
    )

    private const val DAY = 24 * 60

    // The student's own test log (shown on the History tab)
    val history = listOf(
        t(1, 18.6, 32, -61, 20),
        t(5, 1.4, 186, -81, 22),
        t(2, 42.8, 18, -52, 66),
        t(4, 6.8, 92, -70, DAY + 100),
        t(3, 24.2, 28, -59, DAY + 270),
        t(6, 15.3, 39, -64, 2 * DAY + 60)
    )

    // Illustrative download trend for the spot detail chart (Mbps, 08:00 to 14:00)
    val trend = listOf(12f, 12.4f, 14f, 17f, 16.4f, 18.6f, 20f, 19.2f, 21f, 20.4f, 21.5f)
}
