package com.example.sagap.data.sample

import com.example.sagap.data.model.Area
import com.example.sagap.data.model.Floor
import com.example.sagap.data.model.Grade
import com.example.sagap.data.model.Reading
import com.example.sagap.domain.RatingEngine
import kotlin.math.roundToInt

object FloorData {

    // Same 5-slot layout on every floor: 2 rooms on top, a hallway, 2 rooms at the bottom
    private val slots = listOf(
        floatArrayOf(0.25f, 0.25f, 0.40f, 0.30f),
        floatArrayOf(0.75f, 0.25f, 0.40f, 0.30f),
        floatArrayOf(0.50f, 0.58f, 0.90f, 0.12f),
        floatArrayOf(0.25f, 0.82f, 0.40f, 0.28f),
        floatArrayOf(0.75f, 0.82f, 0.40f, 0.28f)
    )

    private fun makeFloor(level: Int, label: String, baseId: Int, names: List<String>) =
        Floor(
            level, label,
            names.mapIndexed { i, name ->
                val s = slots[i]
                Area(baseId + i, name, s[0], s[1], s[2], s[3])
            }
        )

    /** Floors of a building (spot id). Empty = no floor map for that building yet. */
    fun floorsFor(spotId: Int): List<Floor> = when (spotId) {
        1 -> listOf(  // Einstein Building
            makeFloor(1, "1st floor", 110, listOf("Lobby", "Study Room 101", "Hallway", "Room 103", "Room 104")),
            makeFloor(2, "2nd floor", 120, listOf("Room 201", "Room 202", "Hallway", "Room 203", "Room 204")),
            makeFloor(3, "3rd floor", 130, listOf("Room 301", "Room 302", "Hallway", "Room 303", "Room 304"))
        )
        2 -> listOf(  // CICS Building
            makeFloor(1, "1st floor", 210, listOf("Lobby", "Registrar", "Hallway", "Lab 101", "Lab 102")),
            makeFloor(2, "2nd floor", 220, listOf("Lab 201", "Lab 202", "Hallway", "Faculty Room", "Lab 203")),
            makeFloor(3, "3rd floor", 230, listOf("Lab 301", "Lab 302", "Hallway", "Conference Room", "Lab 303")),
            makeFloor(4, "4th floor", 240, listOf("Room 401", "Room 402", "Hallway", "Room 403", "Room 404"))
        )
        else -> emptyList()
    }

    // Sample readings per area id. Areas missing here show as "No data".
    val readings: Map<Int, Reading> = mapOf(
        // Einstein
        110 to Reading(21.4, 28, -58), 111 to Reading(19.8, 31, -61), 112 to Reading(17.2, 36, -64),
        113 to Reading(15.0, 40, -66), 114 to Reading(18.3, 33, -62),
        120 to Reading(14.2, 44, -65), 121 to Reading(9.1, 63, -69), 122 to Reading(6.5, 85, -72),
        123 to Reading(11.8, 50, -66), 124 to Reading(3.2, 110, -77),
        130 to Reading(7.4, 80, -71), 131 to Reading(2.1, 118, -75), 132 to Reading(1.2, 160, -80),
        // CICS
        210 to Reading(44.0, 17, -51), 211 to Reading(41.2, 19, -53), 212 to Reading(38.5, 22, -55),
        213 to Reading(40.1, 20, -54), 214 to Reading(36.0, 24, -57),
        220 to Reading(33.4, 25, -56), 221 to Reading(28.9, 27, -58), 222 to Reading(22.1, 34, -62),
        223 to Reading(30.2, 26, -57), 224 to Reading(19.5, 38, -63),
        230 to Reading(16.4, 41, -65), 231 to Reading(12.8, 52, -66), 232 to Reading(8.3, 70, -70),
        233 to Reading(14.9, 45, -65),
        240 to Reading(5.6, 95, -73), 241 to Reading(3.1, 112, -76), 242 to Reading(1.6, 150, -79)
    )

    fun gradeOf(r: Reading?): Grade =
        if (r == null) Grade.NO_DATA
        else RatingEngine.grade(r.downloadMbps, r.pingMs, r.signalDbm)

    fun averageOf(floor: Floor): Reading? {
        val list = floor.areas.mapNotNull { readings[it.id] }
        if (list.isEmpty()) return null
        return Reading(
            downloadMbps = list.map { it.downloadMbps }.average(),
            pingMs = list.map { it.pingMs }.average().roundToInt(),
            signalDbm = list.map { it.signalDbm }.average().roundToInt()
        )
    }
}