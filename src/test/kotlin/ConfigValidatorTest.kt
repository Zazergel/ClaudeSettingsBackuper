package app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigValidatorTest {

    private fun warnings(text: String) = validateConfig(text).filter { it.severity == Severity.WARNING }

    @Test
    fun validConfigProducesNoDiagnostics() {
        val text = """
            {
              "model": "opus",
              "permissions": {
                "allow": ["Bash"],
                "defaultMode": "acceptEdits"
              },
              "env": {"FOO": "1"}
            }
        """.trimIndent()
        assertEquals(emptyList(), validateConfig(text))
    }

    @Test
    fun unknownTopLevelKeyWarns() {
        val w = warnings("""{"banana": 1}""")
        assertEquals(1, w.size)
        assertTrue("Неизвестный ключ" in w[0].message)
    }

    @Test
    fun deprecatedKeyWarnsWithReplacement() {
        val w = warnings("""{"includeCoAuthoredBy": false}""")
        assertEquals(1, w.size)
        assertTrue("устарел" in w[0].message && "attribution" in w[0].message)
    }

    @Test
    fun managedOnlyKeyWarns() {
        val w = warnings("""{"allowedChannelPlugins": []}""")
        assertEquals(1, w.size)
        assertTrue("managed-settings" in w[0].message)
    }

    @Test
    fun globalConfigKeyWarns() {
        val w = warnings("""{"copyOnSelect": true}""")
        assertEquals(1, w.size)
        assertTrue(".claude.json" in w[0].message)
    }

    @Test
    fun userScopedKeyWarnsAboutProjectFile() {
        val w = warnings("""{"askUserQuestionTimeout": "60s"}""")
        assertEquals(1, w.size)
        assertTrue("не действует в проектных" in w[0].message)
    }

    @Test
    fun typeMismatchWarns() {
        val w = warnings("""{"model": 1}""")
        assertEquals(1, w.size)
        assertTrue("STRING" in w[0].message)
    }

    @Test
    fun stringListTypeMismatchWarns() {
        val w = warnings("""{"claudeMdExcludes": "docs"}""")
        assertEquals(1, w.size)
        assertTrue("STRING_LIST" in w[0].message)
    }

    @Test
    fun enumMismatchWarns() {
        val w = warnings("""{"effortLevel": "ultra"}""")
        assertEquals(1, w.size)
        assertTrue("ultra" in w[0].message)
    }

    @Test
    fun unknownNestedKeyInPermissionsWarns() {
        val w = warnings("""{"permissions": {"banana": 1}}""")
        assertEquals(1, w.size)
        assertTrue("permissions" in w[0].message)
    }

    @Test
    fun unknownHookEventWarns() {
        val w = warnings("""{"hooks": {"OnToolUse": []}}""")
        assertEquals(1, w.size)
        assertTrue("hooks" in w[0].message)
    }

    @Test
    fun envNonStringValueWarns() {
        val w = warnings("""{"env": {"FOO": 1}}""")
        assertEquals(1, w.size)
        assertTrue("строкой" in w[0].message)
    }

    @Test
    fun envBadNameWarns() {
        val w = warnings("""{"env": {"1BAD": "x"}}""")
        assertEquals(1, w.size)
    }

    @Test
    fun rootNotObjectWarns() {
        val w = warnings("""[1, 2]""")
        assertEquals(1, w.size)
        assertTrue("объектом" in w[0].message)
    }

    @Test
    fun brokenJsonReturnsOnlyParseErrors() {
        val d = validateConfig("""{"a": "b""")
        assertTrue(d.isNotEmpty())
        assertTrue(d.all { it.severity == Severity.ERROR })
    }

    @Test
    fun trailingCommaIsWarningNotError() {
        val d = validateConfig("""{"a": 1,}""")
        assertTrue(d.none { it.severity == Severity.ERROR })
        assertTrue(d.any { "запятая" in it.message })
    }

    @Test
    fun defaultModeBypassWarnsAboutUserManagedOnly() {
        val w = warnings("""{"permissions": {"defaultMode": "bypassPermissions"}}""")
        assertEquals(1, w.size)
        assertTrue("user/managed" in w[0].message)
    }

    @Test
    fun defaultModeInvalidValueWarns() {
        val w = warnings("""{"permissions": {"defaultMode": "ultra"}}""")
        assertEquals(1, w.size)
        assertTrue("недопустимое" in w[0].message)
    }

    @Test
    fun schemaKeyIsSilent() {
        assertEquals(0, warnings("""{"${'$'}schema": "https://json.schemastore.org/claude-code-settings.json"}""").size)
    }
}
