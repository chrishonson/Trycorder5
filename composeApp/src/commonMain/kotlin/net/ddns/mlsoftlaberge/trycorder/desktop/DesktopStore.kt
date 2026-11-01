package net.ddns.mlsoftlaberge.trycorder.desktop

import net.ddns.mlsoftlaberge.trycorder.Screen

/** State of the Desktop screen (TrydesktopFragment). */
typealias DesktopState = WalkieState

/** Intents of the Desktop screen. */
typealias DesktopIntent = WalkieIntent

/**
 * Store of the Desktop mode: the walkie-talkie console beside the front camera.
 * The corners and Back return to the Trycorder mode (onTrydesktopModeChange(1)).
 */
class DesktopStore(deps: WalkieDeps, navigate: (Screen) -> Unit) : WalkieStore(
    deps = deps,
    title = TITLE,
    onBack = { navigate(Screen.Trycorder) },
    onSettings = { navigate(Screen.Settings) },
) {
    companion object {
        const val TITLE = "Desktop"
    }
}
