package com.forge.gym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.forge.gym.data.WeightUnit
import com.forge.gym.data.WorkoutExerciseWithSets
import com.forge.gym.data.WorkoutFull
import com.forge.gym.domain.PrHit
import com.forge.gym.domain.PrKind
import com.forge.gym.domain.counts
import com.forge.gym.domain.durationMs
import com.forge.gym.domain.e1rm
import com.forge.gym.domain.formatDuration
import com.forge.gym.domain.formatVolume
import com.forge.gym.domain.formatWeight
import com.forge.gym.domain.prsInWorkout
import com.forge.gym.domain.setCount
import com.forge.gym.domain.trimNum
import com.forge.gym.domain.volumeKg
import com.forge.gym.ui.LocalContainer
import com.forge.gym.ui.Routes
import com.forge.gym.ui.components.CircleIconButton
import com.forge.gym.ui.components.ConfirmDialog
import com.forge.gym.ui.components.DetailTopBar
import com.forge.gym.ui.components.ForgeCard
import com.forge.gym.ui.components.MuscleTag
import com.forge.gym.ui.components.TextInputDialog
import com.forge.gym.ui.components.staggerIn
import com.forge.gym.ui.rememberSettings
import com.forge.gym.ui.theme.Forge
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun WorkoutDetailScreen(nav: NavController, id: Long) {
    val c = LocalContainer.current
    val flow = remember(id) { c.repo.workout(id) }
    val workout by flow.collectAsState(initial = null)
    val all by c.repo.workouts.collectAsStateWithLifecycle()
    val unit = rememberSettings().unit
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var saveTemplate by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(workout) {
        if (workout != null) loaded = true
        else if (loaded) nav.popBackStack()
    }
    val w = workout ?: return
    val prs = remember(w, all) { prsInWorkout(w, all.orEmpty()) }

    Column(Modifier.fillMaxSize().background(Forge.Bg)) {
        DetailTopBar("", onBack = { nav.popBackStack() }) {
            CircleIconButton(Icons.Rounded.Edit, { nav.navigate(Routes.workout(id)) }, contentDescription = "Edit")
            Box {
                CircleIconButton(Icons.Rounded.MoreVert, { menu = true }, contentDescription = "More")
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Save as routine") },
                        leadingIcon = { Icon(Icons.Rounded.BookmarkAdd, null) },
                        onClick = { menu = false; saveTemplate = true },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete workout", color = Forge.Coral) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = Forge.Coral) },
                        onClick = { menu = false; confirmDelete = true },
                    )
                }
            }
        }
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp)) {
            item { WorkoutHeader(w, unit, prs.size, Modifier.staggerIn(0)) }
            if (prs.isNotEmpty()) {
                item { PrList(prs, unit, Modifier.padding(top = 12.dp).staggerIn(1)) }
            }
            itemsIndexed(w.orderedExercises, key = { _, e -> e.entry.id }) { i, e ->
                ExerciseResultCard(e, unit, onOpen = { nav.navigate(Routes.exercise(e.exercise.id)) }, modifier = Modifier.padding(top = 12.dp).staggerIn(2 + i))
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            "Delete workout?", "This removes it from your history, calendar, and stats. This can't be undone.", "Delete",
            onConfirm = { c.launch { deleteWorkout(id) } }, onDismiss = { confirmDelete = false }, destructive = true,
        )
    }
    if (saveTemplate) {
        TextInputDialog(
            "Save as routine", w.workout.name, "Routine name",
            onConfirm = { name -> c.launch { saveWorkoutAsTemplate(id, name) } }, onDismiss = { saveTemplate = false },
        )
    }
}

private val fullDate = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy  ·  h:mm a")

