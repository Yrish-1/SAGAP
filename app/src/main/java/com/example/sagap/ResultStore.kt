package com.example.sagap

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// One saved test
data class TestResult(
    val spot: String,
    val mbps: Double,
    val pingMs: Long,
    val rating: String,
    val timeMillis: Long,
    val rssi: Int? = null,
    val isSample: Boolean = false,
    val ssid: String? = null,
    val band: String? = null,
    val linkSpeedMbps: Int? = null,
    val bssid: String? = null
)

// Saves and loads test results on the phone
object ResultStore {
    private const val PREFS = "sagap_results"
    private const val KEY = "list"

    fun load(context: Context): List<TestResult> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY, "[]") ?: "[]"
        val arr = try {
            JSONArray(jsonStr)
        } catch (e: Exception) {
            JSONArray()
        }
        val list = mutableListOf<TestResult>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            list.add(
                TestResult(
                    spot = o.optString("spot"),
                    mbps = o.optDouble("mbps"),
                    pingMs = o.optLong("pingMs"),
                    rating = o.optString("rating"),
                    timeMillis = o.optLong("time"),
                    rssi = if (o.has("rssi")) o.optInt("rssi") else null,
                    isSample = o.optBoolean("isSample", false),
                    ssid = if (o.has("ssid")) o.optString("ssid") else null,
                    band = if (o.has("band")) o.optString("band") else null,
                    linkSpeedMbps = if (o.has("linkSpeedMbps")) o.optInt("linkSpeedMbps") else null,
                    bssid = if (o.has("bssid")) o.optString("bssid") else null
                )
            )
        }
        return list
    }

    fun add(context: Context, result: TestResult) {
        val current = load(context).toMutableList()
        current.add(result)
        saveAll(context, current)
    }

    fun addAll(context: Context, results: List<TestResult>) {
        val current = load(context).toMutableList()
        current.addAll(results)
        saveAll(context, current)
    }

    fun removeSamples(context: Context) {
        val nonSamples = load(context).filterNot { it.isSample }
        saveAll(context, nonSamples)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY).apply()
    }

    private fun saveAll(context: Context, results: List<TestResult>) {
        val arr = JSONArray()
        results.forEach { r ->
            val o = JSONObject()
                .put("spot", r.spot)
                .put("mbps", r.mbps)
                .put("pingMs", r.pingMs)
                .put("rating", r.rating)
                .put("time", r.timeMillis)
                .put("isSample", r.isSample)
            r.rssi?.let { o.put("rssi", it) }
            r.ssid?.let { o.put("ssid", it) }
            r.band?.let { o.put("band", it) }
            r.linkSpeedMbps?.let { o.put("linkSpeedMbps", it) }
            r.bssid?.let { o.put("bssid", it) }
            arr.put(o)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}

// Color label for a spot, based on its average results (same thresholds as the app)
fun spotStatus(avgMbps: Double, avgPingMs: Double): String = when {
    avgMbps >= 5 && avgPingMs < 100 -> "🟢 Good"
    avgMbps >= 2 && avgPingMs <= 150 -> "🟡 Fair"
    else -> "🔴 Poor"
}
