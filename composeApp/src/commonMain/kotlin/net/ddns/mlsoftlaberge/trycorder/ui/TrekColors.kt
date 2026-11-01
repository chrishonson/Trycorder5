package net.ddns.mlsoftlaberge.trycorder.ui

import androidx.compose.ui.graphics.Color

/**
 * Colors of the original Trycorder button drawables (res/drawable/trekbutton_*.xml).
 * Each button is a vertical gradient from [top] to [bottom].
 */
enum class TrekColor(val top: Color, val bottom: Color) {
    Blue(Color(0xFFE1BEE7), Color(0xFFB39DDB)),
    Gray(Color(0xFF8DBAB3), Color(0xFF58857E)),
    Green(Color(0xFF69F0AE), Color(0xFF689F38)),
    Indigo(Color(0xFF8C9EFF), Color(0xFF3D5AFE)),
    Mauve(Color(0xFFCF5CAB), Color(0xFFB02787)),
    Orange(Color(0xFFFF7043), Color(0xFFE65100)),
    Pink(Color(0xFFCE83B6), Color(0xFFEC407A)),
    Red(Color(0xFFEC407A), Color(0xFFD50000)),
    Vert(Color(0xFF66BB6A), Color(0xFF1B5E20)),
    Yellow(Color(0xFFFFF9C4), Color(0xFFFFEA00)),
}

object TrekPalette {
    val PressedTop = Color(0xFF345953)
    val PressedBottom = Color(0xFF689A92)
    val Stroke = Color(0xFF5E7974)

    /** android.R.color.holo_green_light */
    val HoloGreenLight = Color(0xFF99CC00)

    /** android.R.color.holo_blue_light */
    val HoloBlueLight = Color(0xFF33B5E5)
    val Background = Color.Black
    val Text = Color.White
    val ButtonText = Color.Black
}
