package net.ddns.mlsoftlaberge.trycorder.crew

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.OperationApplicationException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.RemoteException
import android.provider.ContactsContract.Contacts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The contacts module over ContactsContract. Memo and transactions live in the contact's Note
 * data row, in the original text format, so notes written by the old app stay readable.
 */
class AndroidCrewDirectory(private val context: Context) : CrewDirectory {
    private val queries = ContactQueries(context)

    override val supportsStarred: Boolean = true
    override val canEditContacts: Boolean = true

    override fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    override suspend fun members(query: String, starredOnly: Boolean): List<CrewMember> =
        io(emptyList()) { queries.members(query, starredOnly) }

    override suspend fun thumbnail(id: String): ByteArray? = io(null) { queries.photo(id, highRes = false) }

    override suspend fun photo(id: String): ByteArray? = io(null) { queries.photo(id, highRes = true) }

    override suspend fun detail(id: String): CrewDetail? = io(null) { queries.detail(id) }

    override suspend fun saveNote(id: String, note: CrewNote): NoteSaveResult = io(NoteSaveResult.Failed) {
        try {
            queries.writeNote(id, CrewNoteCodec.encode(note))
        } catch (expected: RemoteException) {
            NoteSaveResult.Failed
        } catch (expected: OperationApplicationException) {
            NoteSaveResult.Failed
        }
    }

    override suspend fun budget(): List<BudgetEntry> = io(emptyList()) { queries.budget() }

    override fun addContact(): Boolean = start(Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI))

    override fun editContact(id: String): Boolean {
        val uri = try {
            queries.contactUri(id)
        } catch (expected: SecurityException) {
            null
        } ?: return false
        return start(Intent(Intent.ACTION_EDIT, uri).putExtra(FINISH_ON_SAVE, true))
    }

    override fun addressUrl(address: String): String = GEO_PREFIX + Uri.encode(address)

    override fun now(): String = SimpleDateFormat(DATE_FORMAT, Locale.US).format(Date())

    private fun start(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (expected: ActivityNotFoundException) {
        false
    }

    /** Runs a provider call off the main thread; a missing permission or a stale id gives [fallback]. */
    private suspend fun <T> io(fallback: T, block: () -> T): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (expected: SecurityException) {
            fallback
        } catch (expected: IllegalArgumentException) {
            fallback
        }
    }

    private companion object {
        const val GEO_PREFIX = "geo:0,0?q="
        const val FINISH_ON_SAVE = "finishActivityOnSaveCompleted"
        const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
    }
}
