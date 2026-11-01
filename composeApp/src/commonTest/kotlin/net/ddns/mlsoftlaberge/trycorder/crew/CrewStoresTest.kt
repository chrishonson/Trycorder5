package net.ddns.mlsoftlaberge.trycorder.crew

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ddns.mlsoftlaberge.trycorder.core.SilentSoundPlayer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CrewStoresTest {
    private val dispatcher = StandardTestDispatcher()
    private val directory = FakeCrewDirectory().apply {
        add(
            CrewDetail(
                id = "1",
                name = "James Kirk",
                starred = true,
                phones = listOf(CrewEntry("Mobile", "418-123-4567")),
                emails = listOf(CrewEntry("Work", "kirk@starfleet.org")),
                addresses = listOf(CrewEntry("Home", "1 Iowa Road")),
                note = CrewNoteCodec.decode("Captain\nADMIN|2016-01-01 00:00:00|10.00|Phaser\n"),
            ),
        )
        add(CrewDetail(id = "2", name = "Leonard McCoy", starred = false))
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun listStartsOnStarredContactsOnceThePermissionIsGranted() = runTest(dispatcher) {
        val store = CrewListStore(directory, SilentSoundPlayer)
        store.dispatch(CrewListIntent.Refresh)
        advanceUntilIdle()
        assertEquals(emptyList(), store.state.value.members)
        store.dispatch(CrewListIntent.PermissionResult(true))
        advanceUntilIdle()
        assertEquals(listOf("James Kirk"), store.state.value.members.map { it.name })
        assertEquals(setOf("1"), store.state.value.thumbnails.keys)
        store.dispatch(CrewListIntent.ToggleStarred)
        advanceUntilIdle()
        assertEquals(2, store.state.value.members.size)
        store.dispatch(CrewListIntent.Search("mcc"))
        advanceUntilIdle()
        assertEquals(listOf("Leonard McCoy"), store.state.value.members.map { it.name })
    }

    @Test
    fun deniedPermissionShowsAMessage() = runTest(dispatcher) {
        val store = CrewListStore(FakeCrewDirectory(permission = false), SilentSoundPlayer)
        store.dispatch(CrewListIntent.PermissionResult(false))
        advanceUntilIdle()
        assertEquals(ContactsAccess.Denied, store.state.value.access)
        assertEquals(CrewListStore.DENIED, store.state.value.message)
    }

    @Test
    fun withoutFavoritesTheListShowsEveryoneAndStarExplains() = runTest(dispatcher) {
        val noFavorites = FakeCrewDirectory(supportsStarred = false).apply { add(CrewDetail("3", "Spock", false)) }
        val store = CrewListStore(noFavorites, SilentSoundPlayer)
        store.dispatch(CrewListIntent.PermissionResult(true))
        advanceUntilIdle()
        assertEquals(listOf("Spock"), store.state.value.members.map { it.name })
        store.dispatch(CrewListIntent.ToggleStarred)
        assertEquals(CrewListStore.NO_FAVORITES, store.state.value.message)
    }

    @Test
    fun navigationBetweenPages() = runTest(dispatcher) {
        val store = CrewListStore(directory, SilentSoundPlayer)
        store.dispatch(CrewListIntent.Open("1"))
        assertEquals(CrewPage.Admin("1"), store.state.value.page)
        store.dispatch(CrewListIntent.ClosePage)
        store.dispatch(CrewListIntent.OpenBudget)
        assertEquals(CrewPage.Budget, store.state.value.page)
        store.dispatch(CrewListIntent.Add)
        assertEquals(CrewListStore.NOT_AVAILABLE, store.state.value.message)
    }

    @Test
    fun adminLoadsTheContact() = runTest(dispatcher) {
        val store = admin()
        store.dispatch(CrewAdminIntent.Load("1"))
        advanceUntilIdle()
        val s = store.state.value
        assertEquals("James Kirk", s.name)
        assertEquals("Captain\n", s.note.memo)
        assertEquals(10.0, s.note.total)
        assertEquals("418-123-4567", s.phones.single().value)
    }

    @Test
    fun editMemoSavesTheNote() = runTest(dispatcher) {
        val store = admin()
        store.dispatch(CrewAdminIntent.Load("1"))
        advanceUntilIdle()
        store.dispatch(CrewAdminIntent.EditMemo)
        assertEquals(CrewEditor.Memo("Captain\n"), store.state.value.editor)
        store.dispatch(CrewAdminIntent.MemoChanged("Admiral"))
        store.dispatch(CrewAdminIntent.EditorSave)
        advanceUntilIdle()
        assertNull(store.state.value.editor)
        assertEquals("Admiral\n", store.state.value.note.memo)
        val written = CrewNoteCodec.encode(directory.saved.last().second)
        assertEquals("Admiral\nADMIN|2016-01-01 00:00:00|10.00|Phaser\n", written)
        assertEquals(NoteSaveResult.Updated.message, store.state.value.message)
    }

    @Test
    fun addEditAndDeleteTransactions() = runTest(dispatcher) {
        val store = admin()
        store.dispatch(CrewAdminIntent.Load("1"))
        advanceUntilIdle()
        store.dispatch(CrewAdminIntent.AddTransaction)
        assertEquals(CrewEditor.Transaction(null, "2016-11-01 12:00:00", "", ""), store.state.value.editor)
        store.dispatch(CrewAdminIntent.TransChanged(TransField.Description, "Torpedo"))
        store.dispatch(CrewAdminIntent.TransChanged(TransField.Amount, "2.5"))
        store.dispatch(CrewAdminIntent.EditorSave)
        advanceUntilIdle()
        assertEquals(listOf("Phaser", "Torpedo"), store.state.value.note.transactions.map { it.description })
        assertEquals("2.50", store.state.value.note.transactions[1].amount)

        store.dispatch(CrewAdminIntent.EditTransaction(0))
        store.dispatch(CrewAdminIntent.EditorDelete)
        assertEquals(true, store.state.value.editor?.confirmDelete)
        store.dispatch(CrewAdminIntent.EditorConfirmDelete)
        advanceUntilIdle()
        assertEquals(listOf("Torpedo"), store.state.value.note.transactions.map { it.description })
    }

    @Test
    fun emptyDescriptionAndAmountRemovesTheTransaction() {
        val note = CrewNoteCodec.decode("ADMIN|2016-01-01|1|a\nADMIN|2016-01-02|2|b\n")
        val editor = CrewEditor.Transaction(0, "2016-01-01", "", "")
        val out = CrewNoteEditing.reduce(editor, note, CrewAdminIntent.EditorSave)
        assertNull(out.editor)
        assertEquals(listOf("b"), out.noteToSave?.transactions?.map { it.description })
        val closed = CrewNoteEditing.reduce(CrewEditor.Memo("x"), note, CrewAdminIntent.EditorClose)
        assertNull(closed.noteToSave)
        assertNull(closed.editor)
    }

    @Test
    fun callEmailAndMapGoThroughTheLauncher() = runTest(dispatcher) {
        val launcher = RecordingLauncher()
        val store = CrewAdminStore(directory, SilentSoundPlayer, launcher)
        store.dispatch(CrewAdminIntent.Load("1"))
        advanceUntilIdle()
        store.dispatch(CrewAdminIntent.Call("418-123-4567"))
        store.dispatch(CrewAdminIntent.Email("kirk@starfleet.org"))
        store.dispatch(CrewAdminIntent.ShowAddress("1 Iowa Road"))
        assertEquals("tel:4181234567", launcher.urls[0])
        assertTrue(launcher.urls[1].startsWith("mailto:kirk@starfleet.org?subject=Account%20State&body="))
        assertTrue(launcher.urls[1].contains("Phaser"))
        assertEquals("geo:0,0?q=1%20Iowa%20Road", launcher.urls[2])
        store.dispatch(CrewAdminIntent.EditContact)
        assertTrue(directory.editorOpened)
    }

    @Test
    fun launcherFailureShowsTheOriginalToast() = runTest(dispatcher) {
        val store = CrewAdminStore(directory, SilentSoundPlayer, RecordingLauncher(result = false))
        store.dispatch(CrewAdminIntent.Load("1"))
        advanceUntilIdle()
        store.dispatch(CrewAdminIntent.ShowAddress("x"))
        assertEquals(CrewAdminStore.NO_APP, store.state.value.message)
        store.dispatch(CrewAdminIntent.Email("x@y"))
        assertEquals(CrewAdminStore.EMAIL_ERROR, store.state.value.message)
    }

    @Test
    fun budgetListsClientsWithTransactionsAndTheGrandTotal() = runTest(dispatcher) {
        val store = CrewBudgetStore(directory, SilentSoundPlayer)
        store.dispatch(CrewBudgetIntent.Load)
        advanceUntilIdle()
        assertEquals(listOf("James Kirk"), store.state.value.entries.map { it.name })
        assertEquals(10.0, store.state.value.total)
    }

    private fun admin() = CrewAdminStore(directory, SilentSoundPlayer, RecordingLauncher())
}
