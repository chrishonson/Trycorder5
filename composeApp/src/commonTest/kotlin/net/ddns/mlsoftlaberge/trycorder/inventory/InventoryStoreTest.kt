package net.ddns.mlsoftlaberge.trycorder.inventory

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

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryStoreTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun store() =
        InventoryStore(ProductRepository(InMemoryFileAccessor(), dispatcher = dispatcher), SilentSoundPlayer)

    @Test
    fun fillShowsTheSampleInventory() = runTest(dispatcher) {
        val store = store()
        store.dispatch(InventoryIntent.Fill)
        advanceUntilIdle()
        assertEquals(20, store.state.value.products.size)
    }

    @Test
    fun addOpensAnEmptyFormWithTheOriginalDefaults() = runTest(dispatcher) {
        val store = store()
        store.dispatch(InventoryIntent.Add)
        val s = store.state.value
        assertEquals(InventoryPage.Detail(null), s.page)
        assertEquals("1", s.form.quantity)
        assertEquals("0-99999-11111-0", s.form.upc)
        assertEquals("", s.form.name)
    }

    @Test
    fun saveRefusesAnEmptyName() = runTest(dispatcher) {
        val store = store()
        store.dispatch(InventoryIntent.Add)
        store.dispatch(InventoryIntent.Save)
        assertEquals(InventoryStore.NAME_REQUIRED, store.state.value.message)
        assertEquals(InventoryPage.Detail(null), store.state.value.page)
        store.dispatch(InventoryIntent.DismissMessage)
        assertNull(store.state.value.message)
    }

    @Test
    fun saveStoresTheProductAndReturnsToTheList() = runTest(dispatcher) {
        val store = store()
        store.dispatch(InventoryIntent.Add)
        store.dispatch(InventoryIntent.Edit(ProductField.Name, "Phaser"))
        store.dispatch(InventoryIntent.Edit(ProductField.Quantity, "12"))
        store.dispatch(InventoryIntent.Save)
        advanceUntilIdle()
        val s = store.state.value
        assertEquals(InventoryPage.List, s.page)
        assertEquals("Phaser", s.products.single().name)
        assertEquals("12", s.products.single().quantity)
    }

    @Test
    fun editExistingProductAndBackKeepsTheChangesLikeOnPause() = runTest(dispatcher) {
        val store = store()
        store.dispatch(InventoryIntent.Fill)
        advanceUntilIdle()
        val first = store.state.value.products.first()
        store.dispatch(InventoryIntent.Open(first.id))
        assertEquals("Torpedo", store.state.value.form.name)
        store.dispatch(InventoryIntent.Edit(ProductField.Description, "Quantum"))
        store.dispatch(InventoryIntent.CloseDetail)
        advanceUntilIdle()
        assertEquals("Quantum", store.state.value.products.first().description)
        assertEquals(20, store.state.value.products.size)
    }

    @Test
    fun backFromAnEmptyNewProductSavesNothing() = runTest(dispatcher) {
        val store = store()
        store.dispatch(InventoryIntent.Add)
        store.dispatch(InventoryIntent.CloseDetail)
        advanceUntilIdle()
        assertEquals(emptyList(), store.state.value.products)
    }

    @Test
    fun longPressDeleteRemovesTheProduct() = runTest(dispatcher) {
        val store = store()
        store.dispatch(InventoryIntent.Fill)
        advanceUntilIdle()
        val id = store.state.value.products[1].id
        store.dispatch(InventoryIntent.AskDelete(id))
        assertEquals(id, store.state.value.pendingDelete)
        store.dispatch(InventoryIntent.CancelDelete)
        assertNull(store.state.value.pendingDelete)
        store.dispatch(InventoryIntent.AskDelete(id))
        store.dispatch(InventoryIntent.ConfirmDelete)
        advanceUntilIdle()
        assertEquals(19, store.state.value.products.size)
        assertEquals(false, store.state.value.products.any { it.id == id })
    }
}
