package com.example.sagap.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sagap.R
import com.example.sagap.ui.theme.GradeGreen
import com.example.sagap.ui.theme.Navy
import com.example.sagap.ui.theme.ScreenBg
import com.example.sagap.ui.theme.TextDark
import com.example.sagap.ui.theme.TextMuted

/** The header from the Test screen. SagapApp shows it once, so every tab has the same one. */
@Composable
fun SagapTopBar(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(ScreenBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
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
            Text(
                "SAGAP",
                fontWeight = FontWeight.ExtraBold,
                color = Navy,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "ALANGILAN CAMPUS",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
        Box(Modifier.size(20.dp).background(GradeGreen, CircleShape))
    }
}

@Composable
fun ScreenTitle(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = TextDark)
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = TextMuted)
        }
    }
}