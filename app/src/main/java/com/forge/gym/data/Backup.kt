package com.forge.gym.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

/** Plain JSON export/import of the whole database. */
class Backup(private val db: AppDatabase) {
    private val dao = db.dao()

    suspend fun export(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("exercises", JSONArray(dao.allExercises().map {
            JSONObject().put("id", it.id).put("name", it.name).put("muscleGroup", it.muscleGroup)
                .put("equipment", it.equipment).put("isCustom", it.isCustom).put("notes", it.notes)
        }))
        root.put("workouts", JSONArray(dao.allWorkoutsRaw().map {
            JSONObject().put("id", it.id).put("name", it.name).put("startTime", it.startTime)
                .put("endTime", it.endTime ?: JSONObject.NULL).put("notes", it.notes)
        }))
        root.put("workoutExercises", JSONArray(dao.allWorkoutExercisesRaw().map {
            JSONObject().put("id", it.id).put("workoutId", it.workoutId).put("exerciseId", it.exerciseId)
                .put("position", it.position).put("notes", it.notes)
        }))
        root.put("sets", JSONArray(dao.allSetsRaw().map {
            JSONObject().put("id", it.id).put("workoutExerciseId", it.workoutExerciseId).put("position", it.position)
                .put("weightKg", it.weightKg).put("reps", it.reps).put("completed", it.completed).put("isWarmup", it.isWarmup)
        }))
        root.put("templates", JSONArray(dao.allTemplatesRaw().map {
            JSONObject().put("id", it.id).put("name", it.name).put("notes", it.notes).put("lastUsed", it.lastUsed ?: JSONObject.NULL)
        }))
        root.put("templateExercises", JSONArray(dao.allTemplateExercisesRaw().map {
            JSONObject().put("id", it.id).put("templateId", it.templateId).put("exerciseId", it.exerciseId)
                .put("position", it.position).put("sets", it.sets).put("reps", it.reps)
        }))
        root.put("bodyWeight", JSONArray(dao.allBodyWeightsRaw().map {
            JSONObject().put("id", it.id).put("date", it.date).put("weightKg", it.weightKg)
        }))
        return root.toString()
    }

    private inline fun <T> JSONArray?.mapObjects(f: (JSONObject) -> T): List<T> =
        if (this == null) emptyList() else (0 until length()).map { f(getJSONObject(it)) }

    private fun JSONObject.optLongOrNull(key: String): Long? = if (!has(key) || isNull(key)) null else getLong(key)

    /** Replaces all data with the contents of [json]. Throws if the file is invalid. */
    suspend fun import(json: String) {
        val root = JSONObject(json)
        val exercises = root.optJSONArray("exercises").mapObjects {
            ExerciseEntity(it.getLong("id"), it.getString("name"), it.getString("muscleGroup"), it.getString("equipment"), it.optBoolean("isCustom"), it.optString("notes"))
        }
        require(exercises.isNotEmpty()) { "Backup contains no exercises" }
        val workouts = root.optJSONArray("workouts").mapObjects {
            WorkoutEntity(it.getLong("id"), it.getString("name"), it.getLong("startTime"), it.optLongOrNull("endTime"), it.optString("notes"))
        }
        val wes = root.optJSONArray("workoutExercises").mapObjects {
            WorkoutExerciseEntity(it.getLong("id"), it.getLong("workoutId"), it.getLong("exerciseId"), it.getInt("position"), it.optString("notes"))
        }
        val sets = root.optJSONArray("sets").mapObjects {
            SetEntity(it.getLong("id"), it.getLong("workoutExerciseId"), it.getInt("position"), it.getDouble("weightKg"), it.getInt("reps"), it.optBoolean("completed"), it.optBoolean("isWarmup"))
        }
        val templates = root.optJSONArray("templates").mapObjects {
            TemplateEntity(it.getLong("id"), it.getString("name"), it.optString("notes"), it.optLongOrNull("lastUsed"))
        }
        val tes = root.optJSONArray("templateExercises").mapObjects {
            TemplateExerciseEntity(it.getLong("id"), it.getLong("templateId"), it.getLong("exerciseId"), it.getInt("position"), it.getInt("sets"), it.getInt("reps"))
        }
        val bws = root.optJSONArray("bodyWeight").mapObjects {
            BodyWeightEntity(it.getLong("id"), it.getLong("date"), it.getDouble("weightKg"))
        }
        db.withTransaction {
            val sql = db.openHelper.writableDatabase
            listOf("sets", "workout_exercises", "template_exercises", "workouts", "templates", "body_weight", "exercises")
                .forEach { sql.execSQL("DELETE FROM $it") }
            dao.insertExercises(exercises)
            dao.insertWorkoutsRaw(workouts)
            dao.insertWorkoutExercisesRaw(wes)
            dao.insertSetsRaw(sets)
            dao.insertTemplatesRaw(templates)
            dao.insertTemplateExercisesRaw(tes)
            dao.insertBodyWeightsRaw(bws)
        }
    }
}
