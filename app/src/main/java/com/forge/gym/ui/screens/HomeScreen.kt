package com.forge.gym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.forge.gym.data.TemplateFull
import com.forge.gym.domain.date
import com.forge.gym.domain.durationMs
import com.forge.gym.domain.formatBig
import com.forge.gym.domain.setCount
import com.forge.gym.domain.volumeKg
import com.forge.gym.domain.weekStart
import com.forge.gym.domain.weekStreak
import com.forge.gym.ui.LocalContainer
import com.forge.gym.ui.Routes
import com.forge.gym.ui.bottomBarSpace
import com.forge.gym.ui.components.AnimatedNumber
import com.forge.gym.ui.components.CircleIconButton
import com.forge.gym.ui.components.EmptyState
import com.forge.gym.ui.components.ForgeCard
import com.forge.gym.ui.components.OneRepMaxSheet
import com.forge.gym.ui.components.PlateCalculatorSheet
import com.forge.gym.ui.components.PrimaryButton
import com.forge.gym.ui.components.ProgressRing
import com.forge.gym.ui.components.SectionHeader
import com.forge.gym.ui.components.WorkoutCard
import com.forge.gym.ui.components.bounceClick
import com.forge.gym.ui.components.staggerIn
import com.forge.gym.ui.rememberSettings
import com.forge.gym.ui.theme.Forge
import com.forge.gym.ui.theme.muscleColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(nav: NavController) {
    val c = LocalContainer.current
    val workouts by c.repo.workouts.collectAsStateWithLifecycle()
    val templates by c.repo.templates.collectAsStateWithLifecycle()
    val active by c.repo.activeWorkout.collectAsStateWithLifecycle()
    val settings = rememberSettings()
    val unit = settings.unit
    val scope = rememberCoroutineScope()
    var showPlates by remember { mutableStateOf(false) }
    var show1rm by remember { mutableStateOf(false) }

    val all = workouts.orEmpty()
    val today = LocalDate.now()
    val weekStart = today.weekStart()
    val thisWeek = remember(all) { all.filter { !it.date.isBefore(weekStart) } }
    val streak = remember(all) { weekStreak(all) }
    val daysTrained = remember(thisWeek) { thisWeek.map { it.date }.toSet() }

    val start: (Long?) -> Unit = { tid ->
        scope.launch {
            val id = c.repo.startWorkout(tid)
            nav.navigate(Routes.workout(id))
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomBarSpace()),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 16.dp, top = 12.dp).staggerIn(0),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")).uppercase(),
                        style = MaterialTheme.typography.labelSmall, color = Forge.Text2,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(greeting(), style = MaterialTheme.typography.headlineLarge, color = Forge.Text1)
                }
                CircleIconButton(Icons.Rounded.Settings, { nav.navigate(Routes.SETTINGS) }, contentDescription = "Settings")
            }
        }

        item {
            HeroCard(
                count = thisWeek.size,
                goal = settings.weeklyGoal,
                streak = streak,
                active = active != null,
                onStart = { if (active != null) nav.navigate(Routes.workout(active!!.id)) else start(null) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).staggerIn(1),
            )
        }

        item {
            WeekStrip(weekStart, today, daysTrained, Modifier.padding(horizontal = 16.dp).staggerIn(2))
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).staggerIn(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MiniStat(Icons.Rounded.FitnessCenter, "Volume · ${unit.label}", thisWeek.sumOf { it.volumeKg() }.let { unit.fromKg(it) }, { formatBig(it) }, Modifier.weight(1f))
                MiniStat(Icons.Rounded.ViewInAr, "Sets", thisWeek.sumOf { it.setCount() }.toDouble(), { it.toInt().toString() }, Modifier.weight(1f))
                MiniStat(Icons.Rounded.Timer, "Minutes", thisWeek.sumOf { it.durationMs() } / 60000.0, { it.toInt().toString() }, Modifier.weight(1f))
            }
        }

        item {
            SectionHeader(
                "Routines",
                Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp).staggerIn(4),
                action = "New",
                onAction = { nav.navigate(Routes.template(0)) },
            )
        }
        item {
            LazyRow(
                Modifier.staggerIn(5),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(templates, key = { it.template.id }) { t ->
                    TemplateCard(t, onStart = { start(t.template.id) }, onEdit = { nav.navigate(Routes.template(t.template.id)) })
                }
                item {
                    Column(
                        Modifier.width(120.dp).height(176.dp).clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Forge.StrokeStrong, RoundedCornerShape(24.dp))
                            .bounceClick { nav.navigate(Routes.template(0)) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = Forge.Lime)
                        Spacer(Modifier.height(6.dp))
                        Text("New\nroutine", style = MaterialTheme.typography.labelLarge, color = Forge.Text2, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        item {
            SectionHeader(
                "Recent",
                Modifier.padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp).staggerIn(6),
                action = if (all.isNotEmpty()) "History" else null,
                onAction = { nav.navigate(Routes.HISTORY) },
            )
        }
        if (workouts != null && all.isEmpty()) {
            item {
                EmptyState(
                    Icons.Rounded.FitnessCenter,
                    "No workouts yet",
                    "Start a workout or pick a routine. Everything you log shows up here, on the calendar, and in your progress charts.",
                    Modifier.staggerIn(7),
                )
            }
        }
        itemsIndexed(all.take(4), key = { _, w -> w.workout.id }) { i, w ->
            WorkoutCard(
                w, unit, onClick = { nav.navigate(Routes.detail(w.workout.id)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp).staggerIn(7 + i).animateItem(),
            )
        }

        item {
            SectionHeader("Tools", Modifier.padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp))
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolCard(Icons.Rounded.ViewInAr, "Plate calculator", "Load the bar", Modifier.weight(1f)) { showPlates = true }
                ToolCard(Icons.Rounded.Calculate, "1RM calculator", "Estimate your max", Modifier.weight(1f)) { show1rm = true }
            }
        }
    }

    if (showPlates) PlateCalculatorSheet { showPlates = false }
    if (show1rm) OneRepMaxSheet { show1rm = false }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Late session?"
}

