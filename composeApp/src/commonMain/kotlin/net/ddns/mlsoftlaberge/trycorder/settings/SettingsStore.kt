package net.ddns.mlsoftlaberge.trycorder.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ddns.mlsoftlaberge.trycorder.core.SettingsRepository

/** Store of the settings screen (the original PreferenceFragment). Writes through [SettingsRepository]. */
class SettingsStore(private val repository: SettingsRepository) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState(settings = repository.current))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settings.collect { s -> _state.update { it.copy(settings = s) } }
        }
    }

    fun dispatch(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.Click -> click(intent.key)
            is SettingsIntent.Choose -> {
                repository.update { ValueFields.withLanguage(it, intent.key, intent.language) }
                close()
            }
            is SettingsIntent.DraftChanged -> _state.update { it.copy(draft = intent.text) }
            SettingsIntent.ConfirmText -> confirmText()
            SettingsIntent.Dismiss -> close()
        }
    }

    private fun click(key: String) {
        when (SettingsCatalog.find(key)) {
            is Preference.Check -> repository.update { CheckFields.set(it, key, !CheckFields.get(it, key)) }
            is Preference.LanguageChoice -> _state.update { it.copy(openKey = key) }
            is Preference.Text -> _state.update { it.copy(openKey = key, draft = ValueFields.text(it.settings, key)) }
            null -> Unit
        }
    }

    private fun confirmText() {
        val key = _state.value.openKey ?: return
        val text = _state.value.draft
        repository.update { ValueFields.withText(it, key, text) }
        close()
    }

    private fun close() = _state.update { it.copy(openKey = null, draft = "") }
}
