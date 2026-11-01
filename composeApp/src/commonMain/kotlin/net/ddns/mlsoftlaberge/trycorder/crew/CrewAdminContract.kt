package net.ddns.mlsoftlaberge.trycorder.crew

/** The editor activities opened from the admin page. */
sealed interface CrewEditor {
    val confirmDelete: Boolean

    /** ContactEditMemoActivity. */
    data class Memo(val text: String, override val confirmDelete: Boolean = false) : CrewEditor

    /** ContactEditTransActivity; [index] is null for a transaction added with the + button. */
    data class Transaction(
        val index: Int?,
        val date: String,
        val description: String,
        val amount: String,
        override val confirmDelete: Boolean = false,
    ) : CrewEditor
}

data class CrewAdminState(
    val contactId: String? = null,
    val loading: Boolean = false,
    val name: String = "",
    val starred: Boolean = false,
    val phones: List<CrewEntry> = emptyList(),
    val emails: List<CrewEntry> = emptyList(),
    val addresses: List<CrewEntry> = emptyList(),
    val note: CrewNote = CrewNote(),
    /** Encoded contact photo, or null for ic_contact_picture. */
    val photo: ByteArray? = null,
    val editor: CrewEditor? = null,
    val canEditContact: Boolean = true,
    val message: String? = null,
)

enum class TransField { Date, Description, Amount }

sealed interface CrewAdminIntent {
    data class Load(val id: String) : CrewAdminIntent

    /** Back on screen after the system contact editor. */
    data object Refresh : CrewAdminIntent
    data object Back : CrewAdminIntent
    data object DismissMessage : CrewAdminIntent

    /** Buttons of the admin page. */
    sealed interface Action : CrewAdminIntent
    data object EditContact : Action
    data object EditMemo : Action
    data object AddTransaction : Action
    data class EditTransaction(val index: Int) : Action
    data object SaveNote : Action
    data class Call(val phone: String) : Action
    data class Email(val email: String) : Action
    data class ShowAddress(val address: String) : Action

    /** Buttons and fields of the memo and transaction editors. */
    sealed interface Editor : CrewAdminIntent
    data class MemoChanged(val text: String) : Editor
    data class TransChanged(val field: TransField, val value: String) : Editor
    data object EditorSave : Editor

    /** DEL: opens the CANCEL/DELETE popup. */
    data object EditorDelete : Editor
    data object EditorConfirmDelete : Editor
    data object EditorCancelDelete : Editor

    /** BACK of an editor: leaves without changes. */
    data object EditorClose : Editor
}
