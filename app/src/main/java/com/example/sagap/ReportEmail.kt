package com.example.sagap

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

// TODO: put the real SAGAP team email here
const val SAGAP_EMAIL = "sagap.team@example.com"

const val MAX_NOTES_LENGTH = 300

// The newest test on this phone (for the "Attach latest Wi-Fi test" checkbox)
fun latestTest(context: Context): TestResult? =
    ResultStore.load(context).maxByOrNull { it.timeMillis }

// Builds the report email. For dead-zone and hazard reports it also saves the report on the phone,
// so the spot's safety-hold rule works. spotName can be null for app problems and suggestions.
// No GPS location is included.
fun submitReport(
    context: Context,
    spotName: String?,
    type: ReportType,
    notes: String,
    attachTest: TestResult? = null
): Intent {
    if (spotName != null && (type == ReportType.DEAD_ZONE || type == ReportType.HAZARD)) {
        ReportStore.add(context, spotName, type)
    }

    val subject = "[SAGAP] ${type.label}" + (spotName?.let { " - $it" } ?: "")
    val body = buildString {
        appendLine("Report type: ${type.label}")
        if (spotName != null) appendLine("Spot: $spotName")
        appendLine()
        appendLine("Notes:")
        appendLine(notes.take(MAX_NOTES_LENGTH))
        if (attachTest != null) {
            appendLine()
            appendLine("Attached Wi-Fi test:")
            appendLine("Spot: ${attachTest.spot}")
            appendLine("Time: ${java.util.Date(attachTest.timeMillis)}")
            appendLine("Rating: ${attachTest.rating}")
            appendLine("Download: %.1f Mbps".format(attachTest.mbps))
            appendLine("Ping: ${attachTest.pingMs} ms")
            attachTest.rssi?.let { appendLine("Signal: $it dBm") }
            if (attachTest.isSample) appendLine("(sample data)")
        }
        appendLine()
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}")
        appendLine("No GPS location is included in this report.")
    }

    return Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SAGAP_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
}

// Opens the email app. Returns false if the phone has no email app.
fun openReportEmail(context: Context, intent: Intent): Boolean {
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
