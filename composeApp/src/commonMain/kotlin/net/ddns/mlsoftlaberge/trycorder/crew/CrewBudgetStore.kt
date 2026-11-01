package net.ddns.mlsoftlaberge.trycorder.crew

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ddns.mlsoftlaberge.trycorder.core.SoundPlayer
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound

data class CrewBudgetState(
    val loading: Boolean = false,
    val entries: List<BudgetEntry> = emptyList(),
) {
    /** The grand TOTAL line. */
    val total: Double get() = entries.sumOf { e -> e.transactions.sumOf { it.value } }
}

sealed interface CrewBudgetIntent {
    data object Load : CrewBudgetIntent

    /** Back to the list: only the button sound. */
    data object Back : CrewBudgetIntent
}

/** Store of the budget page (ContactsBudgetFragment): every client with transactions, and the grand total. */
class CrewBudgetStore(
    private val directory: CrewDirectory,
    private val sound: SoundPlayer,
) : ViewModel() {
    private val _state = MutableStateFlow(CrewBudgetState())
    val state: StateFlow<CrewBudgetState> = _state.asStateFlow()

    fun dispatch(intent: CrewBudgetIntent) {
        when (intent) {
            CrewBudgetIntent.Load -> load()
            CrewBudgetIntent.Back -> sound.play(TrekSound.KeyOk)
        }
    }

    private fun load() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val entries = directory.budget().filter { it.transactions.isNotEmpty() }
            _state.update { it.copy(loading = false, entries = entries) }
        }
    }
}
