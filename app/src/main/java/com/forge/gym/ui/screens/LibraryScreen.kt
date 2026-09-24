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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.forge.gym.data.MuscleGroups
import com.forge.gym.ui.LocalContainer
import com.forge.gym.ui.Routes
import com.forge.gym.ui.bottomBarSpace
import com.forge.gym.ui.components.CircleIconButton
import com.forge.gym.ui.components.CreateExerciseDialog
import com.forge.gym.ui.components.EmptyState
import com.forge.gym.ui.components.ExerciseRow
import com.forge.gym.ui.components.FilterPill
import com.forge.gym.ui.components.ForgeCard
import com.forge.gym.ui.components.ForgeField
import com.forge.gym.ui.components.PrimaryButton
import com.forge.gym.ui.components.ScreenHeader
import com.forge.gym.ui.components.SecondaryButton
import com.forge.gym.ui.components.SegmentedTabs
import com.forge.gym.ui.components.staggerIn
import com.forge.gym.ui.theme.Forge
import com.forge.gym.ui.theme.muscleColor
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(nav: NavController) {
    val c = LocalContainer.current
    val exercises by c.repo.exercises.collectAsStateWithLifecycle()
    val templates by c.repo.templates.collectAsStateWithLifecycle()
    val workouts by c.repo.workouts.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var creating by remember { mutableStateOf(false) }

    val usage = remember(workouts) {
        val m = HashMap<Long, Int>()
        workouts.orEmpty().forEach { w -> w.exercises.forEach { m[it.exercise.id] = (m[it.exercise.id] ?: 0) + 1 } }
        m
    }
    val shown = remember(exercises, query, filter) {
        val q = query.trim().lowercase()
        exercises.filter { (q.isEmpty() || it.name.lowercase().contains(q)) && (filter == "All" || it.muscleGroup == filter) }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Library", eyebrow = "${exercises.size} exercises · ${templates.size} routines") {
            CircleIconButton(Icons.Rounded.Add, {
                if (tab == 0) creating = true else nav.navigate(Routes.template(0))
            }, background = Forge.Lime, tint = Forge.OnLime, contentDescription = "Add")
        }
        SegmentedTabs(listOf("Exercises", "Routines"), tab, { tab = it }, Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
        AnimatedContent(tab, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "libTab") { t ->
            if (t == 0) {
                Column {
                    ForgeField(
                        query, { query = it }, "Search exercises",
                        Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        leading = { Icon(Icons.Rounded.Search, null, tint = Forge.Text3) },
                    )
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("All") + MuscleGroups.all) { f -> FilterPill(f, f == filter) { filter = f } }
                    }
                    LazyColumn(contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = bottomBarSpace())) {
                        items(shown, key = { it.id }) { e ->
                            ExerciseRow(
                                e, false, Modifier.animateItem(),
                                trailing = {
                                    val n = usage[e.id] ?: 0
                                    if (n > 0) Text("$n×", style = MaterialTheme.typography.labelMedium, color = Forge.Text2)
                                    Icon(Icons.Rounded.ChevronRight, null, tint = Forge.Text3)
                                },
                            ) { nav.navigate(Routes.exercise(e.id)) }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomBarSpace()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (templates.isEmpty()) {
                        item {
                            EmptyState(
                                Icons.Rounded.ViewList, "No routines", "Routines are reusable workout plans. Build one, then start it in a tap.",
                                action = "Create routine", onAction = { nav.navigate(Routes.template(0)) },
                            )
                        }
                    }
                    items(templates.size) { i ->
                        val t = templates[i]
                        ForgeCard(Modifier.fillMaxWidth().staggerIn(i)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(t.template.name, style = MaterialTheme.typography.titleLarge, color = Forge.Text1, modifier = Modifier.weight(1f))
                                Text("${t.exercises.size} exercises", style = MaterialTheme.typography.labelMedium, color = Forge.Text2)
                            }
                            Spacer(Modifier.height(10.dp))
                            t.orderedExercises.forEach { te ->
                                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(7.dp).clip(CircleShape).background(muscleColor(te.exercise.muscleGroup)))
                                    Spacer(Modifier.width(10.dp))
                                    Text(te.exercise.name, style = MaterialTheme.typography.bodyMedium, color = Forge.Text1, modifier = Modifier.weight(1f))
                                    Text("${te.entry.sets} × ${te.entry.reps}", style = MaterialTheme.typography.labelMedium, color = Forge.Text2)
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PrimaryButton("Start", {
                                    scope.launch { nav.navigate(Routes.workout(c.repo.startWorkout(t.template.id))) }
                                }, Modifier.weight(1f), icon = Icons.Rounded.PlayArrow, height = 46.dp)
                                SecondaryButton("Edit", { nav.navigate(Routes.template(t.template.id)) }, Modifier.weight(1f), icon = Icons.Rounded.Edit, height = 46.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating) {
        CreateExerciseDialog(onDismiss = { creating = false }, onCreate = { n, m, e -> c.launch { createExercise(n, m, e) } })
    }
}
