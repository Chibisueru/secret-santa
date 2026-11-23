package io.github.chibisueru.secretsanta.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.chibisueru.secretsanta.data.GroupSummary
import io.github.chibisueru.secretsanta.data.SantaDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GroupsViewModel(private val dao: SantaDao) : ViewModel() {

    /** null while loading. */
    val groups: StateFlow<List<GroupSummary>?> = dao.observeGroups()
        .map<List<GroupSummary>, List<GroupSummary>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun deleteGroup(id: Long) {
        viewModelScope.launch { dao.deleteGroup(id) }
    }
}
