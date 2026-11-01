package net.ddns.mlsoftlaberge.trycorder.crew

import android.content.ContentProviderOperation
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data

/** One row of the Data table: a phone, email, postal address or note of a contact. */
internal data class DataRow(
    val id: Long,
    val rawContactId: Long,
    val mimeType: String,
    val value: String,
    val type: Int,
    val label: String?,
)

/**
 * The ContactsContract queries of the original fragments (ContactsQuery, ContactDetailQuery, the address,
 * phone, email and notes queries), plus the note write of savenote().
 * Calls block and may throw SecurityException when the permission is missing.
 */
internal class ContactQueries(private val context: Context) {
    private val resolver get() = context.contentResolver

    fun members(query: String, starredOnly: Boolean): List<CrewMember> {
        val uri = if (query.isBlank()) {
            Contacts.CONTENT_URI
        } else {
            Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, Uri.encode(query.trim()))
        }
        val selection = VISIBLE + if (starredOnly) " AND ${Contacts.STARRED}=1" else ""
        return rows(uri, MEMBER_PROJECTION, selection, null, Contacts.SORT_KEY_PRIMARY) { c ->
            CrewMember(
                id = Contacts.getLookupUri(c.long(Contacts._ID), c.text(Contacts.LOOKUP_KEY)).toString(),
                name = c.text(Contacts.DISPLAY_NAME_PRIMARY),
                starred = c.int(Contacts.STARRED) == 1,
            )
        }
    }

    /** The contact URI of a lookup URI (survives contact aggregation changes like the original lookup URIs). */
    fun contactUri(id: String): Uri? = Contacts.lookupContact(resolver, Uri.parse(id))

    fun photo(id: String, highRes: Boolean): ByteArray? {
        val uri = contactUri(id) ?: return null
        return Contacts.openContactPhotoInputStream(resolver, uri, highRes)?.use { it.readBytes() }
    }

    fun detail(id: String): CrewDetail? {
        val head = contactUri(id)?.let { head(it) } ?: return null
        val data = dataRows(head.contactId)
        val res = context.resources
        return CrewDetail(
            id = id,
            name = head.name,
            starred = head.starred,
            phones = data.of(Phone.CONTENT_ITEM_TYPE) { Phone.getTypeLabel(res, it.type, it.label) },
            emails = data.of(Email.CONTENT_ITEM_TYPE) { Email.getTypeLabel(res, it.type, it.label) },
            addresses = data.of(StructuredPostal.CONTENT_ITEM_TYPE) {
                StructuredPostal.getTypeLabel(res, it.type, it.label)
            },
            note = CrewNoteCodec.decode(data.firstOrNull { it.mimeType == Note.CONTENT_ITEM_TYPE }?.value.orEmpty()),
        )
    }

    /** savenote(): updates the first note row of the contact, or inserts one on its name raw contact. */
    fun writeNote(id: String, text: String): NoteSaveResult {
        val head = contactUri(id)?.let { head(it) } ?: return NoteSaveResult.Failed
        val note = dataRows(head.contactId).firstOrNull { it.mimeType == Note.CONTENT_ITEM_TYPE }
        val op = if (note != null) {
            ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                .withSelection("${Data._ID}=?", arrayOf(note.id.toString()))
                .withValue(Note.NOTE, text)
                .build()
        } else {
            ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValue(Data.RAW_CONTACT_ID, head.nameRawContactId)
                .withValue(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE)
                .withValue(Note.NOTE, text)
                .build()
        }
        resolver.applyBatch(ContactsContract.AUTHORITY, arrayListOf(op))
        return if (note != null) NoteSaveResult.Updated else NoteSaveResult.Inserted
    }

    /** ContactsBudgetFragment: every contact whose note holds ADMIN| lines. */
    fun budget(): List<BudgetEntry> {
        val notes = rows(Data.CONTENT_URI, BUDGET_PROJECTION, BUDGET_SELECTION, BUDGET_ARGS, null) { c ->
            BudgetNote(
                contactId = c.long(Data.CONTACT_ID),
                rawContactId = c.long(Data.RAW_CONTACT_ID),
                name = c.text(Data.DISPLAY_NAME_PRIMARY),
                note = c.text(Note.NOTE),
            )
        }
        return notes.distinctBy { it.contactId }.map { n ->
            val data = dataRows(n.contactId)
            fun first(mime: String) = data.firstOrNull { it.mimeType == mime }?.value.orEmpty()
            BudgetEntry(
                id = n.rawContactId.toString(),
                name = n.name,
                phone = first(Phone.CONTENT_ITEM_TYPE),
                email = first(Email.CONTENT_ITEM_TYPE),
                address = first(StructuredPostal.CONTENT_ITEM_TYPE),
                transactions = CrewNoteCodec.decode(n.note).transactions,
            )
        }.sortedBy { it.name.lowercase() }
    }

    private fun head(uri: Uri): ContactHead? = rows(uri, HEAD_PROJECTION, null, null, null) { c ->
        ContactHead(
            contactId = c.long(Contacts._ID),
            name = c.text(Contacts.DISPLAY_NAME_PRIMARY),
            starred = c.int(Contacts.STARRED) == 1,
            nameRawContactId = c.long(Contacts.NAME_RAW_CONTACT_ID),
        )
    }.firstOrNull()

    private fun dataRows(contactId: Long): List<DataRow> =
        rows(Data.CONTENT_URI, DATA_PROJECTION, DATA_SELECTION, arrayOf(contactId.toString()) + DATA_TYPES, null) { c ->
            DataRow(
                id = c.long(Data._ID),
                rawContactId = c.long(Data.RAW_CONTACT_ID),
                mimeType = c.text(Data.MIMETYPE),
                value = c.text(Data.DATA1),
                type = c.int(Data.DATA2),
                label = c.getString(c.getColumnIndexOrThrow(Data.DATA3)),
            )
        }

    private fun List<DataRow>.of(mime: String, label: (DataRow) -> CharSequence): List<CrewEntry> =
        filter { it.mimeType == mime && it.value.isNotEmpty() }.map { CrewEntry(label(it).toString(), it.value) }

    private inline fun <T> rows(
        uri: Uri,
        projection: Array<String>,
        selection: String?,
        args: Array<String>?,
        sort: String?,
        map: (Cursor) -> T,
    ): List<T> = resolver.query(uri, projection, selection, args, sort)?.use { c ->
        buildList { while (c.moveToNext()) add(map(c)) }
    } ?: emptyList()

    private data class ContactHead(
        val contactId: Long,
        val name: String,
        val starred: Boolean,
        val nameRawContactId: Long,
    )

    private fun Cursor.long(column: String): Long = getLong(getColumnIndexOrThrow(column))

    private fun Cursor.int(column: String): Int = getInt(getColumnIndexOrThrow(column))

    private fun Cursor.text(column: String): String = getString(getColumnIndexOrThrow(column)).orEmpty()

    private data class BudgetNote(val contactId: Long, val rawContactId: Long, val name: String, val note: String)

    private companion object {
        const val VISIBLE = "${Contacts.DISPLAY_NAME_PRIMARY}<>'' AND ${Contacts.IN_VISIBLE_GROUP}=1"
        val MEMBER_PROJECTION = arrayOf(
            Contacts._ID,
            Contacts.LOOKUP_KEY,
            Contacts.DISPLAY_NAME_PRIMARY,
            Contacts.STARRED,
        )
        val HEAD_PROJECTION = arrayOf(
            Contacts._ID,
            Contacts.DISPLAY_NAME_PRIMARY,
            Contacts.STARRED,
            Contacts.NAME_RAW_CONTACT_ID,
        )
        val DATA_PROJECTION = arrayOf(Data._ID, Data.RAW_CONTACT_ID, Data.MIMETYPE, Data.DATA1, Data.DATA2, Data.DATA3)
        val DATA_TYPES = arrayOf(
            Phone.CONTENT_ITEM_TYPE,
            Email.CONTENT_ITEM_TYPE,
            StructuredPostal.CONTENT_ITEM_TYPE,
            Note.CONTENT_ITEM_TYPE,
        )
        const val DATA_SELECTION = "${Data.CONTACT_ID}=? AND ${Data.MIMETYPE} IN (?,?,?,?)"
        val BUDGET_PROJECTION = arrayOf(Data.CONTACT_ID, Data.RAW_CONTACT_ID, Data.DISPLAY_NAME_PRIMARY, Note.NOTE)
        const val BUDGET_SELECTION = "${Data.MIMETYPE}=? AND ${Note.NOTE} LIKE ?"
        val BUDGET_ARGS = arrayOf(Note.CONTENT_ITEM_TYPE, "%${CrewNoteCodec.ADMIN}%")
    }
}
