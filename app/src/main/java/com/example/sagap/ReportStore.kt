package com.example.sagap

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// Stores and loads user reports (dead-zone, hazard, etc.) on the phone
object ReportStore {
    private const val PREFS = "sagap_reports"
    private const val KEY = "list"

    fun load(context: Context): List<Report> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY, "[]") ?: "[]"
        val arr = try {
            JSONArray(jsonStr)
        } catch (e: Exception) {
            JSONArray()
        }
        val list = mutableListOf<Report>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val spotName = o.optString("spotName")
            val typeStr = o.optString("type")
            val timeMillis = o.optLong("timeMillis", 0L)
            val type = runCatching { ReportType.valueOf(typeStr) }.getOrNull()
            if (spotName.isNotEmpty() && type != null) {
                list.add(Report(spotName, type, timeMillis))
            }
        }
        return list
    }

    fun add(context: Context, spotName: String, type: ReportType, timeMillis: Long = System.currentTimeMillis()) {
        add(context, Report(spotName, type, timeMillis))
    }

    fun add(context: Context, report: Report) {
        val current = load(context).toMutableList()
        current.add(report)
        save(context, current)
    }

    fun clearForSpot(context: Context, spotName: String) {
        val filtered = load(context).filterNot { it.spotName == spotName }
        save(context, filtered)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY).apply()
    }

    private fun save(context: Context, reports: List<Report>) {
        val arr = JSONArray()
        reports.forEach { r ->
            arr.put(
                JSONObject()
                    .put("spotName", r.spotName)
                    .put("type", r.type.name)
                    .put("timeMillis", r.timeMillis)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}
