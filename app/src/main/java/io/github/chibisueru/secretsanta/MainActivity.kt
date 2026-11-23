package io.github.chibisueru.secretsanta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.chibisueru.secretsanta.data.AppSettings
import io.github.chibisueru.secretsanta.ui.AppNavHost
import io.github.chibisueru.secretsanta.ui.theme.SecretSantaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsFlow = santaApp.settings.settings
        setContent {
            val settings: AppSettings? by settingsFlow.collectAsStateWithLifecycle(initialValue = null)
            // Wait for saved settings (a few ms) so the wrong theme never flashes.
            val loaded = settings ?: return@setContent
            SecretSantaTheme(loaded) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavHost(loaded)
                }
            }
        }
    }
}
