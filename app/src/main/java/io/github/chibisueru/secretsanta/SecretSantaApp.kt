package io.github.chibisueru.secretsanta

import android.app.Application
import android.content.Context
import io.github.chibisueru.secretsanta.data.SantaDatabase
import io.github.chibisueru.secretsanta.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class SecretSantaApp : Application() {
    val database: SantaDatabase by lazy { SantaDatabase.create(this) }
    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    /** Outlives screens, so writes that start as the user leaves a screen still finish. */
    val appScope = CoroutineScope(SupervisorJob())
}

val Context.santaApp: SecretSantaApp get() = applicationContext as SecretSantaApp
