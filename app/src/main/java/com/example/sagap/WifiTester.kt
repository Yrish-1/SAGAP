package com.example.sagap

import android.content.Context
import android.net.Network
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

// ---------- Wi-Fi test with live progress ----------

// Sent to the UI while the test runs (the "Testing your Wi-Fi" screen)
data class TestProgress(
    val stage: String,       // "ping", "download" or "signal"
    val percent: Int,        // 0 to 100
    val secondsLeft: Int,    // "About 7 seconds left"
    val pingMs: Long?,       // null until the ping stage has results
    val mbpsNow: Double?,    // live download speed, null until it starts
    val rssi: Int?           // signal in dBm
)

sealed class TestOutcome {
    data class Success(val result: TestResult) : TestOutcome()
    data class Failed(val reason: String) : TestOutcome()
}

private fun openOn(network: Network, url: String): HttpURLConnection {
    // network.openConnection sends the traffic through THAT network only (the Wi-Fi guard)
    val conn = network.openConnection(URL(url)) as HttpURLConnection
    conn.connectTimeout = 5_000
    conn.readTimeout = 8_000
    return conn
}

// Runs ping, download and signal reading over Wi-Fi only. Saves the result to History on success.
// To cancel: keep the Job returned by scope.launch { ... } and call job.cancel().
suspend fun runWifiTest(
    context: Context,
    spotName: String,
    onProgress: (TestProgress) -> Unit = {}
): TestOutcome = withContext(Dispatchers.IO) {
    val appContext = context.applicationContext
    val blocked = testBlockReason(appContext)
    if (blocked != null) return@withContext TestOutcome.Failed(blocked)
    val network = findWifiNetwork(appContext)
        ?: return@withContext TestOutcome.Failed("Wi-Fi not connected.")

    try {
        val startWifi = readWifiDetails(appContext)
        onProgress(TestProgress("ping", 0, DOWNLOAD_SECONDS + 1, null, null, startWifi.rssi))

        // Stage 1: ping (the first request is skipped because it includes connection setup)
        val pings = mutableListOf<Long>()
        for (i in 0 until PING_SAMPLES) {
            ensureActive()
            val conn = openOn(network, PING_URL)
            val t0 = System.nanoTime()
            conn.responseCode
            val ms = (System.nanoTime() - t0) / 1_000_000
            conn.inputStream?.close()
            if (i > 0) pings.add(ms)
            val avgSoFar = if (pings.isEmpty()) null else pings.average().toLong()
            onProgress(
                TestProgress("ping", 20 * (i + 1) / PING_SAMPLES, DOWNLOAD_SECONDS + 1, avgSoFar, null, startWifi.rssi)
            )
        }
        if (pings.isEmpty()) return@withContext TestOutcome.Failed("No ping replies were received.")
        val pingMs = pings.average().toLong()

        // Stage 2: download for about DOWNLOAD_SECONDS seconds (timing starts at the first data)
        val limitNs = DOWNLOAD_SECONDS * 1_000_000_000L
        var firstNs = 0L
        var lastNs = 0L
        var lastEmitNs = 0L
        var bytes = 0L
        val dl = openOn(network, DOWNLOAD_URL)
        dl.inputStream.use { stream ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                ensureActive()
                val n = stream.read(buffer)
                if (n < 0) break
                val now = System.nanoTime()
                if (firstNs == 0L) {
                    firstNs = now
                    lastNs = now
                    continue
                }
                bytes += n
                lastNs = now
                val elapsed = now - firstNs
                if (now - lastEmitNs > 250_000_000L && elapsed > 200_000_000L) {
                    lastEmitNs = now
                    val mbpsNow = bytes * 8 / 1_000_000.0 / (elapsed / 1e9)
                    val fraction = minOf(elapsed.toDouble() / limitNs, 1.0)
                    val left = maxOf(0, DOWNLOAD_SECONDS - (elapsed / 1_000_000_000L).toInt())
                    onProgress(
                        TestProgress("download", 20 + (80 * fraction).toInt(), left, pingMs, mbpsNow, readWifiDetails(appContext).rssi)
                    )
                }
                if (elapsed >= limitNs) break
            }
        }
        dl.disconnect()

        val seconds = (lastNs - firstNs) / 1e9
        if (bytes <= 0L || seconds <= 0.0) {
            return@withContext TestOutcome.Failed("Not enough data was received to measure the speed.")
        }
        val mbps = bytes * 8 / 1_000_000.0 / seconds

        // Stage 3: signal and Wi-Fi details
        onProgress(TestProgress("signal", 98, 0, pingMs, mbps, startWifi.rssi))
        val endWifi = readWifiDetails(appContext)
        if (findWifiNetwork(appContext) == null) {
            return@withContext TestOutcome.Failed("Wi-Fi disconnected during the test. The test was stopped.")
        }
        val rssiValues = listOfNotNull(startWifi.rssi, endWifi.rssi)
        val rssi = if (rssiValues.isEmpty()) null else rssiValues.average().toInt()

        val result = TestResult(
            spot = spotName,
            mbps = mbps,
            pingMs = pingMs,
            rating = rating(mbps, pingMs, rssi),
            timeMillis = System.currentTimeMillis(),
            rssi = rssi,
            isSample = false,
            ssid = endWifi.ssid ?: startWifi.ssid,
            band = endWifi.band ?: startWifi.band,
            linkSpeedMbps = endWifi.linkSpeedMbps ?: startWifi.linkSpeedMbps,
            bssid = endWifi.bssid ?: startWifi.bssid
        )
        ResultStore.add(appContext, result)
        onProgress(TestProgress("signal", 100, 0, pingMs, mbps, rssi))
        return@withContext TestOutcome.Success(result)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        return@withContext TestOutcome.Failed(
            "The test stopped. Wi-Fi may have disconnected or has no internet. (${e.message})"
        )
    }
}

