package com.example.sagap.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val SagapColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    secondary = Navy,
    onSecondary = Color.White,
    background = ScreenBg,
    onBackground = TextDark,
    surface = Color.White,
    onSurface = TextDark
)

@Composable
fun SAGAPTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SagapColors,
        typography = Typography,   // from Type.kt, already in your project
        content = content
    )
}