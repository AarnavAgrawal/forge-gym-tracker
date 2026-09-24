package com.forge.gym.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.forge.gym.data.SetEntity
import com.forge.gym.data.WeightUnit
import com.forge.gym.data.WorkoutExerciseWithSets
import com.forge.gym.domain.formatClock
import com.forge.gym.domain.formatInputWeight
import com.forge.gym.domain.formatWeight
import com.forge.gym.domain.toLocalDate
import com.forge.gym.ui.LocalContainer
import com.forge.gym.ui.Routes
import com.forge.gym.ui.components.CircleIconButton
import com.forge.gym.ui.components.ConfirmDialog
import com.forge.gym.ui.components.ExercisePickerSheet
import com.forge.gym.ui.components.ForgeCard
import com.forge.gym.ui.components.MuscleTag
import com.forge.gym.ui.components.Pill
import com.forge.gym.ui.components.PrimaryButton
import com.forge.gym.ui.components.SecondaryButton
import com.forge.gym.ui.components.TextInputDialog
import com.forge.gym.ui.components.bounceClick
import com.forge.gym.ui.rememberSettings
import com.forge.gym.ui.theme.Forge
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditorScreen(nav: NavController, id: Long) {
    val c = LocalContainer.current
    val flow = remember(id) { c.repo.workout(id) }
    val full by flow.collectAsState(initial = null)
    val settings = rememberSettings()
    val unit = settings.unit
    val rest by c.restTimer.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val focus = LocalFocusManager.current

    var leaving by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf<String?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var editDuration by remember { mutableStateOf(false) }
    val previous = remember { mutableStateMapOf<Long, List<SetEntity>>() }

    LaunchedEffect(full) {
        if (full != null) loaded = true
        else if (loaded && !leaving) nav.popBackStack()
    }
    val w = full
    if (w == null) {
        Box(Modifier.fillMaxSize().background(Forge.Bg))
        return
    }
    val isLive = w.workout.endTime == null
    val exercises = w.orderedExercises

    LaunchedEffect(exercises.map { it.exercise.id }) {
        exercises.forEach { e ->
            if (e.exercise.id !in previous) previous[e.exercise.id] = c.repo.previousSets(e.exercise.id, id)
        }
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isLive, rest) {
        while (isLive || rest != null) {
            now = System.currentTimeMillis()
            delay(if (rest != null) 200 else 1000)
        }
    }

    val allSets = exercises.flatMap { it.sets }
    val doneCount = allSets.count { it.completed }
    val progress by animateFloatAsState(
        if (allSets.isEmpty()) 0f else doneCount.toFloat() / allSets.size,
        spring(dampingRatio = 0.8f, stiffness = 200f), label = "progress",
    )

    fun finishNow() {
        leaving = true
        focus.clearFocus()
        scope.launch {
            val ok = c.repo.finishWorkout(id)
            if (isLive) {
                c.restTimer.skip()
                if (ok) nav.navigate(Routes.summary(id)) { popUpTo(Routes.WORKOUT) { inclusive = true } }
                else nav.popBackStack()
            } else {
                nav.popBackStack()
            }
        }
    }

    fun requestFinish() {
        if (!isLive) { finishNow(); return }
        val incomplete = allSets.count { !it.completed }
        confirmFinish = when {
            doneCount == 0 -> "You haven't completed any sets. Finishing will discard this workout."
            incomplete > 0 -> "$incomplete unfinished set${if (incomplete == 1) "" else "s"} will be removed. Finish anyway?"
            else -> null
        }
        if (confirmFinish == null) finishNow()
    }

    BackHandler {
        if (isLive) nav.popBackStack() else finishNow()
    }

    Box(Modifier.fillMaxSize().background(Forge.Bg)) {
        Column(Modifier.fillMaxSize().imePadding()) {
            // Top bar
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(Icons.Rounded.KeyboardArrowDown, { if (isLive) nav.popBackStack() else finishNow() }, contentDescription = "Minimize")
                Spacer(Modifier.weight(1f))
                if (isLive) {
                    Row(
                        Modifier.clip(RoundedCornerShape(14.dp)).background(Forge.Surface2).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Timer, null, tint = Forge.Lime, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(formatClock(now - w.workout.startTime), style = MaterialTheme.typography.titleSmall, color = Forge.Text1)
                    }
                } else {
                    Pill("Editing", color = Forge.Surface2, textColor = Forge.Text2)
                }
                Spacer(Modifier.weight(1f))
                PrimaryButton(if (isLive) "Finish" else "Save", { requestFinish() }, height = 42.dp)
            }

            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = if (rest != null) 150.dp else 48.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "header") {
                    Column(Modifier.padding(horizontal = 4.dp)) {
                        Row(Modifier.clickable { rename = true }, verticalAlignment = Alignment.CenterVertically) {
                            Text(w.workout.name, style = MaterialTheme.typography.headlineLarge, color = Forge.Text1, modifier = Modifier.weight(1f, fill = false))
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Rounded.Edit, "Rename", tint = Forge.Text3, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            val start = Instant.ofEpochMilli(w.workout.startTime).atZone(ZoneId.systemDefault())
                            if (isLive) {
                                Pill("Started ${start.format(DateTimeFormatter.ofPattern("h:mm a"))}")
                            } else {
                                Pill(start.format(DateTimeFormatter.ofPattern("EEE, MMM d")), Modifier.bounceClick { pickDate = true }, textColor = Forge.Text1)
                                Pill("${((w.workout.endTime!! - w.workout.startTime) / 60000)} min", Modifier.bounceClick { editDuration = true }, textColor = Forge.Text1)
                            }
                            Pill(if (w.workout.notes.isBlank()) "+ Notes" else "Notes", Modifier.bounceClick { notes = true }, textColor = Forge.Lime)
                        }
                        if (w.workout.notes.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(w.workout.notes, style = MaterialTheme.typography.bodyMedium, color = Forge.Text2)
                        }
                        if (allSets.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(Forge.Surface2)) {
                                    Box(Modifier.fillMaxHeight().fillMaxWidth(progress).clip(CircleShape).background(Forge.Lime))
                                }
                                Spacer(Modifier.width(10.dp))
                                Text("$doneCount/${allSets.size} sets", style = MaterialTheme.typography.labelMedium, color = Forge.Text2)
                            }
                        }
                    }
                }

                items(exercises, key = { it.entry.id }) { e ->
                    ExerciseEditorCard(
                        e = e,
                        index = exercises.indexOf(e),
                        count = exercises.size,
                        unit = unit,
                        previous = previous[e.exercise.id].orEmpty(),
                        isLive = isLive,
                        modifier = Modifier.animateItem(),
                        onOpenHistory = { nav.navigate(Routes.exercise(e.exercise.id)) },
                        onMove = { d -> c.launch { moveExercise(id, e.entry.id, d) } },
                        onRemove = { c.launch { removeExercise(e.entry.id) } },
                        onAddSet = { c.launch { addSet(e.entry.id, completed = !isLive) } },
                        onDelete = { s -> c.launch { deleteSet(s.id) } },
                        onCompleted = { s ->
                            focus.clearFocus()
                            c.launch { fillForward(s) }
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isLive && settings.autoRest && !s.isWarmup) c.restTimer.start(settings.restSeconds)
                        },
                    )
                }

                item(key = "actions") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                        if (exercises.isEmpty()) {
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).border(1.dp, Forge.StrokeStrong, RoundedCornerShape(24.dp)).padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("Empty workout", style = MaterialTheme.typography.titleMedium, color = Forge.Text1)
                                Spacer(Modifier.height(4.dp))
                                Text("Add your first exercise to start logging sets.", style = MaterialTheme.typography.bodyMedium, color = Forge.Text2, textAlign = TextAlign.Center)
                            }
                        }
                        PrimaryButton("Add exercises", { showPicker = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Add)
                        SecondaryButton(
                            if (isLive) "Discard workout" else "Delete workout",
                            { confirmDiscard = true }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Delete, contentColor = Forge.Coral, height = 48.dp,
                        )
                        Spacer(Modifier.navigationBarsPadding())
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = rest != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(dampingRatio = 0.75f, stiffness = 300f)) { it } + fadeIn(),
            exit = slideOutVertically(tween(250)) { it } + fadeOut(),
        ) {
            val r = rest
            if (r != null) RestTimerBar(r.endAt - now, r.totalMs, onAdjust = { c.restTimer.adjust(it) }, onSkip = { c.restTimer.skip() })
        }
    }

    if (showPicker) {
        ExercisePickerSheet(onDismiss = { showPicker = false }, onPick = { ids -> c.launch { addExercises(id, ids, completed = !isLive) } })
    }
    confirmFinish?.let { msg ->
        ConfirmDialog("Finish workout?", msg, "Finish", onConfirm = { finishNow() }, onDismiss = { confirmFinish = null })
    }
    if (confirmDiscard) {
        ConfirmDialog(
            if (isLive) "Discard workout?" else "Delete workout?",
            "All sets in this workout will be deleted. This can't be undone.",
            if (isLive) "Discard" else "Delete",
            onConfirm = {
                leaving = true
                scope.launch {
                    c.restTimer.skip()
                    c.repo.deleteWorkout(id)
                    nav.popBackStack()
                }
            },
            onDismiss = { confirmDiscard = false },
            destructive = true,
        )
    }
    if (rename) {
        TextInputDialog("Rename workout", w.workout.name, "Workout name", onConfirm = { n -> c.launch { updateWorkoutInfo(id, name = n) } }, onDismiss = { rename = false })
    }
    if (notes) {
        TextInputDialog(
            "Notes", w.workout.notes, "How did it feel?",
            onConfirm = { n -> c.launch { updateWorkoutInfo(id, notes = n) } }, onDismiss = { notes = false }, singleLine = false,
        )
    }
    if (editDuration && w.workout.endTime != null) {
        TextInputDialog(
            "Duration (minutes)", ((w.workout.endTime - w.workout.startTime) / 60000).toString(), "Minutes",
            onConfirm = { v -> v.toIntOrNull()?.let { m -> c.launch { changeWorkoutDuration(id, m) } } },
            onDismiss = { editDuration = false },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }
    if (pickDate) {
        val initial = w.workout.startTime.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        val colors = DatePickerDefaults.colors(
            containerColor = Forge.Surface,
            selectedDayContainerColor = Forge.Lime,
            selectedDayContentColor = Forge.OnLime,
            todayDateBorderColor = Forge.Lime,
            todayContentColor = Forge.Lime,
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            colors = colors,
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        if (!d.isAfter(LocalDate.now())) c.launch { changeWorkoutDate(id, d) }
                    }
                    pickDate = false
                }) { Text("OK", color = Forge.Lime) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel", color = Forge.Text2) } },
        ) { DatePicker(state, colors = colors) }
    }
}

