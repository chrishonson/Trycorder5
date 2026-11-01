package net.ddns.mlsoftlaberge.trycorder.crew

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ddns.mlsoftlaberge.trycorder.inventory.HeaderButton
import net.ddns.mlsoftlaberge.trycorder.inventory.TrekField
import net.ddns.mlsoftlaberge.trycorder.inventory.TrekPageHeader
import net.ddns.mlsoftlaberge.trycorder.ui.TrekButton
import net.ddns.mlsoftlaberge.trycorder.ui.TrekColor
import net.ddns.mlsoftlaberge.trycorder.ui.TrekEdge
import net.ddns.mlsoftlaberge.trycorder.ui.TrekFont
import net.ddns.mlsoftlaberge.trycorder.ui.TrekFrameBar
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette

private const val HEADER_WEIGHT = 15f
private const val NAME_WEIGHT = 10f
private const val BODY_WEIGHT = 70f
private const val HALF = 5f
private const val BUTTON_WEIGHT = 2f
private const val NAME_SP = 36
private const val LABEL_SP = 18
private const val LABEL_WEIGHT = 8f
private const val AMOUNT_WEIGHT = 3f

/** contact_editmemo_fragment.xml. */
@Composable
fun MemoEditor(name: String, text: String, confirmDelete: Boolean, onIntent: (CrewAdminIntent) -> Unit) {
    EditorFrame(name, confirmDelete, onIntent) {
        Text("Memo", color = TrekPalette.HoloBlueLight, fontSize = LABEL_SP.sp)
        TrekField(
            value = text,
            onValueChange = { onIntent(CrewAdminIntent.MemoChanged(it)) },
            hint = "Memo to edit",
            fontSize = LABEL_SP,
            singleLine = false,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** contact_edittrans_fragment.xml. */
@Composable
fun TransactionEditor(
    name: String,
    date: String,
    description: String,
    amount: String,
    confirmDelete: Boolean,
    onIntent: (CrewAdminIntent) -> Unit,
) {
    EditorFrame(name, confirmDelete, onIntent) {
        Text("Date", color = TrekPalette.HoloBlueLight, fontSize = LABEL_SP.sp)
        TrekField(
            value = date,
            onValueChange = { onIntent(CrewAdminIntent.TransChanged(TransField.Date, it)) },
            hint = "2016-01-01 00:00:00",
            fontSize = LABEL_SP,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Description", color = TrekPalette.HoloBlueLight, fontSize = LABEL_SP.sp)
        TrekField(
            value = description,
            onValueChange = { onIntent(CrewAdminIntent.TransChanged(TransField.Description, it)) },
            hint = "Description of invoice",
            fontSize = LABEL_SP,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Amount",
                color = TrekPalette.HoloBlueLight,
                fontSize = LABEL_SP.sp,
                modifier = Modifier.weight(LABEL_WEIGHT),
            )
            TrekField(
                value = amount,
                onValueChange = { onIntent(CrewAdminIntent.TransChanged(TransField.Amount, it)) },
                hint = "0.00",
                fontSize = LABEL_SP,
                numeric = true,
                modifier = Modifier.weight(AMOUNT_WEIGHT),
            )
        }
    }
}

/** Header BACK / DEL / SAVE, the contact name, the framed body; DEL opens the CANCEL / DELETE popup. */
@Composable
private fun EditorFrame(
    name: String,
    confirmDelete: Boolean,
    onIntent: (CrewAdminIntent) -> Unit,
    body: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TrekPageHeader(
            leftWeight = HALF,
            rightWeight = HALF,
            onTopLeft = { onIntent(CrewAdminIntent.EditorClose) },
            modifier = Modifier.weight(HEADER_WEIGHT),
            buttons = {
                HeaderButton("BACK", TrekColor.Indigo, TrekEdge.Center, BUTTON_WEIGHT) {
                    onIntent(CrewAdminIntent.EditorClose)
                }
                HeaderButton("DEL", TrekColor.Red, TrekEdge.Center, BUTTON_WEIGHT) {
                    onIntent(CrewAdminIntent.EditorDelete)
                }
                HeaderButton("SAVE", TrekColor.Blue, TrekEdge.Right, BUTTON_WEIGHT) {
                    onIntent(CrewAdminIntent.EditorSave)
                }
            },
            bottom = {},
        )
        if (confirmDelete) {
            Row(Modifier.fillMaxWidth().height(40.dp)) {
                val cancel = { onIntent(CrewAdminIntent.EditorCancelDelete) }
                val delete = { onIntent(CrewAdminIntent.EditorConfirmDelete) }
                TrekButton("CANCEL", TrekColor.Gray, cancel, Modifier.weight(1f), font = TrekFont.Default)
                TrekButton("DELETE", TrekColor.Red, delete, Modifier.weight(1f), font = TrekFont.Default)
            }
        }
        Text(
            text = name,
            color = TrekPalette.HoloBlueLight,
            fontSize = NAME_SP.sp,
            maxLines = 1,
            modifier = Modifier.weight(NAME_WEIGHT).padding(horizontal = 4.dp),
        )
        TrekFrameBar(top = true, cornerWeight = 1f, lineWeight = LABEL_WEIGHT)
        Column(
            Modifier.weight(BODY_WEIGHT).fillMaxWidth().verticalScroll(rememberScrollState()).padding(4.dp),
            content = body,
        )
        TrekFrameBar(top = false, cornerWeight = 1f, lineWeight = LABEL_WEIGHT)
        Spacer(Modifier.height(4.dp))
    }
}
