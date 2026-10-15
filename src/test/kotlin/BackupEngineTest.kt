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
        assertEquals("settings.json.backup-2026-10-15_11-30-05", backup.fileName.toString())
        assertEquals("orig", Files.readString(backup))
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