@Composable
private fun ExerciseEditorCard(
    e: WorkoutExerciseWithSets,
    index: Int,
    count: Int,
    unit: WeightUnit,
    previous: List<SetEntity>,
    isLive: Boolean,
    modifier: Modifier,
    onOpenHistory: () -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    onAddSet: () -> Unit,
    onDelete: (SetEntity) -> Unit,
    onCompleted: (SetEntity) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    ForgeCard(modifier.fillMaxWidth(), padding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(onClick = onOpenHistory)) {
                Text(e.exercise.name, style = MaterialTheme.typography.titleMedium, color = Forge.Lime)
                Spacer(Modifier.height(4.dp))
                MuscleTag(e.exercise.muscleGroup)
            }
            Box {
                CircleIconButton(Icons.Rounded.MoreHoriz, { menu = true }, size = 36.dp, contentDescription = "Exercise options")
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("History & charts") }, leadingIcon = { Icon(Icons.Rounded.Insights, null) }, onClick = { menu = false; onOpenHistory() })
                    if (index > 0) DropdownMenuItem(text = { Text("Move up") }, leadingIcon = { Icon(Icons.Rounded.ArrowUpward, null) }, onClick = { menu = false; onMove(-1) })
                    if (index < count - 1) DropdownMenuItem(text = { Text("Move down") }, leadingIcon = { Icon(Icons.Rounded.ArrowDownward, null) }, onClick = { menu = false; onMove(1) })
                    DropdownMenuItem(
                        text = { Text("Remove exercise", color = Forge.Coral) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = Forge.Coral) },
                        onClick = { menu = false; onRemove() },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            HeaderText("SET", Modifier.width(36.dp))
            HeaderText("PREVIOUS", Modifier.weight(1.3f))
            HeaderText(unit.label.uppercase(), Modifier.weight(1f))
            HeaderText("REPS", Modifier.weight(0.85f))
            HeaderText("", Modifier.width(40.dp))
        }
        Spacer(Modifier.height(6.dp))
        val sets = e.orderedSets
        var working = 0
        sets.forEachIndexed { i, s ->
            val label = if (s.isWarmup) "W" else (++working).toString()
            val prev = previous.getOrNull(i)
            key(s.id) {
                SwipeableSetRow(s, label, prev, unit, onDelete, onCompleted)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(12.dp)).background(Forge.Surface2).bounceClick(scaleDown = 0.98f, onClick = onAddSet),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, null, tint = Forge.Text1, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add set", style = MaterialTheme.typography.labelLarge, color = Forge.Text1)
        }
    }
}

