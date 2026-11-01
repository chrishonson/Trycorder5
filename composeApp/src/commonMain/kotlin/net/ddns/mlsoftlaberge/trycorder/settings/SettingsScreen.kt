package net.ddns.mlsoftlaberge.trycorder.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ddns.mlsoftlaberge.trycorder.BackHandler
import net.ddns.mlsoftlaberge.trycorder.Screen
import net.ddns.mlsoftlaberge.trycorder.core.PlatformServices
import net.ddns.mlsoftlaberge.trycorder.core.TrekLanguage
import net.ddns.mlsoftlaberge.trycorder.core.TrekSound
import net.ddns.mlsoftlaberge.trycorder.inventory.TrekField
import net.ddns.mlsoftlaberge.trycorder.resources.Res
import net.ddns.mlsoftlaberge.trycorder.resources.btn_check_on
import net.ddns.mlsoftlaberge.trycorder.ui.TrekButton
import net.ddns.mlsoftlaberge.trycorder.ui.TrekColor
import net.ddns.mlsoftlaberge.trycorder.ui.TrekEdge
import net.ddns.mlsoftlaberge.trycorder.ui.TrekFont
import net.ddns.mlsoftlaberge.trycorder.ui.TrekPalette
import org.jetbrains.compose.resources.painterResource

private const val TITLE_SP = 18
private const val SUMMARY_SP = 14
private const val CATEGORY_SP = 14
private const val HEADER_SP = 20
private val SummaryColor = Color(0xFFBEBEBE)
private val DividerColor = Color(0xFF2E2E2E)
private val DialogColor = Color(0xFF282828)
private val ScrimColor = Color(0xB0000000)
private val CheckSize = 32.dp

/** What one preference row shows. */
data class PreferenceRow(
    val key: String,
    val title: String,
    val summary: String,
    /** Checked state of a checkbox, null for list and text preferences. */
    val checked: Boolean?,
)

/** The preferences screen (SettingsActivity + SettingsFragment of the original). */
@Suppress("UnusedParameter") // onNavigate: signature fixed by App, this screen opens no other
@Composable
fun SettingsScreen(services: PlatformServices, onNavigate: (Screen) -> Unit, onBack: () -> Unit) {
    val store = viewModel { SettingsStore(services.settings) }
    val state by store.state.collectAsState()
    val rows = SettingsCatalog.all.map { p ->
        val checked = (p as? Preference.Check)?.let { CheckFields.get(state.settings, p.key) }
        PreferenceRow(p.key, p.title, p.summary, checked)
    }
    val open = state.openKey?.let { SettingsCatalog.find(it) }
    BackHandler(enabled = open != null) { store.dispatch(SettingsIntent.Dismiss) }
    Box(Modifier.fillMaxSize().background(TrekPalette.Background)) {
        SettingsList(
            rows = rows,
            onClick = { store.dispatch(SettingsIntent.Click(it)) },
            onBack = {
                services.sound.play(TrekSound.KeyOk)
                onBack()
            },
        )
        when (open) {
            is Preference.LanguageChoice -> LanguageDialog(
                title = open.title,
                selected = ValueFields.language(state.settings, open.key),
                onChoose = { store.dispatch(SettingsIntent.Choose(open.key, it)) },
                onCancel = { store.dispatch(SettingsIntent.Dismiss) },
            )
            is Preference.Text -> TextDialog(
                title = open.title,
                text = state.draft,
                onTextChange = { store.dispatch(SettingsIntent.DraftChanged(it)) },
                onOk = { store.dispatch(SettingsIntent.ConfirmText) },
                onCancel = { store.dispatch(SettingsIntent.Dismiss) },
            )
            else -> Unit
        }
    }
}

