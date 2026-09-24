package com.forge.gym.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.forge.gym.data.WorkoutFull
import com.forge.gym.domain.date
import com.forge.gym.domain.durationMs
import com.forge.gym.domain.formatBig
import com.forge.gym.domain.volumeKg
import com.forge.gym.ui.LocalContainer
import com.forge.gym.ui.Routes
import com.forge.gym.ui.bottomBarSpace
import com.forge.gym.ui.components.AnimatedNumber
import com.forge.gym.ui.components.CircleIconButton
import com.forge.gym.ui.components.EmptyState
import com.forge.gym.ui.components.ForgeCard
import com.forge.gym.ui.components.ScreenHeader
import com.forge.gym.ui.components.WorkoutCard
import com.forge.gym.ui.components.bounceClick
import com.forge.gym.ui.rememberSettings
import com.forge.gym.ui.theme.Forge
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun CalendarScreen(nav: NavController) {
    val c = LocalContainer.current
    val workouts by c.repo.workouts.collectAsStateWithLifecycle()
    val active by c.repo.activeWorkout.collectAsStateWithLifecycle()
    val unit = rememberSettings().unit
    val scope = rememberCoroutineScope()
    val all = workouts.orEmpty()
    val byDate = remember(all) { all.groupBy { it.date } }
    val today = LocalDate.now()

    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var selected by rememberSaveable { mutableStateOf(today) }

    val monthWorkouts = remember(all, month) { all.filter { YearMonth.from(it.date) == month } }
    val dayWorkouts = byDate[selected].orEmpty()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomBarSpace())) {
        item {
            ScreenHeader("Calendar", eyebrow = "${com.forge.gym.domain.plural(all.size, "workout")} logged") {
                CircleIconButton(Icons.Rounded.EventAvailable, {
                    month = YearMonth.now(); selected = today
                }, contentDescription = "Today")
            }
        }
        item {
            ForgeCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(), padding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, { month = month.minusMonths(1) }, size = 38.dp, contentDescription = "Previous month")
                    AnimatedContent(
                        targetState = month,
                        transitionSpec = {
                            val dir = if (targetState > initialState) 1 else -1
                            (slideInHorizontally(tween(300)) { dir * it / 3 } + fadeIn(tween(300))) togetherWith
                                (slideOutHorizontally(tween(300)) { -dir * it / 3 } + fadeOut(tween(200)))
                        },
                        modifier = Modifier.weight(1f),
                        label = "monthTitle",
                    ) { m ->
                        Text(
                            m.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleLarge,
                            color = Forge.Text1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    CircleIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, { month = month.plusMonths(1) }, size = 38.dp, contentDescription = "Next month")
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth()) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = Forge.Text3, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(8.dp))
                var drag by remember { mutableFloatStateOf(0f) }
                AnimatedContent(
                    targetState = month,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(tween(340)) { dir * it } + fadeIn(tween(340))) togetherWith
                            (slideOutHorizontally(tween(340)) { -dir * it } + fadeOut(tween(240)))
                    },
                    modifier = Modifier.pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { drag = 0f },
                            onDragEnd = {
                                if (drag > 120) month = month.minusMonths(1)
                                else if (drag < -120) month = month.plusMonths(1)
                            },
                        ) { _, d -> drag += d }
                    },
                    label = "monthGrid",
                ) { m ->
                    MonthGrid(m, today, selected, byDate) { selected = it }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MonthStat("Workouts", monthWorkouts.size.toDouble(), { it.toInt().toString() }, Modifier.weight(1f))
                MonthStat("Volume · ${unit.label}", unit.fromKg(monthWorkouts.sumOf { it.volumeKg() }), { formatBig(it) }, Modifier.weight(1f))
                MonthStat("Hours", monthWorkouts.sumOf { it.durationMs() } / 3_600_000.0, { String.format(java.util.Locale.US, "%.1f", it) }, Modifier.weight(1f))
            }
        }
        item {
            Text(
                selected.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                style = MaterialTheme.typography.titleLarge,
                color = Forge.Text1,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
            )
        }
        item {
            AnimatedContent(
                targetState = selected to dayWorkouts,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
                label = "day",
            ) { (date, list) ->
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    list.forEach { w ->
                        WorkoutCard(w, unit, onClick = { nav.navigate(Routes.detail(w.workout.id)) })
                    }
                    when {
                        list.isEmpty() && date.isAfter(today) -> EmptyState(Icons.Rounded.SelfImprovement, "Nothing here yet", "This day is still ahead of you.")
                        list.isEmpty() && date == today -> EmptyState(
                            Icons.Rounded.PlayArrow, "No workout today (yet)", "Get after it, or log one you already did.",
                            action = if (active != null) "Resume workout" else "Start workout",
                            onAction = {
                                scope.launch { nav.navigate(Routes.workout(c.repo.startWorkout(null))) }
                            },
                        )
                        list.isEmpty() -> EmptyState(
                            Icons.Rounded.SelfImprovement, "Rest day", "Forgot to log something? Add it after the fact.",
                            action = "Log a workout",
                            onAction = { scope.launch { nav.navigate(Routes.workout(c.repo.createPastWorkout(date))) } },
                        )
                        !date.isAfter(today) -> Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).border(1.dp, Forge.StrokeStrong, RoundedCornerShape(18.dp))
                                .bounceClick { scope.launch { nav.navigate(Routes.workout(c.repo.createPastWorkout(date))) } }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.Icon(Icons.Rounded.Add, null, tint = Forge.Lime, modifier = Modifier.size(18.dp))
                            Text("  Log another workout", style = MaterialTheme.typography.labelLarge, color = Forge.Lime)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate,
    byDate: Map<LocalDate, List<WorkoutFull>>,
    onSelect: (LocalDate) -> Unit,
) {
    val first = month.atDay(1)
    val offset = first.dayOfWeek.value - 1
    val days = month.lengthOfMonth()
    val rows = (offset + days + 6) / 7
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0..6) {
                    val dayNum = r * 7 + col - offset + 1
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..days) {
                            val date = month.atDay(dayNum)
                            DayCell(date, date == today, date == selected, byDate[date].orEmpty().size) { onSelect(date) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, isToday: Boolean, isSelected: Boolean, count: Int, onClick: () -> Unit) {
    val trained = count > 0
    val scale by animateFloatAsState(if (isSelected) 1.08f else 1f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "dayScale")
    val bg by animateColorAsState(
        when {
            trained -> Forge.Lime
            isSelected -> Forge.Surface3
            else -> Color.Transparent
        },
        tween(220), label = "dayBg",
    )
    val borderColor = when {
        isSelected && trained -> Forge.Text1
        isSelected -> Forge.Lime
        isToday -> Forge.Lime.copy(alpha = 0.6f)
        else -> Color.Transparent
    }
    Box(
        Modifier
            .fillMaxSize()
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(if (borderColor == Color.Transparent) 0.dp else 2.dp, borderColor, RoundedCornerShape(14.dp))
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = when {
                trained -> Forge.OnLime
                isToday -> Forge.Lime
                date.isAfter(LocalDate.now()) -> Forge.Text3
                else -> Forge.Text1
            },
        )
        if (count > 1) {
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(count.coerceAtMost(3)) { Box(Modifier.size(4.dp).clip(CircleShape).background(Forge.OnLime)) }
            }
        }
    }
}

@Composable
private fun MonthStat(label: String, value: Double, format: (Double) -> String, modifier: Modifier = Modifier) {
    ForgeCard(modifier, padding = PaddingValues(14.dp)) {
        AnimatedNumber(value, format, MaterialTheme.typography.headlineSmall, Forge.Text1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Forge.Text3, maxLines = 1)
    }
}
