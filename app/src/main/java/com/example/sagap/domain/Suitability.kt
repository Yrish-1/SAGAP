package com.example.sagap.domain

import com.example.sagap.data.model.TestResult

enum class Activity(val title: String) {
    CLASS("Online class"),
    EXAM("Online exam"),
    DOWNLOAD("File download"),
    GAMING("Gaming"),
    BROWSING("Casual browsing")
}

enum class Verdict(val label: String) {
    GOOD("Good fit"),
    CHECK("Check stability"),
    LIMITED("Limited"),
    POOR("Not suitable")
}

data class FitItem(val activity: Activity, val note: String, val verdict: Verdict)

/** Turns one test result into the "What works here?" list. */
fun fitFor(r: TestResult): List<FitItem> {
    val mbps = r.downloadMbps
    val ping = r.pingMs
    val signal = r.signalDbm

    val cls = when {
        mbps >= 5 && ping <= 120 ->
            FitItem(Activity.CLASS, "2–5 Mbps is typical video-class context.", Verdict.GOOD)
        mbps >= 2 ->
            FitItem(Activity.CLASS, "Borderline for video; expect lower quality.", Verdict.LIMITED)
        else ->
            FitItem(Activity.CLASS, "Too slow for a video class.", Verdict.POOR)
    }

    val exam = when {
        mbps >= 5 && ping <= 120 && signal >= -75 ->
            FitItem(Activity.EXAM, "Run Exam Mode first; speed is not a guarantee.", Verdict.CHECK)
        else ->
            FitItem(Activity.EXAM, "Too weak for an exam. Try a better spot.", Verdict.POOR)
    }

    val download = when {
        mbps >= 10 -> FitItem(Activity.DOWNLOAD, "Large files should download comfortably.", Verdict.GOOD)
        mbps >= 2 -> FitItem(Activity.DOWNLOAD, "Downloads will work, but slowly.", Verdict.LIMITED)
        else -> FitItem(Activity.DOWNLOAD, "Expect very slow downloads.", Verdict.POOR)
    }

    val gaming = when {
        ping <= 60 && mbps >= 5 ->
            FitItem(Activity.GAMING, "$ping ms ping; watch for spikes or packet loss.", Verdict.GOOD)
        ping <= 120 ->
            FitItem(Activity.GAMING, "$ping ms ping may cause lag.", Verdict.LIMITED)
        else ->
            FitItem(Activity.GAMING, "High ping; games will lag.", Verdict.POOR)
    }

    val browsing = when {
        mbps >= 2 -> FitItem(Activity.BROWSING, "Plenty of bandwidth for everyday pages.", Verdict.GOOD)
        mbps >= 0.5 -> FitItem(Activity.BROWSING, "Pages may load slowly.", Verdict.LIMITED)
        else -> FitItem(Activity.BROWSING, "Browsing will be unreliable.", Verdict.POOR)
    }

    return listOf(cls, exam, download, gaming, browsing)
}
