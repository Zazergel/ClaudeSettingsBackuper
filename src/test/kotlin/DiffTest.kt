package app

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DiffTest {

    @Test
    fun identicalTextsAreAllUnchanged() {
        val rows = lineDiff(listOf("a", "b"), listOf("a", "b"))
        assertEquals(
            listOf(DiffRow.Unchanged("a", 1, 1), DiffRow.Unchanged("b", 2, 2)),
            rows,
        )
    }

    @Test
    fun middleLineReplacedByRemovedAndAdded() {
        val rows = lineDiff(listOf("a", "old", "c"), listOf("a", "new", "c"))
        assertEquals(DiffRow.Unchanged("a", 1, 1), rows[0])
        assertEquals(DiffRow.Removed("old", 2), rows[1])
        assertEquals(DiffRow.Added("new", 2), rows[2])
        assertEquals(DiffRow.Unchanged("c", 3, 3), rows[3])
    }

    @Test
    fun pureInsertAndPureDelete() {
        assertEquals(
            listOf(DiffRow.Unchanged("a", 1, 1), DiffRow.Added("x", 2), DiffRow.Unchanged("b", 2, 3)),
            lineDiff(listOf("a", "b"), listOf("a", "x", "b")),
        )
        assertEquals(
            listOf(DiffRow.Unchanged("a", 1, 1), DiffRow.Removed("x", 2), DiffRow.Unchanged("b", 3, 2)),
            lineDiff(listOf("a", "x", "b"), listOf("a", "b")),
        )
    }

    @Test
    fun emptySides() {
        assertEquals(emptyList(), lineDiff(emptyList(), emptyList()))
        assertEquals(
            listOf(DiffRow.Added("a", 1), DiffRow.Added("b", 2)),
            lineDiff(emptyList(), listOf("a", "b")),
        )
        assertEquals(
            listOf(DiffRow.Removed("a", 1)),
            lineDiff(listOf("a"), emptyList()),
        )
    }

    @Test
    fun trailingNewlineGivesHonestEmptyLastLine() {
        // "a\n" -> ["a", ""]: пустая строка — честная часть диффа, обе стороны получают её как Unchanged
        assertEquals(
            listOf(DiffRow.Unchanged("a", 1, 1), DiffRow.Unchanged("", 2, 2)),
            lineDiff("a\n".split("\n"), "a\n".split("\n")),
        )
    }

    @TempDir
    lateinit var dir: Path

    @Test
    fun shadowRoundTripAndMissing() {
        val shadowBase = Files.createDirectories(dir.resolve("shadow"))
        val f = dir.resolve("proj").resolve("settings.json").also { Files.createDirectories(it.parent) }
        f.writeText("""{"a": 1}""")
        assertNull(readShadow(f, shadowBase)) // до записи — ничего
        writeShadow(f, shadowBase)
        assertEquals("""{"a": 1}""", readShadow(f, shadowBase))
        f.writeText("""{"a": 2}""")
        writeShadow(f, shadowBase) // перезапись при новом принятии
        assertEquals("""{"a": 2}""", readShadow(f, shadowBase))
        deleteShadow(f, shadowBase)
        assertNull(readShadow(f, shadowBase))
        assertTrue(Files.list(shadowBase).use { it.count() } == 0L) // файл удалён, мусора нет
    }
}
