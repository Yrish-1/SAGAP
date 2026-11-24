package com.example.sagap.ui.screens.map

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sagap.R
import com.example.sagap.data.model.Spot
import com.example.sagap.data.model.TestResult
import com.example.sagap.data.repository.SpotRepository
import com.example.sagap.data.sample.FloorData
import com.example.sagap.domain.fmtMbps
import com.example.sagap.ui.components.GradeChip
import com.example.sagap.ui.components.PrimaryButton
import com.example.sagap.ui.components.ScreenTitle
import com.example.sagap.ui.components.pressable
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
        ScreenTitle("Campus Map", "Tap a building on the campus layout map below")

        // Interactive Campus Map image with clickable building overlays
        InteractiveCampusMap(
            spots = spots,
            results = results,
            onSpotClick = onSpotClick
        )

        Text(
            "Select a building to view its floor plans & test Wi-Fi:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        spots.forEach { spot ->
            val floorCount = FloorData.floorsFor(spot.id).size
            val icon = when (spot.id) {
                3 -> Icons.Outlined.FitnessCenter
                else -> Icons.Outlined.Apartment
            }
            BuildingButtonCard(
                spot = spot,
                floorCount = floorCount,
                icon = icon,
                avg = SpotRepository.average(results, spot.id),
                onClick = { onSpotClick(spot) }
            )
        }

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
private fun InteractiveCampusMap(
    spots: List<Spot>,
    results: List<TestResult>,
    onSpotClick: (Spot) -> Unit
) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFDDE5EA), RoundedCornerShape(24.dp))
    ) {
        val w = maxWidth
        val h = maxHeight

        // Render actual Campus Layout Image
        Image(
            painter = painterResource(id = R.drawable.campus_map),
            contentDescription = "Alangilan Campus Map",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Overlay 1: Sparta Gymnasium (Top-left building)
        val gymSpot = spots.firstOrNull { it.id == 3 } ?: spots.getOrNull(2)
        if (gymSpot != null) {
            BuildingHotspotBadge(
                spot = gymSpot,
                label = "Sparta Gym (1F)",
                modifier = Modifier
                    .offset(w * 0.08f, h * 0.22f)
                    .width(w * 0.38f),
                onClick = { onSpotClick(gymSpot) }
            )
        }

        // Overlay 2: CICS Building (Bottom-left building)
        val cicsSpot = spots.firstOrNull { it.id == 2 } ?: spots.getOrNull(1)
        if (cicsSpot != null) {
            BuildingHotspotBadge(
                spot = cicsSpot,
                label = "CICS (5 Floors)",
                modifier = Modifier
                    .offset(w * 0.18f, h * 0.65f)
                    .width(w * 0.32f),
                onClick = { onSpotClick(cicsSpot) }
            )
        }

        // Overlay 3: Albert Einstein Building (Right building)
        val einsteinSpot = spots.firstOrNull { it.id == 1 } ?: spots.getOrNull(0)
        if (einsteinSpot != null) {
            BuildingHotspotBadge(
                spot = einsteinSpot,
                label = "Einstein (2 Floors)",
                modifier = Modifier
                    .offset(w * 0.58f, h * 0.32f)
                    .width(w * 0.38f),
                onClick = { onSpotClick(einsteinSpot) }
            )
        }
    }
}

@Composable
private fun BuildingHotspotBadge(
    spot: Spot,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .clip(shape)
            .background(Navy.copy(alpha = 0.92f))
            .border(1.5.dp, Teal, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(18.dp)
                .background(Teal, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(spot.id.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun BuildingButtonCard(
    spot: Spot,
    floorCount: Int,
    icon: ImageVector,
    avg: TestResult?,
    onClick: () -> Unit
) {
    val grade = SpotRepository.gradeOf(avg)
    val shape = RoundedCornerShape(20.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.5.dp, Color(0xFFDDE5EA), shape)
            .pressable(shape, onClick = onClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE3F1EE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Teal)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        spot.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Layers,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "$floorCount ${if (floorCount == 1) "Floor" else "Floors"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
            GradeChip(grade)
        }

        if (avg != null) {
            Text(
                "Average ${fmtMbps(avg.downloadMbps)} Mbps · ${avg.pingMs} ms ping",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Teal
            )
        } else {
            Text(
                "No recent test data recorded yet",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        PrimaryButton(
            text = "View ${if (floorCount == 1) "Floor Map" else "$floorCount Floors"} & Test",
            onClick = onClick,
            icon = Icons.AutoMirrored.Outlined.ArrowForward,
            color = Navy,
            accent = Teal
        )
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
            "Open · $testCount tests / 24 h",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            text = "View building details",
            onClick = onClick,
            icon = Icons.AutoMirrored.Outlined.ArrowForward
        )
    }
}
