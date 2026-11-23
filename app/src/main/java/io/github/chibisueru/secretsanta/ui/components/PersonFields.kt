package io.github.chibisueru.secretsanta.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

fun isValidOptionalEmail(email: String): Boolean =
    email.isBlank() || EMAIL_REGEX.matches(email.trim())

/** Name, phone and email fields, shared by the person dialog and the new-group screen. */
@Composable
fun PersonFields(
    name: String,
    onName: (String) -> Unit,
    phone: String,
    onPhone: (String) -> Unit,
    email: String,
    onEmail: (String) -> Unit,
    modifier: Modifier = Modifier,
    nameLabel: String = "Name",
    nameError: String? = null,
) {
    val emailInvalid = !isValidOptionalEmail(email)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onName,
            label = { Text(nameLabel) },
            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = phone,
            onValueChange = onPhone,
            label = { Text("Phone (optional)") },
            leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = email,
            onValueChange = onEmail,
            label = { Text("Email (optional)") },
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
            isError = emailInvalid,
            supportingText = if (emailInvalid) ({ Text("That doesn't look like an email address") }) else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun ParticipantDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    initialPhone: String,
    initialEmail: String,
    takenNames: Set<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, email: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var phone by rememberSaveable { mutableStateOf(initialPhone) }
    var email by rememberSaveable { mutableStateOf(initialEmail) }

    val duplicate = name.trim().lowercase() in takenNames
    val valid = name.isNotBlank() && !duplicate && isValidOptionalEmail(email)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            PersonFields(
                name = name, onName = { name = it },
                phone = phone, onPhone = { phone = it },
                email = email, onEmail = { email = it },
                nameError = if (duplicate) "Someone with this name is already in the group" else null,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, phone, email) }, enabled = valid) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
