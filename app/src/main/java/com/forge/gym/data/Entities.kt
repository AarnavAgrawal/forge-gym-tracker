package com.forge.gym.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
    val isCustom: Boolean = false,
    val notes: String = "",
)

/** A workout session. [endTime] == null means it is currently in progress. */
@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startTime: Long,
    val endTime: Long? = null,
    val notes: String = "",
)

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(WorkoutEntity::class, ["id"], ["workoutId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ExerciseEntity::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("workoutId"), Index("exerciseId")],
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val position: Int,
    val notes: String = "",
)

/** One set. Weight is always stored in kilograms. */
@Entity(
    tableName = "sets",
    foreignKeys = [
        ForeignKey(WorkoutExerciseEntity::class, ["id"], ["workoutExerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("workoutExerciseId")],
)
data class SetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long,
    val position: Int,
    val weightKg: Double = 0.0,
    val reps: Int = 0,
    val completed: Boolean = false,
    val isWarmup: Boolean = false,
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String = "",
    val lastUsed: Long? = null,
)

@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(TemplateEntity::class, ["id"], ["templateId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ExerciseEntity::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("templateId"), Index("exerciseId")],
)
data class TemplateExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val exerciseId: Long,
    val position: Int,
    val sets: Int = 3,
    val reps: Int = 10,
)

@Entity(tableName = "body_weight")
data class BodyWeightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val weightKg: Double,
)

// ---------- Relations ----------

data class WorkoutExerciseWithSets(
    @Embedded val entry: WorkoutExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutExerciseId")
    val sets: List<SetEntity>,
) {
    val orderedSets: List<SetEntity> get() = sets.sortedBy { it.position }
}

data class WorkoutFull(
    @Embedded val workout: WorkoutEntity,
    @Relation(entity = WorkoutExerciseEntity::class, parentColumn = "id", entityColumn = "workoutId")
    val exercises: List<WorkoutExerciseWithSets>,
) {
    val orderedExercises: List<WorkoutExerciseWithSets> get() = exercises.sortedBy { it.entry.position }
}

data class TemplateExerciseWithExercise(
    @Embedded val entry: TemplateExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
)

data class TemplateFull(
    @Embedded val template: TemplateEntity,
    @Relation(entity = TemplateExerciseEntity::class, parentColumn = "id", entityColumn = "templateId")
    val exercises: List<TemplateExerciseWithExercise>,
) {
    val orderedExercises: List<TemplateExerciseWithExercise> get() = exercises.sortedBy { it.entry.position }
}
