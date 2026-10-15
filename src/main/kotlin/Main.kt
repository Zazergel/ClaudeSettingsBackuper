import androidx.compose.material3.Text
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Claude Settings Backuper",
        state = rememberWindowState(size = DpSize(1000.dp, 640.dp)),
    ) {
        Text("OK")
    }
}
