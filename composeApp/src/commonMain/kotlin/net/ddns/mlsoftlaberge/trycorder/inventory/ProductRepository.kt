package net.ddns.mlsoftlaberge.trycorder.inventory

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * The inventory, persisted as one JSON file. Replaces the SQLite content provider of the original.
 * The first load imports the rows of [legacy] when the JSON file does not exist yet.
 */
class ProductRepository(
    private val file: FileAccessor,
    private val legacy: LegacyProductSource = LegacyProductSource.None,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private var data: InventoryData? = null
    private val _products = MutableStateFlow<List<Product>>(emptyList())

    /** All products in insertion order (the original list had no sort order). */
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    suspend fun load(): List<Product> = mutex.withLock { ensureLoaded().products }

    suspend fun find(id: Long): Product? = load().firstOrNull { it.id == id }

    /** Inserts a product with [Product.NEW_ID] (or an unknown id), updates it otherwise. */
    suspend fun save(product: Product): Product = mutate { d ->
        if (d.products.none { it.id == product.id }) {
            val created = product.copy(id = d.nextId)
            d.copy(nextId = d.nextId + 1, products = d.products + created) to created
        } else {
            d.copy(products = d.products.map { if (it.id == product.id) product else it }) to product
        }
    }

    suspend fun insertAll(items: List<Product>) = mutate { d ->
        val created = items.mapIndexed { i, p -> p.copy(id = d.nextId + i) }
        d.copy(nextId = d.nextId + items.size, products = d.products + created) to Unit
    }

    suspend fun delete(id: Long) = mutate { d -> d.copy(products = d.products.filterNot { it.id == id }) to Unit }

    private suspend fun <T> mutate(block: (InventoryData) -> Pair<InventoryData, T>): T = mutex.withLock {
        val (next, result) = block(ensureLoaded())
        withContext(dispatcher) { file.write(json.encodeToString(InventoryData.serializer(), next)) }
        publish(next)
        result
    }

    private suspend fun ensureLoaded(): InventoryData =
        data ?: withContext(dispatcher) { readOrMigrate() }.also { publish(it) }

    private fun publish(next: InventoryData) {
        data = next
        _products.value = next.products
    }

    private fun readOrMigrate(): InventoryData {
        val text = file.read() ?: return migrate()
        return try {
            json.decodeFromString(InventoryData.serializer(), text)
        } catch (expected: SerializationException) {
            InventoryData()
        } catch (expected: IllegalArgumentException) {
            InventoryData()
        }
    }

    private fun migrate(): InventoryData {
        val rows = legacy.readLegacyProducts()
        val migrated = InventoryData(nextId = (rows.maxOfOrNull { it.id } ?: 0L) + 1, products = rows)
        file.write(json.encodeToString(InventoryData.serializer(), migrated))
        return migrated
    }
}
