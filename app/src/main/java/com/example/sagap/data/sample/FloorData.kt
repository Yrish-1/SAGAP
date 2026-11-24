package com.example.sagap.data.sample

import com.example.sagap.R
import com.example.sagap.data.model.Area
import com.example.sagap.data.model.Floor
import com.example.sagap.data.model.Grade
import com.example.sagap.data.model.Reading
import com.example.sagap.domain.RatingEngine
import kotlin.math.roundToInt

object FloorData {

    private fun makeFloor(
        level: Int,
        label: String,
        baseId: Int,
        roomNames: List<String>,
        imageRes: Int
    ): Floor {
        val areas = roomNames.mapIndexed { i, name ->
            Area(
                id = baseId + i,
                name = name,
                mapX = 0.5f,
                mapY = 0.5f,
                w = 0.2f,
                h = 0.2f
            )
        }
        return Floor(level, label, areas, imageRes)
    }

    /** Floors of a building (spot id). */
    fun floorsFor(spotId: Int): List<Floor> = when (spotId) {
        1 -> listOf(  // Albert Einstein Building (2 floors)
            makeFloor(
                1, "1st Floor", 110,
                listOf("Lobby", "Cafeteria", "Audio Visual Room", "CEAFA Faculty Lounge", "CE Lab", "Chemistry Lab", "Electronics Systems Lab", "Power Electronics Lab", "CAD Lab"),
                R.drawable.einstein_floor1
            ),
            makeFloor(
                2, "2nd Floor", 120,
                listOf("Library", "E-Lib", "Classroom 207", "Classroom 208", "Classroom 209", "Classroom 210", "Classroom 211", "Classroom 212", "Engineering Faculty Room", "Conference Room", "COE Dean's Office", "VCAA Office", "ETEEAP Office", "Department Chair's Office"),
                R.drawable.einstein_floor2
            )
        )
        2 -> listOf(  // CICS Building (5 floors)
            makeFloor(1, "1st Floor", 210, listOf("Female Toilet", "Room 101", "Room 102", "Room 103", "Room 104", "Room 105", "Room 106", "Male Toilet"), R.drawable.cics_floor1),
            makeFloor(2, "2nd Floor", 220, listOf("Female Toilet", "Room 201", "Room 202", "CPE Faculty", "CICS Student Services", "Consultation Office", "CICS Faculty", "Dean's Office", "Male Toilet"), R.drawable.cics_floor2),
            makeFloor(3, "3rd Floor", 230, listOf("Female Toilet", "Room 501", "Room 502", "Room 503", "Room 504", "Room 505", "Room 506", "Male Toilet"), R.drawable.cics_floor3),
            makeFloor(4, "4th Floor", 240, listOf("Female Toilet", "Room 401", "Physics Lab", "Smart Lab", "EDL Lab", "Tech Lab", "ITL Lab", "Male Toilet"), R.drawable.cics_floor4),
            makeFloor(5, "5th Floor", 250, listOf("Female Toilet", "Lab 1", "Lab 2", "Lab 3", "Lab 4", "Lab 5", "Lab 6", "Male Toilet"), R.drawable.cics_floor5)
        )
        3 -> listOf(  // Sparta Gymnasium (1 floor)
            makeFloor(
                level = 1,
                label = "Main Gym Floor",
                baseId = 310,
                roomNames = listOf("Main Court", "Stage", "Bleachers", "Entrance"),
                imageRes = R.drawable.gym_floor
            )
        )
        else -> emptyList()
    }

    val readings: Map<Int, Reading> = mapOf(
        // Einstein
        110 to Reading(21.4, 28, -58), 111 to Reading(19.8, 31, -61), 112 to Reading(17.2, 36, -64),
        113 to Reading(15.0, 40, -66), 114 to Reading(18.3, 33, -62), 115 to Reading(16.5, 35, -63),
        120 to Reading(14.2, 44, -65), 121 to Reading(9.1, 63, -69), 122 to Reading(6.5, 85, -72),
        123 to Reading(11.8, 50, -66), 124 to Reading(12.5, 48, -65), 125 to Reading(10.1, 55, -68),
        // CICS
        210 to Reading(44.0, 17, -51), 211 to Reading(41.2, 19, -53), 212 to Reading(38.5, 22, -55),
        213 to Reading(40.1, 20, -54), 214 to Reading(36.0, 24, -57), 215 to Reading(35.0, 25, -58),
        220 to Reading(33.4, 25, -56), 221 to Reading(28.9, 27, -58), 222 to Reading(22.1, 34, -62),
        223 to Reading(30.2, 26, -57), 224 to Reading(19.5, 38, -63), 225 to Reading(25.0, 30, -60),
        230 to Reading(16.4, 41, -65), 231 to Reading(12.8, 52, -66), 232 to Reading(8.3, 70, -70),
        233 to Reading(14.9, 45, -65), 234 to Reading(11.0, 58, -68),
        240 to Reading(22.0, 32, -60), 241 to Reading(18.5, 38, -63), 242 to Reading(15.0, 42, -65),
        250 to Reading(12.0, 48, -67), 251 to Reading(10.5, 52, -69), 252 to Reading(8.0, 60, -71),
        // Gymnasium
        310 to Reading(28.5, 25, -57), 311 to Reading(24.0, 30, -60), 312 to Reading(26.2, 28, -58),
        313 to Reading(22.1, 33, -61)
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
