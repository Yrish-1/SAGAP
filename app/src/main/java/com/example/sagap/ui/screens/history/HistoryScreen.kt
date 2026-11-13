package com.example.sagap.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sagap.data.model.Grade
import com.example.sagap.data.model.Spot
import com.example.sagap.data.model.TestResult
import com.example.sagap.domain.RatingEngine
import com.example.sagap.domain.clockTime
import com.example.sagap.domain.dayLabel
import com.example.sagap.domain.fmtMbps
import com.example.sagap.domain.last7DaysLabel
import com.example.sagap.ui.components.GradeChip
import com.example.sagap.ui.components.WhiteCard
import com.example.sagap.ui.theme.*

private enum class HistoryFilter { ALL, GOOD, ATTENTION }

private fun isGood(g: Grade) = g == Grade.EXCELLENT || g == Grade.GOOD

@Composable
fun HistoryScreen(history: List<TestResult>, spots: List<Spot>) {
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }

    val entries = remember(history) {
        history
            .sortedByDescending { it.timestamp }
            .map { it to RatingEngine.grade(it.downloadMbps, it.pingMs, it.signalDbm) }
    }
    val goodCount = entries.count { isGood(it.second) }
    val attentionCount = entries.size - goodCount

    val shown = when (filter) {
        HistoryFilter.ALL -> entries
        HistoryFilter.GOOD -> entries.filter { isGood(it.second) }
        HistoryFilter.ATTENTION -> entries.filter { !isGood(it.second) }
    }
    val grouped = shown.groupBy { dayLabel(it.first.timestamp) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Teal)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Last 7 days · ${last7DaysLabel()}",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = TextMuted)
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Navy)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Stat(entries.size.toString(), "Wi-Fi tests")
                Stat(goodCount.toString(), "Good or better")
                Stat(attentionCount.toString(), "Need attention")
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("All ${entries.size}", filter == HistoryFilter.ALL) { filter = HistoryFilter.ALL }
                FilterPill("Good+ $goodCount", filter == HistoryFilter.GOOD) { filter = HistoryFilter.GOOD }
                FilterPill("Needs attention $attentionCount", filter == HistoryFilter.ATTENTION) {
                    filter = HistoryFilter.ATTENTION
                }
            }
        }

        if (shown.isEmpty()) {
            item { Text("No tests here yet.", color = TextMuted) }
        }

        grouped.forEach { (label, dayEntries) ->
            item(key = "header-$label") {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )
            }
            items(dayEntries, key = { "${it.first.spotId}-${it.first.timestamp}" }) { entry ->
                val (r, g) = entry
                val name = spots.firstOrNull { it.id == r.spotId }?.name ?: "Unknown spot"
                WhiteCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Bold, color = TextDark)
                            Text(
                                "${fmtMbps(r.downloadMbps)} Mbps · ${r.pingMs} ms · ${r.signalDbm} dBm",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                            Text(
                                "${clockTime(r.timestamp)} · Wi-Fi only · Sample",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        GradeChip(g)
                    }
                }
            }
        }

        item {
            Text(
                "Sample personal history · All values are estimates. These are your individual tests; " +
                        "campus markers show averages across recent student tests.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(
        label,
        color = if (selected) Teal else TextDark,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Color(0xFFE3F1EE) else Color.White)
            .border(1.dp, if (selected) Teal else Color(0xFFD5DEE3), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}
