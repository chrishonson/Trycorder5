package net.ddns.mlsoftlaberge.trycorder.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import net.ddns.mlsoftlaberge.trycorder.resources.Res
import net.ddns.mlsoftlaberge.trycorder.resources.finalnew
import net.ddns.mlsoftlaberge.trycorder.resources.finalold
import net.ddns.mlsoftlaberge.trycorder.resources.sonysketchef
import org.jetbrains.compose.resources.Font

/**
 * The three typefaces the original app loaded from assets:
 * sonysketchef for status lines, finalold for most buttons, finalnew for action buttons.
 */
enum class TrekFont { Status, Button, Action, Default }

@Composable
fun TrekFont.family(): FontFamily = when (this) {
    TrekFont.Status -> FontFamily(Font(Res.font.sonysketchef))
    TrekFont.Button -> FontFamily(Font(Res.font.finalold))
    TrekFont.Action -> FontFamily(Font(Res.font.finalnew))
    TrekFont.Default -> FontFamily.Default
}
