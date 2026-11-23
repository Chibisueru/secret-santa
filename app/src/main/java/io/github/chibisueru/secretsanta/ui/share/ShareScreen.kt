package io.github.chibisueru.secretsanta.ui.share

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.chibisueru.secretsanta.data.GroupEntity
import io.github.chibisueru.secretsanta.data.ParticipantEntity
import io.github.chibisueru.secretsanta.domain.MatchMessage
import io.github.chibisueru.secretsanta.ui.components.InitialAvatar
import io.github.chibisueru.secretsanta.ui.group.GroupEvent
import io.github.chibisueru.secretsanta.ui.group.GroupViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    viewModel: GroupViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val revealAll by viewModel.revealAll.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var menuOpen by remember { mutableStateOf(false) }
    var confirmReveal by rememberSaveable { mutableStateOf(false) }
    var confirmRedraw by rememberSaveable { mutableStateOf(false) }
    var redrawError by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                GroupEvent.DrawSucceeded -> snackbar.showSnackbar("Names re-drawn 🎲 Everyone has a new match.")
                is GroupEvent.DrawFailed -> redrawError = event.reason
            }
        }
    }

    val group = state.group
    // Group was deleted or the draw was reset: nothing to share.
    LaunchedEffect(state.loading, group?.isDrawn) {
        if (!state.loading && group?.isDrawn != true) onBack()
    }
    if (group == null || !group.isDrawn) return

    val byId = state.participants.associateBy { it.id }
    val sentCount = state.participants.count { it.notified }
    val total = state.participants.size

    fun send(person: ParticipantEntity, method: ShareMethod) {
        val receiver = person.assignedToId?.let(byId::get) ?: return
        val opened = sendMatch(
            context = context,
            method = method,
            recipientName = person.name,
            phone = person.phone,
            email = person.email,
            subject = MatchMessage.subject(group),
            body = MatchMessage.body(group, person.name, receiver.name),
        )
        if (opened) {
            viewModel.setNotified(person, true)
        } else {
            scope.launch { snackbar.showSnackbar("No app found to send messages") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Share matches") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(if (revealAll) "Hide pairings" else "Reveal all pairings") },
                                leadingIcon = {
                                    Icon(
                                        if (revealAll) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    if (revealAll) viewModel.setRevealAll(false) else confirmReveal = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Re-draw names") },
                                leadingIcon = { Icon(Icons.Outlined.Casino, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    confirmRedraw = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item { ProgressHeader(group, sentCount, total) }
            items(state.participants, key = { it.id }) { person ->
                ParticipantShareRow(
                    person = person,
                    receiverName = person.assignedToId?.let(byId::get)?.name,
                    reveal = revealAll,
                    onSend = { send(person, it) },
                    onToggleSent = { viewModel.setNotified(person, !person.notified) },
                )
                HorizontalDivider(Modifier.padding(start = 72.dp))
            }
        }
    }

    if (confirmReveal) {
        AlertDialog(
            onDismissRequest = { confirmReveal = false },
            icon = { Icon(Icons.Outlined.Visibility, contentDescription = null) },
            title = { Text("Reveal all pairings?") },
            text = {
                Text(
                    "Every match will be shown on screen. If you're taking part too, " +
                        "this will spoil your own surprise.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmReveal = false
                    viewModel.setRevealAll(true)
                }) { Text("Reveal") }
            },
            dismissButton = { TextButton(onClick = { confirmReveal = false }) { Text("Cancel") } },
        )
    }

    if (confirmRedraw) {
        AlertDialog(
            onDismissRequest = { confirmRedraw = false },
            icon = { Icon(Icons.Outlined.Casino, contentDescription = null) },
            title = { Text("Re-draw names?") },
            text = {
                Text(
                    if (sentCount > 0) {
                        "Everyone gets a new match. The $sentCount ${if (sentCount == 1) "person" else "people"} " +
                            "you've already messaged will need to be sent their new match."
                    } else {
                        "Everyone gets a new match."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRedraw = false
                    viewModel.draw()
                }) { Text("Re-draw") }
            },
            dismissButton = { TextButton(onClick = { confirmRedraw = false }) { Text("Cancel") } },
        )
    }

    redrawError?.let { reason ->
        AlertDialog(
            onDismissRequest = { redrawError = null },
            title = { Text("Can't re-draw") },
            text = { Text(reason) },
            confirmButton = { TextButton(onClick = { redrawError = null }) { Text("OK") } },
        )
    }
}

@Composable
private fun ProgressHeader(group: GroupEntity, sent: Int, total: Int) {
    val done = total > 0 && sent == total
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (done) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                if (done) "All matches sent! 🎄" else "Names are drawn! 🎉",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (done) {
                    "Everyone in ${group.name} knows who they're buying for. Merry Christmas!"
                } else {
                    "Send each person their match. The pairings stay hidden from you, so you can take part too."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else sent.toFloat() / total },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text("$sent of $total sent", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ParticipantShareRow(
    person: ParticipantEntity,
    receiverName: String?,
    reveal: Boolean,
    onSend: (ShareMethod) -> Unit,
    onToggleSent: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val method = preferredMethod(person.phone, person.email)

    ListItem(
        leadingContent = {
            InitialAvatar(
                person.name,
                container = if (person.notified) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                content = if (person.notified) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
        supportingContent = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (person.notified) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (person.notified) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        when {
                            person.notified -> "Sent"
                            method == ShareMethod.SMS -> "Not sent · SMS"
                            method == ShareMethod.EMAIL -> "Not sent · Email"
                            else -> "Not sent · any app"
                        },
                    )
                }
                if (reveal && receiverName != null) {
                    Text(
                        "🎁 → $receiverName",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val sendIcon = when (method) {
                    ShareMethod.SMS -> Icons.Outlined.Sms
                    ShareMethod.EMAIL -> Icons.Outlined.Email
                    ShareMethod.OTHER -> Icons.Outlined.Share
                }
                if (person.notified) {
                    OutlinedButton(onClick = { onSend(method) }) { Text("Resend") }
                } else {
                    FilledTonalButton(onClick = { onSend(method) }) {
                        Icon(sendIcon, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Send")
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More ways to send to ${person.name}")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (person.phone.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("Send SMS") },
                                leadingIcon = { Icon(Icons.Outlined.Sms, contentDescription = null) },
                                onClick = { menuOpen = false; onSend(ShareMethod.SMS) },
                            )
                        }
                        if (person.email.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("Send email") },
                                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                                onClick = { menuOpen = false; onSend(ShareMethod.EMAIL) },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Share via…") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                            onClick = { menuOpen = false; onSend(ShareMethod.OTHER) },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(if (person.notified) "Mark as not sent" else "Mark as sent") },
                            leadingIcon = {
                                Icon(
                                    if (person.notified) Icons.Outlined.RadioButtonUnchecked else Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                )
                            },
                            onClick = { menuOpen = false; onToggleSent() },
                        )
                    }
                }
            }
        },
    ) { Text(if (person.isOrganizer) "${person.name} (you)" else person.name) }
}
