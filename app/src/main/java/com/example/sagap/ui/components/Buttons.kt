package com.example.sagap.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sagap.ui.theme.GradeGray
import com.example.sagap.ui.theme.GradeGrayBg
import com.example.sagap.ui.theme.Teal

/**
 * Filled button. While pressed it turns white with a thick outline in [accent],
 * and the text and icon take the [accent] color.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    color: Color = Teal,
    accent: Color = color
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    val isDown = pressed && enabled

    val scale by animateFloatAsState(
        targetValue = if (isDown) 0.96f else 1f,
        animationSpec = tween(90),
        label = "buttonScale"
    )
    val container by animateColorAsState(
        targetValue = if (isDown) Color.White else color,
        animationSpec = tween(90),
        label = "buttonContainer"
    )
    val content by animateColorAsState(
        targetValue = if (isDown) accent else Color.White,
        animationSpec = tween(90),
        label = "buttonContent"
    )

    Button(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled,
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(28.dp),
        border = if (isDown) BorderStroke(3.dp, accent) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = GradeGrayBg,
            disabledContentColor = GradeGray
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * White outlined button. The text and icon use [accent]. While pressed, the outline
 * becomes thick and takes the [accent] color (use GradeRed for Cancel).
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = Teal
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(90),
        label = "buttonScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (pressed) accent else Color(0xFFD5DEE3),
        animationSpec = tween(90),
        label = "buttonBorder"
    )

    OutlinedButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(if (pressed) 3.dp else 1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = accent)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

/** Small text button (like "Change" or "OK") with the same outline-on-press effect. */
@Composable
fun LinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Teal
) {
    Text(
        text,
        color = accent,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .pressable(RoundedCornerShape(50), accent = accent, borderWidth = 2.dp, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}