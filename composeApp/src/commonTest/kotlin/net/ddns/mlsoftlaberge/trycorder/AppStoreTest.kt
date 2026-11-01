package net.ddns.mlsoftlaberge.trycorder

import net.ddns.mlsoftlaberge.trycorder.core.InMemoryKeyValueStore
import net.ddns.mlsoftlaberge.trycorder.core.PrefKeys
import kotlin.test.Test
import kotlin.test.assertEquals

class AppStoreTest {
    @Test
    fun startsOnTheLastSavedMode() {
        val store = AppStore(InMemoryKeyValueStore(mapOf(PrefKeys.LAST_MODE to Screen.Desktop.modeNumber)))
        assertEquals(Screen.Desktop, store.state.value.screen)
    }

    @Test
    fun modesReplaceEachOtherAndAreSaved() {
        val prefs = InMemoryKeyValueStore()
        val store = AppStore(prefs)
        store.dispatch(AppIntent.Navigate(Screen.PhotoGallery))
        store.dispatch(AppIntent.Navigate(Screen.MultiSensor))
        assertEquals(Screen.MultiSensor, store.state.value.screen)
        assertEquals(emptyList(), store.state.value.backStack)
        assertEquals(Screen.MultiSensor.modeNumber, prefs.getInt(PrefKeys.LAST_MODE, 0))
    }

    @Test
    fun secondaryScreensGoBackWhereTheyWereOpened() {
        val store = AppStore(InMemoryKeyValueStore())
        store.dispatch(AppIntent.Navigate(Screen.Crew))
        store.dispatch(AppIntent.Navigate(Screen.Settings))
        store.dispatch(AppIntent.Back)
        assertEquals(Screen.Crew, store.state.value.screen)
        store.dispatch(AppIntent.Back)
        assertEquals(Screen.Trycorder, store.state.value.screen)
    }

    @Test
    fun backFromAModeReturnsToTheTrycorder() {
        val store = AppStore(InMemoryKeyValueStore())
        store.dispatch(AppIntent.Navigate(Screen.VisionDay))
        store.dispatch(AppIntent.Back)
        assertEquals(Screen.Trycorder, store.state.value.screen)
    }
}
