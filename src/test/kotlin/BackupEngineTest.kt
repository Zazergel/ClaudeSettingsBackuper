import app.BackupEngine
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.io.TempDir

class BackupEngineTest {
    @TempDir lateinit var tmp: Path
    private val settings: Path get() = tmp.resolve("settings.json")

    @Test
    fun backupCreatesTimestampedSiblingCopy() {
        Files.writeString(settings, "orig")
        val stamp = ZonedDateTime.of(2026, 10, 15, 11, 30, 5, 0, ZoneId.of("UTC"))
        val backup = BackupEngine.backup(settings, stamp)
        assertEquals("settings.json.backup-2026-10-15_11-30-05-000", backup.fileName.toString())
        assertEquals("orig", Files.readString(backup))
    }

    @Test
    fun labeledBackupsSanitizeUnsafeCharsAndSkipBlankLabels() {
        Files.writeString(settings, "v")
        val stamp = ZonedDateTime.of(2026, 10, 15, 11, 30, 5, 123_000_000, ZoneId.of("UTC"))
        assertEquals(
            "settings.json.backup-2026-10-15_11-30-05-123_before-restore",
            BackupEngine.backup(settings, stamp, "before:restore").fileName.toString(),
        )
        assertEquals(
            "settings.json.backup-2026-10-15_11-30-05-123",
            BackupEngine.backup(settings, stamp, "   ").fileName.toString(),
        )
    }

    @Test
    fun twoBackupsInSameSecondBothSurvive() {
        Files.writeString(settings, "v")
        val base = ZonedDateTime.of(2026, 10, 15, 11, 30, 5, 0, ZoneId.of("UTC"))
        BackupEngine.backup(settings, base)
        BackupEngine.backup(settings, base.plusNanos(400_000_000))
        assertEquals(2, BackupEngine.listBackups(settings).size)
    }

    @Test
    fun listBackupsSortedNewestFirst() {
        Files.writeString(settings, "v")
        listOf("2026-01-01_10-00-00", "2026-03-01_10-00-00", "2026-02-01_10-00-00")
            .forEach { Files.writeString(tmp.resolve("settings.json.backup-$it"), "b") }
        Files.writeString(tmp.resolve("settings.json.backup-junk"), "x")
        Files.writeString(tmp.resolve("other.txt"), "y")
        assertEquals(
            listOf("2026-03-01_10-00-00", "2026-02-01_10-00-00", "2026-01-01_10-00-00"),
            BackupEngine.listBackups(settings)
                .map { it.fileName.toString().removePrefix("settings.json.backup-") }
        )
    }

    @Test
    fun listBackupsReadsLegacyMillisAndLabeledNames() {
        Files.writeString(settings, "v")
        listOf(
            "settings.json.backup-2025-01-01_10-00-00",              // legacy second-precision
            "settings.json.backup-2026-03-01_10-00-00-250",          // millisecond
            "settings.json.backup-2026-02-01_10-00-00-100_до-правки", // labeled
        ).forEach { Files.writeString(tmp.resolve(it), "b") }
        Files.writeString(tmp.resolve("settings.json.backup-junk"), "x")
        assertEquals(
            listOf("2026-03-01_10-00-00-250", "2026-02-01_10-00-00-100_до-правки", "2025-01-01_10-00-00"),
            BackupEngine.listBackups(settings)
                .map { it.fileName.toString().removePrefix("settings.json.backup-") }
        )
    }

    @Test
    fun restoreOverwritesTarget() {
        val backup = tmp.resolve("settings.json.backup-2026-01-01_00-00-00")
        Files.writeString(backup, "old-version")
        Files.writeString(settings, "current")
        BackupEngine.restore(backup, settings)
        assertEquals("old-version", Files.readString(settings))
    }

    @Test
    fun restoreCreatesMissingTarget() {
        val backup = tmp.resolve("settings.json.backup-2026-01-01_00-00-00")
        Files.writeString(backup, "rescued")
        BackupEngine.restore(backup, settings)
        assertEquals("rescued", Files.readString(settings))
    }
}
