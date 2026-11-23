package io.github.chibisueru.secretsanta.ui.group

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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.chibisueru.secretsanta.data.ParticipantEntity
import io.github.chibisueru.secretsanta.ui.components.EmptyState
import io.github.chibisueru.secretsanta.ui.components.InitialAvatar
import io.github.chibisueru.secretsanta.ui.components.ParticipantDialog

@Composable
fun PeopleTab(
    state: GroupUiState,
    onSave: (existing: ParticipantEntity?, name: String, phone: String, email: String) -> Unit,
    onDelete: (ParticipantEntity) -> Unit,
    onResetDraw: () -> Unit,
) {
    var editing by remember { mutableStateOf<ParticipantEntity?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ParticipantEntity?>(null) }
    val locked = state.isDrawn

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                if (locked) {
                    DrawnBanner(onResetDraw)
                } else {
                    FilledTonalButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add person")
                    }
                }
                Spacer(Modifier.size(8.dp))
            }
        }

        if (state.participants.isEmpty()) {
            item {
                EmptyState(
                    emoji = "🧑‍🎄",
                    title = "No one here yet",
                    message = "Add everyone taking part. A phone number or email lets you send matches straight to them.",
                )
            }
        }

        items(state.participants, key = { it.id }) { person ->
            ListItem(
                leadingContent = { InitialAvatar(person.name) },
                supportingContent = {
                    val contact = listOf(person.phone, person.email).filter { it.isNotBlank() }
                    Text(contact.joinToString(" · ").ifEmpty { "No contact info" })
                },
                trailingContent = if (locked) {
                    null
                } else {
                    {
                        Row {
                            IconButton(onClick = { editing = person }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Edit ${person.name}")
                            }
                            IconButton(onClick = { pendingDelete = person }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Remove ${person.name}")
                            }
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            ) { Text(if (person.isOrganizer) "${person.name} (you)" else person.name) }
        }
    }

    if (adding || editing != null) {
        val existing = editing
        ParticipantDialog(
            title = when {
                existing == null -> "Add person"
                existing.isOrganizer -> "Edit your details"
                else -> "Edit person"
            },
            confirmLabel = if (existing == null) "Add" else "Save",
            initialName = existing?.name ?: "",
            initialPhone = existing?.phone ?: "",
            initialEmail = existing?.email ?: "",
            takenNames = state.participants
                .filter { it.id != existing?.id }
                .map { it.name.trim().lowercase() }
                .toSet(),
            onDismiss = { adding = false; editing = null },
            onSave = { name, phone, email ->
                onSave(existing, name, phone, email)
                adding = false
                editing = null
            },
        )
    }

    pendingDelete?.let { person ->
        val ruleCount = state.rules.count { it.from.id == person.id || it.to.id == person.id }
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (person.isOrganizer) "Leave the draw?" else "Remove ${person.name}?") },
            text = {
                Text(
                    if (person.isOrganizer) {
                        "You'll stay the organizer but won't get a match." +
                            if (ruleCount > 0) " Your rules will be removed too." else ""
                    } else if (ruleCount > 0) {
                        "Their ${if (ruleCount == 1) "rule" else "$ruleCount rules"} will be removed too."
                    } else {
                        "They'll be removed from this group."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(person)
                    pendingDelete = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}
