package net.ddns.mlsoftlaberge.trycorder.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Plays an animated GIF from composeResources/files (the original GIFView).
 * [resourcePath] is a Res path such as "files/warp_animation.gif".
 */
@Composable
expect fun AnimatedGif(resourcePath: String, modifier: Modifier = Modifier)
