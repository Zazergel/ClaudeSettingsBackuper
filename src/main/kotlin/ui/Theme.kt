package ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object Theme {
    val bg0 = Color(0xFF0B1020)
    val bg1 = Color(0xFF141B2E)
    val card = Color(0xFF18203A)
    val cardHover = Color(0xFF1E2947)
    val stroke = Color(0xFF2A3554)
    val accent = Color(0xFF8B7CFF)
    val accent2 = Color(0xFF4FC3F7)
    val text = Color(0xFFE7EBF7)
    val textDim = Color(0xFF96A0BC)
    val ok = Color(0xFF6FDBA8)
    val warn = Color(0xFFFFC46B)

    /** Smooth diagonal wash behind the whole window. */
    val background: Brush = Brush.linearGradient(listOf(bg0, bg1, Color(0xFF101730)))
    val accentGradient: Brush = Brush.linearGradient(listOf(accent, accent2))
    val selectedGradient: Brush = Brush.linearGradient(listOf(Color(0xFF2A2455), Color(0xFF1C2A4E)))
    val clearGradient: Brush = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))

    val cardShape = RoundedCornerShape(14.dp)
    val chipShape = RoundedCornerShape(8.dp)
    val buttonShape = RoundedCornerShape(10.dp)
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    isSystemInDarkTheme() // app is dark-first; kept for future light scheme
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Theme.accent,
            background = Theme.bg0,
            surface = Theme.card,
            onBackground = Theme.text,
            onSurface = Theme.text,
            outline = Theme.stroke,
        ),
        content = content,
    )
}
