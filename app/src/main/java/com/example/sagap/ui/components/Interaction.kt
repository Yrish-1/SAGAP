package com.example.sagap.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sagap.ui.theme.Teal

/**
 * Press effect for cards and rows: shrinks slightly, shows a thick colored outline,
 * ripple, and a light vibration. Put it BEFORE .background() / .border() / .padding().
 */
fun Modifier.pressable(
    shape: Shape,
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    accent: Color = Teal,
    borderWidth: Dp = 3.dp,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    val isDown = pressed && enabled
    val scale by animateFloatAsState(
        targetValue = if (isDown) pressedScale else 1f,
        animationSpec = tween(90),
        label = "pressScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(shape)
        .clickable(
            interactionSource = interaction,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        )
        .then(if (isDown) Modifier.border(borderWidth, accent, shape) else Modifier)
}