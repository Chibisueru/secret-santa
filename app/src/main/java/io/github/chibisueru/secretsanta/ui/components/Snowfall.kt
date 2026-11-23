package io.github.chibisueru.secretsanta.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private class Flake(
    val x: Float,
    val y: Float,
    val radiusDp: Float,
    /** Fraction of the height fallen per second. */
    val speed: Float,
    val phase: Float,
    val alpha: Float,
)

/** Gentle falling snow. Time is only read while drawing, so it never recomposes. */
@Composable
fun Snowfall(
    color: Color,
    modifier: Modifier = Modifier,
    flakeCount: Int = 45,
) {
    val flakes = remember(flakeCount) {
        List(flakeCount) {
            Flake(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radiusDp = 1.5f + Random.nextFloat() * 3f,
                speed = 0.025f + Random.nextFloat() * 0.04f,
                phase = Random.nextFloat() * 2f * PI.toFloat(),
                alpha = 0.35f + Random.nextFloat() * 0.65f,
            )
        }
    }
    var seconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> seconds = (now - start) / 1_000_000_000f }
        }
    }

    Canvas(modifier) {
        val sway = 14.dp.toPx()
        flakes.forEach { f ->
            val y = ((f.y + seconds * f.speed) % 1f) * (size.height + 20f) - 10f
            val x = f.x * size.width + sin(seconds * 0.7f + f.phase) * sway
            drawCircle(
                color = color.copy(alpha = color.alpha * f.alpha),
                radius = f.radiusDp.dp.toPx(),
                center = Offset(x, y),
            )
        }
    }
}
