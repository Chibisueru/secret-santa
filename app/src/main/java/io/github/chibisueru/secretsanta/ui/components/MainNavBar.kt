package io.github.chibisueru.secretsanta.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

enum class MainTab { HOME, DECODER }

/** The bottom dock shown on the top-level screens. */
@Composable
fun MainNavBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == MainTab.HOME,
            onClick = { onSelect(MainTab.HOME) },
            icon = {
                Icon(
                    if (selected == MainTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = null,
                )
            },
            label = { Text("Home") },
        )
        NavigationBarItem(
            selected = selected == MainTab.DECODER,
            onClick = { onSelect(MainTab.DECODER) },
            icon = {
                Icon(
                    if (selected == MainTab.DECODER) Icons.Filled.Key else Icons.Outlined.Key,
                    contentDescription = null,
                )
            },
            label = { Text("Decoder") },
        )
    }
}
