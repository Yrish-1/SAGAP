package com.example.sagap.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sagap.data.model.Spot
import com.example.sagap.data.model.TestResult
import com.example.sagap.data.sample.FloorData
import com.example.sagap.data.repository.SpotRepository
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
        ScreenTitle("Campus Buildings", "Select a building to view floor maps and test Wi-Fi")

        Text(
            "Select an approved campus building below:",
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
