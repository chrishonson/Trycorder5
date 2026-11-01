package net.ddns.mlsoftlaberge.trycorder.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ddns.mlsoftlaberge.trycorder.core.SoundPlayer
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound

/** Store of the inventory list and product detail pages (ProductsListActivity + ProductDetailActivity). */
class InventoryStore(
    private val repository: ProductRepository,
    private val sound: SoundPlayer,
) : ViewModel() {
    private val _state = MutableStateFlow(InventoryState())
    val state: StateFlow<InventoryState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.products.collect { list -> _state.update { it.copy(products = list) } }
        }
        viewModelScope.launch { repository.load() }
    }

    fun dispatch(intent: InventoryIntent) {
        when (intent) {
            is InventoryIntent.AskDelete -> _state.update { it.copy(pendingDelete = intent.id) }
            InventoryIntent.ConfirmDelete -> confirmDelete()
            InventoryIntent.CancelDelete -> _state.update { it.copy(pendingDelete = null) }
            is InventoryIntent.Edit -> _state.update { it.copy(form = it.form.with(intent.field, intent.value)) }
            InventoryIntent.DismissMessage -> _state.update { it.copy(message = null) }
            else -> buttonSound().also { onButton(intent) }
        }
    }

    /** The LCARS buttons and the list items: all play the key sound first. */
    private fun onButton(intent: InventoryIntent) {
        when (intent) {
            InventoryIntent.Fill -> viewModelScope.launch { repository.insertAll(ProductSamples.all) }
            InventoryIntent.Add -> openDetail(null)
            is InventoryIntent.Open -> openDetail(intent.id)
            InventoryIntent.Save -> save()
            InventoryIntent.CloseDetail -> closeDetail()
            else -> Unit
        }
    }

    private fun buttonSound() = sound.play(TrekSound.KeyOk)

    private fun openDetail(id: Long?) {
        val product = id?.let { wanted -> _state.value.products.firstOrNull { it.id == wanted } }
        val form = product?.let { ProductForm.of(it) } ?: ProductForm()
        _state.update { it.copy(page = InventoryPage.Detail(product?.id), form = form, pendingDelete = null) }
    }

    private fun confirmDelete() {
        val id = _state.value.pendingDelete ?: return
        _state.update { it.copy(pendingDelete = null) }
        viewModelScope.launch { repository.delete(id) }
    }

    private fun save() {
        if (_state.value.form.name.isEmpty()) {
            _state.update { it.copy(message = NAME_REQUIRED) }
        } else {
            closeDetail()
        }
    }

    /** The original saved in onPause whenever a name or a description was typed. */
    private fun closeDetail() {
        val form = _state.value.form
        if (form.name.isNotEmpty() || form.description.isNotEmpty()) {
            viewModelScope.launch { repository.save(form.toProduct()) }
        }
        _state.update { it.copy(page = InventoryPage.List) }
    }

    companion object {
        const val NAME_REQUIRED = "Please maintain a name"
    }
}
