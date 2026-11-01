package net.ddns.mlsoftlaberge.trycorder.inventory

/** The two activities of the original products module. */
sealed interface InventoryPage {
    /** ProductsListActivity. */
    data object List : InventoryPage

    /** ProductDetailActivity; [id] is null for a new product. */
    data class Detail(val id: Long?) : InventoryPage
}

enum class ProductField { Quantity, Name, Upc, Description }

/** The fields of the product_edit layout. */
data class ProductForm(
    val id: Long? = null,
    val quantity: String = Product.DEFAULT_QUANTITY,
    val name: String = "",
    val upc: String = Product.DEFAULT_UPC,
    val description: String = "",
) {
    fun with(field: ProductField, value: String): ProductForm = when (field) {
        ProductField.Quantity -> copy(quantity = value)
        ProductField.Name -> copy(name = value)
        ProductField.Upc -> copy(upc = value)
        ProductField.Description -> copy(description = value)
    }

    fun toProduct() = Product(id ?: Product.NEW_ID, quantity, upc, name, description)

    companion object {
        fun of(p: Product) = ProductForm(p.id, p.quantity, p.name, p.upc, p.description)
    }
}

data class InventoryState(
    val products: List<Product> = emptyList(),
    val page: InventoryPage = InventoryPage.List,
    val form: ProductForm = ProductForm(),
    /** Product whose DELETE context menu is shown. */
    val pendingDelete: Long? = null,
    /** Toast text. */
    val message: String? = null,
)

sealed interface InventoryIntent {
    /** BACK of the list: only the button sound, App navigates back. */
    data object Back : InventoryIntent
    data object Fill : InventoryIntent
    data object Add : InventoryIntent
    data class Open(val id: Long) : InventoryIntent
    data class AskDelete(val id: Long) : InventoryIntent
    data object ConfirmDelete : InventoryIntent
    data object CancelDelete : InventoryIntent
    data class Edit(val field: ProductField, val value: String) : InventoryIntent

    /** SAVE of the detail page: refuses an empty name. */
    data object Save : InventoryIntent

    /** BACK of the detail page: like the original onPause, keeps what was typed. */
    data object CloseDetail : InventoryIntent
    data object DismissMessage : InventoryIntent
}
