package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val startXFraction: Float,
    val startYFraction: Float,
    val velocityX: Float,
    val velocityY: Float,
    val gravity: Float,
    val widthPx: Float,
    val heightPx: Float,
    val color: Color,
    val initialRotation: Float,
    val rotationSpeed: Float,
    val flutterSpeed: Float,
    val flutterPhase: Float
)

private val CONFETTI_COLORS = listOf(
    Color(0xFF4ADE80), // Emerald
    Color(0xFF3898EC), // Electric Blue
    Color(0xFFFFB74D), // Amber Gold
    Color(0xFFFF6B6B), // Coral Red
    Color(0xFFA78BFA), // Violet
    Color(0xFFFACC15), // Bright Yellow
    Color(0xFFF1F4F9)  // Crisp White
)

private const val CONFETTI_DURATION_SECONDS = 5.0f
private const val CONFETTI_FADE_START_SECONDS = 3.5f

/**
 * Celebratory confetti burst overlay rendered behind popup dialogs.
 */
@Composable
fun ConfettiPopOverlay(
    modifier: Modifier = Modifier,
    burstKey: Any = Unit
) {
    val density = LocalDensity.current.density

    val particles = remember(burstKey, density) {
        val rng = Random(System.nanoTime())
        List(80) { idx ->
            val angle = rng.nextDouble(-Math.PI * 0.92, -Math.PI * 0.08)
            val speed = rng.nextDouble(380.0, 1150.0)
            ConfettiParticle(
                startXFraction = 0.5f + (rng.nextFloat() - 0.5f) * 0.18f,
                startYFraction = 0.48f + (rng.nextFloat() - 0.5f) * 0.12f,
                velocityX = (cos(angle) * speed).toFloat(),
                velocityY = (sin(angle) * speed).toFloat(),
                gravity = 620f + rng.nextFloat() * 320f,
                widthPx = (6f + rng.nextFloat() * 6f) * density,
                heightPx = (10f + rng.nextFloat() * 8f) * density,
                color = CONFETTI_COLORS[idx % CONFETTI_COLORS.size],
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
            if (dt > CONFETTI_DURATION_SECONDS) break
        }
    }

    if (elapsedSeconds > CONFETTI_DURATION_SECONDS) return

    Canvas(modifier = modifier) {
        val t = elapsedSeconds
        val fadeAlpha = if (t < CONFETTI_FADE_START_SECONDS) {
            0.92f
        } else {
            (((CONFETTI_DURATION_SECONDS - t) / (CONFETTI_DURATION_SECONDS - CONFETTI_FADE_START_SECONDS))
                .coerceIn(0f, 1f)) * 0.92f
        }
        if (fadeAlpha <= 0f) return@Canvas

        val w = size.width
        val h = size.height
        val halfGravityTSq = 0.5f * t * t

        for (i in particles.indices) {
            val p = particles[i]
            val x = p.startXFraction * w + p.velocityX * t
            val y = p.startYFraction * h + p.velocityY * t + p.gravity * halfGravityTSq

            if (y <= h + 40f && x >= -40f && x <= w + 40f) {
                val angle = p.initialRotation + p.rotationSpeed * t
                val flutterScale = abs(cos(p.flutterPhase + p.flutterSpeed * t)).coerceIn(0.22f, 1f)
                val pw = p.widthPx
                val ph = p.heightPx * flutterScale

                withTransform({
                    translate(left = x, top = y)
                    rotate(degrees = angle, pivot = Offset.Zero)
                }) {
                    drawRect(
                        color = p.color,
                        alpha = fadeAlpha,
                        topLeft = Offset(-pw * 0.5f, -ph * 0.5f),
                        size = Size(pw, ph)
                    )
                }
            }
        }
    }
}
