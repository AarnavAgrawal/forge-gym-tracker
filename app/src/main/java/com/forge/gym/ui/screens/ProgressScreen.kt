package com.forge.gym.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.forge.gym.data.BodyWeightEntity
import com.forge.gym.data.MuscleGroups
import com.forge.gym.data.WeightUnit
import com.forge.gym.data.WorkoutFull
import com.forge.gym.domain.counts
import com.forge.gym.domain.date
import com.forge.gym.domain.durationMs
import com.forge.gym.domain.formatBig
import com.forge.gym.domain.formatWeight
import com.forge.gym.domain.sessionsFor
import com.forge.gym.domain.toLocalDate
import com.forge.gym.domain.trimNum
import com.forge.gym.domain.volumeKg
import com.forge.gym.domain.weekStart
import com.forge.gym.ui.LocalContainer
import com.forge.gym.ui.Routes
import com.forge.gym.ui.bottomBarSpace
import com.forge.gym.ui.components.AnimatedNumber
import com.forge.gym.ui.components.BarChart
import com.forge.gym.ui.components.ChartPoint
import com.forge.gym.ui.components.ConsistencyHeatmap
import com.forge.gym.ui.components.EmptyState
import com.forge.gym.ui.components.ForgeCard
import com.forge.gym.ui.components.LineChart
import com.forge.gym.ui.components.PrimaryButton
import com.forge.gym.ui.components.ScreenHeader
import com.forge.gym.ui.components.SegmentedTabs
import com.forge.gym.ui.components.Sparkline
import com.forge.gym.ui.components.TextInputDialog
import com.forge.gym.ui.components.staggerIn
import com.forge.gym.ui.rememberSettings
import com.forge.gym.ui.theme.Forge
import com.forge.gym.ui.theme.muscleColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val shortDate = DateTimeFormatter.ofPattern("MMM d")

@Composable
fun ProgressScreen(nav: NavController) {
    val c = LocalContainer.current
    val workouts by c.repo.workouts.collectAsStateWithLifecycle()
    val bodyWeights by c.repo.bodyWeights.collectAsStateWithLifecycle()
    val unit = rememberSettings().unit
    var tab by rememberSaveable { mutableStateOf(0) }
    val all = workouts.orEmpty()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Progress", eyebrow = "Your numbers")
        SegmentedTabs(listOf("Overview", "Exercises", "Body"), tab, { tab = it }, Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(320)) { dir * it / 6 } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(320)) { -dir * it / 6 } + fadeOut(tween(180)))
            },
            label = "progressTab",
        ) { t ->
            when (t) {
                0 -> OverviewTab(all, unit, nav)
                1 -> ExercisesTab(all, unit, nav)
                else -> BodyTab(bodyWeights, unit)
            }
        }
    }
}

