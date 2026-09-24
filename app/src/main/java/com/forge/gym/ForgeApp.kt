package com.forge.gym

import android.app.Application
import com.forge.gym.data.AppDatabase
import com.forge.gym.data.Backup
import com.forge.gym.data.GymRepository
import com.forge.gym.data.RestTimer
import com.forge.gym.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ForgeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(app: Application) {
    /** Long-lived scope for writes that must not be cancelled when a screen closes. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val db = AppDatabase.build(app)
    val repo = GymRepository(db, scope)
    val settings = SettingsStore(app)
    val restTimer = RestTimer()
    val backup = Backup(db)

    init {
        scope.launch { repo.ensureSeeded() }
    }

    fun launch(block: suspend GymRepository.() -> Unit) {
        scope.launch { repo.block() }
    }
}
