package ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colorizes JSON tokens with the app palette; text itself is left untouched. */
object JsonHighlight : VisualTransformation {
    private val keyStyle = SpanStyle(color = Theme.accent, fontWeight = FontWeight.SemiBold)
    private val stringStyle = SpanStyle(color = Theme.ok)
    private val numberStyle = SpanStyle(color = Theme.accent2)
    private val literalStyle = SpanStyle(color = Theme.warn)

    private val token = Regex("\"(?:\\\\.|[^\"\\\\])*\"|-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?|\\b(?:true|false|null)\\b")

    override fun filter(text: AnnotatedString): TransformedText {
        val s = text.text
        val builder = AnnotatedString.Builder(s)
        for (m in token.findAll(s)) {
            val style = when {
                m.value.startsWith("\"") ->
                    if (s.drop(m.range.last + 1).trimStart().startsWith(":")) keyStyle else stringStyle
                m.value.first() == '-' || m.value.first().isDigit() -> numberStyle
                else -> literalStyle
            }
            builder.addStyle(style, m.range.first, m.range.last + 1)
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

/**
 * Multiline monospace JSON editor. The field sizes to content inside a scrollable
 * Box, so the scrollbar tracks the text and caret bring-into-view still scrolls it.
 */
@Composable
fun JsonEditor(text: String, onTextChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    Box(modifier.background(Theme.bg0, Theme.chipShape)) {
        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                textStyle = TextStyle(color = Theme.text, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                visualTransformation = JsonHighlight, // ponytail: re-lexes the whole text per keystroke; fine for KB-size configs
                cursorBrush = SolidColor(Theme.accent),
            )
        }
        VerticalScrollbar(
            rememberScrollbarAdapter(scrollState),
            Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            style = AppScrollbarStyle(),
        )
    }
}
