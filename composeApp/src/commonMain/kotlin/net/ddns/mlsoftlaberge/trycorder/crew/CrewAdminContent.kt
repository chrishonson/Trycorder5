package net.ddns.mlsoftlaberge.trycorder.crew

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ddns.mlsoftlaberge.trycorder.inventory.HeaderButton
import net.ddns.mlsoftlaberge.trycorder.inventory.HeaderTitle
import net.ddns.mlsoftlaberge.trycorder.inventory.TrekPageHeader
import net.ddns.mlsoftlaberge.trycorder.resources.Res
import net.ddns.mlsoftlaberge.trycorder.resources.call_contact
import net.ddns.mlsoftlaberge.trycorder.resources.ic_action_add
import net.ddns.mlsoftlaberge.trycorder.resources.ic_action_check_in
import net.ddns.mlsoftlaberge.trycorder.resources.ic_dialog_email
import net.ddns.mlsoftlaberge.trycorder.resources.ic_menu_edit
import net.ddns.mlsoftlaberge.trycorder.resources.ic_menu_save
import net.ddns.mlsoftlaberge.trycorder.ui.TrekColor
import net.ddns.mlsoftlaberge.trycorder.ui.TrekEdge
import net.ddns.mlsoftlaberge.trycorder.ui.TrekFrameBar
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette
import net.ddns.mlsoftlaberge.trycorder.ui.trekBackground
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private const val HEADER_WEIGHT = 10f
private const val IDENTITY_WEIGHT = 20f
private const val SCROLL_WEIGHT = 70f
private const val LEFT_WEIGHT = 7f
private const val RIGHT_WEIGHT = 3f
private const val BUTTON_WEIGHT = 3f
private const val PHOTO_WEIGHT = 35f
private const val NAME_WEIGHT = 65f
private const val NAME_SP = 38
private const val TEXT_WEIGHT = 8f
private const val AMOUNT_WEIGHT = 3f
private const val ICON_WEIGHT = 2f
private const val HEADER_LABEL_WEIGHT = 3f
private const val VALUE_WEIGHT = 6f
private const val DESCRIPTION_SP = 16
private const val AMOUNT_SP = 18
private val PhotoHeight = 120.dp
private val MinRow = 32.dp

/** contact_admin_fragment.xml: identity, memo, transactions with total, addresses, phones, emails. */
@Composable
fun CrewAdmin(
    name: String,
    starred: Boolean,
    photo: ByteArray?,
    canEdit: Boolean,
    memo: String,
    transactions: List<CrewTransaction>,
    total: Double,
    addresses: List<CrewEntry>,
    phones: List<CrewEntry>,
    emails: List<CrewEntry>,
    onBack: () -> Unit,
    onIntent: (CrewAdminIntent) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TrekPageHeader(
            leftWeight = LEFT_WEIGHT,
            rightWeight = RIGHT_WEIGHT,
            onTopLeft = onBack,
            modifier = Modifier.weight(HEADER_WEIGHT),
            buttons = {
                HeaderButton("BACK", TrekColor.Indigo, TrekEdge.Center, BUTTON_WEIGHT, onBack)
                if (canEdit) {
                    HeaderButton("EDIT", TrekColor.Blue, TrekEdge.Right, BUTTON_WEIGHT) {
                        onIntent(CrewAdminIntent.EditContact)
                    }
                }
            },
            bottom = { HeaderTitle(if (starred) "Starred" else "") },
        )
        Row(Modifier.weight(IDENTITY_WEIGHT).fillMaxWidth()) {
            Column(Modifier.weight(PHOTO_WEIGHT), horizontalAlignment = Alignment.CenterHorizontally) {
                ContactPhoto(photo, PhotoHeight)
            }
            Text(
                text = name,
                color = TrekPalette.HoloBlueLight,
                fontSize = NAME_SP.sp,
                modifier = Modifier.weight(NAME_WEIGHT).fillMaxHeight(),
            )
        }
        TrekFrameBar(top = true, cornerWeight = 1f, lineWeight = TEXT_WEIGHT)
        Column(Modifier.weight(SCROLL_WEIGHT).fillMaxWidth().verticalScroll(rememberScrollState())) {
            MemoSection(memo) { onIntent(CrewAdminIntent.EditMemo) }
            TransactionsSection(transactions, total, onIntent)
            addresses.forEach { a ->
                EntryRow("${a.label} Address", a.value, Res.drawable.ic_action_check_in) {
                    onIntent(CrewAdminIntent.ShowAddress(a.value))
                }
            }
            phones.forEach { p ->
                EntryRow("${p.label} Phone", p.value, Res.drawable.call_contact) {
                    onIntent(CrewAdminIntent.Call(p.value))
                }
            }
            emails.forEach { e ->
                EntryRow("${e.label} Email", e.value, Res.drawable.ic_dialog_email) {
                    onIntent(CrewAdminIntent.Email(e.value))
                }
            }
        }
        TrekFrameBar(top = false, cornerWeight = 1f, lineWeight = TEXT_WEIGHT)
    }
}

