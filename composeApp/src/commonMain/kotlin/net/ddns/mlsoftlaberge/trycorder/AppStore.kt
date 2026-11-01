package net.ddns.mlsoftlaberge.trycorder

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.ddns.mlsoftlaberge.trycorder.core.KeyValueStore
import net.ddns.mlsoftlaberge.trycorder.core.PrefKeys

/**
 * Navigation between the Trycorder modes. Modes replace each other like the fragment
 * transactions did; secondary screens stack on top and go back to where they were opened.
 */
class AppStore(private val store: KeyValueStore) : ViewModel() {
    private val _state = MutableStateFlow(AppState(screen = Screen.fromModeNumber(store.getInt(PrefKeys.LAST_MODE, 1))))
    val state: StateFlow<AppState> = _state.asStateFlow()

    fun dispatch(intent: AppIntent) {
        when (intent) {
            is AppIntent.Navigate -> navigate(intent.screen)
            AppIntent.Back -> back()
        }
    }

    private fun navigate(screen: Screen) {
        _state.update { current ->
            if (screen.isMode) {
                store.putInt(PrefKeys.LAST_MODE, screen.modeNumber)
                AppState(screen = screen)
            } else {
                current.copy(screen = screen, backStack = current.backStack + current.screen)
            }
        }
    }

    private fun back() {
        _state.update { current ->
            when {
                current.backStack.isNotEmpty() -> current.copy(
                    screen = current.backStack.last(),
                    backStack = current.backStack.dropLast(1),
                )
                current.screen != Screen.Trycorder -> {
                    store.putInt(PrefKeys.LAST_MODE, Screen.Trycorder.modeNumber)
                    AppState(screen = Screen.Trycorder)
                }
                else -> current
            }
        }
    }
}
