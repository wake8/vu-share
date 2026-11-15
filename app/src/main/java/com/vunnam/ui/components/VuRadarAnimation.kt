package com.vunnam.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vunnam.ui.theme.VuShareTheme

@Composable
fun VuRadarAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    centerContent: @Composable () -> Unit = {}
) {
    val colors = VuShareTheme.colors
    val primaryRing = colors.primaryAccent
    val secondaryRing = colors.secondaryHue
    val ringAlphaBase = if (colors.isLight) 0.24f else 0.14f

    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )

    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )

    val pulse3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse3"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxRadius = this.size.width / 2f

            val rings = 4
            for (i in 1..rings) {
                drawCircle(
                    color = primaryRing.copy(alpha = ringAlphaBase),
                    radius = (maxRadius / rings) * i,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            drawLine(
                color = primaryRing.copy(alpha = ringAlphaBase),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = primaryRing.copy(alpha = ringAlphaBase),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1.dp.toPx()
            )

            val pulses = listOf(pulse1, pulse2, pulse3)
            for (p in pulses) {
                val alpha = (1f - p).coerceIn(0f, 1f) * if (colors.isLight) 0.55f else 0.45f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryRing.copy(alpha = alpha * 0.35f),
                            secondaryRing.copy(alpha = alpha)
                        ),
                        center = center,
                        radius = maxRadius * p
                    ),
                    radius = maxRadius * p,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        centerContent()
    }
}
