package com.example.sagap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sagap.domain.fmtMbps
import com.example.sagap.ui.theme.TextDark
import com.example.sagap.ui.theme.TextMuted

/** The Download / Ping / Wi-Fi signal boxes. */
@Composable
fun MetricTiles(download: Double, ping: Int, signal: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Tile("Download", fmtMbps(download), "Mbps", Modifier.weight(1f))
        Tile("Ping", "$ping", "ms", Modifier.weight(1f))
        Tile("Wi-Fi signal", "$signal", "dBm", Modifier.weight(1f))
    }
}

@Composable
private fun Tile(label: String, value: String, unit: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF1F5F8))
            .padding(12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Text(unit, style = MaterialTheme.typography.bodySmall, color = TextMuted)
    }
}
