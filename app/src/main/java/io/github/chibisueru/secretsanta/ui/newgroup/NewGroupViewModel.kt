package io.github.chibisueru.secretsanta.ui.newgroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.chibisueru.secretsanta.data.GroupEntity
import io.github.chibisueru.secretsanta.data.SantaDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OrganizerPrefill(val name: String, val phone: String, val email: String)

class NewGroupViewModel(private val dao: SantaDao) : ViewModel() {

    /** The organizer's details from the most recent group, to save typing them again. */
    private val _prefill = MutableStateFlow<OrganizerPrefill?>(null)
    val prefill: StateFlow<OrganizerPrefill?> = _prefill.asStateFlow()

    private var creating = false

    init {
        viewModelScope.launch {
            dao.latestGroup()
                ?.takeIf { it.organizerName.isNotBlank() }
                ?.let { _prefill.value = OrganizerPrefill(it.organizerName, it.organizerPhone, it.organizerEmail) }
        }
    }

    fun create(
        groupName: String,
        organizerName: String,
        organizerPhone: String,
        organizerEmail: String,
        participates: Boolean,
        onCreated: (Long) -> Unit,
    ) {
        if (creating) return
        creating = true
        viewModelScope.launch {
            val id = dao.createGroup(
                GroupEntity(
                    name = groupName.trim(),
                    organizerName = organizerName.trim(),
                    organizerPhone = organizerPhone.trim(),
                    organizerEmail = organizerEmail.trim(),
                ),
                organizerParticipates = participates,
            )
            onCreated(id)
        }
    }
}
