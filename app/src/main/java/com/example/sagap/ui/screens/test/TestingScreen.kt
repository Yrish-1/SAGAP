package com.example.sagap.ui.screens.test

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sagap.data.model.Spot
import com.example.sagap.data.sample.SampleData
import com.example.sagap.domain.fmtMbps
import com.example.sagap.ui.TestPhase
import com.example.sagap.ui.TestUiState
import com.example.sagap.ui.components.InfoCard
import com.example.sagap.ui.components.SecondaryButton
import com.example.sagap.ui.components.StatusPill
import com.example.sagap.ui.components.WhiteCard
import com.example.sagap.ui.components.ScreenTitle
import com.example.sagap.ui.theme.*

@Composable
fun TestingScreen(
    spot: Spot,
    state: TestUiState,
    onCancel: () -> Unit,
    onFinished: () -> Unit
) {
    // Keep the screen awake while the test runs
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    BackHandler { onCancel() }
    LaunchedEffect(state.phase) {
        if (state.phase == TestPhase.DONE) onFinished()
    }

    val progress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(150),
        label = "progress"
    )
    val tealSoft = Color(0xFFE3F1EE)

    Column(

        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ScreenTitle("Testing your Wi-Fi", spot.name)
                Text(
                    "${SampleData.WIFI_NAME} · 5 GHz",
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }

            StatusPill("Wi-Fi", Teal, tealSoft)
        }

        // Progress ring with the live speed in the middle
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 14.dp.toPx()
                    val topLeft = Offset(stroke / 2, stroke / 2)
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = Color(0xFFDDE5EA),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke)
                    )
                    drawArc(
                        color = Teal,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.ArrowDownward, contentDescription = null, tint = Teal)
                    Text(
                        fmtMbps(state.downloadMbps),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text("Mbps download", color = TextMuted)
                    Spacer(Modifier.height(6.dp))
                    StatusPill(
                        if (state.progress < 0.15f) "Starting…" else "Measuring…",
                        Teal,
                        tealSoft
                    )
                }
            }
        }

        Text(
            "${(state.progress * 100).toInt()}% · About ${state.secondsLeft} seconds left",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Teal,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Keep the app open and pause large downloads.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = TextMuted
        )

        WhiteCard {
            val pingDone = state.pingMs != null
            StatusRow(
                icon = {
                    if (pingDone) Icon(Icons.Outlined.CheckCircle, null, tint = Teal)
                    else Spinner()
                },
                title = "Ping",
                status = if (pingDone) "Complete" else "Waiting",
                value = if (pingDone) "${state.pingMs} ms" else "—",
                valueColor = Teal
            )
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Color(0xFFE6ECF0))
            StatusRow(
                icon = { Spinner() },
                title = "Download",
                status = "Measuring",
                value = "${fmtMbps(state.downloadMbps)} Mbps",
                valueColor = Teal
            )
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Color(0xFFE6ECF0))
            StatusRow(
                icon = { Icon(Icons.Outlined.Wifi, null, tint = TextMuted) },
                title = "Signal",
                status = "Sampling",
                value = if (state.signalDbm != null) "${state.signalDbm} dBm" else "—",
                valueColor = TextMuted
            )
        }

        InfoCard(
            icon = Icons.Outlined.Shield,
            title = "Wi-Fi guard is on",
            body = "If Wi-Fi disconnects, this test stops. SAGAP will not switch the measurement to mobile data.",
            container = tealSoft,
            content = Teal
        )

        SecondaryButton("Cancel test", onClick = onCancel, accent = GradeRed)

        Text(
            "Sample test in progress · Readings are provisional until the test finishes.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

@Composable
private fun Spinner() {
    CircularProgressIndicator(
        modifier = Modifier.size(22.dp),
        strokeWidth = 2.5.dp,
        color = Teal
    )
}

@Composable
private fun StatusRow(
    icon: @Composable () -> Unit,
    title: String,
    status: String,
    value: String,
    valueColor: Color
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = TextDark)
            Text(status, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Text(value, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
