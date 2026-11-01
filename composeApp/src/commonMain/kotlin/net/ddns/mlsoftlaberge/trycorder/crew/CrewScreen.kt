package net.ddns.mlsoftlaberge.trycorder.crew

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ddns.mlsoftlaberge.trycorder.BackHandler
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.core.PlatformServices
import net.ddns.mlsoftlaberge.trycorder.inventory.TrekToast
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette

/** The contacts module: crew list, admin page with memo/transaction editors, and the budget. */
@Suppress("UnusedParameter") // onNavigate: signature fixed by App, this screen opens no other
@Composable
fun CrewScreen(services: PlatformServices, onNavigate: (Screen) -> Unit, onBack: () -> Unit) {
    val store = viewModel { CrewListStore(services.crew, services.sound) }
    val state by store.state.collectAsState()
    ContactsPermissionRequest { store.dispatch(CrewListIntent.PermissionResult(it)) }
    LifecycleResumeEffect(store) {
        store.dispatch(CrewListIntent.Refresh)
        onPauseOrDispose { }
    }
    BackHandler(enabled = state.page != CrewPage.List) { store.dispatch(CrewListIntent.ClosePage) }
    Box(Modifier.fillMaxSize().background(TrekPalette.Background)) {
        when (val page = state.page) {
            CrewPage.List -> CrewList(
                members = state.members,
                thumbnails = state.thumbnails,
                query = state.query,
                access = state.access,
                onBack = {
                    store.dispatch(CrewListIntent.Back)
                    onBack()
                },
                onIntent = store::dispatch,
            )
            is CrewPage.Admin -> CrewAdminPage(services, page.id) { store.dispatch(CrewListIntent.ClosePage) }
            CrewPage.Budget -> CrewBudgetPage(services) { store.dispatch(CrewListIntent.ClosePage) }
        }
        TrekToast(state.message) { store.dispatch(CrewListIntent.DismissMessage) }
    }
}

/** Container of the admin page and its editors. */
@Composable
private fun CrewAdminPage(services: PlatformServices, id: String, onClose: () -> Unit) {
    val store = viewModel { CrewAdminStore(services.crew, services.sound, services.launcher) }
    val state by store.state.collectAsState()
    LaunchedEffect(id) { store.dispatch(CrewAdminIntent.Load(id)) }
    LifecycleResumeEffect(store) {
        store.dispatch(CrewAdminIntent.Refresh)
        onPauseOrDispose { }
    }
    BackHandler(enabled = state.editor != null) { store.dispatch(CrewAdminIntent.EditorClose) }
    val back = {
        store.dispatch(CrewAdminIntent.Back)
        onClose()
    }
    Box(Modifier.fillMaxSize()) {
        if (state.contactId == id) {
            when (val editor = state.editor) {
                null -> CrewAdmin(
                    name = state.name,
                    starred = state.starred,
                    photo = state.photo,
                    canEdit = state.canEditContact,
                    memo = state.note.memo,
                    transactions = state.note.transactions,
                    total = state.note.total,
                    addresses = state.addresses,
                    phones = state.phones,
                    emails = state.emails,
                    onBack = back,
                    onIntent = store::dispatch,
                )
                is CrewEditor.Memo -> MemoEditor(
                    name = state.name,
                    text = editor.text,
                    confirmDelete = editor.confirmDelete,
                    onIntent = store::dispatch,
                )
                is CrewEditor.Transaction -> TransactionEditor(
                    name = state.name,
                    date = editor.date,
                    description = editor.description,
                    amount = editor.amount,
                    confirmDelete = editor.confirmDelete,
                    onIntent = store::dispatch,
                )
            }
        }
        TrekToast(state.message) { store.dispatch(CrewAdminIntent.DismissMessage) }
    }
}

/** Container of the budget page. */
@Composable
private fun CrewBudgetPage(services: PlatformServices, onClose: () -> Unit) {
    val store = viewModel { CrewBudgetStore(services.crew, services.sound) }
    val state by store.state.collectAsState()
    LaunchedEffect(store) { store.dispatch(CrewBudgetIntent.Load) }
    CrewBudget(
        entries = state.entries,
        total = state.total,
        loading = state.loading,
        onBack = {
            store.dispatch(CrewBudgetIntent.Back)
            onClose()
        },
    )
}
