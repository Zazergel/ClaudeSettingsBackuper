import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class ScannerTest {
    @TempDir lateinit var tmp: Path

    /** Creates <tmp>/parts.../.claude/settings.json with content {}. */
    private fun project(vararg parts: String): Path {
        val dir = Path.of(tmp.toString(), *parts)
        Files.createDirectories(dir.resolve(".claude"))
        return Files.writeString(dir.resolve(".claude").resolve("settings.json"), "{}")
    }

    @Test
    fun findsProjectWithSettings() {
        project("proj1")
        val found = Scanner.scan(tmp)
        assertEquals(listOf("proj1"), found.map { it.root.fileName.toString() })
        assertEquals("{}", Files.readString(found.single().settings))
    }

    @Test
    fun findsNestedProject() {
        project("a", "b", "deep-proj")
        assertEquals(listOf("deep-proj"), Scanner.scan(tmp).map { it.root.fileName.toString() })
    }

    @Test
    fun skipsNodeModules() {
        project("node_modules", "vendor-pkg")
        assertTrue(Scanner.scan(tmp).isEmpty())
    }

    @Test
    fun skipsHiddenDirsExceptClaude() {
        project(".hidden-proj")
        assertTrue(Scanner.scan(tmp).isEmpty())
    }

    @Test
    fun ignoresClaudeWithoutSettings() {
        Files.createDirectories(tmp.resolve("proj2").resolve(".claude"))
        assertTrue(Scanner.scan(tmp).isEmpty())
    }

    @Test
    fun missingRootYieldsEmpty() {
        assertTrue(Scanner.scan(tmp.resolve("nope")).isEmpty())
    }
}
