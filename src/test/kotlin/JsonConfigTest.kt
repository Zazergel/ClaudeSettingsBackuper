package app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JsonConfigTest {

    // --- Валидный JSON ---

    @Test
    fun parsesObjectWithAllValueTypes() {
        val r = parseJson("""{"model": "opus", "n": 3, "b": true, "f": false, "nil": null, "arr": [1, "x"], "obj": {"k": "v"}}""")
        assertTrue(r.errors.isEmpty())
        val root = r.root as JsonNode.Obj
        assertEquals(7, root.entries.size)
        assertEquals("model", root.entries[0].first.value)
        assertEquals("opus", (root.entries[0].second as JsonNode.Str).value)
        assertEquals(3.0, (root.entries[1].second as JsonNode.Num).raw.toDouble())
        assertEquals(true, (root.entries[2].second as JsonNode.Bool).value)
        assertEquals(false, (root.entries[3].second as JsonNode.Bool).value)
        assertTrue(root.entries[4].second is JsonNode.Null)
        assertEquals(2, (root.entries[5].second as JsonNode.Arr).items.size)
        assertEquals("v", ((root.entries[6].second as JsonNode.Obj).entries[0].second as JsonNode.Str).value)
    }

    @Test
    fun decodesEscapeSequences() {
        val r = parseJson("{\"s\": \"a\\\"b\\\\c\\n\\tA\"}")
        assertEquals("a\"b\\c\n\tA", ((r.root as JsonNode.Obj).entries[0].second as JsonNode.Str).value)
    }

    @Test
    fun decodesUnicodeEscapesAndSurrogatePairs() {
        val r = parseJson("""{"e": "A😀"}""")
        // A = A, суррогатная пара 😀 = 😀
        val r2 = parseJson("{\"e\": \"\\u0041\\uD83D\\uDE00\"}")
        assertEquals("A😀", ((r.root as JsonNode.Obj).entries[0].second as JsonNode.Str).value)
        assertEquals("A😀", ((r2.root as JsonNode.Obj).entries[0].second as JsonNode.Str).value)
    }

    @Test
    fun keepsNumbersAsRawStrings() {
        val r = parseJson("""[-1, 3.14, 1e5, -2.5E-3]""")
        val raws = (r.root as JsonNode.Arr).items.map { (it as JsonNode.Num).raw }
        assertEquals(listOf("-1", "3.14", "1e5", "-2.5E-3"), raws)
    }

    @Test
    fun parsesEmptyContainers() {
        val o = parseJson("""{}""").root as JsonNode.Obj
        val a = parseJson("""[]""").root as JsonNode.Arr
        assertEquals(0, o.entries.size)
        assertEquals(0, a.items.size)
    }

    @Test
    fun toleratesBomAndCrlf() {
        val r = parseJson("{\r\n  \"a\": \"v\"\r\n}")
        assertTrue(r.errors.isEmpty())
        val r2 = parseJson("﻿{\"a\": \"v\"}")
        assertTrue(r2.errors.isEmpty())
    }

    @Test
    fun nodeOffsetsPointIntoSourceText() {
        val r = parseJson("""{"a": 1}""")
        val root = r.root as JsonNode.Obj
        assertEquals(0, root.offset)
        assertEquals(8, root.length)
        val key = root.entries[0].first
        assertEquals(1, key.offset)
        assertEquals(3, key.length)
        val value = root.entries[0].second
        assertEquals(6, value.offset)
        assertEquals(1, value.length)
    }

    // --- Синтаксические ошибки ---

    @Test
    fun toleratesTrailingCommaInArrayWithWarning() {
        val r = parseJson("""[1,]""")
        assertTrue(r.errors.isEmpty(), "ожидается успех, ошибки: ${r.errors}")
        assertEquals(1, r.warnings.size)
        assertEquals(1, (r.root as JsonNode.Arr).items.size)
    }

    @Test
    fun toleratesTrailingCommaInObjectWithWarning() {
        val r = parseJson("""{"a": 1,}""")
        assertTrue(r.errors.isEmpty())
        assertEquals(1, r.warnings.size)
        assertEquals(1, (r.root as JsonNode.Obj).entries.size)
    }

    @Test
    fun rejectsUnclosedString() {
        assertTrue(parseJson("""{"a": "abc}""").errors.isNotEmpty())
    }

    @Test
    fun rejectsUnclosedBrace() {
        assertTrue(parseJson("""{"a": 1""").errors.isNotEmpty())
    }

    @Test
    fun rejectsGarbageAfterValue() {
        val r = parseJson("""{"a": 1} x""")
        assertTrue(r.errors.isNotEmpty())
    }

    @Test
    fun rejectsUnquotedKey() {
        assertTrue(parseJson("""{a: 1}""").errors.isNotEmpty())
    }

    @Test
    fun rejectsMalformedNumbers() {
        for (text in listOf("""{"a":01}""", """{"a":1.}""", """{"a":-}""", """{"a":NaN}""")) {
            assertTrue(parseJson(text).errors.isNotEmpty(), "Ожидалась ошибка для $text")
        }
    }

    @Test
    fun rejectsControlCharInsideString() {
        val r = parseJson("{\"a\": \"x\ny\"}")
        assertTrue(r.errors.isNotEmpty())
    }

    @Test
    fun rejectsInvalidEscape() {
        assertTrue(parseJson("""{"a": "\q"}""").errors.isNotEmpty())
    }

    @Test
    fun rejectsEmptyAndWhitespaceOnlyInput() {
        assertTrue(parseJson("").errors.isNotEmpty())
        assertTrue(parseJson("   ").errors.isNotEmpty())
    }

    @Test
    fun enforcesDepthLimit() {
        val ok = "[".repeat(100) + "]".repeat(100)
        val tooDeep = "[".repeat(101) + "]".repeat(101)
        assertTrue(parseJson(ok).errors.isEmpty())
        assertTrue(parseJson(tooDeep).errors.isNotEmpty())
    }

    // --- Дубли ключей ---

    @Test
    fun rejectsDuplicateKeyAtSecondOccurrenceOffset() {
        val text = """{"a": 1, "a": 2}"""
        val r = parseJson(text)
        assertEquals(1, r.errors.size)
        val e = r.errors[0]
        assertEquals(Severity.ERROR, e.severity)
        assertEquals(9, e.offset) // позиция второго "a" (включая кавычки)
        assertEquals(3, e.length)
    }

    @Test
    fun rejectsDuplicateKeyInNestedObject() {
        val r = parseJson("""{"x": {"a": 1, "a": 2}}""")
        assertEquals(1, r.errors.size)
    }

    // --- Писатель ---

    @Test
    fun roundTripsCanonicalJson() {
        val t = "{\n  \"a\": 1,\n  \"b\": [\n    1,\n    2\n  ],\n  \"c\": {\n    \"d\": \"v\"\n  }\n}"
        val out = writeJson(parseJson(t).root!!)
        assertEquals(t, out)
    }

    @Test
    fun writeIsIdempotent() {
        val t = """{"b": [1, 2], "a": {"z": true, "y": null}, "e": {}}"""
        val once = writeJson(parseJson(t).root!!)
        val twice = writeJson(parseJson(once).root!!)
        assertEquals(once, twice)
    }

    @Test
    fun doesNotEscapeCyrillic() {
        val out = writeJson(parseJson("""{"имя": "значение"}""").root!!)
        assertTrue("значение" in out)
        assertTrue("\\u" !in out)
    }

    @Test
    fun escapesQuotesBackslashAndControlCharsOnWrite() {
        val out = writeJson(parseJson("{\"s\": \"a\\\"b\\n\"}").root!!)
        assertTrue("\"a\\\"b\\n\"" in out)
    }
}
