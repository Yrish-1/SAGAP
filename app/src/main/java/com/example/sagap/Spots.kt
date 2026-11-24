package com.example.sagap

import android.content.Context

// Approved campus spots (3 main buildings)
val CAMPUS_SPOTS = listOf(
    Spot("Albert Einstein Building", "Albert Einstein Building"),
    Spot("CICS Building", "CICS Building"),
    Spot("Sparta Gymnasium", "Sparta Gymnasium")
)

// Names only, so screens that use a plain list of names keep working
val SPOTS: List<String> = CAMPUS_SPOTS.map { it.name }

object SpotRepository {
    private const val PREFS = "sagap_spots"
    private const val KEY_HOLD = "safety_hold"

    fun find(name: String): Spot? = CAMPUS_SPOTS.firstOrNull { it.name == name }

    fun holdNames(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getStringSet(KEY_HOLD, null)
        return stored?.toSet() ?: CAMPUS_SPOTS.filter { it.startsOnSafetyHold }.map { it.name }.toSet()
    }

    fun setHold(context: Context, spotName: String, hold: Boolean) {
        val set = holdNames(context).toMutableSet()
        if (hold) set.add(spotName) else set.remove(spotName)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_HOLD, set).apply()
    }

    fun isOpenAtHour(spot: Spot, hour: Int): Boolean = hour >= spot.openHour && hour < spot.closeHour
}
