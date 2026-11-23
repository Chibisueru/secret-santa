package io.github.chibisueru.secretsanta.ui.decoder

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.chibisueru.secretsanta.domain.NameCode
import io.github.chibisueru.secretsanta.ui.components.SectionHint
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecoderScreen(bottomBar: @Composable () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var revealed by rememberSaveable { mutableStateOf(false) }
    val decoded = remember(input) { NameCode.decodeInput(input) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    fun setInput(value: String) {
        input = value
        revealed = false
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Decoder 🔐") }) },
        bottomBar = bottomBar,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHint(
                "Got your Secret Santa message? Paste the code in [brackets], or the whole message, " +
                    "to see who you're buying for.",
            )

            OutlinedTextField(
                value = input,
                onValueChange = ::setInput,
                label = { Text("Code or message") },
                placeholder = { Text("e.g. TcOhdMOp") },
                trailingIcon = if (input.isNotEmpty()) {
                    {
                        IconButton(onClick = { setInput("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                } else {
                    null
                },
                minLines = 3,
                maxLines = 8,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            FilledTonalButton(
                onClick = {
                    scope.launch {
                        val text = clipboard.getClipEntry()?.clipData
                            ?.takeIf { it.itemCount > 0 }
                            ?.getItemAt(0)?.text?.toString()
                        if (!text.isNullOrBlank()) setInput(text)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.ContentPaste, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Paste from clipboard")
            }

            Spacer(Modifier.height(4.dp))

            AnimatedContent(
                targetState = when {
                    input.isBlank() -> DecodeState.Empty
                    decoded == null -> DecodeState.Invalid
                    !revealed -> DecodeState.Hidden
                    else -> DecodeState.Revealed
                },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "decode-result",
            ) { state ->
                when (state) {
                    DecodeState.Empty -> Unit
                    DecodeState.Invalid -> InvalidCard()
                    DecodeState.Hidden -> HiddenCard(onReveal = { revealed = true })
                    DecodeState.Revealed -> RevealedCard(
                        name = decoded.orEmpty(),
                        onHide = { revealed = false },
                    )
                }
            }
        }
    }
}

private enum class DecodeState { Empty, Invalid, Hidden, Revealed }

@Composable
private fun HiddenCard(onReveal: () -> Unit) {
    Card(
        onClick = onReveal,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🎁", fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text("Code found!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Make sure nobody's looking, then tap to reveal",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RevealedCard(name: String, onHide: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🎅", fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text("You're the Secret Santa for", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                name,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onHide) {
                Icon(Icons.Outlined.VisibilityOff, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Hide")
            }
        }
    }
}

@Composable
private fun InvalidCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(
                "No valid code found. Make sure you copied the whole code, including every letter.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
