package net.ddns.mlsoftlaberge.trycorder

/**
 * Top level screens. The first seven are the fragment modes of the original TrycorderActivity
 * (their [modeNumber] is what was saved in pref_key_last_mode); the others were separate activities.
 */
enum class Screen(val modeNumber: Int) {
    Trycorder(1),
    PhotoGallery(2),
    VideoGallery(3),
    MultiSensor(4),
    VisionNight(5),
    VisionDay(6),
    Desktop(7),
    Settings(0),
    Client(0),
    Crew(0),
    Inventory(0);

    /** True for the fragment modes that are restored on the next start. */
    val isMode: Boolean get() = modeNumber > 0

    companion object {
        fun fromModeNumber(number: Int): Screen =
            entries.firstOrNull { it.isMode && it.modeNumber == number } ?: Trycorder
    }
}

data class AppState(
    val screen: Screen = Screen.Trycorder,
    val backStack: List<Screen> = emptyList(),
)

sealed interface AppIntent {
    /** Switch fragment mode or open one of the secondary screens. */
    data class Navigate(val screen: Screen) : AppIntent

    /** System back or the Back button of a secondary screen. */
    data object Back : AppIntent
}
