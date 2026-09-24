package com.forge.gym.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.forge.gym.domain.durationMs
import com.forge.gym.domain.formatBig
import com.forge.gym.domain.prsInWorkout
import com.forge.gym.domain.setCount
import com.forge.gym.domain.volumeKg
import com.forge.gym.domain.workingSets
import com.forge.gym.ui.LocalContainer
import com.forge.gym.ui.Routes
import com.forge.gym.ui.components.AnimatedNumber
import com.forge.gym.ui.components.Confetti
import com.forge.gym.ui.components.ExerciseSummaryRow
import com.forge.gym.ui.components.ForgeCard
import com.forge.gym.ui.components.PrimaryButton
import com.forge.gym.ui.components.SecondaryButton
import com.forge.gym.ui.components.TextInputDialog
import com.forge.gym.ui.components.staggerIn
import com.forge.gym.ui.rememberSettings
import com.forge.gym.ui.theme.Forge
import com.forge.gym.ui.vibrate

@Composable
fun SummaryScreen(nav: NavController, id: Long) {
    val c = LocalContainer.current
    val flow = remember(id) { c.repo.workout(id) }
    val workout by flow.collectAsState(initial = null)
    val all by c.repo.workouts.collectAsStateWithLifecycle()
    val unit = rememberSettings().unit
    var saveTemplate by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val badge = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        vibrate(context, longArrayOf(0, 40, 60, 40, 60, 120))
        badge.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 180f))
    }
    val glow = rememberInfiniteTransition(label = "glow")
    val glowScale by glow.animateFloat(1f, 1.18f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "g")

    val w = workout
    val prs = remember(w, all) { if (w == null) emptyList() else prsInWorkout(w, all.orEmpty()) }

    Box(Modifier.fillMaxSize().background(Forge.Bg)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(Modifier.statusBarsPadding().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(Modifier.size(132.dp).scale(glowScale * badge.value).clip(CircleShape).background(Forge.Lime.copy(alpha = 0.12f)))
                        Box(
                            Modifier.size(96.dp).scale(badge.value).clip(CircleShape).background(Forge.Lime),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (prs.isNotEmpty()) Icons.Rounded.EmojiEvents else Icons.Rounded.Check, null,
                                tint = Forge.OnLime,
                                modifier = Modifier.size(48.dp).graphicsLayer { rotationZ = (1f - badge.value) * -90f },
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Workout complete", style = MaterialTheme.typography.displaySmall, color = Forge.Text1, modifier = Modifier.staggerIn(1))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        w?.workout?.name ?: "",
                        style = MaterialTheme.typography.bodyLarge, color = Forge.Text2, modifier = Modifier.staggerIn(2),
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
            if (w != null) {
                item {
                    Column(Modifier.staggerIn(3), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BigStat("Minutes", w.durationMs() / 60000.0, { it.toInt().toString() }, Modifier.weight(1f))
                            BigStat("Volume · ${unit.label}", unit.fromKg(w.volumeKg()), { formatBig(it) }, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BigStat("Sets", w.setCount().toDouble(), { it.toInt().toString() }, Modifier.weight(1f))
                            BigStat("Reps", w.workingSets().sumOf { it.reps }.toDouble(), { it.toInt().toString() }, Modifier.weight(1f))
                        }
                    }
                }
                if (prs.isNotEmpty()) {
                    item { PrList(prs, unit, Modifier.padding(top = 12.dp).staggerIn(4)) }
                }
                item {
                    ForgeCard(Modifier.fillMaxWidth().padding(top = 12.dp).staggerIn(5)) {
                        Text("Exercises", style = MaterialTheme.typography.titleMedium, color = Forge.Text1)
                        Spacer(Modifier.height(8.dp))
                        w.orderedExercises.forEach { e -> ExerciseSummaryRow(e, unit) }
                    }
                }
            }
        }

        Confetti(Modifier.fillMaxSize())

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Forge.Bg.copy(alpha = 0.94f)).navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PrimaryButton("Done", { nav.popBackStack() }, Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Save as routine", { saveTemplate = true }, Modifier.weight(1f), icon = Icons.Rounded.BookmarkAdd, height = 48.dp)
                SecondaryButton("View details", {
                    nav.navigate(Routes.detail(id)) { popUpTo(Routes.SUMMARY) { inclusive = true } }
                }, Modifier.weight(1f), height = 48.dp)
            }
        }
    }

    if (saveTemplate && w != null) {
        TextInputDialog(
            "Save as routine", w.workout.name, "Routine name",
            onConfirm = { name -> c.launch { saveWorkoutAsTemplate(id, name) } }, onDismiss = { saveTemplate = false },
        )
    }
}

@Composable
private fun BigStat(label: String, value: Double, format: (Double) -> String, modifier: Modifier = Modifier) {
    ForgeCard(modifier, padding = PaddingValues(18.dp)) {
        AnimatedNumber(value, format, MaterialTheme.typography.headlineLarge, Forge.Text1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = Forge.Text2)
    }
}
