package io.github.chibisueru.secretsanta.ui.newgroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.chibisueru.secretsanta.ui.components.PersonFields
import io.github.chibisueru.secretsanta.ui.components.SectionHint
import io.github.chibisueru.secretsanta.ui.components.isValidOptionalEmail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupScreen(
    prefill: OrganizerPrefill?,
    onClose: () -> Unit,
    onCreate: (groupName: String, name: String, phone: String, email: String, participates: Boolean) -> Unit,
) {
    var groupName by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var participates by rememberSaveable { mutableStateOf(true) }
    var prefilled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(prefill) {
        if (prefill != null && !prefilled) {
            prefilled = true
            if (name.isBlank()) name = prefill.name
            if (phone.isBlank()) phone = prefill.phone
            if (email.isBlank()) email = prefill.email
        }
    }

    val valid = groupName.isNotBlank() && name.isNotBlank() && isValidOptionalEmail(email)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New group") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
                Button(
                    onClick = { onCreate(groupName, name, phone, email, participates) },
                    enabled = valid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp),
                ) { Text("Create group") }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group name") },
                placeholder = { Text("e.g. Family Christmas 2026") },
                leadingIcon = { Icon(Icons.Outlined.Celebration, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))
            Text("Your details", style = MaterialTheme.typography.titleMedium)
            SectionHint("You're the organizer. Your name is shown at the bottom of each match message.")

            PersonFields(
                name = name, onName = { name = it },
                phone = phone, onPhone = { phone = it },
                email = email, onEmail = { email = it },
                nameLabel = "Your name",
            )

            ParticipateSwitch(participates = participates, onChange = { participates = it })
        }
    }
}

@Composable
fun ParticipateSwitch(
    participates: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (participates) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .toggleable(
                    value = participates,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onChange,
                )
                .padding(16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("I'm taking part in the draw", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (participates) {
                        "You'll get a match too. It's encoded like everyone else's, so the surprise is safe."
                    } else {
                        "You'll only organize. You won't give or get a gift."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = participates, onCheckedChange = null, enabled = enabled)
        }
    }
}
