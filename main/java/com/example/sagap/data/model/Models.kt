package com.example.sagap.data.model

enum class Grade { EXCELLENT, GOOD, FAIR, POOR, NO_DATA }

data class Spot(
    val id: Int,
    val name: String,
    val onHold: Boolean = false,   // "Safety hold"
    val mapX: Float,               // 0f..1f across the map image
    val mapY: Float                // 0f..1f down the map image
)

data class TestResult(
    val spotId: Int,
    val downloadMbps: Double,
    val pingMs: Int,
    val signalDbm: Int,
    val timestamp: Long
)