@Composable
private fun HeaderText(text: String, modifier: Modifier) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = Forge.Text3, textAlign = TextAlign.Center, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableSetRow(
    s: SetEntity,
    label: String,
    prev: SetEntity?,
    unit: WeightUnit,
    onDelete: (SetEntity) -> Unit,
    onCompleted: (SetEntity) -> Unit,
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            if (v == SwipeToDismissBoxValue.EndToStart) { onDelete(s); true } else false
        },
        positionalThreshold = { it * 0.4f },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val active = state.targetValue == SwipeToDismissBoxValue.EndToStart
            val swiping = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
            val bg by animateColorAsState(
                when { active -> Forge.Coral; swiping -> Forge.Coral.copy(alpha = 0.35f); else -> Color.Transparent }, label = "swipe",
            )
            Box(
                Modifier.fillMaxSize().padding(vertical = 2.dp).clip(RoundedCornerShape(12.dp)).background(bg).padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) { if (swiping) Icon(Icons.Rounded.Delete, "Delete set", tint = Color.White) }
        },
    ) {
        SetRow(s, label, prev, unit, onCompleted)
    }
}

@Composable
private fun SetRow(
    s: SetEntity,
    label: String,
    prev: SetEntity?,
    unit: WeightUnit,
    onCompleted: (SetEntity) -> Unit,
) {
    val c = LocalContainer.current
    // Local copy of the numbers: the source of truth while editing, synced from the DB otherwise.
    var weightKg by remember(s.id) { mutableDoubleStateOf(s.weightKg) }
    var reps by remember(s.id) { mutableIntStateOf(s.reps) }
    var editing by remember { mutableStateOf(false) }
    LaunchedEffect(s.weightKg, s.reps, editing) {
        if (!editing) {
            weightKg = s.weightKg
            reps = s.reps
        }
    }
    val bg by animateColorAsState(if (s.completed) Color(0xFF1C2410) else Forge.Surface, tween(250), label = "rowBg")
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp).height(44.dp).clip(RoundedCornerShape(12.dp)).background(bg).padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.width(36.dp).height(30.dp).clip(RoundedCornerShape(9.dp))
                .clickable { c.launch { setWarmup(s.id, !s.isWarmup) } },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = if (s.isWarmup) Forge.Amber else Forge.Text2,
            )
        }
        Text(
            if (prev != null) "${formatWeight(prev.weightKg, unit, withUnit = false)} × ${prev.reps}" else "—",
            style = MaterialTheme.typography.bodySmall,
            color = Forge.Text3,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(1.3f).clip(RoundedCornerShape(8.dp)).clickable(enabled = prev != null) {
                if (prev != null) {
                    weightKg = prev.weightKg
                    reps = prev.reps
                    c.launch { setValues(s.id, prev.weightKg, prev.reps, s.completed) }
                }
            }.padding(vertical = 8.dp),
        )
        NumberCell(
            value = unit.fromKg(weightKg),
            format = { v -> formatInputWeight(unit.toKg(v), unit) },
            decimal = true,
            done = s.completed,
            onFocus = { editing = it },
            modifier = Modifier.weight(1f).padding(horizontal = 3.dp),
        ) { v ->
            weightKg = unit.toKg(v)
            c.launch { setWeight(s.id, unit.toKg(v)) }
        }
        NumberCell(
            value = reps.toDouble(),
            format = { v -> if (v == 0.0) "" else v.toInt().toString() },
            decimal = false,
            done = s.completed,
            onFocus = { editing = it },
            modifier = Modifier.weight(0.85f).padding(horizontal = 3.dp),
        ) { v ->
            reps = v.toInt()
            c.launch { setReps(s.id, v.toInt()) }
        }
        CheckButton(s.completed) {
            val done = !s.completed
            var w = weightKg
            var r = reps
            if (done && prev != null) {
                if (w == 0.0) w = prev.weightKg
                if (r == 0) r = prev.reps
            }
            weightKg = w
            reps = r
            c.launch { setValues(s.id, w, r, done) }
            if (done) onCompleted(s.copy(weightKg = w, reps = r, completed = true))
        }
    }
}

