package com.forge.gym.data

import androidx.room.withTransaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

data class TemplateItem(val exerciseId: Long, val sets: Int, val reps: Int)

class GymRepository(private val db: AppDatabase, scope: CoroutineScope) {
    private val dao = db.dao()

    val exercises: StateFlow<List<ExerciseEntity>> =
        dao.exercises().stateIn(scope, SharingStarted.Eagerly, emptyList())
    val workouts: StateFlow<List<WorkoutFull>?> =
        dao.finishedWorkouts().stateIn(scope, SharingStarted.Eagerly, null)
    val activeWorkout: StateFlow<WorkoutEntity?> =
        dao.activeWorkout().stateIn(scope, SharingStarted.Eagerly, null)
    val templates: StateFlow<List<TemplateFull>> =
        dao.templates().stateIn(scope, SharingStarted.Eagerly, emptyList())
    val bodyWeights: StateFlow<List<BodyWeightEntity>> =
        dao.bodyWeights().stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun workout(id: Long): Flow<WorkoutFull?> = dao.workout(id)

    suspend fun ensureSeeded() {
        if (dao.exerciseCount() > 0) return
        db.withTransaction {
            dao.insertExercises(Seed.exercises)
            val byName = dao.allExercises().associateBy { it.name }
            Seed.templates.forEach { (name, items) ->
                val tid = dao.insertTemplate(TemplateEntity(name = name))
                dao.insertTemplateExercises(items.mapIndexedNotNull { i, (ex, sets, reps) ->
                    byName[ex]?.let { TemplateExerciseEntity(templateId = tid, exerciseId = it.id, position = i, sets = sets, reps = reps) }
                })
            }
        }
    }

    private fun defaultName(time: LocalTime): String = when (time.hour) {
        in 5..11 -> "Morning Workout"
        in 12..16 -> "Afternoon Workout"
        in 17..21 -> "Evening Workout"
        else -> "Night Workout"
    }

