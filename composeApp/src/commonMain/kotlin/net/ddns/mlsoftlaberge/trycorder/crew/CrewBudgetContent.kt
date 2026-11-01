package net.ddns.mlsoftlaberge.trycorder.crew

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ddns.mlsoftlaberge.trycorder.ui.TrekButton
import net.ddns.mlsoftlaberge.trycorder.ui.TrekColor
import net.ddns.mlsoftlaberge.trycorder.ui.TrekEdge
import net.ddns.mlsoftlaberge.trycorder.ui.TrekFont
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette

private const val TITLE_SP = 18
private const val LINE_SP = 16
private const val ID_WEIGHT = 2f
private const val NAME_WEIGHT = 5f
private const val DESCRIPTION_WEIGHT = 8f
private const val AMOUNT_WEIGHT = 2f
private val DarkerGray = Color(0xFFAAAAAA)

/**
 * contacts_budget_fragment.xml: "BUDGET", then each client (id, name, phone, address, email)
 * with its transactions, and the grand TOTAL. The original relied on the action bar Up button:
 * an LCARS BACK button replaces it.
 */
@Composable
fun CrewBudget(entries: List<BudgetEntry>, total: Double, loading: Boolean, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(TrekPalette.Background).padding(4.dp)) {
        Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            TrekButton(
                text = "BACK",
                color = TrekColor.Indigo,
                onClick = onBack,
                modifier = Modifier.weight(1f).height(32.dp),
                edge = TrekEdge.Left,
                font = TrekFont.Default,
                fontSize = LINE_SP.sp,
            )
            Text(
                text = if (loading) "BUDGET ..." else "BUDGET",
                color = TrekPalette.Text,
                fontSize = TITLE_SP.sp,
                modifier = Modifier.weight(2f).padding(start = 8.dp),
            )
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(entries, key = { it.id }) { e -> BudgetClient(e) }
            item { BudgetLine("TOTAL", Amounts.format(total), TITLE_SP) }
        }
    }
}

/** contacts_budget_item.xml. */
@Composable
private fun BudgetClient(entry: BudgetEntry) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text(entry.id, color = TrekPalette.Text, fontSize = TITLE_SP.sp, modifier = Modifier.weight(ID_WEIGHT))
            Text(entry.name, color = TrekPalette.Text, fontSize = TITLE_SP.sp, modifier = Modifier.weight(NAME_WEIGHT))
            Text(entry.phone, color = TrekPalette.Text, fontSize = TITLE_SP.sp, modifier = Modifier.weight(NAME_WEIGHT))
        }
        Text(entry.address, color = TrekPalette.Text, fontSize = LINE_SP.sp)
        Text(entry.email, color = TrekPalette.Text, fontSize = LINE_SP.sp)
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(DarkerGray))
        entry.transactions.forEach { t -> BudgetLine(t.description, t.amount, LINE_SP) }
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(DarkerGray))
    }
}

/** contacts_budget_trans.xml. */
@Composable
private fun BudgetLine(description: String, amount: String, size: Int) {
    Row(Modifier.fillMaxWidth()) {
        Text(description, color = TrekPalette.Text, fontSize = size.sp, modifier = Modifier.weight(DESCRIPTION_WEIGHT))
        Text(
            text = amount,
            color = TrekPalette.Text,
            fontSize = size.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(AMOUNT_WEIGHT),
        )
    }
}