@Composable
private fun HeroCard(count: Int, goal: Int, streak: Int, active: Boolean, onStart: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Forge.LimeDim, Forge.Surface, Forge.Surface)))
            .border(1.dp, Forge.Lime.copy(alpha = 0.18f), RoundedCornerShape(28.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("THIS WEEK", style = MaterialTheme.typography.labelSmall, color = Forge.Lime)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedNumber(count.toDouble(), { it.toInt().toString() }, MaterialTheme.typography.displayLarge, Forge.Text1)
                    Text(" / $goal", style = MaterialTheme.typography.headlineMedium, color = Forge.Text3, modifier = Modifier.padding(bottom = 8.dp))
                }
                Text(
                    if (count >= goal) "Weekly goal smashed. Nice work." else "${goal - count} more to hit your weekly goal",
                    style = MaterialTheme.typography.bodyMedium, color = Forge.Text2,
                )
            }
            ProgressRing(count.toFloat() / goal.coerceAtLeast(1), size = 92.dp, stroke = 9.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, tint = if (streak > 0) Forge.Coral else Forge.Text3, modifier = Modifier.size(22.dp))
                    Text("$streak wk", style = MaterialTheme.typography.labelMedium, color = Forge.Text1)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        PrimaryButton(
            if (active) "Resume workout" else "Start workout",
            onStart,
            Modifier.fillMaxWidth(),
            icon = Icons.Rounded.PlayArrow,
        )
    }
}

@Composable
private fun WeekStrip(weekStart: LocalDate, today: LocalDate, trained: Set<LocalDate>, modifier: Modifier = Modifier) {
    ForgeCard(modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in 0..6) {
                val d = weekStart.plusDays(i.toLong())
                val done = d in trained
                val isToday = d == today
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        d.dayOfWeek.name.take(1),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) Forge.Lime else Forge.Text3,
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (done) Forge.Lime else Forge.Surface2)
                            .then(if (isToday && !done) Modifier.border(1.5.dp, Forge.Lime, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            d.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (done) Forge.OnLime else if (d.isAfter(today)) Forge.Text3 else Forge.Text1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(icon: ImageVector, label: String, value: Double, format: (Double) -> String, modifier: Modifier = Modifier) {
    ForgeCard(modifier, padding = PaddingValues(14.dp)) {
        Icon(icon, null, tint = Forge.Text2, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(10.dp))
        AnimatedNumber(value, format, MaterialTheme.typography.headlineSmall, Forge.Text1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Forge.Text3, maxLines = 1)
    }
}

@Composable
private fun TemplateCard(t: TemplateFull, onStart: () -> Unit, onEdit: () -> Unit) {
    val exercises = t.orderedExercises
    ForgeCard(Modifier.width(220.dp).height(176.dp), onClick = onStart, padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(t.template.name, style = MaterialTheme.typography.titleLarge, color = Forge.Text1, maxLines = 1, modifier = Modifier.weight(1f))
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(Forge.Surface2).bounceClick(onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Edit, "Edit", tint = Forge.Text2, modifier = Modifier.size(15.dp)) }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            exercises.map { it.exercise.muscleGroup }.distinct().take(5).forEach {
                Box(Modifier.size(7.dp).clip(CircleShape).background(muscleColor(it)))
            }
            Spacer(Modifier.width(4.dp))
            Text("${exercises.size} exercises", style = MaterialTheme.typography.labelMedium, color = Forge.Text2)
        }
        Spacer(Modifier.height(10.dp))
        exercises.take(3).forEach {
            Text(
                "${it.entry.sets} × ${it.exercise.name}",
                style = MaterialTheme.typography.bodySmall, color = Forge.Text3, maxLines = 1,
            )
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Forge.Lime, modifier = Modifier.size(18.dp))
            Text("Start", style = MaterialTheme.typography.labelLarge, color = Forge.Lime)
        }
    }
}

@Composable
private fun ToolCard(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ForgeCard(modifier, onClick = onClick, padding = PaddingValues(16.dp)) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Forge.Surface2),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = Forge.Lime, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = Forge.Text1)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Forge.Text2)
    }
}