@Composable
private fun OverviewTab(all: List<WorkoutFull>, unit: WeightUnit, nav: NavController) {
    val today = LocalDate.now()
    val counts = remember(all) { all.groupingBy { it.date }.eachCount() }
    val weeks = 10
    val weekly = remember(all) {
        val start = today.weekStart().minusWeeks((weeks - 1).toLong())
        (0 until weeks).map { i ->
            val ws = start.plusWeeks(i.toLong())
            ws to all.filter { it.date.weekStart() == ws }.sumOf { it.volumeKg() }
        }
    }
    val muscleSets = remember(all) {
        val since = today.minusDays(30)
        val m = MuscleGroups.all.associateWith { 0 }.toMutableMap()
        all.filter { it.date.isAfter(since) }.forEach { w ->
            w.exercises.forEach { e -> m[e.exercise.muscleGroup] = (m[e.exercise.muscleGroup] ?: 0) + e.sets.count { it.counts } }
        }
        m.filterValues { it > 0 }.toList().sortedByDescending { it.second }
    }
    val bests = remember(all) {
        all.flatMap { w -> w.exercises.map { it.exercise } }.distinctBy { it.id }
            .map { ex -> ex to sessionsFor(ex.id, all) }
            .filter { it.second.isNotEmpty() }
            .sortedByDescending { it.second.size }
            .take(6)
    }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomBarSpace()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (all.isEmpty()) {
            item { EmptyState(Icons.Rounded.Insights, "No data yet", "Finish a workout and your charts, streaks, and records will start filling in.") }
            return@LazyColumn
        }
        item {
            ForgeCard(Modifier.fillMaxWidth().staggerIn(0)) {
                Text("ALL TIME", style = MaterialTheme.typography.labelSmall, color = Forge.Text2)
                Spacer(Modifier.height(10.dp))
                Row {
                    Column(Modifier.weight(1f)) {
                        AnimatedNumber(all.size.toDouble(), { it.toInt().toString() }, MaterialTheme.typography.headlineLarge)
                        Text("Workouts", style = MaterialTheme.typography.labelMedium, color = Forge.Text2)
                    }
                    Column(Modifier.weight(1.3f)) {
                        AnimatedNumber(unit.fromKg(all.sumOf { it.volumeKg() }), { formatBig(it) }, MaterialTheme.typography.headlineLarge)
                        Text("Volume · ${unit.label}", style = MaterialTheme.typography.labelMedium, color = Forge.Text2)
                    }
                    Column(Modifier.weight(1f)) {
                        AnimatedNumber(all.sumOf { it.durationMs() } / 3_600_000.0, { trimNum(it, 1) }, MaterialTheme.typography.headlineLarge)
                        Text("Hours", style = MaterialTheme.typography.labelMedium, color = Forge.Text2)
                    }
                }
            }
        }
        item {
            ForgeCard(Modifier.fillMaxWidth().staggerIn(1)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Consistency", style = MaterialTheme.typography.titleMedium, color = Forge.Text1, modifier = Modifier.weight(1f))
                    val recent = all.count { !it.date.isBefore(today.weekStart().minusWeeks(17)) }
                    Text("$recent in 18 weeks", style = MaterialTheme.typography.labelMedium, color = Forge.Text2)
                }
                Spacer(Modifier.height(14.dp))
                ConsistencyHeatmap(counts)
            }
        }
        item {
            ForgeCard(Modifier.fillMaxWidth().staggerIn(2)) {
                Text("Weekly volume", style = MaterialTheme.typography.titleMedium, color = Forge.Text1)
                Text("Total ${unit.label} lifted per week", style = MaterialTheme.typography.bodySmall, color = Forge.Text2)
                Spacer(Modifier.height(16.dp))
                BarChart(
                    values = weekly.map { unit.fromKg(it.second).toFloat() },
                    labels = weekly.map { "${it.first.monthValue}/${it.first.dayOfMonth}" },
                    valueFormatter = { formatBig(it.toDouble()) },
                )
            }
        }
        if (muscleSets.isNotEmpty()) {
            item {
                ForgeCard(Modifier.fillMaxWidth().staggerIn(3)) {
                    Text("Muscle split", style = MaterialTheme.typography.titleMedium, color = Forge.Text1)
                    Text("Working sets in the last 30 days", style = MaterialTheme.typography.bodySmall, color = Forge.Text2)
                    Spacer(Modifier.height(14.dp))
                    val max = muscleSets.maxOf { it.second }.toFloat()
                    muscleSets.forEachIndexed { i, (m, n) -> MuscleBar(m, n, n / max, i) }
                }
            }
        }
        if (bests.isNotEmpty()) {
            item {
                ForgeCard(Modifier.fillMaxWidth().staggerIn(4)) {
                    Text("Best lifts", style = MaterialTheme.typography.titleMedium, color = Forge.Text1)
                    Text("Estimated 1RM for your most-trained exercises", style = MaterialTheme.typography.bodySmall, color = Forge.Text2)
                    Spacer(Modifier.height(8.dp))
                    bests.forEach { (ex, sessions) ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { nav.navigate(Routes.exercise(ex.id)) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(muscleColor(ex.muscleGroup)))
                            Spacer(Modifier.width(12.dp))
                            Text(ex.name, style = MaterialTheme.typography.titleSmall, color = Forge.Text1, modifier = Modifier.weight(1f), maxLines = 1)
                            Sparkline(sessions.takeLast(12).map { unit.fromKg(it.bestE1rm).toFloat() }, Modifier.width(60.dp).height(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(formatWeight(sessions.maxOf { it.bestE1rm }, unit), style = MaterialTheme.typography.titleSmall, color = Forge.Lime)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MuscleBar(muscle: String, sets: Int, fraction: Float, index: Int) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        kotlinx.coroutines.delay(index * 60L)
        anim.animateTo(fraction, tween(700))
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(muscle, style = MaterialTheme.typography.labelLarge, color = Forge.Text2, modifier = Modifier.width(84.dp), maxLines = 1)
        Box(Modifier.weight(1f).height(12.dp).clip(CircleShape).background(Forge.Surface2)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(anim.value.coerceAtLeast(0.02f)).clip(CircleShape).background(muscleColor(muscle)))
        }
        Text("$sets", style = MaterialTheme.typography.labelLarge, color = Forge.Text1, modifier = Modifier.width(36.dp).padding(start = 10.dp))
    }
}

@Composable
private fun ExercisesTab(all: List<WorkoutFull>, unit: WeightUnit, nav: NavController) {
    val rows = remember(all) {
        all.flatMap { w -> w.exercises.map { it.exercise } }.distinctBy { it.id }
            .map { ex -> ex to sessionsFor(ex.id, all) }
            .filter { it.second.isNotEmpty() }
            .sortedByDescending { it.second.last().date }
    }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomBarSpace()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (rows.isEmpty()) {
            item { EmptyState(Icons.Rounded.Insights, "No exercises tracked", "Exercises you complete sets for will show up here with trend lines.") }
        }
        rows.forEachIndexed { i, (ex, sessions) ->
            item(key = ex.id) {
                val recent = sessions.takeLast(10)
                val first = recent.first().bestE1rm
                val last = recent.last().bestE1rm
                val change = if (first > 0 && recent.size > 1) (last - first) / first * 100 else 0.0
                ForgeCard(Modifier.fillMaxWidth().staggerIn(i), onClick = { nav.navigate(Routes.exercise(ex.id)) }, padding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(ex.name, style = MaterialTheme.typography.titleMedium, color = Forge.Text1, maxLines = 1)
                            Text(
                                "${sessions.size} sessions  ·  last ${sessions.last().date.toLocalDate().format(shortDate)}",
                                style = MaterialTheme.typography.bodySmall, color = Forge.Text2,
                            )
                        }
                        Sparkline(recent.map { unit.fromKg(it.bestE1rm).toFloat() }, Modifier.width(72.dp).height(30.dp), color = muscleColor(ex.muscleGroup))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Est. 1RM ", style = MaterialTheme.typography.labelMedium, color = Forge.Text3)
                        Text(formatWeight(last, unit), style = MaterialTheme.typography.titleSmall, color = Forge.Text1)
                        Spacer(Modifier.weight(1f))
                        if (recent.size > 1) {
                            val up = change >= 0
                            Icon(if (up) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown, null, tint = if (up) Forge.Lime else Forge.Coral, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${if (up) "+" else ""}${trimNum(change, 1)}%", style = MaterialTheme.typography.labelLarge, color = if (up) Forge.Lime else Forge.Coral)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BodyTab(entries: List<BodyWeightEntity>, unit: WeightUnit) {
    val c = LocalContainer.current
    var adding by remember { mutableStateOf(false) }
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomBarSpace()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ForgeCard(Modifier.fillMaxWidth().staggerIn(0)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Body weight", style = MaterialTheme.typography.titleMedium, color = Forge.Text1)
                        if (entries.size >= 2) {
                            val diff = unit.fromKg(entries.last().weightKg - entries.first().weightKg)
                            Text(
                                "${if (diff >= 0) "+" else ""}${trimNum(diff, 1)} ${unit.label} since ${entries.first().date.toLocalDate().format(shortDate)}",
                                style = MaterialTheme.typography.bodySmall, color = Forge.Text2,
                            )
                        }
                    }
                    PrimaryButton("Log", { adding = true }, icon = Icons.Rounded.Add, height = 40.dp)
                }
                Spacer(Modifier.height(16.dp))
                if (entries.isEmpty()) {
                    EmptyState(Icons.Rounded.MonitorWeight, "No entries yet", "Log your weight regularly to see the trend.")
                } else {
                    LineChart(
                        points = entries.map { ChartPoint(it.date, unit.fromKg(it.weightKg).toFloat()) },
                        yFormatter = { "${trimNum(it.toDouble(), 1)}" },
                        xFormatter = { it.toLocalDate().format(shortDate) },
                        color = Forge.Blue,
                        emptyText = "Log one more entry to see your trend",
                    )
                }
            }
        }
        items(entries.reversed().size) { i ->
            val e = entries.reversed()[i]
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Forge.Surface).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(e.date.toLocalDate().format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")), style = MaterialTheme.typography.bodyMedium, color = Forge.Text2, modifier = Modifier.weight(1f))
                Text(formatWeight(e.weightKg, unit), style = MaterialTheme.typography.titleSmall, color = Forge.Text1)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Rounded.Close, "Delete", tint = Forge.Text3, modifier = Modifier.size(28.dp).clip(CircleShape).clickable { c.launch { deleteBodyWeight(e.id) } }.padding(5.dp))
            }
        }
    }
    if (adding) {
        TextInputDialog(
            "Log body weight", entries.lastOrNull()?.let { trimNum(unit.fromKg(it.weightKg), 1) } ?: "", "Weight in ${unit.label}",
            onConfirm = { v -> v.toDoubleOrNull()?.takeIf { it > 0 }?.let { w -> c.launch { addBodyWeight(System.currentTimeMillis(), unit.toKg(w)) } } },
            onDismiss = { adding = false },
            confirmText = "Log",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
    }
}
