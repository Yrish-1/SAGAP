package com.example.sagap.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sagap.ReportType
import com.example.sagap.TestOutcome
import com.example.sagap.data.model.Spot
import com.example.sagap.data.model.TestResult
import com.example.sagap.data.sample.SampleData
import com.example.sagap.openReportEmail
import com.example.sagap.readWifiDetails
import com.example.sagap.runWifiTest
import com.example.sagap.submitReport
import com.example.sagap.ui.screens.report.IssueType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TestPhase { IDLE, RUNNING, DONE }

data class TestUiState(
    val phase: TestPhase = TestPhase.IDLE,
    val progress: Float = 0f,
    val secondsLeft: Int = 0,
    val downloadMbps: Double = 0.0,
    val pingMs: Int? = null,
    val signalDbm: Int? = null,
    val errorMessage: String? = null
)

data class WifiInfoUiState(
    val ssid: String = "BatStateU CICS",
    val details: String = "5 GHz · Link speed 144 Mbps",
    val isConnected: Boolean = true
)

data class SagapState(
    val selectedSpot: Spot = SampleData.spots.first(),
    val reportSpotId: Int? = null,
    val test: TestUiState = TestUiState(),
    val lastResult: TestResult? = null,
    val wifiInfo: WifiInfoUiState = WifiInfoUiState(),
    val results: List<TestResult> = SampleData.results,   // campus-wide (map, spot detail)
    val history: List<TestResult> = SampleData.history    // this student's own tests
)

class SagapViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SagapState())
    val state: StateFlow<SagapState> = _state.asStateFlow()

    private var testJob: Job? = null

    init {
        refreshState()
    }

    fun refreshState() {
        val context = getApplication<Application>()

        // 1. Refresh live Wi-Fi connection info
        val wifi = readWifiDetails(context)
        val connected = com.example.sagap.isOnWifi(context)
        val ssid = when {
            wifi.ssid != null -> wifi.ssid
            connected -> "Connected Wi-Fi"
            else -> "Wi-Fi Disconnected"
        }
        val band = wifi.band ?: "Wi-Fi"
        val speed = wifi.linkSpeedMbps?.let { "Link speed $it Mbps" } ?: "Connected"
        val details = if (connected) "$band · $speed" else "Connect to Wi-Fi to test speed"

        // 2. Load stored test results from ResultStore
        val storedBackendResults = com.example.sagap.ResultStore.load(context)

        val loadedUiResults = if (storedBackendResults.isNotEmpty()) {
            storedBackendResults.map { backendResult ->
                val matchedSpot = SampleData.spots.firstOrNull {
                    it.name.contains(backendResult.spot, ignoreCase = true) || backendResult.spot.contains(it.name, ignoreCase = true)
                } ?: SampleData.spots.first()

                TestResult(
                    spotId = matchedSpot.id,
                    downloadMbps = backendResult.mbps,
                    pingMs = backendResult.pingMs.toInt(),
                    signalDbm = backendResult.rssi ?: -65,
                    timestamp = backendResult.timeMillis
                )
            }
        } else {
            SampleData.results
        }

        val last = loadedUiResults.firstOrNull()

        _state.update {
            it.copy(
                wifiInfo = WifiInfoUiState(ssid = ssid, details = details, isConnected = connected),
                results = loadedUiResults,
                history = loadedUiResults,
                lastResult = last ?: it.lastResult
            )
        }
    }

    fun selectSpot(spot: Spot) {
        if (!spot.onHold) _state.update { it.copy(selectedSpot = spot) }
    }

    fun setReportSpot(spotId: Int) {
        _state.update { it.copy(reportSpotId = spotId) }
    }

    fun startTest() {
        testJob?.cancel()
        val context = getApplication<Application>()
        val spot = _state.value.selectedSpot

        refreshState()

        _state.update {
            it.copy(
                test = TestUiState(phase = TestPhase.RUNNING, secondsLeft = 6, progress = 0f)
            )
        }

        testJob = viewModelScope.launch {
            val outcome = runWifiTest(
                context = context,
                spotName = spot.name,
                onProgress = { progress ->
                    _state.update {
                        it.copy(
                            test = it.test.copy(
                                progress = progress.percent / 100f,
                                secondsLeft = progress.secondsLeft,
                                signalDbm = progress.rssi ?: it.test.signalDbm,
                                pingMs = progress.pingMs?.toInt() ?: it.test.pingMs,
                                downloadMbps = progress.mbpsNow ?: it.test.downloadMbps
                            )
                        )
                    }
                }
            )

            when (outcome) {
                is TestOutcome.Success -> {
                    val r = outcome.result
                    val uiResult = TestResult(
                        spotId = spot.id,
                        downloadMbps = r.mbps,
                        pingMs = r.pingMs.toInt(),
                        signalDbm = r.rssi ?: -65,
                        timestamp = r.timeMillis
                    )

                    refreshState()

                    _state.update {
                        it.copy(
                            lastResult = uiResult,
                            test = it.test.copy(
                                phase = TestPhase.DONE,
                                progress = 1f,
                                secondsLeft = 0,
                                downloadMbps = r.mbps,
                                pingMs = r.pingMs.toInt(),
                                signalDbm = r.rssi ?: -65
                            )
                        )
                    }
                }
                is TestOutcome.Failed -> {
                    _state.update {
                        it.copy(
                            test = it.test.copy(
                                phase = TestPhase.DONE,
                                progress = 1f,
                                errorMessage = outcome.reason
                            )
                        )
                    }
                }
            }
        }
    }

    fun sendReport(spotId: Int, issue: IssueType, notes: String, attached: TestResult?) {
        val context = getApplication<Application>()
        val spot = SampleData.spots.firstOrNull { it.id == spotId } ?: _state.value.selectedSpot
        val reportType = when (issue) {
            IssueType.DEAD_ZONE -> ReportType.DEAD_ZONE
            IssueType.UNSAFE -> ReportType.HAZARD
        }
        val attachedBackendResult = attached?.let {
            com.example.sagap.TestResult(
                spot = spot.name,
                mbps = it.downloadMbps,
                pingMs = it.pingMs.toLong(),
                rating = com.example.sagap.rating(it.downloadMbps, it.pingMs.toLong(), it.signalDbm),
                timeMillis = it.timestamp,
                rssi = it.signalDbm
            )
        }
        val intent = submitReport(
            context = context,
            spotName = spot.name,
            type = reportType,
            notes = notes,
            attachTest = attachedBackendResult
        )
        openReportEmail(context, intent)
    }

    fun cancelTest() {
        testJob?.cancel()
        _state.update { it.copy(test = TestUiState()) }
    }
}
