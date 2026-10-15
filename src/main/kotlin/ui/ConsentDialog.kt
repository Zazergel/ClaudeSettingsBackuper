package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import app.RestoreChoice

/**
 * Requirement 6: warning shown when the current settings.json has no backups.
 * Closing the dialog (Esc, X) counts as "Нет".
 */
@Composable
fun ConsentDialog(onChoice: (RestoreChoice) -> Unit) {
    DialogWindow(onCloseRequest = { onChoice(RestoreChoice.CANCEL) }, title = "Внимание", resizable = false) {
        Box(Modifier.fillMaxSize().background(Theme.bg1)) {
            Card(shape = Theme.cardShape, colors = CardDefaults.cardColors(containerColor = Theme.card)) {
            Column(Modifier.padding(24.dp).width(460.dp)) {
                Text(
                    "Текущая конфигурация будет перезаписана. Вы уверены?",
                    color = Theme.warn, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Бекап текущего файла не найден. Продолжая, вы замените settings.json содержимым выбранного бекапа.",
                    color = Theme.textDim, fontSize = 13.sp,
                )
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradientButton("Да", onClick = { onChoice(RestoreChoice.PROCEED) }, modifier = Modifier.fillMaxWidth())
                    GradientButton(
                        "Да, но сначала сохранить текущую конфигурацию",
                        onClick = { onChoice(RestoreChoice.BACKUP_THEN_RESTORE) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    GradientButton("Нет", onClick = { onChoice(RestoreChoice.CANCEL) }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        }
    }
}