// ---------- Exam Mode: stability check ----------

data class StabilityReport(
    val samples: Int,
    val failures: Int,       // checks that got no reply
    val avgPingMs: Long,
    val maxPingMs: Long,
    val jitterMs: Long,      // average change between one ping and the next
    val stable: Boolean,
    val summary: String
)

sealed class StabilityOutcome {
    data class Done(val report: StabilityReport) : StabilityOutcome()
    data class Failed(val reason: String) : StabilityOutcome()
}

// Pings twice a second for [seconds] seconds over Wi-Fi and looks for drops and spikes.
// Cancel it the same way as the test: job.cancel().
suspend fun runStabilityCheck(
    context: Context,
    seconds: Int = 20,
    onProgress: (percent: Int, lastPingMs: Long?) -> Unit = { _, _ -> }
): StabilityOutcome = withContext(Dispatchers.IO) {
    val appContext = context.applicationContext
    val blocked = testBlockReason(appContext)
    if (blocked != null) return@withContext StabilityOutcome.Failed(blocked)
    val network = findWifiNetwork(appContext)
        ?: return@withContext StabilityOutcome.Failed("Wi-Fi not connected.")

    val total = seconds * 2
    val pings = mutableListOf<Long>()
    var failures = 0
    for (i in 0 until total) {
        ensureActive()
        val t0 = System.nanoTime()
        var ms: Long? = null
        try {
            val conn = network.openConnection(URL(PING_URL)) as HttpURLConnection
            conn.connectTimeout = 2_000
            conn.readTimeout = 2_000
            conn.responseCode
            conn.inputStream?.close()
            ms = (System.nanoTime() - t0) / 1_000_000
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // no reply: counted as a failure below
        }
        if (ms == null) failures++ else pings.add(ms)
        onProgress(100 * (i + 1) / total, ms)
        val spentMs = (System.nanoTime() - t0) / 1_000_000
        if (spentMs < 500) delay(500 - spentMs)
    }

    if (pings.isEmpty()) {
        return@withContext StabilityOutcome.Done(
            StabilityReport(total, failures, 0, 0, 0, false, "Unstable: no replies were received.")
        )
    }
    val avg = pings.average().toLong()
    val max = pings.maxOrNull() ?: 0L
    val jitter = if (pings.size < 2) 0L else {
        (1 until pings.size).map { abs(pings[it] - pings[it - 1]).toDouble() }.average().toLong()
    }

    val issues = mutableListOf<String>()
    if (failures.toDouble() > total * 0.02) issues.add("$failures of $total checks got no reply")
    if (jitter > 30) issues.add("ping jumps around (jitter $jitter ms)")
    if (max > 300) issues.add("spikes up to $max ms")
    if (avg > 120) issues.add("average ping is high ($avg ms)")

    val stable = issues.isEmpty()
    val summary = if (stable) {
        "Stable: no drops or large spikes in $seconds seconds."
    } else {
        "Unstable: " + issues.joinToString("; ") + "."
    }
    StabilityOutcome.Done(StabilityReport(total, failures, avg, max, jitter, stable, summary))
}
