package com.example.sagap

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

const val DOWNLOAD_URL = "https://speed.cloudflare.com/__down?bytes=5000000" // about 5 MB
const val PING_URL = "https://www.google.com/generate_204"
const val DOWNLOAD_SECONDS = 5
const val PING_SAMPLES = 5

data class WifiDetails(
    val rssi: Int?,
    val ssid: String?,
    val band: String?,
    val linkSpeedMbps: Int?,
    val bssid: String?
)

// True if the phone is currently connected through Wi-Fi
fun isOnWifi(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

// Wi-Fi signal strength as a label, e.g. "Good (-55 dBm)"
@Suppress("DEPRECATION")
fun wifiSignal(context: Context): String {
    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val rssi = wm.connectionInfo?.rssi ?: return "Unknown"
    if (rssi <= -127) return "Unknown"
    val label = when {
        rssi >= -50 -> "Excellent"
        rssi >= -60 -> "Good"
        rssi >= -70 -> "Fair"
        else -> "Weak"
    }
    return "$label ($rssi dBm)"
}

// Download speed in Mbps (call from a coroutine)
suspend fun testDownloadMbps(): Double = withContext(Dispatchers.IO) {
    val conn = URL(DOWNLOAD_URL).openConnection() as HttpURLConnection
    conn.connectTimeout = 10_000
    conn.readTimeout = 15_000
    val start = System.nanoTime()
    var bytes = 0L
    conn.inputStream.use { input ->
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            bytes += n
        }
    }
    val seconds = (System.nanoTime() - start) / 1_000_000_000.0
    conn.disconnect()
    (bytes * 8 / 1_000_000.0) / seconds
}

// Ping in ms (call from a coroutine)
suspend fun testPingMs(): Long = withContext(Dispatchers.IO) {
    val times = mutableListOf<Long>()
    repeat(5) {
        val conn = URL(PING_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = 5_000
        conn.readTimeout = 5_000
        val start = System.nanoTime()
        conn.responseCode
        times.add((System.nanoTime() - start) / 1_000_000)
        conn.inputStream?.close()
    }
    times.drop(1).average().toLong()
}

// Poor / Fair / Good / Excellent
fun rating(mbps: Double, pingMs: Long, rssi: Int? = null): String = when {
    mbps >= 10 && pingMs < 50 -> "Excellent"
    mbps >= 5 && pingMs < 100 -> "Good"
    mbps >= 2 && pingMs <= 150 -> "Fair"
    else -> "Poor"
}

fun tipFor(rating: String): String = when (rating) {
    "Excellent" -> "Great for class, exams and gaming."
    "Good" -> "Good for class and exams. Gaming may lag."
    "Fair" -> "Class is possible, but turn off your camera if it lags. Avoid exams here."
    else -> "Too weak for class. Move closer to the router, try another spot, or switch to mobile data."
}

fun gradeFromMbps(mbps: Double): Grade = when {
    mbps >= 15.0 -> Grade.EXCELLENT
    mbps >= 5.0 -> Grade.GOOD
    mbps >= 2.0 -> Grade.FAIR
    else -> Grade.POOR
}

fun gradeFromPing(pingMs: Double): Grade = when {
    pingMs < 40 -> Grade.EXCELLENT
    pingMs < 100 -> Grade.GOOD
    pingMs <= 150 -> Grade.FAIR
    else -> Grade.POOR
}

fun gradeFromRssi(rssi: Double): Grade = when {
    rssi >= -55 -> Grade.EXCELLENT
    rssi >= -65 -> Grade.GOOD
    rssi >= -75 -> Grade.FAIR
    else -> Grade.POOR
}

fun overallGrade(mbps: Double, pingMs: Double, rssi: Double? = null): Grade {
    val gMbps = gradeFromMbps(mbps)
    val gPing = gradeFromPing(pingMs)
    val gRssi = rssi?.let { gradeFromRssi(it) } ?: Grade.EXCELLENT
    val minOrdinal = minOf(gMbps.ordinal, gPing.ordinal, gRssi.ordinal)
    return Grade.entries[minOrdinal]
}

@Suppress("DEPRECATION")
fun readWifiDetails(context: Context): WifiDetails {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    var info: WifiInfo? = null

    // Method 1: Modern Android 10+ (API 29+) ConnectivityManager TransportInfo
    if (cm != null) {
        val activeNet = cm.activeNetwork
        if (activeNet != null) {
            val caps = cm.getNetworkCapabilities(activeNet)
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                info = caps.transportInfo as? WifiInfo
            }
        }
    }

    // Method 2: Fallback to WifiManager.connectionInfo
    if (info == null && wm != null) {
        info = wm.connectionInfo
    }

    if (info == null) {
        return WifiDetails(null, null, null, null, null)
    }

    val rssi = if (info.rssi <= -127) null else info.rssi
    val rawSsid = info.ssid
    val ssid = if (rawSsid == null || rawSsid == "<unknown ssid>" || rawSsid == "\"\"") {
        null
    } else {
        rawSsid.removeSurrounding("\"")
    }

    val bssid = if (info.bssid == null || info.bssid == "02:00:00:00:00:00") null else info.bssid
    val linkSpeed = if (info.linkSpeed <= 0) null else info.linkSpeed
    val frequency = info.frequency
    val band = when {
        frequency >= 5000 -> "5 GHz"
        frequency >= 2400 -> "2.4 GHz"
        else -> null
    }

    return WifiDetails(
        rssi = rssi,
        ssid = ssid,
        band = band,
        linkSpeedMbps = linkSpeed,
        bssid = bssid
    )
}

fun findWifiNetwork(context: Context): Network? {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return cm.allNetworks.firstOrNull { net ->
        val caps = cm.getNetworkCapabilities(net)
        caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}

fun testBlockReason(context: Context): String? {
    if (!isOnWifi(context)) return "Wi-Fi is turned off or not connected."
    return null
}
