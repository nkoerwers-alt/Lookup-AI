
package com.lookupai

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FennAIThinking(
    modifier: Modifier = Modifier,
    cubeSize: Dp = 28.dp,
    name: String = "FennAI",
    message: String = " is thinking…"
) {
    val color = if (isSystemInDarkTheme()) Color.White else Color.Black

    val transition = rememberInfiniteTransition(label = "fenn_cube")

    val outerAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing)
        ),
        label = "outer_rotation"
    )

    val innerAngle by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing)
        ),
        label = "inner_rotation"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier.size(cubeSize)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.30f

            fun rotate(point: Offset, angle: Float): Offset {
                val radians = Math.toRadians(angle.toDouble())
                val x = point.x
                val y = point.y
                return Offset(
                    center.x + (x * cos(radians) - y * sin(radians)).toFloat(),
                    center.y + (x * sin(radians) + y * cos(radians)).toFloat()
                )
            }

            val vertices = listOf(
                Offset(-radius, -radius),
                Offset(radius, -radius),
                Offset(radius, radius),
                Offset(-radius, radius),
                Offset(-radius * 0.55f, -radius * 1.55f),
                Offset(radius * 1.45f, -radius * 1.55f),
                Offset(radius * 1.45f, radius * 0.45f),
                Offset(-radius * 0.55f, radius * 0.45f)
            )

            val rotated = vertices.map {
                rotate(it, outerAngle)
            }

            val edges = listOf(
                0 to 1, 1 to 2, 2 to 3, 3 to 0,
                4 to 5, 5 to 6, 6 to 7, 7 to 4,
                0 to 4, 1 to 5, 2 to 6, 3 to 7
            )

            edges.forEach { (a, b) ->
                drawLine(
                    color = color.copy(alpha = 0.65f),
                    start = rotated[a],
                    end = rotated[b],
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            val innerRadius = radius * 0.48f
            val innerVertices = listOf(
                Offset(-innerRadius, -innerRadius),
                Offset(innerRadius, -innerRadius),
                Offset(innerRadius, innerRadius),
                Offset(-innerRadius, innerRadius),
                Offset(-innerRadius * 0.5f, -innerRadius * 1.5f),
                Offset(innerRadius * 1.5f, -innerRadius * 1.5f),
                Offset(innerRadius * 1.5f, innerRadius * 0.5f),
                Offset(-innerRadius * 0.5f, innerRadius * 0.5f)
            ).map {
                rotate(it, innerAngle)
            }

            edges.forEach { (a, b) ->
                drawLine(
                    color = color,
                    start = innerVertices[a],
                    end = innerVertices[b],
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Text(
            text = name + message,
            color = color.copy(alpha = 0.75f),
            fontSize = 13.sp
        )
    }
}
