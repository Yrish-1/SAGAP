package com.example.sagap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sagap.ui.theme.TextMuted

@Composable
fun InfoCard(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    container: Color = Color(0xFFE9EEF1),
    content: Color = TextMuted
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = content)
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, color = content)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = content)
        }
    }
}