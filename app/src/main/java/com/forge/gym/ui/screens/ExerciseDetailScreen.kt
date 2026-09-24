package com.forge.gym.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.forge.gym.domain.ExerciseSession
import com.forge.gym.domain.formatVolume
import com.forge.gym.domain.formatWeight
import com.forge.gym.domain.sessionsFor
import com.forge.gym.domain.toLocalDate
import com.forge.gym.domain.trimNum
import com.forge.gym.ui.LocalContainer
import com.forge.gym.ui.Routes
import com.forge.gym.ui.components.ChartPoint
import com.forge.gym.ui.components.CircleIconButton
import com.forge.gym.ui.components.ConfirmDialog
import com.forge.gym.ui.components.CreateExerciseDialog
import com.forge.gym.ui.components.DetailTopBar
import com.forge.gym.ui.components.EmptyState
import com.forge.gym.ui.components.FilterPill
import com.forge.gym.ui.components.ForgeCard
import com.forge.gym.ui.components.LineChart
import com.forge.gym.ui.components.MuscleTag
import com.forge.gym.ui.components.Pill
import com.forge.gym.ui.components.staggerIn
import com.forge.gym.ui.rememberSettings
import com.forge.gym.ui.theme.Forge
import com.forge.gym.ui.theme.muscleColor
import java.time.format.DateTimeFormatter

private enum class Metric(val label: String) { E1RM("Est. 1RM"), WEIGHT("Top weight"), VOLUME("Volume"), REPS("Total reps") }

