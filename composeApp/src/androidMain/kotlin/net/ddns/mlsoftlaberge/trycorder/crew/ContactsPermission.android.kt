package net.ddns.mlsoftlaberge.trycorder.crew

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

private val CONTACT_PERMISSIONS = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)

/** Requests READ_CONTACTS and WRITE_CONTACTS (needed to store memo and transactions in the contact note). */
@Composable
actual fun ContactsPermissionRequest(onResult: (Boolean) -> Unit) {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        callback(result[Manifest.permission.READ_CONTACTS] == true)
    }
    LaunchedEffect(Unit) {
        val granted = CONTACT_PERMISSIONS.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
        if (granted) callback(true) else launcher.launch(CONTACT_PERMISSIONS)
    }
}
