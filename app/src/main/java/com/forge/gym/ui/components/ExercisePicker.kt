package com.forge.gym.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forge.gym.data.Equipment
import com.forge.gym.data.ExerciseEntity
import com.forge.gym.data.MuscleGroups
import com.forge.gym.ui.LocalContainer
import com.forge.gym.ui.theme.Forge
import com.forge.gym.ui.theme.muscleColor
import kotlinx.coroutines.launch

/** Bottom sheet to pick one or more exercises. */
@Composable
fun ExercisePickerSheet(onDismiss: () -> Unit, onPick: (List<Long>) -> Unit) {
    val c = LocalContainer.current
    val exercises by c.repo.exercises.collectAsStateWithLifecycle()
    val workouts by c.repo.workouts.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val picked = remember { mutableStateListOf<Long>() }
    var creating by remember { mutableStateOf(false) }

    val lastUsed = remember(workouts) {
        val m = HashMap<Long, Long>()
        workouts.orEmpty().forEach { w -> w.exercises.forEach { e -> m[e.exercise.id] = maxOf(m[e.exercise.id] ?: 0L, w.workout.startTime) } }
        m
    }
    val shown = remember(exercises, query, filter, lastUsed) {
        val q = query.trim().lowercase()
        exercises.filter { e ->
            (q.isEmpty() || e.name.lowercase().contains(q) || e.muscleGroup.lowercase().contains(q)) &&
                when (filter) {
                    "All" -> true
                    "Recent" -> e.id in lastUsed
                    else -> e.muscleGroup == filter
                }
        }.let { list -> if (filter == "Recent") list.sortedByDescending { lastUsed[it.id] } else list }
    }

    ForgeSheet(onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.93f).imePadding()) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Add exercises", style = MaterialTheme.typography.headlineSmall, color = Forge.Text1, modifier = Modifier.weight(1f))
                Pill("+ Custom", Modifier.bounceClick { creating = true }, color = Forge.Surface2, textColor = Forge.Lime)
            }
            Spacer(Modifier.height(12.dp))
            ForgeField(
                query, { query = it }, "Search ${exercises.size} exercises",
                Modifier.padding(horizontal = 16.dp),
                leading = { Icon(Icons.Rounded.Search, null, tint = Forge.Text3) },
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(listOf("All", "Recent") + MuscleGroups.all) { f ->
                    FilterPill(f, f == filter) { filter = f }
                }
            }
            Box(Modifier.weight(1f)) {
                LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp)) {
                    if (shown.isEmpty()) {
                        item {
                            Text(
                                if (filter == "Recent") "Exercises you log will show up here." else "No matches. Create a custom exercise?",
                                style = MaterialTheme.typography.bodyMedium, color = Forge.Text3, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                            )
                        }
                    }
                    items(shown, key = { it.id }) { e ->
                        val isPicked = e.id in picked
                        ExerciseRow(e, isPicked, Modifier.animateItem()) {
                            if (isPicked) picked.remove(e.id) else picked.add(e.id)
                        }
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = picked.isNotEmpty(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    PrimaryButton(
                        "Add ${picked.size} exercise${if (picked.size == 1) "" else "s"}",
                        { onPick(picked.toList()); onDismiss() },
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                        icon = Icons.Rounded.Add,
                    )
                }
            }
        }
    }

    if (creating) {
        CreateExerciseDialog(
            onDismiss = { creating = false },
            onCreate = { name, muscle, equip ->
                scope.launch {
                    val id = c.repo.createExercise(name, muscle, equip)
                    picked.add(id)
                    filter = "All"
                    query = ""
                }
            },
        )
    }
}

@Composable
fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Forge.Lime else Forge.Surface2, tween(200), label = "fbg")
    val fg by animateColorAsState(if (selected) Forge.OnLime else Forge.Text2, tween(200), label = "ffg")
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = fg,
        modifier = Modifier.bounceClick(onClick = onClick).clip(RoundedCornerShape(12.dp)).background(bg).padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
fun ExerciseRow(e: ExerciseEntity, picked: Boolean, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    val bg by animateColorAsState(if (picked) Forge.Lime.copy(alpha = 0.08f) else Forge.Surface.copy(alpha = 0f), tween(200), label = "rowbg")
    val color = muscleColor(e.muscleGroup)
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(bg).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(e.name.take(1), style = MaterialTheme.typography.titleMedium, color = color)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(e.name, style = MaterialTheme.typography.titleSmall, color = Forge.Text1, maxLines = 1)
            Text(
                "${e.muscleGroup}  ·  ${e.equipment}${if (e.isCustom) "  ·  Custom" else ""}",
                style = MaterialTheme.typography.bodySmall, color = Forge.Text2, maxLines = 1,
            )
        }
        if (trailing != null) {
            trailing()
        } else {
            val check by animateColorAsState(if (picked) Forge.Lime else Forge.Surface3, tween(200), label = "chk")
            Box(
                Modifier.size(26.dp).clip(CircleShape).background(check),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.animation.AnimatedVisibility(visible = picked, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                    Icon(Icons.Rounded.Check, null, tint = Forge.OnLime, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateExerciseDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, muscle: String, equipment: String) -> Unit,
    initial: ExerciseEntity? = null,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var muscle by remember { mutableStateOf(initial?.muscleGroup ?: "Chest") }
    var equip by remember { mutableStateOf(initial?.equipment ?: "Barbell") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Forge.Surface,
        shape = RoundedCornerShape(28.dp),
        title = { Text(if (initial == null) "New exercise" else "Edit exercise", style = MaterialTheme.typography.headlineSmall, color = Forge.Text1) },
        text = {
            Column {
                ForgeField(name, { name = it.take(40) }, "Exercise name")
                Spacer(Modifier.height(14.dp))
                Text("Muscle group", style = MaterialTheme.typography.labelMedium, color = Forge.Text2)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MuscleGroups.all.forEach { m -> SmallChoice(m, m == muscle) { muscle = m } }
                }
                Spacer(Modifier.height(14.dp))
                Text("Equipment", style = MaterialTheme.typography.labelMedium, color = Forge.Text2)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Equipment.all.forEach { m -> SmallChoice(m, m == equip) { equip = m } }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name.trim(), muscle, equip); onDismiss() }) {
                Text(if (initial == null) "Create" else "Save", color = if (name.isNotBlank()) Forge.Lime else Forge.Text3, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Forge.Text2, style = MaterialTheme.typography.labelLarge) }
        },
    )
}

@Composable
private fun SmallChoice(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) Forge.OnLime else Forge.Text2,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Forge.Lime else Forge.Surface2)
            .border(1.dp, if (selected) Forge.Lime else Forge.Stroke, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
