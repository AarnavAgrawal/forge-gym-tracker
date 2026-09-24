package com.forge.gym.data

object MuscleGroups {
    val all = listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs", "Glutes", "Core", "Cardio", "Full Body")
}

object Equipment {
    val all = listOf("Barbell", "Dumbbell", "Machine", "Cable", "Bodyweight", "Kettlebell", "Band", "Other")
}

object Seed {
    private fun e(name: String, muscle: String, equip: String) =
        ExerciseEntity(name = name, muscleGroup = muscle, equipment = equip)

    val exercises = listOf(
        e("Bench Press", "Chest", "Barbell"),
        e("Incline Bench Press", "Chest", "Barbell"),
        e("Decline Bench Press", "Chest", "Barbell"),
        e("Dumbbell Bench Press", "Chest", "Dumbbell"),
        e("Incline Dumbbell Press", "Chest", "Dumbbell"),
        e("Dumbbell Fly", "Chest", "Dumbbell"),
        e("Cable Crossover", "Chest", "Cable"),
        e("Chest Press Machine", "Chest", "Machine"),
        e("Pec Deck", "Chest", "Machine"),
        e("Push Up", "Chest", "Bodyweight"),
        e("Chest Dip", "Chest", "Bodyweight"),
        e("Deadlift", "Back", "Barbell"),
        e("Barbell Row", "Back", "Barbell"),
        e("Pendlay Row", "Back", "Barbell"),
        e("T-Bar Row", "Back", "Barbell"),
        e("Dumbbell Row", "Back", "Dumbbell"),
        e("Pull Up", "Back", "Bodyweight"),
        e("Chin Up", "Back", "Bodyweight"),
        e("Lat Pulldown", "Back", "Cable"),
        e("Seated Cable Row", "Back", "Cable"),
        e("Straight Arm Pulldown", "Back", "Cable"),
        e("Machine Row", "Back", "Machine"),
        e("Back Extension", "Back", "Bodyweight"),
        e("Shrug", "Back", "Dumbbell"),
        e("Overhead Press", "Shoulders", "Barbell"),
        e("Dumbbell Shoulder Press", "Shoulders", "Dumbbell"),
        e("Arnold Press", "Shoulders", "Dumbbell"),
        e("Lateral Raise", "Shoulders", "Dumbbell"),
        e("Cable Lateral Raise", "Shoulders", "Cable"),
        e("Front Raise", "Shoulders", "Dumbbell"),
        e("Rear Delt Fly", "Shoulders", "Dumbbell"),
        e("Face Pull", "Shoulders", "Cable"),
        e("Shoulder Press Machine", "Shoulders", "Machine"),
        e("Upright Row", "Shoulders", "Barbell"),
        e("Barbell Curl", "Biceps", "Barbell"),
        e("EZ Bar Curl", "Biceps", "Barbell"),
        e("Dumbbell Curl", "Biceps", "Dumbbell"),
        e("Hammer Curl", "Biceps", "Dumbbell"),
        e("Incline Dumbbell Curl", "Biceps", "Dumbbell"),
        e("Preacher Curl", "Biceps", "Machine"),
        e("Cable Curl", "Biceps", "Cable"),
        e("Concentration Curl", "Biceps", "Dumbbell"),
        e("Close Grip Bench Press", "Triceps", "Barbell"),
        e("Skull Crusher", "Triceps", "Barbell"),
        e("Tricep Pushdown", "Triceps", "Cable"),
        e("Rope Pushdown", "Triceps", "Cable"),
        e("Overhead Tricep Extension", "Triceps", "Dumbbell"),
        e("Tricep Dip", "Triceps", "Bodyweight"),
        e("Tricep Kickback", "Triceps", "Dumbbell"),
        e("Squat", "Legs", "Barbell"),
        e("Front Squat", "Legs", "Barbell"),
        e("Leg Press", "Legs", "Machine"),
        e("Hack Squat", "Legs", "Machine"),
        e("Bulgarian Split Squat", "Legs", "Dumbbell"),
        e("Goblet Squat", "Legs", "Dumbbell"),
        e("Walking Lunge", "Legs", "Dumbbell"),
        e("Romanian Deadlift", "Legs", "Barbell"),
        e("Leg Extension", "Legs", "Machine"),
        e("Lying Leg Curl", "Legs", "Machine"),
        e("Seated Leg Curl", "Legs", "Machine"),
        e("Standing Calf Raise", "Legs", "Machine"),
        e("Seated Calf Raise", "Legs", "Machine"),
        e("Hip Thrust", "Glutes", "Barbell"),
        e("Glute Bridge", "Glutes", "Bodyweight"),
        e("Cable Kickback", "Glutes", "Cable"),
        e("Hip Abduction", "Glutes", "Machine"),
        e("Sumo Deadlift", "Glutes", "Barbell"),
        e("Plank", "Core", "Bodyweight"),
        e("Crunch", "Core", "Bodyweight"),
        e("Hanging Leg Raise", "Core", "Bodyweight"),
        e("Cable Crunch", "Core", "Cable"),
        e("Russian Twist", "Core", "Bodyweight"),
        e("Ab Wheel Rollout", "Core", "Other"),
        e("Running", "Cardio", "Other"),
        e("Rowing Machine", "Cardio", "Machine"),
        e("Stationary Bike", "Cardio", "Machine"),
        e("Jump Rope", "Cardio", "Other"),
        e("Kettlebell Swing", "Full Body", "Kettlebell"),
        e("Clean and Press", "Full Body", "Barbell"),
        e("Burpee", "Full Body", "Bodyweight"),
        e("Farmer Walk", "Full Body", "Dumbbell"),
    )

    /** Starter routines: name to list of (exercise name, sets, reps). */
    val templates = listOf(
        "Push" to listOf(
            Triple("Bench Press", 4, 8),
            Triple("Overhead Press", 3, 8),
            Triple("Incline Dumbbell Press", 3, 10),
            Triple("Lateral Raise", 3, 15),
            Triple("Tricep Pushdown", 3, 12),
        ),
        "Pull" to listOf(
            Triple("Deadlift", 3, 5),
            Triple("Pull Up", 3, 8),
            Triple("Barbell Row", 3, 8),
            Triple("Face Pull", 3, 15),
            Triple("Hammer Curl", 3, 12),
        ),
        "Legs" to listOf(
            Triple("Squat", 4, 6),
            Triple("Romanian Deadlift", 3, 8),
            Triple("Leg Press", 3, 12),
            Triple("Lying Leg Curl", 3, 12),
            Triple("Standing Calf Raise", 4, 15),
        ),
    )
}
