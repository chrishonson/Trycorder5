package net.ddns.mlsoftlaberge.trycorder.inventory

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ddns.mlsoftlaberge.trycorder.BackHandler
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.core.PlatformServices
import net.ddns.mlsoftlaberge.trycorder.resources.Res
import net.ddns.mlsoftlaberge.trycorder.resources.reminder
import net.ddns.mlsoftlaberge.trycorder.ui.TrekButton
import net.ddns.mlsoftlaberge.trycorder.ui.TrekColor
import net.ddns.mlsoftlaberge.trycorder.ui.TrekEdge
import net.ddns.mlsoftlaberge.trycorder.ui.TrekFont
import net.ddns.mlsoftlaberge.trycorder.ui.TrekFrameBar
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette
import org.jetbrains.compose.resources.painterResource

private const val HALF = 5f
private const val LIST_HEADER_WEIGHT = 10f
private const val LIST_WEIGHT = 88f
private const val BUTTON_WEIGHT = 2f
private const val CORNER_WEIGHT = 1f
private const val LINE_WEIGHT = 8f
private const val ROW_SP = 24
private const val ROW_NAME_WEIGHT = 7f
private const val ROW_QTY_WEIGHT = 2f
private const val EMPTY_SP = 36
private val DividerColor = Color(0xFF3A3A3A)

/** The products module: list of the ship inventory, and the product detail page. */
@Suppress("UnusedParameter") // onNavigate: signature fixed by App, this screen opens no other
@Composable
fun InventoryScreen(services: PlatformServices, onNavigate: (Screen) -> Unit, onBack: () -> Unit) {
    val store = viewModel { InventoryStore(services.inventory.repository, services.sound) }
    val state by store.state.collectAsState()
    val detail = state.page as? InventoryPage.Detail
    BackHandler(enabled = detail != null) { store.dispatch(InventoryIntent.CloseDetail) }
    Box(Modifier.fillMaxSize().background(TrekPalette.Background)) {
        if (detail == null) {
            ProductList(
                products = state.products,
                pendingDelete = state.pendingDelete,
                onBack = {
                    store.dispatch(InventoryIntent.Back)
                    onBack()
                },
                onIntent = store::dispatch,
            )
        } else {
            val form = state.form
            ProductDetail(
                quantity = form.quantity,
                name = form.name,
                upc = form.upc,
                description = form.description,
                onEdit = { field, value -> store.dispatch(InventoryIntent.Edit(field, value)) },
                onBack = { store.dispatch(InventoryIntent.CloseDetail) },
                onSave = { store.dispatch(InventoryIntent.Save) },
            )
        }
        TrekToast(state.message) { store.dispatch(InventoryIntent.DismissMessage) }
    }
}

/** product_list.xml. */
@Composable
private fun ProductList(
    products: List<Product>,
    pendingDelete: Long?,
    onBack: () -> Unit,
    onIntent: (InventoryIntent) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TrekPageHeader(
            leftWeight = HALF,
            rightWeight = HALF,
            onTopLeft = onBack,
            modifier = Modifier.weight(LIST_HEADER_WEIGHT),
            buttons = {
                HeaderButton("BACK", TrekColor.Indigo, TrekEdge.Center, BUTTON_WEIGHT, onBack)
                HeaderButton("FILL", TrekColor.Yellow, TrekEdge.Center, BUTTON_WEIGHT) {
                    onIntent(InventoryIntent.Fill)
                }
                HeaderButton("ADD", TrekColor.Blue, TrekEdge.Right, BUTTON_WEIGHT) { onIntent(InventoryIntent.Add) }
            },
            bottom = { HeaderTitle("Inventory List") },
        )
        TrekFrameBar(top = true, cornerWeight = CORNER_WEIGHT, lineWeight = LINE_WEIGHT)
        Box(Modifier.weight(LIST_WEIGHT).fillMaxWidth()) {
            if (products.isEmpty()) {
                Text(
                    text = "No Products",
                    color = TrekPalette.Text,
                    fontSize = EMPTY_SP.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(products, key = { it.id }) { p ->
                    ProductRow(
                        name = p.name,
                        quantity = p.quantity,
                        deleting = p.id == pendingDelete,
                        onClick = { onIntent(InventoryIntent.Open(p.id)) },
                        onLongClick = { onIntent(InventoryIntent.AskDelete(p.id)) },
                        onDelete = { onIntent(InventoryIntent.ConfirmDelete) },
                        onCancel = { onIntent(InventoryIntent.CancelDelete) },
                    )
                }
            }
        }
        TrekFrameBar(top = false, cornerWeight = CORNER_WEIGHT, lineWeight = LINE_WEIGHT)
    }
}

