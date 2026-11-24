package com.example.sagap.data.model

/** A room or zone on a floor. Position and size are fractions (0f..1f) of the floor plan. */
data class Area(
    val id: Int,
    val name: String,
    val mapX: Float,   // center
    val mapY: Float,
    val w: Float,      // width
    val h: Float       // height
)

data class Floor(
    val level: Int,
    val label: String,
    val areas: List<Area>,
    val imageRes: Int? = null
)

data class Reading(val downloadMbps: Double, val pingMs: Int, val signalDbm: Int)