    /** Starts a new workout (or returns the one already in progress). */
    suspend fun startWorkout(templateId: Long? = null): Long = db.withTransaction {
        dao.activeWorkoutNow()?.let { return@withTransaction it.id }
        val now = System.currentTimeMillis()
        val template = templateId?.let { dao.templateNow(it) }
        val name = template?.template?.name ?: defaultName(LocalTime.now())
        val wid = dao.insertWorkout(WorkoutEntity(name = name, startTime = now))
        template?.orderedExercises?.forEachIndexed { i, te ->
            val weId = dao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = wid, exerciseId = te.exercise.id, position = i))
            val prev = dao.previousSets(te.exercise.id, wid).filter { !it.isWarmup }
            repeat(te.entry.sets.coerceAtLeast(1)) { s ->
                val p = prev.getOrNull(s) ?: prev.lastOrNull()
                dao.insertSet(SetEntity(workoutExerciseId = weId, position = s, weightKg = p?.weightKg ?: 0.0, reps = te.entry.reps))
            }
        }
        template?.let { dao.updateTemplate(it.template.copy(lastUsed = now)) }
        wid
    }

    /** Creates an already-finished workout on a past date, to be filled in the editor. */
    suspend fun createPastWorkout(date: LocalDate): Long {
        val start = LocalDateTime.of(date, LocalTime.of(18, 0)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return dao.insertWorkout(WorkoutEntity(name = "Workout", startTime = start, endTime = start + 60 * 60 * 1000L))
    }

    suspend fun addExercises(workoutId: Long, exerciseIds: List<Long>, completed: Boolean) = db.withTransaction {
        var pos = dao.maxExercisePosition(workoutId) + 1
        exerciseIds.forEach { exId ->
            val weId = dao.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exId, position = pos++))
            val prev = dao.previousSets(exId, workoutId)
            if (prev.isEmpty()) {
                repeat(3) { dao.insertSet(SetEntity(workoutExerciseId = weId, position = it, completed = completed)) }
            } else {
                prev.take(8).forEachIndexed { i, p ->
                    dao.insertSet(SetEntity(workoutExerciseId = weId, position = i, weightKg = p.weightKg, reps = p.reps, isWarmup = p.isWarmup, completed = completed))
                }
            }
        }
    }

    suspend fun addSet(workoutExerciseId: Long, completed: Boolean) {
        val last = dao.setsFor(workoutExerciseId).lastOrNull()
        dao.insertSet(
            SetEntity(
                workoutExerciseId = workoutExerciseId,
                position = (last?.position ?: -1) + 1,
                weightKg = last?.weightKg ?: 0.0,
                reps = last?.reps ?: 0,
                completed = completed,
            )
        )
    }

    suspend fun updateSet(set: SetEntity) = dao.updateSet(set)

    // Field-level writes so fast edits to different fields never overwrite each other.
    suspend fun setWeight(id: Long, kg: Double) = dao.setWeight(id, kg)
    suspend fun setReps(id: Long, reps: Int) = dao.setReps(id, reps)
    suspend fun setWarmup(id: Long, warmup: Boolean) = dao.setWarmup(id, warmup)
    suspend fun setValues(id: Long, kg: Double, reps: Int, completed: Boolean) = dao.setValues(id, kg, reps, completed)

    /** Copies a just-completed set's numbers into the untouched sets after it. */
    suspend fun fillForward(set: SetEntity) {
        dao.setsFor(set.workoutExerciseId)
            .filter { it.position > set.position && !it.completed && it.weightKg == 0.0 && it.isWarmup == set.isWarmup }
            .forEach { dao.updateSet(it.copy(weightKg = set.weightKg, reps = if (it.reps == 0) set.reps else it.reps)) }
    }
    suspend fun deleteSet(id: Long) = dao.deleteSet(id)
    suspend fun removeExercise(workoutExerciseId: Long) = dao.deleteWorkoutExercise(workoutExerciseId)
    suspend fun previousSets(exerciseId: Long, workoutId: Long) = dao.previousSets(exerciseId, workoutId)

    suspend fun moveExercise(workoutId: Long, workoutExerciseId: Long, delta: Int) = db.withTransaction {
        val list = dao.workoutExercises(workoutId).toMutableList()
        val idx = list.indexOfFirst { it.id == workoutExerciseId }
        val target = idx + delta
        if (idx < 0 || target !in list.indices) return@withTransaction
        val item = list.removeAt(idx)
        list.add(target, item)
        dao.updateWorkoutExercises(list.mapIndexed { i, e -> e.copy(position = i) })
    }

    /** Drops unfinished sets and empty exercises. Returns false if nothing was left (workout deleted). */
    suspend fun finishWorkout(id: Long): Boolean = db.withTransaction {
        dao.deleteIncompleteSets(id)
        dao.deleteEmptyExercises(id)
        if (dao.workoutExercises(id).isEmpty()) {
            dao.deleteWorkout(id)
            false
        } else {
            val w = dao.workoutNow(id) ?: return@withTransaction false
            if (w.endTime == null) dao.updateWorkout(w.copy(endTime = System.currentTimeMillis()))
            true
        }
    }

    suspend fun deleteWorkout(id: Long) = dao.deleteWorkout(id)

    suspend fun updateWorkoutInfo(id: Long, name: String? = null, notes: String? = null) {
        val w = dao.workoutNow(id) ?: return
        dao.updateWorkout(w.copy(name = name?.ifBlank { w.name } ?: w.name, notes = notes ?: w.notes))
    }

    suspend fun changeWorkoutDate(id: Long, date: LocalDate) {
        val w = dao.workoutNow(id) ?: return
        val zone = ZoneId.systemDefault()
        val oldStart = java.time.Instant.ofEpochMilli(w.startTime).atZone(zone)
        val newStart = oldStart.with(date).toInstant().toEpochMilli()
        val shift = newStart - w.startTime
        dao.updateWorkout(w.copy(startTime = newStart, endTime = w.endTime?.plus(shift)))
    }

    suspend fun changeWorkoutDuration(id: Long, minutes: Int) {
        val w = dao.workoutNow(id) ?: return
        if (w.endTime == null) return
        dao.updateWorkout(w.copy(endTime = w.startTime + minutes.coerceAtLeast(1) * 60_000L))
    }

    suspend fun saveWorkoutAsTemplate(workoutId: Long, name: String): Long = db.withTransaction {
        val full = dao.workoutFullNow(workoutId) ?: return@withTransaction 0L
        val tid = dao.insertTemplate(TemplateEntity(name = name.ifBlank { full.workout.name }))
        dao.insertTemplateExercises(full.orderedExercises.mapIndexed { i, e ->
            val working = e.sets.filter { !it.isWarmup }
            val reps = working.groupingBy { it.reps }.eachCount().maxByOrNull { it.value }?.key ?: 10
            TemplateExerciseEntity(templateId = tid, exerciseId = e.exercise.id, position = i, sets = working.size.coerceAtLeast(1), reps = reps)
        })
        tid
    }

    suspend fun template(id: Long) = dao.templateNow(id)

    suspend fun saveTemplate(id: Long, name: String, items: List<TemplateItem>): Long = db.withTransaction {
        val tid = if (id == 0L) {
            dao.insertTemplate(TemplateEntity(name = name))
        } else {
            val existing = dao.templateNow(id)?.template ?: TemplateEntity(id = id, name = name)
            dao.updateTemplate(existing.copy(name = name))
            id
        }
        dao.clearTemplateExercises(tid)
        dao.insertTemplateExercises(items.mapIndexed { i, it ->
            TemplateExerciseEntity(templateId = tid, exerciseId = it.exerciseId, position = i, sets = it.sets, reps = it.reps)
        })
        tid
    }

    suspend fun deleteTemplate(id: Long) = dao.deleteTemplate(id)

    suspend fun createExercise(name: String, muscle: String, equipment: String): Long =
        dao.insertExercise(ExerciseEntity(name = name.trim(), muscleGroup = muscle, equipment = equipment, isCustom = true))

    suspend fun updateExercise(e: ExerciseEntity) = dao.updateExercise(e)
    suspend fun deleteExercise(id: Long) = dao.deleteExercise(id)

    suspend fun addBodyWeight(date: Long, kg: Double) = dao.insertBodyWeight(BodyWeightEntity(date = date, weightKg = kg))
    suspend fun deleteBodyWeight(id: Long) = dao.deleteBodyWeight(id)
}
