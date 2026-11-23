package io.github.chibisueru.secretsanta.ui.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.ui.Alignment
import io.github.chibisueru.secretsanta.ui.components.InitialAvatar
import io.github.chibisueru.secretsanta.ui.components.ParticipantDialog
import io.github.chibisueru.secretsanta.ui.newgroup.ParticipateSwitch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.chibisueru.secretsanta.data.GroupEntity
import io.github.chibisueru.secretsanta.domain.formatEventDate
import io.github.chibisueru.secretsanta.ui.components.SectionHint
import io.github.chibisueru.secretsanta.ui.components.rememberClickInteractionSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsTab(
    group: GroupEntity,
    state: GroupUiState,
    onChange: (GroupDetails) -> Unit,
    onEditOrganizer: (name: String, phone: String, email: String) -> Unit,
    onParticipates: (Boolean) -> Unit,
) {
    // Local copies so typing never waits on the database. Seeded once per group.
    var name by rememberSaveable(group.id) { mutableStateOf(group.name) }
    var budget by rememberSaveable(group.id) { mutableStateOf(group.budget) }
    var date by rememberSaveable(group.id) { mutableStateOf(group.eventDateMillis) }
    var location by rememberSaveable(group.id) { mutableStateOf(group.location) }
    var notes by rememberSaveable(group.id) { mutableStateOf(group.notes) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var editOrganizer by rememberSaveable { mutableStateOf(false) }

    fun push() = onChange(GroupDetails(name, budget, date, location, notes))

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OrganizerCard(
            group = group,
            participates = state.organizerParticipant != null,
            locked = state.isDrawn,
            onEdit = { editOrganizer = true },
            onParticipates = onParticipates,
        )

        Spacer(Modifier.height(4.dp))
        Text("Event", style = MaterialTheme.typography.titleMedium)
        SectionHint("These details are included in each person's match message. Changes save automatically.")

        OutlinedTextField(
            value = name,
            onValueChange = { name = it; push() },
            label = { Text("Group name") },
            leadingIcon = { Icon(Icons.Outlined.Celebration, contentDescription = null) },
            isError = name.isBlank(),
            supportingText = if (name.isBlank()) ({ Text("Give your group a name") }) else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = budget,
            onValueChange = { budget = it; push() },
            label = { Text("Budget") },
            placeholder = { Text("e.g. \$30") },
            leadingIcon = { Icon(Icons.Outlined.Payments, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = date?.let(::formatEventDate) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Gift exchange date") },
            placeholder = { Text("Pick a date") },
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
            trailingIcon = if (date != null) {
                {
                    IconButton(onClick = { date = null; push() }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear date")
                    }
                }
            } else {
                null
            },
            interactionSource = rememberClickInteractionSource { showDatePicker = true },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = location,
            onValueChange = { location = it; push() },
            label = { Text("Location") },
            placeholder = { Text("e.g. Grandma's house") },
            leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it; push() },
            label = { Text("Notes") },
            placeholder = { Text("e.g. Homemade gifts welcome!") },
            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Notes, contentDescription = null) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            minLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
        )
    }

    if (editOrganizer) {
        val organizerId = state.organizerParticipant?.id
        ParticipantDialog(
            title = "Your details",
            confirmLabel = "Save",
            initialName = group.organizerName,
            initialPhone = group.organizerPhone,
            initialEmail = group.organizerEmail,
            takenNames = state.participants
                .filter { it.id != organizerId }
                .map { it.name.trim().lowercase() }
                .toSet(),
            onDismiss = { editOrganizer = false },
            onSave = { name, phone, email ->
                onEditOrganizer(name, phone, email)
                editOrganizer = false
            },
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    date = pickerState.selectedDateMillis
                    push()
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun OrganizerCard(
    group: GroupEntity,
    participates: Boolean,
    locked: Boolean,
    onEdit: () -> Unit,
    onParticipates: (Boolean) -> Unit,
) {
    val hasOrganizer = group.organizerName.isNotBlank()
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Organizer", style = MaterialTheme.typography.titleMedium)
            if (hasOrganizer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InitialAvatar(
                        group.organizerName,
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${group.organizerName} (you)", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            listOf(group.organizerPhone, group.organizerEmail)
                                .filter { it.isNotBlank() }
                                .joinToString(" · ")
                                .ifEmpty { "No contact info" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit your details")
                    }
                }
                ParticipateSwitch(
                    participates = participates,
                    onChange = onParticipates,
                    enabled = !locked,
                )
                if (locked) {
                    SectionHint("Reset the draw to change whether you take part.")
                }
            } else {
                SectionHint("Add your details as the organizer and choose whether you take part in the draw.")
                FilledTonalButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                    Text("Add your details")
                }
            }
        }
    }
}
