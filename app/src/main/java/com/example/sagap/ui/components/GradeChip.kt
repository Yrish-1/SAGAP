package com.example.sagap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sagap.data.model.Grade
import com.example.sagap.ui.theme.*

// Returns (text color, background color) for a grade
fun gradeColors(grade: Grade): Pair<Color, Color> = when (grade) {
    Grade.EXCELLENT, Grade.GOOD -> GradeGreen to GradeGreenBg
    Grade.FAIR -> GradeAmber to GradeAmberBg
    Grade.POOR -> GradeRed to GradeRedBg
    Grade.NO_DATA -> GradeGray to GradeGrayBg
}

fun gradeLabel(grade: Grade): String = when (grade) {
    Grade.EXCELLENT -> "Excellent"
    Grade.GOOD -> "Good"
    Grade.FAIR -> "Fair"
    Grade.POOR -> "Poor"
    Grade.NO_DATA -> "No data"
}

@Composable
fun GradeChip(grade: Grade, modifier: Modifier = Modifier) {
    val (fg, bg) = gradeColors(grade)
    Text(
        text = gradeLabel(grade),
        color = fg,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}