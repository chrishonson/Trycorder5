package net.ddns.mlsoftlaberge.trycorder.inventory

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException

private const val INVENTORY_FILE = "inventory.json"

/** The original ProductDatabaseHelper.DATABASE_NAME. */
private const val LEGACY_DATABASE = "productdata.db"
private const val LEGACY_TABLE = "product"
private val LEGACY_COLUMNS = arrayOf("_id", "quantity", "upc", "name", "description")

/** Android inventory: the shared JSON repository in filesDir, seeded once from the old SQLite database. */
class AndroidInventory(private val context: Context) : Inventory {
    override val repository: ProductRepository by lazy {
        ProductRepository(
            file = AppFileAccessor(context.filesDir.path, INVENTORY_FILE),
            legacy = LegacyProductSource { readLegacyDatabase() },
        )
    }

    /** Rows of the original database (table product), if the old app left one. */
    private fun readLegacyDatabase(): List<Product> {
        val path = context.getDatabasePath(LEGACY_DATABASE)
        if (!path.exists()) return emptyList()
        return try {
            SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.query(LEGACY_TABLE, LEGACY_COLUMNS, null, null, null, null, "_id").use { it.toProducts() }
            }
        } catch (expected: SQLiteException) {
            emptyList()
        }
    }

    private fun Cursor.toProducts(): List<Product> = buildList {
        while (moveToNext()) {
            val id = getLong(getColumnIndexOrThrow("_id"))
            add(Product(id, text("quantity"), text("upc"), text("name"), text("description")))
        }
    }

    private fun Cursor.text(column: String): String = getString(getColumnIndexOrThrow(column)).orEmpty()
}
