package net.ddns.mlsoftlaberge.trycorder.crew

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ddns.mlsoftlaberge.trycorder.core.SoundPlayer
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound

/** Store of the contact list (ContactsListFragment) and of the navigation between the crew pages. */
class CrewListStore(
    private val directory: CrewDirectory,
    private val sound: SoundPlayer,
) : ViewModel() {
    private val _state = MutableStateFlow(CrewListState(starredOnly = directory.supportsStarred))
    val state: StateFlow<CrewListState> = _state.asStateFlow()
    private var loadJob: Job? = null

    fun dispatch(intent: CrewListIntent) {
        when (intent) {
            is CrewListIntent.PermissionResult -> onPermission(intent.granted)
            CrewListIntent.Refresh -> if (_state.value.page == CrewPage.List) reload()
            is CrewListIntent.Search -> _state.update { it.copy(query = intent.text) }.also { reload() }
            CrewListIntent.DismissMessage -> _state.update { it.copy(message = null) }
            CrewListIntent.ClosePage -> _state.update { it.copy(page = CrewPage.List) }.also { reload() }
            else -> buttonSound().also { onButton(intent) }
        }
    }

    /** The header buttons and the list items: all play the key sound first. */
    private fun onButton(intent: CrewListIntent) {
        when (intent) {
            CrewListIntent.ToggleStarred -> toggleStarred()
            is CrewListIntent.Open -> _state.update { it.copy(page = CrewPage.Admin(intent.id)) }
            CrewListIntent.Add -> if (!directory.addContact()) show(NOT_AVAILABLE)
            CrewListIntent.OpenBudget -> _state.update { it.copy(page = CrewPage.Budget) }
            else -> Unit
        }
    }

    private fun buttonSound() = sound.play(TrekSound.KeyOk)

    private fun show(message: String) = _state.update { it.copy(message = message) }

    private fun onPermission(granted: Boolean) {
        val access = if (granted || directory.hasPermission()) ContactsAccess.Granted else ContactsAccess.Denied
        _state.update { it.copy(access = access, message = if (access == ContactsAccess.Denied) DENIED else null) }
        reload()
    }

    private fun toggleStarred() {
        if (!directory.supportsStarred) {
            show(NO_FAVORITES)
            return
        }
        _state.update { it.copy(starredOnly = !it.starredOnly) }
        reload()
    }

    private fun reload() {
        val s = _state.value
        if (s.access != ContactsAccess.Granted) return
        loadJob?.cancel()
        _state.update { it.copy(loading = true) }
        loadJob = viewModelScope.launch {
            val members = directory.members(s.query, s.starredOnly)
            _state.update { it.copy(members = members, loading = false) }
            members.filterNot { it.id in _state.value.thumbnails }.forEach { m ->
                val bytes = directory.thumbnail(m.id) ?: return@forEach
                _state.update { it.copy(thumbnails = it.thumbnails + (m.id to bytes)) }
            }
        }
    }

    companion object {
        const val DENIED = "Permission to read the contacts was denied"
        const val NO_FAVORITES = "Favorites are not available on this device"
        const val NOT_AVAILABLE = "No application found to handle this action!"
    }
}
