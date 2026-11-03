package ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import app.Diagnostic
import app.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProblemHighlightTest {

    @Test
    fun errorRangeGetsDangerBackground() {
        val t = ProblemHighlight(listOf(Diagnostic(Severity.ERROR, 5, 3, "Дубликат ключа")))
        val spans = t.filter(AnnotatedString("""{"a": 1}""")).text.spanStyles
        val bg = spans.first { it.start == 5 && it.end == 8 }.item.background
        assertEquals(ProblemHighlight.ERROR_BG, bg)
    }

    @Test
    fun warningRangeGetsWarnBackground() {
        val t = ProblemHighlight(listOf(Diagnostic(Severity.WARNING, 1, 3, "Неизвестный ключ")))
        val spans = t.filter(AnnotatedString("""{"a": 1}""")).text.spanStyles
        val span = spans.first { it.item.background == ProblemHighlight.WARN_BG }
        assertEquals(1, span.start)
        assertEquals(4, span.end)
    }

    @Test
    fun syntaxColoringSurvivesDelegation() {
        val t = ProblemHighlight(listOf(Diagnostic(Severity.WARNING, 1, 3, "Неизвестный ключ")))
        val spans = t.filter(AnnotatedString("""{"a": 1}""")).text.spanStyles
        // ключ "a" по-прежнему раскрашен акцентом (делегирование JsonHighlight)
        assertTrue(spans.any { it.item.color == Theme.accent && it.start == 1 && it.end == 4 })
        // и фон проблемы добавлен поверх
        assertTrue(spans.any { it.item.background == ProblemHighlight.WARN_BG && it.start == 1 && it.end == 4 })
    }

    @Test
    fun outOfRangeDiagnosticIsIgnoredSafely() {
        val t = ProblemHighlight(listOf(Diagnostic(Severity.ERROR, 100, 5, "За пределами")))
        val spans = t.filter(AnnotatedString("""{"a": 1}""")).text.spanStyles
        assertTrue(spans.none { it.item.background != Color.Unspecified })
    }
}
