package net.ddns.mlsoftlaberge.trycorder.crew

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Contacts.CNAuthorizationStatusNotDetermined
import platform.Contacts.CNContactStore
import platform.Contacts.CNEntityType
import kotlin.coroutines.resume

/** Asks CNContactStore for access the first time; needs NSContactsUsageDescription in Info.plist. */
@Composable
actual fun ContactsPermissionRequest(onResult: (Boolean) -> Unit) {
    val callback by rememberUpdatedState(onResult)
    LaunchedEffect(Unit) {
        val status = CNContactStore.authorizationStatusForEntityType(CNEntityType.CNEntityTypeContacts)
        val granted = if (status == CNAuthorizationStatusNotDetermined) {
            suspendCancellableCoroutine { cont ->
                CNContactStore().requestAccessForEntityType(CNEntityType.CNEntityTypeContacts) { ok, _ ->
                    if (cont.isActive) cont.resume(ok)
                }
            }
        } else {
            contactsAccessGranted()
        }
        callback(granted)
    }
}