/** product_row.xml, with the long-press DELETE context menu shown inline. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductRow(
    name: String,
    quantity: String,
    deleting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(Res.drawable.reminder),
                contentDescription = null,
                modifier = Modifier.weight(1f).height(24.dp).padding(start = 4.dp, end = 8.dp),
            )
            Text(
                text = name,
                color = TrekPalette.Text,
                fontSize = ROW_SP.sp,
                maxLines = 1,
                modifier = Modifier.weight(ROW_NAME_WEIGHT),
            )
            Text(
                text = quantity,
                color = TrekPalette.Text,
                fontSize = ROW_SP.sp,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(ROW_QTY_WEIGHT).padding(end = 2.dp),
            )
        }
        if (deleting) {
            Row(Modifier.fillMaxWidth().height(40.dp)) {
                TrekButton("DELETE", TrekColor.Red, onDelete, Modifier.weight(1f), font = TrekFont.Default)
                TrekButton("CANCEL", TrekColor.Gray, onCancel, Modifier.weight(1f), font = TrekFont.Default)
            }
        }
        Spacer(Modifier.fillMaxWidth().height(2.dp).background(DividerColor))
    }
}

/** product_edit.xml. */
@Composable
private fun ProductDetail(
    quantity: String,
    name: String,
    upc: String,
    description: String,
    onEdit: (ProductField, String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TrekPageHeader(
            leftWeight = HALF,
            rightWeight = HALF,
            onTopLeft = onBack,
            modifier = Modifier.weight(DETAIL_HEADER_WEIGHT),
            buttons = {
                HeaderButton("BACK", TrekColor.Indigo, TrekEdge.Center, DETAIL_BUTTON_WEIGHT, onBack)
                HeaderButton("SAVE", TrekColor.Blue, TrekEdge.Right, DETAIL_BUTTON_WEIGHT, onSave)
            },
            bottom = { HeaderTitle("Product Detail") },
        )
        TrekFrameBar(top = true, cornerWeight = CORNER_WEIGHT, lineWeight = LINE_WEIGHT)
        FieldRow("Qty", quantity, "0", numeric = true) { onEdit(ProductField.Quantity, it) }
        FieldRow("Name", name, "Name") { onEdit(ProductField.Name, it) }
        FieldRow("UPC", upc, "UPC") { onEdit(ProductField.Upc, it) }
        FieldRow("Desc", description, "Description", singleLine = false) { onEdit(ProductField.Description, it) }
        TrekFrameBar(top = false, cornerWeight = CORNER_WEIGHT, lineWeight = LINE_WEIGHT)
        Spacer(Modifier.weight(DETAIL_REST_WEIGHT))
    }
}

private const val DETAIL_HEADER_WEIGHT = 15f
private const val DETAIL_REST_WEIGHT = 85f
private const val DETAIL_BUTTON_WEIGHT = 3f
private const val LABEL_WEIGHT = 2f
private const val FIELD_WEIGHT = 8f
private const val FIELD_SP = 20

@Composable
private fun FieldRow(
    label: String,
    value: String,
    hint: String,
    numeric: Boolean = false,
    singleLine: Boolean = true,
    onChange: (String) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = TrekPalette.HoloBlueLight,
            fontSize = FIELD_SP.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(LABEL_WEIGHT),
        )
        TrekField(
            value = value,
            onValueChange = onChange,
            hint = hint,
            numeric = numeric,
            singleLine = singleLine,
            modifier = Modifier.weight(FIELD_WEIGHT).padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
        )
    }
}
