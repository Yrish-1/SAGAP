package com.example.sagap

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.sagap.ui.theme.SAGAPTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

const val DOWNLOAD_URL = "https://speed.cloudflare.com/__down?bytes=5000000" // about 5 MB
const val PING_URL = "https://www.google.com/generate_204"

// Approved campus spots. EDIT THESE to match your campus.
val SPOTS = listOf(
    "Library 2F Reading Area",
    "Cafeteria",
    "Engineering Bldg Room 301",
    "Study Hall",
    "Gym Lobby"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SAGAPTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SpeedTestScreen(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// ---------- Network checks ----------

// True if the phone is currently connected through Wi-Fi
fun isOnWifi(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

// Reads Wi-Fi signal strength (RSSI in dBm) and turns it into a label
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

// ---------- Speed and ping tests ----------

// Downloads a file and computes speed in Mbps = megabits / seconds
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

// Times small requests and averages them (first one skipped: it includes connection setup)
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

// ---------- Thresholds and tips ----------

fun rating(mbps: Double, pingMs: Long): String = when {
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

// ---------- Screen ----------

@Composable
fun SpeedTestScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("Choose your spot, then tap the button.") }
    var expanded by remember { mutableStateOf(false) }
    var selectedSpot by remember { mutableStateOf(SPOTS[0]) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("SAGAP", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))

        Text("Where are you?", style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text("$selectedSpot  ▼")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                SPOTS.forEach { spot ->
                    DropdownMenuItem(
                        text = { Text(spot) },
                        onClick = {
                            selectedSpot = spot
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            enabled = !running,
            onClick = {
                if (!isOnWifi(context)) {
                    resultText = "You are not connected to Wi-Fi.\nConnect to Wi-Fi to test it, " +
                            "or use mobile data if the Wi-Fi is not working."
                    return@Button
                }
                running = true
                resultText = "Testing..."
                scope.launch {
                    resultText = try {
                        val mbps = testDownloadMbps()
                        val ping = testPingMs()
                        val r = rating(mbps, ping)
                        val forClass = if (r == "Good" || r == "Excellent") "Good" else "Not enough"
                        val forExam = if (r == "Good" || r == "Excellent") "Good" else "Risky"
                        val forGaming = if (r == "Excellent") "Good" else "Not recommended"
                        "Spot: $selectedSpot\n" +
                                "Wi-Fi signal: ${wifiSignal(context)}\n\n" +
                                "Download: %.1f Mbps\nPing: %d ms\nRating: %s\n\n".format(mbps, ping, r) +
                                "Online class: $forClass\nOnline exam: $forExam\nGaming: $forGaming\n\n" +
                                "Tip: ${tipFor(r)}"
                    } catch (e: Exception) {
                        "Test failed. Check your internet connection.\n(${e.message})"
                    }
                    running = false
                }
            }
        ) { Text("Test My Wi-Fi") }

        Spacer(Modifier.height(24.dp))
        if (running) CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(resultText, style = MaterialTheme.typography.bodyLarge)
    }
}