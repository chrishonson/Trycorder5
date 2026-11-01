package net.ddns.mlsoftlaberge.trycorder.crew

/** One line of the contact list. [id] is the platform contact reference (Android lookup URI, iOS identifier). */
data class CrewMember(val id: String, val name: String, val starred: Boolean)

/** A phone, email or address with its type label ("Mobile", "Home"...). */
data class CrewEntry(val label: String, val value: String)

/** Everything the admin page shows. */
data class CrewDetail(
    val id: String,
    val name: String,
    val starred: Boolean,
    val phones: List<CrewEntry> = emptyList(),
    val emails: List<CrewEntry> = emptyList(),
    val addresses: List<CrewEntry> = emptyList(),
    val note: CrewNote = CrewNote(),
)

/** One client of the budget page: the contact and its transactions. */
data class BudgetEntry(
    val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val address: String,
    val transactions: List<CrewTransaction>,
)

/** Outcome of writing the memo and transactions, reported like the original toasts. */
enum class NoteSaveResult(val message: String) {
    Inserted("Transaction Inserted"),
    Updated("Transaction Updated"),
    Failed("Transaction Not Updated"),
}

/**
 * Crew information and evaluation (the original contacts module), over the system address book.
 * Android: ContactsContract, memo and transactions in the contact's Note field like the original.
 * iOS: the Contacts framework, memo and transactions in app storage keyed by contact identifier.
 */
interface CrewDirectory {
    /** False where the address book has no favorites (iOS): the STAR filter is then unavailable. */
    val supportsStarred: Boolean

    /** False where the system contact editor cannot be opened from here (iOS). */
    val canEditContacts: Boolean

    fun hasPermission(): Boolean

    suspend fun members(query: String, starredOnly: Boolean): List<CrewMember>

    suspend fun thumbnail(id: String): ByteArray?

    suspend fun photo(id: String): ByteArray?

    suspend fun detail(id: String): CrewDetail?

    suspend fun saveNote(id: String, note: CrewNote): NoteSaveResult

    /** All contacts that have transactions, for the budget page. */
    suspend fun budget(): List<BudgetEntry>

    /** Opens the system "new contact" editor (the ADD button). */
    fun addContact(): Boolean

    /** Opens the system contact editor (the EDIT button). */
    fun editContact(id: String): Boolean

    /** URL that shows a postal address in the maps app. */
    fun addressUrl(address: String): String

    /** The current local time as "yyyy-MM-dd HH:mm:ss", for new transactions. */
    fun now(): String
}
