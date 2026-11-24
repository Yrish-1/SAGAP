package com.example.sagap.ui.screens.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sagap.data.model.Area
import com.example.sagap.data.model.Grade
import com.example.sagap.data.model.Reading
import com.example.sagap.data.model.Spot
import com.example.sagap.data.sample.FloorData
import com.example.sagap.domain.fmtMbps
import com.example.sagap.ui.components.GradeChip
import com.example.sagap.ui.components.InfoCard
import com.example.sagap.ui.components.MetricTiles
import com.example.sagap.ui.components.ScreenTitle
import com.example.sagap.ui.components.SectionTitle
import com.example.sagap.ui.components.WhiteCard
import com.example.sagap.ui.components.gradeColors
import com.example.sagap.ui.components.pressable
import com.example.sagap.ui.theme.*

@Composable
fun FloorMapScreen(spot: Spot, onBack: () -> Unit) {
    val floors = remember(spot.id) { FloorData.floorsFor(spot.id) }
    var level by remember(spot.id) { mutableStateOf(floors.firstOrNull()?.level ?: 1) }
    var areaId by remember(spot.id) { mutableStateOf<Int?>(null) }
    val floor = floors.firstOrNull { it.level == level }

    // Zoom & pan state for floor plan images
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenTitle("${spot.name} floors", "Wi-Fi quality on each floor", onBack = onBack)

        if (floor == null) {
            InfoCard(
                icon = Icons.Outlined.Info,
                title = "No floor map yet",
                body = "Floor maps are not available for this building yet."
            )
        } else {
            // Floor tabs
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                floors.forEach { f ->
                    FloorChip(
                        label = f.label,
                        grade = FloorData.gradeOf(FloorData.averageOf(f)),
                        selected = f.level == level,
                        onClick = {
                            level = f.level
                            areaId = null
                            scale = 1f
                            offset = Offset.Zero
                        }
                    )
                }
            }

            // Interactive Zoom Controls Bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFDDE5EA), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Zoom: ${(scale * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { scale = (scale - 0.25f).coerceAtLeast(1f); if (scale == 1f) offset = Offset.Zero }) {
                        Icon(Icons.Outlined.Remove, contentDescription = "Zoom Out", tint = Navy)
                    }
                    IconButton(onClick = { scale = (scale + 0.25f).coerceAtMost(4f) }) {
                        Icon(Icons.Outlined.Add, contentDescription = "Zoom In", tint = Navy)
                    }
                    IconButton(onClick = { scale = 1f; offset = Offset.Zero }) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = "Reset Zoom", tint = Teal)
                    }
                }
            }

            // Zoomable & Pannable Floor Plan Image View Container
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFDDE5EA), RoundedCornerShape(24.dp))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            if (scale > 1f) {
                                offset += pan
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
            ) {
                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                ) {
                    val w = maxWidth
                    val h = maxHeight

                    // Render actual uploaded floor plan image
                    if (floor.imageRes != null) {
                        Image(
                            painter = painterResource(id = floor.imageRes),
                            contentDescription = "${spot.name} ${floor.label} Floor Plan",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Text(
                        "${floor.label.uppercase()} · BLUEPRINT",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 12.dp, top = 8.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )

                    floor.areas.forEach { a ->
                        AreaBlock(
                            area = a,
                            reading = FloorData.readings[a.id],
                            selected = a.id == areaId,
                            modifier = Modifier
                                .offset(w * (a.mapX - a.w / 2), h * (a.mapY - a.h / 2))
                                .size(w * a.w, h * a.h),
                            onClick = { areaId = a.id }
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LegendItem(GradeGreen, "Good / Excellent")
                LegendItem(GradeAmber, "Fair")
                LegendItem(GradeRed, "Poor")
                LegendItem(GradeGray, "No data")
            }

            val selectedArea = floor.areas.firstOrNull { it.id == areaId }
            val shown: Reading? =
                if (selectedArea != null) FloorData.readings[selectedArea.id] else FloorData.averageOf(floor)
            val title = selectedArea?.name ?: "${floor.label} average"

            WhiteCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontWeight = FontWeight.Bold, color = TextDark)
                    GradeChip(FloorData.gradeOf(shown))
                }
                Spacer(Modifier.height(12.dp))
                if (shown != null) {
                    MetricTiles(shown.downloadMbps, shown.pingMs, shown.signalDbm)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap any room on the floor blueprint to view Wi-Fi metrics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                } else {
                    Text("No recent tests in this room yet.", color = TextMuted)
                }
            }

            SectionTitle("All floors")
            WhiteCard {
                val ordered = floors.sortedByDescending { it.level }
                ordered.forEachIndexed { index, f ->
                    FloorRow(
                        label = f.label,
                        avg = FloorData.averageOf(f),
                        selected = f.level == level,
                        onClick = {
                            level = f.level
                            areaId = null
                            scale = 1f
                            offset = Offset.Zero
                        }
                    )
                    if (index < ordered.lastIndex) {
                        HorizontalDivider(color = Color(0xFFE6ECF0))
                    }
                }
            }
        }
    }
}

@Composable
private fun AreaBlock(
    area: Area,
    reading: Reading?,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val (fg, bg) = gradeColors(FloorData.gradeOf(reading))
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .pressable(shape, onClick = onClick)
            .background(bg.copy(alpha = 0.85f))
            .border(if (selected) 2.5.dp else 1.dp, if (selected) Navy else fg, shape)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            area.name,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = TextDark
        )
        Text(
            if (reading != null) "${fmtMbps(reading.downloadMbps)} Mbps" else "No data",
            fontSize = 8.sp,
            maxLines = 1,
            color = fg
        )
    }
}

@Composable
private fun FloorChip(label: String, grade: Grade, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .pressable(shape, onClick = onClick)
            .background(if (selected) Navy else Color.White)
            .border(1.dp, if (selected) Navy else Color(0xFFD5DEE3), shape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).background(gradeColors(grade).first, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(label, color = if (selected) Color.White else TextDark, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FloorRow(label: String, avg: Reading?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(RoundedCornerShape(12.dp), onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = TextDark
            )
            Text(
                if (avg != null) "Average ${fmtMbps(avg.downloadMbps)} Mbps · ${avg.pingMs} ms" else "No recent data",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        GradeChip(FloorData.gradeOf(avg))
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
