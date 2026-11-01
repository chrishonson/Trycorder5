package net.ddns.mlsoftlaberge.trycorder.client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.core.PlatformServices
import net.ddns.mlsoftlaberge.trycorder.ui.TrekButton
import net.ddns.mlsoftlaberge.trycorder.ui.TrekColor
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette

/** Placeholder until the feature is ported. Signature is the contract used by App. */
@Composable
fun ClientScreen(services: PlatformServices, onNavigate: (Screen) -> Unit, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(TrekPalette.Background), contentAlignment = Alignment.Center) {
        Text("ClientScreen $services", color = TrekPalette.HoloGreenLight)
        TrekButton("Back", TrekColor.Blue, onBack)
    }
}
