package com.example.sagap.ui.screens.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sagap.data.model.Grade
import com.example.sagap.data.model.Spot
import com.example.sagap.data.model.TestResult
import com.example.sagap.data.repository.SpotRepository
import com.example.sagap.domain.fmtMbps
import com.example.sagap.domain.timeAgo
import com.example.sagap.ui.components.GradeChip
import com.example.sagap.ui.components.PrimaryButton
import com.example.sagap.ui.components.gradeColors
import com.example.sagap.ui.components.gradeLabel
import com.example.sagap.ui.theme.*

@Composable
fun MapScreen(
    spots: List<Spot>,
    results: List<TestResult>,
    onSpotClick: (Spot) -> Unit
) {
    val best = remember(spots, results) { SpotRepository.best(spots, results) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CampusMap(
            spots = spots,
            results = results,
            highlightId = best?.first?.id,
            onSpotClick = onSpotClick
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LegendItem(GradeGreen, "Good / Excellent")
            LegendItem(GradeAmber, "Fair")
            LegendItem(GradeRed, "Poor")
            LegendItem(GradeGray, "No recent data")
        }

        Text(
            "Sample averages of tests in the last 24 h. Numbered markers, not a heat map. " +
                    "Schematic only; no GPS tracking. Safety holds are never recommended.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )

        if (best != null) {
            BestMatchCard(
                spot = best.first,
                avg = best.second,
                testCount = SpotRepository.testCount24h(results, best.first.id),
                onClick = { onSpotClick(best.first) }
            )
        }
    }
}

@Composable
private fun CampusMap(
    spots: List<Spot>,
    results: List<TestResult>,
    highlightId: Int?,
    onSpotClick: (Spot) -> Unit
) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.95f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFE3EBE3))
    ) {
        val w = maxWidth
        val h = maxHeight
        val cardW = w * 0.29f
        val cardH = 72.dp

        // Background: walkways, central quad, park, trees (all in fractions of the map size)
        Canvas(Modifier.fillMaxSize()) {
            fun block(x: Float, y: Float, bw: Float, bh: Float, color: Color) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * x, size.height * y),
                    size = Size(size.width * bw, size.height * bh),
                    cornerRadius = CornerRadius(16f, 16f)
                )
            }
            val walk = Color(0xFFF7F3E8)
            block(0.46f, 0f, 0.08f, 1f, walk)
            block(0f, 0.34f, 1f, 0.04f, walk)
            block(0f, 0.64f, 1f, 0.04f, walk)
            block(0.36f, 0.39f, 0.28f, 0.22f, Color(0xFFD2E3D0))
            block(0.03f, 0.68f, 0.30f, 0.29f, Color(0xFFD2E3D0))
            val tree = Color(0xFFBFD6BE)
            listOf(
                0.05f to 0.06f, 0.36f to 0.05f, 0.62f to 0.10f, 0.95f to 0.40f,
                0.04f to 0.62f, 0.62f to 0.90f, 0.40f to 0.45f, 0.58f to 0.55f
            ).forEach { (x, y) ->
                drawCircle(tree, radius = 9.dp.toPx(), center = Offset(size.width * x, size.height * y))
            }
        }

        Text(
            "BatStateU · ALANGILAN CAMPUS",
            modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 8.dp),
            fontSize = 9.sp,
            color = TextMuted
        )
        Text(
            "SCHEMATIC · NOT TO SCALE",
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 6.dp),
            fontSize = 9.sp,
            color = TextMuted
        )

        spots.forEach { spot ->
            val grade = SpotRepository.gradeOf(SpotRepository.average(results, spot.id))
            val x = maxOf(4.dp, minOf(w * spot.mapX - cardW / 2, w - cardW - 4.dp))
            val y = maxOf(4.dp, minOf(h * spot.mapY - cardH / 2, h - cardH - 4.dp))
            MapSpotCard(
                spot = spot,
                grade = grade,
                highlight = spot.id == highlightId,
                modifier = Modifier.offset(x, y).size(cardW, cardH),
                onClick = { onSpotClick(spot) }
            )
        }
    }
}

@Composable
private fun MapSpotCard(
    spot: Spot,
    grade: Grade,
    highlight: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val badgeColor = if (spot.onHold) GradeAmber else gradeColors(grade).first
    val statusText = when {
        spot.onHold -> "Safety hold"
        grade == Grade.NO_DATA -> "No recent data"
        else -> gradeLabel(grade)
    }
    val statusColor = if (spot.onHold) GradeRed else gradeColors(grade).first
    val shape = RoundedCornerShape(10.dp)

    Column(
        modifier
            .clip(shape)
            .background(Color.White)
            .border(
                width = if (highlight) 2.dp else 1.dp,
                color = if (highlight) Teal else Color(0xFFC9D2D8),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(22.dp).background(badgeColor, CircleShape), contentAlignment = Alignment.Center) {
            Text(spot.id.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            spot.name,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = TextDark
        )
        Text(statusText, fontSize = 9.sp, color = statusColor, maxLines = 1)
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = TextMuted)
    }
}

@Composable
private fun BestMatchCard(spot: Spot, avg: TestResult, testCount: Int, onClick: () -> Unit) {
    val grade = SpotRepository.gradeOf(avg)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFE3F1EE))
            .padding(20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Best recent match", color = Teal, fontWeight = FontWeight.Bold)
            GradeChip(grade)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            spot.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${fmtMbps(avg.downloadMbps)} Mbps · ${avg.pingMs} ms · ${avg.signalDbm} dBm",
            color = Teal,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Open · $testCount tests / 24 h · Updated ${timeAgo(avg.timestamp)}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
        Text(
            "Exam fit is provisional; check stability on arrival.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            text = "View approved spot",
            onClick = onClick,
            icon = Icons.AutoMirrored.Outlined.ArrowForward
        )
    }
}
