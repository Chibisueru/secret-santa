package io.github.chibisueru.secretsanta.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.chibisueru.secretsanta.BuildConfig
import io.github.chibisueru.secretsanta.data.AppSettings
import io.github.chibisueru.secretsanta.data.DarkMode
import io.github.chibisueru.secretsanta.data.ThemeStyle
import io.github.chibisueru.secretsanta.ui.theme.supportsDynamicColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onThemeStyle: (ThemeStyle) -> Unit,
    onDarkMode: (DarkMode) -> Unit,
    onSnowfall: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle("Colors")
            Column(Modifier.selectableGroup()) {
                RadioRow(
                    title = "Festive 🎄",
                    subtitle = "Christmas red, green and gold",
                    selected = settings.themeStyle == ThemeStyle.FESTIVE || !supportsDynamicColor,
                    onClick = { onThemeStyle(ThemeStyle.FESTIVE) },
                )
                RadioRow(
                    title = "Material You",
                    subtitle = if (supportsDynamicColor) "Colors from your wallpaper" else "Requires Android 12 or newer",
                    selected = settings.themeStyle == ThemeStyle.DYNAMIC && supportsDynamicColor,
                    enabled = supportsDynamicColor,
                    onClick = { onThemeStyle(ThemeStyle.DYNAMIC) },
                )
            }

            SectionTitle("Appearance")
            Column(Modifier.selectableGroup()) {
                RadioRow("Follow system", null, settings.darkMode == DarkMode.SYSTEM) { onDarkMode(DarkMode.SYSTEM) }
                RadioRow("Light", null, settings.darkMode == DarkMode.LIGHT) { onDarkMode(DarkMode.LIGHT) }
                RadioRow("Dark", null, settings.darkMode == DarkMode.DARK) { onDarkMode(DarkMode.DARK) }
            }

            SectionTitle("Extras")
            ListItem(
                leadingContent = { Icon(Icons.Outlined.AcUnit, contentDescription = null) },
                supportingContent = { Text("Gentle snow on the home screen") },
                trailingContent = { Switch(checked = settings.snowfall, onCheckedChange = null) },
                modifier = Modifier.toggleable(
                    value = settings.snowfall,
                    role = Role.Switch,
                    onValueChange = onSnowfall,
                ),
            ) { Text("Snowfall") }

            SectionTitle("About")
            ListItem(
                leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                supportingContent = {
                    Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                },
            ) { Text("Secret Santa") }
            ListItem(
                leadingContent = { Icon(Icons.Outlined.Code, contentDescription = null) },
                supportingContent = { Text("Source code, release notes and bug reports") },
                modifier = Modifier.clickable { uriHandler.openUri(REPO_URL) },
            ) { Text("GitHub") }
            ListItem(
                leadingContent = { Icon(Icons.Outlined.Gavel, contentDescription = null) },
                supportingContent = { Text("Free software under the GNU GPL v3.0") },
                modifier = Modifier.clickable { uriHandler.openUri("$REPO_URL/blob/main/LICENSE") },
            ) { Text("License") }
        }
    }
}

private const val REPO_URL = "https://github.com/Chibisueru/secret-santa"

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun RadioRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ListItem(
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { RadioButton(selected = selected, onClick = null, enabled = enabled) },
        modifier = Modifier.selectable(
            selected = selected,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick,
        ),
    ) { Text(title) }
}
