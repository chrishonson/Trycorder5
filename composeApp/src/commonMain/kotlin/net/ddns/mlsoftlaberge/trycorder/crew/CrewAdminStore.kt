package net.ddns.mlsoftlaberge.trycorder.crew

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ddns.mlsoftlaberge.trycorder.core.ExternalLauncher
import net.ddns.mlsoftlaberge.trycorder.core.SoundPlayer
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound

/** Store of the contact admin page (ContactAdminFragment) and of its memo/transaction editors. */
class CrewAdminStore(
    private val directory: CrewDirectory,
    private val sound: SoundPlayer,
    private val launcher: ExternalLauncher,
) : ViewModel() {
    private val _state = MutableStateFlow(CrewAdminState(canEditContact = directory.canEditContacts))
    val state: StateFlow<CrewAdminState> = _state.asStateFlow()
    private var loadJob: Job? = null

    fun dispatch(intent: CrewAdminIntent) {
        when (intent) {
            is CrewAdminIntent.Load -> load(intent.id, fresh = true)
            CrewAdminIntent.Refresh -> _state.value.contactId?.let { if (_state.value.editor == null) load(it, false) }
            CrewAdminIntent.Back -> buttonSound()
            CrewAdminIntent.DismissMessage -> _state.update { it.copy(message = null) }
            is CrewAdminIntent.Action -> buttonSound().also { onAction(intent) }
            is CrewAdminIntent.Editor -> onEditor(intent)
        }
    }

    private fun buttonSound() = sound.play(TrekSound.KeyOk)

    private fun show(message: String) = _state.update { it.copy(message = message) }

    private fun load(id: String, fresh: Boolean) {
        loadJob?.cancel()
        if (fresh) {
            _state.update { CrewAdminState(contactId = id, loading = true, canEditContact = directory.canEditContacts) }
        }
        loadJob = viewModelScope.launch {
            val detail = directory.detail(id)
            _state.update { s ->
                if (detail == null) {
                    s.copy(loading = false)
                } else {
                    s.copy(
                        loading = false,
                        name = detail.name,
                        starred = detail.starred,
                        phones = detail.phones,
                        emails = detail.emails,
                        addresses = detail.addresses,
                        note = detail.note,
                    )
                }
            }
            val photo = directory.photo(id)
            _state.update { it.copy(photo = photo) }
        }
    }

    private fun onAction(action: CrewAdminIntent.Action) {
        val s = _state.value
        when (action) {
            CrewAdminIntent.EditContact -> s.contactId?.let { if (!directory.editContact(it)) show(NO_APP) }
            CrewAdminIntent.EditMemo -> _state.update { it.copy(editor = CrewEditor.Memo(s.note.memo)) }
            CrewAdminIntent.AddTransaction -> _state.update {
                it.copy(editor = CrewEditor.Transaction(null, directory.now(), "", ""))
            }
            is CrewAdminIntent.EditTransaction -> s.note.transactions.getOrNull(action.index)?.let { t ->
                val editor = CrewEditor.Transaction(action.index, t.date, t.description, t.amount)
                _state.update { it.copy(editor = editor) }
            }
            CrewAdminIntent.SaveNote -> save(s.note)
            is CrewAdminIntent.Call -> open(CrewLinks.phone(action.phone), NO_APP)
            is CrewAdminIntent.Email -> open(CrewLinks.email(action.email, s.note.transactions), EMAIL_ERROR)
            is CrewAdminIntent.ShowAddress -> open(directory.addressUrl(action.address), NO_APP)
        }
    }

    private fun open(url: String, failure: String) {
        if (!launcher.openUrl(url)) show(failure)
    }

    private fun onEditor(intent: CrewAdminIntent.Editor) {
        val editor = _state.value.editor ?: return
        if (intent == CrewAdminIntent.EditorSave || intent == CrewAdminIntent.EditorClose) buttonSound()
        val outcome = CrewNoteEditing.reduce(editor, _state.value.note, intent)
        _state.update { it.copy(editor = outcome.editor, note = outcome.noteToSave ?: it.note) }
        outcome.noteToSave?.let { save(it) }
    }

    private fun save(note: CrewNote) {
        val id = _state.value.contactId ?: return
        viewModelScope.launch { show(directory.saveNote(id, note).message) }
    }

    companion object {
        const val NO_APP = "No application found to handle this action!"
        const val EMAIL_ERROR = "Email Error"
    }
}

/** The tel: and mailto: links of the call and email buttons. */
object CrewLinks {
    const val SUBJECT = "Account State"
    private const val DIAL_CHARS = "+*,;"

    fun phone(number: String): String =
        "tel:" + number.filter { it.isDigit() || it in DIAL_CHARS || it == '#' }.replace("#", "%23")

    /** The original sent the account statement (prepare_invoice) to the contact. */
    fun email(address: String, transactions: List<CrewTransaction>): String =
        "mailto:" + address.trim() + "?subject=" + UrlCodec.encode(SUBJECT) +
            "&body=" + UrlCodec.encode(Invoice.build(transactions))
}
