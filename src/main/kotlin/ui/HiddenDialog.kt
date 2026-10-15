package ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import java.nio.file.Path

/** Restore hidden projects back into the list (opened from the File menu). */
@Composable
fun HiddenDialog(paths: List<Path>, onRestore: (Path) -> Unit, onClose: () -> Unit) {
    DialogWindow(
        onCloseRequest = onClose,
        title = "Скрытые проекты",
        state = rememberDialogState(size = DpSize(480.dp, 480.dp)),
        resizable = true,
    ) {
        Box(Modifier.fillMaxSize().background(Theme.bg1)) {
            Card(
                Modifier.fillMaxSize().padding(16.dp),
                shape = Theme.cardShape,
                colors = CardDefaults.cardColors(containerColor = Theme.card),
            ) {
                Column(Modifier.fillMaxSize().padding(20.dp)) {
                    Text("Скрытые проекты", color = Theme.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    if (paths.isEmpty()) {
                        Text("Нет скрытых проектов", color = Theme.textDim, fontSize = 13.sp)
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        val scrollState = rememberScrollState()
                        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
                            paths.forEach { path ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            path.fileName.toString(),
                                            color = Theme.text, fontSize = 13.sp,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            path.parent?.toString() ?: "",
                                            color = Theme.textDim, fontSize = 11.sp,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    GradientButton("Вернуть", onClick = { onRestore(path) })
                                }
                            }
                        }
                        VerticalScrollbar(
                            rememberScrollbarAdapter(scrollState),
                            Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                            style = AppScrollbarStyle(),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    GradientButton("Закрыть", onClick = onClose)
                }
            }
        }
    }
}
