package app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfigTreeTest {

    private val tree = parseJson(
        """{"unknown": "keep", "model": "opus", "permissions": {"defaultMode": "acceptEdits", "allow": ["Bash"]}, "verbose": true}""",
    ).root as JsonNode.Obj

    @Test
    fun valueAtReturnsNestedValueAndNullForMissing() {
        assertEquals("opus", (valueAt(tree, listOf("model")) as JsonNode.Str).value)
        assertEquals("acceptEdits", (valueAt(tree, listOf("permissions", "defaultMode")) as JsonNode.Str).value)
        assertNull(valueAt(tree, listOf("banana")))
        assertNull(valueAt(tree, listOf("model", "sub"))) // путь через не-объект
    }

    @Test
    fun setValueReplacesPreservingOrderAndUnknown() {
        val t2 = setValue(tree, listOf("model"), JsonNode.Str("sonnet", -1, 7))
        assertEquals(4, t2.entries.size) // состав и порядок не изменились
        assertEquals("keep", (t2.entries[0].second as JsonNode.Str).value)
        assertEquals("sonnet", (valueAt(t2, listOf("model")) as JsonNode.Str).value)
    }

    @Test
    fun setValueAppendsMissingTopLevelAtEnd() {
        val t2 = setValue(tree, listOf("theme"), JsonNode.Str("dark", -1, 6))
        assertEquals("theme", t2.entries.last().first.value)
        assertEquals(5, t2.entries.size)
    }

    @Test
    fun setValueCreatesIntermediateObjects() {
        val t2 = setValue(tree, listOf("permissions", "deny"), JsonNode.Arr(emptyList(), -1, 2))
        assertEquals(0, (valueAt(t2, listOf("permissions", "deny")) as JsonNode.Arr).items.size)
        // существующее внутри permissions не потеряно
        assertEquals("acceptEdits", (valueAt(t2, listOf("permissions", "defaultMode")) as JsonNode.Str).value)
    }

    @Test
    fun removeValueRemovesOnlyTargetAndToleratesMissing() {
        val t2 = removeValue(tree, listOf("verbose"))
        assertNull(valueAt(t2, listOf("verbose")))
        assertEquals(3, t2.entries.size)
        assertEquals(tree, removeValue(tree, listOf("banana"))) // отсутствие — дерево без изменений
    }

    @Test
    fun roundTripKeepsUnknownKeysAndValues() {
        val t2 = setValue(tree, listOf("verbose"), JsonNode.Bool(false, -1, 5))
        val out = writeJson(t2)
        assertTrue("\"unknown\": \"keep\"" in out)
        assertTrue("\"verbose\": false" in out)
        assertTrue("\"model\": \"opus\"" in out)
    }
}
