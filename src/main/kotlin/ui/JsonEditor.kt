package ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
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
import app.Diagnostic
import app.Severity

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

/** Layer over JsonHighlight: syntax colors stay, translucent problem backgrounds are added on top. */
class ProblemHighlight(private val problems: List<Diagnostic>) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val base = JsonHighlight.filter(text)
        val builder = AnnotatedString.Builder()
        builder.append(base.text)
        for (d in problems) {
            if (d.length <= 0 || d.offset + d.length > text.length) continue
            val bg = if (d.severity == Severity.ERROR) ERROR_BG else WARN_BG
            builder.addStyle(SpanStyle(background = bg), d.offset, d.offset + d.length)
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    companion object {
        val ERROR_BG = Theme.danger.copy(alpha = 0.25f)
        val WARN_BG = Theme.warn.copy(alpha = 0.18f)
    }
}

/**
 * Multiline monospace JSON editor. The field sizes to content inside a scrollable
 * Box, so the scrollbar tracks the text and caret bring-into-view still scrolls it.
 * problems get inline highlight; revealLine/revealTick scroll to a line on click in the problem list.
 */
@Composable
fun JsonEditor(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    problems: List<Diagnostic> = emptyList(),
    revealLine: Int? = null,
    revealTick: Int = 0,
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    LaunchedEffect(revealLine, revealTick) {
        if (revealLine != null) {
            // lineHeight 18sp выше естественных метрик моно-шрифтов (≤1.5×12sp) — строка ровно 18sp; + верхний отступ
            val linePx = with(density) { 18.sp.toPx() }
            val padTopPx = with(density) { 10.dp.toPx() }
            scrollState.animateScrollTo((padTopPx + revealLine * linePx).toInt().coerceIn(0, scrollState.maxValue))
        }
    }
    Box(modifier.background(Theme.bg0, Theme.chipShape)) {
        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            Row {
                // гуттер: тот же шрифт и lineHeight, что у поля, — строковые боксы совпадают пиксель в пиксель
                // (lineHeight 18sp > естественной высоты моно ≤1.5×12sp: сжатия ниже метрик шрифта не бывает)
                Column(
                    Modifier.width(36.dp).padding(start = 8.dp, top = 10.dp, bottom = 10.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    val lines = text.count { it == '\n' } + 1
                    for (i in 1..lines) {
                        Text(
                            i.toString(),
                            color = Theme.textDim.copy(alpha = 0.55f),
                            fontSize = 12.sp, lineHeight = 18.sp, fontFamily = FontFamily.Monospace,
                        )
                    }
                }
                Column(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                    // поле внутри horizontalScroll получает бесконечную ширину — переноса строк не возникает,
                    // одна логическая строка = одна визуальная (иначе гуттер номеров «отстаёт»); см. #8736 про overload'и
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 10.dp, end = 10.dp),
                        textStyle = TextStyle(color = Theme.text, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp),
                        visualTransformation = ProblemHighlight(problems), // ponytail: re-lexes the whole text per keystroke; fine for KB-size configs
                        cursorBrush = SolidColor(Theme.accent),
                    )
                }
            }
        }
        VerticalScrollbar(
            rememberScrollbarAdapter(scrollState),
            Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            style = AppScrollbarStyle(),
        )
    }
}
