package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.Desktop
import java.net.URI

private const val DOCS_URL = "https://code.claude.com/docs/ru/settings-reference"

private fun openDocs() {
    // на Linux java.awt.Desktop часто не умеет BROWSE — фолбэк на xdg-open
    val desktop = runCatching { Desktop.getDesktop() }.getOrNull()
    if (desktop != null && runCatching { desktop.isSupported(Desktop.Action.BROWSE) }.getOrDefault(false)) {
        if (runCatching { desktop.browse(URI(DOCS_URL)) }.isSuccess) return
    }
    runCatching { ProcessBuilder("xdg-open", DOCS_URL).start() }
}

/** Одна компактная строка вместо MenuBar и отдельного заголовка: название слева, действия и меню «≡» справа. */
@Composable
fun TopBar(
    scanning: Boolean,
    onRescan: () -> Unit,
    onHidden: () -> Unit,
    onAddRoot: () -> Unit,
    onAbout: () -> Unit,
    onExit: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Claude Settings Backuper",
            style = TextStyle(brush = Theme.accentGradient, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.weight(1f))
        if (scanning) {
            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Theme.accent)
        } else {
            MenuAction("Обновить", onRescan)
        }
        MenuAction("Скрытые", onHidden)
        var menuOpen by remember { mutableStateOf(false) }
        Box {
            Text(
                "≡", // «бургер» U+2261 — есть во всех шрифтах, в отличие от ⟳/⋮
                color = Theme.textDim, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .hoverClick(Theme.chipShape) { menuOpen = true }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = Theme.card,
            ) {
                DropdownMenuItem(
                    text = { Text("Добавить папку сканирования", color = Theme.text, fontSize = 13.sp) },
                    onClick = { menuOpen = false; onAddRoot() },
                )
                DropdownMenuItem(
                    text = { Text("О программе", color = Theme.text, fontSize = 13.sp) },
                    onClick = { menuOpen = false; onAbout() },
                )
                DropdownMenuItem(
                    text = { Text("Документация Claude Code ↗", color = Theme.accent, fontSize = 13.sp) },
                    onClick = { menuOpen = false; openDocs() },
                )
                HorizontalDivider(color = Theme.stroke, modifier = Modifier.padding(vertical = 2.dp))
                DropdownMenuItem(
                    text = { Text("Выход", color = Theme.danger, fontSize = 13.sp) },
                    onClick = { menuOpen = false; onExit() },
                )
            }
        }
    }
}

@Composable
private fun MenuAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Theme.textDim, fontSize = 13.sp,
        modifier = Modifier.hoverClick(Theme.chipShape, onClick).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
