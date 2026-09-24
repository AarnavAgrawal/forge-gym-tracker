package com.forge.gym.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.forge.gym.data.WeightUnit
import com.forge.gym.data.WorkoutExerciseWithSets
import com.forge.gym.domain.counts
import com.forge.gym.domain.e1rm
import com.forge.gym.domain.formatWeight
import com.forge.gym.ui.theme.Forge
import com.forge.gym.ui.theme.muscleColor

@Composable
fun ExerciseSummaryRow(e: WorkoutExerciseWithSets, unit: WeightUnit) {
    val working = e.sets.filter { it.counts }
    val best = working.maxByOrNull { e1rm(it.weightKg, it.reps) }
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(muscleColor(e.exercise.muscleGroup)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(e.exercise.name, style = MaterialTheme.typography.titleSmall, color = Forge.Text1)
            Text(com.forge.gym.domain.plural(working.size, "set"), style = MaterialTheme.typography.bodySmall, color = Forge.Text2)
        }
        if (best != null) {
            Text("${formatWeight(best.weightKg, unit)} × ${best.reps}", style = MaterialTheme.typography.titleSmall, color = Forge.Text1)
        }
    }
}
