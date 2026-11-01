package net.ddns.mlsoftlaberge.trycorder.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.desktop.WalkieIntent
import net.ddns.mlsoftlaberge.trycorder.desktop.WalkieWorld
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClientStoreTest {
    private val navigated = mutableListOf<Screen>()
    private var backs = 0

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun store(world: WalkieWorld) = ClientStore(world.deps, { navigated += it }, { backs++ })

    @Test
    fun titleIsWalkieTalkie() {
        assertEquals("Walkie Talkie", store(WalkieWorld()).state.value.topStatus)
    }

    @Test
    fun backClosesTheScreen() {
        val store = store(WalkieWorld())
        store.dispatch(WalkieIntent.Back)
        assertEquals(1, backs)
        assertTrue(navigated.isEmpty())
    }

    @Test
    fun settingsOpensTheSettingsScreen() {
        store(WalkieWorld()).dispatch(WalkieIntent.Settings)
        assertEquals(listOf(Screen.Settings), navigated)
    }

    @Test
    fun spokenSentencesAreSent() = runTest {
        val world = WalkieWorld()
        val store = store(world)
        store.dispatch(WalkieIntent.Start)
        advanceUntilIdle()
        world.listener.emit(listOf("beam me up"))
        advanceUntilIdle()
        assertEquals(listOf("beam me up"), world.link.sent)
        assertTrue(store.state.value.consoleLog.contains("Understood: beam me up"))
    }
}
