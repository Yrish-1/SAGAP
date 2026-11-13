package com.example.sagap.ui.screens.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sagap.data.model.Spot
import com.example.sagap.data.model.TestResult
import com.example.sagap.data.repository.SpotRepository
import com.example.sagap.data.sample.SampleData
import com.example.sagap.domain.RatingEngine
import com.example.sagap.domain.clockTime
import com.example.sagap.domain.fmtMbps
import com.example.sagap.domain.timeAgo
import com.example.sagap.ui.components.GradeChip
import com.example.sagap.ui.components.InfoCard
import com.example.sagap.ui.components.MetricTiles
import com.example.sagap.ui.components.PrimaryButton
import com.example.sagap.ui.components.SecondaryButton
import com.example.sagap.ui.components.SectionTitle
import com.example.sagap.ui.components.StatusPill
import com.example.sagap.ui.components.WhiteCard
import com.example.sagap.ui.theme.*

@Composable
fun SpotDetailScreen(
    spot: Spot,
    results: List<TestResult>,
    isSelected: Boolean,
    onBack: () -> Unit,
    onTest: () -> Unit,
    onReport: () -> Unit
) {
    val avg = remember(results, spot.id) { SpotRepository.average(results, spot.id) }
    val recent = remember(results, spot.id) { SpotRepository.recent(results, spot.id).take(3) }
    val grade = SpotRepository.gradeOf(avg)
    val tealSoft = Color(0xFFE3F1EE)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (spot.onHold) StatusPill("SAFETY HOLD", GradeRed, GradeRedBg)
            else StatusPill("OPEN", GradeGreen, GradeGreenBg)
            if (isSelected) StatusPill("Selected", Teal, tealSoft)
        }

        WhiteCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("24-hour average", fontWeight = FontWeight.Bold, color = TextDark)
                GradeChip(grade)
            }
            Spacer(Modifier.height(12.dp))
            if (avg != null) {
                MetricTiles(avg.downloadMbps, avg.pingMs, avg.signalDbm)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Sample data · ${SpotRepository.testCount24h(results, spot.id)} tests / 24 h · " +
                            "Updated ${timeAgo(avg.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Text(
                    "Averages use the same proposed demo rating rules.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            } else {
                Text("No recent data for this spot yet.", color = TextMuted)
            }
        }

        WhiteCard {
            val trend = SampleData.trend
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Download trend", fontWeight = FontWeight.Bold, color = TextDark)
                Text(
                    "${trend.min().toInt()}–${trend.max().toInt()} Mbps",
                    color = Teal,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(12.dp))
            TrendChart(trend)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("08:00", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Text("14:00", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Sample pattern · Yesterday, 08:00–14:00. Illustrative test averages, not a live forecast.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        SectionTitle("Recent sample tests")
        WhiteCard {
            if (recent.isEmpty()) {
                Text("No tests yet.", color = TextMuted)
            }
            recent.forEachIndexed { index, r ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(clockTime(r.timestamp), color = TextMuted, modifier = Modifier.width(52.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${fmtMbps(r.downloadMbps)} Mbps", fontWeight = FontWeight.Bold, color = TextDark)
                        Text(
                            "${r.pingMs} ms · ${r.signalDbm} dBm",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    GradeChip(RatingEngine.grade(r.downloadMbps, r.pingMs, r.signalDbm))
                }
                if (index < recent.lastIndex) {
                    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Color(0xFFE6ECF0))
                }
            }
        }

        InfoCard(
            icon = Icons.Outlined.Shield,
            title = "Good bandwidth is only a start",
            body = "For an online exam, monitor stability before joining. Report any safety concern; " +
                    "held spots are excluded from recommendations.",
            container = tealSoft,
            content = Teal
        )

        PrimaryButton(
            text = "Test Wi-Fi at this spot",
            onClick = onTest,
            icon = Icons.Outlined.Speed,
            enabled = !spot.onHold
        )
        SecondaryButton("Report a problem here", onClick = onReport, icon = Icons.Outlined.Flag)
    }
}

@Composable
private fun TrendChart(values: List<Float>) {
    Canvas(Modifier.fillMaxWidth().height(110.dp)) {
        val min = values.min()
        val range = (values.max() - min).coerceAtLeast(0.001f)

        // three horizontal grid lines
        for (i in 0..2) {
            val y = size.height * i / 2f
            drawLine(Color(0xFFDDE5EA), Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
        }

        val path = Path()
        values.forEachIndexed { i, v ->
            val x = size.width * i / (values.size - 1)
            val y = size.height * 0.95f - (v - min) / range * size.height * 0.9f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = Teal,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
