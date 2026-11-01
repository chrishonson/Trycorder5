package net.ddns.mlsoftlaberge.trycorder

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ddns.mlsoftlaberge.trycorder.client.ClientScreen
import net.ddns.mlsoftlaberge.trycorder.core.PlatformServices
import net.ddns.mlsoftlaberge.trycorder.crew.CrewScreen
import net.ddns.mlsoftlaberge.trycorder.desktop.DesktopScreen
import net.ddns.mlsoftlaberge.trycorder.gallery.GalleryScreen
import net.ddns.mlsoftlaberge.trycorder.inventory.InventoryScreen
import net.ddns.mlsoftlaberge.trycorder.multisensor.MultiSensorScreen
import net.ddns.mlsoftlaberge.trycorder.settings.SettingsScreen
import net.ddns.mlsoftlaberge.trycorder.trycorder.TrycorderScreen
import net.ddns.mlsoftlaberge.trycorder.trycorder.TrycorderStore
import net.ddns.mlsoftlaberge.trycorder.vision.VisionScreen

/** Root of the shared UI on both platforms. */
@Composable
fun App(services: PlatformServices, appStore: AppStore = viewModel { AppStore(services.keyValueStore) }) {
    val state by appStore.state.collectAsState()
    val navigate: (Screen) -> Unit = { appStore.dispatch(AppIntent.Navigate(it)) }
    val back: () -> Unit = { appStore.dispatch(AppIntent.Back) }
    BackHandler(enabled = state.backStack.isNotEmpty() || state.screen != Screen.Trycorder, onBack = back)
    when (state.screen) {
        Screen.Trycorder -> TrycorderScreen(viewModel { TrycorderStore(services, navigate) }, services.camera)
        Screen.PhotoGallery -> GalleryScreen(services, video = false, onNavigate = navigate, onBack = back)
        Screen.VideoGallery -> GalleryScreen(services, video = true, onNavigate = navigate, onBack = back)
        Screen.MultiSensor -> MultiSensorScreen(services, onNavigate = navigate, onBack = back)
        Screen.VisionNight -> VisionScreen(services, day = false, onNavigate = navigate, onBack = back)
        Screen.VisionDay -> VisionScreen(services, day = true, onNavigate = navigate, onBack = back)
        Screen.Desktop -> DesktopScreen(services, onNavigate = navigate, onBack = back)
        Screen.Settings -> SettingsScreen(services, onNavigate = navigate, onBack = back)
        Screen.Client -> ClientScreen(services, onNavigate = navigate, onBack = back)
        Screen.Crew -> CrewScreen(services, onNavigate = navigate, onBack = back)
        Screen.Inventory -> InventoryScreen(services, onNavigate = navigate, onBack = back)
    }
}

/** System back gesture/button. Android: BackHandler. iOS: no system back, screens show a Back button. */
@Composable
expect fun BackHandler(enabled: Boolean, onBack: () -> Unit)
