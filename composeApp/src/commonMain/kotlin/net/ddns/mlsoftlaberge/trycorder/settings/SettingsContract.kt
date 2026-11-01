package net.ddns.mlsoftlaberge.trycorder.settings

import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import net.ddns.mlsoftlaberge.trycorder.core.TrycorderSettings

data class SettingsState(
    val settings: TrycorderSettings = TrycorderSettings(),
    /** Key of the list or text preference whose dialog is open, or null. */
    val openKey: String? = null,
    /** Text typed in the EditTextPreference dialog. */
    val draft: String = "",
)

sealed interface SettingsIntent {
    /** Click on a preference: toggles a checkbox, opens the dialog of a list or text preference. */
    data class Click(val key: String) : SettingsIntent
    data class Choose(val key: String, val language: TrekLanguage) : SettingsIntent
    data class DraftChanged(val text: String) : SettingsIntent

    /** OK of the text dialog. */
    data object ConfirmText : SettingsIntent

    /** Cancel of the dialog. */
    data object Dismiss : SettingsIntent
}
