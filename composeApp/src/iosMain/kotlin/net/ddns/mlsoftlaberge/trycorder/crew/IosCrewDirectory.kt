package net.ddns.mlsoftlaberge.trycorder.crew

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ddns.mlsoftlaberge.trycorder.inventory.AppFileAccessor
import net.ddns.mlsoftlaberge.trycorder.inventory.iosDocumentsDirectory
import platform.Contacts.CNAuthorizationStatusDenied
import platform.Contacts.CNAuthorizationStatusNotDetermined
import platform.Contacts.CNAuthorizationStatusRestricted
import platform.Contacts.CNContact
import platform.Contacts.CNContactEmailAddressesKey
import platform.Contacts.CNContactFetchRequest
import platform.Contacts.CNContactFormatter
import platform.Contacts.CNContactFormatterStyle
import platform.Contacts.CNContactIdentifierKey
import platform.Contacts.CNContactImageDataKey
import platform.Contacts.CNContactPhoneNumbersKey
import platform.Contacts.CNContactPostalAddressesKey
import platform.Contacts.CNContactStore
import platform.Contacts.CNContactThumbnailImageDataKey
import platform.Contacts.CNEntityType
import platform.Contacts.CNLabeledValue
import platform.Contacts.CNPhoneNumber
import platform.Contacts.CNPostalAddress
import platform.Contacts.CNPostalAddressFormatter
import platform.Contacts.CNPostalAddressFormatterStyle
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.posix.memcpy

private const val NOTES_FILE = "crew.json"

/** True when the app may read the address book (authorized, or limited access on iOS 18). */
fun contactsAccessGranted(): Boolean {
    val status = CNContactStore.authorizationStatusForEntityType(CNEntityType.CNEntityTypeContacts)
    return status != CNAuthorizationStatusNotDetermined &&
        status != CNAuthorizationStatusDenied &&
        status != CNAuthorizationStatusRestricted
}

/**
 * The contacts module over the Contacts framework. Writing the contact note needs the
 * com.apple.developer.contacts.notes entitlement, so memo and transactions are kept in app storage
 * (Documents/crew.json) keyed by contact identifier, in the same text format as the Android note.
 * iOS has no favorites in the Contacts framework and no editor reachable without UIKit
 * presentation: the STAR filter and the ADD / EDIT buttons are unavailable.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosCrewDirectory : CrewDirectory {
    private val store = CNContactStore()
    private val notes by lazy { CrewNoteStore(AppFileAccessor(iosDocumentsDirectory(), NOTES_FILE)) }
    private val nameKeys = CNContactFormatter.descriptorForRequiredKeysForStyle(
        CNContactFormatterStyle.CNContactFormatterStyleFullName,
    )

    override val supportsStarred: Boolean = false
    override val canEditContacts: Boolean = false

    override fun hasPermission(): Boolean = contactsAccessGranted()

    override suspend fun members(query: String, starredOnly: Boolean): List<CrewMember> =
        if (hasPermission()) withContext(Dispatchers.Default) { fetchMembers(query) } else emptyList()

    private fun fetchMembers(query: String): List<CrewMember> {
        val found = mutableListOf<CrewMember>()
        val request = CNContactFetchRequest(keysToFetch = listOf(CNContactIdentifierKey, nameKeys))
        store.enumerateContactsWithFetchRequest(request, error = null) { contact, _ ->
            contact?.let { c ->
                val name = nameOf(c)
                if (name.isNotEmpty() && (query.isBlank() || name.contains(query.trim(), ignoreCase = true))) {
                    found += CrewMember(c.identifier, name, starred = false)
                }
            }
        }
        return found.sortedBy { it.name.lowercase() }
    }

    override suspend fun thumbnail(id: String): ByteArray? = withContext(Dispatchers.Default) {
        contact(id, listOf(CNContactThumbnailImageDataKey))?.thumbnailImageData?.toByteArray()
    }

    override suspend fun photo(id: String): ByteArray? = withContext(Dispatchers.Default) {
        contact(id, listOf(CNContactImageDataKey))?.imageData?.toByteArray()
    }

    override suspend fun detail(id: String): CrewDetail? {
        val c = withContext(Dispatchers.Default) { contact(id, DETAIL_KEYS + nameKeys) } ?: return null
        return CrewDetail(
            id = id,
            name = nameOf(c),
            starred = false,
            phones = c.phoneNumbers.entries { (it as? CNPhoneNumber)?.stringValue },
            emails = c.emailAddresses.entries { it as? String },
            addresses = c.postalAddresses.entries { postal(it as? CNPostalAddress) },
            note = CrewNoteCodec.decode(notes.get(id)),
        )
    }

    override suspend fun saveNote(id: String, note: CrewNote): NoteSaveResult =
        if (notes.put(id, CrewNoteCodec.encode(note))) NoteSaveResult.Updated else NoteSaveResult.Inserted

    override suspend fun budget(): List<BudgetEntry> = notes.all().mapNotNull { (id, text) ->
        val transactions = CrewNoteCodec.decode(text).transactions
        val c = withContext(Dispatchers.Default) { contact(id, DETAIL_KEYS + nameKeys) }
        if (transactions.isEmpty() || c == null) {
            null
        } else {
            BudgetEntry(
                id = id.take(ID_LENGTH),
                name = nameOf(c),
                phone = c.phoneNumbers.entries { (it as? CNPhoneNumber)?.stringValue }.firstOrNull()?.value.orEmpty(),
                email = c.emailAddresses.entries { it as? String }.firstOrNull()?.value.orEmpty(),
                address = c.postalAddresses.entries { postal(it as? CNPostalAddress) }.firstOrNull()?.value.orEmpty(),
                transactions = transactions,
            )
        }
    }.sortedBy { it.name.lowercase() }

    override fun addContact(): Boolean = false

    override fun editContact(id: String): Boolean = false

    override fun addressUrl(address: String): String = "https://maps.apple.com/?q=" + UrlCodec.encode(address)

    override fun now(): String {
        val formatter = NSDateFormatter()
        formatter.locale = NSLocale(localeIdentifier = "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return formatter.stringFromDate(NSDate())
    }

    private fun contact(id: String, keys: List<Any?>): CNContact? =
        if (hasPermission()) store.unifiedContactWithIdentifier(id, keysToFetch = keys, error = null) else null

    private fun nameOf(c: CNContact): String =
        CNContactFormatter.stringFromContact(c, CNContactFormatterStyle.CNContactFormatterStyleFullName).orEmpty()


    private companion object {
        val DETAIL_KEYS: List<Any?> = listOf(
            CNContactIdentifierKey,
            CNContactPhoneNumbersKey,
            CNContactEmailAddressesKey,
            CNContactPostalAddressesKey,
        )

        /** The budget shows a short id, like the raw contact id of the original. */
        const val ID_LENGTH = 8
    }
}

private fun postal(address: CNPostalAddress?): String? = address?.let {
    CNPostalAddressFormatter.stringFromPostalAddress(
        it,
        CNPostalAddressFormatterStyle.CNPostalAddressFormatterStyleMailingAddress,
    )
}

/** Labeled values (phones, emails, addresses) with their localized label. */
private fun List<*>.entries(value: (Any?) -> String?): List<CrewEntry> = mapNotNull { item ->
    val labeled = item as? CNLabeledValue ?: return@mapNotNull null
    val text = value(labeled.value) ?: return@mapNotNull null
    CrewEntry(labeled.label?.let { CNLabeledValue.localizedStringForLabel(it) }.orEmpty(), text)
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val out = ByteArray(size)
    if (size > 0) out.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return out
}
