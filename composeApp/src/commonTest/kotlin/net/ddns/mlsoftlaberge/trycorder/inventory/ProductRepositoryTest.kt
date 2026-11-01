package net.ddns.mlsoftlaberge.trycorder.inventory

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A file kept in memory, for repository tests. */
class InMemoryFileAccessor(var content: String? = null) : FileAccessor {
    var writes = 0
        private set

    override fun read(): String? = content

    override fun write(text: String) {
        writes++
        content = text
    }
}

class ProductRepositoryTest {
    private val dispatcher = StandardTestDispatcher()

    @Test
    fun emptyFileStartsEmptyAndInsertAssignsIds() = runTest(dispatcher) {
        val file = InMemoryFileAccessor()
        val repo = ProductRepository(file, dispatcher = dispatcher)
        assertEquals(emptyList(), repo.load())
        val a = repo.save(Product(name = "Torpedo", quantity = "500"))
        val b = repo.save(Product(name = "Shuttle"))
        assertEquals(1L, a.id)
        assertEquals(2L, b.id)
        assertEquals(listOf("Torpedo", "Shuttle"), repo.products.value.map { it.name })
        assertTrue(file.content.orEmpty().contains("Torpedo"))
    }

    @Test
    fun updateDeleteAndReloadFromFile() = runTest(dispatcher) {
        val file = InMemoryFileAccessor()
        val repo = ProductRepository(file, dispatcher = dispatcher)
        val a = repo.save(Product(name = "Crew", quantity = "1024"))
        repo.save(a.copy(quantity = "1025"))
        repo.save(Product(name = "Pools"))
        repo.delete(a.id)

        val reloaded = ProductRepository(file, dispatcher = dispatcher)
        val products = reloaded.load()
        assertEquals(listOf("Pools"), products.map { it.name })
        assertNull(reloaded.find(a.id))
        // ids are never reused after a delete
        assertEquals(3L, reloaded.save(Product(name = "Bathrooms")).id)
    }

    @Test
    fun legacyRowsAreMigratedOnlyWhenTheFileDoesNotExist() = runTest(dispatcher) {
        var legacyReads = 0
        val legacy = LegacyProductSource {
            legacyReads++
            listOf(Product(id = 7, quantity = "3", upc = "u", name = "Consultant", description = "Shrink"))
        }
        val file = InMemoryFileAccessor()
        val repo = ProductRepository(file, legacy, dispatcher)
        val first = repo.load()
        assertEquals(1, legacyReads)
        assertEquals(7L, first.single().id)
        assertNotNull(file.content)
        assertEquals(8L, repo.save(Product(name = "New")).id)

        val again = ProductRepository(file, legacy, dispatcher)
        assertEquals(2, again.load().size)
        assertEquals(1, legacyReads)
    }

    @Test
    fun fillInsertsTheTwentySamples() = runTest(dispatcher) {
        val repo = ProductRepository(InMemoryFileAccessor(), dispatcher = dispatcher)
        repo.insertAll(ProductSamples.all)
        val products = repo.load()
        assertEquals(20, products.size)
        assertEquals((1L..20L).toList(), products.map { it.id })
        assertEquals("Trycorder", products.last().name)
    }

    @Test
    fun corruptFileGivesAnEmptyInventory() = runTest(dispatcher) {
        val repo = ProductRepository(InMemoryFileAccessor("not json"), dispatcher = dispatcher)
        assertEquals(emptyList(), repo.load())
    }
}