@Composable
fun ExerciseDetailScreen(nav: NavController, id: Long) {
    val c = LocalContainer.current
    val exercises by c.repo.exercises.collectAsStateWithLifecycle()
    val workouts by c.repo.workouts.collectAsStateWithLifecycle()
    val unit = rememberSettings().unit
    var metric by rememberSaveable { mutableStateOf(Metric.E1RM) }
    var menu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var seen by remember { mutableStateOf(false) }

    val ex = exercises.firstOrNull { it.id == id }
    LaunchedEffect(ex) { if (ex != null) seen = true else if (seen) nav.popBackStack() }
    if (ex == null) return
    val sessions = remember(workouts, id) { sessionsFor(id, workouts.orEmpty()) }
    val color = muscleColor(ex.muscleGroup)

    Column(Modifier.fillMaxSize().background(Forge.Bg)) {
        DetailTopBar("", onBack = { nav.popBackStack() }) {
            if (ex.isCustom) {
                Box {
                    CircleIconButton(Icons.Rounded.MoreVert, { menu = true }, contentDescription = "More")
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Edit exercise") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menu = false; editing = true })
                        DropdownMenuItem(
                            text = { Text("Delete exercise", color = Forge.Coral) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = Forge.Coral) },
                            onClick = { menu = false; deleting = true },
                        )
                    }
                }
            }
        }
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(Modifier.padding(horizontal = 4.dp).staggerIn(0)) {
                    Text(ex.name, style = MaterialTheme.typography.displaySmall, color = Forge.Text1)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        MuscleTag(ex.muscleGroup)
                        Pill(ex.equipment)
                        if (ex.isCustom) Pill("Custom")
                    }
                }
            }
            if (sessions.isEmpty()) {
                item { EmptyState(Icons.Rounded.ShowChart, "No history yet", "Complete a set of ${ex.name} and your records and charts will appear here.") }
                return@LazyColumn
            }
            item {
                val bestE1 = sessions.maxBy { it.bestE1rm }
                val heaviest = sessions.flatMap { it.working }.maxBy { it.weightKg }
                val bestSetVol = sessions.flatMap { it.working }.maxBy { it.weightKg * it.reps }
                Column(Modifier.staggerIn(1), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RecordTile("Best est. 1RM", formatWeight(bestE1.bestE1rm, unit), bestE1.date.toLocalDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy")), Modifier.weight(1f), highlight = true)
                        RecordTile("Heaviest", formatWeight(heaviest.weightKg, unit), "× ${heaviest.reps} reps", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RecordTile("Best set", "${formatWeight(bestSetVol.weightKg, unit, false)} × ${bestSetVol.reps}", "${formatVolume(bestSetVol.weightKg * bestSetVol.reps, unit)} ${unit.label}", Modifier.weight(1f))
                        RecordTile("Sessions", sessions.size.toString(), com.forge.gym.domain.plural(sessions.sumOf { it.working.size }, "working set"), Modifier.weight(1f))
                    }
                }
            }
            item {
                ForgeCard(Modifier.fillMaxWidth().staggerIn(2)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Metric.entries) { m -> FilterPill(m.label, m == metric) { metric = m } }
                    }
                    Spacer(Modifier.height(16.dp))
                    AnimatedContent(metric, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "metric") { m ->
                        val points = sessions.map {
                            ChartPoint(
                                it.date,
                                when (m) {
                                    Metric.E1RM -> unit.fromKg(it.bestE1rm)
                                    Metric.WEIGHT -> unit.fromKg(it.maxWeight)
                                    Metric.VOLUME -> unit.fromKg(it.volume)
                                    Metric.REPS -> it.totalReps.toDouble()
                                }.toFloat(),
                            )
                        }
                        LineChart(
                            points = points,
                            yFormatter = { v -> if (m == Metric.REPS) v.toInt().toString() else "${trimNum(v.toDouble(), 0)}" },
                            xFormatter = { it.toLocalDate().format(DateTimeFormatter.ofPattern("MMM d")) },
                            color = color,
                        )
                    }
                }
            }
            item {
                Text("History", style = MaterialTheme.typography.titleLarge, color = Forge.Text1, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            }
            itemsIndexed(sessions.reversed(), key = { _, s -> "${s.workoutId}-${s.date}" }) { i, s ->
                SessionCard(s, unit, Modifier.staggerIn(3 + i)) { nav.navigate(Routes.detail(s.workoutId)) }
            }
        }
    }

    if (editing) {
        CreateExerciseDialog(
            initial = ex,
            onDismiss = { editing = false },
            onCreate = { name, muscle, equip -> c.launch { updateExercise(ex.copy(name = name, muscleGroup = muscle, equipment = equip)) } },
        )
    }
    if (deleting) {
        ConfirmDialog(
            "Delete ${ex.name}?", "This also deletes every set you've logged for it, and removes it from routines.", "Delete",
            onConfirm = { c.launch { deleteExercise(ex.id) } }, onDismiss = { deleting = false }, destructive = true,
        )
    }
}

@Composable
private fun RecordTile(label: String, value: String, sub: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(if (highlight) Forge.LimeDim else Forge.Surface).padding(16.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = if (highlight) Forge.Lime else Forge.Text2)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, color = if (highlight) Forge.Lime else Forge.Text1, maxLines = 1)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = Forge.Text2, maxLines = 1)
    }
}

@Composable
private fun SessionCard(s: ExerciseSession, unit: com.forge.gym.data.WeightUnit, modifier: Modifier, onClick: () -> Unit) {
    ForgeCard(modifier.fillMaxWidth(), onClick = onClick, padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.date.toLocalDate().format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")), style = MaterialTheme.typography.titleSmall, color = Forge.Text1)
                Text(s.workoutName, style = MaterialTheme.typography.bodySmall, color = Forge.Text2)
            }
            Text(formatWeight(s.bestE1rm, unit), style = MaterialTheme.typography.titleSmall, color = Forge.Text1)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            s.sets.filter { it.completed }.take(8).forEach { set ->
                val best = set == s.bestSet
                Text(
                    "${formatWeight(set.weightKg, unit, false)}×${set.reps}",
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        set.isWarmup -> Forge.Amber
                        best -> Forge.OnLime
                        else -> Forge.Text1
                    },
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (best) Forge.Lime else Forge.Surface2).padding(horizontal = 7.dp, vertical = 4.dp),
                )
            }
        }
    }
}
