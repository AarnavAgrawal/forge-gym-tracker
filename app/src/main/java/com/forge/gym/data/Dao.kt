package com.forge.gym.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {
    // ---- Exercises ----
    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    fun exercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises")
    suspend fun allExercises(): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun exerciseCount(): Int

    @Insert
    suspend fun insertExercise(e: ExerciseEntity): Long

    @Insert
    suspend fun insertExercises(e: List<ExerciseEntity>)

    @Update
    suspend fun updateExercise(e: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExercise(id: Long)

    // ---- Workouts ----
    @Transaction
    @Query("SELECT * FROM workouts WHERE endTime IS NOT NULL ORDER BY startTime DESC")
    fun finishedWorkouts(): Flow<List<WorkoutFull>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    fun workout(id: Long): Flow<WorkoutFull?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun workoutFullNow(id: Long): WorkoutFull?

    @Query("SELECT * FROM workouts WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    fun activeWorkout(): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workouts WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun activeWorkoutNow(): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun workoutNow(id: Long): WorkoutEntity?

    @Insert
    suspend fun insertWorkout(w: WorkoutEntity): Long

    @Update
    suspend fun updateWorkout(w: WorkoutEntity)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Long)

    // ---- Workout exercises ----
    @Insert
    suspend fun insertWorkoutExercise(e: WorkoutExerciseEntity): Long

    @Update
    suspend fun updateWorkoutExercises(e: List<WorkoutExerciseEntity>)

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteWorkoutExercise(id: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun maxExercisePosition(workoutId: Long): Int

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY position")
    suspend fun workoutExercises(workoutId: Long): List<WorkoutExerciseEntity>

    // ---- Sets ----
    @Insert
    suspend fun insertSet(s: SetEntity): Long

    @Update
    suspend fun updateSet(s: SetEntity)

    @Query("DELETE FROM sets WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("UPDATE sets SET weightKg = :weightKg WHERE id = :id")
    suspend fun setWeight(id: Long, weightKg: Double)

    @Query("UPDATE sets SET reps = :reps WHERE id = :id")
    suspend fun setReps(id: Long, reps: Int)

    @Query("UPDATE sets SET isWarmup = :warmup WHERE id = :id")
    suspend fun setWarmup(id: Long, warmup: Boolean)

    @Query("UPDATE sets SET weightKg = :weightKg, reps = :reps, completed = :completed WHERE id = :id")
    suspend fun setValues(id: Long, weightKg: Double, reps: Int, completed: Boolean)

    @Query("SELECT * FROM sets WHERE workoutExerciseId = :weId ORDER BY position")
    suspend fun setsFor(weId: Long): List<SetEntity>

    @Query("DELETE FROM sets WHERE workoutExerciseId IN (SELECT id FROM workout_exercises WHERE workoutId = :workoutId) AND completed = 0")
    suspend fun deleteIncompleteSets(workoutId: Long)

    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId AND id NOT IN (SELECT workoutExerciseId FROM sets)")
    suspend fun deleteEmptyExercises(workoutId: Long)

    /** Sets from the most recent finished workout (other than [excludeWorkoutId]) containing this exercise. */
    @Query(
        """
        SELECT s.* FROM sets s
        WHERE s.workoutExerciseId = (
            SELECT we2.id FROM workout_exercises we2
            JOIN workouts w ON w.id = we2.workoutId
            WHERE we2.exerciseId = :exerciseId AND w.endTime IS NOT NULL AND w.id != :excludeWorkoutId
            ORDER BY w.startTime DESC LIMIT 1
        )
        ORDER BY s.position
        """
    )
    suspend fun previousSets(exerciseId: Long, excludeWorkoutId: Long): List<SetEntity>

    // ---- Templates ----
    @Transaction
    @Query("SELECT * FROM templates ORDER BY COALESCE(lastUsed, 0) DESC, name")
    fun templates(): Flow<List<TemplateFull>>

    @Transaction
    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun templateNow(id: Long): TemplateFull?

    @Insert
    suspend fun insertTemplate(t: TemplateEntity): Long

    @Update
    suspend fun updateTemplate(t: TemplateEntity)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    @Insert
    suspend fun insertTemplateExercises(e: List<TemplateExerciseEntity>)

    @Query("DELETE FROM template_exercises WHERE templateId = :templateId")
    suspend fun clearTemplateExercises(templateId: Long)

    // ---- Body weight ----
    @Query("SELECT * FROM body_weight ORDER BY date ASC")
    fun bodyWeights(): Flow<List<BodyWeightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyWeight(b: BodyWeightEntity): Long

    @Query("DELETE FROM body_weight WHERE id = :id")
    suspend fun deleteBodyWeight(id: Long)

    // ---- Backup ----
    @Query("SELECT * FROM workouts") suspend fun allWorkoutsRaw(): List<WorkoutEntity>
    @Query("SELECT * FROM workout_exercises") suspend fun allWorkoutExercisesRaw(): List<WorkoutExerciseEntity>
    @Query("SELECT * FROM sets") suspend fun allSetsRaw(): List<SetEntity>
    @Query("SELECT * FROM templates") suspend fun allTemplatesRaw(): List<TemplateEntity>
    @Query("SELECT * FROM template_exercises") suspend fun allTemplateExercisesRaw(): List<TemplateExerciseEntity>
    @Query("SELECT * FROM body_weight") suspend fun allBodyWeightsRaw(): List<BodyWeightEntity>

    @Insert suspend fun insertWorkoutsRaw(l: List<WorkoutEntity>)
    @Insert suspend fun insertWorkoutExercisesRaw(l: List<WorkoutExerciseEntity>)
    @Insert suspend fun insertSetsRaw(l: List<SetEntity>)
    @Insert suspend fun insertTemplatesRaw(l: List<TemplateEntity>)
    @Insert suspend fun insertTemplateExercisesRaw(l: List<TemplateExerciseEntity>)
    @Insert suspend fun insertBodyWeightsRaw(l: List<BodyWeightEntity>)
}
