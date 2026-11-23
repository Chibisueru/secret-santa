package io.github.chibisueru.secretsanta.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.chibisueru.secretsanta.data.ExclusionEntity
import io.github.chibisueru.secretsanta.data.GroupEntity
import io.github.chibisueru.secretsanta.data.ParticipantEntity
import io.github.chibisueru.secretsanta.data.SantaDao
import io.github.chibisueru.secretsanta.domain.DrawPerson
import io.github.chibisueru.secretsanta.domain.DrawResult
import io.github.chibisueru.secretsanta.domain.DrawRule
import io.github.chibisueru.secretsanta.domain.SecretSantaDraw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class GroupDetails(
    val name: String,
    val budget: String,
    val eventDateMillis: Long?,
    val location: String,
    val notes: String,
)

/** A rule as shown in the UI: one row per pair, mutual rules merged. */
data class RulePair(
    val from: ParticipantEntity,
    val to: ParticipantEntity,
    val mutual: Boolean,
)

data class GroupUiState(
    val loading: Boolean = true,
    val group: GroupEntity? = null,
    val participants: List<ParticipantEntity> = emptyList(),
    val exclusions: List<ExclusionEntity> = emptyList(),
) {
    val isDrawn: Boolean get() = group?.isDrawn == true
    val canDraw: Boolean get() = participants.size >= SecretSantaDraw.MIN_PARTICIPANTS
    val organizerParticipant: ParticipantEntity? get() = participants.firstOrNull { it.isOrganizer }

    val rules: List<RulePair> by lazy {
        val byId = participants.associateBy { it.id }
        val keys = exclusions.map { it.fromId to it.toId }.toSet()
        exclusions.mapNotNull { e ->
            val mutual = (e.toId to e.fromId) in keys
            // For mutual rules keep only one of the two rows.
            if (mutual && e.fromId > e.toId) return@mapNotNull null
            val from = byId[e.fromId] ?: return@mapNotNull null
            val to = byId[e.toId] ?: return@mapNotNull null
            RulePair(from, to, mutual)
        }.sortedWith(compareBy({ it.from.name.lowercase() }, { it.to.name.lowercase() }))
    }
}

sealed interface GroupEvent {
    data object DrawSucceeded : GroupEvent
    data class DrawFailed(val reason: String) : GroupEvent
}

class GroupViewModel(
    private val dao: SantaDao,
    private val appScope: CoroutineScope,
    private val groupId: Long,
) : ViewModel() {

    val state: StateFlow<GroupUiState> = combine(
        dao.observeGroup(groupId),
        dao.observeParticipants(groupId),
        dao.observeExclusions(groupId),
    ) { group, participants, exclusions ->
        GroupUiState(
            loading = false,
            group = group,
            participants = participants,
            exclusions = exclusions,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupUiState())

    private val _events = Channel<GroupEvent>(Channel.BUFFERED)
    val events: Flow<GroupEvent> = _events.receiveAsFlow()

    /** Not saved: pairings stay hidden whenever the screen is reopened. */
    private val _revealAll = MutableStateFlow(false)
    val revealAll: StateFlow<Boolean> = _revealAll.asStateFlow()

    // Detail edits are written per keystroke. The mutex keeps them in order, and appScope
    // lets the last one finish even if the user leaves the screen right away.
    private val writeMutex = Mutex()

    fun updateDetails(details: GroupDetails) {
        appScope.launch {
            writeMutex.withLock {
                dao.updateDetails(
                    id = groupId,
                    name = details.name.trim().ifEmpty { "Untitled group" },
                    budget = details.budget,
                    eventDateMillis = details.eventDateMillis,
                    location = details.location,
                    notes = details.notes,
                )
            }
        }
    }

    fun saveParticipant(existing: ParticipantEntity?, name: String, phone: String, email: String) {
        if (existing?.isOrganizer == true) {
            updateOrganizer(name, phone, email)
            return
        }
        viewModelScope.launch {
            if (existing == null) {
                dao.insertParticipant(
                    ParticipantEntity(
                        groupId = groupId,
                        name = name.trim(),
                        phone = phone.trim(),
                        email = email.trim(),
                    ),
                )
            } else {
                dao.updateParticipant(
                    existing.copy(name = name.trim(), phone = phone.trim(), email = email.trim()),
                )
            }
        }
    }

    /** Also updates the organizer's participant row, so the two never drift apart. */
    fun updateOrganizer(name: String, phone: String, email: String) {
        viewModelScope.launch { dao.updateOrganizer(groupId, name.trim(), phone.trim(), email.trim()) }
    }

    fun setOrganizerParticipates(participates: Boolean) {
        viewModelScope.launch { dao.setOrganizerParticipates(groupId, participates) }
    }

    fun deleteParticipant(participant: ParticipantEntity) {
        viewModelScope.launch { dao.deleteParticipant(participant) }
    }

    fun addRule(fromId: Long, toId: Long, bothWays: Boolean) {
        if (fromId == toId) return
        viewModelScope.launch {
            val rows = buildList {
                add(ExclusionEntity(groupId = groupId, fromId = fromId, toId = toId))
                if (bothWays) add(ExclusionEntity(groupId = groupId, fromId = toId, toId = fromId))
            }
            dao.insertExclusions(rows)
        }
    }

    fun deleteRule(rule: RulePair) {
        viewModelScope.launch {
            if (rule.mutual) {
                dao.deleteExclusionPair(rule.from.id, rule.to.id)
            } else {
                dao.deleteExclusion(rule.from.id, rule.to.id)
            }
        }
    }

    fun draw() {
        viewModelScope.launch {
            val people = dao.getParticipants(groupId).map { DrawPerson(it.id, it.name) }
            val rules = dao.getExclusions(groupId).map { DrawRule(it.fromId, it.toId) }
            when (val result = SecretSantaDraw.draw(people, rules)) {
                is DrawResult.Success -> {
                    dao.saveDraw(groupId, result.assignments)
                    _revealAll.value = false
                    _events.send(GroupEvent.DrawSucceeded)
                }
                is DrawResult.Failure -> _events.send(GroupEvent.DrawFailed(result.reason))
            }
        }
    }

    fun resetDraw() {
        viewModelScope.launch {
            dao.resetDraw(groupId)
            _revealAll.value = false
        }
    }

    fun setNotified(participant: ParticipantEntity, notified: Boolean) {
        viewModelScope.launch { dao.setNotified(participant.id, notified) }
    }

    fun setRevealAll(reveal: Boolean) {
        _revealAll.value = reveal
    }

    fun deleteGroup(onDeleted: () -> Unit) {
        viewModelScope.launch {
            dao.deleteGroup(groupId)
            onDeleted()
        }
    }
}
