package com.example.sagap.data.sample

import com.example.sagap.data.model.Spot
import com.example.sagap.data.model.TestResult

object SampleData {

    const val WIFI_NAME = "BatStateU CICS"
    const val WIFI_DETAILS = "5 GHz · AP LIB-02 · Link speed 144 Mbps"

    val spots = listOf(
        Spot(1, "Albert Einstein Building", mapX = 0.25f, mapY = 0.35f),
        Spot(2, "CICS Building", mapX = 0.50f, mapY = 0.50f),
        Spot(3, "Sparta Gymnasium", mapX = 0.75f, mapY = 0.65f)
    )

    private fun ago(minutes: Int): Long = System.currentTimeMillis() - minutes * 60_000L

    private fun t(spotId: Int, mbps: Double, ping: Int, dbm: Int, minutesAgo: Int) =
        TestResult(spotId, mbps, ping, dbm, ago(minutesAgo))

    val results = listOf(
        t(1, 18.6, 32, -61, 5), t(1, 17.9, 35, -62, 40), t(1, 19.2, 30, -60, 75),
        t(2, 42.8, 18, -52, 3), t(2, 40.1, 20, -53, 30), t(2, 44.0, 17, -51, 65),
        t(3, 24.2, 28, -59, 8), t(3, 22.8, 31, -60, 50), t(3, 25.9, 27, -58, 90)
    )

    private const val DAY = 24 * 60

    val history = listOf(
        t(1, 18.6, 32, -61, 20),
        t(2, 42.8, 18, -52, 66),
        t(3, 24.2, 28, -59, DAY + 270)
    )

    val trend = listOf(12f, 12.4f, 14f, 17f, 16.4f, 18.6f, 20f, 19.2f, 21f, 20.4f, 21.5f)
}
