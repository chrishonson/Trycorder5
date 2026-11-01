package net.ddns.mlsoftlaberge.trycorder.inventory

private const val INVENTORY_FILE = "inventory.json"

/** iOS inventory: the shared JSON repository in the Documents directory (no legacy database). */
class IosInventory : Inventory {
    override val repository: ProductRepository by lazy {
        ProductRepository(AppFileAccessor(iosDocumentsDirectory(), INVENTORY_FILE))
    }
}
