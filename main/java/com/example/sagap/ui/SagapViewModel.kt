package com.example.sagap.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sagap.data.model.Spot
import com.example.sagap.data.model.TestResult
import com.example.sagap.data.repository.SpotRepository
import com.example.sagap.data.sample.SampleData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

enum class TestPhase { IDLE, RUNNING, DONE }

data class TestUiState(
    val phase: TestPhase = TestPhase.IDLE,
    val progress: Float = 0f,
    val secondsLeft: Int = 0,
    val downloadMbps: Double = 0.0,
    val pingMs: Int? = null,
    val signalDbm: Int? = null
)

data class SagapState(
    val selectedSpot: Spot = SampleData.spots.first(),
    val reportSpotId: Int? = null,
    val test: TestUiState = TestUiState(),
    val lastResult: TestResult? = null,
    val results: List<TestResult> = SampleData.results,   // campus-wide (map, spot detail)
    val history: List<TestResult> = SampleData.history    // this student's own tests
)

/**
 * One ViewModel shared by every screen. Screens only READ [state] and call these functions.
 * The speed test below is SIMULATED; swap the loop for the real SpeedTester later.
 */
class SagapViewModel : ViewModel() {

    private val _state = MutableStateFlow(SagapState())
    val state: StateFlow<SagapState> = _state.asStateFlow()

    private var testJob: Job? = null

    fun selectSpot(spot: Spot) {
        if (!spot.onHold) _state.update { it.copy(selectedSpot = spot) }
    }

    fun setReportSpot(spotId: Int) {
        _state.update { it.copy(reportSpotId = spotId) }
    }

    fun startTest() {
        testJob?.cancel()
        val spot = _state.value.selectedSpot
        val base = SpotRepository.latest(_state.value.results, spot.id)

        // Fake "measured" values near the spot's recent readings
        val targetMbps = ((base?.downloadMbps ?: 8.0) * Random.nextDouble(0.9, 1.1) * 10).roundToInt() / 10.0
        val targetPing = ((base?.pingMs ?: 60) * Random.nextDouble(0.9, 1.1)).roundToInt()
        val targetDbm = (base?.signalDbm ?: -68) + Random.nextInt(-2, 3)

        // Set RUNNING right away so the Testing screen never sees an old DONE state
        _state.update { it.copy(test = TestUiState(phase = TestPhase.RUNNING, secondsLeft = 10)) }

        testJob = viewModelScope.launch {
            val totalMs = 10_000L
            val stepMs = 100L
            var elapsed = 0L
            while (elapsed < totalMs) {
                delay(stepMs)
                elapsed += stepMs
                val p = elapsed.toFloat() / totalMs
                val q = ((p - 0.15f) / 0.85f).coerceIn(0f, 1f)
                _state.update {
                    it.copy(
                        test = it.test.copy(
                            progress = p,
                            secondsLeft = ceil((totalMs - elapsed) / 1000.0).toInt(),
                            signalDbm = if (p >= 0.05f) targetDbm else null,
                            pingMs = if (p >= 0.15f) targetPing else null,
                            downloadMbps = targetMbps * sqrt(q.toDouble())
                        )
                    )
                }
            }
            val result = TestResult(spot.id, targetMbps, targetPing, targetDbm, System.currentTimeMillis())
            _state.update {
                it.copy(
                    lastResult = result,
                    results = listOf(result) + it.results,
                    history = listOf(result) + it.history,
                    test = it.test.copy(
                        phase = TestPhase.DONE,
                        progress = 1f,
                        secondsLeft = 0,
                        downloadMbps = targetMbps
                    )
                )
            }
        }
    }

    fun cancelTest() {
        testJob?.cancel()
        _state.update { it.copy(test = TestUiState()) }
    }
}
