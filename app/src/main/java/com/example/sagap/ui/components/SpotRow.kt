package com.example.sagap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sagap.data.model.Grade
import com.example.sagap.data.model.Spot
import com.example.sagap.data.model.TestResult
import com.example.sagap.domain.RatingEngine
import com.example.sagap.domain.fmtMbps
import com.example.sagap.domain.timeAgo
import com.example.sagap.ui.theme.*

/** One row in the "Choose your location" list. */
@Composable
fun SpotRow(
    spot: Spot,
    latest: TestResult?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val grade = if (latest != null) {
        RatingEngine.grade(latest.downloadMbps, latest.pingMs, latest.signalDbm)
    } else Grade.NO_DATA
    val (fg, bg) = if (spot.onHold) GradeAmber to GradeAmberBg else gradeColors(grade)
    val subtitle = when {
        spot.onHold -> "Safety hold • unavailable"
        latest != null -> "${gradeLabel(grade)} · ${fmtMbps(latest.downloadMbps)} Mbps · ${timeAgo(latest.timestamp)}"
        else -> "No recent data • open"
    }
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Color(0xFFE3F1EE) else Color.White)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Teal else Color(0xFFDDE5EA),
                shape = shape
            )
            .clickable(enabled = !spot.onHold, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(34.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
            Text(spot.id.toString(), color = fg, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(spot.name, fontWeight = FontWeight.Bold, color = TextDark)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
        when {
            spot.onHold -> Icon(Icons.Outlined.Lock, contentDescription = "Unavailable", tint = TextMuted)
            selected -> Icon(Icons.Outlined.CheckCircle, contentDescription = "Selected", tint = Teal)
            else -> Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextMuted)
        }
    }
}
