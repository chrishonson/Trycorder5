package net.ddns.mlsoftlaberge.trycorder.client

import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.desktop.WalkieDeps
import net.ddns.mlsoftlaberge.trycorder.desktop.WalkieIntent
import net.ddns.mlsoftlaberge.trycorder.desktop.WalkieState
import net.ddns.mlsoftlaberge.trycorder.desktop.WalkieStore

/** State of the Client screen (TryclientFragment, "Walkie Talkie"). */
typealias ClientState = WalkieState

/** Intents of the Client screen. */
typealias ClientIntent = WalkieIntent

/**
 * Store of the Client screen. It was a separate activity: the corners and Back close it
 * (onTryclientModeChange -> finish()).
 */
class ClientStore(deps: WalkieDeps, navigate: (Screen) -> Unit, back: () -> Unit) : WalkieStore(
    deps = deps,
    title = TITLE,
    onBack = back,
    onSettings = { navigate(Screen.Settings) },
) {
    companion object {
        const val TITLE = "Walkie Talkie"
    }
}
