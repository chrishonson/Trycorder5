package net.ddns.mlsoftlaberge.trycorder.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsRepositoryTest {
    @Test
    fun defaultsMatchPreferencesXml() {
        val s = SettingsRepository(InMemoryKeyValueStore()).current
        assertFalse(s.autoListen)
        assertTrue(s.isMaster)
        assertTrue(s.autoStop)
        assertEquals("192.168.0.184", s.debugAddr)
        assertEquals(TrycorderSettings.PUBLIC_SERVER, s.serverHost)
    }

    @Test
    fun readsTheOriginalKeys() {
        val store = InMemoryKeyValueStore(
            mapOf(PrefKeys.SPEAK_LANGUAGE to "FR", PrefKeys.DEBUG_MODE to true, PrefKeys.DEBUG_ADDR to "10.0.0.2"),
        )
        val s = SettingsRepository(store).current
        assertEquals(TrekLanguage.French, s.speakLanguage)
        assertEquals("10.0.0.2", s.serverHost)
    }

    @Test
    fun placeholderDeviceNameIsReplacedAndSaved() {
        val store = InMemoryKeyValueStore()
        val repo = SettingsRepository(store) { "Pixel 9" }
        assertEquals("Pixel 9", repo.current.deviceName)
        assertEquals("Pixel 9", store.getString(PrefKeys.DEVICE_NAME, ""))
    }

    @Test
    fun updatePersistsAndPublishes() {
        val store = InMemoryKeyValueStore()
        val repo = SettingsRepository(store)
        repo.update { it.copy(isChatty = true, listenLanguage = TrekLanguage.Auto) }
        assertTrue(repo.settings.value.isChatty)
        assertEquals("Auto", store.getString(PrefKeys.LISTEN_LANGUAGE, ""))
        assertTrue(SettingsRepository(store).current.isChatty)
    }
}
