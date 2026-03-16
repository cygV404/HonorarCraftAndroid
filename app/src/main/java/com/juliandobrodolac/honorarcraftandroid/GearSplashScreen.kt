package com.juliandobrodolac.honorarcraftandroid

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.juliandobrodolac.honorarcraftandroid.ui.theme.HonorarCraftAndroidTheme

@Composable
fun GearSplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        GearAnimation()
    }
}

@Composable
fun GearAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "gearRotation")
    val textMeasurer = rememberTextMeasurer()
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val backgroundColor = MaterialTheme.colorScheme.background

    // Rotation des äußeren Zahnrads
    val rotationOuter by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationOuter"
    )

    // Rotation des inneren Zahnrads
    val rotationInner by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationInner"
    )

    // Pulsierender Scale-Effekt für mehr Dynamik
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Sanftes Pulsieren der Deckkraft des Textes
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Canvas(modifier = Modifier.size(900.dp)) {
        val center = Offset(size.width / 2, size.height / 2)

        scale(pulseScale, center) {
            // Äußeres Zahnrad
            clipRect(right = center.x) {
                rotate(rotationOuter, center) {
                    drawGear(
                        center = center,
                        radius = 300f,
                        innerRadius = 213f,
                        toothCount = 12,
                        toothWidth = 99f,
                        toothHeight = 126f,
                        cornerRadius = 21f,
                        color = onBackgroundColor
                    )
                }
            }
            clipRect(left = center.x) {
                rotate(rotationOuter, center) {
                    drawGear(
                        center = center,
                        radius = 300f,
                        innerRadius = 213f,
                        toothCount = 12,
                        toothWidth = 99f,
                        toothHeight = 126f,
                        cornerRadius = 21f,
                        color = backgroundColor
                    )
                }
            }

            // Inneres Zahnrad
            val innerGearRadius = 160f
            val innerGearInnerRadius = 110f

            clipRect(right = center.x) {
                rotate(rotationInner, center) {
                    drawGear(
                        center = center,
                        radius = innerGearRadius,
                        innerRadius = innerGearInnerRadius,
                        toothCount = 12,
                        toothWidth = 45f,
                        toothHeight = 57f,
                        cornerRadius = 9f,
                        color = backgroundColor
                    )
                }
            }
            clipRect(left = center.x) {
                rotate(rotationInner, center) {
                    drawGear(
                        center = center,
                        radius = innerGearRadius,
                        innerRadius = innerGearInnerRadius,
                        toothCount = 12,
                        toothWidth = 45f,
                        toothHeight = 57f,
                        cornerRadius = 9f,
                        color = onBackgroundColor
                    )
                }
            }
        }

        // "HC" Text in der Mitte
        val textStyle = TextStyle(
            color = onBackgroundColor.copy(alpha = alpha),
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Serif,
            shadow = Shadow(
                color = onBackgroundColor.copy(alpha = 0.3f),
                offset = Offset(4f, 4f),
                blurRadius = 8f
            )
        )

        val textLayoutResult = textMeasurer.measure(
            text = "HC",
            style = textStyle
        )

        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(
                center.x - textLayoutResult.size.width / 2f,
                center.y - textLayoutResult.size.height / 2f
            )
        )
    }
}

private fun DrawScope.drawGear(
    center: Offset,
    radius: Float,
    innerRadius: Float,
    toothCount: Int,
    toothWidth: Float,
    toothHeight: Float,
    cornerRadius: Float,
    color: Color
) {
    drawCircle(
        color = color,
        radius = (radius + innerRadius) / 2,
        center = center,
        style = Stroke(width = radius - innerRadius)
    )

    val angleStep = 360f / toothCount
    for (i in 0 until toothCount) {
        rotate(angleStep * i, center) {
            drawRoundRect(
                color = color,
                topLeft = Offset(center.x - toothWidth / 2, center.y - radius - toothHeight / 3),
                size = Size(toothWidth, toothHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GearSplashScreenPreview() {
    HonorarCraftAndroidTheme {
        GearSplashScreen()
    }
}
