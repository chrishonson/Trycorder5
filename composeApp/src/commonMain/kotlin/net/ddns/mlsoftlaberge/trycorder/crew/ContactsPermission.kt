package net.ddns.mlsoftlaberge.trycorder.crew

import androidx.compose.runtime.Composable

/**
 * Asks for the address book permission when the crew screen opens, then reports the answer.
 * Android: READ_CONTACTS + WRITE_CONTACTS runtime permissions. iOS: CNContactStore access.
 */
@Composable
expect fun ContactsPermissionRequest(onResult: (Boolean) -> Unit)
