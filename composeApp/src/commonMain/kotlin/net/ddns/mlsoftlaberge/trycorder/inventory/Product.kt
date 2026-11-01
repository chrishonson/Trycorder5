package net.ddns.mlsoftlaberge.trycorder.inventory

import kotlinx.serialization.Serializable

/** One row of the original product table (all columns were text). */
@Serializable
data class Product(
    val id: Long = NEW_ID,
    val quantity: String = DEFAULT_QUANTITY,
    val upc: String = DEFAULT_UPC,
    val name: String = "",
    val description: String = "",
) {
    companion object {
        const val NEW_ID = 0L
        const val DEFAULT_QUANTITY = "1"
        const val DEFAULT_UPC = "0-99999-11111-0"
    }
}

/** Content of the inventory JSON file. */
@Serializable
data class InventoryData(
    val nextId: Long = 1,
    val products: List<Product> = emptyList(),
)

/** The FILL button of the original list: the starship sample inventory. */
object ProductSamples {
    private const val UPC = Product.DEFAULT_UPC

    val all: List<Product> = listOf(
        sample("500", "Torpedo", "Photon Torpedo to fire at ennemys"),
        sample("10", "Dilithium", "Dilithium cristals to run the ship"),
        sample("4", "Shuttle", "Space shuttle"),
        sample("1024", "Crew", "Active Crew on ship"),
        sample("8", "Officers", "Senior officer who can drive the ship"),
        sample("243", "DaysOfFood", "Days of food reserve for ship complement"),
        sample("1500", "SpaceSuits", "Space proof suit"),
        sample("47534", "ToiletPaper", "Space proof simple toilet paper rolls"),
        sample("12", "Doctors", "Doctors who can practice medecine"),
        sample("3", "Consultant", "Shrink"),
        sample("10", "Antiproton Inhibitors", "Transporter fuel containers"),
        sample("256", "Escape Pods", "Whatatatow"),
        sample("12", "Vulcan Ale", "OOps, Prohibited, but have a special allowance from Kirk."),
        sample("1", "Cardassian Dildo", "For Sick Bay purposes. (Which?)"),
        sample("10", "Federation flags", "For ceremony purposes. (Like spock first death)"),
        sample("10000", "Crew Uniforms", "To Stay clean-cut"),
        sample("5", "Worf's furnitures", "To replace broken ones"),
        sample("128", "Bathrooms", "To use when needed"),
        sample("20", "Pools", "Recreation and Rescue"),
        sample("2500", "Trycorder", "Ship operation and evaluation controllers"),
    )

    private fun sample(quantity: String, name: String, description: String) =
        Product(quantity = quantity, upc = UPC, name = name, description = description)
}
