package com.example.sagap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.sagap.ui.theme.SAGAPTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SAGAPTheme {
                SagapApp()
            }
        }
    }
}

@Composable
fun SagapApp() {
    var screen by remember { mutableStateOf("test") }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = screen == "test",
                    onClick = { screen = "test" },
                    icon = {},
                    label = { Text("Test") },
                )
                NavigationBarItem(
                    selected = screen == "history",
                    onClick = { screen = "history" },
                    icon = {},
                    label = { Text("History") },
                )
            }
        },
    ) { innerPadding ->
        if (screen == "test") {
            SpeedTestScreen(Modifier.padding(innerPadding))
        } else {
            HistoryScreen(Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun SpeedTestScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(value = false) }
    var resultText by remember { mutableStateOf("Choose your spot, then tap the button.") }
    var expanded by remember { mutableStateOf(value = false) }
    var selectedSpot by remember { mutableStateOf(SPOTS[0]) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("SAGAP", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))

        Text("Where are you?", style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text("$selectedSpot  ▼")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                for (spot in SPOTS) {
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

                        ResultStore.add(
                            context,
                            TestResult(selectedSpot, mbps, ping, r, System.currentTimeMillis()),
                        )

                        val forClass = if ((r == "Good") || (r == "Excellent")) "Good" else "Not enough"
                        val forExam = if ((r == "Good") || (r == "Excellent")) "Good" else "Risky"
                        val forGaming = if (r == "Excellent") "Good" else "Not recommended"
                        "Spot: $selectedSpot\n" +
                                "Wi-Fi signal: ${wifiSignal(context)}\n\n" +
                                "Download: %.1f Mbps\nPing: %d ms\nRating: %s\n\n".format(mbps, ping, r) +
                                "Online class: $forClass\nOnline exam: $forExam\nGaming: $forGaming\n\n" +
                                "Tip: ${tipFor(r)}\n\n(Saved to History)"
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

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var results by remember { mutableStateOf(ResultStore.load(context)) }
    val timeFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val bySpot = results.groupBy { result -> result.spot }

    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Spot summary", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            if (results.isEmpty()) {
                Text("No tests saved yet. Run a test on the Test tab first.")
            }
        }
        items(bySpot.keys.toList()) { spot ->
            val list = bySpot.getValue(spot)
            val avgMbps = list.asSequence().map { testResult -> testResult.mbps }.average()
            val avgPing = list.asSequence().map { testResult -> testResult.pingMs.toDouble() }.average()
            Text(
                "${spotStatus(avgMbps, avgPing)}  $spot\n" +
                        "Average: %.1f Mbps, %.0f ms (%d tests)".format(avgMbps, avgPing, list.size)
            )
            Spacer(Modifier.height(8.dp))
        }
        item {
            Spacer(Modifier.height(16.dp))
            Text("All tests", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
        }
        items(results.reversed()) { result ->
            Text(
                "${timeFormat.format(Date(result.timeMillis))}  •  ${result.spot}\n" +
                        "%.1f Mbps, %d ms, %s".format(result.mbps, result.pingMs, result.rating),
            )
            Spacer(Modifier.height(8.dp))
        }
        item {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    ResultStore.clear(context)
                    results = emptyList()
                }
            ) { Text("Clear history") }
        }
    }
}
