package com.example.sagap

import android.content.Context
import kotlin.random.Random

// Demo data, clearly marked as sample (isSample = true). Call seed() right before a demo.
object SampleData {

    private data class Profile(val mbps: Double, val ping: Long, val rssi: Int)

    // One profile per spot, in the same order as CAMPUS_SPOTS
    private val profiles = listOf(
        Profile(18.6, 32, -61),   // 1st spot: Good
        Profile(30.0, 25, -58),   // 2nd: Excellent
        Profile(42.8, 18, -52),   // 3rd: Excellent
        Profile(14.0, 45, -64),   // 4th: Good
        Profile(6.8, 92, -70),    // 5th: Fair
        Profile(1.4, 186, -81),   // 6th: Poor
        Profile(15.3, 39, -64),   // 7th: Good
        Profile(20.0, 30, -60)    // 8th: Good (on safety hold by default)
    )

    // Creates 8 sample tests per spot spread over the last ~20 hours (newest only a few minutes old)
    fun seed(context: Context) {
        ResultStore.removeSamples(context) // so loading twice does not duplicate
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
