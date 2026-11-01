package net.ddns.mlsoftlaberge.trycorder.crew

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ddns.mlsoftlaberge.trycorder.inventory.HeaderButton
import net.ddns.mlsoftlaberge.trycorder.inventory.HeaderTitle
import net.ddns.mlsoftlaberge.trycorder.inventory.TrekField
import net.ddns.mlsoftlaberge.trycorder.inventory.TrekPageHeader
import net.ddns.mlsoftlaberge.trycorder.resources.Res
import net.ddns.mlsoftlaberge.trycorder.resources.ic_contact_picture
import net.ddns.mlsoftlaberge.trycorder.resources.trek_bottomleft
import net.ddns.mlsoftlaberge.trycorder.resources.trek_bottomright
import net.ddns.mlsoftlaberge.trycorder.resources.trek_horizontalline
import net.ddns.mlsoftlaberge.trycorder.ui.TrekColor
import net.ddns.mlsoftlaberge.trycorder.ui.TrekEdge
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.painterResource

private const val HEADER_WEIGHT = 10f
private const val LIST_WEIGHT = 85f
private const val FOOTER_WEIGHT = 5f
private const val LEFT_WEIGHT = 4f
private const val RIGHT_WEIGHT = 6f
private const val BUTTON_WEIGHT = 1.5f
private const val NAME_SP = 22
private const val INFO_SP = 18
private val ItemHeight = 64.dp

/** contact_list_fragment.xml: BACK / ADD / Budget / STAR, the "Starship Crew" title and a search field. */
@Composable
fun CrewList(
    members: List<CrewMember>,
    thumbnails: Map<String, ByteArray>,
    query: String,
    access: ContactsAccess,
    onBack: () -> Unit,
    onIntent: (CrewListIntent) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(TrekPalette.Background)) {
        TrekPageHeader(
            leftWeight = LEFT_WEIGHT,
            rightWeight = RIGHT_WEIGHT,
            onTopLeft = onBack,
            modifier = Modifier.weight(HEADER_WEIGHT),
            buttons = {
                HeaderButton("BACK", TrekColor.Gray, TrekEdge.Center, BUTTON_WEIGHT, onBack)
                HeaderButton("ADD", TrekColor.Green, TrekEdge.Center, BUTTON_WEIGHT) { onIntent(CrewListIntent.Add) }
                HeaderButton("Budget", TrekColor.Indigo, TrekEdge.Center, BUTTON_WEIGHT) {
                    onIntent(CrewListIntent.OpenBudget)
                }
                HeaderButton("STAR", TrekColor.Blue, TrekEdge.Right, BUTTON_WEIGHT) {
                    onIntent(CrewListIntent.ToggleStarred)
                }
            },
            bottom = {
                HeaderTitle("Starship Crew", Modifier.weight(1f))
                TrekField(
                    value = query,
                    onValueChange = { onIntent(CrewListIntent.Search(it)) },
                    hint = "Search",
                    fontSize = INFO_SP,
                    modifier = Modifier.weight(1f),
                )
            },
        )
        Box(Modifier.weight(LIST_WEIGHT).fillMaxWidth()) {
            if (access == ContactsAccess.Denied) {
                Text(
                    text = "Contacts permission denied",
                    color = TrekPalette.Text,
                    fontSize = INFO_SP.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(members, key = { it.id }) { m ->
                    CrewRow(m.name, thumbnails[m.id]) { onIntent(CrewListIntent.Open(m.id)) }
                }
            }
        }
        CrewFooter(Modifier.weight(FOOTER_WEIGHT))
    }
}

/** contact_list_item.xml: the QuickContactBadge photo and the holo green display name. */
@Composable
private fun CrewRow(name: String, thumbnail: ByteArray?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(ItemHeight).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContactPhoto(thumbnail, ItemHeight)
        Text(
            text = name,
            color = TrekPalette.HoloGreenLight,
            fontSize = NAME_SP.sp,
            fontWeight = FontWeight.Light,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

/** A contact photo from its encoded bytes, or ic_contact_picture. */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun ContactPhoto(bytes: ByteArray?, size: Dp, modifier: Modifier = Modifier) {
    val bitmap: ImageBitmap? = remember(bytes) { bytes?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() } }
    if (bitmap != null) {
        Image(bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier.size(size))
    } else {
        Image(
            painter = painterResource(Res.drawable.ic_contact_picture),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size),
        )
    }
}

/** The bottom frame of the list: corners 10 / line 85 / corner 5, aligned to the bottom. */
@Composable
private fun CrewFooter(modifier: Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        FooterImage(Res.drawable.trek_bottomleft, FOOTER_CORNER_LEFT, 8.dp)
        FooterImage(Res.drawable.trek_horizontalline, FOOTER_LINE, 4.dp)
        FooterImage(Res.drawable.trek_bottomright, FOOTER_CORNER_RIGHT, 8.dp)
    }
}

private const val FOOTER_CORNER_LEFT = 10f
private const val FOOTER_LINE = 85f
private const val FOOTER_CORNER_RIGHT = 5f

@Composable
private fun androidx.compose.foundation.layout.RowScope.FooterImage(res: DrawableResource, weight: Float, height: Dp) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.weight(weight).height(height),
    )
}
