package io.github.chibisueru.secretsanta.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.github.chibisueru.secretsanta.data.AppSettings
import io.github.chibisueru.secretsanta.data.DarkMode
import io.github.chibisueru.secretsanta.data.ThemeStyle

val supportsDynamicColor: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun SecretSantaTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val dark = when (settings.darkMode) {
        DarkMode.SYSTEM -> isSystemInDarkTheme()
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        settings.themeStyle == ThemeStyle.DYNAMIC && supportsDynamicColor ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> FestiveDarkColors
        else -> FestiveLightColors
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
