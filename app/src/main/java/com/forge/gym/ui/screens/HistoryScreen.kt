package com.forge.gym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.forge.gym.domain.date
import com.forge.gym.ui.LocalContainer
import com.forge.gym.ui.Routes
import com.forge.gym.ui.components.DetailTopBar
import com.forge.gym.ui.components.EmptyState
import com.forge.gym.ui.components.ForgeField
import com.forge.gym.ui.components.WorkoutCard
import com.forge.gym.ui.rememberSettings
import com.forge.gym.ui.theme.Forge
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(nav: NavController) {
    val c = LocalContainer.current
    val workouts by c.repo.workouts.collectAsStateWithLifecycle()
    val unit = rememberSettings().unit
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(workouts, query) {
        val q = query.trim().lowercase()
        workouts.orEmpty().filter { w ->
            q.isEmpty() || w.workout.name.lowercase().contains(q) || w.exercises.any { it.exercise.name.lowercase().contains(q) }
        }
    }
    val grouped = remember(filtered) { filtered.groupBy { YearMonth.from(it.date) } }

    Column(Modifier.fillMaxSize().background(Forge.Bg)) {
        DetailTopBar("History", onBack = { nav.popBackStack() })
        ForgeField(
            query, { query = it }, "Search workouts or exercises",
            Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            leading = { Icon(Icons.Rounded.Search, null, tint = Forge.Text3) },
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        ) {
            if (workouts != null && filtered.isEmpty()) {
                item { EmptyState(Icons.Rounded.History, "Nothing found", if (query.isEmpty()) "Finished workouts will appear here." else "Try a different search.") }
            }
            grouped.forEach { (month, list) ->
                item(key = "h$month") {
                    Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 16.dp, bottom = 8.dp).animateItem(), verticalAlignment = Alignment.Bottom) {
                        Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.titleLarge, color = Forge.Text1, modifier = Modifier.weight(1f))
                        Text(com.forge.gym.domain.plural(list.size, "workout"), style = MaterialTheme.typography.labelMedium, color = Forge.Text2)
                    }
                }
                items(list, key = { it.workout.id }) { w ->
                    WorkoutCard(w, unit, onClick = { nav.navigate(Routes.detail(w.workout.id)) }, modifier = Modifier.padding(vertical = 5.dp).animateItem())
                }
            }
            item { androidx.compose.foundation.layout.Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}
