package com.forge.gym.domain

import com.forge.gym.data.SetEntity
import com.forge.gym.data.WeightUnit
import com.forge.gym.data.WorkoutFull
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** Epley estimated one-rep max. */
fun e1rm(weight: Double, reps: Int): Double = when {
    reps <= 0 || weight <= 0 -> 0.0
    reps == 1 -> weight
    else -> weight * (1 + reps / 30.0)
}

val SetEntity.counts: Boolean get() = completed && !isWarmup

fun WorkoutFull.workingSets(): List<SetEntity> = exercises.flatMap { it.sets }.filter { it.counts }
fun WorkoutFull.volumeKg(): Double = workingSets().sumOf { it.weightKg * it.reps }
fun WorkoutFull.setCount(): Int = workingSets().size
fun WorkoutFull.durationMs(): Long = ((workout.endTime ?: System.currentTimeMillis()) - workout.startTime).coerceAtLeast(0)
val WorkoutFull.date: LocalDate get() = workout.startTime.toLocalDate()

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
fun LocalDate.startMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
fun LocalDate.weekStart(): LocalDate = with(DayOfWeek.MONDAY)

data class ExerciseSession(val workoutId: Long, val workoutName: String, val date: Long, val sets: List<SetEntity>) {
    val working = sets.filter { it.counts }
    val maxWeight = working.maxOfOrNull { it.weightKg } ?: 0.0
    val bestE1rm = working.maxOfOrNull { e1rm(it.weightKg, it.reps) } ?: 0.0
    val volume = working.sumOf { it.weightKg * it.reps }
    val totalReps = working.sumOf { it.reps }
    val bestSet = working.maxByOrNull { e1rm(it.weightKg, it.reps) }
}

/** Sessions for an exercise, oldest first. */
fun sessionsFor(exerciseId: Long, workouts: List<WorkoutFull>): List<ExerciseSession> =
    workouts.flatMap { w ->
        w.exercises.filter { it.exercise.id == exerciseId }
            .map { ExerciseSession(w.workout.id, w.workout.name, w.workout.startTime, it.orderedSets) }
    }.filter { it.working.isNotEmpty() }.sortedBy { it.date }

enum class PrKind(val label: String) { WEIGHT("Heaviest weight"), E1RM("Best est. 1RM"), VOLUME("Most volume") }

data class PrHit(val exerciseName: String, val kind: PrKind, val valueKg: Double, val reps: Int = 0)

fun prsInWorkout(target: WorkoutFull, all: List<WorkoutFull>): List<PrHit> {
    val prior = all.filter { it.workout.startTime < target.workout.startTime && it.workout.id != target.workout.id }
    return target.orderedExercises.flatMap { ex ->
        val session = ExerciseSession(target.workout.id, target.workout.name, target.workout.startTime, ex.orderedSets)
        if (session.working.isEmpty()) return@flatMap emptyList()
        val past = sessionsFor(ex.exercise.id, prior)
        if (past.isEmpty()) return@flatMap emptyList()
        buildList {
            val heaviest = session.working.maxBy { it.weightKg }
            if (session.maxWeight > past.maxOf { it.maxWeight } + 1e-6) add(PrHit(ex.exercise.name, PrKind.WEIGHT, heaviest.weightKg, heaviest.reps))
            val best = session.bestSet
            if (best != null && session.bestE1rm > past.maxOf { it.bestE1rm } + 1e-6) add(PrHit(ex.exercise.name, PrKind.E1RM, session.bestE1rm, best.reps))
            if (session.volume > past.maxOf { it.volume } + 1e-6) add(PrHit(ex.exercise.name, PrKind.VOLUME, session.volume))
        }
    }
}

/** Consecutive weeks (Mon-Sun) with at least one workout, counting back from this week. */
fun weekStreak(workouts: List<WorkoutFull>, today: LocalDate = LocalDate.now()): Int {
    val weeks = workouts.map { it.date.weekStart() }.toSet()
    var w = today.weekStart()
    if (w !in weeks) w = w.minusWeeks(1)
    var count = 0
    while (w in weeks) {
        count++
        w = w.minusWeeks(1)
    }
    return count
}

// ---------- Formatting ----------

fun plural(n: Int, word: String) = "$n $word${if (n == 1) "" else "s"}"

fun trimNum(v: Double, decimals: Int = 1): String {
    if (abs(v - v.roundToLong()) < 0.005) return v.roundToLong().toString()
    val s = String.format(Locale.US, "%.${decimals}f", v)
    return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
}

fun formatWeight(kg: Double, unit: WeightUnit, withUnit: Boolean = true): String {
    val s = trimNum(unit.fromKg(kg), 1)
    return if (withUnit) "$s ${unit.label}" else s
}

fun formatInputWeight(kg: Double, unit: WeightUnit): String =
    if (kg == 0.0) "" else trimNum(unit.fromKg(kg), 2)

fun formatBig(v: Double): String = when {
    v >= 1_000_000 -> trimNum(v / 1_000_000, 2) + "M"
    v >= 10_000 -> trimNum(v / 1000, 1) + "k"
    else -> String.format(Locale.US, "%,d", v.roundToLong())
}

fun formatVolume(kg: Double, unit: WeightUnit): String = formatBig(unit.fromKg(kg))

fun formatDuration(ms: Long): String {
    val totalMin = ms / 60_000
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
}