@Composable
private fun NumberCell(
    value: Double,
    format: (Double) -> String,
    decimal: Boolean,
    done: Boolean,
    onFocus: (Boolean) -> Unit,
    modifier: Modifier,
    onValue: (Double) -> Unit,
) {
    var text by remember { mutableStateOf(format(value)) }
    var focused by remember { mutableStateOf(false) }
    // Outside changes (copy previous, fill-forward) only apply while the user isn't typing here.
    LaunchedEffect(value, focused) {
        val parsed = text.toDoubleOrNull() ?: 0.0
        if (!focused && abs(parsed - value) > 0.001) text = format(value)
    }
    BasicTextField(
        value = text,
        onValueChange = { raw ->
            val filtered = if (decimal) {
                val f = raw.filter { it.isDigit() || it == '.' }
                if (f.count { it == '.' } > 1) text else f.take(7)
            } else raw.filter { it.isDigit() }.take(3)
            text = filtered
            onValue(filtered.toDoubleOrNull() ?: 0.0)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = ImeAction.Next,
        ),
        textStyle = MaterialTheme.typography.titleSmall.copy(color = Forge.Text1, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(Forge.Lime),
        modifier = modifier.onFocusChanged {
            focused = it.isFocused
            onFocus(it.isFocused)
        },
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (done) Color.Transparent else Forge.Surface2),
                contentAlignment = Alignment.Center,
            ) {
                if (text.isEmpty()) Text("0", style = MaterialTheme.typography.titleSmall, color = Forge.Text3)
                inner()
            }
        },
    )
}

