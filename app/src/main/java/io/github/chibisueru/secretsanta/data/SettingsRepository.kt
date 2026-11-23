package io.github.chibisueru.secretsanta.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeStyle { FESTIVE, DYNAMIC }

enum class DarkMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeStyle: ThemeStyle = ThemeStyle.FESTIVE,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val snowfall: Boolean = true,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val themeStyle = stringPreferencesKey("theme_style")
        val darkMode = stringPreferencesKey("dark_mode")
        val snowfall = booleanPreferencesKey("snowfall")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeStyle = prefs[Keys.themeStyle].toEnum(ThemeStyle.FESTIVE),
            darkMode = prefs[Keys.darkMode].toEnum(DarkMode.SYSTEM),
            snowfall = prefs[Keys.snowfall] ?: true,
        )
    }

    suspend fun setThemeStyle(style: ThemeStyle) {
        context.dataStore.edit { it[Keys.themeStyle] = style.name }
    }

    suspend fun setDarkMode(mode: DarkMode) {
        context.dataStore.edit { it[Keys.darkMode] = mode.name }
    }

    suspend fun setSnowfall(enabled: Boolean) {
        context.dataStore.edit { it[Keys.snowfall] = enabled }
    }
}

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    enumValues<T>().firstOrNull { it.name == this } ?: default
