package com.forge.gym.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class RestState(val endAt: Long, val totalMs: Long)

/** App-wide rest timer so it keeps running while you navigate around. */
class RestTimer {
    private val _state = MutableStateFlow<RestState?>(null)
    val state: StateFlow<RestState?> = _state.asStateFlow()

    fun start(seconds: Int) {
        if (seconds <= 0) return
        _state.value = RestState(System.currentTimeMillis() + seconds * 1000L, seconds * 1000L)
    }

    fun adjust(seconds: Int) {
        _state.update { st ->
            st ?: return@update null
            val newEnd = st.endAt + seconds * 1000L
            if (newEnd <= System.currentTimeMillis()) null
            else st.copy(endAt = newEnd, totalMs = (st.totalMs + seconds * 1000L).coerceAtLeast(1000L))
        }
    }

    fun skip() {
        _state.value = null
    }
}
