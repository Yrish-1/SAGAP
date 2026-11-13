package com.example.sagap.ui.screens.test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sagap.data.model.Grade
import com.example.sagap.data.model.Spot
import com.example.sagap.data.model.TestResult
import com.example.sagap.data.sample.SampleData
import com.example.sagap.domain.Activity
import com.example.sagap.domain.RatingEngine
import com.example.sagap.domain.Verdict
import com.example.sagap.domain.dayAndTime
import com.example.sagap.domain.fitFor
import com.example.sagap.ui.components.GradeChip
import com.example.sagap.ui.components.MetricTiles
import com.example.sagap.ui.components.PrimaryButton
import com.example.sagap.ui.components.ScreenTitle
import com.example.sagap.ui.components.SecondaryButton
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import com.example.sagap.ui.components.SectionTitle
import com.example.sagap.ui.components.WhiteCard
import com.example.sagap.ui.theme.*

@Composable
fun ResultScreen(
    spot: Spot,
    result: TestResult,
    onViewMap: () -> Unit,
    onTestAgain: () -> Unit,
    onGoHome: () -> Unit
) {
    val grade = RatingEngine.grade(result.downloadMbps, result.pingMs, result.signalDbm)
    val title = when (grade) {
        Grade.EXCELLENT, Grade.GOOD -> "You’re good to go"
        Grade.FAIR -> "Usable, with limits"
        else -> "Not ideal here"
    }

    Column(Modifier.fillMaxSize()) {

        // Scrollable results
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScreenTitle(title, "${spot.name} · ${dayAndTime(result.timestamp)}")

            WhiteCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Teal)
                        Spacer(Modifier.width(8.dp))
                        Text("Test complete", fontWeight = FontWeight.Bold, color = TextDark)
                    }
                    GradeChip(grade)
                }
                Spacer(Modifier.height(12.dp))
                MetricTiles(result.downloadMbps, result.pingMs, result.signalDbm)
                Spacer(Modifier.height(12.dp))
                Text("${SampleData.WIFI_NAME} · 5 GHz", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Wi-Fi only · Estimates · Saved to history",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            SectionTitle("What works here?")
            WhiteCard {
                val items = fitFor(result)
                items.forEachIndexed { index, item ->
                    FitRow(item.activity, item.note, item.verdict)
                    if (index < items.lastIndex) {
                        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Color(0xFFE6ECF0))
                    }
                }
            }

            WhiteCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = Teal)
                    Spacer(Modifier.width(8.dp))
                    Text("How this demo rates Wi-Fi", fontWeight = FontWeight.Bold, color = TextDark)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Proposed thresholds, not official campus standards. Grade each metric separately; " +
                            "the lowest metric grade sets the overall rating.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Grade", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Text("Mbps ↓ / Ping / Signal", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                ThresholdRow("Excellent", GradeGreen, "≥25 / ≤30 ms / ≥−60 dBm")
                ThresholdRow("Good", GradeGreen, "≥10 / ≤60 ms / ≥−67 dBm")
                ThresholdRow("Fair", GradeAmber, "≥2 / ≤120 ms / ≥−75 dBm")
                ThresholdRow("Poor", GradeRed, "Any metric misses Fair limits")
            }

            Text(
                "A single test cannot guarantee a stable class or exam. Signal, latency variation and " +
                        "interruptions matter too.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Pinned action bar: always visible, never hidden behind scrolling content
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            HorizontalDivider(color = Color(0xFFE6ECF0))
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrimaryButton("View spot on campus map", onClick = onViewMap, icon = Icons.Outlined.Map)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Go back home", onClick = onGoHome, icon = Icons.AutoMirrored.Outlined.ArrowBack, modifier = Modifier.weight(1f))
                    SecondaryButton("Test again", onClick = onTestAgain, icon = Icons.Outlined.Refresh, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun activityIcon(a: Activity): ImageVector = when (a) {
    Activity.CLASS -> Icons.Outlined.Videocam
    Activity.EXAM -> Icons.Outlined.Shield
    Activity.DOWNLOAD -> Icons.Outlined.Download
    Activity.GAMING -> Icons.Outlined.SportsEsports
    Activity.BROWSING -> Icons.Outlined.Language
}

private fun verdictColor(v: Verdict): Color = when (v) {
    Verdict.GOOD -> GradeGreen
    Verdict.CHECK, Verdict.LIMITED -> GradeAmber
    Verdict.POOR -> GradeRed
}

@Composable
private fun FitRow(activity: Activity, note: String, verdict: Verdict) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(activityIcon(activity), contentDescription = null, tint = Teal)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(activity.title, fontWeight = FontWeight.Bold, color = TextDark)
            Text(note, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            verdict.label,
            color = verdictColor(verdict),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ThresholdRow(label: String, color: Color, rule: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(rule, color = TextMuted, style = MaterialTheme.typography.bodySmall)
    }
}