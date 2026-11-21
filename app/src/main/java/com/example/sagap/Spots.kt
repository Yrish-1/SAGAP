package com.example.sagap

import android.content.Context

// Approved campus spots. Only these can be selected or recommended.
// TODO: the last four are placeholders. Replace them with your real spots (keep 8 to match the wireframe).
val CAMPUS_SPOTS = listOf(
    Spot("Albert Einstein Building Ground Floor", "Albert Einstein Building"),
    Spot("Albert Einstein Building Second Floor", "Albert Einstein Building"),
    Spot("CICS Building", "CICS Building"),
    Spot("Sparta Gymnasium", "Sparta Gymnasium"),
    Spot("Student Center Lounge", "Student Center"),                       // placeholder
    Spot("Cafeteria", "Cafeteria"),                                        // placeholder
    Spot("KIST Park", "KIST Park"),                                        // placeholder
    Spot("COE Building", "COE Building", startsOnSafetyHold = true)        // placeholder, starts on safety hold
)

// Names only, so screens that use a plain list of names keep working
val SPOTS: List<String> = CAMPUS_SPOTS.map { it.name }

object SpotRepository {
    private const val PREFS = "sagap_spots"
    private const val KEY_HOLD = "safety_hold"

    fun find(name: String): Spot? = CAMPUS_SPOTS.firstOrNull { it.name == name }

    // Spots on safety hold (set by an admin). Until an admin changes anything, the defaults apply.
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
