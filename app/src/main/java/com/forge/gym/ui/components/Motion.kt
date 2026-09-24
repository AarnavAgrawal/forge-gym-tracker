package com.forge.gym.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.forge.gym.ui.theme.Forge
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** Fades and lifts content in once, staggered by [index]. Does not replay on return. */
@Composable
fun Modifier.staggerIn(index: Int): Modifier {
    var played by rememberSaveable { mutableStateOf(false) }
    val anim = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!played) {
            delay(index.coerceAtMost(10) * 55L)
            anim.animateTo(1f, tween(600, easing = EmphasizedEasing))
            played = true
        }
    }
    return this.graphicsLayer {
        alpha = anim.value
        translationY = (1f - anim.value) * 36.dp.toPx()
    }
}

private data class Particle(val angle: Float, val speed: Float, val size: Float, val spin: Float, val color: Int)

@Composable
fun Confetti(modifier: Modifier = Modifier) {
    val palette = listOf(Forge.Lime, Forge.Coral, Forge.Blue, Forge.Amber, Forge.Violet, Forge.Teal)
    val particles = remember {
        List(56) {
            Particle(
                angle = Random.nextFloat() * 2f * Math.PI.toFloat(),
                speed = 0.35f + Random.nextFloat() * 0.75f,
                size = 5f + Random.nextFloat() * 7f,
                spin = Random.nextFloat() * 4f - 2f,
                color = Random.nextInt(palette.size),
            )
        }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(2200, easing = LinearOutSlowInEasing)) }
    Canvas(modifier) {
        if (t.value >= 1f) return@Canvas
        val origin = Offset(size.width / 2f, size.height * 0.28f)
        particles.forEach { p ->
            val d = p.speed * size.width * 0.62f * t.value
            val x = origin.x + cos(p.angle) * d
            val y = origin.y + sin(p.angle) * d + 520.dp.toPx() * t.value * t.value * 0.5f
            val s = p.size.dp.toPx()
            rotate(p.spin * 360f * t.value, Offset(x, y)) {
                drawRect(palette[p.color].copy(alpha = (1f - t.value).coerceIn(0f, 1f)), Offset(x - s / 2, y - s / 4), Size(s, s / 2))
            }
        }
    }
}