@Composable
private fun MemoSection(memo: String, onEdit: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(4.dp)) {
        Text("MEMO", color = TrekPalette.HoloBlueLight, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().heightIn(min = MinRow), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = memo.ifEmpty { "Text Note" }.trimEnd('\n'),
                color = TrekPalette.Text,
                modifier = Modifier.weight(1f),
            )
            IconAction(Res.drawable.ic_menu_edit, "Edit memo", Modifier, onEdit)
        }
    }
}

@Composable
private fun TransactionsSection(
    transactions: List<CrewTransaction>,
    total: Double,
    onIntent: (CrewAdminIntent) -> Unit,
) {
    IndigoBar {
        BarText("DESCRIPTION", TEXT_WEIGHT, TextAlign.Start)
        BarText("AMOUNT", AMOUNT_WEIGHT, TextAlign.End)
        IconAction(Res.drawable.ic_action_add, "Add transaction", Modifier.weight(ICON_WEIGHT)) {
            onIntent(CrewAdminIntent.AddTransaction)
        }
    }
    Column(Modifier.fillMaxWidth().heightIn(min = MinRow)) {
        transactions.forEachIndexed { i, t ->
            TransactionRow(t.date, t.description, t.amount) { onIntent(CrewAdminIntent.EditTransaction(i)) }
        }
    }
    IndigoBar {
        BarText("TOTAL", TEXT_WEIGHT, TextAlign.End)
        BarText(Amounts.format(total), AMOUNT_WEIGHT, TextAlign.End)
        IconAction(Res.drawable.ic_menu_save, "Save transactions", Modifier.weight(ICON_WEIGHT)) {
            onIntent(CrewAdminIntent.SaveNote)
        }
    }
}

/** contact_transaction_item.xml. */
@Composable
private fun TransactionRow(date: String, description: String, amount: String, onEdit: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(TEXT_WEIGHT).padding(start = 4.dp)) {
            Text(date, color = TrekPalette.Text)
            Text(description, color = TrekPalette.Text, fontSize = DESCRIPTION_SP.sp)
        }
        Text(
            text = amount,
            color = TrekPalette.Text,
            fontSize = AMOUNT_SP.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(AMOUNT_WEIGHT),
        )
        IconAction(Res.drawable.ic_menu_edit, "Edit transaction", Modifier.weight(ICON_WEIGHT), onEdit)
    }
}

/** contact_address_item / contact_phone_item / contact_email_item. */
@Composable
private fun EntryRow(header: String, value: String, icon: DrawableResource, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = header.uppercase(),
            color = TrekPalette.HoloBlueLight,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(HEADER_LABEL_WEIGHT),
        )
        Text(value, color = TrekPalette.Text, modifier = Modifier.weight(VALUE_WEIGHT))
        IconAction(icon, header, Modifier, onClick)
    }
}

@Composable
private fun IndigoBar(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = MinRow)
            .trekBackground(TrekColor.Indigo, TrekEdge.Round)
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun RowScope.BarText(text: String, weight: Float, align: TextAlign) {
    Text(
        text = text,
        color = TrekPalette.Text,
        fontWeight = FontWeight.Bold,
        textAlign = align,
        modifier = Modifier.weight(weight),
    )
}

@Composable
private fun IconAction(icon: DrawableResource, description: String, modifier: Modifier, onClick: () -> Unit) {
    Image(
        painter = painterResource(icon),
        contentDescription = description,
        modifier = modifier.size(MinRow + 8.dp).clickable(onClick = onClick).padding(4.dp),
    )
}
