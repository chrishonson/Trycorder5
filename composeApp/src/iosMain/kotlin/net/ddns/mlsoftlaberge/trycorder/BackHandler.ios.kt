package net.ddns.mlsoftlaberge.trycorder

import androidx.compose.runtime.Composable

/** iOS has no system back button; secondary screens show their own Back button. */
@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
