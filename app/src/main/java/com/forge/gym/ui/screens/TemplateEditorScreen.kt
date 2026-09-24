package com.forge.gym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.forge.gym.data.ExerciseEntity
import com.forge.gym.data.TemplateItem
import com.forge.gym.ui.LocalContainer
import com.forge.gym.ui.components.CircleIconButton
import com.forge.gym.ui.components.ConfirmDialog
import com.forge.gym.ui.components.DetailTopBar
import com.forge.gym.ui.components.ExercisePickerSheet
import com.forge.gym.ui.components.ForgeCard
import com.forge.gym.ui.components.ForgeField
import com.forge.gym.ui.components.MuscleTag
import com.forge.gym.ui.components.PrimaryButton
import com.forge.gym.ui.components.Stepper
import com.forge.gym.ui.theme.Forge

private data class EditItem(val key: Long, val exercise: ExerciseEntity, val sets: Int, val reps: Int)

@Composable
fun TemplateEditorScreen(nav: NavController, id: Long) {
    val c = LocalContainer.current
    val exercises by c.repo.exercises.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<EditItem>() }
    var loaded by remember { mutableStateOf(id == 0L) }
    var picker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var nextKey by remember { mutableStateOf(1L) }

    LaunchedEffect(id) {
        if (id != 0L && !loaded) {
            c.repo.template(id)?.let { t ->
                name = t.template.name
                items.clear()
                t.orderedExercises.forEach { items.add(EditItem(nextKey++, it.exercise, it.entry.sets, it.entry.reps)) }
            }
            loaded = true
        }
    }

    fun save() {
        val n = name.trim().ifBlank { "My Routine" }
        val list = items.map { TemplateItem(it.exercise.id, it.sets, it.reps) }
        c.launch { saveTemplate(id, n, list) }
        nav.popBackStack()
    }

    Column(Modifier.fillMaxSize().background(Forge.Bg).imePadding()) {
        DetailTopBar(if (id == 0L) "New routine" else "Edit routine", onBack = { nav.popBackStack() }) {
            if (id != 0L) CircleIconButton(Icons.Rounded.Delete, { confirmDelete = true }, tint = Forge.Coral, contentDescription = "Delete routine")
            PrimaryButton("Save", { save() }, height = 42.dp, enabled = items.isNotEmpty())
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ForgeField(name, { name = it.take(40) }, "Routine name (e.g. Upper A)", textStyle = MaterialTheme.typography.titleLarge)
            }
            if (items.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).border(1.dp, Forge.StrokeStrong, RoundedCornerShape(24.dp)).padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("No exercises yet", style = MaterialTheme.typography.titleMedium, color = Forge.Text1)
                        Spacer(Modifier.height(4.dp))
                        Text("Add exercises and set target sets and reps.", style = MaterialTheme.typography.bodyMedium, color = Forge.Text2, textAlign = TextAlign.Center)
                    }
                }
            }
            itemsIndexed(items, key = { _, it -> it.key }) { i, item ->
                ForgeCard(Modifier.fillMaxWidth().animateItem(), padding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.exercise.name, style = MaterialTheme.typography.titleMedium, color = Forge.Text1)
                            Spacer(Modifier.height(4.dp))
                            MuscleTag(item.exercise.muscleGroup)
                        }
                        if (i > 0) CircleIconButton(Icons.Rounded.ArrowUpward, { items.add(i - 1, items.removeAt(i)) }, size = 34.dp, contentDescription = "Move up")
                        if (i < items.lastIndex) {
                            Spacer(Modifier.width(6.dp))
                            CircleIconButton(Icons.Rounded.ArrowDownward, { items.add(i + 1, items.removeAt(i)) }, size = 34.dp, contentDescription = "Move down")
                        }
                        Spacer(Modifier.width(6.dp))
                        CircleIconButton(Icons.Rounded.Close, { items.removeAt(i) }, size = 34.dp, tint = Forge.Coral, contentDescription = "Remove")
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sets", style = MaterialTheme.typography.labelLarge, color = Forge.Text2, modifier = Modifier.width(44.dp))
                        Stepper(item.sets, { v -> items[i] = item.copy(sets = v) }, min = 1, max = 12)
                        Spacer(Modifier.weight(1f))
                        Text("Reps", style = MaterialTheme.typography.labelLarge, color = Forge.Text2, modifier = Modifier.width(44.dp))
                        Stepper(item.reps, { v -> items[i] = item.copy(reps = v) }, min = 1, max = 50)
                    }
                }
            }
            item {
                PrimaryButton("Add exercises", { picker = true }, Modifier.fillMaxWidth().navigationBarsPadding(), icon = Icons.Rounded.Add,
                    color = Forge.Surface2, contentColor = Forge.Lime)
            }
        }
    }

    if (picker) {
        ExercisePickerSheet(onDismiss = { picker = false }, onPick = { ids ->
            ids.mapNotNull { pid -> exercises.firstOrNull { it.id == pid } }.forEach { items.add(EditItem(nextKey++, it, 3, 10)) }
        })
    }
    if (confirmDelete) {
        ConfirmDialog("Delete routine?", "Your workout history won't be affected.", "Delete", onConfirm = {
            c.launch { deleteTemplate(id) }
            nav.popBackStack()
        }, onDismiss = { confirmDelete = false }, destructive = true)
    }
}
