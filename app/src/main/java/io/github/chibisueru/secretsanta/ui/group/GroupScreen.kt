package io.github.chibisueru.secretsanta.ui.group

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.chibisueru.secretsanta.domain.SecretSantaDraw

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    viewModel: GroupViewModel,
    onBack: () -> Unit,
    onOpenShare: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var drawError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                GroupEvent.DrawSucceeded -> onOpenShare()
                is GroupEvent.DrawFailed -> drawError = event.reason
            }
        }
    }

    val group = state.group
    // The group disappeared (deleted from elsewhere): leave.
    LaunchedEffect(state.loading, group) {
        if (!state.loading && group == null) onBack()
    }
    if (group == null) return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isDrawn) {
                        IconButton(onClick = { confirmReset = true }) {
                            Icon(Icons.Outlined.RestartAlt, contentDescription = "Reset draw")
                        }
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete group")
                    }
                },
            )
        },
        bottomBar = {
            DrawBar(
                isDrawn = state.isDrawn,
                canDraw = state.canDraw,
                onDraw = viewModel::draw,
                onOpenShare = onOpenShare,
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Details") })
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text("People · ${state.participants.size}") },
                )
                Tab(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    text = { Text("Rules · ${state.rules.size}") },
                )
            }
            when (tab) {
                0 -> DetailsTab(
                    group = group,
                    state = state,
                    onChange = viewModel::updateDetails,
                    onEditOrganizer = viewModel::updateOrganizer,
                    onParticipates = viewModel::setOrganizerParticipates,
                )
                1 -> PeopleTab(
                    state = state,
                    onSave = viewModel::saveParticipant,
                    onDelete = viewModel::deleteParticipant,
                    onResetDraw = { confirmReset = true },
                )
                else -> RulesTab(
                    state = state,
                    onAdd = viewModel::addRule,
                    onDelete = viewModel::deleteRule,
                    onResetDraw = { confirmReset = true },
                )
            }
        }
    }

    drawError?.let { reason ->
        AlertDialog(
            onDismissRequest = { drawError = null },
            icon = { Icon(Icons.Outlined.ErrorOutline, contentDescription = null) },
            title = { Text("Can't draw names") },
            text = { Text(reason) },
            confirmButton = { TextButton(onClick = { drawError = null }) { Text("OK") } },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            icon = { Icon(Icons.Outlined.RestartAlt, contentDescription = null) },
            title = { Text("Reset the draw?") },
            text = {
                Text(
                    "All matches will be removed so you can change people and rules. " +
                        "Anyone you've already messaged will need a new match.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    viewModel.resetDraw()
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text("Delete “${group.name}”?") },
            text = { Text("All participants, rules and matches in this group will be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteGroup(onDeleted = onBack)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DrawBar(
    isDrawn: Boolean,
    canDraw: Boolean,
    onDraw: () -> Unit,
    onOpenShare: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isDrawn) {
                Button(onClick = onOpenShare, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share matches")
                }
            } else {
                Button(
                    onClick = onDraw,
                    enabled = canDraw,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.Filled.Casino, contentDescription = null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Draw names")
                }
                if (!canDraw) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Add at least ${SecretSantaDraw.MIN_PARTICIPANTS} people to draw names",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