@Composable
private fun SettingsList(rows: List<PreferenceRow>, onClick: (String) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(48.dp).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            TrekButton(
                text = "BACK",
                color = TrekColor.Indigo,
                onClick = onBack,
                modifier = Modifier.weight(1f).height(36.dp),
                edge = TrekEdge.Left,
                font = TrekFont.Default,
                fontSize = 16.sp,
            )
            Text(
                text = "Settings",
                color = TrekPalette.HoloGreenLight,
                fontSize = HEADER_SP.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(2f).padding(start = 12.dp),
            )
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            item { CategoryHeader(SettingsCatalog.CATEGORY) }
            items(rows, key = { it.key }) { row ->
                PreferenceItem(row.title, row.summary, row.checked) { onClick(row.key) }
            }
        }
    }
}

@Composable
private fun CategoryHeader(title: String) {
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
        Text(
            text = title.uppercase(),
            color = TrekPalette.HoloBlueLight,
            fontSize = CATEGORY_SP.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.fillMaxWidth().padding(top = 4.dp).height(1.dp).background(TrekPalette.HoloBlueLight))
    }
}

@Composable
private fun PreferenceItem(title: String, summary: String, checked: Boolean?, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = TrekPalette.Text, fontSize = TITLE_SP.sp)
                Text(summary, color = SummaryColor, fontSize = SUMMARY_SP.sp)
            }
            if (checked != null) CheckMark(checked)
        }
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(DividerColor))
    }
}

/** The holo checkbox: btn_check_on when checked, an empty holo box otherwise. */
@Composable
private fun CheckMark(checked: Boolean) {
    if (checked) {
        Image(painterResource(Res.drawable.btn_check_on), contentDescription = "checked", Modifier.size(CheckSize))
    } else {
        Box(Modifier.size(CheckSize).padding(8.dp).border(2.dp, SummaryColor))
    }
}

/** Full-screen scrim with a centered dialog card, like the stock preference dialogs. */
@Composable
private fun DialogFrame(title: String, onCancel: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(ScrimColor).clickable(onClick = onCancel),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth(DIALOG_WIDTH)
                .background(DialogColor, RoundedCornerShape(4.dp))
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .padding(16.dp),
        ) {
            Text(title, color = TrekPalette.HoloBlueLight, fontSize = HEADER_SP.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(TrekPalette.HoloBlueLight))
            content()
        }
    }
}

private const val DIALOG_WIDTH = 0.85f

@Composable
private fun LanguageDialog(
    title: String,
    selected: TrekLanguage,
    onChoose: (TrekLanguage) -> Unit,
    onCancel: () -> Unit,
) {
    DialogFrame(title, onCancel) {
        SettingsCatalog.languages.forEach { language ->
            Row(
                Modifier.fillMaxWidth().clickable { onChoose(language) }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(language.label, color = TrekPalette.Text, fontSize = TITLE_SP.sp, modifier = Modifier.weight(1f))
                val ring = if (language == selected) TrekPalette.HoloBlueLight else SummaryColor
                Box(Modifier.size(20.dp).border(2.dp, ring, CircleShape).padding(5.dp)) {
                    if (language == selected) Box(Modifier.fillMaxSize().background(ring, CircleShape))
                }
            }
        }
        DialogButtons(onCancel = onCancel, onOk = null)
    }
}

@Composable
private fun TextDialog(
    title: String,
    text: String,
    onTextChange: (String) -> Unit,
    onOk: () -> Unit,
    onCancel: () -> Unit,
) {
    DialogFrame(title, onCancel) {
        TrekField(
            value = text,
            onValueChange = onTextChange,
            hint = title,
            fontSize = TITLE_SP,
            modifier = Modifier.fillMaxWidth().border(1.dp, TrekPalette.HoloBlueLight),
        )
        DialogButtons(onCancel = onCancel, onOk = onOk)
    }
}

@Composable
private fun DialogButtons(onCancel: () -> Unit, onOk: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp).height(40.dp)) {
        TrekButton("Cancel", TrekColor.Gray, onCancel, Modifier.weight(1f), font = TrekFont.Default, fontSize = 16.sp)
        if (onOk != null) {
            TrekButton("OK", TrekColor.Blue, onOk, Modifier.weight(1f), font = TrekFont.Default, fontSize = 16.sp)
        }
    }
}
