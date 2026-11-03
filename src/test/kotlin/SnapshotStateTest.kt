package app

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SnapshotStateTest {

    @Test
    fun sha256IsDeterministicAndContentSensitive() {
        val a = sha256Hex("hello".toByteArray())
        assertEquals(a, sha256Hex("hello".toByteArray()))
        assertNotEquals(a, sha256Hex("world".toByteArray()))
        assertEquals(64, a.length)
        assertTrue(a.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun snapshotCodecRoundTrips() {
        val snapshot = mapOf(
            "/home/u/proj/.claude/settings.json" to sha256Hex("a".toByteArray()),
            "/home/u/другой/.claude/settings.json" to sha256Hex("b".toByteArray()),
        )
        assertEquals(snapshot, decodeSnapshot(encodeSnapshot(snapshot)))
    }

    @Test
    fun decodeSkipsBrokenLines() {
        val lines = encodeSnapshot(mapOf("/ok" to "x".repeat(64))) + listOf("мусор без пробела", "?? bad-hash", "bm90LWJhc2U2NC<?> short")
        assertEquals(mapOf("/ok" to "x".repeat(64)), decodeSnapshot(lines))
    }

    @Test
    fun diffClassifiesChangedAddedRemoved() {
        val stored = mapOf("a" to "h1", "b" to "h2", "c" to "h3")
        val current = mapOf("a" to "h1", "b" to "hX", "d" to "h4")
        val d = diff(current, stored)
        assertEquals(listOf("b"), d.changed)
        assertEquals(listOf("d"), d.added)
        assertEquals(listOf("c"), d.removed)
    }

    @Test
    fun diffWithEmptyMaps() {
        val d = diff(emptyMap(), emptyMap())
        assertTrue(d.changed.isEmpty() && d.added.isEmpty() && d.removed.isEmpty())
        val d2 = diff(mapOf("x" to "h"), emptyMap())
        assertEquals(listOf("x"), d2.added)
    }

    @Test
    fun hashesFileContentStably(@TempDir dir: Path) {
        val f = dir.resolve("settings.json")
        f.writeText("""{"a": 1}""")
        val h1 = hashFile(f)
        assertEquals(h1, hashFile(f))
        f.writeText("""{"a": 2}""")
        assertNotEquals(h1, hashFile(f))
    }

    @Test
    fun hashOfMissingFileIsNull(@TempDir dir: Path) {
        assertEquals(null, hashFile(dir.resolve("нет.json")))
    }
}