@Composable
private fun CheckButton(checked: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (checked) Forge.Lime else Forge.Surface3, tween(200), label = "chkBg")
    val pop = remember { Animatable(1f) }
    LaunchedEffect(checked) {
        if (checked) {
            pop.snapTo(0.7f)
            pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f))
        }
    }
    Box(
        Modifier.padding(start = 4.dp).size(36.dp).scale(pop.value).clip(RoundedCornerShape(11.dp)).background(bg).bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Check, if (checked) "Completed" else "Mark complete", tint = if (checked) Forge.OnLime else Forge.Text3, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun RestTimerBar(remainingMs: Long, totalMs: Long, onAdjust: (Int) -> Unit, onSkip: () -> Unit) {
    val fraction = (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f)
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Forge.Surface3)
            .border(1.dp, Forge.Lime.copy(alpha = 0.3f), RoundedCornerShape(26.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(52.dp)) {
                val st = 5.dp.toPx()
                drawArc(Forge.Surface2, -90f, 360f, false, topLeft = androidx.compose.ui.geometry.Offset(st / 2, st / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - st, size.height - st), style = Stroke(st))
                drawArc(Forge.Lime, -90f, 360f * fraction, false, topLeft = androidx.compose.ui.geometry.Offset(st / 2, st / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - st, size.height - st), style = Stroke(st, cap = StrokeCap.Round))
            }
            Icon(Icons.Rounded.Timer, null, tint = Forge.Lime, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("REST", style = MaterialTheme.typography.labelSmall, color = Forge.Lime)
            Text(formatClock(remainingMs + 999), style = MaterialTheme.typography.headlineMedium, color = Forge.Text1)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallTimerButton("-15") { onAdjust(-15) }
            SmallTimerButton("+15") { onAdjust(15) }
            SmallTimerButton("Skip", highlight = true, onClick = onSkip)
        }
    }
}

@Composable
private fun SmallTimerButton(text: String, highlight: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (highlight) Forge.OnLime else Forge.Text1,
        modifier = Modifier.bounceClick(onClick = onClick).clip(RoundedCornerShape(12.dp))
            .background(if (highlight) Forge.Lime else Forge.Surface2).padding(horizontal = 11.dp, vertical = 9.dp),
    )
}
