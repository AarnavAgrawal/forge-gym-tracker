package com.forge.gym.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.forge.gym.data.WeightUnit
import com.forge.gym.data.WorkoutFull
import com.forge.gym.domain.date
import com.forge.gym.domain.durationMs
import com.forge.gym.domain.formatDuration
import com.forge.gym.domain.formatVolume
import com.forge.gym.domain.setCount
import com.forge.gym.domain.volumeKg
import com.forge.gym.ui.theme.Forge
import com.forge.gym.ui.theme.muscleColor
import java.time.format.DateTimeFormatter

private val monthShort = DateTimeFormatter.ofPattern("MMM")

@Composable
fun WorkoutCard(workout: WorkoutFull, unit: WeightUnit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val stats = remember(workout, unit) {
        Triple(formatDuration(workout.durationMs()), formatVolume(workout.volumeKg(), unit), workout.setCount())
    }
    val muscles = remember(workout) { workout.orderedExercises.map { it.exercise.muscleGroup }.distinct().take(5) }
    ForgeCard(modifier.fillMaxWidth(), onClick = onClick, padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(Forge.Surface2),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(workout.date.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge, color = Forge.Text1)
                Text(workout.date.format(monthShort).uppercase(), style = MaterialTheme.typography.labelSmall, color = Forge.Text2)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(workout.workout.name, style = MaterialTheme.typography.titleMedium, color = Forge.Text1, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${stats.first}  ·  ${stats.second} ${unit.label}  ·  ${com.forge.gym.domain.plural(stats.third, "set")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Forge.Text2,
                    maxLines = 1,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    muscles.forEach { Box(Modifier.size(7.dp).clip(CircleShape).background(muscleColor(it))) }
                    Spacer(Modifier.width(3.dp))
                    Text(
                        workout.orderedExercises.joinToString { it.exercise.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = Forge.Text3,
                        maxLines = 1,
                    )
                }
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Forge.Text3, modifier = Modifier.padding(start = 6.dp))
        }
    }
}
