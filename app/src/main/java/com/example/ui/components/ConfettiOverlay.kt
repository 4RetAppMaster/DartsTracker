package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val startXFraction: Float,
    val startYFraction: Float,
    val velocityX: Float,
    val velocityY: Float,
    val gravity: Float,
    val widthDp: Float,
    val heightDp: Float,
    val color: Color,
    val initialRotation: Float,
    val rotationSpeed: Float,
    val flutterSpeed: Float,
    val flutterPhase: Float
)

/**
 * Celebratory confetti burst overlay rendered behind popup dialogs.
 */
@Composable
fun ConfettiPopOverlay(
    modifier: Modifier = Modifier,
    burstKey: Any = Unit
) {
    val confettiColors = remember {
        listOf(
            Color(0xFF4ADE80), // Emerald
            Color(0xFF3898EC), // Electric Blue
            Color(0xFFFFB74D), // Amber Gold
            Color(0xFFFF6B6B), // Coral Red
            Color(0xFFA78BFA), // Violet
            Color(0xFFFACC15), // Bright Yellow
            Color(0xFFF1F4F9)  // Crisp White
        )
    }

    val particles = remember(burstKey) {
        val rng = Random(System.nanoTime())
        List(90) { idx ->
            val angle = rng.nextDouble(-Math.PI * 0.92, -Math.PI * 0.08)
            val speed = rng.nextDouble(380.0, 1150.0)
            val vx = (cos(angle) * speed).toFloat()
            val vy = (sin(angle) * speed).toFloat()
            ConfettiParticle(
                startXFraction = 0.5f + (rng.nextFloat() - 0.5f) * 0.18f,
                startYFraction = 0.48f + (rng.nextFloat() - 0.5f) * 0.12f,
                velocityX = vx,
                velocityY = vy,
                gravity = 620f + rng.nextFloat() * 320f,
                widthDp = 6f + rng.nextFloat() * 6f,
                heightDp = 10f + rng.nextFloat() * 8f,
                color = confettiColors[idx % confettiColors.size],
                initialRotation = rng.nextFloat() * 360f,
                rotationSpeed = (rng.nextFloat() - 0.5f) * 420f,
                flutterSpeed = 4f + rng.nextFloat() * 6f,
                flutterPhase = rng.nextFloat() * (2f * PI.toFloat())
            )
        }
    }

    var elapsedSeconds by remember(burstKey) { mutableFloatStateOf(0f) }

    LaunchedEffect(burstKey) {
        val startNanos = withFrameNanos { it }
        while (true) {
            val nowNanos = withFrameNanos { it }
            val dt = (nowNanos - startNanos) / 1_000_000_000f
            elapsedSeconds = dt
            if (dt > 5.0f) break
        }
    }

    Canvas(modifier = modifier) {
        val t = elapsedSeconds
        if (t > 5.0f) return@Canvas

        val fadeAlpha = when {
            t < 3.5f -> 1f
            else -> ((5.0f - t) / 1.5f).coerceIn(0f, 1f)
        }

        val w = size.width
        val h = size.height

        particles.forEach { p ->
            val x = p.startXFraction * w + p.velocityX * t
            val y = p.startYFraction * h + p.velocityY * t + 0.5f * p.gravity * t * t

            if (y <= h + 40f && x >= -40f && x <= w + 40f) {
                val angle = p.initialRotation + p.rotationSpeed * t
                val flutterScale = cos(p.flutterPhase + p.flutterSpeed * t).coerceIn(-1f, 1f)
                val pw = p.widthDp.dp.toPx()
                val ph = p.heightDp.dp.toPx() * kotlin.math.abs(flutterScale).coerceAtLeast(0.22f)

                withTransform({
                    translate(left = x, top = y)
                    rotate(degrees = angle, pivot = Offset.Zero)
                }) {
                    drawRect(
                        color = p.color.copy(alpha = fadeAlpha * 0.92f),
                        topLeft = Offset(-pw / 2f, -ph / 2f),
                        size = Size(pw, ph)
                    )
                }
            }
        }
    }
}
