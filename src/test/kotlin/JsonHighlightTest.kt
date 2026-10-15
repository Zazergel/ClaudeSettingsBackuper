import androidx.compose.ui.text.AnnotatedString
import ui.JsonHighlight
import ui.Theme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonHighlightTest {
    private fun colorAt(json: String, index: Int) =
        JsonHighlight.filter(AnnotatedString(json)).text.spanStyles
            .first { index in it.start until it.end }.item.color

    @Test
    fun classifiesKeysStringsNumbersLiterals() {
        val json = "{\"model\": \"opus\", \"n\": 42, \"on\": true, \"x\": null}"
        assertEquals(Theme.accent, colorAt(json, json.indexOf("model")))
        assertEquals(Theme.ok, colorAt(json, json.indexOf("opus")))
        assertEquals(Theme.accent2, colorAt(json, json.indexOf("42")))
        assertEquals(Theme.warn, colorAt(json, json.indexOf("true")))
        assertEquals(Theme.warn, colorAt(json, json.indexOf("null")))
    }

    @Test
    fun escapedQuotesAndNegativeNumber() {
        val json = "{\"a\": \"say \\\"hi\\\"\", \"b\": -3.5e2}"
        val t = JsonHighlight.filter(AnnotatedString(json))
        assertEquals(json, t.text.text) // styling only — text untouched
        assertEquals(Theme.accent2, colorAt(json, json.indexOf("-3.5e2")))
    }

    @Test
    fun plainTextHasNoSpans() {
        assertTrue(JsonHighlight.filter(AnnotatedString("hello world")).text.spanStyles.isEmpty())
    }
}
