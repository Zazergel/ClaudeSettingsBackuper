package ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import app.BackupEngine
import app.DiffRow
import app.Project
import app.lineDiff
import app.readShadow
import java.nio.file.Files

private val REMOVED_BG = Theme.danger.copy(alpha = 0.18f)
private val ADDED_BG = Theme.ok.copy(alpha = 0.18f)

/** rows — построчный дифф; fromBackup — «было» взят из бекапа (нет теневой копии); noOld — «было» нет вовсе. */
private data class DiffData(val rows: List<DiffRow>, val fromBackup: Boolean, val noOld: Boolean)

private fun loadDiff(p: Project): DiffData {
    val newText = runCatching { Files.readString(p.settings) }.getOrDefault("")
    fun lines(s: String) = s.split("\n").map { it.removeSuffix("\r") } // CRLF не должен светиться как изменение
    val shadow = readShadow(p.settings)
    val backup = BackupEngine.listBackups(p.settings).firstOrNull()
        ?.let { runCatching { Files.readString(it) }.getOrNull() }
    val oldText = shadow ?: backup
    return DiffData(
        // без «было» дифф не строим — весь текст показываем как «стало» без подсветки
        rows = if (oldText != null) lineDiff(lines(oldText), lines(newText)) else lines(newText).mapIndexed { i, t -> DiffRow.Unchanged(t, 0, i + 1) },
        fromBackup = oldText != null && shadow == null,
        noOld = oldText == null,
    )
}

/** Диалог «Было / Стало» для внешне изменённого конфига: одна общая прокрутка, у ячеек общей строки — равная высота. */
@Composable
fun DiffDialog(project: Project, onClose: () -> Unit) {
    var data by remember { mutableStateOf<DiffData?>(null) }
    LaunchedEffect(project) { data = loadDiff(project) }
    DialogWindow(
        onCloseRequest = onClose,
        title = "Изменения: " + project.root.fileName,
        state = rememberDialogState(size = DpSize(880.dp, 620.dp)),
        resizable = false, // как у EditDialog: у DialogWindow нет minimumSize — фиксированный размер вместо ломающегося ресайза
    ) {
        Box(Modifier.fillMaxSize().background(Theme.bg1)) {
            Card(
                Modifier.fillMaxSize().padding(16.dp),
                shape = Theme.cardShape,
                colors = CardDefaults.cardColors(containerColor = Theme.card),
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    val d = data
                    if (d == null) return@Column
                    Row {
                        Text(
                            if (d.fromBackup) "Было (из последнего бекапа)" else "Было",
                            color = if (d.noOld) Theme.textDim else Theme.text,
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text("Стало", color = Theme.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (d.noOld) Text(
                        "Нет сохранённой копии для сравнения — показан только текущий файл",
                        color = Theme.warn, fontSize = 11.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        val scrollState = rememberScrollState()
                        // ponytail: мягкий перенос длинных строк вместо horizontalScroll — гуттеры обеих колонок остаются на месте;
                        // IntrinsicSize.Min + fillMaxHeight выравнивают фоны подсветки ячеек общей строки
                        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
                            d.rows.forEach { row ->
                                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                                    Cell(row, side = false, Modifier.weight(1f))
                                    Cell(row, side = true, Modifier.weight(1f))
                                }
                            }
                        }
                        VerticalScrollbar(
                            rememberScrollbarAdapter(scrollState),
                            Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 2.dp),
                            style = AppScrollbarStyle(),
                        )
                    }
                }
            }
        }
    }
}

/** Ячейка одной строки диффа: номер + текст; фон — только у изменённой строки на её стороне, напротив — пусто. */
@Composable
private fun Cell(row: DiffRow, side: Boolean, modifier: Modifier) {
    // side: false = «было», true = «стало»
    val num: Int?
    val text: String
    val bg: Color
    when {
        row is DiffRow.Unchanged -> { num = if (side) row.newLine else row.oldLine; text = row.text; bg = Color.Transparent }
        row is DiffRow.Removed && !side -> { num = row.oldLine; text = row.text; bg = REMOVED_BG }
        row is DiffRow.Added && side -> { num = row.newLine; text = row.text; bg = ADDED_BG }
        else -> { num = null; text = " "; bg = Color.Transparent }
    }
    Row(
        modifier
            .fillMaxHeight()
            .background(bg)
            .padding(start = 6.dp, end = 6.dp, top = 0.dp, bottom = 0.dp),
    ) {
        Text(
            num?.toString() ?: "",
            color = Theme.textDim.copy(alpha = 0.55f),
            fontSize = 11.sp, lineHeight = 18.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(30.dp),
        )
        Text(
            text,
            color = Theme.text,
            fontSize = 12.sp, lineHeight = 18.sp, fontFamily = FontFamily.Monospace,
        )
    }
}
