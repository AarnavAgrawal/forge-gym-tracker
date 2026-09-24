# Forge

A fast, good-looking Android workout tracker built with Kotlin and Jetpack Compose.

## Features

- **Workout logging**: sets, weight and reps, with the previous session shown inline, tap-to-copy, auto-filled follow-up sets, warm-up sets and swipe-to-delete
- **Rest timer**: starts automatically after each set, adjustable, vibrates when done
- **Calendar**: month view of training days, day details, and logging past workouts
- **Progress**: consistency heatmap, weekly volume, muscle split, per-exercise charts (est. 1RM, top weight, volume, reps) and automatic PR detection
- **Routines**: reusable workout templates (Push / Pull / Legs included)
- **Exercise library**: 80+ built-in exercises plus custom ones
- **Body weight tracking** with a trend chart
- **Tools**: plate calculator and 1RM calculator
- **lb / kg**, weekly goal, and JSON backup export/import

All data is stored locally on the device (Room database).

## Build

Requirements: JDK 17+ and the Android SDK (compileSdk 35).

```bash
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release APK (see signing below)
```

Output: `app/build/outputs/apk/<variant>/`.

### Release signing

Create `keystore.properties` in the project root (it is git-ignored):

```properties
storeFile=forge-release.jks
storePassword=...
keyAlias=forge
keyPassword=...
```

Without it, release builds fall back to the debug signing key.

## Tech

Kotlin, Jetpack Compose (Material 3), Navigation Compose, Room, coroutines/Flow. Fonts: Space Grotesk and Inter (SIL Open Font License).
