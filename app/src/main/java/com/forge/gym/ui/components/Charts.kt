package com.forge.gym.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.forge.gym.domain.weekStart
import com.forge.gym.ui.theme.Forge
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

data class ChartPoint(val x: Long, val y: Float)

/** Smooth animated line chart with drag-to-scrub. */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    yFormatter: (Float) -> String,
    xFormatter: (Long) -> String,
    modifier: Modifier = Modifier,
    color: Color = Forge.Lime,
    height: Dp = 180.dp,
    emptyText: String = "Log at least two sessions to see a trend",
) {
    if (points.size < 2) {
        Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
            Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = Forge.Text3, textAlign = TextAlign.Center)
        }
        return
    }
    val progress = remember(points) { Animatable(0f) }
    LaunchedEffect(points) { progress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }
    var selected by remember(points) { mutableStateOf<Int?>(null) }
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = Forge.Text3)
    val shown = points[selected ?: points.lastIndex]

    val minY = points.minOf { it.y }
    val maxY = points.maxOf { it.y }
    val range = (maxY - minY).takeIf { it > 0f } ?: (maxY.coerceAtLeast(1f) * 0.2f)
    val lo = (minY - range * 0.2f).coerceAtLeast(0f)
    val hi = maxY + range * 0.2f

    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(yFormatter(shown.y), style = MaterialTheme.typography.headlineMedium, color = Forge.Text1)
            Spacer(Modifier.width(10.dp))
            Text(xFormatter(shown.x), style = MaterialTheme.typography.labelMedium, color = Forge.Text2, modifier = Modifier.padding(bottom = 5.dp))
        }
        Spacer(Modifier.height(14.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(points) {
                    val leftPad = 44.dp.toPx()
                    fun idx(x: Float): Int {
                        val w = size.width - leftPad
                        return ((x - leftPad) / (w / (points.size - 1))).roundToInt().coerceIn(0, points.lastIndex)
                    }
                    detectTapGestures { selected = idx(it.x) }
                }
                .pointerInput(points) {
                    val leftPad = 44.dp.toPx()
                    fun idx(x: Float): Int {
                        val w = size.width - leftPad
                        return ((x - leftPad) / (w / (points.size - 1))).roundToInt().coerceIn(0, points.lastIndex)
                    }
                    detectHorizontalDragGestures { change, _ -> selected = idx(change.position.x) }
                },
        ) {
            val leftPad = 44.dp.toPx()
            val topPad = 10.dp.toPx()
            val bottomPad = 10.dp.toPx()
            val w = size.width - leftPad
            val h = size.height - topPad - bottomPad
            val n = points.size
            fun px(i: Int) = leftPad + w * i / (n - 1)
            fun py(v: Float) = topPad + h * (1f - (v - lo) / (hi - lo))

            for (k in 0..3) {
                val v = lo + (hi - lo) * k / 3f
                val y = py(v)
                drawLine(
                    Forge.Stroke, Offset(leftPad, y), Offset(size.width, y), 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                )
                val r = measurer.measure(yFormatter(v), labelStyle)
                drawText(r, topLeft = Offset(0f, y - r.size.height / 2f))
            }

            val line = Path().apply {
                moveTo(px(0), py(points[0].y))
                for (i in 1 until n) {
                    val x0 = px(i - 1); val y0 = py(points[i - 1].y)
                    val x1 = px(i); val y1 = py(points[i].y)
                    val mx = (x0 + x1) / 2
                    cubicTo(mx, y0, mx, y1, x1, y1)
                }
            }
            val fill = Path().apply {
                addPath(line)
                lineTo(px(n - 1), topPad + h)
                lineTo(px(0), topPad + h)
                close()
            }
            clipRect(right = leftPad + w * progress.value + 6.dp.toPx()) {
                drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0f)), startY = topPad, endY = topPad + h))
                drawPath(line, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            val sel = selected ?: if (progress.value >= 0.99f) n - 1 else null
            if (sel != null) {
                val c = Offset(px(sel), py(points[sel].y))
                if (selected != null) drawLine(Forge.Text3, Offset(c.x, topPad), Offset(c.x, topPad + h), 1.dp.toPx())
                drawCircle(color.copy(alpha = 0.22f), 11.dp.toPx(), c)
                drawCircle(color, 5.5.dp.toPx(), c)
                drawCircle(Forge.Bg, 2.5.dp.toPx(), c)
            }
        }
    }
}

/** Animated vertical bars; tap a bar to see its value. */
@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    valueFormatter: (Float) -> String,
    modifier: Modifier = Modifier,
    color: Color = Forge.Lime,
    height: Dp = 150.dp,
) {
    val max = values.maxOrNull()?.takeIf { it > 0f } ?: 1f
    var selected by remember(values) { mutableStateOf(values.lastIndex) }
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            if (selected in values.indices) {
                Text(valueFormatter(values[selected]), style = MaterialTheme.typography.headlineSmall, color = Forge.Text1)
                Spacer(Modifier.width(8.dp))
                Text(labels.getOrElse(selected) { "" }, style = MaterialTheme.typography.labelMedium, color = Forge.Text2, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().height(height),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            values.forEachIndexed { i, v ->
                val anim = remember(values) { Animatable(0f) }
                LaunchedEffect(values) {
                    delay(i * 45L)
                    anim.animateTo(v / max, spring(dampingRatio = 0.65f, stiffness = 110f))
                }
                val isSel = i == selected
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(remember { MutableInteractionSource() }, null) { selected = i },
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(anim.value.coerceIn(0.02f, 1f))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                            .background(
                                if (isSel) Brush.verticalGradient(listOf(color, color.copy(alpha = 0.7f)))
                                else Brush.verticalGradient(listOf(Forge.Surface3, Forge.Surface3.copy(alpha = 0.6f)))
                            ),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.forEachIndexed { i, l ->
                Text(
                    l,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == selected) Forge.Text1 else Forge.Text3,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** GitHub-style consistency grid: columns are weeks, rows are days (Mon at top). */
@Composable
fun ConsistencyHeatmap(counts: Map<LocalDate, Int>, modifier: Modifier = Modifier, weeks: Int = 18) {
    val today = LocalDate.now()
    val start = today.weekStart().minusWeeks((weeks - 1).toLong())
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val gap = 4.dp
        val cell = (maxWidth - gap * (weeks - 1)) / weeks
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            for (w in 0 until weeks) {
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    for (d in 0..6) {
                        val date = start.plusWeeks(w.toLong()).plusDays(d.toLong())
                        val c = counts[date] ?: 0
                        val visible = w.toFloat() / weeks <= appear.value
                        val color = when {
                            date.isAfter(today) -> Color.Transparent
                            c == 0 -> Forge.Surface3
                            c == 1 -> Forge.Lime.copy(alpha = 0.6f)
                            else -> Forge.Lime
                        }
                        Box(
                            Modifier
                                .size(cell)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (visible || date.isAfter(today)) color else Forge.Surface2)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Sparkline(values: List<Float>, modifier: Modifier = Modifier, color: Color = Forge.Lime) {
    if (values.size < 2) return
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    Canvas(modifier) {
        val min = values.min()
        val max = values.max()
        val r = (max - min).takeIf { it > 0f } ?: 1f
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = size.width * i / (values.size - 1)
            val y = size.height * (1f - (v - min) / r) * 0.9f + size.height * 0.05f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        clipRect(right = size.width * progress.value) {
            drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
