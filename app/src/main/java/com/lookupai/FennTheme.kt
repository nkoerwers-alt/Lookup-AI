package com.lookupai

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Fenn AI brand colors. Change a color here and it changes everywhere.
 */
object FennColors {
    val Background = Color(0xFF0B0B0C)
    val Surface = Color(0xFF141415)
    val SurfaceHigh = Color(0xFF1D1D1F)
    val Border = Color(0xFF2A2A2D)

    val Ink = Color(0xFFF3EFEA)
    val Muted = Color(0xFF8E8A85)

    val Ember = Color(0xFFFF7A2F)
    val Amber = Color(0xFFFFB27A)
    val OnAccent = Color(0xFF1A0E05)

    val Danger = Color(0xFFE57373)
}

/**
 * The Fenn logo: a light rounded square with "f" and an ember-orange dot.
 */
@Composable
fun FennLogo(
    logoSize: Dp,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(logoSize)
            .background(FennColors.Ink, RoundedCornerShape(logoSize * 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Color.Black)) { append("f") }
                withStyle(SpanStyle(color = FennColors.Ember)) { append(".") }
            },
            fontSize = fontSize,
            fontWeight = FontWeight.Black
        )
    }
}

/**
 * A microphone icon drawn in code, so no extra icon library is needed.
 */
@Composable
fun MicIcon(
    color: Color,
    iconSize: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.085f

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.36f, h * 0.08f),
            size = Size(w * 0.28f, h * 0.50f),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
        )

        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.22f, h * 0.30f),
            size = Size(w * 0.56f, h * 0.46f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        drawLine(
            color = color,
            start = Offset(w * 0.50f, h * 0.76f),
            end = Offset(w * 0.50f, h * 0.92f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        drawLine(
            color = color,
            start = Offset(w * 0.36f, h * 0.92f),
            end = Offset(w * 0.64f, h * 0.92f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}
