package net.ddns.mlsoftlaberge.trycorder.crew

/** Result of an editor intent: the next editor (null closes it) and the note to save, if any. */
data class EditorOutcome(val editor: CrewEditor?, val noteToSave: CrewNote? = null)

/**
 * The onActivityResult logic of ContactAdminFragment for the memo and transaction editors,
 * as a pure reducer.
 */
object CrewNoteEditing {
    fun reduce(editor: CrewEditor, note: CrewNote, intent: CrewAdminIntent.Editor): EditorOutcome = when (intent) {
        is CrewAdminIntent.MemoChanged ->
            EditorOutcome((editor as? CrewEditor.Memo)?.copy(text = intent.text) ?: editor)
        is CrewAdminIntent.TransChanged -> EditorOutcome((editor as? CrewEditor.Transaction)?.with(intent) ?: editor)
        CrewAdminIntent.EditorSave -> save(editor, note)
        CrewAdminIntent.EditorDelete -> EditorOutcome(editor.withConfirm(true))
        CrewAdminIntent.EditorCancelDelete -> EditorOutcome(editor.withConfirm(false))
        CrewAdminIntent.EditorConfirmDelete -> delete(editor, note)
        CrewAdminIntent.EditorClose -> EditorOutcome(null)
    }

    /** SAVE: the memo text, or the transaction (removed when description and amount are both empty). */
    private fun save(editor: CrewEditor, note: CrewNote): EditorOutcome = when (editor) {
        is CrewEditor.Memo -> EditorOutcome(null, note.copy(memo = CrewNoteCodec.normalizeMemo(editor.text)))
        is CrewEditor.Transaction -> {
            val next = if (editor.description.isEmpty() && editor.amount.isEmpty()) {
                note.transactions.without(editor.index)
            } else {
                note.transactions.upsert(
                    editor.index,
                    CrewNoteCodec.transaction(editor.date, editor.amount, editor.description),
                )
            }
            EditorOutcome(null, note.copy(transactions = next))
        }
    }

    /** DELETE of the popup: the editor returned empty values. */
    private fun delete(editor: CrewEditor, note: CrewNote): EditorOutcome = when (editor) {
        is CrewEditor.Memo -> EditorOutcome(null, note.copy(memo = ""))
        is CrewEditor.Transaction ->
            EditorOutcome(null, note.copy(transactions = note.transactions.without(editor.index)))
    }

    private fun CrewEditor.Transaction.with(intent: CrewAdminIntent.TransChanged) = when (intent.field) {
        TransField.Date -> copy(date = intent.value)
        TransField.Description -> copy(description = intent.value)
        TransField.Amount -> copy(amount = intent.value)
    }

    private fun CrewEditor.withConfirm(confirm: Boolean): CrewEditor = when (this) {
        is CrewEditor.Memo -> copy(confirmDelete = confirm)
        is CrewEditor.Transaction -> copy(confirmDelete = confirm)
    }

    private fun List<CrewTransaction>.without(index: Int?) =
        if (index == null) this else filterIndexed { i, _ -> i != index }

    private fun List<CrewTransaction>.upsert(index: Int?, t: CrewTransaction) =
        if (index == null || index !in indices) this + t else mapIndexed { i, old -> if (i == index) t else old }
}
