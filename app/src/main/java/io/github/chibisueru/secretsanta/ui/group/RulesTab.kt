package io.github.chibisueru.secretsanta.ui.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.chibisueru.secretsanta.data.ParticipantEntity
import io.github.chibisueru.secretsanta.ui.components.EmptyState
import io.github.chibisueru.secretsanta.ui.components.SectionHint
import io.github.chibisueru.secretsanta.ui.components.rememberClickInteractionSource

@Composable
fun RulesTab(
    state: GroupUiState,
    onAdd: (fromId: Long, toId: Long, bothWays: Boolean) -> Unit,
    onDelete: (RulePair) -> Unit,
    onResetDraw: () -> Unit,
) {
    var adding by rememberSaveable { mutableStateOf(false) }
    val locked = state.isDrawn
    val enoughPeople = state.participants.size >= 2

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHint("Rules stop someone from drawing a particular person, e.g. so partners don't get each other.")
                if (locked) {
                    DrawnBanner(onResetDraw)
                } else {
                    FilledTonalButton(
                        onClick = { adding = true },
                        enabled = enoughPeople,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (enoughPeople) "Add rule" else "Add at least 2 people first")
                    }
                }
            }
        }

        if (state.rules.isEmpty()) {
            item {
                EmptyState(
                    emoji = "🎄",
                    title = "No rules",
                    message = "Anyone can draw anyone except themselves.",
                )
            }
        }

        items(state.rules, key = { "${it.from.id}-${it.to.id}" }) { rule ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                ListItem(
                    leadingContent = {
                        Icon(
                            if (rule.mutual) Icons.Filled.SyncAlt else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    supportingContent = {
                        Text(
                            if (rule.mutual) {
                                "Can't draw each other"
                            } else {
                                "${rule.from.name} can't draw ${rule.to.name}"
                            },
                        )
                    },
                    trailingContent = if (locked) {
                        null
                    } else {
                        {
                            IconButton(onClick = { onDelete(rule) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Delete rule")
                            }
                        }
                    },
                ) {
                        Text(
                            if (rule.mutual) {
                                "${rule.from.name} & ${rule.to.name}"
                            } else {
                                "${rule.from.name} → ${rule.to.name}"
                            },
                        )
                    }
            }
        }
    }

    if (adding) {
        AddRuleDialog(
            people = state.participants,
            onDismiss = { adding = false },
            onAdd = { from, to, both ->
                onAdd(from, to, both)
                adding = false
            },
        )
    }
}

@Composable
private fun AddRuleDialog(
    people: List<ParticipantEntity>,
    onDismiss: () -> Unit,
    onAdd: (fromId: Long, toId: Long, bothWays: Boolean) -> Unit,
) {
    var fromId by rememberSaveable { mutableStateOf<Long?>(null) }
    var toId by rememberSaveable { mutableStateOf<Long?>(null) }
    var bothWays by rememberSaveable { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PersonPicker(
                    label = "Person",
                    people = people,
                    selectedId = fromId,
                    onSelect = {
                        fromId = it
                        if (toId == it) toId = null
                    },
                )
                PersonPicker(
                    label = if (bothWays) "And" else "Can't draw",
                    people = people.filter { it.id != fromId },
                    selectedId = toId,
                    onSelect = { toId = it },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Both ways", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (bothWays) "Neither can draw the other" else "Only the first person is restricted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = bothWays, onCheckedChange = { bothWays = it })
                }
            }
        },
        confirmButton = {
            val from = fromId
            val to = toId
            TextButton(
                onClick = { if (from != null && to != null) onAdd(from, to, bothWays) },
                enabled = from != null && to != null && from != to,
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PersonPicker(
    label: String,
    people: List<ParticipantEntity>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selected = people.firstOrNull { it.id == selectedId }

    Box {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("Choose…") },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            interactionSource = rememberClickInteractionSource { expanded = true },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            people.forEach { person ->
                DropdownMenuItem(
                    text = { Text(person.name) },
                    onClick = {
                        onSelect(person.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
