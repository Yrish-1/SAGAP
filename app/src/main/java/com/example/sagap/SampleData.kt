package com.example.sagap

import android.content.Context
import kotlin.random.Random

// Demo data, clearly marked as sample (isSample = true).
object SampleData {

    private data class Profile(val mbps: Double, val ping: Long, val rssi: Int)

    // One profile per spot (3 spots)
    private val profiles = listOf(
        Profile(18.6, 32, -61),   // 1st spot: Albert Einstein
        Profile(42.8, 18, -52),   // 2nd spot: CICS
        Profile(24.2, 28, -59)    // 3rd spot: Sparta Gymnasium
    )

    fun seed(context: Context) {
        ResultStore.removeSamples(context)
        val now = System.currentTimeMillis()
        val rnd = Random(42)
        val list = mutableListOf<TestResult>()
        CAMPUS_SPOTS.forEachIndexed { i, spot ->
            val p = profiles.getOrNull(i) ?: return@forEachIndexed
            for (k in 0 until 8) {
                val minutesAgo = 3L + i + k * 170L + rnd.nextInt(0, 40)
                val mbps = p.mbps * (0.9 + rnd.nextDouble() * 0.2)
                val ping = (p.ping * (0.9 + rnd.nextDouble() * 0.2)).toLong()
                val rssi = p.rssi + rnd.nextInt(-2, 3)
                list.add(
                    TestResult(
                        spot = spot.name,
                        mbps = mbps,
                        pingMs = ping,
                        rating = rating(mbps, ping, rssi),
                        timeMillis = now - minutesAgo * 60_000L,
                        rssi = rssi,
                        isSample = true,
                        ssid = "BatStateU CICS",
                        band = "5 GHz",
                        linkSpeedMbps = 144
                    )
                )
            }
        }
        ResultStore.addAll(context, list)
    }

    fun remove(context: Context) = ResultStore.removeSamples(context)
}
