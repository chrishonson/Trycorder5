package net.ddns.mlsoftlaberge.trycorder.crew

import net.ddns.mlsoftlaberge.trycorder.core.ExternalLauncher

/** An address book in memory. */
class FakeCrewDirectory(
    override val supportsStarred: Boolean = true,
    var permission: Boolean = true,
) : CrewDirectory {
    val details = mutableMapOf<String, CrewDetail>()
    val saved = mutableListOf<Pair<String, CrewNote>>()
    var editorOpened = false
    val queries = mutableListOf<Pair<String, Boolean>>()

    override val canEditContacts: Boolean = true

    fun add(detail: CrewDetail) {
        details[detail.id] = detail
    }

    override fun hasPermission(): Boolean = permission

    override suspend fun members(query: String, starredOnly: Boolean): List<CrewMember> {
        queries += query to starredOnly
        return details.values
            .filter { !starredOnly || it.starred }
            .filter { it.name.contains(query, ignoreCase = true) }
            .map { CrewMember(it.id, it.name, it.starred) }
            .sortedBy { it.name }
    }

    override suspend fun thumbnail(id: String): ByteArray? = if (id == "1") byteArrayOf(1, 2) else null

    override suspend fun photo(id: String): ByteArray? = null

    override suspend fun detail(id: String): CrewDetail? = details[id]

    override suspend fun saveNote(id: String, note: CrewNote): NoteSaveResult {
        saved += id to note
        val d = details[id] ?: return NoteSaveResult.Failed
        details[id] = d.copy(note = note)
        return NoteSaveResult.Updated
    }

    override suspend fun budget(): List<BudgetEntry> = details.values.map {
        BudgetEntry(it.id, it.name, "", "", "", it.note.transactions)
    }

    override fun addContact(): Boolean = false

    override fun editContact(id: String): Boolean {
        editorOpened = true
        return true
    }

    override fun addressUrl(address: String): String = "geo:0,0?q=" + UrlCodec.encode(address)

    override fun now(): String = "2016-11-01 12:00:00"
}

class RecordingLauncher(private val result: Boolean = true) : ExternalLauncher {
    val urls = mutableListOf<String>()

    override fun openMap(latitude: Double, longitude: Double): Boolean = result

    override fun openUrl(url: String): Boolean {
        urls += url
        return result
    }
}