@Composable
fun WorkoutHeader(w: WorkoutFull, unit: WeightUnit, prCount: Int, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
        Text(w.workout.name, style = MaterialTheme.typography.displaySmall, color = Forge.Text1)
        Spacer(Modifier.height(4.dp))
        Text(
            Instant.ofEpochMilli(w.workout.startTime).atZone(ZoneId.systemDefault()).format(fullDate),
            style = MaterialTheme.typography.bodyMedium, color = Forge.Text2,
        )
        if (w.workout.notes.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(w.workout.notes, style = MaterialTheme.typography.bodyMedium, color = Forge.Text1)
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeaderStat("Duration", formatDuration(w.durationMs()), Modifier.weight(1f))
            HeaderStat("Volume", "${formatVolume(w.volumeKg(), unit)} ${unit.label}", Modifier.weight(1.2f))
            HeaderStat("Sets", w.setCount().toString(), Modifier.weight(0.8f))
            HeaderStat("PRs", prCount.toString(), Modifier.weight(0.7f), highlight = prCount > 0)
        }
    }
}

@Composable
private fun HeaderStat(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(if (highlight) Forge.LimeDim else Forge.Surface).padding(12.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = if (highlight) Forge.Lime else Forge.Text1, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Forge.Text3)
    }
}

@Composable
fun PrList(prs: List<PrHit>, unit: WeightUnit, modifier: Modifier = Modifier) {
    ForgeCard(modifier.fillMaxWidth(), color = Forge.LimeDim, borderColor = Forge.Lime.copy(alpha = 0.25f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.EmojiEvents, null, tint = Forge.Lime)
            Spacer(Modifier.width(8.dp))
            Text("Personal records", style = MaterialTheme.typography.titleMedium, color = Forge.Lime)
        }
        Spacer(Modifier.height(10.dp))
        prs.forEach { pr ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(pr.exerciseName, style = MaterialTheme.typography.titleSmall, color = Forge.Text1)
                    Text(pr.kind.label, style = MaterialTheme.typography.bodySmall, color = Forge.Text2)
                }
                Text(
                    when (pr.kind) {
                        PrKind.WEIGHT -> "${formatWeight(pr.valueKg, unit)} × ${pr.reps}"
                        PrKind.E1RM -> formatWeight(pr.valueKg, unit)
                        PrKind.VOLUME -> "${formatVolume(pr.valueKg, unit)} ${unit.label}"
                    },
                    style = MaterialTheme.typography.titleSmall, color = Forge.Lime,
                )
            }
        }
    }
}

@Composable
fun ExerciseResultCard(e: WorkoutExerciseWithSets, unit: WeightUnit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val sets = e.orderedSets
    val best = sets.filter { it.counts }.maxByOrNull { e1rm(it.weightKg, it.reps) }
    ForgeCard(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.exercise.name, style = MaterialTheme.typography.titleMedium, color = Forge.Text1)
                Spacer(Modifier.height(4.dp))
                MuscleTag(e.exercise.muscleGroup)
            }
            if (best != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatWeight(e1rm(best.weightKg, best.reps), unit), style = MaterialTheme.typography.titleSmall, color = Forge.Text1)
                    Text("est. 1RM", style = MaterialTheme.typography.labelSmall, color = Forge.Text3)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        var working = 0
        sets.forEach { s ->
            val label = if (s.isWarmup) "W" else (++working).toString()
            val isBest = s == best
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape).background(if (s.isWarmup) Forge.Amber.copy(alpha = 0.15f) else Forge.Surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = if (s.isWarmup) Forge.Amber else Forge.Text2)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "${formatWeight(s.weightKg, unit)}  ×  ${s.reps}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (s.completed) Forge.Text1 else Forge.Text3,
                    modifier = Modifier.weight(1f),
                )
                if (isBest) Icon(Icons.Rounded.Star, "Best set", tint = Forge.Lime, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    trimNum(unit.fromKg(e1rm(s.weightKg, s.reps))),
                    style = MaterialTheme.typography.labelMedium,
                    color = Forge.Text3,
                )
            }
        }
    }
}
