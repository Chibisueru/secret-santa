package io.github.chibisueru.secretsanta.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Interaction source that calls [onClick] when tapped. Use it to make a read-only
 * text field act like a button (date and person pickers).
 */
@Composable
fun rememberClickInteractionSource(onClick: () -> Unit): MutableInteractionSource {
    val source = remember { MutableInteractionSource() }
    val currentOnClick = rememberUpdatedState(onClick)
    LaunchedEffect(source) {
        source.interactions.collect {
            if (it is PressInteraction.Release) currentOnClick.value()
        }
    }
    return source
}
