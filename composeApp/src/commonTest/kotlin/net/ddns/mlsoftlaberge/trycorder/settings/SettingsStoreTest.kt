package net.ddns.mlsoftlaberge.trycorder.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ddns.mlsoftlaberge.trycorder.core.InMemoryKeyValueStore
import net.ddns.mlsoftlaberge.trycorder.core.PrefKeys
import net.ddns.mlsoftlaberge.trycorder.core.SettingsRepository
import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import net.ddns.mlsoftlaberge.trycorder.core.TrycorderSettings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsStoreTest {
    private val dispatcher = StandardTestDispatcher()
    private val kv = InMemoryKeyValueStore()
    private val repo = SettingsRepository(kv)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun catalogHasEveryPreferenceOfPreferencesXml() {
        assertEquals(16, SettingsCatalog.all.size)
        assertEquals(SettingsCatalog.all.size, SettingsCatalog.all.map { it.key }.toSet().size)
        assertEquals("Automatic loop", SettingsCatalog.all.first().title)
        assertEquals(listOf("French", "English", "Auto"), SettingsCatalog.languages.map { it.label })
    }

    @Test
    fun clickTogglesACheckboxAndPersistsIt() = runTest(dispatcher) {
        val store = SettingsStore(repo)
        store.dispatch(SettingsIntent.Click(PrefKeys.AUTO_LISTEN))
        advanceUntilIdle()
        assertEquals(true, store.state.value.settings.autoListen)
        assertEquals(true, kv.getBoolean(PrefKeys.AUTO_LISTEN, false))
        store.dispatch(SettingsIntent.Click(PrefKeys.AUTO_STOP))
        advanceUntilIdle()
        assertEquals(false, store.state.value.settings.autoStop)
        assertNull(store.state.value.openKey)
    }

    @Test
    fun everyCheckboxKeyRoundTrips() {
        SettingsCatalog.all.filterIsInstance<Preference.Check>().forEach { p ->
            val d = TrycorderSettings()
            val flipped = CheckFields.set(d, p.key, !CheckFields.get(d, p.key))
            assertEquals(!CheckFields.get(d, p.key), CheckFields.get(flipped, p.key), p.key)
        }
    }

    @Test
    fun languageChoiceOpensTheDialogAndStoresTheCode() = runTest(dispatcher) {
        val store = SettingsStore(repo)
        store.dispatch(SettingsIntent.Click(PrefKeys.SPEAK_LANGUAGE))
        assertEquals(PrefKeys.SPEAK_LANGUAGE, store.state.value.openKey)
        store.dispatch(SettingsIntent.Choose(PrefKeys.SPEAK_LANGUAGE, TrekLanguage.French))
        advanceUntilIdle()
        assertNull(store.state.value.openKey)
        assertEquals(TrekLanguage.French, store.state.value.settings.speakLanguage)
        assertEquals("FR", kv.getString(PrefKeys.SPEAK_LANGUAGE, ""))
    }

    @Test
    fun textPreferenceEditsADraftAndConfirms() = runTest(dispatcher) {
        val store = SettingsStore(repo)
        store.dispatch(SettingsIntent.Click(PrefKeys.DEBUG_ADDR))
        assertEquals(TrycorderSettings.DEFAULT_DEBUG_ADDR, store.state.value.draft)
        store.dispatch(SettingsIntent.DraftChanged("10.0.0.2"))
        store.dispatch(SettingsIntent.ConfirmText)
        advanceUntilIdle()
        assertEquals("10.0.0.2", store.state.value.settings.debugAddr)
        assertEquals("10.0.0.2", kv.getString(PrefKeys.DEBUG_ADDR, ""))
    }

    @Test
    fun dismissLeavesTheValueUnchanged() = runTest(dispatcher) {
        val store = SettingsStore(repo)
        store.dispatch(SettingsIntent.Click(PrefKeys.DEVICE_NAME))
        store.dispatch(SettingsIntent.DraftChanged("Enterprise"))
        store.dispatch(SettingsIntent.Dismiss)
        advanceUntilIdle()
        assertEquals(TrycorderSettings.DEFAULT_DEVICE_NAME, store.state.value.settings.deviceName)
        assertEquals("", store.state.value.draft)
    }

    @Test
    fun externalChangesAreReflected() = runTest(dispatcher) {
        val store = SettingsStore(repo)
        advanceUntilIdle()
        repo.update { it.copy(debugMode = true) }
        advanceUntilIdle()
        assertEquals(true, store.state.value.settings.debugMode)
    }
}
