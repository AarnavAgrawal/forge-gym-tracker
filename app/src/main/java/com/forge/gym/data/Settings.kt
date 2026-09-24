package com.forge.gym.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WeightUnit(val label: String, val perKg: Double) {
    KG("kg", 1.0),
    LB("lb", 2.2046226218);

    fun fromKg(kg: Double) = kg * perKg
    fun toKg(value: Double) = value / perKg
}

data class AppSettings(
    val unit: WeightUnit = WeightUnit.LB,
    val restSeconds: Int = 90,
    val weeklyGoal: Int = 4,
    val autoRest: Boolean = true,
    val barWeightKg: Double = 20.4117,
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("forge_settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    private fun load() = AppSettings(
        unit = runCatching { WeightUnit.valueOf(prefs.getString("unit", "LB")!!) }.getOrDefault(WeightUnit.LB),
        restSeconds = prefs.getInt("rest", 90),
        weeklyGoal = prefs.getInt("goal", 4),
        autoRest = prefs.getBoolean("autoRest", true),
        barWeightKg = prefs.getFloat("barKg", 20.4117f).toDouble(),
    )

    fun update(transform: (AppSettings) -> AppSettings) {
        val new = transform(_state.value)
        prefs.edit()
            .putString("unit", new.unit.name)
            .putInt("rest", new.restSeconds)
            .putInt("goal", new.weeklyGoal)
            .putBoolean("autoRest", new.autoRest)
            .putFloat("barKg", new.barWeightKg.toFloat())
            .apply()
        _state.value = new
    }
}
