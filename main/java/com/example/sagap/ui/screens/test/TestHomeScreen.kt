package com.example.sagap.ui.screens.test

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sagap.data.model.Spot
import com.example.sagap.data.model.TestResult
import com.example.sagap.domain.RatingEngine
import com.example.sagap.domain.timeAgo
import com.example.sagap.ui.components.InfoCard
import com.example.sagap.ui.components.gradeColors
import com.example.sagap.ui.components.gradeLabel
import com.example.sagap.ui.theme.*
import com.example.sagap.R

@Composable
fun TestHomeScreen(
    spot: Spot,
    latest: TestResult?,
    onChangeLocation: () -> Unit,
    onStartTest: () -> Unit,
    onFindSpot: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SagapHeader()

        Column {
            Text(
                "A better place to connect.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(Modifier.height(4.dp))
            Text("Check your campus Wi-Fi before you settle in.", color = TextMuted)
        }

        // Sample values for now; WifiMonitor will provide real ones later
        WifiStatusCard(ssid = "BatStateU CICS", details = "5 GHz · AP LIB-02 · Link speed 144 Mbps")

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Your current location",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            TextButton(onClick = onChangeLocation) { Text("Change", color = Teal) }
        }

        LocationCard(spot = spot, latest = latest, onClick = onChangeLocation)

        Button(
            onClick = onStartTest,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Navy)
        ) {
            Icon(Icons.Outlined.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start Wi-Fi test", fontWeight = FontWeight.SemiBold)
        }

        InfoCard(
            icon = Icons.Outlined.WifiOff,
            title = "Wi-Fi not connected?",
            body = "Connect to campus Wi-Fi first. Testing stays disabled on mobile data; it is only a fallback suggestion."
        )

        Text(
            "Plan your next session",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShortcutCard(Icons.Outlined.Map, "Find a good spot", "Compare recent Wi-Fi", onFindSpot, Modifier.weight(1f))
            ShortcutCard(Icons.Outlined.Shield, "Before an exam", "Monitor stability", {}, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SagapHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)                       // circle stays the same
                .background(Navy, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.sagap_logo),
                contentDescription = "Sagap logo",
                modifier = Modifier.size(35.dp),   // change only this to resize the logo
                contentScale = ContentScale.Fit
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("SAGAP", fontWeight = FontWeight.ExtraBold, color = Navy,
                style = MaterialTheme.typography.titleMedium)
            Text("ALANGILAN CAMPUS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
        Box(Modifier.size(20.dp).background(GradeGreen, CircleShape))
    }
}

@Composable
private fun WifiStatusCard(ssid: String, details: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Navy, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Wifi, contentDescription = null, tint = Color.White)
            Text(
                "connected",
                color = GradeGreen,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(GradeGreenBg, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(ssid, color = Color.White, style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        Text(details, color = Color.White.copy(alpha = 0.8f))
        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.2f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Shield, contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Wi-Fi only. Mobile data is never tested.",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun LocationCard(spot: Spot, latest: TestResult?, onClick: () -> Unit) {
    val grade = latest?.let { RatingEngine.grade(it.downloadMbps, it.pingMs, it.signalDbm) }
    val (fg, bg) = if (grade != null) gradeColors(grade) else GradeGray to GradeGrayBg
    val subtitle = if (latest != null && grade != null) {
        "${gradeLabel(grade)} · ${"%.1f".format(latest.downloadMbps)} Mbps · ${timeAgo(latest.timestamp)}"
    } else "No recent data"

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(32.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
            Text(spot.id.toString(), color = fg, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(spot.name, fontWeight = FontWeight.Bold, color = TextDark)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Teal)
    }
}

@Composable
private fun ShortcutCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Teal)
        Text(title, fontWeight = FontWeight.Bold, color = TextDark)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}