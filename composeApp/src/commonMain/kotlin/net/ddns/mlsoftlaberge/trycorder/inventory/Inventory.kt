package net.ddns.mlsoftlaberge.trycorder.inventory

/**
 * Ship inventory (the original products module). The storage is shared: each platform only
 * provides where the JSON file lives and, on Android, the rows of the old SQLite database.
 */
interface Inventory {
    val repository: ProductRepository
}

/** Rows of a previous storage, imported once when the JSON file does not exist yet. */
fun interface LegacyProductSource {
    fun readLegacyProducts(): List<Product>

    companion object {
        val None = LegacyProductSource { emptyList() }
    }
}
