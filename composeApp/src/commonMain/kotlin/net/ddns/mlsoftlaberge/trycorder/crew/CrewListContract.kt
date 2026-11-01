package net.ddns.mlsoftlaberge.trycorder.crew

/** The activities of the original contacts module, shown inside the crew screen. */
sealed interface CrewPage {
    /** ContactsListActivity. */
    data object List : CrewPage

    /** ContactAdminActivity (and its memo/transaction editors). */
    data class Admin(val id: String) : CrewPage

    /** ContactsBudgetActivity. */
    data object Budget : CrewPage
}

enum class ContactsAccess { Unknown, Granted, Denied }

data class CrewListState(
    val access: ContactsAccess = ContactsAccess.Unknown,
    val members: List<CrewMember> = emptyList(),
    val query: String = "",
    /** The original list started on the starred contacts (starredfind=1). */
    val starredOnly: Boolean = true,
    val loading: Boolean = false,
    /** Encoded contact thumbnails by contact id, filled in the background. */
    val thumbnails: Map<String, ByteArray> = emptyMap(),
    val page: CrewPage = CrewPage.List,
    val message: String? = null,
)

sealed interface CrewListIntent {
    data class PermissionResult(val granted: Boolean) : CrewListIntent

    /** Back on screen (and the list after the system contact editor). */
    data object Refresh : CrewListIntent
    data class Search(val text: String) : CrewListIntent
    data object ToggleStarred : CrewListIntent
    data class Open(val id: String) : CrewListIntent
    data object Add : CrewListIntent
    data object OpenBudget : CrewListIntent

    /** Back from the admin or budget page to the list. */
    data object ClosePage : CrewListIntent

    /** BACK of the list: only the button sound, App navigates back. */
    data object Back : CrewListIntent
    data object DismissMessage : CrewListIntent
}
